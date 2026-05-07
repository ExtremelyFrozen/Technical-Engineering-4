package com.modularmc.ten.client;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.option.MachineType;
import com.modularmc.ten.client.gui.CmContainerMachine;
import com.modularmc.ten.client.gui.screen.*;
import com.modularmc.ten.common.data.TENMenuTypes;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public class ClientProxy {

    public static void init(IEventBus modBus) {
        modBus.addListener(ClientProxy::onRegisterScreens);
    }

    private static void onRegisterScreens(RegisterMenuScreensEvent event) {
        var menuType = TENMenuTypes.MACHINE.get();
        TEN.LOGGER.info("[TEN-GUI] Registering screens for menuType={}", menuType);
        event.register(menuType, ClientProxy::createScreen);
        TEN.LOGGER.info("[TEN-GUI] Screen registration complete");
    }

    private static AbstractContainerScreen<CmContainerMachine> createScreen(
                                                                            CmContainerMachine container, Inventory inv, Component title) {
        int type = container.machine != null ? container.machine.machineType() : container.machineType;
        TEN.LOGGER.info("[TEN-GUI] createScreen: machine={} type={}",
                container.machine != null ? container.machine.getClass().getSimpleName() : "null", type);
        return switch (type) {
            case MachineType.FURNACE -> new FurnaceScreen(container, inv, title);
            case MachineType.PULVERIZER -> new PulverizerScreen(container, inv, title);
            case MachineType.COMPRESSOR -> new CompressorScreen(container, inv, title);
            case MachineType.REFINER -> new RefinerScreen(container, inv, title);
            case MachineType.INDUCTION_FURNACE -> new IndfurScreen(container, inv, title);
            case MachineType.PSIONICANT -> new PsionicantScreen(container, inv, title);
            case MachineType.MATTER_CONDENSER -> new CondenserScreen(container, inv, title);
            case MachineType.ENCHANTMENT_FLUSHER -> new EncfluScreen(container, inv, title);
            case MachineType.BEACON -> new BeaconScreen(container, inv, title);
            case MachineType.MOB_RIPPER -> new MobRipScreen(container, inv, title);
            case MachineType.QUARRY -> new QuarryScreen(container, inv, title);
            case MachineType.FARM -> new FarmScreen(container, inv, title);
            case MachineType.CELL -> new CellScreen(container, inv, title);
            case MachineType.ENGINE_SOLAR -> new SolarScreen(container, inv, title);
            case MachineType.ENGINE_EXTRACTION, MachineType.ENGINE_METAL, MachineType.ENGINE_BIOMASS -> new EngineScreen(container, inv, title);
            default -> {
                TEN.LOGGER.warn("[TEN-GUI] Unknown machine type {}, falling back to FurnaceScreen", type);
                yield new FurnaceScreen(container, inv, title);
            }
        };
    }
}
