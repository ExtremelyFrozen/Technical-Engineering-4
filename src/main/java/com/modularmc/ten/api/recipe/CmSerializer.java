package com.modularmc.ten.api.recipe;

import net.minecraft.world.item.crafting.RecipeSerializer;

public interface CmSerializer<T extends FormsCombinedRecipe> extends RecipeSerializer<T> {

    int fallBackTime = 150;

    String id();
}
