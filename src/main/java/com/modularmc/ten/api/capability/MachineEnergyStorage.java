package com.modularmc.ten.api.capability;

import net.neoforged.neoforge.energy.EnergyStorage;

/**
 * 机器储能能力：容量可在运行时调整（并行运行 B 缩放、频道动态容量），存量超容时截断。
 * 收发速率恒为不限——能量进出只受容量与接收方自身上限约束。
 */
public class MachineEnergyStorage extends EnergyStorage {

    private Runnable changeListener = () -> {};

    public MachineEnergyStorage(int capacity) {
        super(capacity, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    public void setChangeListener(Runnable changeListener) {
        this.changeListener = changeListener != null ? changeListener : () -> {};
    }

    public void setEnergy(int energy) {
        this.energy = energy;
        changeListener.run();
    }

    /**
     * 设置容量（频道共享存储动态容量用）。若当前储能超过新容量则截断。
     */
    public void setCapacity(int capacity) {
        this.capacity = Math.max(0, capacity);
        if (this.energy > this.capacity) {
            this.energy = this.capacity;
        }
        changeListener.run();
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int received = super.receiveEnergy(maxReceive, simulate);
        if (received > 0 && !simulate) {
            changeListener.run();
        }
        return received;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        int extracted = super.extractEnergy(maxExtract, simulate);
        if (extracted > 0 && !simulate) {
            changeListener.run();
        }
        return extracted;
    }
}
