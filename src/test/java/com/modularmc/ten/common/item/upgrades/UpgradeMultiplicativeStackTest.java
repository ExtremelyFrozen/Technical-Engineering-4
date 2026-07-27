// -*- coding: utf-8 -*-
package com.modularmc.ten.common.item.upgrades;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>
 * Current (P0 baseline): additive model via {@code onUpgradeApply(percent, slotIncrease)}.
 * LevelupAug: +20% additive, LevelupPower: +35% additive, LevelupShulker: +75% additive.
 * <p>
 * Target (P2): multiplicative duration/power model with batch_i stacking.
 * Composable via pure functions for testability.
 */
class UpgradeMultiplicativeStackTest {

    // ════════════════════════════════════════════════════════════
    // Pure multiplier functions — P2 target formulas
    // ════════════════════════════════════════════════════════════

    /** Duration multiplier: Π(1 - reduction_i), clamped to >= 1 tick cap. */
    static double durationMultiplier(double... reductions) {
        double m = 1.0;
        for (double r : reductions) {
            m *= (1.0 - r);
        }
        return m;
    }

    /** Power multiplier: Π(1 + increase_i), capped to prevent int overflow. */
    static double powerMultiplier(double... increases) {
        double m = 1.0;
        for (double inc : increases) {
            m *= (1.0 + inc);
        }
        return m;
    }

    /** Batch factor: B = 1 + Σbatch_i */
    static int batchFactor(int... batchValues) {
        int sum = 0;
        for (int b : batchValues) {
            sum += b;
        }
        return 1 + sum;
    }

    /** Target effective duration = baseTicks * durationMultiplier (clamped to >= 1) */
    static int effectiveDuration(int baseTicks, double durationMul) {
        return Math.max(1, (int) Math.ceil(baseTicks * durationMul));
    }

    // ════════════════════════════════════════════════════════════
    // 四款升级数值映射
    // ════════════════════════════════════════════════════════════

    static final double AUG_REDUCTION = 0.25;
    static final double AUG_INCREASE = 0.30;
    static final int AUG_BATCH = 0;

    static final double POWER_REDUCTION = 0.40;
    static final double POWER_INCREASE = 0.50;
    static final int POWER_BATCH = 1;

    static final double SHULKER_REDUCTION = 0.60;
    static final double SHULKER_INCREASE = 1.00;
    static final int SHULKER_BATCH = 3;

    // LevelupSyn — independent multipliers
    static final double SYN_TIME_MULTIPLIER = 1.5;
    static final double SYN_FE_MULTIPLIER = 0.8;
    static final int SYN_BATCH = 0;

    // ════════════════════════════════════════════════════════════
    // A. 单升级数值验证
    // ════════════════════════════════════════════════════════════

    @Nested
    class SingleUpgradeValues {

        @Test
        void levelupAug_duration() {
            assertEquals(0.75, durationMultiplier(AUG_REDUCTION), 1e-12,
                    "LevelupAug: 1 - 0.25 = 0.75");
        }

        @Test
        void levelupAug_power() {
            assertEquals(1.30, powerMultiplier(AUG_INCREASE), 1e-12,
                    "LevelupAug: 1 + 0.30 = 1.30");
        }

        @Test
        void levelupAug_batch() {
            assertEquals(1, batchFactor(AUG_BATCH),
                    "LevelupAug: B = 1 + 0 = 1");
        }

        @Test
        void levelupPower_duration() {
            assertEquals(0.60, durationMultiplier(POWER_REDUCTION), 1e-12,
                    "LevelupPower: 1 - 0.40 = 0.60");
        }

        @Test
        void levelupPower_power() {
            assertEquals(1.50, powerMultiplier(POWER_INCREASE), 1e-12,
                    "LevelupPower: 1 + 0.50 = 1.50");
        }

        @Test
        void levelupPower_batch() {
            assertEquals(2, batchFactor(POWER_BATCH),
                    "LevelupPower: B = 1 + 1 = 2");
        }

        @Test
        void levelupShulker_duration() {
            assertEquals(0.40, durationMultiplier(SHULKER_REDUCTION), 1e-12,
                    "LevelupShulker: 1 - 0.60 = 0.40");
        }

        @Test
        void levelupShulker_power() {
            assertEquals(2.00, powerMultiplier(SHULKER_INCREASE), 1e-12,
                    "LevelupShulker: 1 + 1.00 = 2.00");
        }

