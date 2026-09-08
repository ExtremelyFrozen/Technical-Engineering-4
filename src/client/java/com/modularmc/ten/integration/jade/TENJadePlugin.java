package com.modularmc.ten.integration.jade;

import com.modularmc.ten.TEN;
import com.modularmc.ten.common.block.machine.CableBased;
import com.modularmc.ten.common.block.machine.ChannelBlock;

import net.minecraft.resources.ResourceLocation;

import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Jade (WAILA) 集成入口：注册频道接入状态、管道过滤模式与管道自动抽取面 provider。
 * <p>
 * 纯客户端集成：@WailaPlugin 注解由 Jade 运行时按类路径自动扫描发现。
 * 频道方块（能量/物品/流体）均为 ChannelBlock 实例，按父类注册一次；
 * 管道（pipe/pipe_white/pipe_black）共用 CableBased 基类（与线缆同基类），
 * 线缆经 provider 内 instanceof 守卫过滤；
 * 自动抽取面 provider 同样挂在 CableBased 上（读取 transmitter 的 PULL 面集合）。
 */
@WailaPlugin(TEN.MOD_ID)
public class TENJadePlugin implements IWailaPlugin {

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.addConfig(PipePullProvider.UID, true);
        registration.registerBlockComponent(new ChannelJadeProvider(), ChannelBlock.class);
        registration.registerBlockComponent(new PipeJadeProvider(), CableBased.class);
        registration.registerBlockComponent(new PipePullProvider(), CableBased.class);
    }
}
