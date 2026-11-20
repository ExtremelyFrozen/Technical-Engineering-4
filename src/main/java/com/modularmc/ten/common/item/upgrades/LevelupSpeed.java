package com.modularmc.ten.common.item.upgrades;

/** 增益组件·运行速度三档：只加 N（每 tick 推进的格数），功率与配方长度都不变。 */
public class LevelupSpeed extends UpgradeItem {

    public LevelupSpeed() {
        this(1);
    }

    public LevelupSpeed(int level) {
        super(0);
        int i = UpgradeConstants.index(level, UpgradeConstants.SPEED_ADD.length);
        asModifier(UpgradeEffect.ofSpeed(UpgradeConstants.SPEED_ADD[i])).variant(level);
    }
}
