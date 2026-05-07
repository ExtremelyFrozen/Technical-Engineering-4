package com.modularmc.ten.core.item.upgrades;

public class LevelupShulker extends UpgradeItem {

    public LevelupShulker() {
        super(0.75);
    }

    @Override
    public boolean effect(IUpgradableMachine machine) {
        return machine.onUpgradeApply(percent, 3);
    }
}
