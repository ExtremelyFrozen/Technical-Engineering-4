package com.modularmc.ten.api.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;

public interface IBaseRecipeCm extends Recipe<RecipeInput> {

    int time();

    int inputLimit(ItemStack stack);

    int inputLimit(FluidStack stack);

    default boolean isSpecial() {
        return true;
    }

    default boolean canCraftInDimensions(int w, int h) {
        return true;
    }
}
