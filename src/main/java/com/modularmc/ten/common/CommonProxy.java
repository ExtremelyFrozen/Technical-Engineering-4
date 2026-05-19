package com.modularmc.ten.common;

import com.modularmc.ten.common.block.machine.BaseMachineBlock;
import com.modularmc.ten.common.data.*;
import com.modularmc.ten.common.item.EnergyUnitItem;
import com.modularmc.ten.config.ConfigHolder;
import com.modularmc.ten.data.TENDataGen;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.energy.IEnergyStorage;
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
            return new IEnergyStorage() {
                private int doReceive(int maxReceive) {
                    int capacity = EnergyUnitItem.maxEnergy();
                    int stored = data.getEnergy();
                    int canAccept = Math.min(capacity - stored, Math.min(maxReceive, EnergyUnitItem.inputRate()));
                    if (canAccept <= 0) return 0;
                    data.setEnergy(stored + canAccept);
                    data.save(stack);
                    return canAccept;
                }

                @Override
                public int receiveEnergy(int maxReceive, boolean simulate) {
                    if (!canReceive() || maxReceive <= 0) return 0;
                    if (simulate) {
                        int capacity = EnergyUnitItem.maxEnergy();
                        int stored = data.getEnergy();
                        return Math.min(capacity - stored, Math.min(maxReceive, EnergyUnitItem.inputRate()));
                    }
                    return doReceive(maxReceive);
                }

                @Override
                public int extractEnergy(int maxExtract, boolean simulate) {
                    if (!canExtract() || maxExtract <= 0) return 0;
                    int stored = data.getEnergy();
                    int canGive = Math.min(stored, Math.min(maxExtract, EnergyUnitItem.outputRate()));
                    if (canGive <= 0) return 0;
                    if (!simulate) {
                        data.setEnergy(stored - canGive);
                        data.save(stack);
                    }
                    return canGive;
                }

                @Override
                public int getEnergyStored() {
                    return data.getEnergy();
                }

                @Override
                public int getMaxEnergyStored() {
                    return EnergyUnitItem.maxEnergy();
                }

                @Override
                public boolean canExtract() {
                    return EnergyUnitItem.outputRate() > 0 && data.getEnergy() > 0;
                }

                @Override
                public boolean canReceive() {
                    return EnergyUnitItem.inputRate() > 0 && data.getEnergy() < EnergyUnitItem.maxEnergy();
                }
            };
        }, com.modularmc.ten.common.data.TENItems.ENERGY_CAPACITY.get());
    }
}
