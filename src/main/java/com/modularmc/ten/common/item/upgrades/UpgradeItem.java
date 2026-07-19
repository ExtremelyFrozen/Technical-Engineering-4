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

    /**
     * Check if this upgrade can be applied to the given machine.
     * <p>
     * This is the compatibility check — separated from {@link #effect(IUpgradableMachine)}
     * so that both GUI validation and quick-install can determine applicability
     * without executing potentially side-effectful effect code.
     * <p>
     * Subclasses that restrict machine type or radius MUST override this method
     * and move the type/radius check here, keeping only actual stat/behavior
     * modifications in {@code effect}.
     *
     * @param machine the target machine
     * @return true if this upgrade is compatible
     */
    public boolean canApply(IUpgradableMachine machine) {
        return true;
    }

    public boolean effect(IUpgradableMachine machine) {
        return true;
    }
}
