package com.modularmc.ten.api.recipe;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link EngineFuelRecipe} 的序列化器：数据包走 {@link #CODEC}，网络同步走 {@link #STREAM_CODEC}。
 *
 * <p>
 * JSON 形状：
 *
 * <pre>{@code
 * {
 *   "type": "kenergyengineering:engine_fuel",
 *   "ingredient": { "item": "minecraft:coal" },   // 或 { "tag": "..." }
 *   "engines": ["extractor"],                      // 省略 = 所有引擎
 *   "duration": 1067,                              // 持续时间 tick
 *   "rate_multiplier": 1.0                         // 对引擎额定的 FE/t 修正
 * }
 * }</pre>
 */
public class EngineFuelRecipeSerializer implements RecipeSerializer<EngineFuelRecipe> {

    public static final MapCodec<EngineFuelRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC.fieldOf("ingredient").forGetter(EngineFuelRecipe::ingredient),
            Codec.STRING.listOf().optionalFieldOf("engines", List.of()).forGetter(EngineFuelRecipe::engines),
            Codec.INT.fieldOf("duration").forGetter(EngineFuelRecipe::duration),
            Codec.DOUBLE.optionalFieldOf("rate_multiplier", 1.0).forGetter(EngineFuelRecipe::rateMultiplier))
            .apply(instance, EngineFuelRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, EngineFuelRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, EngineFuelRecipe::ingredient,
            ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8), EngineFuelRecipe::engines,
            ByteBufCodecs.VAR_INT, EngineFuelRecipe::duration,
            ByteBufCodecs.DOUBLE, EngineFuelRecipe::rateMultiplier,
            EngineFuelRecipe::new);

    @Override
    public MapCodec<EngineFuelRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, EngineFuelRecipe> streamCodec() {
        return STREAM_CODEC;
    }
}
