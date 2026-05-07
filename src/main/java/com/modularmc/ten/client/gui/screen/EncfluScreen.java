package com.modularmc.ten.client.gui.screen;

import com.modularmc.ten.client.gui.CmContainerMachine;
import com.modularmc.ten.client.gui.CmScreenMachine;
import com.modularmc.ten.client.gui.element.ElementBurnLeft;
import com.modularmc.ten.client.gui.element.ElementProgress;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class EncfluScreen extends CmScreenMachine {

    ElementBurnLeft energy;
    ElementProgress progress;

    public EncfluScreen(CmContainerMachine container, Inventory inv, Component title) {
        super(container, inv, title, "textures/gui/enchantment_flusher.png", 256, 256);
        xSize = 176 + getExtras();
        ySize = 222;
    }

    @Override
    public void addWidgets() {
        super.addWidgets();
        widgets.add(energy = getDefaultEne());
        widgets.add(progress = new ElementProgress(74, 37, 22, 6, 0, 0, HANDLER));
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (energy != null) {
            energy.setPer(pEnergy());
            energy.setValue(energy(), maxEnergy());
        }
        if (progress != null) progress.setPer(pProgress());
    }
}
