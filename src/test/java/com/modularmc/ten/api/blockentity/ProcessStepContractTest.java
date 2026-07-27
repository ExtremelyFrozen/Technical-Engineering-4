// -*- coding: utf-8 -*-
package com.modularmc.ten.api.blockentity;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract tests for {@link ProcessingMachineBlockEntity#process()} step ordering.
 * <p>
 * Tests the critical behavior: when {@code cooking()} blocks (output full),
 * progress must NOT advance and energy must NOT be consumed. Progress only
 * advances and energy is only consumed in the same tick AFTER {@code cooking()}
 * passes.
 * <p>
 * Uses a pure-Java simulation of the process step decision since
 * {@link ProcessingMachineBlockEntity} requires Minecraft bootstrap.
 * The simulation is intentionally BUGGY at first (RED phase), then fixed
 * (GREEN phase), and the same fix is applied to the actual source.
 */
class ProcessStepContractTest {

    // ════════════════════════════════════════════════════════════
    // Process step simulation — initially BUGGY (RED)
    // ════════════════════════════════════════════════════════════

    /**
     * Result of one simulated process tick.
     */
    record StepOutcome(
            int progress,
            int energy,
            boolean onCookFinishCalled,
            boolean active
    ) {}

    /**
     * Simulates one tick of the process step decision.
     * <p>
     * Mirrors the production tick model:
     * <ul>
     *   <li>Progress advances by exactly 1 per tick ({@code progress++})</li>
     *   <li>Completion fires when {@code progress >= maxProgress}</li>
     *   <li>Energy consumption is independent of progress advancement</li>
     * </ul>
     *
     * @param progress       current progress
     * @param maxProgress    max progress before completion
     * @param energy         current energy stored
     * @param efficiency     actual efficiency (energy consumed per tick)
     * @param canProcess     conditionStart() && signalAllowRun() && energyAllowRun()
     * @param cookingBlocks  cooking() returns true (output blocked)
     * @return tick outcome
     */
    static StepOutcome simulateTick(
            int progress,
            int maxProgress,
            int energy,
            int efficiency,
            boolean canProcess,
            boolean cookingBlocks
    ) {
        if (!canProcess) {
            // ── Conditions not met: reset ──
            return new StepOutcome(0, energy, false, false);
        }

        int energyConsumed = Math.min(efficiency, energy);
        if (energyConsumed <= 0) {
            // ── No energy available: preserve progress, set inactive ──
            // Source: ProcessingMachineBlockEntity.process() returns early
            // without resetting progress when fePerTick > energyStored
            return new StepOutcome(progress, energy, false, false);
        }

        // ════════════════════════════════════════════════════════
        // Production model: progress advances AFTER cooking check
        // ════════════════════════════════════════════════════════
        if (cookingBlocks) {
            // Correct: if cooking blocks, progress and energy stay unchanged
            return new StepOutcome(progress, energy, false, true);
        }

        // Cooking passed: consume energy AND advance progress by 1
        int newProgress = progress + 1;
        int newEnergy = energy - energyConsumed;

        // Completion fires when progress >= maxProgress (not >)
        if (newProgress >= maxProgress) {
            return new StepOutcome(0, newEnergy, true, true);
        }

        return new StepOutcome(newProgress, newEnergy, false, true);
    }

    // ════════════════════════════════════════════════════════════
    // A. Cooking block contract — progress must NOT advance
    // ════════════════════════════════════════════════════════════

    @Nested
    class CookingBlockContract {

        @Test
        void blocked_output_doesNotAdvanceProgress() {
            int initialProgress = 50;
            int initialEnergy = 1000;
            int efficiency = 100;

            StepOutcome outcome = simulateTick(
                    initialProgress, 200, initialEnergy, efficiency,
                    true, true // canProcess=true, cookingBlocks=true
            );

            // PROGRESS MUST NOT CHANGE when cooking blocks
            assertEquals(initialProgress, outcome.progress(),
                    "Progress must NOT advance when cooking() blocks");
        }

        @Test
        void blocked_output_doesNotConsumeEnergy() {
            int initialEnergy = 1000;

            StepOutcome outcome = simulateTick(
                    50, 200, initialEnergy, 100,
                    true, true
            );

            // ENERGY MUST NOT BE CONSUMED when cooking blocks
            assertEquals(initialEnergy, outcome.energy(),
                    "Energy must NOT be consumed when cooking() blocks");
        }

        @Test
        void blocked_output_doesNotTriggerOnCookFinish() {
            StepOutcome outcome = simulateTick(
                    50, 200, 1000, 100,
                    true, true
            );

            assertFalse(outcome.onCookFinishCalled(),
                    "onCookFinish must NOT be called when cooking() blocks");
        }

        @Test
        void blocked_output_progressNeverExceedsOldProgress() {
            // Even with high efficiency, blocked progress must not exceed initial
            int initialProgress = 10;
            int highEfficiency = 9999;

            StepOutcome outcome = simulateTick(
                    initialProgress, 200, 10000, highEfficiency,
                    true, true
            );

            assertTrue(outcome.progress() <= initialProgress,
                    "Progress must not exceed initial value when blocked");
        }

        @Test
        void blocked_output_repeatedTicks_noProgressDrift() {
            int progress = 0;
            int energy = 1000;
            int maxProgress = 200;
            int efficiency = 100;

            // Simulate 10 ticks with output blocked
            for (int tick = 0; tick < 10; tick++) {
                StepOutcome outcome = simulateTick(
                        progress, maxProgress, energy, efficiency,
                        true, true
                );
                // After fix: progress never changes on blocked ticks
                // BUG: progress increases each tick even though blocked
                progress = outcome.progress();
                energy = outcome.energy();
            }

            assertEquals(0, progress,
                    "Progress must not drift after multiple blocked ticks");
            assertEquals(1000, energy,
                    "Energy must not drift after multiple blocked ticks");
        }
    }

    // ════════════════════════════════════════════════════════════
    // B. Normal processing contract
    // ════════════════════════════════════════════════════════════

    @Nested
    class NormalProcessingContract {

        @Test
        void normalTick_advancesProgress_byOne() {
            int initialProgress = 0;

            StepOutcome outcome = simulateTick(
                    initialProgress, 200, 1000, 100,
                    true, false
            );

            assertEquals(initialProgress + 1, outcome.progress(),
                    "Progress must increase by exactly 1 per tick (progress++)");
        }

        @Test
        void normalTick_consumesEnergy_byEfficiency() {
            int initialEnergy = 1000;
            int efficiency = 100;

            StepOutcome outcome = simulateTick(
                    0, 200, initialEnergy, efficiency,
                    true, false
            );

            assertEquals(initialEnergy - efficiency, outcome.energy(),
                    "Energy must decrease by efficiency on normal tick");
        }

        @Test
        void normalTick_energyConsumed_cappedByAvailableEnergy() {
            int initialEnergy = 30; // less than efficiency=100

            StepOutcome outcome = simulateTick(
                    0, 200, initialEnergy, 100,
                    true, false
            );

            assertEquals(1, outcome.progress(),
                    "Progress advances by 1 regardless of energy cap (progress++)");
            assertEquals(0, outcome.energy(),
                    "Energy must be fully consumed");
        }

        @Test
        void normalTick_zeroEnergy_setsInactivePreservesProgress() {
            StepOutcome outcome = simulateTick(
                    50, 200, 0, 100,
                    true, false
            );

            assertEquals(50, outcome.progress(),
                    "Progress must be preserved when energy is 0 (source returns early without reset)");
            assertFalse(outcome.active(),
                    "Must be inactive when energy is 0");
        }

        @Test
        void normalTick_isActive() {
            StepOutcome outcome = simulateTick(
                    0, 200, 1000, 100,
                    true, false
            );

            assertTrue(outcome.active(),
                    "Must be active during normal processing");
        }

        @Test
        void progressAndEnergy_advanceTogether_overMultipleTicks() {
            int progress = 0;
            int energy = 1000;
            int maxProgress = 500;
            int efficiency = 100;
            boolean onCook = false;

            // Simulate 4 normal ticks: progress 0→1→2→3→4
            for (int tick = 0; tick < 4; tick++) {
                StepOutcome outcome = simulateTick(
                        progress, maxProgress, energy, efficiency,
                        true, false
                );
                progress = outcome.progress();
                energy = outcome.energy();
                onCook = outcome.onCookFinishCalled();
            }

            assertEquals(4, progress,
                    "Progress should advance to 4 after 4 ticks at 1/tick");
            assertEquals(600, energy,
                    "Energy should decrease to 600 after 4 ticks at 100/tick");
            assertFalse(onCook,
                    "onCookFinish should not fire before progress >= maxProgress");
        }
    }

    // ════════════════════════════════════════════════════════════
    // C. Unblock contract — resume from saved progress
    // ════════════════════════════════════════════════════════════

    @Nested
    class UnblockContract {

        @Test
        void unblocked_resumesFromPreviousProgress() {
            int progress = 75;
            int maxProgress = 200;
            int energy = 1000;
            int efficiency = 100;

            // Tick 1: blocked — progress must stay at 75
            StepOutcome blocked = simulateTick(
                    progress, maxProgress, energy, efficiency,
                    true, true
            );

            // Tick 2: unblocked — progress must continue from 75
            StepOutcome unblocked = simulateTick(
                    blocked.progress(), maxProgress, blocked.energy(), efficiency,
                    true, false
            );

            // Progress advances by +1 on unblocked tick
            assertEquals(76, unblocked.progress(),
                    "After unblock, progress must continue from 75 + 1 = 76");
        }

        @Test
        void unblocked_doesNotDoubleCountBlockedProgress() {
            int progress = 0;
            int maxProgress = 200;
            int energy = 1000;
            int efficiency = 100;

            // 3 blocked ticks, then 1 normal tick
            for (int tick = 0; tick < 3; tick++) {
                StepOutcome outcome = simulateTick(
                        progress, maxProgress, energy, efficiency,
                        true, true
                );
                progress = outcome.progress();
                energy = outcome.energy();
            }

            // After first normal tick, progress = 0 + 1 = 1 (correct)
            // With bug: progress = 300 after blocked ticks + 100 = 400 (wrong!)
            StepOutcome outcome = simulateTick(
                    progress, maxProgress, energy, efficiency,
                    true, false
            );

            assertEquals(1, outcome.progress(),
                    "Unblocked progress = 0 + 1 = 1 (no phantom blocked progress)");
        }

        @Test
        void blockUnblockCycle_preservesProgress() {
            int progress = 50;
            int maxProgress = 200;
            int energy = 1000;
            int efficiency = 50;

            // Block → unblock → block → unblock
            StepOutcome r1 = simulateTick(progress, maxProgress, energy, efficiency, true, true);
            StepOutcome r2 = simulateTick(r1.progress(), maxProgress, r1.energy(), efficiency, true, false);
            StepOutcome r3 = simulateTick(r2.progress(), maxProgress, r2.energy(), efficiency, true, true);
            StepOutcome r4 = simulateTick(r3.progress(), maxProgress, r3.energy(), efficiency, true, false);

            // 50 + 1 (r2) + 1 (r4) = 52
            assertEquals(52, r4.progress(),
                    "Progress should accumulate only on unblocked ticks: 50 + 1 + 1 = 52");
        }

        @Test
        void energyPreserved_throughBlockedTicks() {
            int energy = 500;

            StepOutcome blocked1 = simulateTick(50, 200, energy, 100, true, true);
            StepOutcome blocked2 = simulateTick(blocked1.progress(), 200, blocked1.energy(), 100, true, true);
            StepOutcome blocked3 = simulateTick(blocked2.progress(), 200, blocked2.energy(), 100, true, true);

            assertEquals(500, blocked3.energy(),
                    "Energy must be preserved through blocked ticks");
        }
    }

    // ════════════════════════════════════════════════════════════
    // D. Completion contract — not off-by-one
    // ════════════════════════════════════════════════════════════

    @Nested
    class CompletionContract {

        @Test
        void completion_triggers_onCookFinish() {
            // progress=199 + 1 = 200 >= 200 → complete
            StepOutcome outcome = simulateTick(
                    199, 200, 1000, 100,
                    true, false
            );

            assertTrue(outcome.onCookFinishCalled(),
                    "onCookFinish must be called when progress >= maxProgress");
            assertEquals(0, outcome.progress(),
                    "Progress must reset to 0 on completion");
        }

        @Test
        void completion_firesOnReachingMaxProgress() {
            // Production uses >= (not >), so reaching maxProgress fires completion
            // progress=199 + 1 = 200 >= 200 → fires
            StepOutcome outcome = simulateTick(
                    199, 200, 1000, 100,
                    true, false
            );

            assertTrue(outcome.onCookFinishCalled(),
                    "onCookFinish must fire when progress reaches maxProgress (>=)");
            assertEquals(0, outcome.progress(),
                    "Progress must reset to 0 on completion");
        }

        @Test
        void completion_notYetWhenBelowMax() {
            // progress=198 + 1 = 199 < 200 → NOT complete yet
            StepOutcome outcome = simulateTick(
                    198, 200, 1000, 100,
                    true, false
            );

            assertFalse(outcome.onCookFinishCalled(),
                    "onCookFinish must NOT fire when progress < maxProgress");
            assertEquals(199, outcome.progress(),
                    "Progress should be 199 (198+1), not reset yet");
        }

        @Test
        void completion_resetsProgress_properly() {
            // After completion, progress=0 and next tick starts fresh
            StepOutcome complete = simulateTick(
                    199, 200, 1000, 100,
                    true, false
            );
            assertTrue(complete.onCookFinishCalled());
            assertEquals(0, complete.progress());

            // Next tick: 0 + 1 = 1
            StepOutcome nextTick = simulateTick(
                    complete.progress(), 200, complete.energy(), 100,
                    true, false
            );
            assertEquals(1, nextTick.progress(),
                    "Progress should start from 0 after completion");
        }

        @Test
        void completion_sameTick_energyConsumed() {
            // On completion tick, energy is still consumed
            int initialEnergy = 1000;
            StepOutcome outcome = simulateTick(
                    199, 200, initialEnergy, 100,
                    true, false
            );

            assertTrue(outcome.onCookFinishCalled());
            assertEquals(initialEnergy - 100, outcome.energy(),
                    "Energy must still be consumed on completion tick");
        }
    }

    // ════════════════════════════════════════════════════════════
    // E. Regression — conditions not met
    // ════════════════════════════════════════════════════════════

    @Nested
    class RegressionContract {

        @Test
        void conditionsNotMet_resetsProgress() {
            StepOutcome outcome = simulateTick(
                    75, 200, 1000, 100,
                    false, false
            );

            assertEquals(0, outcome.progress(),
                    "Progress must reset when conditions are not met");
            assertFalse(outcome.active(),
                    "Must be inactive when conditions are not met");
        }

        @Test
        void conditionsNotMet_noOnCookFinish() {
            StepOutcome outcome = simulateTick(
                    75, 200, 1000, 100,
                    false, false
            );

            assertFalse(outcome.onCookFinishCalled(),
                    "onCookFinish must not be called when conditions are not met");
        }

        @Test
        void conditionsNotMet_energyUnchanged() {
            StepOutcome outcome = simulateTick(
                    75, 200, 1000, 100,
                    false, false
            );

            assertEquals(1000, outcome.energy(),
                    "Energy must not change when conditions are not met");
        }

        @Test
        void blockedWithNoEnergy_doesNotAdvance() {
            // cooking blocks AND energy is 0
            // Energy-zero branch fires before cooking() check, so progress is preserved
            StepOutcome outcome = simulateTick(
                    50, 200, 0, 100,
                    true, true
            );

            assertEquals(50, outcome.progress(),
                    "Progress must be preserved when energy is 0 (early return before cooking check)");
            assertFalse(outcome.active(),
                    "Must be inactive when energy is 0");
        }
    }

    // ════════════════════════════════════════════════════════════
    // F. Source verification — process() ordering
    // ════════════════════════════════════════════════════════════

    @Nested
    class SourceVerification {

        @Test
        void processMethod_advancesProgress_afterCookingCheck() throws Exception {
            var sourceFile = new java.io.File(
                    "src/main/java/com/modularmc/ten/api/blockentity/ProcessingMachineBlockEntity.java");
            assertTrue(sourceFile.exists(),
                    "Source file must exist");

            var content = java.nio.file.Files.readString(sourceFile.toPath());

            // The progress increment must appear AFTER the cooking() check
            // In the processed source, we look for the pattern:
            //   if (cooking()) { ... return; }
            //   ... progress += energyConsumed;
            int cookingReturnIdx = content.indexOf("if (cooking())");
            assertTrue(cookingReturnIdx >= 0,
                    "Source must contain cooking() check");

            int progressAddIdx = content.indexOf("progress++");
            assertTrue(progressAddIdx >= 0,
                    "Source must contain progress++ (P1 target)");

            assertTrue(
                    progressAddIdx > cookingReturnIdx,
                    "progress += energyConsumed must appear AFTER cooking() check " +
                            "(cookingCheck=" + cookingReturnIdx +
                            ", progressAdd=" + progressAddIdx + ")"
            );
        }

        @Test
        void processMethod_consumesEnergy_afterCookingCheck() throws Exception {
            var sourceFile = new java.io.File(
                    "src/main/java/com/modularmc/ten/api/blockentity/ProcessingMachineBlockEntity.java");
            assertTrue(sourceFile.exists());

            var content = java.nio.file.Files.readString(sourceFile.toPath());

            int cookingReturnIdx = content.indexOf("if (cooking())");
            assertTrue(cookingReturnIdx >= 0);

            int extractEnergyIdx = content.indexOf("extractEnergy(fePerTick, false)");
            assertTrue(extractEnergyIdx >= 0,
                    "Source must contain energy extraction (fePerTick)");

            assertTrue(
                    extractEnergyIdx > cookingReturnIdx,
                    "energy extraction must appear AFTER cooking() check " +
                            "(cookingCheck=" + cookingReturnIdx +
                            ", extractEnergy=" + extractEnergyIdx + ")"
            );
        }

        // ════════════════════════════════════════════════════════════
        // M1: Atomic energy extraction ordering
        // ════════════════════════════════════════════════════════════

        @Test
        void simulateExtract_beforeRealExtract() throws Exception {
            var sourceFile = new java.io.File(
                    "src/main/java/com/modularmc/ten/api/blockentity/ProcessingMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = java.nio.file.Files.readString(sourceFile.toPath());

            int simulateIdx = content.indexOf("extractEnergy(fePerTick, true)");
            assertTrue(simulateIdx >= 0,
                    "Source must contain simulate extract (simulate=true)");

            int realExtractIdx = content.indexOf("extractEnergy(fePerTick, false)");
            assertTrue(realExtractIdx >= 0,
                    "Source must contain real extract (simulate=false)");

            assertTrue(realExtractIdx > simulateIdx,
                    "Real extract (simulate=false) must appear AFTER simulate (simulate=true) " +
                            "(simulate=" + simulateIdx + ", real=" + realExtractIdx + ")");
        }

        @Test
        void simulateExtract_afterCookingCheck() throws Exception {
            var sourceFile = new java.io.File(
                    "src/main/java/com/modularmc/ten/api/blockentity/ProcessingMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = java.nio.file.Files.readString(sourceFile.toPath());

            int cookingIdx = content.indexOf("if (cooking())");
            assertTrue(cookingIdx >= 0);

            int simulateIdx = content.indexOf("extractEnergy(fePerTick, true)");
            assertTrue(simulateIdx >= 0,
                    "Source must contain simulate extract");

            assertTrue(simulateIdx > cookingIdx,
                    "Simulate extract must appear AFTER cooking() check " +
                            "(cooking=" + cookingIdx + ", simulate=" + simulateIdx + ")");
        }

        @Test
        void progressIncrement_afterBothExtracts() throws Exception {
            var sourceFile = new java.io.File(
                    "src/main/java/com/modularmc/ten/api/blockentity/ProcessingMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = java.nio.file.Files.readString(sourceFile.toPath());

            int simulateIdx = content.indexOf("extractEnergy(fePerTick, true)");
            assertTrue(simulateIdx >= 0);

            int realExtractIdx = content.indexOf("extractEnergy(fePerTick, false)");
            assertTrue(realExtractIdx >= 0);

            int progressIdx = content.indexOf("progress++");
            assertTrue(progressIdx >= 0);

            assertTrue(progressIdx > realExtractIdx,
                    "progress++ must appear AFTER both extractEnergy calls " +
                            "(realExtract=" + realExtractIdx + ", progress=" + progressIdx + ")");
        }
    }
}
