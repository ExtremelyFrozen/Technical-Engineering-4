package com.modularmc.ten.api.option;

/**
 * 冷却剂数据：定义一种冷却剂的三项行为参数。
 * <p>
 * 冷却器消耗冷却剂后，按 {@code intervalSeconds} 秒为间隔触发冷却效果，
 * 每次减少正面机器的 {@code reductionTicks} tick 工作耗时，
 * 同一份冷却剂共可作用 {@code uses} 次（耗尽后消耗一个物品并重置）。
 *
 * @param intervalSeconds 冷却触发间隔（秒）。由冷却器 {@code effectInterval()} 消费。
 * @param reductionTicks  每次触发减少的工作耗时（tick，1 秒 = 20 tick）。
 * @param uses            一份冷却剂可作用的次数。
 */
public record Coolant(int intervalSeconds, int reductionTicks, int uses) {

    public Coolant {
        if (intervalSeconds <= 0) {
            throw new IllegalArgumentException("intervalSeconds must be > 0, got " + intervalSeconds);
        }
        if (reductionTicks <= 0) {
            throw new IllegalArgumentException("reductionTicks must be > 0, got " + reductionTicks);
        }
        if (uses <= 0) {
            throw new IllegalArgumentException("uses must be > 0, got " + uses);
        }
    }
}