package com.modularmc.ten.api.blockentity;

import com.modularmc.ten.api.option.MachineType;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class EffectMachineBlockEntity extends CmMachineBlockEntity {

    public EffectMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public int machineType() {
        return MachineType.MACHINE_EFFECT;
    }

    @Override
    public void tick() {
        doBaseData();
        process();
    }

    public void process() {
        if (energyAllowRun() && signalAllowRun() && conditionStart()) {
            setActive(true);
            int energyConsumed = Math.min(getActualEfficiency(), energyStorage.getEnergyStored());
            if (energyConsumed <= 0) {
                setActive(false);
                return;
            }
            progress += energyConsumed;
            energyStorage.extractEnergy(energyConsumed, false);
            maxProgress = (int) (effectInterval() * 20 * Math.max(initialEfficientIn, 1));

            if (progress > maxProgress) {
                progress = 0;
                applyEffect();
            }
        } else {
            setActive(false);
            progress = 0;
        }
    }

    public abstract void applyEffect();

    public abstract double effectInterval();

    public boolean conditionStart() {
        return true;
    }
}
