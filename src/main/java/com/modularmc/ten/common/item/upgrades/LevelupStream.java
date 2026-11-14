package com.modularmc.ten.common.item.upgrades;

import com.modularmc.ten.api.blockentity.IUpgradableMachine;

/**
 * 增益组件·传输：能量收发已取消限速，本卡暂时没有可观察效果。
 * 归增益组件而不是独特组件，避免占用「整机仅一个独特组件」的名额；
 * 二期接入缓冲 / 传输维度后再定义系数，见 docs/ARCHITECTURE.md 的升级章节。
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
