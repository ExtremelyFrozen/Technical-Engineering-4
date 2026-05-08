package com.modularmc.ten.client.gui.screen;

import com.modularmc.ten.client.gui.CmContainerMachine;
import com.modularmc.ten.client.gui.CmScreenMachine;
import com.modularmc.ten.client.gui.element.ElementBurnLeft;
import com.modularmc.ten.client.gui.element.ElementFluid;
import com.modularmc.ten.client.gui.element.ElementProgress;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class RefinerScreen extends CmScreenMachine {

    ElementBurnLeft energy;
    ElementBurnLeft left;
    ElementProgress progress;
    ElementFluid fluidi;
    ElementFluid fluido;

    public RefinerScreen(CmContainerMachine container, Inventory inv, Component title) {
        super(container, inv, title, "textures/gui/one_to_one_fluid.png", 256, 256);
        xSize = 176;
        ySize = 166;
    }

    @Override
    public void addWidgets() {
        super.addWidgets();
        widgets.add(energy = getDefaultEne());
        widgets.add(left = new ElementBurnLeft(60, 56, 13, 13, 14, 0, HANDLER));
        widgets.add(progress = new ElementProgress(84, 35, 22, 16, 27, 159, HANDLER, false));
        widgets.add(fluidi = new ElementFluid(37, 17, 18, 50, 0, 92, HANDLER, 0, true));
        widgets.add(fluido = new ElementFluid(143, 17, 18, 50, 0, 92, HANDLER, 1, true));
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (energy != null) {
            energy.setPer(pEnergy());
            energy.setValue(energy(), maxEnergy());
        }
        if (fluidi != null) fluidi.update(container);
        if (fluido != null) fluido.update(container);
        if (left != null) left.setPer(pEnergy());
        if (progress != null) progress.setPer(pProgress());
    }
}
