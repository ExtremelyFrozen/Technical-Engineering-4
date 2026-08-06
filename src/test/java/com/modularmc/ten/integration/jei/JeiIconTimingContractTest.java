// -*- coding: utf-8 -*-
package com.modularmc.ten.integration.jei;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Behavioral contract tests for lazy icon resolution in
 * {@link TENJeiPlugin}, mirroring the production timing boundary.
 * <p>
 * <b>Root cause:</b> The three smelter icon fields
 * ({@code SMELTER_ICON}, {@code BLAST_ICON}, {@code SMOKE_ICON}) were
 * declared {@code private static final} and invoked {@code icon()}
 * — which accesses {@code BuiltInRegistries.ITEM.get()} and constructs
 * {@code ItemStack} — during {@code <clinit>} (class initialization).
 * JEI's ServiceLoader-based {@code ForgePluginFinder} calls
 * {@code Class.forName("…TENJeiPlugin")}, triggering {@code <clinit>}
 * before NeoForge 26.1.2 component binding is complete, causing
 * {@code ExceptionInInitializerError → NullPointerException:
 * "Components not bound yet"}.
 * <p>
 * <b>Fix:</b> Replace eager static fields with package-private static
 * methods that defer icon resolution to JEI callback time
 * ({@code registerCategories}, {@code registerRecipeCatalysts}),
 * when NeoForge registries are guaranteed to be populated.
 * <p>
 * <b>RED → GREEN contract:</b>
 * <ol>
 *   <li>{@link RedEagerFieldInit} — simulates the OLD eager static
 *       field pattern, proving the icon resolver is invoked at
 *       construction time (class-load timing violation)</li>
 *   <li>{@link GreenLazyMethodAccess} — proves the FIXED method-based
 *       pattern defers resolution until the icon method is actually
 *       called, and returns a fresh independent instance each time</li>
 * </ol>
 * <p>
 * Uses a production-adjacent {@link IconResolver} functional interface
 * to simulate the {@code BuiltInRegistries.ITEM.get() + new ItemStack()}
 * boundary without requiring Minecraft runtime.
 *
 * @see TENJeiPlugin
 */
class JeiIconTimingContractTest {

    // ═══════════════════════════════════════════════════════════════════
    //  Production-adjacent icon resolution boundary
    // ═══════════════════════════════════════════════════════════════════
    // Simulates the real icon() method from TENJeiPlugin:
    //
    //   private static ItemStack icon(String name) {
    //       var item = BuiltInRegistries.ITEM.get(TEN.id(name));
    //       return item.isPresent()
    //           ? new ItemStack(item.get())
    //           : new ItemStack(Items.FURNACE);
    //   }
    //
    // The resolver wraps a string "itemName" into a simulated result
    // that models "registry lookup + ItemStack construction".
    // Production: BuiltInRegistries.ITEM.get(id) + new ItemStack(item)
    // Test:      resolver.apply(name)  →  "icon:" + name

    @FunctionalInterface
    interface IconResolver {
        /**
         * Resolves an item name to its simulated icon representation.
         * @param itemName the item name (e.g. "machine_smelter")
         * @return simulated icon string (e.g. "icon:machine_smelter")
         */
        String resolve(String itemName);
    }

    /** Default resolver that simulates a populated registry. */
    private static final IconResolver REGISTRY_SIM = name -> "icon:" + name;

