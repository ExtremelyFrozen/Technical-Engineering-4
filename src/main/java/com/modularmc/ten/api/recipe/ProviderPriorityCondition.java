package com.modularmc.ten.api.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.modularmc.ten.TEN;
import com.modularmc.ten.config.TENConfig;

import net.neoforged.neoforge.common.conditions.ICondition;

import java.util.List;

/**
 * 材料归属优先级条件：
 * {@code {"type":"kenergyengineering:provider_priority","mod":"mekanism","candidates":["kenergyengineering","mekanism"]}}。
 * <p>
 * 同一种材料（粗料、粉）往往有多个模组都能提供。数据侧给每个候选模组各写一条配方，输出都是
 * <b>具体物品</b>（标签输出按标准处理会得到空产物），靠这条条件在加载期互斥：
 * {@code candidates} 按 {@code compat.providerPriority} 排序后，第一个已加载的模组才是赢家，
 * 其余同组变体整条不进配方表。于是「装了哪些邻居」与「配置里的顺序」共同决定归属，
 * 邻居增减或改顺序都不必重新生成数据。
 * <p>
 * 赢家按候选组独立计算：{@code c:ores/X → 粗料} 与 {@code c:raw_materials/X → 粉} 两段各自选人，
 * 所以粗料来自 A 模组、粉来自 B 模组是可以接受的（两段都吃标签输入，接得上）。
 */
public record ProviderPriorityCondition(String mod, List<String> candidates) implements ICondition {

    public static final MapCodec<ProviderPriorityCondition> CODEC = RecordCodecBuilder.mapCodec(
            builder -> builder.group(
                            Codec.STRING.fieldOf("mod").forGetter(ProviderPriorityCondition::mod),
                            Codec.STRING.listOf().fieldOf("candidates").forGetter(ProviderPriorityCondition::candidates))
                    .apply(builder, ProviderPriorityCondition::new));

    @Override
    public boolean test(IContext context) {
        return mod.equals(TENConfig.COMPAT.firstProvider(candidates));
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }

    @Override
    public String toString() {
        return TEN.MOD_ID + ":provider_priority(" + mod + " of " + candidates + ")";
    }
}
