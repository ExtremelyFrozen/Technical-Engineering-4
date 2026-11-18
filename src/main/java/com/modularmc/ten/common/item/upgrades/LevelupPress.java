package com.modularmc.ten.common.item.upgrades;

import com.modularmc.ten.api.blockentity.IUpgradableMachine;

/**
 * 独特组件·副产物压榨：粉碎机副产概率 ×50%，副产物产出量 ×3。
 *
 * <p>
 * 只写 marker——概率与产出量的改写由 {@code RecipeMachineBlockEntity} 在收集概率输出时
 * 按 {@code hasUpgrade(LevelupPress.class)} 读取 {@link UpgradeConstants} 完成，
 * 预检也按同一倍率放大最坏产出，保持快照-模拟-提交的一致性。
 * 与副产物筛选的互斥由 {@code SPECIALIZATION} 的整机唯一语义在放入槽位时拦截。
 */
public class LevelupPress extends UpgradeItem {

    public LevelupPress() {
        super(0);
        asSpecialization();
    }

    public LevelupPress(Properties properties) {
        super(0, properties);
        asSpecialization();
    }

    @Override
    public boolean canApply(IUpgradableMachine machine) {
        return machine.supportsByproductTuning();
    }
}
