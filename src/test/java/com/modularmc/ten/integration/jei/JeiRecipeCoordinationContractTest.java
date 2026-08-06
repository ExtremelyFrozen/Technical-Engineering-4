// -*- coding: utf-8 -*-
package com.modularmc.ten.integration.jei;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Minimal source contract tests for JEI plugin and cache structure.
 * <p>
 * Behavior tests for the state machine live in
 * {@link com.modularmc.ten.network.JeiSyncStateTest}.
 * These contracts only verify production source patterns that
 * cannot be tested through the pure state machine alone.
 */
class JeiRecipeCoordinationContractTest {

    private static final File PLUGIN_SRC = new File(
            "src/client/java/com/modularmc/ten/integration/jei/TENJeiPlugin.java");
    private static final File CACHE_SRC = new File(
            "src/client/java/com/modularmc/ten/client/TENClientRecipeCache.java");

    @Nested
    class PluginStructure {

        @Test
        void noOldMachineGenerationField() throws Exception {
            var content = Files.readString(PLUGIN_SRC.toPath());
            assertFalse(content.contains("machinesInjectedGeneration"),
                    "machinesInjectedGeneration must be removed; use JeiSyncState instead");
        }

        @Test
        void noOldPopulatedRecipeManager() throws Exception {
            var content = Files.readString(PLUGIN_SRC.toPath());
            assertFalse(content.contains("populatedRecipeManager"),
                    "populatedRecipeManager must be removed");
        }

        @Test
        void hasSmelterPopulatedManager() throws Exception {
            var content = Files.readString(PLUGIN_SRC.toPath());
            assertTrue(content.contains("smelterPopulatedManager"),
                    "Must retain separate smelter guard");
        }

        @Test
        void hasActiveRecipeManager() throws Exception {
            var content = Files.readString(PLUGIN_SRC.toPath());
            assertTrue(content.contains("volatile IRecipeManager activeRecipeManager"),
                    "activeRecipeManager must be volatile for cross-thread visibility");
        }

        @Test
        void smelterPopulatedManagerIsVolatile() throws Exception {
            var content = Files.readString(PLUGIN_SRC.toPath());
            assertTrue(content.contains("volatile IRecipeManager smelterPopulatedManager"),
                    "smelterPopulatedManager must be volatile");
        }

        @Test
        void cacheListenerFlagsAreVolatile() throws Exception {
            var content = Files.readString(PLUGIN_SRC.toPath());
            assertTrue(content.contains("volatile boolean cacheListenerRegistered"),
                    "cacheListenerRegistered must be volatile");
            assertTrue(content.contains("volatile boolean cacheListenerActive"),
                    "cacheListenerActive must be volatile");
        }

        @Test
        void identityChangeCallsDeactivate() throws Exception {
            var content = Files.readString(PLUGIN_SRC.toPath());
            int availIdx = content.indexOf("void onRuntimeAvailable");
            assertTrue(availIdx >= 0);
            String body = content.substring(availIdx,
                    content.indexOf("private static void deactivateRuntime()", availIdx));
            // The identity-change branch must call deactivateRuntime() when
            // a different IRecipeManager is detected
            assertTrue(body.contains("deactivateRuntime()"),
                    "onRuntimeAvailable identity-change path must call deactivateRuntime()");
            assertTrue(body.contains("activateRuntime()"),
                    "onRuntimeAvailable must call state.activateRuntime() for new manager");
        }

        @Test
        void unavailableCallsDeactivate() throws Exception {
            var content = Files.readString(PLUGIN_SRC.toPath());
            int idx = content.indexOf("private static void deactivateRuntime");
            assertTrue(idx >= 0);
            String body = content.substring(idx,
                    content.indexOf("private static <T extends AbstractCookingRecipe>", idx));
            assertTrue(body.contains("deactivateRuntime()"),
                    "deactivateRuntime must call deactivateRuntime on the state");
        }

        @Test
        void tryInjectUsesTryClaimInject() throws Exception {
            var content = Files.readString(PLUGIN_SRC.toPath());
            // Verify the tryInjectMachineRecipes method uses the state machine
            assertTrue(content.contains("tryClaimInject"),
                    "tryInjectMachineRecipes must use JeiSyncState.tryClaimInject()");
            assertTrue(content.contains("markMachinesInjected"),
                    "tryInjectMachineRecipes must call markMachinesInjected after injection");
        }

        // ── M1 timing safety — lazy registry resolution ───────────────

