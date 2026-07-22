package com.modularmc.ten.integration.jei;

import com.modularmc.ten.api.recipe.FormsCombinedIngredient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

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
 * Designed as a pure data class with static factory/decision methods
 * for testability without JEI runtime.
 */
public class TENJeiSlotOverlay implements IDrawable {

    /**
     * Horizontal offset for chance/rolls overlay text relative to slot origin.
     * Must remain {@code -1} so text aligns with the slot interior.
     */
    public static final int X_OFFSET = -1;

    /**
     * Vertical offset for chance/rolls overlay text relative to slot origin.
     * Current value: {@code -1} (text shifted 2px upward relative to old +1).
     */
    public static final int Y_OFFSET = -1;

    /**
     * Vertical offset of chance percentage text relative to the overlay origin
     * ({@code Y_OFFSET}). {@code -1} places chance at absolute slot.y - 2,
     * which is 1px higher than the previous baseline (relative y = 0 → slot.y - 1).
     */
    public static final int CHANCE_TEXT_Y_OFFSET = -1;

    /**
     * Vertical offset of rolls text relative to the overlay origin
     * ({@code Y_OFFSET}). {@code 11} places rolls at absolute slot.y + 10,
     * which is 1px lower than the previous baseline (relative y = 10 → slot.y + 9).
     * <p>
     * Spacing between chance and rolls: 12px ({@code 11 - (-1)}).
     */
    public static final int ROLLS_TEXT_Y_OFFSET = 11;

    /**
     * Scale factor applied to both chance and rolls text.
     * {@code 0.75f} reduces rendered text to 75% of default size,
     * ensuring "100%" and "R999" fit within the 18×18 slot width.
     */
    public static final float TEXT_SCALE = 0.75f;

    private final String chanceText;
    private final String rollsText;

    /**
     * @param ingredient the ingredient to display overlay text for
     */
    public TENJeiSlotOverlay(FormsCombinedIngredient ingredient) {
        this.chanceText = ingredient.chanceOverlayText();
        this.rollsText = ingredient.rollsOverlayText();
    }

    /**
     * Pure-function testable decision: should an ITEM+OUTPUT slot get an overlay?
     *
     * @param isItemSlot true if the slot is ITEM kind (not FLUID)
     * @param isOutput   true if the slot is OUTPUT role (not INPUT)
     * @param ingredient the ingredient for this slot, may be null
     * @return true if an overlay should be attached
     */
    public static boolean shouldHaveOverlay(
            boolean isItemSlot, boolean isOutput, @Nullable FormsCombinedIngredient ingredient) {
        if (!isItemSlot) return false;
        if (!isOutput) return false;
        if (ingredient == null) return false;
        return ingredient.chanceOverlayText() != null || ingredient.rollsOverlayText() != null;
    }

    /**
     * Factory: creates a {@link TENJeiSlotOverlay} for the given ingredient,
     * or returns {@code null} if no overlay text is needed.
     */
    @Nullable
    public static TENJeiSlotOverlay create(FormsCombinedIngredient ingredient) {
        String chance = ingredient.chanceOverlayText();
        String rolls = ingredient.rollsOverlayText();
        if (chance == null && rolls == null) return null;
        return new TENJeiSlotOverlay(ingredient);
    }

    @Override
    public void draw(GuiGraphicsExtractor guiGraphics, int xOffset, int yOffset) {
        Font font = Minecraft.getInstance().font;
        if (chanceText != null) {
            // Top-left: chance percentage (e.g., "40%") at 75% scale
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate(xOffset, yOffset + CHANCE_TEXT_Y_OFFSET);
            guiGraphics.pose().scale(TEXT_SCALE, TEXT_SCALE);
            guiGraphics.text(font, chanceText, 0, 0, 0xFFFFFFFF, true);
            guiGraphics.pose().popMatrix();
        }
        if (rollsText != null) {
            // Bottom-left: rolls (e.g., "R9") at 75% scale
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate(xOffset, yOffset + ROLLS_TEXT_Y_OFFSET);
            guiGraphics.pose().scale(TEXT_SCALE, TEXT_SCALE);
            guiGraphics.text(font, rollsText, 0, 0, 0xFFFFFFFF, true);
            guiGraphics.pose().popMatrix();
        }
    }

    @Override
    public int getWidth() {
        return 18;
    }

    @Override
    public int getHeight() {
        return 18;
    }
}
