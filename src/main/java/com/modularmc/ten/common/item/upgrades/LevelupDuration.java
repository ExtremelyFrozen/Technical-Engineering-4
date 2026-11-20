package com.modularmc.ten.common.item.upgrades;

/**
 * 增益组件·工序精简三档：缩短配方本身的长度（{@code T × 0.90 / 0.80 / 0.70}），
 * 运行速度与运行功率都不动。
 *
 * <p>
 * 它走的是「运行耗时」这条独立通道：进度条总刻度变短，每 tick 仍推进 N 格。
 * 与超频回路（每 tick 推进更多格）一起装时两者各自生效，
 * 实际耗时 = {@code 配方耗时 × 耗时倍率 ÷ 速度}。
 */
public class LevelupDuration extends UpgradeItem {

    public LevelupDuration() {
        this(1);
    }

    public LevelupDuration(int level) {
        super(0);
        int i = UpgradeConstants.index(level, UpgradeConstants.DURATION_MULTIPLY.length);
        asModifier(UpgradeEffect.ofDuration(UpgradeConstants.DURATION_MULTIPLY[i])).variant(level);
    }
}
