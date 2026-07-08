package com.modularmc.ten.common.item.upgrades;

public class LevelupMagma extends UpgradeItem {

    public LevelupMagma() {
        super(0);
    }

    public LevelupMagma(Properties properties) {
        super(0, properties);
    }

    @Override
    public boolean effect(IUpgradableMachine machine) {
        return machine.isType("QUARRY");
    }
}
