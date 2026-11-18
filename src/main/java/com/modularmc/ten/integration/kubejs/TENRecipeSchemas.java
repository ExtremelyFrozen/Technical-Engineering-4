package com.modularmc.ten.integration.kubejs;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.recipe.FormsCombinedIngredient;

import dev.latvian.mods.kubejs.recipe.component.IngredientComponent;
import dev.latvian.mods.kubejs.recipe.component.ListRecipeComponent;
import dev.latvian.mods.kubejs.recipe.component.NumberComponent;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import dev.latvian.mods.kubejs.recipe.component.StringComponent;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchemaRegistry;

import java.util.List;

/**
 * 六个数据包配方的 KubeJS schema。
 * <p>
 * 五台配方机器的 JSON 结构完全同构（{@code inputs} / {@code outputs} / {@code time}），差别只在
 * 序列化器接受的槽位数（pulverizer 1→4、compressor 2→1、psionicant 2→1、
 * induction_furnace 3→1、refiner 2→2），槽位补齐由 {@code FormsCombinedRecipeSerializer} 负责，
 * schema 不需要重复表达。第六个是生产端的 {@code engine_fuel}，结构另算，见
 * {@link #engineFuelSchema()}。
 */
public final class TENRecipeSchemas {

    private static final RecipeComponent<FormsCombinedIngredient> INGREDIENT = FormsIngredientComponent.TYPE.instance();
    private static final ListRecipeComponent<FormsCombinedIngredient> INPUTS = INGREDIENT.asList();
    private static final ListRecipeComponent<FormsCombinedIngredient> OUTPUTS = INGREDIENT.asList();

    public static void register(RecipeSchemaRegistry event) {
        event.register(TEN.id("pulverizer"), schema());
        event.register(TEN.id("compressor"), schema());
        event.register(TEN.id("psionicant"), schema());
        event.register(TEN.id("induction_furnace"), schema());
        event.register(TEN.id("refiner"), schema());
        event.register(TEN.id("engine_fuel"), engineFuelSchema());
    }

    private static RecipeSchema schema() {
        return new RecipeSchema(
                INPUTS.inputKey("inputs"),
                OUTPUTS.outputKey("outputs"),
                NumberComponent.INT.otherKey("time"));
    }

    /**
     * 引擎燃料：{@code ingredient} + {@code duration} + 可选 {@code engines} + 可选 {@code rate_multiplier}。
     *
     * <pre>{@code
     * event.custom({
     *   type: 'kenergyengineering:engine_fuel',
     *   ingredient: 'minecraft:coal',          // 或 '#minecraft:logs'
     *   duration: 1067,
     *   engines: ['extractor'],                 // 省略 = 所有引擎
     *   rate_multiplier: 1.0
     * })
     * }</pre>
     *
     * <p>
     * <b>键顺序不能随意调</b>：{@link RecipeSchema} 的构造函数会遍历键表，遇到第一个可选键就
     * 锁定「必填段」，之后再出现必填键会直接抛
     * {@code Required key 'xxx' must be ahead of optional keys!}，整个数据包加载失败。
     * 因此两个必填键 {@code ingredient} / {@code duration} 必须排在两个可选键之前。
     * JSON 是按键名取值的，这个顺序不影响数据包本身。
     */
    private static RecipeSchema engineFuelSchema() {
        return new RecipeSchema(
                IngredientComponent.INGREDIENT.instance().otherKey("ingredient"),
                NumberComponent.INT.otherKey("duration"),
                StringComponent.STRING.instance().asList().otherKey("engines").optional(List.<String>of()),
                NumberComponent.DOUBLE.otherKey("rate_multiplier").optional(1.0D));
    }

    private TENRecipeSchemas() {}
}
