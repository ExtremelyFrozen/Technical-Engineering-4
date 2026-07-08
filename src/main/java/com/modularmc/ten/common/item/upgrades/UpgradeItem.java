package com.modularmc.ten.common.item.upgrades;

import com.modularmc.ten.common.item.TENBaseItem;

public abstract class UpgradeItem extends TENBaseItem {

    double percent;

    public UpgradeItem(double per) {
        super(new Properties().stacksTo(1));
        percent = per;
    }

    public UpgradeItem(double per, Properties properties) {
        super(properties.stacksTo(1));
        percent = per;
    }

    public boolean effect(IUpgradableMachine machine) {
        return true;
    }
}
