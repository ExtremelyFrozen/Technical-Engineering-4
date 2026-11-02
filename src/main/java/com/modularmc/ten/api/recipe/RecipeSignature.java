package com.modularmc.ten.api.recipe;

import com.modularmc.ten.TEN;
import com.modularmc.ten.common.data.TENRecipeTypes;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 机器配方的输入签名：同一台机器里签名相同的两条配方可同时匹配同一组输入，
 * 而 {@code findRecipe} 取第一条命中，其余形同不存在。兼容层是机械导入的，
 * 邻居更新后重跑转换器最容易撞车，所以判定收在一处、生成期与运行期共用同一算法。
 */
public final class RecipeSignature {

    private RecipeSignature() {}

    /** 生成期登记：签名（变体为「签名#候选组#模组」）→ 首个占用它的文件路径。 */
    private static final Map<String, String> CLAIMS = new ConcurrentHashMap<>();
    /** 互斥变体组登记：签名 → 首个占用该输入的候选模组组与其文件（一个输入只允许一条链）。 */
    private static final Map<String, ChainOwner> CHAIN_GROUPS = new ConcurrentHashMap<>();
    /** 输出路径 → 首个写到该路径的签名（拦生成器的同名覆盖）。 */
    private static final Map<String, String> PATHS = new ConcurrentHashMap<>();
    /**
     * 同签名下的条件快照。分流表用 {@code mod_loaded(X)} 与 {@code not(mod_loaded(X))} 成对表达
     * 「装了 A 走 A，否则走 B」，两条同签名但加载期互斥，属于设计内的重复。
     * 判定只看顶层条件：无条件配方（正负集合都空）与任何同签名配方都能同时成立，照旧算冲突。
     */
    private static final Map<String, List<CondEntry>> COND_CLAIMS = new ConcurrentHashMap<>();

    /**
     * 运行期用：只算输入部分，机器类型由调用方分组（见 {@code TENRecipeAudit}）。
     * 序列化器补的空槽占位（ALLOW_ALL）不算输入，否则同一份 JSON 在两侧得到不同签名。
     */
    public static String ofInputs(List<FormsCombinedIngredient> inputs) {
        List<String> terms = new ArrayList<>();
        for (var in : inputs) {
            if (in.isAllowAll()) continue;
            terms.add(in.form() + ":" + in.key() + "x" + in.amountOrCount() + "p" + in.chance());
        }
        terms.sort(String::compareTo);
        return String.join(",", terms);
    }

    /**
     * 生成期用：返回「类型|输入签名」；非本模组机器配方（原版合成、烧炼）返回 {@code null}。
     * 数量与概率的读法对齐 {@link FormsCombinedRecipeSerializer}：流体取 {@code amount}（缺省 0）、
     * 物品取 {@code count}（缺省 1）、{@code chance} 缺省 1。
     */
    public static String ofJson(JsonObject json) {
        if (!json.has("type") || !json.has("inputs") || !isMachineType(json.get("type").getAsString())) return null;
        List<String> terms = new ArrayList<>();
        for (var e : json.getAsJsonArray("inputs")) {
            JsonObject o = e.getAsJsonObject();
            String form = o.has("form") ? o.get("form").getAsString() : "item";
            String key = o.has("key") ? o.get("key").getAsString() : "";
            int qty = o.has("count") ? o.get("count").getAsInt() : o.has("amount") ? o.get("amount").getAsInt() : 1;
            double chance = o.has("chance") ? o.get("chance").getAsDouble() : 1.0;
            terms.add(form + ":" + key + "x" + qty + "p" + chance);
        }
        terms.sort(String::compareTo);
        return json.get("type").getAsString() + "|" + String.join(",", terms);
    }

    /**
     * 生成期断言：同一签名落到两个不同文件即抛错让 DataGen 失败——留到实机排查成本高。
     * <p>
     * {@code provider_priority} 的互斥变体组是设计内的重复：同一输入、同一候选模组组的多条配方
     * 在加载期只会剩一条。但同一组里同一模组出现两次、同一输入挂两条不同的候选链，
     * 以及候选链与无条件配方撞同一输入，都仍按冲突处理。
     */
    public static void claim(JsonObject json, String path) {
        String sig = ofJson(json);
        if (sig == null) return;
        // 两条不同配方抢同一个输出文件＝生成器命名有缺陷（邻居按材料分层时只取文件名会撞在一起，
        // 后写的静默覆盖前写的，日志里看不出来）
        String written = PATHS.putIfAbsent(path, sig);
        if (written != null && !written.equals(sig)) {
            throw new IllegalStateException("同一个输出文件被两条不同配方占用：" + shorten(path) + " 先写入 " + written + "，又写入 " + sig);
        }
        Variant variant = exclusiveVariant(json);
        if (variant == null) {
            ChainOwner owner = CHAIN_GROUPS.get(sig);
            if (owner != null) {
                throw conflict(sig, owner.path(), path, "无条件配方与互斥变体组（" + owner.group() + "）抢同一输入");
            }
            claimConditional(sig, json, path);
            return;
        }
        if (COND_CLAIMS.containsKey(sig)) {
            throw conflict(sig, COND_CLAIMS.get(sig).get(0).path(), path, "互斥变体组与条件分流行抢同一输入");
        }
        ChainOwner owner = CHAIN_GROUPS.putIfAbsent(sig, new ChainOwner(variant.group(), path));
        if (owner != null && !owner.group().equals(variant.group())) {
            throw conflict(sig, owner.path(), path,
                    "同一输入挂了两个不同的候选模组组（" + owner.group() + " / " + variant.group() + "）");
        }
        claimKey(CLAIMS, sig + "#" + variant.group() + "#" + variant.mod(), path);
    }

