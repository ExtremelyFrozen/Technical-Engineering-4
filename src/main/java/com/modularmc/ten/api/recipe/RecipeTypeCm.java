package com.modularmc.ten.api.recipe;

import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * RecipeType 简单实现：仅持有注册 path 供注册表与日志显示，
 * 匹配逻辑全部在机器侧自定义 matches 中。
 */
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
