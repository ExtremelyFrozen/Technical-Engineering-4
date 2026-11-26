package com.modularmc.ten.common.item.upgrades;

import com.modularmc.ten.api.blockentity.IUpgradableMachine;

/**
 * 增益组件·传输：机器能量收发当前不限速，本卡没有可观察效果。
 * 归增益组件而不是独特组件，避免占用「整机仅一个独特组件」的名额；
 * 系数为 {@link UpgradeEffect#NONE}，安装状态只写兼容标记。
 */
public class LevelupStream extends UpgradeItem {

    public LevelupStream() {
        super(0);
        asModifier(UpgradeEffect.NONE);
    }

    public LevelupStream(Properties properties) {
        super(0, properties);
        asModifier(UpgradeEffect.NONE);
    }

    @Override
    public boolean canApply(IUpgradableMachine machine) {
        return true;
    }

    @Override
    public boolean effect(IUpgradableMachine machine) {
        machine.setUnlimitedEnergyTransfer(true);
        return true;
    }
}