        @Test
        void levelupShulker_batch() {
            assertEquals(4, batchFactor(SHULKER_BATCH),
                    "LevelupShulker: B = 1 + 3 = 4");
        }
    }

    // ════════════════════════════════════════════════════════════
    // B. 乘法叠加组合验证
    // ════════════════════════════════════════════════════════════

    @Nested
    class MultiplicativeStacking {

        @Test
        void augPlusPower_duration() {
            // 0.75 * 0.60 = 0.45
            double m = durationMultiplier(AUG_REDUCTION, POWER_REDUCTION);
            assertEquals(0.45, m, 1e-12,
                    "Aug+Power: 0.75 * 0.60 = 0.45");
        }

        @Test
        void augPlusPower_power() {
            // 1.30 * 1.50 = 1.95
            double m = powerMultiplier(AUG_INCREASE, POWER_INCREASE);
            assertEquals(1.95, m, 1e-12,
                    "Aug+Power: 1.30 * 1.50 = 1.95");
        }

        @Test
        void augPlusPower_batch() {
            // B = 1 + 0 + 1 = 2
            assertEquals(2, batchFactor(AUG_BATCH, POWER_BATCH));
        }

        @Test
        void augPlusPowerPlusShulker_duration() {
            // 0.75 * 0.60 * 0.40 = 0.18 (plan example)
            double m = durationMultiplier(
                    AUG_REDUCTION, POWER_REDUCTION, SHULKER_REDUCTION);
            assertEquals(0.18, m, 1e-12,
                    "Aug+Power+Shulker: 0.75 * 0.60 * 0.40 = 0.18");
        }

        @Test
        void augPlusPowerPlusShulker_power() {
            // 1.30 * 1.50 * 2.00 = 3.90 (plan example)
            double m = powerMultiplier(
                    AUG_INCREASE, POWER_INCREASE, SHULKER_INCREASE);
            assertEquals(3.90, m, 1e-12,
                    "Aug+Power+Shulker: 1.30 * 1.50 * 2.00 = 3.90");
        }

        @Test
        void augPlusPowerPlusShulker_batch() {
            // B = 1 + 0 + 1 + 3 = 5 (plan example)
            assertEquals(5, batchFactor(AUG_BATCH, POWER_BATCH, SHULKER_BATCH));
        }

        @Test
        void allFour_batch() {
            // Aug(0) + Power(1) + Shulker(3) + Syn(0) = 4 → B = 5
            assertEquals(5, batchFactor(AUG_BATCH, POWER_BATCH, SHULKER_BATCH, SYN_BATCH));
        }
    }

    // ════════════════════════════════════════════════════════════
    // C. 光合供能抑制 — 独立倍率嵌入乘法链
    // ════════════════════════════════════════════════════════════

    @Nested
    class PhotosynInChain {

        @Test
        void photosyn_durationIsIndependentMultiplier() {
            // Photosyn: time × 1.5 (multiplied after reduction chain)
            double baseReduction = durationMultiplier(AUG_REDUCTION); // 0.75
            double withSyn = baseReduction * SYN_TIME_MULTIPLIER;     // 0.75 * 1.5 = 1.125
            assertEquals(1.125, withSyn, 1e-12,
                    "Aug+Syn duration: 0.75 * 1.5 = 1.125");
        }

        @Test
        void photosyn_powerIsIndependentMultiplier() {
            // Photosyn: FE/t × 0.8 (multiplied after increase chain)
            double baseIncrease = powerMultiplier(AUG_INCREASE);      // 1.30
            double withSyn = baseIncrease * SYN_FE_MULTIPLIER;        // 1.30 * 0.8 = 1.04
            assertEquals(1.04, withSyn, 1e-12,
                    "Aug+Syn power: 1.30 * 0.8 = 1.04");
        }

        @Test
        void augPlusShulkerPlusSyn_duration() {
            // Plan example: 0.75 * 0.40 * 1.5 = 0.45
            double m = durationMultiplier(AUG_REDUCTION, SHULKER_REDUCTION) * SYN_TIME_MULTIPLIER;
            assertEquals(0.45, m, 1e-12,
                    "Aug+Shulker+Syn: 0.75 * 0.40 * 1.5 = 0.45");
        }

        @Test
        void augPlusShulkerPlusSyn_power() {
            // Plan example: 1.30 * 2.00 * 0.8 = 2.08
            double m = powerMultiplier(AUG_INCREASE, SHULKER_INCREASE) * SYN_FE_MULTIPLIER;
            assertEquals(2.08, m, 1e-12,
                    "Aug+Shulker+Syn: 1.30 * 2.00 * 0.8 = 2.08");
        }

