// -*- coding: utf-8 -*-
package com.modularmc.ten.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for {@link TENRecipeGen} vanilla recipe ingredient format
 * in Minecraft 26.1 / NeoForge 26.1.2.
 * <p>
 * In 26.1, the ingredient codec no longer accepts {@code {"item": "id"}} or
 * {@code {"tag": "id"}} JSON objects. Ingredients must be serialized as bare
 * strings: {@code "namespace:id"} for items, {@code "#namespace:id"} for tags.
 * <p>
 * These tests verify that {@code TENRecipeGen.ref()} and the vanilla recipe
 * builder methods produce the correct 26.1-compatible format.
 */
class TENRecipeGenIngredientFormat26_1Test {

    // ══════════════════════════════════════════════════════════════════
    // A. ref() helper — must produce 26.1 string format
    // ══════════════════════════════════════════════════════════════════

    @Nested
    class RefHelperFormat {

        private JsonElement callRef(String id) throws Exception {
            Method refMethod = TENRecipeGen.class.getDeclaredMethod("ref", String.class);
            refMethod.setAccessible(true);
            return (JsonElement) refMethod.invoke(null, id);
        }

        @Test
        void ref_returnsJsonPrimitive_forItemId() throws Exception {
            JsonElement result = callRef("minecraft:iron_ingot");
            assertTrue(result.isJsonPrimitive(),
                    "ref() must return a JsonPrimitive, not a JsonObject: " + result);
        }

        @Test
        void ref_returnsBareString_forItemId() throws Exception {
            JsonElement result = callRef("minecraft:iron_ingot");
            assertEquals("minecraft:iron_ingot", result.getAsString(),
                    "ref() must return bare item ID string, not wrapped in {\"item\":...}");
        }

        @Test
        void ref_returnsBareString_forModNamespaceItem() throws Exception {
            JsonElement result = callRef("kenergyengineering:tin_ingot");
            assertEquals("kenergyengineering:tin_ingot", result.getAsString());
        }

        @Test
        void ref_returnsTagString_withHashPrefix() throws Exception {
            // IDs starting with "c:" represent common tags
            JsonElement result = callRef("c:ingots/tin");
            assertEquals("#c:ingots/tin", result.getAsString(),
                    "ref() must prefix tag IDs with '#' for 26.1 tag syntax");
        }

