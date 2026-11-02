package com.modularmc.ten.integration.jei;

import com.modularmc.ten.api.recipe.FormsCombinedIngredient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import mezz.jei.api.gui.drawable.IDrawable;

import org.jetbrains.annotations.Nullable;

/**
 * JEI slot overlay for chance percentage and rolls text.
 * <p>
 * Implements {@link IDrawable} so the overlay is drawn by JEI's slot
 * rendering pipeline — after the item stack — avoiding the z-order bug
 * where text drawn in {@code IRecipeCategory.draw()} gets covered by
 * subsequent slot item rendering.
 * <p>
 * 1.21.1 适配：GuiGraphics 替代 GuiGraphicsExtractor。
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
            // Top-left: chance percentage at 75% scale
            drawScaled(guiGraphics, font, chanceText, xOffset, yOffset + CHANCE_TEXT_Y_OFFSET);
        }
        if (rollsText != null) {
            // Bottom-left: rolls text at 75% scale
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