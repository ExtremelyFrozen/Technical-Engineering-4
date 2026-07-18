package com.modularmc.ten.integration.jei;

import com.modularmc.ten.TENConstants;
import com.modularmc.ten.api.recipe.FormsCombinedRecipe;
import com.modularmc.ten.integration.xei.TENRecipeWidget;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
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
    private final IDrawable icon;
    private final IDrawable inputSlot;
    private final IDrawable fluidSlot;
    private final TENRecipeWidget.Layout layout;

    public TENJeiCategory(IGuiHelper helper, Identifier categoryId, RecipeType<FormsCombinedRecipe> type, ItemStack iconStack) {
        this.recipeType = type;
        this.layout = TENRecipeWidget.layout(categoryId);
        this.title = TENRecipeWidget.titleJei(categoryId);
        this.icon = helper.createDrawableItemStack(iconStack);
        this.inputSlot = helper.getSlotDrawable();
        this.fluidSlot = helper.createDrawable(TENConstants.GUI_HANDLER, 0, 92, 18, 50);
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
    public int getWidth() {
        return layout.width();
    }

    @Override
    public int getHeight() {
        return layout.height();
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, FormsCombinedRecipe recipe, IFocusGroup focuses) {
        for (var slot : layout.slots()) {
            var ingredient = TENRecipeWidget.ingredientFor(recipe, slot);
            if (slot.kind() == TENRecipeWidget.SlotKind.ITEM) {
                var jeiSlot = builder.addSlot(toJeiRole(slot.role()), slot.x() + 2, slot.y())
                        .setBackground(inputSlot, -1, -1);
                if (ingredient != null) {
                    var itemStacks = ingredient.itemStacks();
                    var filteredItemStacks = itemStacks.stream().filter(s -> !s.isEmpty()).toList();
                    if (!filteredItemStacks.isEmpty()) {
                        jeiSlot.addItemStacks(filteredItemStacks);
                    }
                    if (slot.role() == TENRecipeWidget.SlotRole.OUTPUT && ingredient.chance() < 1.0d) {
                        jeiSlot.addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("kenergyengineering.jei_addition_chance", TENRecipeWidget.chancePercent(ingredient.chance()))));
                    }
                    if (slot.role() == TENRecipeWidget.SlotRole.INPUT && ingredient.chance() <= 0) {
                        jeiSlot.addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("kenergyengineering.not_consumed")));
                    }
                }
            } else if (slot.kind() == TENRecipeWidget.SlotKind.FLUID) {
                var jeiSlot = builder.addSlot(toJeiRole(slot.role()), slot.x() + 1, slot.y() + 1)
                        .setBackground(fluidSlot, -1, -1);
                int capacity = ingredient != null ? Math.max(1, TENRecipeWidget.fluidCapacity(ingredient)) : 1;
                jeiSlot.setFluidRenderer(capacity, true, slot.width() - 2, slot.height() - 2);
                if (ingredient != null) {
                    var fluidStacks = ingredient.fluidStacks();
                    var filteredFluidStacks = fluidStacks.stream().filter(s -> !s.isEmpty()).toList();
                    if (!filteredFluidStacks.isEmpty()) {
                        jeiSlot.addIngredients(NeoForgeTypes.FLUID_STACK, filteredFluidStacks);
                    }
                    if (slot.role() == TENRecipeWidget.SlotRole.OUTPUT && ingredient.chance() < 1.0d) {
                        jeiSlot.addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("kenergyengineering.jei_addition_chance", TENRecipeWidget.chancePercent(ingredient.chance()))));
                    }
                    if (slot.role() == TENRecipeWidget.SlotRole.INPUT && ingredient.chance() <= 0) {
                        jeiSlot.addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("kenergyengineering.not_consumed")));
                    }
                }
            } else {
                // unsupported kind: no slot registered
            }
        }
    }

    @Override
    public void draw(FormsCombinedRecipe recipe, IRecipeSlotsView slotsView, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        TENRecipeWidget.drawJei(recipe, graphics, layout);
    }

    private static RecipeIngredientRole toJeiRole(TENRecipeWidget.SlotRole role) {
        return role == TENRecipeWidget.SlotRole.INPUT ? RecipeIngredientRole.INPUT : RecipeIngredientRole.OUTPUT;
    }
}
