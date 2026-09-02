package com.modularmc.ten.client;

import com.modularmc.ten.TEN;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Client setup handler that initializes client-only subsystems.
 * <p>
 * Must be in the {@code client} source set so it can reference client-only
 * classes without classloading issues on dedicated servers.
 */
@EventBusSubscriber(modid = TEN.MOD_ID, value = Dist.CLIENT)
public class TENClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Initialize the client recipe cache that receives RecipesUpdatedEvent
        // from the NeoForge game bus. This cache provides TEN machine recipes
        // to the JEI plugin without requiring an integrated server.
        TENClientRecipeCache.init();
    }
}