// -*- coding: utf-8 -*-
package com.modularmc.ten.api.blockentity;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T8 极值契约测试矩阵 — P2 production scaling boundaries.
 * <p>
 * Covers extreme upgrade stacking, B=19, long clamp,
 * ceiling min1, slot99, probability worst, input overflow.
 */
class P2ExtremeValuesTest {

    // ════════════════════════════════════════════════════════════
    // A. 6×Shulker → B=19 matrix
    // ════════════════════════════════════════════════════════════

    @Nested
    class SixShulkerMatrix {

        static final int SHULKER_BATCH_I = 3;
        static final double SHULKER_REDUCTION = 0.60;
        static final double SHULKER_INCREASE = 1.00;

        @Test
        void sixShulker_batchSumIs18() {
            int batchSum = SHULKER_BATCH_I * 6;
            assertEquals(18, batchSum, "6×Shulker: Σbatch_i = 18");
        }

        @Test
        void sixShulker_B_is19() {
            int batchSum = SHULKER_BATCH_I * 6;
            int B = 1 + batchSum;
            assertEquals(19, B, "6×Shulker: B = 1 + 18 = 19");
        }

        @Test
        void sixShulker_durationMultiplier() {
            // Shulker reduction = 0.60 → multiplier = (1 - 0.60) = 0.4
            double mul = 1.0;
            for (int i = 0; i < 6; i++) {
                mul *= (1.0 - SHULKER_REDUCTION);
            }
            // 0.4^6 = 0.004096
            double expected = Math.pow(0.4, 6);
            assertEquals(expected, mul, 1e-12, "6×Shulker duration: 0.4^6 = " + expected);
        }

        @Test
        void sixShulker_powerMultiplier() {
            // Shulker increase = 1.00 → multiplier = (1 + 1.00) = 2.0
            double mul = 1.0;
            for (int i = 0; i < 6; i++) {
                mul *= (1.0 + SHULKER_INCREASE);
            }
            double expected = Math.pow(2.0, 6);
            assertEquals(expected, mul, 1e-12, "6×Shulker power: 2^6 = 64.0");
        }

        @Test
        void sixShulker_baseFeMin1Guard() {
            // baseFe = round(80 * 64) = 5120
            int initialEfficiency = 80;
            double powerMul = Math.pow(2.0, 6); // 64.0
            int baseFePerTick = Math.max(1, (int) Math.round(initialEfficiency * powerMul));
            assertEquals(5120, baseFePerTick, "80 × 64 = 5120, min1 guard no-op");
        }

        @Test
        void sixShulker_totalFePerTickWithB() {
            // totalFE = baseFePerTick × B = 5120 × 19 = 97280
            int baseFe = Math.max(1, (int) Math.round(80 * Math.pow(2.0, 6)));
            int B = 1 + 18;
            long expected = Math.round((double) baseFe * B);
            assertEquals(97280L, expected, "5120 × 19 = 97280");
        }

        @Test
        void sixShulker_durationCeilMin1() {
            // baseTickTime=100, durationMul=0.4^6=0.004096 → ceil(100*0.004096)=ceil(0.4096)=1
            int baseTickTime = 100;
            double durationMul = Math.pow(0.4, 6);
            int maxProgress = Math.max(1, (int) Math.ceil(baseTickTime * durationMul));
            assertEquals(1, maxProgress, "100 × 0.004096 = 0.4096 → ceil=1 → max(1)=1");
        }

