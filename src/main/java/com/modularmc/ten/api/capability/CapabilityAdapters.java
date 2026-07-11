package com.modularmc.ten.api.capability;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.transfer.ItemAccessResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;

/**
 * Adapters bridging NeoForge 26.1.2 Resource/Transaction capability API
 * to the legacy IEnergyStorage/IItemHandler/IFluidHandler interfaces.
 *
 * <p>These adapters let BE code keep using the old interfaces internally
 * while the capability boundary speaks the new API.
 */
public final class CapabilityAdapters {

    private CapabilityAdapters() {}

    // ──────────────── Energy (IEnergyStorage ↔ EnergyHandler) ────────────────

    /**
     * Wraps an {@link IEnergyStorage} as an {@link EnergyHandler} for capability registration.
     */
    public static EnergyHandler asEnergyHandler(IEnergyStorage storage) {
        if (storage == null) return null;
        return new EnergyHandler() {
            @Override
            public long getAmountAsLong() {
                return storage.getEnergyStored();
            }

            @Override
            public long getCapacityAsLong() {
                return storage.getMaxEnergyStored();
            }

            @Override
            public int extract(int amount, TransactionContext transaction) {
                return storage.extractEnergy(amount, false);
            }

            @Override
            public int insert(int amount, TransactionContext transaction) {
                return storage.receiveEnergy(amount, false);
            }
        };
    }

    /**
     * Looks up energy capability at a block position and wraps it back to IEnergyStorage.
     */
    @Nullable
    public static IEnergyStorage getEnergy(Level level, BlockPos pos, @Nullable Direction side) {
        EnergyHandler handler = level.getCapability(Capabilities.Energy.BLOCK, pos, side);
        return handler != null ? IEnergyStorage.of(handler) : null;
    }

    /**
     * Looks up energy capability on an item stack using the new ItemCapability API.
     */
    @Nullable
    public static IEnergyStorage getEnergy(ItemStack stack) {
        // Use ItemCapability.getCapability(ItemStack, Context) directly
        EnergyHandler handler = Capabilities.Energy.ITEM.getCapability(stack, null);
        return handler != null ? IEnergyStorage.of(handler) : null;
    }

    // ──────────────── Item (IItemHandler ↔ ResourceHandler<ItemResource>) ────────────────

    /**
     * Looks up item capability at a block position and wraps it back to IItemHandler.
     */
    @Nullable
    public static IItemHandler getItems(Level level, BlockPos pos, @Nullable Direction side) {
        ResourceHandler<ItemResource> handler = level.getCapability(Capabilities.Item.BLOCK, pos, side);
        return handler != null ? IItemHandler.of(handler) : null;
    }

    /**
     * Looks up item capability on an item stack.
     */
    @Nullable
    public static IItemHandler getItems(ItemStack stack) {
        ResourceHandler<ItemResource> handler = Capabilities.Item.ITEM.getCapability(stack, null);
        return handler != null ? IItemHandler.of(handler) : null;
    }

    // ──────────────── Fluid (IFluidHandler ↔ ResourceHandler<FluidResource>) ────────────────

    /**
     * Looks up fluid capability at a block position and wraps it back to IFluidHandler.
     */
    @Nullable
    public static IFluidHandler getFluids(Level level, BlockPos pos, @Nullable Direction side) {
        ResourceHandler<FluidResource> handler = level.getCapability(Capabilities.Fluid.BLOCK, pos, side);
        return handler != null ? IFluidHandler.of(handler) : null;
    }

    /**
     * Looks up fluid capability on an item stack.
     */
    @Nullable
    public static IFluidHandler getFluids(ItemStack stack) {
        ResourceHandler<FluidResource> handler = Capabilities.Fluid.ITEM.getCapability(stack, null);
        return handler != null ? IFluidHandler.of(handler) : null;
    }

    // Reverse adapters (IItemHandler → ResourceHandler<ItemResource>, IFluidHandler → ResourceHandler<FluidResource>)
    // are implemented in ItemHandlerResourceAdapter and FluidHandlerResourceAdapter respectively.
    // Capability registration is in CommonProxy.registerCapabilities.
}
