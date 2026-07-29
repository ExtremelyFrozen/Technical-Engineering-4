// -*- coding: utf-8 -*-
package com.modularmc.ten.integration.jei;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unified contract tests for Smelter JEI integration.
 * <p>
 * Tests check production source/API structure, the nested {@code Recipe}
 * data factory, three category/type/catalyst/lang entries, runtime lookup
 * via {@code IRecipeHolderType}, and the manager identity guard.
 * <p>
 * No fake models or synthetic cache duplication — all assertions target
 * actual source files and production API signatures.
 */
class SmelterJeiContractTest {

    // ═══════════════════════════════════════════════════════════════════
    // 1. Nested Recipe record — data factory
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    class NestedRecipeRecord {

        @Test
        void smelterCategory_hasNestedRecipeRecord() throws Exception {
            var sourceFile = new File(
                    "src/client/java/com/modularmc/ten/integration/jei/SmelterJeiCategory.java");
            assertTrue(sourceFile.exists(), "SmelterJeiCategory must exist");
            var content = Files.readString(sourceFile.toPath());

            assertTrue(content.contains("public record Recipe("),
                    "SmelterJeiCategory must have a public nested Recipe record");
            assertTrue(content.contains("List<ItemStack> inputs"),
                    "Recipe record must preserve all ingredient ItemStacks");
        }

        @Test
        void recipeFactory_usesGetValuesNotItems() throws Exception {
            var sourceFile = new File(
                    "src/client/java/com/modularmc/ten/integration/jei/SmelterJeiCategory.java");
            var content = Files.readString(sourceFile.toPath());

            assertTrue(content.contains("getValues()"),
                    "Recipe.of() must call Ingredient.getValues() to avoid deprecated items()");
            assertFalse(content.contains(".items()"),
                    "Recipe.of() must NOT use deprecated Ingredient.items()");
        }

        @Test
        void recipeFactory_returnsNullOnEmptyOutput() throws Exception {
            var sourceFile = new File(
                    "src/client/java/com/modularmc/ten/integration/jei/SmelterJeiCategory.java");
            var content = Files.readString(sourceFile.toPath());

            assertTrue(content.contains("if (output.isEmpty()) return null"),
                    "Recipe.of() must return null for empty output");
        }

        @Test
        void recipeFactory_copiesOutputStack() throws Exception {
            var sourceFile = new File(
                    "src/client/java/com/modularmc/ten/integration/jei/SmelterJeiCategory.java");
            var content = Files.readString(sourceFile.toPath());

            assertTrue(content.contains("output.copy()"),
                    "Recipe.of() must copy the output ItemStack");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // 2. Three independent smelter RecipeType constants & categories
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    class ThreeSmelterTypes {

        @Test
        void jeiPlugin_hasThreeSmelterTypes() throws Exception {
            var sourceFile = new File(
                    "src/client/java/com/modularmc/ten/integration/jei/TENJeiPlugin.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());

            assertTrue(content.contains("SMELTER_SMELTING"),
                    "TENJeiPlugin must define SMELTER_SMELTING");
            assertTrue(content.contains("SMELTER_BLASTING"),
                    "TENJeiPlugin must define SMELTER_BLASTING");
            assertTrue(content.contains("SMELTER_SMOKING"),
                    "TENJeiPlugin must define SMELTER_SMOKING");
        }

        @Test
        void smelterUids_useCorrectPaths() throws Exception {
            var sourceFile = new File(
                    "src/client/java/com/modularmc/ten/integration/jei/TENJeiPlugin.java");
            var content = Files.readString(sourceFile.toPath());

            assertTrue(content.contains("smelter_smelting"),
                    "UID must contain 'smelter_smelting'");
            assertTrue(content.contains("smelter_blasting"),
                    "UID must contain 'smelter_blasting'");
            assertTrue(content.contains("smelter_smoking"),
                    "UID must contain 'smelter_smoking'");
        }

        @Test
        void jeiPlugin_registersThreeSmelterCategories() throws Exception {
            var sourceFile = new File(
                    "src/client/java/com/modularmc/ten/integration/jei/TENJeiPlugin.java");
            var content = Files.readString(sourceFile.toPath());

            int catStart = content.indexOf("void registerCategories");
            assertTrue(catStart >= 0);
            String catBody = content.substring(catStart,
                    content.indexOf("void registerRecipes", catStart));

            int categoryCount = countOccurrences(catBody, "SmelterJeiCategory(");
            assertEquals(3, categoryCount,
                    "registerCategories must create exactly 3 SmelterJeiCategory instances");
        }

        @Test
        void smelterCategory_usesNestedRecipeType() throws Exception {
            var sourceFile = new File(
                    "src/client/java/com/modularmc/ten/integration/jei/SmelterJeiCategory.java");
            var content = Files.readString(sourceFile.toPath());

            assertTrue(content.contains("IRecipeCategory<SmelterJeiCategory.Recipe>"),
                    "Category must use SmelterJeiCategory.Recipe as its generic type");
            assertTrue(content.contains("RecipeType<Recipe>"),
                    "Category must use RecipeType<Recipe> for its recipe type field");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // 3. Category layout — input/output slots, cooking time
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    class CategoryLayout {

        @Test
        void smelterCategory_hasCorrectLayout() throws Exception {
            var sourceFile = new File(
                    "src/client/java/com/modularmc/ten/integration/jei/SmelterJeiCategory.java");
            assertTrue(sourceFile.exists(), "SmelterJeiCategory must exist");
            var content = Files.readString(sourceFile.toPath());

            assertTrue(content.contains("RecipeIngredientRole.INPUT"),
                    "Category must have INPUT slot");
            assertTrue(content.contains("RecipeIngredientRole.OUTPUT"),
                    "Category must have OUTPUT slot");
            assertTrue(content.contains("cookingTime"),
                    "Category must display cooking time");
        }

        @Test
        void smelterCategory_noDeadChatFormattingImport() throws Exception {
            var sourceFile = new File(
                    "src/client/java/com/modularmc/ten/integration/jei/SmelterJeiCategory.java");
            var content = Files.readString(sourceFile.toPath());

            assertFalse(content.contains("ChatFormatting"),
                    "Category must not import unused ChatFormatting");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // 4. Runtime recipe collection — onRuntimeAvailable + IRecipeHolderType
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    class RuntimeRecipeCollection {

        @Test
        void onRuntimeAvailable_exists() throws Exception {
            var sourceFile = new File(
                    "src/client/java/com/modularmc/ten/integration/jei/TENJeiPlugin.java");
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("void onRuntimeAvailable"),
                    "TENJeiPlugin must have onRuntimeAvailable method");
        }

        @Test
        void onRuntimeAvailable_usesCreateRecipeLookup() throws Exception {
            var sourceFile = new File(
                    "src/client/java/com/modularmc/ten/integration/jei/TENJeiPlugin.java");
            var content = Files.readString(sourceFile.toPath());

            int runtimeIdx = content.indexOf("void onRuntimeAvailable");
            assertTrue(runtimeIdx >= 0);
            String runtimeBody = content.substring(runtimeIdx,
                    content.indexOf("private static", runtimeIdx));

            assertTrue(runtimeBody.contains("createRecipeLookup"),
                    "onRuntimeAvailable must use createRecipeLookup");
            assertTrue(runtimeBody.contains("collectFromBuiltin"),
                    "onRuntimeAvailable must call collectFromBuiltin");
            assertTrue(runtimeBody.contains("recipeManager.addRecipes"),
                    "onRuntimeAvailable must call recipeManager.addRecipes");
        }

        @Test
        void onRuntimeAvailable_addsAllThreeSmelterTypes() throws Exception {
            var sourceFile = new File(
                    "src/client/java/com/modularmc/ten/integration/jei/TENJeiPlugin.java");
            var content = Files.readString(sourceFile.toPath());

            int runtimeIdx = content.indexOf("void onRuntimeAvailable");
            assertTrue(runtimeIdx >= 0);
            String runtimeBody = content.substring(runtimeIdx,
                    content.indexOf("private static", runtimeIdx));

            assertTrue(runtimeBody.contains("SMELTER_SMELTING"),
                    "onRuntimeAvailable must add SMELTER_SMELTING");
            assertTrue(runtimeBody.contains("SMELTER_BLASTING"),
                    "onRuntimeAvailable must add SMELTER_BLASTING");
            assertTrue(runtimeBody.contains("SMELTER_SMOKING"),
                    "onRuntimeAvailable must add SMELTER_SMOKING");
        }

        @Test
        void onRuntimeAvailable_usesIRecipeHolderType() throws Exception {
            var sourceFile = new File(
                    "src/client/java/com/modularmc/ten/integration/jei/TENJeiPlugin.java");
            var content = Files.readString(sourceFile.toPath());

            assertTrue(content.contains("IRecipeHolderType.create("),
                    "Must use IRecipeHolderType.create() instead of deprecated createFromVanilla");
            assertFalse(content.contains("createFromVanilla"),
                    "Must NOT contain deprecated RecipeType.createFromVanilla");
        }

        @Test
        void onRuntimeAvailable_hasManagerIdentityGuard() throws Exception {
            var sourceFile = new File(
                    "src/client/java/com/modularmc/ten/integration/jei/TENJeiPlugin.java");
            var content = Files.readString(sourceFile.toPath());

            assertTrue(content.contains("populatedSmelterManager"),
                    "onRuntimeAvailable must have manager identity guard field");
            assertTrue(content.contains("if (recipeManager == populatedSmelterManager) return"),
                    "Must skip if same manager already populated");
        }

        @Test
        void collectFromBuiltin_exists() throws Exception {
            var sourceFile = new File(
                    "src/client/java/com/modularmc/ten/integration/jei/TENJeiPlugin.java");
            var content = Files.readString(sourceFile.toPath());

            assertTrue(content.contains("collectFromBuiltin"),
                    "TENJeiPlugin must have collectFromBuiltin method");
        }

        @Test
        void collectFromBuiltin_usesIRecipeHolderTypeParam() throws Exception {
            var sourceFile = new File(
                    "src/client/java/com/modularmc/ten/integration/jei/TENJeiPlugin.java");
            var content = Files.readString(sourceFile.toPath());

            assertTrue(content.contains("IRecipeHolderType<T> builtinType"),
                    "collectFromBuiltin must take IRecipeHolderType<T> parameter");
            assertTrue(content.contains("SmelterJeiCategory.Recipe.of"),
                    "collectFromBuiltin must wrap via SmelterJeiCategory.Recipe.of()");
            assertTrue(content.contains(".input()") || content.contains("recipe.input"),
                    "collectFromBuiltin must call recipe.input() for ingredient");
        }

        @Test
        void getSingleplayerServer_notUsedInSmelterRuntime() throws Exception {
            var sourceFile = new File(
                    "src/client/java/com/modularmc/ten/integration/jei/TENJeiPlugin.java");
            var content = Files.readString(sourceFile.toPath());

            // getSingleplayerServer is still present for non-smelter machine
            // categories in registerRecipes, but must NOT appear inside the
            // onRuntimeAvailable smelter path.
            int runtimeIdx = content.indexOf("void onRuntimeAvailable");
            assertTrue(runtimeIdx >= 0);
            String runtimeBody = content.substring(runtimeIdx,
                    content.indexOf("private static", runtimeIdx));
            assertFalse(runtimeBody.contains("getSingleplayerServer"),
                    "onRuntimeAvailable must not use getSingleplayerServer");

            // Also verify it's absent from collectFromBuiltin
            int collectIdx = content.indexOf("collectFromBuiltin");
            assertTrue(collectIdx >= 0);
            String collectBody = content.substring(collectIdx,
                    content.indexOf("private static List<EngineFuelRecipe>", collectIdx));
            assertFalse(collectBody.contains("getSingleplayerServer"),
                    "collectFromBuiltin must not use getSingleplayerServer");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // 5. Catalyst registration — removed from vanilla SMELTING
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    class CatalystRegistration {

        @Test
        void catalystRegistration_noVanillaSmelting() throws Exception {
            var sourceFile = new File(
                    "src/client/java/com/modularmc/ten/integration/jei/TENJeiPlugin.java");
            var content = Files.readString(sourceFile.toPath());

            assertFalse(content.contains("machine_smelter") && content.contains("RecipeTypes.SMELTING"),
                    "machine_smelter must NOT be registered to vanilla RecipeTypes.SMELTING");
        }

        @Test
        void catalystRegistration_hasThreeSmelterTypes() throws Exception {
            var sourceFile = new File(
                    "src/client/java/com/modularmc/ten/integration/jei/TENJeiPlugin.java");
            var content = Files.readString(sourceFile.toPath());

            int catalystStart = content.indexOf("void registerRecipeCatalysts");
            assertTrue(catalystStart >= 0);
            String catalystBody = content.substring(catalystStart,
                    content.indexOf("void registerGuiHandlers", catalystStart));

            assertTrue(catalystBody.contains("SMELTER_SMELTING"),
                    "Catalyst registration must reference SMELTER_SMELTING");
            assertTrue(catalystBody.contains("SMELTER_BLASTING"),
                    "Catalyst registration must reference SMELTER_BLASTING");
            assertTrue(catalystBody.contains("SMELTER_SMOKING"),
                    "Catalyst registration must reference SMELTER_SMOKING");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // 6. Language entries for JEI category keys
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    class JeiCategoryLangEntries {

        @Test
        void enUsJson_hasJeiCategoryEntries() throws Exception {
            var sourceFile = new File(
                    "src/generated/resources/assets/kenergyengineering/lang/en_us.json");
            var content = Files.readString(sourceFile.toPath());

            assertTrue(content.contains("jei.category.smelter_smelting"),
                    "en_us.json must have smelter_smelting category entry");
            assertTrue(content.contains("jei.category.smelter_blasting"),
                    "en_us.json must have smelter_blasting category entry");
            assertTrue(content.contains("jei.category.smelter_smoking"),
                    "en_us.json must have smelter_smoking category entry");
        }

        @Test
        void zhCnJson_hasJeiCategoryEntries() throws Exception {
            var sourceFile = new File(
                    "src/generated/resources/assets/kenergyengineering/lang/zh_cn.json");
            var content = Files.readString(sourceFile.toPath());

            assertTrue(content.contains("jei.category.smelter_smelting"),
                    "zh_cn.json (generated) must have smelter_smelting category entry");
            assertTrue(content.contains("jei.category.smelter_blasting"),
                    "zh_cn.json (generated) must have smelter_blasting category entry");
            assertTrue(content.contains("jei.category.smelter_smoking"),
                    "zh_cn.json (generated) must have smelter_smoking category entry");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════════

    private static int countOccurrences(String text, String target) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(target, idx)) >= 0) {
            count++;
            idx++;
        }
        return count;
    }
}
