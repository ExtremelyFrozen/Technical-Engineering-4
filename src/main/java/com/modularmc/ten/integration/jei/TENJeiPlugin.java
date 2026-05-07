package com.modularmc.ten.integration.jei;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.recipe.FormsCombinedRecipe;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class TENJeiPlugin implements IModPlugin {

    private static final ResourceLocation UID = TEN.id("jei_plugin");

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return UID;
    }

    private record CategoryDef(String name, ResourceLocation id, ItemStack icon) {}

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
        return item != null ? new ItemStack(item) : new ItemStack(Items.FURNACE);
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper helper = registration.getJeiHelpers().getGuiHelper();
        for (var data : buildCategories()) {
            RecipeType<FormsCombinedRecipe> type = new RecipeType<>(data.id, FormsCombinedRecipe.class);
            registration.addRecipeCategories(new TENJeiCategory(helper, type,
                    Component.translatable("jei.ten." + data.name), data.icon));
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;

        for (var data : buildCategories()) {
            List<FormsCombinedRecipe> recipes = new ArrayList<>();
            var type = new RecipeType<FormsCombinedRecipe>(data.id, FormsCombinedRecipe.class);
            for (var entry : level.getRecipeManager().getRecipes()) {
                if (entry.value() instanceof FormsCombinedRecipe r) {
                    if (r.getType() != null && r.getType().toString().equals(data.id.getPath())) {
                        recipes.add(r);
                    }
                }
            }
            registration.addRecipes(type, recipes);
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (var data : buildCategories()) {
            var type = new RecipeType<FormsCombinedRecipe>(data.id, FormsCombinedRecipe.class);
            registration.addRecipeCatalyst(data.icon, type);
        }
    }
}
