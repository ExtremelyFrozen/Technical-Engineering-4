package com.modularmc.ten.client.gui.element;

import com.modularmc.ten.utils.ComponentHelper;
import com.modularmc.ten.utils.RenderHelper;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class ElementButton extends ElementBase {

    public interface ClickAction {

        void click();
    }

    ClickAction action;
    public boolean state;
    Component text;
    boolean noChange;

    public ElementButton(int x, int y, int width, int height, int xOff, int yOff, ResourceLocation resourceLocation, ClickAction action) {
        super(x, y, width, height, xOff, yOff, resourceLocation);
        this.action = action;
    }

    public void setTxt(String... key) {
        text = ComponentHelper.translated(ComponentHelper.GOLD, key);
    }

    public ElementButton withNoChange() {
        noChange = true;
        return this;
    }

    @Override
    public void addToolTip(List<Component> tooltips) {
        if (text != null) tooltips.add(text);
    }

    @Override
    public void onMouseClicked(int mouseX, int mouseY) {
        action.click();
    }

    @Override
    public void draw(GuiGraphics guiGraphics) {
        if (noChange) {
            RenderHelper.render(guiGraphics, x, y, width, height, textureW, textureH, xOff, yOff, resourceLocation);
            return;
        }
        int yOff = this.yOff;
        if (hanging) yOff += state ? height * 3 : height;
        else yOff += state ? height * 2 : 0;
        RenderHelper.render(guiGraphics, x, y, width, height, textureW, textureH, xOff, yOff, resourceLocation);
    }
}
