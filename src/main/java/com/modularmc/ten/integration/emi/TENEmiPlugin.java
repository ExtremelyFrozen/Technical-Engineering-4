package com.modularmc.ten.integration.emi;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.recipe.FormsCombinedRecipe;
import com.modularmc.ten.common.data.TENBlocks;
import com.modularmc.ten.common.data.TENRecipeTypes;
import com.modularmc.ten.integration.xei.TENRecipeWidget;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;

import java.util.function.Supplier;

@EmiEntrypoint
public class TENEmiPlugin implements EmiPlugin {

    private record CategoryDef(ResourceLocation id,
                               DeferredHolder<RecipeType<?>, RecipeType<FormsCombinedRecipe>> recipeType,
                               Supplier<ItemStack> iconStack) {}

    private static final int RECIPE_LOG_SAMPLE_LIMIT = 5;

    private static final java.util.List<CategoryDef> CATEGORIES = java.util.List.of(
            new CategoryDef(TEN.id("pulverizer"), TENRecipeTypes.PULVERIZER_T, TENBlocks.MACHINE_PULVERIZER::asStack),
            new CategoryDef(TEN.id("compressor"), TENRecipeTypes.COMPRESSOR_T, TENBlocks.MACHINE_COMPRESSOR::asStack),
            new CategoryDef(TEN.id("refiner"), TENRecipeTypes.REFINER_T, TENBlocks.MACHINE_REFINER::asStack),
            new CategoryDef(TEN.id("induction_furnace"), TENRecipeTypes.INDUCTION_FURNACE_T, TENBlocks.MACHINE_INDUCTION_FURNACE::asStack),
            new CategoryDef(TEN.id("psionicant"), TENRecipeTypes.PSIONICANT_T, TENBlocks.MACHINE_PSIONICANT::asStack));

    private static final class TENEmiCategory extends EmiRecipeCategory {

        private final ResourceLocation id;

        private TENEmiCategory(ResourceLocation id, EmiStack icon) {
            super(id, icon);
            this.id = id;
        }

        @Override
        public Component getName() {
            return TENRecipeWidget.title(id);
        }
    }

    private static EmiStack icon(ItemStack stack) {
        return stack.isEmpty() ? EmiStack.EMPTY : EmiStack.of(stack);
    }

    @Override
    public void register(EmiRegistry registry) {
        var allRecipeEntries = registry.getRecipeManager().getRecipes();
        var tenRecipeEntries = allRecipeEntries.stream()
                .filter(entry -> TEN.MOD_ID.equals(entry.id().getNamespace()))
                .toList();

        TEN.LOGGER.info(
                "[EMI] RecipeManager currently has {} total recipes, {} from {}",
                allRecipeEntries.size(),
                tenRecipeEntries.size(),
                TEN.MOD_ID);
        for (var def : CATEGORIES) {
            int pathMatches = (int) tenRecipeEntries.stream()
                    .filter(entry -> entry.id().getPath().contains("/" + def.id().getPath() + "/"))
                    .count();
            TEN.LOGGER.info(
                    "[EMI] Recipe path scan for {} matched {} entries under namespace {}",
                    def.id(),
                    pathMatches,
                    TEN.MOD_ID);
        }

        TEN.LOGGER.info("[EMI] Registering TEN EMI plugin with {} categories", CATEGORIES.size());
        for (var def : CATEGORIES) {
            var iconStack = def.iconStack().get();
            var icon = icon(iconStack);
            var category = new TENEmiCategory(def.id(), icon);
            var recipeType = def.recipeType().get();
            var recipes = registry.getRecipeManager().getAllRecipesFor(recipeType);

            TEN.LOGGER.info(
                    "[EMI] Category {} -> recipeType={}, iconEmpty={}, iconItem={}, recipeCount={}",
                    def.id(),
                    recipeType,
                    icon == EmiStack.EMPTY,
                    iconStack,
                    recipes.size());

            registry.addCategory(category);
            registry.addWorkstation(category, icon);
            TEN.LOGGER.info("[EMI] Category {} registered with workstation {}", def.id(), iconStack);

            int index = 0;
            for (var entry : recipes) {
                var recipe = entry.value();
                if (index < RECIPE_LOG_SAMPLE_LIMIT) {
                    TEN.LOGGER.info(
                            "[EMI]   Recipe {} -> id={}, inputs(items={}, fluids={}), outputs(items={}, fluids={}), time={}",
                            def.id(),
                            entry.id(),
                            recipe.allInputItems().size(),
                            recipe.allInputFluids().size(),
                            recipe.allOutputItems().size(),
                            recipe.allOutputFluids().size(),
                            recipe.time());
                }
                registry.addRecipe(new TENEmiRecipe(def.id(), category, recipe));
                index++;
            }
            TEN.LOGGER.info("[EMI] Category {} finished registering {} recipes", def.id(), index);
        }
    }
}
