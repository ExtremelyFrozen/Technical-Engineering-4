package com.modularmc.ten.common.channel;

import com.mojang.serialization.Codec;

/**
 * 频道存储类型，与频道方块一一对应（物品/流体/能量）。
 * <p>
 * 同名称不同类型构成不同的 {@link ChannelKey}，共享存储按类型完全隔离——
 * 「铁锭」物品频道与「铁锭」流体频道互不干扰。
 */
public enum ChannelType {

    ITEM,
    FLUID,
    ENERGY;

    public static final Codec<ChannelType> CODEC = Codec.STRING.xmap(ChannelType::valueOf, ChannelType::name);
}
