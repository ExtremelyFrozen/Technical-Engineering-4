package com.modularmc.ten.integration.kubejs;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.recipe.FormsCombinedIngredient;
import com.modularmc.ten.api.recipe.FormsCombinedRecipeSerializer;

import com.mojang.serialization.Codec;

import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentType;
import dev.latvian.mods.rhino.type.TypeInfo;

/**
 * KubeJS 侧的「材料条目」组件，直接复用 {@link FormsCombinedRecipeSerializer#INGREDIENT_CODEC}。
 * <p>
 * 该 codec 产出的 JSON 就是配方里的原始形状（{@code form} / {@code type} / {@code key} /
 * {@code count} 或 {@code amount} / {@code chance} / {@code rolls}），与 KER 的序列化器完全一致，
 * 所以不需要额外写 {@code RecipeMapping} 做形状转换。
 * <p>
 * 脚本里仍用 KubeJS 的惯用写法：物品写 {@code 'minecraft:coal'}、标签写 {@code '#c:ores/iron'}，
 * 由 {@link #wrap} 的默认实现把字符串交给 codec 解析成 {@link FormsCombinedIngredient}。
 */
public record FormsIngredientComponent(RecipeComponentType<?> typeOverride) implements RecipeComponent<FormsCombinedIngredient> {

    public static final RecipeComponentType<FormsCombinedIngredient> TYPE =
            RecipeComponentType.unit(TEN.id("forms_ingredient"), FormsIngredientComponent::new);

    @Override
    public RecipeComponentType<?> type() {
        return typeOverride;
    }

    @Override
    public Codec<FormsCombinedIngredient> codec() {
        return FormsCombinedRecipeSerializer.INGREDIENT_CODEC;
    }

    @Override
    public TypeInfo typeInfo() {
        return TypeInfo.of(FormsCombinedIngredient.class);
    }
}
