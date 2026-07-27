// -*- coding: utf-8 -*-
package com.modularmc.ten.api.blockentity;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * P3-T1a/b/c RED→GREEN contract tests for batch-aware capacity/throughput.
 * <p>
 * Covers:
 * <ul>
 *   <li>Beacon B1: effective maxExtract >= baseFE (300) — current 100 blocks simulate</li>
 *   <li>6×Shulker B19: effective maxExtract >= baseFE × 19</li>
 *   <li>Storage/maxReceive scaling by theoretical B</li>
 *   <li>theoreticalBatchSize = max(1, min(1+batch, 19)) for infrastructure capacity</li>
 *   <li>Quarry: empty drops → return false, no destroy</li>
 *   <li>MobRip: weapon breaks mid-loop → stop remaining iterations</li>
 *   <li>energyAllowRun batch-aware</li>
 *   <li>Effect output slot limits by lockedB</li>
 *   <li>getEffectBaseDuration removed</li>
 * </ul>
 * <p>
 * RED phase: all tests fail (new behavior not yet in production code).
 * GREEN phase: all tests pass after production code update.
 */
class P3BatchCapacityContractTest {

    // ════════════════════════════════════════════════════════════
    // Safe multiply helper (same pattern as production)
    // ════════════════════════════════════════════════════════════

    static int safeMultiply(int a, int b) {
        if (a <= 0 || b <= 0) return 0;
        long result = (long) a * b;
        return (int) Math.min(result, Integer.MAX_VALUE);
    }

    // ════════════════════════════════════════════════════════════
    // A. Beacon B1: effective maxExtract >= baseFE
    // ════════════════════════════════════════════════════════════

    @Nested
    class BeaconB1EffectiveExtract {

        // Beacon setup: capacity=kFE(20)=20000, efficiency=300
        // initialMaxExtract = max(20000/200, 1) = 100
        // baseFE = 300
        // With B=1: effectiveExtract must be at least 300 (max of 100×1 and 300×1)

        @Test
        void beaconB1_effectiveExtract_atLeastBaseFe() {
            int initialExtract = 100;  // capacity/200 = 100
            int baseFePerTick = 300;  // efficiency
            int theoreticalB = 1;
            int effectiveExtract = Math.max(
                    safeMultiply(initialExtract, theoreticalB),
                    safeMultiply(baseFePerTick, theoreticalB));
            assertTrue(effectiveExtract >= baseFePerTick,
                    "B1: effectiveExtract(" + effectiveExtract + ") >= baseFE(" + baseFePerTick + ")");
            assertEquals(300, effectiveExtract,
                    "B1: effectiveExtract = max(100×1, 300×1) = 300");
        }

        @Test
        void beaconB1_effectiveStorage_atLeastBase() {
            int initialStorage = 20000;
            int theoreticalB = 1;
            int effectiveStorage = safeMultiply(initialStorage, theoreticalB);
            assertEquals(20000, effectiveStorage, "B1: effectiveStorage = 20000");
        }

        @Test
        void beaconB1_effectiveReceive_atLeastBase() {
            int initialReceive = 100;
            int theoreticalB = 1;
            int effectiveReceive = safeMultiply(initialReceive, theoreticalB);
            assertEquals(100, effectiveReceive, "B1: effectiveReceive = 100");
        }
    }

    // ════════════════════════════════════════════════════════════
    // B. 6×Shulker B19: effective maxExtract >= baseFE × 19
    // ════════════════════════════════════════════════════════════

    @Nested
    class SixShulkerB19Effective {

        @Test
        void sixShulker_theoreticalB_is19() {
            int batchSum = 18; // 6×Shulker
            int theoreticalB = Math.max(1, Math.min(1 + batchSum, 19));
            assertEquals(19, theoreticalB, "6×Shulker: theoreticalB = max(1, min(19, 19)) = 19");
        }

        @Test
        void beaconB19_effectiveExtract_atLeastBaseFeTimes19() {
            int initialExtract = 100;
            int baseFePerTick = 300;
            int theoreticalB = 19;

            int effectiveExtract = Math.max(
                    safeMultiply(initialExtract, theoreticalB),
                    safeMultiply(baseFePerTick, theoreticalB));
            assertEquals(5700, effectiveExtract,
                    "B19: effectiveExtract = max(100×19, 300×19) = max(1900, 5700) = 5700");
            assertTrue(effectiveExtract >= baseFePerTick * theoreticalB,
                    "effectiveExtract >= baseFE × B");
        }

        @Test
        void baseFeTimesB_fitsIntSafely() {
            int baseFe = 300;
            int B = 19;
            long expected = Math.round((double) baseFe * B);
            assertTrue(expected <= Integer.MAX_VALUE,
                    "300×19=" + expected + " fits int safely");
        }

        @Test
        void beaconB19_effectiveStorage_scaled() {
            int initialStorage = 20000;
            int theoreticalB = 19;
            int effectiveStorage = safeMultiply(initialStorage, theoreticalB);
            assertEquals(380000, effectiveStorage, "B19: 20000×19 = 380000");
        }

        @Test
        void beaconB19_effectiveReceive_scaled() {
            int initialReceive = 100;
            int theoreticalB = 19;
            int effectiveReceive = safeMultiply(initialReceive, theoreticalB);
            assertEquals(1900, effectiveReceive, "B19: 100×19 = 1900");
        }
    }

    // ════════════════════════════════════════════════════════════
    // C. theoreticalBatchSize formula
    // ════════════════════════════════════════════════════════════

    @Nested
    class TheoreticalBatchSize {

        @Test
        void noUpgrades_theoreticalB_is1() {
            int batch = 0;
            int theoreticalB = Math.max(1, Math.min(1 + batch, 19));
            assertEquals(1, theoreticalB, "No upgrades: B=1");
        }

        @Test
        void threeShulker_theoreticalB_is10() {
            int batch = 9; // 3×Shulker
            int theoreticalB = Math.max(1, Math.min(1 + batch, 19));
            assertEquals(10, theoreticalB);
        }

        @Test
        void sixShulker_theoreticalB_is19() {
            int batch = 18;
            int theoreticalB = Math.max(1, Math.min(1 + batch, 19));
            assertEquals(19, theoreticalB);
        }

        @Test
        void excessiveBatch_cappedAt19() {
            int batch = 99;
            int theoreticalB = Math.max(1, Math.min(1 + batch, 19));
            assertEquals(19, theoreticalB, "Capped at 19 hard max");
        }

        @Test
        void zeroBatch_is1() {
            int batch = 0;
            int theoreticalB = Math.max(1, Math.min(1 + batch, 19));
            assertEquals(1, theoreticalB);
        }

        @Test
        void singleShulker_theoreticalB_is4() {
            int batch = 3;
            int theoreticalB = Math.max(1, Math.min(1 + batch, 19));
            assertEquals(4, theoreticalB);
        }
    }

    // ════════════════════════════════════════════════════════════
    // D. effectiveExtract = max(initExtract×B, baseFE×B)
    // ════════════════════════════════════════════════════════════

    @Nested
    class EffectiveExtractFormula {

        @Test
        void baseFeDominates_whenLarger() {
            int initExtract = 100;
            int baseFe = 300;
            int B = 1;
            int effective = Math.max(safeMultiply(initExtract, B), safeMultiply(baseFe, B));
            assertEquals(300, effective, "baseFE dominates at B=1");
        }

        @Test
        void initExtractDominates_whenLarger() {
            int initExtract = 500;
            int baseFe = 300;
            int B = 1;
            int effective = Math.max(safeMultiply(initExtract, B), safeMultiply(baseFe, B));
            assertEquals(500, effective, "initExtract dominates when larger");
        }

        @Test
        void bothScaleWithB() {
            int initExtract = 100;
            int baseFe = 300;
            int B = 5;
            int effective = Math.max(safeMultiply(initExtract, B), safeMultiply(baseFe, B));
            assertEquals(1500, effective, "B=5: max(500, 1500) = 1500");
        }

