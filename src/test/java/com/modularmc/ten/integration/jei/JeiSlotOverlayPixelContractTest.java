// -*- coding: utf-8 -*-
package com.modularmc.ten.integration.jei;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pixel contract test for {@link TENJeiSlotOverlay}.
 * <p>
 * Reads the client source file directly (the test source set does not include
 * the client compilation output) and validates the rendering contract:
 * <ul>
 *   <li>{@code CHANCE_TEXT_Y_OFFSET = -1} — chance text at slot.y - 2</li>
 *   <li>{@code ROLLS_TEXT_Y_OFFSET = 11} — rolls text at slot.y + 10</li>
 *   <li>{@code TEXT_SCALE = 0.75f} — 75% scale for both texts</li>
 *   <li>{@code draw()} uses {@code pushMatrix()/translate()/scale()/popMatrix()}
 *       to avoid global state leakage</li>
 * </ul>
 * Old values (0, 9, 1.0f/no-scale) MUST fail these assertions.
 */
class JeiSlotOverlayPixelContractTest {

    // ── Expected contract ────────────────────────────────────────────
    private static final int EXPECTED_CHANCE_Y_OFFSET = -1;
    private static final int EXPECTED_ROLLS_Y_OFFSET = 11;
    private static final float EXPECTED_TEXT_SCALE = 0.75f;

    // ── Source file location ─────────────────────────────────────────
    // Relative to project root (Gradle test working directory).
    private static final Path SOURCE_PATH = Paths.get(
            "src", "client", "java", "com", "modularmc", "ten",
            "integration", "jei", "TENJeiSlotOverlay.java");

    private static String readSource() {
        try {
            return Files.readString(SOURCE_PATH);
        } catch (IOException e) {
            throw new AssertionError(
                    "Cannot read source file at " + SOURCE_PATH.toAbsolutePath()
                            + " (working dir: " + System.getProperty("user.dir") + ")", e);
        }
    }

    private static int extractIntConstant(String source, String name) {
        Pattern p = Pattern.compile(
                "public\\s+static\\s+final\\s+int\\s+" + name + "\\s*=\\s*(-?\\d+)\\s*;");
        Matcher m = p.matcher(source);
        assertTrue(m.find(),
                "Constant '" + name + "' not found in " + SOURCE_PATH);
        return Integer.parseInt(m.group(1));
    }

    private static float extractFloatConstant(String source, String name) {
        Pattern p = Pattern.compile(
                "public\\s+static\\s+final\\s+float\\s+" + name + "\\s*=\\s*(-?\\d+\\.?\\d*[fF]?)\\s*;");
        Matcher m = p.matcher(source);
        assertTrue(m.find(),
                "Constant '" + name + "' not found in " + SOURCE_PATH);
        String val = m.group(1).replace("f", "").replace("F", "");
        return Float.parseFloat(val);
    }

