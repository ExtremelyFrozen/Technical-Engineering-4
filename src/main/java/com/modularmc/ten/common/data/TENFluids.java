package com.modularmc.ten.common.data;

import com.modularmc.ten.TEN;
import com.modularmc.ten.common.item.TENBucketItem;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.LinkedHashMap;

import static com.modularmc.ten.common.registry.Registration.BLOCKS;
import static com.modularmc.ten.common.registry.Registration.FLUIDS;
import static com.modularmc.ten.common.registry.Registration.FLUID_TYPES;
import static com.modularmc.ten.common.registry.Registration.ITEMS;

public class TENFluids {

    public static final LinkedHashMap<String, String> ZH_NAMES = new LinkedHashMap<>();
    public static final LinkedHashMap<String, String> ZH_FLUID_KEYS = new LinkedHashMap<>();
    public static final LinkedHashMap<String, String> EN_NAMES = new LinkedHashMap<>();
    public static final LinkedHashMap<String, String> EN_FLUID_KEYS = new LinkedHashMap<>();

    // ══════════════════════════════════════════════════════════════════
    // Per-fluid FluidType holders
    // ══════════════════════════════════════════════════════════════════
    public static final DeferredHolder<FluidType, FluidType> LIQUID_ROYAL_JELLY_FLUID_TYPE = FLUID_TYPES.register("liquid_royal_jelly", () -> new FluidType(
            FluidType.Properties.create().descriptionId("fluid." + TEN.MOD_ID + ".liquid_royal_jelly")));
    public static final DeferredHolder<FluidType, FluidType> LIQUID_SPICY_JELLY_FLUID_TYPE = FLUID_TYPES.register("liquid_spicy_jelly", () -> new FluidType(
            FluidType.Properties.create().descriptionId("fluid." + TEN.MOD_ID + ".liquid_spicy_jelly")));
    public static final DeferredHolder<FluidType, FluidType> LIQUID_HONEY_FLUID_TYPE = FLUID_TYPES.register("liquid_honey", () -> new FluidType(
            FluidType.Properties.create().descriptionId("fluid." + TEN.MOD_ID + ".liquid_honey")));
    public static final DeferredHolder<FluidType, FluidType> LIQUID_XP_FLUID_TYPE = FLUID_TYPES.register("liquid_xp", () -> new FluidType(
            FluidType.Properties.create().descriptionId("fluid." + TEN.MOD_ID + ".liquid_xp")));
    public static final DeferredHolder<FluidType, FluidType> LIQUID_BIZARRERIE_FLUID_TYPE = FLUID_TYPES.register("liquid_bizarrerie", () -> new FluidType(
            FluidType.Properties.create().descriptionId("fluid." + TEN.MOD_ID + ".liquid_bizarrerie")));

    // ══════════════════════════════════════════════════════════════════
    // Liquid Royal Jelly
    // ══════════════════════════════════════════════════════════════════
    private static BaseFlowingFluid.Properties royalJellyProps;