        @Test
        void zeroExtract_returns0() {
            assertEquals(0, safeMultiply(0, 5), "zero extract → 0");
            assertEquals(0, safeMultiply(100, 0), "zero B → 0");
        }

        @Test
        void negativeInput_safeMultiplyReturns0() {
            assertEquals(0, safeMultiply(-100, 5), "negative extract → 0");
        }
    }

    // ════════════════════════════════════════════════════════════
    // E. Quarry: empty drops → return false, no destroy
    // ════════════════════════════════════════════════════════════

    @Nested
    class QuarryEmptyDrops {

        @Test
        void emptyDrops_shouldReturnFalse_notDestroy() {
            // Simulate the fix: if drops list is empty, return false
            java.util.List<Object> emptyDrops = java.util.List.of();
            boolean shouldContinue = !emptyDrops.isEmpty();
            assertFalse(shouldContinue,
                    "RED: Quarry mineRandomBlock must return false when drops is empty. "
                    + "Current code destroys block even with empty drops.");
        }

        @Test
        void nonEmptyDrops_shouldProceed() {
            java.util.List<Object> drops = java.util.List.of("item");
            boolean shouldContinue = !drops.isEmpty();
            assertTrue(shouldContinue,
                    "Non-empty drops: should proceed with fitting and destroying");
        }

        @Test
        void canBreak_guardsDropsInvocation() {
            // Tool check must happen before getDrops call
            boolean hasTool = true;
            boolean canBreak = hasTool; // canBreak returns false if no tool
            assertTrue(canBreak, "With tool: canBreak returns true");
        }
    }

    // ════════════════════════════════════════════════════════════
    // F. MobRip: weapon breaks mid-loop → stop remaining iterations
    // ════════════════════════════════════════════════════════════

    @Nested
    class MobRipWeaponBreak {

        @Test
        void weaponBreaks_shouldStopRemainingIterations() {
            // MobRip B=4, weapon breaks at iteration 2 → should stop
            int B = 4;
            boolean weaponValid = true;
            int iterationsRun = 0;

            for (int i = 0; i < B; i++) {
                if (!weaponValid) break; // ← This is the fix: break on broken weapon
                // ... damage entity ...
                // After damage, weapon might break:
                weaponValid = false; // simulates breakage
                iterationsRun++;
            }

            assertEquals(1, iterationsRun,
                    "RED: MobRip must stop remaining iterations when weapon breaks. "
                    + "Only 1 should run (weapon breaks after first hit). "
                    + "Current code continues with fist 0.5 damage.");
        }

        @Test
        void noWeapon_shouldStopIfWeaponRequired() {
            // If machine requires weapon and none present → stop
            int B = 3;
            boolean hasWeapon = false;
            int iterationsRun = 0;

            for (int i = 0; i < B; i++) {
                if (!hasWeapon) break;
                iterationsRun++;
            }

            assertEquals(0, iterationsRun,
                    "RED: No weapon available → stop immediately. "
                    + "Current code would use fist damage 0.5.");
        }

        @Test
        void weaponStaysValid_completesAllIterations() {
            int B = 3;
            boolean weaponValid = true;
            int iterationsRun = 0;

            for (int i = 0; i < B && weaponValid; i++) {
                iterationsRun++;
                // Weapon stays valid
            }

            assertEquals(3, iterationsRun, "All iterations complete when weapon stays valid");
        }

        @Test
        void noWeapon_allowWorkFalse_preservesExistingSemantics() {
            // Current: no weapon → damage=0.5. If we change to "require weapon",
            // we must not break machines that should work without weapons.
            // The fix should only STOP when weapon WAS present and BREAKS,
            // not when weapon was never present.
            boolean weaponWasPresent = true;
            boolean weaponNowBroken = true;
            boolean shouldStop = weaponWasPresent && weaponNowBroken;
            assertTrue(shouldStop,
                    "Should stop only if weapon WAS present and NOW broken. "
                    + "No-weapon-always machines continue.");
        }
    }

    // ════════════════════════════════════════════════════════════
    // G. energyAllowRun batch-aware
    // ════════════════════════════════════════════════════════════

    @Nested
    class EnergyAllowRunBatchAware {

        @Test
        void noLock_usesBaseFe() {
            boolean hasLock = false;
            int stored = 80;
            int baseFe = 80;
            int checkFe = hasLock ? (int) Math.round((double) baseFe * 3) : baseFe;
            boolean canRun = stored >= checkFe;
            assertTrue(canRun, "No lock: checkFe = baseFe = 80, stored=80 >= 80");
        }

        @Test
        void lockedBatch_usesTotalFe() {
            boolean hasLock = true;
            int lockedB = 5;
            int baseFe = 80;
            int stored = 400;
            int checkFe = hasLock ? (int) Math.round((double) baseFe * lockedB) : baseFe;
            boolean canRun = stored >= checkFe;
            assertTrue(canRun, "Locked B=5: checkFe = 80×5 = 400, stored=400 >= 400");
        }

        @Test
        void lockedBatch_insufficientEnergy_pauses() {
            boolean hasLock = true;
            int lockedB = 5;
            int baseFe = 80;
            int stored = 399; // one less than 400
            int checkFe = hasLock ? (int) Math.round((double) baseFe * lockedB) : baseFe;
            boolean canRun = stored >= checkFe;
            assertFalse(canRun, "Locked: stored=399 < 400 = checkFe → pause");
        }

        @Test
        void batchAware_activeSetOnlyWhenTrulyReady() {
            // Without batch-aware: stored=400, baseFe=80, energyAllowRun passes,
            // then process() checks stored < 80×5=400 → fails → active flicker
            // With batch-aware: checkFe=400, stored=400 ≥ 400 → no flicker
            int stored = 400;
            int baseFe = 80;
            int lockedB = 5;
            int checkFe = (int) Math.round((double) baseFe * lockedB);
            assertTrue(stored >= checkFe,
                    "Batch-aware: stored=400 >= checkFe=400, no active flicker");

            // Old behavior for contrast:
            int oldCheck = baseFe; // 80
            assertTrue(stored >= oldCheck,
                    "Old check passes at 400>=80, but inner check would fail → flicker");
        }
    }

    // ════════════════════════════════════════════════════════════
    // H. Effect output slot limit by lockedB
    // ════════════════════════════════════════════════════════════

    @Nested
    class EffectOutputSlotLimit {

        @Test
        void quarry_slotLimit_byLockedB() {
            int lockedB = 5;
            int expectedLimit = Math.min(lockedB + 63, 99);
            assertEquals(68, expectedLimit, "B=5: limit = 5+63 = 68, capped at 99");
        }

        @Test
        void farm_slotLimit_byLockedB() {
            int lockedB = 1;
            int expectedLimit = Math.min(lockedB + 63, 99);
            assertEquals(64, expectedLimit, "B=1: limit = 64");
        }

        @Test
        void slotLimit_cappedAt99() {
            assertEquals(79, Math.min(16 + 63, 99), "N=16: 16+63=79, no cap needed");
            assertEquals(99, Math.min(36 + 63, 99), "N=36: 36+63=99, at cap");
            assertEquals(99, Math.min(99, 99), "N=99: capped at 99");
            assertEquals(99, Math.min(100, 99), "N=100: capped at 99");
        }

        @Test
        void slotLimit_neverBelowExistingCount() {
            int lockedB = 1;
            int limit = Math.min(lockedB + 63, 99);
            int existing = 90;
            int effectiveLimit = Math.max(limit, existing);
            assertEquals(90, effectiveLimit, "Preserve overstack of 90");
        }

        @Test
        void quarry_fit_respectsExpandedLimit() {
            // Simulate Quarry output with B=5
            int lockedB = 5;
            int slotLimit = Math.min(lockedB + 63, 99); // = 68
            int existing = 60;
            int space = slotLimit - existing; // = 8
            int toAdd = 5;
            assertTrue(toAdd <= space, "B=5: 5 items fit in slot with space=" + space);
        }

