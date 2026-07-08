package com.modularmc.ten.api.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

public interface RandRecipe extends IBaseRecipeCm {

    default List<ItemStack> generateItems() {
        List<ItemStack> list = new ArrayList<>();
        for (var ing : output()) list.add(ing.genItem());
        return list;
    }

    default List<FluidStack> generateFluids() {
        List<FluidStack> list = new ArrayList<>();
        for (var ing : output()) list.add(ing.genFluid());
        return list;
    }

    List<FormsCombinedIngredient> output();

    @Override
    default ItemStack getResultItem(HolderLookup.Provider registries) {
        return output().isEmpty() ? ItemStack.EMPTY : output().get(0).symbolItem();
    }
}
