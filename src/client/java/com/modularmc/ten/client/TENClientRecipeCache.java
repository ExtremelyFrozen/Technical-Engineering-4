// -*- coding: utf-8 -*-
package com.modularmc.ten.client;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.recipe.FormsCombinedRecipe;
import com.modularmc.ten.common.data.TENRecipeTypes;
import com.modularmc.ten.network.JeiSyncState;

import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Client-only cache of TEN machine recipes received from the server via
 * NeoForge's {@link RecipesReceivedEvent}.
 * <p>
 * This cache exists only on the client and has no JEI import statements.
 * {@link JeiSyncState} governs session gate, snapshot readiness, and the
 * machine injection guard — see that class for state machine semantics.
 * <p>
 * <b>Session gate:</b> The cache only accepts recipe events when the
 * session is active (between login and logout). Late events after logout
 * are silently rejected.
 * <p>
 * <b>Complete snapshot semantics:</b> The cache only marks a snapshot
 * as complete (via {@link JeiSyncState#acceptSnapshot()}) when a
 * {@link RecipesReceivedEvent} confirms that <em>all five</em> TEN
 * target recipe types were received from the server. Partial events
 * or events missing any target type are ignored.
 * <p>
 * Call {@link #init()} during client setup to register game bus listeners.
 * {@link #init()} is idempotent.
 */
public final class TENClientRecipeCache {

    static final JeiSyncState STATE = new JeiSyncState();

    private static final Map<RecipeType<?>, List<FormsCombinedRecipe>> CACHED = new HashMap<>();
    private static final AtomicBoolean initCalled = new AtomicBoolean(false);

    private TENClientRecipeCache() {}

    private static final List<RecipeType<?>> TARGET_TYPES = List.of(
            TENRecipeTypes.PULVERIZER_T.get(),
            TENRecipeTypes.COMPRESSOR_T.get(),
            TENRecipeTypes.REFINER_T.get(),
            TENRecipeTypes.INDUCTION_FURNACE_T.get(),
            TENRecipeTypes.PSIONICANT_T.get()
    );

    private static final Set<RecipeType<?>> TARGET_TYPE_SET = Set.copyOf(TARGET_TYPES);

    public static void init() {
        if (!initCalled.compareAndSet(false, true)) {
            TEN.LOGGER.debug("TENClientRecipeCache.init() called again — skipping");
            return;
        }
        NeoForge.EVENT_BUS.addListener(TENClientRecipeCache::onRecipesReceived);
        NeoForge.EVENT_BUS.addListener(TENClientRecipeCache::onPlayerLogin);
        NeoForge.EVENT_BUS.addListener(TENClientRecipeCache::onPlayerLogout);
        TEN.LOGGER.debug("TENClientRecipeCache initialized");
    }

    // ── Session gate ─────────────────────────────────────────────────

    private static void onPlayerLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        STATE.startSession();
        synchronized (TENClientRecipeCache.class) {
            CACHED.clear();
        }
        TEN.LOGGER.debug("TENClientRecipeCache: session started");
    }

    private static void onPlayerLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        STATE.endSession();
        synchronized (TENClientRecipeCache.class) {
            CACHED.clear();
        }
        TEN.LOGGER.debug("TENClientRecipeCache: session ended, cache cleared");
    }

    // ── Recipe event handling ────────────────────────────────────────

    private static void onRecipesReceived(RecipesReceivedEvent event) {
        // Reject events when not in an active session (late/stale events)
        if (!STATE.isSessionAccepting()) return;

        Set<RecipeType<?>> eventTypes = event.getRecipeTypes();
        if (eventTypes == null || !eventTypes.containsAll(TARGET_TYPE_SET)) {
            TEN.LOGGER.debug("TENClientRecipeCache: partial or null event — not marking snapshot complete");
            return;
        }

        RecipeMap recipeMap = event.getRecipeMap();
        if (recipeMap == null) {
            TEN.LOGGER.debug("TENClientRecipeCache: null recipe map — ignoring event");
            return;
        }

        Map<RecipeType<?>, List<FormsCombinedRecipe>> newCache = new HashMap<>();

        for (RecipeType<?> type : TARGET_TYPES) {
            List<FormsCombinedRecipe> recipes = collectRecipes(recipeMap, type);
            newCache.put(type, Collections.unmodifiableList(recipes));
        }

        synchronized (TENClientRecipeCache.class) {
            CACHED.clear();
            CACHED.putAll(newCache);
        }

        STATE.acceptSnapshot();

        // Log snapshot summary: generation and recipe count per type
        StringBuilder recipeSummary = new StringBuilder();
        for (RecipeType<?> type : TARGET_TYPES) {
            List<FormsCombinedRecipe> recipes = CACHED.getOrDefault(type, List.of());
            recipeSummary.append(type.toString()).append("=").append(recipes.size()).append(" ");
        }
        TEN.LOGGER.info("TENClientRecipeCache: snapshot accepted (generation {}), recipes: {}",
                STATE.getGeneration(), recipeSummary.toString().trim());
    }

    // ── Public queries ───────────────────────────────────────────────

    public static List<FormsCombinedRecipe> getMachineRecipes(RecipeType<?> type) {
        synchronized (TENClientRecipeCache.class) {
            return CACHED.getOrDefault(type, List.of());
        }
    }

    public static boolean hasRecipes(RecipeType<?> type) {
        synchronized (TENClientRecipeCache.class) {
            return CACHED.containsKey(type) && !CACHED.get(type).isEmpty();
        }
    }

    // Package-private for use by TENJeiPlugin
    public static JeiSyncState getState() {
        return STATE;
    }

    // ── Recipe collection ────────────────────────────────────────────

    @SuppressWarnings("rawtypes")
    private static List<FormsCombinedRecipe> collectRecipes(RecipeMap recipeMap, RecipeType<?> targetType) {
        @SuppressWarnings("unchecked")
        java.util.Collection<RecipeHolder<?>> raw = (java.util.Collection) recipeMap.byType((RecipeType) targetType);
        if (raw == null || raw.isEmpty()) return List.of();

        List<FormsCombinedRecipe> result = new ArrayList<>();
        for (Object obj : raw) {
            if (obj instanceof RecipeHolder<?> holder && holder.value() instanceof FormsCombinedRecipe fcr) {
                result.add(fcr);
            }
        }
        return result;
    }
}