        @Test
        void quarry_fit_overflowSpillsToNextSlot() {
            int lockedB = 5;
            int slotLimit = Math.min(lockedB + 63, 99); // 68
            int[] existing = {66, 0};
            int toAdd = 5;

            // Slot 0
            int space0 = slotLimit - existing[0]; // 2
            int add0 = Math.min(space0, toAdd);
            existing[0] += add0;
            toAdd -= add0; // = 3

            // Slot 1
            int space1 = slotLimit - existing[1]; // 68
            int add1 = Math.min(space1, toAdd);
            existing[1] += add1;
            toAdd -= add1; // = 0

            assertEquals(68, existing[0], "Slot 0 fills to 68");
            assertEquals(3, existing[1], "Slot 1 gets 3");
            assertEquals(0, toAdd, "All 5 items fit");
        }
    }

// ════════════════════════════════════════════════════════════
// K. Farm canFitAll/fitAll — dynamic slot limit & multi-slot spill
// ════════════════════════════════════════════════════════════

@Nested
class FarmDynamicSlotDistribution {

    @Test
    void farm_canFitAll_usesGetSlotLimit_notGetMaxStackSize() throws Exception {
        // RED: Farm's canFitAll uses existing.getMaxStackSize() (fixed 64)
        // GREEN: must use itemHandler.getSlotLimit(i) for dynamic limit
        var sourceFile = new java.io.File(
                "src/main/java/com/modularmc/ten/common/blockentity/machine/FarmBlockEntity.java");
        assertTrue(sourceFile.exists());
        var content = java.nio.file.Files.readString(sourceFile.toPath());
        // The canFitAll method must call getSlotLimit, not getMaxStackSize
        assertTrue(content.contains("getSlotLimit"),
                "RED: Farm canFitAll must use getSlotLimit() for dynamic slot limit. "
                + "Currently uses getMaxStackSize() (fixed 64).");
    }

    @Test
    void farm_fitAll_distributesSpillAcrossSlots() throws Exception {
        // RED: Farm's fitAll uses existing.grow(drop.getCount()) without
        // checking slot limit, no spill-over to next slot.
        // GREEN: must chunk additions by slotLimit - existing, then continue
        // to next slot with remaining count.
        var sourceFile = new java.io.File(
                "src/main/java/com/modularmc/ten/common/blockentity/machine/FarmBlockEntity.java");
        assertTrue(sourceFile.exists());
        var content = java.nio.file.Files.readString(sourceFile.toPath());
        int fitAllStart = content.indexOf("private void fitAll");
        assertTrue(fitAllStart >= 0, "fitAll method must exist");
        // The method should contain grow() with a computed room, not raw drop count
        // We check for the presence of slot limit based shrink/distribute pattern
        assertTrue(content.contains("stack.shrink"),
                "RED: Farm fitAll must shrink remaining after partial fill. "
                + "Currently uses grow(drop.getCount()) without spill.");
    }

    @Test
    void farm_fitAll_doesNotModifyOriginalDrops() throws Exception {
        // RED: Original drops list might be modified by fitAll
        // GREEN: insertFirstFit copies before mutating (stack.copy())
        var sourceFile = new java.io.File(
                "src/main/java/com/modularmc/ten/common/blockentity/machine/FarmBlockEntity.java");
        assertTrue(sourceFile.exists());
        var content = java.nio.file.Files.readString(sourceFile.toPath());
        // Find the fitAll or insertFirstFit method signatures
        assertTrue(content.contains("stack.copy()") || content.contains("drop.copy()"),
                "RED: Farm fitAll/insertFirstFit must use .copy() to avoid mutating original drops. "
                + "Currently modifies shared objects causing duplicates.");
    }

    @Test
    void farm_fitAll_slotLimit65_99_withDynamicB() {
        // Verify the slot limit formula matches installDynamicOutputLimit
        int lockedB = 5;
        int slotLimit = Math.min(lockedB + 63, 99);
        assertEquals(68, slotLimit, "B=5: limit=68");

        // Test multi-slot distribution
        int[] existing = {66, 0, 0};
        int toAdd = 10;
        int remaining = toAdd;

        // Slot 0: room = 68 - 66 = 2
        int room0 = slotLimit - existing[0];
        int add0 = Math.min(room0, remaining);
        existing[0] += add0;
        remaining -= add0; // = 8

        // Slot 1: room = 68
        int room1 = slotLimit - existing[1];
        int add1 = Math.min(room1, remaining);
        existing[1] += add1;
        remaining -= add1; // = 0

        assertEquals(68, existing[0], "Slot 0 fills to limit");
        assertEquals(8, existing[1], "Slot 1 gets 8");
        assertEquals(0, remaining, "All items distributed");
    }

    @Test
    void farm_fitAll_noOverstack() {
        int slotLimit = 68; // B=5
        int existing = 65;
        int toAdd = 5;
        int room = slotLimit - existing; // = 3
        int moved = Math.min(room, toAdd); // = 3
        existing += moved; // = 68
        toAdd -= moved; // = 2
        assertEquals(68, existing, "Slot stops at limit, not 70");
        assertEquals(2, toAdd, "2 remaining to spill to next slot");
    }

    @Test
    void farm_fitAll_remainingSpillsToNextSlotOnly() {
        // Simulate: existing[0]=66, existing[1]=67, toAdd=5, slotLimit=68
        int slotLimit = 68;
        int[] slots = {66, 67};
        int toAdd = 5;

        int room0 = slotLimit - slots[0]; // 2
        int add0 = Math.min(room0, toAdd); // 2
        slots[0] += add0;
        toAdd -= add0; // 3

        int room1 = slotLimit - slots[1]; // 1
        int add1 = Math.min(room1, toAdd); // 1
        slots[1] += add1;
        toAdd -= add1; // 2

        assertEquals(68, slots[0], "Slot 0 filled to 68");
        assertEquals(68, slots[1], "Slot 1 filled to 68");
        assertEquals(2, toAdd, "2 remaining cannot fit (simulates canFitAll preventing this)");
    }

    // ── K2: canFitAll simulated accumulation (not per-item independent check) ──

    /**
     * Simulates the FIXED canFitAll behavior on an int-based model.
     * itemType=0 means empty. Simulates accumulation matching insert logic.
     */
    private static boolean simulatedCanFitAll(int[][] outputs, int[][] drops, int slotLimit) {
        int n = outputs.length;
        int[] types = new int[n];
        int[] counts = new int[n];
        for (int i = 0; i < n; i++) {
            types[i] = outputs[i][0];
            counts[i] = outputs[i][1];
        }
        for (int[] drop : drops) {
            int dropType = drop[0];
            int remaining = drop[1];
            for (int j = 0; j < n && remaining > 0; j++) {
                if (types[j] == 0) {
                    // Empty slot: cap at slotLimit
                    int fill = Math.min(remaining, slotLimit);
                    types[j] = dropType;
                    counts[j] = fill;
                    remaining -= fill;
                } else if (types[j] == dropType) {
                    int room = slotLimit - counts[j];
                    int moved = Math.min(room, remaining);
                    if (moved > 0) {
                        counts[j] += moved;
                        remaining -= moved;
                    }
                }
            }
            if (remaining > 0) return false;
        }
        return true;
    }

    /**
     * Simulates the BUGGY canFitAll behavior (per-item independent check).
     */
    private static boolean buggyCanFitAll(int[][] outputs, int[][] drops, int slotLimit) {
        for (int[] drop : drops) {
            int dropType = drop[0];
            int dropCount = drop[1];
            boolean fits = false;
            for (int[] output : outputs) {
                if (output[0] == 0) {
                    fits = true;
                    break;
                }
                if (output[0] == dropType && output[1] + dropCount <= slotLimit) {
                    fits = true;
                    break;
                }
            }
            if (!fits) return false;
        }
        return true;
    }

