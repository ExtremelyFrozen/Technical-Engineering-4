package com.modularmc.ten.common.data;

import com.modularmc.ten.TEN;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;

import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

import com.tterrag.registrate.util.entry.FluidEntry;

import java.util.LinkedHashMap;

import static com.modularmc.ten.common.registry.Registration.REGISTRATE;

public class TENFluids {

    public static final LinkedHashMap<String, String> ZH_NAMES = new LinkedHashMap<>();
    public static final LinkedHashMap<String, String> ZH_FLUID_KEYS = new LinkedHashMap<>();

    public static final FluidEntry<BaseFlowingFluid.Flowing> LIQUID_ROYAL_JELLY = fluid(
            "liquid_royal_jelly", "Liquid Royal Jelly", "Liquid Royal Jelly Bucket", "蜂王浆", "蜂王浆桶", 1200, 4, 300, 2000, 2, 100.0f, 4);
    public static final FluidEntry<BaseFlowingFluid.Flowing> LIQUID_SPICY_JELLY = fluid(
            "liquid_spicy_jelly", "Liquid Spicy Jelly", "Liquid Spicy Jelly Bucket", "香辣蜂王浆", "香辣蜂王浆桶", 1200, 4, 300, 2000, 2, 100.0f, 4);
    public static final FluidEntry<BaseFlowingFluid.Flowing> LIQUID_HONEY = fluid(
            "liquid_honey", "Liquid Honey", "Liquid Honey Bucket", "蜂蜜", "蜂蜜桶", 1400, 2, 350, 6000, 1, 100.0f, 2,
        SoundEvents.BOTTLE_FILL, SoundEvents.BOTTLE_EMPTY, Tags.Fluids.HONEY);
    public static final FluidEntry<BaseFlowingFluid.Flowing> LIQUID_XP = fluid(
            "liquid_xp", "Liquid XP", "Liquid XP Bucket", "液态经验", "液态经验桶", 100, 10, 100, 25000, 1, 1000.0f, 6,
        SoundEvents.BOTTLE_FILL, SoundEvents.BOTTLE_EMPTY, Tags.Fluids.EXPERIENCE);
    public static final FluidEntry<BaseFlowingFluid.Flowing> LIQUID_BIZARRERIE = fluid(
            "liquid_bizarrerie", "Liquid Bizarrerie", "Liquid Bizarrerie Bucket", "奇异物质", "奇异物质桶", 100000, 15, -500, 1000, 2, 5000.0f, 4);
    public static final FluidEntry<BaseFlowingFluid.Flowing> CRUDE_OIL = fluid(
        "crude_oil", "Crude Oil", "Crude Oil Bucket", "原油", "原桶", 100000, 0, 300, 1000, 2, 5000.0f, 4,
        TENTags.CRUDE_OIL);
    public static final FluidEntry<BaseFlowingFluid.Flowing> NAPHTHA = fluid(
        "naphtha", "Naphtha", "Naphtha Bucket", "石脑油", "石脑油桶", 100000, 0, 300, 1000, 2, 5000.0f, 4,
        TENTags.NAPHTHA);
    public static final FluidEntry<BaseFlowingFluid.Flowing> KEROSENE = fluid(
        "kerosene", "Kerosene", "Kerosene Bucket", "煤油", "煤油桶", 100000, 0, 300, 1000, 2, 5000.0f, 4,
        TENTags.KEROSENE);
    public static final FluidEntry<BaseFlowingFluid.Flowing> NUTRIENT_SOLUTION = fluid(
        "nutrient_solution", "Nutrient Solution", "Nutrient Solution Bucket", "营养液", "营养液桶", 100000, 0, 300, 1000, 2, 5000.0f, 4);
    public static final FluidEntry<BaseFlowingFluid.Flowing> HEATED_REDSTONE = fluid(
        "heated_redstone", "Heated Redstone", "Heated Redstone Bucket", "高热红石", "高热红石桶", 100000, 15, 1500, 1000, 2, 5000.0f, 4);
    public static final FluidEntry<BaseFlowingFluid.Flowing> SYRUP = fluid(
        "syrup", "Syrup", "Syrup Bucket", "糖浆", "糖浆桶", 100000, 0, 300, 1000, 2, 5000.0f, 4,
        TENTags.SYRUP);
    public static final FluidEntry<BaseFlowingFluid.Flowing> WASTE = fluid(
        "waste", "Waste", "Waste Bucket", "废液", "废液桶", 100000, 0, 300, 1000, 2, 5000.0f, 4);
    public static final FluidEntry<BaseFlowingFluid.Flowing> TORAN_CONCENTRATE = fluid(
        "toran_concentrate", "Toran Concentrate", "Toran Concentrate Bucket", "托兰原液", "托兰原液桶", 100000, 15, -100, 1000, 2, 5000.0f, 4);
    public static final FluidEntry<BaseFlowingFluid.Flowing> POWDER_SNOW = snowfluid("powder_snow", "Powder Snow", "细雪");

