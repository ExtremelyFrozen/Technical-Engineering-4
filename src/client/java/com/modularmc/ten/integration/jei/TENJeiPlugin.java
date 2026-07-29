// -*- coding: utf-8 -*-
package com.modularmc.ten.integration.jei;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.recipe.FormsCombinedRecipe;
import com.modularmc.ten.common.blockentity.EngineFuelRecipe;
import com.modularmc.ten.common.blockentity.MatchFuel;
import com.modularmc.ten.common.blockentity.machine.BiomassBlockEntity;
import com.modularmc.ten.common.blockentity.machine.ExtractorBlockEntity;
import com.modularmc.ten.common.blockentity.machine.MetalizerBlockEntity;
import com.modularmc.ten.common.data.TENBlocks;
import com.modularmc.ten.common.data.TENRecipeTypes;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.neoforged.neoforge.registries.DeferredHolder;

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

    // ── Engine fuel recipe types ─────────────────────────────────────
    public static final mezz.jei.api.recipe.RecipeType<EngineFuelRecipe> EXTRACTOR_FUEL =
            new mezz.jei.api.recipe.RecipeType<>(TEN.id("extractor_fuel"), EngineFuelRecipe.class);
    public static final mezz.jei.api.recipe.RecipeType<EngineFuelRecipe> METALIZER_FUEL =
            new mezz.jei.api.recipe.RecipeType<>(TEN.id("metalizer_fuel"), EngineFuelRecipe.class);
    public static final mezz.jei.api.recipe.RecipeType<EngineFuelRecipe> BIOMASS_FUEL =
            new mezz.jei.api.recipe.RecipeType<>(TEN.id("biomass_fuel"), EngineFuelRecipe.class);

    // ── Smelter runtime manager identity ──────────────────────────────
    // Guards against duplicate addRecipes when JEI calls onRuntimeAvailable
    // with the same IRecipeManager instance. A new manager (server switch)
    // still triggers a fresh collection.
    private IRecipeManager populatedSmelterManager;

    // ── Smelter category icons ───────────────────────────────────────
    private static final ItemStack SMELTER_ICON = icon("machine_smelter");
    private static final ItemStack BLAST_ICON = icon("blast_levelup");
    private static final ItemStack SMOKE_ICON = icon("smoke_levelup");

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

    private record CategoryDef(String name, Identifier id, ItemStack icon) {}

    private List<CategoryDef> buildCategories() {
        return List.of(
                new CategoryDef("Pulverizer", TEN.id("pulverizer"), icon("machine_pulverizer")),
                new CategoryDef("Compressor", TEN.id("compressor"), icon("machine_compressor")),
                new CategoryDef("Refiner", TEN.id("refiner"), icon("machine_refiner")),
                new CategoryDef("Induction Furnace", TEN.id("induction_furnace"), icon("machine_induction_furnace")),
                new CategoryDef("Psionicant", TEN.id("psionicant"), icon("machine_psionicant")));
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

        // P3: Three independent smelter JEI pages
        registration.addRecipeCategories(
                new SmelterJeiCategory(helper, TEN.id("smelter_smelting"), SMELTER_SMELTING, SMELTER_ICON),
                new SmelterJeiCategory(helper, TEN.id("smelter_blasting"), SMELTER_BLASTING, BLAST_ICON),
                new SmelterJeiCategory(helper, TEN.id("smelter_smoking"), SMELTER_SMOKING, SMOKE_ICON)
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
    }

    // ── Recipe Registration ──────────────────────────────────────────────
    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (!TEN.Mods.isJEILoaded()) return;

        // ── Smelter recipes are no longer collected here.
        // They are now sourced at runtime from JEI built-in lookups in
        // {@link #onRuntimeAvailable(IJeiRuntime)}. This avoids the
        // dependency on {@code Minecraft.getInstance().getSingleplayerServer()}
        // which is null on dedicated server clients.
        //
        // ── Existing machine recipes ───────────────────────────────
        // These still use the integrated-server RecipeManager path;
        // only smelter recipe sourcing has been migrated to the
        // runtime-available pattern.
        var server = Minecraft.getInstance().getSingleplayerServer();
        net.minecraft.world.item.crafting.RecipeManager recipeManager = null;
        if (server != null) {
            recipeManager = server.getRecipeManager();
        }

        if (recipeManager != null) {
            // Existing machine categories
            for (var data : buildCategories()) {
                List<FormsCombinedRecipe> recipes = new ArrayList<>();
                var type = new mezz.jei.api.recipe.RecipeType<>(data.id, FormsCombinedRecipe.class);
                var targetType = recipeType(data.id).get();
                for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
                    if (holder.value().getType() == targetType) {
                        recipes.add((FormsCombinedRecipe) holder.value());
                    }
                }
                registration.addRecipes(type, recipes);
            }

            // [Smelter recipes removed from registerRecipes — see onRuntimeAvailable]
        }

        // ── Engine fuel recipes from IIngredientManager ────────────────
        var ingredientManager = registration.getIngredientManager();
        if (ingredientManager == null) return;

        var allStacks = ingredientManager.getAllItemStacks();
        if (allStacks == null) return;

        var clientLevel = Objects.requireNonNull(Minecraft.getInstance().level,
                "Client level required for JEI Extractor fuel registration");

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

    // ── Runtime Available — Smelter recipe sourcing ───────────────────────
    /**
     * Populates the three custom smelter JEI categories from JEI's built-in
     * vanilla cooking recipe lookups.
     * <p>
     * This method replaces the old {@code registerRecipes}-based smelter
     * collection that relied on {@code Minecraft.getInstance().getSingleplayerServer()},
     * which is {@code null} on dedicated server clients. The new approach uses
     * {@code IRecipeManager#createRecipeLookup(mezz.jei.api.recipe.RecipeType)}
     * on JEI's client-side runtime, working correctly in both singleplayer and
     * multiplayer environments.
     * <p>
     * A manager identity guard prevents duplicate additions when JEI calls
     * this method multiple times with the same {@link IRecipeManager} instance.
     * A different manager (server switch) still triggers fresh collection.
     */
    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        if (!TEN.Mods.isJEILoaded()) return;
        IRecipeManager recipeManager = jeiRuntime.getRecipeManager();

        // Manager identity guard: skip if same manager already populated
        if (recipeManager == populatedSmelterManager) return;

        // Collect from JEI built-in vanilla recipe lookups via IRecipeHolderType,
        // which is the non-deprecated holder-based API for vanilla recipe types.
        var smelting = collectFromBuiltin(recipeManager,
                IRecipeHolderType.create(net.minecraft.world.item.crafting.RecipeType.SMELTING));
        var blasting = collectFromBuiltin(recipeManager,
                IRecipeHolderType.create(net.minecraft.world.item.crafting.RecipeType.BLASTING));
        var smoking = collectFromBuiltin(recipeManager,
                IRecipeHolderType.create(net.minecraft.world.item.crafting.RecipeType.SMOKING));

        // Add to custom smelter types. JEI runtime is fresh per invocation,
        // so there is no residue from a previous lifecycle to clear.
        recipeManager.addRecipes(SMELTER_SMELTING, smelting);
        recipeManager.addRecipes(SMELTER_BLASTING, blasting);
        recipeManager.addRecipes(SMELTER_SMOKING, smoking);

        // Identity guard populated only after successful addRecipes.
        // If any step above throws, the guard is not set and the next
        // onRuntimeAvailable call with the same manager will retry.
        populatedSmelterManager = recipeManager;
    }

    /**
     * Collect vanilla cooking recipes from JEI's built-in recipe lookups
     * and wrap them into {@link SmelterJeiCategory.Recipe} payloads.
     * <p>
     * Uses {@code IRecipeManager#createRecipeLookup} on JEI's client-side
     * runtime, making it available on dedicated server clients.
     * <p>
     * The generic bound {@code T extends AbstractCookingRecipe} preserves
     * type safety for all three vanilla cooking recipe subtypes.
     */
    private static <T extends AbstractCookingRecipe> List<SmelterJeiCategory.Recipe> collectFromBuiltin(
            IRecipeManager recipeManager,
            IRecipeHolderType<T> builtinType) {
        return recipeManager.createRecipeLookup(builtinType)
                .get()
                .map(holder -> {
                    T recipe = holder.value();
                    var ingredient = recipe.input();
                    if (ingredient.isEmpty()) return null;
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

        // P3: Smelter — three independent JEI pages, removed from vanilla SMELTING
        registration.addRecipeCatalyst(SMELTER_ICON, SMELTER_SMELTING);
        registration.addRecipeCatalyst(SMELTER_ICON, SMELTER_BLASTING);
        registration.addRecipeCatalyst(SMELTER_ICON, SMELTER_SMOKING);

        registration.addRecipeCatalyst(engineIcon("extraction"), EXTRACTOR_FUEL);
        registration.addRecipeCatalyst(engineIcon("metal"), METALIZER_FUEL);
        registration.addRecipeCatalyst(engineIcon("biomass"), BIOMASS_FUEL);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        if (!TEN.Mods.isJEILoaded()) return;

        registration.addGuiContainerHandler(ModularUIContainerScreen.class, ModularUIJEIHandlers.GUI_CONTAINER_HANDLER);
    }

    private static DeferredHolder<net.minecraft.world.item.crafting.RecipeType<?>, net.minecraft.world.item.crafting.RecipeType<FormsCombinedRecipe>> recipeType(Identifier id) {
        if (id.equals(TEN.id("pulverizer"))) return TENRecipeTypes.PULVERIZER_T;
        if (id.equals(TEN.id("compressor"))) return TENRecipeTypes.COMPRESSOR_T;
        if (id.equals(TEN.id("refiner"))) return TENRecipeTypes.REFINER_T;
        if (id.equals(TEN.id("induction_furnace"))) return TENRecipeTypes.INDUCTION_FURNACE_T;
        if (id.equals(TEN.id("psionicant"))) return TENRecipeTypes.PSIONICANT_T;
        throw new IllegalArgumentException("Unknown TEN recipe type id: " + id);
    }
}
