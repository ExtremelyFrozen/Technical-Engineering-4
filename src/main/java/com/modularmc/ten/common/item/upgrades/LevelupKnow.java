package com.modularmc.ten.common.item.upgrades;

public class LevelupKnow extends UpgradeItem {

    public LevelupKnow() {
        super(0);
    }

    public LevelupKnow(Properties properties) {
        super(0, properties);
    }

    /**
     * LevelupKnow is a legacy-compatible upgrade with no stat effect (percent=0).
     * It previously granted +5 upgrade slots; slot unlocks are now deprecated
     * since all 6 slots are always available. This upgrade is kept registered
     * for backward compatibility — canApply always returns true (no machine
     * type restriction), and effect is a no-op.
     */
    @Override
    public boolean canApply(IUpgradableMachine machine) {
        return true;
    }

    @Override
    public boolean effect(IUpgradableMachine machine) {
        // slotIncrease (5) is ignored in onUpgradeApply; percent=0 so no stat change.
        // This preserves compatibility with existing installed upgrades.
        machine.onUpgradeApply(0, 5);
        return true;
    }
}