    @SafeVarargs
    private static FluidEntry<BaseFlowingFluid.Flowing> fluid(
        String name, String fluidLang, String bucketLang, String fluidCn, String bucketCn, int density, int light, int temperature, int viscosity,
        int levelDecrease, float explosionResistance, int slopeFindDistance, TagKey<Fluid>... tags) {
        return fluid(name, fluidLang, bucketLang, fluidCn, bucketCn, density, light, temperature, viscosity,
            levelDecrease, explosionResistance, slopeFindDistance, SoundEvents.BUCKET_FILL, SoundEvents.BUCKET_EMPTY, tags);
    }

    @SafeVarargs
    private static FluidEntry<BaseFlowingFluid.Flowing> fluid(
        String name, String fluidLang, String bucketLang, String fluidCn, String bucketCn, int density, int light, int temperature, int viscosity,
        int levelDecrease, float explosionResistance, int slopeFindDistance, SoundEvent fill, SoundEvent empty, TagKey<Fluid>... tags) {
        ZH_NAMES.put(name, fluidCn);
        ZH_FLUID_KEYS.put("fluid." + TEN.MOD_ID + "." + name, fluidCn);
        ResourceLocation still = TEN.id("block/" + name);
        ResourceLocation flowing = TEN.id("block/" + name + "_flowing");
        return REGISTRATE
            .fluid(name, still, flowing)
            .lang(fluidLang)
            .properties(p -> p
                .descriptionId("fluid." + TEN.MOD_ID + "." + name)
                .density(density)
                .lightLevel(light)
                .temperature(temperature)
                .viscosity(viscosity)
                .sound(SoundActions.BUCKET_FILL, fill)
                .sound(SoundActions.BUCKET_EMPTY, empty))
            .tag(tags)
            .fluidProperties(p -> p
                .levelDecreasePerBlock(levelDecrease)
                .explosionResistance(explosionResistance)
                .slopeFindDistance(slopeFindDistance)
                .tickRate(10))
            .source(BaseFlowingFluid.Source::new)
            .block()
            .lang(fluidLang)
            .build()
            .bucket(BucketItem::new)
            .lang(bucketLang)
            .model((ctx, prov) -> prov.generated(ctx::getEntry, prov.modLoc("block/" + name + "_bucket")))
            .build()
            .register();
    }

    private static FluidEntry<BaseFlowingFluid.Flowing> snowfluid(String name, String fluidLang, String fluidCn) {
        ZH_NAMES.put(name, fluidCn);
        ZH_FLUID_KEYS.put("fluid." + TEN.MOD_ID + "." + name, fluidCn);
        ResourceLocation still = ResourceLocation.withDefaultNamespace("block/powder_snow");
        return REGISTRATE
            .fluid(name, still, still)
            .lang(fluidLang)
            .tag(TENTags.POWDER_SNOW)
            .properties(p -> p
                .descriptionId("fluid." + TEN.MOD_ID + "." + name)
                .density(1000)
                .lightLevel(2)
                .temperature(270)
                .viscosity(6000)
                .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL_POWDER_SNOW)
                .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY_POWDER_SNOW))
            .tag(TENTags.POWDER_SNOW)
            .noBlock()
            .noBucket()
            .fluidProperties(p -> p.bucket(() -> Items.POWDER_SNOW_BUCKET))
            .register();
    }

    public static Fluid LIQUID_XP_FLOWING() {
        return LIQUID_XP.get();
    }

    public static Fluid LIQUID_BIZARRERIE_FLOWING() {
        return LIQUID_BIZARRERIE.get();
    }

    public static void init() {}
}
