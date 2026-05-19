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
        modBus.register(CommonProxy.class);
    }

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        TENBlocks.init();
        TENFluids.init();
        TENBlockEntities.init();
        TENRecipeTypes.init();
        TENItems.init();
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        for (var block : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
            if (block instanceof BaseMachineBlock) {
                event.registerBlock(Capabilities.EnergyStorage.BLOCK, (level, pos, state, blockEntity, side) -> {
                    if (blockEntity instanceof com.modularmc.ten.common.blockentity.CableBlockEntity cable) {
                        return cable.getEnergy(side);
                    }
                    if (blockEntity instanceof com.modularmc.ten.api.blockentity.CmMachineBlockEntity machine) {
                        return machine.getEnergyStorage(side);
                    }
                    return null;
                }, block);

                event.registerBlock(Capabilities.ItemHandler.BLOCK, (level, pos, state, blockEntity, side) -> {
                    if (blockEntity instanceof com.modularmc.ten.common.blockentity.PipeBlockEntity pipe) {
                        return pipe.getTransportHandler(side);
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

        // Energy Unit item capability
        event.registerItem(Capabilities.EnergyStorage.ITEM, (stack, ctx) -> {
            var data = com.modularmc.ten.component.EnergyUnitData.of(stack);
            return new net.neoforged.neoforge.energy.EnergyStorage(
                    com.modularmc.ten.common.item.EnergyUnitItem.maxEnergy(),
                    com.modularmc.ten.common.item.EnergyUnitItem.inputRate(),
                    com.modularmc.ten.common.item.EnergyUnitItem.outputRate()) {

                @Override
                public int getEnergyStored() {
                    return data.getEnergy();
                }

                @Override
                public int getMaxEnergyStored() {
                    return com.modularmc.ten.common.item.EnergyUnitItem.maxEnergy();
                }

                @Override
                public int receiveEnergy(int maxReceive, boolean simulate) {
                    int received = super.receiveEnergy(maxReceive, simulate);
                    if (!simulate && received > 0) {
                        data.setEnergy(getEnergyStored());
                        data.save(stack);
                    }
                    return received;
                }

                @Override
                public int extractEnergy(int maxExtract, boolean simulate) {
                    int extracted = super.extractEnergy(maxExtract, simulate);
                    if (!simulate && extracted > 0) {
                        data.setEnergy(getEnergyStored());
                        data.save(stack);
                    }
                    return extracted;
                }
            };
        }, com.modularmc.ten.common.data.TENItems.ENERGY_CAPACITY.get());
    }
}
