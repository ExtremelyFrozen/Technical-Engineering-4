package com.modularmc.ten.common.item.upgrades;

import com.modularmc.ten.api.blockentity.IUpgradableMachine;

public class LevelupMineral extends UpgradeItem {

    public LevelupMineral() {
        super(0);
    }

    public LevelupMineral(Properties properties) {
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
