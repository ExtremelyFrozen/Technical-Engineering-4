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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.registries.DeferredHolder;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerScreen;
import com.lowdragmc.lowdraglib2.integration.xei.jei.ModularUIJEIHandlers;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
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

    // ── Engine fuel recipe types (shared EngineFuelRecipe class) ──────────
    public static final RecipeType<EngineFuelRecipe> EXTRACTOR_FUEL =
            new RecipeType<>(TEN.id("extractor_fuel"), EngineFuelRecipe.class);
    public static final RecipeType<EngineFuelRecipe> METALIZER_FUEL =
            new RecipeType<>(TEN.id("metalizer_fuel"), EngineFuelRecipe.class);
    public static final RecipeType<EngineFuelRecipe> BIOMASS_FUEL =
            new RecipeType<>(TEN.id("biomass_fuel"), EngineFuelRecipe.class);

    @Override
    public @NotNull Identifier getPluginUid() {
        return UID;
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

    private static ItemStack icon(String name) {
        var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(TEN.id(name));
        return item.isPresent() ? new ItemStack(item.get()) : new ItemStack(Items.FURNACE);
    }

    // ── Engine icon helpers ──────────────────────────────────────────────
    private static ItemStack engineIcon(String engineName) {
        String blockId = "engine_" + engineName;
        var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(TEN.id(blockId));
        return item.isPresent() ? new ItemStack(item.get()) : new ItemStack(Items.FURNACE);
    }

    // ── Category Registration ────────────────────────────────────────────
    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        if (!TEN.Mods.isJEILoaded()) return;

        IGuiHelper helper = registration.getJeiHelpers().getGuiHelper();

        // Existing machine categories
        for (var data : buildCategories()) {
            RecipeType<FormsCombinedRecipe> type = new RecipeType<>(data.id, FormsCombinedRecipe.class);
            registration.addRecipeCategories(new TENJeiCategory(helper, data.id, type, data.icon));
        }

        // Engine fuel categories (Solar excluded)
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

        // Existing machine recipes (from server recipe manager)
        var server = Minecraft.getInstance().getSingleplayerServer();
        if (server != null) {
            var recipeManager = server.getRecipeManager();
            if (recipeManager != null) {
                for (var data : buildCategories()) {
                    List<FormsCombinedRecipe> recipes = new ArrayList<>();
                    var type = new RecipeType<>(data.id, FormsCombinedRecipe.class);
                    var targetType = recipeType(data.id).get();
                    for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
                        if (holder.value().getType() == targetType) {
                            recipes.add((FormsCombinedRecipe) holder.value());
                        }
                    }
                    registration.addRecipes(type, recipes);
                }
            }
        }

        // ── Engine fuel recipes from IIngredientManager ────────────────
        var ingredientManager = registration.getIngredientManager();
        if (ingredientManager == null) return;

        var allStacks = ingredientManager.getAllItemStacks();
        if (allStacks == null) return;

        // Client level is guaranteed to be loaded at registerRecipes time
        // (same contract as JEI's own FuelRecipeMaker).
        var level = Objects.requireNonNull(Minecraft.getInstance().level,
                "Client level required for JEI Extractor fuel registration");

        List<EngineFuelRecipe> extractorRecipes = new ArrayList<>();
        List<EngineFuelRecipe> metalizerRecipes = new ArrayList<>();
        List<EngineFuelRecipe> biomassRecipes = new ArrayList<>();

        for (ItemStack stack : allStacks) {
            if (stack == null || stack.isEmpty()) continue;

            // Extractor: uses FuelValues via required non-null Level
            int extractorValue = MatchFuel.getExtractorFuelValue(level, stack);
            if (extractorValue > 0) {
                extractorRecipes.add(new EngineFuelRecipe(stack, extractorValue, ExtractorBlockEntity.BASE_GENERATION_RATE));
            }

            // Metalizer
            int metalValue = MatchFuel.getMetalFuelValue(stack);
            if (metalValue > 0) {
                metalizerRecipes.add(new EngineFuelRecipe(stack, metalValue, MetalizerBlockEntity.BASE_GENERATION_RATE));
            }

            // Biomass
            int biomassValue = MatchFuel.getBiomassFuelValue(stack);
            if (biomassValue > 0) {
                biomassRecipes.add(new EngineFuelRecipe(stack, biomassValue, BiomassBlockEntity.BASE_GENERATION_RATE));
            }
        }

        // Deterministic sort: by registry item ID first, then component stability.
        // We keep variant stacks with different components since fuel values may
        // differ per component.
        Comparator<EngineFuelRecipe> byItemIdThenComponents = Comparator.comparing(
                (EngineFuelRecipe r) -> r.ingredients().get(0).getItem().toString());

        // Component-aware dedup: keep stacks with the same item AND the same
        // components (isSameItemSameComponents). Different components are
        // preserved because they may have different fuel values.
        // The IIngredientManager may already return unique stacks, but we
        // dedup here to be safe — list sizes are small so O(n²) is fine.
        registration.addRecipes(EXTRACTOR_FUEL, dedupSorted(extractorRecipes, byItemIdThenComponents));
        registration.addRecipes(METALIZER_FUEL, dedupSorted(metalizerRecipes, byItemIdThenComponents));
        registration.addRecipes(BIOMASS_FUEL, dedupSorted(biomassRecipes, byItemIdThenComponents));
    }

    /**
     * Component-aware deduplication: two recipes are considered duplicates
     * only if their first ingredient has the same item AND the same components
     * ({@link ItemStack#isSameItemSameComponents}). Sorted by the given comparator.
     */
    private static List<EngineFuelRecipe> dedupSorted(List<EngineFuelRecipe> recipes, Comparator<EngineFuelRecipe> sortKey) {
        if (recipes.isEmpty()) return List.of();

        recipes.sort(sortKey);

        List<EngineFuelRecipe> result = new ArrayList<>();
        result.add(recipes.get(0));

        for (int i = 1; i < recipes.size(); i++) {
            ItemStack current = recipes.get(i).ingredients().get(0);
            ItemStack last = result.get(result.size() - 1).ingredients().get(0);
            // isSameItemSameComponents: same item type AND same NBT/components.
            // This preserves stacks with different components (e.g., different
            // damage values or modifier data) since fuel values may differ.
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

        // Existing machine catalysts
        for (var data : buildCategories()) {
            var type = new RecipeType<FormsCombinedRecipe>(data.id, FormsCombinedRecipe.class);
            registration.addRecipeCatalyst(data.icon, type);
        }
        // Smelter — uses vanilla RecipeType.SMELTING
        registration.addRecipeCatalyst(icon("machine_smelter"), mezz.jei.api.constants.RecipeTypes.SMELTING);

        // Engine catalysts (Solar excluded)
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
