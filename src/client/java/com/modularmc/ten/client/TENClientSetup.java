// -*- coding: utf-8 -*-
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
 * <p>
 * Uses {@code value = Dist.CLIENT} to restrict registration to the
 * physical client side. In NeoForge 26.1, {@code @EventBusSubscriber}
 * with a distribution value registers for both mod bus and game bus
 * events on that side.
 */
@EventBusSubscriber(modid = TEN.MOD_ID, value = Dist.CLIENT)
public class TENClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Initialize the client recipe cache that receives RecipesReceivedEvent
        // from the NeoForge game bus. This cache provides TEN machine recipes
        // to the JEI plugin without requiring an integrated server.
        TENClientRecipeCache.init();
    }
}