    private static BaseFlowingFluid.Properties getRoyalJellyProps() {
        if (royalJellyProps == null) {
            royalJellyProps = new BaseFlowingFluid.Properties(LIQUID_ROYAL_JELLY_FLUID_TYPE, LIQUID_ROYAL_JELLY_SOURCE, LIQUID_ROYAL_JELLY_FLOWING)
                    .block(LIQUID_ROYAL_JELLY_BLOCK)
                    .bucket(LIQUID_ROYAL_JELLY_BUCKET);
        }
        return royalJellyProps;
    }

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> LIQUID_ROYAL_JELLY_SOURCE = FLUIDS.register("liquid_royal_jelly", () -> new BaseFlowingFluid.Source(getRoyalJellyProps()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> LIQUID_ROYAL_JELLY_FLOWING = FLUIDS.register("liquid_royal_jelly_flowing", () -> new BaseFlowingFluid.Flowing(getRoyalJellyProps()));
    public static final DeferredHolder<Block, LiquidBlock> LIQUID_ROYAL_JELLY_BLOCK = BLOCKS.register("liquid_royal_jelly", () -> new LiquidBlock(LIQUID_ROYAL_JELLY_SOURCE.get(),
            liquidBlockProps("liquid_royal_jelly", 100.0f, 4)));
    public static final DeferredHolder<Item, BucketItem> LIQUID_ROYAL_JELLY_BUCKET = ITEMS.register("liquid_royal_jelly_bucket", () -> new TENBucketItem(LIQUID_ROYAL_JELLY_SOURCE.get(),
            bucketItemProps("liquid_royal_jelly_bucket")));
    static {
        ZH_NAMES.put("liquid_royal_jelly", "蜂王浆");
        ZH_FLUID_KEYS.put("fluid." + TEN.MOD_ID + ".liquid_royal_jelly", "蜂王浆");
        EN_NAMES.put("liquid_royal_jelly", "Liquid Royal Jelly");
        EN_FLUID_KEYS.put("fluid." + TEN.MOD_ID + ".liquid_royal_jelly", "Liquid Royal Jelly");
    }

    // ══════════════════════════════════════════════════════════════════
    // Liquid Spicy Jelly
    // ══════════════════════════════════════════════════════════════════
    private static BaseFlowingFluid.Properties spicyJellyProps;

    private static BaseFlowingFluid.Properties getSpicyJellyProps() {
        if (spicyJellyProps == null) {
            spicyJellyProps = new BaseFlowingFluid.Properties(LIQUID_SPICY_JELLY_FLUID_TYPE, LIQUID_SPICY_JELLY_SOURCE, LIQUID_SPICY_JELLY_FLOWING)
                    .block(LIQUID_SPICY_JELLY_BLOCK)
                    .bucket(LIQUID_SPICY_JELLY_BUCKET);
        }
        return spicyJellyProps;
    }

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> LIQUID_SPICY_JELLY_SOURCE = FLUIDS.register("liquid_spicy_jelly", () -> new BaseFlowingFluid.Source(getSpicyJellyProps()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> LIQUID_SPICY_JELLY_FLOWING = FLUIDS.register("liquid_spicy_jelly_flowing", () -> new BaseFlowingFluid.Flowing(getSpicyJellyProps()));
    public static final DeferredHolder<Block, LiquidBlock> LIQUID_SPICY_JELLY_BLOCK = BLOCKS.register("liquid_spicy_jelly", () -> new LiquidBlock(LIQUID_SPICY_JELLY_SOURCE.get(),
            liquidBlockProps("liquid_spicy_jelly", 100.0f, 4)));
    public static final DeferredHolder<Item, BucketItem> LIQUID_SPICY_JELLY_BUCKET = ITEMS.register("liquid_spicy_jelly_bucket", () -> new TENBucketItem(LIQUID_SPICY_JELLY_SOURCE.get(),
            bucketItemProps("liquid_spicy_jelly_bucket")));
    static {
        ZH_NAMES.put("liquid_spicy_jelly", "香辣蜂王浆");
        ZH_FLUID_KEYS.put("fluid." + TEN.MOD_ID + ".liquid_spicy_jelly", "香辣蜂王浆");
        EN_NAMES.put("liquid_spicy_jelly", "Liquid Spicy Jelly");
        EN_FLUID_KEYS.put("fluid." + TEN.MOD_ID + ".liquid_spicy_jelly", "Liquid Spicy Jelly");
    }

    // ══════════════════════════════════════════════════════════════════
    // Liquid Honey
    // ══════════════════════════════════════════════════════════════════
    private static BaseFlowingFluid.Properties honeyProps;

    private static BaseFlowingFluid.Properties getHoneyProps() {
        if (honeyProps == null) {
            honeyProps = new BaseFlowingFluid.Properties(LIQUID_HONEY_FLUID_TYPE, LIQUID_HONEY_SOURCE, LIQUID_HONEY_FLOWING)
                    .block(LIQUID_HONEY_BLOCK)
                    .bucket(LIQUID_HONEY_BUCKET);
        }
        return honeyProps;
    }

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> LIQUID_HONEY_SOURCE = FLUIDS.register("liquid_honey", () -> new BaseFlowingFluid.Source(getHoneyProps()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> LIQUID_HONEY_FLOWING = FLUIDS.register("liquid_honey_flowing", () -> new BaseFlowingFluid.Flowing(getHoneyProps()));
    public static final DeferredHolder<Block, LiquidBlock> LIQUID_HONEY_BLOCK = BLOCKS.register("liquid_honey", () -> new LiquidBlock(LIQUID_HONEY_SOURCE.get(),
            liquidBlockProps("liquid_honey", 100.0f, 2)));
    public static final DeferredHolder<Item, BucketItem> LIQUID_HONEY_BUCKET = ITEMS.register("liquid_honey_bucket", () -> new TENBucketItem(LIQUID_HONEY_SOURCE.get(),
            bucketItemProps("liquid_honey_bucket")));
    static {
        ZH_NAMES.put("liquid_honey", "蜂蜜");
        ZH_FLUID_KEYS.put("fluid." + TEN.MOD_ID + ".liquid_honey", "蜂蜜");
        EN_NAMES.put("liquid_honey", "Liquid Honey");
        EN_FLUID_KEYS.put("fluid." + TEN.MOD_ID + ".liquid_honey", "Liquid Honey");
    }

    // ══════════════════════════════════════════════════════════════════
    // Liquid XP
    // ══════════════════════════════════════════════════════════════════
    private static BaseFlowingFluid.Properties xpProps;

    private static BaseFlowingFluid.Properties getXpProps() {
        if (xpProps == null) {
            xpProps = new BaseFlowingFluid.Properties(LIQUID_XP_FLUID_TYPE, LIQUID_XP_SOURCE, LIQUID_XP_FLOWING)
                    .block(LIQUID_XP_BLOCK)
                    .bucket(LIQUID_XP_BUCKET);
        }
        return xpProps;
    }

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> LIQUID_XP_SOURCE = FLUIDS.register("liquid_xp", () -> new BaseFlowingFluid.Source(getXpProps()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> LIQUID_XP_FLOWING = FLUIDS.register("liquid_xp_flowing", () -> new BaseFlowingFluid.Flowing(getXpProps()));
    public static final DeferredHolder<Block, LiquidBlock> LIQUID_XP_BLOCK = BLOCKS.register("liquid_xp", () -> new LiquidBlock(LIQUID_XP_SOURCE.get(),
            liquidBlockProps("liquid_xp", 1000.0f, 6)));
    public static final DeferredHolder<Item, BucketItem> LIQUID_XP_BUCKET = ITEMS.register("liquid_xp_bucket", () -> new TENBucketItem(LIQUID_XP_SOURCE.get(),
            bucketItemProps("liquid_xp_bucket")));
    static {
        ZH_NAMES.put("liquid_xp", "液态经验");
        ZH_FLUID_KEYS.put("fluid." + TEN.MOD_ID + ".liquid_xp", "液态经验");
        EN_NAMES.put("liquid_xp", "Liquid XP");
        EN_FLUID_KEYS.put("fluid." + TEN.MOD_ID + ".liquid_xp", "Liquid XP");
    }

    // ══════════════════════════════════════════════════════════════════
    // Liquid Bizarrerie
    // ══════════════════════════════════════════════════════════════════
    private static BaseFlowingFluid.Properties bizarrerieProps;

    private static BaseFlowingFluid.Properties getBizarrerieProps() {
        if (bizarrerieProps == null) {
            bizarrerieProps = new BaseFlowingFluid.Properties(LIQUID_BIZARRERIE_FLUID_TYPE, LIQUID_BIZARRERIE_SOURCE, LIQUID_BIZARRERIE_FLOWING)
                    .block(LIQUID_BIZARRERIE_BLOCK)
                    .bucket(LIQUID_BIZARRERIE_BUCKET);
        }
        return bizarrerieProps;
    }

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> LIQUID_BIZARRERIE_SOURCE = FLUIDS.register("liquid_bizarrerie", () -> new BaseFlowingFluid.Source(getBizarrerieProps()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> LIQUID_BIZARRERIE_FLOWING = FLUIDS.register("liquid_bizarrerie_flowing", () -> new BaseFlowingFluid.Flowing(getBizarrerieProps()));
    public static final DeferredHolder<Block, LiquidBlock> LIQUID_BIZARRERIE_BLOCK = BLOCKS.register("liquid_bizarrerie", () -> new LiquidBlock(LIQUID_BIZARRERIE_SOURCE.get(),
            liquidBlockProps("liquid_bizarrerie", 5000.0f, 4)));
    public static final DeferredHolder<Item, BucketItem> LIQUID_BIZARRERIE_BUCKET = ITEMS.register("liquid_bizarrerie_bucket", () -> new TENBucketItem(LIQUID_BIZARRERIE_SOURCE.get(),
            bucketItemProps("liquid_bizarrerie_bucket")));
    static {
        ZH_NAMES.put("liquid_bizarrerie", "奇异物质");
        ZH_FLUID_KEYS.put("fluid." + TEN.MOD_ID + ".liquid_bizarrerie", "奇异物质");
        EN_NAMES.put("liquid_bizarrerie", "Liquid Bizarrerie");
        EN_FLUID_KEYS.put("fluid." + TEN.MOD_ID + ".liquid_bizarrerie", "Liquid Bizarrerie");
    }

    // ══════════════════════════════════════════════════════════════════
    // Compatibility helpers
    // ══════════════════════════════════════════════════════════════════

    public static Fluid LIQUID_XP_FLOWING_FLUID() {
        return LIQUID_XP_FLOWING.get();
    }

    public static Fluid LIQUID_BIZARRERIE_FLOWING_FLUID() {
        return LIQUID_BIZARRERIE_FLOWING.get();
    }

    // ══════════════════════════════════════════════════════════════════
    // Shared properties builders (NeoForge 26.1.2: must call setId)
    // ══════════════════════════════════════════════════════════════════

    private static BlockBehaviour.Properties liquidBlockProps(String name, float explosionResistance, int lightLevel) {
        var key = ResourceKey.create(Registries.BLOCK,
                Identifier.fromNamespaceAndPath(TEN.MOD_ID, name));
        return BlockBehaviour.Properties.of()
                .setId(key)
                .replaceable()
                .noCollision()
                .liquid()
                .strength(explosionResistance)
                .lightLevel(s -> lightLevel)
                .pushReaction(PushReaction.DESTROY)
                .noLootTable();
    }

    private static Item.Properties bucketItemProps(String name) {
        var key = ResourceKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath(TEN.MOD_ID, name));
        return new Item.Properties()
                .setId(key)
                .stacksTo(1);
    }

    public static void init() {}
}
