package com.modularmc.ten.common.item.upgrades;

import com.modularmc.ten.api.blockentity.IUpgradableMachine;

public class LevelupMagma extends UpgradeItem {

    public LevelupMagma() {
        super(0);
    }

    public LevelupMagma(Properties properties) {
        super(0, properties);
    }

    @Override
    public boolean canApply(IUpgradableMachine machine) {
        return machine.isType("QUARRY");
    }

    @Override
    public boolean effect(IUpgradableMachine machine) {
        return true;
    }
}
