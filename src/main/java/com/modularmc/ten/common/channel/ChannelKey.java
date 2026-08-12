package com.modularmc.ten.common.channel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * 频道唯一标识：名称 + 类型。
 * <p>
 * 绑定同一频道的方块共享同一虚拟存储；不同名称或不同类型即为不同频道
 * （物品/流体/能量按类型独立，跨维度全局共享）。
 */
public record ChannelKey(String name, ChannelType type) {

    public static final Codec<ChannelKey> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(ChannelKey::name),
            ChannelType.CODEC.fieldOf("type").forGetter(ChannelKey::type)).apply(instance, ChannelKey::new));
}
