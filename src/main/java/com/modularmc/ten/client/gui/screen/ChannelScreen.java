package com.modularmc.ten.client.gui.screen;

import com.modularmc.ten.api.option.FaceOption;
import com.modularmc.ten.client.ClientData;
import com.modularmc.ten.client.gui.CmContainerMachine;
import com.modularmc.ten.client.gui.CmScreenMachine;
import com.modularmc.ten.client.gui.element.ElementBase;
import com.modularmc.ten.client.gui.element.ElementButton;
import com.modularmc.ten.utils.ComponentHelper;
import com.modularmc.ten.utils.DisplayHelper;
import com.modularmc.ten.utils.RenderHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public class ChannelScreen extends CmScreenMachine {

    private static final ResourceLocation CHANNEL_GUI = HANDLER;

    private final List<ChannelEntry> entries = new ArrayList<>();
    private int cursorFrom;

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
        front.mode = modeNow == 0 ? ClientData.channelModeInput.getOrDefault(container.pos, FaceOption.OFF) : ClientData.channelModeOutput.getOrDefault(container.pos, FaceOption.OFF);
        back.mode = FaceOption.NONE;
        left.mode = FaceOption.NONE;
        right.mode = FaceOption.NONE;
        up.mode = FaceOption.NONE;
        down.mode = FaceOption.NONE;
    }

    @Override
    public void addWidgets() {
        super.addWidgets();
        rebuildEntries();
        widgets.add(new ElementButton(107, 5, 12, 12, 84, 166, CHANNEL_GUI, () -> {
            cursorFrom = Math.max(0, cursorFrom - 1);
            updateEntries();
        }));
        widgets.add(new ElementButton(107, 64, 12, 12, 96, 166, CHANNEL_GUI, () -> {
            if (entries.size() <= 5) {
                return;
            }
            cursorFrom = Math.min(entries.size() - 5, cursorFrom + 1);
            updateEntries();
        }));
        updateEntries();
    }

    @Override
    public void containerTick() {
        super.containerTick();
        rebuildEntries();
        updateEntries();
    }

    private void rebuildEntries() {
        List<BlockPos> inputs = ClientData.channelInputs.getOrDefault(container.pos, List.of());
        List<BlockPos> outputs = ClientData.channelOutputs.getOrDefault(container.pos, List.of());
        int expected = 0;
        for (BlockPos pos : inputs) {
            if (pos != null) expected++;
        }
        for (BlockPos pos : outputs) {
            if (pos != null) expected++;
        }
        if (entries.size() == expected) {
            syncEntryData(inputs, outputs);
            return;
        }

        widgets.removeAll(entries);
        entries.clear();
        int index = 0;
        for (BlockPos pos : inputs) {
            if (pos == null) continue;
            entries.add(new ChannelEntry(53, 7 + index * 14, 48, 13, 0, 166, CHANNEL_GUI).set(true, pos, index));
            index++;
        }
        for (BlockPos pos : outputs) {
            if (pos == null) continue;
            entries.add(new ChannelEntry(53, 7 + index * 14, 48, 13, 0, 179, CHANNEL_GUI).set(false, pos, index));
            index++;
        }
        widgets.addAll(entries);
    }

    private void syncEntryData(List<BlockPos> inputs, List<BlockPos> outputs) {
        int index = 0;
        for (BlockPos pos : inputs) {
            if (pos == null) continue;
            entries.get(index).set(true, pos, index);
            index++;
        }
        for (BlockPos pos : outputs) {
            if (pos == null) continue;
            entries.get(index).set(false, pos, index);
            index++;
        }
    }

    private void updateEntries() {
        for (ChannelEntry entry : entries) {
            entry.setVisible(false);
        }
        for (int i = 0; i < 5; i++) {
            int index = i + cursorFrom;
            if (index >= entries.size()) {
                break;
            }
            ChannelEntry entry = entries.get(index);
            entry.setVisible(true);
            entry.locate(53, 7 + i * 14);
            entry.updateLocWhenFrameResize(getGuiLeft(), getGuiTop());
        }
    }

    private static class ChannelEntry extends ElementBase {

        private boolean isInput;
        private BlockPos pos;
        private int index;

        private ChannelEntry(int x, int y, int width, int height, int xOff, int yOff, ResourceLocation resourceLocation) {
            super(x, y, width, height, xOff, yOff, resourceLocation);
        }

        private ChannelEntry set(boolean input, BlockPos blockPos, int entryIndex) {
            this.isInput = input;
            this.pos = blockPos;
            this.index = entryIndex;
            return this;
        }

        @Override
        public void draw(GuiGraphics guiGraphics) {
            RenderHelper.render(guiGraphics, x, y, width, height, textureW, textureH, xOff, yOff, resourceLocation);
            RenderHelper.renderString(
                    guiGraphics,
                    x + 6,
                    y + 3,
                    0xFFFFFF,
                    ComponentHelper.translated("kenergyengineering.channel")
                            .append(ComponentHelper.make("#", String.valueOf(index)))
                            .withStyle(isInput ? ChatFormatting.RED : ChatFormatting.GREEN));
        }

        @Override
        public void addToolTip(List<Component> tooltips) {
            if (pos == null) {
                return;
            }
            tooltips.add(ComponentHelper.translated("kenergyengineering.channel.pos").append(ComponentHelper.make(DisplayHelper.toString(pos))));
            tooltips.add(ComponentHelper.translated(isInput ? "kenergyengineering.channel.in" : "kenergyengineering.channel.out"));
        }

        private void locate(int x, int y) {
            this.ix = x;
            this.iy = y;
        }
    }
}
