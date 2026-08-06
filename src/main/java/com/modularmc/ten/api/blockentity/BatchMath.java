package com.modularmc.ten.api.blockentity;

/**
 * Pure domain math for batch factor (B) calculation.
 * <p>
 * Contains no Minecraft dependencies — safe to call from unit tests
 * without game bootstrap. Used by {@link CmMachineBlockEntity#validateAndLockB}
 * during normal production operation.
 * <p>
 * All methods are stateless and thread-safe.
 */
public final class BatchMath {

    private BatchMath() {}

    /** Hard cap for B_actual (6 × Shulker = 19). */
    public static final int B_HARD_MAX = 19;

    /**
     * Validates that the given batch size B is positive.
     *
     * @param B the batch size to validate
     * @throws IllegalArgumentException if B <= 0
     */
    public static void requirePositiveBatchSize(int B) {
        if (B <= 0) throw new IllegalArgumentException("B must be >= 1, got: " + B);
    }

    /**
     * Pure calculation of B_actual from all dimensional constraints.
     * <p>
     * Takes the minimum across all constraints, capped at {@value #B_HARD_MAX}.
     * Returns 0 if any constraint reduces B below 1 (operation cannot start).
     *
     * @param B_theory   theoretical B (= 1 + Σbatch_i)
     * @param B_byItems  item input dimension constraint
     * @param B_byFluids fluid input dimension constraint
     * @param B_byOutput output capacity dimension constraint
     * @param B_byEnergy energy dimension constraint
     * @return clamped B in 0..{@value #B_HARD_MAX}, where 0 means cannot start
     */
    public static int calculateBActual(int B_theory, int B_byItems, int B_byFluids,
                                       int B_byOutput, int B_byEnergy) {
        int B = Math.max(0, B_theory);
        B = Math.min(B, Math.max(0, B_byItems));
        B = Math.min(B, Math.max(0, B_byFluids));
        B = Math.min(B, Math.max(0, B_byOutput));
        B = Math.min(B, Math.max(0, B_byEnergy));
        B = Math.min(B, B_HARD_MAX);
        return Math.max(0, B);
    }
}
