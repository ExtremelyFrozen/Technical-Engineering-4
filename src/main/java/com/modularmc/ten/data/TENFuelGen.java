package com.modularmc.ten.data;

import com.modularmc.ten.TEN;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 引擎燃料数据生成：把「哪个物品 / 标签在哪台引擎上烧多久」写成
 * {@code data/kenergyengineering/recipe/engine_fuel/*.json}。
 *
 * <p>
 * 生成后这些 JSON 就是运行期的唯一事实源（{@code EngineFuels} 直接读配方），
 * 代码里不再保留任何物品 / 标签表；数据包或 KubeJS 覆盖同名文件即可改数值。
 *
 * <p>
 * 每条配方只声明<b>持续时间</b>与<b>FE/t 修正倍率</b>：产出率由引擎额定功率乘修正倍率算出。
 * 这里的耗时按旧口径 {@code ceil(能量密度 × 原版燃烧 tick ÷ 引擎额定)} 反向排出，
 * 因此迁移后玩家可见的能量总量与持续时间逐位不变。
 */
public class TENFuelGen implements DataProvider {

    // 引擎额定功率（与各 BlockEntity 的 BASE_GENERATION_RATE 一致）
    private static final int EXTRACTOR_RATE = 30;
    private static final int METAL_RATE = 80;
    private static final int BIOMASS_RATE = 80;

    // 能量密度（FE / 原版燃烧 tick），只用于把旧数值折算成持续时间
    private static final int EXTRACTOR_DENSITY = 20;
    private static final int METAL_DENSITY = 20;
    private static final int BIOMASS_DENSITY = 60;

    /** 一条燃料定义：kind = item / tag，burnTicks = 折算用的原版燃烧时间。 */
    private record Fuel(String engine, String kind, String id, int burnTicks) {}

    private final PackOutput output;

    public TENFuelGen(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        Path dir = output.getOutputFolder().resolve("data/" + TEN.MOD_ID + "/recipe/engine_fuel");
        List<CompletableFuture<?>> futures = new ArrayList<>();

        for (Fuel fuel : fuels()) {
            int rate = rateOf(fuel.engine());
            int density = densityOf(fuel.engine());
            int duration = durationOf(density, fuel.burnTicks(), rate);
            futures.add(DataProvider.saveStable(cache, json(fuel, duration), path(dir, fuel)));
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private static Path path(Path dir, Fuel fuel) {
        String engine = fuel.engine();
        String name = fuel.kind() + "_" + fuel.id().substring(fuel.id().indexOf(':') + 1);
        return dir.resolve(engine + "/" + name + ".json");
    }

    private static JsonObject json(Fuel fuel, int duration) {
        JsonObject ingredient = new JsonObject();
        ingredient.addProperty(fuel.kind(), fuel.id());

        JsonArray engines = new JsonArray();
        engines.add(fuel.engine());

        JsonObject root = new JsonObject();
        root.addProperty("type", TEN.MOD_ID + ":engine_fuel");
        root.add("ingredient", ingredient);
        root.add("engines", engines);
        root.addProperty("duration", duration);
        root.addProperty("rate_multiplier", 1.0);
        return root;
    }

    private static int rateOf(String engine) {
        return switch (engine) {
            case "extractor" -> EXTRACTOR_RATE;
            case "biomass" -> BIOMASS_RATE;
            default -> METAL_RATE;
        };
    }

    private static int densityOf(String engine) {
        return switch (engine) {
            case "extractor" -> EXTRACTOR_DENSITY;
            case "biomass" -> BIOMASS_DENSITY;
            default -> METAL_DENSITY;
        };
    }

    /** 持续时间 = ceil(能量密度 × 燃烧 tick ÷ 引擎额定)，与旧版 FuelProfile 的换算逐位一致。 */
    private static int durationOf(int density, int burnTicks, int rate) {
        int budget = density * burnTicks;
        return Math.max(1, (budget + rate - 1) / rate);
    }

    // ══════════════════════════════════════════════════════════════════
    // 默认燃料表（迁移自旧 MatchFuel 的硬编码表，数值不变）
    // ══════════════════════════════════════════════════════════════════

    private static List<Fuel> fuels() {
        List<Fuel> list = new ArrayList<>();

        // ── 萃取引擎：原版可燃物 ──
        item(list, "extractor", "minecraft:lava_bucket", 20000);
        item(list, "extractor", "minecraft:coal_block", 16000);
        item(list, "extractor", "minecraft:dried_kelp_block", 4000);
        item(list, "extractor", "minecraft:blaze_rod", 2400);
        item(list, "extractor", "minecraft:coal", 1600);
        item(list, "extractor", "minecraft:charcoal", 1600);
        item(list, "extractor", "minecraft:bamboo_block", 300);
        item(list, "extractor", "minecraft:bamboo_mosaic", 300);
        item(list, "extractor", "minecraft:bamboo_planks", 300);
        item(list, "extractor", "minecraft:bamboo_slab", 150);
        item(list, "extractor", "minecraft:bamboo_stairs", 300);
        item(list, "extractor", "minecraft:stick", 100);
        item(list, "extractor", "minecraft:bowl", 100);
        item(list, "extractor", "minecraft:ladder", 300);
        item(list, "extractor", "minecraft:bookshelf", 300);
        item(list, "extractor", "minecraft:chest", 300);
        item(list, "extractor", "minecraft:trapped_chest", 300);
        item(list, "extractor", "minecraft:crafting_table", 300);
        item(list, "extractor", "minecraft:jukebox", 300);
        item(list, "extractor", "minecraft:note_block", 300);
        item(list, "extractor", "minecraft:fishing_rod", 300);
        item(list, "extractor", "minecraft:bow", 300);
        item(list, "extractor", "minecraft:crossbow", 300);
        item(list, "extractor", "minecraft:wooden_shovel", 200);
        item(list, "extractor", "minecraft:wooden_pickaxe", 200);
        item(list, "extractor", "minecraft:wooden_axe", 200);
        item(list, "extractor", "minecraft:wooden_hoe", 200);
        item(list, "extractor", "minecraft:wooden_sword", 200);
        item(list, "extractor", "minecraft:flint_and_steel", 100);
        item(list, "extractor", "minecraft:scaffolding", 400);
        item(list, "extractor", "minecraft:daylight_detector", 300);
        item(list, "extractor", "minecraft:composter", 300);
        item(list, "extractor", "minecraft:barrel", 300);
        item(list, "extractor", "minecraft:cartography_table", 300);
        item(list, "extractor", "minecraft:fletching_table", 300);
        item(list, "extractor", "minecraft:smithing_table", 300);
        item(list, "extractor", "minecraft:loom", 300);
        item(list, "extractor", "minecraft:beehive", 300);
        item(list, "extractor", "minecraft:bee_nest", 300);
        item(list, "extractor", "minecraft:azalea", 100);
        item(list, "extractor", "minecraft:flowering_azalea", 100);
        tag(list, "extractor", "minecraft:logs", 300);
        tag(list, "extractor", "minecraft:planks", 300);
        tag(list, "extractor", "minecraft:wooden_slabs", 150);
        tag(list, "extractor", "minecraft:wooden_stairs", 300);
        tag(list, "extractor", "minecraft:wooden_trapdoors", 300);
        tag(list, "extractor", "minecraft:wooden_doors", 200);
        tag(list, "extractor", "minecraft:wooden_pressure_plates", 300);
        tag(list, "extractor", "minecraft:wooden_buttons", 100);
        tag(list, "extractor", "minecraft:wool", 100);
        tag(list, "extractor", "minecraft:banners", 300);
        tag(list, "extractor", "minecraft:boats", 600);

        // ── 金属引擎：按锭的稀有度档位 ──
        tag(list, "metalizer", "kenergyengineering:common_ingots", 1000);
        tag(list, "metalizer", "kenergyengineering:uncommon_ingots", 1500);
        tag(list, "metalizer", "kenergyengineering:valuable_ingots", 6400);

        // ── 生物质引擎：按植物类别 ──
        tag(list, "biomass", "minecraft:leaves", 20);
        tag(list, "biomass", "minecraft:logs", 120);
        tag(list, "biomass", "minecraft:flowers", 10);
        tag(list, "biomass", "minecraft:saplings", 15);

        return list;
    }

    private static void item(List<Fuel> list, String engine, String id, int burnTicks) {
        list.add(new Fuel(engine, "item", id, burnTicks));
    }

    private static void tag(List<Fuel> list, String engine, String id, int burnTicks) {
        list.add(new Fuel(engine, "tag", id, burnTicks));
    }

    @Override
    public String getName() {
        return "TEN Engine Fuel Recipes";
    }
}
