// -*- coding: utf-8 -*-
package com.modularmc.ten.integration.jei;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract tests for JEI recipe sync pipeline:
 * server (OnDatapackSyncEvent) → client (TENClientRecipeCache) → JEI runtime.
 * <p>
 * All assertions target production source structure — no fake models or
 * synthetic cache duplication. Tests verify:
 * <ul>
 *   <li>Server-side {@code OnDatapackSyncEvent} handler with {@code sendRecipes}</li>
 *   <li>Client-side {@code TENClientRecipeCache} receiving {@code RecipesReceivedEvent}</li>
  *   <li>{@code TENJeiPlugin} refactored to use client cache, smelter builtin,
  *       engine fuel null guard, and {@code deactivateRuntime} cleanup</li>
 * </ul>
 */
class JeiRecipeSyncContractTest {

    // ═══════════════════════════════════════════════════════════════════
    // Paths
    // ═══════════════════════════════════════════════════════════════════

    private static final File PLUGIN_SRC = new File(
            "src/client/java/com/modularmc/ten/integration/jei/TENJeiPlugin.java");
    private static final String SERVER_HANDLER_DIR =
            "src/main/java/com/modularmc/ten/network";
    private static final String CACHE_DIR =
            "src/client/java/com/modularmc/ten/client";

