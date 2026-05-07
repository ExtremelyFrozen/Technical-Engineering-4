package com.modularmc.ten.client.gui.element;

import com.modularmc.ten.api.option.FaceOption;
import com.modularmc.ten.utils.ComponentHelper;
import com.modularmc.ten.utils.RenderHelper;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class ElementButtonTransf extends ElementButton {

    public int mode;

    public ElementButtonTransf(int x, int y, int width, int height, int xOff, int yOff, ResourceLocation resourceLocation, ClickAction action) {
        super(x, y, width, height, xOff, yOff, resourceLocation, action);
    }

    Component text;

    public void setTxt(String... key) {
        text = ComponentHelper.translated(ComponentHelper.GOLD, key);
    }

    @Override
    public void addToolTip(List<Component> tooltips) {
        if (text != null) {
            tooltips.add(text);
            tooltips.add(ComponentHelper.translated("technicalengineering.info." + FaceOption.toStr(mode)));
        }
    }

    @Override
    public void draw(GuiGraphics guiGraphics) {
        int yOff;
        if (mode == FaceOption.IN) yOff = this.yOff + height;
        else if (mode == FaceOption.OUT) yOff = this.yOff + height * 2;
        else if (mode == FaceOption.BE_IN) yOff = this.yOff + height * 3;
        else if (mode == FaceOption.BE_OUT) yOff = this.yOff + height * 4;
        else if (mode == FaceOption.BOTH) yOff = this.yOff + height * 5;
        else yOff = this.yOff;
        RenderHelper.render(guiGraphics, x, y, width, height, textureW, textureH, xOff, yOff, resourceLocation);
    }
}
