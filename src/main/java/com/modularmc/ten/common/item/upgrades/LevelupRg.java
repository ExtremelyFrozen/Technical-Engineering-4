package com.modularmc.ten.common.item.upgrades;

public class LevelupRg extends UpgradeItem {

    public LevelupRg() {
        super(0);
    }

    public LevelupRg(Properties properties) {
        super(0, properties);
    }

    @Override
    public boolean effect(IUpgradableMachine machine) {
        if (machine.getCurrentRadius() > 0) {
            machine.setCurrentRadius(machine.getCurrentRadius() + (int) (machine.getInitialRadius() * 0.5));
            return true;
        }
        return false;
    }
}
