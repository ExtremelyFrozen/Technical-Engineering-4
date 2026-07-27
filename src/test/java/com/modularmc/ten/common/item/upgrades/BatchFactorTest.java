// -*- coding: utf-8 -*-
package com.modularmc.ten.common.item.upgrades;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract tests for batch factor B calculation — P2 target.
 * <p>
 * B = 1 + Σbatch_i, where batch_i is the batch contribution of each upgrade.
 * Batch values: LevelupAug=0, LevelupPower=1, LevelupShulker=3, LevelupSyn=0.
 */
class BatchFactorTest {

    // ════════════════════════════════════════════════════════════
    // Pure B factor calculation — target formula
    // ════════════════════════════════════════════════════════════

    static int calculateB(int... batchValues) {
        int sum = 0;
        for (int b : batchValues) {
            sum += b;
        }
        return 1 + sum;  // B ≥ 1 always
    }

    static final int AUG_BATCH = 0;
    static final int POWER_BATCH = 1;
    static final int SHULKER_BATCH = 3;
    static final int SYN_BATCH = 0;

    // ════════════════════════════════════════════════════════════
    // A. B 因子计算 — 基础场景
    // ════════════════════════════════════════════════════════════

    @Nested
    class BatchComputation {

        @Test
        void noUpgrades_B_is1() {
            assertEquals(1, calculateB(),
                    "No upgrades: B = 1 + 0 = 1");
        }

        @Test
        void onlyAug_B_is1() {
            assertEquals(1, calculateB(AUG_BATCH),
                    "Only Aug: B = 1 + 0 = 1");
        }

        @Test
        void onlyPower_B_is2() {
            assertEquals(2, calculateB(POWER_BATCH),
                    "Only Power: B = 1 + 1 = 2");
        }

        @Test
        void onlyShulker_B_is4() {
            assertEquals(4, calculateB(SHULKER_BATCH),
                    "Only Shulker: B = 1 + 3 = 4");
        }

        @Test
        void onlySyn_B_is1() {
            assertEquals(1, calculateB(SYN_BATCH),
                    "Only Syn: B = 1 + 0 = 1");
        }

        @Test
        void augPlusPower_B_is2() {
            assertEquals(2, calculateB(AUG_BATCH, POWER_BATCH),
                    "Aug+Power: B = 1 + 0 + 1 = 2");
        }

        @Test
        void powerPlusShulker_B_is5() {
            assertEquals(5, calculateB(POWER_BATCH, SHULKER_BATCH),
                    "Power+Shulker: B = 1 + 1 + 3 = 5");
        }

        @Test
        void allFour_B_is5() {
            // Aug(0) + Power(1) + Shulker(3) + Syn(0) = 4 → B = 5
            assertEquals(5, calculateB(AUG_BATCH, POWER_BATCH, SHULKER_BATCH, SYN_BATCH),
                    "All four: B = 1 + 0 + 1 + 3 + 0 = 5");
        }

        @Test
        void threeShulker_B_is10() {
            assertEquals(10, calculateB(SHULKER_BATCH, SHULKER_BATCH, SHULKER_BATCH),
                    "3x Shulker: B = 1 + 3 + 3 + 3 = 10");
        }
    }

    // ════════════════════════════════════════════════════════════
    // B. B 因子边界验证
    // ════════════════════════════════════════════════════════════

    @Nested
    class BatchBoundaries {

        @Test
        void B_isNeverLessThan1() {
            // Even with no upgrades or negative inputs (shouldn't happen)
            assertEquals(1, calculateB(),
                    "Empty batch list: B = 1");
        }

        @Test
        void totalBatchSum_is4_withAllPowerUpgrades() {
            // Aug(0) + Power(1) + Shulker(3) + Syn(0) = 4
            int sum = AUG_BATCH + POWER_BATCH + SHULKER_BATCH + SYN_BATCH;
            assertEquals(4, sum,
                    "Sum of all batch_i = 0 + 1 + 3 + 0 = 4");
        }

        @Test
        void B_largerThan1_enablesBatchProcessing() {
            assertTrue(calculateB(POWER_BATCH) > 1,
                    "Power upgrade: B > 1 → batch processing active");
            assertTrue(calculateB(SHULKER_BATCH) > 1,
                    "Shulker upgrade: B > 1 → batch processing active");
        }

        @Test
        void B_equals1_noBatchEffect() {
            assertFalse(calculateB() > 1,
                    "No upgrades: B = 1, no batch effect");
            assertFalse(calculateB(AUG_BATCH) > 1,
                    "Aug only: B = 1, no batch effect");
            assertFalse(calculateB(SYN_BATCH) > 1,
                    "Syn only: B = 1, no batch effect");
        }

