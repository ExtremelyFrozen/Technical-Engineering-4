package com.modularmc.ten.common.item.upgrades;

public class LevelupStream extends UpgradeItem {

    public LevelupStream() {
        super(0);
    }

    public LevelupStream(Properties properties) {
        super(0, properties);
    }

    @Override
    public boolean effect(IUpgradableMachine machine) {
        return true;
    }
}
