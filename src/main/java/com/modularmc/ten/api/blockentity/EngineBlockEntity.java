package com.modularmc.ten.api.blockentity;

import com.modularmc.ten.api.option.FaceOption;
import com.modularmc.ten.api.option.MachineType;
import com.modularmc.ten.common.gui.TENMachineBlockUIFactory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;

public abstract class EngineBlockEntity extends CmMachineBlockEntity {

    public EngineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public int machineType() {
        return MachineType.GENERATOR;
    }

    @Override
    public int initialFaceModeEnergy() {
        return FaceOption.OUT;
    }

    @Override
    public boolean hasFaceCapabilityFluid(Direction side) {
        return false;
    }

    @Override
    public boolean hasFaceCapabilityEnergy(Direction side) {
        return side == null || side == Direction.UP;
    }

    @Override
    public boolean hasFaceCapabilityItem(Direction side) {
        return side != Direction.UP;
    }

    @Override
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        return buildMachineUI(holder, TENMachineBlockUIFactory.backgroundFor(machineType()), root -> {
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 0, 43, 36));
        }, root -> {
            root.addChild(TENMachineBlockUIFactory.energyGaugeModular(this, 117, 22, true));
            root.addChild(TENMachineBlockUIFactory.fuelGaugeModular(this, 81, 39, false));
        });
    }

    @Override
    public void tick() {
        doBaseData();

        if (!signalAllowRun() || !energyAllowRun()) return;

        ItemStack fuelStack = ItemStack.EMPTY;
        if (itemHandler != null && itemHandler.getSlots() > 0) {
            fuelStack = itemHandler.getStackInSlot(0);
        }

        if (fuel > 0) {
            energyStorage.receiveEnergy(getActualEfficiency(), false);
            fuel = Math.max(fuel - efficientIn, 0);
            setActive(true);
        } else {
            int fuelVal = matchFuel(fuelStack, true);
            if (fuelVal > 0) {
                matchFuel(fuelStack, false);
                fuel = fuelVal;
                maxFuel = fuelVal;
                setActive(true);
            } else {
                setActive(false);
            }
        }
    }

    public abstract int matchFuel(ItemStack stack, boolean simulate);
}
