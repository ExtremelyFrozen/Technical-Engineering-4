package com.modularmc.ten.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.modularmc.ten.TEN;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Auto-generates compressor pack/split recipes for vanilla items.
 * <p>
 * Separate from {@link TENRecipeGen} to avoid polluting mod material logic
 * with vanilla-specific mappings.
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
            // compress: 9 unit → 1 block/ingot
            futures.add(saveJson(cache, recipeDir.resolve(
                    "cp_" + key(p.unit) + ".json"),
                    buildVanilla(p.unit, p.result, 9, 1, "mould_compressed_large")));
            // split: 1 block/ingot → 9 unit
            futures.add(saveJson(cache, recipeDir.resolve(
                    "sp_" + key(p.result) + ".json"),
                    buildVanilla(p.result, p.unit, 1, 9, "mould_split")));
        }

        for (var p : PACK_4TO1) {
            // small compress: 4 unit → 1 block
            futures.add(saveJson(cache, recipeDir.resolve(
                    "cp4_" + key(p.result) + ".json"),
                    buildVanilla(p.unit, p.result, 4, 1, "mould_compressed_small")));
            // split: 1 block → 4 unit
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
                ingr("item", "static", TEN.MOD_ID + ":" + mould, null, null)));
        j.add("outputs", arr(
                ingr("item", "static", outputId, outputCount, null)));
        j.addProperty("time", 100);
        return j;
    }

    // ── Data definitions ──────────────────────────────────────────────

    private record PackFmt(String unit, String result) {}

    /** 9:1 pairs — only entries NOT covered by mod material system (hasBlock/hasNugget+hasIngot). */
    private static final List<PackFmt> PACK_9TO1 = List.of(
            // Nugget → Ingot (9:1): mod hasNugget=true, hasIngot=false → no mod recipe
            new PackFmt("kenergyengineering:diamond_nugget", "minecraft:diamond"),
            new PackFmt("kenergyengineering:emerald_nugget", "minecraft:emerald"),
            new PackFmt("kenergyengineering:lapis_nugget",   "minecraft:lapis_lazuli"),
            new PackFmt("kenergyengineering:quartz_nugget",  "minecraft:quartz"),
            // Ingot → Block (9:1): mod hasBlock=false → no mod recipe
            new PackFmt("minecraft:iron_ingot",        "minecraft:iron_block"),
            new PackFmt("minecraft:gold_ingot",        "minecraft:gold_block"),
            new PackFmt("minecraft:copper_ingot",      "minecraft:copper_block"),
            new PackFmt("minecraft:diamond",           "minecraft:diamond_block"),
            new PackFmt("minecraft:emerald",           "minecraft:emerald_block"),
            new PackFmt("minecraft:lapis_lazuli",      "minecraft:lapis_block"),
            new PackFmt("minecraft:quartz",            "minecraft:quartz_block"),
            new PackFmt("minecraft:netherite_ingot",   "minecraft:netherite_block"),
            new PackFmt("minecraft:redstone",          "minecraft:redstone_block"));

    /** 4:1 pairs: unit → block. */
    private static final List<PackFmt> PACK_4TO1 = List.of(
            new PackFmt("minecraft:snowball",         "minecraft:snow_block"),
            new PackFmt("minecraft:amethyst_shard",   "minecraft:amethyst_block"));

    private static String key(String id) {
        return id.replace(':', '_');
    }

    // ── JSON helpers ─────────────────────────────────────────────────

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