        @Test
        void machineTypesDef_usesLazySupplierNotEagerGet() throws Exception {
            var content = Files.readString(PLUGIN_SRC.toPath());
            // buildMachineTypeDefs() must use Supplier + ::get method references
            // so that DeferredHolder.get() is NOT called during class loading.
            // If it uses .get() directly, JEI ServiceLoader could trigger
            // TENJeiPlugin class load before NeoForge registries are populated,
            // causing silent null RecipeType entries.
            int methodIdx = content.indexOf("private static List<MachineTypeDef> buildMachineTypeDefs");
            assertTrue(methodIdx >= 0, "buildMachineTypeDefs method must exist");
            int bodyStart = content.indexOf('{', methodIdx);
            int bodyEnd = content.indexOf('}', bodyStart);
            assertTrue(bodyStart > 0 && bodyEnd > bodyStart,
                    "buildMachineTypeDefs must have a valid method body");
            String body = content.substring(bodyStart, bodyEnd);

            // Must NOT contain .get() — that eagerly resolves during class load
            assertFalse(body.contains(".get()"),
                    "buildMachineTypeDefs must not call .get() — eager resolution at class load time\n" +
                    "  Use ::get method references instead (lazy resolution via Supplier).");
            // Must contain ::get for each machine type — deferred resolution
            assertTrue(body.contains("::get"),
                    "buildMachineTypeDefs must use ::get method references for lazy DeferredHolder resolution\n" +
                    "  Supplier defers the .get() call until injection time.");
        }

        @Test
        void machineTypesField_typeUsesSupplier() throws Exception {
            var content = Files.readString(PLUGIN_SRC.toPath());
            // The MachineTypeDef record must declare tenType as Supplier,
            // not as the resolved RecipeType directly.
            int recordIdx = content.indexOf("private record MachineTypeDef");
            assertTrue(recordIdx >= 0, "MachineTypeDef record must exist");
            int recordEnd = content.indexOf('}', recordIdx);
            String recordDef = content.substring(recordIdx, recordEnd);

            assertTrue(recordDef.contains("Supplier<"),
                    "MachineTypeDef.tenType must be a Supplier<RecipeType<...>> for lazy resolution\n" +
                    "  Supplier defers .get() until injection, avoiding class-load-time registry access.");
            // Must NOT be the eager type
            assertFalse(recordDef.matches("RecipeType\\s*<"),
                    "MachineTypeDef.tenType must NOT be a resolved RecipeType — use Supplier<RecipeType<...>>");
        }

        @Test
        void machineTypes_doNotHaveStaticFinalTypeList() throws Exception {
            var content = Files.readString(PLUGIN_SRC.toPath());
            // The static MACHINE_TYPES field should use Supplier-based MachineTypeDef,
            // meaning it should NOT have the old pattern with resolved types.
            // The field declaration should reference the MachineTypeDef record (with Supplier).
            int fieldIdx = content.indexOf("MACHINE_TYPES = buildMachineTypeDefs");
            assertTrue(fieldIdx >= 0, "MACHINE_TYPES field must exist");
            // The field type must be List<MachineTypeDef> which now has Supplier inside
            assertTrue(content.contains("List<MachineTypeDef> MACHINE_TYPES"),
                    "MACHINE_TYPES must be List<MachineTypeDef> (with Supplier-based record)");
        }

        @Test
        void tryInjectMachineRecipes_guardsNullTypeResolution() throws Exception {
            var content = Files.readString(PLUGIN_SRC.toPath());
            // tryInjectMachineRecipes must guard against null from Supplier.get()
            // when registries aren't populated yet.
            int methodIdx = content.indexOf("private static void tryInjectMachineRecipes");
            assertTrue(methodIdx >= 0, "tryInjectMachineRecipes method must exist");
            int bodyStart = content.indexOf('{', methodIdx);
            int injectionEnd = content.indexOf("state.markMachinesInjected", methodIdx);
            int bodyEnd = injectionEnd > 0
                    ? content.indexOf(';', injectionEnd) + 80
                    : bodyStart + 500;
            String body = content.substring(bodyStart, Math.min(bodyEnd, content.length()));

            assertTrue(body.contains("recipeType == null") || body.contains("== null"),
                    "tryInjectMachineRecipes must guard against null RecipeType from Supplier.get()");
            assertTrue(body.contains("continue"),
                    "Must skip entries with null RecipeType, not attempt injection");
            assertTrue(body.contains("def.tenType.get()") || body.contains(".get()"),
                    "Must call Supplier.get() lazily at injection time, not use a pre-resolved type");
        }
    }

