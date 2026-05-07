package com.modularmc.ten.client.gui.element;

import com.modularmc.ten.utils.ComponentHelper;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class ElementBarEnergy extends ElementImage {

    List<Component> tooltipList = new ArrayList<>();
    int auc, mxe, eneI, eneO, itmI, itmO, fluI, fluO;

    public ElementBarEnergy(int xr, int y, int w, int h, int xOff, int yOff, ResourceLocation rl) {
        super(xr, y, w, h, xOff, yOff, rl);
    }

    public void update(int ch, int fet, int ei, int eo, int ii, int io, int fi, int fo) {
        auc = fet;
        mxe = ch;
        eneI = ei;
        eneO = eo;
        itmI = ii;
        itmO = io;
        fluI = fi;
        fluO = fo;
    }

    @Override
    public void onMouseClicked(int mouseX, int mouseY) {}

    @Override
    public void addToolTip(List<Component> tooltips) {
        tooltipList.add(ComponentHelper.translated(ComponentHelper.GOLD, "technicalengineering.info.bar_energy"));
        tooltipList.add(ComponentHelper.translated("technicalengineering.info.bar_energy_fact"));
        tooltipList.add(ComponentHelper.translated(ComponentHelper.RED, Math.abs(mxe) + " FE/t"));
        tooltipList.add(ComponentHelper.translated("technicalengineering.info.bar_energy_max"));
        tooltipList.add(ComponentHelper.translated(ComponentHelper.RED, auc + " FE/t"));
        tooltipList.add(ComponentHelper.translated("technicalengineering.info.bar_energy_in_max"));
        tooltipList.add(ComponentHelper.translated(ComponentHelper.RED, eneI + " FE/t"));
        tooltipList.add(ComponentHelper.translated("technicalengineering.info.bar_energy_out_max"));
        tooltipList.add(ComponentHelper.translated(ComponentHelper.RED, eneO + " FE/t"));
        tooltipList.add(ComponentHelper.translated("technicalengineering.info.bar_item_in_max"));
        tooltipList.add(ComponentHelper.translated(ComponentHelper.RED, itmI + " IS/t"));
        tooltipList.add(ComponentHelper.translated("technicalengineering.info.bar_item_out_max"));
        tooltipList.add(ComponentHelper.translated(ComponentHelper.RED, itmO + " IS/t"));
        tooltipList.add(ComponentHelper.translated("technicalengineering.info.bar_fluid_in_max"));
        tooltipList.add(ComponentHelper.translated(ComponentHelper.RED, fluI + " mB/t"));
        tooltipList.add(ComponentHelper.translated("technicalengineering.info.bar_fluid_out_max"));
        tooltipList.add(ComponentHelper.translated(ComponentHelper.RED, fluO + " mB/t"));
        tooltips.addAll(tooltipList);
        tooltipList.clear();
    }
}
