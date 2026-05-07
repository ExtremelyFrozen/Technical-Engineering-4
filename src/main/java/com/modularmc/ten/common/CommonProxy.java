package com.modularmc.ten.common;

import com.modularmc.ten.common.registry.*;
import com.modularmc.ten.config.ConfigHolder;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import static com.modularmc.ten.common.registry.Registration.REGISTRATE;

public class CommonProxy {

    private static IEventBus modBus;

    public static void init(final IEventBus modBus) {
        CommonProxy.modBus = modBus;

        ConfigHolder.init();

        REGISTRATE.registerEventListeners(modBus);
        TENCreativeModeTabs.init();

        modBus.register(CommonProxy.class);
    }

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        TENBlocks.init();
        TENFluids.init();
        TENBlockEntities.init();
        TENMenuTypes.init();
        TENRecipeTypes.init();
        TENItems.init();
    }
}
