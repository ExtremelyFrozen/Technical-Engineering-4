package com.modularmc.ten.api.recipe;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * 配方工厂 SPI：序列化器解析完材料列表后经此回调创建配方实例。
 */
public interface IFactoryCm<T extends FormsCombinedRecipe> {

    T create(ResourceLocation regName, ResourceLocation idIn,
             List<FormsCombinedIngredient> ip,
             List<FormsCombinedIngredient> op, int cookTimeIn);
}
