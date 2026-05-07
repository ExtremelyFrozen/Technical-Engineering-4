package com.modularmc.ten.common.data;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.recipe.FormsCombinedRecipe;
import com.modularmc.ten.api.recipe.FormsCombinedRecipeSerializer;
import com.modularmc.ten.api.recipe.RecipeTypeCm;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class TENRecipeTypes {

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, TEN.MOD_ID);
    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, TEN.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, FormsCombinedRecipeSerializer<FormsCombinedRecipe>> PULVERIZER_S;
    public static final DeferredHolder<RecipeSerializer<?>, FormsCombinedRecipeSerializer<FormsCombinedRecipe>> COMPRESSOR_S;
    public static final DeferredHolder<RecipeSerializer<?>, FormsCombinedRecipeSerializer<FormsCombinedRecipe>> PSIONICANT_S;
    public static final DeferredHolder<RecipeSerializer<?>, FormsCombinedRecipeSerializer<FormsCombinedRecipe>> INDUCTION_FURNACE_S;
    public static final DeferredHolder<RecipeSerializer<?>, FormsCombinedRecipeSerializer<FormsCombinedRecipe>> REFINER_S;

    public static final DeferredHolder<RecipeType<?>, RecipeType<FormsCombinedRecipe>> PULVERIZER_T;
    public static final DeferredHolder<RecipeType<?>, RecipeType<FormsCombinedRecipe>> COMPRESSOR_T;
    public static final DeferredHolder<RecipeType<?>, RecipeType<FormsCombinedRecipe>> PSIONICANT_T;
    public static final DeferredHolder<RecipeType<?>, RecipeType<FormsCombinedRecipe>> INDUCTION_FURNACE_T;
    public static final DeferredHolder<RecipeType<?>, RecipeType<FormsCombinedRecipe>> REFINER_T;

    static {
        PULVERIZER_S = SERIALIZERS.register("pulverizer", () -> new FormsCombinedRecipeSerializer<>(FormsCombinedRecipe::new, 1, 4));
        COMPRESSOR_S = SERIALIZERS.register("compressor", () -> new FormsCombinedRecipeSerializer<>(FormsCombinedRecipe::new, 2, 1));
        PSIONICANT_S = SERIALIZERS.register("psionicant", () -> new FormsCombinedRecipeSerializer<>(FormsCombinedRecipe::new, 2, 1));
        INDUCTION_FURNACE_S = SERIALIZERS.register("induction_furnace", () -> new FormsCombinedRecipeSerializer<>(FormsCombinedRecipe::new, 3, 1));
        REFINER_S = SERIALIZERS.register("refiner", () -> new FormsCombinedRecipeSerializer<>(FormsCombinedRecipe::new, 2, 2));

        PULVERIZER_T = TYPES.register("pulverizer", () -> new RecipeTypeCm<>("pulverizer"));
        COMPRESSOR_T = TYPES.register("compressor", () -> new RecipeTypeCm<>("compressor"));
        PSIONICANT_T = TYPES.register("psionicant", () -> new RecipeTypeCm<>("psionicant"));
        INDUCTION_FURNACE_T = TYPES.register("induction_furnace", () -> new RecipeTypeCm<>("induction_furnace"));
        REFINER_T = TYPES.register("refiner", () -> new RecipeTypeCm<>("refiner"));
    }

    public static void init() {}
}
