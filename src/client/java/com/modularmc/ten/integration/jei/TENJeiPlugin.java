package com.modularmc.ten.integration.jei;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.recipe.FormsCombinedRecipe;
import com.modularmc.ten.client.TENClientRecipeCache;
import com.modularmc.ten.common.blockentity.EngineFuelRecipe;
import com.modularmc.ten.common.blockentity.MatchFuel;
import com.modularmc.ten.common.blockentity.machine.BiomassBlockEntity;
import com.modularmc.ten.common.blockentity.machine.ExtractorBlockEntity;
import com.modularmc.ten.common.blockentity.machine.MetalizerBlockEntity;
import com.modularmc.ten.common.data.TENBlocks;
import com.modularmc.ten.common.data.TENRecipeTypes;
import com.modularmc.ten.network.JeiSyncState;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.client.event.RecipesUpdatedEvent;
import net.neoforged.neoforge.common.NeoForge;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerScreen;
import com.lowdragmc.lowdraglib2.integration.xei.jei.ModularUIJEIHandlers;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 升级版 JEI 插件（深度集成）：
 * <ul>
 *   <li>Smelter 三独立页（smelting/blasting/smoking）</li>
 *   <li>引擎燃料三分类（Extractor/Metalizer/Biomass）</li>
 *   <li>机器配方经 TENClientRecipeCache + JeiSyncState 状态机注入</li>
 *   <li>1.21.1 适配：RecipesUpdatedEvent 替代 RecipesReceivedEvent；JEI 19.x API</li>
 * </ul>
 */
@JeiPlugin
public class TENJeiPlugin implements IModPlugin {

    private static final ResourceLocation UID = TEN.id("jei_plugin");

    // ── Smelter JEI recipe types (three independent pages) ────────────
    public static final mezz.jei.api.recipe.RecipeType<SmelterJeiCategory.Recipe> SMELTER_SMELTING =
            new mezz.jei.api.recipe.RecipeType<>(TEN.id("smelter_smelting"), SmelterJeiCategory.Recipe.class);
    public static final mezz.jei.api.recipe.RecipeType<SmelterJeiCategory.Recipe> SMELTER_BLASTING =
            new mezz.jei.api.recipe.RecipeType<>(TEN.id("smelter_blasting"), SmelterJeiCategory.Recipe.class);
    public static final mezz.jei.api.recipe.RecipeType<SmelterJeiCategory.Recipe> SMELTER_SMOKING =
            new mezz.jei.api.recipe.RecipeType<>(TEN.id("smelter_smoking"), SmelterJeiCategory.Recipe.class);

    // ── Machine JEI recipe type definitions ──────────────────────────
    private record MachineTypeDef(
            mezz.jei.api.recipe.RecipeType<FormsCombinedRecipe> jeiType,
            Supplier<RecipeType<FormsCombinedRecipe>> tenType) {}

    private static final List<MachineTypeDef> MACHINE_TYPES = buildMachineTypeDefs();

    // ── Engine fuel recipe types ─────────────────────────────────────
    public static final mezz.jei.api.recipe.RecipeType<EngineFuelRecipe> EXTRACTOR_FUEL =
            new mezz.jei.api.recipe.RecipeType<>(TEN.id("extractor_fuel"), EngineFuelRecipe.class);
    public static final mezz.jei.api.recipe.RecipeType<EngineFuelRecipe> METALIZER_FUEL =
            new mezz.jei.api.recipe.RecipeType<>(TEN.id("metalizer_fuel"), EngineFuelRecipe.class);
    public static final mezz.jei.api.recipe.RecipeType<EngineFuelRecipe> BIOMASS_FUEL =
            new mezz.jei.api.recipe.RecipeType<>(TEN.id("biomass_fuel"), EngineFuelRecipe.class);

    // ═══════════════════════════════════════════════════════════════════
    //  Runtime coordination
    // ═══════════════════════════════════════════════════════════════════

    private static volatile IRecipeManager activeRecipeManager;
    private static volatile IRecipeManager smelterPopulatedManager;
    private static volatile boolean cacheListenerRegistered;
    private static volatile boolean cacheListenerActive;

    // ── Static helpers ───────────────────────────────────────────────

    private static ItemStack icon(String name) {
        var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(TEN.id(name));
        return item != null ? new ItemStack(item) : new ItemStack(Items.FURNACE);
    }

