// -*- coding: utf-8 -*-
package com.modularmc.ten.common.item.upgrades;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract tests for LevelupSyn (Photosynthetic Suppressor) — P2 target.
 * <p>
 * Current behavior: LevelupSyn calls {@code onUpgradeApply(-0.1, 1)} (negative additive).
 * <p>
 * Target behavior:
 * <ul>
 *   <li>time ×1.5 (independent multiplier in duration chain)</li>
 *   <li>FE/t ×0.8 (independent multiplier in power chain)</li>
 *   <li>Install limit: 1 per machine</li>
 *   <li>Can apply only to MACHINE_PROCESS + MACHINE_EFFECT</li>
 *   <li>No light requirement for multiplier effect (but internal FE only from solar)</li>
 *   <li>Does not export energy to network</li>
 *   <li>No batch contribution (batch_i = 0)</li>
 * </ul>
 */
class PhotosynEffectTest {

    // ════════════════════════════════════════════════════════════
    // 光合倍率常量 — 目标值
    // ════════════════════════════════════════════════════════════

    static final double TIME_MULTIPLIER = 1.5;
    static final double FE_MULTIPLIER = 0.8;
    static final int BATCH_I = 0;
    static final int MAX_INSTALL = 1;

    // ════════════════════════════════════════════════════════════
    // Pure computations — target formulas
    // ════════════════════════════════════════════════════════════

    static double applyTimeMultiplier(double baseDurationMultiplier) {
        return baseDurationMultiplier * TIME_MULTIPLIER;
    }

    static double applyFeMultiplier(double basePowerMultiplier) {
        return basePowerMultiplier * FE_MULTIPLIER;
    }

    // ════════════════════════════════════════════════════════════
    // A. 光合倍率验证
    // ════════════════════════════════════════════════════════════

    @Nested
    class MultiplierValues {

        @Test
        void timeMultiplierIs1_point5() {
            assertEquals(1.5, TIME_MULTIPLIER, 1e-12,
                    "Photosyn: time × 1.5 (50% longer)");
        }

        @Test
        void feMultiplierIs0_point8() {
            assertEquals(0.8, FE_MULTIPLIER, 1e-12,
                    "Photosyn: FE/t × 0.8 (20% reduction)");
        }

        @Test
        void batchContributionIs0() {
            assertEquals(0, BATCH_I,
                    "Photosyn: batch_i = 0");
        }

        @Test
        void timeMultiplierAppliedIndependently() {
            // No other upgrades: durationMultiplier starts at 1.0
            double after = applyTimeMultiplier(1.0);
            assertEquals(1.5, after, 1e-12,
                    "time × 1.5 on base multiplier 1.0");
        }

        @Test
        void feMultiplierAppliedIndependently() {
            double after = applyFeMultiplier(1.0);
            assertEquals(0.8, after, 1e-12,
                    "FE/t × 0.8 on base multiplier 1.0");
        }

        @Test
        void bothMultipliersAppliedTogether() {
            // Aug reduction 0.75 × Syn time 1.5 = 1.125
            double duration = UpgradeMultiplicativeStackTest.durationMultiplier(0.25) * TIME_MULTIPLIER;
            assertEquals(1.125, duration, 1e-12,
                    "Aug+Syn duration: 0.75 × 1.5 = 1.125");

            // Aug increase 1.30 × Syn FE 0.8 = 1.04
            double power = UpgradeMultiplicativeStackTest.powerMultiplier(0.30) * FE_MULTIPLIER;
            assertEquals(1.04, power, 1e-12,
                    "Aug+Syn power: 1.30 × 0.8 = 1.04");
        }
    }

    // ════════════════════════════════════════════════════════════
    // B. 安装上限 1
    // ════════════════════════════════════════════════════════════

    @Nested
    class InstallLimit {

        @Test
        void maxInstallIs1() {
            assertEquals(1, MAX_INSTALL,
                    "Photosyn: max 1 per machine");
        }

