package com.modularmc.ten.api.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * TEN 配方根基接口：声明 time() 与输入上限 inputLimit()，
 * 并为原版 Recipe 的网格/特殊性检查提供恒真默认实现。
 */
public interface IBaseRecipeCm extends Recipe<RecipeInput> {

    int time();

    int inputLimit(ItemStack stack);

    int inputLimit(FluidStack stack);

    @Override
    default boolean isSpecial() {
        return true;
    }

    @Override
    default boolean canCraftInDimensions(int w, int h) {
        return true;
    }
}
