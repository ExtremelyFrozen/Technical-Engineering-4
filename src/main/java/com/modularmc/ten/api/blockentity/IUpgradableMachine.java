package com.modularmc.ten.api.blockentity;

/**
 * 可升级机器 SPI：升级物品（UpgradeItem/Levelup*）经此接口向机器施加效果。
 * <p>
 * 旧百分比接口 {@code onUpgradeApply} 保留兼容；乘法模型 API 采用乘子叠加
 * （total = Π factor_i），并行运行增量直接累加。未实现的升级钩子使用空默认实现，
 * 兼容只实现部分接口的机器。
 * 仅 {@link CmMachineBlockEntity} 实现。
 */
public interface IUpgradableMachine {

    boolean onUpgradeApply(double percent, int slotIncrease);

    boolean isType(String type);

    int getCurrentRadius();

    void setCurrentRadius(int radius);

    int getInitialRadius();

    // ───── 乘法模型 API ─────

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
     * 范围升级（{@code LevelupRg}）：把可调节半径上限按倍率放大。
     * 只有接入范围调节面板的机器需要实现；其它机器保持默认 no-op，
     * 其范围仍由 {@link #setCurrentRadius(int)} 语义驱动。
     *
     * @param factor 正倍率（当前实现为每件 ×2）
     */
    default void applyAdjustRangeCapMultiplier(int factor) {}

    /**
     * 该机器是否接入范围调节面板（面板的 XYZ 盒就是它的工作区）。
     * 范围升级以它作为安装门禁：接面板的机器装得上，其余机器（半径已不驱动工作区）装不上。
     */
    default boolean supportsRangeAdjust() {
        return false;
    }

    /**
     * Mark that a Photosynthetic Suppressor (LevelupSyn) is installed.
     * Duplicate calls are silently ignored (safe idempotency).
     * <p>
     * Only implemented on {@link com.modularmc.ten.api.blockentity.CmMachineBlockEntity}.
     * Default no-op for backward compatibility.
     */
    default void applyPhotosyn() {}

    /**
     * 该机器是否真的消费并行运行（`getLockedBatchSize()` / 每 lane 的 `laneB`）。
     * <p>
     * 判定依据是机器循环里有没有按 B 结算，而不是机型名字：加工 / 配方 / 效果
     * 三类基类读 `lockedB`，熔炼机三条 lane 各读自己的 `laneB`，都返回 true；
     * 光伏引擎没有并行运行通道，返回 false。
     * 并行运行升级只放行返回 true 的机器，否则装上只会白占槽位。
     *
     * @return true 表示并行运行对本机有实际产出效果
     */
    default boolean supportsBatch() {
        return false;
    }

    /**
     * 该机器是否消费「副产物」维度（配方里 {@code chance < 1} 的概率输出）。
     * <p>
     * 粉碎机返回 true；副产物压榨 / 副产物筛选两张独特组件以此放行，
     * 避免把机型字符串塞进 {@code isType} 的分支表。
     */
    default boolean supportsByproductTuning() {
        return false;
    }

    /**
     * 该机器是否有「收获」动作（农场管理器返回 true）。
     * 奇异催生 / 幼芽复种两张独特组件以此放行。
     */
    default boolean supportsHarvestSpecialization() {
        return false;
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
    default void setRecipeMode(int mode) {}

    /**
     * @return the current recipe mode (default: {@link #RECIPE_MODE_SMELTING})
     */
    default int getRecipeMode() {
        return RECIPE_MODE_SMELTING;
    }

    // ───── 传输升级（Stream）兼容标记 ─────

    /**
     * 记录「星辉能量传输」升级是否安装的兼容标记。
     * <p>
     * 机器能量传输当前不限速，该标记不改变行为。默认空实现用于兼容未覆盖此钩子的机器；
     * 仅 {@link com.modularmc.ten.api.blockentity.CmMachineBlockEntity} 保存该状态。
     */
    default void setUnlimitedEnergyTransfer(boolean unlimited) {}

    /**
     * 返回「星辉能量传输」兼容标记是否已设置；能量传输始终不限速。
     */
    default boolean hasUnlimitedEnergyTransfer() {
        return false;
    }
}
