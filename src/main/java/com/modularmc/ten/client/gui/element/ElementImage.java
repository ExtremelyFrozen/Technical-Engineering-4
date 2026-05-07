package com.modularmc.ten.client.gui.element;

import com.modularmc.ten.utils.RenderHelper;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class ElementImage extends ElementBase {

    public ElementImage(int x, int y, int width, int height, int xOff, int yOff, ResourceLocation resourceLocation) {
        super(x, y, width, height, xOff, yOff, resourceLocation);
    }

    @Override
    public void draw(GuiGraphics guiGraphics) {
        RenderHelper.bindTexture(resourceLocation);
        RenderHelper.render(guiGraphics, x, y, width, height, textureW, textureH, xOff, yOff, resourceLocation);
    }
}
