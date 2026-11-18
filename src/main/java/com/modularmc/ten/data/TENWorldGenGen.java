package com.modularmc.ten.data;

import com.modularmc.ten.TEN;
import com.modularmc.ten.common.data.TENBlocks;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.Arrays;
import java.util.List;

/**
 * 锡与镍的主世界矿石生成，DataGen 侧唯一事实源。
 * <p>
 * 每条 feature 同时带 stone 与 deepslate 两个 target，深层变体按高度自动分层，不必再开 feature。
 * 档位取科技模组的起步档（略富，让粉碎链和采矿场很快有活干），调档只改下面的常量。
 */
public final class TENWorldGenGen {

    // ── 分布常量 ──────────────────────────────────────────────────
    /** 主矿脉：trapezoid 全柱分布，峰值在中层，靠 size 决定单条矿脉规模。 */
    private static final int TIN_MAIN_COUNT = 8;
    private static final int TIN_MAIN_SIZE = 12;
    private static final int NICKEL_MAIN_COUNT = 7;
    private static final int NICKEL_MAIN_SIZE = 10;
    /** 散布矿：scattered_ore 的 size 是「每处可放几块」的上限（均值约 size/2），不是矿脉形状，所以次数多、块数少。 */
    private static final int TIN_SCATTER_COUNT = 12;
    private static final int TIN_SCATTER_SIZE = 4;
    /** 镍比锡少两档：镍的工具门槛与深度都更高，同数量下观感会过密。 */
    private static final int NICKEL_SCATTER_COUNT = 10;
    private static final int NICKEL_SCATTER_SIZE = 3;

    /** 高度地板：aboveBottom(0) 即世界底，主世界为 -64。 */
    private static final VerticalAnchor WORLD_FLOOR = VerticalAnchor.aboveBottom(0);
    private static final VerticalAnchor TIN_CEIL = VerticalAnchor.absolute(72);
    private static final VerticalAnchor NICKEL_CEIL = VerticalAnchor.absolute(40);
    private static final VerticalAnchor NICKEL_SCATTER_CEIL = VerticalAnchor.absolute(8);

    // ── 注册键 ──────────────────────────────────────────────────────
    private static final ResourceKey<ConfiguredFeature<?, ?>> TIN_MAIN_CF = cf("ore_tin");
    private static final ResourceKey<ConfiguredFeature<?, ?>> TIN_SCATTER_CF = cf("ore_tin_scatter");
    private static final ResourceKey<ConfiguredFeature<?, ?>> NICKEL_MAIN_CF = cf("ore_nickel");
    private static final ResourceKey<ConfiguredFeature<?, ?>> NICKEL_SCATTER_CF = cf("ore_nickel_scatter");

    private static final ResourceKey<PlacedFeature> TIN_MAIN = pf("ore_tin");
    private static final ResourceKey<PlacedFeature> TIN_SCATTER = pf("ore_tin_scatter");
    private static final ResourceKey<PlacedFeature> NICKEL_MAIN = pf("ore_nickel");
    private static final ResourceKey<PlacedFeature> NICKEL_SCATTER = pf("ore_nickel_scatter");

    private static final ResourceKey<BiomeModifier> ADD_TIN_ORES = bm("add_tin_ores");
    private static final ResourceKey<BiomeModifier> ADD_NICKEL_ORES = bm("add_nickel_ores");

