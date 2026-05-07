package com.modularmc.ten.api.recipe;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class FormsCombinedRecipeSerializer<T extends FormsCombinedRecipe> implements RecipeSerializer<T> {

    private record IngredientData(String form, String type, ResourceLocation key, int count, int amount, double chance) {}

    private static final Codec<IngredientData> INGREDIENT_DATA_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("form").forGetter(IngredientData::form),
            Codec.STRING.fieldOf("type").forGetter(IngredientData::type),
            ResourceLocation.CODEC.fieldOf("key").forGetter(IngredientData::key),
            Codec.INT.optionalFieldOf("count", 1).forGetter(IngredientData::count),
            Codec.INT.optionalFieldOf("amount", 0).forGetter(IngredientData::amount),
            Codec.DOUBLE.optionalFieldOf("chance", 1.0d).forGetter(IngredientData::chance))
            .apply(instance, IngredientData::new));

    private static final Codec<FormsCombinedIngredient> INGREDIENT_CODEC = INGREDIENT_DATA_CODEC.xmap(
            data -> FormsCombinedIngredient.create(
                    "fluid".equals(data.form()) ? data.amount() : data.count(),
                    data.form(),
                    data.type(),
                    data.key().toString(),
                    data.chance()),
            ingredient -> new IngredientData(
                    ingredient.form(),
                    ingredient.type(),
                    ingredient.key(),
                    "fluid".equals(ingredient.form()) ? 1 : ingredient.amountOrCount(),
                    "fluid".equals(ingredient.form()) ? ingredient.amountOrCount() : 0,
                    ingredient.chance()));

    private final IFactoryCm<T> factory;
    private final Supplier<RecipeType<?>> recipeType;
    public final int sizeIn, sizeOut;
    private final MapCodec<T> codec;

    public FormsCombinedRecipeSerializer(IFactoryCm<T> fac, Supplier<RecipeType<?>> recipeType, int si, int so) {
        this.factory = fac;
        this.recipeType = recipeType;
        this.sizeIn = si;
        this.sizeOut = so;
        this.codec = RecordCodecBuilder.mapCodec(instance -> instance.group(
                INGREDIENT_CODEC.listOf().fieldOf("inputs").forGetter(recipe -> recipe.input()),
                INGREDIENT_CODEC.listOf().fieldOf("outputs").forGetter(recipe -> recipe.output()),
                Codec.INT.optionalFieldOf("time", 150).forGetter(FormsCombinedRecipe::time))
                .apply(instance, (inputs, outputs, time) -> createRecipe(null, null, padIngredients(inputs, sizeIn), padIngredients(outputs, sizeOut), time)));
    }

    @Override
    public MapCodec<T> codec() {
        return codec;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, T> streamCodec() {
        return StreamCodec.of(
                (buf, recipe) -> {
                    for (var ip : recipe.input) ip.writeTo(buf);
                    for (var op : recipe.output) op.writeTo(buf);
                    buf.writeInt(recipe.time);
                },
                buf -> {
                    List<FormsCombinedIngredient> ip = new ArrayList<>();
                    for (int i = 0; i < sizeIn; i++) ip.add(FormsCombinedIngredient.parseFrom(buf));
                    List<FormsCombinedIngredient> op = new ArrayList<>();
                    for (int i = 0; i < sizeOut; i++) op.add(FormsCombinedIngredient.parseFrom(buf));
                    int cook = buf.readInt();
                    return createRecipe(null, null, ip, op, cook);
                });
    }

    public T fromJson(ResourceLocation recipeId, JsonObject json) {
        List<FormsCombinedIngredient> ip = getInputs(json);
        List<FormsCombinedIngredient> op = getOutputs(json);
        int time = JsonParser.getIntOr(json, "time", 150);
        return createRecipe(recipeId, recipeId, ip, op, time);
    }

    private List<FormsCombinedIngredient> getInputs(JsonObject json) {
        List<FormsCombinedIngredient> list = new ArrayList<>();
        JsonArray arr = json.getAsJsonArray("inputs");
        for (JsonElement e : arr) list.add(FormsCombinedIngredient.parseFrom(e.getAsJsonObject()));
        while (list.size() < sizeIn) list.add(EMPTY());
        return list;
    }

    private List<FormsCombinedIngredient> getOutputs(JsonObject json) {
        List<FormsCombinedIngredient> list = new ArrayList<>();
        JsonArray arr = json.getAsJsonArray("outputs");
        for (JsonElement e : arr) {
            JsonObject o = e.getAsJsonObject();
            String form = JsonParser.getString(o, "form");
            String key = JsonParser.getString(o, "key");
            int count = "fluid".equals(form) ? JsonParser.getIntOr(o, "amount", 0) : JsonParser.getIntOr(o, "count", 1);
            double chance = JsonParser.getFloatOr(o, "chance", 1);
            list.add(FormsCombinedIngredient.create(count, form, "static", key, chance));
        }
        while (list.size() < sizeOut) list.add(EMPTY());
        return list;
    }

    private static FormsCombinedIngredient EMPTY() {
        var ig = FormsCombinedIngredient.create(0, "item", "static", "air", 1);
        ig.ALLOW_ALL = true;
        return ig;
    }

    private T createRecipe(ResourceLocation regName, ResourceLocation id, List<FormsCombinedIngredient> ip,
                           List<FormsCombinedIngredient> op, int time) {
        T recipe = factory.create(regName, id, ip, op, time);
        recipe.recipeType = recipeType.get();
        recipe.serializer = this;
        return recipe;
    }

    private static List<FormsCombinedIngredient> padIngredients(List<FormsCombinedIngredient> ingredients, int size) {
        List<FormsCombinedIngredient> padded = new ArrayList<>(ingredients);
        while (padded.size() < size) {
            padded.add(EMPTY());
        }
        return padded;
    }
}
