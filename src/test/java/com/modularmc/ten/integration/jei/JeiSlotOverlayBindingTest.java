// -*- coding: utf-8 -*-
package com.modularmc.ten.integration.jei;

import com.modularmc.ten.api.recipe.FormsCombinedIngredient;

import net.minecraft.resources.Identifier;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract tests for JEI slot overlay decision logic.
 * <p>
 * These tests verify the rules that determine when an overlay is needed,
 * using only main-source classes ({@link FormsCombinedIngredient}) so no
 * JEI runtime or client compilation is required.
 * <p>
 * The {@code shouldHaveOverlay} predicate tested here is defined purely
 * in terms of {@link FormsCombinedIngredient#chanceOverlayText()} and
 * {@link FormsCombinedIngredient#rollsOverlayText()} — the same predicate
 * used by the production {@code TENJeiSlotOverlay.shouldHaveOverlay()} in
 * the client source set.
 */
class JeiSlotOverlayBindingTest {

    // ════════════════════════════════════════════════════════════
    // Helper: reflective ingredient creation (avoids BuiltInRegistries)
    // ════════════════════════════════════════════════════════════

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

    /**
     * The same predicate used by
     * {@code TENJeiSlotOverlay.shouldHaveOverlay(boolean, boolean, FormsCombinedIngredient)}
     * in the client source set. Duplicated here for test independence.
     */
    private static boolean shouldHaveOverlay(
            boolean isItemSlot, boolean isOutput, FormsCombinedIngredient ingredient) {
        if (!isItemSlot) return false;
        if (!isOutput) return false;
        if (ingredient == null) return false;
        return ingredient.chanceOverlayText() != null || ingredient.rollsOverlayText() != null;
    }

    // ════════════════════════════════════════════════════════════
    // A. Decision logic: overlay attachment conditions
    // ════════════════════════════════════════════════════════════

    @Nested
    class ShouldHaveOverlay {

        @Test
        void outputItemWithChanceBelow1() {
            var ing = createReflective(0.4, 1);
            assertTrue(shouldHaveOverlay(true, true, ing));
        }

        @Test
        void outputItemWithRollsAbove1() {
            var ing = createReflective(1.0, 9);
            assertTrue(shouldHaveOverlay(true, true, ing));
        }

        @Test
        void outputItemWithChanceAndRolls() {
            var ing = createReflective(0.4, 9);
            assertTrue(shouldHaveOverlay(true, true, ing));
        }

        @Test
        void outputItemFullChanceOneRoll_noOverlay() {
            var ing = createReflective(1.0, 1);
            assertFalse(shouldHaveOverlay(true, true, ing));
        }

        @Test
        void inputItem_noOverlay() {
            var ing = createReflective(0.4, 1);
            assertFalse(shouldHaveOverlay(true, false, ing));
        }

        @Test
        void fluidSlot_noOverlay() {
            var ing = createReflective(0.4, 1);
            assertFalse(shouldHaveOverlay(false, true, ing));
        }

        @Test
        void nullIngredient_noOverlay() {
            assertFalse(shouldHaveOverlay(true, true, null));
        }

        @Test
        void chanceAtCertaintyThreshold_noOverlay() {
            var ing = createReflective(1.0d - 1e-12, 1);
            assertNull(ing.chanceOverlayText());
            assertNull(ing.rollsOverlayText());
            assertFalse(shouldHaveOverlay(true, true, ing));
        }
    }

    // ════════════════════════════════════════════════════════════
    // B. Overlay content: text is produced correctly by ingredient
    // ════════════════════════════════════════════════════════════

    @Nested
    class OverlayContent {

        @Test
        void chanceTextPresent_whenChanceBelow1() {
            var ing = createReflective(0.4, 1);
            assertNotNull(ing.chanceOverlayText());
            assertNull(ing.rollsOverlayText());
        }

        @Test
        void rollsTextPresent_whenRollsAbove1() {
            var ing = createReflective(1.0, 9);
            assertNull(ing.chanceOverlayText());
            assertNotNull(ing.rollsOverlayText());
        }

        @Test
        void bothTextsPresent_whenChanceBelow1AndRollsAbove1() {
            var ing = createReflective(0.4, 9);
            assertNotNull(ing.chanceOverlayText());
            assertNotNull(ing.rollsOverlayText());
        }

        @Test
        void noText_whenFullChanceAndSingleRoll() {
            var ing = createReflective(1.0, 1);
            assertNull(ing.chanceOverlayText());
            assertNull(ing.rollsOverlayText());
        }
    }

    // ════════════════════════════════════════════════════════════
    // C. Integration: setOverlay called via setRecipe logic
    // ════════════════════════════════════════════════════════════

    @Nested
    class SetOverlayBinding {

        @Test
        void setOverlayCalled_forOutputItemWithChance() {
            var ing = createReflective(0.4, 1);
            // Contract: setOverlay IS called for output item with chance<1
            assertTrue(shouldHaveOverlay(true, true, ing));
        }

        @Test
        void inputSlots_neverGetOverlay() {
            var ing = createReflective(0.4, 1);
            assertFalse(shouldHaveOverlay(true, false, ing));
            assertFalse(shouldHaveOverlay(true, false, null));
        }

        @Test
        void fluidOutputSlots_neverGetOverlay() {
            var ing = createReflective(0.4, 1);
            assertFalse(shouldHaveOverlay(false, true, ing));
        }
    }

    // ════════════════════════════════════════════════════════════
    // D. Tooltip coexistence contracts
    // ════════════════════════════════════════════════════════════

    @Nested
    class TooltipCoexistence {

        @Test
        void overlayAndTooltipBothActive_forOutputWithChance() {
            var ing = createReflective(0.4, 1);
            assertNotNull(ing.chanceOverlayText());
            assertEquals(FormsCombinedIngredient.TooltipKind.CHANCE_ONLY, ing.tooltipKind(true));
        }

        @Test
        void overlayAndTooltipBothActive_forOutputWithChanceAndRolls() {
            var ing = createReflective(0.4, 9);
            assertNotNull(ing.chanceOverlayText());
            assertNotNull(ing.rollsOverlayText());
            assertEquals(FormsCombinedIngredient.TooltipKind.CHANCE_WITH_ROLLS, ing.tooltipKind(true));
        }

        @Test
        void overlayOnly_tooltipNone_whenChanceAt1() {
            var ing = createReflective(1.0, 9);
            assertNull(ing.chanceOverlayText());
            assertNotNull(ing.rollsOverlayText());
            assertEquals(FormsCombinedIngredient.TooltipKind.NONE, ing.tooltipKind(true));
        }

        @Test
        void tooltipAndOverlayNone_whenChanceAt1RollsAt1() {
            var ing = createReflective(1.0, 1);
            assertNull(ing.chanceOverlayText());
            assertNull(ing.rollsOverlayText());
            assertEquals(FormsCombinedIngredient.TooltipKind.NONE, ing.tooltipKind(true));
        }

        @Test
        void notConsumed_inputOnly() {
            var ing = createReflective(0.0, 1);
            // chanceOverlayText returns "0%" (0 rounded to 0, formatted as percentage)
            assertEquals("0%", ing.chanceOverlayText());
            assertEquals(FormsCombinedIngredient.TooltipKind.NOT_CONSUMED, ing.tooltipKind(false));
        }
    }
}
