package com.modularmc.ten.common.item.upgrades;

/**
 * 升级对运行参数的声明式系数。四条通道各自独立，互不推导：
 *
 * <ul>
 * <li>{@code power} — 运行功率倍率（FE/t 的强度），小数倍率；</li>
 * <li>{@code speed} — 运行速度增量，<b>只能是自然数</b>（0 表示不改速度）。
 * 基准速度是 1，所以 {@code speed = 2} 表示这台机器每 tick 推进 2 tick 的配方进度，
 * 耗时变成原来的三分之一。它只让时间走得更快，不碰每 tick 的耗电；</li>
 * <li>{@code duration} — 运行耗时倍率（配方本身的长度），小数倍率，1.0 表示不改。
 * 它缩短的是「配方总刻度」：{@code maxProgress = T × duration}，
 * 与速度通道的「每 tick 推进 N 格」互不干涉，两张卡同时装时 {@code 耗时 = T × duration ÷ N}；</li>
 * <li>{@code batch} — 并行运行增量（一个周期同时做几份）。</li>
 * </ul>
 *
 * 速度之所以是整数增量而不是倍率：N 是玩家在进度条上直接读到的物理量，
 * 倍率式的小数速度（1.14）会让进度读数出现半格这种无法对齐的中间态。
 * 整数增量让 {@code 运行耗时 = ceil(T × duration ÷ N)} 恒为整数，
 * 也让卡面能直接写「运行速度 +2」。
 *
 * <p>
 * 机器端的可观测量分别来自四条通道：{@code 实际 FE/t = P × power × B}、
 * {@code 运行耗时 = T × duration ÷ N}，其中 {@code N = (1 + Σspeed) × k}、
 * {@code duration = 1 + Σ(d − 1)}。一次配方的耗电是两者的乘积，
 * 不单独设系数、也不单独起名——卡面只写真正被声明的通道。
 *
 * <p>
 * 结算按 {@code 1 + Σ(k - 1)}（功率 / 耗时）、{@code Σspeed}（速度）与
 * {@code Σbatch}（并行运行）分别线性汇总，再一次性落到机器上，
 * 所以同系列叠多张不会出现乘性爆炸。
 */
public record UpgradeEffect(double power, int speed, double duration, int batch) {

    public static final UpgradeEffect NONE = new UpgradeEffect(1.0, 0, 1.0, 0);

    public UpgradeEffect {
        if (!Double.isFinite(power) || power <= 0 || speed < 0 || !Double.isFinite(duration) || duration <= 0 || batch < 0) {
            throw new IllegalArgumentException("Invalid upgrade effect: " + power + "/" + speed + "/" + duration + "/" + batch);
        }
    }

    /**
     * 提速：运行速度 +{@code add}，功率与配方耗时都不变。
     * 实际耗时按 {@code T ÷ (1 + add)} 缩短，FE/t 不变，于是每份耗电随之下降。
     */
    public static UpgradeEffect ofSpeed(int add) {
        return new UpgradeEffect(1.0, add, 1.0, 0);
    }

    /**
     * 精简工序：只缩短配方本身的长度（{@code T × k}），速度与功率都不动。
     * 每 tick 推进的格数不变，但总量变短，所以耗时同比缩短、每 tick 耗电不变、每份耗电同比下降。
     */
    public static UpgradeEffect ofDuration(double k) {
        return new UpgradeEffect(1.0, 0, k, 0);
    }

    /**
     * 节能：只降功率（FE/t），速度与配方耗时都不变。
     * 耗时不变，一次配方的耗电随之 ×k。
     */
    public static UpgradeEffect ofEfficiency(double k) {
        return new UpgradeEffect(k, 0, 1.0, 0);
    }

    /**
     * 并行：只加并行运行数，功率、速度与配方耗时都不变。
     * 每份的耗时与耗电都不变，整机吞吐 = 单条流水线 × B。
     */
    public static UpgradeEffect ofBatch(int batch) {
        return new UpgradeEffect(1.0, 0, 1.0, batch);
    }

    /**
     * 整合组件·全局倍率 k：速度 ×k、功率 ×k、并行运行 ×k，一次配方的耗电不变。
     * 速度不在这里声明——k 由机器端乘在 {@code (1 + Σspeed)} 上，这样 N 仍是自然数。
     */
    public static UpgradeEffect global(double k) {
        return new UpgradeEffect(k, 0, 1.0, 0);
    }
}
