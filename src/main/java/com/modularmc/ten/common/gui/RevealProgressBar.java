package com.modularmc.ten.common.gui;

import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.data.FillDirection;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar;
import com.lowdragmc.lowdraglib2.math.Position;
import com.lowdragmc.lowdraglib2.math.Size;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.AlignContent;
import net.minecraft.util.Mth;

/**
 * A ProgressBar variant that uses UV clipping (reveal) instead of flex-only
 * shrinking, preventing texture distortion at partial fills.
 * <p>
 * Root cause: LDLib2 {@link ProgressBar#updateProgressBarStyle} changes the bar element's
 * {@code widthPercent / heightPercent} based on the normalized progress. The full UV
 * SpriteTexture is then stretched/compressed into the reduced bar area, distorting
 * the texture pattern.
 * <p>
 * Fix: instead of relying on flex-only shrinkage (which compresses a full-UV sprite
 * into a smaller area), this override:
 * <ol>
 *   <li>Sets the bar layout proportionally to progress (same bounds as super would)</li>
 *   <li>Modifies the source rectangle ({@link SpriteTexture#spritePosition} /
 *       {@link SpriteTexture#spriteSize}) to sample only the corresponding fraction
 *       of the source image</li>
 * </ol>
 * Because both the element bounds and source UV shrink by the same factor, the drawn
 * texture maintains correct pixel density at every fill level — no distortion.
 * <p>
 * Supported directions:
 * <ul>
 *   <li>{@link FillDirection#LEFT_TO_RIGHT} — bar widthPercent = progress × 100;
 *       sample left progress fraction of source width</li>
 *   <li>{@link FillDirection#RIGHT_TO_LEFT} — bar widthPercent = progress × 100;
 *       sample right progress fraction</li>
 *   <li>{@link FillDirection#UP_TO_DOWN} — bar heightPercent = progress × 100;
 *       sample top progress fraction of source height</li>
 *   <li>{@link FillDirection#DOWN_TO_UP} — bar heightPercent = progress × 100;
 *       sample bottom progress fraction</li>
 * </ol>
 * <p>
 * Usage in {@code TENMachineBlockUIFactory}:
 * <pre>{@code
 * var filled = SpriteTexture.of(HANDLER).setSprite(xOff, yOff + height, width, height);
 * var progress = absolute(new RevealProgressBar(filled), x, y, width, height);
 * }</pre>
 *
 * @see TENMachineBlockUIFactory#verticalGauge
 * @see TENMachineBlockUIFactory#progressGauge
 */
public class RevealProgressBar extends ProgressBar {

    private final SpriteTexture filledSprite;
    private final int origSpriteX;
    private final int origSpriteY;
    private final int origSpriteWidth;
    private final int origSpriteHeight;
    private boolean initialized;

    /**
     * @param filledTexture a {@link SpriteTexture} already configured with its
     *                      original sprite position and size via {@link SpriteTexture#setSprite}
     */
    public RevealProgressBar(SpriteTexture filledTexture) {
        super();
        this.filledSprite = filledTexture;
        this.origSpriteX = filledTexture.spritePosition.getX();
        this.origSpriteY = filledTexture.spritePosition.getY();
        this.origSpriteWidth = filledTexture.spriteSize.getWidth();
        this.origSpriteHeight = filledTexture.spriteSize.getHeight();
        // Attach the filled sprite to the bar element (replaces default texture)
        this.bar(bar -> bar.style(style -> style.backgroundTexture(filledTexture)));
        this.initialized = true;
        // Re-apply styling so the current value reflects UV clipping immediately
        this.onProgressBarStyleChanged();
    }