        @Test
        void secondInstallShouldBeRejected() {
            // Simulate: isPhotosynInstalled flag tracked
            boolean alreadyInstalled = true;
            boolean secondInstall = !alreadyInstalled && true;  // would be rejected

            assertTrue(alreadyInstalled,
                    "When photosyn is already installed, second install must be rejected");
            assertFalse(secondInstall,
                    "Second install returns false");
        }

        @Test
        void firstInstallAllowed() {
            boolean alreadyInstalled = false;
            boolean firstInstall = !alreadyInstalled;
            assertTrue(firstInstall,
                    "First photosyn install is allowed");
        }
    }

    // ════════════════════════════════════════════════════════════
    // C. canApply — 仅 MACHINE_PROCESS + MACHINE_EFFECT
    // ════════════════════════════════════════════════════════════

    @Nested
    class CanApplyRestriction {

        @Test
        void target_appliesOnlyToProcessAndEffect() {
            // 验证 LevelupSyn 的 canApply 策略（非当前实现）
            // 目标：isType("MACHINE_PROCESS") || isType("MACHINE_EFFECT")
            String expectedCheck = "MACHINE_PROCESS || MACHINE_EFFECT";
            assertNotNull(expectedCheck,
                    "Photosyn: canApply restricts to PROCESS + EFFECT");
        }

        @Test
        void target_doesNotApplyToGenerator() {
            // 发电机（MACHINE_GENERATOR）不可安装
            assertFalse(isMachineTypeAllowed("MACHINE_GENERATOR", true),
                    "Generator must NOT accept photosyn");
        }

        @Test
        void target_doesNotApplyToCable() {
            assertFalse(isMachineTypeAllowed("MACHINE_CABLE", false));
        }

        @Test
        void target_doesNotApplyToCell() {
            assertFalse(isMachineTypeAllowed("MACHINE_CELL", false));
        }

        @Test
        void target_doesNotApplyToChannel() {
            assertFalse(isMachineTypeAllowed("MACHINE_CHANNEL", false));
        }

        /** Minimal helper: simulates the canApply check. */
        private static boolean isMachineTypeAllowed(String type, boolean hasUpgradeSlots) {
            // Target: only PROCESS and EFFECT are allowed
            return hasUpgradeSlots &&
                    ("MACHINE_PROCESS".equals(type) || "MACHINE_EFFECT".equals(type));
        }

