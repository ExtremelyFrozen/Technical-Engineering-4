// -*- coding: utf-8 -*-
package com.modularmc.ten.integration.jei;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.recipe.FormsCombinedRecipe;
import com.modularmc.ten.client.TENClientRecipeCache;
import com.modularmc.ten.common.blockentity.EngineFuelRecipe;
import com.modularmc.ten.common.blockentity.MatchFuel;
import com.modularmc.ten.common.blockentity.machine.BiomassBlockEntity;
import com.modularmc.ten.common.blockentity.machine.ExtractorBlockEntity;
import com.modularmc.ten.common.blockentity.machine.MetalizerBlockEntity;
import com.modularmc.ten.common.data.TENBlocks;
import com.modularmc.ten.common.data.TENRecipeTypes;
import com.modularmc.ten.network.JeiSyncState;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;
import net.neoforged.neoforge.common.NeoForge;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerScreen;
import com.lowdragmc.lowdraglib2.integration.xei.jei.ModularUIJEIHandlers;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

@JeiPlugin
public class TENJeiPlugin implements IModPlugin {

    private static final Identifier UID = TEN.id("jei_plugin");

    // ── Smelter JEI recipe types (three independent pages) ────────────
    public static final mezz.jei.api.recipe.RecipeType<SmelterJeiCategory.Recipe> SMELTER_SMELTING =
            new mezz.jei.api.recipe.RecipeType<>(TEN.id("smelter_smelting"), SmelterJeiCategory.Recipe.class);
    public static final mezz.jei.api.recipe.RecipeType<SmelterJeiCategory.Recipe> SMELTER_BLASTING =
            new mezz.jei.api.recipe.RecipeType<>(TEN.id("smelter_blasting"), SmelterJeiCategory.Recipe.class);
    public static final mezz.jei.api.recipe.RecipeType<SmelterJeiCategory.Recipe> SMELTER_SMOKING =
            new mezz.jei.api.recipe.RecipeType<>(TEN.id("smelter_smoking"), SmelterJeiCategory.Recipe.class);

    // ── Machine JEI recipe type definitions ──────────────────────────
    // NOTE: tenType uses Supplier<RecipeType> to avoid eager DeferredHolder.get()
    // at class-load time.  JEI ServiceLoader may trigger TENJeiPlugin loading
    // before NeoForge registries are fully populated; Supplier defers resolution
    // until injection time (tryInjectMachineRecipes), where null is handled safely.
    private record MachineTypeDef(
            mezz.jei.api.recipe.RecipeType<FormsCombinedRecipe> jeiType,
            Supplier<net.minecraft.world.item.crafting.RecipeType<FormsCombinedRecipe>> tenType) {}

    private static final List<MachineTypeDef> MACHINE_TYPES = buildMachineTypeDefs();

    // ── Engine fuel recipe types ─────────────────────────────────────
    public static final mezz.jei.api.recipe.RecipeType<EngineFuelRecipe> EXTRACTOR_FUEL =
            new mezz.jei.api.recipe.RecipeType<>(TEN.id("extractor_fuel"), EngineFuelRecipe.class);
    public static final mezz.jei.api.recipe.RecipeType<EngineFuelRecipe> METALIZER_FUEL =
            new mezz.jei.api.recipe.RecipeType<>(TEN.id("metalizer_fuel"), EngineFuelRecipe.class);
    public static final mezz.jei.api.recipe.RecipeType<EngineFuelRecipe> BIOMASS_FUEL =
            new mezz.jei.api.recipe.RecipeType<>(TEN.id("biomass_fuel"), EngineFuelRecipe.class);

    // ═══════════════════════════════════════════════════════════════════
    //  Runtime coordination
    // ═══════════════════════════════════════════════════════════════════

    // Active JEI runtime recipe manager — set on onRuntimeAvailable,
    // nulled in deactivateRuntime(). volatile for cross-thread visibility.
    private static volatile IRecipeManager activeRecipeManager;

    // Smelter guard: separate from machine injection. volatile for
    // cross-thread visibility (JEI thread vs event bus thread).
    private static volatile IRecipeManager smelterPopulatedManager;

    // Cache update listener life-cycle: volatile since these flags are
    // written from onRuntimeAvailable/deactivateRuntime (JEI thread) and
    // read from onCacheUpdated (NeoForge event bus thread).
    private static volatile boolean cacheListenerRegistered;
    private static volatile boolean cacheListenerActive;

    // ── Static helpers ───────────────────────────────────────────────

    private static ItemStack icon(String name) {
        var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(TEN.id(name));
        return item.isPresent() ? new ItemStack(item.get()) : new ItemStack(Items.FURNACE);
    }

