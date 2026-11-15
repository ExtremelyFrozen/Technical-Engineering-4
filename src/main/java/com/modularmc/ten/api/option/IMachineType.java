package com.modularmc.ten.api.option;

public interface IMachineType {
    MachineType.Category getCategory();
    MachineType.EnergyFlow getEnergyFlow();
    MachineType.EnergyDemand getEnergyDemand();
    boolean isMachineGUI();

    enum Category {
        MACHINE_PROCESS, MACHINE_EFFECT, MACHINE_RADIUSED, GENERATOR, STORAGE
    }

    /** Interaction with the outside world. */
    enum EnergyFlow {
        INPUT,  // input only
        OUTPUT, // output only
        BOTH    // both directions
    }

    /** Energy requirement while running. */
    enum EnergyDemand {
        SUFFICIENT,   // runs when energy is sufficient
        NOT_FULL, // runs while energy is not full
        ALWAYS     // runs perpetually
    }
}
