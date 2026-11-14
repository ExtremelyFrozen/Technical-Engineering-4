package com.modularmc.ten.common.item.upgrades;

import com.modularmc.ten.api.blockentity.IUpgradableMachine;

/** 整合组件·二档：全局倍率 ×3.00，耗时按倒数缩短，总能耗不变。 */
public class LevelupPower extends UpgradeItem {

    public LevelupPower() {
        super(0.35);
        configure();
    }

    public LevelupPower(Properties properties) {
        super(0.35, properties);
        configure();
    }

    private void configure() {
        asTier("core", 2, UpgradeEffect.global(UpgradeConstants.TIER_MULTIPLY[1]));
    }

    @Override
    public boolean canApply(IUpgradableMachine machine) {
        // 信标模拟机不支持整合组件
        return !machine.isType("BEACON");
    }
}
