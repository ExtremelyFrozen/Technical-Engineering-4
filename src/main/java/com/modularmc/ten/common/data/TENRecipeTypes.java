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

    // Helper serializer factories (keep private, use via asSerializer())
    private static FormsCombinedRecipeSerializer<FormsCombinedRecipe> PULVERIZER_S0;
    private static FormsCombinedRecipeSerializer<FormsCombinedRecipe> COMPRESSOR_S0;
    private static FormsCombinedRecipeSerializer<FormsCombinedRecipe> PSIONICANT_S0;
    private static FormsCombinedRecipeSerializer<FormsCombinedRecipe> INDUCTION_FURNACE_S0;
    private static FormsCombinedRecipeSerializer<FormsCombinedRecipe> REFINER_S0;

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FormsCombinedRecipe>> PULVERIZER_S;
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FormsCombinedRecipe>> COMPRESSOR_S;
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FormsCombinedRecipe>> PSIONICANT_S;
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FormsCombinedRecipe>> INDUCTION_FURNACE_S;
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FormsCombinedRecipe>> REFINER_S;

    public static final DeferredHolder<RecipeType<?>, RecipeType<FormsCombinedRecipe>> PULVERIZER_T;
    public static final DeferredHolder<RecipeType<?>, RecipeType<FormsCombinedRecipe>> COMPRESSOR_T;
    public static final DeferredHolder<RecipeType<?>, RecipeType<FormsCombinedRecipe>> PSIONICANT_T;
    public static final DeferredHolder<RecipeType<?>, RecipeType<FormsCombinedRecipe>> INDUCTION_FURNACE_T;
    public static final DeferredHolder<RecipeType<?>, RecipeType<FormsCombinedRecipe>> REFINER_T;

    static {
        PULVERIZER_T = TYPES.register("pulverizer", () -> new RecipeTypeCm<>("pulverizer"));
        COMPRESSOR_T = TYPES.register("compressor", () -> new RecipeTypeCm<>("compressor"));
        PSIONICANT_T = TYPES.register("psionicant", () -> new RecipeTypeCm<>("psionicant"));
        INDUCTION_FURNACE_T = TYPES.register("induction_furnace", () -> new RecipeTypeCm<>("induction_furnace"));
        REFINER_T = TYPES.register("refiner", () -> new RecipeTypeCm<>("refiner"));

        // Create serializer helpers
        PULVERIZER_S0 = new FormsCombinedRecipeSerializer<>(FormsCombinedRecipe::new, PULVERIZER_T::get, 1, 4);
        COMPRESSOR_S0 = new FormsCombinedRecipeSerializer<>(FormsCombinedRecipe::new, COMPRESSOR_T::get, 2, 1);
        PSIONICANT_S0 = new FormsCombinedRecipeSerializer<>(FormsCombinedRecipe::new, PSIONICANT_T::get, 2, 1);
        INDUCTION_FURNACE_S0 = new FormsCombinedRecipeSerializer<>(FormsCombinedRecipe::new, INDUCTION_FURNACE_T::get, 3, 1);
        REFINER_S0 = new FormsCombinedRecipeSerializer<>(FormsCombinedRecipe::new, REFINER_T::get, 2, 2);

        // Register RecipeSerializer records
        PULVERIZER_S = SERIALIZERS.register("pulverizer", () -> PULVERIZER_S0.asSerializer());
        COMPRESSOR_S = SERIALIZERS.register("compressor", () -> COMPRESSOR_S0.asSerializer());
        PSIONICANT_S = SERIALIZERS.register("psionicant", () -> PSIONICANT_S0.asSerializer());
        INDUCTION_FURNACE_S = SERIALIZERS.register("induction_furnace", () -> INDUCTION_FURNACE_S0.asSerializer());
        REFINER_S = SERIALIZERS.register("refiner", () -> REFINER_S0.asSerializer());
    }

    public static void init() {}
}