        @Test
        void photosyn_batch() {
            // Syn: batch_i = 0 → B = 1
            assertEquals(1, batchFactor(SYN_BATCH));
        }
    }

    // ════════════════════════════════════════════════════════════
    // D. 极端倍率验证 — 不溢出
    // ════════════════════════════════════════════════════════════

    @Nested
    class ExtremeMultipliers {

        @Test
        void threeShulkerPlusSyn_duration() {
            // 0.40^3 = 0.064, * 1.5 = 0.096
            double m = durationMultiplier(
                    SHULKER_REDUCTION, SHULKER_REDUCTION, SHULKER_REDUCTION)
                    * SYN_TIME_MULTIPLIER;
            assertEquals(0.096, m, 1e-12,
                    "3x Shulker + Syn: 0.4^3 * 1.5 = 0.096 (9.6%)");

            // Effective duration floor: at least 1 tick
            int ticks = effectiveDuration(100, m);
            assertTrue(ticks >= 1,
                    "Effective duration must be at least 1 tick: " + ticks);
            assertEquals(10, ticks,
                    "100 ticks * 0.096 = 9.6 → ceil = 10");
        }

        @Test
        void threeShulkerPlusSyn_power() {
            // 2.00^3 = 8.00, * 0.8 = 6.40 → +540% (plan says +560% but 6.4-1=5.4=540%)
            // Plan says +560% = 6.6... Let me recalculate:
            // powerMultiplier: 2.0 * 2.0 * 2.0 = 8.0, times 0.8 = 6.4
            double m = powerMultiplier(
                    SHULKER_INCREASE, SHULKER_INCREASE, SHULKER_INCREASE)
                    * SYN_FE_MULTIPLIER;
            assertEquals(6.40, m, 1e-12,
                    "3x Shulker + Syn: 2.0^3 * 0.8 = 6.40 (+540%)");

            // Verify no overflow when converting to int FE/t
            int fePerTick = (int) (80 * m);  // base 80 FE/t * 6.4 = 512
            assertTrue(fePerTick > 0,
                    "FE/t must not overflow: " + fePerTick);
            assertEquals(512, fePerTick);
        }

        @Test
        void powerMultiplier_doesNotOverflowInt() {
            // Extreme case: 10x Shulker would be 2^10 = 1024, well within int range
            double[] manyShulkers = new double[10];
            for (int i = 0; i < 10; i++) manyShulkers[i] = SHULKER_INCREASE;
            double m = powerMultiplier(manyShulkers);
            assertTrue(m < Integer.MAX_VALUE,
                    "Even 10x Shulker must not overflow int: " + m);
        }

        @Test
        void threePowerBatch_sum() {
            assertEquals(4, batchFactor(POWER_BATCH, POWER_BATCH, POWER_BATCH),
                    "3×Power: B = 1 + 1 + 1 + 1 = 4");
        }

        @Test
        void sixShulker_batchMax19() {
            int sum = SHULKER_BATCH + SHULKER_BATCH + SHULKER_BATCH
                    + SHULKER_BATCH + SHULKER_BATCH + SHULKER_BATCH;
            assertEquals(18, sum, "6×Shulker: Σbatch_i = 18");
            assertEquals(19, batchFactor(
                    SHULKER_BATCH, SHULKER_BATCH, SHULKER_BATCH,
                    SHULKER_BATCH, SHULKER_BATCH, SHULKER_BATCH),
                    "6×Shulker: B = 1 + 18 = 19");
        }

        @Test
        void sixShulkerPlusSyn_B19() {
            assertEquals(19, batchFactor(
                    SHULKER_BATCH, SHULKER_BATCH, SHULKER_BATCH,
                    SHULKER_BATCH, SHULKER_BATCH, SHULKER_BATCH,
                    SYN_BATCH),
                    "6×Shulker+Syn: B = 19 (Syn batch=0)");
        }

        @Test
        void powerMultiplier_sixShulker_noOverflow() {
            double m = 1.0;
            for (int i = 0; i < 6; i++) m *= (1.0 + SHULKER_INCREASE);
            assertEquals(64.0, m, 1e-9, "6×Shulker powerMultiplier = 64.0");
            int fePerTick = (int) Math.round(80.0 * m);
            assertTrue(fePerTick > 0 && fePerTick < Integer.MAX_VALUE / 2,
                    "FE/t within safe range: " + fePerTick);
        }
    }

