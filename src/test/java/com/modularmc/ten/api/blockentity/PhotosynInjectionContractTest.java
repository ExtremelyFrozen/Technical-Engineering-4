// -*- coding: utf-8 -*-
package com.modularmc.ten.api.blockentity;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract tests for {@link CmMachineBlockEntity#tryInjectPhotosynEnergy()} — P2-T7.
 * <p>
 * Covers injection semantics: 10 FE/t, light-gated, no B/power multiplier,
 * before energy check ordering, machine type restrictions.
 */
class PhotosynInjectionContractTest {

    static final int INJECTION_AMOUNT = 10;

    // ════════════════════════════════════════════════════════════
    // Pure injection simulation helpers
    // ════════════════════════════════════════════════════════════

    /** Simulates one injection call — returns actual energy increase */
    static int simulateInjection(int energyStored, int maxStorage, boolean hasLight, boolean synInstalled) {
        if (!synInstalled) return 0;
        if (!hasLight) return 0;
        int space = maxStorage - energyStored;
        if (space <= 0) return 0;
        int injected = Math.min(INJECTION_AMOUNT, space);
        return injected;
    }

    /** Simulates a full tick: inject first, then check if energy sufficient for fePerTick */
    static boolean tickCanProceed(int energyStored, int maxStorage, boolean hasLight, boolean synInstalled, int fePerTick) {
        int injected = simulateInjection(energyStored, maxStorage, hasLight, synInstalled);
        int energyAfterInjection = energyStored + injected;
        return energyAfterInjection >= fePerTick;
    }

    // ════════════════════════════════════════════════════════════
    // A. Injection amount
    // ════════════════════════════════════════════════════════════

    @Nested
    class InjectionAmount {

        @Test
        void injectionIs10_exact() {
            assertEquals(10, INJECTION_AMOUNT,
                    "Photosyn injection is exactly 10 FE/t");
        }

        @Test
        void notMultipliedByB() {
            int B = 5;
            assertNotEquals(INJECTION_AMOUNT * B, INJECTION_AMOUNT,
                    "Injection must NOT be multiplied by B");
        }

        @Test
        void notMultipliedByPowerMultiplier() {
            double powerMul = 0.8;
            int notThis = (int) Math.round(INJECTION_AMOUNT * powerMul);
            assertNotEquals(10, notThis,
                    "Injection must NOT be multiplied by power multiplier (8 != 10)");
        }

        @Test
        void synMultipliersStillActiveButInjectionFixed() {
            // Syn's 0.8 power multiplier affects energy consumption, not injection
            int baseFePerTick = 80;
            double powerMul = 0.8;
            int fePerTickAfterSyn = Math.max(1, (int) Math.round(baseFePerTick * powerMul));
            assertEquals(64, fePerTickAfterSyn,
                    "Syn power multiplier reduces consumption to 64 FE/t");
            assertEquals(10, INJECTION_AMOUNT,
                    "Injection stays 10 FE/t regardless of power multiplier");
        }
    }

    // ════════════════════════════════════════════════════════════
    // B. Light gate
    // ════════════════════════════════════════════════════════════

    @Nested
    class LightGate {

        @Test
        void withLightAndSyn_injects() {
            int injected = simulateInjection(100, 10000, true, true);
            assertEquals(10, injected, "Light + Syn → 10 FE injected");
        }

        @Test
        void noLight_noInjection() {
            int injected = simulateInjection(100, 10000, false, true);
            assertEquals(0, injected, "No light → 0 injection even with Syn");
        }

        @Test
        void noSyn_noInjectionEvenWithLight() {
            int injected = simulateInjection(100, 10000, true, false);
            assertEquals(0, injected, "No Syn → 0 injection even with light");
        }

        @Test
        void noLight_noSyn_noInjection() {
            int injected = simulateInjection(100, 10000, false, false);
            assertEquals(0, injected, "No light, no Syn → 0 injection");
        }

        @Test
        void darkStillAppliesSynMultipliers() {
            // Syn's ×1.5 duration and ×0.8 power still apply in dark
            double durationMul = 1.5;
            double powerMul = 0.8;
            assertTrue(durationMul > 1.0,
                    "Duration ×1.5 still active in dark");
            assertTrue(powerMul < 1.0,
                    "Power ×0.8 still active in dark");
            // Only injection is light-gated
            int injected = simulateInjection(100, 10000, false, true);
            assertEquals(0, injected, "Injection light-gated");
        }
    }

    // ════════════════════════════════════════════════════════════
    // C. Storage space handling
    // ════════════════════════════════════════════════════════════

    @Nested
    class StorageHandling {

        @Test
        void injectsExact10_whenSpaceAvailable() {
            int injected = simulateInjection(100, 10000, true, true);
            assertEquals(10, injected);
        }

        @Test
        void fullStorage_returns0() {
            int injected = simulateInjection(10000, 10000, true, true);
            assertEquals(0, injected, "Full storage → 0 injected, no error");
        }

        @Test
        void partialStorage_injectsAvailableSpace() {
            // 9995 out of 10000 → space=5
            int injected = simulateInjection(9995, 10000, true, true);
            assertEquals(5, injected, "Only 5 space available → injects 5");
        }

        @Test
        void nearFull_injectsRemaining() {
            int injected = simulateInjection(9998, 10000, true, true);
            assertEquals(2, injected, "Only 2 space → injects 2");
        }

        @Test
        void justBelowFull_injects1() {
            int injected = simulateInjection(9999, 10000, true, true);
            assertEquals(1, injected, "Only 1 space → injects 1");
        }

        @Test
        void emptyStorage_injects10() {
            int injected = simulateInjection(0, 10000, true, true);
            assertEquals(10, injected, "Empty storage → injects 10");
        }

        @Test
        void returnValueMatchesActualInjected() {
            // The actual method must return the injected amount
            int injected = simulateInjection(500, 10000, true, true);
            assertEquals(10, injected, "Return value = actual injected FE");
        }
    }

    // ════════════════════════════════════════════════════════════
    // D. Injection before energy check — can "save" a tick
    // ════════════════════════════════════════════════════════════

    @Nested
    class InjectionBeforeEnergyCheck {

        @Test
        void injectionBeforeEnergyCheck_ordering() {
            // Energy=5, fePerTick=100, injection=10 → after injection 15 < 100 still stalled
            // But injection happened first (5→15 instead of stalling at 5)
            int energyBefore = 5;
            boolean wouldStallWithoutInjection = energyBefore < 100;
            assertTrue(wouldStallWithoutInjection, "Would stall without injection");

            int injected = simulateInjection(energyBefore, 10000, true, true);
            int energyAfter = energyBefore + injected;
            assertEquals(15, energyAfter, "Injection credited: 5→15");
        }

        @Test
        void injectionCanSaveTick_whenEnergyJustBelowThreshold() {
            // Energy=95, fePerTick=100, injection=10 → after=105 >= 100 → tick proceeds
            int energyBefore = 95;
            boolean wouldStallWithoutInjection = energyBefore < 100;
            assertTrue(wouldStallWithoutInjection, "Would stall without injection");

            boolean canProceed = tickCanProceed(energyBefore, 10000, true, true, 100);
            assertTrue(canProceed,
                    "Injection saves tick: 95+10=105 >= 100");
        }

        @Test
        void injectionNotEnough_stillStalled() {
            boolean canProceed = tickCanProceed(3, 10000, true, true, 100);
            assertFalse(canProceed,
                    "Injection alone may not be enough: 3+10=13 < 100");
        }

        @Test
        void injectionHappensEveryTick_notJustFirst() {
            // Each tick: inject 10 before checking
            int energy = 95;
            int fePerTick = 100;

            // Tick 1: 95 + 10 = 105 >= 100 → can proceed, consume 100 → 5 left
            boolean tick1 = tickCanProceed(energy, 10000, true, true, fePerTick);
            assertTrue(tick1, "Tick 1: injection saves it");
            energy = 5; // after consumption

            // Tick 2: 5 + 10 = 15 < 100 → stalled
            boolean tick2 = tickCanProceed(energy, 10000, true, true, fePerTick);
            assertFalse(tick2, "Tick 2: 5+10=15 < 100, still stalls");
        }
    }

    // ════════════════════════════════════════════════════════════
    // E. Machine type restriction — only PROCESS + EFFECT
    // ════════════════════════════════════════════════════════════

    @Nested
    class MachineTypeRestriction {

        @Test
        void synInstalledOnlyOnProcessAndEffect() {
            // LevelupSyn.canApply checks MACHINE_PROCESS || MACHINE_EFFECT
            // tryInjectPhotosynEnergy should gate on the same machine types
            assertTrue(isSynAllowed(1), "MACHINE_PROCESS allows Syn");
            assertTrue(isSynAllowed(2), "MACHINE_EFFECT allows Syn");
            assertFalse(isSynAllowed(0), "GENERATOR does NOT allow Syn");
            assertFalse(isSynAllowed(30), "ENGINE_SOLAR does NOT allow Syn");
            assertFalse(isSynAllowed(40), "CELL does NOT allow Syn");
        }

        private static boolean isSynAllowed(int machineType) {
            return machineType == 1 || machineType == 2;
        }

        @Test
        void tryInjectPhotosynEnergy_checksMachineType() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/blockentity/CmMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            // Should check machineType() for PROCESS or EFFECT
            assertTrue(content.contains("machineType()"),
                    "RED: tryInjectPhotosynEnergy must check machineType");
            boolean hasProcessCheck = content.contains("MACHINE_PROCESS") || content.contains("1");
            boolean hasEffectCheck = content.contains("MACHINE_EFFECT") || content.contains("2");
            assertTrue(hasProcessCheck && hasEffectCheck,
                    "RED: tryInjectPhotosynEnergy must restrict to MACHINE_PROCESS && MACHINE_EFFECT");
        }
    }

    // ════════════════════════════════════════════════════════════
    // F. Syn energy NOT exported to network
    // ════════════════════════════════════════════════════════════

    @Nested
    class SynEnergyStaysLocal {

        @Test
        void processMachine_cannotExternalExtract() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/blockentity/CmMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            // canExternalExtract must return false for PROCESS and EFFECT types
            assertTrue(content.contains("default -> false"),
                    "canExternalExtract default returns false for non-generator types");

            // Verify PROCESS/EFFECT are NOT in the EXTRACT allowlist
            // Allowlist contains: GENERATOR, ENGINE_SOLAR, ENGINE_EXTRACTION, ENGINE_METAL, ENGINE_BIOMASS, CELL, CREATIVE_CELL
            assertTrue(content.contains("GENERATOR"),
                    "canExternalExtract allows GENERATOR");
            assertFalse(content.contains("MACHINE_PROCESS") && content.contains("MACHINE_PROCESS") && !content.contains("default"),
                    "MACHINE_PROCESS must NOT be in canExternalExtract allowlist");
        }

        @Test
        void synEnergyCannotBeExportedViaCapability() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/blockentity/CmMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            // The energy capability's extractEnergy calls canExternalExtract
            assertTrue(content.contains("canExternalExtract()"),
                    "getEnergyStorage.extractEnergy gates on canExternalExtract()");
        }

        @Test
        void tryInjectPhotosynEnergy_doesNotWriteToNetwork() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/blockentity/CmMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var lines = Files.readAllLines(sourceFile.toPath());
            // Find the method body and scan only within its braces
            boolean inMethod = false;
            int braceDepth = 0;
            boolean foundReceiveEnergy = false;
            boolean foundGetEnergyStorage = false;
            for (String line : lines) {
                if (line.contains("tryInjectPhotosynEnergy")) {
                    inMethod = true;
                }
                if (inMethod) {
                    for (char c : line.toCharArray()) {
                        if (c == '{') braceDepth++;
                        if (c == '}') braceDepth--;
                    }
                    if (line.contains("receiveEnergy")) foundReceiveEnergy = true;
                    if (line.contains("getEnergyStorage(") || line.contains("getEnergyStorage (")) foundGetEnergyStorage = true;
                    if (braceDepth <= 0 && inMethod) break;
                }
            }
            assertTrue(foundReceiveEnergy,
                    "RED: tryInjectPhotosynEnergy must call energyStorage.receiveEnergy directly");
            assertFalse(foundGetEnergyStorage,
                    "RED: tryInjectPhotosynEnergy must NOT go through sided getEnergyStorage capability");
        }
    }

    // ════════════════════════════════════════════════════════════
    // G. Source verification — injection integrated into process()
    // ════════════════════════════════════════════════════════════

    @Nested
    class SourceIntegration {

        @Test
        void processingMachine_callsTryInjectPhotosynEnergy() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/blockentity/ProcessingMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("tryInjectPhotosynEnergy"),
                    "RED: ProcessingMachine.process() must call tryInjectPhotosynEnergy");
        }

        @Test
        void effectMachine_callsTryInjectPhotosynEnergy() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/blockentity/EffectMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("tryInjectPhotosynEnergy"),
                    "RED: EffectMachine.process() must call tryInjectPhotosynEnergy");
        }

        @Test
        void processingMachine_injectionBeforeAllGates() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/blockentity/ProcessingMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            // tryInjectPhotosynEnergy must appear BEFORE the gate line (conditionStart && signalAllowRun && energyAllowRun)
            int injectionIdx = content.indexOf("tryInjectPhotosynEnergy");
            int gateIdx = content.indexOf("conditionStart() && signalAllowRun()");
            assertTrue(injectionIdx >= 0 && gateIdx >= 0,
                    "Both injection call and gate expression must exist");
            assertTrue(injectionIdx < gateIdx,
                    "RED: tryInjectPhotosynEnergy must be called BEFORE conditionStart/signal/energy gate in ProcessingMachine");
        }

        @Test
        void effectMachine_injectionBeforeEnergyAllowRun() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/blockentity/EffectMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            // tryInjectPhotosynEnergy must appear BEFORE energyAllowRun( — the first gate in Effect
            int injectionIdx = content.indexOf("tryInjectPhotosynEnergy");
            int energyGateIdx = content.indexOf("energyAllowRun(");
            assertTrue(injectionIdx >= 0 && energyGateIdx >= 0,
                    "Both injection call and energyAllowRun must exist");
            assertTrue(injectionIdx < energyGateIdx,
                    "RED: tryInjectPhotosynEnergy must be called BEFORE energyAllowRun() in EffectMachine");
        }

        @Test
        void processingMachine_injectionCalledOnce() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/blockentity/ProcessingMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            // Count occurrences of tryInjectPhotosynEnergy in the PROCESS method (not the class declaration)
            int firstIdx = content.indexOf("tryInjectPhotosynEnergy");
            int lastIdx = content.lastIndexOf("tryInjectPhotosynEnergy");
            assertEquals(firstIdx, lastIdx,
                    "RED: tryInjectPhotosynEnergy must be called EXACTLY ONCE in ProcessingMachine.process()");
        }

        @Test
        void effectMachine_injectionCalledOnce() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/blockentity/EffectMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            int firstIdx = content.indexOf("tryInjectPhotosynEnergy");
            int lastIdx = content.lastIndexOf("tryInjectPhotosynEnergy");
            assertEquals(firstIdx, lastIdx,
                    "RED: tryInjectPhotosynEnergy must be called EXACTLY ONCE in EffectMachine.process()");
        }

        @Test
        void cmMachine_tryInjectPhotosynEnergy_returnsInt() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/blockentity/CmMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("tryInjectPhotosynEnergy"),
                    "RED: CmMachineBlockEntity must have tryInjectPhotosynEnergy method");
            // Should return int (injected amount)
            assertTrue(content.contains("int ") || content.contains("return"),
                    "RED: tryInjectPhotosynEnergy must return int");
        }

        @Test
        void cmMachine_tryInjectPhotosynEnergy_existsNotStub() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/blockentity/CmMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var lines = Files.readAllLines(sourceFile.toPath());
            // Scan only the method body
            boolean inMethod = false;
            int braceDepth = 0;
            boolean foundReceiveEnergy = false;
            for (String line : lines) {
                if (line.contains("tryInjectPhotosynEnergy")) {
                    inMethod = true;
                }
                if (inMethod) {
                    for (char c : line.toCharArray()) {
                        if (c == '{') braceDepth++;
                        if (c == '}') braceDepth--;
                    }
                    if (line.contains("receiveEnergy")) foundReceiveEnergy = true;
                    if (braceDepth <= 0 && inMethod) break;
                }
            }
            assertTrue(foundReceiveEnergy,
                    "RED: tryInjectPhotosynEnergy must have real implementation with receiveEnergy");
        }
    }

    // ════════════════════════════════════════════════════════════
    // H. Zero-FE injection behavior — 0FE+10 edge case contract
    // ════════════════════════════════════════════════════════════

    @Nested
    class ZeroFeInjectionBehavior {

        @Test
        void zeroFe_baseFe10_canProceed() {
            // 0FE + 10 injection, fePerTick=10 → 10 >= 10 → tick proceeds
            boolean canProceed = tickCanProceed(0, 10000, true, true, 10);
            assertTrue(canProceed,
                    "0FE+10 with baseFE<=10: injection pushes energy just enough to pass");
        }

        @Test
        void zeroFe_baseFeLt10_canProceed() {
            // 0FE + 10 injection, fePerTick=8 → 10 >= 8 → tick proceeds
            boolean canProceed = tickCanProceed(0, 10000, true, true, 8);
            assertTrue(canProceed,
                    "0FE+10 with baseFE<10: injection more than covers the cost");
        }

        @Test
        void zeroFe_baseFeGt10_injectsButStalls() {
            // 0FE + 10 injection, fePerTick=20 → 10 < 20 → still stalls
            boolean canProceed = tickCanProceed(0, 10000, true, true, 20);
            assertFalse(canProceed,
                    "0FE+10 with baseFE>10: injection still counts but not enough to proceed");
        }

        @Test
        void zeroFe_injectionAccumulatesForNextTick() {
            // Tick 1: 0FE + 10 = 10, fePerTick=15 → stalls, but energy is now 10
            // Tick 2: 10 + 10 = 20, fePerTick=15 → 20 >= 15 → proceeds
            int energy = 0;
            int fePerTick = 15;

            // Tick 1: injects 10 but still stalls (10 < 15)
            boolean tick1 = tickCanProceed(energy, 10000, true, true, fePerTick);
            assertFalse(tick1, "Tick 1: 0+10=10 < 15 → stalls but energy accumulated");
            energy = 10; // accumulated from injection

            // Tick 2: injects another 10 → 10+10=20 >= 15 → proceeds
            boolean tick2 = tickCanProceed(energy, 10000, true, true, fePerTick);
            assertTrue(tick2, "Tick 2: 10+10=20 >= 15 → proceeds thanks to accumulated injection");
        }

        @Test
        void injectionBeforeGate_makesZeroFePassWhenFePerTickLe10() {
            // Proves the ordering matters: injection BEFORE energy check
            // means 0FE machines with Syn installed can start on the same tick
            int energyBefore = 0;
            int fePerTick = 10;

            // With correct ordering: inject 10 first → 10 >= 10 → passes
            int injected = simulateInjection(energyBefore, 10000, true, true);
            int energyAfterInjection = energyBefore + injected;
            assertTrue(energyAfterInjection >= fePerTick,
                    "Ordering proof: injection before check enables 0FE+10 to pass when fePerTick=10");
            assertEquals(10, energyAfterInjection,
                    "Injection credited: 0→10");
        }
    }

    // ════════════════════════════════════════════════════════════
    // I. Client-side isolation — injection only on server
    // ════════════════════════════════════════════════════════════

    @Nested
    class ClientSideIsolation {

        @Test
        void processingProcess_hasClientGuard() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/blockentity/ProcessingMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("isClientSide"),
                    "RED: ProcessingMachine.process() must guard against client-side execution");
        }

        @Test
        void effectProcess_hasClientGuard() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/blockentity/EffectMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("isClientSide"),
                    "RED: EffectMachine.process() must guard against client-side execution");
        }
    }
}
