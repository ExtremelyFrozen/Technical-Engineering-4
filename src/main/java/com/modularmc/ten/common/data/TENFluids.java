package com.modularmc.ten.common.data;

import com.modularmc.ten.TEN;
import com.modularmc.ten.common.item.TENBucketItem;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.loaders.DynamicFluidContainerModelBuilder;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

import com.tterrag.registrate.util.entry.FluidEntry;

import static com.modularmc.ten.common.registry.Registration.REGISTRATE;

public class TENFluids {

    public static final java.util.LinkedHashMap<String, String> ZH_NAMES = new java.util.LinkedHashMap<>();
    public static final java.util.LinkedHashMap<String, String> ZH_FLUID_KEYS = new java.util.LinkedHashMap<>();

    public static final FluidEntry<BaseFlowingFluid.Flowing> LIQUID_ROYAL_JELLY = fluid(
            "liquid_royal_jelly", "Liquid Royal Jelly", "Liquid Royal Jelly Bucket", "蜂王浆", "蜂王浆桶", 1200, 4, 300, 2000, 2, 100.0f, 4);
    public static final FluidEntry<BaseFlowingFluid.Flowing> LIQUID_SPICY_JELLY = fluid(
            "liquid_spicy_jelly", "Liquid Spicy Jelly", "Liquid Spicy Jelly Bucket", "香辣蜂王浆", "香辣蜂王浆桶", 1200, 4, 300, 2000, 2, 100.0f, 4);
    public static final FluidEntry<BaseFlowingFluid.Flowing> LIQUID_HONEY = fluid(
            "liquid_honey", "Liquid Honey", "Liquid Honey Bucket", "蜂蜜", "蜂蜜桶", 1400, 2, 350, 6000, 1, 100.0f, 2);
    public static final FluidEntry<BaseFlowingFluid.Flowing> LIQUID_XP = fluid(
            "liquid_xp", "Liquid XP", "Liquid XP Bucket", "液态经验", "液态经验桶", 100, 10, 100, 25000, 1, 1000.0f, 6);
    public static final FluidEntry<BaseFlowingFluid.Flowing> LIQUID_BIZARRERIE = fluid(
            "liquid_bizarrerie", "Liquid Bizarrerie", "Liquid Bizarrerie Bucket", "奇异物质", "奇异物质桶", 100000, 15, -500, 1000, 2, 5000.0f, 4);

    private static FluidEntry<BaseFlowingFluid.Flowing> fluid(
                                                              String name, String fluidLang, String bucketLang, String fluidCn, String bucketCn, int density, int light, int temperature, int viscosity,
                                                              int levelDecrease, float explosionResistance, int slopeFindDistance) {
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
                        .sound(SoundActions.BUCKET_FILL, SoundEvents.BOTTLE_FILL)
                        .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BOTTLE_EMPTY))
                .fluidProperties(p -> p
                        .levelDecreasePerBlock(levelDecrease)
                        .explosionResistance(explosionResistance)
                        .slopeFindDistance(slopeFindDistance)
                        .tickRate(10))
                .source(BaseFlowingFluid.Source::new)
                .block()
                .lang(fluidLang)
                .build()
                .bucket(TENBucketItem::new)
                .lang(bucketLang)
                .model((ctx, prov) -> prov.getBuilder(prov.name(ctx::getEntry))
                        .parent(new ModelFile.UncheckedModelFile("neoforge:item/bucket_drip"))
                        .customLoader(DynamicFluidContainerModelBuilder::begin)
                        .fluid(((BucketItem) ctx.getEntry()).content)
                        .applyFluidLuminosity(light > 0))
                .build()
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
