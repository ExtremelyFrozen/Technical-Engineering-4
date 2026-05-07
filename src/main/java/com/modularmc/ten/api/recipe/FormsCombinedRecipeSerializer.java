package com.modularmc.ten.api.recipe;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.RecipeSerializer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.MapCodec;

import java.util.ArrayList;
import java.util.List;

public class FormsCombinedRecipeSerializer<T extends FormsCombinedRecipe> implements RecipeSerializer<T> {

    private final IFactoryCm<T> factory;
    public final int sizeIn, sizeOut;

    public FormsCombinedRecipeSerializer(IFactoryCm<T> fac, int si, int so) {
        this.factory = fac;
        this.sizeIn = si;
        this.sizeOut = so;
    }

    @Override
    public MapCodec<T> codec() {
        return MapCodec.unit(null); // Custom JSON handled via fromJson-like approach
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
                    return factory.create(null, null, ip, op, cook);
                });
    }

    public T fromJson(ResourceLocation recipeId, JsonObject json) {
        List<FormsCombinedIngredient> ip = getInputs(json);
        List<FormsCombinedIngredient> op = getOutputs(json);
        int time = JsonParser.getIntOr(json, "time", 150);
        return factory.create(recipeId, recipeId, ip, op, time);
    }

    private List<FormsCombinedIngredient> getInputs(JsonObject json) {
        List<FormsCombinedIngredient> list = new ArrayList<>();
        JsonArray arr = GsonHelper.getAsJsonArray(json, "inputs");
        for (JsonElement e : arr) list.add(FormsCombinedIngredient.parseFrom(e.getAsJsonObject()));
        while (list.size() < sizeIn) list.add(EMPTY());
        return list;
    }

    private List<FormsCombinedIngredient> getOutputs(JsonObject json) {
        List<FormsCombinedIngredient> list = new ArrayList<>();
        JsonArray arr = GsonHelper.getAsJsonArray(json, "outputs");
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
}
