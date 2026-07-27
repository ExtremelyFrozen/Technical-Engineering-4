// -*- coding: utf-8 -*-
package com.modularmc.ten.api.blockentity;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract tests for {@link
 * com.modularmc.ten.common.blockentity.machine.FurnaceBlockEntity#cooking()}
 * return value semantics.
 * <p>
 * Verifies the RED→GREEN transition: when the output slot contains a different
 * item than the recipe result, cooking() must return {@code true} (block),
 * preventing further progress and energy consumption. The current RED-phase
 * simulation reproduces the P2 bug (returns {@code false} → allows progress
 * on blocked output); the GREEN fix changes that branch to {@code true}.
 * <p>
 * Also verifies the same-item full-stack case is correctly blocked, and the
 * process() step respects cooking() return values (no progress, no energy
 * waste on blocked ticks).
 * <p>
 * Uses pure-Java simulation since FurnaceBlockEntity requires Minecraft bootstrap.
 */
class FurnaceCookingContractTest {

    // ════════════════════════════════════════════════════════════
    // Simulated item representation (Minecraft-free)
    // ════════════════════════════════════════════════════════════

    /** Sentinel for empty/no item. */
    static final int NO_ITEM = 0;
    static final int ITEM_INGOT = 1;
    static final int ITEM_ORE = 2;
    static final int ITEM_DUST = 3;
    static final int MAX_STACK = 64;

    /**
     * Minimal item stack, mirroring the subset of
     * {@link net.minecraft.world.item.ItemStack} used by cooking().
     */
    record SimStack(int item, int count, int maxStack) {
        static SimStack empty() {
            return new SimStack(NO_ITEM, 0, MAX_STACK);
        }
        static SimStack of(int item, int count) {
            return new SimStack(item, count, MAX_STACK);
        }
        boolean isEmpty() {
            return item == NO_ITEM || count <= 0;
        }
    }

    /**
     * Simulates FurnaceBlockEntity.cooking().
     * <p>
     * GREEN (fixed): returns {@code true} when output has a different item,
     * blocking progress and energy consumption. The RED-phase bug returned
     * {@code false} (allow progress), causing an infinite energy-draining cycle
     * where onCookFinish would exit without producing output.
     *
     * @param output the current output slot stack
     * @param result the recipe result stack (what would be produced)
     * @return {@code true} if the machine should be blocked (output busy),
     *         {@code false} if it can proceed with processing
     */
    static boolean simulateCooking(SimStack output, SimStack result) {
        if (output.isEmpty()) return false;          // empty → proceed
        if (output.item() != result.item()) return true;  // GREEN: different → block (was BUGGY false)
        return output.count() + result.count() > output.maxStack(); // full → block
    }

    // ════════════════════════════════════════════════════════════
    // A. Empty output — can process
    // ════════════════════════════════════════════════════════════

    @Nested
    class EmptyOutput {

        @Test
        void emptyOutput_allowsProcessing() {
            assertFalse(simulateCooking(SimStack.empty(), SimStack.of(ITEM_INGOT, 1)),
                    "Empty output: cooking() must return false (can process)");
        }

        @Test
        void emptyOutput_evenWhenResultWouldBeLarge() {
            assertFalse(simulateCooking(SimStack.empty(), SimStack.of(ITEM_DUST, 5)),
                    "Empty output always allows processing regardless of result size");
        }
    }

    // ════════════════════════════════════════════════════════════
    // B. Same item with space — can process
    // ════════════════════════════════════════════════════════════

    @Nested
    class SameItemHasSpace {

        @Test
        void sameItemPartialStack_allowsProcessing() {
            assertFalse(simulateCooking(
                    SimStack.of(ITEM_INGOT, 32),
                    SimStack.of(ITEM_INGOT, 1)),
                    "Same item with space: cooking() must return false (can process)");
        }

        @Test
        void sameItemExactFit_allowsProcessing() {
            assertFalse(simulateCooking(
                    SimStack.of(ITEM_INGOT, 63),
                    SimStack.of(ITEM_INGOT, 1)),
                    "Same item exact fit 63+1=64: cooking() must return false (can process)");
        }

        @Test
        void sameItemLargeResultWithSpace() {
            assertFalse(simulateCooking(
                    SimStack.of(ITEM_INGOT, 10),
                    SimStack.of(ITEM_INGOT, 5)),
                    "Same item with room for multiple: cooking() returns false");
        }
    }

    // ════════════════════════════════════════════════════════════
    // C. Different item in output — MUST BLOCK (P2 BUG FIX)
    // ════════════════════════════════════════════════════════════
    //
    // This is the core P2 fix: when the output slot has a different item
    // than the recipe result, cooking() MUST return true to prevent
    // energy consumption and phantom progress.
    //
    // The current (BUGGY) code returns false here, causing the machine to
    // consume energy each tick, advance progress, then onCookFinish() exits
    // without producing output — an infinite energy-draining cycle.
    //
    // RED phase: these tests FAIL because simulateCooking has the same bug.
    // GREEN phase: simulateCooking fixed → tests pass.
    //
    // ════════════════════════════════════════════════════════════

    @Nested
    class DifferentItemBlocks {

        @Test
        void differentItem_blocksProcessing() {
            assertTrue(simulateCooking(
                    SimStack.of(ITEM_ORE, 1),     // output has ORE
                    SimStack.of(ITEM_INGOT, 1)),   // recipe produces INGOT
                    "P2 FIX: Different item in output must return true (block), " +
                    "currently BUGGY returns false (allows progress)");
        }

        @Test
        void differentItem_largeOutputStillBlocks() {
            assertTrue(simulateCooking(
                    SimStack.of(ITEM_DUST, 64),
                    SimStack.of(ITEM_INGOT, 1)),
                    "Different item regardless of count must block");
        }

        @Test
        void differentItem_sameCountDifferentType() {
            assertTrue(simulateCooking(
                    SimStack.of(ITEM_ORE, 1),
                    SimStack.of(ITEM_DUST, 1)),
                    "Same count but different type must block");
        }
    }

    // ════════════════════════════════════════════════════════════
    // D. Same item full stack — MUST BLOCK
    // ════════════════════════════════════════════════════════════

    @Nested
    class SameItemFullStack {

        @Test
        void sameItemFullStack_blocksProcessing() {
            assertTrue(simulateCooking(
                    SimStack.of(ITEM_INGOT, 64),
                    SimStack.of(ITEM_INGOT, 1)),
                    "Same item at max stack must block (would overflow)");
        }

        @Test
        void sameItemOneBelowFull_allowsProcessing() {
            assertFalse(simulateCooking(
                    SimStack.of(ITEM_INGOT, 63),
                    SimStack.of(ITEM_INGOT, 1)),
                    "63+1=64 exactly fits — must allow processing");
        }

        @Test
        void sameItemOverMaxStack_blocksProcessing() {
            assertTrue(simulateCooking(
                    new SimStack(ITEM_INGOT, 65, 64),
                    SimStack.of(ITEM_INGOT, 1)),
                    "Already over max stack must block");
        }
    }

    // ════════════════════════════════════════════════════════════
    // E. Clear & resume — after resolving block, processing resumes
    // ════════════════════════════════════════════════════════════

    @Nested
    class ClearAndResume {

        @Test
        void clearDifferentItem_resumesProcessing() {
            // Step 1: output has a foreign item → blocked
            assertTrue(simulateCooking(
                    SimStack.of(ITEM_DUST, 1),
                    SimStack.of(ITEM_INGOT, 1)),
                    "Foreign item blocks");

            // Step 2: clear the foreign item → resumes
            assertFalse(simulateCooking(
                    SimStack.empty(),
                    SimStack.of(ITEM_INGOT, 1)),
                    "After clearing foreign item, processing resumes");
        }

        @Test
        void makeSpaceInFullStack_resumesProcessing() {
            // Step 1: output is full (64/64) → blocked
            assertTrue(simulateCooking(
                    SimStack.of(ITEM_INGOT, 64),
                    SimStack.of(ITEM_INGOT, 1)),
                    "Full stack blocks");

            // Step 2: remove 1 (now 63/64) → space available
            assertFalse(simulateCooking(
                    SimStack.of(ITEM_INGOT, 63),
                    SimStack.of(ITEM_INGOT, 1)),
                    "After making space, processing resumes");
        }

        @Test
        void clearAndRefill_reblocks() {
            assertFalse(simulateCooking(SimStack.empty(), SimStack.of(ITEM_INGOT, 1)),
                    "Empty: proceed");
            assertTrue(simulateCooking(SimStack.of(ITEM_INGOT, 64), SimStack.of(ITEM_INGOT, 1)),
                    "Full: block");
            assertFalse(simulateCooking(SimStack.of(ITEM_INGOT, 63), SimStack.of(ITEM_INGOT, 1)),
                    "63/64: proceed");
        }
    }

    // ════════════════════════════════════════════════════════════
    // F. Process step semantics — blocked ticks must not consume
    //    energy or advance progress
    // ════════════════════════════════════════════════════════════
    //
    // Reuses ProcessStepContractTest.simulateTick() to verify the
    // integration: when cooking() returns true (blocked), process()
    // must NOT advance progress and must NOT consume energy.
    //

    @Nested
    class ProcessStepSemantics {

        @Test
        void blockedTick_doesNotAdvanceProgress() {
            var outcome = ProcessStepContractTest.simulateTick(
                    50, 200, 1000, 100,
                    true, true  // conditionStart OK, cooking BLOCKS
            );
            assertEquals(50, outcome.progress(),
                    "Progress must NOT change when cooking() blocks");
        }

        @Test
        void blockedTick_doesNotConsumeEnergy() {
            var outcome = ProcessStepContractTest.simulateTick(
                    50, 200, 1000, 100,
                    true, true
            );
            assertEquals(1000, outcome.energy(),
                    "Energy must NOT change when cooking() blocks");
        }

        @Test
        void blockedTick_noOnCookFinish() {
            var outcome = ProcessStepContractTest.simulateTick(
                    199, 200, 1000, 100,
                    true, true  // nearly complete but blocked
            );
            assertFalse(outcome.onCookFinishCalled(),
                    "onCookFinish must NOT fire when cooking() blocks, " +
                    "even if progress would exceed maxProgress");
        }

        @Test
        void blockedThenUnblocked_resumesProgress() {
            int progress = 75;
            int energy = 1000;

            // 3 blocked ticks
            for (int i = 0; i < 3; i++) {
                var outcome = ProcessStepContractTest.simulateTick(
                        progress, 200, energy, 100,
                        true, true
                );
                progress = outcome.progress();
                energy = outcome.energy();
            }

            assertEquals(75, progress, "Progress preserved through 3 blocked ticks");
            assertEquals(1000, energy, "Energy preserved through 3 blocked ticks");

            // Unblocked: progress resumes from 75
            var unblocked = ProcessStepContractTest.simulateTick(
                    progress, 200, energy, 100,
                    true, false
            );
            assertEquals(76, unblocked.progress(),
                    "Progress continues from 75 + 1 = 76 (progress++)");
            assertEquals(900, unblocked.energy(),
                    "Energy consumed only after unblock: 1000 - 100 = 900");
            assertTrue(unblocked.active(),
                    "Machine must be active during normal processing");
        }

        @Test
        void multipleBlockingTicks_phantomProgressDoesNotAccumulate() {
            int progress = 0;
            int energy = 1000;

            // 10 blocked ticks
            for (int i = 0; i < 10; i++) {
                var outcome = ProcessStepContractTest.simulateTick(
                        progress, 200, energy, 100,
                        true, true
                );
                progress = outcome.progress();
                energy = outcome.energy();
            }

            assertEquals(0, progress,
                    "No phantom progress after 10 blocked ticks");
            assertEquals(1000, energy,
                    "No energy drain after 10 blocked ticks");
        }
    }

    // ════════════════════════════════════════════════════════════
    // I. Recipe identity verification on completion (S3 fix)
    // ════════════════════════════════════════════════════════════

    /**
     * Simulates Furnace onCookFinish with S3 identity check.
     * Returns true if consumption would proceed (identity matches),
     * false if blocked (identity mismatch or input/output issues).
     */
    static boolean simulateIdentityCheckedOnCookFinish(
            String lockedRecipeId, String currentRecipeId,
            int inputCount, int outputCount, int outputMaxStack,
            int resultCount, int B) {
        // S3: verify recipe identity still matches
        if (!java.util.Objects.equals(lockedRecipeId, currentRecipeId)) {
            return false; // identity mismatch → don't consume
        }
        // Check input has at least B items
        if (inputCount < B) return false;
        // Check output has room for B×result
        long totalResult = (long) resultCount * B;
        if (totalResult > Integer.MAX_VALUE) return false;
        int totalResultInt = (int) totalResult;
        if (outputCount + totalResultInt > outputMaxStack) return false;
        return true; // would proceed
    }

    @Nested
    class IdentityVerification {

        @Test
        void sameIdentity_allowsConsumption() {
            assertTrue(simulateIdentityCheckedOnCookFinish(
                    "ten:furnace_iron", "ten:furnace_iron",
                    10, 0, 64, 1, 3),
                    "Same recipe identity: consumption allowed");
        }

        @Test
        void differentIdentity_blocksConsumption() {
            assertFalse(simulateIdentityCheckedOnCookFinish(
                    "ten:furnace_iron", "ten:furnace_gold",
                    10, 0, 64, 1, 3),
                    "Different recipe identity: consumption blocked (S3 fix)");
        }

        @Test
        void nullIdentity_blocksConsumption() {
            assertFalse(simulateIdentityCheckedOnCookFinish(
                    "ten:furnace_iron", null,
                    10, 0, 64, 1, 3),
                    "Null current identity: consumption blocked");
        }

        @Test
        void insufficientInput_blocksConsumption() {
            assertFalse(simulateIdentityCheckedOnCookFinish(
                    "ten:furnace_iron", "ten:furnace_iron",
                    2, 0, 64, 1, 5),
                    "Input count < B: consumption blocked");
        }

        @Test
        void outputFull_blocksConsumption() {
            assertFalse(simulateIdentityCheckedOnCookFinish(
                    "ten:furnace_iron", "ten:furnace_iron",
                    10, 63, 64, 1, 3),
                    "63+3 > 64: consumption blocked");
        }

        @Test
        void totalResultOverflow_blocksConsumption() {
            // resultCount=Integer.MAX_VALUE, B=2 → overflow
            assertFalse(simulateIdentityCheckedOnCookFinish(
                    "ten:furnace_iron", "ten:furnace_iron",
                    10, 0, 64, Integer.MAX_VALUE, 2),
                    "Long overflow: consumption blocked");
        }
    }

    // ════════════════════════════════════════════════════════════
    // J. Source verification — identity check present in onCookFinish
    // ════════════════════════════════════════════════════════════

    @Nested
    class SourceVerificationS3 {

        @Test
        void onCookFinish_checksRecipeIdentity() throws Exception {
            var sourceFile = new java.io.File(
                    "src/main/java/com/modularmc/ten/common/blockentity/machine/FurnaceBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = java.nio.file.Files.readString(sourceFile.toPath());

            // After fix: onCookFinish must check lastRecipeId against current
            assertTrue(content.contains("lastRecipeId"),
                    "FurnaceBlockEntity must have lastRecipeId field for identity tracking");

            int onCookStart = content.indexOf("public void onCookFinish()");
            assertTrue(onCookStart >= 0);
            String afterOnCook = content.substring(onCookStart);

            // Must have identity comparison in onCookFinish
            assertTrue(afterOnCook.contains("Objects.equals(lastRecipeId") ||
                            afterOnCook.contains("lastRecipeId"),
                    "onCookFinish must verify recipe identity before consuming");
        }

        @Test
        void onCookFinish_usesGetSlotLimitForCapacity() throws Exception {
            var sourceFile = new java.io.File(
                    "src/main/java/com/modularmc/ten/common/blockentity/machine/FurnaceBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = java.nio.file.Files.readString(sourceFile.toPath());

            int onCookStart = content.indexOf("public void onCookFinish()");
            assertTrue(onCookStart >= 0);
            String afterOnCook = content.substring(onCookStart);

            // Must use getSlotLimit(1) for output capacity check
            assertTrue(afterOnCook.contains("getSlotLimit(1)"),
                    "onCookFinish must use getSlotLimit(1) for output capacity");
        }
    }

    @Nested
    class SourceVerification {

        @Test
        void furnaceCookingSourceExists() throws Exception {
            var sourceFile = new java.io.File(
                    "src/main/java/com/modularmc/ten/common/blockentity/machine/FurnaceBlockEntity.java");
            assertTrue(sourceFile.exists(), "FurnaceBlockEntity source must exist");
        }

        @Test
        void differentItemBranch_returnsTrue_blocking() throws Exception {
            var sourceFile = new java.io.File(
                    "src/main/java/com/modularmc/ten/common/blockentity/machine/FurnaceBlockEntity.java");
            assertTrue(sourceFile.exists());

            var content = java.nio.file.Files.readString(sourceFile.toPath());

            // Find the cooking() method body
            int cookingStart = content.indexOf("public boolean cooking()");
            assertTrue(cookingStart >= 0, "cooking() method must exist");

            String cookingBody = content.substring(cookingStart);

            // The fix: after !ItemStack.isSameItem(output, result), return true (block)
            int isSameItemIdx = cookingBody.indexOf("ItemStack.isSameItem(output, result)");
            assertTrue(isSameItemIdx >= 0,
                    "cooking() must contain isSameItem check");

            // Look at the content after the isSameItem check
            String afterSameItem = cookingBody.substring(isSameItemIdx);

            // Between isSameItem and the output-stacksize return, there must be
            // "return true" (the different-item branch) — not "return false".
            int outputCountReturnIdx = afterSameItem.indexOf("getMaxStackSize()");
            assertTrue(outputCountReturnIdx >= 0,
                    "cooking() must contain the max stack size comparison");

            String beforeOutputCountReturn = afterSameItem.substring(0, outputCountReturnIdx);

            int returnTrueIdx = beforeOutputCountReturn.indexOf("return true");
            assertTrue(returnTrueIdx >= 0,
                    "GREEN FIX REQUIRED: different-item branch in cooking() must return true (block), " +
                    "not false (proceed). Check content before output-count return:\n" +
                    beforeOutputCountReturn);
        }

        @Test
        void emptyOutputBranch_stillReturnsFalse() throws Exception {
            var sourceFile = new java.io.File(
                    "src/main/java/com/modularmc/ten/common/blockentity/machine/FurnaceBlockEntity.java");
            assertTrue(sourceFile.exists());

            var content = java.nio.file.Files.readString(sourceFile.toPath());

            int cookingStart = content.indexOf("public boolean cooking()");
            String cookingBody = content.substring(cookingStart);

            int isEmptyIdx = cookingBody.indexOf("output.isEmpty()");
            assertTrue(isEmptyIdx >= 0, "cooking() must check output.isEmpty()");

            String afterIsEmpty = cookingBody.substring(isEmptyIdx);
            assertTrue(afterIsEmpty.contains("return false"),
                    "Empty output branch must still return false (allow processing)");
        }

        @Test
        void cookingUsesBatchSizeForOverflowCheck() throws Exception {
            var sourceFile = new java.io.File(
                    "src/main/java/com/modularmc/ten/common/blockentity/machine/FurnaceBlockEntity.java");
            assertTrue(sourceFile.exists());

            var content = java.nio.file.Files.readString(sourceFile.toPath());

            int cookingStart = content.indexOf("public boolean cooking()");
            assertTrue(cookingStart >= 0);
            String cookingBody = content.substring(cookingStart);

            // Must reference getLockedBatchSize() for B-aware overflow check
            assertTrue(cookingBody.contains("getLockedBatchSize()"),
                    "P1-T3a: cooking() must use getLockedBatchSize() for B-aware capacity check");
        }
    }

    // ════════════════════════════════════════════════════════════
    // H (old). Batch contract — B>1 semantics
    // ════════════════════════════════════════════════════════════

    /**
     * Simulates Furnace onCookFinish with batch support.
     * Consumes B input items and produces B result items.
     * Early exits if input depletes.
     */
    static SimStack[] simulateBatchOnCookFinish(SimStack input, SimStack output,
                                                 SimStack result, int B) {
        // Returns [inputAfter, outputAfter]
        SimStack inputAfter = input;
        SimStack outputAfter = output;
        int remaining = B;

        while (remaining > 0) {
            // Check input available
            if (inputAfter.isEmpty() || inputAfter.count() < 1) break;

            // Check output space
            if (!outputAfter.isEmpty()) {
                if (outputAfter.item() != result.item()) break;
                if (outputAfter.count() + result.count() > outputAfter.maxStack()) break;
            }

            // Consume 1 input
            inputAfter = new SimStack(inputAfter.item(), inputAfter.count() - 1, inputAfter.maxStack());

            // Produce result
            if (outputAfter.isEmpty()) {
                outputAfter = result;
            } else {
                outputAfter = new SimStack(outputAfter.item(), outputAfter.count() + result.count(), outputAfter.maxStack());
            }
            remaining--;
        }

        return new SimStack[]{inputAfter, outputAfter};
    }

    @Nested
    class BatchContract {

        @Test
        void B1_producesSingleResult() {
            var result = simulateBatchOnCookFinish(
                    SimStack.of(ITEM_ORE, 5),
                    SimStack.empty(),
                    SimStack.of(ITEM_INGOT, 1),
                    1
            );
            assertEquals(4, result[0].count(), "Input 5→4 after 1 consumption");
            assertEquals(ITEM_INGOT, result[1].item(), "Output has ingot");
            assertEquals(1, result[1].count(), "Output count = 1");
        }

        @Test
        void B3_consumes3Inputs_produces3Results() {
            var result = simulateBatchOnCookFinish(
                    SimStack.of(ITEM_ORE, 10),
                    SimStack.empty(),
                    SimStack.of(ITEM_INGOT, 1),
                    3
            );
            assertEquals(7, result[0].count(), "Input 10→7 after 3 consumptions");
            assertEquals(ITEM_INGOT, result[1].item());
            assertEquals(3, result[1].count(), "Output count = 3 (B=3)");
        }

        @Test
        void inputDepleted_earlyExit() {
            var result = simulateBatchOnCookFinish(
                    SimStack.of(ITEM_ORE, 2),
                    SimStack.empty(),
                    SimStack.of(ITEM_INGOT, 1),
                    5 // B=5 but only 2 input available
            );
            assertTrue(result[0].isEmpty(), "Input fully consumed");
            assertEquals(2, result[1].count(), "Only 2 results produced (limited by input)");
        }

        @Test
        void outputFull_blocksBatch() {
            // 2 items already in output, maxStack=64, result=1, B=3
            // 2 + 1*3 = 5 <= 64, so this should work
            var result = simulateBatchOnCookFinish(
                    SimStack.of(ITEM_ORE, 10),
                    SimStack.of(ITEM_INGOT, 63),
                    SimStack.of(ITEM_INGOT, 1),
                    3
            );
            // 63 + 1 = 64 OK for first unit, but 64+1 = 65 > 64 for second
            // So only 1 should be produced
            assertEquals(64, result[1].count(), "Output capped at max stack");
            assertEquals(9, result[0].count(), "Only 1 input consumed");
        }

        @Test
        void B1_behaviorMatchesNonBatch() {
            var batchResult = simulateBatchOnCookFinish(
                    SimStack.of(ITEM_ORE, 5),
                    SimStack.empty(),
                    SimStack.of(ITEM_INGOT, 1),
                    1
            );
            assertEquals(4, batchResult[0].count());
            assertEquals(1, batchResult[1].count());
        }

        @Test
        void emptyInput_doesNothing() {
            var result = simulateBatchOnCookFinish(
                    SimStack.empty(),
                    SimStack.empty(),
                    SimStack.of(ITEM_INGOT, 1),
                    3
            );
            assertTrue(result[0].isEmpty());
            assertTrue(result[1].isEmpty());
        }
    }
}
