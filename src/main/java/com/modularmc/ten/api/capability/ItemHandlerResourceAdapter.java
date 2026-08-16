package com.modularmc.ten.api.capability;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * A reverse adapter that wraps a legacy {@link IItemHandler} as a {@link ResourceHandler<ItemResource>}.
 *
 * <p>
 * This allows NeoForge 26.1.2 capability consumers (pipes, channels, etc.) to interact with
 * machines that still use the old {@code IItemHandler} interface internally.
 *
 * <p>
 * <b>Transaction semantics (Journal model, NeoForge 26.1.2):</b> This adapter <em>extends</em>
 * {@link SnapshotJournal} (matching the native {@code ItemStackResourceHandler} pattern).
 * Every mutating insert/extract first calls {@link #updateSnapshots(TransactionContext)} so the
 * current transaction records a before-state snapshot; if the transaction is aborted (e.g. a
 * simulate probe via {@code IItemHandler.of()}), the journal's {@link #revertToSnapshot} restores
 * the underlying handler — simulate never produces a real mutation (fixes item loss/duplication).
 *
 * <p>
 * <b>Guards:</b> Empty resources and negative amounts are rejected via
 * {@link TransferPreconditions}. Null handlers throw at construction.
 */
public class ItemHandlerResourceAdapter
                                        extends SnapshotJournal<ItemStack[]>
                                        implements ResourceHandler<ItemResource> {

    private final IItemHandler handler;

    public ItemHandlerResourceAdapter(IItemHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("handler must not be null");
        }
        this.handler = handler;
    }

    /** 事务快照：深拷贝全部槽位（abort 时恢复）。 */
    @Override
    protected ItemStack[] createSnapshot() {
        int n = handler.getSlots();
        ItemStack[] snap = new ItemStack[n];
        for (int i = 0; i < n; i++) {
            snap[i] = handler.getStackInSlot(i).copy();
        }
        return snap;
    }

    /**
     * 事务回滚：恢复快照槽位（需底层 handler 可写，即 IItemHandlerModifiable；
     * TEN 机器 handler 均为 ItemStackHandler 子类，满足）。
     */
    @Override
    protected void revertToSnapshot(ItemStack[] snapshot) {
        if (!(handler instanceof IItemHandlerModifiable modifiable)) {
            // 非可写 handler 无法回滚：不应发生（TEN 机器均 modifiable）；告警而非静默跳过，
            // 便于排查潜在的真实变更未恢复（物品丢失/复制风险）。
            org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ItemHandlerResourceAdapter.class);
            logger.warn("ItemHandlerResourceAdapter.revertToSnapshot: handler {} is not modifiable; "
                    + "transaction rollback skipped (potential item mutation not restored)", handler.getClass().getName());
            return;
        }
        int n = Math.min(snapshot.length, handler.getSlots());
        for (int i = 0; i < n; i++) {
            modifiable.setStackInSlot(i, snapshot[i]);
        }
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
        if (amount <= 0) return 0;
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (index < 0 || index >= size()) return 0;
        if (!isValid(index, resource)) return 0;

        // 先 simulate 计算可插入量（不改 handler），>0 时先记录事务快照（修改前状态），
        // 再真实插入——事务 abort 时 journal 恢复 before，simulate 不产生真实变更。
        ItemStack toInsert = resource.toStack(amount);
        ItemStack remainder = handler.insertItem(index, toInsert, true);
        int inserted = amount - remainder.getCount();
        if (inserted > 0) {
            this.updateSnapshots(transaction);
            handler.insertItem(index, toInsert, false);
        }
        return inserted;
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        if (amount <= 0) return 0;
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (index < 0 || index >= size()) return 0;

        ItemStack inSlot = handler.getStackInSlot(index);
        if (inSlot.isEmpty()) return 0;

        // Verify the slot contains the requested resource type
        if (!ItemResource.of(inSlot).equals(resource)) return 0;

        int toExtract = Math.min(amount, inSlot.getCount());
        if (toExtract <= 0) return 0;

        // 先 simulate 计算可抽取量，>0 时先记录事务快照再真实抽取（abort 时恢复）。
        ItemStack extracted = handler.extractItem(index, toExtract, true);
        int extractedCount = extracted.getCount();
        if (extractedCount > 0) {
            this.updateSnapshots(transaction);
            handler.extractItem(index, extractedCount, false);
        }
        return extractedCount;
    }
}
