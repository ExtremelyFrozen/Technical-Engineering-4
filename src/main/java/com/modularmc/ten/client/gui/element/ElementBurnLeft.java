package com.modularmc.ten.client.gui.element;

import com.modularmc.ten.utils.ComponentHelper;
import com.modularmc.ten.utils.DisplayHelper;
import com.modularmc.ten.utils.RenderHelper;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class ElementBurnLeft extends ElementBase {

    double percent;
    boolean displayValue;
    int value, maxValue;

    public ElementBurnLeft(int x, int y, int w, int h, int xOff, int yOff, ResourceLocation rl) {
        super(x, y, w, h, xOff, yOff, rl);
    }

    public ElementBurnLeft(int x, int y, int w, int h, int xOff, int yOff, ResourceLocation rl, boolean dv) {
        super(x, y, w, h, xOff, yOff, rl);
        displayValue = dv;
    }

    @Override
    public void draw(GuiGraphics guiGraphics) {
        int h = (int) (height * (1 - percent));
        RenderHelper.render(guiGraphics, x, y, width, height, textureW, textureH, xOff, yOff, resourceLocation);
        RenderHelper.render(guiGraphics, x, y + h, width, height - h, textureW, textureH, xOff, yOff + height + (int) (height * (1 - percent)), resourceLocation);
    }

    @Override
    public void addToolTip(List<Component> tooltips) {
        if (!displayValue) {
            tooltips.add(ComponentHelper.make((int) (percent * 100) + "%"));
        } else {
            tooltips.add(DisplayHelper.join(value, maxValue));
        }
    }

    public void setValue(int v, int mv) {
        value = v;
        maxValue = mv;
    }

    public void setPer(double per) {
        percent = per;
    }

    public double getPer() {
        return percent;
    }
}
