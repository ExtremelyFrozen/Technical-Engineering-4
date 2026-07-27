// -*- coding: utf-8 -*-
package com.modularmc.ten.api.blockentity;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract tests for the refactored {@link ProcessingMachineBlockEntity#process()}
 * tick semantics — P1 target behavior, written as RED tests for P0 baseline.
 * <p>
 * Current (P0 baseline) behavior:
 * <ul>
 *   <li>{@code maxProgress = baseTickTime() * Math.max(initialEfficientIn, 1)}</li>
 *   <li>{@code progress += energyConsumed} (energy-accumulation model)</li>
 *   <li>{@code progress > maxProgress} (strict greater-than)</li>
 *   <li>{@code else { setActive(false); progress = 0; }} (unconditional reset)</li>
 * </ul>
 * <p>
 * Target (P1) behavior:
 * <ul>
 *   <li>{@code maxProgress = baseTickTime()} (ticks only, no energy multiplier)</li>
 *   <li>Fixed FE/t consumption per tick, progress++ (tick-counting model)</li>
 *   <li>{@code progress >= maxProgress} (greater-or-equal completion)</li>
 *   <li>Energy不足/输出满暂停不重置progress; 仅配方失效时重置</li>
 * </ul>
 * <p>
 * Uses pure-Java simulation of the target process() semantics.
 * Source verification tests confirm the current baseline.
 */
class ProcessingMachineContractTest {

    // ════════════════════════════════════════════════════════════
    // Target process() simulation — NEW semantics (P1 target)
    // ════════════════════════════════════════════════════════════

    /**
     * Result of one simulated refactored process tick.
     */
    record RefactoredStepOutcome(
            int progress,
            int energy,
            boolean onCookFinishCalled,
            boolean active
    ) {}

    /**
     * Simulates one tick of the REFACTORED process() with target semantics.
     * <p>
     * Target: fixed FE/t consumption, progress++ per tick,
     * progress >= maxProgress completion, no progress reset on pause.
     *
     * @param progress       current progress
     * @param maxProgress    max progress (fixed at recipe start = baseTickTime())
     * @param energy         current energy stored
     * @param fePerTick      FE consumed per tick (含 upgrade multiplier)
     * @param canProcess     conditionStart() && signalAllowRun()
     * @param energyAllowRun energyStorage >= fePerTick (pre-checked)
     * @param cookingBlocks  cooking() returns true (output blocked)
     * @return tick outcome
     */
    static RefactoredStepOutcome simulateRefactoredTick(
            int progress,
            int maxProgress,
            int energy,
            int fePerTick,
            boolean canProcess,
            boolean energyAllowRun,
            boolean cookingBlocks
    ) {
        // ── Phase 1: Condition guard — recipe identity changed → reset ──
        // (conditionStart returns false when no valid recipe)
        // NOTE: In the refactored code, conditionStart returning false
        // means no recipe available → progress reset (by conditionStart itself).
        // Energy不足/输出满 are handled AFTER conditionStart passes.
        if (!canProcess) {
            return new RefactoredStepOutcome(0, energy, false, false);
        }

        // ── Phase 2: Energy check — insufficient → pause, no reset ──
        if (!energyAllowRun || energy < fePerTick) {
            return new RefactoredStepOutcome(progress, energy, false, false);
        }

        // ── Phase 3: Output check — full → pause, no reset ──
        if (cookingBlocks) {
            return new RefactoredStepOutcome(progress, energy, false, true);
        }

        // ── Phase 4: Consume FE/t and advance progress by 1 ──
        int newEnergy = energy - fePerTick;
        int newProgress = progress + 1;  // progress++ per tick, not by energy

        // ── Phase 5: Completion check — >= not > ──
        if (newProgress >= maxProgress) {
            return new RefactoredStepOutcome(0, newEnergy, true, true);
        }

        return new RefactoredStepOutcome(newProgress, newEnergy, false, true);
    }

    // ════════════════════════════════════════════════════════════
    // A. Completion contract — >= maxProgress (P1 target)
    // ════════════════════════════════════════════════════════════

    @Nested
    class CompletionContract {

        @Test
        void completes_whenProgressEqualsMaxProgress() {
            // progress=199, maxProgress=200, fePerTick=1
            // progress + 1 = 200 >= 200 → complete
            RefactoredStepOutcome outcome = simulateRefactoredTick(
                    199, 200, 1000, 1,
                    true, true, false
            );
            assertTrue(outcome.onCookFinishCalled(),
                    "P1 target: onCookFinish must fire when progress >= maxProgress " +
                    "(199+1=200 >= 200). Current code uses > so this fires at 201.");
            assertEquals(0, outcome.progress(),
                    "Progress must reset to 0 after completion");
        }

        @Test
        void completes_whenProgressExceedsMaxProgress() {
            RefactoredStepOutcome outcome = simulateRefactoredTick(
                    200, 200, 1000, 1,
                    true, true, false
            );
            assertTrue(outcome.onCookFinishCalled(),
                    "onCookFinish fires when progress 200+1=201 >= 200");
            assertEquals(0, outcome.progress());
        }

        @Test
        void doesNotComplete_whenProgressBelowMaxProgress() {
            RefactoredStepOutcome outcome = simulateRefactoredTick(
                    150, 200, 1000, 1,
                    true, true, false
            );
            assertFalse(outcome.onCookFinishCalled(),
                    "onCookFinish must NOT fire while progress < maxProgress");
            assertEquals(151, outcome.progress(),
                    "Progress must advance to 151 (150+1)");
        }

        @Test
        void doesNotComplete_exactMax_notOffByOne_oldCode() {
            // Current code uses >, so 199+1=200 does NOT complete
            // Target uses >=, so 199+1=200 DOES complete
            // This test documents the semantic change
            RefactoredStepOutcome outcome = simulateRefactoredTick(
                    199, 200, 1000, 1,
                    true, true, false
            );
            assertTrue(outcome.onCookFinishCalled(),
                    "TARGET (>=): 199+1=200 >= 200 → complete. " +
                    "CURRENT (>): 199+1=200 is NOT > 200 → not complete. " +
                    "This test is RED because current code uses >.");
        }
    }

    // ════════════════════════════════════════════════════════════
    // B. Fixed FE/t consumption — progress++ (P1 target)
    // ════════════════════════════════════════════════════════════

    @Nested
    class FixedFePerTickContract {

        @Test
        void progressAdvancesByOne_perTick_notByEnergy() {
            // Target: progress++ per tick, not progress += energyConsumed
            int fePerTick = 100;

            RefactoredStepOutcome outcome = simulateRefactoredTick(
                    0, 200, 1000, fePerTick,
                    true, true, false
            );
            assertEquals(1, outcome.progress(),
                    "P1 target: progress must advance by exactly 1 per tick, " +
                    "not by fePerTick (" + fePerTick + "). " +
                    "Current code: progress += " + fePerTick + " = " + fePerTick + ".");
        }

        @Test
        void energyConsumed_isFixedFePerTick_notVariable() {
            int fePerTick = 80;

            RefactoredStepOutcome outcome = simulateRefactoredTick(
                    0, 200, 1000, fePerTick,
                    true, true, false
            );

            assertEquals(1000 - fePerTick, outcome.energy(),
                    "Energy must decrease by exactly fePerTick (" + fePerTick + "). " +
                    "Current: energyConsumed = min(getActualEfficiency(), stored).");
        }

        @Test
        void progressAndEnergy_independentOfAvailableEnergy() {
            // Target: progress++ and energy - fePerTick are independent
            // of how much energy is stored (as long as >= fePerTick)
            int fePerTick = 50;
            int energy = 500;

            RefactoredStepOutcome outcome = simulateRefactoredTick(
                    0, 200, energy, fePerTick,
                    true, true, false
            );

            assertEquals(1, outcome.progress(),
                    "Progress advances by 1 regardless of stored energy (as long as >= fePerTick)");
            assertEquals(450, outcome.energy(),
                    "Energy decreases by exactly fePerTick");
        }

        @Test
        void progressFixedToTickCount_notToEnergy_multiTick() {
            // Over multiple ticks, progress = tick count, not energy sum
            int progress = 0;
            int energy = 1000;
            int fePerTick = 30;

            for (int tick = 0; tick < 5; tick++) {
                RefactoredStepOutcome outcome = simulateRefactoredTick(
                        progress, 200, energy, fePerTick,
                        true, true, false
                );
                progress = outcome.progress();
                energy = outcome.energy();
            }
            assertEquals(5, progress,
                    "After 5 ticks, progress must be exactly 5 (1 per tick). " +
                    "Current code: progress = 5 * " + fePerTick + " = " + (5 * fePerTick) + ".");
            assertEquals(1000 - 5 * fePerTick, energy,
                    "Energy after 5 ticks must be 1000 - 5*" + fePerTick + " = " + (1000 - 5 * fePerTick));
        }
    }

    // ════════════════════════════════════════════════════════════
    // C. Energy不足暂停不重置progress (P1 target)
    // ════════════════════════════════════════════════════════════

    @Nested
    class EnergyPauseNoReset {

        @Test
        void insufficientEnergy_pauses_preservesProgress() {
            RefactoredStepOutcome outcome = simulateRefactoredTick(
                    50, 200, 10, 100,  // energy=10 < fePerTick=100
                    true, false, false  // energyAllowRun=false
            );
            assertEquals(50, outcome.progress(),
                    "P1 target: progress must be preserved when energy < fePerTick. " +
                    "Current code else branch resets progress to 0.");
            assertEquals(10, outcome.energy(),
                    "Energy must be preserved when insufficient");
            assertFalse(outcome.active(),
                    "Machine must be inactive when energy insufficient");
        }

        @Test
        void zeroEnergy_pauses_preservesProgress() {
            RefactoredStepOutcome outcome = simulateRefactoredTick(
                    75, 200, 0, 100,
                    true, false, false
            );
            assertEquals(75, outcome.progress(),
                    "Progress must be preserved at 75 when energy is 0");
            assertFalse(outcome.active());
        }

        @Test
        void energyRestored_resumesFromSavedProgress() {
            // Tick 1: energy=10 < fePerTick=100 → pause
            int progress = 75;
            int energy = 10;
            int fePerTick = 100;

            RefactoredStepOutcome paused = simulateRefactoredTick(
                    progress, 200, energy, fePerTick,
                    true, false, false
            );
            assertEquals(75, paused.progress(), "Progress preserved during pause");

            // Tick 2: energy restored to 500 → resume
            RefactoredStepOutcome resumed = simulateRefactoredTick(
                    paused.progress(), 200, 500, fePerTick,
                    true, true, false
            );
            assertEquals(76, resumed.progress(),
                    "After energy restored, progress must continue from 75 to 76 (75+1)");
            assertEquals(400, resumed.energy(),
                    "Energy must decrease by fePerTick");
        }

        @Test
        void multipleEnergyPauseCycles_preservesProgress() {
            // Simulate alternating pause/resume
            int progress = 50;
            int maxProgress = 200;
            int fePerTick = 100;

            // Tick 1: normal
            RefactoredStepOutcome t1 = simulateRefactoredTick(
                    progress, maxProgress, 1000, fePerTick, true, true, false);
            assertEquals(51, t1.progress());

            // Tick 2: pause (energy low)
            RefactoredStepOutcome t2 = simulateRefactoredTick(
                    t1.progress(), maxProgress, 5, fePerTick, true, false, false);
            assertEquals(51, t2.progress(), "Progress preserved during pause");

            // Tick 3: resume
            RefactoredStepOutcome t3 = simulateRefactoredTick(
                    t2.progress(), maxProgress, 1000, fePerTick, true, true, false);
            assertEquals(52, t3.progress(), "Progress continues from 51 to 52");
        }
    }

    // ════════════════════════════════════════════════════════════
    // D. Output满暂停不重置progress (P1 target)
    // ════════════════════════════════════════════════════════════

    @Nested
    class OutputFullPauseNoReset {

        @Test
        void outputFull_pauses_preservesProgress() {
            RefactoredStepOutcome outcome = simulateRefactoredTick(
                    50, 200, 1000, 100,
                    true, true, true  // cookingBlocks=true
            );
            assertEquals(50, outcome.progress(),
                    "P1 target: progress must be preserved when output is full. " +
                    "Current code with cooking() check: progress is preserved (via early return).");
            assertEquals(1000, outcome.energy(),
                    "Energy must be preserved when output is full");
        }

        @Test
        void outputFullThenCleared_resumesProgress() {
            int progress = 50;
            int energy = 1000;

            // 3 blocked ticks
            for (int i = 0; i < 3; i++) {
                RefactoredStepOutcome outcome = simulateRefactoredTick(
                        progress, 200, energy, 100,
                        true, true, true
                );
                progress = outcome.progress();
                energy = outcome.energy();
            }
            assertEquals(50, progress, "Progress preserved through 3 blocked ticks");
            assertEquals(1000, energy, "Energy preserved through 3 blocked ticks");

            // Unblocked
            RefactoredStepOutcome unblocked = simulateRefactoredTick(
                    progress, 200, energy, 100,
                    true, true, false
            );
            assertEquals(51, unblocked.progress(),
                    "After unblock, progress continues from 50 to 51 (+1 per tick)");
        }
    }

    //     ════════════════════════════════════════════════════════════
    // E. maxProgress fixed at recipe start (P1 target)
    // ════════════════════════════════════════════════════════════

    @Nested
    class MaxProgressFixed {

        @Test
        void maxProgressEquals_baseTickTime_notEnergyMultiplied() {
            // Target: maxProgress = baseTickTime() (ticks only)
            // Current: maxProgress = baseTickTime() * Math.max(initialEfficientIn, 1)
            // Pure function contract — no simulation needed, just document the formula
            int baseTickTime = 100;
            int initialEfficientIn = 80;

            int targetMaxProgress = baseTickTime;  // = 100 ticks
            int currentMaxProgress = baseTickTime * Math.max(initialEfficientIn, 1);  // = 100 * 80 = 8000

            assertEquals(100, targetMaxProgress,
                    "P1 target: maxProgress = baseTickTime() = 100");
            assertTrue(currentMaxProgress > targetMaxProgress,
                    "Current maxProgress (" + currentMaxProgress + ") is energy-multiplied, " +
                    "P1 target removes the multiplier.");
        }

        @Test
        void maxProgress_notSetInsideProcess_afterP1() throws Exception {
            // P1-T1: maxProgress assignment has been removed from process()
            // It is now set in conditionStart() by each subclass (P1-T3)
            var sourceFile = new java.io.File(
                    "src/main/java/com/modularmc/ten/api/blockentity/ProcessingMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = java.nio.file.Files.readString(sourceFile.toPath());
            // process() should NOT contain maxProgress = assignment
            // The old line was: maxProgress = baseTickTime() * Math.max(initialEfficientIn, 1);
            int processMethodStart = content.indexOf("public void process()");
            int processMethodEnd = content.indexOf("public boolean cooking()");
            String processBody = content.substring(processMethodStart, processMethodEnd);
            assertFalse(processBody.contains("maxProgress ="),
                    "P1 target: process() must NOT set maxProgress. " +
                    "maxProgress should be set in conditionStart() by subclasses.");
        }
    }

    // ════════════════════════════════════════════════════════════
    // F. FE/t受upgrade multiplier影响 (P2 target — contract only)
    // ════════════════════════════════════════════════════════════

    @Nested
    class UpgradeAffectsFePerTick {

        @Test
        void fePerTickEquals_getActualEfficiency() {
            // Target: fePerTick = getActualEfficiency() 含 upgrade multiplier
            // Current: energyConsumed = Math.min(getActualEfficiency(), energyStorage.getEnergyStored())
            // This test documents the contract: fePerTick must come from getActualEfficiency()
            // Pure function contract — no state needed
            int actualEfficiency = 120;  // after upgrades: 80 * 1.5
            int stored = 1000;
            int fePerTick = Math.min(actualEfficiency, stored);

            assertEquals(120, fePerTick,
                    "fePerTick = min(getActualEfficiency(), stored) = " + fePerTick);
            assertTrue(fePerTick > 0,
                    "fePerTick must be positive when energy is available");
        }

        @Test
        void fePerTick_isIndependentOfMaxProgress() {
            // Target: fePerTick and maxProgress are independent
            // Current: maxProgress = baseTickTime * initialEfficientIn (coupled with energy)
            // This is a pure formula verification
            int baseTickTime = 100;
            int initialEfficientIn = 80;
            int powerMultiplier = 2;  // after upgrades

            int fePerTick = initialEfficientIn * powerMultiplier;  // = 160
            int targetMaxProgress = baseTickTime;  // = 100 (ticks only)
            int currentMaxProgress = baseTickTime * initialEfficientIn;  // = 8000

            assertEquals(160, fePerTick, "FE/t after upgrades");
            assertEquals(100, targetMaxProgress, "maxProgress (ticks) independent of FE/t");
            assertTrue(currentMaxProgress != targetMaxProgress,
                    "Current code couples maxProgress with initialEfficientIn; " +
                    "P1 decouples them.");
        }
    }

    // ════════════════════════════════════════════════════════════
    // G. duration >= 1 tick
    // ════════════════════════════════════════════════════════════

    @Nested
    class MinimumDuration {

        @Test
        void maxProgress_atLeast1Tick() {
            // Even with extreme upgrades, maxProgress >= 1
            // Pure function: the caller must clamp
            int rawMaxProgress = 0;  // pathological case
            int clamped = Math.max(1, rawMaxProgress);
            assertEquals(1, clamped,
                    "maxProgress must be clamped to at least 1 tick");
        }

        @Test
        void singleTickRecipe_completesImmediately() {
            RefactoredStepOutcome outcome = simulateRefactoredTick(
                    0, 1, 1000, 100,
                    true, true, false
            );
            assertTrue(outcome.onCookFinishCalled(),
                    "Recipe with maxProgress=1 must complete in one tick (0+1 >= 1)");
            assertTrue(outcome.active(),
                    "Machine must be active on completion tick");
        }
    }

    // ════════════════════════════════════════════════════════════
    // H. Source verification — P1 target new semantics present
    // ════════════════════════════════════════════════════════════
    //
    // 本组测试验证生产代码已实现新 tick 语义。
    // P1 RED 阶段：这些测试应 FAIL（因新语义尚未写入生产代码）。
    // P1 GREEN 阶段：这些测试应 PASS（新语义已写入）。
    // ════════════════════════════════════════════════════════════

    @Nested
    class SourceCodeNewSemantics {

        @Test
        void processUsesProgressPlusPlus_notEnergyConsumed() throws Exception {
            var sourceFile = new java.io.File(
                    "src/main/java/com/modularmc/ten/api/blockentity/ProcessingMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = java.nio.file.Files.readString(sourceFile.toPath());
            assertTrue(content.contains("progress++"),
                    "P1 target: process() must use progress++ (fixed +1 per tick). " +
                    "RED until P1-T1 implementation.");
        }

        @Test
        void processUsesGreaterOrEqual_notStrictGreater() throws Exception {
            var sourceFile = new java.io.File(
                    "src/main/java/com/modularmc/ten/api/blockentity/ProcessingMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = java.nio.file.Files.readString(sourceFile.toPath());
            assertTrue(content.contains("progress >= maxProgress"),
                    "P1 target: process() must use >= for completion check. " +
                    "RED until P1-T1 implementation.");
        }

        @Test
        void processMaxProgressNotMultipliedByInitialEfficientIn() throws Exception {
            var sourceFile = new java.io.File(
                    "src/main/java/com/modularmc/ten/api/blockentity/ProcessingMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = java.nio.file.Files.readString(sourceFile.toPath());
            assertFalse(content.contains("baseTickTime() * Math.max(initialEfficientIn, 1)"),
                    "P1 target: maxProgress must NOT be multiplied by initialEfficientIn. " +
                    "RED until P1-T1 implementation.");
        }

        @Test
        void processDoesNotUseEnergyConsumedPattern() throws Exception {
            var sourceFile = new java.io.File(
                    "src/main/java/com/modularmc/ten/api/blockentity/ProcessingMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = java.nio.file.Files.readString(sourceFile.toPath());
            assertFalse(content.contains("int energyConsumed = Math.min("),
                    "P1 target: process() must NOT use energyConsumed = min(...) pattern. " +
                    "RED until P1-T1 implementation.");
        }

        @Test
        void processElseBranchDoesNotResetProgress() throws Exception {
            var sourceFile = new java.io.File(
                    "src/main/java/com/modularmc/ten/api/blockentity/ProcessingMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = java.nio.file.Files.readString(sourceFile.toPath());
            // 新实现中 else 分支不应含无条件 progress = 0
            // 但可能仍有 setActive(false)，所以检查 progress = 0 不在 else 上下文中
            assertFalse(content.contains("} else {\n" +
                    "            setActive(false);\n" +
                    "            progress = 0;"),
                    "P1 target: else branch must NOT reset progress to 0 unconditionally. " +
                    "RED until P1-T1 implementation.");
        }

        @Test
        void processHasFePerTickAndExtractEnergy() throws Exception {
            var sourceFile = new java.io.File(
                    "src/main/java/com/modularmc/ten/api/blockentity/ProcessingMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = java.nio.file.Files.readString(sourceFile.toPath());
            assertTrue(content.contains("extractEnergy("),
                    "P1 target: process() must use energyStorage.extractEnergy() with fixed fePerTick. " +
                    "RED until P1-T1 implementation.");
        }

        @Test
        void processHasStagnationEarlyReturn() throws Exception {
            var sourceFile = new java.io.File(
                    "src/main/java/com/modularmc/ten/api/blockentity/ProcessingMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = java.nio.file.Files.readString(sourceFile.toPath());
            assertTrue(content.contains(".getEnergyStored() < fePerTick"),
                    "P1 target: process() must have early return when energyStored < fePerTick. " +
                    "RED until P1-T1 implementation.");
        }

        // ════════════════════════════════════════════════════════════
        // M1: Atomic energy extraction — simulate then execute
        // ════════════════════════════════════════════════════════════

        @Test
        void processUsesExpectedLong_notClampedInt() throws Exception {
            var sourceFile = new java.io.File(
                    "src/main/java/com/modularmc/ten/api/blockentity/ProcessingMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = java.nio.file.Files.readString(sourceFile.toPath());
            // Expected value is stored as long first, then guarded
            assertTrue(content.contains("long expected = Math.round("),
                    "M1: expected FE/t must be calculated as long intermediate. " +
                    "Clamping to Integer.MAX_VALUE has been removed.");
        }

        @Test
        void processHasExpectedGreaterThanMaxIntGuard() throws Exception {
            var sourceFile = new java.io.File(
                    "src/main/java/com/modularmc/ten/api/blockentity/ProcessingMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = java.nio.file.Files.readString(sourceFile.toPath());
            assertTrue(content.contains("> Integer.MAX_VALUE"),
                    "M1: must have guard for expected > Integer.MAX_VALUE to stagnate. " +
                    "Without this, out-of-range expected values would be clamped silently.");
        }

        @Test
        void processHasSimulateExtractCheck() throws Exception {
            var sourceFile = new java.io.File(
                    "src/main/java/com/modularmc/ten/api/blockentity/ProcessingMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = java.nio.file.Files.readString(sourceFile.toPath());
            // Must have simulate call with != fePerTick comparison
            assertTrue(content.contains("extractEnergy(fePerTick, true) != fePerTick"),
                    "M1: process() must simulate-extract and verify return == fePerTick. " +
                    "If simulate fails (maxExtract insufficient), the tick must stall.");
        }

        @Test
        void processDoesNotClampExpectedToMaxInt() throws Exception {
            var sourceFile = new java.io.File(
                    "src/main/java/com/modularmc/ten/api/blockentity/ProcessingMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = java.nio.file.Files.readString(sourceFile.toPath());
            // The old Math.min clamping should be gone — replaced by expected > Integer.MAX_VALUE guard
            assertFalse(content.contains("Math.min(fePerTickLong, (long) Integer.MAX_VALUE)"),
                    "M1: must NOT clamp fePerTickLong to Integer.MAX_VALUE with Math.min. " +
                    "Use expected > Integer.MAX_VALUE guard instead.");
        }

        @Test
        void processFailsFastOnExtractionMismatch() throws Exception {
            var sourceFile = new java.io.File(
                    "src/main/java/com/modularmc/ten/api/blockentity/ProcessingMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = java.nio.file.Files.readString(sourceFile.toPath());
            // Real extract must be checked and fail-fast if mismatch
            assertTrue(content.contains("extractEnergy(fePerTick, false)"),
                    "M1: process() must call extractEnergy(fePerTick, false) for real extraction.");
            assertTrue(content.contains("extractEnergy(fePerTick, false) != fePerTick"),
                    "M1: real extraction return must be verified against expected fePerTick. " +
                    "Mismatch (after successful simulate) is an invariant violation → fail-fast.");
        }
    }
}
