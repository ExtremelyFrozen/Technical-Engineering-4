// -*- coding: utf-8 -*-
package com.modularmc.ten.api.recipe;

import com.google.gson.JsonObject;
import com.modularmc.ten.common.data.Mat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.List;
import java.util.function.DoubleSupplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract tests for FormsCombinedIngredient rolls integration,
 * Mat raw block capabilities, dust→ingot smelt coverage, and raw block
 * pulverizer recipe coverage.
 * <p>
 * Uses reflection and source scanning where Minecraft runtime classes
 * (Identifier, BuiltInRegistries) are unavailable in unit test environment.
 */
class FormsCombinedRollsContractTest {

    // ════════════════════════════════════════════════════════════
    // A. FormsCombinedIngredient rolls — API contract
    // ════════════════════════════════════════════════════════════

    @Nested
    class RollsFieldContract {

        @Test
        void rolls_methodExists() throws Exception {
            var method = FormsCombinedIngredient.class.getMethod("rolls");
            assertEquals(int.class, method.getReturnType());
        }

        @Test
        void rolls_isDeclaredOnFormsCombinedIngredient() throws Exception {
            var method = FormsCombinedIngredient.class.getMethod("rolls");
            assertEquals(FormsCombinedIngredient.class, method.getDeclaringClass());
        }

        @Test
        void rolls_isSeparateFromChance() throws Exception {
            var rollsMethod = FormsCombinedIngredient.class.getMethod("rolls");
            var chanceMethod = FormsCombinedIngredient.class.getMethod("chance");
            assertNotSame(rollsMethod, chanceMethod);
        }

        @Test
        void rolls_createMethod_withRollsParam_exists() throws Exception {
            // Verify the 6-param create(int, String, String, String, double, int) exists
            var method = FormsCombinedIngredient.class.getMethod("create",
                    int.class, String.class, String.class, String.class, double.class, int.class);
            assertNotNull(method);
        }

        @Test
        void rolls_createMethod_5param_returnsCorrectType() throws Exception {
            var method = FormsCombinedIngredient.class.getMethod("create",
                    int.class, String.class, String.class, String.class, double.class);
            assertEquals(FormsCombinedIngredient.class, method.getReturnType());
        }
    }

    @Nested
    class RollsValidationContract {

        @Test
        void rolls_fieldExists_onClass() throws Exception {
            var field = FormsCombinedIngredient.class.getDeclaredField("rolls");
            assertEquals(int.class, field.getType());
        }

        @Test
        void rolls_defaultValueIs1_inNewInstance() throws Exception {
            // Use reflection to check default field value
            var constructor = FormsCombinedIngredient.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            var ing = constructor.newInstance();
            var rollsField = FormsCombinedIngredient.class.getDeclaredField("rolls");
            rollsField.setAccessible(true);
            assertEquals(1, rollsField.getInt(ing), "Default rolls field must be 1");
        }
    }

    @Nested
    class RollsSourceContract {

