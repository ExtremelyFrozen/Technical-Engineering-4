package com.modularmc.ten;

import com.modularmc.ten.common.CommonProxy;
import com.modularmc.ten.config.ConfigHolder;
import com.modularmc.ten.core.network.TENNetwork;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.javafmlmod.FMLModContainer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;

@Mod(TEN.MOD_ID)
public class TEN {

    public static final String MOD_ID = "technicalengineering";
    public static final String MOD_NAME = "Technical-Engineering";
    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);

    @ApiStatus.Internal
    public static IEventBus tenModBus;

    public TEN(IEventBus modBus, FMLModContainer container) {
        TEN.tenModBus = modBus;

        ConfigHolder.init();
        CommonProxy.init(modBus);

        modBus.addListener(TENNetwork::registerPayloads);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
