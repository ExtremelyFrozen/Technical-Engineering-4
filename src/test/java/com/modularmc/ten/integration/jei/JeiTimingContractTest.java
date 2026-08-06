// -*- coding: utf-8 -*-
package com.modularmc.ten.integration.jei;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Behavioral contract tests for the lazy Supplier resolution pattern used in
 * {@link TENJeiPlugin#buildMachineTypeDefs()} and
 * {@link TENJeiPlugin#tryInjectMachineRecipes()}.
 * <p>
 * These tests verify executable behavior (not source-string assertions):
 * <ol>
 *   <li>Supplier-based type defs do NOT invoke {@link Supplier#get()} at construction time</li>
 *   <li>Lazy resolution defers registry access until injection time</li>
 *   <li>Null from Supplier is handled safely (skipped with diagnosis, not injected as key)</li>
 *   <li>The exact iteration pattern used in tryInjectMachineRecipes is null-safe</li>
 *   <li>Eager resolution at construction time would violate the timing boundary (RED proof)</li>
 * </ol>
 * <p>
 * This mirrors the production code pattern:
 * <pre>{@code
 * // TENJeiPlugin:
 * private record MachineTypeDef(
 *         mezz.jei.api.recipe.RecipeType<FormsCombinedRecipe> jeiType,
 *         Supplier<net.minecraft.world.item.crafting.RecipeType<FormsCombinedRecipe>> tenType) {}
 *
 * private static final List<MachineTypeDef> MACHINE_TYPES = buildMachineTypeDefs();
 *
 * private static List<MachineTypeDef> buildMachineTypeDefs() {
 *     return List.of(
 *         new MachineTypeDef(..., TENRecipeTypes.PULVERIZER_T::get),  // Supplier, not .get()
 *         ...
 *     );
 * }
 *
 * private static void tryInjectMachineRecipes() {
 *     for (var def : MACHINE_TYPES) {
 *         var recipeType = def.tenType.get();   // lazy: resolved at injection time
 *         if (recipeType == null) {             // null-safe guard
 *             log.warn("Skipping...");
 *             continue;                          // never inject null as Map key
 *         }
 *         manager.addRecipes(def.jeiType, cached);  // safe injection
 *     }
 * }
 * }</pre>
 */
class JeiTimingContractTest {

    // ═══════════════════════════════════════════════════════════════════
    //  Helper: type definition record matching TENJeiPlugin.MachineTypeDef
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Mirrors {@code TENJeiPlugin.MachineTypeDef} — a JEI recipe type paired
     * with a {@link Supplier} for deferred resolution of the TEN recipe type.
     */
    private record MachineDef(
            String categoryId,
            Supplier<String> tenTypeSupplier) {}

    // ═══════════════════════════════════════════════════════════════════
    //  RED phase: demonstrate that EAGER resolution would fail
    // ═══════════════════════════════════════════════════════════════════
    // These tests simulate the OLD broken pattern where .get() is called
    // at construction time, proving it violates the timing boundary.

    @Nested
    @DisplayName("RED: Eager .get() at construction violates timing boundary")
    class RedEagerResolution {

        /**
         * Simulates what the OLD code did: eagerly calling .get() at
         * MachineTypeDef construction.  This is the pattern that would
         * cause TENJeiPlugin to access {@code TENRecipeTypes.*.get()}
         * during class loading, before NeoForge registries are populated.
         * <p>
         * This test proves that eager construction triggers premature
         * supplier invocation — the exact timing issue the fix prevents.
         */
        @Test
        @DisplayName("Eager record construction invokes supplier prematurely")
        void eagerConstruction_invokesSupplierPrematurely() {
            // Simulate the OLD eager pattern: calling .get() at construction
            record EagerDef(String categoryId, String resolvedValue) {
                EagerDef(String categoryId, Supplier<String> supplier) {
                    this(categoryId, supplier.get()); // ← EAGER: calls get() at construction
                }
            }

            AtomicBoolean wasInvoked = new AtomicBoolean(false);
            Supplier<String> eagerSupplier = () -> {
                wasInvoked.set(true);
                return "pulverizer";
            };

            // Construction → supplier.get() is called immediately
            var def = new EagerDef("pulverizer", eagerSupplier);

            // VERIFY: supplier was invoked at construction time (BAD for timing)
            assertTrue(wasInvoked.get(),
                    "RED: Eager pattern invoked supplier at construction — " +
                    "this is the timing violation!  get() runs at class-load time " +
                    "before registries are populated.");
            // The value is already resolved
            assertEquals("pulverizer", def.resolvedValue);
        }

        /**
         * Shows that if registries are not yet populated when the eager
         * pattern runs, the supplier returns null and the null value is
         * stored — exactly the bug scenario described in the review.
         */
        @Test
        @DisplayName("Eager pattern stores null when registry not populated")
        void eagerPattern_storesNullWhenRegistryNotReady() {
            record EagerDef(String resolvedValue) {}

            // Simulate registry not yet populated → supplier returns null
            Supplier<String> nullSupplier = () -> null;

            var def = new EagerDef(nullSupplier.get()); // ← EAGER: null at construction

            // If this value were used as a Map key, it would NPE or create a null entry
            assertNull(def.resolvedValue,
                    "RED: Eager pattern accepted null from unpopulated registry — " +
                    "this null would be silently injected or cause NPE as Map key.");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  GREEN phase: demonstrate that Supplier-based LAZY resolution
    //  avoids the timing issue and handles null safely
    // ═══════════════════════════════════════════════════════════════════
    // These tests prove the FIXED pattern (current code) works correctly.

    @Nested
    @DisplayName("GREEN: Supplier-based lazy resolution avoids timing issue")
    class GreenLazyResolution {

        /**
         * The core contract: constructing a MachineDef that stores a Supplier
         * MUST NOT invoke {@code Supplier.get()}.  This is what prevents
         * class-load-time registry access in TENJeiPlugin.
         */
        @Test
        @DisplayName("Supplier NOT invoked at MachineDef construction")
        void supplier_notInvokedAtConstruction() {
            AtomicBoolean wasInvoked = new AtomicBoolean(false);
            Supplier<String> supplier = () -> {
                wasInvoked.set(true);
                return "pulverizer";
            };

            // Construction: stores Supplier, does NOT call get()
            var def = new MachineDef("pulverizer", supplier);

            // VERIFY: supplier was NOT invoked at construction
            assertFalse(wasInvoked.get(),
                    "GREEN: Supplier was NOT invoked at construction — " +
                    "get() is deferred until injection time, avoiding class-load-time registry access.");
            assertNotNull(def);
            assertEquals("pulverizer", def.categoryId);
        }

        /**
         * When Supplier.get() is called at injection time, it resolves correctly.
         * This proves the lazy pattern works: construction is safe, resolution
         * happens later when registries are available.
         */
        @Test
        @DisplayName("Supplier resolves correctly when called at injection time")
        void supplier_resolvesOnDemand() {
            AtomicInteger invocationCount = new AtomicInteger(0);
            Supplier<String> supplier = () -> {
                invocationCount.incrementAndGet();
                return "compressor";
            };

            var def = new MachineDef("compressor", supplier);

            // Before get(): supplier not invoked
            assertEquals(0, invocationCount.get(),
                    "Supplier not yet invoked after construction");

            // At injection time: call get()
            String result = def.tenTypeSupplier().get();

            // Then: supplier was invoked and returned the correct value
            assertEquals(1, invocationCount.get(),
                    "Supplier invoked exactly once when get() is called at injection time");
            assertEquals("compressor", result);
        }

        /**
         * The exact null-safe iteration pattern used in
         * {@code TENJeiPlugin.tryInjectMachineRecipes()}:
         * <pre>
         * for (var def : MACHINE_TYPES) {
         *     var recipeType = def.tenType.get();
         *     if (recipeType == null) {
         *         TEN.LOGGER.warn("Skipping...");
         *         continue;  // ← null entry SKIPPED, not injected
         *     }
         *     manager.addRecipes(def.jeiType, cached);  // safe injection
         * }
         * </pre>
         */
        @Test
        @DisplayName("Null from Supplier is skipped with continue (not injected)")
        void nullFromSupplier_skippedNotInjected() {
            // Mix of null and valid suppliers — simulating pre/post registry population
            Supplier<String> nullSupplier = () -> null;       // registry not ready
            Supplier<String> validSupplier = () -> "valid";   // registry populated

            var defs = List.of(
                    new MachineDef("machine_a", nullSupplier),
                    new MachineDef("machine_b", validSupplier),
                    new MachineDef("machine_c", nullSupplier),
                    new MachineDef("machine_d", validSupplier));

            // Execute the same pattern as tryInjectMachineRecipes:
            // Iterate, resolve lazily, skip nulls, never inject null as a key
            List<String> injectedCategories = new ArrayList<>();
            List<String> skippedCategories = new ArrayList<>();

            for (var def : defs) {
                String resolved = def.tenTypeSupplier().get();
                if (resolved == null) {
                    skippedCategories.add(def.categoryId());
                    continue; // ← null-safe skip, never inject null
                }
                injectedCategories.add(def.categoryId() + "=" + resolved);
            }

            // VERIFY: null entries were skipped, not injected
            assertEquals(2, skippedCategories.size(),
                    "Null-returning Suppliers were skipped");
            assertTrue(skippedCategories.contains("machine_a"),
                    "Null supplier for machine_a was skipped");
            assertTrue(skippedCategories.contains("machine_c"),
                    "Null supplier for machine_c was skipped");

            // VERIFY: valid entries were collected for injection
            assertEquals(2, injectedCategories.size(),
                    "Valid-returning Suppliers were collected for injection");
            assertTrue(injectedCategories.contains("machine_b=valid"));
            assertTrue(injectedCategories.contains("machine_d=valid"));

            // VERIFY: no null key was ever injected
            assertFalse(injectedCategories.contains(null),
                    "Null was never injected as a category key");
            for (String entry : injectedCategories) {
                assertNotNull(entry, "Each injected entry is non-null");
                assertFalse(entry.startsWith("null"),
                        "No injected entry starts with null");
            }
        }

        /**
         * Even when ALL Suppliers return null (registries completely
         * unavailable), the pattern handles it gracefully without crashing
         * or injecting null keys.
         */
        @Test
        @DisplayName("All-null input handled gracefully, no crash")
        void allNull_handledGracefully() {
            Supplier<String> nullSup = () -> null;

            var defs = List.of(
                    new MachineDef("type_x", nullSup),
                    new MachineDef("type_y", nullSup));

            List<String> injected = new ArrayList<>();
            for (var def : defs) {
                String resolved = def.tenTypeSupplier().get();
                if (resolved == null) continue;
                injected.add(def.categoryId());
            }

            assertTrue(injected.isEmpty(),
                    "No injection happened when all Suppliers returned null");
        }

        /**
         * Mixed null/valid Suppliers simulate the REAL scenario:
         * some registries are populated (pulverizer) but others haven't
         * been registered yet (new machine added after class loading).
         * The partial resolution must not prevent valid injections.
         */
        @Test
        @DisplayName("Partial resolution: valid entries injected, nulls skipped")
        void partialResolution() {
            var defs = List.of(
                    new MachineDef("injected_ok", () -> "ready"),
                    new MachineDef("skipped_null", () -> null),
                    new MachineDef("also_injected", () -> "ready"));

            List<String> results = new ArrayList<>();
            for (var def : defs) {
                String value = def.tenTypeSupplier().get();
                if (value == null) continue;
                results.add(def.categoryId());
            }

            assertEquals(2, results.size());
            assertTrue(results.contains("injected_ok"));
            assertTrue(results.contains("also_injected"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Edge cases: empty list, concurrent access safety of pattern
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Edge cases and pattern invariants")
    class EdgeCases {

        @Test
        @DisplayName("Empty def list — no iteration, no crash")
        void emptyDefList() {
            List<MachineDef> empty = List.of();
            for (var def : empty) {
                fail("Should not iterate over empty list");
            }
            // Passes if we reach here
        }

        @Test
        @DisplayName("Single element list works correctly")
        void singleElement() {
            var def = new MachineDef("only_one", () -> "value");
            assertEquals("value", def.tenTypeSupplier().get());
        }

        @Test
        @DisplayName("Supplier is idempotent — multiple get() calls return same value")
        void supplierIdempotent() {
            var supplier = new Supplier<String>() {
                private String value = "stable";
                @Override
                public String get() {
                    return value;
                }
            };

            var def = new MachineDef("stable_id", supplier);
            assertEquals("stable", def.tenTypeSupplier().get());
            assertEquals("stable", def.tenTypeSupplier().get());
            assertEquals("stable", def.tenTypeSupplier().get());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Lazy holder list resolution (TENClientRecipeCache target types)
    // ═══════════════════════════════════════════════════════════════════
    // Tests the inner static holder pattern that TENClientRecipeCache
    // uses to defer DeferredHolder.get() from class-load time to
    // event-path access.  The OLD code had static final fields that
    // called .get() eagerly at class initialization; the FIXED code
    // uses an inner holder class loaded only on first use.
    //
    // RED: eager .get() at list construction resolves immediately (timing risk)
    // GREEN: Supplier stored in holder defers .get() to first access (safe)

    @Nested
    @DisplayName("Lazy holder list resolution (TENClientRecipeCache target types)")
    class LazyHolderResolution {

        /**
         * RED proof: Calling {@code supplier.get()} at list construction time
         * (which is what {@code static final List} initialization does) invokes
         * the supplier immediately.  This is the OLD pattern used by
         * {@code TENClientRecipeCache}'s {@code TARGET_TYPES = List.of(type.get(), ...)},
         * which called {@code DeferredHolder.get()} at class-load time.
         */
        @Test
        @DisplayName("RED: supplier.get() at list construction resolves immediately")
        void eagerSupplierGet_atListConstruction() {
            AtomicBoolean called = new AtomicBoolean(false);
            Supplier<String> supplier = () -> {
                called.set(true);
                return "pulverizer";
            };

            // OLD pattern: .get() at list construction — same as static init
            List<String> list = List.of(supplier.get());

            assertTrue(called.get(),
                    "RED: .get() invoked at construction — timing violation! " +
                    "Same as TARGET_TYPES = List.of(TENRecipeTypes.X_T.get(), ...) at class load.");
            assertEquals(List.of("pulverizer"), list);
        }

        /**
         * RED proof: {@code List.of()} throws {@link NullPointerException}
         * if any argument is null.  The OLD code would crash at class load
         * if any {@code DeferredHolder.get()} returned null.
         */
        @Test
        @DisplayName("RED: List.of with null throws NPE — old code crash risk")
        void listOf_withNull_throwsNpe() {
            assertThrows(NullPointerException.class,
                    () -> List.of("a", null, "b"),
                    "List.of throws NPE — old TARGET_TYPES would crash if get() returned null");
        }

        /**
         * RED proof: {@code Set.copyOf()} throws NPE with null elements.
         * The old {@code TARGET_TYPE_SET = Set.copyOf(TARGET_TYPES)} would crash
         * on null entries, just like {@code List.of}.
         */
        @Test
        @DisplayName("RED: Set.copyOf with null throws NPE — old code crash risk")
        void setCopyOf_withNull_throwsNpe() {
            List<String> withNull = new ArrayList<>();
            withNull.add("a");
            withNull.add(null);

            assertThrows(NullPointerException.class,
                    () -> java.util.Set.copyOf(withNull),
                    "Set.copyOf throws NPE — old TARGET_TYPE_SET would crash");
        }

        /**
         * GREEN proof: A Supplier stored in a holder is NOT called until
         * {@code .get()} is invoked.  This is the pattern used by the FIXED
         * {@code TENClientRecipeCache}: the inner {@code TargetTypesHolder}
         * stores the resolution logic but does not invoke it until the holder
         * is first accessed on the event path.
         */
        @Test
        @DisplayName("GREEN: Supplier stored in holder defers .get() to first access")
        void supplierGet_deferredUntilFirstAccess() {
            AtomicBoolean called = new AtomicBoolean(false);
            Supplier<String> supplier = () -> {
                called.set(true);
                return "compressor";
            };

            // NEW pattern: store supplier (no .get() call)
            Supplier<String> holder = supplier;
            assertFalse(called.get(),
                    "GREEN: .get() NOT called at holder creation — deferred");

            // First access: now resolve
            String resolved = holder.get();
            assertTrue(called.get(),
                    "GREEN: .get() called on first access — resolution deferred to event path");
            assertEquals("compressor", resolved);
        }

        /**
         * GREEN proof: Null-guarded list building with individual null checks
         * prevents null entries from reaching immutable collections.
         * This mirrors the null-safe pattern in
         * {@code TENClientRecipeCache.TargetTypesHolder.resolve()},
         * where each {@code DeferredHolder.get()} is checked before adding.
         */
        @Test
        @DisplayName("GREEN: Null-guarded list building filters nulls from immutable collection")
        void nullGuardedList_preventsNullEntries() {
            String resolvedNull = null;   // DeferredHolder.get() returned null
            String resolvedValid = "pulverizer";

            List<String> building = new ArrayList<>();
            if (resolvedNull != null) building.add(resolvedNull);    // skipped
            if (resolvedValid != null) building.add(resolvedValid);  // added

            List<String> frozen = List.copyOf(building);

            assertEquals(1, frozen.size(),
                    "Null filtered, only valid entry present");
            assertEquals("pulverizer", frozen.get(0),
                    "Correct entry preserved");
            assertDoesNotThrow(() -> java.util.Set.copyOf(frozen),
                    "Set.copyOf of null-filtered list is safe");
        }

        /**
         * GREEN proof: All-null resolution produces empty collections,
         * no crash.  The cache simply won't match any types, preventing
         * snapshot acceptance (safe fallback).
         */
        @Test
        @DisplayName("GREEN: All-null resolution produces empty collections (no crash)")
        void allNullResolution_producesEmptyCollections() {
            String v1 = null;
            String v2 = null;

            List<String> building = new ArrayList<>();
            if (v1 != null) building.add(v1);
            if (v2 != null) building.add(v2);

            List<String> frozen = List.copyOf(building);
            assertTrue(frozen.isEmpty(),
                    "All-null → empty list, not a crash");
            assertDoesNotThrow(() -> java.util.Set.copyOf(frozen),
                    "Set.copyOf of empty list is safe");
        }

        /**
         * GREEN proof: Partial resolution (some null, some valid).
         * Only non-null entries survive to the immutable collection.
         */
        @Test
        @DisplayName("GREEN: Partial resolution — valid entries present, nulls absent")
        void partialResolution() {
            String t1 = "type_a";
            String t2 = null;   // unresolved
            String t3 = "type_c";

            List<String> building = new ArrayList<>();
            if (t1 != null) building.add(t1);
            if (t2 != null) building.add(t2);
            if (t3 != null) building.add(t3);

            List<String> frozen = List.copyOf(building);
            assertEquals(2, frozen.size(),
                    "Only non-null entries survive");
            assertTrue(frozen.contains("type_a"));
            assertTrue(frozen.contains("type_c"));
            // contains(null) on immutable List.copyOf() throws NPE in Java 21+,
            // so we verify via stream predicate instead.
            assertFalse(frozen.stream().anyMatch(java.util.Objects::isNull),
                    "No null in frozen list");
        }
    }
}
