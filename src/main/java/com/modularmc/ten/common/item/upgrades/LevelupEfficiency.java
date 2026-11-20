package com.modularmc.ten.common.item.upgrades;

/** 增益组件·效率三档：只压运行功率，速度与耗时都不动，每份耗电同比下降。 */
public class LevelupEfficiency extends UpgradeItem {

    public LevelupEfficiency() {
        this(1);
    }

    public LevelupEfficiency(int level) {
        super(0);
        int i = UpgradeConstants.index(level, UpgradeConstants.EFFICIENCY_POWER.length);
        asModifier(UpgradeEffect.ofEfficiency(UpgradeConstants.EFFICIENCY_POWER[i])).variant(level);
    }
}
