// -*- coding: utf-8 -*-
package com.modularmc.ten.integration.jade;

import com.modularmc.ten.TEN;
import com.modularmc.ten.common.blockentity.PipeBlockEntity;
import com.modularmc.ten.common.blockentity.PipeUpgradeType;
import com.modularmc.ten.utils.ComponentHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade 管道状态 provider：对准任一管道方块（pipe / pipe_white / pipe_black）时在 tooltip
 * 中显示过滤模式（白/黑名单）、激活升级层级（level>0，各占一行）与 IO 速率（单次量）。
 * <p>
 * 纯客户端 tooltip 提供（appendTooltip 在客户端渲染时调用），不注册服务端数据 provider：
 * 升级等级/过滤模式经 {@code readTileData}/{@code writeTileData}（getUpdateTag 路径）
 * 随方块实体数据同步到客户端 BlockEntity，无需额外请求；频道名同步机制见
 * {@link ChannelJadeProvider}。注册于 {@code CableBased} 基类（管道与线缆共用），
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
        // 2. 升级层级（按枚举序 Pull/Push/Speed/Page/Ender）：仅激活升级（level > 0）
        //    独占一行显示（如 "Pull: 1/1"、"Speed: 3/9"）；level = 0 的未激活升级不渲染。
        for (PipeUpgradeType type : PipeUpgradeType.values()) {
            if (pipe.getUpgradeLevel(type) == 0) {
                continue;
            }
            tooltip.add(Component.translatable(ComponentHelper.getKey(type.nameKey()))
                    .append(": ")
                    .append(Component.literal(pipe.getUpgradeLevel(type) + "/" + type.maxLevel()))
                    .withStyle(ComponentHelper.GOLD));
        }
        // 3. IO 速率（单次量 = (1 + 7×speedLevel) × 2^enderLevel，PipeBlockEntity 权威实现）
        tooltip.add(Component.translatable(ComponentHelper.getKey("pipe.jade.io_rate"), pipe.singleTransferAmount())
                .withStyle(ComponentHelper.GREEN));
        // 4. 过滤页数（扩写升级后 1+pageLevel > 1 时显示，避免默认单页噪音）
        if (pipe.getFilterPageCount() > 1) {
            tooltip.add(Component.translatable(ComponentHelper.getKey("pipe.jade.pages"), pipe.getFilterPageCount())
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
