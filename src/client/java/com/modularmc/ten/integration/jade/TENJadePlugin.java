// -*- coding: utf-8 -*-
package com.modularmc.ten.integration.jade;

import com.modularmc.ten.TEN;
import com.modularmc.ten.common.block.machine.ChannelBlock;

import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Jade (WAILA) 集成入口：注册频道接入状态 provider（{@link ChannelJadeProvider}）。
 * <p>
 * 纯客户端集成：{@code @WailaPlugin} 注解由 Jade 运行时按类路径自动扫描发现
 * （注解值 = 所属 mod id），无需 mods.toml 声明、无需 services 文件（与 JEI 的
 * ServiceLoader 机制不同）。能量/物品/流体三型频道方块均为 {@link ChannelBlock}
 * 实例，按父类注册一次即覆盖全部（Jade 内部 HierarchyLookup 按类层级匹配）。
 */
@WailaPlugin(TEN.MOD_ID)
public class TENJadePlugin implements IWailaPlugin {

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(new ChannelJadeProvider(), ChannelBlock.class);
    }
}
