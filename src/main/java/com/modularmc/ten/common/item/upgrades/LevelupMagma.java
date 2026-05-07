package com.modularmc.ten.common.item.upgrades;

public class LevelupMagma extends UpgradeItem {

    public LevelupMagma() {
        super(0);
    }

    @Override
    public boolean effect(IUpgradableMachine machine) {
        return machine.isType("QUARRY");
    }
}
