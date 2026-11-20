package com.modularmc.ten.common.data;

import com.modularmc.ten.TEN;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;

import java.util.List;
import java.util.function.Supplier;

/**
 * 模组 DataComponent 注册中枢。
 * <p>
 * <ul>
 * <li>{@link #CHANNEL_CONFIG} — 频道连接器复制/应用的面配置载荷
 * （能量/物品/流体三组面模式 + 频道名）。</li>
 * </ul>
 */
public final class TENDataComponents {

    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, TEN.MOD_ID);

    public static final Supplier<DataComponentType<ChannelConfigComponent>> CHANNEL_CONFIG = DATA_COMPONENTS.register("channel_config",
            () -> DataComponentType.<ChannelConfigComponent>builder()
                    .persistent(ChannelConfigComponent.CODEC)
                    .networkSynchronized(ChannelConfigComponent.STREAM_CODEC)
                    .build());

    public static final Supplier<DataComponentType<FluidQuality>> QUALITY =
        DATA_COMPONENTS.register("quality", () -> DataComponentType.<FluidQuality>builder()
            .persistent(FluidQuality.CODEC)
            .networkSynchronized(FluidQuality.STREAM_CODEC)
            .build());
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

    public record FluidQuality(float item1, float item2, float fluid1, float fluid2) {
        public static final Codec<FluidQuality> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("item1").forGetter(FluidQuality::item1),
            Codec.FLOAT.fieldOf("item2").forGetter(FluidQuality::item2),
            Codec.FLOAT.fieldOf("fluid1").forGetter(FluidQuality::fluid1),
            Codec.FLOAT.fieldOf("fluid2").forGetter(FluidQuality::fluid2)
        ).apply(instance, FluidQuality::new));

        public static final StreamCodec<ByteBuf, FluidQuality> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, FluidQuality::item1,
            ByteBufCodecs.FLOAT, FluidQuality::item2,
            ByteBufCodecs.FLOAT, FluidQuality::fluid1,
            ByteBufCodecs.FLOAT, FluidQuality::fluid2,
            FluidQuality::new);

        public float itemMul(int index) {
            return switch (index) {
                case 0 -> clamp(item1);
                case 1 -> clamp(item2);
                default -> 1.0f;
            };
        }

        /** 第 {@code index} 个流体产物的倍率，钳 [0,2]；越界返回 1.0。 */
        public float fluidMul(int index) {
            return switch (index) {
                case 0 -> clamp(fluid1);
                case 1 -> clamp(fluid2);
                default -> 1.0f;
            };
        }

        private static float clamp(float v) {
            if (Float.isNaN(v)) return 1.0f;
            return Math.max(0f, Math.min(4f, v));
        }

        /** 默认档案：品质 1.0、温度 300、浓度 1.0。 */
        public static final FluidQuality DEFAULT = new FluidQuality(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private TENDataComponents() {}
}
