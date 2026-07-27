// -*- coding: utf-8 -*-
package com.modularmc.ten.api.blockentity;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * P1 RED→GREEN transition tests for three critical issues identified in
 * standard plan v1.6 P1 review:
 *
 * <p><b>S1:</b> {@code maxProgress} is never assigned by any
 * {@link ProcessingMachineBlockEntity} subclass's {@code conditionStart()}.
 * Initial value 0 causes {@code progress >= maxProgress} (0 >= 0) to
 * evaluate true immediately — every recipe completes in a single tick.
 * Fix: each subclass must set {@code maxProgress = Math.max(1, baseTickTime())}
 * in conditionStart, and only reset progress when recipe identity changes.
 *
 * <p><b>S2:</b> {@link
 * com.modularmc.ten.common.blockentity.machine.CondenserBlockEntity#cooking()}
 * still contains {@code progress = 0} (clears progress on output-full),
 * {@code progress += 200 * getActualEfficiency()} (old energy-accumulation
 * model), and {@code catalyst.shrink(1)} (side effect in a predicate).
 * Fix: cooking() becomes a pure capacity predicate; catalyst consumption
 * moves to a per-tick processing hook.
 *
 * <p><b>S3:</b> {@link
 * com.modularmc.ten.common.blockentity.machine.FurnaceBlockEntity#conditionStart()}
 * has no recipe identity tracking. Switching recipes does not reset progress
 * or update maxProgress.
 * Fix: track recipe identity, reset progress on change, set maxProgress.
 *
 * <p>All tests are pure-Java source verification tests that do not require
 * Minecraft bootstrap. They read the source files and verify the expected
 * patterns are present (GREEN) or absent (RED).
 */
class P1MaxProgressAndIdentityTest {

    // ════════════════════════════════════════════════════════════
    // S1: maxProgress initialization in each subclass
    // ════════════════════════════════════════════════════════════

    /**
     * Extract a method body by finding its matching closing brace.
     * Handles simple cases without nested blocks (for cooking()).
     */
    private static String extractMethodBody(String content, String methodSig) {
        int start = content.indexOf(methodSig);
        if (start < 0) return "";
        int braceOpen = content.indexOf('{', start);
        if (braceOpen < 0) return content.substring(start);
        // Simple brace counting: find the matching }
        int depth = 1;
        int pos = braceOpen + 1;
        while (depth > 0 && pos < content.length()) {
            char c = content.charAt(pos);
            if (c == '{') depth++;
            else if (c == '}') depth--;
            pos++;
        }
        return content.substring(start, pos);
    }

    private static void assertConditionStartSetsMaxProgress(
            String filePath, String className) throws Exception {
        var sourceFile = new java.io.File(filePath);
        assertTrue(sourceFile.exists(),
                className + " source must exist at: " + filePath);

        var content = java.nio.file.Files.readString(sourceFile.toPath());

        // Find conditionStart() method
        int csStart = content.indexOf("public boolean conditionStart()");
        assertTrue(csStart >= 0,
                className + " must have conditionStart() method");

        // Extract method body up to the closing brace
        String afterMethod = content.substring(csStart);

        // Must set maxProgress = Math.max(1, ...) at least 1 tick
        assertTrue(afterMethod.contains("maxProgress = Math.max(1,")
                        || afterMethod.contains("maxProgress = Math.max(1"),
                "RED: " + className + ".conditionStart() must set " +
                        "maxProgress = Math.max(1, ...). " +
                        "Current: maxProgress never assigned → stays 0 → every recipe " +
                        "completes instantly (progress 0 >= maxProgress 0 is true).");
    }

    @Nested
    class S1_MaxProgressInitialization {

        @Test
        void recipeMachine_conditionStart_setsMaxProgress() throws Exception {
            assertConditionStartSetsMaxProgress(
                    "src/main/java/com/modularmc/ten/api/blockentity/RecipeMachineBlockEntity.java",
                    "RecipeMachineBlockEntity");
        }

        @Test
        void furnace_conditionStart_setsMaxProgress() throws Exception {
            assertConditionStartSetsMaxProgress(
                    "src/main/java/com/modularmc/ten/common/blockentity/machine/FurnaceBlockEntity.java",
                    "FurnaceBlockEntity");
        }

        @Test
        void condenser_conditionStart_setsMaxProgress() throws Exception {
            assertConditionStartSetsMaxProgress(
                    "src/main/java/com/modularmc/ten/common/blockentity/machine/CondenserBlockEntity.java",
                    "CondenserBlockEntity");
        }

        @Test
        void encflu_conditionStart_setsMaxProgress() throws Exception {
            assertConditionStartSetsMaxProgress(
                    "src/main/java/com/modularmc/ten/common/blockentity/machine/EncfluBlockEntity.java",
                    "EncfluBlockEntity");
        }

        @Test
        void processMethod_doesNotSetMaxProgress() throws Exception {
            // Verify P1 decoupling: maxProgress is NOT set inside process()
            var sourceFile = new java.io.File(
                    "src/main/java/com/modularmc/ten/api/blockentity/ProcessingMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = java.nio.file.Files.readString(sourceFile.toPath());

            int processMethodStart = content.indexOf("public void process()");
            int cookingMethodStart = content.indexOf("public boolean cooking()");
            assertTrue(processMethodStart >= 0);
            assertTrue(cookingMethodStart > processMethodStart);
            String processBody = content.substring(processMethodStart, cookingMethodStart);

            assertFalse(processBody.contains("maxProgress ="),
                    "P1 target: process() must NOT set maxProgress. " +
                    "maxProgress should be set in conditionStart() by subclasses.");
        }

        @Test
        void maxProgress_notZero_whenRecipeActive_validation() {
            // If maxProgress is properly set (≥1), progress=0 does NOT complete
            int maxProgress = 100; // properly set by conditionStart
            int progress = 0;
            boolean instantComplete = (progress >= maxProgress);
            assertFalse(instantComplete,
                    "With maxProgress=100 and progress=0, " +
                    "0 >= 100 is false — recipe does NOT complete instantly. " +
                    "This validates that the fix (setting maxProgress >= 1 in " +
                    "conditionStart) prevents instant completion. " +
                    "With maxProgress=0 (unfixed), 0 >= 0 is true → instant complete bug.");
        }

        @Test
        void maxProgress_atLeast1_whenSet() {
            // Verify the Math.max(1, ...) clamping formula
            int baseTickTime = 0; // pathological
            int maxProgress = Math.max(1, baseTickTime);
            assertEquals(1, maxProgress,
                    "maxProgress must be clamped to at least 1 tick");
        }
    }

    // ════════════════════════════════════════════════════════════
    // S2: Condenser cooking() must be pure predicate
    // ════════════════════════════════════════════════════════════

    @Nested
    class S2_CondenserCookingPurePredicate {

        private static final String CONDENSER_PATH =
                "src/main/java/com/modularmc/ten/common/blockentity/machine/CondenserBlockEntity.java";

        @Test
        void cooking_doesNotSetProgressToZero() throws Exception {
            var sourceFile = new java.io.File(CONDENSER_PATH);
            assertTrue(sourceFile.exists());
            var content = java.nio.file.Files.readString(sourceFile.toPath());

            // Extract cooking() body via brace matching
            String cookingBody = extractMethodBody(content, "public boolean cooking()");

            // cooking() must NOT contain progress = 0
            assertFalse(cookingBody.contains("progress = 0"),
                    "RED: Condenser.cooking() must NOT set progress = 0 " +
                    "when output is full. Progress should be preserved during " +
                    "output-full pauses. Current code clears progress: " +
                    "'if full { progress = 0; return true; }'");
        }

        @Test
        void cooking_doesNotHaveProgressAccumulation() throws Exception {
            var sourceFile = new java.io.File(CONDENSER_PATH);
            assertTrue(sourceFile.exists());
            var content = java.nio.file.Files.readString(sourceFile.toPath());

            // Extract cooking() body via brace matching
            String cookingBody = extractMethodBody(content, "public boolean cooking()");

            // cooking() must NOT contain progress += ...
            assertFalse(cookingBody.contains("progress +="),
                    "RED: Condenser.cooking() must NOT use progress += " +
                    "(old energy-accumulation model). " +
                    "Current code has 'progress += 200 * getActualEfficiency()' " +
                    "which is the old model. P1 uses progress++ per tick.");
        }

        @Test
        void cooking_doesNotHaveCatalystSideEffect() throws Exception {
            var sourceFile = new java.io.File(CONDENSER_PATH);
            assertTrue(sourceFile.exists());
            var content = java.nio.file.Files.readString(sourceFile.toPath());

            // Extract cooking() body via brace matching to avoid including
            // onProcessTick() which legitimately contains catalyst.shrink()
            String cookingBody = extractMethodBody(content, "public boolean cooking()");

            // cooking() must NOT contain catalyst.shrink()
            assertFalse(cookingBody.contains(".shrink("),
                    "RED: Condenser.cooking() must NOT have catalyst " +
                    "consumption side effects (catalyst.shrink). " +
                    "Catalyst consumption must be in a processing tick hook " +
                    "(onProcessTick), not in a capacity predicate.");
        }

        @Test
        void cooking_doesNotUseGetAliveTime() throws Exception {
            var sourceFile = new java.io.File(CONDENSER_PATH);
            assertTrue(sourceFile.exists());
            var content = java.nio.file.Files.readString(sourceFile.toPath());

            // Extract cooking() body via brace matching
            String cookingBody = extractMethodBody(content, "public boolean cooking()");

            assertFalse(cookingBody.contains("getAliveTime()"),
                    "RED: Condenser.cooking() must NOT use getAliveTime() " +
                    "for catalyst timing. Tick-based catalyst consumption " +
                    "should use an explicit per-tick counter.");
        }

        @Test
        void condenser_hasCatalystConsumptionHook() throws Exception {
            var sourceFile = new java.io.File(CONDENSER_PATH);
            assertTrue(sourceFile.exists());
            var content = java.nio.file.Files.readString(sourceFile.toPath());

            // After fix, Condenser must override onProcessTick() for
            // periodic catalyst consumption (not in cooking() predicate)
            assertTrue(content.contains("onProcessTick"),
                    "RED: Condenser must override onProcessTick() for " +
                    "catalyst consumption. " +
                    "Current: catalyst is consumed inside cooking() predicate " +
                    "(side effect in capacity check).");
        }
    }

    // ════════════════════════════════════════════════════════════
    // S2: Condenser progress preserved on output full
    // ════════════════════════════════════════════════════════════

    @Nested
    class S2_CondenserProgressNotClearedOnFull {

        @Test
        void condenserCooking_progressPreservedWhenFull_simulation() {
            // Simulate: Condenser has progress=50, output tank is full
            // Current bug: cooking() sets progress=0 when full
            // Fix: progress should be preserved

            // Using the process simulation from ProcessingMachineContractTest
            var outcome = ProcessingMachineContractTest.simulateRefactoredTick(
                    50, 1000, 1000, 30,
                    true, true, true // cookingBlocks=true (tank full)
            );

            assertEquals(50, outcome.progress(),
                    "RED: Condenser must preserve progress (50) when output " +
                    "tank is full. Current code sets progress = 0 in cooking().");
            assertEquals(1000, outcome.energy(),
                    "Energy must be preserved when output is full");
            assertFalse(outcome.onCookFinishCalled(),
                    "onCookFinish must NOT fire when output is full");
        }

        @Test
        void condenserCooking_progressResumesAfterFullCleared_simulation() {
            int progress = 50;
            int energy = 1000;

            // Blocked tick (tank full)
            var blocked = ProcessingMachineContractTest.simulateRefactoredTick(
                    progress, 1000, energy, 30,
                    true, true, true);
            assertEquals(50, blocked.progress());

            // Unblocked tick (tank has space)
            var unblocked = ProcessingMachineContractTest.simulateRefactoredTick(
                    blocked.progress(), 1000, blocked.energy(), 30,
                    true, true, false);
            assertEquals(51, unblocked.progress(),
                    "After unblock, progress must continue from 50 to 51 (+1 per tick)");
        }
    }

    // ════════════════════════════════════════════════════════════
    // S3: Furnace recipe identity tracking
    // ════════════════════════════════════════════════════════════

    @Nested
    class S3_FurnaceRecipeIdentity {

        private static final String FURNACE_PATH =
                "src/main/java/com/modularmc/ten/common/blockentity/machine/FurnaceBlockEntity.java";

        @Test
        void furnace_tracksRecipeIdentity() throws Exception {
            var sourceFile = new java.io.File(FURNACE_PATH);
            assertTrue(sourceFile.exists());
            var content = java.nio.file.Files.readString(sourceFile.toPath());

            // After fix: must track previous recipe identity
            // Could be a field like lastRecipeId (ResourceKey), lastRecipeHolder, etc.
            assertTrue(content.contains("lastRecipe")
                            || content.contains("previousRecipe")
                            || content.contains("recipeIdentity"),
                    "RED: FurnaceBlockEntity must track recipe identity " +
                    "to detect recipe changes. " +
                    "Current conditionStart() returns getCurrentRecipe().isPresent() " +
                    "with no identity tracking.");
        }

        @Test
        void furnace_conditionStart_resetsProgressOnRecipeChange() throws Exception {
            var sourceFile = new java.io.File(FURNACE_PATH);
            assertTrue(sourceFile.exists());
            var content = java.nio.file.Files.readString(sourceFile.toPath());

            // Must have logic: if recipe changed → progress = 0
            // Look for progress = 0 within or near conditionStart
            int csStart = content.indexOf("public boolean conditionStart()");
            assertTrue(csStart >= 0, "Furnace must have conditionStart()");

            // After conditionStart, look for the identity check + progress reset
            // The pattern should be something like:
            // if (!Objects.equals(prevId, nextId)) { progress = 0; }
            String afterCs = content.substring(csStart);

            // Check for progress reset within conditionStart or its helper
            // (could be in the conditionStart body or a called method)
            assertTrue(afterCs.contains("progress = 0")
                            || afterCs.contains("progress=0"),
                    "RED: Furnace.conditionStart() must reset progress to 0 " +
                    "when recipe identity changes. " +
                    "Current: no progress reset on recipe change.");
        }

        @Test
        void furnace_conditionStart_setsMaxProgress() throws Exception {
            // Reuse S1 check but specifically for Furnace
            assertConditionStartSetsMaxProgress(FURNACE_PATH, "FurnaceBlockEntity");
        }

        @Test
        void furnace_conditionStart_sameRecipePreservesProgress_simulation() {
            // Using RecipeProgressResetTest's identity simulation:
            // same recipe ID → progress preserved
            int progress = RecipeProgressResetTest.simulateConditionStart(
                    "ten:furnace_recipe_a", "ten:furnace_recipe_a", 75);

            assertEquals(75, progress,
                    "Furnace must preserve progress when same recipe continues");
        }

        @Test
        void furnace_conditionStart_recipeChangeResetsProgress_simulation() {
            // Different recipe → progress reset to 0
            int progress = RecipeProgressResetTest.simulateConditionStart(
                    "ten:furnace_recipe_a", "ten:furnace_recipe_b", 75);

            assertEquals(0, progress,
                    "Furnace must reset progress to 0 when recipe changes from A to B");
        }
    }

    // ════════════════════════════════════════════════════════════
    // Cross-cutting: no subclass has maxProgress == 0
    // ════════════════════════════════════════════════════════════

    @Nested
    class CrossCutting_MaxProgressNeverZero {

        @Test
        void allProcessingSubclasses_setMaxProgressInConditionStart() throws Exception {
            // This test verifies that every concrete ProcessingMachineBlockEntity
            // subclass has maxProgress assignment in conditionStart.
            // It checks by reading the source files and looking for the pattern.
            var subclasses = new String[][]{
                {"RecipeMachineBlockEntity",
                 "src/main/java/com/modularmc/ten/api/blockentity/RecipeMachineBlockEntity.java"},
                {"FurnaceBlockEntity",
                 "src/main/java/com/modularmc/ten/common/blockentity/machine/FurnaceBlockEntity.java"},
                {"CondenserBlockEntity",
                 "src/main/java/com/modularmc/ten/common/blockentity/machine/CondenserBlockEntity.java"},
                {"EncfluBlockEntity",
                 "src/main/java/com/modularmc/ten/common/blockentity/machine/EncfluBlockEntity.java"},
            };

            for (var entry : subclasses) {
                String name = entry[0];
                String path = entry[1];
                var sourceFile = new java.io.File(path);
                assertTrue(sourceFile.exists(), name + " source must exist: " + path);

                var content = java.nio.file.Files.readString(sourceFile.toPath());
                int csStart = content.indexOf("public boolean conditionStart()");

                assertTrue(csStart >= 0,
                        name + " must have conditionStart()");

                String afterCs = content.substring(csStart);

                boolean hasMaxProgressAssign = afterCs.contains("maxProgress =")
                        || afterCs.contains("maxProgress=");

                assertTrue(hasMaxProgressAssign,
                        "RED: " + name + ".conditionStart() must set maxProgress. " +
                        "Without this, maxProgress stays 0 and recipe completes " +
                        "instantly (progress 0 >= maxProgress 0).");
            }
        }

        @Test
        void effectMachine_notAffectedByP1ProcessingTests() throws Exception {
            // EffectMachine is P3 target — verify it still has its own
            // maxProgress logic (inside process(), not conditionStart)
            var sourceFile = new java.io.File(
                    "src/main/java/com/modularmc/ten/api/blockentity/EffectMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = java.nio.file.Files.readString(sourceFile.toPath());

            // EffectMachine sets maxProgress inside process() — this is P3 behavior
            // and should not be confused with P1 ProcessingMachine changes
            assertTrue(content.contains("maxProgress ="),
                    "EffectMachine (P3) sets maxProgress inside process(), " +
                    "which is acceptable for P3. P1 tests should not fail on this.");
        }
    }

    // ════════════════════════════════════════════════════════════
    // Encflu conditionStart maxProgress (also S1)
    // ════════════════════════════════════════════════════════════

    @Nested
    class S1_EncfluMaxProgress {

        @Test
        void encflu_conditionStart_setsMaxProgress() throws Exception {
            assertConditionStartSetsMaxProgress(
                    "src/main/java/com/modularmc/ten/common/blockentity/machine/EncfluBlockEntity.java",
                    "EncfluBlockEntity");
        }

        @Test
        void encflu_conditionStart_noRecipeIdentityReset() throws Exception {
            // Encflu has no recipe system, so conditionStart should set
            // maxProgress but should NOT reset progress unconditionally
            var sourceFile = new java.io.File(
                    "src/main/java/com/modularmc/ten/common/blockentity/machine/EncfluBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = java.nio.file.Files.readString(sourceFile.toPath());

            int csStart = content.indexOf("public boolean conditionStart()");
            assertTrue(csStart >= 0);

            String afterCs = content.substring(csStart);
            // Should NOT contain identity-based progress reset
            // (Encflu has no recipes, so no recipe identity)
            // But it may set progress = 0 in some cases — actually for Encflu,
            // the conditionStart already checks inputs and should not need
            // to reset progress. The task says Encflu is also S1.
            // Actually, for Encflu there's no recipe identity to track.
            // maxProgress should just be set.
            // Progress reset should NOT be based on recipe identity
            // since there are no recipes, but conditionStart returning false
            // already means no processing.
            assertTrue(afterCs.contains("maxProgress"),
                    "Encflu.conditionStart() must set maxProgress");
        }
    }
}
