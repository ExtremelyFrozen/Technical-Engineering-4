package com.modularmc.ten.common.blockentity;

import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the new pure query methods in {@link MatchFuel}.
 * <p>
 * The non-null Level requirement for {@code getExtractorFuelValue} means
 * it cannot be tested outside a running game environment (requires a real
 * Level instance with FuelValues). Empty-stack and Metalizer/Biomass tests
 * are verifiable here.
 */
class MatchFuelQueryTest {

    @Test
    void getMetalFuelValue_emptyStack_shouldReturnZero() {
        int value = MatchFuel.getMetalFuelValue(ItemStack.EMPTY);
        assertEquals(0, value);
    }

    @Test
    void getBiomassFuelValue_emptyStack_shouldReturnZero() {
        int value = MatchFuel.getBiomassFuelValue(ItemStack.EMPTY);
        assertEquals(0, value);
    }

    @Test
    void matchMetal_consumerDelegatesToQuery_emptyStack() {
        int value = MatchFuel.matchMetal(ItemStack.EMPTY, true);
        assertEquals(0, value);
    }

    @Test
    void matchPlant_consumerDelegatesToQuery_emptyStack() {
        int value = MatchFuel.matchPlant(ItemStack.EMPTY, true);
        assertEquals(0, value);
    }
}
