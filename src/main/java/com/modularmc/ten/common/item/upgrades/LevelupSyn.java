package com.modularmc.ten.common.item.upgrades;

import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;
import com.modularmc.ten.api.blockentity.IUpgradableMachine;

public class LevelupSyn extends UpgradeItem {

    public LevelupSyn() {
        super(0);
    }

    public LevelupSyn(Properties properties) {
        super(0, properties);
    }

    @Override
    public boolean canApply(IUpgradableMachine machine) {
        return machine.isType("MACHINE_PROCESS") || machine.isType("MACHINE_EFFECT");
    }

    @Override
    public boolean effect(IUpgradableMachine machine) {
        if (machine instanceof CmMachineBlockEntity cm) {
            // Idempotent: only apply Syn effects once per machine
            if (cm.photosynInstalled) return true;
            cm.applyDurationMultiplier(UpgradeConstants.SYN_DURATION); // time ×1.5 (50% longer)
            cm.applyPowerMultiplier(UpgradeConstants.SYN_POWER);       // FE/t ×0.8 (20% reduction)
            cm.applyBatchIncrease(0);                                  // batch +0
            cm.applyPhotosyn();                                        // mark installed
        }
        return true;
    }
}
