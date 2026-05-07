package com.modularmc.ten.integration.jei;

import com.modularmc.ten.api.recipe.FormsCombinedRecipe;
import com.modularmc.ten.integration.xei.TENRecipeWidget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

public class TENJeiCategory implements IRecipeCategory<FormsCombinedRecipe> {

    private final RecipeType<FormsCombinedRecipe> recipeType;
    private final Component title;
    private final IDrawable background;
    private final IDrawable icon;

    public TENJeiCategory(IGuiHelper helper, RecipeType<FormsCombinedRecipe> type, Component title, ItemStack iconStack) {
        this.recipeType = type;
        this.title = title;
        this.background = helper.createDrawable(TENRecipeWidget.JEI_BG, 0, 0, 160, 80);
        this.icon = helper.createDrawableItemStack(iconStack);
    }

    @Override
    public RecipeType<FormsCombinedRecipe> getRecipeType() {
        return recipeType;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, FormsCombinedRecipe recipe, IFocusGroup focuses) {
        var inputs = recipe.allInputItems();
        var outputs = recipe.allOutputItems();

        // Inputs - left side, stacked vertically
        int x = 10, y = 10;
        for (int i = 0; i < inputs.size(); i++) {
            var ing = inputs.get(i);
            if (!ing.symbolItem().isEmpty()) {
                builder.addSlot(RecipeIngredientRole.INPUT, x, y + i * 22)
                        .addItemStack(ing.symbolItem());
            }
        }

        // Outputs - right side
        x = 90;
        for (int i = 0; i < outputs.size(); i++) {
            var ing = outputs.get(i);
            if (!ing.symbolItem().isEmpty()) {
                var slot = builder.addSlot(RecipeIngredientRole.OUTPUT, x + (i % 2) * 22, y + (i / 2) * 22);
                slot.addItemStack(ing.symbolItem());
                if (ing.chance() < 1.0f) {
                    slot.addTooltipCallback((view, tooltip) -> {
                        tooltip.add(Component.literal(String.format("%.0f%% chance", ing.chance() * 100)));
                    });
                }
            }
        }
    }

    @Override
    public void draw(FormsCombinedRecipe recipe, IRecipeSlotsView slotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        // Draw arrow
        int arrowX = 65, arrowY = 30;
        double time = (System.currentTimeMillis() / 1000.0) % 2.0 / 2.0;
        TENRecipeWidget.drawProgressArrow(graphics, arrowX, arrowY, time);

        // Draw energy
        TENRecipeWidget.drawEnergy(graphics, 5, 60, 15);

        // Draw time
        TENRecipeWidget.drawTime(graphics, 80, 65, recipe.time());
    }
}
