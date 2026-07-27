package com.modularmc.ten.common.item.upgrades;

import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;

public class LevelupShulker extends UpgradeItem {

    public LevelupShulker() {
        super(0.75);
    }

    public LevelupShulker(Properties properties) {
        super(0.75, properties);
    }

    @Override
    public boolean canApply(IUpgradableMachine machine) {
        return true; // Repeatable on all upgradable machines (up to 6 slots)
    }

    @Override
    public boolean effect(IUpgradableMachine machine) {
        if (machine instanceof CmMachineBlockEntity cm) {
            cm.applyDurationMultiplier(0.40);  // time ×0.40 (-60%)
            cm.applyPowerMultiplier(2.00);     // power ×2.00 (+100%)
            cm.applyBatchIncrease(3);          // batch +3
        }
        return true;
    }
}
