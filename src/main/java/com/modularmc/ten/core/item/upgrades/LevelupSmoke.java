package com.modularmc.ten.core.item.upgrades;

public class LevelupSmoke extends UpgradeItem {

    public LevelupSmoke() {
        super(0);
    }

    @Override
    public boolean effect(IUpgradableMachine machine) {
        return machine.isType("FURNACE");
    }
}