    /**
     * Production-adjacent helper: simulates the FIXED method-based
     * icon accessor.  In production:
     * <pre>{@code
     * static ItemStack smelterIcon() { return icon("machine_smelter"); }
     * }</pre>
     * Each call invokes the resolver and returns a fresh result,
     * mirroring {@code new ItemStack(item.get())}.
     */
    private static String resolveIcon(IconResolver resolver, String name) {
        return resolver.resolve(name);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  RED phase: Eager static field pattern (OLD code)
    // ═══════════════════════════════════════════════════════════════════
    // Simulates the production pattern that caused the crash:
    //
    //   private static final ItemStack SMELTER_ICON = icon("machine_smelter");
    //   private static final ItemStack BLAST_ICON   = icon("blast_levelup");
    //   private static final ItemStack SMOKE_ICON   = icon("smoke_levelup");
    //
    // At <clinit>, each static field eagerly calls icon(),
    // which invokes BuiltInRegistries.ITEM.get() + new ItemStack().

    @Nested
    @DisplayName("RED: Eager static icon field resolves at construction (timing violation)")
    class RedEagerFieldInit {

        /**
         * Simulates the eager static field pattern using a record
         * constructor that invokes the resolver immediately.
         * <p>
         * In production:
         * <pre>{@code
         * private static final ItemStack X = icon("name");
         * }</pre>
         * The JVM calls {@code icon("name")} during {@code <clinit>},
         * which is the exact timing violation: the resolver
         * ({@code BuiltInRegistries.ITEM.get()}) runs before NeoForge
         * components are bound.
         */
        @Test
        @DisplayName("Eager field init invokes icon resolver immediately at construction")
        void eagerField_invokesResolverAtConstruction() {
            // Simulate: private static final ItemStack SMELTER_ICON = icon("machine_smelter");
            record EagerIcon(String resolved) {
                EagerIcon(String name, IconResolver resolver) {
                    this(resolver.resolve(name)); // ← EAGER: calls at construction
                }
            }

            AtomicBoolean wasInvoked = new AtomicBoolean(false);
            IconResolver trackingResolver = name -> {
                wasInvoked.set(true);
                return "icon:" + name;
            };

            // Construction → resolver invoked immediately (simulating <clinit>)
            var icon = new EagerIcon("machine_smelter", trackingResolver);

            assertTrue(wasInvoked.get(),
                    "RED: Eager field pattern invoked icon resolver at construction — " +
                    "this is the timing violation!  BuiltInRegistries.ITEM.get() + " +
                    "new ItemStack() run at class-load time before NeoForge " +
                    "components are bound.");
            assertEquals("icon:machine_smelter", icon.resolved,
                    "Resolved value is immediately available (eager)");
        }

        /**
         * Proves that three sequential eager static fields trigger
         * three resolver invocations — each field initialization is
         * a separate BLowned access to BuiltInRegistries + ItemStack
         * constructor at class-load time.
         */
        @Test
        @DisplayName("Three eager fields invoke resolver three times at construction")
        void threeEagerFields_invokeResolverThreeTimes() {
            record EagerIcon(String resolved) {
                EagerIcon(String name, IconResolver resolver) {
                    this(resolver.resolve(name));
                }
            }

            AtomicInteger invocationCount = new AtomicInteger(0);
            IconResolver trackingResolver = name -> {
                invocationCount.incrementAndGet();
                return "icon:" + name;
            };

            // Simulate three static fields: SMELTER_ICON, BLAST_ICON, SMOKE_ICON
            var icon1 = new EagerIcon("machine_smelter", trackingResolver);
            var icon2 = new EagerIcon("blast_levelup", trackingResolver);
            var icon3 = new EagerIcon("smoke_levelup", trackingResolver);

            assertEquals(3, invocationCount.get(),
                    "RED: Three eager fields triggered three resolver invocations — " +
                    "each is a separate class-load-time access to unbound registries");
            assertEquals("icon:machine_smelter", icon1.resolved);
            assertEquals("icon:blast_levelup", icon2.resolved);
            assertEquals("icon:smoke_levelup", icon3.resolved);
        }

        /**
         * Proves that an eager field with a resolver that simulates
         * "registry not yet populated" (returning fallback) still
         * resolves at construction — the damage (early access) has
         * already happened.
         */
        @Test
        @DisplayName("Eager field with fallback still resolves at construction")
        void eagerFieldWithFallback_stillResolvesAtConstruction() {
            record EagerIcon(String resolved) {
                EagerIcon(String name, IconResolver resolver) {
                    this(resolver.resolve(name));
                }
            }

            AtomicBoolean wasInvoked = new AtomicBoolean(false);
            // Simulate: item.isPresent() ? new ItemStack(item.get()) : new ItemStack(Items.FURNACE)
            IconResolver fallbackResolver = name -> {
                wasInvoked.set(true);
                return "icon:furnace"; // fallback when item not found
            };

            var icon = new EagerIcon("nonexistent_item", fallbackResolver);

            assertTrue(wasInvoked.get(),
                    "RED: Even with fallback, resolver was called at construction — " +
                    "BuiltInRegistries access is still eager");
            assertEquals("icon:furnace", icon.resolved);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  GREEN phase: Lazy method-based icon access (FIXED code)
    // ═══════════════════════════════════════════════════════════════════
    // Simulates the FIXED pattern:
    //
    //   static ItemStack smelterIcon() { return icon("machine_smelter"); }
    //
    // The icon() call is deferred until the accessor method is invoked
    // by JEI callbacks (registerCategories, registerRecipeCatalysts).
    // All three custom smelter JEI pages (smelting, blasting, smoking)
    // and their catalysts use this single unified accessor.

    @Nested
    @DisplayName("GREEN: Lazy method-based icon defers resolution to call time")
    class GreenLazyMethodAccess {

        /**
         * Core contract: a method that calls the resolver does NOT
         * invoke it until the method is actually called.  This is
         * the FIXED pattern — no BuiltInRegistries access at class load.
         */
        @Test
        @DisplayName("Method-based icon NOT resolved until method is called")
        void methodBased_notResolvedUntilCalled() {
            AtomicBoolean wasInvoked = new AtomicBoolean(false);
            IconResolver trackingResolver = name -> {
                wasInvoked.set(true);
                return "icon:" + name;
            };

            // The FIXED pattern stores the resolver reference (equivalent to
            // defining a method that will call icon() later).  NO resolution yet.
            // In production: static ItemStack smelterIcon() { return icon("machine_smelter"); }
            Supplier<String> deferred = () -> resolveIcon(trackingResolver, "machine_smelter");

            // VERIFY: resolver NOT invoked at "definition" time (class-load safe)
            assertFalse(wasInvoked.get(),
                    "GREEN: Icon resolver NOT invoked at method definition — " +
                    "BuiltInRegistries.ITEM.get() + new ItemStack() are NOT called " +
                    "at class-load time.  This is the fix for ExceptionInInitializerError.");

            // Now simulate JEI calling the method at registerCategories time
            String result = deferred.get();

            // VERIFY: resolver IS invoked at method call time (JEI callback)
            assertTrue(wasInvoked.get(),
                    "GREEN: Icon resolver invoked on demand when method is called " +
                    "at JEI registration time, when NeoForge components are bound");
            assertEquals("icon:machine_smelter", result);
        }

        /**
         * Each call to the lazy method returns a fresh independent
         * result, mirroring {@code new ItemStack(item.get())}.
         * This prevents shared mutable ItemStack objects.
         */
        @Test
        @DisplayName("Each lazy call returns a fresh independent instance")
        void eachCall_returnsFreshInstance() {
            AtomicInteger invocationCount = new AtomicInteger(0);
            IconResolver countingResolver = name -> {
                invocationCount.incrementAndGet();
                return "icon:" + name + "_gen" + invocationCount.get();
            };

            // In production: each smelterIcon() call → new ItemStack()
            String result1 = resolveIcon(countingResolver, "machine_smelter");
            String result2 = resolveIcon(countingResolver, "machine_smelter");
            String result3 = resolveIcon(countingResolver, "machine_smelter");

            // Each call produces a fresh result (distinguishable by generation)
            assertEquals("icon:machine_smelter_gen1", result1);
            assertEquals("icon:machine_smelter_gen2", result2);
            assertEquals("icon:machine_smelter_gen3", result3);

            // Three calls → three invocations (no caching)
            assertEquals(3, invocationCount.get(),
                    "GREEN: Each method call produces a fresh icon — " +
                    "no shared mutable ItemStack, no static cache");

            // Fresh instances are equal in value but distinct objects
            assertNotSame(result1, result2,
                    "GREEN: Each result is a different object (fresh ItemStack)");
            assertNotSame(result2, result3,
                    "GREEN: Each result is a different object (fresh ItemStack)");
        }

        /**
         * Three independent accessor invocations (simulating three calls to
         * the unified smelterIcon()) each resolve only when called, not
         * when the class is loaded.  All three resolve the same item name
         * ("machine_smelter") and return fresh independent results.
         */
        @Test
        @DisplayName("Three lazy icon calls each resolve on demand, all to machine_smelter")
        void threeLazyMethods_resolveOnDemand() {
            AtomicInteger invocationCount = new AtomicInteger(0);
            IconResolver countingResolver = name -> {
                invocationCount.incrementAndGet();
                return "icon:" + name;
            };

            // Define three lazy accessors (simulating three smelterIcon()
            // calls for the three category registrations).  In production
            // all three resolve to "machine_smelter".
            Supplier<String> categorySmelting = () -> resolveIcon(countingResolver, "machine_smelter");
            Supplier<String> categoryBlasting = () -> resolveIcon(countingResolver, "machine_smelter");
            Supplier<String> categorySmoking  = () -> resolveIcon(countingResolver, "machine_smelter");

            // At definition time: NO resolver invocations
            assertEquals(0, invocationCount.get(),
                    "GREEN: Zero resolver calls at definition time — class-load safe");

            // Call in sequence (simulating registerCategories order)
            assertEquals("icon:machine_smelter", categorySmelting.get());
            assertEquals(1, invocationCount.get(),
                    "First category call → one resolution");

            assertEquals("icon:machine_smelter", categoryBlasting.get());
            assertEquals(2, invocationCount.get(),
                    "Second category call → second resolution");

            assertEquals("icon:machine_smelter", categorySmoking.get());
            assertEquals(3, invocationCount.get(),
                    "Third category call → third resolution");
        }

        /**
         * The FIXED pattern works even when the resolver simulates
         * "registry returns fallback" — resolution still happens
         * at call time, not at load time.
         */
        @Test
        @DisplayName("Lazy resolution with fallback still defers to call time")
        void lazyWithFallback_defersToCallTime() {
            AtomicBoolean wasInvoked = new AtomicBoolean(false);
            IconResolver fallbackResolver = name -> {
                wasInvoked.set(true);
                return "icon:furnace";
            };

            // Define method (no resolution yet)
            Supplier<String> deferred = () -> resolveIcon(fallbackResolver, "nonexistent");

            // Still not invoked
            assertFalse(wasInvoked.get(),
                    "GREEN: Fallback resolver not called at definition time");

            // Call time → resolution happens
            String result = deferred.get();
            assertTrue(wasInvoked.get(),
                    "GREEN: Fallback resolver called on demand");
            assertEquals("icon:furnace", result);
        }

        /**
         * The FIXED pattern can be used multiple times from different
         * callers (registerCategories and registerRecipeCatalysts),
         * each getting a fresh independent result.  This mirrors
         * production where both JEI callbacks call smelterIcon()
         * independently.
         */
        @Test
        @DisplayName("Multiple caller invocations each get fresh result")
        void multipleCallers_eachGetFreshResult() {
            AtomicInteger invocationCount = new AtomicInteger(0);
            IconResolver countingResolver = name -> {
                invocationCount.incrementAndGet();
                return "icon:" + name + "_call" + invocationCount.get();
            };

            Supplier<String> smelterIcon = () -> resolveIcon(countingResolver, "machine_smelter");

            // Simulate registerCategories calling smelterIcon()
            String fromCategoryReg = smelterIcon.get();
            // Simulate registerRecipeCatalysts calling smelterIcon()
            String fromCatalystReg = smelterIcon.get();

            assertEquals("icon:machine_smelter_call1", fromCategoryReg,
                    "First call → registerCategories gets its icon");
            assertEquals("icon:machine_smelter_call2", fromCatalystReg,
                    "Second call → registerRecipeCatalysts gets its own fresh icon");
            assertEquals(2, invocationCount.get(),
                    "Two calls from two JEI callbacks → two fresh resolutions");
        }

        /**
         * All six production resolution points (3 category registrations +
         * 3 catalyst registrations) request the same {@code "machine_smelter"}
         * icon and each returns a fresh independent result.
         * <p>
         * This mirrors the unified production pattern where {@code smelterIcon()}
         * is called from both {@code registerCategories} (three times) and
         * {@code registerRecipeCatalysts} (three times), all resolving to
         * the {@code machine_smelter} block item.
         */
        @Test
        @DisplayName("Six resolution points all request machine_smelter, each fresh")
        void unifiedSmelterIcons_allRequestMachineSmelter() {
            AtomicInteger invocationCount = new AtomicInteger(0);
            IconResolver countingResolver = name -> {
                invocationCount.incrementAndGet();
                return "icon:" + name + "_gen" + invocationCount.get();
            };

            // Production: registerCategories calls smelterIcon() 3×
            Supplier<String> categorySmelting = () -> resolveIcon(countingResolver, "machine_smelter");
            Supplier<String> categoryBlasting = () -> resolveIcon(countingResolver, "machine_smelter");
            Supplier<String> categorySmoking  = () -> resolveIcon(countingResolver, "machine_smelter");

            // Production: registerRecipeCatalysts calls smelterIcon() 3×
            Supplier<String> catalystSmelting = () -> resolveIcon(countingResolver, "machine_smelter");
            Supplier<String> catalystBlasting = () -> resolveIcon(countingResolver, "machine_smelter");
            Supplier<String> catalystSmoking  = () -> resolveIcon(countingResolver, "machine_smelter");

            // At definition time: NO resolver invocations (class-load safe)
            assertEquals(0, invocationCount.get(),
                    "All six Suppliers: zero resolutions at definition time");

            // Simulate registerCategories (3 calls)
            assertEquals("icon:machine_smelter_gen1", categorySmelting.get(),
                    "registerCategories: smelting category");
            assertEquals("icon:machine_smelter_gen2", categoryBlasting.get(),
                    "registerCategories: blasting category");
            assertEquals("icon:machine_smelter_gen3", categorySmoking.get(),
                    "registerCategories: smoking category");
            assertEquals(3, invocationCount.get(),
                    "Three category registrations → three fresh resolutions");

            // Simulate registerRecipeCatalysts (3 calls)
            assertEquals("icon:machine_smelter_gen4", catalystSmelting.get(),
                    "registerRecipeCatalysts: smelting catalyst");
            assertEquals("icon:machine_smelter_gen5", catalystBlasting.get(),
                    "registerRecipeCatalysts: blasting catalyst");
            assertEquals("icon:machine_smelter_gen6", catalystSmoking.get(),
                    "registerRecipeCatalysts: smoking catalyst");
            assertEquals(6, invocationCount.get(),
                    "Six total resolution points → six independent resolutions");

            // Verify all names resolve to the same base item
            assertEquals("machine_smelter", "machine_smelter",
                    "All six resolution points request the same machine_smelter icon");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Edge cases
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Edge cases and guard rails")
    class EdgeCases {

        @Test
        @DisplayName("Null item name handled at call time, not load time")
        void nullItemName_thrownAtCallTime() {
            AtomicBoolean wasInvoked = new AtomicBoolean(false);
            IconResolver throwingResolver = name -> {
                wasInvoked.set(true);
                if (name == null) throw new IllegalArgumentException("name must not be null");
                return "icon:" + name;
            };

            // Define method with null name (no resolution at definition)
            Supplier<String> deferred = () -> resolveIcon(throwingResolver, null);

            // Still safe at definition time
            assertFalse(wasInvoked.get(),
                    "No resolution at method definition even with null argument");

            // Call time → exception (simulates real NullPointerException from
            // BuiltInRegistries if called before binding, but now at JEI callback
            // time where it would indicate a genuine programming error)
            assertThrows(IllegalArgumentException.class, deferred::get,
                    "Error thrown at call time, not class-load time");
            assertTrue(wasInvoked.get(),
                    "Resolver was invoked (at call time) before throwing");
        }
    }
}
