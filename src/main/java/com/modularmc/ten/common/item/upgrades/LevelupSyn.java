package com.modularmc.ten.common.item.upgrades;

import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;
import com.modularmc.ten.api.blockentity.IUpgradableMachine;

/** 增益组件·光合供能抑制：运行功率 -20%、速度与耗时不变，另在有光时向本机注能。 */
public class LevelupSyn extends UpgradeItem {

    public LevelupSyn() {
        super(0);
        configure();
    }

    public LevelupSyn(Properties properties) {
        super(0, properties);
        configure();
    }

    private void configure() {
        asModifier(UpgradeEffect.ofEfficiency(UpgradeConstants.SYN_POWER)).limitedTo(UpgradeConstants.SYN_UNIQUE);
    }

    @Override
    public boolean canApply(IUpgradableMachine machine) {
        return machine.isType("MACHINE_PROCESS") || machine.isType("MACHINE_EFFECT");
    }

    @Override
    public boolean effect(IUpgradableMachine machine) {
        if (machine instanceof CmMachineBlockEntity cm) {
            cm.applyPhotosyn();
        }
        return true;
    }
}
