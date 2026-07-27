package com.modularmc.ten.common.item.upgrades;

public interface IUpgradableMachine {

    boolean onUpgradeApply(double percent, int slotIncrease);

    boolean isType(String type);

    int getCurrentRadius();

    void setCurrentRadius(int radius);

    int getInitialRadius();

    // ───── P2 乘法模型 API (T1-T5) ─────

    /**
     * Apply a duration multiplier to the machine's processing time.
     * The multiplier is compounded multiplicatively: total = Π factor_i.
     * <p>
     * Only implemented on {@link com.modularmc.ten.api.blockentity.CmMachineBlockEntity}.
     * Default no-op for backward compatibility.
     *
     * @param factor the duration multiplier (must be finite, > 0)
     * @throws IllegalArgumentException if factor is NaN, infinite, or ≤ 0
     */
    default void applyDurationMultiplier(double factor) {
        if (Double.isNaN(factor) || Double.isInfinite(factor) || factor <= 0) {
            throw new IllegalArgumentException("Invalid duration multiplier: " + factor);
        }
    }

    /**
     * Apply a power (FE/t) multiplier to the machine's base efficiency.
     * The multiplier is compounded multiplicatively: total = Π factor_i.
     * <p>
     * Only implemented on {@link com.modularmc.ten.api.blockentity.CmMachineBlockEntity}.
     * Default no-op for backward compatibility.
     *
     * @param factor the power multiplier (must be finite, > 0)
     * @throws IllegalArgumentException if factor is NaN, infinite, or ≤ 0
     */
    default void applyPowerMultiplier(double factor) {
        if (Double.isNaN(factor) || Double.isInfinite(factor) || factor <= 0) {
            throw new IllegalArgumentException("Invalid power multiplier: " + factor);
        }
    }

    /**
     * Increase the batch counter by the given amount (additive).
     * B_theory = 1 + Σbatch_i, capped at 19.
     * <p>
     * Only implemented on {@link com.modularmc.ten.api.blockentity.CmMachineBlockEntity}.
     * Default no-op for backward compatibility.
     *
     * @param increase non-negative batch increment
     * @throws IllegalArgumentException if increase is negative
     */
    default void applyBatchIncrease(int increase) {
        if (increase < 0) {
            throw new IllegalArgumentException("Batch increase must be non-negative: " + increase);
        }
    }

    /**
     * Mark that a Photosynthetic Suppressor (LevelupSyn) is installed.
     * Duplicate calls are silently ignored (safe idempotency).
     * <p>
     * Only implemented on {@link com.modularmc.ten.api.blockentity.CmMachineBlockEntity}.
     * Default no-op for backward compatibility.
     */
    default void applyPhotosyn() {
        // Default no-op
    }
}
