package com.modularmc.ten.common.item.upgrades;

public class LevelupBlast extends UpgradeItem {

    public LevelupBlast() {
        super(0);
    }

    public LevelupBlast(Properties properties) {
        super(0, properties);
    }

    @Override
    public boolean canApply(IUpgradableMachine machine) {
        return machine.isType("FURNACE");
    }

    @Override
    public boolean effect(IUpgradableMachine machine) {
        return true;
    }
}
