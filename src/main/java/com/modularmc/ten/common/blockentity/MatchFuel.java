package com.modularmc.ten.common.blockentity;

/**
 * 生产端燃料的共用数据结构。
 *
 * <p>
 * 自 2026-11 起，具体燃料表已移出代码，改由数据包配方
 * {@code kenergyengineering:engine_fuel} 定义（见 {@code EngineFuelRecipe}），
 * 本类只保留「一份燃料」的值对象与派生公式，不再有任何写死的物品 / 标签表。
 */
public final class MatchFuel {

    /**
     * 一份燃料的成分：实际产出率、持续时间与 FE/t 修正倍率。
     * <p>
     * {@code rate = round(引擎额定功率 × rateMultiplier)} 是配方的实际 FE/t；
     * {@code durationTicks} 是配方直接声明的持续时间，总能量 = {@code rate × durationTicks}。
     * 升级抬高产出率后，持续时间按倍率缩短、总能量不变。
     */
    public record FuelProfile(int rate, int durationTicks, double rateMultiplier) {

        public static final FuelProfile EMPTY = new FuelProfile(0, 0, 1.0);

        /**
         * 由「燃料总能量预算 + 引擎额定功率 + 修正倍率」排出实际费率与持续时间。
         * 仅用于兼容兜底（原版燃烧时间 → 总能量），数据包配方直接给 duration。
         *
         * @param budget         一份燃料的总能量（FE）
         * @param engineRate     引擎额定 FE/t
         * @param rateMultiplier 配方对额定功率的修正倍率
         */
        public static FuelProfile of(int budget, int engineRate, double rateMultiplier) {
            int rate = Math.max(1, (int) Math.round(engineRate * rateMultiplier));
            int ticks = Math.max(1, (budget + rate - 1) / rate);
            return new FuelProfile(rate, ticks, rateMultiplier);
        }

        /** 一份燃料的总能量（FE）。 */
        public int budget() {
            return rate * durationTicks;
        }

        public boolean isEmpty() {
            return rate <= 0 || durationTicks <= 0;
        }
    }

    private MatchFuel() {}
}
