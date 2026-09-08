package com.modularmc.ten.integration.jade;

import com.modularmc.ten.TEN;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Jade 管道/线缆输出面 provider。
 * <p>
 * ker 体系管道（CableBased）的连接面只有一种形态（NONE/NORMAL 二态渲染），
 * 扳手切到 PULL（输出：主动从容器抽取）后玩家无法从模型上区分哪些面在输出——
 * 本 provider 在 Jade tooltip 追加「输出面：南,东」样式的行，
 * 列出所有设定为 PULL 的面（多面逗号分隔；无 PULL 面不显示该行）。
 */
public class PipePullProvider implements IBlockComponentProvider {

    public static final ResourceLocation UID = TEN.id("pipe_pull");

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!(accessor.getBlockEntity() instanceof com.modularmc.ten.common.blockentity.CableBlockEntity cable)
                && !(accessor.getBlockEntity() instanceof com.modularmc.ten.common.blockentity.PipeBlockEntity pipe)) {
            return;
        }
        byte mask = accessor.getBlockEntity() instanceof com.modularmc.ten.common.blockentity.CableBlockEntity c
                ? c.pullFacesMask : ((com.modularmc.ten.common.blockentity.PipeBlockEntity) accessor.getBlockEntity()).pullFacesMask;
        List<Component> pullSides = new ArrayList<>(3);
        for (Direction side : Direction.values()) {
            if ((mask & (1 << side.ordinal())) != 0) {
                pullSides.add(Component.translatable("kenergyengineering.dire." + side.getName()));
            }
        }
        if (pullSides.isEmpty()) {
            return;
        }
        tooltip.add(Component.translatable("kenergyengineering.jade.auto_extract",
                pullSides.stream().map(Component::getString).collect(java.util.stream.Collectors.joining(", "))));
    }
}
