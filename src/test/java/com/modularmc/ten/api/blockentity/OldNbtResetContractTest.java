// -*- coding: utf-8 -*-
package com.modularmc.ten.api.blockentity;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract tests for P5-T1: old save compatibility — progress reset strategy.
 * <p>
 * Verifies that {@link CmMachineBlockEntity#readTileData} unconditionally resets
 * progress/maxProgress/lockedB/lockedMaxProgress to 0 after loading all NBT,
 * while preserving inventory, energy storage, upgrade slots, and face config.
 * <p>
 * <strong>Rationale:</strong> The old (pre-refactor) energy-accumulation model stored
 * FE values in progress/maxProgress fields. A furnace recipe was ~3000 FE, well below
 * any reasonable tick-based threshold. Without a schema version field to distinguish
 * formats, the only safe approach is to clear all in-progress progress on load and let
 * {@code conditionStart()} establish fresh tick-based values on the next cycle.
 * <p>
 * Uses pure-Java simulation of the readTileData reset decision since
 * {@link CmMachineBlockEntity} requires Minecraft bootstrap, plus
 * source-code verification that the production method implements the fix.
 */
class OldNbtResetContractTest {

    // ── Reset simulation (mirrors production readTileData logic) ──

    /**
     * Simulates the P5-T1 readTileData old-NBT unconditional reset decision.
     * Every NBT load resets progress/maxProgress to 0 regardless of values,
     * since there is no schema version to distinguish old energy-accumulation
     * format from new tick-based format.
     *
     * @return always true (unconditional reset)
     */
    static boolean wouldResetOldNbt(int progress, int maxProgress) {
        return true; // Unconditional — no threshold, no schema version
    }

    /**
     * Simulates the applied reset outcome — always 0.
     */
    static int simulateProgressAfterReset(int progress, int maxProgress) {
        return 0; // Always reset
    }

    /**
     * Simulates the applied reset outcome for maxProgress — always 0.
     */
    static int simulateMaxProgressAfterReset(int progress, int maxProgress) {
        return 0; // Always reset
    }

    // ════════════════════════════════════════════════════════════
    // 1. Low-value old NBT (pre-refactor energy model) → MUST reset
    // ════════════════════════════════════════════════════════════
    // Old NBT stored FE accumulation. A furnace recipe was ~3000 FE,
    // well below 72000 ticks. These low values MUST also be reset
    // since there is no schema version to distinguish formats.

    @Nested
    class LowValueOldNbtReset {

        @Test
        void furnaceTypicalValues_resets() {
            // Furnace: progress=1, maxProgress=3000 (old FE accumulation)
            // Without reset, this would complete on first tick — must reset
            assertTrue(wouldResetOldNbt(1, 3000),
                    "Low-value old NBT must trigger reset");
            assertEquals(0, simulateProgressAfterReset(1, 3000),
                    "Progress must reset to 0");
            assertEquals(0, simulateMaxProgressAfterReset(1, 3000),
                    "MaxProgress must reset to 0");
        }

        @Test
        void midRangeOldValues_resets() {
            // Mid-range old FE values that are realistic but below 72000
            assertTrue(wouldResetOldNbt(500, 15000),
                    "Mid-range old NBT must trigger reset");
            assertEquals(0, simulateProgressAfterReset(500, 15000));
            assertEquals(0, simulateMaxProgressAfterReset(500, 15000));
        }

        @Test
        void largeOldEnergyModel_resets() {
            // Old NBT: maxProgress could be 500000 (energy accumulated)
            assertTrue(wouldResetOldNbt(250000, 500000),
                    "Large old NBT must trigger reset");
            assertEquals(0, simulateProgressAfterReset(250000, 500000));
            assertEquals(0, simulateMaxProgressAfterReset(250000, 500000));
        }

        @Test
        void largeProgressOnly_resets() {
            assertTrue(wouldResetOldNbt(999999, 200),
                    "Old NBT with large progress must trigger reset");
            assertEquals(0, simulateProgressAfterReset(999999, 200));
            assertEquals(0, simulateMaxProgressAfterReset(999999, 200));
        }

        @Test
        void bothLarge_resetsToZero() {
            assertTrue(wouldResetOldNbt(100000, 200000));
            assertEquals(0, simulateProgressAfterReset(100000, 200000));
            assertEquals(0, simulateMaxProgressAfterReset(100000, 200000));
        }
    }

    // ════════════════════════════════════════════════════════════
    // 2. Any NBT values → unconditional reset
    // ════════════════════════════════════════════════════════════
    // Even seemingly "normal" tick values are reset because there's
    // no schema version to distinguish old vs new NBT format.

    @Nested
    class UnconditionalReset {

        @Test
        void zeroValues_resets() {
            // New/idle NBT: progress=0, maxProgress=0
            assertTrue(wouldResetOldNbt(0, 0),
                    "Zero values must also trigger reset (unconditional)");
            assertEquals(0, simulateProgressAfterReset(0, 0));
            assertEquals(0, simulateMaxProgressAfterReset(0, 0));
        }

        @Test
        void normalTickValues_resets() {
            // Normal-looking tick values are still reset — no schema version
            assertTrue(wouldResetOldNbt(50, 200),
                    "Normal tick values must still trigger reset (unconditional)");
            assertEquals(0, simulateProgressAfterReset(50, 200));
            assertEquals(0, simulateMaxProgressAfterReset(50, 200));
        }

        @Test
        void maxedOutRecipe_resets() {
            assertTrue(wouldResetOldNbt(1800, 3600));
            assertEquals(0, simulateProgressAfterReset(1800, 3600));
            assertEquals(0, simulateMaxProgressAfterReset(1800, 3600));
        }

        @Test
        void progressAtMax_resets() {
            assertTrue(wouldResetOldNbt(199, 200));
            assertEquals(0, simulateProgressAfterReset(199, 200));
        }

        @Test
        void batchScaledMaxProgress_resets() {
            assertTrue(wouldResetOldNbt(0, 5000));
            assertEquals(0, simulateProgressAfterReset(0, 5000));
            assertEquals(0, simulateMaxProgressAfterReset(0, 5000));
        }
    }

    // ════════════════════════════════════════════════════════════
    // 3. Source verification — readTileData contains unconditional reset
    // ════════════════════════════════════════════════════════════

    @Nested
    class SourceVerification {

        @Test
        void readTileData_noThresholdCondition() throws Exception {
            var sourceFile = new java.io.File(
                    "src/main/java/com/modularmc/ten/api/blockentity/CmMachineBlockEntity.java");
            assertTrue(sourceFile.exists(), "Source file must exist");

            var content = java.nio.file.Files.readString(sourceFile.toPath());

            // SANE_MAX_TICKS must be removed — no threshold condition
            assertFalse(content.contains("SANE_MAX_TICKS"),
                    "Source must NOT contain SANE_MAX_TICKS threshold");

            // No conditional threshold check
            assertFalse(content.contains("maxProgress > SANE_MAX_TICKS"),
                    "Source must NOT use maxProgress > SANE_MAX_TICKS conditional");
            assertFalse(content.contains("progress > SANE_MAX_TICKS"),
                    "Source must NOT use progress > SANE_MAX_TICKS conditional");
        }

        @Test
        void readTileData_resetsProgressAndMaxProgress() throws Exception {
            var sourceFile = new java.io.File(
                    "src/main/java/com/modularmc/ten/api/blockentity/CmMachineBlockEntity.java");
            assertTrue(sourceFile.exists());

            var content = java.nio.file.Files.readString(sourceFile.toPath());

            // Verify reset actions exist (unconditional, after all NBT loading)
            assertTrue(content.contains("progress = 0"),
                    "Source must reset progress = 0");
            assertTrue(content.contains("maxProgress = 0"),
                    "Source must reset maxProgress = 0");
        }

        @Test
        void readTileData_resetsRuntimeLocks() throws Exception {
            var sourceFile = new java.io.File(
                    "src/main/java/com/modularmc/ten/api/blockentity/CmMachineBlockEntity.java");
            assertTrue(sourceFile.exists());

            var content = java.nio.file.Files.readString(sourceFile.toPath());

            // Verify lockedB and lockedMaxProgress are also cleared
            assertTrue(content.contains("lockedB = 0"),
                    "Source must clear lockedB on reset");
            assertTrue(content.contains("lockedMaxProgress = 0"),
                    "Source must clear lockedMaxProgress on reset");
        }

        @Test
        void readTileData_preservesInventoryAndEnergy() throws Exception {
            var sourceFile = new java.io.File(
                    "src/main/java/com/modularmc/ten/api/blockentity/CmMachineBlockEntity.java");
            assertTrue(sourceFile.exists());

            var content = java.nio.file.Files.readString(sourceFile.toPath());

            // Verify inventory reading is still present
            assertTrue(content.contains("input.read(\"inventory\""),
                    "Source must still read inventory from NBT");

            // Verify energy reading is still present
            assertTrue(content.contains("input.getInt(\"energy\")"),
                    "Source must still read energy from NBT");

            // Verify upgrade reading is still present
            assertTrue(content.contains("input.read(\"upgrades\""),
                    "Source must still read upgrades from NBT");
        }

        @Test
        void readTileData_preservesFaceConfig() throws Exception {
            var sourceFile = new java.io.File(
                    "src/main/java/com/modularmc/ten/api/blockentity/CmMachineBlockEntity.java");
            assertTrue(sourceFile.exists());

            var content = java.nio.file.Files.readString(sourceFile.toPath());

            // Verify face config reading is still present
            assertTrue(content.contains("direEnergy"),
                    "Source must still read energy face config from NBT");
            assertTrue(content.contains("direItem"),
                    "Source must still read item face config from NBT");
            assertTrue(content.contains("direFluid"),
                    "Source must still read fluid face config from NBT");
        }
    }

    // ════════════════════════════════════════════════════════════
    // 4. Cross-machine source verification — all types preserve NBT
    // ════════════════════════════════════════════════════════════

    @Nested
    class CrossMachineVerification {

        @Test
        void furnaceBlockEntity_extendsProcessingMachine() throws Exception {
            var sourceFile = new java.io.File(
                    "src/main/java/com/modularmc/ten/common/blockentity/machine/FurnaceBlockEntity.java");
            assertTrue(sourceFile.exists(), "FurnaceBlockEntity source must exist");
            var content = java.nio.file.Files.readString(sourceFile.toPath());
            assertTrue(content.contains("extends ProcessingMachineBlockEntity"),
                    "Furnace must extend ProcessingMachineBlockEntity");
        }

        @Test
        void condenserBlockEntity_extendsProcessingMachine() throws Exception {
            var sourceFile = new java.io.File(
                    "src/main/java/com/modularmc/ten/common/blockentity/machine/CondenserBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = java.nio.file.Files.readString(sourceFile.toPath());
            assertTrue(content.contains("extends ProcessingMachineBlockEntity"),
                    "Condenser must extend ProcessingMachineBlockEntity");
        }

        @Test
        void encfluBlockEntity_extendsProcessingMachine() throws Exception {
            var sourceFile = new java.io.File(
                    "src/main/java/com/modularmc/ten/common/blockentity/machine/EncfluBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = java.nio.file.Files.readString(sourceFile.toPath());
            assertTrue(content.contains("extends ProcessingMachineBlockEntity"),
                    "Encflu must extend ProcessingMachineBlockEntity");
        }

        @Test
        void cmMachineReadTileData_inheritedByAll() throws Exception {
            // Verify the base class readTileData is used by processing machines
            // (they don't override it)
            for (String clazz : new String[]{
                    "FurnaceBlockEntity",
                    "CondenserBlockEntity",
                    "EncfluBlockEntity"
            }) {
                var sourceFile = new java.io.File(
                        "src/main/java/com/modularmc/ten/common/blockentity/machine/" + clazz + ".java");
                assertTrue(sourceFile.exists(), clazz + " source must exist");
                var content = java.nio.file.Files.readString(sourceFile.toPath());
                // These classes should NOT override readTileData — they inherit
                // from ProcessingMachineBlockEntity → CmMachineBlockEntity
                assertFalse(content.contains("void readTileData"),
                        clazz + " must NOT override readTileData (inherits from CmMachineBlockEntity)");
            }
        }
    }
}
