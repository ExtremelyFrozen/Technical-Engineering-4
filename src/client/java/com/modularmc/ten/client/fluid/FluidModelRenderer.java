package com.modularmc.ten.client.fluid;

import com.modularmc.ten.TEN;
import com.modularmc.ten.common.data.TENFluids;

import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.client.color.block.BlockTintSource;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;

/**
 * Registers client-side FluidModel instances for all TEN fluids.
 * Maps each source and flowing fluid to its texture at block/fluid/<name> and block/fluid/<name>_flowing.
 */
@EventBusSubscriber(modid = TEN.MOD_ID, value = Dist.CLIENT)
public class FluidModelRenderer {

    private static final BlockTintSource NO_TINT = state -> -1;

    @SubscribeEvent
    public static void onRegisterFluidModels(RegisterFluidModelsEvent event) {
        // Liquid Royal Jelly
        registerFluidPair(event, TENFluids.LIQUID_ROYAL_JELLY_SOURCE, TENFluids.LIQUID_ROYAL_JELLY_FLOWING, "liquid_royal_jelly");
        // Liquid Spicy Jelly
        registerFluidPair(event, TENFluids.LIQUID_SPICY_JELLY_SOURCE, TENFluids.LIQUID_SPICY_JELLY_FLOWING, "liquid_spicy_jelly");
        // Liquid Honey
        registerFluidPair(event, TENFluids.LIQUID_HONEY_SOURCE, TENFluids.LIQUID_HONEY_FLOWING, "liquid_honey");
        // Liquid XP
        registerFluidPair(event, TENFluids.LIQUID_XP_SOURCE, TENFluids.LIQUID_XP_FLOWING, "liquid_xp");
        // Liquid Bizarrerie
        registerFluidPair(event, TENFluids.LIQUID_BIZARRERIE_SOURCE, TENFluids.LIQUID_BIZARRERIE_FLOWING, "liquid_bizarrerie");
    }

    private static void registerFluidPair(RegisterFluidModelsEvent event,
                                           net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.level.material.Fluid, ?> source,
                                           net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.level.material.Fluid, ?> flowing,
                                           String baseName) {
        Material still = new Material(TEN.id("block/fluid/" + baseName));
        Material flowingMat = new Material(TEN.id("block/fluid/" + baseName + "_flowing"));
        var unbaked = new FluidModel.Unbaked(still, flowingMat, still, NO_TINT);
        event.register(unbaked, source, flowing);
    }
}
