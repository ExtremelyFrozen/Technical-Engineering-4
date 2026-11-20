package com.modularmc.ten.api.option;

import java.util.EnumSet;
import java.util.Set;

public enum MachineType implements IMachineType{
    // Form kinds
    GENERATOR       (Category.GENERATOR,       EnergyFlow.OUTPUT, EnergyDemand.NOT_FULL),
    MACHINE_PROCESS (Category.MACHINE_PROCESS, EnergyFlow.INPUT,  EnergyDemand.SUFFICIENT),
    MACHINE_EFFECT  (Category.MACHINE_EFFECT,  EnergyFlow.INPUT,  EnergyDemand.SUFFICIENT),
    MACHINE_RADIUSED(Category.MACHINE_RADIUSED,EnergyFlow.INPUT,  EnergyDemand.SUFFICIENT),
    STORAGE         (Category.STORAGE,         EnergyFlow.BOTH,   EnergyDemand.ALWAYS),

    // Concrete machines
    FURNACE             (Category.MACHINE_PROCESS, EnergyFlow.INPUT, EnergyDemand.SUFFICIENT),
    PULVERIZER          (Category.MACHINE_PROCESS, EnergyFlow.INPUT, EnergyDemand.SUFFICIENT),
    COMPRESSOR          (Category.MACHINE_PROCESS, EnergyFlow.INPUT, EnergyDemand.SUFFICIENT),
    REFINER             (Category.MACHINE_PROCESS, EnergyFlow.INPUT, EnergyDemand.SUFFICIENT),
    INDUCTION_FURNACE   (Category.MACHINE_PROCESS, EnergyFlow.INPUT, EnergyDemand.SUFFICIENT),
    PSIONICANT          (Category.MACHINE_PROCESS, EnergyFlow.INPUT, EnergyDemand.SUFFICIENT),
    MATTER_CONDENSER    (Category.MACHINE_PROCESS, EnergyFlow.INPUT, EnergyDemand.SUFFICIENT),
    ENCHANTMENT_FLUSHER (Category.MACHINE_PROCESS, EnergyFlow.INPUT, EnergyDemand.SUFFICIENT),

    // Functional blocks
    BEACON       (Category.MACHINE_EFFECT, EnergyFlow.INPUT, EnergyDemand.SUFFICIENT),
    MOB_RIPPER   (Category.MACHINE_EFFECT, EnergyFlow.INPUT, EnergyDemand.SUFFICIENT),
    QUARRY       (Category.MACHINE_EFFECT, EnergyFlow.INPUT, EnergyDemand.SUFFICIENT),
    FARM         (Category.MACHINE_EFFECT, EnergyFlow.INPUT, EnergyDemand.SUFFICIENT),
    BLOCK_BREAKER(Category.MACHINE_EFFECT, EnergyFlow.INPUT, EnergyDemand.SUFFICIENT),
    BLOCK_FORMER (Category.MACHINE_EFFECT, EnergyFlow.INPUT, EnergyDemand.SUFFICIENT),
    COOLER       (Category.MACHINE_EFFECT, EnergyFlow.INPUT, EnergyDemand.SUFFICIENT),
    CAULDRON     (Category.MACHINE_EFFECT, EnergyFlow.INPUT, EnergyDemand.SUFFICIENT),
    DISTILLATOR  (Category.MACHINE_EFFECT, EnergyFlow.INPUT, EnergyDemand.SUFFICIENT),

    // Engines
    ENGINE_SOLAR     (Category.GENERATOR, EnergyFlow.OUTPUT, EnergyDemand.NOT_FULL),
    ENGINE_EXTRACTION(Category.GENERATOR, EnergyFlow.OUTPUT, EnergyDemand.NOT_FULL),
    ENGINE_METAL     (Category.GENERATOR, EnergyFlow.OUTPUT, EnergyDemand.NOT_FULL),
    ENGINE_BIOMASS   (Category.GENERATOR, EnergyFlow.OUTPUT, EnergyDemand.NOT_FULL),
    ENGINE_RECYCLER  (Category.GENERATOR, EnergyFlow.OUTPUT, EnergyDemand.ALWAYS),

    // Storage
    CELL         (Category.STORAGE, EnergyFlow.BOTH, EnergyDemand.ALWAYS),
    CREATIVE_CELL(Category.STORAGE, EnergyFlow.BOTH, EnergyDemand.ALWAYS);

    private final Category category;
    private final EnergyFlow energyFlow;
    private final EnergyDemand energyDemand;

    MachineType(Category category, EnergyFlow energyFlow, EnergyDemand energyDemand) {
        this.category = category;
        this.energyFlow = energyFlow;
        this.energyDemand = energyDemand;
    }

    private static final Set<MachineType> forms = EnumSet.of(GENERATOR, MACHINE_PROCESS, MACHINE_EFFECT, MACHINE_RADIUSED, STORAGE);

    @Override
    public Category getCategory() {
        return this.category;
    }

    @Override
    public EnergyFlow getEnergyFlow() {
        return this.energyFlow;
    }

    @Override
    public EnergyDemand getEnergyDemand() {
        return this.energyDemand;
    }

    @Override
    public boolean isMachineGUI() {
        return true;
    }
}
