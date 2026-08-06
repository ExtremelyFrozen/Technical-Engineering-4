// -*- coding: utf-8 -*-
package com.modularmc.ten.network;

import com.modularmc.ten.TEN;
import com.modularmc.ten.common.data.TENRecipeTypes;

import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Server-side handler that sends TEN machine recipe types and vanilla cooking
 * recipe types to the client via NeoForge's native datapack sync mechanism.
 * <p>
 * Uses {@link OnDatapackSyncEvent#sendRecipes(RecipeType<?>...)} which works
 * without JEI on the server — the client will receive a
 * {@link net.neoforged.neoforge.client.event.RecipesReceivedEvent} with the
 * requested recipe types populated in the recipe map.
 * <p>
 * This fires on both player join and datapack reload (when
 * {@code event.getPlayer() == null}), covering both scenarios.
 * <p>
 * Five TEN {@link com.modularmc.ten.api.recipe.FormsCombinedRecipe} types plus
 * three vanilla cooking types (SMELTING, BLASTING, SMOKING) are sent so JEI
 * on the client can display them without an integrated server.
 * <p>
 * Call {@link #init()} during common setup to register the game bus listener.
 */
public final class TENRecipeSync {

    private TENRecipeSync() {
        // Utility class — no instances
    }

    private static final AtomicBoolean initialized = new AtomicBoolean(false);

    /**
     * Registers the {@link OnDatapackSyncEvent} listener on
     * {@link NeoForge#EVENT_BUS}. Idempotent — repeated calls do not
     * re-register the listener. Must be called during common setup.
     */
    public static void init() {
        if (!initialized.compareAndSet(false, true)) {
            TEN.LOGGER.debug("TENRecipeSync.init() called again — skipping");
            return;
        }
        NeoForge.EVENT_BUS.addListener(TENRecipeSync::onDatapackSync);
        TEN.LOGGER.debug("TENRecipeSync initialized — OnDatapackSyncEvent listener registered");
    }

    private static final int TEN_TYPE_COUNT = 5;
    private static final int VANILLA_TYPE_COUNT = 3;
    private static final int TOTAL_TYPE_COUNT = TEN_TYPE_COUNT + VANILLA_TYPE_COUNT;

    private static void onDatapackSync(OnDatapackSyncEvent event) {
        // ── Five TEN FormsCombinedRecipe types ────────────────────────
        // These are the machine recipe types that JEI displays on client.
        // Without explicit sendRecipes, dedicated server clients won't
        // receive them in RecipesReceivedEvent.
        event.sendRecipes(
                TENRecipeTypes.PULVERIZER_T.get(),
                TENRecipeTypes.COMPRESSOR_T.get(),
                TENRecipeTypes.REFINER_T.get(),
                TENRecipeTypes.INDUCTION_FURNACE_T.get(),
                TENRecipeTypes.PSIONICANT_T.get());

        // ── Three vanilla cooking types ──────────────────────────────
        // Smelter JEI categories source their recipes from JEI's built-in
        // vanilla recipe lookups. On servers without JEI, these must be
        // explicitly sent or the JEI lookup will return empty.
        event.sendRecipes(
                RecipeType.SMELTING,
                RecipeType.BLASTING,
                RecipeType.SMOKING);

        TEN.LOGGER.info("TENRecipeSync: sent {} TEN + {} vanilla recipe types (total {})",
                TEN_TYPE_COUNT, VANILLA_TYPE_COUNT, TOTAL_TYPE_COUNT);
    }
}
