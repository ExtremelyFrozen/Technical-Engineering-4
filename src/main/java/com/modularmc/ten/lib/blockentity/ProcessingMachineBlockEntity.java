package com.modularmc.ten.lib.blockentity;

import com.modularmc.ten.lib.option.MachineType;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class ProcessingMachineBlockEntity extends CmMachineBlockEntity {

    public static final int PROGRESS = 0;
    public static final int MAX_PROGRESS = 1;
    public static final int ENERGY = 2;
    public static final int MAX_ENERGY = 3;
    public static final int FUEL = 4;
    public static final int MAX_FUEL = 5;
    public static final int E_REC = 6;
    public static final int E_EXT = 7;
    public static final int I_REC = 8;
    public static final int I_EXT = 9;
    public static final int F_REC = 10;
    public static final int F_EXT = 11;
    public static final int RED_MODE = 12;
    public static final int FACE = 13;
    public static final int EFF_AUC = 14;
    public static final int EFF = 15;
    public static final int UPGSIZE = 16;

    public ProcessingMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public int machineType() {
        return MachineType.MACHINE_PROCESS;
    }

    public abstract int baseTickTime();

    @Override
    public void tick() {
        doBaseData();
        process();
    }

    public void process() {
        if (conditionStart() && signalAllowRun() && energyAllowRun()) {
            setActive(true);
            data.set(MAX_PROGRESS, baseTickTime() * Math.max(initialEfficientIn, 1));
            data.translate(PROGRESS, getActualEfficiency());

            if (cooking()) {
                setActive(false);
                return;
            }
            energyStorage.extractEnergy(getActualEfficiency(), false);

            if (data.get(PROGRESS) > data.get(MAX_PROGRESS)) {
                onCookFinish();
                data.set(PROGRESS, 0);
            }
        } else {
            setActive(false);
            data.set(PROGRESS, 0);
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
