package com.modularmc.ten.data;

import com.modularmc.ten.TEN;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 为原版物品生成压缩机压缩与拆分配方。
 * <p>
 * 与 {@link TENRecipeGen} 分开维护，原版物品映射不混入模组材料逻辑。
 */
public class TENVanillaPackGen implements DataProvider {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final PackOutput output;

    public TENVanillaPackGen(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        Path recipeDir = output.getOutputFolder()
                .resolve("data/" + TEN.MOD_ID + "/recipe/compressor");

        List<CompletableFuture<?>> futures = new ArrayList<>();

        for (var p : PACK_9TO1) {
            // 压缩：9 单位 → 1 方块或锭。
            futures.add(saveJson(cache, recipeDir.resolve(
                    "cp_" + key(p.unit) + ".json"),
                    buildVanilla(p.unit, p.result, 9, 1, "mould_compressed_large")));
            // 拆分：1 方块或锭 → 9 单位。
            futures.add(saveJson(cache, recipeDir.resolve(
                    "sp_" + key(p.result) + ".json"),
                    buildVanilla(p.result, p.unit, 1, 9, "mould_split")));
        }

        for (var p : PACK_4TO1) {
            // 小型压缩：4 单位 → 1 方块。
            futures.add(saveJson(cache, recipeDir.resolve(
                    "cp4_" + key(p.result) + ".json"),
                    buildVanilla(p.unit, p.result, 4, 1, "mould_compressed_small")));
            // 拆分：1 方块 → 4 单位。
            futures.add(saveJson(cache, recipeDir.resolve(
                    "sp4_" + key(p.result) + ".json"),
                    buildVanilla(p.result, p.unit, 1, 4, "mould_split")));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    @Override
    public String getName() {
        return "Vanilla Pack/Split Recipes";
    }

    private CompletableFuture<?> saveJson(CachedOutput cache, Path path, JsonObject json) {
        path.getParent().toFile().mkdirs();
        return DataProvider.saveStable(cache, json, path);
    }

    private JsonObject buildVanilla(String inputId, String outputId, int inputCount, int outputCount, String mould) {
        var j = new JsonObject();
        j.addProperty("type", TEN.MOD_ID + ":compressor");
        j.add("inputs", arr(
                ingr("item", "static", inputId, inputCount, null),
                // chance=0.0 表示催化位，只参与匹配且不消耗。
                ingr("item", "static", TEN.MOD_ID + ":" + mould, null, 0.0)));
        j.add("outputs", arr(
                ingr("item", "static", outputId, outputCount, null)));
        j.addProperty("time", 100);
        return j;
    }

    // ── 数据定义 ──────────────────────────────────────────────

    private record PackFmt(String unit, String result) {}

    /** 9:1 配对，仅收录模组材料体系未覆盖的条目（hasBlock/hasNugget+hasIngot）。 */
    private static final List<PackFmt> PACK_9TO1 = List.of(
            // 粒→锭（9:1）：mod hasNugget=true、hasIngot=false，无模组配方。
            new PackFmt("kenergyengineering:diamond_nugget", "minecraft:diamond"),
            new PackFmt("kenergyengineering:emerald_nugget", "minecraft:emerald"),
            new PackFmt("kenergyengineering:lapis_nugget", "minecraft:lapis_lazuli"),
            new PackFmt("kenergyengineering:quartz_nugget", "minecraft:quartz"),
            // 锭→方块（9:1）：mod hasBlock=false，无模组配方。
            new PackFmt("minecraft:iron_ingot", "minecraft:iron_block"),
            new PackFmt("minecraft:gold_ingot", "minecraft:gold_block"),
            new PackFmt("minecraft:copper_ingot", "minecraft:copper_block"),
            new PackFmt("minecraft:diamond", "minecraft:diamond_block"),
            new PackFmt("minecraft:emerald", "minecraft:emerald_block"),
            new PackFmt("minecraft:lapis_lazuli", "minecraft:lapis_block"),
            new PackFmt("minecraft:quartz", "minecraft:quartz_block"),
            new PackFmt("minecraft:netherite_ingot", "minecraft:netherite_block"),
            new PackFmt("minecraft:redstone", "minecraft:redstone_block"),
            // 粗矿→方块（9:1）：原版粗矿不在模组材料体系内。
            new PackFmt("minecraft:raw_iron", "minecraft:raw_iron_block"),
            new PackFmt("minecraft:raw_gold", "minecraft:raw_gold_block"),
            new PackFmt("minecraft:raw_copper", "minecraft:raw_copper_block"));

    /** 4:1 配对：单位 → 方块。 */
    private static final List<PackFmt> PACK_4TO1 = List.of(
            new PackFmt("minecraft:snowball", "minecraft:snow_block"),
            new PackFmt("minecraft:amethyst_shard", "minecraft:amethyst_block"));

    private static String key(String id) {
        return id.replace(':', '_');
    }

    // ── JSON 辅助方法 ─────────────────────────────────────────────────

    private static JsonObject ingr(String form, String type, String key, Integer count, Double chance) {
        var o = new JsonObject();
        o.addProperty("form", form);
        o.addProperty("type", type);
        o.addProperty("key", key);
        if (count != null) o.addProperty("count", count);
        if (chance != null) o.addProperty("chance", chance);
        return o;
    }

    private static JsonArray arr(Object... items) {
        var a = new JsonArray();
        for (var item : items) {
            if (item instanceof JsonElement je) a.add(je);
            else a.add(item.toString());
        }
        return a;
    }
}
