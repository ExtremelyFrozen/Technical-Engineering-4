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
        machine.setCurrentRadius(machine.getCurrentRadius() + (int) (machine.getInitialRadius() * 0.5));
        return true;
    }
}
