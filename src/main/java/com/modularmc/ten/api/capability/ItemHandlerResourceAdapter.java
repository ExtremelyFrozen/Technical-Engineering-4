package com.modularmc.ten.api.capability;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * A reverse adapter that wraps a legacy {@link IItemHandler} as a {@link ResourceHandler<ItemResource>}.
 *
 * <p>
 * This allows NeoForge 26.1.2 capability consumers (pipes, channels, etc.) to interact with
 * machines that still use the old {@code IItemHandler} interface internally.
 *
 * <p>
 * <b>Transaction semantics:</b> The legacy {@code IItemHandler} API does not support rollback.
 * Operations are executed immediately on the underlying handler, consistent with the existing
 * {@link CapabilityAdapters#asEnergyHandler} pattern. Callers requiring proper transaction
 * rollback should use a native {@code ResourceHandler} implementation on the BE.
 *
 * <p>
 * <b>Guards:</b> Empty resources and negative amounts are rejected via
 * {@link TransferPreconditions}. Null handlers throw at construction.
 */
public class ItemHandlerResourceAdapter implements ResourceHandler<ItemResource> {

    private final IItemHandler handler;

    public ItemHandlerResourceAdapter(IItemHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("handler must not be null");
        }
        this.handler = handler;
    }

    @Override
    public int size() {
        return handler.getSlots();
    }

    @Override
    public ItemResource getResource(int index) {
        return ItemResource.of(handler.getStackInSlot(index));
    }

    @Override
    public long getAmountAsLong(int index) {
        return handler.getStackInSlot(index).getCount();
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        if (resource == null || resource.isEmpty()) return 0;
        return handler.getSlotLimit(index);
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        if (resource == null || resource.isEmpty()) return false;
        return handler.isItemValid(index, resource.toStack(1));
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (amount <= 0) return 0;
        if (index < 0 || index >= size()) return 0;
        if (!isValid(index, resource)) return 0;

        ItemStack toInsert = resource.toStack(amount);
        ItemStack remainder = handler.insertItem(index, toInsert, false);
        return amount - remainder.getCount();
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (amount <= 0) return 0;
        if (index < 0 || index >= size()) return 0;

        ItemStack inSlot = handler.getStackInSlot(index);
        if (inSlot.isEmpty()) return 0;

        // Verify the slot contains the requested resource type
        if (!ItemResource.of(inSlot).equals(resource)) return 0;

        int toExtract = Math.min(amount, inSlot.getCount());
        if (toExtract <= 0) return 0;

        ItemStack extracted = handler.extractItem(index, toExtract, false);
        return extracted.getCount();
    }
}
