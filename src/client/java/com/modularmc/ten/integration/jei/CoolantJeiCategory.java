package com.modularmc.ten.integration.jei;

import com.modularmc.ten.TENConstants;
import com.modularmc.ten.api.option.CoolantRecipe;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

/**
 * 冷却器的冷却剂分类（形状同 {@link EngineFuelCategory}）：一件冷却剂 + 周期/推进量/次数。
 * <p>
 * 条目由 {@code Coolants} 的标签查询在运行时合成，冷却剂本身没有 RecipeType。
 */
public class CoolantJeiCategory implements IRecipeCategory<CoolantRecipe> {

    private static final int WIDTH = 150;
    private static final int HEIGHT = 50;

    private static final int INPUT_SLOT_X = 10;
    private static final int INPUT_SLOT_Y = 16;
    private static final int TEXT_X = 36;
    private static final int TEXT_LINE1_Y = 8;
    private static final int TEXT_LINE2_Y = 21;
    private static final int TEXT_LINE3_Y = 34;

    private final RecipeType<CoolantRecipe> recipeType;
    private final Component title;
    private final IDrawable icon;
    private final IDrawable background;
    private final IDrawable inputSlot;

    public CoolantJeiCategory(IGuiHelper helper, ResourceLocation categoryId,
                              RecipeType<CoolantRecipe> type, ItemStack iconStack, Component title) {
        this.recipeType = type;
        this.title = title;
        this.icon = helper.createDrawableItemStack(iconStack);
        this.background = helper.createDrawable(TENConstants.JEI_RECIPE_BG, 0, 0, WIDTH, HEIGHT);
        this.inputSlot = helper.drawableBuilder(TENConstants.ITEM_SLOT_SMALL, 0, 0, 18, 18).setTextureSize(18, 18).build();
    }

    @Override
    public RecipeType<CoolantRecipe> getRecipeType() {
        return recipeType;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CoolantRecipe recipe, IFocusGroup focuses) {
        var slot = builder.addSlot(RecipeIngredientRole.INPUT, INPUT_SLOT_X, INPUT_SLOT_Y)
                .setBackground(inputSlot, -1, -1)
                .addItemStacks(recipe.ingredients());

        slot.addRichTooltipCallback((view, tooltip) -> {
            tooltip.add(Component.translatable("kenergyengineering.jei.coolant_interval", recipe.intervalTicks()));
            tooltip.add(Component.translatable("kenergyengineering.jei.coolant_reduction", recipe.reductionTicks()));
            tooltip.add(Component.translatable("kenergyengineering.jei.coolant_uses", recipe.uses()));
            tooltip.add(Component.translatable("kenergyengineering.jei.coolant_total", recipe.totalReductionTicks()));
            tooltip.add(Component.translatable("kenergyengineering.jei.coolant_lifetime", recipe.lifetimeTicks()));
        });
    }

    @Override
    public void draw(CoolantRecipe recipe, IRecipeSlotsView slotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        background.draw(graphics);

        var font = Minecraft.getInstance().font;
        int textColor = 0xFF404040;

        graphics.drawString(font, Component.translatable("kenergyengineering.jei.coolant_interval_short", recipe.intervalTicks()),
                TEXT_X, TEXT_LINE1_Y, textColor, false);
        graphics.drawString(font, Component.translatable("kenergyengineering.jei.coolant_reduction_short", recipe.reductionTicks()),
                TEXT_X, TEXT_LINE2_Y, textColor, false);
        graphics.drawString(font, Component.translatable("kenergyengineering.jei.coolant_uses_short", recipe.uses()),
                TEXT_X, TEXT_LINE3_Y, textColor, false);
    }
}
