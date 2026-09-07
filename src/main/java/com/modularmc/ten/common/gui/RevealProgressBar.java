package com.modularmc.ten.common.gui;

import net.minecraft.util.Mth;

import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.data.FillDirection;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar;
import com.modularmc.ten.TEN;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;

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
 * <li>Sets the bar layout proportionally to progress (same bounds as super would)</li>
 * <li>Modifies the source rectangle ({@link SpriteTexture#spritePosition} /
 * {@link SpriteTexture#spriteSize}) to sample only the corresponding fraction
 * of the source image</li>
 * </ol>
 * Because both the element bounds and source UV shrink by the same factor, the drawn
 * texture maintains correct pixel density at every fill level — no distortion.
 * <p>
 * Supported directions:
 * <ul>
 * <li>{@link FillDirection#LEFT_TO_RIGHT} — bar widthPercent = progress × 100;
 * sample left progress fraction of source width</li>
 * <li>{@link FillDirection#RIGHT_TO_LEFT} — bar widthPercent = progress × 100;
 * sample right progress fraction</li>
 * <li>{@link FillDirection#UP_TO_DOWN} — bar heightPercent = progress × 100;
 * sample top progress fraction of source height</li>
 * <li>{@link FillDirection#DOWN_TO_UP} — bar heightPercent = progress × 100;
 * sample bottom progress fraction</li>
 * </ol>
 * <p>
 * Usage in {@code TENMachineBlockUIFactory}:
 *
 * <pre>{@code
 *
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
     * <li>Sets bar flex layout to the correct proportion (same as super)</li>
     * <li>Clips the {@link SpriteTexture} source UV to match the same proportion</li>
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

        // 完成重置检测（插值模式）：目标值低于当前显示值 → 直接跳到目标。
        // 背景：interpolate(true) 下配方完成时进度 100%→0 会被插值反向播放（倒放被误读为缩放），
        // 且反向动画尾帧（lastValue≤step）无法完全检测，残留细条闪现——因此下行一律瞬移，
        // 仅保留上行填充的插值平滑（与 26.1.2 参照的瞬时语义一致）。能量条连续下降
        // 本身就是逐 tick 平滑变化的，瞬移后视觉无差异。已知取舍：大幅下行
        // （燃料切换/能量骤降）从多 tick 下滑动画变为单帧瞬移——设计上接受，
        // 若未来需要区分场景，需引入下行速率阈值判据。
        var style = getProgressBarStyle();
        if (style != null && style.interpolate()) {
            Float targetValue = getValue();
            if (targetValue != null) {
                float target = Mth.clamp(getNormalizedValue(targetValue), 0.0f, 1.0f);
                if (target < progress - 1e-4f) {
                    progress = target;
                }
            }
        }

        FillDirection dir = getProgressBarStyle().fillDirection();

        // ---- Step 1a: Mirror parent ProgressBar barBackground direction/alignment ----
        // Parent bytecode sets flexDirection + alignItems on barBackground per FillDirection:
        // L2R → COLUMN + FLEX_START, R2L → COLUMN + FLEX_END
        // U2D → ROW + FLEX_START, D2U → ROW + FLEX_END
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
        // 注：与 26.1.2 工作参照（另一 MC 版本分支的对照工程，该行为已长期验证正常）保持一致，不做 setDisplay 切换 —— progress=0 时 width(0)
        // 已使 bar 不渲染（零尺寸元素无绘制），display:none→FLEX 的恢复转换在
        // LDLIB2 2.2.37 运行时存在不恢复风险（曾导致进度条永久隐藏）。
        // bar（填充）与 UV 采样同用整数像素值 —— 无亚像素错位
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
