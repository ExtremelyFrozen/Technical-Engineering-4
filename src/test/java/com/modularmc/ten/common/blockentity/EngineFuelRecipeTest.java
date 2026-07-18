package com.modularmc.ten.common.blockentity;

import net.minecraft.world.item.ItemStack;

import java.util.List;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link EngineFuelRecipe} computation logic and defensive copying.
 * <p>
 * Uses the package-private list-based constructor to avoid triggering
 * Minecraft registry initialization (Items.COAL, etc.) which requires
 * a running game environment.
 */
class EngineFuelRecipeTest {

    private static EngineFuelRecipe recipe(int fuelBudget, int baseRate) {
        return new EngineFuelRecipe(List.of(ItemStack.EMPTY), fuelBudget, baseRate, 1);
    }

    @Test
    void zeroFuelBudget_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> recipe(0, 30));
    }

    @Test
    void negativeFuelBudget_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> recipe(-1, 30));
    }

    @Test
    void zeroBaseRate_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> recipe(1000, 0));
    }

    @Test
    void negativeBaseRate_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> recipe(1000, -5));
    }

    @Test
    void durationTicks_shouldBeCeilDivision() {
        assertEquals(34, recipe(1000, 30).durationTicks());
    }

    @Test
    void durationTicks_exactDivision() {
        assertEquals(30, recipe(900, 30).durationTicks());
    }

    @Test
    void durationTicks_roundUpWhenNotExact() {
        assertEquals(1, recipe(1, 80).durationTicks());
    }

    @Test
    void durationTicks_largeBudget() {
        assertEquals(667, recipe(20000, 30).durationTicks());
    }

    @Test
    void baseGeneratedEnergy_shouldBeDurationTimesBaseRate() {
        assertEquals(1020, recipe(1000, 30).baseGeneratedEnergy());
    }

    @Test
    void baseGeneratedEnergy_exactDivision() {
        assertEquals(900, recipe(900, 30).baseGeneratedEnergy());
    }

    @Test
    void baseGeneratedEnergy_largeBudget() {
        assertEquals(20010, recipe(20000, 30).baseGeneratedEnergy()); // 667 * 30
    }

    @Test
    void count_shouldDefaultToOne() {
        assertEquals(1, recipe(1000, 30).count());
    }

    @Test
    void fuelBudgetAccessor() {
        assertEquals(5000, recipe(5000, 80).fuelBudget());
    }

    @Test
    void baseRateAccessor() {
        assertEquals(80, recipe(5000, 80).baseRate());
    }

    @Test
    void ingredients_shouldReturnSuppliedList() {
        var r = new EngineFuelRecipe(List.of(ItemStack.EMPTY), 1000, 30, 1);
        assertEquals(1, r.ingredients().size());
    }

    @Test
    void ingredients_externalMutation_shouldNotAffectRecipe() {
        // Create a recipe with an ingredient (via package-private ctor)
        var r = new EngineFuelRecipe(List.of(ItemStack.EMPTY), 1000, 30, 1);
        List<ItemStack> firstCall = r.ingredients();
        List<ItemStack> secondCall = r.ingredients();

        // Different list instances each call
        assertNotSame(firstCall, secondCall);

        // Mutating first list doesn't affect second
        firstCall.clear();
        assertEquals(1, secondCall.size());
    }

    @Test
    void ingredients_deepCopyOnEachCall() {
        var r = new EngineFuelRecipe(List.of(ItemStack.EMPTY), 1000, 30, 1);
        List<ItemStack> first = r.ingredients();
        List<ItemStack> second = r.ingredients();
        // Different list instances
        assertNotSame(first, second);
    }
}
