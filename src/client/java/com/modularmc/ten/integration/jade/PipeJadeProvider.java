// -*- coding: utf-8 -*-
package com.modularmc.ten.integration.jade;

import com.modularmc.ten.TEN;
import com.modularmc.ten.common.blockentity.PipeBlockEntity;
import com.modularmc.ten.utils.ComponentHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade 管道状态 provider：对准任一管道方块（pipe / pipe_white / pipe_black）时在 tooltip
 * 中显示过滤模式（白/黑名单）、统一传输速率（64/tick）与管道内缓冲状态。
 * <p>
 * 纯客户端 tooltip 提供（appendTooltip 在客户端渲染时调用），不注册服务端数据 provider：
 * 过滤模式经 {@code readTileData}/{@code writeTileData}（getUpdateTag 路径）随方块实体
 * 数据同步到客户端 BlockEntity。注册于 {@code CableBased} 基类（管道与线缆共用），
 * 线缆方块经 instanceof 守卫早退出，与 {@link ChannelJadeProvider} 的注册模式一致。
 */
public class PipeJadeProvider implements IBlockComponentProvider {

    /** Provider 唯一 ID（Jade 配置开关键，kenergyengineering:pipe_status）。 */
    @Override
    public Identifier getUid() {
        return TEN.id("pipe_status");
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        // 仅对管道方块生效；accessor.getBlockEntity() 对非 BE 方块可为 null，
        // instanceof 模式匹配同时覆盖 null 与非管道 BE（线缆）两种情况（早退出）。
        if (!(accessor.getBlockEntity() instanceof PipeBlockEntity pipe)) {
            return;
        }
        // 1. 过滤模式：pipe_white 白名单 / pipe_black 黑名单；普通 pipe 无过滤，不显示
        if (pipe.isFiltered()) {
            boolean whitelist = pipe.isWhitelist();
            String modeKey = whitelist ? "pipe.filter.whitelist" : "pipe.filter.blacklist";
            ChatFormatting color = whitelist ? ChatFormatting.GREEN : ChatFormatting.RED;
            tooltip.add(ComponentHelper.translated(ComponentHelper.getKey(modeKey)).withStyle(color));
        }
        // 2. 传输速率（化繁为简：所有管道统一 64 物品/tick，PipeBlockEntity 权威常量）
        tooltip.add(Component.translatable(ComponentHelper.getKey("pipe.jade.io_rate"), PipeBlockEntity.TRANSFER_RATE)
                .withStyle(ComponentHelper.GREEN));
        // 2b. 抽入点面（扳手配置的主动拉取连接端；无则不显示）
        List<String> pullSides = new ArrayList<>();
        for (Direction d : Direction.values()) {
            if (pipe.isPullSide(d)) {
                pullSides.add(d.getName());
            }
        }
        if (!pullSides.isEmpty()) {
            tooltip.add(Component.translatable(ComponentHelper.getKey("pipe.jade.pull_side"), String.join(", ", pullSides))
                    .withStyle(ChatFormatting.AQUA));
        }
        // 3. 管道缓冲状态（逐级传递中间态：管道内当前持有的物品与数量；空则不显示）
        ItemStack buf = pipe.getBuffer();
        if (!buf.isEmpty()) {
            tooltip.add(Component.translatable(ComponentHelper.getKey("pipe.jade.buffer"), buf.getHoverName(), buf.getCount())
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
