package com.modularmc.ten.common.item.upgrades;

/**
 * Shared upgrade constants — single source of truth for numerical values.
 * <p>
 * Both {@link UpgradeItem#effect} implementations and
 * {@link UpgradeTooltipFormatter} read from these constants.
 * No numerical value should be hardcoded in lang or tooltip logic.
 */
public final class UpgradeConstants {

    private UpgradeConstants() {}

    // ── LevelupAug ──────────────────────────────────────────────────
    public static final double AUG_DURATION = 0.75;
    public static final double AUG_POWER = 1.30;

    // ── LevelupPower ────────────────────────────────────────────────
    public static final double POWER_DURATION = 0.60;
    public static final double POWER_POWER = 1.50;
    public static final int POWER_BATCH = 1;

    // ── LevelupShulker ──────────────────────────────────────────────
    public static final double SHULKER_DURATION = 0.40;
    public static final double SHULKER_POWER = 2.00;
    public static final int SHULKER_BATCH = 3;

    // ── LevelupSyn (Photosynthetic Suppressor) ──────────────────────
    public static final double SYN_DURATION = 1.50;
    public static final double SYN_POWER = 0.80;
    /** FE/t injected when photosyn conditions are met. */
    public static final int SYN_PHOTOSYN_FE = 10;
    /** Max 1 Syn per machine. */
    public static final int SYN_UNIQUE = 1;

    // ── LevelupRg (Range) ───────────────────────────────────────────
    /** Fraction of initial radius added per Range upgrade. */
    public static final double RG_RANGE_FRACTION = 0.50;

    /**
     * 采矿场范围升级外扩步长（格/件）：范围 = 所属区块 16×16 以区块中心对称外扩
     * QUARRY_RANGE_STEP × Rg件数（等效半径 8×RG_RANGE_FRACTION 对齐信标 50% 规则）。
     */
    public static final int QUARRY_RANGE_STEP = 4;

    // ── LevelupPotion ───────────────────────────────────────────────
    /** Potion effect amplifier boost. */
    public static final int POTION_AMPLIFIER = 1;
}
