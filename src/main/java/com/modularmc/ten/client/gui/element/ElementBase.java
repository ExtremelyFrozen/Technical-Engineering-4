package com.modularmc.ten.client.gui.element;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class ElementBase {

    protected int ix, iy, x, y, textureW = 256, textureH = 256;
    protected int xOff, yOff, width, height;
    protected boolean visible = true;
    public boolean hanging;
    protected ResourceLocation resourceLocation;

    public ElementBase(int x, int y, int width, int height, int xOff, int yOff, ResourceLocation resourceLocation) {
        this.x = this.ix = x;
        this.y = this.iy = y;
        this.width = width;
        this.height = height;
        this.xOff = xOff;
        this.yOff = yOff;
        this.resourceLocation = resourceLocation;
    }

    public void updateLocWhenFrameResize(int i, int j) {
        x = ix + i;
        y = iy + j;
    }

    public void draw(GuiGraphics guiGraphics) {}

    public void update() {}

    public void addToolTip(List<Component> tooltips) {}

    public void onMouseClicked(int mouseX, int mouseY) {}

    public void hangingEvent(boolean hang, int mouseX, int mouseY) {
        hanging = hang && checkInstr(mouseX, mouseY);
    }

    public boolean checkInstr(int mouseX, int mouseY) {
        return mouseX >= x && mouseY >= y && mouseX <= x + width && mouseY <= y + height;
    }

    public void setVisible(boolean v) {
        this.visible = v;
    }

    public boolean isVisible() {
        return visible;
    }
}
