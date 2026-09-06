package com.modularmc.ten.integration.emi;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.recipe.FormsCombinedRecipe;
import com.modularmc.ten.common.data.TENBlocks;
import com.modularmc.ten.common.data.TENRecipeTypes;
import com.modularmc.ten.integration.xei.TENRecipeWidget;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerScreen;
import com.lowdragmc.lowdraglib2.integration.xei.emi.ModularUIEMIHandlers;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@EmiEntrypoint
public class TENEmiPlugin implements EmiPlugin {

    private record CategoryDef(ResourceLocation id,
                               DeferredHolder<RecipeType<?>, RecipeType<FormsCombinedRecipe>> recipeType,
                               Supplier<ItemStack> iconStack) {}

    private static final int RECIPE_LOG_SAMPLE_LIMIT = 5;

    private static final java.util.List<CategoryDef> CATEGORIES = java.util.List.of(
            new CategoryDef(TEN.id("pulverizer"), TENRecipeTypes.PULVERIZER_T, TENBlocks.MACHINE_PULVERIZER::asStack),
            new CategoryDef(TEN.id("compressor"), TENRecipeTypes.COMPRESSOR_T, TENBlocks.MACHINE_COMPRESSOR::asStack),
            new CategoryDef(TEN.id("refiner"), TENRecipeTypes.REFINER_T, TENBlocks.MACHINE_REFINER::asStack),
            new CategoryDef(TEN.id("induction_furnace"), TENRecipeTypes.INDUCTION_FURNACE_T, TENBlocks.MACHINE_INDUCTION_FURNACE::asStack),
            new CategoryDef(TEN.id("psionicant"), TENRecipeTypes.PSIONICANT_T, TENBlocks.MACHINE_PSIONICANT::asStack));

    private static final class TENEmiCategory extends EmiRecipeCategory {

        private final ResourceLocation id;
        private final Component titleOverride;

        private TENEmiCategory(ResourceLocation id, EmiStack icon) {
            this(id, icon, null);
        }

        private TENEmiCategory(ResourceLocation id, EmiStack icon, Component titleOverride) {
            super(id, icon);
            this.id = id;
            this.titleOverride = titleOverride;
        }

        @Override
        public Component getName() {
            // smelter/engine 燃料类用 JEI 同源 lang 键（kenergyengineering.jei.category.<path>）
            return titleOverride != null ? titleOverride : TENRecipeWidget.titleEmi(id);
        }
    }

    private static EmiStack icon(ItemStack stack) {
        return stack.isEmpty() ? EmiStack.EMPTY : EmiStack.of(stack);
    }

    @Override
    public void register(EmiRegistry registry) {
        registry.addExclusionArea(ModularUIContainerScreen.class, TENEmiPlugin::addModularExclusionArea);

        var allRecipeEntries = registry.getRecipeManager().getRecipes();
        var tenRecipeEntries = allRecipeEntries.stream()
                .filter(entry -> TEN.MOD_ID.equals(entry.id().getNamespace()))
                .toList();

        TEN.LOGGER.debug(
                "[EMI] RecipeManager currently has {} total recipes, {} from {}",
                allRecipeEntries.size(),
                tenRecipeEntries.size(),
                TEN.MOD_ID);
        for (var def : CATEGORIES) {
            int pathMatches = (int) tenRecipeEntries.stream()
                    .filter(entry -> entry.id().getPath().contains("/" + def.id().getPath() + "/"))
                    .count();
            TEN.LOGGER.debug(
                    "[EMI] Recipe path scan for {} matched {} entries under namespace {}",
                    def.id(),
                    pathMatches,
                    TEN.MOD_ID);
        }
        // Smelter — uses vanilla RecipeType.SMELTING
        registry.addWorkstation(dev.emi.emi.api.recipe.VanillaEmiRecipeCategories.SMELTING,
                dev.emi.emi.api.stack.EmiStack.of(com.modularmc.ten.common.data.TENBlocks.MACHINE_SMELTER));

        registerMachineCategories(registry);
        registerSmelterCategories(registry);
        registerEngineFuelCategories(registry);
    }

