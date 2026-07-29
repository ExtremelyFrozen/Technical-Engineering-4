package com.modularmc.ten.common;

import com.modularmc.ten.api.capability.CapabilityAdapters;
import com.modularmc.ten.api.capability.ItemHandlerResourceAdapter;
import com.modularmc.ten.common.block.machine.BaseMachineBlock;
import com.modularmc.ten.common.data.*;
import com.modularmc.ten.common.item.EnergyUnitItem;
import com.modularmc.ten.common.registry.Registration;

import net.minecraft.core.Direction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public class CommonProxy {

    private static IEventBus modBus;

    public static void init(final IEventBus modBus) {
        CommonProxy.modBus = modBus;

        // Register all DeferredRegisters (BLOCKS, ITEMS, FLUIDS, BLOCK_ENTITIES, CREATIVE_TABS)
        Registration.register(modBus);

        // Trigger class loading for all registration holders so their static blocks
        // execute before the registry event fires.
        TENBlocks.init();
        TENFluids.init();
        TENBlockEntities.init();
        TENItems.init();
        TENCreativeModeTabs.init();

        // Recipe serializer/type DeferredRegisters (kept in TENRecipeTypes)
        TENRecipeTypes.SERIALIZERS.register(modBus);
        TENRecipeTypes.TYPES.register(modBus);
        modBus.register(CommonProxy.class);
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        for (var block : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
            if (block instanceof BaseMachineBlock) {
                // Energy capability — adapted via CapabilityAdapters
                event.registerBlock(Capabilities.Energy.BLOCK, (level, pos, state, blockEntity, ctx) -> {
                    Direction side = ctx instanceof Direction d ? d : null;
                    if (blockEntity instanceof com.modularmc.ten.common.blockentity.CableBlockEntity cable) {
                        return CapabilityAdapters.asEnergyHandler(cable.getEnergy(side));
                    }
                    if (blockEntity instanceof com.modularmc.ten.api.blockentity.CmMachineBlockEntity machine) {
                        return CapabilityAdapters.asEnergyHandler(machine.getEnergyStorage(side));
                    }
                    return null;
                }, block);

                // Item capability — IItemHandler → ResourceHandler<ItemResource> adapter
                event.registerBlock(Capabilities.Item.BLOCK, (level, pos, state, blockEntity, ctx) -> {
                    Direction side = ctx instanceof Direction d ? d : null;
                    if (blockEntity instanceof com.modularmc.ten.api.blockentity.CmMachineBlockEntity machine) {
                        var itemHandler = machine.getItemHandler(side);
                        return itemHandler != null ? new ItemHandlerResourceAdapter(itemHandler) : null;
                    }
                    return null;
                }, block);

                // Fluid capability — delegates to machine.getFluidResourceHandler (cached adapter)
                event.registerBlock(Capabilities.Fluid.BLOCK, (level, pos, state, blockEntity, ctx) -> {
                    Direction side = ctx instanceof Direction d ? d : null;
                    if (blockEntity instanceof com.modularmc.ten.api.blockentity.CmMachineBlockEntity machine) {
                        return machine.getFluidResourceHandler(side);
                    }
                    return null;
                }, block);
            }
        }

        // Energy Unit item capability — adapted via CapabilityAdapters
        event.registerItem(Capabilities.Energy.ITEM, (stack, ctx) -> {
            var data = com.modularmc.ten.component.EnergyUnitData.of(stack);

            return CapabilityAdapters.asEnergyHandler(new net.neoforged.neoforge.energy.IEnergyStorage() {

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
            });
        }, com.modularmc.ten.common.data.TENItems.ENERGY_CAPACITY.get());
    }
}
