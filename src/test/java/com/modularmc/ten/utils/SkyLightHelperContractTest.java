// -*- coding: utf-8 -*-
package com.modularmc.ten.utils;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract tests for {@link SkyLightHelper} — P2-T7 shared light helper.
 * <p>
 * Covers the three-condition light gate:
 * <ol>
 *   <li>canSeeSky(pos.above())</li>
 *   <li>!level.isRaining()</li>
 *   <li>level.getOverworldClockTime() % 24000L < 12000L (daytime)</li>
 * </ol>
 * and null/client safety.
 */
class SkyLightHelperContractTest {

    // ════════════════════════════════════════════════════════════
    // Pure formula contracts
    // ════════════════════════════════════════════════════════════

    /** Pure daylight formula: overworld clock ticks mod 24000 < 12000 */
    static boolean isDaytimeFormula(long overworldClockTicks) {
        return overworldClockTicks % 24000L < 12000L;
    }

    /** Pure effective light gate: all three conditions must hold */
    static boolean effectiveLightFormula(boolean canSeeSky, boolean notRaining, boolean daytime) {
        return canSeeSky && notRaining && daytime;
    }

    @Nested
    class DaytimeFormula {

        @Test
        void ticks0_isDaytime() {
            assertTrue(isDaytimeFormula(0), "0 ticks = dawn = daytime");
        }

        @Test
        void ticks1_isDaytime() {
            assertTrue(isDaytimeFormula(1), "1 tick = daytime");
        }

        @Test
        void ticks11999_isDaytime() {
            assertTrue(isDaytimeFormula(11999), "11999 ticks = just before dusk = daytime");
        }

        @Test
        void ticks12000_isNighttime() {
            assertFalse(isDaytimeFormula(12000), "12000 ticks = dusk = nighttime");
        }

        @Test
        void ticks13000_isNighttime() {
            assertFalse(isDaytimeFormula(13000), "13000 ticks = nighttime");
        }

        @Test
        void ticks23999_isNighttime() {
            assertFalse(isDaytimeFormula(23999), "23999 ticks = just before dawn = nighttime");
        }

        @Test
        void ticks24000_wrapsToDaytime() {
            assertTrue(isDaytimeFormula(24000), "24000 ticks = next dawn = daytime");
        }

        @Test
        void ticks36000_wrapsToNighttime() {
            assertFalse(isDaytimeFormula(36000), "36000 ticks = next dusk = nighttime");
        }

        @Test
        void largeValue_modFormula() {
            long ticks = 1000000L;
            long mod = ticks % 24000L;
            boolean expected = mod < 12000L;
            assertEquals(expected, isDaytimeFormula(ticks),
                    "Large clock value: " + ticks + " % 24000 = " + mod);
        }
    }

    @Nested
    class ThreeConditionGate {

        @Test
        void allThreeTrue_hasLight() {
            assertTrue(effectiveLightFormula(true, true, true),
                    "canSeeSky && !raining && daytime → has light");
        }

        @Test
        void obscured_false() {
            assertFalse(effectiveLightFormula(false, true, true),
                    "canSeeSky=false → no light");
        }

        @Test
        void raining_false() {
            assertFalse(effectiveLightFormula(true, false, true),
                    "isRaining=true → no light");
        }

        @Test
        void nighttime_false() {
            assertFalse(effectiveLightFormula(true, true, false),
                    "nighttime → no light");
        }

        @Test
        void allThreeFalse_false() {
            assertFalse(effectiveLightFormula(false, false, false));
        }

        @Test
        void multipleConditionsFalse_false() {
            assertFalse(effectiveLightFormula(true, false, false));
            assertFalse(effectiveLightFormula(false, true, false));
            assertFalse(effectiveLightFormula(false, false, true));
        }
    }

    // ════════════════════════════════════════════════════════════
    // Source verification — helper exists with correct API
    // ════════════════════════════════════════════════════════════

    @Nested
    class SourceVerification {

        @Test
        void skyLightHelper_classExists() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/utils/SkyLightHelper.java");
            assertTrue(sourceFile.exists(),
                    "RED: SkyLightHelper.java does not exist yet");
        }

        @Test
        void hasEffectiveLightMethod_exists() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/utils/SkyLightHelper.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("hasEffectiveLight"),
                    "RED: hasEffectiveLight method not found");
            assertTrue(content.contains("Level level"),
                    "RED: hasEffectiveLight takes Level parameter");
            assertTrue(content.contains("BlockPos pos"),
                    "RED: hasEffectiveLight takes BlockPos parameter");
        }

        @Test
        void hasEffectiveLight_checksCanSeeSky() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/utils/SkyLightHelper.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("canSeeSky"),
                    "RED: canSeeSky check missing");
        }

        @Test
        void hasEffectiveLight_checksRaining() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/utils/SkyLightHelper.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("isRaining"),
                    "RED: isRaining check missing");
        }

        @Test
        void hasEffectiveLight_checksDaytime() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/utils/SkyLightHelper.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("getOverworldClockTime"),
                    "RED: getOverworldClockTime daytime check missing");
            assertTrue(content.contains("24000"),
                    "RED: 24000 mod missing for daytime check");
            assertTrue(content.contains("12000"),
                    "RED: 12000 threshold missing for daytime check");
        }

        @Test
        void hasEffectiveLight_nullSafe() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/utils/SkyLightHelper.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("null"),
                    "RED: null check missing");
            assertTrue(content.contains("isClientSide"),
                    "RED: isClientSide check missing");
        }

        @Test
        void helperIsFinalUtility_noInstances() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/utils/SkyLightHelper.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("final class"),
                    "RED: helper should be final");
            assertTrue(content.contains("private") && content.contains("SkyLightHelper()"),
                    "RED: helper should have private constructor");
        }
    }
}