    /**
     * 同签名按条件互斥登记：两条配方的 {@code mod_loaded} 集合互为否定才能共存。
     * 无条件配方（两个集合都空）与任何同签名配方都能同时成立，仍是冲突。
     */
    private static void claimConditional(String sig, JsonObject json, String path) {
        CondEntry cur = condOf(json, path);
        List<CondEntry> list = COND_CLAIMS.computeIfAbsent(sig, k -> new ArrayList<>());
        synchronized (list) {
            for (var prev : list) {
                if (canCoexist(prev, cur)) {
                    throw conflict(sig, prev.path(), path,
                            "机器配方输入签名重复且两条能同时生效（" + prev.describe() + " / " + cur.describe() + "）");
                }
            }
            list.add(cur);
        }
    }

    /** 顶层条件里的 mod_loaded 与 not(mod_loaded) 快照；其他条件（tag_empty、compat_enabled）不参与互斥判定。 */
    private static CondEntry condOf(JsonObject json, String path) {
        Set<String> on = new HashSet<>();
        Set<String> off = new HashSet<>();
        if (json.has("neoforge:conditions") && json.get("neoforge:conditions").isJsonArray()) {
            for (var e : json.getAsJsonArray("neoforge:conditions")) {
                if (!e.isJsonObject()) continue;
                JsonObject o = e.getAsJsonObject();
                if (!o.has("type")) continue;
                String type = o.get("type").getAsString();
                if (type.equals("neoforge:mod_loaded")) {
                    collectMod(o, on);
                } else if (type.equals("neoforge:not") && o.has("value") && o.get("value").isJsonObject()) {
                    // 负侧可能是 mod_loaded，也可能是 and(mod_loaded, compat_enabled)——
                    // 后者表达「该来源可用」，否定它与「该来源加载」互斥，照样记进负集合
                    collectMod(o.getAsJsonObject("value"), off);
                }
            }
        }
        return new CondEntry(Set.copyOf(on), Set.copyOf(off), path);
    }

    /** 收集条件里的 mod_loaded 模组；{@code neoforge:and} 递归展开（只为取模组名，其他条件忽略）。 */
    private static void collectMod(JsonObject c, Set<String> into) {
        if (!c.has("type")) return;
        String t = c.get("type").getAsString();
        if (t.equals("neoforge:mod_loaded") && c.has("modid")) {
            into.add(c.get("modid").getAsString());
        } else if (t.equals("neoforge:and") && c.has("values") && c.get("values").isJsonArray()) {
            for (var e : c.getAsJsonArray("values")) {
                if (e.isJsonObject()) collectMod(e.getAsJsonObject(), into);
            }
        }
    }

    /** 两条能同时成立即冲突：只要没有「一条要 X、另一条要非 X」这种互斥对，加载期就会并存。 */
    private static boolean canCoexist(CondEntry a, CondEntry b) {
        return disjoint(a.on(), b.off()) && disjoint(b.on(), a.off());
    }

    private static boolean disjoint(Set<String> a, Set<String> b) {
        for (String x : a) if (b.contains(x)) return false;
        return true;
    }

    /** 条件快照：正集合、负集合与占用它的文件。 */
    private record CondEntry(Set<String> on, Set<String> off, String path) {
        String describe() {
            StringBuilder sb = new StringBuilder();
            for (String m : on) sb.append("+").append(m);
            for (String m : off) sb.append("-").append(m);
            return sb.length() == 0 ? "无条件" : sb.toString();
        }
    }

    private static void claimKey(Map<String, String> claims, String key, String path) {
        String prev = claims.putIfAbsent(key, path);
        if (prev != null && !prev.equals(path)) throw conflict(key, prev, path, "机器配方输入签名重复");
    }

    private static IllegalStateException conflict(String sig, String prev, String path, String what) {
        return new IllegalStateException(what + "：" + sig + " 同时出现在 " + shorten(prev) + " 与 " + shorten(path));
    }

    /** 生成期登记：签名 → 首个占用它的文件路径。 */
    private record ChainOwner(String group, String path) {}

    /** 变体身份：所属候选模组组 + 本条归属的模组。 */
    private record Variant(String group, String mod) {}

    /** 报错里只留 pack 内路径，绝对前缀太长会把签名挤出日志。 */
    private static String shorten(String path) {
        int cut = path.indexOf("resources/");
        return cut < 0 ? path : path.substring(cut + "resources/".length()).replace('\\', '/');
    }

    /**
     * 该配方所属的互斥变体（{@code provider_priority} 条件），非变体返回 {@code null}。
     * 只看顶层条件数组——生成器一律平铺，不产生 and/or 嵌套。
     */
    private static Variant exclusiveVariant(JsonObject json) {
        if (!json.has("neoforge:conditions") || !json.get("neoforge:conditions").isJsonArray()) return null;
        for (var e : json.getAsJsonArray("neoforge:conditions")) {
            if (!e.isJsonObject()) continue;
            JsonObject o = e.getAsJsonObject();
            if (!o.has("type") || !o.get("type").getAsString().equals(TEN.MOD_ID + ":provider_priority")) continue;
            List<String> candidates = new ArrayList<>();
            for (var c : o.getAsJsonArray("candidates")) candidates.add(c.getAsString());
            return new Variant(String.join("+", candidates), o.get("mod").getAsString());
        }
        return null;
    }

    /** 是否本模组的机器配方类型（跟着 {@link TENRecipeTypes#TYPES} 走，新增机器不用改这里）。 */
    private static boolean isMachineType(String typeId) {
        for (var holder : TENRecipeTypes.TYPES.getEntries()) {
            if (holder.getId().toString().equals(typeId)) return true;
        }
        return false;
    }
}
