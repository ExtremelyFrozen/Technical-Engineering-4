package com.modularmc.ten.common.item.upgrades;

import com.modularmc.ten.api.blockentity.IUpgradableMachine;

public class LevelupKnow extends UpgradeItem {

    public LevelupKnow() {
        super(0);
    }

    public LevelupKnow(Properties properties) {
        super(0, properties);
    }

    @Override
    public boolean canApply(IUpgradableMachine machine) {
        return machine.isType("FURNACE");
    }

    @Override
    public boolean effect(IUpgradableMachine machine) {
        // All Knowledge logic (XP tank, XP output) is handled directly
        // in FurnaceBlockEntity's conditionStart/cooking/onCookFinish.
        // This upgrade serves as a marker — effect is a no-op because
        // the machine action is not stat-based but structural (tank + recipe XP).
        // The upgrade presence is detected via hasUpgrade(LevelupKnow.class).
        return true;
    }
}