    /**
     * 交给 {@link net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider} 的补丁构建器。
     * feature → placement → modifier 只是阅读顺序，交叉引用由 RegistrySetBuilder 惰性解析，不依赖登记先后。
     */
    public static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
            .add(Registries.CONFIGURED_FEATURE, TENWorldGenGen::bootstrapConfigured)
            .add(Registries.PLACED_FEATURE, TENWorldGenGen::bootstrapPlaced)
            .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, TENWorldGenGen::bootstrapModifiers);

    private TENWorldGenGen() {}

    // ── configured feature ─────────────────────────────────────────
    private static void bootstrapConfigured(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {
        ctx.register(TIN_MAIN_CF, ore(Feature.ORE, TENBlocks.TIN_ORE.get(), TENBlocks.DEEP_TIN_ORE.get(), TIN_MAIN_SIZE));
        ctx.register(TIN_SCATTER_CF, ore(Feature.SCATTERED_ORE, TENBlocks.TIN_ORE.get(), TENBlocks.DEEP_TIN_ORE.get(), TIN_SCATTER_SIZE));
        ctx.register(NICKEL_MAIN_CF, ore(Feature.ORE, TENBlocks.NICKEL_ORE.get(), TENBlocks.DEEP_NICKEL_ORE.get(), NICKEL_MAIN_SIZE));
        ctx.register(NICKEL_SCATTER_CF, ore(Feature.SCATTERED_ORE, TENBlocks.NICKEL_ORE.get(), TENBlocks.DEEP_NICKEL_ORE.get(), NICKEL_SCATTER_SIZE));
    }

    private static ConfiguredFeature<?, ?> ore(Feature<OreConfiguration> feature, Block surface, Block deep, int size) {
        return new ConfiguredFeature<>(feature, new OreConfiguration(List.of(
                OreConfiguration.target(new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES), surface.defaultBlockState()),
                OreConfiguration.target(new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES), deep.defaultBlockState())), size));
    }

    // ── placed feature ────────────────────────────────────────────
    private static void bootstrapPlaced(BootstrapContext<PlacedFeature> ctx) {
        HolderGetter<ConfiguredFeature<?, ?>> configured = ctx.lookup(Registries.CONFIGURED_FEATURE);
        // BiomeFilter 必须自带：PlacementUtils.register 不会补，缺了它群系过滤不生效
        PlacementUtils.register(ctx, TIN_MAIN, configured.getOrThrow(TIN_MAIN_CF),
                CountPlacement.of(TIN_MAIN_COUNT), InSquarePlacement.spread(),
                HeightRangePlacement.triangle(WORLD_FLOOR, TIN_CEIL), BiomeFilter.biome());
        PlacementUtils.register(ctx, TIN_SCATTER, configured.getOrThrow(TIN_SCATTER_CF),
                CountPlacement.of(TIN_SCATTER_COUNT), InSquarePlacement.spread(),
                HeightRangePlacement.uniform(WORLD_FLOOR, TIN_CEIL), BiomeFilter.biome());
        PlacementUtils.register(ctx, NICKEL_MAIN, configured.getOrThrow(NICKEL_MAIN_CF),
                CountPlacement.of(NICKEL_MAIN_COUNT), InSquarePlacement.spread(),
                HeightRangePlacement.triangle(WORLD_FLOOR, NICKEL_CEIL), BiomeFilter.biome());
        PlacementUtils.register(ctx, NICKEL_SCATTER, configured.getOrThrow(NICKEL_SCATTER_CF),
                CountPlacement.of(NICKEL_SCATTER_COUNT), InSquarePlacement.spread(),
                HeightRangePlacement.uniform(WORLD_FLOOR, NICKEL_SCATTER_CEIL), BiomeFilter.biome());
    }

    // ── biome modifier ────────────────────────────────────────────
    private static void bootstrapModifiers(BootstrapContext<BiomeModifier> ctx) {
        HolderGetter<PlacedFeature> placed = ctx.lookup(Registries.PLACED_FEATURE);
        HolderSet<Biome> overworld = ctx.lookup(Registries.BIOME).getOrThrow(BiomeTags.IS_OVERWORLD);

        ctx.register(ADD_TIN_ORES, add(overworld, placed, TIN_MAIN, TIN_SCATTER));
        ctx.register(ADD_NICKEL_ORES, add(overworld, placed, NICKEL_MAIN, NICKEL_SCATTER));
    }

    @SafeVarargs
    private static BiomeModifiers.AddFeaturesBiomeModifier add(HolderSet<Biome> biomes, HolderGetter<PlacedFeature> placed,
                                                               ResourceKey<PlacedFeature>... keys) {
        List<Holder<PlacedFeature>> features = Arrays.stream(keys).<Holder<PlacedFeature>>map(placed::getOrThrow).toList();
        return new BiomeModifiers.AddFeaturesBiomeModifier(biomes, HolderSet.direct(features), GenerationStep.Decoration.UNDERGROUND_ORES);
    }

    private static ResourceKey<ConfiguredFeature<?, ?>> cf(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, TEN.id(name));
    }

    private static ResourceKey<PlacedFeature> pf(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, TEN.id(name));
    }

    private static ResourceKey<BiomeModifier> bm(String name) {
        return ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, TEN.id(name));
    }
}
