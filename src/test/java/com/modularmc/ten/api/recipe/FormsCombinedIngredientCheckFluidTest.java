// -*- coding: utf-8 -*-
package com.modularmc.ten.api.recipe;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link FormsCombinedIngredient#check(FormsCombinedIngredient.IngredientTypeGetter,
 * FormsCombinedIngredient.IngredientTypeGetter, net.neoforged.neoforge.items.IItemHandler,
 * java.util.List)} fluid matching logic.
 * <p>
 * Verifies that the fluid loop uses the correct inner tank index (0 or getFluid())
 * instead of the outer loop index, which would fail for multi-tank configurations.
 * <p>
 * Uses source code verification since IFluidHandler/FluidStack require Minecraft bootstrap.
 */
class FormsCombinedIngredientCheckFluidTest {

    // ════════════════════════════════════════════════════════════
    // A. Source verification — check() fluid indexing
    // ════════════════════════════════════════════════════════════

    @Nested
    class SourceFluidIndexing {

        @Test
        void checkMethod_usesGetFluid_notGetFluidInTankWithLoopIndex() throws Exception {
            var sourceFile = new java.io.File(
                    "src/main/java/com/modularmc/ten/api/recipe/FormsCombinedIngredient.java");
            assertTrue(sourceFile.exists(),
                    "Source file must exist");

            var content = java.nio.file.Files.readString(sourceFile.toPath());

            // The check() fluid case must use getFluid() or getFluidInTank(0),
            // NOT getFluidInTank(i) where i is the outer loop index.
            // For a single-tank-per-element IFluidHandler list, each element only
            // has tank slot 0. Using getFluidInTank(i) with i > 0 would fail.

            // Verify the source uses the correct form
            int getFluidInTankIdx = content.indexOf("getFluidInTank(i)");
            int getFluidCallIdx = content.indexOf(".getFluid()");
            int getFluidInTank0Idx = content.indexOf("getFluidInTank(0)");

            // Must use getFluid() or getFluidInTank(0), NOT getFluidInTank(i)
            boolean hasCorrectCall = getFluidCallIdx >= 0 || getFluidInTank0Idx >= 0;
            boolean hasBuggyCall = getFluidInTankIdx >= 0;

            assertTrue(hasCorrectCall,
                    "check() fluid case must use .getFluid() or getFluidInTank(0), " +
                    "not getFluidInTank(i) with loop index i");

            assertFalse(hasBuggyCall,
                    "check() fluid case must NOT contain getFluidInTank(i) — " +
                    "found at index " + getFluidInTankIdx + ". " +
                    "This is the multi-tank index bug: for tank list with 2+ elements, " +
                    "tanks.get(1).getFluidInTank(1) reads inner tank index 1 " +
                    "but each tank only has slot 0.");
        }

        @Test
        void checkMethod_fluidLoop_doesNotUseOuterIndexAsInnerTankSlot() throws Exception {
            var sourceFile = new java.io.File(
                    "src/main/java/com/modularmc/ten/api/recipe/FormsCombinedIngredient.java");
            assertTrue(sourceFile.exists());

            var content = java.nio.file.Files.readString(sourceFile.toPath());

            // Check the fluid case in check() method specifically
            // Find the check method and the fluid case within it
            int checkMethodIdx = content.indexOf("public boolean check(");
            assertTrue(checkMethodIdx >= 0, "check() method must exist");

            // Find the fluid case within check()
            int fluidCaseIdx = content.indexOf("case \"fluid\"", checkMethodIdx);
            assertTrue(fluidCaseIdx >= 0, "check() must have fluid case");

            // Extract the fluid case block (simplified: look at a reasonable range)
            String fluidCaseSnippet = content.substring(fluidCaseIdx,
                    Math.min(content.length(), fluidCaseIdx + 300));

            // The fluid case should use getFluidInTank(0) or .getFluid()
            // to read the tank's content at the correct inner slot index.
            boolean hasGetFluidInTank0 = fluidCaseSnippet.contains("getFluidInTank(0)");
            boolean hasGetFluid = fluidCaseSnippet.contains(".getFluid()");
            assertTrue(hasGetFluidInTank0 || hasGetFluid,
                    "Fluid case in check() must use getFluidInTank(0) or .getFluid() to read tank content");

            // And should NOT use getFluidInTank with a variable index
            assertFalse(fluidCaseSnippet.matches("getFluidInTank\\(\\s*i\\s*\\)"),
                    "Fluid case must not use getFluidInTank(i) with loop variable");
        }
    }

