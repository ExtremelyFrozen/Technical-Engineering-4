package com.modularmc.ten.integration.jei;

import com.modularmc.ten.api.recipe.FormsCombinedIngredient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import mezz.jei.api.gui.drawable.IDrawable;

import org.jetbrains.annotations.Nullable;

/**
 * JEI 槽位覆盖层，显示 chance 百分比与 rolls 文本。
 * <p>
 * 实现 {@link IDrawable}，由 JEI 槽位渲染管线在物品之后绘制，保证文字不会被后续物品渲染遮盖。
 */
public class TENJeiSlotOverlay implements IDrawable {

    public static final int X_OFFSET = -1;
    public static final int Y_OFFSET = -1;
    public static final int CHANCE_TEXT_Y_OFFSET = -1;
    public static final int ROLLS_TEXT_Y_OFFSET = 11;
    public static final float TEXT_SCALE = 0.75f;

    private final String chanceText;
    private final String rollsText;

    public TENJeiSlotOverlay(FormsCombinedIngredient ingredient) {
        this.chanceText = ingredient.chanceOverlayText();
        this.rollsText = ingredient.rollsOverlayText();
    }

    public static boolean shouldHaveOverlay(
            boolean isItemSlot, boolean isOutput, @Nullable FormsCombinedIngredient ingredient) {
        if (!isItemSlot) return false;
        if (!isOutput) return false;
        if (ingredient == null) return false;
        return ingredient.chanceOverlayText() != null || ingredient.rollsOverlayText() != null;
    }

    @Nullable
    public static TENJeiSlotOverlay create(FormsCombinedIngredient ingredient) {
        String chance = ingredient.chanceOverlayText();
        String rolls = ingredient.rollsOverlayText();
        if (chance == null && rolls == null) return null;
        return new TENJeiSlotOverlay(ingredient);
    }

    @Override
    public void draw(GuiGraphics guiGraphics, int xOffset, int yOffset) {
        Font font = Minecraft.getInstance().font;
        if (chanceText != null) {
            drawScaled(guiGraphics, font, chanceText, xOffset, yOffset + CHANCE_TEXT_Y_OFFSET);
        }
        if (rollsText != null) {
            drawScaled(guiGraphics, font, rollsText, xOffset, yOffset + ROLLS_TEXT_Y_OFFSET);
        }
    }

    private static void drawScaled(GuiGraphics guiGraphics, Font font, String text, int x, int y) {
        var pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale(TEXT_SCALE, TEXT_SCALE, 1.0F);
        guiGraphics.drawString(font, text, 0, 0, 0xFFFFFFFF, true);
        pose.popPose();
    }

    @Override
    public int getWidth() { return 18; }

    @Override
    public int getHeight() { return 18; }
}