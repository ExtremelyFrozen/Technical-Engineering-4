package com.modularmc.ten.common.item.upgrades;

public class LevelupIce extends UpgradeItem {

    public LevelupIce() {
        super(0);
    }

    public LevelupIce(Properties properties) {
        super(0, properties);
    }

    @Override
    public boolean effect(IUpgradableMachine machine) {
        return machine.isType("QUARRY");
    }
}
