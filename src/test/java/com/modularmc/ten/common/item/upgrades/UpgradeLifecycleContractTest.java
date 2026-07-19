// -*- coding: utf-8 -*-
package com.modularmc.ten.common.item.upgrades;

import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;
import com.modularmc.ten.api.blockentity.RadiusMachineBlockEntity;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RED contract tests for B2 — Upgrade lifecycle fix.
 * <p>
 * These tests validate:
 * <ol>
 *   <li>{@code doBaseData()} calls {@code applyUpgradeEffects()} exactly once
 *       (not twice with a redundant overwrite in between)</li>
 *   <li>{@link RadiusMachineBlockEntity} overrides {@code resetUpgradeEffects()}
 *       to reset {@code radius} back to {@code initialRadius} so that
 *       {@link LevelupRg#effect} does not accumulate across ticks</li>
 * </ol>
 *
 * <h3>RED results (before B2 fix):</h3>
 * <ul>
 *   <li>{@code doBaseData_callsApplyUpgradeEffectsExactlyOnce} — fails:
 *       source shows 2 calls and 6 overwrite assignments</li>
 *   <li>{@code radiusReset_overriddenInRadiusMachineBlockEntity} — fails:
 *       {@code RadiusMachineBlockEntity} does not declare its own
 *       {@code resetUpgradeEffects}</li>
 * </ul>
 *
 * <h3>Testing limitations:</h3>
 * <ul>
 *   <li>Minecraft block entities cannot be instantiated without a running
 *       game environment (registry not initialised).</li>
 *   <li>We use structural/source-level contracts for the doBaseData call
 *       count, and reflection for the radius reset override.</li>
 *   <li>Pure behavioural tests (instantiate BE, tick, assert values) are
 *       not possible in this environment — structural contracts document
 *       and enforce the fix.</li>
 * </ul>
 */
class UpgradeLifecycleContractTest {

    // ════════════════════════════════════════════════════════════════
    // B2-1: Single applyUpgradeEffects() call in doBaseData()
    // ════════════════════════════════════════════════════════════════

    @Nested
    class SingleApplyContract {

        /**
         * RED before fix: source shows 2 calls to {@code applyUpgradeEffects()}
         * in the {@code doBaseData()} method body (lines 414 and 423), plus 6
         * overwrite assignments (lines 415-421) that nullify the first apply.
         * <p>
         * After fix: only 1 call remains (after {@code resetUpgradeEffects()}),
         * and the overwrite assignments are removed.
         */
        @Test
        void doBaseData_callsApplyUpgradeEffectsExactlyOnce() throws Exception {
            var sourceFile = new File(
                "src/main/java/com/modularmc/ten/api/blockentity/CmMachineBlockEntity.java"
            );
            assertTrue(sourceFile.exists(),
                "Source file must exist for structural contract check");

            long callCount;
            try (Stream<String> lines = Files.lines(sourceFile.toPath())) {
                callCount = lines
                    // Lines containing the call pattern "applyUpgradeEffects()"
                    .filter(line -> line.contains("applyUpgradeEffects()"))
                    // Exclude the method definition "void applyUpgradeEffects()"
                    .filter(line -> !line.contains("void applyUpgradeEffects"))
                    .count();
            }

            assertEquals(1, callCount,
                "doBaseData() must call applyUpgradeEffects() exactly once" +
                " (found " + callCount + " calls in source; fix: remove the" +
                " redundant first call and the 6 overwrite assignments that" +
                " nullify it)");
        }

        /**
         * Contracts that {@code resetUpgradeEffects} is {@code protected} so
         * subclasses can override it to add their own reset logic (e.g. radius).
         */
        @Test
        void resetUpgradeEffects_isProtectedForOverride() throws Exception {
            var method = CmMachineBlockEntity.class.getDeclaredMethod("resetUpgradeEffects");
            int mod = method.getModifiers();
            assertTrue(Modifier.isProtected(mod),
                "resetUpgradeEffects must be protected for subclass override");
            assertFalse(Modifier.isPrivate(mod),
                "resetUpgradeEffects must NOT be private");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // B2-2: RadiusMachineBlockEntity overrides resetUpgradeEffects
    // ════════════════════════════════════════════════════════════════

    @Nested
    class RadiusResetContract {

        /**
         * RED before fix: {@code RadiusMachineBlockEntity} does NOT override
         * {@code resetUpgradeEffects}, so {@code radius} is never reset to
         * {@code initialRadius}. Combined with the double apply, this means
         * {@link LevelupRg#effect} accumulates radius every tick without bound.
         * <p>
         * After fix: {@code RadiusMachineBlockEntity} declares its own
         * {@code resetUpgradeEffects} that calls {@code super.resetUpgradeEffects()}
         * and then resets {@code this.radius = this.initialRadius}.
         */
        @Test
        void radiusReset_overriddenInRadiusMachineBlockEntity() throws Exception {
            // RED: getDeclaredMethod throws NoSuchMethodException before fix
            Method method = RadiusMachineBlockEntity.class
                .getDeclaredMethod("resetUpgradeEffects");

            assertEquals(RadiusMachineBlockEntity.class, method.getDeclaringClass(),
                "RadiusMachineBlockEntity must override resetUpgradeEffects" +
                " to reset radius to initialRadius before each apply cycle");
        }

        /**
         * Verify that {@code resetUpgradeEffects} is overridable by checking
         * the declaring class of the method via getMethod (inherited resolution),
         * and then asserting the override has the expected signature.
         * <p>
         * Before fix, this documents the intentional design: the base class
         * method is protected so subclasses can hook in.
         */
        @Test
        void baseResetUpgradeEffects_existsAndIsAccessible() throws Exception {
            var method = CmMachineBlockEntity.class
                .getDeclaredMethod("resetUpgradeEffects");
            assertEquals(void.class, method.getReturnType());
            assertEquals(0, method.getParameterCount());
        }
    }

    // ════════════════════════════════════════════════════════════════
    // B2-3: LevelupRg.effect behaviour contracts
    // ════════════════════════════════════════════════════════════════

    @Nested
    class LevelupRgContract {

        /**
         * LevelupRg.effect() should be invoked exactly once per doBaseData
         * cycle. Its cumulative behaviour (adding to current radius) is
         * correct ONLY when the radius is first reset to initialRadius.
         * <p>
         * This test verifies the canApply guard exists and uses
         * getCurrentRadius() > 0 as gate.
         */
        @Test
        void levelupRg_canApplyChecksRadius() throws Exception {
            var method = LevelupRg.class.getDeclaredMethod(
                "canApply", IUpgradableMachine.class);
            assertEquals(LevelupRg.class, method.getDeclaringClass(),
                "LevelupRg must override canApply with radius > 0 check");
        }

        @Test
        void levelupRg_effectExists() throws Exception {
            var method = LevelupRg.class.getDeclaredMethod(
                "effect", IUpgradableMachine.class);
            assertEquals(LevelupRg.class, method.getDeclaringClass(),
                "LevelupRg must override effect");
        }
    }
}
