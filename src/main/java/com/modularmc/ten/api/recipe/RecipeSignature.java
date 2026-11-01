package com.modularmc.ten.api.recipe;

import com.modularmc.ten.TEN;
import com.modularmc.ten.common.data.TENRecipeTypes;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        Variant variant = exclusiveVariant(json);
        if (variant == null) {
            ChainOwner owner = CHAIN_GROUPS.get(sig);
            if (owner != null) {
                throw conflict(sig, owner.path(), path, "无条件配方与互斥变体组（" + owner.group() + "）抢同一输入");
            }
            claimKey(CLAIMS, sig, path);
            return;
        }
        String plain = CLAIMS.get(sig);
        if (plain != null) throw conflict(sig, plain, path, "互斥变体组与无条件配方抢同一输入");
        ChainOwner owner = CHAIN_GROUPS.putIfAbsent(sig, new ChainOwner(variant.group(), path));
        if (owner != null && !owner.group().equals(variant.group())) {
            throw conflict(sig, owner.path(), path,
                    "同一输入挂了两个不同的候选模组组（" + owner.group() + " / " + variant.group() + "）");
        }
        claimKey(CLAIMS, sig + "#" + variant.group() + "#" + variant.mod(), path);
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
