package com.modularmc.ten.api.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

public interface RandRecipe extends IBaseRecipeCm {

    default List<ItemStack> generateItems() {
        List<ItemStack> list = new ArrayList<>();
        for (var ing : output()) {
            // 仅物品条目参与物品生成（26.1.2 对齐：按 form 过滤，流体条目不调 genItem）
            if ("item".equals(ing.form())) list.add(ing.genItem());
        }
        return list;
    }

    default List<FluidStack> generateFluids() {
        List<FluidStack> list = new ArrayList<>();
        for (var ing : output()) {
            // 仅流体条目参与流体生成（26.1.2 对齐）
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