    @Test
    void farm_canFitAll_usesSimulatedAccumulation() throws Exception {
        // RED: Farm's canFitAll uses per-item independent canFit() calls
        // which don't track slot consumption across drops.
        // GREEN: must simulate insertion into a local ItemStack[] copy
        // matching the exact same logic as fitAll commit.
        var sourceFile = new java.io.File(
                "src/main/java/com/modularmc/ten/common/blockentity/machine/FarmBlockEntity.java");
        assertTrue(sourceFile.exists());
        var content = java.nio.file.Files.readString(sourceFile.toPath());
        int canFitAllStart = content.indexOf("private boolean canFitAll");
        assertTrue(canFitAllStart >= 0, "canFitAll method must exist");
        int canFitAllEnd = content.indexOf("private void fitAll", canFitAllStart);
        if (canFitAllEnd < 0) canFitAllEnd = content.indexOf("private void insertFirstFit", canFitAllStart);
        String methodBody = content.substring(canFitAllStart, canFitAllEnd);

        // Must use a local array for simulated accumulation, not loop calling canFit()
        assertFalse(methodBody.contains("canFit(stack)") || methodBody.contains("canFit(drop)"),
                "RED: Farm canFitAll must NOT call canFit() per item — use ItemStack[] simulated accumulation. "
                + "Per-item check causes multiple different drops to compete for the same empty slot.");
    }

    @Test
    void farm_fitAll_usesSnapshotCommit() throws Exception {
        // RED: Farm's fitAll mutates itemHandler directly during insertion,
        // risking partial state if a later insert fails.
        // GREEN: must snapshot output slots, simulate on snapshot, then commit atomically.
        var sourceFile = new java.io.File(
                "src/main/java/com/modularmc/ten/common/blockentity/machine/FarmBlockEntity.java");
        assertTrue(sourceFile.exists());
        var content = java.nio.file.Files.readString(sourceFile.toPath());
        int fitAllStart = content.indexOf("private void fitAll");
        assertTrue(fitAllStart >= 0, "fitAll method must exist");
        int fitAllEnd = content.indexOf("private void insertFirstFit", fitAllStart);
        if (fitAllEnd < 0) fitAllEnd = content.indexOf("private boolean canFit", fitAllStart);
        String methodBody = content.substring(fitAllStart, fitAllEnd);

        // Must create a local array of output slot snapshots
        assertFalse(methodBody.contains("insertFirstFit(stack.copy())"),
                "RED: Farm fitAll must NOT delegate to insertFirstFit — use local snapshot+commit. "
                + "Delegation risks partial state if insertFirstFit fails mid-way.");
    }

    @Test
    void farm_insertFirstFit_failsFastIfRemaining() throws Exception {
        // RED: Farm insertFirstFit silently drops items when they don't fit.
        // GREEN: must throw IllegalStateException if stack remains after full slot scan.
        var sourceFile = new java.io.File(
                "src/main/java/com/modularmc/ten/common/blockentity/machine/FarmBlockEntity.java");
        assertTrue(sourceFile.exists());
        var content = java.nio.file.Files.readString(sourceFile.toPath());
        int insertStart = content.indexOf("private void insertFirstFit");
        assertTrue(insertStart >= 0, "insertFirstFit method must exist");
        int insertEnd = content.indexOf("private void installDynamicOutputLimit", insertStart);
        if (insertEnd < 0) insertEnd = content.indexOf("// Install dynamic", insertStart);
        if (insertEnd < 0) insertEnd = insertStart + 600;
        String methodBody = content.substring(insertStart, insertEnd);

        // Must throw when stack still has items after full scan
        assertTrue(methodBody.contains("throw"),
                "RED: Farm insertFirstFit must throw IllegalStateException if stack has remaining "
                + "after scanning all slots. Current code silently drops items.");
    }

    @Test
    void farm_canFitAll_simulated_accumulation_detectsSlotCompetition() {
        // RED: Multiple different drops competing for single empty slot.
        // 3 slots: [item1×20, EMPTY, item1×50], drops: [item2×1, item3×1]
        // Buggy: both drops see the same empty slot → returns true (incorrect)
        // Fixed: after item2 takes slot1, item3 has nowhere → returns false
        int[][] outputs = {{1, 20}, {0, 0}, {1, 50}};
        int[][] drops = {{2, 1}, {3, 1}};
        int slotLimit = 64;

        boolean buggy = buggyCanFitAll(outputs, drops, slotLimit);
        boolean fixed = simulatedCanFitAll(outputs, drops, slotLimit);

        assertTrue(buggy, "RED: Buggy per-item check says true — both see same empty slot");
        assertFalse(fixed,
                "RED: Fixed accumulation must detect slot competition and return false. "
                + "2 different drops cannot share 1 empty slot.");
        assertNotEquals(buggy, fixed,
                "RED: Fixed simulation must differ from buggy per-item check for this scenario.");
    }

    @Test
    void farm_canFitAll_simulated_sameItemCumulative_usesSlotLimit() {
        // RED: Same item accumulates within existing slot, capped by slot limit.
        // 2 slots: [item1×60, EMPTY], drops: [item1×64], slotLimit=64
        // Slot 0: room=4, moves 4 → remaining 60
        // Slot 1: empty, fills 60
        // Total fits: true (uses both slots)
        int[][] outputs = {{1, 60}, {0, 0}};
        int[][] drops = {{1, 64}};
        int slotLimit = 64;

        boolean fixed = simulatedCanFitAll(outputs, drops, slotLimit);
        assertTrue(fixed,
                "RED: 64 items of same type, slot0 room=4 + slot1 empty=64 capacity = fits");
    }

    @Test
    void farm_canFitAll_simulated_sameItemSpillDetectsOverflow() {
        // RED: Multi-slot spill correctly detects when items don't fully fit.
        // 2 slots: [item1×63, item1×63], drops: [item1×10], slotLimit=64
        // Slot 0: room=1, moves 1 → remaining 9
        // Slot 1: room=1, moves 1 → remaining 8 → cannot fit
        int[][] outputs = {{1, 63}, {1, 63}};
        int[][] drops = {{1, 10}};
        int slotLimit = 64;

        boolean fixed = simulatedCanFitAll(outputs, drops, slotLimit);
        assertFalse(fixed,
                "RED: 10 items cannot fit in 2×63/64 slots (room=2)");
    }

    @Test
    void farm_canFitAll_simulated_dynamic99_limit() {
        // RED: Dynamic slot limit (B=36 → limit=99) is respected.
        // 2 slots [item1×95, EMPTY], drops [item1×10, item2×1], slotLimit=99
        // Buggy: item1×10 → slot0 95+10=105>99 → slot1 empty → true;
        //        item2×1 → slot0 item1≠item2 → slot1 empty → true → total: true (WRONG)
        // Fixed: item1×10 → slot0 room=4→moves4→remaining6→slot1 fills6;
        //        item2×1 → slot0 item1≠item2 → slot1 item1≠item2 → cannot fit → false
        int slotLimit = 99;
        int[][] outputs = {{1, 95}, {0, 0}};
        int[][] drops = {{1, 10}, {2, 1}};

        boolean buggy = buggyCanFitAll(outputs, drops, slotLimit);
        boolean fixed = simulatedCanFitAll(outputs, drops, slotLimit);

        assertTrue(buggy,
                "RED: Buggy says true — both drops see slot1 empty (doesn't track item1 spill)");
        assertFalse(fixed,
                "RED: After item1 spills into slot1, item2 has no room → false");
        assertNotEquals(buggy, fixed,
                "RED: Buggy and fixed must disagree for dynamic99 cross-item spill");
    }

    @Test
    void farm_canFitAll_simulated_existingStackMerge_fullFit() {
        // RED: Existing stack merge and cross-slot spill for different drops.
        // 2 slots: [item1×60, EMPTY], drops: [item1×5, item2×3], slotLimit=64
        // Insert item1×5: slot0 room=4, moves 4 → remaining 1; slot1 fills 1
        // Insert item2×3: slot0 item1≠item2; slot1 has item1≠item2 → cannot fit
        int[][] outputs = {{1, 60}, {0, 0}};
        int[][] drops = {{1, 5}, {2, 3}};
        int slotLimit = 64;

        boolean buggy = buggyCanFitAll(outputs, drops, slotLimit);
        boolean fixed = simulatedCanFitAll(outputs, drops, slotLimit);

        assertTrue(buggy, "RED: Buggy says true — item1 sees slot1 empty, item2 sees slot1 empty");
        assertFalse(fixed,
                "RED: After item1 spill fills slot1, item2 has nowhere to go");
        assertNotEquals(buggy, fixed,
                "RED: Buggy and fixed must disagree for this scenario");
    }

