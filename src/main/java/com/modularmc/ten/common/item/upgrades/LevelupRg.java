package com.modularmc.ten.common.item.upgrades;

import com.modularmc.ten.api.blockentity.IUpgradableMachine;

/**
 * 增益组件·范围：把面板的可设上限按倍率抬高（每张 ×2）。
 * 面板的 XYZ 盒是唯一工作区数据源，卡片只决定「最多能设到多大」，不改实际范围。
 */
public class LevelupRg extends UpgradeItem {

    public LevelupRg() {
        super(0);
        asModifier(UpgradeEffect.NONE);
    }

    public LevelupRg(Properties properties) {
        super(0, properties);
        asModifier(UpgradeEffect.NONE);
    }

    @Override
    public boolean canApply(IUpgradableMachine machine) {
        // 只有接面板的机器装得上（采矿场 / 信标 / 农场 / 生物粉碎器 / 破坏器 / 成型器）
        return machine.supportsRangeAdjust();
    }

    @Override
    public boolean effect(IUpgradableMachine machine) {
        machine.applyAdjustRangeCapMultiplier(UpgradeConstants.RG_CAP_MULTIPLIER);
        return true;
    }
}
