package com.modularmc.ten.common.item.upgrades;

import com.modularmc.ten.api.blockentity.IUpgradableMachine;

/**
 * 独特组件·副产物筛选：粉碎机副产概率 ×200%（单次概率上限 100%）。
 *
 * <p>
 * 只写 marker——有效概率由 {@code RecipeMachineBlockEntity} 传给
 * {@code FormsCombinedIngredient#genItem(double)} / {@code genFluid(double)}，
 * 只作用于 {@code chance < 1} 的概率输出，确定产出不受影响。
 * 与副产物压榨的互斥由 {@code SPECIALIZATION} 的整机唯一语义在放入槽位时拦截。
 */
public class LevelupFilter extends UpgradeItem {

    public LevelupFilter() {
        super(0);
        asSpecialization();
    }

    public LevelupFilter(Properties properties) {
        super(0, properties);
        asSpecialization();
    }

    @Override
    public boolean canApply(IUpgradableMachine machine) {
        return machine.supportsByproductTuning();
    }
}
