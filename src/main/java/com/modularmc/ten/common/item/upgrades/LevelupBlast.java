package com.modularmc.ten.common.item.upgrades;

public class LevelupBlast extends UpgradeItem {

    public LevelupBlast() {
        super(0);
    }

    @Override
    public boolean effect(IUpgradableMachine machine) {
        return machine.isType("FURNACE");
    }
}
