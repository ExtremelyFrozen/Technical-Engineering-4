package com.modularmc.ten.api.blockentity;

import com.modularmc.ten.api.option.MachineType;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class ProcessingMachineBlockEntity extends CmMachineBlockEntity {

    public ProcessingMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public int machineType() {
        return MachineType.MACHINE_PROCESS;
    }

    /**
     * Base processing time in ticks at 1 FE/t energy input.
     * Higher energy input reduces processing time proportionally.
     */
    public abstract int baseTickTime();

    @Override
    public void tick() {
        doBaseData();
        process();
    }

    public void process() {
        if (conditionStart() && signalAllowRun() && energyAllowRun()) {
            setActive(true);

            // Max progress = time needed * base energy rate
            // This represents the total "energy units" needed to complete
            maxProgress = baseTickTime() * Math.max(initialEfficientIn, 1);

            // Progress increases by actual efficiency (energy consumed this tick)
            int energyConsumed = Math.min(getActualEfficiency(), energyStorage.getEnergyStored());
            if (energyConsumed <= 0) {
                setActive(false);
                return;
            }
            progress += energyConsumed;

            if (cooking()) {
                setActive(false);
                return;
            }

            // Consume energy
            energyStorage.extractEnergy(energyConsumed, false);

            if (progress > maxProgress) {
                onCookFinish();
                progress = 0;
            }
        } else {
            setActive(false);
            progress = 0;
        }
    }

    public boolean cooking() {
        return false;
    }

    public void onCookFinish() {}

    public boolean conditionStart() {
        return true;
    }
}
