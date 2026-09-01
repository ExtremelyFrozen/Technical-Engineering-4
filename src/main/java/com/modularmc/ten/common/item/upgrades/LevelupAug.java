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
        // 模拟信标不支持增强组件（规格：信标禁增强/充能/潜影）
        return !machine.isType("BEACON");
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
