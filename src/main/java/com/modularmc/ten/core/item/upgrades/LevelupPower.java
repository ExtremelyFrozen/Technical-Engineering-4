package com.modularmc.ten.core.item.upgrades;

public class LevelupPower extends UpgradeItem {

    public LevelupPower() {
        super(0.35);
    }

    @Override
    public boolean effect(IUpgradableMachine machine) {
        return machine.onUpgradeApply(percent, 2);
    }
}
