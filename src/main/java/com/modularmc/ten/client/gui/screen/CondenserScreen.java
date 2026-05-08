package com.modularmc.ten.client.gui.screen;

import com.modularmc.ten.client.gui.CmContainerMachine;
import com.modularmc.ten.client.gui.CmScreenMachine;
import com.modularmc.ten.client.gui.element.ElementBurnLeft;
import com.modularmc.ten.client.gui.element.ElementFluid;
import com.modularmc.ten.client.gui.element.ElementProgress;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class CondenserScreen extends CmScreenMachine {

    ElementBurnLeft energy;
    ElementProgress progress;
    ElementFluid fluido;

    public CondenserScreen(CmContainerMachine container, Inventory inv, Component title) {
        super(container, inv, title, "textures/gui/matter_condenser.png", 256, 256);
        xSize = 176;
        ySize = 166;
    }

    @Override
    public void addWidgets() {
        super.addWidgets();
        widgets.add(energy = getDefaultEne());
        widgets.add(progress = new ElementProgress(48, 57, 80, 5, 97, 0, HANDLER, true));
        widgets.add(fluido = new ElementFluid(143, 17, 18, 50, 0, 92, HANDLER, 0, true));
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (energy != null) {
            energy.setPer(pEnergy());
            energy.setValue(energy(), maxEnergy());
        }
        if (progress != null) progress.setPer(pProgress());
        if (fluido != null) fluido.update(container);
    }
}
