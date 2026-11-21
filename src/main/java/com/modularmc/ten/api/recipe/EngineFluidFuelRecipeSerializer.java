package com.modularmc.ten.api.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link EngineFluidFuelRecipe} 的序列化器：数据包走 {@link #CODEC}，网络同步走 {@link #STREAM_CODEC}。
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
public class EngineFluidFuelRecipeSerializer implements RecipeSerializer<EngineFluidFuelRecipe> {

    public static final MapCodec<EngineFluidFuelRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            FluidIngredient.CODEC.fieldOf("ingredient").forGetter(EngineFluidFuelRecipe::ingredient),
            Codec.STRING.listOf().optionalFieldOf("engines", List.of()).forGetter(EngineFluidFuelRecipe::engines),
            Codec.INT.fieldOf("duration").forGetter(EngineFluidFuelRecipe::duration),
            Codec.DOUBLE.optionalFieldOf("rate_multiplier", 1.0).forGetter(EngineFluidFuelRecipe::rateMultiplier))
            .apply(instance, EngineFluidFuelRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, EngineFluidFuelRecipe> STREAM_CODEC = StreamCodec.composite(
            FluidIngredient.STREAM_CODEC, EngineFluidFuelRecipe::ingredient,
            ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8), EngineFluidFuelRecipe::engines,
            ByteBufCodecs.VAR_INT, EngineFluidFuelRecipe::duration,
            ByteBufCodecs.DOUBLE, EngineFluidFuelRecipe::rateMultiplier,
            EngineFluidFuelRecipe::new);

    @Override
    public MapCodec<EngineFluidFuelRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, EngineFluidFuelRecipe> streamCodec() {
        return STREAM_CODEC;
    }
}
