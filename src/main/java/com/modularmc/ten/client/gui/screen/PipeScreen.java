package com.modularmc.ten.client.gui.screen;

import com.modularmc.ten.client.gui.CmContainerMachine;
import com.modularmc.ten.client.gui.CmScreen;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class PipeScreen extends CmScreen<CmContainerMachine> {

    public PipeScreen(CmContainerMachine container, Inventory inv, Component title) {
        super(container, inv, title, "textures/gui/pipe.png", 256, 256);
        xSize = 176;
        ySize = 222;
    }
}
