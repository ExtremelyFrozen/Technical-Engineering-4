package com.modularmc.ten.integration.jade;

import com.modularmc.ten.TEN;
import com.modularmc.ten.common.blockentity.channel.AbstractChannelBlockEntity;
import com.modularmc.ten.utils.ComponentHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public class ChannelJadeProvider implements IBlockComponentProvider {

    @Override
    public ResourceLocation getUid() {
        return TEN.id("channel_status");
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!(accessor.getBlockEntity() instanceof AbstractChannelBlockEntity channel)) return;
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