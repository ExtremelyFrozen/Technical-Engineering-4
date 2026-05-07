package com.modularmc.ten.api.capability;

import net.neoforged.neoforge.energy.EnergyStorage;

public class MachineEnergyStorage extends EnergyStorage {

    public MachineEnergyStorage(int capacity, int maxReceive, int maxExtract) {
        super(capacity, maxReceive, maxExtract);
    }

    public void setEnergy(int energy) {
        this.energy = energy;
    }

    public void setMaxReceive(int max) {
        this.maxReceive = max;
    }

    public void setMaxExtract(int max) {
        this.maxExtract = max;
    }
}
