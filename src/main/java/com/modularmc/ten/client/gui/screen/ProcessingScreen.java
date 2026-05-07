package com.modularmc.ten.client.gui.screen;

import com.modularmc.ten.client.gui.CmContainerMachine;
import com.modularmc.ten.client.gui.CmScreenMachine;
import com.modularmc.ten.client.gui.element.ElementBurnLeft;
import com.modularmc.ten.client.gui.element.ElementProgress;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ProcessingScreen extends CmScreenMachine {

    protected ElementBurnLeft energy;
    protected ElementProgress progress;
    protected final int inputCount;
    protected final int outputCount;

    public ProcessingScreen(CmContainerMachine container, Inventory inv, Component title,
                            String path, int texW, int texH,
                            int inputCount, int outputCount) {
        super(container, inv, title, path, texW, texH);
        xSize = 176;
        ySize = 166;
        this.inputCount = inputCount;
        this.outputCount = outputCount;
    }

    @Override
    public void addWidgets() {
        super.addWidgets();
        widgets.add(energy = getDefaultEne());
        addProgress();
    }

    protected void addProgress() {
        widgets.add(progress = new ElementProgress(76, 35, 22, 16, 27, 0, HANDLER));
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (energy != null) {
            energy.setPer(pEnergy());
            energy.setValue(energy(), maxEnergy());
        }
        if (progress != null) {
            progress.setPer(pProgress());
        }
    }
}