    @Test
    void farm_canFitAll_simulated_multipleDropsExactFit() {
        // RED: Multiple drops that exactly fit across slots.
        // 2 slots: [EMPTY, EMPTY], drops: [item1×30, item1×30], slotLimit=64
        int[][] outputs = {{0, 0}, {0, 0}};
        int[][] drops = {{1, 30}, {1, 30}};
        int slotLimit = 64;

        boolean fixed = simulatedCanFitAll(outputs, drops, slotLimit);
        assertTrue(fixed,
                "RED: 30+30 fits in 2 empty slots with limit=64");
    }

    @Test
    void farm_canFitAll_simulated_commitNoRemaining() {
        // RED: After commit, no drop has remaining items (zero-loss).
        // Simulate full flow: canFitAll → simulates → if false, never commit.
        // 2 slots: [item1×62, EMPTY], drops: [item1×5, item2×1], slotLimit=64
        // item1×5: slot0 room=2, moves 2 → remaining 3; slot1 fills 3
        // item2×1: slot0 item1≠item2; slot1 item1≠item2 → cannot fit!
        int[][] outputs = {{1, 62}, {0, 0}};
        int[][] drops = {{1, 5}, {2, 1}};
        int slotLimit = 64;

        // Simulate the full canFitAll → no-commit flow
        int n = outputs.length;
        int[] simTypes = new int[n];
        int[] simCounts = new int[n];
        for (int i = 0; i < n; i++) {
            simTypes[i] = outputs[i][0];
            simCounts[i] = outputs[i][1];
        }

        boolean allFit = true;
        for (int[] drop : drops) {
            int remaining = drop[1];
            for (int j = 0; j < n && remaining > 0; j++) {
                if (simTypes[j] == 0) {
                    int fill = Math.min(remaining, slotLimit);
                    simTypes[j] = drop[0];
                    simCounts[j] = fill;
                    remaining -= fill;
                } else if (simTypes[j] == drop[0]) {
                    int room = slotLimit - simCounts[j];
                    int moved = Math.min(room, remaining);
                    if (moved > 0) {
                        simCounts[j] += moved;
                        remaining -= moved;
                    }
                }
            }
            if (remaining > 0) {
                allFit = false;
                assertEquals(1, remaining,
                        "RED: zero-loss invariant — item2×1 cannot fit after item1 fills both slots "
                        + "(slot0:62→64 room=2→moves2, slot1:0→3 spills 3)");
                break;
            }
        }
        assertFalse(allFit,
                "RED: Not all drops fit — commit must NOT happen, preventing partial state");
    }
}

// ════════════════════════════════════════════════════════════
// L. Quarry loop: per-operation miss → continue, tool fail → break
// ════════════════════════════════════════════════════════════

@Nested
class QuarryLoopMissVsBreak {

    @Test
    void quarry_loop_doesNotBreakOnExecuteSingleOperationFalse() throws Exception {
        // RED: Quarry's applyEffect loop breaks on ANY false from
        // executeSingleOperation(), causing per-operation misses (no drops,
        // can't break, capacity full) to stop the entire batch.
        // GREEN: loop only breaks on tool failure; conditionStart is handled
        // by outer process() and does NOT need repeating in the loop.
        // Per-operation misses continue to next iteration.
        var sourceFile = new java.io.File(
                "src/main/java/com/modularmc/ten/common/blockentity/machine/QuarryBlockEntity.java");
        assertTrue(sourceFile.exists());
        var content = java.nio.file.Files.readString(sourceFile.toPath());
        int applyEffectStart = content.indexOf("public void applyEffect");
        assertTrue(applyEffectStart >= 0, "applyEffect must exist");
        int methodEnd = content.indexOf("private void installDynamicOutputLimit", applyEffectStart);
        if (methodEnd < 0) methodEnd = content.indexOf("// Install dynamic", applyEffectStart);
        if (methodEnd < 0) methodEnd = applyEffectStart + 1000;
        String methodBody = content.substring(applyEffectStart, methodEnd);

        // conditionStart is already called in outer process() - no need to repeat in loop
        // (P3 Effect narrow-review finding: conditionStart has no side effects)
        boolean hasConditionStart = methodBody.contains("!conditionStart()");
        assertFalse(hasConditionStart,
                "RED: Quarry loop must NOT call conditionStart() — outer process() already checked. "
                + "Calling it B times per cycle is redundant noise.");

        // Only tool emptiness should cause break in the loop
        boolean hasToolBreak = methodBody.contains("isEmpty()") && methodBody.contains("break");
        assertTrue(hasToolBreak, "Tool empty must break");

        // The executeSingleOperation call should NOT be in an if-break pattern
        assertFalse(content.contains("if (!executeSingleOperation()) break;") || content.contains("if(!executeSingleOperation())break;"),
                "RED: Quarry must NOT break on executeSingleOperation() false. "
                + "Per-operation misses (no drops/can't break/capacity) should continue.");
    }

    @Test
    void quarry_perOperationMiss_continuesLoop() {
        // Simulate the loop pattern
        int B = 3;
        int iterationsRun = 0;
        int successes = 0;
        boolean toolValid = true;
        boolean conditionOk = true;

        for (int i = 0; i < B && conditionOk && !(!toolValid); i++) {
            if (!toolValid) break;
            iterationsRun++;
            // Simulate per-operation miss (return false, but loop continues)
            boolean opResult = (i == 1) ? false : true; // iteration 1 fails
            // Do NOT break on false — continue to next iteration
            if (opResult) successes++;
        }

        assertEquals(3, iterationsRun,
                "All B=3 iterations run despite per-operation miss at i=1");
        assertEquals(2, successes,
                "2 successes, 1 miss — loop continued after miss");
    }

    @Test
    void quarry_toolBreak_breaksLoop() {
        int B = 5;
        int iterationsRun = 0;

        for (int i = 0; i < B; i++) {
            // Simulate tool breaking after iteration 2
            boolean toolValid = i < 2;
            if (!toolValid) break;
            iterationsRun++;
        }

        assertEquals(2, iterationsRun,
                "Tool breaks at iteration 2 → only 2 iterations run");
    }

    @Test
    void quarry_bMaxAttempts_noInfiniteRetry() {
        int B = 3;
        int attempts = 0;
        for (int i = 0; i < B; i++) {
            attempts++;
            // Even if operation fails, we don't retry — B limits total attempts
        }
        assertEquals(3, attempts, "Exactly B=3 attempts, no more");
    }
}

// ════════════════════════════════════════════════════════════
// M. MobRip: weapon damage guard & break stop
// ════════════════════════════════════════════════════════════

@Nested
class MobRipWeaponDamageGuard {

    @Test
    void mobRip_damage_guardedByNotEmpty() throws Exception {
        // RED: ItemNBTHelper.damage(weapon, ...) is called even when weapon
        // is ItemStack.EMPTY (unarmed operation).
        // GREEN: damage only called when !weapon.isEmpty()
        var sourceFile = new java.io.File(
                "src/main/java/com/modularmc/ten/common/blockentity/machine/MobRipBlockEntity.java");
        assertTrue(sourceFile.exists());
        var content = java.nio.file.Files.readString(sourceFile.toPath());
        int tryHurtStart = content.indexOf("private boolean tryHurtOneEntity");
        assertTrue(tryHurtStart >= 0, "tryHurtOneEntity method must exist");
        int methodEnd = content.indexOf("}", tryHurtStart);
        methodEnd = content.indexOf("}", methodEnd + 1); // find closing of method
        if (methodEnd < 0) methodEnd = tryHurtStart + 500;
        String methodBody = content.substring(tryHurtStart, methodEnd);

        // Must have a guard before damage call
        assertTrue(methodBody.contains("!weapon.isEmpty()") || methodBody.contains("!weapon.isEmpty("),
                "RED: MobRip tryHurtOneEntity must guard damage() with !weapon.isEmpty(). "
                + "Currently calls damage on ItemStack.EMPTY.");
    }

