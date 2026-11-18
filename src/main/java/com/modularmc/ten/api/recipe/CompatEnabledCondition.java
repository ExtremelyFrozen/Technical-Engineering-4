package com.modularmc.ten.api.recipe;

import com.modularmc.ten.TEN;
import com.modularmc.ten.config.TENConfig;

import net.neoforged.neoforge.common.conditions.ICondition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * 邻居兼容层开关条件：{@code {"type":"kenergyengineering:compat_enabled","source":"enderio"}}。
 * <p>
 * 之所以要自定义条件而不是运行期摘除配方：NeoForge 1.21.1 只有 {@code RecipesUpdatedEvent}
 * 这种加载完成后的通知，没有受支持的加载期增删入口。条件在加载期求值，
 * 关掉即整条配方不进配方表，JEI 也就无从显示——「关闭」与「隐藏」因此是同一件事。
 */
public record CompatEnabledCondition(String source) implements ICondition {

    public static final MapCodec<CompatEnabledCondition> CODEC = RecordCodecBuilder.mapCodec(
            builder -> builder.group(
                    com.mojang.serialization.Codec.STRING.fieldOf("source").forGetter(CompatEnabledCondition::source))
                    .apply(builder, CompatEnabledCondition::new));

    @Override
    public boolean test(IContext context) {
        return TENConfig.COMPAT.enabled(source);
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }

    @Override
    public String toString() {
        return TEN.MOD_ID + ":compat_enabled(" + source + ")";
    }
}
