package com.modularmc.ten.common.gui;

import net.minecraft.util.Mth;

import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.data.FillDirection;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;

/**
 * 使用 UV 裁切显示填充的进度条。
 * <p>
 * 元素尺寸与 {@link SpriteTexture} 采样区域按同一比例同步缩放，避免部分填充时纹理被拉伸。
 * 水平方向裁切宽度，竖直方向裁切高度；采样端随填充方向对齐。
 */
public class RevealProgressBar extends ProgressBar {

    private final SpriteTexture filledSprite;
    private final int origSpriteX;
    private final int origSpriteY;
    private final int origSpriteWidth;
    private final int origSpriteHeight;
    private boolean initialized;

    /**
     * @param filledTexture 已通过 {@link SpriteTexture#setSprite} 设置原始采样位置与尺寸的填充纹理
     */
    public RevealProgressBar(SpriteTexture filledTexture) {
        super();
        this.filledSprite = filledTexture;
        this.origSpriteX = filledTexture.spritePosition.getX();
        this.origSpriteY = filledTexture.spritePosition.getY();
        this.origSpriteWidth = filledTexture.spriteSize.getWidth();
        this.origSpriteHeight = filledTexture.spriteSize.getHeight();
        // 用填充纹理替换默认纹理
        this.bar(bar -> bar.style(style -> style.backgroundTexture(filledTexture)));
        this.initialized = true;
        // 立即按当前值应用 UV 裁切
        this.onProgressBarStyleChanged();
    }

    /**
     * 同步设置填充元素尺寸与源纹理采样区域，二者使用同一组整数像素值。
     * <p>
     * 父类构造期间会先调用本方法，此时字段尚未初始化；{@link #initialized} 守卫在此期间回退父类实现。
     */
    @Override
    protected void updateProgressBarStyle(float normalized) {
        // 父类构造期间字段尚未初始化
        if (!initialized || filledSprite == null) {
            super.updateProgressBarStyle(normalized);
            return;
        }

        // 非有限值按 0 处理，并钳制到 [0, 1]，避免非法 UV 坐标
        float progress = Float.isFinite(normalized) ? Mth.clamp(normalized, 0.0f, 1.0f) : 0.0f;

        // 插值模式下进度下降时直接落到目标值，避免配方完成后从 100% 反向播放；
        // 仅上行填充保留插值。能量连续下降按 tick 更新，无需额外下行动画。
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

        // 背景容器的主轴与对齐必须匹配填充方向
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

        // 可见像素只在这里取整，布局与 UV 共用同一组整数尺寸
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

        // 布局与 UV 使用同一组整数像素尺寸
        // progress=0 时用 width(0)（竖向为 height(0)）隐藏填充，不切换 setDisplay；
        // 零尺寸元素不绘制，也无需恢复显示状态。
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

        // 按同一整数像素尺寸裁切源纹理 UV
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
        } // ALWAYS_FULL 及其他方向保持完整纹理

        filledSprite.setSprite(newX, newY, newW, newH);
    }
}
