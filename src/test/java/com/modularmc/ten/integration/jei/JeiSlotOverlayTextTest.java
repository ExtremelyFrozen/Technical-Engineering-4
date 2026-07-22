// -*- coding: utf-8 -*-
package com.modularmc.ten.integration.jei;

import com.modularmc.ten.api.recipe.FormsCombinedIngredient;

import net.minecraft.resources.Identifier;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure unit tests for JEI slot overlay text logic.
 * <p>
 * Tests the two overlay text methods added to {@link FormsCombinedIngredient}:
 * <ul>
 *   <li>{@link FormsCombinedIngredient#chanceOverlayText()} — "40%" / null</li>
 *   <li>{@link FormsCombinedIngredient#rollsOverlayText()} — "R9" / null</li>
 * </ul>
 * <p>
 * These methods are pure functions of ingredient state and do not require
 * any Minecraft client runtime.
 */
class JeiSlotOverlayTextTest {

    // ════════════════════════════════════════════════════════════════
    // Helper: reflective instance creation (avoids BuiltInRegistries)
    // ════════════════════════════════════════════════════════════════

    private static FormsCombinedIngredient createReflective(
            double chance, int rolls) {
        try {
            var ctor = FormsCombinedIngredient.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            var ing = ctor.newInstance();

            setField(ing, "form", "item");
            setField(ing, "type", "static");
            setField(ing, "amountOrCount", 1);
            setField(ing, "chance", chance);
            setField(ing, "rolls", rolls);
            setField(ing, "key", Identifier.parse("minecraft:iron_ingot"));

            return ing;
        } catch (Exception e) {
            throw new RuntimeException("Reflective creation failed", e);
        }
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        var field = FormsCombinedIngredient.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    // ════════════════════════════════════════════════════════════════
    // A. chanceOverlayText — chance formatting
    // ════════════════════════════════════════════════════════════════

    @Nested
    class ChanceOverlayText {

        @Test
        void chance0_4_returns40percent() {
            var ing = createReflective(0.4, 1);
            assertEquals("40%", ing.chanceOverlayText());
        }

        @Test
        void chance1_0_returnsNull() {
            var ing = createReflective(1.0, 1);
            assertNull(ing.chanceOverlayText());
        }

        @Test
        void chanceJustBelowOne_returnsOverlay() {
            // 0.9999999999 is < 1.0 but rounds to 100% — should still show overlay.
            // This differs from chance=1.0 exactly, which returns null.
            // Also below the certainty threshold (1.0 - 1e-12), so overlay is shown.
            var ing = createReflective(0.9999999999, 1);
            assertNotNull(ing.chanceOverlayText());
        }

        @Test
        void chance0_returns0percent() {
            var ing = createReflective(0.0, 1);
            assertEquals("0%", ing.chanceOverlayText());
        }

        @Test
        void chance0_5_returns50percent() {
            var ing = createReflective(0.5, 1);
            assertEquals("50%", ing.chanceOverlayText());
        }

        @Test
        void chance0_99_returns99percent() {
            var ing = createReflective(0.99, 1);
            assertEquals("99%", ing.chanceOverlayText());
        }

        @Test
        void chance0_999_returns100percent_roundingUp() {
            // 0.999 → 99.9 → rounds to 100
            var ing = createReflective(0.999, 1);
            assertEquals("100%", ing.chanceOverlayText());
        }
    }

    // ════════════════════════════════════════════════════════════════
    // B. rollsOverlayText — rolls formatting
    // ════════════════════════════════════════════════════════════════

    @Nested
    class RollsOverlayText {

        @Test
        void rolls9_returnsR9() {
            var ing = createReflective(1.0, 9);
            assertEquals("R9", ing.rollsOverlayText());
        }

        @Test
        void rolls1_returnsNull() {
            var ing = createReflective(1.0, 1);
            assertNull(ing.rollsOverlayText());
        }

        @Test
        void rolls2_returnsR2() {
            var ing = createReflective(1.0, 2);
            assertEquals("R2", ing.rollsOverlayText());
        }

        @Test
        void rolls99_returnsR99() {
            var ing = createReflective(1.0, 99);
            assertEquals("R99", ing.rollsOverlayText());
        }

        @Test
        void rolls100_returnsR100() {
            var ing = createReflective(1.0, 100);
            assertEquals("R100", ing.rollsOverlayText());
        }
    }

    // ════════════════════════════════════════════════════════════════
    // C. Combined — both texts independent
    // ════════════════════════════════════════════════════════════════

    @Nested
    class CombinedOverlay {

        @Test
        void bothVisible_whenChanceBelow1_andRollsAbove1() {
            var ing = createReflective(0.4, 9);
            assertEquals("40%", ing.chanceOverlayText());
            assertEquals("R9", ing.rollsOverlayText());
        }

        @Test
        void chanceOnly_whenRollsIs1() {
            var ing = createReflective(0.4, 1);
            assertEquals("40%", ing.chanceOverlayText());
            assertNull(ing.rollsOverlayText());
        }

        @Test
        void rollsOnly_whenChanceIs1() {
            var ing = createReflective(1.0, 9);
            assertNull(ing.chanceOverlayText());
            assertEquals("R9", ing.rollsOverlayText());
        }

        @Test
        void neitherVisible_whenChanceIs1_andRollsIs1() {
            var ing = createReflective(1.0, 1);
            assertNull(ing.chanceOverlayText());
            assertNull(ing.rollsOverlayText());
        }
    }

    // ════════════════════════════════════════════════════════════════
    // D. Boundary: overlay text must fit in 18x18 slot
    //    (verified through character count, not pixel measurement)
    // ════════════════════════════════════════════════════════════════

    @Nested
    class TextLengthBoundary {

        @Test
        void chanceText_maxLength_4chars() {
            // "100%" is the longest possible (chance < 1 but rounding up)
            var ing = createReflective(0.999, 1);
            String text = ing.chanceOverlayText();
            assertNotNull(text);
            assertTrue(text.length() <= 4,
                    "chance overlay text must be ≤4 chars, got: '" + text + "' (" + text.length() + ")");
        }

        @Test
        void rollsText_maxLength_5chars_forRollsUpTo999() {
            var ing = createReflective(1.0, 999);
            String text = ing.rollsOverlayText();
            assertNotNull(text);
            assertTrue(text.length() <= 5,
                    "rolls overlay text must be ≤5 chars for rolls ≤999, got: '" + text + "' (" + text.length() + ")");
        }

        @Test
        void rollsText_fitsInSlot_forMaxExpected() {
            // Realistic max: rolls=9 gives "R9" (2 chars), rolls=99 gives "R99" (3 chars)
            var ing = createReflective(1.0, 99);
            String text = ing.rollsOverlayText();
            assertNotNull(text);
            assertEquals(3, text.length());
        }
    }

    // ════════════════════════════════════════════════════════════════
    // E. Method contract: declared on FormsCombinedIngredient
    // ════════════════════════════════════════════════════════════════

    @Nested
    class MethodContract {

        @Test
        void chanceOverlayText_methodExists() throws Exception {
            var method = FormsCombinedIngredient.class.getMethod("chanceOverlayText");
            assertEquals(String.class, method.getReturnType());
        }

        @Test
        void rollsOverlayText_methodExists() throws Exception {
            var method = FormsCombinedIngredient.class.getMethod("rollsOverlayText");
            assertEquals(String.class, method.getReturnType());
        }

        @Test
        void tooltipKind_methodExists() throws Exception {
            var method = FormsCombinedIngredient.class.getMethod("tooltipKind", boolean.class);
            assertEquals(FormsCombinedIngredient.TooltipKind.class, method.getReturnType());
        }
    }

    // ════════════════════════════════════════════════════════════════
    // F. Tooltip registration contract (no JEI runtime needed)
    // ════════════════════════════════════════════════════════════════

    @Nested
    class TooltipRegistrationContract {

        @Test
        void outputChanceBelow1WithoutRolls_isChanceOnly() {
            var ing = createReflective(0.4, 1);
            assertEquals(FormsCombinedIngredient.TooltipKind.CHANCE_ONLY, ing.tooltipKind(true));
        }

        @Test
        void outputChanceBelow1WithRollsAbove1_isChanceWithRolls() {
            var ing = createReflective(0.4, 9);
            assertEquals(FormsCombinedIngredient.TooltipKind.CHANCE_WITH_ROLLS, ing.tooltipKind(true));
        }

        @Test
        void outputChanceAt1_isNone() {
            var ing = createReflective(1.0, 1);
            assertEquals(FormsCombinedIngredient.TooltipKind.NONE, ing.tooltipKind(true));
        }

        @Test
        void outputChanceAtCertaintyThreshold_isNone() {
            // At the certainty threshold (1.0 - 1e-12), both overlay and tooltip
            // agree: no display needed. Unified epsilon prevents narrow window
            // where overlay hides but tooltip still shows.
            var ing = createReflective(1.0d - 1e-12, 1);
            assertEquals(FormsCombinedIngredient.TooltipKind.NONE, ing.tooltipKind(true));
            assertNull(ing.chanceOverlayText());
        }

        @Test
        void inputChanceAt0_isNotConsumed() {
            var ing = createReflective(0.0, 1);
            assertEquals(FormsCombinedIngredient.TooltipKind.NOT_CONSUMED, ing.tooltipKind(false));
        }

        @Test
        void inputChanceAbove0_isNone() {
            var ing = createReflective(0.5, 1);
            assertEquals(FormsCombinedIngredient.TooltipKind.NONE, ing.tooltipKind(false));
        }

        @Test
        void outputChanceBelow1WithRolls1_isChanceOnly_notChanceWithRolls() {
            // rolls=1 should never produce CHANCE_WITH_ROLLS even when chance<1
            var ing = createReflective(0.3, 1);
            assertEquals(FormsCombinedIngredient.TooltipKind.CHANCE_ONLY, ing.tooltipKind(true));
        }

        @Test
        void inputChanceNegative_isNotConsumed() {
            // chance <= 0 → NOT_CONSUMED for input slots
            var ing = createReflective(-0.01, 1);
            assertEquals(FormsCombinedIngredient.TooltipKind.NOT_CONSUMED, ing.tooltipKind(false));
        }
    }
}
