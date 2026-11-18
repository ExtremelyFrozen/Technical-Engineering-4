package com.modularmc.ten.common.item.upgrades;

import com.modularmc.ten.api.blockentity.IUpgradableMachine;

/** 整合组件·一档：全局倍率 ×2.00，耗时按倒数缩短，总能耗不变。 */
public class LevelupAug extends UpgradeItem {

    public LevelupAug() {
        super(0.25);
        configure();
    }

    public LevelupAug(Properties properties) {
        super(0.25, properties);
        configure();
    }

    private void configure() {
        asTier("core", 1, UpgradeEffect.global(UpgradeConstants.TIER_MULTIPLY[0]));
    }

    @Override
    public boolean canApply(IUpgradableMachine machine) {
        // 信标模拟机不支持整合组件
        return !machine.isType("BEACON");
    }
}