    private static ItemStack engineIcon(String engineName) {
        String blockId = "engine_" + engineName;
        var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(TEN.id(blockId));
        return item.isPresent() ? new ItemStack(item.get()) : new ItemStack(Items.FURNACE);
    }

    // ── Lazy icon accessor ──────────────────────────────────────────────
    // Single unified icon method for all three smelter JEI pages (smelting,
    // blasting, smoking) and their catalysts.  All use the machine_smelter
    // block item icon, consistent with other TEN machine categories.
    // Resolution is deferred to JEI callback time (registerCategories,
    // registerRecipeCatalysts), when BuiltInRegistries is fully populated
    // and ItemStack construction is safe.  Each call returns a fresh
    // independent ItemStack.
    static ItemStack smelterIcon() { return icon("machine_smelter"); }

    private record CategoryDef(String name, Identifier id, ItemStack icon) {}

    private List<CategoryDef> buildCategories() {
        return List.of(
                new CategoryDef("Pulverizer", TEN.id("pulverizer"), icon("machine_pulverizer")),
                new CategoryDef("Compressor", TEN.id("compressor"), icon("machine_compressor")),
                new CategoryDef("Refiner", TEN.id("refiner"), icon("machine_refiner")),
                new CategoryDef("Induction Furnace", TEN.id("induction_furnace"), icon("machine_induction_furnace")),
                new CategoryDef("Psionicant", TEN.id("psionicant"), icon("machine_psionicant")));
    }

    private static List<MachineTypeDef> buildMachineTypeDefs() {
        // Use method references (::get) for lazy Supplier resolution,
        // avoiding direct DeferredHolder get() at class-load time.
        return List.of(
                new MachineTypeDef(
                        new mezz.jei.api.recipe.RecipeType<>(TEN.id("pulverizer"), FormsCombinedRecipe.class),
                        TENRecipeTypes.PULVERIZER_T::get),
                new MachineTypeDef(
                        new mezz.jei.api.recipe.RecipeType<>(TEN.id("compressor"), FormsCombinedRecipe.class),
                        TENRecipeTypes.COMPRESSOR_T::get),
                new MachineTypeDef(
                        new mezz.jei.api.recipe.RecipeType<>(TEN.id("refiner"), FormsCombinedRecipe.class),
                        TENRecipeTypes.REFINER_T::get),
                new MachineTypeDef(
                        new mezz.jei.api.recipe.RecipeType<>(TEN.id("induction_furnace"), FormsCombinedRecipe.class),
                        TENRecipeTypes.INDUCTION_FURNACE_T::get),
                new MachineTypeDef(
                        new mezz.jei.api.recipe.RecipeType<>(TEN.id("psionicant"), FormsCombinedRecipe.class),
                        TENRecipeTypes.PSIONICANT_T::get)
        );
    }

    @Override
    public @NotNull Identifier getPluginUid() {
        return UID;
    }

    // ── Category Registration ────────────────────────────────────────────
    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        if (!TEN.Mods.isJEILoaded()) return;

        IGuiHelper helper = registration.getJeiHelpers().getGuiHelper();

        for (var data : buildCategories()) {
            var type = new mezz.jei.api.recipe.RecipeType<FormsCombinedRecipe>(data.id, FormsCombinedRecipe.class);
            registration.addRecipeCategories(new TENJeiCategory(helper, data.id, type, data.icon));
        }

        registration.addRecipeCategories(
                new SmelterJeiCategory(helper, TEN.id("smelter_smelting"), SMELTER_SMELTING, smelterIcon()),
                new SmelterJeiCategory(helper, TEN.id("smelter_blasting"), SMELTER_BLASTING, smelterIcon()),
                new SmelterJeiCategory(helper, TEN.id("smelter_smoking"), SMELTER_SMOKING, smelterIcon())
        );

        registration.addRecipeCategories(
                new EngineFuelCategory(helper, TEN.id("extractor_fuel"), EXTRACTOR_FUEL,
                        engineIcon("extraction"),
                        TENBlocks.ENGINE_EXTRACTION.get().getName()),
                new EngineFuelCategory(helper, TEN.id("metalizer_fuel"), METALIZER_FUEL,
                        engineIcon("metal"),
                        TENBlocks.ENGINE_METAL.get().getName()),
                new EngineFuelCategory(helper, TEN.id("biomass_fuel"), BIOMASS_FUEL,
                        engineIcon("biomass"),
                        TENBlocks.ENGINE_BIOMASS.get().getName())
        );

