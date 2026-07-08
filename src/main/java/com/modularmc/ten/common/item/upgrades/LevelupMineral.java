package com.modularmc.ten.common.item.upgrades;

public class LevelupMineral extends UpgradeItem {

    public LevelupMineral() {
        super(0);
    }

    @Override
    public boolean effect(IUpgradableMachine machine) {
        return machine.isType("QUARRY");
    }
}