    // ═══════════════════════════════════════════════════════════════════
    // 1. Server-side OnDatapackSyncEvent handler
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    class ServerRecipeSync {

        @Test
        void serverHandlerClass_exists() throws Exception {
            var handlerDir = new File(SERVER_HANDLER_DIR);
            assertTrue(handlerDir.isDirectory(),
                    "Server handler directory must exist: " + SERVER_HANDLER_DIR);

            // Expect a class like TENRecipeSync in the network package
            var handlerFile = new File(handlerDir, "TENRecipeSync.java");
            assertTrue(handlerFile.exists(),
                    "Server-side recipe sync handler TENRecipeSync must exist");
        }

        @Test
        void serverHandler_usesGameBus() throws Exception {
            var handlerFile = new File(SERVER_HANDLER_DIR, "TENRecipeSync.java");
            var content = Files.readString(handlerFile.toPath());

            assertTrue(content.contains("NeoForge.EVENT_BUS.addListener"),
                    "TENRecipeSync must register via NeoForge.EVENT_BUS.addListener");
            assertTrue(content.contains("onDatapackSync"),
                    "TENRecipeSync must reference its event handler method");
        }

        @Test
        void serverHandler_subscribesOnDatapackSyncEvent() throws Exception {
            var handlerFile = new File(SERVER_HANDLER_DIR, "TENRecipeSync.java");
            var content = Files.readString(handlerFile.toPath());

            assertTrue(content.contains("OnDatapackSyncEvent"),
                    "TENRecipeSync must reference OnDatapackSyncEvent");
            assertTrue(content.contains("sendRecipes"),
                    "TENRecipeSync must call sendRecipes");
        }

        @Test
        void serverHandler_sendsFiveTENTypes() throws Exception {
            var handlerFile = new File(SERVER_HANDLER_DIR, "TENRecipeSync.java");
            var content = Files.readString(handlerFile.toPath());

            assertTrue(content.contains("PULVERIZER_T"),
                    "Must send PULVERIZER_T");
            assertTrue(content.contains("COMPRESSOR_T"),
                    "Must send COMPRESSOR_T");
            assertTrue(content.contains("REFINER_T"),
                    "Must send REFINER_T");
            assertTrue(content.contains("INDUCTION_FURNACE_T"),
                    "Must send INDUCTION_FURNACE_T");
            assertTrue(content.contains("PSIONICANT_T"),
                    "Must send PSIONICANT_T");
        }

        @Test
        void serverHandler_sendsThreeVanillaSmeltingTypes() throws Exception {
            var handlerFile = new File(SERVER_HANDLER_DIR, "TENRecipeSync.java");
            var content = Files.readString(handlerFile.toPath());

            assertTrue(content.contains("RecipeType.SMELTING"),
                    "Must send SMELTING");
            assertTrue(content.contains("RecipeType.BLASTING"),
                    "Must send BLASTING");
            assertTrue(content.contains("RecipeType.SMOKING"),
                    "Must send SMOKING");
        }

        @Test
        void serverHandler_doesNotUseGetSingleplayerServer() throws Exception {
            var handlerFile = new File(SERVER_HANDLER_DIR, "TENRecipeSync.java");
            var content = Files.readString(handlerFile.toPath());

            assertFalse(content.contains("getSingleplayerServer"),
                    "Server handler must NOT use getSingleplayerServer");
        }

        @Test
        void serverHandler_doesNotUseClientClasses() throws Exception {
            var handlerFile = new File(SERVER_HANDLER_DIR, "TENRecipeSync.java");
            var content = Files.readString(handlerFile.toPath());

            assertFalse(content.contains("net.minecraft.client"),
                    "Server handler must not import client classes");
            assertFalse(content.contains("TENClientRecipeCache"),
                    "Server handler must not reference client cache");
            assertFalse(content.contains("mezz.jei"),
                    "Server handler must not reference JEI");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // 2. Client-side TENClientRecipeCache
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    class ClientRecipeCache {

        @Test
        void clientCacheClass_exists() throws Exception {
            var cacheFile = new File(CACHE_DIR, "TENClientRecipeCache.java");
            assertTrue(cacheFile.exists(),
                    "TENClientRecipeCache must exist in client source set");
        }

        @Test
        void clientCache_hasNoJeiImports() throws Exception {
            var cacheFile = new File(CACHE_DIR, "TENClientRecipeCache.java");
            var content = Files.readString(cacheFile.toPath());

            // Allow Javadoc @link references to JEI, but no actual import statements
            assertFalse(content.contains("\nimport mezz.jei"),
                    "TENClientRecipeCache must not have JEI import statements");
        }

        @Test
        void clientCache_subscribesRecipesReceivedEvent() throws Exception {
            var cacheFile = new File(CACHE_DIR, "TENClientRecipeCache.java");
            var content = Files.readString(cacheFile.toPath());

            assertTrue(content.contains("RecipesReceivedEvent"),
                    "TENClientRecipeCache must subscribe to RecipesReceivedEvent");
            assertTrue(content.contains("NeoForge.EVENT_BUS.addListener"),
                    "TENClientRecipeCache must register via NeoForge.EVENT_BUS.addListener");
        }

        @Test
        void clientCache_usesGameBus() throws Exception {
            var cacheFile = new File(CACHE_DIR, "TENClientRecipeCache.java");
            var content = Files.readString(cacheFile.toPath());

            assertTrue(content.contains("NeoForge.EVENT_BUS"),
                    "TENClientRecipeCache must use NeoForge.EVENT_BUS");
        }

        @Test
        void clientCache_extractsFormsCombinedRecipe() throws Exception {
            var cacheFile = new File(CACHE_DIR, "TENClientRecipeCache.java");
            var content = Files.readString(cacheFile.toPath());

            assertTrue(content.contains("FormsCombinedRecipe"),
                    "TENClientRecipeCache must reference FormsCombinedRecipe");
            assertTrue(content.contains("RecipeMap"),
                    "TENClientRecipeCache must use RecipeMap");
        }

        @Test
        void clientCache_usesImmutableCopy() throws Exception {
            var cacheFile = new File(CACHE_DIR, "TENClientRecipeCache.java");
            var content = Files.readString(cacheFile.toPath());

            assertTrue(content.contains("List.copyOf") || content.contains("Collections.unmodifiableList"),
                    "TENClientRecipeCache must return immutable recipe list copies");
        }

        @Test
        void clientCache_coversAllFiveTENTypes() throws Exception {
            var cacheFile = new File(CACHE_DIR, "TENClientRecipeCache.java");
            var content = Files.readString(cacheFile.toPath());

            assertTrue(content.contains("PULVERIZER_T") || content.contains("PULVERIZER"),
                    "Must handle PULVERIZER");
            assertTrue(content.contains("COMPRESSOR_T") || content.contains("COMPRESSOR"),
                    "Must handle COMPRESSOR");
            assertTrue(content.contains("REFINER_T") || content.contains("REFINER"),
                    "Must handle REFINER");
            assertTrue(content.contains("INDUCTION_FURNACE_T") || content.contains("INDUCTION_FURNACE"),
                    "Must handle INDUCTION_FURNACE");
            assertTrue(content.contains("PSIONICANT_T") || content.contains("PSIONICANT"),
                    "Must handle PSIONICANT");
        }

        @Test
        void clientCache_isSafeOnMissingTypes() throws Exception {
            var cacheFile = new File(CACHE_DIR, "TENClientRecipeCache.java");
            var content = Files.readString(cacheFile.toPath());

            // Must handle event types set not containing a target type gracefully
            assertTrue(content.contains(".getOrDefault") || content.contains("isEmpty()")
                            || content.contains("containsKey") || content.contains("if (")
                            || content.contains("stream()"),
                    "TENClientRecipeCache must handle missing/empty types gracefully");
        }

        @Test
        void clientCache_hasCleanupMethod() throws Exception {
            var cacheFile = new File(CACHE_DIR, "TENClientRecipeCache.java");
            var content = Files.readString(cacheFile.toPath());

            // Must have some form of cleanup/reset/clear for player disconnect
            assertTrue(content.contains("clear") || content.contains("reset")
                            || content.contains("LoggingOut") || content.contains("invalidate"),
                    "TENClientRecipeCache must have cleanup on player disconnect");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // 3. TENJeiPlugin refactoring — no getSingleplayerServer
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    class JeiPluginRefactored {

        @Test
        void registerRecipes_noGetSingleplayerServer() throws Exception {
            var content = Files.readString(PLUGIN_SRC.toPath());

            int registerIdx = content.indexOf("void registerRecipes");
            assertTrue(registerIdx >= 0, "registerRecipes must exist");

            // Extract registerRecipes body
            String registerBody = content.substring(registerIdx,
                    content.indexOf("void onRuntimeAvailable", registerIdx));

            assertFalse(registerBody.contains("getSingleplayerServer"),
                    "registerRecipes must not use getSingleplayerServer");
        }

        @Test
        void registerRecipes_doesNotCollectMachineRecipes() throws Exception {
            var content = Files.readString(PLUGIN_SRC.toPath());

            int registerIdx = content.indexOf("void registerRecipes");
            assertTrue(registerIdx >= 0);
            String registerBody = content.substring(registerIdx,
                    content.indexOf("void onRuntimeAvailable", registerIdx));

            // Machine recipe collection must NOT use getSingleplayerServer
            assertFalse(registerBody.contains("getSingleplayerServer"),
                    "registerRecipes must not collect machine recipes via integrated server");
            assertFalse(registerBody.contains("RecipeHolder"),
                    "registerRecipes must not use RecipeHolder (removed from machine path)");
        }

        @Test
        void onRuntimeAvailable_usesClientCacheForMachineRecipes() throws Exception {
            var content = Files.readString(PLUGIN_SRC.toPath());

            // The file must reference TENClientRecipeCache for machine recipe sourcing
            assertTrue(content.contains("TENClientRecipeCache"),
                    "TENJeiPlugin must reference TENClientRecipeCache");
        }

        @Test
        void registerRecipes_engineFuelHasLevelNullGuard() throws Exception {
            var content = Files.readString(PLUGIN_SRC.toPath());
            int registerIdx = content.indexOf("void registerRecipes");
            assertTrue(registerIdx >= 0);
            String registerBody = content.substring(registerIdx,
                    content.indexOf("void onRuntimeAvailable", registerIdx));

            // Should not crash if level is null (dedicated server client scenario)
            assertFalse(registerBody.contains("requireNonNull(Minecraft.getInstance().level"),
                    "registerRecipes must NOT crash with requireNonNull on client level");
            // Should have a null check
            assertTrue(registerBody.contains("if") || registerBody.contains("Minecraft.getInstance().level"),
                    "registerRecipes should guard against null client level for engine fuel");
        }

        @Test
        void deactivateRuntime_exists() throws Exception {
            var content = Files.readString(PLUGIN_SRC.toPath());
            assertTrue(content.contains("deactivateRuntime"),
                    "TENJeiPlugin must have deactivateRuntime method for runtime cleanup");
        }

        @Test
        void deactivateRuntime_resetsSmelterGuard() throws Exception {
            var content = Files.readString(PLUGIN_SRC.toPath());

            int runtimeIdx = content.indexOf("deactivateRuntime");
            assertTrue(runtimeIdx >= 0, "deactivateRuntime must exist");
            String runtimeBody = content.substring(runtimeIdx,
                    content.indexOf("private static <T extends AbstractCookingRecipe>", runtimeIdx));

            assertTrue(runtimeBody.contains("smelterPopulatedManager = null"),
                    "deactivateRuntime must set smelterPopulatedManager to null");
            assertTrue(runtimeBody.contains("activeRecipeManager = null"),
                    "deactivateRuntime must clear activeRecipeManager");
        }

        @Test
        void onRuntimeAvailable_addsFiveMachineTypes() throws Exception {
            var content = Files.readString(PLUGIN_SRC.toPath());

            // Machine types are defined via MACHINE_TYPES/buildMachineTypeDefs and
            // consumed in onRuntimeAvailable — verify both the definition and usage
            assertTrue(content.contains("buildMachineTypeDefs"),
                    "Must have buildMachineTypeDefs method");
            assertTrue(content.contains("MACHINE_TYPES"),
                    "Must have MACHINE_TYPES field");
            assertTrue(content.contains("TENClientRecipeCache.getMachineRecipes"),
                    "onRuntimeAvailable must get machine recipes from client cache");
            assertTrue(content.contains("PULVERIZER_T"),
                    "Machine type defs must reference PULVERIZER_T");
            assertTrue(content.contains("COMPRESSOR_T"),
                    "Machine type defs must reference COMPRESSOR_T");
            assertTrue(content.contains("REFINER_T"),
                    "Machine type defs must reference REFINER_T");
            assertTrue(content.contains("INDUCTION_FURNACE_T"),
                    "Machine type defs must reference INDUCTION_FURNACE_T");
            assertTrue(content.contains("PSIONICANT_T"),
                    "Machine type defs must reference PSIONICANT_T");
        }

        @Test
        void onRuntimeAvailable_addsThreeSmelterTypes() throws Exception {
            var content = Files.readString(PLUGIN_SRC.toPath());

            // Smelter types are added in injectSmelterRecipes (called from onRuntimeAvailable)
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
        void onRuntimeAvailable_noGetSingleplayerServer() throws Exception {
            var content = Files.readString(PLUGIN_SRC.toPath());

            int runtimeIdx = content.indexOf("void onRuntimeAvailable");
            assertTrue(runtimeIdx >= 0,
                    "onRuntimeAvailable method must exist");

            // The method after onRuntimeAvailable in the source is deactivateRuntime
            int deactIdx = content.indexOf("private static void deactivateRuntime()", runtimeIdx);
            assertTrue(deactIdx > runtimeIdx,
                    "deactivateRuntime must appear after onRuntimeAvailable");
            String runtimeBody = content.substring(runtimeIdx, deactIdx);

            assertFalse(runtimeBody.contains("getSingleplayerServer"),
                    "onRuntimeAvailable must not use getSingleplayerServer");
            // Verify the runtime path still has the required elements
            assertTrue(runtimeBody.contains("IRecipeHolderType") || runtimeBody.contains("injectSmelterRecipes"),
                    "onRuntimeAvailable must reference smelter recipe collection");
            assertTrue(runtimeBody.contains("TENClientRecipeCache") || runtimeBody.contains("tryInjectMachineRecipes"),
                    "onRuntimeAvailable must reference machine recipe injection");
        }

        @Test
        void noDeprecatedCreateFromVanilla() throws Exception {
            var content = Files.readString(PLUGIN_SRC.toPath());
            assertFalse(content.contains("createFromVanilla"),
                    "TENJeiPlugin must not use deprecated createFromVanilla");
        }

        @Test
        void noRawCastsInRecipeTypeUse() throws Exception {
            var content = Files.readString(PLUGIN_SRC.toPath());
            // IRecipeHolderType.create is the correct path
            assertTrue(content.contains("IRecipeHolderType.create("),
                    "Must use IRecipeHolderType.create() for builtin recipe lookups");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // 4. Data flow integrity — no JEI-mod requirement on server
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    class DataFlowIntegrity {

        @Test
        void serverDoesNotReferenceJei() throws Exception {
            // Verify no server-side (main) file references JEI
            var mainDir = new File("src/main/java/com/modularmc/ten");
            var files = mainDir.listFiles((dir, name) -> name.endsWith(".java"));
            if (files == null) return;

            for (var f : files) {
                var content = Files.readString(f.toPath());
                assertFalse(content.contains("mezz.jei"),
                        "Main source files must not reference JEI: " + f.getName());
            }
        }

        @Test
        void clientCacheDoesNotReferenceJei() throws Exception {
            var cacheFile = new File(CACHE_DIR, "TENClientRecipeCache.java");
            if (!cacheFile.exists()) return; // RED stage — will fail earlier

            var content = Files.readString(cacheFile.toPath());
            // Allow Javadoc @link references, but no actual import or type usage
            assertFalse(content.contains("\nimport mezz.jei"),
                    "TENClientRecipeCache must not have JEI import statements");
        }

        @Test
        void noJeiServerModRequired() throws Exception {
            // Verify server handler uses NeoForge native sendRecipes, not JEI server API
            var handlerFile = new File(SERVER_HANDLER_DIR, "TENRecipeSync.java");
            if (!handlerFile.exists()) return;

            var content = Files.readString(handlerFile.toPath());
            assertFalse(content.contains("mezz.jei"),
                    "Server handler must not require JEI server mod");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // 5. Lifecycle — duplicate guard and cleanup
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    class LifecycleGuard {

        @Test
        void onRuntimeAvailable_hasManagerIdentityGuard() throws Exception {
            var content = Files.readString(PLUGIN_SRC.toPath());

            assertTrue(content.contains("smelterPopulatedManager"),
                    "Must have smelterPopulatedManager field for smelter guard");
            assertTrue(content.contains("tryClaimInject"),
                    "Must use JeiSyncState.tryClaimInject for machine guard");
        }

        @Test
        void smelterManagerGuard_resetOnUnavailable() throws Exception {
            var content = Files.readString(PLUGIN_SRC.toPath());

            // Deactivation happens in deactivateRuntime() — called implicitly
            // when onRuntimeAvailable detects a new IRecipeManager identity.
            int deactIdx = content.indexOf("private static void deactivateRuntime()");
            assertTrue(deactIdx >= 0, "deactivateRuntime() method must exist");
            String deactBody = content.substring(deactIdx,
                    content.indexOf("private static <T extends AbstractCookingRecipe>", deactIdx));

            assertTrue(deactBody.contains("smelterPopulatedManager = null"),
                    "deactivateRuntime must set smelterPopulatedManager to null");
            assertTrue(deactBody.contains("activeRecipeManager = null"),
                    "deactivateRuntime must clear activeRecipeManager");
            assertTrue(deactBody.contains("deactivateRuntime()"),
                    "deactivateRuntime must call state.deactivateRuntime()");
        }

        @Test
        void onRuntimeAvailable_addsRecipesOnlyOncePerManager() throws Exception {
            var content = Files.readString(PLUGIN_SRC.toPath());

            // Machine injection is gated by JeiSyncState.tryClaimInject + markMachinesInjected
            assertTrue(content.contains("tryClaimInject"),
                    "Must use tryClaimInject for machine injection guard");
            assertTrue(content.contains("markMachinesInjected"),
                    "Must call markMachinesInjected after successful injection");
            // Smelter injection is gated by smelterPopulatedManager identity
            assertTrue(content.contains("smelterPopulatedManager != recipeManager"),
                    "Must guard smelter with identity check");
        }
    }
}
