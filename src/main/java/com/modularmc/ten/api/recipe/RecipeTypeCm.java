package com.modularmc.ten.api.recipe;

import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;

public class RecipeTypeCm<T extends Recipe<?>> implements RecipeType<T> {

    private final String path;

    public RecipeTypeCm(String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return path;
    }
}
