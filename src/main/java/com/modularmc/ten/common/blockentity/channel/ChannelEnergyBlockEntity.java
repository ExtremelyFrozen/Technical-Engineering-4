package com.modularmc.ten.common.blockentity.channel;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.option.MachineType;
import com.modularmc.ten.common.blockentity.TransferNetworks;
import com.modularmc.ten.common.gui.TENMachineBlockUIFactory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;

public class ChannelEnergyBlockEntity extends AbstractChannelBlockEntity {

    public ChannelEnergyBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setCapacity(kFE(10));
    }

    @Override
    public int inventorySize() {
        return 0;
    }

    @Override
    public int machineType() {
        return MachineType.MACHINE_EFFECT;
    }

    @Override
    public boolean hasFaceCapabilityEnergy(Direction side) {
        return side == null || side == getFacing();
    }

    @Override
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        return buildChannelUI(holder, TEN.id("textures/gui/channel.png"), root -> {
            root.addChild(TENMachineBlockUIFactory.energyGauge(this, 79, 17, 18, 60, 0, 0, true));
            root.addChild(label(58, 82, "Wireless energy relay"));
        });
    }

    @Override
    public void tick() {
        doBaseData();
        if (!signalAllowRun() || energyStorage == null) {
            setActive(false);
            return;
        }

        int moved = 0;
        var input = nextRoundRobin(resolveInputs(), true);
        if (input != null && input.energyStorage != null) {
            moved += TransferNetworks.moveEnergy(input.energyStorage, energyStorage, maxReceiveEnergy, false);
        }
        var output = nextRoundRobin(resolveOutputs(), false);
        if (output != null && output.energyStorage != null) {
            moved += TransferNetworks.moveEnergy(energyStorage, output.energyStorage, maxExtractEnergy, false);
        }
        setActive(moved > 0);
    }
}
