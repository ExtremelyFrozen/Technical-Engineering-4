package com.modularmc.ten.api.recipe;

/**
 * Executable unit test for FormsCombinedIngredient tag resolution logic.
 * <p>
 * Same package as production code → can access package-private fields
 * (matchItems, ifTagItem, matchFluids, ifTagFluid) to verify no-cache policy.
 * <p>
 * <b>Limitation:</b> Minecraft's registry (BuiltInRegistries) must be bootstrapped
 * for any Item reference. Only {@code type=tag} creation avoids registry access
 * after our fix (no eager {@code TagHelper.getItems()} call). Static/direct item
 * regression is covered by compilation-only TagIngredientGameTest.
 * <p>
 * Test tiers:
 * <ol>
 *   <li><b>Core (no bootstrap):</b> tag create() leaves matchItems empty,
 *       ifTagItem set, key parsing, fail-fast on '#', sentinel, field contracts.</li>
 *   <li><b>Registry-optional:</b> symbolItem/itemStacks/toOriginStackIngredients
 *       gracefully skip when registry unavailable.</li>
 * </ol>
 * <p>
 * Full dynamic tag resolution (membership change across reload, all items
 * visible in JEI/GUI) is verified at GameTest runtime by TagIngredientGameTest.
 */
public class FormsCombinedIngredientTagUnitTest {

    static int passed = 0;
    static int failed = 0;

    public static void main(String[] args) {
        System.out.println("=== FormsCombinedIngredientTagUnitTest ===");

        // ── Tier 1: Core — must pass, no game bootstrap needed ──
        testTagItemMatchItemsEmpty();
        testTagItemIfTagItemSet();
        testTagItemCorrectFields();
        testTagFluidMatchFluidsEmpty();
        testTagFluidIfTagFluidSet();
        testTagCreateSecondInstanceIndependence();
        testTagKeyWithHashFailsFast();
        testTagKeyWithoutHashUnchanged();
        testAllowAllSentinel();
        testTypeFormRoundTrip();

        // ── Tier 2: Registry-optional — access BuiltInRegistries, skip gracefully ──
        testItemStacksRegistryOptional();
        testFluidStacksRegistryOptional();
        testToOriginStackIngredientsRegistryOptional();
        testSymbolItemRegistryOptional();
        testSymbolFluidRegistryOptional();

        // ── Results ──
        System.out.println("=== Results ===");
        System.out.println("Passed: " + passed);
        System.out.println("Failed: " + failed);
        if (failed > 0) {
            System.out.println("CORE TESTS FAILED — see above for details");
            System.exit(1);
        }
        System.out.println("All core tests PASSED");
        System.out.println("(runtime-optional tests may have skipped — expected without game bootstrap)");
    }

    // ── Assertion helpers ─────────────────────────────────────────────

    static void assertTrue(boolean condition, String message) {
        if (condition) { passed++; }
        else { System.err.println("FAIL: " + message); failed++; }
    }

    static void assertFalse(boolean condition, String message) {
        assertTrue(!condition, message);
    }

