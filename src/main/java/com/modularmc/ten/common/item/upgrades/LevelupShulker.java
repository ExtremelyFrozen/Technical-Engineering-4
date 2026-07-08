package com.modularmc.ten.common.item.upgrades;

public class LevelupShulker extends UpgradeItem {

    public LevelupShulker() {
        super(0.75);
    }

    public LevelupShulker(Properties properties) {
        super(0.75, properties);
    }

    @Override
    public boolean effect(IUpgradableMachine machine) {
        return machine.onUpgradeApply(percent, 3);
    }
}
