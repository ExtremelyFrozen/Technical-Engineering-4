package com.modularmc.ten.common.blockentity.channel;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.capability.MachineFluidTank;
import com.modularmc.ten.api.option.IngredientType;
import com.modularmc.ten.api.option.MachineType;
import com.modularmc.ten.common.blockentity.TransferNetworks;
import com.modularmc.ten.common.gui.TENMachineBlockUIFactory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;

public class ChannelFluidBlockEntity extends AbstractChannelBlockEntity {

    public ChannelFluidBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        tanks.add(new MachineFluidTank(2000));
        tanks.add(new MachineFluidTank(2000));
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
    public IngredientType tankType(int tank) {
        return IngredientType.BOTH;
    }

    @Override
    public boolean valid(int slot, FluidStack stack) {
        return true;
    }

    @Override
    public boolean hasFaceCapabilityFluid(Direction side) {
        return side == null || side == getFacing();
    }

    @Override
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        return buildChannelUI(holder, TEN.id("textures/gui/channel.png"), root -> {
        }, root -> {
            root.addChild(TENMachineBlockUIFactory.fluidGauge(this, 7, 17, 18, 50, 0, true));
            root.addChild(TENMachineBlockUIFactory.fluidGauge(this, 25, 17, 18, 50, 1, true));
        });
    }

    @Override
    public void tick() {
        doBaseData();
        var ownHandler = getFluidHandler(null);
        if (!signalAllowRun() || ownHandler == null) {
            setActive(false);
            return;
        }

        int moved = 0;
        var input = nextRoundRobin(resolveInputs(), true);
        if (input != null) {
            var inputHandler = input.getFluidHandler(null);
            if (inputHandler != null) {
                moved += TransferNetworks.moveFluid(inputHandler, ownHandler, maxReceiveFluid, false);
            }
        }
        var output = nextRoundRobin(resolveOutputs(), false);
        if (output != null) {
            var outputHandler = output.getFluidHandler(null);
            if (outputHandler != null) {
                moved += TransferNetworks.moveFluid(ownHandler, outputHandler, maxExtractFluid, false);
            }
        }
        setActive(moved > 0);
    }
}
