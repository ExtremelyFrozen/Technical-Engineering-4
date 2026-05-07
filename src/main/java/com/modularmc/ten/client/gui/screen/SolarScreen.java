package com.modularmc.ten.client.gui.screen;

import com.modularmc.ten.client.gui.CmContainerMachine;
import com.modularmc.ten.client.gui.CmScreenMachine;
import com.modularmc.ten.client.gui.element.ElementBurnLeft;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class SolarScreen extends CmScreenMachine {

    ElementBurnLeft energy;
    ElementBurnLeft sun;

    public SolarScreen(CmContainerMachine container, Inventory inv, Component title) {
        super(container, inv, title, "textures/gui/engine_solar.png", 256, 256);
        xSize = 176 + getExtras();
        ySize = 222;
    }

    @Override
    public void addWidgets() {
        super.addWidgets();
        widgets.add(energy = new ElementBurnLeft(80, 12, 14, 46, 0, 0, HANDLER, true));
        widgets.add(sun = new ElementBurnLeft(80, 62, 14, 14, 0, 0, HANDLER, true));
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (energy != null) {
            energy.setPer(pEnergy());
            energy.setValue(energy(), maxEnergy());
        }
        if (sun != null) {
            sun.setPer(pFuel());
        }
    }
}