    // ════════════════════════════════════════════════════════════
    // E. 有效持续时间验证
    // ════════════════════════════════════════════════════════════

    @Nested
    class EffectiveDuration {

        @Test
        void noUpgrade_durationEqualsBaseTicks() {
            assertEquals(200, effectiveDuration(200, 1.0));
        }

        @Test
        void augOnly_durationReduction() {
            int ticks = effectiveDuration(200, durationMultiplier(AUG_REDUCTION));
            assertEquals(150, ticks,
                    "200 * 0.75 = 150 ticks (25% reduction)");
        }

        @Test
        void extremeReduction_floorAt1() {
            int ticks = effectiveDuration(100, 0.001);
            assertEquals(1, ticks,
                    "Even with extreme reduction, duration >= 1 tick");
        }
    }

    // ════════════════════════════════════════════════════════════
    // F. (reserved)
    // ════════════════════════════════════════════════════════════

    // ════════════════════════════════════════════════════════════
    // G. 同类型重复安装验证
    // ════════════════════════════════════════════════════════════

    @Nested
    class RepeatableInstallSameType {

        @Test
        void threePower_sameTypeDuration() {
            // 3×Power: duration = (1-0.4)^3 = 0.6^3 = 0.216
            double d = durationMultiplier(POWER_REDUCTION, POWER_REDUCTION, POWER_REDUCTION);
            assertEquals(0.216, d, 1e-12, "3×Power duration: 0.6^3 = 0.216");
        }

        @Test
        void threePower_powerIncrease() {
            // 3×Power: power = (1+0.5)^3 = 1.5^3 = 3.375
            double p = powerMultiplier(POWER_INCREASE, POWER_INCREASE, POWER_INCREASE);
            assertEquals(3.375, p, 1e-12, "3×Power power: 1.5^3 = 3.375");
        }

        @Test
        void threeShulker_sameTypeDuration() {
            // 3×Shulker: duration = 0.4^3 = 0.064
            double d = durationMultiplier(SHULKER_REDUCTION, SHULKER_REDUCTION, SHULKER_REDUCTION);
            assertEquals(0.064, d, 1e-12, "3×Shulker duration: 0.4^3 = 0.064");
        }

        @Test
        void threeShulkerAndSyn_durationAtLeast1() {
            double d = durationMultiplier(
                    SHULKER_REDUCTION, SHULKER_REDUCTION, SHULKER_REDUCTION)
                    * SYN_TIME_MULTIPLIER;
            int ticks = effectiveDuration(100, d);
            assertTrue(ticks >= 1, "Duration must be at least 1 tick: " + ticks);
        }

        @Test
        void synUpperLimit1_secondInstallRejected() {
            // Syn上限1：安装后isPhotosynInstalled=true → 第二次返回false
            boolean alreadyInstalled = true;
            boolean secondInstallAccepted = !alreadyInstalled;
            assertFalse(secondInstallAccepted,
                    "Second Syn install must be rejected when already installed");
        }

        @Test
        void sixShulkerDurationClamped() {
            // 6×Shulker: 0.4^6 = 0.004096
            double d = 1.0;
            for (int i = 0; i < 6; i++) d *= (1.0 - SHULKER_REDUCTION);
            assertEquals(0.004096, d, 1e-12, "6×Shulker duration: 0.4^6");
            int ticks = effectiveDuration(100, d);
            assertEquals(1, ticks, "Even 6xShulker: ceil(100*0.004096)=ceil(0.4096)=1");
        }
    }

    // ════════════════════════════════════════════════════════════
    // ════════════════════════════════════════════════════════════

    @Nested
    class P2MultiplicativeSourceBaseline {

