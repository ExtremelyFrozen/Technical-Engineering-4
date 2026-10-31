package com.modularmc.ten.api.option;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 冷却器在配方查看器里使用的展示条目（无 RecipeType，形状同 {@code EngineFuelRecipe}）。
 * <p>
 * 一份冷却剂物品 + 它的三档数值。数值本身来自 {@link Coolants} 的标签查询，
 * 这里只做查看器需要的派生量（周期 tick、累计推进量、总时长）。
 */
public final class CoolantRecipe {

    private final List<ItemStack> ingredients;
    private final Coolant coolant;

    public CoolantRecipe(ItemStack stack, Coolant coolant) {
        if (stack == null || stack.isEmpty()) {
            throw new IllegalArgumentException("stack must be non-empty");
        }
        if (coolant == null) {
            throw new IllegalArgumentException("coolant must not be null");
        }
        ItemStack copy = stack.copy();
        copy.setCount(1);
        this.ingredients = List.of(copy);
        this.coolant = coolant;
    }

    public Coolant coolant() {
        return coolant;
    }

    /** 一次冷却周期时长（tick）：intervalSeconds × 20。 */
    public int intervalTicks() {
        return coolant.intervalSeconds() * 20;
    }

    /** 每次周期推进目标机器的 tick 数。 */
    public int reductionTicks() {
        return coolant.reductionTicks();
    }

    /** 一份冷却剂可作用的次数。 */
    public int uses() {
        return coolant.uses();
    }

    /** 一份冷却剂累计推进目标机器的 tick 数：reductionTicks × uses。 */
    public int totalReductionTicks() {
        return coolant.reductionTicks() * coolant.uses();
    }

    /** 一份冷却剂从放入到耗尽的总时长（tick）：intervalTicks × uses。 */
    public int lifetimeTicks() {
        return intervalTicks() * uses();
    }

    /** 返回新复制的物品列表。 */
    public List<ItemStack> ingredients() {
        return ingredients.stream().map(ItemStack::copy).toList();
    }
}
