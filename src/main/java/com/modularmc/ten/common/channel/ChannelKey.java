package com.modularmc.ten.common.channel;

/**
 * 频道唯一标识：名称 + 类型。
 * <p>
 * 绑定同一频道的方块共享同一虚拟存储；不同名称或不同类型即为不同频道
 * （物品/流体/能量按类型独立，跨维度全局共享）。
 */
public record ChannelKey(String name, ChannelType type) {}