    /** 5 个 FormsCombinedRecipe 机器类别（既有机型）。 */
    private void registerMachineCategories(EmiRegistry registry) {
        TEN.LOGGER.debug("[EMI] Registering TEN EMI plugin with {} categories", CATEGORIES.size());
        for (var def : CATEGORIES) {
            var iconStack = def.iconStack().get();
            var icon = icon(iconStack);
            var category = new TENEmiCategory(def.id(), icon);
            var recipeType = def.recipeType().get();
            var recipes = registry.getRecipeManager().getAllRecipesFor(recipeType);

            TEN.LOGGER.debug(
                    "[EMI] Category {} -> recipeType={}, iconEmpty={}, iconItem={}, recipeCount={}",
                    def.id(),
                    recipeType,
                    icon == EmiStack.EMPTY,
                    iconStack,
                    recipes.size());

            registry.addCategory(category);
            registry.addWorkstation(category, icon);
            TEN.LOGGER.debug("[EMI] Category {} registered with workstation {}", def.id(), iconStack);

            int index = 0;
            for (var entry : recipes) {
                var recipe = entry.value();
                if (index < RECIPE_LOG_SAMPLE_LIMIT) {
                    TEN.LOGGER.debug(
                            "[EMI]   Recipe {} -> id={}, inputs(items={}, fluids={}), outputs(items={}, fluids={}), time={}",
                            def.id(),
                            entry.id(),
                            recipe.allInputItems().size(),
                            recipe.allInputFluids().size(),
                            recipe.allOutputItems().size(),
                            recipe.allOutputFluids().size(),
                            recipe.time());
                }
                registry.addRecipe(new TENEmiRecipe(def.id(), category, recipe));
                index++;
            }
            TEN.LOGGER.debug("[EMI] Category {} finished registering {} recipes", def.id(), index);
        }
    }

    private void registerSmelterCategories(EmiRegistry registry) {
        record SmelterDef(ResourceLocation id,
                          net.minecraft.world.item.crafting.RecipeType<? extends net.minecraft.world.item.crafting.AbstractCookingRecipe> builtinType) {}

        var defs = List.of(
                new SmelterDef(TEN.id("smelter_smelting"), net.minecraft.world.item.crafting.RecipeType.SMELTING),
                new SmelterDef(TEN.id("smelter_blasting"), net.minecraft.world.item.crafting.RecipeType.BLASTING),
                new SmelterDef(TEN.id("smelter_smoking"), net.minecraft.world.item.crafting.RecipeType.SMOKING));

        var icon = icon(com.modularmc.ten.common.data.TENBlocks.MACHINE_SMELTER.asStack());
        for (var def : defs) {
            // 标题复用 JEI 同源键 kenergyengineering.jei.category.smelter_*（TENLangHandler 已定义）
            var category = new TENEmiCategory(def.id(), icon,
                    Component.translatable(TEN.MOD_ID + ".jei.category." + def.id().getPath()));
            registry.addCategory(category);
            registry.addWorkstation(category, icon);

            var recipes = collectSmelterRecipes(def.builtinType());
            int index = 0;
            for (var r : recipes) {
                registry.addRecipe(new SmelterEmiRecipe(category, recipeId(def.id(), index++), r));
            }
            TEN.LOGGER.info("[EMI] Smelter category {} registered with {} recipes", def.id(), recipes.size());
        }
    }

