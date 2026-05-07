package com.modularmc.ten.common;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

public class CommonProxy {

    private static IEventBus modBus;

    public static void init(final IEventBus modBus) {
        CommonProxy.modBus = modBus;
        modBus.register(CommonProxy.class);
    }


    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {

    }
}
