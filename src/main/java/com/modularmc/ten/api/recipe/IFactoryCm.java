package com.modularmc.ten.api.recipe;

import net.minecraft.resources.Identifier;

import java.util.List;

public interface IFactoryCm<T extends FormsCombinedRecipe> {

    T create(Identifier regName, Identifier idIn,
             List<FormsCombinedIngredient> ip,
             List<FormsCombinedIngredient> op, int cookTimeIn);
}