        TEN.LOGGER.info("TEN JEI plugin categories registered — uid: {}", UID);
    }

    // ── Recipe Registration ──────────────────────────────────────────────
    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (!TEN.Mods.isJEILoaded()) return;

        // ── Engine fuel recipes from IIngredientManager ────────────────
        var ingredientManager = registration.getIngredientManager();
        if (ingredientManager == null) return;

        var allStacks = ingredientManager.getAllItemStacks();
        if (allStacks == null) return;

        var clientLevel = Minecraft.getInstance().level;
        if (clientLevel == null) return;

        List<EngineFuelRecipe> extractorRecipes = new ArrayList<>();
        List<EngineFuelRecipe> metalizerRecipes = new ArrayList<>();
        List<EngineFuelRecipe> biomassRecipes = new ArrayList<>();

        for (ItemStack stack : allStacks) {
            if (stack == null || stack.isEmpty()) continue;

            int extractorValue = MatchFuel.getExtractorFuelValue(clientLevel, stack);
            if (extractorValue > 0) {
                extractorRecipes.add(new EngineFuelRecipe(stack, extractorValue, ExtractorBlockEntity.BASE_GENERATION_RATE));
            }

            int metalValue = MatchFuel.getMetalFuelValue(stack);
            if (metalValue > 0) {
                metalizerRecipes.add(new EngineFuelRecipe(stack, metalValue, MetalizerBlockEntity.BASE_GENERATION_RATE));
            }

            int biomassValue = MatchFuel.getBiomassFuelValue(stack);
            if (biomassValue > 0) {
                biomassRecipes.add(new EngineFuelRecipe(stack, biomassValue, BiomassBlockEntity.BASE_GENERATION_RATE));
            }
        }

        Comparator<EngineFuelRecipe> byItemIdThenComponents = Comparator.comparing(
                (EngineFuelRecipe r) -> r.ingredients().get(0).getItem().toString());

        registration.addRecipes(EXTRACTOR_FUEL, dedupSorted(extractorRecipes, byItemIdThenComponents));
        registration.addRecipes(METALIZER_FUEL, dedupSorted(metalizerRecipes, byItemIdThenComponents));
        registration.addRecipes(BIOMASS_FUEL, dedupSorted(biomassRecipes, byItemIdThenComponents));
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Runtime Available — cache→runtime bridge
    // ═══════════════════════════════════════════════════════════════════

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        if (!TEN.Mods.isJEILoaded()) return;
        IRecipeManager recipeManager = jeiRuntime.getRecipeManager();

        // Detect runtime change: a different IRecipeManager identity means
        // the previous JEI runtime is gone. deactivateRuntime() cleans up
        // all guards and nulls the old reference before we activate the new one.
        if (recipeManager != activeRecipeManager) {
            if (activeRecipeManager != null) {
                deactivateRuntime();
            }
            activeRecipeManager = recipeManager;
            TENClientRecipeCache.getState().activateRuntime();
        }

        // ── Smelter recipes (independent of cache readiness) ─────────
        if (smelterPopulatedManager != recipeManager) {
            injectSmelterRecipes(recipeManager);
            smelterPopulatedManager = recipeManager;
        }

        // ── Register cache → runtime bridge listener (once) ─────────
        if (!cacheListenerRegistered) {
            NeoForge.EVENT_BUS.addListener(TENJeiPlugin::onCacheUpdated);
            cacheListenerRegistered = true;
        }
        cacheListenerActive = true;

        // ── Machine recipes (from cache, if ready) ──────────────────
        tryInjectMachineRecipes();
    }

    /**
     * Deactivates the current runtime state: nulls the active manager
     * reference, clears the smelter guard, deactivates the cache listener,
     * and tells {@link JeiSyncState} to reset the machine injection guard.
     * <p>
     * Called from {@link #onRuntimeAvailable(IJeiRuntime)} when a change
     * in {@link IRecipeManager} identity is detected (world switch,
     * disconnect, reload). Not a JEI {@code IModPlugin} override — JEI
     * 29.13's API does not declare {@code onRuntimeUnavailable}.
     */
    private static void deactivateRuntime() {
        activeRecipeManager = null;
        smelterPopulatedManager = null;
        cacheListenerActive = false;
        TENClientRecipeCache.getState().deactivateRuntime();
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Cache → Runtime bridge listener
    // ═══════════════════════════════════════════════════════════════════

    private static void onCacheUpdated(RecipesReceivedEvent event) {
        if (!cacheListenerActive) return;
        tryInjectMachineRecipes();
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Machine recipe injection
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Attempts to inject machine recipes via {@link JeiSyncState#tryClaimInject()}.
     * Only proceeds if the state machine grants an injection slot.
     * At most one injection per active runtime.
     * <p>
     * Resolves {@link #MACHINE_TYPES} via {@link Supplier#get()} lazily at
     * injection time (not at class load), and guards against null if NeoForge
     * registries are not yet populated.
     */
    private static void tryInjectMachineRecipes() {
        JeiSyncState state = TENClientRecipeCache.getState();
        if (!state.tryClaimInject()) return;

        IRecipeManager manager = activeRecipeManager;
        if (manager == null) return;

        for (var def : MACHINE_TYPES) {
            var recipeType = def.tenType.get();
            if (recipeType == null) {
                TEN.LOGGER.warn("Skipping machine {} — RecipeType not yet registered",
                        def.jeiType.getUid());
                continue;
            }
            List<FormsCombinedRecipe> cached = TENClientRecipeCache.getMachineRecipes(recipeType);
            if (!cached.isEmpty()) {
                manager.addRecipes(def.jeiType, cached);
            }
        }

        state.markMachinesInjected();

        int totalRecipes = MACHINE_TYPES.stream()
                .mapToInt(def -> TENClientRecipeCache.getMachineRecipes(def.tenType.get()).size())
                .sum();
        TEN.LOGGER.info("TEN JEI machine recipes injected — generation: {}, total recipes: {}",
                state.getGeneration(), totalRecipes);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Smelter recipe injection
    // ═══════════════════════════════════════════════════════════════════

    private static void injectSmelterRecipes(IRecipeManager recipeManager) {
        var smelting = collectFromBuiltin(recipeManager,
                IRecipeHolderType.create(net.minecraft.world.item.crafting.RecipeType.SMELTING));
        var blasting = collectFromBuiltin(recipeManager,
                IRecipeHolderType.create(net.minecraft.world.item.crafting.RecipeType.BLASTING));
        var smoking = collectFromBuiltin(recipeManager,
                IRecipeHolderType.create(net.minecraft.world.item.crafting.RecipeType.SMOKING));

        recipeManager.addRecipes(SMELTER_SMELTING, smelting);
        recipeManager.addRecipes(SMELTER_BLASTING, blasting);
        recipeManager.addRecipes(SMELTER_SMOKING, smoking);

        TEN.LOGGER.info("TEN JEI smelter recipes injected — smelting: {}, blasting: {}, smoking: {}",
                smelting.size(), blasting.size(), smoking.size());
    }

    private static <T extends AbstractCookingRecipe> List<SmelterJeiCategory.Recipe> collectFromBuiltin(
            IRecipeManager recipeManager,
            IRecipeHolderType<T> builtinType) {
        return recipeManager.createRecipeLookup(builtinType)
                .get()
                .map(holder -> {
                    T recipe = holder.value();
                    var ingredient = recipe.input();
                    if (ingredient.isEmpty()) return null;
                    // Safe for AbstractCookingRecipe: result is static (result.copy()),
                    // independent of input. Single-argument assemble(RecipeInput) is
                    // the valid API in NeoForge 26.1.
                    ItemStack output = recipe.assemble(new SingleRecipeInput(ItemStack.EMPTY));
                    if (output.isEmpty()) return null;
                    return SmelterJeiCategory.Recipe.of(ingredient, output, recipe.cookingTime());
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private static List<EngineFuelRecipe> dedupSorted(List<EngineFuelRecipe> recipes, Comparator<EngineFuelRecipe> sortKey) {
        if (recipes.isEmpty()) return List.of();
        recipes.sort(sortKey);
        List<EngineFuelRecipe> result = new ArrayList<>();
        result.add(recipes.get(0));
        for (int i = 1; i < recipes.size(); i++) {
            ItemStack current = recipes.get(i).ingredients().get(0);
            ItemStack last = result.get(result.size() - 1).ingredients().get(0);
            if (!ItemStack.isSameItemSameComponents(current, last)) {
                result.add(recipes.get(i));
            }
        }
        return result;
    }

    // ── Catalyst Registration ────────────────────────────────────────────
    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        if (!TEN.Mods.isJEILoaded()) return;

        for (var data : buildCategories()) {
            var type = new mezz.jei.api.recipe.RecipeType<FormsCombinedRecipe>(data.id, FormsCombinedRecipe.class);
            registration.addRecipeCatalyst(data.icon, type);
        }

        registration.addRecipeCatalyst(smelterIcon(), SMELTER_SMELTING);
        registration.addRecipeCatalyst(smelterIcon(), SMELTER_BLASTING);
        registration.addRecipeCatalyst(smelterIcon(), SMELTER_SMOKING);

        registration.addRecipeCatalyst(engineIcon("extraction"), EXTRACTOR_FUEL);
        registration.addRecipeCatalyst(engineIcon("metal"), METALIZER_FUEL);
        registration.addRecipeCatalyst(engineIcon("biomass"), BIOMASS_FUEL);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        if (!TEN.Mods.isJEILoaded()) return;
        registration.addGuiContainerHandler(ModularUIContainerScreen.class, ModularUIJEIHandlers.GUI_CONTAINER_HANDLER);
    }
}
