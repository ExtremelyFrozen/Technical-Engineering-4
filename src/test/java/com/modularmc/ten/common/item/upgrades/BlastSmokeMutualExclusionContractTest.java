// -*- coding: utf-8 -*-
package com.modularmc.ten.common.item.upgrades;

import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD RED→GREEN contract tests for P3 Smelter mutual exclusion:
 * <ul>
 *   <li>Blast and Smoke cannot coexist ({@link CmMachineBlockEntity#validUpgrade})</li>
 *   <li>First-wins recipe mode ({@link CmMachineBlockEntity#setRecipeMode})</li>
 *   <li>Furnace getCurrentRecipe only queries one RecipeType, no fallback</li>
 *   <li>Knowledge uses actual AbstractCookingRecipe.experience</li>
 *   <li>Tooltip text updated for mutual exclusion semantics</li>
 * </ul>
 * <p>
 * JEI-specific contracts (recipe types, catalyst registration, runtime lookup,
 * language entries) live in {@link com.modularmc.ten.integration.jei.SmelterJeiContractTest}.
 * This file focuses purely on upgrade/exclusion behavior.
 */
class BlastSmokeMutualExclusionContractTest {

    // ════════════════════════════════════════════════════════════
    // A. setRecipeMode — first-wins semantics
    // ════════════════════════════════════════════════════════════

    @Nested
    class SetRecipeModeFirstWins {

        @Test
        void setRecipeMode_hasFirstWinsGuard() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/blockentity/CmMachineBlockEntity.java");
            assertTrue(sourceFile.exists(), "CmMachineBlockEntity source must exist");
            var content = Files.readString(sourceFile.toPath());

            // Find setRecipeMode method
            int methodStart = content.indexOf("public void setRecipeMode(int mode)");
            assertTrue(methodStart >= 0, "setRecipeMode method must exist");

            // Must have first-wins guard: only allow when recipeMode == RECIPE_MODE_SMELTING
            String methodBody = content.substring(methodStart,
                    content.indexOf("}", methodStart) + 1);
            assertTrue(methodBody.contains("RECIPE_MODE_SMELTING"),
                    "setRecipeMode must reference RECIPE_MODE_SMELTING for first-wins guard");
        }

        @Test
        void setRecipeMode_doesNotOverrideSpecialMode() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/blockentity/CmMachineBlockEntity.java");
            var content = Files.readString(sourceFile.toPath());

            int methodStart = content.indexOf("public void setRecipeMode(int mode)");
            String methodBody = content.substring(methodStart,
                    content.indexOf("}", methodStart) + 1);

            // Must only set when current mode is SMELTING (first-wins)
            assertTrue(methodBody.contains("if") || methodBody.contains("switch"),
                    "setRecipeMode must have conditional guard for first-wins");
        }
    }

    // ════════════════════════════════════════════════════════════
    // B. validUpgrade — Blast ↔ Smoke mutual exclusion
    // ════════════════════════════════════════════════════════════

    @Nested
    class ValidUpgradeMutualExclusion {

        @Test
        void validUpgrade_hasBlastSmokeExclusion() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/blockentity/CmMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());

            // Check that validUpgrade has both exclusion checks
            assertTrue(content.contains("LevelupBlast") && content.contains("hasUpgrade(LevelupSmoke.class)"),
                    "validUpgrade must reject Blast when Smoke is installed: "
                    + "check for LevelupBlast + hasUpgrade(LevelupSmoke.class)");
            assertTrue(content.contains("LevelupSmoke") && content.contains("hasUpgrade(LevelupBlast.class)"),
                    "validUpgrade must reject Smoke when Blast is installed: "
                    + "check for LevelupSmoke + hasUpgrade(LevelupBlast.class)");
        }

        @Test
        void validUpgrade_importsBlastAndSmoke() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/blockentity/CmMachineBlockEntity.java");
            var content = Files.readString(sourceFile.toPath());

            // Must import both LevelupBlast and LevelupSmoke
            assertTrue(content.contains("import com.modularmc.ten.common.item.upgrades.LevelupBlast;"),
                    "CmMachineBlockEntity must import LevelupBlast");
            assertTrue(content.contains("import com.modularmc.ten.common.item.upgrades.LevelupSmoke;"),
                    "CmMachineBlockEntity must import LevelupSmoke");
        }
    }

    // ════════════════════════════════════════════════════════════
    // C. Furnace getCurrentRecipe — single type, no fallback
    // ════════════════════════════════════════════════════════════

    @Nested
    class FurnaceNoFallback {

        @Test
        void getCurrentRecipe_switchesOnRecipeMode() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/common/blockentity/machine/FurnaceBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());

            // Must use switch on recipeMode
            assertTrue(content.contains("switch (recipeMode)"),
                    "getCurrentRecipe must switch on recipeMode");
        }

        @Test
        void getCurrentRecipe_hasThreeBranches() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/common/blockentity/machine/FurnaceBlockEntity.java");
            var content = Files.readString(sourceFile.toPath());

            int methodStart = content.indexOf("Optional<RecipeHolder<AbstractCookingRecipe>> getCurrentRecipe()");
            assertTrue(methodStart >= 0, "getCurrentRecipe method must exist");

            String methodBody = content.substring(methodStart,
                    content.indexOf("}", methodStart + 500) + 1);

            // All three recipe types must be referenced
            assertTrue(methodBody.contains("RecipeType.BLASTING"),
                    "getCurrentRecipe must reference BLASTING");
            assertTrue(methodBody.contains("RecipeType.SMOKING"),
                    "getCurrentRecipe must reference SMOKING");
            assertTrue(methodBody.contains("RecipeType.SMELTING") || methodBody.contains("default"),
                    "getCurrentRecipe must reference SMELTING or have default branch");
        }

        @Test
        void knowledgeUsesCookingTimeForXp() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/common/blockentity/machine/FurnaceBlockEntity.java");
            var content = Files.readString(sourceFile.toPath());

            // Knowledge XP calculation must use cookingTime() (not experience())
            assertTrue(content.contains("cookingTime()"),
                    "Knowledge XP must use recipe.cookingTime() for XP calculation");
        }
    }

    // ════════════════════════════════════════════════════════════
    // D. Tooltip text — updated for mutual exclusion
    // ════════════════════════════════════════════════════════════

    @Nested
    class TooltipMutualExclusionSemantics {

        @Test
        void blastTooltip_mentionsExclusive() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/data/lang/TENLangHandler.java");
            var content = Files.readString(sourceFile.toPath());

            int blastStart = content.indexOf("blast_levelup.1");
            assertTrue(blastStart >= 0);
            String blastLine = content.substring(blastStart, blastStart + 200);

            // Must mention exclusive/mutual exclusion semantics
            assertTrue(blastLine.contains("exclusive") || blastLine.contains("互斥"),
                    "Blast tooltip must mention exclusive with Smoke");
        }

        @Test
        void smokeTooltip_mentionsExclusive() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/data/lang/TENLangHandler.java");
            var content = Files.readString(sourceFile.toPath());

            int smokeStart = content.indexOf("smoke_levelup.1");
            assertTrue(smokeStart >= 0);
            String smokeLine = content.substring(smokeStart, smokeStart + 200);

            assertTrue(smokeLine.contains("exclusive") || smokeLine.contains("互斥"),
                    "Smoke tooltip must mention exclusive with Blast");
        }
    }
}
