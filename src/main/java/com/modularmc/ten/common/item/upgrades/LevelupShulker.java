package com.modularmc.ten.common.item.upgrades;

import com.modularmc.ten.api.blockentity.IUpgradableMachine;

/** 整合组件·三档：全局倍率 ×4.00，耗时按倒数缩短，总能耗不变。 */
public class LevelupShulker extends UpgradeItem {

    public LevelupShulker() {
        super(0.75);
        configure();
    }

    public LevelupShulker(Properties properties) {
        super(0.75, properties);
        configure();
    }

    private void configure() {
        asTier("core", 3, UpgradeEffect.global(UpgradeConstants.TIER_MULTIPLY[2]));
    }

    @Override
    public boolean canApply(IUpgradableMachine machine) {
        // 信标模拟机不支持整合组件
        return !machine.isType("BEACON");
    }
}
