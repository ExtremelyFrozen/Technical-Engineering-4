// -*- coding: utf-8 -*-
package com.modularmc.ten.integration.jade;

import com.modularmc.ten.TEN;
import com.modularmc.ten.common.blockentity.channel.AbstractChannelBlockEntity;
import com.modularmc.ten.utils.ComponentHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade 频道接入状态 provider：对准任一频道方块（能量/物品/流体）时在 tooltip 中
 * 高亮显示当前接入状态 —— 已接入显示频道名（绿色），未接入显示「未接入频道」（灰）。
 * <p>
 * 纯客户端 tooltip 提供（appendTooltip 在客户端渲染时调用），不注册服务端数据
 * provider：频道名经 {@code @DescSynced} 已同步到客户端 BlockEntity，无需额外请求。
 */
public class ChannelJadeProvider implements IBlockComponentProvider {

    /** Provider 唯一 ID（Jade 配置开关键，如 kenergyengineering:channel_status）。 */
    @Override
    public Identifier getUid() {
        return TEN.id("channel_status");
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        // 仅对频道方块生效；accessor.getBlockEntity() 对非 BE 方块可为 null，
        // instanceof 模式匹配同时覆盖 null 与非频道 BE 两种情况（早退出）。
        if (!(accessor.getBlockEntity() instanceof AbstractChannelBlockEntity channel)) {
            return;
        }
        if (channel.isJoined()) {
            tooltip.add(Component.translatable(
                            ComponentHelper.getKey("channel.jade.joined"), channel.joinedChannelName())
                    .withStyle(ChatFormatting.GREEN));
        } else {
            tooltip.add(ComponentHelper.translated(ComponentHelper.getKey("channel.jade.not_joined"))
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