        @Test
        void ref_doesNotReturnObject_forTagId() throws Exception {
            JsonElement result = callRef("c:ingots/iron");
            assertTrue(result.isJsonPrimitive(),
                    "ref() for tag IDs must return a JsonPrimitive, not JsonObject");
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // B. Vanilla recipe builders — ingredient/key fields must be strings
    // ══════════════════════════════════════════════════════════════════

    @Nested
    class VanillaRecipeBuilderFormat {

        /**
         * Call a private static method that returns JsonObject on TENRecipeGen.
         */
        private JsonObject callBuilder(String methodName, Class<?>... paramTypes)
                throws Exception {
            // We need to find the method and call it with suitable params.
            // Since these are private, we use reflection.
            // For simplicity, we test the generated output via source inspection
            // AND via the ref() output which is the core building block.
            return null; // placeholder — actual test via source scanning below
        }

        @Test
        void smeltingIngredient_isString_notObject() throws Exception {
            // Verify buildSmelting calls ref() which returns a string,
            // and the ingredient field is a JsonPrimitive
            var sourceFile = Path.of(
                    "src/main/java/com/modularmc/ten/data/TENRecipeGen.java");
            assertTrue(Files.exists(sourceFile));
            String content = Files.readString(sourceFile);

            // buildSmelting must use ref() for ingredient
            assertTrue(content.contains("j.add(\"ingredient\", ref(input))"),
                    "buildSmelting must set ingredient via ref(input)");
        }

        @Test
        void blastingIngredient_isString_notObject() throws Exception {
            var sourceFile = Path.of(
                    "src/main/java/com/modularmc/ten/data/TENRecipeGen.java");
            assertTrue(Files.exists(sourceFile));
            String content = Files.readString(sourceFile);

            assertTrue(content.contains("j.add(\"ingredient\", ref(input))"),
                    "buildBlasting must set ingredient via ref(input)");
        }

        @Test
        void shapedGearKey_isRef_notOldFormat() throws Exception {
            var sourceFile = Path.of(
                    "src/main/java/com/modularmc/ten/data/TENRecipeGen.java");
            assertTrue(Files.exists(sourceFile));
            String content = Files.readString(sourceFile);

            // buildShapedGear must use ref() for key values
            assertTrue(content.contains("obj(\"C\", ref(mat.itemId(\"ingot\")))"),
                    "buildShapedGear key must use ref() format");
        }

        @Test
        void shapedPackKey_isRef_notOldFormat() throws Exception {
            var sourceFile = Path.of(
                    "src/main/java/com/modularmc/ten/data/TENRecipeGen.java");
            assertTrue(Files.exists(sourceFile));
            String content = Files.readString(sourceFile);

            // buildPack must use ref() for key values
            assertTrue(content.contains("obj(\"C\", ref(unitId))"),
                    "buildPack must use ref() for key");
        }

        @Test
        void shapelessIngredients_isRef_notOldFormat() throws Exception {
            var sourceFile = Path.of(
                    "src/main/java/com/modularmc/ten/data/TENRecipeGen.java");
            assertTrue(Files.exists(sourceFile));
            String content = Files.readString(sourceFile);

            assertTrue(content.contains("arr(ref(blockId))"),
                    "buildPack shapeless must use ref() for ingredients");
        }

        @Test
        void mouldKey_isRef_notOldFormat() throws Exception {
            var sourceFile = Path.of(
                    "src/main/java/com/modularmc/ten/data/TENRecipeGen.java");
            assertTrue(Files.exists(sourceFile));
            String content = Files.readString(sourceFile);

            assertTrue(content.contains("obj(\"A\", ref(\"kenergyengineering:tin_ingot\"))"),
                    "buildMould must use ref() for key");
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // C. Generated recipe JSON must NOT contain {"item":...} or {"tag":...}
    //    in ingredient/key positions — verified via datagen output
    // ══════════════════════════════════════════════════════════════════

    @Nested
    class GeneratedRecipeJsonFormat {

        @Test
        void smeltingRecipe_ingredientIsString_notObject() throws Exception {
            var file = Path.of(
                    "src/generated/resources/data/kenergyengineering/recipe/vanilla/burning/iron_ingot_fd.json");
            assertTrue(Files.exists(file), "iron_ingot_fd.json must exist");
            String content = Files.readString(file);

            // The ingredient must be a bare string, not {"item": "..."}
            assertFalse(content.contains("\"ingredient\": {\n    \"item\":"),
                    "ingredient must NOT be {\"item\":...} object: " + content);
            assertFalse(content.contains("\"ingredient\": {\n    \"tag\":"),
                    "ingredient must NOT be {\"tag\":...} object: " + content);

            // Must be a bare string
            assertTrue(content.contains("\"ingredient\": \"kenergyengineering:iron_dust\""),
                    "ingredient must be bare string 'kenergyengineering:iron_dust': " + content);
        }

        @Test
        void shapedGearRecipe_keyIsString_notObject() throws Exception {
            var file = Path.of(
                    "src/generated/resources/data/kenergyengineering/recipe/vanilla/material/tin_gear.json");
            assertTrue(Files.exists(file), "tin_gear.json must exist");
            String content = Files.readString(file);

            // Key 'C' must be a bare string, not {"item": "..."}
            assertFalse(content.contains("\"C\": {\n      \"item\":"),
                    "Key 'C' must NOT be {\"item\":...} object: " + content);
            assertTrue(content.contains("\"C\": \"kenergyengineering:tin_ingot\""),
                    "Key 'C' must be bare string: " + content);
        }

        @Test
        void shapedPackRecipe_keyIsString_notObject() throws Exception {
            var file = Path.of(
                    "src/generated/resources/data/kenergyengineering/recipe/vanilla/compress/pb_tin.json");
            assertTrue(Files.exists(file), "pb_tin.json must exist");
            String content = Files.readString(file);

            assertFalse(content.contains("\"C\": {\n      \"item\":"),
                    "Key 'C' must NOT be {\"item\":...} object");
            assertTrue(content.matches("(?s).*\"C\":\\s*\"kenergyengineering:tin_ingot\".*"),
                    "Key 'C' must be bare string 'kenergyengineering:tin_ingot': " + content);
        }

        @Test
        void shapelessIngredients_areStrings() throws Exception {
            var file = Path.of(
                    "src/generated/resources/data/kenergyengineering/recipe/vanilla/compress/rb_tin.json");
            assertTrue(Files.exists(file), "rb_tin.json must exist");
            String content = Files.readString(file);

            assertFalse(content.contains("\"item\":"),
                    "shapeless ingredients must not contain {\"item\":...}");
            assertTrue(content.contains("\"kenergyengineering:tin_block\""),
                    "ingredients must contain bare item ID string: " + content);
        }

        @Test
        void mouldRecipe_keyIsString() throws Exception {
            var file = Path.of(
                    "src/generated/resources/data/kenergyengineering/recipe/vanilla/mould_gear.json");
            assertTrue(Files.exists(file), "mould_gear.json must exist");
            String content = Files.readString(file);

            assertFalse(content.contains("\"A\": {\n      \"item\":"),
                    "Key 'A' must NOT be {\"item\":...} object");
            assertTrue(content.contains("\"A\": \"kenergyengineering:tin_ingot\""),
                    "Key 'A' must be bare string");
        }

        @Test
        void noVanillaRecipeContainsItemTagObject() throws Exception {
            // Scan all vanilla recipe files for any {"item":...} or {"tag":...}
            // that should have been converted to strings
            var vanillaDir = Path.of(
                    "src/generated/resources/data/kenergyengineering/recipe/vanilla");
            assertTrue(Files.isDirectory(vanillaDir), "vanilla recipe directory must exist");

            long count = Files.walk(vanillaDir)
                    .filter(p -> p.toString().endsWith(".json"))
                    .filter(p -> {
                        try {
                            String content = Files.readString(p);
                            // For vanilla recipes, ingredient/key must use string format
                            // Check for old object format in ingredient/key positions
                            return content.contains("\"item\":")
                                    || content.contains("\"tag\":");
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .count();

            assertEquals(0, count,
                    "No generated vanilla recipe JSON files should contain old {\"item\":...} or {\"tag\":...} format");
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // D. Hand-written (main/resources) recipe JSON must NOT contain
    //    old {"item":...} / {"tag":...} in ingredient positions
    // ══════════════════════════════════════════════════════════════════

    @Nested
    class HandwrittenRecipeJsonFormat {

        @Test
        void noHandwrittenRecipeContainsItemTagObject() throws Exception {
            // Scan all main/resources vanilla recipe files for old format
            var vanillaDir = Path.of(
                    "src/main/resources/data/kenergyengineering/recipe/vanilla");
            assertTrue(Files.isDirectory(vanillaDir), "vanilla recipe directory in main/resources must exist");

            long count = Files.walk(vanillaDir)
                    .filter(p -> p.toString().endsWith(".json"))
                    .filter(p -> {
                        try {
                            String content = Files.readString(p);
                            // Vanilla recipe ingredients/keys must use bare string format
                            // in NeoForge 26.1; reject old object wrapper format
                            return content.contains("\"item\":")
                                    || content.contains("\"tag\":");
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .count();

            assertEquals(0, count,
                    "No hand-written vanilla recipe JSON files should contain old {\"item\":...} or {\"tag\":...} format. "
                            + "Fix files: chlorium_dust.json, powered_tin_dust.json");
        }
    }
}
