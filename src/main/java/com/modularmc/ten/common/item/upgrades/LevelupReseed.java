package com.modularmc.ten.common.item.upgrades;

import com.modularmc.ten.api.blockentity.IUpgradableMachine;

/**
 * 独特组件·幼芽复种：农场管理器每次收获后有 25% 概率，把种子槽的作物种子
 * 蔓延到目标同层周围 4 格的空耕地，不消耗种子槽物品。
 *
 * <p>
 * 只写 marker——蔓延由 {@code FarmBlockEntity#afterHarvest} 按
 * {@code hasUpgrade(LevelupReseed.class)} 执行。与奇异催生的互斥由
 * {@code SPECIALIZATION} 的整机唯一语义拦截。
 */
public class LevelupReseed extends UpgradeItem {

    public LevelupReseed() {
        super(0);
        asSpecialization();
    }

    public LevelupReseed(Properties properties) {
        super(0, properties);
        asSpecialization();
    }

    @Override
    public boolean canApply(IUpgradableMachine machine) {
        return machine.supportsHarvestSpecialization();
    }
}
