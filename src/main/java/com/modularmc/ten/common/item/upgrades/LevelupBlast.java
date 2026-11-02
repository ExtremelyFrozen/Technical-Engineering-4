package com.modularmc.ten.common.item.upgrades;

import com.modularmc.ten.api.blockentity.IUpgradableMachine;

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
        machine.setRecipeMode(IUpgradableMachine.RECIPE_MODE_BLASTING);
        return true;
    }
}
