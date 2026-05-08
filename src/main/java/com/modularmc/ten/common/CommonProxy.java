package com.modularmc.ten.common;

import com.modularmc.ten.common.block.machine.BaseMachineBlock;
import com.modularmc.ten.common.data.*;
import com.modularmc.ten.config.ConfigHolder;
import com.modularmc.ten.data.TENDataGen;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import static com.modularmc.ten.common.registry.Registration.REGISTRATE;

public class CommonProxy {

    private static IEventBus modBus;

    public static void init(final IEventBus modBus) {
        CommonProxy.modBus = modBus;

        ConfigHolder.init();
        TENDataGen.init();

        REGISTRATE.registerEventListeners(modBus);
        TENCreativeModeTabs.init();

        TENRecipeTypes.SERIALIZERS.register(modBus);
        TENRecipeTypes.TYPES.register(modBus);
        TENMenuTypes.MENUS.register(modBus);

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

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        for (var block : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
            if (block instanceof BaseMachineBlock) {
                event.registerBlock(Capabilities.EnergyStorage.BLOCK, (level, pos, state, blockEntity, side) -> {
                    if (blockEntity instanceof com.modularmc.ten.common.blockentity.CableBlockEntity cable) {
                        return cable.getEnergy();
                    }
                    if (blockEntity instanceof com.modularmc.ten.api.blockentity.CmMachineBlockEntity machine) {
                        return machine.getEnergyStorage(side);
                    }
                    return null;
                }, block);

                event.registerBlock(Capabilities.ItemHandler.BLOCK, (level, pos, state, blockEntity, side) -> {
                    if (blockEntity instanceof com.modularmc.ten.common.blockentity.PipeBlockEntity pipe) {
                        return pipe.getItemHandler();
                    }
                    if (blockEntity instanceof com.modularmc.ten.api.blockentity.CmMachineBlockEntity machine) {
                        return machine.getItemHandler(side);
                    }
                    return null;
                }, block);

                event.registerBlock(Capabilities.FluidHandler.BLOCK, (level, pos, state, blockEntity, side) -> {
                    if (blockEntity instanceof com.modularmc.ten.api.blockentity.CmMachineBlockEntity machine) {
                        return machine.getFluidHandler(side);
                    }
                    return null;
                }, block);
            }
        }
    }
}