    // ═══════════════════════════════════════════════════════════════════
    // A. Constant contract — old values 0 / 9 / 1.0f MUST fail
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    class ConstantContract {

        @Test
        void chanceTextYOffset_isNegativeOne() {
            String source = readSource();
            int value = extractIntConstant(source, "CHANCE_TEXT_Y_OFFSET");
            assertEquals(EXPECTED_CHANCE_Y_OFFSET, value,
                    "CHANCE_TEXT_Y_OFFSET must be -1 (chance at slot.y - 2); "
                            + "old value 0 would fail this assertion");
        }

        @Test
        void rollsTextYOffset_isTen() {
            String source = readSource();
            int value = extractIntConstant(source, "ROLLS_TEXT_Y_OFFSET");
            assertEquals(EXPECTED_ROLLS_Y_OFFSET, value,
                    "ROLLS_TEXT_Y_OFFSET must be 11 (rolls at slot.y + 10); "
                            + "old value 10 would fail this assertion");
        }

        @Test
        void textScale_isZeroPointSevenFive() {
            String source = readSource();
            float value = extractFloatConstant(source, "TEXT_SCALE");
            assertEquals(EXPECTED_TEXT_SCALE, value, 0.0001f,
                    "TEXT_SCALE must be 0.75f (75% scaling); "
                            + "old value 1.0f or missing scale would fail this assertion");
        }

        @Test
        void spacingBetweenTexts_is12px() {
            // chance at yOffset - 1, rolls at yOffset + 11 → 12px apart
            String source = readSource();
            int chanceY = extractIntConstant(source, "CHANCE_TEXT_Y_OFFSET");
            int rollsY = extractIntConstant(source, "ROLLS_TEXT_Y_OFFSET");
            assertEquals(12, rollsY - chanceY,
                    "Rolls Y offset minus Chance Y offset must be 12px");
        }

        @Test
        void oldZeroValue_wouldFail() {
            String source = readSource();
            int value = extractIntConstant(source, "CHANCE_TEXT_Y_OFFSET");
            assertNotEquals(0, value,
                    "Old chance offset 0 must fail this test");
        }

        @Test
        void oldNineValue_wouldFail() {
            String source = readSource();
            int value = extractIntConstant(source, "ROLLS_TEXT_Y_OFFSET");
            assertNotEquals(9, value,
                    "Old rolls offset 9 must fail this test");
        }

        @Test
        void oldNoScale_wouldFail() {
            String source = readSource();
            float value = extractFloatConstant(source, "TEXT_SCALE");
            assertNotEquals(1.0f, value, 0.0001f,
                    "Scale of 1.0f (no scaling) must fail this test");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // B. Matrix contract — draw() uses push/pop/translate/scale
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    class MatrixContract {

        @Test
        void drawMethodUsesPushMatrix() {
            String source = readSource();
            // Check draw() calls pushMatrix — must use the right pose API
            assertTrue(source.contains("pushMatrix()"),
                    "draw() must call pushMatrix() for matrix isolation");
        }

        @Test
        void drawMethodUsesPopMatrix() {
            String source = readSource();
            assertTrue(source.contains("popMatrix()"),
                    "draw() must call popMatrix() to restore matrix state");
        }

        @Test
        void drawMethodUsesScale() {
            String source = readSource();
            assertTrue(source.contains(".scale("),
                    "draw() must call scale() for 75% text scaling");
        }

        @Test
        void drawMethodUsesTranslate() {
            String source = readSource();
            // Must translate to (xOffset, yOffset + Y_OFFSET) before scaling
            assertTrue(source.contains(".translate("),
                    "draw() must call translate() to position text origin");
        }

        @Test
        void drawUsesPoseApi_notGuiGraphicsDirect() {
            // Scale/translate must be on pose(), not directly on guiGraphics
            String source = readSource();
            assertTrue(source.contains("guiGraphics.pose()."),
                    "Matrix operations must use guiGraphics.pose() API");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // C. Anchor contract — X/Y_OFFSET and setOverlay unchanged
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    class AnchorContract {

        @Test
        void xOffset_unchanged_negativeOne() {
            String source = readSource();
            int value = extractIntConstant(source, "X_OFFSET");
            assertEquals(-1, value,
                    "X_OFFSET must remain -1");
        }

        @Test
        void yOffset_unchanged_negativeOne() {
            String source = readSource();
            int value = extractIntConstant(source, "Y_OFFSET");
            assertEquals(-1, value,
                    "Y_OFFSET must remain -1");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // D. Tooltip coexistence — tooltip registration unchanged
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    class TooltipCoexistenceContract {

        @Test
        void chanceTextColor_white() {
            String source = readSource();
            // 0xFFFFFFFF must still be used for text color
            assertTrue(source.contains("0xFFFFFFFF"),
                    "Text color must remain white (0xFFFFFFFF)");
        }

        @Test
        void textShadow_enabled() {
            String source = readSource();
            // The 'true' shadow parameter must be present
            assertTrue(source.contains(", true"),
                    "Text shadow must remain enabled (true)");
        }
    }
}
