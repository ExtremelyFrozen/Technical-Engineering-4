package com.modularmc.ten.client.gui.element;

import com.modularmc.ten.utils.ComponentHelper;
import com.modularmc.ten.utils.RenderHelper;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class ElementBar extends ElementBase {

    public boolean state;
    static final int BAR_SIZE = 21;
    static final int BAR_MAX = 90;
    int barH = BAR_SIZE;
    int wait;
    int bw = BAR_SIZE;
    Component text;

    public ElementBar(int xr, int y, int xOff, int yOff, ResourceLocation rl) {
        super(xr, y, BAR_SIZE, BAR_SIZE, xOff, yOff, rl);
    }

    public ElementBar(int xr, int y, int h, int xOff, int yOff, ResourceLocation rl) {
        super(xr, y, BAR_SIZE, h, xOff, yOff, rl);
        barH = h;
    }

    public void setTxt(String key) {
        text = ComponentHelper.translated(ComponentHelper.GOLD, key);
    }

    @Override
    public void onMouseClicked(int mouseX, int mouseY) {
        if (wait <= 0) {
            state = !state;
            wait = 4;
        }
    }

    @Override
    public void draw(GuiGraphics guiGraphics) {
        RenderHelper.render(guiGraphics, x - bw, y, bw, barH, textureW, textureH, xOff, yOff, resourceLocation);
        wait--;
        if (state) {
            if (bw < BAR_MAX) bw = Math.min(BAR_MAX, bw + 6);
            else drawAdd(guiGraphics);
        } else {
            if (bw > BAR_SIZE) bw = Math.max(BAR_SIZE, bw - 6);
            else bw = BAR_SIZE;
        }
    }

    @Override
    public void addToolTip(List<Component> tooltips) {
        if (text != null) tooltips.add(text);
    }

    @Override
    public boolean checkInstr(int mouseX, int mouseY) {
        return (mouseX >= x - bw && mouseY >= y && mouseX <= x - bw + BAR_SIZE && mouseY <= y + height) ||
                (mouseX >= x - BAR_SIZE && mouseY >= y && mouseX <= x && mouseY <= y + height);
    }

    public boolean isOpen() {
        return bw >= BAR_MAX;
    }

    public void drawAdd(GuiGraphics guiGraphics) {}
}
