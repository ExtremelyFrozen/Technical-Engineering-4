package com.modularmc.ten.common.blockentity;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Shared recipe type for Extractor, Metalizer, and Biomass engine fuel in JEI.
 * <p>
 * Each instance represents a single item fuel source with its fuel budget
 * (as returned by {@link MatchFuel} query methods) and the engine's base
 * generation rate (FE/t).
 * <p>
 * Duration (ticks) is computed as {@code ceil(fuelBudget / baseRate)}.
 * Total base energy = {@code durationTicks * baseRate}; note that the last
 * tick may only produce a fraction of baseRate if the fuel budget is not
 * an exact multiple of baseRate — the ceil guarantees at least the full
 * budget is covered.
 */
public final class EngineFuelRecipe {

    private final int fuelBudget;
    private final int baseRate;
    private final int count;
    private final List<ItemStack> ingredients;

    public EngineFuelRecipe(ItemStack stack, int fuelBudget, int baseRate) {
        this(stack, fuelBudget, baseRate, 1);
    }

    public EngineFuelRecipe(ItemStack stack, int fuelBudget, int baseRate, int count) {
        if (stack == null || stack.isEmpty()) throw new IllegalArgumentException("stack must be non-empty");
        if (fuelBudget <= 0) throw new IllegalArgumentException("fuelBudget must be positive: " + fuelBudget);
        if (baseRate <= 0) throw new IllegalArgumentException("baseRate must be positive: " + baseRate);
        if (count <= 0) throw new IllegalArgumentException("count must be positive: " + count);
        this.fuelBudget = fuelBudget;
        this.baseRate = baseRate;
        this.count = count;
        ItemStack copy = stack.copy();
        copy.setCount(count);
        this.ingredients = Collections.singletonList(copy);
    }

    /** Fuel budget in FE-equivalent units (from MatchFuel). */
    public int fuelBudget() { return fuelBudget; }

    /** Base generation rate in FE/t for the engine consuming this fuel. */
    public int baseRate() { return baseRate; }

    /** Duration in ticks: {@code ceil(fuelBudget / baseRate)}. */
    public int durationTicks() {
        return (fuelBudget + baseRate - 1) / baseRate;
    }

    /** Total energy generated over the full duration: {@code durationTicks * baseRate}. */
    public int baseGeneratedEnergy() {
        return durationTicks() * baseRate;
    }

    /** Number of items consumed (always 1). */
    public int count() { return count; }

    /**
     * Returns a freshly copied list of ingredient stacks.
     */
    public List<ItemStack> ingredients() {
        List<ItemStack> result = new ArrayList<>(ingredients.size());
        for (ItemStack s : ingredients) {
            result.add(s.copy());
        }
        return result;
    }
}