    @Nested
    class CacheStructure {

        @Test
        void cacheUsesJeiSyncState() throws Exception {
            var content = Files.readString(CACHE_SRC.toPath());
            assertTrue(content.contains("JeiSyncState"),
                    "TENClientRecipeCache must reference JeiSyncState");
        }

        @Test
        void cacheIsReadyRedirectedToState() throws Exception {
            var content = Files.readString(CACHE_SRC.toPath());
            // isReady no longer on cache; it's on JeiSyncState
            assertFalse(content.contains("private static volatile boolean ready"),
                    "ready field should be on JeiSyncState, not directly on cache");
        }

        @Test
        void cacheHasSessionGate() throws Exception {
            var content = Files.readString(CACHE_SRC.toPath());
            assertTrue(content.contains("onPlayerLogin"),
                    "Must handle login event");
            assertTrue(content.contains("onPlayerLogout"),
                    "Must handle logout event");
            assertTrue(content.contains("isSessionAccepting"),
                    "Must check session gate before accepting recipe events");
        }

        @Test
        void recipeEventNullGuard() throws Exception {
            var content = Files.readString(CACHE_SRC.toPath());
            assertTrue(content.contains("eventTypes == null"),
                    "Must guard against null event.getRecipeTypes()");
            assertTrue(content.contains("recipeMap == null"),
                    "Must guard against null event.getRecipeMap()");
        }

        @Test
        void suppressWarningsNarrowed() throws Exception {
            var content = Files.readString(CACHE_SRC.toPath());
            // Must NOT have method-level @SuppressWarnings({"unchecked", "rawtypes"})
            int methIdx = content.indexOf("private static List<FormsCombinedRecipe> collectRecipes");
            assertTrue(methIdx >= 0);
            // Look for the annotation on the line before the method
            String beforeMethod = content.substring(0, methIdx);
            int lastAnnotation = beforeMethod.lastIndexOf("@SuppressWarnings");
            if (lastAnnotation >= 0) {
                String annotLine = beforeMethod.substring(lastAnnotation,
                        beforeMethod.indexOf('\n', lastAnnotation));
                // Only suppression is "rawtypes" at method level
                assertTrue(annotLine.contains("rawtypes"),
                        "Method-level @SuppressWarnings must only suppress rawtypes");
                assertFalse(annotLine.contains("unchecked"),
                        "unchecked suppression must be local, not at method level");
            }
        }
    }

    @Nested
    class ServerSyncStructure {

        @Test
        void recipeSyncInitIdempotent() throws Exception {
            var src = new File(
                    "src/main/java/com/modularmc/ten/network/TENRecipeSync.java");
            var content = Files.readString(src.toPath());
            assertTrue(content.contains("AtomicBoolean"),
                    "TENRecipeSync.init() must use AtomicBoolean for idempotency");
        }

        @Test
        void recipeSyncSendsVanillaTypes() throws Exception {
            var src = new File(
                    "src/main/java/com/modularmc/ten/network/TENRecipeSync.java");
            var content = Files.readString(src.toPath());
            assertTrue(content.contains("RecipeType.SMELTING"),
                    "Must send SMELTING to client");
            assertTrue(content.contains("RecipeType.BLASTING"),
                    "Must send BLASTING to client");
            assertTrue(content.contains("RecipeType.SMOKING"),
                    "Must send SMOKING to client");
        }
    }

    @Nested
    class SyncStateStructure {

        @Test
        void syncStateHasAllRequiredMethods() throws Exception {
            var src = new File(
                    "src/main/java/com/modularmc/ten/network/JeiSyncState.java");
            var content = Files.readString(src.toPath());
            assertTrue(content.contains("tryClaimInject"),
                    "Must have tryClaimInject for machine injection guard");
            assertTrue(content.contains("markMachinesInjected"),
                    "Must have markMachinesInjected");
            assertTrue(content.contains("activateRuntime"),
                    "Must have activateRuntime");
            assertTrue(content.contains("deactivateRuntime"),
                    "Must have deactivateRuntime");
            assertTrue(content.contains("startSession"),
                    "Must have startSession for login gate");
            assertTrue(content.contains("endSession"),
                    "Must have endSession for logout gate");
            assertTrue(content.contains("acceptSnapshot"),
                    "Must have acceptSnapshot");
            assertTrue(content.contains("isSessionAccepting"),
                    "Must have isSessionAccepting for late event rejection");
        }
    }
}
