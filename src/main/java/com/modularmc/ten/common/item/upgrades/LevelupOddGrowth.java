package com.modularmc.ten.common.item.upgrades;

import com.modularmc.ten.api.blockentity.IUpgradableMachine;

/**
 * 独特组件·奇异催生：农场管理器每次收获后，从目标同层周围 8 格中随机选 1 株作物，
 * 推进 1 个生长刻度。
 *
 * <p>
 * 只写 marker——催生由 {@code FarmBlockEntity#afterHarvest} 按
 * {@code hasUpgrade(LevelupOddGrowth.class)} 执行，候选只取尚未满龄的作物，
 * 不消耗骨粉、不产生掉落。与幼芽复种的互斥由 {@code SPECIALIZATION} 的整机唯一语义拦截。
 */
public class LevelupOddGrowth extends UpgradeItem {

    public LevelupOddGrowth() {
        super(0);
        asSpecialization();
    }

    public LevelupOddGrowth(Properties properties) {
        super(0, properties);
        asSpecialization();
    }

    @Override
    public boolean canApply(IUpgradableMachine machine) {
        return machine.supportsHarvestSpecialization();
    }
}
