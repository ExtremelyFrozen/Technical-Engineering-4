package com.modularmc.ten.common.item.upgrades;

public class LevelupPotion extends UpgradeItem {

    public LevelupPotion() {
        super(0);
    }

    public LevelupPotion(Properties properties) {
        super(0, properties);
    }

    @Override
    public boolean canApply(IUpgradableMachine machine) {
        return machine.isType("BEACON");
    }

    @Override
    public boolean effect(IUpgradableMachine machine) {
        return true;
    }
}
