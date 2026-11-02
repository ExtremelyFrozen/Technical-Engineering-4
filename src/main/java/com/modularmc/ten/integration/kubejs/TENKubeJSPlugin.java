package com.modularmc.ten.integration.kubejs;

import com.modularmc.ten.TEN;

import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;

/**
 * KubeJS 集成入口，靠 jar 根的 {@code kubejs.plugins.txt} 被发现。
 * <p>
 * 本类只被 KubeJS 加载：KubeJS 缺席时没人读那个文件，这个类也就不会被引用，
 * 主包不依赖任何 KubeJS 类型（与 JEI/Jade 的注解自动发现是同一套隔离方式）。
 */
public class TENKubeJSPlugin implements KubeJSPlugin {

    @Override
    public void init() {
        TEN.LOGGER.info("KubeJS integration enabled");
    }
}