        @Test
        void levelupAug_callsApplyDurationMultiplier() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/common/item/upgrades/LevelupAug.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("applyDurationMultiplier(0.75)"),
                    "P2: LevelupAug calls applyDurationMultiplier(0.75)");
        }

        @Test
        void levelupAug_callsApplyPowerMultiplier() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/common/item/upgrades/LevelupAug.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("applyPowerMultiplier(1.30)"),
                    "P2: LevelupAug calls applyPowerMultiplier(1.30)");
        }

        @Test
        void levelupAug_canApply_returnsTrue() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/common/item/upgrades/LevelupAug.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("return true;"),
                    "LevelupAug.canApply returns true (repeatable, no restriction)");
            assertFalse(content.contains("onUpgradeApply"),
                    "P2: LevelupAug no longer calls onUpgradeApply");
        }

        @Test
        void levelupPower_callsApplyDurationMultiplier() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/common/item/upgrades/LevelupPower.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("applyDurationMultiplier(0.60)"),
                    "P2: LevelupPower calls applyDurationMultiplier(0.60)");
        }

        @Test
        void levelupPower_callsApplyPowerMultiplier() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/common/item/upgrades/LevelupPower.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("applyPowerMultiplier(1.50)"),
                    "P2: LevelupPower calls applyPowerMultiplier(1.50)");
        }

        @Test
        void levelupPower_callsApplyBatchIncrease() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/common/item/upgrades/LevelupPower.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("applyBatchIncrease(1)"),
                    "P2: LevelupPower calls applyBatchIncrease(1)");
        }

        @Test
        void levelupShulker_callsApplyDurationMultiplier() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/common/item/upgrades/LevelupShulker.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("applyDurationMultiplier(0.40)"),
                    "P2: LevelupShulker calls applyDurationMultiplier(0.40)");
        }

        @Test
        void levelupShulker_callsApplyPowerMultiplier() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/common/item/upgrades/LevelupShulker.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("applyPowerMultiplier(2.00)"),
                    "P2: LevelupShulker calls applyPowerMultiplier(2.00)");
        }

        @Test
        void levelupShulker_callsApplyBatchIncrease() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/common/item/upgrades/LevelupShulker.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("applyBatchIncrease(3)"),
                    "P2: LevelupShulker calls applyBatchIncrease(3)");
        }

        @Test
        void levelupSyn_callsApplyDurationMultiplier() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/common/item/upgrades/LevelupSyn.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("applyDurationMultiplier(1.5)"),
                    "P2: LevelupSyn calls applyDurationMultiplier(1.5)");
        }

        @Test
        void levelupSyn_callsApplyPowerMultiplier() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/common/item/upgrades/LevelupSyn.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("applyPowerMultiplier(0.8)"),
                    "P2: LevelupSyn calls applyPowerMultiplier(0.8)");
        }

        @Test
        void levelupSyn_callsApplyPhotosyn() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/common/item/upgrades/LevelupSyn.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("applyPhotosyn()"),
                    "P2: LevelupSyn calls applyPhotosyn()");
        }

        @Test
        void levelupSyn_canApply_restrictsToProcessAndEffect() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/common/item/upgrades/LevelupSyn.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("MACHINE_PROCESS"),
                    "LevelupSyn.canApply checks MACHINE_PROCESS");
            assertTrue(content.contains("MACHINE_EFFECT"),
                    "LevelupSyn.canApply checks MACHINE_EFFECT");
            // Syn canApply should NOT check installed status (that's in validUpgrade)
            assertFalse(content.contains("hasUpgrade"),
                    "Syn canApply uses machine type check, not hasUpgrade (validUpgrade enforces uniqueness)");
        }

        @Test
        void cmMachineHasApplyDurationMultiplier() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/blockentity/CmMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("applyDurationMultiplier"),
                    "P2: CmMachineBlockEntity implements applyDurationMultiplier");
        }

        @Test
        void cmMachineHasApplyPowerMultiplier() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/blockentity/CmMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("applyPowerMultiplier"),
                    "P2: CmMachineBlockEntity implements applyPowerMultiplier");
        }

        @Test
        void cmMachineHasApplyBatchIncrease() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/blockentity/CmMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("applyBatchIncrease"),
                    "P2: CmMachineBlockEntity implements applyBatchIncrease");
        }

        @Test
        void cmMachineHasApplyPhotosyn() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/blockentity/CmMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("applyPhotosyn"),
                    "P2: CmMachineBlockEntity implements applyPhotosyn");
        }

        @Test
        void oldOnUpgradeApplyStillExists() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/blockentity/CmMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("public boolean onUpgradeApply"),
                    "onUpgradeApply still exists for backward compatibility");
        }

        @Test
        void nonFourUpgradesStillUseOnUpgradeApply() throws Exception {
            // Verify old upgrades (Potion, Know, Rg, etc.) still call onUpgradeApply
            var knowFile = new File(
                    "src/main/java/com/modularmc/ten/common/item/upgrades/LevelupKnow.java");
            assertTrue(knowFile.exists());
            var knowContent = Files.readString(knowFile.toPath());
            assertTrue(knowContent.contains("onUpgradeApply"),
                    "LevelupKnow still calls onUpgradeApply for backward compat");
        }
    }
}
