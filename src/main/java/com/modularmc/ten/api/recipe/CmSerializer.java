package com.modularmc.ten.api.recipe;

import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * TEN 配方序列化器 SPI：在原版 RecipeSerializer 之上约定统一兜底时长
 * （fallBackTime）与注册 id 命名。
 */
public interface CmSerializer<T extends FormsCombinedRecipe> extends RecipeSerializer<T> {

    int fallBackTime = 150;

    String id();
}
