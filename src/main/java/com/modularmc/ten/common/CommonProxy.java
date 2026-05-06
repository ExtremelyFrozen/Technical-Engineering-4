package com.modularmc.ten.common;

import net.neoforged.bus.api.IEventBus;

public class CommonProxy {

    private static IEventBus modBus;

    public static void init(final IEventBus modBus) {
        CommonProxy.modBus = modBus;
        modBus.register(CommonProxy.class);
    }
}