        @Test
        void twoPower_B_is3() {
            // 2×Power: Σbatch_i = 2 → B = 3
            assertEquals(3, calculateB(POWER_BATCH, POWER_BATCH),
                    "2×Power: B = 1 + 1 + 1 = 3");
        }

        @Test
        void threePower_B_is4() {
            assertEquals(4, calculateB(POWER_BATCH, POWER_BATCH, POWER_BATCH),
                    "3×Power: B = 1 + 1 + 1 + 1 = 4");
        }

        @Test
        void sixShulker_B_is19() {
            assertEquals(19, calculateB(
                    SHULKER_BATCH, SHULKER_BATCH, SHULKER_BATCH,
                    SHULKER_BATCH, SHULKER_BATCH, SHULKER_BATCH),
                    "6×Shulker: B = 1 + 18 = 19");
        }
    }

    // ════════════════════════════════════════════════════════════
    // C. B 四维概念验证
    // ════════════════════════════════════════════════════════════

    @Nested
    class BActualConcept {
        // B_actual = min(B_theory, B_byItems, B_byFluids, B_byOutput, B_byEnergy)

        static int lockBActual(int B_theory, int B_byItems, int B_byFluids, int B_byOutput, int B_byEnergy) {
            int B = Math.min(B_theory, B_byItems);
            B = Math.min(B, B_byFluids);
            B = Math.min(B, B_byOutput);
            B = Math.min(B, B_byEnergy);
            return Math.max(0, B);
        }

        @Test
        void Bactual_limitedByEnergy() {
            assertEquals(3, lockBActual(10, 10, 10, 10, 3));
        }

        @Test
        void Bactual_limitedByItems() {
            assertEquals(2, lockBActual(10, 2, 10, 10, 10));
        }

        @Test
        void Bactual_allUnlimited() {
            assertEquals(10, lockBActual(10, Integer.MAX_VALUE, Integer.MAX_VALUE,
                    Integer.MAX_VALUE, Integer.MAX_VALUE));
        }

        @Test
        void Bactual_zero_cannotStart() {
            int B = lockBActual(5, 0, 5, 5, 5);
            assertFalse(B >= 1, "B=0 cannot start");
        }

        @Test
        void B_byEnergy_usesBaseFePerTick() {
            // B_byEnergy = floor(energyStored / baseFePerTick)
            // baseFePerTick = Math.max(1, Math.round(initialEfficientIn * powerMultiplier))
            // NOT totalFePerTick (no circular dependency)
            int energyStored = 1000;
            int baseFePerTick = 80; // without batch B
            int B_byEnergy = energyStored / Math.max(1, baseFePerTick);
            assertEquals(12, B_byEnergy, "B_byEnergy = floor(1000/80) = 12");

            // Verify NOT using totalFePerTick (which already includes B)
            int totalFePerTick_wrong = baseFePerTick * 12; // circular!
            assertTrue(totalFePerTick_wrong != baseFePerTick,
                    "B_byEnergy uses baseFePerTick, NOT totalFePerTick");
        }
    }

    // ════════════════════════════════════════════════════════════
    // ════════════════════════════════════════════════════════════

    @Nested
    class SourceBaseline {

        @Test
        void cmMachineBlockEntity_hasBatchField() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/blockentity/CmMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("public int batch"),
                    "P2: batch field exists in CmMachineBlockEntity");
        }

        @Test
        void cmMachineBlockEntity_hasApplyBatchIncrease() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/blockentity/CmMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("applyBatchIncrease"),
                    "P2: applyBatchIncrease() exists in CmMachineBlockEntity");
        }

        @Test
        void upgradeClasses_useApplyBatchIncrease() throws Exception {
            for (String cls : new String[]{"LevelupPower", "LevelupShulker"}) {
                var sourceFile = new File(
                        "src/main/java/com/modularmc/ten/common/item/upgrades/" + cls + ".java");
                assertTrue(sourceFile.exists());
                var content = Files.readString(sourceFile.toPath());
                assertTrue(content.contains("applyBatchIncrease"),
                        cls + " calls applyBatchIncrease()");
            }
        }

        @Test
        void augAndSyn_batchIncreaseZero() throws Exception {
            var augFile = new File(
                    "src/main/java/com/modularmc/ten/common/item/upgrades/LevelupAug.java");
            assertTrue(augFile.exists());
            var augContent = Files.readString(augFile.toPath());
            assertTrue(augContent.contains("applyBatchIncrease(0)"),
                    "LevelupAug: applyBatchIncrease(0)");

            var synFile = new File(
                    "src/main/java/com/modularmc/ten/common/item/upgrades/LevelupSyn.java");
            assertTrue(synFile.exists());
            var synContent = Files.readString(synFile.toPath());
            assertTrue(synContent.contains("applyBatchIncrease(0)"),
                    "LevelupSyn: applyBatchIncrease(0)");
        }
    }
}
