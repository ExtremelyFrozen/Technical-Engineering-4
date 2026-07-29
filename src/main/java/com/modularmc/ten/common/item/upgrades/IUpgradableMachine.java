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

    // ───── Recipe mode (Blast/Smoke) ─────

    /** Recipe mode: vanilla furnace (smelting). */
    int RECIPE_MODE_SMELTING = 0;
    /** Recipe mode: blast furnace. */
    int RECIPE_MODE_BLASTING = 1;
    /** Recipe mode: smoker. */
    int RECIPE_MODE_SMOKING = 2;

    /**
     * Set the recipe mode for this machine.
     * Used by {@link LevelupBlast} and {@link LevelupSmoke} to switch
     * furnace recipe types. Only meaningful on FURNACE machines.
     * <p>
     * Default no-op for backward compatibility.
     */
    default void setRecipeMode(int mode) {
        // Default no-op
    }

    /**
     * @return the current recipe mode (default: {@link #RECIPE_MODE_SMELTING})
     */
    default int getRecipeMode() {
        return RECIPE_MODE_SMELTING;
    }

    // ───── Unlimited energy transfer (Stream) ─────

    /**
     * Enable or disable unlimited energy transfer for this machine.
     * When enabled, maxReceive and maxExtract are set to Integer.MAX_VALUE
     * during doBaseData, removing all rate limits on energy transfer.
     * <p>
     * Only affects transfer rate — capacity, FaceOption, canExternalExtract,
     * and direction guards remain unchanged.
     * <p>
     * Default no-op for backward compatibility.
     * <p>
     * Only implemented on {@link com.modularmc.ten.api.blockentity.CmMachineBlockEntity}.
     */
    default void setUnlimitedEnergyTransfer(boolean unlimited) {
        // Default no-op
    }

    /**
     * @return true if unlimited energy transfer is enabled
     */
    default boolean hasUnlimitedEnergyTransfer() {
        return false;
    }
}
