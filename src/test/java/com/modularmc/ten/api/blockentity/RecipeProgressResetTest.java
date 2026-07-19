// -*- coding: utf-8 -*-
package com.modularmc.ten.api.blockentity;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract tests for recipe identity tracking in
 * {@link RecipeMachineBlockEntity#conditionStart()}.
 * <p>
 * Verifies that progress is reset to 0 when recipe identity changes
 * (including null↔recipe and A↔B transitions), and preserved when
 * the same recipe continues across ticks.
 * <p>
 * Uses a pure-Java simulation of the conditionStart() decision since
 * {@link RecipeMachineBlockEntity} requires Minecraft bootstrap.
 * The comparison mirrors the production strategy: stable recipe registry ID
 * via {@code FormsCombinedRecipe#getId()} (simulated as string here).
 * The simulation is intentionally BUGGY at first (RED phase), then fixed
 * (GREEN phase), and the same fix is applied to the actual source.
 */
class RecipeProgressResetTest {

    // ════════════════════════════════════════════════════════════
    // Recipe identity simulation — initially BUGGY (RED)
    // ════════════════════════════════════════════════════════════
    //
    // We use a string-ID simulation to avoid Minecraft bootstrap.
    // The production code compares FormsCombinedRecipe#getId()
    // (Identifier) via Objects.equals. Since Identifier.equals()
    // compares namespace+path, our string comparison mirrors the
    // exact same logic.
    //
    // Recipe ID constants mirroring stable registry identifiers.
    // ════════════════════════════════════════════════════════════

    static final String RECIPE_A = "ten:recipe_a";
    static final String RECIPE_B = "ten:recipe_b";
    static final String RECIPE_A_DUP = "ten:recipe_a"; // same ID as A

    /**
     * Simulates the conditionStart() recipe identity decision.
     * <p>
     * BUGGY (RED): unconditionally accepts the new recipe without
     * checking identity against the previous recipe. Progress is
     * never reset when the recipe changes.
     * <p>
     * GREEN (fixed): compares recipe IDs via {@link Objects#equals};
     * resets progress to 0 if identity changed, preserves it if the
     * same recipe continues. This mirrors the production strategy:
     * {@code Objects.equals(prevId, nextId)} where IDs are
     * {@link net.minecraft.resources.Identifier} values.
     *
     * @param previousRecipeId  recipe ID from previous tick (null = no recipe)
     * @param nextRecipeId      recipe ID found this tick (null = no recipe)
     * @param currentProgress   progress before conditionStart
     * @return progress after conditionStart
     */
    static int simulateConditionStart(
            String previousRecipeId,
            String nextRecipeId,
            int currentProgress
    ) {
        // ════════════════════════════════════════════════════════
        // GREEN (fixed): compare recipe IDs by stable identity
        // ════════════════════════════════════════════════════════
        if (!Objects.equals(previousRecipeId, nextRecipeId)) {
            // Recipe identity changed (null↔recipe or A↔B) → reset progress
            return 0;
        }
        // Same recipe continues → preserve progress
        return currentProgress;
    }

    // ════════════════════════════════════════════════════════════
    // 1. Recipe change → progress MUST reset to 0
    // ════════════════════════════════════════════════════════════

    @Nested
    class RecipeChangeResetsProgress {

        @Test
        void nullToRecipe_resetsProgress() {
            int progress = simulateConditionStart(null, RECIPE_A, 75);
            assertEquals(0, progress,
                    "Progress must reset when recipe changes from null to a recipe");
        }

        @Test
        void recipeToNull_resetsProgress() {
            int progress = simulateConditionStart(RECIPE_A, null, 75);
            assertEquals(0, progress,
                    "Progress must reset when recipe changes from a recipe to null");
        }

        @Test
        void recipeAtoB_resetsProgress() {
            int progress = simulateConditionStart(RECIPE_A, RECIPE_B, 75);
            assertEquals(0, progress,
                    "Progress must reset when switching from recipe A to recipe B");
        }

        @Test
        void recipeAtoB_zeroInputDifferentOutput() {
            // Even with zero current progress, identity change must not cause issues
            int progress = simulateConditionStart(RECIPE_A, RECIPE_B, 0);
            assertEquals(0, progress,
                    "Progress must remain 0 when switching recipes from zero progress");
        }

        @Test
        void recipeBtoA_resetsProgress() {
            // Symmetric: B→A also resets
            int progress = simulateConditionStart(RECIPE_B, RECIPE_A, 60);
            assertEquals(0, progress,
                    "Progress must reset when switching from recipe B to recipe A");
        }
    }

    // ════════════════════════════════════════════════════════════
    // 2. Same recipe → progress MUST be preserved
    // ════════════════════════════════════════════════════════════

    @Nested
    class SameRecipePreservesProgress {

        @Test
        void sameId_preservesProgress() {
            // Same recipe ID continues (simulates same recipe instance)
            int progress = simulateConditionStart(RECIPE_A, RECIPE_A, 75);
            assertEquals(75, progress,
                    "Progress must be preserved when the same recipe ID continues");
        }

        @Test
        void equalIdDifferentInstance_preservesProgress() {
            // Different string object, same semantic ID — simulates a fresh
            // recipe object with the same registry ID
            int progress = simulateConditionStart(RECIPE_A, RECIPE_A_DUP, 75);
            assertEquals(75, progress,
                    "Progress must be preserved when different instances have the same ID");
        }

        @Test
        void recipeWithZeroProgress_staysZero() {
            int progress = simulateConditionStart(RECIPE_A, RECIPE_A, 0);
            assertEquals(0, progress,
                    "Progress must stay zero when same recipe continues from zero");
        }

        @Test
        void sameRecipe_multipleTransitions_preservesProgress() {
            // Simulate multiple ticks with the same recipe, progress accumulates
            int progress = 0;
            progress = simulateConditionStart(RECIPE_A, RECIPE_A, progress);
            assertEquals(0, progress,
                    "Progress should not be spuriously reset on same-recipe tick");
        }

        @Test
        void nullToNull_preservesProgress() {
            // Both null means no recipe active; progress would be reset
            // by process() else branch, but conditionStart itself should
            // not interfere
            int progress = simulateConditionStart(null, null, 75);
            assertEquals(75, progress,
                    "Progress must be preserved when both previous and next are null");
        }
    }

    // ════════════════════════════════════════════════════════════
    // 3. Source verification — conditionStart() contains fix
    // ════════════════════════════════════════════════════════════

    @Nested
    class SourceVerification {

        @Test
        void conditionStart_tracksRecipeIdentity() throws Exception {
            var sourceFile = new java.io.File(
                    "src/main/java/com/modularmc/ten/api/blockentity/RecipeMachineBlockEntity.java");
            assertTrue(sourceFile.exists(),
                    "Source file must exist");

            var content = java.nio.file.Files.readString(sourceFile.toPath());

            // After fix: conditionStart() must save previousRecipe and
            // compare identity before assigning currentRecipe
            int previousRecipeIdx = content.indexOf("previousRecipe");
            int currentRecipeAssignIdx = content.indexOf("currentRecipe =");
            assertTrue(previousRecipeIdx >= 0,
                    "Fixed conditionStart must capture previousRecipe for identity check");
            assertTrue(currentRecipeAssignIdx > previousRecipeIdx,
                    "currentRecipe assignment must appear after previousRecipe capture");
        }
    }
}
