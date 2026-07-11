package com.modularmc.ten.api.capability;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * A reverse adapter that wraps a legacy {@link IFluidHandler} as a {@link ResourceHandler<FluidResource>}.
 *
 * <p>This allows NeoForge 26.1.2 capability consumers (pipes, channels, etc.) to interact with
 * machines that still use the old {@code IFluidHandler} interface internally.
 *
 * <p><b>Transaction semantics:</b> The legacy {@code IFluidHandler} API does not support rollback.
 * Operations are executed immediately on the underlying handler, consistent with the existing
 * {@link CapabilityAdapters#asEnergyHandler} pattern. Callers requiring proper transaction
 * rollback should use a native {@code ResourceHandler} implementation on the BE.
 *
 * <p><b>Guards:</b> Empty resources and negative amounts are rejected via
 * {@link TransferPreconditions}. Null handlers throw at construction.
 */
public class FluidHandlerResourceAdapter implements ResourceHandler<FluidResource> {

    private final IFluidHandler handler;

    public FluidHandlerResourceAdapter(IFluidHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("handler must not be null");
        }
        this.handler = handler;
    }

    @Override
    public int size() {
        return handler.getTanks();
    }

    @Override
    public FluidResource getResource(int index) {
        return FluidResource.of(handler.getFluidInTank(index));
    }

    @Override
    public long getAmountAsLong(int index) {
        return handler.getFluidInTank(index).getAmount();
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        if (resource == null || resource.isEmpty()) return 0;
        return handler.getTankCapacity(index);
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        if (resource == null || resource.isEmpty()) return false;
        return handler.isFluidValid(index, resource.toStack(1));
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (amount <= 0) return 0;
        if (index < 0 || index >= size()) return 0;
        if (!isValid(index, resource)) return 0;

        FluidStack toInsert = resource.toStack(amount);
        return handler.fill(toInsert, IFluidHandler.FluidAction.EXECUTE);
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (amount <= 0) return 0;
        if (index < 0 || index >= size()) return 0;

        FluidStack inTank = handler.getFluidInTank(index);
        if (inTank.isEmpty()) return 0;

        // Verify the tank contains the requested resource type
        if (!FluidResource.of(inTank).equals(resource)) return 0;

        FluidStack toDrain = resource.toStack(amount);
        FluidStack drained = handler.drain(toDrain, IFluidHandler.FluidAction.EXECUTE);
        return drained.getAmount();
    }
}
