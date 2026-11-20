package com.modularmc.ten.common.item.upgrades;

import com.modularmc.ten.api.blockentity.IUpgradableMachine;

/** 增益组件·范围：无数值系数，副作用写在 {@link #effect} 里（半径或可调上限）。 */
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
        // 范围机仍以 getCurrentRadius() 作为门禁；面板型机器二期改走能力声明
        return machine.getCurrentRadius() > 0;
    }

    @Override
    public boolean effect(IUpgradableMachine machine) {
        // Only called when canApply already returned true, so radius > 0 is guaranteed
        if (machine instanceof com.modularmc.ten.common.blockentity.machine.BlockBreakerBlockEntity || machine instanceof com.modularmc.ten.common.blockentity.machine.BlockFormerBlockEntity) {
            // 这两台的工作范围已交给面板的 XYZ 范围与偏移，半径不再自动增长；
            // 范围升级改为把「最大可调节半径」翻倍。
            machine.applyAdjustRangeCapMultiplier(2);
            return true;
        }
        int addRadius;
        if (machine instanceof com.modularmc.ten.common.blockentity.machine.QuarryBlockEntity) {
            // 采矿场：范围 = 所属区块 16×16 为基础，每件范围升级以区块中心对称外扩 4 格
            addRadius = UpgradeConstants.QUARRY_RANGE_STEP;
        } else {
            addRadius = (int) (machine.getInitialRadius() * UpgradeConstants.RG_RANGE_FRACTION);
        }
        machine.setCurrentRadius(machine.getCurrentRadius() + addRadius);
        return true;
    }
}
