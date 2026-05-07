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

    public void setMaxExtract(int max) {
        this.maxExtract = max;
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
