package com.modularmc.ten.common.item.upgrades;

public class LevelupSyn extends UpgradeItem {

    public LevelupSyn() {
        super(0);
    }

    public LevelupSyn(Properties properties) {
        super(0, properties);
    }

    @Override
    public boolean canApply(IUpgradableMachine machine) {
        return machine.isType("MACHINE_PROCESS") || machine.isType("MACHINE_EFFECT");
    }

    @Override
    public boolean effect(IUpgradableMachine machine) {
        machine.onUpgradeApply(-0.1, 1);
        return true;
    }
}