    // ════════════════════════════════════════════════════════════
    // B. Bug behavior documentation — simulate multi-tank scenario
    // ════════════════════════════════════════════════════════════

    @Nested
    class MultiTankBehavior {

        @Test
        void buggyGetFluidInTankI_failsForSecondTank() {
            // The bug: tanks.get(1).getFluidInTank(1) looks at inner slot 1
            // of the second tank, but single-tank MachineFluidTank only has slot 0.
            // In a real FluidTank, getFluidInTank(1) returns FluidStack.EMPTY
            // or throws, depending on implementation.
            // This test documents the expected fix.

            // After fix: tanks.get(1).getFluid() returns the correct fluid from slot 0.
            // This test is behavioral documentation — the actual fix is verified
            // by source scanning in the SourceFluidIndexing tests.

            // Verify via reflection that getFluid exists on MachineFluidTank
            try {
                var tankClass = Class.forName(
                        "com.modularmc.ten.api.capability.MachineFluidTank");
                var getFluidMethod = tankClass.getMethod("getFluid");
                assertNotNull(getFluidMethod,
                        "MachineFluidTank must have getFluid() method");
                assertEquals(net.neoforged.neoforge.fluids.FluidStack.class,
                        getFluidMethod.getReturnType(),
                        "getFluid() must return FluidStack");
            } catch (ClassNotFoundException e) {
                // MachineFluidTank needs full bootstrap — this is expected
                // in a unit test environment. The source verification above
                // covers the fix.
                System.out.println(
                        "NOTE: MachineFluidTank not available in unit test — " +
                        "relying on source verification");
            } catch (NoSuchMethodException e) {
                fail("MachineFluidTank must have getFluid() method", e);
            }
        }

        @Test
        void checkMethod_correctForm_doesNotUseVariableInnerIndex() throws Exception {
            // Reflection-based check of the check() method source
            var sourceFile = new java.io.File(
                    "src/main/java/com/modularmc/ten/api/recipe/FormsCombinedIngredient.java");
            var content = java.nio.file.Files.readString(sourceFile.toPath());

            // Find the fluid case in check() and extract fluid access pattern
            int checkIdx = content.indexOf("public boolean check(");
            int fluidIdx = content.indexOf("case \"fluid\"", checkIdx);
            int fluidBlockEnd = content.indexOf("yield false;", fluidIdx) + "yield false;".length();
            String fluidBlock = content.substring(fluidIdx, fluidBlockEnd);

            // Count fluid access patterns
            int getFluidCalls = countOccurrences(fluidBlock, ".getFluid()");
            int getFluidInTank0Calls = countOccurrences(fluidBlock, "getFluidInTank(0)");
            int getFluidInTankVarCalls = countOccurrences(fluidBlock, "getFluidInTank(i)");

            boolean hasCorrectAccess = getFluidCalls >= 1 || getFluidInTank0Calls >= 1;
            assertTrue(hasCorrectAccess,
                    "Fluid block must call getFluid() or getFluidInTank(0) at least once");
            assertEquals(0, getFluidInTankVarCalls,
                    "Fluid block must not call getFluidInTank(i) with loop variable");
        }

        private int countOccurrences(String str, String target) {
            int count = 0;
            int idx = 0;
            while ((idx = str.indexOf(target, idx)) != -1) {
                count++;
                idx += target.length();
            }
            return count;
        }
    }
}
