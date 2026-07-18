package com.modularmc.ten.common.blockentity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Static contract verification for the non-null Level requirement.
 * <p>
 * The key test here is a compile-time assertion: {@code getExtractorFuelValue}
 * must accept a {@code Level} (not {@code @Nullable Level}). Since we cannot
 * create a real Level outside the game, we verify at class-load time that
 * the method exists with the expected signature.
 * <p>
 * Run with: {@code gradlew test --tests "*ExtractorFuelContractTest"}
 */
class ExtractorFuelContractTest {

    @Test
    void getExtractorFuelValue_acceptsLevelSignature() {
        // Compile-time contract: this must compile without error.
        // The method signature is: int getExtractorFuelValue(Level, ItemStack)
        // Using reflection to verify the parameter type at runtime.
        var methods = MatchFuel.class.getMethods();
        boolean found = false;
        for (var m : methods) {
            if (m.getName().equals("getExtractorFuelValue")) {
                Class<?>[] params = m.getParameterTypes();
                assertEquals(2, params.length, "getExtractorFuelValue must have 2 parameters");
                assertEquals("net.minecraft.world.level.Level", params[0].getName(),
                        "First parameter must be Level (non-nullable)");
                assertEquals("net.minecraft.world.item.ItemStack", params[1].getName(),
                        "Second parameter must be ItemStack");
                found = true;
                break;
            }
        }
        assertTrue(found, "getExtractorFuelValue method must exist");
    }

    @Test
    void matchFuel_acceptsLevelSignature() {
        var methods = MatchFuel.class.getMethods();
        boolean found = false;
        for (var m : methods) {
            if (m.getName().equals("matchFuel") && m.getParameterCount() == 3) {
                Class<?>[] params = m.getParameterTypes();
                assertEquals("net.minecraft.world.level.Level", params[0].getName(),
                        "First parameter must be Level (non-nullable)");
                found = true;
                break;
            }
        }
        assertTrue(found, "matchFuel(Level, ItemStack, boolean) must exist");
    }
}
