package com.modularmc.ten.common.item.upgrades;

import com.modularmc.ten.api.blockentity.IUpgradableMachine;

/**
 * 增益组件·并行处理核心三档：只加并行运行数，功率与运行耗时都不动。
 * 能不能装由 {@link IUpgradableMachine#supportsBatch()} 在放入时判定，
 * 卡面不再写「仅某类机器可用」——那属于拦截层的职责。
 */
public class LevelupParallel extends UpgradeItem {

    public LevelupParallel() {
        this(1);
    }

    public LevelupParallel(int level) {
        super(0);
        int i = UpgradeConstants.index(level, UpgradeConstants.PARALLEL_BATCH.length);
        asModifier(UpgradeEffect.ofBatch(UpgradeConstants.PARALLEL_BATCH[i])).variant(level);
    }

    @Override
    public boolean canApply(IUpgradableMachine machine) {
        // 不消费并行运行的机器（如光伏引擎）装了只会白占槽位，放入时直接拒绝
        return machine.supportsBatch();
    }
}