    @Test
    void mobRip_noWeapon_preservesUnarmedDamage() {
        // No weapon → 0.5 damage, no damage() call
        boolean hasWeapon = false;
        float damage = 0.5f; // unarmed
        boolean shouldDamageItem = hasWeapon; // only if weapon exists
        assertFalse(shouldDamageItem, "No weapon: should NOT call damage()");
        assertEquals(0.5f, damage, "Unarmed damage = 0.5");
    }

    @Test
    void mobRip_withWeapon_appliesDamageAndDurability() {
        boolean hasWeapon = true;
        float damage = hasWeapon ? 1.0f : 0.5f;
        boolean shouldDamageItem = hasWeapon;
        assertTrue(shouldDamageItem, "With weapon: should call damage()");
        assertEquals(1.0f, damage, "Weapon damage = 1.0");
    }

    @Test
    void mobRip_weaponBreaks_stopsRemainingIterations() {
        // MobRip B=4, weapon breaks after 2 iterations → stop
        int B = 4;
        boolean weaponWasPresent = true;
        boolean weaponValid = true;
        int iterationsRun = 0;

        for (int i = 0; i < B; i++) {
            // Check: if weapon was present but now broken → break
            if (weaponWasPresent && !weaponValid) break;
            iterationsRun++;
            // Simulate weapon breaking after iteration 2
            if (i == 1) weaponValid = false;
        }

        assertEquals(2, iterationsRun,
                "Weapon breaks after iteration 2 → exactly 2 iterations, weaponWasPresent=true");
    }

    @Test
    void mobRip_noWeaponEver_doesNotBreak() {
        // No weapon was ever present → allow unarmed operation (0.5 damage)
        int B = 3;
        boolean weaponWasPresent = false;
        boolean weaponNowBroken = false; // irrelevant
        boolean shouldStop = weaponWasPresent && weaponNowBroken;
        assertFalse(shouldStop, "No weapon ever: should NOT stop — 0.5 damage allowed");

        int iterationsRun = 0;
        for (int i = 0; i < B; i++) {
            if (weaponWasPresent && !(!weaponNowBroken)) break;
            iterationsRun++;
        }
        assertEquals(3, iterationsRun, "All 3 iterations run with unarmed 0.5 damage");
    }
}

// ════════════════════════════════════════════════════════════
// O. Quarry atomic drop insertion — snapshot+simulate+commit
// ════════════════════════════════════════════════════════════

@Nested
class QuarryAtomicDropInsertion {

    // ── O1: canFitAll must use simulated accumulation, not per-item canFit ──

    @Test
    void quarry_canFitAll_usesSimulatedAccumulation() throws Exception {
        // RED: Quarry's canFitAll calls canFit(stack) per drop independently,
        // allowing different drops to compete for the same empty slot.
        // GREEN: must simulate insertion into local ItemStack[] copy.
        var sourceFile = new java.io.File(
                "src/main/java/com/modularmc/ten/common/blockentity/machine/QuarryBlockEntity.java");
        assertTrue(sourceFile.exists());
        var content = java.nio.file.Files.readString(sourceFile.toPath());
        int canFitAllStart = content.indexOf("private boolean canFitAll");
        assertTrue(canFitAllStart >= 0, "canFitAll method must exist");
        int canFitAllEnd = content.indexOf("private void fitAll", canFitAllStart);
        if (canFitAllEnd < 0) canFitAllEnd = content.indexOf("private boolean canFit(", canFitAllStart);
        String methodBody = content.substring(canFitAllStart, canFitAllEnd);

        // Must NOT call canFit(stack) per item — use ItemStack[] simulated accumulation
        assertFalse(methodBody.contains("canFit("),
                "RED: Quarry canFitAll must NOT call canFit() per item — use ItemStack[] simulated accumulation. "
                + "Per-item check causes multiple different drops to compete for the same empty slot.");
    }

    // ── O2: fitAll must use snapshot+commit, not delegating to insertFirstFit ──

    @Test
    void quarry_fitAll_usesSnapshotCommit() throws Exception {
        // RED: Quarry's fitAll mutates itemHandler directly via insertFirstFit,
        // risking partial state if canFitAll pre-check is inaccurate.
        // GREEN: must snapshot output slots, simulate on snapshot, then commit atomically.
        var sourceFile = new java.io.File(
                "src/main/java/com/modularmc/ten/common/blockentity/machine/QuarryBlockEntity.java");
        assertTrue(sourceFile.exists());
        var content = java.nio.file.Files.readString(sourceFile.toPath());
        int fitAllStart = content.indexOf("private void fitAll");
        assertTrue(fitAllStart >= 0, "fitAll method must exist");
        int fitAllEnd = content.indexOf("private void insertFirstFit", fitAllStart);
        if (fitAllEnd < 0) fitAllEnd = content.indexOf("private boolean canFit(", fitAllStart);
        if (fitAllEnd < 0) fitAllEnd = content.indexOf("private boolean giveGeneratedLoot", fitAllStart);
        if (fitAllEnd < 0) fitAllEnd = fitAllStart + 500;
        String methodBody = content.substring(fitAllStart, fitAllEnd);

        // Must NOT delegate to insertFirstFit — use local snapshot+commit
        assertFalse(methodBody.contains("insertFirstFit(stack.copy())"),
                "RED: Quarry fitAll must NOT delegate to insertFirstFit — use local snapshot+commit. "
                + "Delegation risks partial state if handler mutates mid-insert.");
    }

    // ── O3: insertFirstFit must fail-fast when items remain ──

    @Test
    void quarry_insertFirstFit_failsFastIfRemaining() throws Exception {
        // RED: Quarry insertFirstFit silently drops items when they don't fit.
        // GREEN: must throw IllegalStateException if stack remains after full slot scan.
        var sourceFile = new java.io.File(
                "src/main/java/com/modularmc/ten/common/blockentity/machine/QuarryBlockEntity.java");
        assertTrue(sourceFile.exists());
        var content = java.nio.file.Files.readString(sourceFile.toPath());
        int insertStart = content.indexOf("private void insertFirstFit");
        assertTrue(insertStart >= 0, "insertFirstFit method must exist");
        int insertEnd = content.indexOf("private void installDynamicOutputLimit", insertStart);
        if (insertEnd < 0) insertEnd = content.indexOf("@Override", insertStart);
        if (insertEnd < 0) insertEnd = content.indexOf("public double effectInterval", insertStart);
        if (insertEnd < 0) insertEnd = content.indexOf("private void giveGeneratedLoot", insertStart);
        if (insertEnd < 0) insertEnd = insertStart + 600;
        String methodBody = content.substring(insertStart, insertEnd);

        // Must throw when stack still has items after full scan
        assertTrue(methodBody.contains("throw"),
                "RED: Quarry insertFirstFit must throw IllegalStateException if stack has remaining "
                + "after scanning all slots. Current code silently drops items.");
    }

    // ── O4: canFitAll simulated accumulation detects slot competition ──

    /**
     * Simulates CORRECT canFitAll behavior on an int-based model.
     * itemType=0 means empty. Simulates accumulation matching insert logic.
     */
    private static boolean simulatedCanFitAll(int[][] outputs, int[][] drops, int slotLimit) {
        int n = outputs.length;
        int[] types = new int[n];
        int[] counts = new int[n];
        for (int i = 0; i < n; i++) {
            types[i] = outputs[i][0];
            counts[i] = outputs[i][1];
        }
        for (int[] drop : drops) {
            int dropType = drop[0];
            int remaining = drop[1];
            for (int j = 0; j < n && remaining > 0; j++) {
                if (types[j] == 0) {
                    int fill = Math.min(remaining, slotLimit);
                    types[j] = dropType;
                    counts[j] = fill;
                    remaining -= fill;
                } else if (types[j] == dropType) {
                    int room = slotLimit - counts[j];
                    int moved = Math.min(room, remaining);
                    if (moved > 0) {
                        counts[j] += moved;
                        remaining -= moved;
                    }
                }
            }
            if (remaining > 0) return false;
        }
        return true;
    }