    private void registerEngineFuelCategories(EmiRegistry registry) {
        record FuelDef(ResourceLocation id, ItemStack icon, Component title,
                       java.util.function.ToIntFunction<net.minecraft.world.item.ItemStack> fuelValue, int baseRate) {}

        var defs = List.of(
                new FuelDef(TEN.id("extractor_fuel"), engineIcon("extraction"),
                        com.modularmc.ten.common.data.TENBlocks.ENGINE_EXTRACTION.get().getName(),
                        com.modularmc.ten.common.blockentity.MatchFuel::getExtractorFuelValue,
                        com.modularmc.ten.common.blockentity.machine.ExtractorBlockEntity.BASE_GENERATION_RATE),
                new FuelDef(TEN.id("metalizer_fuel"), engineIcon("metal"),
                        com.modularmc.ten.common.data.TENBlocks.ENGINE_METAL.get().getName(),
                        com.modularmc.ten.common.blockentity.MatchFuel::getMetalFuelValue,
                        com.modularmc.ten.common.blockentity.machine.MetalizerBlockEntity.BASE_GENERATION_RATE),
                new FuelDef(TEN.id("biomass_fuel"), engineIcon("biomass"),
                        com.modularmc.ten.common.data.TENBlocks.ENGINE_BIOMASS.get().getName(),
                        com.modularmc.ten.common.blockentity.MatchFuel::getBiomassFuelValue,
                        com.modularmc.ten.common.blockentity.machine.BiomassBlockEntity.BASE_GENERATION_RATE));

        for (var def : defs) {
            // 标题与 JEI 同源（TENBlocks.ENGINE_*.getName()）；jei.category.fuel_* 键无 lang 定义
            var category = new TENEmiCategory(def.id(), icon(def.icon()),
                    def.title());
            registry.addCategory(category);
            registry.addWorkstation(category, icon(def.icon()));

            var recipes = new java.util.ArrayList<com.modularmc.ten.common.blockentity.EngineFuelRecipe>();
            for (var holder : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
                var stack = new net.minecraft.world.item.ItemStack(holder);
                if (stack.isEmpty()) continue;
                int value = def.fuelValue().applyAsInt(stack);
                if (value > 0) {
                    recipes.add(new com.modularmc.ten.common.blockentity.EngineFuelRecipe(stack, value, def.baseRate()));
                }
            }
            recipes.sort(java.util.Comparator.comparing(r -> r.ingredients().get(0).getItem().toString()));

            int index = 0;
            com.modularmc.ten.common.blockentity.EngineFuelRecipe last = null;
            for (var r : recipes) {
                if (last != null && net.minecraft.world.item.ItemStack.isSameItemSameComponents(
                        r.ingredients().get(0), last.ingredients().get(0)))
                    continue;
                registry.addRecipe(new EngineFuelEmiRecipe(category, recipeId(def.id(), index++), r));
                last = r;
            }
            TEN.LOGGER.info("[EMI] Engine fuel category {} registered with {} recipes", def.id(), index);
        }
    }

    /** 从 vanilla RecipeManager 采集烹饪配方为熔炼机 EMI 记录（与 JEI collectFromBuiltin 同源）。 */
    private List<SmelterEmiRecipe.RecipeData> collectSmelterRecipes(
                                                                    net.minecraft.world.item.crafting.RecipeType<? extends net.minecraft.world.item.crafting.AbstractCookingRecipe> builtinType) {
        var level = net.minecraft.client.Minecraft.getInstance().level;
        if (level == null) return List.of();
        var registryAccess = level.registryAccess();
        @SuppressWarnings({ "rawtypes", "unchecked" })
        List<net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.AbstractCookingRecipe>> all = (List) level.getRecipeManager().getAllRecipesFor((net.minecraft.world.item.crafting.RecipeType) builtinType);
        return all.stream()
                .map(net.minecraft.world.item.crafting.RecipeHolder::value)
                .map(r -> {
                    var ingredient = r.getIngredients().get(0);
                    if (ingredient == null || ingredient.isEmpty()) return null;
                    ItemStack output = r.getResultItem(registryAccess);
                    if (output.isEmpty()) return null;
                    return new SmelterEmiRecipe.RecipeData(
                            java.util.Arrays.stream(ingredient.getItems()).filter(s -> !s.isEmpty()).toList(), output.copy(), r.getCookingTime());
                })
                .filter(java.util.Objects::nonNull)
                // 边缘 modded Ingredient 过滤后可能为空列表：与 JEI Recipe.of 的 nonEmpty.isEmpty() 防御对齐
                .filter(r -> !r.inputs().isEmpty())
                .toList();
    }

    private static ItemStack engineIcon(String engineName) {
        String blockId = "engine_" + engineName;
        var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(TEN.id(blockId));
        return item != null ? new net.minecraft.world.item.ItemStack(item) : new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.FURNACE);
    }

    private static ResourceLocation recipeId(ResourceLocation categoryId, int index) {
        return TEN.id(categoryId.getPath() + "/" + index);
    }

    private static void addModularExclusionArea(ModularUIContainerScreen screen, Consumer<Bounds> consumer) {
        ModularUIEMIHandlers.EXCLUSION_AREA.addExclusionArea(screen, consumer);
    }
}
