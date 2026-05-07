package com.modularmc.ten.lib.blockentity;

import com.modularmc.ten.lib.option.MachineType;

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
            data.translate(PROGRESS, getActualEfficiency());
            energyStorage.extractEnergy(getActualEfficiency(), false);
            data.set(MAX_PROGRESS, (int) (effectInterval() * 20 * Math.max(initialEfficientIn, 1)));

            if (data.get(PROGRESS) > data.get(MAX_PROGRESS)) {
                data.set(PROGRESS, 0);
                applyEffect();
            }
        } else {
            setActive(false);
            data.set(PROGRESS, 0);
        }
    }

    public abstract void applyEffect();
    public abstract double effectInterval();
    public boolean conditionStart() { return true; }
}
