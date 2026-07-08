package com.modularmc.ten.common.item.upgrades;

public class LevelupKnow extends UpgradeItem {

    public LevelupKnow() {
        super(0);
    }

    @Override
    public boolean effect(IUpgradableMachine machine) {
        machine.onUpgradeApply(0, 5);
        return true;
    }
}
