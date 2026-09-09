package com.modularmc.ten;

import com.modularmc.ten.common.CommonProxy;
import com.modularmc.ten.config.ConfigHolder;
import com.modularmc.ten.config.TENConfig;
import com.modularmc.ten.network.ToggleEnergyUnitPayload;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.javafmlmod.FMLModContainer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;

@Mod(TEN.MOD_ID)
public class TEN {

    public static final String MOD_ID = "kenergyengineering";
    public static final String MOD_NAME = "科能工程:再技术化";
    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);

    @ApiStatus.Internal
    public static IEventBus tenModBus;

    public TEN(IEventBus modBus, FMLModContainer container) {
        TEN.tenModBus = modBus;

        // P1-3: 配置系统从 dev.toma.configuration 迁移到 NeoForge ModConfigSpec
        container.registerConfig(ModConfig.Type.COMMON, TENConfig.COMMON_SPEC, "te4-config.toml");
        container.registerConfig(ModConfig.Type.CLIENT, TENConfig.CLIENT_SPEC, "te4-client.toml");

        ConfigHolder.init();
        CommonProxy.init(modBus);

        modBus.addListener(TEN::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(TEN.MOD_ID);
        ToggleEnergyUnitPayload.register(registrar);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static class Mods {

        private static boolean isModLoaded(String modId) {
            return ModList.get().isLoaded(modId);
        }

        public static boolean isAnyRecipeViewerLoaded() {
            return isEMILoaded() || isModLoaded("jei");
        }

        public static boolean isJEILoaded() {
            return !isEMILoaded() && isModLoaded("jei");
        }

        public static boolean isEMILoaded() {
            return isModLoaded("emi");
        }
    }
}
