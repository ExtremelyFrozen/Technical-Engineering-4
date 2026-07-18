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
 * <p>
 * Count is always 1 (defensive copy). {@link #ingredients()} returns a
 * freshly copied list with freshly copied stacks on each call.
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
        // Defensive copy of the item stack
        ItemStack copy = stack.copy();
        copy.setCount(count);
        this.ingredients = Collections.singletonList(copy);
    }

    /**
     * Package-private constructor for test-only use.
     * Accepts a pre-built ingredient list instead of a raw ItemStack,
     * allowing tests to bypass Minecraft registry initialization.
     * Makes defensive copies of all caller-supplied ItemStacks at
     * construction time (count applied), then stores immutably.
     */
    EngineFuelRecipe(List<ItemStack> ingredients, int fuelBudget, int baseRate, int count) {
        if (ingredients == null || ingredients.isEmpty()) throw new IllegalArgumentException("ingredients must be non-empty");
        if (fuelBudget <= 0) throw new IllegalArgumentException("fuelBudget must be positive: " + fuelBudget);
        if (baseRate <= 0) throw new IllegalArgumentException("baseRate must be positive: " + baseRate);
        if (count <= 0) throw new IllegalArgumentException("count must be positive: " + count);
        this.fuelBudget = fuelBudget;
        this.baseRate = baseRate;
        this.count = count;
        // Defensive copy: copy each caller-supplied stack (EMPTY is immutable
        // and copy() returns itself, so skip mutation for empty stacks) and
        // apply count, then wrap in unmodifiable list for double protection.
        var copied = ingredients.stream()
                .map(ItemStack::copy)
                .peek(s -> { if (!s.isEmpty()) s.setCount(count); })
                .toList();
        this.ingredients = Collections.unmodifiableList(copied);
    }

    /** Fuel budget in FE-equivalent units (from MatchFuel). */
    public int fuelBudget() {
        return fuelBudget;
    }

    /** Base generation rate in FE/t. */
    public int baseRate() {
        return baseRate;
    }

    /** Item count (always 1 for JEI display). */
    public int count() {
        return count;
    }

    /** Number of ticks the fuel lasts: ceil(fuelBudget / baseRate). */
    public int durationTicks() {
        return (int) Math.ceil((double) fuelBudget / (double) baseRate);
    }

    /**
     * Total base energy generated over the full burn: durationTicks * baseRate.
     * Because duration uses ceil division, the last tick may not run for a full
     * tick-worth of fuel consumption — the engine emits baseRate FE every tick
     * regardless, so total FE produced equals durationTicks * baseRate.
     */
    public int baseGeneratedEnergy() {
        return durationTicks() * baseRate;
    }

    /**
     * Fuel item stack(s) for JEI slot display.
     * Returns a freshly copied list with freshly copied stacks each call,
     * so external mutation via {@link ItemStack#grow} or {@link ItemStack#shrink}
     * cannot affect the recipe.
     */
    public List<ItemStack> ingredients() {
        return new ArrayList<>(ingredients.stream().map(ItemStack::copy).toList());
    }
}
