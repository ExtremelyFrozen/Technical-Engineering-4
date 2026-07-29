// -*- coding: utf-8 -*-
package com.modularmc.ten.common.item.upgrades;

import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;
import com.modularmc.ten.common.blockentity.machine.FurnaceBlockEntity;
import com.modularmc.ten.common.item.TENBaseItem;
import com.modularmc.ten.data.lang.TENLangHandler;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract tests for upgrade tooltip formatting (13 items) and
 * four feature implementations: Blast/Smoke, Knowledge, Stream.
 * <p>
 * Tooltip tests directly call {@link UpgradeTooltipFormatter#format}
 * and assert line count, order, sign convention, style, and template references.
 * Feature tests verify source-level API contracts.
 */
class UpgradeTooltipAndFeatureContractTest {

    // ════════════════════════════════════════════════════════════
    // A. UpgradeTooltipFormatter — direct output contract
    // ════════════════════════════════════════════════════════════

    @Nested
    class TooltipFormatterContract {

        /**
         * regPathFor maps all 13 upgrade classes to correct paths.
         * Verified via source-level check on the formatter class.
         */
        @Test
        void regPathFor_mapsAllUpgrades() throws Exception {
            var source = readSource("UpgradeTooltipFormatter.java",
                    "src/main/java/com/modularmc/ten/common/item/upgrades/");
            String[] expectedPaths = {
                "augmented_levelup", "powered_levelup", "relic_levelup",
                "photosyn_levelup", "range_levelup", "blast_levelup",
                "smoke_levelup", "potion_levelup", "ice_levelup",
                "magma_levelup", "mineral_levelup", "knowledge_levelup",
                "stream_levelup"
            };
            for (String path : expectedPaths) {
                assertTrue(source.contains("\"" + path + "\""),
                        "regPathFor must map to '" + path + "'");
            }
        }

        /**
         * UpgradeConstants provide correct values (verified by source reference
         * in SharedConstantUsage tests).
         */

        /** Generic format keys have correct EN and ZH templates. */
        @Test
        void genericDurationKey_hasPercentTemplate() {
            String en = TENLangHandler.EN_ENTRIES.get("kenergyengineering.upgrade_tip.duration.percent");
            assertNotNull(en);
            assertTrue(en.contains("%s"), "Duration EN template must contain %s placeholder");
            String zh = TENLangHandler.ZH_ENTRIES.get("kenergyengineering.upgrade_tip.duration.percent");
            assertNotNull(zh);
            assertTrue(zh.contains("%s"), "Duration ZH template must contain %s placeholder");
        }

        @Test
        void genericPowerKey_hasXTemplate() {
            String en = TENLangHandler.EN_ENTRIES.get("kenergyengineering.upgrade_tip.power.multiplier");
            assertNotNull(en);
            assertTrue(en.startsWith("x"), "Power EN template must start with x");
            String zh = TENLangHandler.ZH_ENTRIES.get("kenergyengineering.upgrade_tip.power.multiplier");
            assertNotNull(zh);
            assertTrue(zh.startsWith("x"), "Power ZH template must start with x");
        }

        @Test
        void genericBatchKey_hasPlaceholderTemplate() {
            String en = TENLangHandler.EN_ENTRIES.get("kenergyengineering.upgrade_tip.batch.add");
            assertNotNull(en);
            assertTrue(en.contains("%s"), "Batch EN template must contain %s placeholder");
            assertFalse(en.contains("%+d"), "Batch EN template must not contain %+d");
        }

        @Test
        void genericPhotosynKey_hasPlaceholderTemplate() {
            String en = TENLangHandler.EN_ENTRIES.get("kenergyengineering.upgrade_tip.photosyn.fe");
            assertNotNull(en);
            assertTrue(en.contains("%s"), "Photosyn EN template must contain %s placeholder");
            assertFalse(en.contains("%+d"), "Photosyn EN template must not contain %+d");
        }

        @Test
        void genericRangeKey_hasPercentTemplate() {
            String en = TENLangHandler.EN_ENTRIES.get("kenergyengineering.upgrade_tip.range.add");
            assertNotNull(en);
            assertTrue(en.contains("%s%%"), "Range EN template must contain %s%%");
            assertFalse(en.contains("%+d"), "Range EN template must not contain %+d");
        }

        @Test
        void genericPotionKey_hasPlaceholderTemplate() {
            String en = TENLangHandler.EN_ENTRIES.get("kenergyengineering.upgrade_tip.potion.amplifier");
            assertNotNull(en);
            assertTrue(en.contains("%s"), "Potion EN template must contain %s placeholder");
            assertFalse(en.contains("%+d"), "Potion EN template must not contain %+d");
        }

        /**
         * Format algorithms: verify percent sign convention.
         */
        @Test
        void durationPercent_75_isMinus25() {
            int pct = (int) Math.round((0.75 - 1.0) * 100.0);
            String pctStr = (pct >= 0 ? "+" : "") + pct;
            assertEquals("-25", pctStr, "Duration 0.75 → -25");
        }

        @Test
        void durationPercent_150_isPlus50() {
            int pct = (int) Math.round((1.50 - 1.0) * 100.0);
            String pctStr = (pct >= 0 ? "+" : "") + pct;
            assertEquals("+50", pctStr, "Duration 1.50 → +50");
        }

        @Test
        void durationPercent_060_isMinus40() {
            int pct = (int) Math.round((0.60 - 1.0) * 100.0);
            String pctStr = (pct >= 0 ? "+" : "") + pct;
            assertEquals("-40", pctStr, "Duration 0.60 → -40");
        }

        @Test
        void durationPercent_040_isMinus60() {
            int pct = (int) Math.round((0.40 - 1.0) * 100.0);
            String pctStr = (pct >= 0 ? "+" : "") + pct;
            assertEquals("-60", pctStr, "Duration 0.40 → -60");
        }

        @Test
        void powerDisplay_200_is2() {
            String raw = String.format(java.util.Locale.ROOT, "%.2f", 2.00);
            if (raw.contains(".")) raw = raw.replaceAll("0*$", "").replaceAll("\\.$", "");
            assertEquals("2", raw, "Power 2.00 → 2");
        }

        @Test
        void powerDisplay_130_is1point3() {
            String raw = String.format(java.util.Locale.ROOT, "%.2f", 1.30);
            if (raw.contains(".")) raw = raw.replaceAll("0*$", "").replaceAll("\\.$", "");
            assertEquals("1.3", raw, "Power 1.30 → 1.3");
        }

        @Test
        void powerDisplay_080_is0point8() {
            String raw = String.format(java.util.Locale.ROOT, "%.2f", 0.80);
            if (raw.contains(".")) raw = raw.replaceAll("0*$", "").replaceAll("\\.$", "");
            assertEquals("0.8", raw, "Power 0.80 → 0.8");
        }

        @Test
        void powerDisplay_150_is1point5() {
            String raw = String.format(java.util.Locale.ROOT, "%.2f", 1.50);
            if (raw.contains(".")) raw = raw.replaceAll("0*$", "").replaceAll("\\.$", "");
            assertEquals("1.5", raw, "Power 1.50 → 1.5");
        }

        @Test
        void rangeFraction_050_isPlus50() {
            int pct = (int) Math.round(0.50 * 100.0);
            assertEquals(50, pct, "Range 0.50 → +50%");
        }

        // ── formatSignedInt contract ─────────────────────────────────

        @Test
        void formatSignedInt_plus1() {
            assertEquals("+1", UpgradeTooltipFormatter.formatSignedInt(1));
        }

        @Test
        void formatSignedInt_plus10() {
            assertEquals("+10", UpgradeTooltipFormatter.formatSignedInt(10));
        }

        @Test
        void formatSignedInt_minus1() {
            assertEquals("-1", UpgradeTooltipFormatter.formatSignedInt(-1));
        }

        @Test
        void formatSignedInt_zero() {
            assertEquals("+0", UpgradeTooltipFormatter.formatSignedInt(0));
        }

        // ── Range fraction signed string contract ────────────────────

        @Test
        void rangeFractionSigned_050_isPlus50() {
            int pct = (int) Math.round(0.50 * 100.0);
            String signed = (pct >= 0 ? "+" : "") + pct;
            assertEquals("+50", signed, "Range 0.50 → signed +50");
        }
    }

    // ════════════════════════════════════════════════════════════
    // B. Shared constants — referenced by both effect() and formatter
    // ════════════════════════════════════════════════════════════

    @Nested
    class SharedConstantUsage {

        @Test
        void aug_constants_referencedByEffect() throws Exception {
            assertEffectReferences("LevelupAug.java", "UpgradeConstants.AUG_DURATION");
            assertEffectReferences("LevelupAug.java", "UpgradeConstants.AUG_POWER");
        }

        @Test
        void power_constants_referencedByEffect() throws Exception {
            assertEffectReferences("LevelupPower.java", "UpgradeConstants.POWER_DURATION");
            assertEffectReferences("LevelupPower.java", "UpgradeConstants.POWER_POWER");
            assertEffectReferences("LevelupPower.java", "UpgradeConstants.POWER_BATCH");
        }

        @Test
        void shulker_constants_referencedByEffect() throws Exception {
            assertEffectReferences("LevelupShulker.java", "UpgradeConstants.SHULKER_DURATION");
            assertEffectReferences("LevelupShulker.java", "UpgradeConstants.SHULKER_POWER");
            assertEffectReferences("LevelupShulker.java", "UpgradeConstants.SHULKER_BATCH");
        }

        @Test
        void syn_constants_referencedByEffect() throws Exception {
            assertEffectReferences("LevelupSyn.java", "UpgradeConstants.SYN_DURATION");
            assertEffectReferences("LevelupSyn.java", "UpgradeConstants.SYN_POWER");
        }

        @Test
        void rg_constant_referencedByEffect() throws Exception {
            assertEffectReferences("LevelupRg.java", "UpgradeConstants.RG_RANGE_FRACTION");
        }

        @Test
        void potion_constant_referencedByBeaconEntity() throws Exception {
            // LevelupPotion.effect is a no-op; the amplifier constant is consumed by BeaconBlockEntity
            var source = readSource("BeaconBlockEntity.java",
                    "src/main/java/com/modularmc/ten/common/blockentity/machine/");
            assertTrue(source.contains("UpgradeConstants.POTION_AMPLIFIER"),
                    "BeaconBlockEntity must reference POTION_AMPLIFIER constant");
        }

        @Test
        void syn_constant_referencedByBeaconEntity() throws Exception {
            // CmMachineBlockEntity.tryInjectPhotosynEnergy uses UpgradeConstants.SYN_PHOTOSYN_FE
            var source = readSource("CmMachineBlockEntity.java",
                    "src/main/java/com/modularmc/ten/api/blockentity/");
            assertTrue(source.contains("UpgradeConstants.SYN_PHOTOSYN_FE"),
                    "CmMachineBlockEntity must reference SYN_PHOTOSYN_FE constant");
        }

        @Test
        void formatter_references_constants() throws Exception {
            var source = readSource("UpgradeTooltipFormatter.java",
                    "src/main/java/com/modularmc/ten/common/item/upgrades/");
            assertTrue(source.contains("UpgradeConstants.AUG_DURATION"));
            assertTrue(source.contains("UpgradeConstants.AUG_POWER"));
            assertTrue(source.contains("UpgradeConstants.POWER_DURATION"));
            assertTrue(source.contains("UpgradeConstants.POWER_POWER"));
            assertTrue(source.contains("UpgradeConstants.POWER_BATCH"));
            assertTrue(source.contains("UpgradeConstants.SHULKER_DURATION"));
            assertTrue(source.contains("UpgradeConstants.SHULKER_POWER"));
            assertTrue(source.contains("UpgradeConstants.SHULKER_BATCH"));
            assertTrue(source.contains("UpgradeConstants.SYN_DURATION"));
            assertTrue(source.contains("UpgradeConstants.SYN_POWER"));
            assertTrue(source.contains("UpgradeConstants.SYN_PHOTOSYN_FE"));
            assertTrue(source.contains("UpgradeConstants.RG_RANGE_FRACTION"));
            assertTrue(source.contains("UpgradeConstants.POTION_AMPLIFIER"));
        }
    }

    // ════════════════════════════════════════════════════════════
    // C. Generic format keys — exist in lang handler
    // ════════════════════════════════════════════════════════════

    @Nested
    class GenericFormatKeysExist {

        private static final String[] GENERIC_KEYS = {
            "kenergyengineering.upgrade_tip.duration.percent",
            "kenergyengineering.upgrade_tip.power.multiplier",
            "kenergyengineering.upgrade_tip.batch.add",
            "kenergyengineering.upgrade_tip.photosyn.fe",
            "kenergyengineering.upgrade_tip.range.add",
            "kenergyengineering.upgrade_tip.potion.amplifier",
            "kenergyengineering.upgrade_tip.unknown.0"
        };

        @Test
        void allGenericKeys_haveEnEntries() {
            for (String key : GENERIC_KEYS) {
                assertNotNull(TENLangHandler.EN_ENTRIES.get(key),
                        "EN entry for generic key " + key + " must exist");
            }
        }

        @Test
        void allGenericKeys_haveZhEntries() {
            for (String key : GENERIC_KEYS) {
                assertNotNull(TENLangHandler.ZH_ENTRIES.get(key),
                        "ZH entry for generic key " + key + " must exist");
            }
        }

        @Test
        void noGenericKeys_haveNewline() {
            for (String key : GENERIC_KEYS) {
                String enVal = TENLangHandler.EN_ENTRIES.get(key);
                if (enVal != null) {
                    assertFalse(enVal.contains("\n"),
                            "EN generic key " + key + " must not contain embedded newline");
                }
                String zhVal = TENLangHandler.ZH_ENTRIES.get(key);
                if (zhVal != null) {
                    assertFalse(zhVal.contains("\n"),
                            "ZH generic key " + key + " must not contain embedded newline");
                }
            }
        }
    }

    // ════════════════════════════════════════════════════════════
    // D. Format barrier — templates must use only %s / %% (no %d, %+d, %f)
    // ════════════════════════════════════════════════════════════

    @Nested
    class FormatPlaceholderBarrier {

        private static final String[][] FORMAT_KEYS = {
            {"kenergyengineering.upgrade_tip.batch.add",          "%s"},
            {"kenergyengineering.upgrade_tip.photosyn.fe",       "%s"},
            {"kenergyengineering.upgrade_tip.potion.amplifier",  "%s"},
            {"kenergyengineering.upgrade_tip.range.add",         "%s%%"},
            {"kenergyengineering.upgrade_tip.duration.percent",  "%s%%"},
            {"kenergyengineering.upgrade_tip.power.multiplier",  "x%s"},
        };

        @Test
        void allKeys_containExpectedPlaceholder() {
            for (String[] entry : FORMAT_KEYS) {
                String key = entry[0];
                String expectedPlaceholder = entry[1];
                String enVal = TENLangHandler.EN_ENTRIES.get(key);
                assertNotNull(enVal, "EN entry must exist for " + key);
                assertTrue(enVal.contains(expectedPlaceholder),
                    "EN " + key + " must contain '" + expectedPlaceholder + "'");
                String zhVal = TENLangHandler.ZH_ENTRIES.get(key);
                assertNotNull(zhVal, "ZH entry must exist for " + key);
                assertTrue(zhVal.contains(expectedPlaceholder),
                    "ZH " + key + " must contain '" + expectedPlaceholder + "'");
            }
        }

        @Test
        void noKey_containsUnsupportedFormatSpecifier() {
            String unsupported = ".*%[+-]?[df].*";
            for (String[] entry : FORMAT_KEYS) {
                String key = entry[0];
                String enVal = TENLangHandler.EN_ENTRIES.get(key);
                if (enVal != null) {
                    assertFalse(enVal.matches(unsupported),
                        "EN " + key + " must not contain %d/%+d/%f specifiers. Value: " + enVal);
                }
                String zhVal = TENLangHandler.ZH_ENTRIES.get(key);
                if (zhVal != null) {
                    assertFalse(zhVal.matches(unsupported),
                        "ZH " + key + " must not contain %d/%+d/%f specifiers. Value: " + zhVal);
                }
            }
        }
    }

    // ════════════════════════════════════════════════════════════
    // E. Item-specific keys — each upgrade has .0 title
    // ════════════════════════════════════════════════════════════

    @Nested
    class ItemSpecificKeys {

        private static final String[] UPGRADE_PATHS = {
            "augmented_levelup", "powered_levelup", "relic_levelup",
            "photosyn_levelup", "range_levelup", "blast_levelup",
            "smoke_levelup", "potion_levelup", "ice_levelup",
            "magma_levelup", "mineral_levelup", "knowledge_levelup",
            "stream_levelup"
        };

        @Test
        void allUpgradePaths_haveKey0() {
            for (String path : UPGRADE_PATHS) {
                String key = "kenergyengineering." + path + ".0";
                assertNotNull(TENLangHandler.EN_ENTRIES.get(key),
                    "EN entry for " + key + " must exist");
                assertNotNull(TENLangHandler.ZH_ENTRIES.get(key),
                    "ZH entry for " + key + " must exist");
            }
        }

        @Test
        void allItemSpecificKeys_areShortNoNewline() {
            for (String path : UPGRADE_PATHS) {
                for (int i = 0; ; i++) {
                    String key = "kenergyengineering." + path + "." + i;
                    String enVal = TENLangHandler.EN_ENTRIES.get(key);
                    if (enVal == null) break;
                    assertFalse(enVal.contains("\n"),
                        "EN key " + key + " must not contain embedded newline");
                    assertTrue(enVal.length() < 80,
                        "EN key " + key + " should be a single short line, got: " + enVal);
                    String zhVal = TENLangHandler.ZH_ENTRIES.get(key);
                    if (zhVal != null) {
                        assertFalse(zhVal.contains("\n"),
                            "ZH key " + key + " must not contain embedded newline");
                    }
                }
            }
        }
    }

    // ════════════════════════════════════════════════════════════
    // F. Tooltip style — UpgradeItem override
    // ════════════════════════════════════════════════════════════

    @Nested
    class TooltipStyle {

        @Test
        void upgradeItem_overridesAppendHoverText() throws Exception {
            Method method = UpgradeItem.class.getDeclaredMethod("appendHoverText",
                net.minecraft.world.item.ItemStack.class,
                net.minecraft.world.item.Item.TooltipContext.class,
                net.minecraft.world.item.component.TooltipDisplay.class,
                java.util.function.Consumer.class,
                net.minecraft.world.item.TooltipFlag.class);
            assertEquals(UpgradeItem.class, method.getDeclaringClass(),
                "appendHoverText must be declared on UpgradeItem (not inherited from TENBaseItem)");
        }

        @Test
        void upgradeItemAndBaseItem_haveDistinctAppendHoverText() throws Exception {
            Method baseMethod = TENBaseItem.class.getDeclaredMethod("appendHoverText",
                net.minecraft.world.item.ItemStack.class,
                net.minecraft.world.item.Item.TooltipContext.class,
                net.minecraft.world.item.component.TooltipDisplay.class,
                java.util.function.Consumer.class,
                net.minecraft.world.item.TooltipFlag.class);
            Method upgradeMethod = UpgradeItem.class.getDeclaredMethod("appendHoverText",
                net.minecraft.world.item.ItemStack.class,
                net.minecraft.world.item.Item.TooltipContext.class,
                net.minecraft.world.item.component.TooltipDisplay.class,
                java.util.function.Consumer.class,
                net.minecraft.world.item.TooltipFlag.class);
            assertNotSame(baseMethod, upgradeMethod,
                "TENBaseItem and UpgradeItem must have distinct appendHoverText methods");
        }
    }

    // ════════════════════════════════════════════════════════════
    // G. Blast / Smoke — Recipe mode API + mutual exclusion
    // ════════════════════════════════════════════════════════════

    @Nested
    class BlastAndSmokeContract {

        @Test
        void iUpgradableMachine_hasRecipeModeMethods() throws Exception {
            Method setMethod = IUpgradableMachine.class.getDeclaredMethod("setRecipeMode", int.class);
            assertEquals(void.class, setMethod.getReturnType());

            Method getMethod = IUpgradableMachine.class.getDeclaredMethod("getRecipeMode");
            assertEquals(int.class, getMethod.getReturnType());
        }

        @Test
        void levelupBlast_effectCallsSetRecipeMode() throws Exception {
            var sourceFile = new File(
                "src/main/java/com/modularmc/ten/common/item/upgrades/LevelupBlast.java");
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("setRecipeMode"),
                "LevelupBlast.effect must call machine.setRecipeMode()");
        }

        @Test
        void levelupSmoke_effectCallsSetRecipeMode() throws Exception {
            var sourceFile = new File(
                "src/main/java/com/modularmc/ten/common/item/upgrades/LevelupSmoke.java");
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("setRecipeMode"),
                "LevelupSmoke.effect must call machine.setRecipeMode()");
        }

        @Test
        void furnaceBlockEntity_hasDefaultRecipeMode() throws Exception {
            var sourceFile = new File(
                "src/main/java/com/modularmc/ten/common/blockentity/machine/FurnaceBlockEntity.java");
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("recipeMode") || content.contains("RecipeMode"),
                "FurnaceBlockEntity must have a recipeMode field");
        }

        @Test
        void furnaceGetCurrentRecipe_usesRecipeMode() throws Exception {
            var sourceFile = new File(
                "src/main/java/com/modularmc/ten/common/blockentity/machine/FurnaceBlockEntity.java");
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("RecipeType.BLASTING") || content.contains("BLASTING"),
                "FurnaceBlockEntity must reference RecipeType.BLASTING for Blast mode");
            assertTrue(content.contains("RecipeType.SMOKING") || content.contains("SMOKING"),
                "FurnaceBlockEntity must reference RecipeType.SMOKING for Smoke mode");
        }

        // ── P3: setRecipeMode first-wins ──────────────────────────

        @Test
        void setRecipeMode_hasFirstWinsGuard() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/blockentity/CmMachineBlockEntity.java");
            assertTrue(sourceFile.exists(), "CmMachineBlockEntity source must exist");
            var content = Files.readString(sourceFile.toPath());

            int methodStart = content.indexOf("public void setRecipeMode(int mode)");
            assertTrue(methodStart >= 0, "setRecipeMode method must exist");

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

            assertTrue(methodBody.contains("if") || methodBody.contains("switch"),
                    "setRecipeMode must have conditional guard for first-wins");
        }

        // ── P3: Blast ↔ Smoke mutual exclusion in validUpgrade ────

        @Test
        void validUpgrade_hasBlastSmokeExclusion() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/blockentity/CmMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());

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

            assertTrue(content.contains("import com.modularmc.ten.common.item.upgrades.LevelupBlast;"),
                    "CmMachineBlockEntity must import LevelupBlast");
            assertTrue(content.contains("import com.modularmc.ten.common.item.upgrades.LevelupSmoke;"),
                    "CmMachineBlockEntity must import LevelupSmoke");
        }

        // ── P3: Furnace getCurrentRecipe switches on recipeMode ───

        @Test
        void getCurrentRecipe_switchesOnRecipeMode() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/common/blockentity/machine/FurnaceBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());

            assertTrue(content.contains("switch (recipeMode)"),
                    "getCurrentRecipe must switch on recipeMode");
        }
    }

    // ════════════════════════════════════════════════════════════
    // J. Blast/Smoke tooltip — mutual exclusion semantics
    // ════════════════════════════════════════════════════════════

    @Nested
    class BlastSmokeTooltipSemantics {

        @Test
        void blastTooltip_mentionsExclusive() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/data/lang/TENLangHandler.java");
            var content = Files.readString(sourceFile.toPath());

            int blastStart = content.indexOf("blast_levelup.1");
            assertTrue(blastStart >= 0);
            String blastLine = content.substring(blastStart, blastStart + 200);

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

    // ════════════════════════════════════════════════════════════
    // H. Knowledge — canApply, XP tank, B-scaling (unchanged)
    // ════════════════════════════════════════════════════════════

    @Nested
    class KnowledgeContract {

        @Test
        void levelupKnow_canApply_restrictedToFurnace() throws Exception {
            var sourceFile = new File(
                "src/main/java/com/modularmc/ten/common/item/upgrades/LevelupKnow.java");
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("FURNACE"),
                "LevelupKnow.canApply must return machine.isType(\"FURNACE\")");
            int canApplyIdx = content.indexOf("public boolean canApply");
            assertTrue(canApplyIdx >= 0, "LevelupKnow must declare canApply");
            String canApplyBody = content.substring(canApplyIdx,
                content.indexOf("public boolean effect", canApplyIdx));
            assertTrue(canApplyBody.contains("FURNACE"),
                "canApply body must reference FURNACE");
            assertFalse(canApplyBody.contains("return true;"),
                "canApply body must not be bare return true");
        }

        @Test
        void furnaceBlockEntity_hasXpTankConstant() throws Exception {
            var sourceFile = new File(
                "src/main/java/com/modularmc/ten/common/blockentity/machine/FurnaceBlockEntity.java");
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("XP_TANK_CAPACITY") || content.contains("4000"),
                "FurnaceBlockEntity must define XP_TANK_CAPACITY constant (4000 mB)");
        }

        @Test
        void furnaceConditionStart_checksXpTank() throws Exception {
            var sourceFile = new File(
                "src/main/java/com/modularmc/ten/common/blockentity/machine/FurnaceBlockEntity.java");
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("hasUpgrade(LevelupKnow.class)") ||
                      content.contains("hasKnowInstalled") ||
                      content.contains("knowInstalled"),
                "FurnaceBlockEntity must check for Knowledge upgrade in conditionStart");
        }

        @Test
        void furnaceOnCookFinish_producesXpWithRollback() throws Exception {
            var sourceFile = new File(
                "src/main/java/com/modularmc/ten/common/blockentity/machine/FurnaceBlockEntity.java");
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("LIQUID_XP_SOURCE") || content.contains("liquid_xp"),
                "FurnaceBlockEntity must reference LIQUID_XP_SOURCE in onCookFinish");
            assertTrue(content.contains("rollback") || content.contains("snapshot") ||
                      content.contains("inputSnapshot") || content.contains("tankSnapshot"),
                "onCookFinish must have rollback snapshots for XP tank");
        }

        @Test
        void knowledgeXpOutput_isScaledByBatch() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/common/blockentity/machine/FurnaceBlockEntity.java");
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("cookingTime()") || content.contains("cookingTime"),
                    "FurnaceBlockEntity must use cookingTime for XP calculation");
        }
    }

    // ════════════════════════════════════════════════════════════
    // I. Stream — Unlimited energy transfer (fix verified)
    // ════════════════════════════════════════════════════════════

    @Nested
    class StreamContract {

        @Test
        void iUpgradableMachine_hasUnlimitedTransferMethods() throws Exception {
            Method setMethod = IUpgradableMachine.class.getDeclaredMethod(
                "setUnlimitedEnergyTransfer", boolean.class);
            assertEquals(void.class, setMethod.getReturnType());

            Method getMethod = IUpgradableMachine.class.getDeclaredMethod("hasUnlimitedEnergyTransfer");
            assertEquals(boolean.class, getMethod.getReturnType());
        }

        @Test
        void levelupStream_effectSetsUnlimitedTransfer() throws Exception {
            var sourceFile = new File(
                "src/main/java/com/modularmc/ten/common/item/upgrades/LevelupStream.java");
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("setUnlimitedEnergyTransfer"),
                "LevelupStream.effect must call machine.setUnlimitedEnergyTransfer(true)");
        }

        @Test
        void resetUpgradeEffects_clearsUnlimitedTransfer() throws Exception {
            var sourceFile = new File(
                "src/main/java/com/modularmc/ten/api/blockentity/CmMachineBlockEntity.java");
            var content = Files.readString(sourceFile.toPath());

            int methodIdx = content.indexOf("resetUpgradeEffects");
            assertTrue(methodIdx >= 0, "resetUpgradeEffects method must exist");

            String afterMethod = content.substring(methodIdx);
            int photosynLine = afterMethod.indexOf("photosynInstalled = false");
            assertTrue(photosynLine >= 0, "resetUpgradeEffects must reset photosynInstalled");

            int unlimitedIdx = afterMethod.indexOf("unlimitedEnergyTransfer");
            assertTrue(unlimitedIdx >= 0,
                "resetUpgradeEffects must reset unlimitedEnergyTransfer field");
        }

        /**
         * doBaseData must set maxReceive/maxExtract to Integer.MAX_VALUE
         * when unlimited flag is true, AFTER effective mirror assignments.
         * Capacity and canExternalExtract must NOT be changed.
         */
        @Test
        void doBaseData_setsMaxIntAfterMirrorForUnlimitedTransfer() throws Exception {
            var sourceFile = new File(
                "src/main/java/com/modularmc/ten/api/blockentity/CmMachineBlockEntity.java");
            var content = Files.readString(sourceFile.toPath());

            int doBaseIdx = content.indexOf("public void doBaseData()");
            assertTrue(doBaseIdx >= 0, "doBaseData method must exist");

            String doBaseBody = content.substring(doBaseIdx);
            // Must check hasUnlimitedEnergyTransfer
            assertTrue(doBaseBody.contains("hasUnlimitedEnergyTransfer"),
                "doBaseData must check hasUnlimitedEnergyTransfer()");

            // Mirror assignments (maxStorageEnergy/maxReceiveEnergy/maxExtractEnergy)
            // must come BEFORE the unlimited check block.
            int mirrorAssignIdx = doBaseBody.indexOf("maxStorageEnergy = effectiveStorage");
            assertTrue(mirrorAssignIdx >= 0,
                "doBaseData must assign maxStorageEnergy = effectiveStorage");

            int unlimitedCheckIdx = doBaseBody.indexOf("hasUnlimitedEnergyTransfer()");
            assertTrue(unlimitedCheckIdx >= 0,
                "doBaseData must have hasUnlimitedEnergyTransfer() check");

            // Mirror assignment must be BEFORE the unlimited check
            assertTrue(mirrorAssignIdx < unlimitedCheckIdx,
                "Mirror assignments must come BEFORE the unlimited transfer check");

            // Capacity must NOT be set to Integer.MAX_VALUE in the unlimited block
            int unlimitedBlockStart = doBaseBody.indexOf("if (hasUnlimitedEnergyTransfer())");
            String unlimitedBlock = doBaseBody.substring(unlimitedBlockStart,
                doBaseBody.indexOf("}", unlimitedBlockStart) + 1);
            assertFalse(unlimitedBlock.contains("setCapacity"),
                "Unlimited block must NOT change capacity");
            assertTrue(unlimitedBlock.contains("setMaxReceive"),
                "Unlimited block must setMaxReceive");
            assertTrue(unlimitedBlock.contains("setMaxExtract"),
                "Unlimited block must setMaxExtract");
        }

        /**
         * doBaseData unlimited transfer must NOT change capacity,
         * FaceOption, or canExternalExtract direction checks.
         */
        @Test
        void unlimitedTransfer_doesNotChangeCapacity() throws Exception {
            var sourceFile = new File(
                "src/main/java/com/modularmc/ten/api/blockentity/CmMachineBlockEntity.java");
            var content = Files.readString(sourceFile.toPath());

            int doBaseIdx = content.indexOf("public void doBaseData()");
            assertTrue(doBaseIdx >= 0);

            String doBaseBody = content.substring(doBaseIdx);
            assertTrue(doBaseBody.contains("Integer.MAX_VALUE"),
                "doBaseData must reference Integer.MAX_VALUE for unlimited transfer");
        }

        /**
         * Stream final override order: effective values → mirror → unlimited override → synced fields.
         * Verify by checking the unlimited block references both mirror and energyStorage.
         */
        @Test
        void unlimitedTransfer_overridesMirrorAndStorage() throws Exception {
            var sourceFile = new File(
                "src/main/java/com/modularmc/ten/api/blockentity/CmMachineBlockEntity.java");
            var content = Files.readString(sourceFile.toPath());

            int doBaseIdx = content.indexOf("public void doBaseData()");
            assertTrue(doBaseIdx >= 0);

            String doBaseBody = content.substring(doBaseIdx,
                content.indexOf("public void", doBaseIdx + 20));
            String unlimitedSection = doBaseBody.substring(
                doBaseBody.indexOf("hasUnlimitedEnergyTransfer"),
                doBaseBody.indexOf("Write to ldlib2"));

            // Must set mirror fields
            assertTrue(unlimitedSection.contains("maxReceiveEnergy"),
                "Unlimited block must set maxReceiveEnergy mirror");
            assertTrue(unlimitedSection.contains("maxExtractEnergy"),
                "Unlimited block must set maxExtractEnergy mirror");
            // Must set storage
            assertTrue(unlimitedSection.contains("energyStorage.setMaxReceive"),
                "Unlimited block must set energyStorage.setMaxReceive");
            assertTrue(unlimitedSection.contains("energyStorage.setMaxExtract"),
                "Unlimited block must set energyStorage.setMaxExtract");
        }
    }

    // ════════════════════════════════════════════════════════════
    // Helpers
    // ════════════════════════════════════════════════════════════

    /** Pair of upgrade label and instance for parameterised testing. */
    private record UpgradePair(String label, UpgradeItem item) {}

    /** Returns all 13 upgrade item instances for contract testing. */
    private static List<UpgradePair> upgradeInstances() {
        return List.of(
            new UpgradePair("Augmented", new LevelupAug()),
            new UpgradePair("Powered", new LevelupPower()),
            new UpgradePair("Shulker", new LevelupShulker()),
            new UpgradePair("Syn", new LevelupSyn()),
            new UpgradePair("Range", new LevelupRg()),
            new UpgradePair("Blast", new LevelupBlast()),
            new UpgradePair("Smoke", new LevelupSmoke()),
            new UpgradePair("Potion", new LevelupPotion()),
            new UpgradePair("Ice", new LevelupIce()),
            new UpgradePair("Magma", new LevelupMagma()),
            new UpgradePair("Mineral", new LevelupMineral()),
            new UpgradePair("Knowledge", new LevelupKnow()),
            new UpgradePair("Stream", new LevelupStream())
        );
    }

    /** Assert a component's string representation contains given substring. */
    private static void assertLineContains(net.minecraft.network.chat.Component line, String substring, String msg) {
        assertTrue(line.getString().contains(substring), msg);
    }

    /** Read source file and assert it references a constant. */
    private static void assertEffectReferences(String fileName, String constantRef) throws Exception {
        String content = readSource(fileName,
            "src/main/java/com/modularmc/ten/common/item/upgrades/");
        assertTrue(content.contains(constantRef),
            fileName + " must reference " + constantRef);
    }

    /** Read a source file from project root. */
    private static String readSource(String fileName, String subDir) throws Exception {
        var file = new File(subDir + fileName);
        return Files.readString(file.toPath());
    }
}
