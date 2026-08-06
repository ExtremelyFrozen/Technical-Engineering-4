// -*- coding: utf-8 -*-
package com.modularmc.ten.integration.jei;

import org.junit.jupiter.api.DisplayName;
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
    // 4. Runtime recipe collection
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

            assertTrue(content.contains("createRecipeLookup"),
                    "Plugin must use createRecipeLookup (in injectSmelterRecipes)");
        }

        @Test
        void onRuntimeAvailable_addsAllThreeSmelterTypes() throws Exception {
            var sourceFile = new File(
                    "src/client/java/com/modularmc/ten/integration/jei/TENJeiPlugin.java");
            var content = Files.readString(sourceFile.toPath());

            int injectIdx = content.indexOf("injectSmelterRecipes");
            assertTrue(injectIdx >= 0);
            String injectBody = content.substring(injectIdx,
                    content.indexOf("private static <T extends AbstractCookingRecipe>", injectIdx));

            assertTrue(injectBody.contains("SMELTER_SMELTING"),
                    "injectSmelterRecipes must add SMELTER_SMELTING");
            assertTrue(injectBody.contains("SMELTER_BLASTING"),
                    "injectSmelterRecipes must add SMELTER_BLASTING");
            assertTrue(injectBody.contains("SMELTER_SMOKING"),
                    "injectSmelterRecipes must add SMELTER_SMOKING");
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

            assertTrue(content.contains("smelterPopulatedManager"),
                    "Must have smelterPopulatedManager guard field");
            assertTrue(content.contains("smelterPopulatedManager != recipeManager"),
                    "Must guard smelter with smelterPopulatedManager identity check");
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

            // getSingleplayerServer must not appear in runtime or recipe
            // code paths (regardless of presence in comments)
            int commentIdx = content.indexOf("getSingleplayerServer");
            if (commentIdx >= 0) {
                // If it appears, verify it's only inside a comment
                int lineStart = content.lastIndexOf('\n', commentIdx) + 1;
                String line = content.substring(lineStart, content.indexOf('\n', commentIdx));
                assertTrue(line.trim().startsWith("//") || line.trim().startsWith("*"),
                        "getSingleplayerServer must only appear in comments");
            }
        }

        @Test
        void runtimeCleanup_exists() throws Exception {
            var sourceFile = new File(
                    "src/client/java/com/modularmc/ten/integration/jei/TENJeiPlugin.java");
            var content = Files.readString(sourceFile.toPath());
            assertTrue(content.contains("deactivateRuntime"),
                    "TENJeiPlugin must have deactivateRuntime method for cleanup");
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
    // 7. Smelter category icons — unified machine_smelter
    // ═══════════════════════════════════════════════════════════════════
    // RED contract: all three custom smelter JEI categories (smelting,
    // blasting, smoking) must use the machine_smelter block item icon,
    // the same as other TEN machine categories.  No eger class-load-time
    // ItemStack resolution; all icon access is lazy via smelterIcon()
    // which defers BuiltInRegistries.ITEM.get() to JEI callback time.

    @Nested
    @DisplayName("7. Smelter category icons — unified machine_smelter")
    class SmelterCategoryIcons {

        @Test
        @DisplayName("registerCategories uses smelterIcon() for all three smelter categories")
        void registerCategories_allSmelterCategoriesUseSmelterIcon() throws Exception {
            var sourceFile = new File(
                    "src/client/java/com/modularmc/ten/integration/jei/TENJeiPlugin.java");
            var content = Files.readString(sourceFile.toPath());

            int catStart = content.indexOf("void registerCategories");
            assertTrue(catStart >= 0, "registerCategories must exist");
            String catBody = content.substring(catStart,
                    content.indexOf("void registerRecipes", catStart));

            long smelterIconCalls = countOccurrences(catBody, "smelterIcon()");
            long blastIconCalls = countOccurrences(catBody, "blastIcon()");
            long smokeIconCalls = countOccurrences(catBody, "smokeIcon()");

            // RED contract: all three SmelterJeiCategory calls use smelterIcon()
            assertEquals(3, smelterIconCalls,
                    "All three SmelterJeiCategory registrations must use smelterIcon() — " +
                    "blasting and smoking categories must NOT use blastIcon/smokeIcon");
            assertEquals(0, blastIconCalls,
                    "No smelter category may use blastIcon()");
            assertEquals(0, smokeIconCalls,
                    "No smelter category may use smokeIcon()");
        }

        @Test
        @DisplayName("registerRecipeCatalysts uses smelterIcon() for all three smelter catalysts")
        void catalystRegistration_allSmelterCatalystsUseSmelterIcon() throws Exception {
            var sourceFile = new File(
                    "src/client/java/com/modularmc/ten/integration/jei/TENJeiPlugin.java");
            var content = Files.readString(sourceFile.toPath());

            int catStart = content.indexOf("void registerRecipeCatalysts");
            assertTrue(catStart >= 0, "registerRecipeCatalysts must exist");
            String catBody = content.substring(catStart,
                    content.indexOf("void registerGuiHandlers", catStart));

            long smelterIconCalls = countOccurrences(catBody, "smelterIcon()");
            long blastIconCalls = countOccurrences(catBody, "blastIcon()");
            long smokeIconCalls = countOccurrences(catBody, "smokeIcon()");

            // All three catalyst registrations must use smelterIcon()
            assertEquals(3, smelterIconCalls,
                    "registerRecipeCatalysts must call smelterIcon() exactly 3 times");
            assertEquals(0, blastIconCalls,
                    "No catalyst may use blastIcon()");
            assertEquals(0, smokeIconCalls,
                    "No catalyst may use smokeIcon()");
        }

        @Test
        @DisplayName("blastIcon and smokeIcon methods are removed (unused)")
        void blastAndSmokeIconMethods_removed() throws Exception {
            var sourceFile = new File(
                    "src/client/java/com/modularmc/ten/integration/jei/TENJeiPlugin.java");
            var content = Files.readString(sourceFile.toPath());

            assertFalse(content.contains("static ItemStack blastIcon()"),
                    "blastIcon() method must be removed when no longer referenced " +
                    "from any registration method");
            assertFalse(content.contains("static ItemStack smokeIcon()"),
                    "smokeIcon() method must be removed when no longer referenced " +
                    "from any registration method");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // 8. Smelter energy gauge — left-side burnLeft decoration
    // ═══════════════════════════════════════════════════════════════════
    // RED contract: all custom smelter JEI categories must have a 14×46
    // burnLeft energy gauge at (8, 2) drawn from modular ENERGY_GAUGE_BG/FILL,
    // matching the machine decoration style.  Hovering the gauge shows
    // "15 FE/t" via the existing kenergyengineering.jei.base_rate_short lang
    // key.  规则 v4：gauge 左缘 x=8、右缘 x=22；组件群左缘=8、右缘=OUTPUT_X+18=142、
    // 总宽 134，中心 = (8+142)/2 = 75 = 面板中心。输入 43（22+21）、箭头 82（输入右缘
    // 61+21）、输出 124（箭头右缘 104+20），间隔 21/21/20 自适应均分剩余 62px，无重叠。
    // Background uses JEI_HANDLER_MODULAR from UV (0,0) at 150×50 (modular
    // top layer shared with other 150×50 machines).

    @Nested
    @DisplayName("8. Smelter energy gauge — left-side burnLeft decoration")
    class SmelterEnergyGauge {

        @Test
        @DisplayName("Category expands to width 150 (modular top layer)")
        void smelterCategory_width150() throws Exception {
            var sourceFile = new File(
                    "src/client/java/com/modularmc/ten/integration/jei/SmelterJeiCategory.java");
            var content = Files.readString(sourceFile.toPath());

            assertTrue(content.contains("WIDTH = 150"),
                    "Width must be 150 to use the modular JEI_HANDLER_MODULAR top layer");
        }

        @Test
        @DisplayName("Category height is 50 (modular top layer)")
        void smelterCategory_height50() throws Exception {
            var sourceFile = new File(
                    "src/client/java/com/modularmc/ten/integration/jei/SmelterJeiCategory.java");
            var content = Files.readString(sourceFile.toPath());

            assertTrue(content.contains("HEIGHT = 50"),
                    "Height must be 50 matching modular top layer's non-transparent area");
        }

        @Test
        @DisplayName("Background drawable from JEI_HANDLER_MODULAR at UV (0,0) using WIDTH/HEIGHT")
        void smelterCategory_backgroundUsesJeiHandlerModular() throws Exception {
            var sourceFile = new File(
                    "src/client/java/com/modularmc/ten/integration/jei/SmelterJeiCategory.java");
            var content = Files.readString(sourceFile.toPath());

            assertTrue(content.contains("JEI_HANDLER_MODULAR"),
                    "Background must use JEI_HANDLER_MODULAR texture");
            assertTrue(content.contains("JEI_HANDLER_MODULAR, 0, 0, WIDTH, HEIGHT"),
                    "Background must draw from UV (0,0) using WIDTH and HEIGHT constants");
        }

        @Test
        @DisplayName("Time text TEXT_Y = 38 keeps text within 50px panel")
        void smelterCategory_textY38() throws Exception {
            var sourceFile = new File(
                    "src/client/java/com/modularmc/ten/integration/jei/SmelterJeiCategory.java");
            var content = Files.readString(sourceFile.toPath());

            assertTrue(content.contains("TEXT_Y = 38"),
                    "TEXT_Y must be 38 to keep cooking time text fully within 50px panel");
        }

        @Test
        @DisplayName("Slot and arrow backgrounds use modular sprites (ITEM_SLOT_SMALL / PROGRESS_ARROW_SMELTER_BG)")
        void smelterCategory_slotAndArrowUseModularSprites() throws Exception {
            var sourceFile = new File(
                    "src/client/java/com/modularmc/ten/integration/jei/SmelterJeiCategory.java");
            var content = Files.readString(sourceFile.toPath());

            assertTrue(content.contains("ITEM_SLOT_SMALL"),
                    "Input/output slots must use modular ITEM_SLOT_SMALL background");
            assertTrue(content.contains(".setTextureSize(18, 18)"),
                    "Slot background must declare actual texture size (18×18) — JEI createDrawable "
                            + "defaults to 256×256 atlas UV semantics and would only render the "
                            + "texture's top-left corner otherwise");
            assertTrue(content.contains("PROGRESS_ARROW_SMELTER_BG"),
                    "Arrow must use modular PROGRESS_ARROW_SMELTER_BG texture");
            assertTrue(content.contains("PROGRESS_ARROW_SMELTER_FILL"),
                    "Arrow must use modular PROGRESS_ARROW_SMELTER_FILL texture for the animated fill");
        }

        @Test
        @DisplayName("Arrow fill animates via PROGRESS_CYCLE_MS progress cycle (left→right crop)")
        void smelterCategory_arrowHasAnimatedFill() throws Exception {
            var sourceFile = new File(
                    "src/client/java/com/modularmc/ten/integration/jei/SmelterJeiCategory.java");
            var content = Files.readString(sourceFile.toPath());

            assertTrue(content.contains("PROGRESS_CYCLE_MS"),
                    "Arrow animation must define a progress cycle constant");
            assertTrue(content.contains("progressPercent()"),
                    "Arrow fill must be driven by progressPercent()");
            assertTrue(content.contains("filledWidth"),
                    "Arrow fill must crop by filledWidth (left→right progress)");
        }

        @Test
        @DisplayName("BurnLeft gauge constants at (8, 2) 14×46")
        void smelterCategory_hasBurnLeftConstants() throws Exception {
            var sourceFile = new File(
                    "src/client/java/com/modularmc/ten/integration/jei/SmelterJeiCategory.java");
            var content = Files.readString(sourceFile.toPath());

            assertTrue(content.contains("BURN_LEFT_X = 8"),
                    "gauge X must be 8 (规则 v3: left edge 8px from panel, right edge 22)");
            assertTrue(content.contains("BURN_LEFT_Y = 2"),
                    "gauge Y must be 2 (top gutter, matching existing machine style)");
            assertTrue(content.contains("BURN_LEFT_W = 14"),
                    "gauge width must be 14 (matching existing machine style)");
            assertTrue(content.contains("BURN_LEFT_H = 46"),
                    "gauge height must be 46 (matching existing machine style)");
        }

        @Test
        @DisplayName("Burn animation constant BURN_CYCLE_MS exists")
        void smelterCategory_hasBurnCycleMs() throws Exception {
            var sourceFile = new File(
                    "src/client/java/com/modularmc/ten/integration/jei/SmelterJeiCategory.java");
            var content = Files.readString(sourceFile.toPath());

            assertTrue(content.contains("BURN_CYCLE_MS"),
                    "gauge animation cycle constant must be defined");
        }

        @Test
        @DisplayName("Input slot at x=43 (21px gap from gauge right edge 22, 组件群居中)")
        void smelterCategory_inputAtX43() throws Exception {
            var sourceFile = new File(
                    "src/client/java/com/modularmc/ten/integration/jei/SmelterJeiCategory.java");
            var content = Files.readString(sourceFile.toPath());

            assertTrue(content.contains("INPUT_X = 43"),
                    "INPUT_X must be 43 (规则 v4 组件群居中: gauge 右缘 22 + 自适应间隔 21)");
            assertFalse(content.contains("INPUT_X = 26"),
                    "INPUT_X must NOT be 26 (v3 旧值，已迁移到组件群居中 v4)");
            assertFalse(content.contains("INPUT_X = 25"),
                    "INPUT_X must NOT be 25 (v3 旧值，已迁移到组件群居中 v4)");
            assertFalse(content.contains("INPUT_X = 11"),
                    "INPUT_X must NOT be 11 (would overlap with gauge at x=8..22)");
        }

        @Test
        @DisplayName("Arrow at x=82 (21px from input right edge 61, 组件群居中)")
        void smelterCategory_arrowAtX82() throws Exception {
            var sourceFile = new File(
                    "src/client/java/com/modularmc/ten/integration/jei/SmelterJeiCategory.java");
            var content = Files.readString(sourceFile.toPath());

            assertTrue(content.contains("ARROW_X = 82"),
                    "ARROW_X must be 82 = input right edge 61 + 21 (规则 v4 自适应间隔)");
            assertFalse(content.contains("ARROW_X = 52"),
                    "ARROW_X must NOT be 52 (v3 旧值，已迁移到组件群居中 v4)");
            assertFalse(content.contains("ARROW_X = 55"),
                    "ARROW_X must NOT be 55 (v3 旧值，已迁移到组件群居中 v4)");
        }

        @Test
        @DisplayName("Time text aligned with input slot at x=43")
        void smelterCategory_textAlignedWithInputX43() throws Exception {
            var sourceFile = new File(
                    "src/client/java/com/modularmc/ten/integration/jei/SmelterJeiCategory.java");
            var content = Files.readString(sourceFile.toPath());

            assertTrue(content.contains("TEXT_X = 43"),
                    "TEXT_X must be 43 to align left with the input slot at x=43");
            assertFalse(content.contains("TEXT_X = 26"),
                    "TEXT_X must NOT be 26 (v3 旧值，已迁移到组件群居中 v4)");
        }

        @Test
        @DisplayName("createRecipeExtras with IRecipeWidget provides gauge tooltip showing 15 FE/t")
        void smelterCategory_hasGaugeTooltip() throws Exception {
            var sourceFile = new File(
                    "src/client/java/com/modularmc/ten/integration/jei/SmelterJeiCategory.java");
            var content = Files.readString(sourceFile.toPath());

            assertTrue(content.contains("createRecipeExtras"),
                    "Category must override createRecipeExtras for gauge area tooltip");
            assertTrue(content.contains("base_rate_short"),
                    "Tooltip must reuse existing base_rate_short lang key");
            assertTrue(content.contains("IRecipeWidget"),
                    "Must use IRecipeWidget for tooltip support");
            assertTrue(content.contains("getTooltip"),
                    "Widget must override getTooltip for gauge area");
            assertTrue(content.contains("15"),
                    "Tooltip must show 15 FE/t (constant energy per tick)");
        }

        @Test
        @DisplayName("Gauge is drawn from modular ENERGY_GAUGE_BG/FILL in draw()")
        void smelterCategory_gaugeUsesEnergyGauge() throws Exception {
            var sourceFile = new File(
                    "src/client/java/com/modularmc/ten/integration/jei/SmelterJeiCategory.java");
            var content = Files.readString(sourceFile.toPath());

            int drawStart = content.indexOf("void draw(");
            assertTrue(drawStart >= 0, "draw method must exist");
            String drawBody = content.substring(drawStart);

            assertTrue(drawBody.contains("BURN_LEFT"),
                    "draw() must reference BURN_LEFT constants");
            assertTrue(drawBody.contains("ENERGY_GAUGE_BG") && drawBody.contains("ENERGY_GAUGE_FILL"),
                    "draw() must use modular ENERGY_GAUGE_BG/FILL textures for gauge rendering");
            assertFalse(drawBody.contains("GUI_HANDLER"),
                    "draw() must NOT use GUI_HANDLER (gauge migrated to modular ENERGY_GAUGE)");
        }

        @Test
        @DisplayName("Layout has no overlap — gauge right edge 22 < input left edge 43 (gap 21)")
        void smelterCategory_noGaugeOverlap() throws Exception {
            var sourceFile = new File(
                    "src/client/java/com/modularmc/ten/integration/jei/SmelterJeiCategory.java");
            var content = Files.readString(sourceFile.toPath());

            // Gauge ends at x=22 (8+14), input starts at x=26 (gap 4, 规则 v3 ≥4)
            // Verify by checking both constants produce no overlap
            assertTrue(content.contains("BURN_LEFT_X = 8") && content.contains("INPUT_X = 43"),
                    "Gauge right edge (22) < input left edge (43): gap 21, no overlap");
            assertTrue(content.contains("BURN_LEFT_W = 14") || !content.contains("BURN_LEFT_W"),
                    "Gauge width 14 confirmed");
        }

        @Test
        @DisplayName("Slots vertically centered at y=16 within 50px background")
        void smelterCategory_slotsVerticallyCentered() throws Exception {
            var sourceFile = new File(
                    "src/client/java/com/modularmc/ten/integration/jei/SmelterJeiCategory.java");
            var content = Files.readString(sourceFile.toPath());

            // y = (HEIGHT-18)/2 = 16 puts slot center y=25 exactly on the
            // background center; text at TEXT_Y=38 stays below the slot bottom (34).
            assertTrue(content.contains("INPUT_Y = 16") && content.contains("OUTPUT_Y = 16"),
                    "Slots must be vertically centered: INPUT_Y/OUTPUT_Y = (50-18)/2 = 16");
        }

        @Test
        @DisplayName("Layout 组件群居中 — gauge 8 → input 43 → arrow 82 → output 124, 中心 75")
        void smelterCategory_layoutCenteredAt75() throws Exception {
            var sourceFile = new File(
                    "src/client/java/com/modularmc/ten/integration/jei/SmelterJeiCategory.java");
            var content = Files.readString(sourceFile.toPath());

            // 规则 v4：组件群左缘=8（能量条左缘）、右缘=OUTPUT_X+18=142、总宽 134，
            // 中心 = (8+142)/2 = 75 = 面板中心。三个间隔自适应均分剩余空间：
            // 能量条右缘 22 → 输入 43（+21）、输入右缘 61 → 箭头 82（+21）、
            // 箭头右缘 104 → 输出 124（+20），总间隔 62 = 134 − 固定宽 72。
            assertTrue(content.contains("INPUT_X = 43") && content.contains("OUTPUT_X = 124")
                            && content.contains("ARROW_X = 82") && content.contains("BURN_LEFT_X = 8"),
                    "组件群居中: gauge 8 → input 43 → arrow 82 → output 124，右缘 142，中心 75");
            assertFalse(content.contains("INPUT_X = 26") || content.contains("ARROW_X = 52")
                            || content.contains("OUTPUT_X = 82"),
                    "不得回退到 v3 固定间隔坐标 (26 → 52 → 82)");
        }

        @Test
        @DisplayName("Category is NOT engine-ized — no EngineFuelCategory, fuel slot, burn icon, or 3-line text")
        void smelterCategory_notEngineized() throws Exception {
            var sourceFile = new File(
                    "src/client/java/com/modularmc/ten/integration/jei/SmelterJeiCategory.java");
            var content = Files.readString(sourceFile.toPath());

            assertFalse(content.contains("EngineFuelCategory"),
                    "Must NOT inherit or reference EngineFuelCategory");
            assertFalse(content.contains("BURN_ICON"),
                    "Must NOT have burn icon constants (engine-specific)");
            assertFalse(content.contains("INPUT_SLOT_X = 10"),
                    "Must NOT use engine fuel slot position x=10");
            assertFalse(content.contains("TEXT_LINE2_Y"),
                    "Must NOT have three-line text layout (engine-specific)");
            assertFalse(content.contains("fuelBudget"),
                    "Must NOT reference engine fuel budget");
        }

        @Test
        @DisplayName("Energy widget getPosition() returns gauge coordinates (BURN_LEFT_X, BURN_LEFT_Y)")
        void smelterCategory_energyWidgetPositionMatchesGauge() throws Exception {
            var sourceFile = new File(
                    "src/client/java/com/modularmc/ten/integration/jei/SmelterJeiCategory.java");
            var content = Files.readString(sourceFile.toPath());

            // Locate getPosition() method within createRecipeExtras / IRecipeWidget boundary
            int getPositionIdx = content.indexOf("getPosition()");
            assertTrue(getPositionIdx >= 0,
                    "createRecipeExtras must define getPosition() on IRecipeWidget");

            // Get the return statement immediately following getPosition()
            int returnIdx = content.indexOf("return", getPositionIdx);
            assertTrue(returnIdx >= 0,
                    "getPosition() must have a return statement");
            int returnLineEnd = content.indexOf('\n', returnIdx);
            String returnLine = content.substring(returnIdx, returnLineEnd).trim();

            // Must reference BURN_LEFT_X and BURN_LEFT_Y, not hardcoded (0,0)
            assertTrue(returnLine.contains("BURN_LEFT_X") && returnLine.contains("BURN_LEFT_Y"),
                    "getPosition() must return ScreenPosition(BURN_LEFT_X, BURN_LEFT_Y) "
                            + "matching gauge position, not hardcoded coordinates");
            assertFalse(returnLine.contains("(0, 0)"),
                    "getPosition() must NOT return (0,0) — widget position must match gauge");
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
