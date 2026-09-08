package com.modularmc.ten.common.item.upgrades;

public class LevelupRg extends UpgradeItem {

    public LevelupRg() {
        super(0);
    }

    public LevelupRg(Properties properties) {
        super(0, properties);
    }

    @Override
    public boolean canApply(IUpgradableMachine machine) {
        return machine.getCurrentRadius() > 0;
    }

    @Override
    public boolean effect(IUpgradableMachine machine) {
        // Only called when canApply already returned true, so radius > 0 is guaranteed
        int addRadius;
        if (machine instanceof com.modularmc.ten.common.blockentity.machine.QuarryBlockEntity) {
            // 采矿场：范围 = 所属区块 16×16 为基础，每件范围升级以区块中心对称外扩 4 格
            // （等效半径 8×RG_RANGE_FRACTION，对齐信标 50% 规则）
            addRadius = UpgradeConstants.QUARRY_RANGE_STEP;
        } else if (machine instanceof com.modularmc.ten.common.blockentity.machine.BlockBreakerBlockEntity || machine instanceof com.modularmc.ten.common.blockentity.machine.BlockFormerBlockEntity) {
            // 方块破坏器/成型器：工作宽度每件 +1（initialRadius=1 的 50% 分数取整为 0，
            // 通用百分比公式在此语义失效，故用整格步进）
            addRadius = 1;
        } else {
            addRadius = (int) (machine.getInitialRadius() * UpgradeConstants.RG_RANGE_FRACTION);
        }
        machine.setCurrentRadius(machine.getCurrentRadius() + addRadius);
        return true;
    }
}
