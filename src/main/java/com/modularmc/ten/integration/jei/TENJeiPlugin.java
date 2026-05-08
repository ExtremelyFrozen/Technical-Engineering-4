package com.modularmc.ten.integration.jei;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.recipe.FormsCombinedRecipe;
import com.modularmc.ten.client.gui.CmScreenMachine;
import com.modularmc.ten.client.gui.screen.CompressorScreen;
import com.modularmc.ten.client.gui.screen.IndfurScreen;
import com.modularmc.ten.client.gui.screen.PsionicantScreen;
import com.modularmc.ten.client.gui.screen.PulverizerScreen;
import com.modularmc.ten.client.gui.screen.RefinerScreen;
import com.modularmc.ten.common.data.TENRecipeTypes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredHolder;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
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

        var level = Minecraft.getInstance().level;
        if (level == null) return;

        for (var data : buildCategories()) {
            List<FormsCombinedRecipe> recipes = new ArrayList<>();
            var type = new RecipeType<>(data.id, FormsCombinedRecipe.class);
            for (var entry : level.getRecipeManager().getAllRecipesFor(recipeType(data.id).get())) {
                recipes.add(entry.value());
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
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        if (!TEN.Mods.isJEILoaded()) return;

        addArea(registration, PulverizerScreen.class, TEN.id("pulverizer"), 76, 35, 22, 16);
        addArea(registration, CompressorScreen.class, TEN.id("compressor"), 76, 35, 22, 16);
        addArea(registration, RefinerScreen.class, TEN.id("refiner"), 81, 35, 22, 16);
        addArea(registration, IndfurScreen.class, TEN.id("induction_furnace"), 92, 35, 22, 16);
        addArea(registration, PsionicantScreen.class, TEN.id("psionicant"), 76, 35, 22, 16);

        registration.addGuiContainerHandler(CmScreenMachine.class, new IGuiContainerHandler<>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(CmScreenMachine screen) {
                return List.of(new Rect2i(screen.getGuiLeft() - screen.getExtras(), screen.getGuiTop(), screen.getExtras(), screen.ySize));
            }
        });
    }

    private static DeferredHolder<net.minecraft.world.item.crafting.RecipeType<?>, net.minecraft.world.item.crafting.RecipeType<FormsCombinedRecipe>> recipeType(ResourceLocation id) {
        if (id.equals(TEN.id("pulverizer"))) return TENRecipeTypes.PULVERIZER_T;
        if (id.equals(TEN.id("compressor"))) return TENRecipeTypes.COMPRESSOR_T;
        if (id.equals(TEN.id("refiner"))) return TENRecipeTypes.REFINER_T;
        if (id.equals(TEN.id("induction_furnace"))) return TENRecipeTypes.INDUCTION_FURNACE_T;
        if (id.equals(TEN.id("psionicant"))) return TENRecipeTypes.PSIONICANT_T;
        throw new IllegalArgumentException("Unknown TEN recipe type id: " + id);
    }

    private static <T extends net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>> void addArea(
            IGuiHandlerRegistration registration,
            Class<? extends T> screenClass,
            ResourceLocation id,
            int x,
            int y,
            int width,
            int height) {
        registration.addRecipeClickArea(screenClass, x, y, width, height, new RecipeType<>(id, FormsCombinedRecipe.class));
    }
}
