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
     * Initially uses the BUGGY ordering: progress advances BEFORE
     * the cooking() check, so blocked ticks gain free progress.
     * <p>
     * After GREEN fix: progress advances AFTER cooking() passes,
     * in the same branch as energy consumption.
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
            // without resetting progress when energyConsumed <= 0
            return new StepOutcome(progress, energy, false, false);
        }

        // ════════════════════════════════════════════════════════
        // GREEN (fixed): progress advances AFTER cooking check passes
        // ════════════════════════════════════════════════════════
        if (cookingBlocks) {
            // Correct: if cooking blocks, progress and energy stay unchanged
            return new StepOutcome(progress, energy, false, true);
        }

        // Cooking passed: advance progress AND consume energy in same branch
        int newProgress = progress + energyConsumed;
        int newEnergy = energy - energyConsumed;

        if (newProgress > maxProgress) {
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
        void normalTick_advancesProgress_byEnergyConsumed() {
            int initialProgress = 0;
            int efficiency = 100;

            StepOutcome outcome = simulateTick(
                    initialProgress, 200, 1000, efficiency,
                    true, false
            );

            assertEquals(initialProgress + efficiency, outcome.progress(),
                    "Progress must increase by energyConsumed on normal tick");
        }

        @Test
        void normalTick_consumesEnergy_byEnergyConsumed() {
            int initialEnergy = 1000;
            int efficiency = 100;

            StepOutcome outcome = simulateTick(
                    0, 200, initialEnergy, efficiency,
                    true, false
            );

            assertEquals(initialEnergy - efficiency, outcome.energy(),
                    "Energy must decrease by energyConsumed on normal tick");
        }

        @Test
        void normalTick_energyConsumed_cappedByAvailableEnergy() {
            int initialEnergy = 30; // less than efficiency=100

            StepOutcome outcome = simulateTick(
                    0, 200, initialEnergy, 100,
                    true, false
            );

            assertEquals(30, outcome.progress(),
                    "Progress advance must be capped by available energy");
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

            // Simulate 4 normal ticks: progress 0→100→200→300→400
            for (int tick = 0; tick < 4; tick++) {
                StepOutcome outcome = simulateTick(
                        progress, maxProgress, energy, efficiency,
                        true, false
                );
                progress = outcome.progress();
                energy = outcome.energy();
                onCook = outcome.onCookFinishCalled();
            }

            assertEquals(400, progress,
                    "Progress should advance to 400 after 4 ticks at 100/tick");
            assertEquals(600, energy,
                    "Energy should decrease to 600 after 4 ticks at 100/tick");
            assertFalse(onCook,
                    "onCookFinish should not fire before progress > maxProgress");
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

            // After fix: blocked doesn't change progress, unblocked adds 100
            assertEquals(75 + efficiency, unblocked.progress(),
                    "After unblock, progress must continue from where it stopped");
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

            // After first normal tick, progress = 0 + 100 = 100 (correct)
            // With bug: progress = 300 after blocked ticks + 100 = 400 (wrong!)
            StepOutcome outcome = simulateTick(
                    progress, maxProgress, energy, efficiency,
                    true, false
            );

            assertEquals(100, outcome.progress(),
                    "Unblocked progress must not include blocked-era phantom progress");
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

            // After fix: 50 + 50 (r2) + 50 (r4) = 150
            assertEquals(150, r4.progress(),
                    "Progress should accumulate only on unblocked ticks");
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
            // progress + energyConsumed > maxProgress → complete
            StepOutcome outcome = simulateTick(
                    150, 200, 1000, 100, // 150 + 100 = 250 > 200
                    true, false
            );

            assertTrue(outcome.onCookFinishCalled(),
                    "onCookFinish must be called when progress exceeds maxProgress");
            assertEquals(0, outcome.progress(),
                    "Progress must reset to 0 on completion");
        }

        @Test
        void completion_exactMax_notOffByOne() {
            // progress + energyConsumed == maxProgress → NOT complete yet
            // Need > not >= for completion trigger
            StepOutcome outcome = simulateTick(
                    100, 200, 1000, 100, // 100 + 100 = 200, NOT > 200
                    true, false
            );

            assertFalse(outcome.onCookFinishCalled(),
                    "onCookFinish must NOT fire when progress == maxProgress (needs >)");
            assertEquals(200, outcome.progress(),
                    "Progress should be exactly maxProgress, not reset yet");
        }

        @Test
        void completion_exceedsByOne() {
            // progress + energyConsumed = maxProgress + 1 → complete
            StepOutcome outcome = simulateTick(
                    101, 200, 1000, 100, // 101 + 100 = 201 > 200
                    true, false
            );

            assertTrue(outcome.onCookFinishCalled(),
                    "onCookFinish must fire when progress exceeds maxProgress");
            assertEquals(0, outcome.progress(),
                    "Progress must reset after completion");
        }

        @Test
        void completion_resetsProgress_properly() {
            // After completion, progress=0 and next tick starts fresh
            StepOutcome complete = simulateTick(
                    150, 200, 1000, 100,
                    true, false
            );
            assertTrue(complete.onCookFinishCalled());
            assertEquals(0, complete.progress());

            // Next tick: 0 + 100 = 100
            StepOutcome nextTick = simulateTick(
                    complete.progress(), 200, complete.energy(), 100,
                    true, false
            );
            assertEquals(100, nextTick.progress(),
                    "Progress should start from 0 after completion");
        }

        @Test
        void completion_sameTick_energyConsumed() {
            // On completion tick, energy is still consumed
            int initialEnergy = 1000;
            StepOutcome outcome = simulateTick(
                    150, 200, initialEnergy, 100,
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

            int progressAddIdx = content.indexOf("progress += energyConsumed");
            assertTrue(progressAddIdx >= 0,
                    "Source must contain progress increment");

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

            int extractEnergyIdx = content.indexOf("extractEnergy(energyConsumed, false)");
            assertTrue(extractEnergyIdx >= 0,
                    "Source must contain energy extraction");

            assertTrue(
                    extractEnergyIdx > cookingReturnIdx,
                    "energy extraction must appear AFTER cooking() check " +
                            "(cookingCheck=" + cookingReturnIdx +
                            ", extractEnergy=" + extractEnergyIdx + ")"
            );
        }
    }
}
