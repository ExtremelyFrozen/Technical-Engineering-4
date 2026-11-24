package com.modularmc.ten.common.item.upgrades;

/**
 * 升级数值的唯一事实源，{@link UpgradeItem#modifiers()} 与
 * {@link UpgradeTooltipFormatter} 都从这里取数，语言文件不写死数值。
 * 每个增益系列三个档位，数组下标 = 档位 - 1。
 *
 * <p>
 * 数值口径见 {@link UpgradeEffect}：功率倍率定 FE/t，速度增量（自然数）定每 tick 推进几格，
 * 耗时倍率定配方本身的长度，一次配方的耗电由这些量派生，不单独声明。
 *
 * <p>
 * <b>三个硬性约束</b>：
 * <ol>
 * <li>速度增量必须是自然数（0 / 1 / 2 / 3 …），不允许写成倍率或小数——
 * 机器上的 N 直接进进度条读数；</li>
 * <li>所有倍率（功率、耗时）必须是 0.05 的整数倍（即卡面百分比恒为 5% 的整数倍），
 * 卡面只把倍率换成 {@code ±n%} 或整数 {@code xN}，不做二次取整；</li>
 * <li>每张卡只声明它真的要改的通道，不为了「留个代价」去动无关参数——
 * 提速卡不动功率与配方长度、并行处理核心只加份数。</li>
 * </ol>
 */
public final class UpgradeConstants {

    private UpgradeConstants() {}

    // ── 整合组件：只给全局倍率 k（速度 ×k、耗时 ÷k、每份耗电不变）──────
    public static final double[] TIER_MULTIPLY = { 2.00, 3.00, 4.00 };

    // ── 增益组件·超频回路：只加运行速度，功率与配方耗时不变 ──────────────
    public static final int[] SPEED_ADD = { 1, 2, 3 };

    // ── 增益组件·工序精简：只缩短运行耗时（配方总刻度），速度与功率不变 ──
    public static final double[] DURATION_MULTIPLY = { 0.90, 0.80, 0.70 };

    // ── 增益组件·节能回路：只降运行功率，速度与耗时都不动 ──────────────
    public static final double[] EFFICIENCY_POWER = { 0.90, 0.80, 0.75 };

    // ── 增益组件·并行处理核心：只加并行运行数，功率与运行耗时都不动 ─────
    public static final int[] PARALLEL_BATCH = { 1, 2, 3 };

    // ── 光合供能抑制 ────────────────────────────────────────────────
    public static final double SYN_POWER = 0.80;
    /** FE/t injected when photosyn conditions are met. */
    public static final int SYN_PHOTOSYN_FE = 10;
    /** Max 1 Syn per machine. */
    public static final int SYN_UNIQUE = 1;

    // ── 范围与药水 ──────────────────────────────────────────────────
    /**
     * 范围升级每张对「可设上限」的倍率（实际范围由面板设定，升级不改工作盒）。
     * 必须取整数，卡面按 {@code xN} 书写。
     */
    public static final int RG_CAP_MULTIPLIER = 2;

    /** Potion effect amplifier boost. */
    public static final int POTION_AMPLIFIER = 1;

    // ── 独特组件·粉碎机副产物 ──────────────────────────────────────
    /** 副产物压榨：概率倍率（0.50 = 减半）与产出量倍率（期望 ×1.5）。 */
    public static final double BYPRODUCT_PRESS_CHANCE = 0.50;
    public static final int BYPRODUCT_PRESS_COUNT = 3;
    /** 副产物筛选：概率倍率（单次概率钳到 100%）。 */
    public static final double BYPRODUCT_FILTER_CHANCE = 2.00;

    // ── 独特组件·农场管理器收获期 ───────────────────────────────────
    /** 奇异催生：从周围 8 格中随机选中的作物株数与每株推进的生长刻度。 */
    public static final int FARM_GROW_TARGETS = 1;
    public static final int FARM_GROW_AGE_STEPS = 1;
    /** 幼芽复种：每次收获触发蔓延的概率。 */
    public static final double FARM_RESEED_CHANCE = 0.25;
    /** 幼芽复种：四邻偏移（同层，上下左右）。 */
    public static final int[][] FARM_RESEED_OFFSETS = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };

    public static int index(int level, int length) {
        return Math.max(0, Math.min(level - 1, length - 1));
    }
}
