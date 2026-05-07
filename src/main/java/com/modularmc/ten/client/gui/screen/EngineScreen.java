package com.modularmc.ten.client.gui.screen;

import com.modularmc.ten.client.gui.CmContainerMachine;
import com.modularmc.ten.client.gui.CmScreenMachine;
import com.modularmc.ten.client.gui.element.ElementBurnLeft;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class EngineScreen extends CmScreenMachine {

    ElementBurnLeft energy;
    ElementBurnLeft fuel;

    public EngineScreen(CmContainerMachine container, Inventory inv, Component title) {
        super(container, inv, title, "textures/gui/engine.png", 256, 256);
        xSize = 176 + getExtras();
        ySize = 222;
    }

    @Override
    public void addWidgets() {
        super.addWidgets();
        widgets.add(energy = new ElementBurnLeft(117, 22, 14, 46, 0, 0, HANDLER, true));
        widgets.add(fuel = new ElementBurnLeft(78, 26, 14, 14, 0, 0, HANDLER, true));
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (energy != null) {
            energy.setPer(pEnergy());
            energy.setValue(energy(), maxEnergy());
        }
        if (fuel != null) {
            fuel.setPer(pFuel());
        }
    }
}
