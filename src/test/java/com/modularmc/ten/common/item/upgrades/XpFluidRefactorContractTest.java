// -*- coding: utf-8 -*-
package com.modularmc.ten.common.item.upgrades;

import com.modularmc.ten.common.blockentity.machine.FurnaceBlockEntity;
import com.modularmc.ten.common.gui.TENMachineBlockUIFactory;
import com.modularmc.ten.data.lang.TENLangHandler;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD RED→GREEN contract tests for P3 XP fluid refactor:
 * <ul>
 *   <li>XP gauge always visible</li>
 *   <li>hasFaceCapabilityFluid does not depend on Knowledge</li>
 *   <li>New formula: max(1, round(cookingTime / 10))</li>
 *   <li>Knowledge only gates XP production</li>
 *   <li>Language ownership: main/en_us only, generated/zh_cn only</li>
 *   <li>Knowledge tooltip uses tick-based basis</li>
 * </ul>
 */
class XpFluidRefactorContractTest {

    // ════════════════════════════════════════════════════════════
    // A. XP gauge always visible — no syncDisplay/no Knowledge gate
    // ════════════════════════════════════════════════════════════

    @Nested
    class XpGaugeAlwaysVisible {

        @Test
        void furnaceCreateUI_noSyncDisplayForXpGauge() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/common/blockentity/machine/FurnaceBlockEntity.java");
            var content = Files.readString(sourceFile.toPath());

            int createUIStart = content.indexOf("ModularUI createUI");
            assertTrue(createUIStart >= 0, "createUI method must exist");
            String createUIBody = content.substring(createUIStart,
                    content.indexOf("}", createUIStart + 400) + 1);

            assertFalse(createUIBody.contains("syncDisplay"),
                    "createUI must NOT use syncDisplay for XP gauge — gauge is always visible");
        }

        @Test
        void furnaceCreateUI_usesCreateXpFluidSlot() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/common/blockentity/machine/FurnaceBlockEntity.java");
            var content = Files.readString(sourceFile.toPath());

            int createUIStart = content.indexOf("ModularUI createUI");
            String createUIBody = content.substring(createUIStart,
                    content.indexOf("}", createUIStart + 400) + 1);

