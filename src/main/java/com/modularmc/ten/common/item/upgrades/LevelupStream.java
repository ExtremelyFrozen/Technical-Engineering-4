package com.modularmc.ten.common.item.upgrades;

public class LevelupStream extends UpgradeItem {

    public LevelupStream() {
        super(0);
    }

    public LevelupStream(Properties properties) {
        super(0, properties);
    }

    @Override
    public boolean canApply(IUpgradableMachine machine) {
        return true; // No type restriction; works on all upgradable machines
    }

    @Override
    public boolean effect(IUpgradableMachine machine) {
        return true;
    }
}
