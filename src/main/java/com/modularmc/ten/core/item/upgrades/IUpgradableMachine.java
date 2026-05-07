package com.modularmc.ten.core.item.upgrades;

public interface IUpgradableMachine {

    boolean onUpgradeApply(double percent, int slotIncrease);
    boolean isType(String type);

    int getCurrentRadius();
    void setCurrentRadius(int radius);
    int getInitialRadius();
}
