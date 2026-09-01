package com.modularmc.ten.common.gui;

import net.minecraft.util.Mth;

import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.data.FillDirection;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;

/**
 * ProgressBar variant using UV clipping (reveal) to prevent texture distortion
 * at partial fills.
 */
public class RevealProgressBar extends ProgressBar {

    private final SpriteTexture filledSprite;
    private final int origSpriteX, origSpriteY, origSpriteWidth, origSpriteHeight;
    private boolean initialized;

    public RevealProgressBar(SpriteTexture filledTexture) {
        super();
        this.filledSprite = filledTexture;
        this.origSpriteX = filledTexture.spritePosition.getX();
        this.origSpriteY = filledTexture.spritePosition.getY();
        this.origSpriteWidth = filledTexture.spriteSize.getWidth();
        this.origSpriteHeight = filledTexture.spriteSize.getHeight();
        this.bar(bar -> bar.style(style -> style.backgroundTexture(filledTexture)));
        this.initialized = true;
        this.onProgressBarStyleChanged();
    }

    @Override
    protected void updateProgressBarStyle(float normalized) {
        if (!initialized || filledSprite == null) {
            super.updateProgressBarStyle(normalized);
            return;
        }

        float progress = Float.isFinite(normalized) ? Mth.clamp(normalized, 0.0f, 1.0f) : 0.0f;
        FillDirection dir = getProgressBarStyle().fillDirection();

        barBackground.layout(bbLayout -> {
            switch (dir) {
                case LEFT_TO_RIGHT -> { bbLayout.flexDirection(FlexDirection.COLUMN); bbLayout.alignItems(AlignItems.FLEX_START); }
                case RIGHT_TO_LEFT -> { bbLayout.flexDirection(FlexDirection.COLUMN); bbLayout.alignItems(AlignItems.FLEX_END); }
                case UP_TO_DOWN -> { bbLayout.flexDirection(FlexDirection.ROW); bbLayout.alignItems(AlignItems.FLEX_START); }
                case DOWN_TO_UP -> { bbLayout.flexDirection(FlexDirection.ROW); bbLayout.alignItems(AlignItems.FLEX_END); }
            }
        });

        boolean isHorizontal = dir == FillDirection.LEFT_TO_RIGHT || dir == FillDirection.RIGHT_TO_LEFT;
        boolean isVertical = dir == FillDirection.UP_TO_DOWN || dir == FillDirection.DOWN_TO_UP;

        final int visibleWidth = isHorizontal ? Math.max(Math.round(origSpriteWidth * progress), 0) : origSpriteWidth;
        final int visibleHeight = isVertical ? Math.max(Math.round(origSpriteHeight * progress), 0) : origSpriteHeight;

        bar.layout(layout -> {
            switch (dir) {
                case LEFT_TO_RIGHT -> { layout.flexDirection(FlexDirection.ROW); layout.justifyContent(AlignContent.FLEX_START); }
                case RIGHT_TO_LEFT -> { layout.flexDirection(FlexDirection.ROW); layout.justifyContent(AlignContent.FLEX_END); }
                case UP_TO_DOWN -> { layout.flexDirection(FlexDirection.COLUMN); layout.justifyContent(AlignContent.FLEX_START); }
                case DOWN_TO_UP -> { layout.flexDirection(FlexDirection.COLUMN); layout.justifyContent(AlignContent.FLEX_END); }
            }
            layout.width(visibleWidth);
            layout.height(visibleHeight);
        });

        int newX = origSpriteX, newY = origSpriteY, newW = origSpriteWidth, newH = origSpriteHeight;
        if (dir == FillDirection.LEFT_TO_RIGHT) { newW = visibleWidth; }
        else if (dir == FillDirection.RIGHT_TO_LEFT) { newX = visibleWidth > 0 ? origSpriteX + (origSpriteWidth - visibleWidth) : origSpriteX; newW = visibleWidth; }
        else if (dir == FillDirection.UP_TO_DOWN) { newH = visibleHeight; }
        else if (dir == FillDirection.DOWN_TO_UP) { newY = visibleHeight > 0 ? origSpriteY + (origSpriteHeight - visibleHeight) : origSpriteY; newH = visibleHeight; }

        filledSprite.setSprite(newX, newY, newW, newH);
    }
}