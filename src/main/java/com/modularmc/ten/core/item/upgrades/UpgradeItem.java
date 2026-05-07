package com.modularmc.ten.core.item.upgrades;

import com.modularmc.ten.core.item.TENBaseItem;

public abstract class UpgradeItem extends TENBaseItem {

    double percent;

    public UpgradeItem(double per) {
        super(new Properties().stacksTo(1));
        percent = per;
    }

    public boolean effect(IUpgradableMachine machine) {
        return true;
    }
}
