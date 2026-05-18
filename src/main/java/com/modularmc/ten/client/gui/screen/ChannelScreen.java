package com.modularmc.ten.client.gui.screen;

import com.modularmc.ten.api.option.FaceOption;
import com.modularmc.ten.client.gui.CmContainerMachine;
import com.modularmc.ten.client.gui.CmScreenMachine;
import com.modularmc.ten.client.gui.element.ElementButton;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ChannelScreen extends CmScreenMachine {

    private static final ResourceLocation CHANNEL_GUI = HANDLER;

    public ChannelScreen(CmContainerMachine container, Inventory inv, Component title) {
        super(container, inv, title, "textures/gui/channel.png", 256, 256);
        xSize = 176;
        ySize = 166;
    }

    @Override
    protected void setSides() {
        if (front == null || back == null || left == null || right == null || up == null || down == null) {
            return;
        }
        // Channel mode display — fall back to machine face data
        front.mode = getChannelMode();
        back.mode = FaceOption.NONE;
        left.mode = FaceOption.NONE;
        right.mode = FaceOption.NONE;
        up.mode = FaceOption.NONE;
        down.mode = FaceOption.NONE;
    }

    private int getChannelMode() {
        if (container.machine == null) return FaceOption.OFF;
        return modeNow == 0 ? container.machine.energyFaceData[0] : container.machine.itemFaceData[0];
    }

    @Override
    public void addWidgets() {
        super.addWidgets();
        widgets.add(new ElementButton(107, 5, 12, 12, 84, 166, CHANNEL_GUI, () -> {}));
        widgets.add(new ElementButton(107, 64, 12, 12, 96, 166, CHANNEL_GUI, () -> {}));
    }

    @Override
    public void containerTick() {
        super.containerTick();
    }
}
