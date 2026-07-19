package com.modularmc.ten.api.capability;

import com.modularmc.ten.api.option.IngredientType;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.IntFunction;

/**
 * A transaction-safe {@link ResourceHandler<FluidResource>} backed by per-index
 * {@link MachineFluidTank} access.
 *
 * <p>
 * Extends {@link SnapshotJournal<FluidStack[]>} to support atomic multi-tank
 * transaction rollback. Each {@code insert}/{@code extract} call operates on exactly
 * one tank by index; generic handler-wide {@code fill}/{@code drain} are never used.
 *
 * <p>
 * <b>Read vs write gating:</b> Read methods ({@link #getResource},
 * {@link #getAmountAsLong}, {@link #getCapacityAsLong}) have no write-permission
 * checks, ensuring Jade and other capability consumers can always read tank contents.
 * Write methods respect {@code insertAllowed/extractAllowed}, side-based
 * {@link IngredientType} rules, and per-tank validation.
 *
 * <p>
 * <b>Component-aware matching:</b> Insert and extract use
 * {@link FluidResource#matches(FluidStack)} to compare resources, which covers both
 * fluid type and {@link net.minecraft.core.component.DataComponentPatch}, preventing
 * mixing of chemically distinct fluids that share the same base type.
 */
public class FluidHandlerResourceAdapter extends SnapshotJournal<FluidStack[]>
                                         implements ResourceHandler<FluidResource> {

    private final List<MachineFluidTank> tanks;
    private final IntFunction<IngredientType> tankType;
    private final BiPredicate<Integer, FluidStack> validator;
    private final BooleanSupplier insertAllowed;
    private final BooleanSupplier extractAllowed;
    private final Runnable commitCallback;

    /**
     * Creates a new adapter backed by the given tanks and policy functions.
     *
     * @param tanks          the machine's fluid tanks (ordered list)
     * @param tankType       returns the {@link IngredientType} for a given tank index
     * @param validator      returns true if a {@link FluidStack} is valid for a given tank index
     * @param insertAllowed  supplier for the current insert permission state
     * @param extractAllowed supplier for the current extract permission state
     * @param commitCallback callback invoked exactly once when a root transaction commits
     */
    public FluidHandlerResourceAdapter(
                                       List<MachineFluidTank> tanks,
                                       IntFunction<IngredientType> tankType,
                                       BiPredicate<Integer, FluidStack> validator,
                                       BooleanSupplier insertAllowed,
                                       BooleanSupplier extractAllowed,
                                       Runnable commitCallback) {
        if (tanks == null) throw new IllegalArgumentException("tanks must not be null");
        this.tanks = tanks;
        this.tankType = tankType != null ? tankType : idx -> IngredientType.BOTH;
        this.validator = validator != null ? validator : (idx, stack) -> true;
        this.insertAllowed = insertAllowed != null ? insertAllowed : () -> true;
        this.extractAllowed = extractAllowed != null ? extractAllowed : () -> true;
        this.commitCallback = commitCallback != null ? commitCallback : () -> {};
    }

    // ───── ResourceHandler read methods (no write gating) ─────

    @Override
    public int size() {
        return tanks.size();
    }

    @Override
    public FluidResource getResource(int index) {
        if (index < 0 || index >= tanks.size()) return FluidResource.of(FluidStack.EMPTY);
        return FluidResource.of(tanks.get(index).getFluid());
    }

    @Override
    public long getAmountAsLong(int index) {
        if (index < 0 || index >= tanks.size()) return 0;
        return tanks.get(index).getFluid().getAmount();
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        if (index < 0 || index >= tanks.size()) return 0;
        // Return general tank capacity regardless of resource emptiness.
        // Callers use isValid() to check whether a specific resource is accepted.
        return tanks.get(index).getCapacity();
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        if (resource == null || resource.isEmpty()) return false;
        if (index < 0 || index >= tanks.size()) return false;
        IngredientType type = tankType.apply(index);
        if (!type.canIn()) return false;
        FluidStack stack = resource.toStack(1);
        if (!validator.test(index, stack)) return false;
        return tanks.get(index).isFluidValid(stack);
    }

    // ───── ResourceHandler write methods (per-index, transaction-safe) ─────

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (amount <= 0) return 0;
        if (index < 0 || index >= tanks.size()) return 0;
        if (!insertAllowed.getAsBoolean()) return 0;

        IngredientType type = tankType.apply(index);
        if (!type.canIn()) return 0;

        FluidStack toInsert = resource.toStack(amount);
        if (!validator.test(index, toInsert)) return 0;

        MachineFluidTank tank = tanks.get(index);
        if (!tank.isFluidValid(toInsert)) return 0;

        // Calculate how much can actually fit
        FluidStack current = tank.getFluid();
        long currentAmount = current.isEmpty() ? 0 : current.getAmount();
        long capacity = tank.getCapacity();
        long canInsert = Math.min(amount, capacity - currentAmount);
        if (canInsert <= 0) return 0;

        // Component-aware compatibility: reject if tank has a mismatched resource
        if (!current.isEmpty() && !resource.matches(current)) return 0;

        // --- Transaction-safe mutation ---
        updateSnapshots(transaction);

        long totalAmount = currentAmount + canInsert;
        FluidStack newFluid = resource.toStack((int) totalAmount);
        tank.setFluid(newFluid);
        return (int) canInsert;
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (amount <= 0) return 0;
        if (index < 0 || index >= tanks.size()) return 0;
        if (!extractAllowed.getAsBoolean()) return 0;

        IngredientType type = tankType.apply(index);
        if (!type.canOut()) return 0;

        MachineFluidTank tank = tanks.get(index);
        FluidStack current = tank.getFluid();
        if (current.isEmpty()) return 0;
        // Component-aware: verify exact resource match (type + components)
        if (!resource.matches(current)) return 0;

        int canExtract = Math.min(amount, current.getAmount());
        if (canExtract <= 0) return 0;

        // --- Transaction-safe mutation ---
        updateSnapshots(transaction);

        int remaining = current.getAmount() - canExtract;
        if (remaining <= 0) {
            tank.setFluid(FluidStack.EMPTY);
        } else {
            tank.setFluid(current.copyWithAmount(remaining));
        }
        return canExtract;
    }

    // ───── SnapshotJournal: createSnapshot / revertToSnapshot / onRootCommit ─────

    @Override
    protected FluidStack[] createSnapshot() {
        FluidStack[] snapshot = new FluidStack[tanks.size()];
        for (int i = 0; i < tanks.size(); i++) {
            FluidStack current = tanks.get(i).getFluid();
            snapshot[i] = current.isEmpty() ? FluidStack.EMPTY : current.copy();
        }
        return snapshot;
    }

    @Override
    protected void revertToSnapshot(FluidStack[] snapshot) {
        int len = Math.min(snapshot.length, tanks.size());
        for (int i = 0; i < len; i++) {
            tanks.get(i).setFluid(snapshot[i].isEmpty() ? FluidStack.EMPTY : snapshot[i].copy());
        }
    }

    @Override
    protected void onRootCommit(FluidStack[] originalState) {
        commitCallback.run();
    }
}