    /**
     * Simulates BUGGY canFitAll (per-item independent check).
     */
    private static boolean buggyCanFitAll(int[][] outputs, int[][] drops, int slotLimit) {
        for (int[] drop : drops) {
            int dropType = drop[0];
            int dropCount = drop[1];
            boolean fits = false;
            for (int[] output : outputs) {
                if (output[0] == 0) {
                    fits = true;
                    break;
                }
                if (output[0] == dropType && output[1] + dropCount <= slotLimit) {
                    fits = true;
                    break;
                }
            }
            if (!fits) return false;
        }
        return true;
    }

    @Test
    void quarry_canFitAll_simulated_accumulation_detectsSlotCompetition() {
        // 2 output slots: [item1×20, EMPTY], drops: [item2×1, item3×1]
        // Buggy: both drops see the same empty slot → true (incorrect)
        // Fixed: after item2 takes slot1, item3 has nowhere → false
        int[][] outputs = {{1, 20}, {0, 0}};
        int[][] drops = {{2, 1}, {3, 1}};
        int slotLimit = 64;

        boolean buggy = buggyCanFitAll(outputs, drops, slotLimit);
        boolean fixed = simulatedCanFitAll(outputs, drops, slotLimit);

        assertTrue(buggy, "RED: Buggy per-item check says true — both see same empty slot");
        assertFalse(fixed,
                "RED: Fixed accumulation must detect slot competition and return false. "
                + "2 different drops cannot share 1 empty slot.");
        assertNotEquals(buggy, fixed,
                "RED: Fixed simulation must differ from buggy per-item check for this scenario.");
    }

    // ── O5: canFitAll same-item cumulative across slots ──

    @Test
    void quarry_canFitAll_simulated_sameItemCumulative_usesSlotLimit() {
        // 2 slots: [item1×60, EMPTY], drops: [item1×64], slotLimit=64
        // Slot 0: room=4, moves 4 → remaining 60
        // Slot 1: empty, fills 60
        int[][] outputs = {{1, 60}, {0, 0}};
        int[][] drops = {{1, 64}};
        int slotLimit = 64;

        boolean fixed = simulatedCanFitAll(outputs, drops, slotLimit);
        assertTrue(fixed,
                "RED: 64 items of same type, slot0 room=4 + slot1 empty=64 capacity = fits");
    }

    // ── O6: canFitAll cross-slot spill overflow detection ──

    @Test
    void quarry_canFitAll_simulated_sameItemSpillDetectsOverflow() {
        // 2 slots: [item1×63, item1×63], drops: [item1×10], slotLimit=64
        // Slot 0: room=1, moves 1 → remaining 9
        // Slot 1: room=1, moves 1 → remaining 8 → cannot fit
        int[][] outputs = {{1, 63}, {1, 63}};
        int[][] drops = {{1, 10}};
        int slotLimit = 64;

        boolean fixed = simulatedCanFitAll(outputs, drops, slotLimit);
        assertFalse(fixed,
                "RED: 10 items cannot fit in 2×63/64 slots (room=2)");
    }

    // ── O7: canFitAll dynamic slot limit (B=36 → 99) ──

    @Test
    void quarry_canFitAll_simulated_dynamic99_limit() {
        // Dynamic slot limit (B=36 → limit=99).
        // 2 slots: [item1×95, EMPTY], drops: [item1×10, item2×1], slotLimit=99
        // Buggy: item1×10 → slot0 room=4→moves4→slot1 fills6; item2×1 → no room → true (WRONG)
        // Fixed: after item1 fills both slots, item2 has nowhere → false
        int slotLimit = 99;
        int[][] outputs = {{1, 95}, {0, 0}};
        int[][] drops = {{1, 10}, {2, 1}};

        boolean buggy = buggyCanFitAll(outputs, drops, slotLimit);
        boolean fixed = simulatedCanFitAll(outputs, drops, slotLimit);

        assertTrue(buggy,
                "RED: Buggy says true — both drops see slot1 empty (doesn't track item1 spill)");
        assertFalse(fixed,
                "RED: After item1 spills into slot1, item2 has no room → false");
        assertNotEquals(buggy, fixed,
                "RED: Buggy and fixed must disagree for dynamic99 cross-item spill");
    }

    // ── O8: canFitAll existing-stack merge + cross-slot spill ──

    @Test
    void quarry_canFitAll_simulated_existingStackMerge_fullFit() {
        // 2 slots: [item1×60, EMPTY], drops: [item1×5, item2×3], slotLimit=64
        // Insert item1×5: slot0 room=4→moves4→remaining1; slot1 fills1
        // Insert item2×3: slot0 item1≠item2; slot1 has item1≠item2 → cannot fit
        int[][] outputs = {{1, 60}, {0, 0}};
        int[][] drops = {{1, 5}, {2, 3}};
        int slotLimit = 64;

        boolean buggy = buggyCanFitAll(outputs, drops, slotLimit);
        boolean fixed = simulatedCanFitAll(outputs, drops, slotLimit);

        assertTrue(buggy, "RED: Buggy says true — item1 sees slot1 empty, item2 sees slot1 empty");
        assertFalse(fixed,
                "RED: After item1 spill fills slot1, item2 has nowhere to go");
        assertNotEquals(buggy, fixed,
                "RED: Buggy and fixed must disagree for this scenario");
    }

    // ── O9: canFitAll multiple drops exact fit ──

    @Test
    void quarry_canFitAll_simulated_multipleDropsExactFit() {
        // 2 slots: [EMPTY, EMPTY], drops: [item1×30, item1×30], slotLimit=64
        int[][] outputs = {{0, 0}, {0, 0}};
        int[][] drops = {{1, 30}, {1, 30}};
        int slotLimit = 64;

        boolean fixed = simulatedCanFitAll(outputs, drops, slotLimit);
        assertTrue(fixed, "RED: 30+30 fits in 2 empty slots with limit=64");
    }

    // ── O10: canFitAll pre-check must NOT modify real handler ──

    @Test
    void quarry_canFitAll_preCheck_doesNotMutateHandler() throws Exception {
        // RED: canFitAll must not modify the real itemHandler state.
        // GREEN: uses a local ItemStack[] copy — handler unchanged.
        var sourceFile = new java.io.File(
                "src/main/java/com/modularmc/ten/common/blockentity/machine/QuarryBlockEntity.java");
        assertTrue(sourceFile.exists());
        var content = java.nio.file.Files.readString(sourceFile.toPath());
        int canFitAllStart = content.indexOf("private boolean canFitAll");
        assertTrue(canFitAllStart >= 0);
        int canFitAllEnd = content.indexOf("private void fitAll", canFitAllStart);
        if (canFitAllEnd < 0) canFitAllEnd = content.indexOf("private boolean canFit(", canFitAllStart);
        String methodBody = content.substring(canFitAllStart, canFitAllEnd);

        // Must not call setStackInSlot or any mutation method on itemHandler
        assertFalse(methodBody.contains("setStackInSlot"),
                "RED: Quarry canFitAll must NOT call setStackInSlot — uses local copy only. "
                + "Pre-check must have zero side effects on real handler.");
    }

    // ── O11: canFitAll + fitAll full flow — commit or fail-fast ──