    private static ItemStack engineIcon(String engineName) {
        String blockId = "engine_" + engineName;
        var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(TEN.id(blockId));
        return item != null ? new ItemStack(item) : new ItemStack(Items.FURNACE);
    }

    static ItemStack smelterIcon() { return icon("machine_smelter"); }

    private record CategoryDef(String name, ResourceLocation id, ItemStack icon) {}

    private List<CategoryDef> buildCategories() {
        return List.of(
                new CategoryDef("Pulverizer", TEN.id("pulverizer"), icon("machine_pulverizer")),
                new CategoryDef("Compressor", TEN.id("compressor"), icon("machine_compressor")),
                new CategoryDef("Refiner", TEN.id("refiner"), icon("machine_refiner")),
                new CategoryDef("Induction Furnace", TEN.id("induction_furnace"), icon("machine_induction_furnace")),
                new CategoryDef("Psionicant", TEN.id("psionicant"), icon("machine_psionicant")));
    }

    private static List<MachineTypeDef> buildMachineTypeDefs() {
        return List.of(
                new MachineTypeDef(
                        new mezz.jei.api.recipe.RecipeType<>(TEN.id("pulverizer"), FormsCombinedRecipe.class),
                        TENRecipeTypes.PULVERIZER_T::get),
                new MachineTypeDef(
                        new mezz.jei.api.recipe.RecipeType<>(TEN.id("compressor"), FormsCombinedRecipe.class),
                        TENRecipeTypes.COMPRESSOR_T::get),
                new MachineTypeDef(
                        new mezz.jei.api.recipe.RecipeType<>(TEN.id("refiner"), FormsCombinedRecipe.class),
                        TENRecipeTypes.REFINER_T::get),
                new MachineTypeDef(
                        new mezz.jei.api.recipe.RecipeType<>(TEN.id("induction_furnace"), FormsCombinedRecipe.class),
                        TENRecipeTypes.INDUCTION_FURNACE_T::get),
                new MachineTypeDef(
                        new mezz.jei.api.recipe.RecipeType<>(TEN.id("psionicant"), FormsCombinedRecipe.class),
                        TENRecipeTypes.PSIONICANT_T::get)
        );
    }

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return UID;
    }

    // ── Category Registration ────────────────────────────────────────────
    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        if (!TEN.Mods.isJEILoaded()) return;

        IGuiHelper helper = registration.getJeiHelpers().getGuiHelper();

        for (var data : buildCategories()) {
            var type = new mezz.jei.api.recipe.RecipeType<FormsCombinedRecipe>(data.id, FormsCombinedRecipe.class);
            registration.addRecipeCategories(new TENJeiCategory(helper, data.id, type, data.icon));
        }

        registration.addRecipeCategories(
                new SmelterJeiCategory(helper, TEN.id("smelter_smelting"), SMELTER_SMELTING, smelterIcon()),
                new SmelterJeiCategory(helper, TEN.id("smelter_blasting"), SMELTER_BLASTING, smelterIcon()),
                new SmelterJeiCategory(helper, TEN.id("smelter_smoking"), SMELTER_SMOKING, smelterIcon())
        );

        registration.addRecipeCategories(
                new EngineFuelCategory(helper, TEN.id("extractor_fuel"), EXTRACTOR_FUEL,
                        engineIcon("extraction"),
                        TENBlocks.ENGINE_EXTRACTION.get().getName()),
                new EngineFuelCategory(helper, TEN.id("metalizer_fuel"), METALIZER_FUEL,
                        engineIcon("metal"),
                        TENBlocks.ENGINE_METAL.get().getName()),
                new EngineFuelCategory(helper, TEN.id("biomass_fuel"), BIOMASS_FUEL,
                        engineIcon("biomass"),
                        TENBlocks.ENGINE_BIOMASS.get().getName())
        );

        TEN.LOGGER.info("TEN JEI plugin categories registered — uid: {}", UID);
    }

    // ── Recipe Registration ──────────────────────────────────────────────
    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (!TEN.Mods.isJEILoaded()) return;

        var ingredientManager = registration.getIngredientManager();
        if (ingredientManager == null) return;

        var allStacks = ingredientManager.getAllItemStacks();
        if (allStacks == null) return;

        List<EngineFuelRecipe> extractorRecipes = new ArrayList<>();
        List<EngineFuelRecipe> metalizerRecipes = new ArrayList<>();
        List<EngineFuelRecipe> biomassRecipes = new ArrayList<>();

        for (ItemStack stack : allStacks) {
            if (stack == null || stack.isEmpty()) continue;

            int extractorValue = MatchFuel.getExtractorFuelValue(stack);
            if (extractorValue > 0) {
                extractorRecipes.add(new EngineFuelRecipe(stack, extractorValue, ExtractorBlockEntity.BASE_GENERATION_RATE));
            }

            int metalValue = MatchFuel.getMetalFuelValue(stack);
            if (metalValue > 0) {
                metalizerRecipes.add(new EngineFuelRecipe(stack, metalValue, MetalizerBlockEntity.BASE_GENERATION_RATE));
            }

            int biomassValue = MatchFuel.getBiomassFuelValue(stack);
            if (biomassValue > 0) {
                biomassRecipes.add(new EngineFuelRecipe(stack, biomassValue, BiomassBlockEntity.BASE_GENERATION_RATE));
            }
        }

        Comparator<EngineFuelRecipe> byItemIdThenComponents = Comparator.comparing(
                (EngineFuelRecipe r) -> r.ingredients().get(0).getItem().toString());

        registration.addRecipes(EXTRACTOR_FUEL, dedupSorted(extractorRecipes, byItemIdThenComponents));
        registration.addRecipes(METALIZER_FUEL, dedupSorted(metalizerRecipes, byItemIdThenComponents));
        registration.addRecipes(BIOMASS_FUEL, dedupSorted(biomassRecipes, byItemIdThenComponents));
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Runtime Available — cache→runtime bridge
    // ═══════════════════════════════════════════════════════════════════

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        if (!TEN.Mods.isJEILoaded()) return;
        IRecipeManager recipeManager = jeiRuntime.getRecipeManager();

        if (recipeManager != activeRecipeManager) {
            if (activeRecipeManager != null) {
                deactivateRuntime();
            }
            activeRecipeManager = recipeManager;
            TENClientRecipeCache.getState().activateRuntime();
        }

        // ── Smelter recipes (independent of cache readiness) ─────────
        if (smelterPopulatedManager != recipeManager) {
            injectSmelterRecipes(recipeManager);
            smelterPopulatedManager = recipeManager;
        }

        if (!cacheListenerRegistered) {
            NeoForge.EVENT_BUS.addListener(TENJeiPlugin::onRecipesUpdated);
            cacheListenerRegistered = true;
        }
        cacheListenerActive = true;

        tryInjectMachineRecipes();
    }

    private static void deactivateRuntime() {
        activeRecipeManager = null;
        smelterPopulatedManager = null;
        cacheListenerActive = false;
        TENClientRecipeCache.getState().deactivateRuntime();
    }

    // ── Cache → Runtime bridge listener ─────────────────────────────

    private static void onRecipesUpdated(RecipesUpdatedEvent event) {
        if (!cacheListenerActive) return;
        // Smelter recipes: level is guaranteed present on RecipesUpdatedEvent
        IRecipeManager manager = activeRecipeManager;
        if (manager != null && smelterPopulatedManager != manager) {
            injectSmelterRecipes(manager);
            smelterPopulatedManager = manager;
        }
        tryInjectMachineRecipes();
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Machine recipe injection (via cache)
    // ═══════════════════════════════════════════════════════════════════

    private static void tryInjectMachineRecipes() {
        JeiSyncState state = TENClientRecipeCache.getState();
        if (!state.tryClaimInject()) return;

        IRecipeManager manager = activeRecipeManager;
        if (manager == null) return;

        for (var def : MACHINE_TYPES) {
            var recipeType = def.tenType.get();
            if (recipeType == null) {
                TEN.LOGGER.warn("Skipping machine {} — RecipeType not yet registered",
                        def.jeiType.getUid());
                continue;
            }
            List<FormsCombinedRecipe> cached = TENClientRecipeCache.getMachineRecipes(recipeType);
            if (!cached.isEmpty()) {
                manager.addRecipes(def.jeiType, cached);
            }
        }

        state.markMachinesInjected();

        int totalRecipes = MACHINE_TYPES.stream()
                .mapToInt(def -> TENClientRecipeCache.getMachineRecipes(def.tenType.get()).size())
                .sum();
        TEN.LOGGER.info("TEN JEI machine recipes injected — generation: {}, total recipes: {}",
                state.getGeneration(), totalRecipes);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Smelter recipe injection (from builtin vanilla recipes)
    // ═══════════════════════════════════════════════════════════════════

    private static void injectSmelterRecipes(IRecipeManager recipeManager) {
        var smelting = collectFromBuiltin(recipeManager, net.minecraft.world.item.crafting.RecipeType.SMELTING);
        var blasting = collectFromBuiltin(recipeManager, net.minecraft.world.item.crafting.RecipeType.BLASTING);
        var smoking = collectFromBuiltin(recipeManager, net.minecraft.world.item.crafting.RecipeType.SMOKING);

        recipeManager.addRecipes(SMELTER_SMELTING, smelting);
        recipeManager.addRecipes(SMELTER_BLASTING, blasting);
        recipeManager.addRecipes(SMELTER_SMOKING, smoking);

        TEN.LOGGER.info("TEN JEI smelter recipes injected — smelting: {}, blasting: {}, smoking: {}",
                smelting.size(), blasting.size(), smoking.size());
    }

    private static List<SmelterJeiCategory.Recipe> collectFromBuiltin(
            IRecipeManager recipeManager,
            net.minecraft.world.item.crafting.RecipeType<? extends net.minecraft.world.item.crafting.AbstractCookingRecipe> builtinType) {
        var level = Minecraft.getInstance().level;
        if (level == null) return List.of();
        var registryAccess = level.registryAccess();
        @SuppressWarnings({"rawtypes", "unchecked"})
        List<RecipeHolder<net.minecraft.world.item.crafting.AbstractCookingRecipe>> allRecipes =
                (List<RecipeHolder<net.minecraft.world.item.crafting.AbstractCookingRecipe>>) (List) level.getRecipeManager().getAllRecipesFor((net.minecraft.world.item.crafting.RecipeType) builtinType);
        return allRecipes.stream()
                .map(RecipeHolder::value)
                .map(recipe -> {
                    var ingredient = recipe.getIngredients().get(0);
                    if (ingredient == null || ingredient.isEmpty()) return null;
                    ItemStack output = recipe.getResultItem(registryAccess);
                    if (output.isEmpty()) return null;
                    return SmelterJeiCategory.Recipe.of(ingredient, output, recipe.getCookingTime());
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private static List<EngineFuelRecipe> dedupSorted(List<EngineFuelRecipe> recipes, Comparator<EngineFuelRecipe> sortKey) {
        if (recipes.isEmpty()) return List.of();
        recipes.sort(sortKey);
        List<EngineFuelRecipe> result = new ArrayList<>();
        result.add(recipes.get(0));
        for (int i = 1; i < recipes.size(); i++) {
            ItemStack current = recipes.get(i).ingredients().get(0);
            ItemStack last = result.get(result.size() - 1).ingredients().get(0);
            if (!ItemStack.isSameItemSameComponents(current, last)) {
                result.add(recipes.get(i));
            }
        }
        return result;
    }

    // ── Catalyst Registration ────────────────────────────────────────────
    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        if (!TEN.Mods.isJEILoaded()) return;

        for (var data : buildCategories()) {
            var type = new mezz.jei.api.recipe.RecipeType<FormsCombinedRecipe>(data.id, FormsCombinedRecipe.class);
            registration.addRecipeCatalyst(data.icon, type);
        }

        registration.addRecipeCatalyst(smelterIcon(), SMELTER_SMELTING);
        registration.addRecipeCatalyst(smelterIcon(), SMELTER_BLASTING);
        registration.addRecipeCatalyst(smelterIcon(), SMELTER_SMOKING);

        registration.addRecipeCatalyst(engineIcon("extraction"), EXTRACTOR_FUEL);
        registration.addRecipeCatalyst(engineIcon("metal"), METALIZER_FUEL);
        registration.addRecipeCatalyst(engineIcon("biomass"), BIOMASS_FUEL);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        if (!TEN.Mods.isJEILoaded()) return;
        registration.addGuiContainerHandler(ModularUIContainerScreen.class, ModularUIJEIHandlers.GUI_CONTAINER_HANDLER);
    }
}