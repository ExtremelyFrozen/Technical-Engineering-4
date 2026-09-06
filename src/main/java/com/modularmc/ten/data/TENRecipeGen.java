package com.modularmc.ten.data;

import com.modularmc.ten.TEN;
import com.modularmc.ten.common.data.Mat;

import net.minecraft.core.HolderLookup;
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
 * Auto-generates material variant recipes for every {@link Mat} entry.
 * <p>
 * Writes JSON directly to the data output directory.
 * Vanilla recipes (crafting/smelting) and custom machine recipes
 * (compressor/pulverizer) are all generated as JSON files.
 */
public class TENRecipeGen implements DataProvider {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final PackOutput output;

    public TENRecipeGen(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        Path recipeDir = output.getOutputFolder()
                .resolve("data/" + TEN.MOD_ID + "/recipe");

        List<CompletableFuture<?>> futures = new ArrayList<>();

        for (var mat : Mat.values()) {
            for (var r : COMPRESS) if (mat.hasForm(r.form) && mat.isRegistered(r.form))
                futures.add(saveJson(cache, recipeDir.resolve("compressor/" + mat.id + "_" + r.form + ".json"), buildCompress(mat, r)));

            if (mat.isRegistered("gear"))
                futures.add(saveJson(cache, recipeDir.resolve("vanilla/material/" + mat.id + "_gear.json"), buildShapedGear(mat)));

            for (var r : PULV) {
                boolean canPulv = canPulv(mat, r);
                if (!canPulv && "gems".equals(mat.compressTagCategory()) && "ingots".equals(r.tagCat))
                    canPulv = mat != Mat.REDSTONE;
                if (canPulv)
                    futures.add(saveJson(cache, recipeDir.resolve("pulverizer/metal/" + mat.id + r.suffix() + ".json"), buildPulv(mat, r)));
            }

            // Raw block → 9 dust + 9×40% bonus dust（26.1.2 对齐，roll 掷骰配方）
            if (mat.hasRawBlock()) {
                futures.add(saveJson(cache, recipeDir.resolve("pulverizer/metal/" + mat.id + "_raw_block.json"), buildRawBlockPulv(mat)));
            }

            for (var r : SMELT) if (canSmelt(mat, r)) {
                String inputId = smeltInput(mat, r);
                futures.add(saveJson(cache, recipeDir.resolve("vanilla/burning/" + mat.id + "_" + r.suffix + ".json"), buildSmelting(inputId, mat.itemId(r.output), r.xp, 200)));
                futures.add(saveJson(cache, recipeDir.resolve("vanilla/burning/" + mat.id + "_" + r.suffix + "_blast.json"), buildBlasting(inputId, mat.itemId(r.output), r.xp, 100)));
            }

            for (var r : PACK) if (canPack(mat, r))
                buildPack(cache, recipeDir, mat, r, futures);
            // Large compress (9→1) and split (1→9) for mod materials
            if (mat.hasNugget && mat.hasIngot)
                futures.add(saveJson(cache, recipeDir.resolve("compressor/cp_" + mat.id + "_nugget_ingot.json"), buildMouldPack(mat, "nugget", "ingot", 9, 1, "mould_compressed_large")));
            if (mat.hasBlock && mat.hasIngot) {
                futures.add(saveJson(cache, recipeDir.resolve("compressor/cp_" + mat.id + "_ingot_block.json"), buildMouldPack(mat, "ingot", "block", 9, 1, "mould_compressed_large")));
                futures.add(saveJson(cache, recipeDir.resolve("compressor/sp_" + mat.id + "_block_ingot.json"), buildMouldPack(mat, "block", "ingot", 1, 9, "mould_split")));
            }
            if (mat.hasIngot && mat.hasNugget)
                futures.add(saveJson(cache, recipeDir.resolve("compressor/sp_" + mat.id + "_ingot_nugget.json"), buildMouldPack(mat, "ingot", "nugget", 1, 9, "mould_split")));
            if (mat.hasRaw && mat.hasBlock) {
                futures.add(saveJson(cache, recipeDir.resolve("compressor/cp_" + mat.id + "_raw_block.json"), buildMouldPack(mat, "raw", "raw_block", 9, 1, "mould_compressed_large")));
                futures.add(saveJson(cache, recipeDir.resolve("compressor/sp_" + mat.id + "_block_raw.json"), buildMouldPack(mat, "raw_block", "raw", 1, 9, "mould_split")));
            }
        }

        // Moulds
        for (var m : MOULDS)
            futures.add(saveJson(cache, recipeDir.resolve("vanilla/" + m.id + ".json"), buildMould(m)));

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    @Override
    public String getName() {
        return "Material Variant Recipes";
    }

    private CompletableFuture<?> saveJson(CachedOutput cache, Path path, JsonObject json) {
        path.getParent().toFile().mkdirs();
        return DataProvider.saveStable(cache, json, path);
    }

    // ══════════════════════════════════════════════════════════════════
    // Recipe format templates
    // ══════════════════════════════════════════════════════════════════

    private record CompressFmt(String form, int ingots, String mould, int outputCount) {}

    private static final List<CompressFmt> COMPRESS = List.of(
            new CompressFmt("plate", 1, "mould_plate", 1),
            new CompressFmt("gear", 4, "mould_gear", 1),
            new CompressFmt("rod", 1, "mould_rod", 1),
            new CompressFmt("wire", 1, "mould_string", 2));

    private record PulvFmt(String tagCat, int dustCount, int time, String suffix) {}

    private static final List<PulvFmt> PULV = List.of(
            new PulvFmt("ingots", 1, 100, "_dust"),
            new PulvFmt("ores", 2, 180, "_dust_ore"),
            new PulvFmt("raw_materials", 1, 120, "_dust_raw"));

    private record SmeltFmt(String input, String output, float xp, String suffix) {}

    private static final List<SmeltFmt> SMELT = List.of(
            new SmeltFmt("ore", "ingot", 0.35f, "ingot"),
            new SmeltFmt("deep_ore", "ingot", 0.35f, "ingot_deep"),
            new SmeltFmt("raw", "ingot", 0.35f, "ingot_raw"),
            new SmeltFmt("dust", "ingot", 0f, "ingot_fd"));

    private record PackFmt(String unit, String block) {}

    private static final List<PackFmt> PACK = List.of(
            new PackFmt("ingot", "block"),
            new PackFmt("nugget", "ingot"),
            new PackFmt("raw", "raw_block"));

    private record MouldFmt(String id, String[] pattern) {}

    private static final List<MouldFmt> MOULDS = List.of(
            new MouldFmt("mould_gear", new String[] { "A A", " A ", "A A" }),
            new MouldFmt("mould_plate", new String[] { "A A", "   ", "A A" }),
            new MouldFmt("mould_rod", new String[] { "A A", "A A", "A A" }),
            new MouldFmt("mould_string", new String[] { "AA ", " AA", "AA " }));

    // ══════════════════════════════════════════════════════════════════
    // Conditions
    // ══════════════════════════════════════════════════════════════════

    private static boolean canPulv(Mat mat, PulvFmt r) {
        return switch (r.tagCat) {
            case "ingots" -> mat.hasIngot;
            case "ores" -> mat.hasOre;
            case "raw_materials" -> mat.hasRaw;
            default -> false;
        };
    }

    private static boolean canSmelt(Mat mat, SmeltFmt r) {
        return switch (r.input) {
            case "ore" -> mat.hasOre;
            case "deep_ore" -> mat.hasDeepOre;
            case "raw" -> mat.hasRaw;
            case "dust" -> mat.hasForm("dust") && mat.hasIngot;
            default -> false;
        };
    }

    private static boolean canPack(Mat mat, PackFmt r) {
        boolean hasUnit = switch (r.unit) {
            case "ingot" -> mat.hasIngot;
            case "nugget" -> mat.hasNugget;
            case "raw" -> mat.hasRaw;
            default -> false;
        };
        boolean hasBlock = switch (r.block) {
            case "block" -> mat.hasBlock;
            case "ingot" -> mat.hasIngot;
            case "raw_block" -> mat.hasRaw && mat.hasBlock;
            default -> false;
        };
        return hasUnit && hasBlock;
    }

    private static String smeltInput(Mat mat, SmeltFmt r) {
        return switch (r.input) {
            case "ore" -> mat.itemId("ore");
            case "deep_ore" -> mat.itemId("deep_ore");
            case "raw" -> "kenergyengineering:raw_" + mat.id;
            case "dust" -> mat.itemId("dust");
            default -> mat.itemId(r.input);
        };
    }

    // ══════════════════════════════════════════════════════════════════
    // JSON builders
    // ══════════════════════════════════════════════════════════════════

    private JsonObject buildCompress(Mat mat, CompressFmt r) {
        var j = new JsonObject();
        j.addProperty("type", TEN.MOD_ID + ":compressor");
        String srcType = mat.compressUsesTag() ? "tag" : "item";
        j.add("inputs", arr(
                ingr("item", srcType, mat.compressSource(), r.ingots > 1 ? r.ingots : null, null),
                ingr("item", "static", TEN.MOD_ID + ":" + r.mould, null, 0.0)));
        j.add("outputs", arr(
                ingr("item", "static", mat.itemId(r.form), r.outputCount > 1 ? r.outputCount : null, null)));
        j.addProperty("time", 100);
        return j;
    }

    private JsonObject buildPulv(Mat mat, PulvFmt r) {
        var j = new JsonObject();
        j.addProperty("type", TEN.MOD_ID + ":pulverizer");
        String tagCat = mat.compressTagCategory();
        if (tagCat == null) tagCat = r.tagCat;
        j.add("inputs", arr(ingr("item", "tag", mat.tag(tagCat), null, null)));

        var outs = new JsonArray();
        outs.add(ingr("item", "static", mat.itemId("dust"), r.dustCount, null));
        if (r.suffix.equals("_dust_ore"))
            outs.add(ingr("item", "static", "minecraft:gravel", 1, 0.2));
        else if (r.suffix.equals("_dust_raw"))
            outs.add(ingr("item", "static", mat.itemId("dust"), 1, 0.4));
        j.add("outputs", outs);
        j.addProperty("time", r.time);
        return j;
    }

    private JsonObject buildShapedGear(Mat mat) {
        var j = new JsonObject();
        j.addProperty("type", "minecraft:crafting_shaped");
        j.add("pattern", arr(" C ", "C C", " C "));
        j.add("key", obj("C", ref(mat.itemId("ingot"))));
        j.add("result", obj("id", mat.itemId("gear")));
        return j;
    }

    private JsonObject buildSmelting(String input, String result, float xp, int time) {
        var j = new JsonObject();
        j.addProperty("type", "minecraft:smelting");
        j.add("ingredient", ref(input));
        j.add("result", obj("id", result));
        j.addProperty("experience", xp);
        j.addProperty("cookingtime", time);
        return j;
    }

    private JsonObject buildBlasting(String input, String result, float xp, int time) {
        var j = new JsonObject();
        j.addProperty("type", "minecraft:blasting");
        j.add("ingredient", ref(input));
        j.add("result", obj("id", result));
        j.addProperty("experience", xp);
        j.addProperty("cookingtime", time);
        return j;
    }

    private void buildPack(CachedOutput cache, Path recipeDir, Mat mat, PackFmt r, List<CompletableFuture<?>> futures) {
        String unitId = r.unit.equals("raw") ? "kenergyengineering:raw_" + mat.id : mat.itemId(r.unit);
        String blockId = mat.itemId(r.block);
        String ingBlockId = r.block.equals("ingot") ? mat.itemId("ingot") : blockId;
        String subdir = r.unit.equals("nugget") ? "vanilla/compress/nugget" : "vanilla/compress";
        String suffix = r.unit.equals("raw") ? "_raw" : "";

        // 9 → 1
        var pack = new JsonObject();
        pack.addProperty("type", "minecraft:crafting_shaped");
        pack.add("pattern", arr("CCC", "CCC", "CCC"));
        pack.add("key", obj("C", ref(unitId)));
        pack.add("result", obj("id", ingBlockId));
        futures.add(saveJson(cache, recipeDir.resolve(subdir + "/pb_" + mat.id + suffix + ".json"), pack));

        // 1 → 9
        var unpack = new JsonObject();
        unpack.addProperty("type", "minecraft:crafting_shapeless");
        unpack.add("ingredients", arr(ref(blockId)));
        unpack.add("result", obj("id", unitId, "count", 9));
        futures.add(saveJson(cache, recipeDir.resolve(subdir + "/rb_" + mat.id + suffix + ".json"), unpack));
    }

    private JsonObject buildMould(MouldFmt m) {
        var j = new JsonObject();
        j.addProperty("type", "minecraft:crafting_shaped");
        j.add("pattern", arr((Object[]) m.pattern));
        j.add("key", obj("A", ref("kenergyengineering:tin_ingot")));
        j.add("result", obj("id", TEN.MOD_ID + ":" + m.id));
        return j;
    }

    /** Compressor pack/split recipe: inputForm + mould → outputForm (inputCount → outputCount). */
    private JsonObject buildMouldPack(Mat mat, String inputForm, String outputForm,
                                      int inputCount, int outputCount, String mould) {
        var j = new JsonObject();
        j.addProperty("type", TEN.MOD_ID + ":compressor");
        j.add("inputs", arr(
                ingr("item", "static", mat.itemId(inputForm), inputCount, null),
                ingr("item", "static", TEN.MOD_ID + ":" + mould, null, 0.0)));
        j.add("outputs", arr(
                ingr("item", "static", mat.itemId(outputForm), outputCount, null)));
        j.addProperty("time", 100);
        return j;
    }

    // ══════════════════════════════════════════════════════════════════
    // JSON helpers
    // ══════════════════════════════════════════════════════════════════

    /** Raw block → 9 dust with 9×40% bonus dust（roll 掷骰配方，26.1.2 对齐）。 */
    private JsonObject buildRawBlockPulv(Mat mat) {
        var j = new JsonObject();
        j.addProperty("type", TEN.MOD_ID + ":pulverizer");
        j.add("inputs", arr(ingr("item", "tag", mat.rawBlockTag(), null, null)));
        var outs = new JsonArray();
        outs.add(ingr("item", "static", mat.itemId("dust"), 9, null));
        outs.add(ingr("item", "static", mat.itemId("dust"), 1, 0.4, 9));
        j.add("outputs", outs);
        j.addProperty("time", 900);
        return j;
    }

    private static JsonObject ref(String id) {
        return obj(id.startsWith("c:") ? "tag" : "item", id);
    }

    /** Machine recipe ingredient. 5-param: form, type, key, count, chance. */
    private static JsonObject ingr(String form, String type, String key, Integer count, Double chance) {
        return ingr(form, type, key, count, chance, null);
    }

    /** Machine recipe ingredient. 6-param: form, type, key, count, chance, rolls. */
    private static JsonObject ingr(String form, String type, String key, Integer count, Double chance, Integer rolls) {
        var o = new JsonObject();
        o.addProperty("form", form);
        o.addProperty("type", type);
        o.addProperty("key", key);
        if (count != null) o.addProperty("count", count);
        if (chance != null) o.addProperty("chance", chance);
        if (rolls != null) o.addProperty("rolls", rolls);
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

    private static JsonObject obj(String k1, Object v1, Object... rest) {
        var o = new JsonObject();
        put(o, k1, v1);
        for (int i = 0; i < rest.length; i += 2) put(o, (String) rest[i], rest[i + 1]);
        return o;
    }

    private static void put(JsonObject o, String key, Object value) {
        if (value instanceof String s) o.addProperty(key, s);
        else if (value instanceof Number n) o.addProperty(key, n);
        else if (value instanceof Boolean b) o.addProperty(key, b);
        else if (value instanceof JsonElement e) o.add(key, e);
    }
}
