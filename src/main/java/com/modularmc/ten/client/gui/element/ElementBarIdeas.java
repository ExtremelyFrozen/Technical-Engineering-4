package com.modularmc.ten.client.gui.element;

import com.modularmc.ten.utils.ComponentHelper;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class ElementBarIdeas extends ElementImage {

    List<Component> list = new ArrayList<>();

    public ElementBarIdeas(int xr, int y, int w, int h, int xOff, int yOff, ResourceLocation rl, String key) {
        super(xr, y, w, h, xOff, yOff, rl);
        list.add(ComponentHelper.translated(ComponentHelper.GOLD, "technicalengineering.info.bar_ideas"));
        for (int i = 0; true; i++) {
            String k = "technicalengineering.info." + ComponentHelper.exceptMachineOrGiveCell(key) + "." + i;
            Component ttc = ComponentHelper.translated(k);
            if (ttc.getString().equals(k)) break;
            list.add(ttc);
        }
    }

    @Override
    public void onMouseClicked(int mouseX, int mouseY) {}

    @Override
    public void addToolTip(List<Component> tooltips) {
        tooltips.addAll(list);
    }
}
