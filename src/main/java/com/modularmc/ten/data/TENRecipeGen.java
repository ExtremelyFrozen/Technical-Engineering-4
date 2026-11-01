package com.modularmc.ten.data;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.recipe.RecipeSignature;
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

            // 粗矿块→粉改由 ORE_CHAIN 的 block_dust 段承担（标签驱动，邻居材料同样能走）

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

        // 矿石三段式（输入标签驱动，输出具体物品，多提供者写互斥变体）
        for (var row : ORE_CHAIN) {
            buildOreToRaw(futures, cache, recipeDir, row.material(), row.rawItems());
            buildRawToDust(futures, cache, recipeDir, row.material(), row.dustItems());
            buildRawBlockToDust(futures, cache, recipeDir, row.material(), row.dustItems());
            buildIngotToDust(futures, cache, recipeDir, row.material(), row.dustItems());
        }
        buildAlloys(futures, cache, recipeDir);

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
        RecipeSignature.claim(json, path.toString());
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

    // 矿石与粗料段改由 ORE_CHAIN 的标签驱动配方承担（pulverizer/chain/），
    // 这里只留锭→粉，避免同一输入标签出现两条可匹配配方。
    private static final List<PulvFmt> PULV = List.of(
            new PulvFmt("ingots", 1, 100, "_dust"));

    /**
     * 粉碎机矿石三段式：{@code c:ores/X → 粗料 → 粉}。
     * <p>
     * 输入用标签（别的模组有这些标签就自动生效），**输出必须是具体物品**——
     * 配方输出的标准语义是一个确定的 ItemStack，标签输出只有各家自定义 serializer
     * 自行解析才行（EIO 就是自己解析的），按标准处理会得到空产物。
     * 所以每段把「谁能提供这一样」都列成候选：铁/铜/金的粗料只有原版有，粉有本模组/Mekanism/
     * 沉浸工程/ATO 四家；锡/镍本模组与邻居都有；铅/锇/铀/铝/银来自邻居（Mekanism、沉浸工程、
     * ATO、EIO 进化）；铱/铂/锌来自 ATO，万用金/振金/不可得金来自 Allthemodium。
     * ECO（NEO ECO AE Extension）自带铝/钨两种矿石与铝/钨/铁粉，在默认优先级里排第二
     * （仅次于本模组），所以铝的粗料与粉、以及钨的整条链默认落到它；删掉它的候选即退回
     * 沉浸工程/ATO 的那几份。
     * <p>
     * 只有一个候选时按输出归属写 {@code neoforge:mod_loaded}（{@link #gateOf}，原版与本模组不写）；
     * 多个候选时每个候选各写一条互斥变体，由 {@code kenergyengineering:provider_priority} 按
     * {@code compat.providerPriority} 选出唯一赢家——邻居增减、改配置顺序都不必重新生成数据，
     * 而默认顺序（本模组优先）与只写一条时的行为一致。两段各自选人，粗料与粉可以来自不同模组，
     * 因为第二段吃 {@code c:raw_materials/X} 标签，接得住任何一家的粗料。
     * 解析不到具体物品的材料（无粗料形态的 bauxite/cinnabar 一类）不生成；
     * 装了新邻居后由兼容层转换器按同一优先级报出可补候选，再往本表加行。
     */
    private record ChainRow(String material, List<String> rawItems, List<String> dustItems) {}

    private static final List<ChainRow> ORE_CHAIN = List.of(
            new ChainRow("iron", List.of("minecraft:raw_iron"),
                    List.of("kenergyengineering:iron_dust", "neoecoae:iron_dust", "enderio:powdered_iron", "mekanism:dust_iron", "immersiveengineering:dust_iron", "alltheores:iron_dust")),
            new ChainRow("copper", List.of("minecraft:raw_copper"),
                    List.of("kenergyengineering:copper_dust", "enderio:powdered_copper", "mekanism:dust_copper", "immersiveengineering:dust_copper", "alltheores:copper_dust")),
            new ChainRow("gold", List.of("minecraft:raw_gold"),
                    List.of("kenergyengineering:gold_dust", "enderio:powdered_gold", "mekanism:dust_gold", "immersiveengineering:dust_gold", "alltheores:gold_dust")),
            new ChainRow("tin", List.of("kenergyengineering:raw_tin", "mekanism:raw_tin", "alltheores:raw_tin"),
                    List.of("kenergyengineering:tin_dust", "enderio:powdered_tin", "mekanism:dust_tin", "alltheores:tin_dust")),
            new ChainRow("nickel", List.of("kenergyengineering:raw_nickel", "immersiveengineering:raw_nickel", "alltheores:raw_nickel"),
                    List.of("kenergyengineering:nickel_dust", "immersiveengineering:dust_nickel", "alltheores:nickel_dust")),
            new ChainRow("lead", List.of("mekanism:raw_lead", "immersiveengineering:raw_lead", "alltheores:raw_lead"),
                    List.of("mekanism:dust_lead", "immersiveengineering:dust_lead", "alltheores:lead_dust")),
            new ChainRow("osmium", List.of("mekanism:raw_osmium", "alltheores:raw_osmium"),
                    List.of("mekanism:dust_osmium", "alltheores:osmium_dust")),
            new ChainRow("uranium", List.of("mekanism:raw_uranium", "immersiveengineering:raw_uranium", "alltheores:raw_uranium"),
                    List.of("mekanism:dust_uranium", "immersiveengineering:dust_uranium", "alltheores:uranium_dust")),
            new ChainRow("aluminum", List.of("neoecoae:raw_aluminum_ore", "immersiveengineering:raw_aluminum",
                            "alltheores:raw_aluminum"),
                    List.of("neoecoae:aluminum_dust", "immersiveengineering:dust_aluminum", "alltheores:aluminum_dust")),
            new ChainRow("tungsten", List.of("neoecoae:raw_tungsten_ore"), List.of("neoecoae:tungsten_dust")),
            new ChainRow("silver", List.of("enderio_evolution:raw_silver", "immersiveengineering:raw_silver", "alltheores:raw_silver"),
                    List.of("immersiveengineering:dust_silver", "alltheores:silver_dust")),
            new ChainRow("iridium", List.of("alltheores:raw_iridium"), List.of("alltheores:iridium_dust")),
            new ChainRow("platinum", List.of("alltheores:raw_platinum"), List.of("alltheores:platinum_dust")),
            new ChainRow("zinc", List.of("alltheores:raw_zinc"), List.of("alltheores:zinc_dust")),
            new ChainRow("allthemodium", List.of("allthemodium:raw_allthemodium"), List.of("allthemodium:allthemodium_dust")),
            new ChainRow("unobtainium", List.of("allthemodium:raw_unobtainium"), List.of("allthemodium:unobtainium_dust")),
            new ChainRow("vibranium", List.of("allthemodium:raw_vibranium"), List.of("allthemodium:vibranium_dust")));

    /**
     * 固定比率合金：感应炉的通用配方，也就是这些合金的**默认**配比，一律由邻居的**粉末**配方推导。
     * <ul>
     * <li>ATO：{@code alltheores/recipe/crafting/<alloy>/alloy_blending_from_dust.json} 与它的锭层面
     *     {@code alloy_smelting/<alloy>_ingot.json} 一致——青铜/黄铜 3+1→4、因瓦 2+1→3、电子/康铜 1+1→2；
     *     非常规的 steel/enderium/lumium/signalum 照同一份表。</li>
     * <li>ECO（NEO ECO AE Extension）：{@code neoecoae} 的 {@code <alloy>_dust.json} 无形状合成——
     *     铝合金 1 铁 + 1 铝 + 2 赛斯特石英、黑钨合金 1 钨 + 1 铝合金 + 2 流克斯；它的粉→锭是 1:1，
     *     所以这里直接落成 1 锭。</li>
     * </ul>
     * 产出数按邻居数据里的 {@code result.count} 写死，不套「原子守恒」：lumium 是 4+1+3→4，
     * 守恒假设会算成 8。
     * <p>
     * 输入写标签：{@code c:ingots/X} 在该材料有 {@code c:mats/X} 聚合标签时由 {@link #matInput}
     * 自动升级成「粉或锭皆可入」（与感应炉既有设计同一判据），萤石粉/红石/煤/赛斯特石英这类
     * 没有锭形态的写它们自己的标签。聚合标签由 {@code TENMaterialTagGen} 生成，成员是对
     * {@code #c:ingots/X} / {@code #c:dusts/X} 的可选引用，所以谁提供这种金属都能进配方；
     * 赛斯特石英/流克斯由 AE2 提供，没装 AE2 时标签为空、整条（含 JEI 显示）在加载期消失。
     * 输出仍必须是具体物品，因此按候选模组各写一条互斥变体，与 {@link #ORE_CHAIN} 共用
     * {@code provider_priority} 机制；耗时按 {@link #furnaceTime} 的锚点算，与原生感应炉配方同尺度。
     */
    private record AlloyIn(String tag, int count) {}

    private record AlloyRow(String alloy, List<AlloyIn> inputs, List<String> outputs, int yieldCount) {}

    private static final List<AlloyRow> ALLOY_CHAIN = List.of(
            new AlloyRow("bronze", List.of(new AlloyIn("c:ingots/copper", 3), new AlloyIn("c:ingots/tin", 1)),
                    List.of("alltheores:bronze_ingot", "mekanism:ingot_bronze"), 4),
            new AlloyRow("brass", List.of(new AlloyIn("c:ingots/copper", 3), new AlloyIn("c:ingots/zinc", 1)),
                    List.of("alltheores:brass_ingot"), 4),
            new AlloyRow("invar", List.of(new AlloyIn("c:ingots/iron", 2), new AlloyIn("c:ingots/nickel", 1)),
                    List.of("alltheores:invar_ingot"), 3),
            new AlloyRow("electrum", List.of(new AlloyIn("c:ingots/gold", 1), new AlloyIn("c:ingots/silver", 1)),
                    List.of("immersiveengineering:ingot_electrum", "alltheores:electrum_ingot"), 2),
            new AlloyRow("constantan", List.of(new AlloyIn("c:ingots/copper", 1), new AlloyIn("c:ingots/nickel", 1)),
                    List.of("immersiveengineering:ingot_constantan", "alltheores:constantan_ingot"), 2),
            new AlloyRow("steel", List.of(new AlloyIn("c:ingots/iron", 1), new AlloyIn("minecraft:coals", 1)),
                    List.of("alltheores:steel_ingot", "mekanism:ingot_steel", "immersiveengineering:ingot_steel"), 1),
            new AlloyRow("enderium", List.of(new AlloyIn("c:ingots/lead", 3), new AlloyIn("c:ingots/platinum", 1),
                            new AlloyIn("c:ender_pearls", 2)),
                    List.of("alltheores:enderium_ingot"), 4),
            new AlloyRow("lumium", List.of(new AlloyIn("c:dusts/glowstone", 4), new AlloyIn("c:ingots/silver", 1),
                            new AlloyIn("c:ingots/tin", 3)),
                    List.of("alltheores:lumium_ingot"), 4),
            new AlloyRow("signalum", List.of(new AlloyIn("c:ingots/copper", 3), new AlloyIn("c:ingots/silver", 1),
                            new AlloyIn("c:dusts/redstone", 4)),
                    List.of("alltheores:signalum_ingot"), 4),
            new AlloyRow("aluminum_alloy", List.of(new AlloyIn("c:ingots/iron", 1), new AlloyIn("c:ingots/aluminum", 1),
                            new AlloyIn("c:dusts/certus_quartz", 2)),
                    List.of("neoecoae:aluminum_alloy_ingot"), 1),
            new AlloyRow("black_tungsten_alloy", List.of(new AlloyIn("c:ingots/tungsten", 1),
                            new AlloyIn("c:ingots/aluminum_alloy", 1), new AlloyIn("c:dusts/fluix", 2)),
                    List.of("neoecoae:black_tungsten_alloy_ingot"), 1));

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
        j.add("outputs", outs);
        j.addProperty("time", r.time);
        return j;
    }

    /** 三段式第一段：{@code c:ores/X} → 具体粗料物品 1 + 0.33，附带 0.15 圆石副产。 */
    private void buildOreToRaw(List<CompletableFuture<?>> futures, CachedOutput cache, Path recipeDir, String mat,
            List<String> candidates) {
        buildChainStage(futures, cache, recipeDir, mat, "ore_raw", "c:ores/" + mat, candidates, 120, item -> {
            var outs = new JsonArray();
            outs.add(ingr("item", "static", item, 1, null));
            outs.add(ingr("item", "static", item, 1, 0.33));
            outs.add(ingr("item", "static", "minecraft:cobblestone", 1, 0.15));
            return outs;
        });
    }

    /** 三段式第二段：{@code c:raw_materials/X} → 具体粉尘物品 1 + 0.8（合计 1.8 粉/粗料）。 */
    private void buildRawToDust(List<CompletableFuture<?>> futures, CachedOutput cache, Path recipeDir, String mat,
            List<String> candidates) {
        buildChainStage(futures, cache, recipeDir, mat, "raw_dust", "c:raw_materials/" + mat, candidates, 120, item -> {
            var outs = new JsonArray();
            outs.add(ingr("item", "static", item, 1, null));
            outs.add(ingr("item", "static", item, 1, 0.8));
            return outs;
        });
    }

    /**
     * 通用段（原来自设、现按标签驱动）：{@code c:storage_blocks/raw_X} → 粉 9 + 9×40% 掷骰。
     * 耗时 200 对齐原生「1 进 8 出」那一档（{@code pulverizer/snow_ice_blue.json}），
     * 原来按 9 倍写 900 是把「物料份数」当成了「操作次数」，粉碎机原生一律是 100–200 一级。
     */
    private void buildRawBlockToDust(List<CompletableFuture<?>> futures, CachedOutput cache, Path recipeDir, String mat,
            List<String> candidates) {
        buildChainStage(futures, cache, recipeDir, mat, "block_dust", "c:storage_blocks/raw_" + mat, candidates, 200, item -> {
            var outs = new JsonArray();
            outs.add(ingr("item", "static", item, 9, null));
            outs.add(ingr("item", "static", item, 1, 0.4, 9));
            return outs;
        });
    }

    /**
     * 通用段：{@code c:ingots/X} → 粉 1。本模组注册的粉已由 {@code PULV} 表按材料覆盖，
     * 这里只补邻居材料（铅/锇/铀/铝/银/锌…），所以候选头一个是我们的粉时直接跳过。
     */
    private void buildIngotToDust(List<CompletableFuture<?>> futures, CachedOutput cache, Path recipeDir, String mat,
            List<String> candidates) {
        if (providerOf(candidates.get(0)).equals(TEN.MOD_ID)) return;
        buildChainStage(futures, cache, recipeDir, mat, "ingot_dust", "c:ingots/" + mat, candidates, 100, item -> {
            var outs = new JsonArray();
            outs.add(ingr("item", "static", item, 1, null));
            return outs;
        });
    }

    /**
     * 一个候选一条配方。单候选沿用输出归属的 {@code mod_loaded} 门控；多候选写
     * {@code provider_priority} 互斥变体组，加载期只留赢家（规则见 {@link #ORE_CHAIN} 说明）。
     */
    private void buildChainStage(List<CompletableFuture<?>> futures, CachedOutput cache, Path recipeDir,
            String mat, String stage, String inputTag, List<String> candidates, int time,
            java.util.function.Function<String, JsonArray> outputs) {
        boolean exclusive = candidates.size() > 1;
        var group = candidates.stream().map(TENRecipeGen::providerOf).toList();
        for (String item : candidates) {
            var j = new JsonObject();
            j.addProperty("type", TEN.MOD_ID + ":pulverizer");
            if (exclusive) addConditions(j, tagPresent(inputTag), priorityOf(providerOf(item), group));
            else addConditions(j, tagPresent(inputTag), gateOf(item));
            j.add("inputs", arr(ingr("item", "tag", inputTag, null, null)));
            j.add("outputs", outputs.apply(item));
            j.addProperty("time", time);
            String name = mat + "_" + stage + (exclusive ? "_" + providerOf(item) : "");
            futures.add(saveJson(cache, recipeDir.resolve("pulverizer/chain/" + name + ".json"), j));
        }
    }

    /** 输出物品归属的模组命名空间。 */
    private static String providerOf(String itemId) {
        int colon = itemId.indexOf(':');
        return colon < 0 ? "minecraft" : itemId.substring(0, colon);
    }

    /** 条件：本条是候选组里的一个变体，加载期由 {@code compat.providerPriority} 在组里选唯一赢家。 */
    private static JsonObject priorityOf(String mod, List<String> candidates) {
        var a = new JsonArray();
        candidates.forEach(a::add);
        return obj("type", TEN.MOD_ID + ":provider_priority", "mod", mod, "candidates", a);
    }

    /**
     * 固定比率合金：每个候选锭一条感应炉配方。输入与该合金的存在性都补 {@code not(tag_empty)} 门控，
     * 所以缺这种金属时整条（含 JEI 显示）一起消失，不会留下跑不起来的配方。
     */
    private void buildAlloys(List<CompletableFuture<?>> futures, CachedOutput cache, Path recipeDir) {
        for (var row : ALLOY_CHAIN) {
            var group = row.outputs().stream().map(TENRecipeGen::providerOf).toList();
            boolean exclusive = row.outputs().size() > 1;
            int units = row.inputs().stream().mapToInt(AlloyIn::count).sum();
            for (String out : row.outputs()) {
                var conds = new ArrayList<JsonObject>();
                var ins = new JsonArray();
                for (var in : row.inputs()) {
                    String tag = matInput(in.tag());
                    ins.add(ingr("item", "tag", tag, in.count(), null));
                    conds.add(tagPresent(tag));
                }
                conds.add(tagPresent("c:ingots/" + row.alloy()));
                conds.add(exclusive ? priorityOf(providerOf(out), group) : gateOf(out));
                var j = new JsonObject();
                j.addProperty("type", TEN.MOD_ID + ":induction_furnace");
                addConditions(j, conds.toArray(new JsonObject[0]));
                j.add("inputs", ins);
                var outs = new JsonArray();
                outs.add(ingr("item", "static", out, row.yieldCount(), null));
                j.add("outputs", outs);
                j.addProperty("time", furnaceTime(units));
                String name = "alloy/" + row.alloy() + (exclusive ? "_" + providerOf(out) : "");
                futures.add(saveJson(cache, recipeDir.resolve("induction_furnace/" + name + ".json"), j));
            }
        }
    }

    /**
     * 感应炉耗时尺规：**每个输入物料 +50 tick**，线性、不夹上限。
     * 原生手写配方与新生成的合金/兼容行都按这一条走，见 {@code recipe/induction_furnace/}。
     */
    private static int furnaceTime(int inputUnits) {
        return 50 * inputUnits;
    }

    /** 材料输入标签：该材料有 {@code c:mats/X} 聚合标签时粉与锭皆可入，否则原样返回。 */
    private static String matInput(String tag) {
        if (!tag.startsWith("c:ingots/")) return tag;
        String material = tag.substring("c:ingots/".length());
        return TENMaterialTagGen.hasMatTag(material) ? "c:mats/" + material : tag;
    }

    /** 输出物品归属的模组 → {@code mod_loaded} 门控；原版与本模组不需要。 */
    private static JsonObject gateOf(String itemId) {
        int colon = itemId.indexOf(':');
        String ns = colon < 0 ? "" : itemId.substring(0, colon);
        if (ns.isEmpty() || "minecraft".equals(ns) || TEN.MOD_ID.equals(ns)) return null;
        return obj("type", "neoforge:mod_loaded", "modid", ns);
    }

    /** 写入条件数组，自动丢掉 null（无门控）与重复项。 */
    private static void addConditions(JsonObject j, JsonObject... conds) {
        var a = new JsonArray();
        java.util.Set<String> seen = new java.util.LinkedHashSet<>();
        for (var c : conds) if (c != null && seen.add(c.toString())) a.add(c);
        if (a.size() > 0) j.add("neoforge:conditions", a);
    }

    /** 条件：该标签非空（材料缺席时整条配方在加载期被剔除）。 */
    private static JsonObject tagPresent(String tag) {
        return obj("type", "neoforge:not", "value", obj("type", "neoforge:tag_empty", "tag", tag));
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

    /** Raw block → 9 dust with 9×40% bonus dust（roll 掷骰配方）。 */
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
