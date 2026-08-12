package com.modularmc.ten.api.capability;

import net.neoforged.neoforge.energy.EnergyStorage;

public class MachineEnergyStorage extends EnergyStorage {

    private Runnable changeListener = () -> {};

    public MachineEnergyStorage(int capacity, int maxReceive, int maxExtract) {
        super(capacity, maxReceive, maxExtract);
    }

    public void setChangeListener(Runnable changeListener) {
        this.changeListener = changeListener != null ? changeListener : () -> {};
    }

    public void setEnergy(int energy) {
        this.energy = energy;
        changeListener.run();
    }

    public void setMaxReceive(int max) {
        this.maxReceive = max;
    }

    public int getMaxReceive() {
        return maxReceive;
    }

    public void setMaxExtract(int max) {
        this.maxExtract = max;
    }

    public int getMaxExtract() {
        return maxExtract;
    }

    /**
     * Sets the capacity of this energy storage.
     * If the current stored energy exceeds the new capacity, it is truncated.
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
