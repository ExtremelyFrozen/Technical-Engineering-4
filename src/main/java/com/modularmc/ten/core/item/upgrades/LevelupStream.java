package com.modularmc.ten.core.item.upgrades;

public class LevelupStream extends UpgradeItem {

    public LevelupStream() {
        super(0);
    }

    @Override
    public boolean effect(IUpgradableMachine machine) {
        return true;
    }
}
