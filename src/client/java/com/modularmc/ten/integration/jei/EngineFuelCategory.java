package com.modularmc.ten.integration.jei;

import com.modularmc.ten.TENConstants;
import com.modularmc.ten.common.blockentity.EngineFuelRecipe;

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
 * JEI category for engine fuel recipes (Extractor, Metalizer, Biomass).
 * <p>
 * 1.21.1 适配：GuiGraphics 替代 GuiGraphicsExtractor，ResourceLocation 替代 Identifier。
 */
public class EngineFuelCategory implements IRecipeCategory<EngineFuelRecipe> {

    private static final int WIDTH = 150;
    private static final int HEIGHT = 50;

    private static final int INPUT_SLOT_X = 10;
    private static final int INPUT_SLOT_Y = 16;
    private static final int FUEL_GAUGE_X = 37;
    private static final int FUEL_GAUGE_Y = 19;
    private static final int FUEL_GAUGE_W = 13;
    private static final int FUEL_GAUGE_H = 13;
    private static final int FUEL_GAUGE_U = 0;
    private static final int FUEL_GAUGE_V = 0;

    private static final int TEXT_X = 58;
    private static final int TEXT_LINE1_Y = 8;
    private static final int TEXT_LINE2_Y = 21;
    private static final int TEXT_LINE3_Y = 34;

    private final RecipeType<EngineFuelRecipe> recipeType;
    private final Component title;
    private final IDrawable icon;
    private final IDrawable background;
    private final IDrawable inputSlot;
    private final IDrawable fuelGaugeBg;
    private final IDrawable fuelGaugeFill;

    public EngineFuelCategory(IGuiHelper helper, ResourceLocation categoryId,
                              RecipeType<EngineFuelRecipe> type, ItemStack iconStack,
                              Component title) {
        this.recipeType = type;
        this.title = title;
        this.icon = helper.createDrawableItemStack(iconStack);
        this.background = helper.createDrawable(TENConstants.JEI_HANDLER_MODULAR, 0, 0, WIDTH, HEIGHT);
        this.inputSlot = helper.drawableBuilder(TENConstants.ITEM_SLOT_SMALL, 0, 0, 18, 18).setTextureSize(18, 18).build();
        this.fuelGaugeBg = helper.drawableBuilder(TENConstants.FUEL_GAUGE_BG, FUEL_GAUGE_U, FUEL_GAUGE_V, FUEL_GAUGE_W, FUEL_GAUGE_H).setTextureSize(FUEL_GAUGE_W, FUEL_GAUGE_H).build();
        this.fuelGaugeFill = helper.drawableBuilder(TENConstants.FUEL_GAUGE_FILL, FUEL_GAUGE_U, FUEL_GAUGE_V, FUEL_GAUGE_W, FUEL_GAUGE_H).setTextureSize(FUEL_GAUGE_W, FUEL_GAUGE_H).build();
    }

    @Override
    public RecipeType<EngineFuelRecipe> getRecipeType() { return recipeType; }

    @Override
    public Component getTitle() { return title; }

    @Override
    public int getWidth() { return WIDTH; }

    @Override
    public int getHeight() { return HEIGHT; }

    @Override
    public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, EngineFuelRecipe recipe, IFocusGroup focuses) {
        var slot = builder.addSlot(RecipeIngredientRole.INPUT, INPUT_SLOT_X, INPUT_SLOT_Y)
                .setBackground(inputSlot, -1, -1)
                .addItemStacks(recipe.ingredients());

        slot.addRichTooltipCallback((view, tooltip) -> {
            tooltip.add(Component.translatable("kenergyengineering.jei.base_output", recipe.fuelBudget()));
            tooltip.add(Component.translatable("kenergyengineering.jei.base_rate", recipe.baseRate()));
            tooltip.add(Component.translatable("kenergyengineering.jei.duration_ticks", recipe.durationTicks()));
            tooltip.add(Component.translatable("kenergyengineering.jei.total_energy", recipe.baseGeneratedEnergy()));
        });
    }

    @Override
    public void draw(EngineFuelRecipe recipe, IRecipeSlotsView slotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        background.draw(graphics);

        fuelGaugeBg.draw(graphics, FUEL_GAUGE_X, FUEL_GAUGE_Y);
        fuelGaugeFill.draw(graphics, FUEL_GAUGE_X, FUEL_GAUGE_Y);

        var font = Minecraft.getInstance().font;
        int textColor = 0xFF404040;

        graphics.drawString(font, Component.translatable("kenergyengineering.jei.base_rate_short", recipe.baseRate()),
                TEXT_X, TEXT_LINE1_Y, textColor, false);
        graphics.drawString(font, Component.translatable("kenergyengineering.jei.duration_short", recipe.durationTicks()),
                TEXT_X, TEXT_LINE2_Y, textColor, false);
        graphics.drawString(font, Component.translatable("kenergyengineering.jei.total_short", recipe.baseGeneratedEnergy()),
                TEXT_X, TEXT_LINE3_Y, textColor, false);
    }
}