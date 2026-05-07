package com.modularmc.ten.client.gui.element;

import com.modularmc.ten.utils.ComponentHelper;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class ElementButtonSlot extends ElementButton {

    public ElementButtonSlot(int x, int y, int width, int height, int xOff, int yOff, ResourceLocation resourceLocation, ClickAction action) {
        super(x, y, width, height, xOff, yOff, resourceLocation, action);
    }

    @Override
    public void addToolTip(List<Component> tooltips) {
        if (!state) {
            tooltips.add(ComponentHelper.translated(ComponentHelper.RED, "technicalengineering.locked_slot"));
        }
    }
}
