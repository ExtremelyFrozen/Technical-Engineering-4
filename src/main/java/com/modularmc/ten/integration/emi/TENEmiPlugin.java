package com.modularmc.ten.integration.emi;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.recipe.FormsCombinedRecipe;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;

@EmiEntrypoint
public class TENEmiPlugin implements EmiPlugin {

    private record CategoryDef(String name, ResourceLocation id) {}

    private static final java.util.List<CategoryDef> CATEGORIES = java.util.List.of(
            new CategoryDef("Pulverizer", TEN.id("pulverizer")),
            new CategoryDef("Compressor", TEN.id("compressor")),
            new CategoryDef("Refiner", TEN.id("refiner")),
            new CategoryDef("induction_furnace", TEN.id("induction_furnace")),
            new CategoryDef("Psionicant", TEN.id("psionicant")));

    private static EmiStack icon(String name) {
        var item = BuiltInRegistries.ITEM.get(TEN.id(name));
        return item != null ? EmiStack.of(item) : EmiStack.EMPTY;
    }

    @Override
    public void register(EmiRegistry registry) {
        var level = registry.getRecipeManager();
        if (level == null) return;

        for (var def : CATEGORIES) {
            var icon = icon(def.name().toLowerCase());
            var category = new EmiRecipeCategory(def.id(), icon);
            registry.addCategory(category);
            registry.addWorkstation(category, icon);

            for (var entry : level.getRecipes()) {
                if (entry.value() instanceof FormsCombinedRecipe r) {
                    if (r.getType() != null && r.getType().toString().equals(def.id().getPath())) {
                        registry.addRecipe(new TENEmiRecipe(category, r));
                    }
                }
            }
        }
    }
}
