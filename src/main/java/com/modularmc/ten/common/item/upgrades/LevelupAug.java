package com.modularmc.ten.common.item.upgrades;

public class LevelupAug extends UpgradeItem {

    public LevelupAug() {
        super(0.2);
    }

    public LevelupAug(Properties properties) {
        super(0.2, properties);
    }

    @Override
    public boolean effect(IUpgradableMachine machine) {
        return machine.onUpgradeApply(percent, 1);
    }
}
