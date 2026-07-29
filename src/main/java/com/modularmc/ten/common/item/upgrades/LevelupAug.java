package com.modularmc.ten.common.item.upgrades;

import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;

public class LevelupAug extends UpgradeItem {

    public LevelupAug() {
        super(0.25);
    }

    public LevelupAug(Properties properties) {
        super(0.25, properties);
    }

    @Override
    public boolean canApply(IUpgradableMachine machine) {
        return true; // Repeatable on all upgradable machines (up to 6 slots)
    }

    @Override
    public boolean effect(IUpgradableMachine machine) {
        if (machine instanceof CmMachineBlockEntity cm) {
            cm.applyDurationMultiplier(UpgradeConstants.AUG_DURATION);   // time ×0.75 (-25%)
            cm.applyPowerMultiplier(UpgradeConstants.AUG_POWER);         // power ×1.30 (+30%)
            cm.applyBatchIncrease(0);                                    // batch +0
        }
        return true;
    }
}
