package com.modularmc.ten.client;

import com.modularmc.ten.TEN;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.javafmlmod.FMLModContainer;

@Mod(value = TEN.MOD_ID, dist = Dist.CLIENT)
public class TENClient {

    public TENClient(IEventBus modBus, FMLModContainer container) {
        ClientProxy.init(modBus);
    }
}
