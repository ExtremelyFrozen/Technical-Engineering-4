// -*- coding: utf-8 -*-
package com.modularmc.ten.data;

import com.modularmc.ten.common.data.Mat;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RED→GREEN tests for migrating two hand-written shapeless recipes into
 * data generation via {@link Mat} matrix metadata.
 * <p>
 * Phase RED: These tests must fail because:
 * <ul>
 *   <li>{@link Mat#shapelessRecipes()} does not yet exist</li>
 *   <li>{@link TENRecipeGen#buildShapeless(Mat.ShapelessRecipe)} does not yet exist</li>
 *   <li>Hand-written source files still exist in {@code src/main/resources/}</li>
 *   <li>Generated files do not yet exist in {@code src/generated/resources/}</li>
 * </ul>
 */
class TENRecipeGenShapelessRecipeTest {

    // ══════════════════════════════════════════════════════════════════
    // A. Mat shapeless recipe metadata — must return correct semantics
    // ══════════════════════════════════════════════════════════════════

    @Nested
    class MatShapelessRecipeMetadata {

        @Test
        void poweredTin_hasOneShapelessRecipe() {
            List<Mat.ShapelessRecipe> recipes = Mat.POWERED_TIN.shapelessRecipes();
            assertEquals(1, recipes.size(),
                    "POWERED_TIN must define exactly one shapeless recipe");
        }

        @Test
        void poweredTin_recipe_ingredients_exact() {
            Mat.ShapelessRecipe recipe = Mat.POWERED_TIN.shapelessRecipes().get(0);
            assertEquals(
                    List.of("c:dusts/tin", "c:dusts/copper", "minecraft:redstone", "minecraft:redstone"),
                    recipe.ingredients(),
                    "POWERED_TIN shapeless recipe must have exact ingredient IDs");
        }

        @Test
        void poweredTin_recipe_resultItem() {
            Mat.ShapelessRecipe recipe = Mat.POWERED_TIN.shapelessRecipes().get(0);
            assertEquals("kenergyengineering:powered_tin_dust", recipe.resultItem());
        }

        @Test
        void poweredTin_recipe_resultCount() {
            Mat.ShapelessRecipe recipe = Mat.POWERED_TIN.shapelessRecipes().get(0);
            assertEquals(2, recipe.resultCount());
        }

        @Test
        void chlorium_hasOneShapelessRecipe() {
            List<Mat.ShapelessRecipe> recipes = Mat.CHLORIUM.shapelessRecipes();
            assertEquals(1, recipes.size(),
                    "CHLORIUM must define exactly one shapeless recipe");
        }

        @Test
        void chlorium_recipe_ingredients_exact() {
            Mat.ShapelessRecipe recipe = Mat.CHLORIUM.shapelessRecipes().get(0);
            assertEquals(
                    List.of("c:dusts/powered_tin", "c:dusts/powered_tin",
                            "minecraft:glowstone_dust", "minecraft:glowstone_dust",
                            "c:dusts/nickel"),
                    recipe.ingredients(),
                    "CHLORIUM shapeless recipe must have exact ingredient IDs including duplicates");
        }

        @Test
        void chlorium_recipe_resultItem() {
            Mat.ShapelessRecipe recipe = Mat.CHLORIUM.shapelessRecipes().get(0);
            assertEquals("kenergyengineering:chlorium_dust", recipe.resultItem());
        }

        @Test
        void chlorium_recipe_resultCount() {
            Mat.ShapelessRecipe recipe = Mat.CHLORIUM.shapelessRecipes().get(0);
            assertEquals(2, recipe.resultCount());
        }

        @Test
        void otherMats_haveEmptyShapelessRecipes() {
            for (Mat mat : Mat.values()) {
                if (mat != Mat.POWERED_TIN && mat != Mat.CHLORIUM) {
                    assertTrue(mat.shapelessRecipes().isEmpty(),
                            mat.id + " should have no shapeless recipes");
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // B. buildShapeless() JSON builder — must produce correct 26.1 format
    // ══════════════════════════════════════════════════════════════════

    @Nested
    class BuildShapelessJson {

        private JsonObject callBuildShapeless(Mat.ShapelessRecipe recipe) throws Exception {
            Method method = TENRecipeGen.class.getDeclaredMethod(
                    "buildShapeless", Mat.ShapelessRecipe.class);
            method.setAccessible(true);
            return (JsonObject) method.invoke(null, recipe);
        }

        @Test
        void type_isCraftingShapeless() throws Exception {
            Mat.ShapelessRecipe recipe = Mat.POWERED_TIN.shapelessRecipes().get(0);
            JsonObject json = callBuildShapeless(recipe);
            assertEquals("minecraft:crafting_shapeless", json.get("type").getAsString());
        }

        @Test
        void poweredTin_ingredients_areBareStrings() throws Exception {
            Mat.ShapelessRecipe recipe = Mat.POWERED_TIN.shapelessRecipes().get(0);
            JsonObject json = callBuildShapeless(recipe);
            JsonArray ingredients = json.getAsJsonArray("ingredients");
            assertEquals(4, ingredients.size());

            // Tags get # prefix, items stay bare
            assertEquals("#c:dusts/tin", ingredients.get(0).getAsString(),
                    "Tag ingredient must have # prefix");
            assertEquals("#c:dusts/copper", ingredients.get(1).getAsString(),
                    "Tag ingredient must have # prefix");
            assertEquals("minecraft:redstone", ingredients.get(2).getAsString(),
                    "Item ingredient must be bare string");
            assertEquals("minecraft:redstone", ingredients.get(3).getAsString(),
                    "Duplicate item ingredient must be preserved");
        }

        @Test
        void chlorium_ingredients_areBareStrings() throws Exception {
            Mat.ShapelessRecipe recipe = Mat.CHLORIUM.shapelessRecipes().get(0);
            JsonObject json = callBuildShapeless(recipe);
            JsonArray ingredients = json.getAsJsonArray("ingredients");
            assertEquals(5, ingredients.size());

            assertEquals("#c:dusts/powered_tin", ingredients.get(0).getAsString());
            assertEquals("#c:dusts/powered_tin", ingredients.get(1).getAsString(),
                    "Duplicate tag ingredient must be preserved");
            assertEquals("minecraft:glowstone_dust", ingredients.get(2).getAsString());
            assertEquals("minecraft:glowstone_dust", ingredients.get(3).getAsString(),
                    "Duplicate item ingredient must be preserved");
            assertEquals("#c:dusts/nickel", ingredients.get(4).getAsString());
        }

        @Test
        void ingredients_arePrimitives_notObjects() throws Exception {
            Mat.ShapelessRecipe recipe = Mat.POWERED_TIN.shapelessRecipes().get(0);
            JsonObject json = callBuildShapeless(recipe);
            JsonArray ingredients = json.getAsJsonArray("ingredients");
            for (int i = 0; i < ingredients.size(); i++) {
                assertTrue(ingredients.get(i).isJsonPrimitive(),
                        "Ingredient " + i + " must be a primitive string, not an object");
            }
        }

        @Test
        void result_hasId_andCount() throws Exception {
            Mat.ShapelessRecipe recipe = Mat.POWERED_TIN.shapelessRecipes().get(0);
            JsonObject json = callBuildShapeless(recipe);
            JsonObject result = json.getAsJsonObject("result");
            assertEquals("kenergyengineering:powered_tin_dust", result.get("id").getAsString());
            assertEquals(2, result.get("count").getAsInt());
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // C. Hand-written source files must be removed (generated replaces them)
    // ══════════════════════════════════════════════════════════════════

    @Nested
    class HandwrittenSourceFilesRemoved {

        @Test
        void chloriumDust_handwritten_shouldNotExist() {
            Path file = Path.of(
                    "src/main/resources/data/kenergyengineering/recipe/vanilla/chlorium_dust.json");
            assertFalse(Files.exists(file),
                    "chlorium_dust.json must be deleted from main/resources; "
                    + "generated version at src/generated/resources is the sole source");
        }

        @Test
        void poweredTinDust_handwritten_shouldNotExist() {
            Path file = Path.of(
                    "src/main/resources/data/kenergyengineering/recipe/vanilla/powered_tin_dust.json");
            assertFalse(Files.exists(file),
                    "powered_tin_dust.json must be deleted from main/resources; "
                    + "generated version at src/generated/resources is the sole source");
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // D. Generated recipe files must exist and match original semantics
    // ══════════════════════════════════════════════════════════════════

    @Nested
    class GeneratedRecipeFilesExist {

        @Test
        void chloriumDust_generated_exists() {
            Path file = Path.of(
                    "src/generated/resources/data/kenergyengineering/recipe/vanilla/chlorium_dust.json");
            assertTrue(Files.exists(file),
                    "chlorium_dust.json must be generated by runClientData");
        }

        @Test
        void poweredTinDust_generated_exists() {
            Path file = Path.of(
                    "src/generated/resources/data/kenergyengineering/recipe/vanilla/powered_tin_dust.json");
            assertTrue(Files.exists(file),
                    "powered_tin_dust.json must be generated by runClientData");
        }

        @Test
        void chloriumDust_semantics_matchOriginal() throws Exception {
            Path file = Path.of(
                    "src/generated/resources/data/kenergyengineering/recipe/vanilla/chlorium_dust.json");
            assertTrue(Files.exists(file));
            String content = Files.readString(file);
            JsonObject json = JsonParser.parseString(content).getAsJsonObject();

            assertEquals("minecraft:crafting_shapeless", json.get("type").getAsString());

            JsonArray ingredients = json.getAsJsonArray("ingredients");
            assertEquals(5, ingredients.size());
            assertEquals("#c:dusts/powered_tin", ingredients.get(0).getAsString());
            assertEquals("#c:dusts/powered_tin", ingredients.get(1).getAsString());
            assertEquals("minecraft:glowstone_dust", ingredients.get(2).getAsString());
            assertEquals("minecraft:glowstone_dust", ingredients.get(3).getAsString());
            assertEquals("#c:dusts/nickel", ingredients.get(4).getAsString());

            JsonObject result = json.getAsJsonObject("result");
            assertEquals("kenergyengineering:chlorium_dust", result.get("id").getAsString());
            assertEquals(2, result.get("count").getAsInt());
        }

        @Test
        void poweredTinDust_semantics_matchOriginal() throws Exception {
            Path file = Path.of(
                    "src/generated/resources/data/kenergyengineering/recipe/vanilla/powered_tin_dust.json");
            assertTrue(Files.exists(file));
            String content = Files.readString(file);
            JsonObject json = JsonParser.parseString(content).getAsJsonObject();

            assertEquals("minecraft:crafting_shapeless", json.get("type").getAsString());

            JsonArray ingredients = json.getAsJsonArray("ingredients");
            assertEquals(4, ingredients.size());
            assertEquals("#c:dusts/tin", ingredients.get(0).getAsString());
            assertEquals("#c:dusts/copper", ingredients.get(1).getAsString());
            assertEquals("minecraft:redstone", ingredients.get(2).getAsString());
            assertEquals("minecraft:redstone", ingredients.get(3).getAsString());

            JsonObject result = json.getAsJsonObject("result");
            assertEquals("kenergyengineering:powered_tin_dust", result.get("id").getAsString());
            assertEquals(2, result.get("count").getAsInt());
        }
    }
}
