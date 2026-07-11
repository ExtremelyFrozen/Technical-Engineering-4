package com.modularmc.ten;

import com.modularmc.ten.common.CommonProxy;
import com.modularmc.ten.config.ConfigValidator;
import com.modularmc.ten.config.TENConfig;
import com.modularmc.ten.network.ToggleEnergyUnitPayload;

import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.javafmlmod.FMLModContainer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;

@Mod(TEN.MOD_ID)
public class TEN {

    public static final String MOD_ID = "kenergyengineering";
    public static final String MOD_NAME = "Kenergy Engineering: Retechnicalized";
    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);

    @ApiStatus.Internal
    public static IEventBus tenModBus;

    public TEN(IEventBus modBus, FMLModContainer container) {
        TEN.tenModBus = modBus;

        // Register COMMON and CLIENT config specs with TOML persistence.
        // COMMON: te4-config.toml (machine, energyUnit, farm blocks)
        // CLIENT: te4-client.toml (client block)
        container.registerConfig(ModConfig.Type.COMMON, TENConfig.COMMON_SPEC, "te4-config.toml");
        container.registerConfig(ModConfig.Type.CLIENT, TENConfig.CLIENT_SPEC, "te4-client.toml");

        // Validate loaded config values at common setup
        modBus.addListener(TEN::onCommonSetup);
        modBus.addListener(TEN::onClientSetup);

        CommonProxy.init(modBus);

        modBus.addListener(TEN::registerPayloads);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        ConfigValidator.validate();
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        // CLIENT config is auto-loaded by NeoForge; no additional setup needed.
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(TEN.MOD_ID);
        ToggleEnergyUnitPayload.register(registrar);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
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