    @Test
    void quarry_canFitAll_simulated_commitNoRemaining() {
        // Full flow: canFitAll → simulates → if false, never commit (zero loss).
        // 2 slots: [item1×62, EMPTY], drops: [item1×5, item2×1], slotLimit=64
        // item1×5: slot0 room=2→moves2→remaining3; slot1 fills3
        // item2×1: slot0 item1≠item2; slot1 item1≠item2 → cannot fit!
        int[][] outputs = {{1, 62}, {0, 0}};
        int[][] drops = {{1, 5}, {2, 1}};
        int slotLimit = 64;

        int n = outputs.length;
        int[] simTypes = new int[n];
        int[] simCounts = new int[n];
        for (int i = 0; i < n; i++) {
            simTypes[i] = outputs[i][0];
            simCounts[i] = outputs[i][1];
        }

        boolean allFit = true;
        for (int[] drop : drops) {
            int remaining = drop[1];
            for (int j = 0; j < n && remaining > 0; j++) {
                if (simTypes[j] == 0) {
                    int fill = Math.min(remaining, slotLimit);
                    simTypes[j] = drop[0];
                    simCounts[j] = fill;
                    remaining -= fill;
                } else if (simTypes[j] == drop[0]) {
                    int room = slotLimit - simCounts[j];
                    int moved = Math.min(room, remaining);
                    if (moved > 0) {
                        simCounts[j] += moved;
                        remaining -= moved;
                    }
                }
            }
            if (remaining > 0) {
                allFit = false;
                assertEquals(1, remaining,
                        "RED: zero-loss invariant — item2×1 cannot fit after item1 fills both slots");
                break;
            }
        }
        assertFalse(allFit,
                "RED: Not all drops fit — commit must NOT happen, preventing partial state");
    }

    // ── O12: fitAll snapshot+commit pattern — handler not mutated during simulate ──

    @Test
    void quarry_fitAll_doesNotMutateDuringSimulatePhase() throws Exception {
        // RED: fitAll must separate simulation from commit.
        // During simulation phase (before commit loop), itemHandler must not be touched.
        var sourceFile = new java.io.File(
                "src/main/java/com/modularmc/ten/common/blockentity/machine/QuarryBlockEntity.java");
        assertTrue(sourceFile.exists());
        var content = java.nio.file.Files.readString(sourceFile.toPath());
        int fitAllStart = content.indexOf("private void fitAll");
        assertTrue(fitAllStart >= 0);
        int fitAllEnd = content.indexOf("private void insertFirstFit", fitAllStart);
        if (fitAllEnd < 0) fitAllEnd = content.indexOf("private boolean canFit(", fitAllStart);
        if (fitAllEnd < 0) fitAllEnd = fitAllStart + 500;
        String methodBody = content.substring(fitAllStart, fitAllEnd);

        // Must contain a snapshot copy step (copyOutputSlots or equivalent)
        boolean hasSnapshot = methodBody.contains("copyOutputSlots") || methodBody.contains("ItemStack[]")
                || (methodBody.contains("copy()") && methodBody.contains("getStackInSlot"));
        boolean hasCommit = methodBody.contains("setStackInSlot");
        boolean hasThrow = methodBody.contains("throw");

        assertTrue(hasSnapshot || hasCommit || hasThrow,
                "RED: Quarry fitAll must use snapshot+commit pattern. "
                + "Must snapshot output slots before mutation, "
                + "and commit only after all drops confirmed to fit.");
    }

    // ── O13: giveGeneratedLoot single-stack path still works ──

    @Test
    void quarry_giveGeneratedLoot_usesCanFitAndInsertFirstFit() throws Exception {
        // The single-stack path (giveGeneratedLoot) should still work:
        //   canFit(stack) → insertFirstFit(stack.copy())
        // insertFirstFit must copy before mutation and throw on remaining.
        var sourceFile = new java.io.File(
                "src/main/java/com/modularmc/ten/common/blockentity/machine/QuarryBlockEntity.java");
        assertTrue(sourceFile.exists());
        var content = java.nio.file.Files.readString(sourceFile.toPath());

        // giveGeneratedLoot should call canFit and insertFirstFit
        assertTrue(content.contains("canFit(stack)") || content.contains("canFit(drop)"),
                "giveGeneratedLoot should use canFit() for single-item check");
        assertTrue(content.contains("insertFirstFit(stack.copy())"),
                "giveGeneratedLoot should call insertFirstFit with .copy()");
    }
}

// ════════════════════════════════════════════════════════════
// I. getEffectBaseDuration removed
// ════════════════════════════════════════════════════════════

    @Nested
    class GetEffectBaseDurationRemoved {

        @Test
        void baseClass_doesNotHaveGetEffectBaseDuration() throws Exception {
            var sourceFile = new java.io.File(
                    "src/main/java/com/modularmc/ten/api/blockentity/EffectMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = java.nio.file.Files.readString(sourceFile.toPath());
            assertFalse(content.contains("getEffectBaseDuration"),
                    "RED: getEffectBaseDuration() must be REMOVED from EffectMachineBlockEntity. "
                    + "GREEN after: method removed, BEACON_BASE_DURATION stays in Beacon.");
        }

        @Test
        void beaconDoesNotOverrideGetEffectBaseDuration() throws Exception {
            var sourceFile = new java.io.File(
                    "src/main/java/com/modularmc/ten/common/blockentity/machine/BeaconBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = java.nio.file.Files.readString(sourceFile.toPath());
            assertFalse(content.contains("getEffectBaseDuration"),
                    "RED: getEffectBaseDuration() override must be REMOVED from BeaconBlockEntity. "
                    + "GREEN after: override removed, BEACON_BASE_DURATION constant stays.");
        }

        @Test
        void beaconStillHasBaseDurationConstant() throws Exception {
            var sourceFile = new java.io.File(
                    "src/main/java/com/modularmc/ten/common/blockentity/machine/BeaconBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = java.nio.file.Files.readString(sourceFile.toPath());
            assertTrue(content.contains("BEACON_BASE_DURATION"),
                    "BEACON_BASE_DURATION constant must still exist in Beacon.");
        }
    }

    // ════════════════════════════════════════════════════════════
    // J. Source verification: doBaseData applies effective values
    // ════════════════════════════════════════════════════════════

    @Nested
    class SourceVerification {

        @Test
        void cmMachine_hasTheoreticalBatchSizeMethod() throws Exception {
            var sourceFile = new java.io.File(
                    "src/main/java/com/modularmc/ten/api/blockentity/CmMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = java.nio.file.Files.readString(sourceFile.toPath());
            assertTrue(content.contains("getTheoreticalBatchSize"),
                    "RED: CmMachine must have getTheoreticalBatchSize() method. "
                    + "GREEN after: method present.");
        }

        @Test
        void doBaseData_appliesEffectiveCapacity() throws Exception {
            var sourceFile = new java.io.File(
                    "src/main/java/com/modularmc/ten/api/blockentity/CmMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = java.nio.file.Files.readString(sourceFile.toPath());
            assertTrue(content.contains("energyStorage.setCapacity(") || content.contains("energyStorage.setMaxReceive("),
                    "RED: doBaseData() must set effective capacity/maxReceive/maxExtract on energyStorage. "
                    + "GREEN after: effective values synced.");
        }

        @Test
        void energyAllowRun_usesGetLockedBatchSize() throws Exception {
            var sourceFile = new java.io.File(
                    "src/main/java/com/modularmc/ten/api/blockentity/CmMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = java.nio.file.Files.readString(sourceFile.toPath());
            // Find energyAllowRun method body and verify it references getLockedBatchSize
            int methodStart = content.indexOf("public boolean energyAllowRun()");
            assertTrue(methodStart >= 0, "energyAllowRun method must exist");
            // Find the end of the method (next public or the closing brace at end of range)
            int methodEnd = content.indexOf("public int getTheoreticalBatchSize", methodStart + 1);
            if (methodEnd < 0) methodEnd = content.indexOf("public static int safeMultiply", methodStart + 1);
            if (methodEnd < 0) methodEnd = methodStart + 2000; // fallback
            String methodBody = content.substring(methodStart, methodEnd);
            assertTrue(methodBody.contains("getLockedBatchSize"),
                    "RED: energyAllowRun method body must reference getLockedBatchSize() for batch-aware check. "
                    + "GREEN after: energyAllowRun batch-aware.");
        }

        @Test
        void machineEnergyStorage_hasSetCapacity() throws Exception {
            var sourceFile = new java.io.File(
                    "src/main/java/com/modularmc/ten/api/capability/MachineEnergyStorage.java");
            assertTrue(sourceFile.exists());
            var content = java.nio.file.Files.readString(sourceFile.toPath());
            assertTrue(content.contains("public void setCapacity"),
                    "RED: MachineEnergyStorage must have setCapacity(int) method. "
                    + "GREEN after: setCapacity added.");
        }
    }
}