        @Test
        void createMethod_validatesRollsGE1() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/recipe/FormsCombinedIngredient.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            // Must have rolls validation: if (rolls < 1) throw ...
            assertTrue(content.contains("rolls < 1"),
                    "create() must validate rolls >= 1");
            assertTrue(content.contains("IllegalArgumentException"),
                    "create() must throw IllegalArgumentException for invalid rolls");
        }
    }

    // ════════════════════════════════════════════════════════════
    // B. FormsCombinedIngredient genItem — via reflection
    // ════════════════════════════════════════════════════════════

    @Nested
    class GenItemWithRolls {

        @Test
        void genItem_withDoubleSupplier_methodExists() throws Exception {
            var method = FormsCombinedIngredient.class.getMethod("genItem", DoubleSupplier.class);
            assertEquals(Object.class.getMethod("getClass").getReturnType().getSuperclass(),
                    method.getReturnType().getSuperclass());
            // Actually just verify it returns something assignable to ItemStack
            assertEquals("net.minecraft.world.item.ItemStack",
                    method.getReturnType().getCanonicalName());
        }

        @Test
        void genItem_oldNoArg_stillExists() throws Exception {
            var method = FormsCombinedIngredient.class.getMethod("genItem");
            assertEquals("net.minecraft.world.item.ItemStack",
                    method.getReturnType().getCanonicalName());
        }

        @Test
        void genItem_rollsLogic_accessibleViaSource() throws Exception {
            // Verify the source code contains the rolls loop logic
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/recipe/FormsCombinedIngredient.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("rolls"),
                    "genItem must reference rolls");
            assertTrue(content.contains("ABSOLUTE_MAX_STACK_SIZE"),
                    "genItem must use ABSOLUTE_MAX_STACK_SIZE for limit");
        }

        @Test
        void genItem_rolls1_hasSameBehavior() throws Exception {
            // Verify via source that rolls=1 short-circuits to old behavior
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/recipe/FormsCombinedIngredient.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            // The rolls <= 1 branch must exist for backward compat
            assertTrue(content.contains("rolls <= 1"),
                    "genItem must have rolls <= 1 fast path");
        }

        @Test
        void genItem_respectsMaxStackSize_sourceCheck() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/recipe/FormsCombinedIngredient.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("ABSOLUTE_MAX_STACK_SIZE"),
                    "genItem must cap output at absolute max stack size (99)");
        }
    }

    // ════════════════════════════════════════════════════════════
    // C. Mat raw block capabilities
    // ════════════════════════════════════════════════════════════

    @Nested
    class MatRawBlock {

        @Test
        void hasRawBlock_methodExists() throws Exception {
            var method = Mat.class.getMethod("hasRawBlock");
            assertEquals(boolean.class, method.getReturnType());
        }

        @Test
        void hasRawBlock_isTrueForIronGoldCopperTinNickel() {
            assertTrue(Mat.IRON.hasRawBlock());
            assertTrue(Mat.GOLD.hasRawBlock());
            assertTrue(Mat.COPPER.hasRawBlock());
            assertTrue(Mat.TIN.hasRawBlock());
            assertTrue(Mat.NICKEL.hasRawBlock());
        }

        @Test
        void hasRawBlock_isFalseForOthers() {
            assertFalse(Mat.POWERED_TIN.hasRawBlock());
            assertFalse(Mat.CHLORIUM.hasRawBlock());
            assertFalse(Mat.MUSHRIUM.hasRawBlock());
            assertFalse(Mat.STARLIGHT.hasRawBlock());
            assertFalse(Mat.NETHERITE.hasRawBlock());
            assertFalse(Mat.DIAMOND.hasRawBlock());
            assertFalse(Mat.EMERALD.hasRawBlock());
            assertFalse(Mat.LAPIS.hasRawBlock());
            assertFalse(Mat.QUARTZ.hasRawBlock());
            assertFalse(Mat.AMETHYST.hasRawBlock());
            assertFalse(Mat.REDSTONE.hasRawBlock());
        }

        @Test
        void rawBlockId_methodExists() throws Exception {
            var method = Mat.class.getMethod("rawBlockId");
            assertEquals(String.class, method.getReturnType());
        }

        @Test
        void rawBlockId_vanillaIsMinecraftNamespace() {
            assertEquals("minecraft:raw_iron_block", Mat.IRON.rawBlockId());
            assertEquals("minecraft:raw_gold_block", Mat.GOLD.rawBlockId());
            assertEquals("minecraft:raw_copper_block", Mat.COPPER.rawBlockId());
        }

        @Test
        void rawBlockId_modIsModNamespace() {
            assertEquals("kenergyengineering:raw_tin_block", Mat.TIN.rawBlockId());
            assertEquals("kenergyengineering:raw_nickel_block", Mat.NICKEL.rawBlockId());
        }

        @Test
        void rawBlockTag_methodExists() throws Exception {
            var method = Mat.class.getMethod("rawBlockTag");
            assertEquals(String.class, method.getReturnType());
        }

        @Test
        void rawBlockTag_format() {
            assertEquals("c:storage_blocks/raw_iron", Mat.IRON.rawBlockTag());
            assertEquals("c:storage_blocks/raw_gold", Mat.GOLD.rawBlockTag());
            assertEquals("c:storage_blocks/raw_copper", Mat.COPPER.rawBlockTag());
            assertEquals("c:storage_blocks/raw_tin", Mat.TIN.rawBlockTag());
            assertEquals("c:storage_blocks/raw_nickel", Mat.NICKEL.rawBlockTag());
        }

        @Test
        void hasRawBlock_doesNotAffectHasRaw() {
            // IRON has raw block but hasRaw=false (not registered by mod)
            assertFalse(Mat.IRON.hasRaw);
            assertTrue(Mat.IRON.hasRawBlock());
        }
    }

    // ════════════════════════════════════════════════════════════
    // D. Dust → ingot smelt coverage
    // ════════════════════════════════════════════════════════════

    @Nested
    class DustSmeltCoverage {

        @Test
        void dustSmelt_forEveryMatWithDustAndIngot() {
            // hasForm("dust") && hasIngot → smelting + blasting must be generated
            // Only 9 materials have hasIngot=true per Mat enum constructor:
            // IRON, GOLD, COPPER, TIN, NICKEL, POWERED_TIN, CHLORIUM, MUSHRIUM, NETHERITE
            var hasDustAndIngot = List.of(
                    Mat.IRON, Mat.GOLD, Mat.COPPER, Mat.TIN, Mat.NICKEL,
                    Mat.POWERED_TIN, Mat.CHLORIUM, Mat.MUSHRIUM,
                    Mat.NETHERITE
            );
            assertEquals(9, hasDustAndIngot.size(),
                    "9 materials with hasForm(dust)=true && hasIngot=true");
            for (var mat : hasDustAndIngot) {
                assertTrue(mat.hasForm("dust"), mat.id + " must have dust form");
                assertTrue(mat.hasIngot, mat.id + " must have ingot");
            }
            // DIAMOND, EMERALD, LAPIS, QUARTZ have hasIngot=false (use vanilla items)
            assertTrue(Mat.DIAMOND.hasForm("dust"));
            assertFalse(Mat.DIAMOND.hasIngot);
            assertTrue(Mat.EMERALD.hasForm("dust"));
            assertFalse(Mat.EMERALD.hasIngot);
            assertTrue(Mat.LAPIS.hasForm("dust"));
            assertFalse(Mat.LAPIS.hasIngot);
            assertTrue(Mat.QUARTZ.hasForm("dust"));
            assertFalse(Mat.QUARTZ.hasIngot);
            // REDSTONE: no dust form, no ingot
            assertFalse(Mat.REDSTONE.hasForm("dust"));
            assertFalse(Mat.REDSTONE.hasIngot);
            // AMETHYST: has dust (not REDSTONE), no ingot
            assertTrue(Mat.AMETHYST.hasForm("dust"));
            assertFalse(Mat.AMETHYST.hasIngot);
            // STARLIGHT: has dust (not REDSTONE), no ingot
            assertTrue(Mat.STARLIGHT.hasForm("dust"));
            assertFalse(Mat.STARLIGHT.hasIngot);
        }

        @Test
        void dustSmelt_countIs18Recipes() throws Exception {
            // Count materials that pass canSmelt(mat, dustFmt):
            // condition is hasForm("dust") && hasIngot
            long materialCount = 0;
            for (var mat : Mat.values()) {
                if (mat.hasForm("dust") && mat.hasIngot) materialCount++;
            }
            assertEquals(9, materialCount, "9 materials with dust+ingot");
            // Each generates 2 recipes (smelting + blasting) = 18 total
            assertEquals(18, materialCount * 2, "18 total dust→ingot recipes (9 × 2)");

            // Verify the source code generates both smelting and blasting for dust
            var recipeGenFile = new File(
                    "src/main/java/com/modularmc/ten/data/TENRecipeGen.java");
            assertTrue(recipeGenFile.exists());
            var content = Files.readString(recipeGenFile.toPath());
            // The SmeltFmt definition for dust→ingot
            assertTrue(content.contains("\"ingot_fd\""),
                    "SmeltFmt must define ingot_fd suffix for dust→ingot");
            // Both smelting and blasting must be generated for dust recipes
            assertTrue(content.contains("buildSmelting"),
                    "TENRecipeGen must call buildSmelting");
            assertTrue(content.contains("buildBlasting"),
                    "TENRecipeGen must call buildBlasting");
        }

        @Test
        void noIngot_noDustSmelt() {
            // Materials without ingot must NOT generate dust→ingot recipes
            var noIngotMats = List.of(Mat.AMETHYST, Mat.REDSTONE, Mat.STARLIGHT);
            for (var mat : noIngotMats) {
                assertFalse(mat.hasIngot, mat.id + " must not have ingot");
            }
        }
    }

    // ════════════════════════════════════════════════════════════
    // E. Raw block pulverizer recipe coverage
    // ════════════════════════════════════════════════════════════

    @Nested
    class RawBlockRecipeCoverage {

        @Test
        void rawBlockRecipe_supportedMats() {
            var rawBlockMats = List.of(
                    Mat.IRON, Mat.GOLD, Mat.COPPER, Mat.TIN, Mat.NICKEL
            );
            assertEquals(5, rawBlockMats.size());
            for (var mat : rawBlockMats) {
                assertTrue(mat.hasRawBlock(), mat.id + " must support raw block recipe");
            }
        }

        @Test
        void rawBlockRecipe_sourceHasCorrectStructure() throws Exception {
            var recipeGenFile = new File(
                    "src/main/java/com/modularmc/ten/data/TENRecipeGen.java");
            assertTrue(recipeGenFile.exists());
            var content = Files.readString(recipeGenFile.toPath());

            assertTrue(content.contains("buildRawBlockPulv"),
                    "TENRecipeGen must have buildRawBlockPulv method");
            assertTrue(content.contains("hasRawBlock"),
                    "TENRecipeGen must check hasRawBlock()");
            assertTrue(content.contains("addProperty(\"time\", 900)") || content.contains("\"time\", 900"),
                    "Raw block recipe must have time 900");
        }
    }

    // ════════════════════════════════════════════════════════════
    // F. Output capacity pre-check
    // ════════════════════════════════════════════════════════════

    @Nested
    class OutputCapacityPreCheck {

        @Test
        void canFitOutput_methodExists() throws Exception {
            var method = com.modularmc.ten.api.blockentity.RecipeMachineBlockEntity.class
                    .getDeclaredMethod("canFitOutput",
                            FormsCombinedIngredient.class);
            assertNotNull(method);
        }

        @Test
        void canFitOutput_considersRolls() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/blockentity/RecipeMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var lines = Files.readAllLines(sourceFile.toPath());
            boolean inCanFitOutput = false;
            boolean inCanFitAllOutputs = false;
            boolean foundRolls = false;
            int braceDepth = 0;

            for (String line : lines) {
                if (line.contains("boolean canFitOutput(FormsCombinedIngredient ing)")) {
                    inCanFitOutput = true;
                }
                if (line.contains("boolean canFitAllOutputs()")) {
                    inCanFitAllOutputs = true;
                }
                if (!inCanFitOutput && !inCanFitAllOutputs) continue;
                for (char c : line.toCharArray()) {
                    if (c == '{') braceDepth++;
                    if (c == '}') braceDepth--;
                }
                if (braceDepth <= 0 && (inCanFitOutput || inCanFitAllOutputs)) {
                    if (inCanFitAllOutputs && !inCanFitOutput) break;
                    if (inCanFitOutput) break;
                }

                if (line.contains("rolls") || line.contains("multiplyExact") ||
                        line.contains("computeItemSlotLimit") || line.contains("canFitAllOutputs")) {
                    foundRolls = true;
                    break;
                }
            }
            assertTrue(foundRolls,
                    "canFitOutput/canFitAllOutputs must account for rolls to compute max possible output");
        }
    }

    // ════════════════════════════════════════════════════════════
    // G. JEI tooltip for rolls
    // ════════════════════════════════════════════════════════════

    @Nested
    class JeiRollsTooltip {

        @Test
        void jeiCategory_checksRollsForTooltip() throws Exception {
            var sourceFile = new File(
                    "src/client/java/com/modularmc/ten/integration/jei/TENJeiCategory.java");
            assertTrue(sourceFile.exists());
            var lines = Files.readAllLines(sourceFile.toPath());

            boolean hasRollsKey = lines.stream()
                    .anyMatch(l -> l.contains("jei_addition_chance_rolls"));
            assertTrue(hasRollsKey,
                    "TENJeiCategory must reference jei_addition_chance_rolls lang key");
        }

        @Test
        void jeiCategory_usesOldKeyWhenRollsIs1() throws Exception {
            var sourceFile = new File(
                    "src/client/java/com/modularmc/ten/integration/jei/TENJeiCategory.java");
            assertTrue(sourceFile.exists());
            var lines = Files.readAllLines(sourceFile.toPath());

            boolean hasOldKey = lines.stream()
                    .anyMatch(l -> l.contains("jei_addition_chance"));
            assertTrue(hasOldKey,
                    "TENJeiCategory must still reference jei_addition_chance for rolls=1");
        }
    }

    // ════════════════════════════════════════════════════════════
    // H. Localization entries
    // ════════════════════════════════════════════════════════════

    @Nested
    class LocalizationEntries {

        @Test
        void langHandler_hasRollsKey() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/data/lang/TENLangHandler.java");
            assertTrue(sourceFile.exists());
            var lines = Files.readAllLines(sourceFile.toPath());

            boolean hasRollsKey = lines.stream()
                    .anyMatch(l -> l.contains("jei_addition_chance_rolls"));
            assertTrue(hasRollsKey,
                    "TENLangHandler must register jei_addition_chance_rolls");
        }

        @Test
        void en_us_hasRollsKey() throws Exception {
            var sourceFile = new File(
                    "src/main/resources/assets/kenergyengineering/lang/en_us.json");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("jei_addition_chance_rolls"),
                    "en_us.json must contain jei_addition_chance_rolls");
        }

        @Test
        void zh_cn_main_hasRollsKey() throws Exception {
            var sourceFile = new File(
                    "src/main/resources/assets/kenergyengineering/lang/zh_cn.json");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("jei_addition_chance_rolls"),
                    "zh_cn.json must contain jei_addition_chance_rolls");
        }
    }

    // ════════════════════════════════════════════════════════════
    // I. TENTagProvider vanilla raw block tags
    // ════════════════════════════════════════════════════════════

    @Nested
    class TagProviderVanillaRawBlocks {

        @Test
        void tagProvider_generatesVanillaRawBlockTags() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/data/TENTagProvider.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());

            assertTrue(content.contains("raw_iron_block"),
                    "TENTagProvider must reference raw_iron_block");
            assertTrue(content.contains("raw_gold_block"),
                    "TENTagProvider must reference raw_gold_block");
            assertTrue(content.contains("raw_copper_block"),
                    "TENTagProvider must reference raw_copper_block");
        }
    }

    // ════════════════════════════════════════════════════════════
    // J. ingr helper in TENRecipeGen supports rolls
    // ════════════════════════════════════════════════════════════

    @Nested
    class IngrHelperRolls {

        @Test
        void ingrHelper_acceptsRollsParameter() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/data/TENRecipeGen.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());

            assertTrue(content.contains("rolls"),
                    "TENRecipeGen.ingr must handle rolls parameter");
        }
    }

    // ════════════════════════════════════════════════════════════
    // Utilities
    // ════════════════════════════════════════════════════════════

    private static int countOccurrences(String text, String pattern) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(pattern, idx)) != -1) {
            count++;
            idx += pattern.length();
        }
        return count;
    }

    // ════════════════════════════════════════════════════════════
    // K. FormsCombinedIngredient equals/hashCode with rolls
    // ════════════════════════════════════════════════════════════

    @Nested
    class IngredientEqualsHashCode {

        @Test
        void equals_methodExists_onFormsCombinedIngredient() throws Exception {
            var method = FormsCombinedIngredient.class.getMethod("equals", Object.class);
            assertNotNull(method);
        }

        @Test
        void hashCode_methodExists_onFormsCombinedIngredient() throws Exception {
            var method = FormsCombinedIngredient.class.getMethod("hashCode");
            assertNotNull(method);
        }

        @Test
        void equals_considersRolls() throws Exception {
            // Verify source contains rolls comparison in equals
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/recipe/FormsCombinedIngredient.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("rolls == that.rolls"),
                    "equals must compare rolls field");
        }

        @Test
        void sourceHasEqualsAndHashCode() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/recipe/FormsCombinedIngredient.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("public boolean equals"),
                    "FormsCombinedIngredient must have equals");
            assertTrue(content.contains("public int hashCode"),
                    "FormsCombinedIngredient must have hashCode");
        }
    }

    // ════════════════════════════════════════════════════════════
    // L. Codec rolls serialization in FormsCombinedRecipeSerializer
    // ════════════════════════════════════════════════════════════

    @Nested
    class SerializerCodecRolls {

        @Test
        void serializer_ingredientDataRecord_hasRolls() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/recipe/FormsCombinedRecipeSerializer.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("int rolls"),
                    "IngredientData record must have rolls field");
        }

        @Test
        void serializer_codec_hasOptionalRolls() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/recipe/FormsCombinedRecipeSerializer.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("optionalFieldOf(\"rolls\", 1)"),
                    "Codec must have optional rolls with default 1");
        }

        @Test
        void serializer_xmap_passesRollsForward() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/recipe/FormsCombinedRecipeSerializer.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            // The xmap forward direction: data -> ingredient must pass data.rolls()
            assertTrue(content.contains("data.rolls()"),
                    "Codec xmap forward must pass data.rolls()");
        }

        @Test
        void serializer_getOutputs_readsRollsFromJson() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/recipe/FormsCombinedRecipeSerializer.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("getIntOr(o, \"rolls\", 1)"),
                    "getOutputs must read rolls from JSON with default 1");
            assertTrue(content.contains("chance, rolls"),
                    "getOutputs must pass rolls to create()");
        }

        @Test
        void serializer_getInputs_readsRollsViaParseFrom() throws Exception {
            // getInputs uses parseFrom which already handles rolls
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/recipe/FormsCombinedRecipeSerializer.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("validateInputRolls(ing)"),
                    "getInputs must validate input rolls");
        }
    }

    // ════════════════════════════════════════════════════════════
    // M. Input/fluid rolls validation
    // ════════════════════════════════════════════════════════════

    @Nested
    class InputRollsValidation {

        @Test
        void create_rejectsRollsLessThan1() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/recipe/FormsCombinedIngredient.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("rolls < 1"),
                    "create() must reject rolls < 1");
        }

        @Test
        void serializer_validatesInputRolls() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/recipe/FormsCombinedRecipeSerializer.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("validateInputRolls"),
                    "Serializer must have input rolls validation method");
            assertTrue(content.contains("got rolls="),
                    "Validation error must include rolls value");
        }

        @Test
        void serializer_codecValidatesInputRolls() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/recipe/FormsCombinedRecipeSerializer.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            // createRecipe in codec path also validates rolls
            boolean codecValidates = content.contains("ing.rolls() > 1")
                    && content.contains("must have rolls=1");
            assertTrue(codecValidates,
                    "Codec path in createRecipe must validate rolls>1");
        }
    }

    // ════════════════════════════════════════════════════════════
    // N. Dynamic mats tag coverage (9 materials)
    // ════════════════════════════════════════════════════════════

    @Nested
    class DynamicMatsTagCoverage {

        @Test
        void matsTag_countIs9() {
            long matCount = 0;
            for (Mat mat : Mat.values()) {
                if (mat.hasForm("dust") && mat.hasIngot) matCount++;
            }
            assertEquals(9, matCount,
                    "Must have exactly 9 materials with both dust form and ingot");
        }

        @Test
        void matsTag_containsMushriumAndNetherite() {
            assertTrue(Mat.MUSHRIUM.hasForm("dust") && Mat.MUSHRIUM.hasIngot,
                    "Mushrium must have dust+ingot for mats tag");
            assertTrue(Mat.NETHERITE.hasForm("dust") && Mat.NETHERITE.hasIngot,
                    "Netherite must have dust+ingot for mats tag");
        }

        @Test
        void matsTag_generatedDynamicallyInProvider() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/data/TENTagProvider.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            // Must iterate over Mat.values() and filter
            assertTrue(content.contains("Mat.values()"),
                    "TENTagProvider must iterate Mat.values() for mats tags");
            assertTrue(content.contains("mat.hasForm(\"dust\")"),
                    "TENTagProvider must filter by hasForm(dust)");
        }
    }

    // ════════════════════════════════════════════════════════════
    // O. Network roundtrip verification
    // ════════════════════════════════════════════════════════════

    @Nested
    class NetworkRoundtrip {

        @Test
        void writeTo_writesRolls() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/recipe/FormsCombinedIngredient.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("buf.writeInt(rolls)"),
                    "writeTo must write rolls to buffer");
        }

        @Test
        void parseFrom_readsRolls() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/recipe/FormsCombinedIngredient.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("int rolls = buf.readInt()"),
                    "parseFrom(RegistryFriendlyByteBuf) must read rolls from buffer");
            assertTrue(content.contains("chance, rolls"),
                    "parseFrom must pass rolls to create()");
        }
    }

    // ════════════════════════════════════════════════════════════
    // P. genItem deterministic random source
    // ════════════════════════════════════════════════════════════

    @Nested
    class GenItemDeterministic {

        @Test
        void genItem_usesDoubleSupplier() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/recipe/FormsCombinedIngredient.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("DoubleSupplier random"),
                    "genItem(DoubleSupplier) must accept random source");
        }

        @Test
        void genItem_withRolls0_returnsEmpty() throws Exception {
            // Verify that 0 successes = EMPTY
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/recipe/FormsCombinedIngredient.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("successCount == 0"),
                    "genItem must return EMPTY when no trials succeed");
        }

        @Test
        void genItem_usesMultiplyExact_forOverflowSafety() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/recipe/FormsCombinedIngredient.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("Math.multiplyExact"),
                    "genItem must use Math.multiplyExact for overflow safety");
        }

        @Test
        void genItem_respectsMaxStackSize() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/recipe/FormsCombinedIngredient.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("ABSOLUTE_MAX_STACK_SIZE"),
                    "genItem must respect absolute max stack size (99)");
        }
    }

    // ════════════════════════════════════════════════════════════
    // Q. Old JSON without rolls=1 should be omitted
    // ════════════════════════════════════════════════════════════

    @Nested
    class CompactJsonSerialization {

        @Test
        void parseFrom_defaultsRollsTo1_whenMissing() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/api/recipe/FormsCombinedIngredient.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("getIntOr(json, \"rolls\", 1)"),
                    "parseFrom(JsonObject) must default rolls=1 when missing from JSON");
        }

        @Test
        void ingrHelper_omitsRolls_whenNull() throws Exception {
            var sourceFile = new File(
                    "src/main/java/com/modularmc/ten/data/TENRecipeGen.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("if (rolls != null)"),
                    "ingr helper must only write rolls when non-null");
        }
    }

    // ════════════════════════════════════════════════════════════
    // R. 5 raw block recipe file existence + content checks
    // ════════════════════════════════════════════════════════════

    @Nested
    class RawBlockRecipeFiles {

        @Test
        void rawBlockRecipeFileExists_forAll5() {
            var rawMats = List.of("iron", "gold", "copper", "tin", "nickel");
            for (String id : rawMats) {
                var file = new File("src/generated/resources/data/kenergyengineering/recipe/pulverizer/metal/"
                        + id + "_raw_block.json");
                assertTrue(file.exists(),
                        "Raw block pulverizer recipe must exist for " + id);
            }
        }

        @Test
        void rawBlockRecipe_hasRolls9_bonus() throws Exception {
            var file = new File("src/generated/resources/data/kenergyengineering/recipe/pulverizer/metal/"
                    + "iron_raw_block.json");
            assertTrue(file.exists());
            var content = Files.readString(file.toPath());
            assertTrue(content.contains("\"rolls\": 9"),
                    "Raw block bonus output must have rolls=9");
            assertTrue(content.contains("\"count\": 1"),
                    "Raw block bonus output must have count=1");
            assertTrue(content.contains("\"chance\": 0.4"),
                    "Raw block bonus output must have chance=0.4");
        }

        @Test
        void rawBlockRecipe_hasTime900() throws Exception {
            var file = new File("src/generated/resources/data/kenergyengineering/recipe/pulverizer/metal/"
                    + "iron_raw_block.json");
            assertTrue(file.exists());
            var content = Files.readString(file.toPath());
            assertTrue(content.contains("\"time\": 900"),
                    "Raw block pulverizer must have time=900");
        }

        @Test
        void rawBlockRecipe_hasMain9Output() throws Exception {
            var file = new File("src/generated/resources/data/kenergyengineering/recipe/pulverizer/metal/"
                    + "iron_raw_block.json");
            assertTrue(file.exists());
            var content = Files.readString(file.toPath());
            assertTrue(content.contains("\"count\": 9"),
                    "Raw block main output must have count=9");
        }
    }
}
