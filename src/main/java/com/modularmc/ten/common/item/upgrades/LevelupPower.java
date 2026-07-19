package com.modularmc.ten.common.item.upgrades;

public class LevelupPower extends UpgradeItem {

    public LevelupPower() {
        super(0.35);
    }

    public LevelupPower(Properties properties) {
        super(0.35, properties);
    }

    @Override
    public boolean canApply(IUpgradableMachine machine) {
        return true; // No type restriction; works on all upgradable machines
    }

    @Override
    public boolean effect(IUpgradableMachine machine) {
        return machine.onUpgradeApply(percent, 2);
    }
}
