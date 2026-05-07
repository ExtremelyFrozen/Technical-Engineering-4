package com.modularmc.ten.integration.jei;

import com.modularmc.ten.api.recipe.FormsCombinedRecipe;
import com.modularmc.ten.integration.xei.TENRecipeWidget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

public class TENJeiCategory implements IRecipeCategory<FormsCombinedRecipe> {

    private final RecipeType<FormsCombinedRecipe> recipeType;
    private final Component title;
    private final IDrawable background;
    private final IDrawable icon;
    private final TENRecipeWidget.Layout layout;

    public TENJeiCategory(IGuiHelper helper, ResourceLocation categoryId, RecipeType<FormsCombinedRecipe> type, ItemStack iconStack) {
        this.recipeType = type;
        this.layout = TENRecipeWidget.layout(categoryId);
        this.title = TENRecipeWidget.title(categoryId);
        this.background = helper.createDrawable(
                layout.background(),
                layout.u(),
                layout.v(),
                layout.width(),
                layout.height());
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
        for (var slot : layout.slots()) {
            var ingredient = TENRecipeWidget.ingredientFor(recipe, slot);
            if (ingredient == null) {
                continue;
            }
            if (slot.kind() == TENRecipeWidget.SlotKind.ITEM) {
                var itemStacks = ingredient.itemStacks();
                if (itemStacks.isEmpty() || itemStacks.getFirst().isEmpty()) {
                    continue;
                }
                var jeiSlot = builder.addSlot(toJeiRole(slot.role()), slot.x() + 1, slot.y() + 1)
                        .addItemStacks(itemStacks);
                if (slot.role() == TENRecipeWidget.SlotRole.OUTPUT && ingredient.chance() < 1.0d) {
                    jeiSlot.addTooltipCallback((view, tooltip) -> tooltip.add(Component.literal(TENRecipeWidget.formatChance(ingredient.chance()))));
                }
            } else {
                var fluidStacks = ingredient.fluidStacks();
                if (fluidStacks.isEmpty() || fluidStacks.getFirst().isEmpty()) {
                    continue;
                }
                var jeiSlot = builder.addSlot(toJeiRole(slot.role()), slot.x() + 1, slot.y() + 1)
                        .addIngredients(NeoForgeTypes.FLUID_STACK, fluidStacks)
                        .setFluidRenderer(Math.max(1, TENRecipeWidget.fluidCapacity(ingredient)), true, slot.width() - 2, slot.height() - 2);
                if (slot.role() == TENRecipeWidget.SlotRole.OUTPUT && ingredient.chance() < 1.0d) {
                    jeiSlot.addTooltipCallback((view, tooltip) -> tooltip.add(Component.literal(TENRecipeWidget.formatChance(ingredient.chance()))));
                }
            }
        }
    }

    @Override
    public void draw(FormsCombinedRecipe recipe, IRecipeSlotsView slotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        TENRecipeWidget.drawJei(recipe, graphics, layout);
    }

    private static RecipeIngredientRole toJeiRole(TENRecipeWidget.SlotRole role) {
        return role == TENRecipeWidget.SlotRole.INPUT ? RecipeIngredientRole.INPUT : RecipeIngredientRole.OUTPUT;
    }
}
