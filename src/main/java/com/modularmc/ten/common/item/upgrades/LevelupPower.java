package com.modularmc.ten.common.item.upgrades;

import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;

public class LevelupPower extends UpgradeItem {

    public LevelupPower() {
        super(0.35);
    }

    public LevelupPower(Properties properties) {
        super(0.35, properties);
    }

    @Override
    public boolean canApply(IUpgradableMachine machine) {
        return true; // Repeatable on all upgradable machines (up to 6 slots)
    }

    @Override
    public boolean effect(IUpgradableMachine machine) {
        if (machine instanceof CmMachineBlockEntity cm) {
            cm.applyDurationMultiplier(UpgradeConstants.POWER_DURATION); // time ×0.60 (-40%)
            cm.applyPowerMultiplier(UpgradeConstants.POWER_POWER);       // power ×1.50 (+50%)
            cm.applyBatchIncrease(UpgradeConstants.POWER_BATCH);         // batch +1
        }
        return true;
    }
}
