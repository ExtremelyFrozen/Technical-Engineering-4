package com.modularmc.ten.client.gui.element;

import com.modularmc.ten.utils.ComponentHelper;
import com.modularmc.ten.utils.RenderHelper;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class ElementProgress extends ElementBase {

    double percent;
    boolean displayValue;

    public ElementProgress(int x, int y, int w, int h, int xOff, int yOff, ResourceLocation rl) {
        super(x, y, w, h, xOff, yOff, rl);
    }

    public ElementProgress(int x, int y, int w, int h, int xOff, int yOff, ResourceLocation rl, boolean dv) {
        super(x, y, w, h, xOff, yOff, rl);
        displayValue = dv;
    }

    @Override
    public void draw(GuiGraphics guiGraphics) {
        RenderHelper.render(guiGraphics, x, y, width, height, textureW, textureH, xOff, yOff, resourceLocation);
        RenderHelper.render(guiGraphics, x, y, (int) (percent * width), height, textureW, textureH, xOff, yOff + height, resourceLocation);
    }

    @Override
    public void addToolTip(List<Component> tooltips) {
        if (displayValue) {
            tooltips.add(ComponentHelper.make((int) (percent * 100) + "%"));
        }
    }

    public void setPer(double per) {
        percent = per;
    }

    public double getPer() {
        return percent;
    }
}
