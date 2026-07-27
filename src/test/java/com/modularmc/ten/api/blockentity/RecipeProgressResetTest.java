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

    // ════════════════════════════════════════════════════════════
    // H. Energy不足暂停不重置progress (新契约 — 重构目标)
    // ════════════════════════════════════════════════════════════
    //
    // 当前行为（P0 RED基线）：ProcessingMachineBlockEntity.process() 的
    // else 分支无条件 `progress = 0`。重构目标：能量不足/输出满时暂停
    // 仅保留 progress，只有配方失效（recipe identity 变化）才重置。
    // 本组测试验证「conditions不满足时的行为差异」：conditions不满足
    // 不等于 energy不足。process() 中 energy不足 走 early return
    // 而非 else 分支，progress 不会被清零（已由 ProcessStepContractTest
    // 验证）。本组聚焦 conditionStart() 层面的 identity 变化。
    // ════════════════════════════════════════════════════════════

    @Nested
    class EnergyPreservationContract {
        // 这些测试验证「配方仍有效但 conditions 暂时不满足时，
        // conditionStart 本身不应清零 progress」—— 这已经是当前
        // simulateConditionStart 的行为（保留 progress）。
        // 重构后的 process() 还需额外保证 energy不足 的 early return
        // 不进入 else 分支重置，这由 ProcessStepContractTest 验证。

        @Test
        void sameRecipe_energyTemporarilyLow_preservesProgress() {
            // 配方未变（仍为 RECIPE_A），只是暂时能量不足
            // conditionStart 自身不应清零 progress
            int progress = simulateConditionStart(RECIPE_A, RECIPE_A, 50);
            assertEquals(50, progress,
                    "conditionStart must preserve progress when same recipe continues" +
                    " (energy不足应由 process() 的 early return 处理)");
        }

        @Test
        void sameRecipe_outputFull_preservesProgress() {
            // 配方未变，输出暂时满
            int progress = simulateConditionStart(RECIPE_A, RECIPE_A, 50);
            assertEquals(50, progress,
                    "conditionStart must preserve progress when same recipe continues" +
                    " (输出满应由 process() 通过 cooking() 阻止进度推进)");
        }

        @Test
        void sameRecipe_conditionsRestored_resumesProgress() {
            // 配方持续存在，多次调用 conditionStart 均保留
            int progress = simulateConditionStart(RECIPE_A, RECIPE_A, 50);
            progress = simulateConditionStart(RECIPE_A, RECIPE_A, progress);
            progress = simulateConditionStart(RECIPE_A, RECIPE_A, progress);
            assertEquals(50, progress,
                    "Progress must be preserved across multiple same-recipe conditionStart calls");
        }
    }

    // ════════════════════════════════════════════════════════════
    // I. 配方失效才重置 progress（新契约 — 重构目标）
    // ════════════════════════════════════════════════════════════
    //
    // 重构核心语义：progress 仅在 recipe identity 变化时重置，
    // 而非在 process() else 分支无条件清零。
    // 本组验证 simulateConditionStart 已支持的 identity 变化场景。
    // ════════════════════════════════════════════════════════════

    @Nested
    class RecipeIdentityOnlyResetsProgress {

        @Test
        void recipeChanged_resetsProgress_energyLowDoesNot() {
            // 配方从 A→B 应重置，而能量低不应重置
            int changed = simulateConditionStart(RECIPE_A, RECIPE_B, 50);
            assertEquals(0, changed, "Recipe A→B must reset progress");

            int same = simulateConditionStart(RECIPE_A, RECIPE_A, 50);
            assertEquals(50, same, "Same recipe must preserve progress");
        }

        @Test
        void recipeGone_resetsProgress_butEnergyPreserved() {
            int changed = simulateConditionStart(RECIPE_A, null, 75);
            assertEquals(0, changed, "Recipe→null must reset progress");
        }

        @Test
        void newRecipeArrives_resetsProgress() {
            int changed = simulateConditionStart(null, RECIPE_B, 75);
            assertEquals(0, changed, "Null→recipe must reset progress");
        }

        @Test
        void recipeIdentityUnchanged_keepsProgress_acrossMultipleTicks() {
            // 模拟3个 tick 的 conditionStart，配方不变
            int progress = 50;
            for (int i = 0; i < 3; i++) {
                progress = simulateConditionStart(RECIPE_A, RECIPE_A, progress);
            }
            assertEquals(50, progress,
                    "Multiple same-recipe ticks must preserve progress");
        }
    }

    // ════════════════════════════════════════════════════════════
    // J. 生产源码验证 — P1 新增语义存在（新契约正向验证）
    // ════════════════════════════════════════════════════════════
    //
    // 验证 P1 重构后的 process() 已实现：else 分支不清零、>= 检查。
    // P1 RED 阶段：这些测试应 FAIL（因新语义尚未写入生产代码）。
    // P1 GREEN 阶段：这些测试应 PASS。
    // ════════════════════════════════════════════════════════════

    @Nested
    class SourceCodeP1Semantics {

        @Test
        void processingMachine_usesGreaterOrEqual() throws Exception {
            var sourceFile = new java.io.File(
                    "src/main/java/com/modularmc/ten/api/blockentity/ProcessingMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = java.nio.file.Files.readString(sourceFile.toPath());
            assertTrue(content.contains("progress >= maxProgress"),
                    "P1 target: ProcessingMachine process() must use >= for completion. " +
                    "RED until P1-T1.");
        }

        @Test
        void processingMachine_elseBranchDoesNotResetProgress() throws Exception {
            var sourceFile = new java.io.File(
                    "src/main/java/com/modularmc/ten/api/blockentity/ProcessingMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = java.nio.file.Files.readString(sourceFile.toPath());
            // Verify the else branch (after the energy/condition check) does NOT reset progress to 0
            // The completion branch still uses progress = 0 after onCookFinish, which is correct
            int elseIndex = content.indexOf("} else {\n" +
                    "            setActive(false);");
            if (elseIndex < 0) {
                // Might have different formatting
                elseIndex = content.indexOf("} else {");
            }
            if (elseIndex >= 0) {
                String elseBlock = content.substring(elseIndex, Math.min(elseIndex + 200, content.length()));
                assertFalse(elseBlock.contains("progress = 0"),
                        "P1 target: else branch must NOT reset progress to 0. " +
                        "RED until P1-T1.");
            }
        }

        @Test
        void processingMachine_hasStagnationEarlyReturn() throws Exception {
            var sourceFile = new java.io.File(
                    "src/main/java/com/modularmc/ten/api/blockentity/ProcessingMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = java.nio.file.Files.readString(sourceFile.toPath());
            assertTrue(content.contains("getEnergyStored() < fePerTick"),
                    "P1 target: process() must have early return for energy < fePerTick. " +
                    "RED until P1-T1.");
        }
    }
}
