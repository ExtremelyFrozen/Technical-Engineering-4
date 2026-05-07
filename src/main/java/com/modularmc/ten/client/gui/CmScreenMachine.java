package com.modularmc.ten.client.gui;

import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;
import com.modularmc.ten.api.option.RedstoneMode;
import com.modularmc.ten.client.ClientData;
import com.modularmc.ten.client.gui.element.*;
import com.modularmc.ten.common.network.packet.RedstoneModePacket;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public class CmScreenMachine extends CmScreen<CmContainerMachine> {

    protected ElementBarEnergy barEnergy;
    protected ElementBarControl barControl;
    protected ElementButton rsHigh, rsLow, rsOff;
    protected int modeNow;

    public CmScreenMachine(CmContainerMachine container, Inventory inv, Component title, String path, int texW, int texH) {
        super(container, inv, title, path, texW, texH);
    }

    @Override
    public void addWidgets() {
        super.addWidgets();
        int w = 26;

        // Energy bar
        widgets.add(barEnergy = new ElementBarEnergy(-w - 1, 0, w, w, 132, 211, HANDLER));
        // Control bar
        widgets.add(barControl = new ElementBarControl(-w - 1, (w + 1) * 3, w, w, 152, 40, HANDLER));

        // Redstone buttons
        rsHigh = new ElementButton(-w - 1, (w + 1) * 2, w, w, 186, 184, HANDLER, this::cycleRedstone).withNoChange();
        rsLow = new ElementButton(-w - 1, (w + 1) * 2, w, w, 186, 157, HANDLER, this::cycleRedstone).withNoChange();
        rsOff = new ElementButton(-w - 1, (w + 1) * 2, w, w, 186, 211, HANDLER, this::cycleRedstone).withNoChange();
        rsHigh.setTxt("technicalengineering.info.bar_redstone", "technicalengineering.info.high");
        rsLow.setTxt("technicalengineering.info.bar_redstone", "technicalengineering.info.low");
        rsOff.setTxt("technicalengineering.info.bar_redstone", "technicalengineering.info.off");
        widgets.add(rsHigh);
        widgets.add(rsLow);
        widgets.add(rsOff);
    }

    private void cycleRedstone() {
        int m = container.data.get(CmMachineBlockEntity.RED_MODE);
        m++;
        if (m > RedstoneMode.HIGH) m = RedstoneMode.OFF;
        ClientData.redstone.put(container.pos, m);
        PacketDistributor.sendToServer(new RedstoneModePacket(m, container.pos));
    }

    @Override
    public void containerTick() {
        super.containerTick();
        var data = container.data;

        rsHigh.setVisible(false);
        rsLow.setVisible(false);
        rsOff.setVisible(false);
        switch (data.get(CmMachineBlockEntity.RED_MODE)) {
            case RedstoneMode.HIGH -> rsHigh.setVisible(true);
            case RedstoneMode.LOW -> rsLow.setVisible(true);
            default -> rsOff.setVisible(true);
        }

        barEnergy.update(
                data.get(CmMachineBlockEntity.EFF_AUC),
                data.get(CmMachineBlockEntity.EFF),
                data.get(CmMachineBlockEntity.E_REC),
                data.get(CmMachineBlockEntity.E_EXT),
                data.get(CmMachineBlockEntity.I_REC),
                data.get(CmMachineBlockEntity.I_EXT),
                data.get(CmMachineBlockEntity.F_REC),
                data.get(CmMachineBlockEntity.F_EXT));

        barControl.show = true; // TEMP
    }

    public ElementBurnLeft getDefaultEne() {
        return new ElementBurnLeft(9, 18, 14, 46, 0, 0, HANDLER, true);
    }
}
