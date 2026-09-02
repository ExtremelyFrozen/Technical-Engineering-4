package com.modularmc.ten.client;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.recipe.FormsCombinedRecipe;
import com.modularmc.ten.common.data.TENRecipeTypes;
import com.modularmc.ten.network.JeiSyncState;

import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RecipesUpdatedEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Client-only cache of TEN machine recipes received from the server via
 * NeoForge's {@link RecipesUpdatedEvent}.
 * <p>
 * 1.21.1 适配：使用 {@link RecipesUpdatedEvent}（仅含 getRecipeManager）而非
 * 26.1.2 的 {@code RecipesReceivedEvent}（含 getRecipeTypes/getRecipeMap）。
 * 完整性检查通过 {@link RecipeManager#getAllRecipesFor(RecipeType)} 逐类型验证。
 */
public final class TENClientRecipeCache {

    static final JeiSyncState STATE = new JeiSyncState();

    private static final Map<RecipeType<?>, List<FormsCombinedRecipe>> CACHED = new HashMap<>();
    private static final AtomicBoolean initCalled = new AtomicBoolean(false);

    private TENClientRecipeCache() {}

    @SuppressWarnings("unchecked")
    private static final List<RecipeType<? extends FormsCombinedRecipe>> TARGET_TYPES = List.of(
            (RecipeType<? extends FormsCombinedRecipe>) TENRecipeTypes.PULVERIZER_T.get(),
            (RecipeType<? extends FormsCombinedRecipe>) TENRecipeTypes.COMPRESSOR_T.get(),
            (RecipeType<? extends FormsCombinedRecipe>) TENRecipeTypes.REFINER_T.get(),
            (RecipeType<? extends FormsCombinedRecipe>) TENRecipeTypes.INDUCTION_FURNACE_T.get(),
            (RecipeType<? extends FormsCombinedRecipe>) TENRecipeTypes.PSIONICANT_T.get()
    );

    public static void init() {
        if (!initCalled.compareAndSet(false, true)) {
            TEN.LOGGER.debug("TENClientRecipeCache.init() called again — skipping");
            return;
        }
        NeoForge.EVENT_BUS.addListener(TENClientRecipeCache::onRecipesUpdated);
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

    private static void onRecipesUpdated(RecipesUpdatedEvent event) {
        if (!STATE.isSessionAccepting()) return;

        RecipeManager recipeManager = event.getRecipeManager();
        if (recipeManager == null) return;

        // 1.21.1 适配：逐类型检查完整性（无 getRecipeTypes）
        for (var type : TARGET_TYPES) {
            var recipes = recipeManager.getAllRecipesFor((RecipeType) type);
            if (recipes == null || recipes.isEmpty()) {
                TEN.LOGGER.debug("TENClientRecipeCache: type {} not yet received — not marking snapshot complete", type);
                return;
            }
        }

        // All target types received — accept snapshot
        Map<RecipeType<?>, List<FormsCombinedRecipe>> newCache = new HashMap<>();
        for (var type : TARGET_TYPES) {
            List<FormsCombinedRecipe> recipes = collectRecipes(recipeManager, type);
            newCache.put(type, Collections.unmodifiableList(recipes));
        }

        synchronized (TENClientRecipeCache.class) {
            CACHED.clear();
            CACHED.putAll(newCache);
        }

        STATE.acceptSnapshot();

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

    @SuppressWarnings("unchecked")
    private static <T extends FormsCombinedRecipe> List<FormsCombinedRecipe> collectRecipes(
            RecipeManager recipeManager, RecipeType<? extends FormsCombinedRecipe> targetType) {
        var holders = recipeManager.getAllRecipesFor((RecipeType<T>) targetType);
        if (holders == null || holders.isEmpty()) return List.of();
        List<FormsCombinedRecipe> result = new ArrayList<>();
        for (var holder : holders) {
            result.add(holder.value());
        }
        return result;
    }
}