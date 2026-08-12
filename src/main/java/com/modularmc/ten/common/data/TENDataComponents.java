package com.modularmc.ten.common.data;

import com.modularmc.ten.TEN;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;

import java.util.List;

/**
 * 模组 DataComponent 注册中枢（NeoForge 26.1.2 标准模式）。
 * <p>
 * 26.1.2 迁移规范：物品栈数据必须走 DataComponent 系统，禁止注册过时
 * NBT/CompoundTag 物品栈存储。此处集中注册自定义 {@link DataComponentType}。
 *
 * <ul>
 * <li>{@link #CHANNEL_CONFIG} — 频道连接器复制/应用的面配置载荷
 * （能量/物品/流体三组面模式 + 频道名）。</li>
 * </ul>
 */
public final class TENDataComponents {

    public static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, TEN.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ChannelConfigComponent>> CHANNEL_CONFIG = DATA_COMPONENTS.registerComponentType("channel_config",
            builder -> builder.persistent(ChannelConfigComponent.CODEC)
                    .networkSynchronized(ChannelConfigComponent.STREAM_CODEC));

    /**
     * 频道连接器配置载荷：按 {@link net.minecraft.core.Direction#values()} 序
     * （DOWN/UP/NORTH/SOUTH/WEST/EAST，即 3D 数据值 0..5）排列的三组面配置
     * （每组固定 6 项，对应 {@code energyFaceMode/itemFaceMode/fluidFaceMode}
     * 的 {@code Map<Direction,Integer>} 扁平化），外加可空频道名
     * （空串 = 未携带频道接入信息）。
     */
    public record ChannelConfigComponent(List<Integer> energyFaces, List<Integer> itemFaces,
                                         List<Integer> fluidFaces, String channelId) {

        public static final Codec<ChannelConfigComponent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.listOf().fieldOf("energyFaces").forGetter(ChannelConfigComponent::energyFaces),
                Codec.INT.listOf().fieldOf("itemFaces").forGetter(ChannelConfigComponent::itemFaces),
                Codec.INT.listOf().fieldOf("fluidFaces").forGetter(ChannelConfigComponent::fluidFaces),
                Codec.STRING.optionalFieldOf("channelId", "").forGetter(ChannelConfigComponent::channelId)).apply(instance, ChannelConfigComponent::new));

        public static final StreamCodec<ByteBuf, ChannelConfigComponent> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.INT.apply(ByteBufCodecs.list()), ChannelConfigComponent::energyFaces,
                ByteBufCodecs.INT.apply(ByteBufCodecs.list()), ChannelConfigComponent::itemFaces,
                ByteBufCodecs.INT.apply(ByteBufCodecs.list()), ChannelConfigComponent::fluidFaces,
                ByteBufCodecs.STRING_UTF8, ChannelConfigComponent::channelId,
                ChannelConfigComponent::new);
    }

    private TENDataComponents() {}
}