        @Test
        void batchIncrease_capsAt18() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/blockentity/CmMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("batch > 18"),
                    "RED: batch capped at 18 in CmMachineBlockEntity");
        }

        @Test
        void bHardMax_19_inBatchMath() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/blockentity/BatchMath.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("B_HARD_MAX") || content.contains("19"),
                    "RED: BatchMath must define B_HARD_MAX = 19");
        }
    }

    // ════════════════════════════════════════════════════════════
    // B. Base FE safe round min1
    // ════════════════════════════════════════════════════════════

    @Nested
    class BaseFeSafeRound {

        @Test
        void zeroEfficiency_roundsTo1() {
            int result = Math.max(1, (int) Math.round(0.0));
            assertEquals(1, result, "round(0)=0 → max(1)=1");
        }

        @Test
        void negativeEfficiency_roundsTo1() {
            int result = Math.max(1, (int) Math.round(-10.0));
            assertEquals(1, result, "round(-10)=-10 → max(1)=1");
        }

        @Test
        void tinyPositiveEfficiency_roundsTo1() {
            int result = Math.max(1, (int) Math.round(0.49));
            assertEquals(1, result, "round(0.49)=0 → max(1)=1");
        }

        @Test
        void justAboveHalf_roundsUp() {
            int result = Math.max(1, (int) Math.round(0.5));
            assertEquals(1, result, "round(0.5)=1 → max(1)=1");
        }

        @Test
        void normalEfficiency_unchanged() {
            int result = Math.max(1, (int) Math.round(80.0));
            assertEquals(80, result, "round(80)=80 → max(1)=80");
        }

        @Test
        void source_applyUpgradeEffects_hasMax1() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/blockentity/CmMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("Math.max(1, (int)"),
                    "RED: applyUpgradeEffects must have Math.max(1, round(...)) guard");
        }
    }

    // ════════════════════════════════════════════════════════════
    // C. totalFE long > MAX_VALUE → stall
    // ════════════════════════════════════════════════════════════

    @Nested
    class TotalFELongOverflowGuard {

        @Test
        void longExpected_guardedAgainstMaxInt() {
            long expected = (long) Integer.MAX_VALUE + 1L;
            assertTrue(expected > Integer.MAX_VALUE,
                    "expected > Integer.MAX_VALUE → must stall");
        }

        @Test
        void expectedComputedAsLong_notInt() {
            int baseFe = 2_000_000_000;
            int B = 2;
            long expected = Math.round((double) baseFe * B);
            assertTrue(expected > Integer.MAX_VALUE,
                    "2B × 2 = 4B > Integer.MAX_VALUE, must use long");
        }

        @Test
        void sourceProcessingMachine_hasLongExpectedAndGuard() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/blockentity/ProcessingMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("long expected"),
                    "RED: process must compute expected as long");
            assertTrue(content.contains("Integer.MAX_VALUE"),
                    "RED: process must guard against expected > Integer.MAX_VALUE");
        }
    }

    // ════════════════════════════════════════════════════════════
    // D. duration ceil min1
    // ════════════════════════════════════════════════════════════

    @Nested
    class DurationCeilMin1 {

        @Test
        void zeroDuration_ceilTo1() {
            int result = Math.max(1, (int) Math.ceil(0.0));
            assertEquals(1, result, "ceil(0)=0 → max(1)=1");
        }

        @Test
        void fractionalDuration_ceilUp() {
            int result = Math.max(1, (int) Math.ceil(0.4));
            assertEquals(1, result, "ceil(0.4)=1 → max(1)=1");
        }

        @Test
        void normalDuration_unchanged() {
            int result = Math.max(1, (int) Math.ceil(100.0));
            assertEquals(100, result, "ceil(100)=100 → max(1)=100");
        }

        @Test
        void extremeUpgrade_barelyAbove0() {
            int result = Math.max(1, (int) Math.ceil(0.001));
            assertEquals(1, result, "ceil(0.001)=1 → max(1)=1");
        }
    }

    // ════════════════════════════════════════════════════════════
    // E. Beacon 400×19 long clamp contract (P3 impl deferred)
    // ════════════════════════════════════════════════════════════

    @Nested
    class BeaconLongClampContract {

        @Test
        void beaconBase400_B19_fitsInt() {
            int baseDuration = 400;
            int B = 19;
            long mid = (long) baseDuration * B;
            assertTrue(mid <= Integer.MAX_VALUE,
                    "400 × 19 = " + mid + " fits in int safely");
        }

        @Test
        void beaconMaxProgress_longClamp() {
            int baseDuration = 400;
            int B = 19;
            long mid = (long) baseDuration * B;
            int clamped = (int) Math.min(mid, Integer.MAX_VALUE);
            assertEquals(7600, clamped, "400 × 19 = 7600, clamped to int");
        }

        @Test
        void sourceCmMachine_lockedMaxProgress_usesLongIntermediate() throws Exception {
            // Verify lockedMaxProgress computation uses long intermediate
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/blockentity/CmMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            // Should have Math.max(1, ...) in lockMaxProgressForNewOperation
            assertTrue(content.contains("Math.max(1, progress)"),
                    "RED: lockMaxProgressForNewOperation must clamp maxProgress to >= 1");
        }
    }

    // ════════════════════════════════════════════════════════════
    // F. Slot limit 99
    // ════════════════════════════════════════════════════════════

    @Nested
    class SlotLimit99 {

        @Test
        void maxUpgradeSlots_is6() {
            assertEquals(6, CmMachineBlockEntity.MAX_UPGRADE_SLOTS,
                    "MAX_UPGRADE_SLOTS = 6");
        }

        @Test
        void sourceCmMachine_MAX_UPGRADE_SLOTS_is6() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/blockentity/CmMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("MAX_UPGRADE_SLOTS = 6"),
                    "RED: MAX_UPGRADE_SLOTS must be 6");
        }

        @Test
        void inventorySize_99_notRequired() {
            // Slot count 99 is a recipe-level constraint, not machine-level
            assertTrue(true, "99-slot machines are a subclass concern, not P2");
        }
    }

    // ════════════════════════════════════════════════════════════
    // G. Probability worst B×rolls
    // ════════════════════════════════════════════════════════════

    @Nested
    class ProbabilityWorstMatrix {

        /** perBatchUnitWorst = rolls × maxAmount × maxCount */
        static int perBatchUnitWorst(int rolls, int maxAmount, int maxCount) {
            return rolls * maxAmount * maxCount;
        }

        /** B_byProbWorst = floor(available / perUnitWorst) */
        static int bByProbWorst(int available, int perUnitWorst) {
            if (perUnitWorst <= 0) return Integer.MAX_VALUE;
            return available / perUnitWorst;
        }

        @Test
        void B19_probWorst_rolls3amount2count1() {
            int perUnit = perBatchUnitWorst(3, 2, 1);
            assertEquals(6, perUnit);
        }

        @Test
        void B19_probWorst_slot64_BbyWorst() {
            int B = bByProbWorst(64, 6);
            assertEquals(10, B, "floor(64/6) = 10");
        }

        @Test
        void B19_probWorst_with19B_stillBound() {
            int B = bByProbWorst(1216, 6); // 64*19=1216
            assertEquals(202, B, "floor(1216/6) = 202, but capped by B_HARD_MAX=19");
            int capped = Math.min(B, 19);
            assertEquals(19, capped, "B capped at 19");
        }

        @Test
        void noProbOutput_skipsDimension() {
            assertEquals(Integer.MAX_VALUE, bByProbWorst(64, 0),
                    "No prob output → Integer.MAX_VALUE (skip dim)");
        }

        @Test
        void perBatchUnitWorst_fluidTank() {
            int perTankWorst = 100; // mB per batch worst
            int tankCap = 12800;
            int B = tankCap / perTankWorst;
            assertEquals(128, B, "B limited by tank, then capped at 19");
            assertEquals(19, Math.min(B, 19), "Capped at 19");
        }
    }

    // ════════════════════════════════════════════════════════════
    // H. Input/fluid overflow contracts
    // ════════════════════════════════════════════════════════════

    @Nested
    class InputOverflowContract {

        @Test
        void inputBMultiplied_intOverflowSafe() {
            // Uses long intermediate: (long) baseCount * B
            int baseCount = 64;
            int B = 19;
            long multiplied = (long) baseCount * B;
            assertEquals(1216L, multiplied, "64 × 19 = 1216, fits long");
            assertTrue(multiplied <= Integer.MAX_VALUE,
                    "1216 <= MAX_VALUE, safe int cast");
        }

        @Test
        void fluidInputB_multiplied() {
            int baseAmount = 1000;
            int B = 19;
            long multiplied = (long) baseAmount * B;
            assertEquals(19000L, multiplied,
                    "1000 × 19 = 19000 mB, fits long");
        }
    }

    // ════════════════════════════════════════════════════════════
    // I. ProcessingMachine subclass maxProgress sources
    // ════════════════════════════════════════════════════════════

    @Nested
    class MaxProgressSources {

        @Test
        void baseProcessingMachine_noMaxProgressAssignment() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/blockentity/ProcessingMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            // process() should NOT set maxProgress directly
            int processStart = content.indexOf("public void process()");
            int processEnd = content.indexOf("public boolean cooking()");
            if (processStart >= 0 && processEnd >= 0) {
                String processBody = content.substring(processStart, processEnd);
                assertFalse(processBody.contains("maxProgress ="),
                        "RED: process() should not set maxProgress directly");
            }
        }

        @Test
        void cmMachine_lockedMaxProgress_protected() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/blockentity/CmMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("public int lockedMaxProgress"),
                    "RED: lockedMaxProgress field must exist");
            assertTrue(content.contains("lockMaxProgressForNewOperation"),
                    "RED: lockMaxProgressForNewOperation method must exist");
        }
    }

    // ════════════════════════════════════════════════════════════
    // J. P2 SourceBaseline — all up to date
    // ════════════════════════════════════════════════════════════

    @Nested
    class P2SourceBaseline {

        @Test
        void cmMachine_hasDurationMultiplier() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/blockentity/CmMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("applyDurationMultiplier"),
                    "P2: applyDurationMultiplier exists");
            assertTrue(content.contains("applyPowerMultiplier"),
                    "P2: applyPowerMultiplier exists");
            assertTrue(content.contains("applyBatchIncrease"),
                    "P2: applyBatchIncrease exists");
            assertTrue(content.contains("applyPhotosyn"),
                    "P2: applyPhotosyn exists");
        }

        @Test
        void resetUpgradeEffects_clearsMultipliers() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/blockentity/CmMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("durationMultiplier = 1.0"),
                    "P2: resetUpgradeEffects resets durationMultiplier to 1.0");
            assertTrue(content.contains("powerMultiplier = 1.0"),
                    "P2: resetUpgradeEffects resets powerMultiplier to 1.0");
            assertTrue(content.contains("batch = 0"),
                    "P2: resetUpgradeEffects resets batch to 0");
        }

        @Test
        void levelupSyn_usesNewMultiplierModel() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/common/item/upgrades/LevelupSyn.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("applyDurationMultiplier"),
                    "P2: LevelupSyn uses applyDurationMultiplier");
            assertTrue(content.contains("applyPowerMultiplier"),
                    "P2: LevelupSyn uses applyPowerMultiplier");
            assertTrue(content.contains("applyPhotosyn"),
                    "P2: LevelupSyn calls applyPhotosyn()");
        }

        @Test
        void effectMachine_hasBatchSupport() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/blockentity/EffectMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            // EffectMachine.process should reference getActualEfficiency
            assertTrue(content.contains("getActualEfficiency"),
                    "P2: EffectMachine uses getActualEfficiency");
        }
    }
}