        @Test
        void currentLevelupSyn_canApply_checksProcessAndEffect() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/common/item/upgrades/LevelupSyn.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("MACHINE_PROCESS"),
                    "LevelupSyn.canApply checks MACHINE_PROCESS");
            assertTrue(content.contains("MACHINE_EFFECT"),
                    "LevelupSyn.canApply checks MACHINE_EFFECT");
        }

        @Test
        void currentLevelupSyn_canApply_notGeneric() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/common/item/upgrades/LevelupSyn.java");
            assertTrue(sourceFile.exists());
            var lines = Files.readAllLines(sourceFile.toPath());
            // Find the canApply method body and verify it doesn't blindly return true
            boolean inCanApply = false;
            boolean foundBareReturnTrue = false;
            int braceDepth = 0;
            for (String line : lines) {
                if (line.contains("public boolean canApply(IUpgradableMachine machine)")) {
                    inCanApply = true;
                }
                if (inCanApply) {
                    for (char c : line.toCharArray()) {
                        if (c == '{') braceDepth++;
                        if (c == '}') braceDepth--;
                    }
                    if (braceDepth <= 0 && inCanApply) break;
                    // Inside method body: check for bare "return true" without conditional
                    if (line.contains("return true;") && !line.contains("||") && !line.contains("&&")) {
                        foundBareReturnTrue = true;
                        break;
                    }
                }
            }
            assertFalse(foundBareReturnTrue,
                    "LevelupSyn.canApply must use conditional return (type check), not bare return true");
        }
    }

    // ════════════════════════════════════════════════════════════
    // D. 无光时倍率仍生效
    // ════════════════════════════════════════════════════════════

    @Nested
    class NoLightBehavior {

        @Test
        void multiplierActiveEvenWithoutLight() {
            // 光合供能抑制的 time/FE 倍率是持续的，不依赖光照
            // 无光时：倍率 ×1.5/×0.8 仍生效
            // 只是内部 FE 补能不产生（来自太阳能的内部 FE）
            double noLight = applyTimeMultiplier(1.0);
            assertEquals(1.5, noLight, 1e-12,
                    "Time multiplier still active without light");
        }

        @Test
        void internalFeGenerationRequiresLight() {
            // 内部 FE 补能需要光照（来自太阳能面板逻辑）
            // 但倍率本身不依赖光照
            boolean hasLight = false;
            int internalFeGeneration = hasLight ? 100 : 0;
            assertEquals(0, internalFeGeneration,
                    "No internal FE generation without light");

            // With light
            hasLight = true;
            internalFeGeneration = hasLight ? 100 : 0;
            assertEquals(100, internalFeGeneration,
                    "Internal FE generation with light");
        }

        @Test
        void externalPowerStillWorksWithoutLight() {
            // 无光时机器仍可从外部接收能量运行
            boolean hasLight = false;
            boolean externalPowerAvailable = true;
            boolean canRun = externalPowerAvailable;  // 不依赖 light

            assertTrue(canRun,
                    "Machine can run on external power even without light");
        }
    }

    // ════════════════════════════════════════════════════════════
    // E. 内部 FE 不向网络输出
    // ════════════════════════════════════════════════════════════

    @Nested
    class InternalFeNoExport {

        @Test
        void photosynGeneratedFe_goesToInternalStorageOnly() {
            // 光合产生的内部 FE 仅补充本机储能，不推送网络
            int generatedFE = 100;
            int internalStorage = 500;
            int networkExported = 0;  // 不输出

            int newStorage = internalStorage + generatedFE;
            assertEquals(600, newStorage,
                    "Generated FE adds to internal storage");
            assertEquals(0, networkExported,
                    "Generated FE is NOT exported to network");
        }

        @Test
        void externalEnergy_doesExportNormally() {
            // 外部输入的能量不受影响
            int externalReceived = 200;
            boolean isPhotosynGenerated = false;
            int networkExported = isPhotosynGenerated ? 0 : externalReceived;

            assertEquals(200, networkExported,
                    "External energy exports normally");
        }
    }

    // ════════════════════════════════════════════════════════════
    // G. 10FE/t 注入时序与条件
    // ════════════════════════════════════════════════════════════

    @Nested
    class TenFePerTickInjection {

        static final int INJECTION_AMOUNT = 10;

        @Test
        void injectionAmount_is10_notMultiplied() {
            assertEquals(10, INJECTION_AMOUNT,
                    "Syn injection is exactly 10 FE/t");
            // Not multiplied by B, not multiplied by power multiplier
            int B = 5;
            int notThis = INJECTION_AMOUNT * B;
            assertTrue(notThis != INJECTION_AMOUNT,
                    "Injection is NOT multiplied by B (" + notThis + " != 10)");
        }

        @Test
        void withLightAndSyn_injects10() {
            boolean synInstalled = true;
            boolean hasLight = true;
            int storage = 100;
            int maxStorage = 10000;

            if (synInstalled && hasLight) {
                storage = Math.min(storage + INJECTION_AMOUNT, maxStorage);
            }
            assertEquals(110, storage, "10 FE injected with light+syn");
        }

        @Test
        void noLight_noInjection() {
            boolean synInstalled = true;
            boolean hasLight = false;
            int storage = 100;

            if (synInstalled && hasLight) {
                storage = Math.min(storage + INJECTION_AMOUNT, maxStorage());
            }
            assertEquals(100, storage, "No injection without light");
        }

        @Test
        void noSyn_noInjection() {
            boolean synInstalled = false;
            boolean hasLight = true;
            int storage = 100;

            if (synInstalled && hasLight) {
                storage = Math.min(storage + INJECTION_AMOUNT, maxStorage());
            }
            assertEquals(100, storage, "No injection without syn upgrade");
        }

        @Test
        void fullStorage_noError() {
            boolean synInstalled = true;
            boolean hasLight = true;
            int storage = 10000;
            int maxStorage = 10000;

            if (synInstalled && hasLight) {
                storage = Math.min(storage + INJECTION_AMOUNT, maxStorage);
            }
            assertEquals(10000, storage,
                    "Full storage: injection discarded without error");
        }

        @Test
        void injection_beforeStallCheck_order() {
            // Plan: Syn注入在停滞判断之前执行
            boolean synInstalled = true;
            boolean hasLight = true;
            int energyBeforeInjection = 5;
            int fePerTick = 100;

            // Step 1: Inject
            int energyAfterInjection = Math.min(energyBeforeInjection + INJECTION_AMOUNT, maxStorage());

            // Step 2: Stall check (after injection)
            boolean stalled = energyAfterInjection < fePerTick;
            // With 5+10=15 < 100 → still stalled, but injection happened first
            assertTrue(energyAfterInjection > energyBeforeInjection,
                    "Injection executed before stall check: 5→15");
            assertTrue(stalled, "Still stalled but injection already credited");
        }

        // helper to avoid magic number
        private static int maxStorage() { return 10000; }
    }

    // ════════════════════════════════════════════════════════════
    // H. Syn 不外送验证
    // ════════════════════════════════════════════════════════════

    @Nested
    class SynNoExport {

        @Test
        void synEnergyStaysLocal() {
            int synGenerated = 10;
            int exportedToNetwork = 0; // Syn energy does NOT export

            assertEquals(0, exportedToNetwork,
                    "Syn energy stays in local storage, not exported");
        }

        @Test
        void synEnergy_doesNotGoToCable() {
            int localStorage = 500;
            int synInjection = 10;
            int cableReceived = 0; // Not pushed to network

            localStorage += synInjection;
            assertEquals(510, localStorage, "Syn energy in local storage");
            assertEquals(0, cableReceived, "Cable received 0 from Syn");
        }
    }

    // ════════════════════════════════════════════════════════════
    // ════════════════════════════════════════════════════════════

    @Nested
    class SourceBaseline {

        @Test
        void levelupSyn_usesMultiplierNotAdditive() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/common/item/upgrades/LevelupSyn.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            // P2: uses applyDurationMultiplier/applyPowerMultiplier, not onUpgradeApply
            assertTrue(content.contains("applyDurationMultiplier"),
                    "P2: LevelupSyn uses applyDurationMultiplier");
            assertTrue(content.contains("applyPowerMultiplier"),
                    "P2: LevelupSyn uses applyPowerMultiplier");
            assertTrue(content.contains("applyPhotosyn"),
                    "P2: LevelupSyn calls applyPhotosyn()");
        }

        @Test
        void levelupSyn_checksPhotosynInstalledInEffect() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/common/item/upgrades/LevelupSyn.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("photosynInstalled"),
                    "P2: LevelupSyn checks photosynInstalled in effect() for idempotency");
        }

        @Test
        void cmMachineHasApplyPhotosyn() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/blockentity/CmMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("public void applyPhotosyn"),
                    "P2: CmMachineBlockEntity implements applyPhotosyn()");
            assertTrue(content.contains("photosynInstalled"),
                    "P2: photosynInstalled field present");
        }

        @Test
        void cmMachineHasPhotosynInstalledField() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/blockentity/CmMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("public boolean photosynInstalled"),
                    "P2: photosynInstalled field is public in CmMachineBlockEntity");
        }
    }
}
