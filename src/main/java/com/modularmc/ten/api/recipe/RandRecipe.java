package com.modularmc.ten.api.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 带随机产出语义的配方接口：按条目 form 过滤生成物品/流体产出，
 * 符号物品取首个输出条目（供 JEI/EMI 展示）。
 */
public interface RandRecipe extends IBaseRecipeCm {

    default List<ItemStack> generateItems() {
        List<ItemStack> list = new ArrayList<>();
        for (var ing : output()) {
            // 仅物品条目参与物品生成（按 form 过滤，流体条目不调 genItem）
            if ("item".equals(ing.form())) list.add(ing.genItem());
        }
        return list;
    }

    default List<FluidStack> generateFluids() {
        List<FluidStack> list = new ArrayList<>();
        for (var ing : output()) {
            // 仅流体条目参与流体生成
            if ("fluid".equals(ing.form())) list.add(ing.genFluid());
        }
        return list;
    }

    List<FormsCombinedIngredient> output();

    @Override
    default ItemStack getResultItem(HolderLookup.Provider registries) {
        return output().isEmpty() ? ItemStack.EMPTY : output().get(0).symbolItem();
    }
}
