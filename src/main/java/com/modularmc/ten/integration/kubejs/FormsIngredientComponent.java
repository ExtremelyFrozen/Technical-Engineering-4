package com.modularmc.ten.integration.kubejs;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.recipe.FormsCombinedIngredient;
import com.modularmc.ten.api.recipe.FormsCombinedRecipeSerializer;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;

import dev.latvian.mods.kubejs.recipe.RecipeScriptContext;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentType;
import dev.latvian.mods.kubejs.util.JsonUtils;
import dev.latvian.mods.rhino.type.TypeInfo;

/**
 * KubeJS 侧的「材料条目」组件，直接复用 {@link FormsCombinedRecipeSerializer#INGREDIENT_CODEC}。
 * <p>
 * 该 codec 产出的 JSON 就是配方里的原始形状（{@code form} / {@code type} / {@code key} /
 * {@code count} 或 {@code amount} / {@code chance} / {@code rolls}），与 KER 的序列化器完全一致，
 * 所以不需要额外写 {@code RecipeMapping} 做形状转换。
 * <p>
 * {@link #wrap} 必须重写：默认实现走 {@code Context.jsToJava(from, typeInfo())}，Rhino 无法把
 * JS 对象直接构造成 {@link FormsCombinedIngredient}（没有对应构造器），实测会抛
 * 「Cannot convert {form: item, ...} to FormsCombinedIngredient」。这里改成两步：
 * 脚本值先转 JSON，再交给 codec 解析；纯字符串走简写解析。
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

    /**
     * 接受三种写法：
     * <ul>
     * <li>完整对象：{@code {form:'item', type:'static', key:'minecraft:coal', count:2}}</li>
     * <li>物品/标签：{@code 'minecraft:coal'}、{@code '#c:ores/iron'}</li>
     * <li>带数量：{@code '2x minecraft:coal'}、{@code '4x #c:ingots/iron'}</li>
     * </ul>
     * 流体只能走完整对象（需要 {@code form:'fluid'} 与 {@code amount}）。
     */
    @Override
    public FormsCombinedIngredient wrap(RecipeScriptContext cx, Object from) {
        if (from instanceof FormsCombinedIngredient ingredient) {
            return ingredient;
        }
        if (from instanceof CharSequence text) {
            return parseShorthand(text.toString());
        }
        JsonElement json = JsonUtils.of(cx.cx(), from);
        return codec().parse(JsonOps.INSTANCE, json)
                .getOrThrow(message -> new IllegalArgumentException("材料条目无法解析：" + from + " —— " + message));
    }

    private static FormsCombinedIngredient parseShorthand(String raw) {
        String text = raw.trim();
        int count = 1;
        int x = text.indexOf('x');
        if (x > 0) {
            try {
                count = Integer.parseInt(text.substring(0, x).trim());
                text = text.substring(x + 1).trim();
            } catch (NumberFormatException ignored) {
                // 前缀不是数字，按原样当作 id 处理
            }
        }
        boolean tag = text.startsWith("#");
        if (tag) {
            text = text.substring(1);
        }
        return FormsCombinedIngredient.create(count, "item", tag ? "tag" : "static", text, 1.0, 1);
    }
}
