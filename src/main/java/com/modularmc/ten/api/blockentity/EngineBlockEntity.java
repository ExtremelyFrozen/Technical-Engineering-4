package com.modularmc.ten.api.blockentity;

import com.modularmc.ten.api.option.FaceOption;
import com.modularmc.ten.api.option.MachineType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

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
    public void tick() {
        doBaseData();

        if (!signalAllowRun() || !energyAllowRun()) return;

        ItemStack fuel = ItemStack.EMPTY;
        if (itemHandler != null && itemHandler.getSlots() > 0) {
            fuel = itemHandler.getStackInSlot(0);
        }

        if (data.get(FUEL) > 0) {
            energyStorage.receiveEnergy(getActualEfficiency(), false);
            data.translate(FUEL, -efficientIn, 0);
            setActive(true);
        } else {
            int fuelVal = matchFuel(fuel, true);
            if (fuelVal > 0) {
                matchFuel(fuel, false);
                data.set(FUEL, fuelVal);
                data.set(MAX_FUEL, fuelVal);
                setActive(true);
            } else {
                setActive(false);
            }
        }
    }

    public abstract int matchFuel(ItemStack stack, boolean simulate);
}