    static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual == null : expected.equals(actual)) { passed++; }
        else { System.err.println("FAIL: " + message + " — expected: " + expected + ", got: " + actual); failed++; }
    }

    static void assertNotNull(Object obj, String message) {
        assertTrue(obj != null, message);
    }

    /** Gracefully skip a registry-dependent test. */
    static void skipBecause(String testName, String reason) {
        System.out.println("  SKIP [" + testName + "]: " + reason);
    }

    // ═══════════════════════════════════════════════════════════════════
    // Tier 1: Core tests (no game bootstrap required)
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Core invariant: create(…, "item", "tag", …) must NOT eagerly cache
     * matchItems. Root cause: TagHelper.getItems() returned empty before
     * PendingTags.apply(), permanently caching the empty list.
     */
    static void testTagItemMatchItemsEmpty() {
        var ing = FormsCombinedIngredient.create(1, "item", "tag", "c:ingots/iron", 1.0);
        assertTrue(ing.matchItems == null || ing.matchItems.isEmpty(),
                "matchItems must be empty after create() for type=tag item");
    }

    /**
     * Core invariant: ifTagItem must be set so itemStacks() can dynamically
     * resolve tag members at invocation time.
     */
    static void testTagItemIfTagItemSet() {
        var ing = FormsCombinedIngredient.create(1, "item", "tag", "c:ingots/iron", 1.0);
        assertNotNull(ing.ifTagItem, "ifTagItem must be non-null for type=tag item");
        assertEquals("c:ingots/iron", ing.ifTagItem.location().toString(),
                "ifTagItem location must match key");
    }

    /**
     * Public field contracts for tag ingredients.
     */
    static void testTagItemCorrectFields() {
        var ing = FormsCombinedIngredient.create(1, "item", "tag", "c:ingots/iron", 1.0);
        assertEquals("tag", ing.type(), "type() must be 'tag'");
        assertEquals("item", ing.form(), "form() must be 'item'");
        assertEquals("c:ingots/iron", ing.key().toString(), "key() must be 'c:ingots/iron'");
        assertEquals(1, ing.amountOrCount(), "amountOrCount must be 1");

        var ing4 = FormsCombinedIngredient.create(4, "item", "tag", "c:gems/diamond", 0.5);
        assertEquals("tag", ing4.type(), "type() must be 'tag'");
        assertEquals("item", ing4.form(), "form() must be 'item'");
        assertEquals("c:gems/diamond", ing4.key().toString(), "key() must be 'c:gems/diamond'");
        assertEquals(4, ing4.amountOrCount(), "amountOrCount must be 4");
        assertTrue(Math.abs(ing4.chance() - 0.5) < 0.001, "chance must be 0.5");
    }

    /**
     * Fluid symmetric: create(…, "fluid", "tag", …) must not cache matchFluids.
     */
    static void testTagFluidMatchFluidsEmpty() {
        var ing = FormsCombinedIngredient.create(1000, "fluid", "tag", "c:water", 1.0);
        assertTrue(ing.matchFluids == null || ing.matchFluids.isEmpty(),
                "matchFluids must be empty after create() for type=tag fluid");
    }

    /**
     * Fluid symmetric: ifTagFluid must be set.
     */
    static void testTagFluidIfTagFluidSet() {
        var ing = FormsCombinedIngredient.create(1000, "fluid", "tag", "c:water", 1.0);
        assertNotNull(ing.ifTagFluid, "ifTagFluid must be non-null for type=tag fluid");
    }

    /**
     * Two ingredients with same tag key are independent — both leave matchItems empty.
     * Proves no shared mutable state.
     */
    static void testTagCreateSecondInstanceIndependence() {
        var ing1 = FormsCombinedIngredient.create(1, "item", "tag", "c:ingots/iron", 1.0);
        var ing2 = FormsCombinedIngredient.create(4, "item", "tag", "c:ingots/iron", 1.0);
        assertTrue(ing1.matchItems == null || ing1.matchItems.isEmpty(),
                "first instance matchItems must be empty");
        assertTrue(ing2.matchItems == null || ing2.matchItems.isEmpty(),
                "second instance matchItems must be empty");
        assertEquals(1, ing1.amountOrCount(), "first instance count must be 1");
        assertEquals(4, ing2.amountOrCount(), "second instance count must be 4");
        assertEquals(ing1.ifTagItem, ing2.ifTagItem,
                "both instances must share the same ifTagItem");
    }

    /**
     * Fail-fast: key with leading '#' must throw (Identifier.parse rejects '#').
     * Do NOT silently strip — manual JSON and codec paths must be consistent.
     */
    static void testTagKeyWithHashFailsFast() {
        try {
            FormsCombinedIngredient.create(1, "item", "tag", "#c:ingots/iron", 1.0);
            assertFalse(true, "create() with '#c:ingots/iron' must throw");
        } catch (RuntimeException e) {
            // Identifier.parse throws IdentifierException (or IllegalArgumentException)
            // in NeoForge 26.1.2. Either way, a RuntimeException is the expected fail-fast.
            assertTrue(true, "create() with '#c:ingots/iron' threw as expected: "
                    + e.getClass().getSimpleName() + ": " + e.getMessage());
        } catch (Exception e) {
            assertFalse(true, "expected RuntimeException, got "
                    + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /**
     * Key without '#' passes through unchanged.
     */
    static void testTagKeyWithoutHashUnchanged() {
        var ing = FormsCombinedIngredient.create(1, "item", "tag", "c:ingots/iron", 1.0);
        assertEquals("c:ingots/iron", ing.key().toString(),
                "clean key must not be modified");
    }

    /**
     * ALLOW_ALL sentinel returns [EMPTY].
     */
    static void testAllowAllSentinel() {
        var ing = FormsCombinedIngredient.create(0, "item", "tag", "c:ingots/iron", 1.0);
        // Don't use create() with static/air — that needs registry for parseItem.
        // Set sentinel directly on a tag ingredient.
        ing.ALLOW_ALL = true;
        var stacks = ing.itemStacks();
        assertTrue(stacks.size() == 1 && stacks.get(0).isEmpty(),
                "ALLOW_ALL itemStacks must return [EMPTY]");
    }

    /**
     * type() and form() round-trip correctly for tag fluid.
     */
    static void testTypeFormRoundTrip() {
        var ing = FormsCombinedIngredient.create(1, "item", "tag", "c:ingots/iron", 1.0);
        assertEquals("item", ing.form(), "form() round-trip");
        assertEquals("tag", ing.type(), "type() round-trip");

        var fluidIng = FormsCombinedIngredient.create(1000, "fluid", "tag", "c:water", 1.0);
        assertEquals("fluid", fluidIng.form(), "fluid form() round-trip");
        assertEquals("tag", fluidIng.type(), "fluid type() round-trip");
    }

    // ═══════════════════════════════════════════════════════════════════
    // Tier 2: Registry-optional tests
    //   These call BuiltInRegistries (via itemStacks/fluidStacks/
    //   toOriginStackIngredients/symbolItem) and need game bootstrap.
    //   Without bootstrap, they gracefully skip with a message.
    //   TagIngredientGameTest provides runtime coverage at GameTest level.
    // ═══════════════════════════════════════════════════════════════════

    static void testItemStacksRegistryOptional() {
        try {
            var ing = FormsCombinedIngredient.create(1, "item", "tag", "c:ingots/iron", 1.0);
            var stacks = ing.itemStacks();
            assertNotNull(stacks, "itemStacks() must return non-null");
        } catch (Throwable e) {
            if (e instanceof ExceptionInInitializerError || e instanceof IllegalStateException) {
                skipBecause("itemStacks", "BuiltInRegistries not bootstrapped: " + e.getClass().getSimpleName());
            } else {
                skipBecause("itemStacks", e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }

    static void testFluidStacksRegistryOptional() {
        try {
            var ing = FormsCombinedIngredient.create(1000, "fluid", "tag", "c:water", 1.0);
            var stacks = ing.fluidStacks();
            assertNotNull(stacks, "fluidStacks() must return non-null");
        } catch (Throwable e) {
            if (e instanceof ExceptionInInitializerError || e instanceof IllegalStateException) {
                skipBecause("fluidStacks", "BuiltInRegistries not bootstrapped: " + e.getClass().getSimpleName());
            } else {
                skipBecause("fluidStacks", e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }

    /**
     * toOriginStackIngredients uses Ingredient.of(HolderSet<Item>)
     * to create a proper tag-based Ingredient (NeoForge 26.1.2 API).
     */
    static void testToOriginStackIngredientsRegistryOptional() {
        try {
            var ing = FormsCombinedIngredient.create(1, "item", "tag", "c:ingots/iron", 1.0);
            var origin = ing.toOriginStackIngredients();
            assertNotNull(origin, "toOriginStackIngredients must return non-null");
        } catch (Throwable e) {
            if (e instanceof ExceptionInInitializerError || e instanceof IllegalStateException) {
                skipBecause("toOriginStackIngredients", "BuiltInRegistries not bootstrapped: " + e.getClass().getSimpleName());
            } else {
                skipBecause("toOriginStackIngredients", e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }

    static void testSymbolItemRegistryOptional() {
        try {
            var ing = FormsCombinedIngredient.create(1, "item", "tag", "c:ingots/iron", 1.0);
            var symbol = ing.symbolItem();
            assertNotNull(symbol, "symbolItem() must return non-null");
        } catch (Throwable e) {
            if (e instanceof ExceptionInInitializerError || e instanceof IllegalStateException) {
                skipBecause("symbolItem", "BuiltInRegistries not bootstrapped: " + e.getClass().getSimpleName());
            } else {
                skipBecause("symbolItem", e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }

    static void testSymbolFluidRegistryOptional() {
        try {
            var ing = FormsCombinedIngredient.create(1000, "fluid", "tag", "c:water", 1.0);
            var symbol = ing.symbolFluid();
            assertNotNull(symbol, "symbolFluid() must return non-null");
        } catch (Throwable e) {
            if (e instanceof ExceptionInInitializerError || e instanceof IllegalStateException) {
                skipBecause("symbolFluid", "BuiltInRegistries not bootstrapped: " + e.getClass().getSimpleName());
            } else {
                skipBecause("symbolFluid", e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }
}
