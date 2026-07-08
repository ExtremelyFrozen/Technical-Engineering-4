package com.modularmc.ten.common.item.upgrades;

public class LevelupPotion extends UpgradeItem {

    public LevelupPotion() {
        super(0);
    }

    @Override
    public boolean effect(IUpgradableMachine machine) {
        return machine.isType("BEACON");
    }
}
