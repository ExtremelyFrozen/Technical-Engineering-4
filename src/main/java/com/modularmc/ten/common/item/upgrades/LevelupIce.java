package com.modularmc.ten.common.item.upgrades;

public class LevelupIce extends UpgradeItem {

    public LevelupIce() {
        super(0);
    }

    @Override
    public boolean effect(IUpgradableMachine machine) {
        return machine.isType("QUARRY");
    }
}
