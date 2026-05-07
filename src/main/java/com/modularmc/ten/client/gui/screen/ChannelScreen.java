package com.modularmc.ten.client.gui.screen;

import com.modularmc.ten.client.gui.CmContainerMachine;
import com.modularmc.ten.client.gui.CmScreenMachine;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ChannelScreen extends CmScreenMachine {

    public ChannelScreen(CmContainerMachine container, Inventory inv, Component title) {
        super(container, inv, title, "textures/gui/channel.png", 256, 256);
        xSize = 176 + getExtras();
        ySize = 222;
    }

    @Override
    public void addWidgets() {
        super.addWidgets();
        // Channel entries are handled by ElementBarControl
    }
}