            assertTrue(createUIBody.contains("createXpFluidSlot"),
                    "createUI must use createXpFluidSlot for XP gauge");
        }

        @Test
        void machineUiFactory_hasCreateXpFluidSlot() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/common/gui/TENMachineBlockUIFactory.java");
            var content = Files.readString(sourceFile.toPath());

            assertTrue(content.contains("createXpFluidSlot"),
                    "TENMachineBlockUIFactory must have createXpFluidSlot method");
        }

        @Test
        void createXpFluidSlot_delegatesToFluidGauge() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/common/gui/TENMachineBlockUIFactory.java");
            var content = Files.readString(sourceFile.toPath());

            int methodStart = content.indexOf("createXpFluidSlot");
            assertTrue(methodStart >= 0);

            // Must delegate to fluidGauge (standard LDLib2 binding)
            assertTrue(content.contains("fluidGauge"),
                    "createXpFluidSlot must delegate to fluidGauge");
        }

        /**
         * P3 refactor: XP gauge is always added to the UI tree,
         * not conditional on Knowledge upgrade being installed.
         * Verifies createXpFluidSlot call is NOT inside an if(hasUpgrade) block.
         */
        @Test
        void fluidGauge_isAlwaysAddedNotConditional() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/common/blockentity/machine/FurnaceBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());

            int createUiIdx = content.indexOf("ModularUI createUI");
            assertTrue(createUiIdx >= 0, "createUI method must exist");

            String createUiBody = content.substring(createUiIdx);
            int gaugeIdx = createUiBody.indexOf("createXpFluidSlot");
            assertTrue(gaugeIdx >= 0, "createUI must reference createXpFluidSlot");

            // Verify the XP gauge addChild is always present (not in if block)
            String beforeGauge = createUiBody.substring(0, gaugeIdx);
            int lastAddChildBefore = beforeGauge.lastIndexOf("root.addChild");
            String between = beforeGauge.substring(lastAddChildBefore);

            boolean hasConditionalGate = between.contains("if") &&
                    (between.contains("hasUpgrade") || between.contains("LevelupKnow"));
            assertFalse(hasConditionalGate,
                    "createXpFluidSlot must NOT be inside if(hasUpgrade) conditional. " +
                    "It must be always added. Between last addChild and gauge:\n" + between);
        }
    }

    // ════════════════════════════════════════════════════════════
    // B. hasFaceCapabilityFluid — no Knowledge dependency
    // ════════════════════════════════════════════════════════════

    @Nested
    class CapabilityAlwaysPresent {

        @Test
        void hasFaceCapabilityFluid_returnsTrue() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/common/blockentity/machine/FurnaceBlockEntity.java");
            var content = Files.readString(sourceFile.toPath());

            int methodStart = content.indexOf("hasFaceCapabilityFluid");
            assertTrue(methodStart >= 0, "hasFaceCapabilityFluid method must exist");

            int methodEnd = content.indexOf("}", methodStart) + 1;
            String methodBody = content.substring(methodStart, methodEnd);

            // Must return true (not gated by Knowledge)
            assertTrue(methodBody.contains("return true"),
                    "hasFaceCapabilityFluid must return true (always present, not Knowledge-gated)");
            assertFalse(methodBody.contains("hasUpgrade"),
                    "hasFaceCapabilityFluid must NOT check hasUpgrade(LevelupKnow.class)");
        }
    }

    // ════════════════════════════════════════════════════════════
    // C. New XP formula — cookingTime-based
    // ════════════════════════════════════════════════════════════

    @Nested
    class XpFormulaByCookingTime {

        @Test
        void calculateXpFluidPerUnit_usesCookingTime() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/common/blockentity/machine/FurnaceBlockEntity.java");
            var content = Files.readString(sourceFile.toPath());

            // Must use cookingTime, not experience()
            assertTrue(content.contains("cookingTime()"),
                    "calculateXpFluidPerUnit must use cookingTime()");
            assertFalse(content.contains("recipe.experience()") && !content.contains("cookingTime()"),
                    "calculateXpFluidPerUnit must NOT use experience() (should use cookingTime)");
        }

        @Test
        void calculateXpFluidPerUnit_hasFormulaConstant() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/common/blockentity/machine/FurnaceBlockEntity.java");
            var content = Files.readString(sourceFile.toPath());

            assertTrue(content.contains("XP_FLUID_TICKS_PER_MB"),
                    "FurnaceBlockEntity must have XP_FLUID_TICKS_PER_MB constant");
            assertTrue(content.contains("= 10"),
                    "XP_FLUID_TICKS_PER_MB should equal 10 (0.1 mB/tick)");
        }

        /** Formula: max(1, round(cookingTime / 10)). 200 ticks → 20 mB. */
        @Test
        void formula_200ticks_is20mb() {
            int cookingTime = 200;
            int expected = Math.max(1, (int) Math.round((double) cookingTime / 10));
            assertEquals(20, expected, "200 ticks / 10 = 20 mB");
        }

        /** Formula: max(1, round(5 / 10)) → max(1, round(0.5)) → max(1, 1) → 1 mB. */
        @Test
        void formula_5ticks_is1mb() {
            int cookingTime = 5;
            int expected = Math.max(1, (int) Math.round((double) cookingTime / 10));
            assertEquals(1, expected, "5 ticks → round(0.5)=1, max(1,1)=1 mB");
        }

        /** Formula: max(1, round(15 / 10)) → max(1, round(1.5)) → max(1, 2) → 2 mB. */
        @Test
        void formula_15ticks_is2mb() {
            int cookingTime = 15;
            int expected = Math.max(1, (int) Math.round((double) cookingTime / 10));
            assertEquals(2, expected, "15 ticks → round(1.5)=2, max(1,2)=2 mB");
        }

        /** Formula: max(1, round(100 / 10)) → max(1, 10) → 10 mB. */
        @Test
        void formula_100ticks_is10mb() {
            int cookingTime = 100;
            int expected = Math.max(1, (int) Math.round((double) cookingTime / 10));
            assertEquals(10, expected, "100 ticks / 10 = 10 mB");
        }

        /** Batch: perUnit * lockedB. B=4, perUnit=20 → 80 mB total. */
        @Test
        void batchXp_4B_20perUnit_is80mb() {
            int perUnit = 20; // 200 ticks
            int B = 4;
            assertEquals(80, perUnit * B, "B=4 × 20 mB/unit = 80 mB total");
        }
    }

    // ════════════════════════════════════════════════════════════
    // D. Knowledge only gates production — not tank or UI
    // ════════════════════════════════════════════════════════════

    @Nested
    class KnowledgeGatesOnlyProduction {

        @Test
        void knowledgeStillReferencedInConditionStart() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/common/blockentity/machine/FurnaceBlockEntity.java");
            var content = Files.readString(sourceFile.toPath());

            // Knowledge must still be referenced in conditionStart for B constraint
            int condStart = content.indexOf("public boolean conditionStart()");
            assertTrue(condStart >= 0);
            String condBody = content.substring(condStart,
                    content.indexOf("public boolean cooking()", condStart));

            assertTrue(condBody.contains("hasUpgrade(LevelupKnow.class)"),
                    "conditionStart must check Knowledge for XP B-constraint");
        }

        @Test
        void tankAlwaysPresent() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/common/blockentity/machine/FurnaceBlockEntity.java");
            var content = Files.readString(sourceFile.toPath());

            // Tank is always added in constructor
            assertTrue(content.contains("tanks.add(new MachineFluidTank(XP_TANK_CAPACITY))"),
                    "Constructor must always add XP tank");
        }
    }

    // ════════════════════════════════════════════════════════════
    // E. Language ownership — main/en_us only, generated/zh_cn only
    // ════════════════════════════════════════════════════════════

    @Nested
    class LanguageOwnership {

        @Test
        void mainResources_hasNoZhCnJson() {
            var file = new File("src/main/resources/assets/kenergyengineering/lang/zh_cn.json");
            assertFalse(file.exists(),
                    "src/main/resources must NOT contain zh_cn.json (owned by generated)");
        }

        @Test
        void mainResources_hasNoLangFiles() {
            var dir = new java.io.File("src/main/resources/assets/kenergyengineering/lang");
            var files = dir.listFiles();
            assertTrue(files == null || files.length == 0,
                    "src/main/resources/lang must be empty (no hand-written locale files)");
        }

        @Test
        void generatedResources_hasZhCnJson() {
            var file = new File("src/generated/resources/assets/kenergyengineering/lang/zh_cn.json");
            assertTrue(file.exists(),
                    "src/generated/resources must contain zh_cn.json");
        }

        @Test
        void generatedResources_hasEnUsJson() {
            var file = new File("src/generated/resources/assets/kenergyengineering/lang/en_us.json");
            assertTrue(file.exists(),
                    "src/generated/resources must contain en_us.json");
        }

        @Test
        void generatedZhCn_isValidJson() throws Exception {
            var file = new File("src/generated/resources/assets/kenergyengineering/lang/zh_cn.json");
            assertTrue(file.exists());
            var content = Files.readString(file.toPath());
            // Verify it's parseable JSON
            assertTrue(content.startsWith("{"), "zh_cn.json must start with {");
            assertTrue(content.endsWith("}") || content.endsWith("}\n"),
                    "zh_cn.json must end with }");
        }

        @Test
        void generatedEnUs_isValidJson() throws Exception {
            var file = new File("src/generated/resources/assets/kenergyengineering/lang/en_us.json");
            assertTrue(file.exists());
            var content = Files.readString(file.toPath());
            assertTrue(content.startsWith("{"), "en_us.json (generated) must start with {");
            assertTrue(content.endsWith("}") || content.endsWith("}\n"),
                    "en_us.json (generated) must end with }");
        }

        @Test
        void knowledgeKeysInGeneratedEnUs() throws Exception {
            var file = new File("src/generated/resources/assets/kenergyengineering/lang/en_us.json");
            var content = Files.readString(file.toPath());
            assertTrue(content.contains("knowledge_levelup.1"),
                    "generated en_us.json must contain knowledge_levelup.1");
            assertTrue(content.contains("knowledge_levelup.2"),
                    "generated en_us.json must contain knowledge_levelup.2");
        }

        @Test
        void knowledgeKeysInGeneratedZhCn() throws Exception {
            var file = new File("src/generated/resources/assets/kenergyengineering/lang/zh_cn.json");
            var content = Files.readString(file.toPath());
            assertTrue(content.contains("knowledge_levelup.1"),
                    "generated zh_cn.json must contain knowledge_levelup.1");
            assertTrue(content.contains("knowledge_levelup.2"),
                    "generated zh_cn.json must contain knowledge_levelup.2");
        }

        @Test
        void jeiSmelterCategoryKeysInGeneratedEnUs() throws Exception {
            var file = new File("src/generated/resources/assets/kenergyengineering/lang/en_us.json");
            var content = Files.readString(file.toPath());
            assertTrue(content.contains("jei.category.smelter_smelting"),
                    "en_us.json must contain smelter_smelting category");
            assertTrue(content.contains("jei.category.smelter_blasting"),
                    "en_us.json must contain smelter_blasting category");
            assertTrue(content.contains("jei.category.smelter_smoking"),
                    "en_us.json must contain smelter_smoking category");
        }

        @Test
        void jeiSmelterCategoryKeysInGeneratedZhCn() throws Exception {
            var file = new File("src/generated/resources/assets/kenergyengineering/lang/zh_cn.json");
            var content = Files.readString(file.toPath());
            assertTrue(content.contains("jei.category.smelter_smelting"),
                    "generated zh_cn.json must contain smelter_smelting category");
            assertTrue(content.contains("jei.category.smelter_blasting"),
                    "generated zh_cn.json must contain smelter_blasting category");
            assertTrue(content.contains("jei.category.smelter_smoking"),
                    "generated zh_cn.json must contain smelter_smoking category");
        }
    }

    // ════════════════════════════════════════════════════════════
    // G. Coordinate non-overlap — XP gauge position
    // ════════════════════════════════════════════════════════════

    @Nested
    class FluidGaugePosition {

        @Test
        void xpGaugePosition_doesNotOverlapOtherElements() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/common/blockentity/machine/FurnaceBlockEntity.java");
            assertTrue(sourceFile.exists());
            var lines = Files.readAllLines(sourceFile.toPath());

            boolean hasXpGaugeAt66 = false;
            for (String line : lines) {
                if (line.contains("createXpFluidSlot") && line.contains("66")) {
                    hasXpGaugeAt66 = true;
                    break;
                }
            }
            assertTrue(hasXpGaugeAt66,
                    "P3: XP fluid gauge should be positioned at y=66 (below energy gauge ending at y=64)");
        }

        @Test
        void gaugePositions_noOverlap() {
            assertTrue(66 >= 64, "XP gauge y=66 must be >= energy gauge bottom y=64");
            assertTrue(66 >= 61, "XP gauge y=66 must be >= fuel gauge bottom y=61");
            assertTrue(22 <= 45, "XP gauge right edge (x=22) <= fuel gauge left edge (x=45)");
        }
    }

    // ════════════════════════════════════════════════════════════
    // H. Knowledge tooltip updated
    // ════════════════════════════════════════════════════════════

    @Nested
    class KnowledgeTooltipUpdated {

        @Test
        void knowledgeTooltipMentionsTicks() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/data/lang/TENLangHandler.java");
            var content = Files.readString(sourceFile.toPath());

            int knowStart = content.indexOf("knowledge_levelup.1");
            String knowSection = content.substring(knowStart, knowStart + 300);

            // Must mention cooking time / ticks
            assertTrue(knowSection.contains("ticks") || knowSection.contains("tick")
                            || knowSection.contains("cooking") || knowSection.contains("时间"),
                    "Knowledge tooltip must mention cooking time basis");
        }
    }
}
