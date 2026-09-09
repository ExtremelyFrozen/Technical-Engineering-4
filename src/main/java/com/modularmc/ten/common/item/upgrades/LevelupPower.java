package com.modularmc.ten.common.item.upgrades;

import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;
import com.modularmc.ten.api.blockentity.IUpgradableMachine;

public class LevelupPower extends UpgradeItem {

    public LevelupPower() {
        super(0.35);
    }

    public LevelupPower(Properties properties) {
        super(0.35, properties);
    }

    @Override
    public boolean canApply(IUpgradableMachine machine) {
        // 模拟信标不支持充能组件（规格：信标禁增强/充能/潜影）
        return !machine.isType("BEACON");
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