    /**
     * Overrides the flex-percentage approach from {@link ProgressBar#updateProgressBarStyle}.
     * <p>
     * Unlike super (which only changes {@code widthPercent / heightPercent} and lets the
     * full-UV sprite stretch into the smaller area), this method does <b>both</b>:
     * <ol>
     *   <li>Sets bar flex layout to the correct proportion (same as super)</li>
     *   <li>Clips the {@link SpriteTexture} source UV to match the same proportion</li>
     * </ol>
     * Because element bounds and source UV shrink by the same factor, texture pixel
     * density is preserved at every fill level.
     * <p>
     * When constructed, super invokes this method before our fields are set.
     * The {@link #initialized} guard falls back to super in that case.
     */
    @Override
    protected void updateProgressBarStyle(float normalized) {
        // Guard: super constructor calls this before our fields are initialized
        if (!initialized || filledSprite == null) {
            super.updateProgressBarStyle(normalized);
            return;
        }

        // NaN/Infinity safety: if normalized is not finite, treat as 0%
        // Then clamp to [0, 1] — prevent negative or overflow UV coordinates
        float progress = Float.isFinite(normalized) ? Mth.clamp(normalized, 0.0f, 1.0f) : 0.0f;

        FillDirection dir = getProgressBarStyle().fillDirection();

        // ---- Step 1a: Mirror parent ProgressBar barBackground direction/alignment ----
        // Parent bytecode sets flexDirection + alignItems on barBackground per FillDirection:
        //   L2R → COLUMN + FLEX_START,  R2L → COLUMN + FLEX_END
        //   U2D → ROW   + FLEX_START,   D2U → ROW   + FLEX_END
        barBackground.layout(bbLayout -> {
            switch (dir) {
                case LEFT_TO_RIGHT -> {
                    bbLayout.flexDirection(FlexDirection.COLUMN);
                    bbLayout.alignItems(AlignItems.FLEX_START);
                }
                case RIGHT_TO_LEFT -> {
                    bbLayout.flexDirection(FlexDirection.COLUMN);
                    bbLayout.alignItems(AlignItems.FLEX_END);
                }
                case UP_TO_DOWN -> {
                    bbLayout.flexDirection(FlexDirection.ROW);
                    bbLayout.alignItems(AlignItems.FLEX_START);
                }
                case DOWN_TO_UP -> {
                    bbLayout.flexDirection(FlexDirection.ROW);
                    bbLayout.alignItems(AlignItems.FLEX_END);
                }
            }
        });

        // ---- Step 1b: Compute integer visible pixels (single quantization point) ----
        // Both bar layout and UV use these same integer values — no sub-pixel mismatch.
        boolean isHorizontal = dir == FillDirection.LEFT_TO_RIGHT || dir == FillDirection.RIGHT_TO_LEFT;
        boolean isVertical = dir == FillDirection.UP_TO_DOWN || dir == FillDirection.DOWN_TO_UP;

        final int visibleWidth;
        final int visibleHeight;
        if (isHorizontal) {
            visibleWidth = Math.max(Math.round(origSpriteWidth * progress), 0);
            visibleHeight = origSpriteHeight;
        } else {
            visibleWidth = origSpriteWidth;
            visibleHeight = isVertical ? Math.max(Math.round(origSpriteHeight * progress), 0) : origSpriteHeight;
        }

        // ---- Step 1c: Set bar layout to absolute pixel dimensions ----
        // Uses the same quantized pixel values as UV — no sub-pixel re-layout each tick.
        bar.layout(layout -> {
            switch (dir) {
                case LEFT_TO_RIGHT -> {
                    layout.flexDirection(FlexDirection.ROW);
                    layout.justifyContent(AlignContent.FLEX_START);
                }
                case RIGHT_TO_LEFT -> {
                    layout.flexDirection(FlexDirection.ROW);
                    layout.justifyContent(AlignContent.FLEX_END);
                }
                case UP_TO_DOWN -> {
                    layout.flexDirection(FlexDirection.COLUMN);
                    layout.justifyContent(AlignContent.FLEX_START);
                }
                case DOWN_TO_UP -> {
                    layout.flexDirection(FlexDirection.COLUMN);
                    layout.justifyContent(AlignContent.FLEX_END);
                }
            }
            layout.width(visibleWidth);
            layout.height(visibleHeight);
        });

        // ---- Step 2: Clip source UV to match the same quantized pixel count ----
        int newX = origSpriteX;
        int newY = origSpriteY;
        int newW = origSpriteWidth;
        int newH = origSpriteHeight;

        if (dir == FillDirection.LEFT_TO_RIGHT) {
            newW = visibleWidth;
        } else if (dir == FillDirection.RIGHT_TO_LEFT) {
            newX = visibleWidth > 0 ? origSpriteX + (origSpriteWidth - visibleWidth) : origSpriteX;
            newW = visibleWidth;
        } else if (dir == FillDirection.UP_TO_DOWN) {
            newH = visibleHeight;
        } else if (dir == FillDirection.DOWN_TO_UP) {
            newY = visibleHeight > 0 ? origSpriteY + (origSpriteHeight - visibleHeight) : origSpriteY;
            newH = visibleHeight;
        } // ALWAYS_FULL and others: keep full sprite

        filledSprite.setSprite(newX, newY, newW, newH);
    }
}
