package com.modularmc.ten.integration.jade;

import com.modularmc.ten.TEN;
import com.modularmc.ten.common.blockentity.PipeBlockEntity;
import com.modularmc.ten.utils.ComponentHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade 管道状态 provider：对准管道方块（pipe/pipe_white/pipe_black）时显示过滤模式。
 * <p>
 * 1.21.1 适配：管道为旧 root 撮合模型（冻结维护，不重构），无 isPullSide/getBuffer，
 * 仅显示过滤模式（白/黑名单）。
 */
public class PipeJadeProvider implements IBlockComponentProvider {

    @Override
    public ResourceLocation getUid() {
        return TEN.id("pipe_status");
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!(accessor.getBlockEntity() instanceof PipeBlockEntity pipe)) return;
        if (pipe.isFiltered()) {
            boolean whitelist = pipe.isWhitelist();
            String modeKey = whitelist ? "pipe.filter.whitelist" : "pipe.filter.blacklist";
            ChatFormatting color = whitelist ? ChatFormatting.GREEN : ChatFormatting.RED;
            tooltip.add(ComponentHelper.translated(ComponentHelper.getKey(modeKey)).withStyle(color));
        }
    }
}