package com.modularmc.ten.integration.kubejs;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.recipe.FormsCombinedIngredient;

import dev.latvian.mods.kubejs.recipe.component.ListRecipeComponent;
import dev.latvian.mods.kubejs.recipe.component.NumberComponent;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchemaRegistry;

/**
 * 五个带数据包配方的机器的 KubeJS schema。
 * <p>
 * 它们的 JSON 结构完全同构（{@code inputs} / {@code outputs} / {@code time}），差别只在
 * 序列化器接受的槽位数（pulverizer 1→4、compressor 2→1、psionicant 2→1、
 * induction_furnace 3→1、refiner 2→2），槽位补齐由 {@code FormsCombinedRecipeSerializer} 负责，
 * schema 不需要重复表达。
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
    }

    private static RecipeSchema schema() {
        return new RecipeSchema(
                INPUTS.inputKey("inputs"),
                OUTPUTS.outputKey("outputs"),
                NumberComponent.INT.otherKey("time"));
    }

    private TENRecipeSchemas() {}
}
