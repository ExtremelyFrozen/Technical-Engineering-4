package com.modularmc.ten.integration.jei;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.recipe.FormsCombinedRecipe;
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
import java.util.List;

@JeiPlugin
public class TENJeiPlugin implements IModPlugin {

    private static final Identifier UID = TEN.id("jei_plugin");

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

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        if (!TEN.Mods.isJEILoaded()) return;

        IGuiHelper helper = registration.getJeiHelpers().getGuiHelper();
        for (var data : buildCategories()) {
            RecipeType<FormsCombinedRecipe> type = new RecipeType<>(data.id, FormsCombinedRecipe.class);
            registration.addRecipeCategories(new TENJeiCategory(helper, data.id, type, data.icon));
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (!TEN.Mods.isJEILoaded()) return;

        var server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null) return;

        var recipeManager = server.getRecipeManager();
        if (recipeManager == null) return;

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

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        if (!TEN.Mods.isJEILoaded()) return;

        for (var data : buildCategories()) {
            var type = new RecipeType<FormsCombinedRecipe>(data.id, FormsCombinedRecipe.class);
            registration.addRecipeCatalyst(data.icon, type);
        }
        // Smelter — uses vanilla RecipeType.SMELTING
        registration.addRecipeCatalyst(icon("machine_smelter"), mezz.jei.api.constants.RecipeTypes.SMELTING);
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
