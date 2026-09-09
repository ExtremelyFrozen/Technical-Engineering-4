package com.modularmc.ten.api.capability;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

import lombok.Setter;

import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.ToIntBiFunction;

/**
 * 机器物品槽能力：槽位级插入/提取校验（回调注入），并支持
 * 批处理动态堆叠上限（setDynamicSlotLimit，输出槽 64×B 超堆叠）。
 */
@Setter
public class MachineItemHandler extends ItemStackHandler {

    private BiPredicate<Integer, ItemStack> validator;
    private Runnable changeListener = () -> {};
    private ToIntBiFunction<Integer, ItemStack> dynamicSlotLimit;

    public MachineItemHandler(int slots) {
        super(slots);
        this.validator = (slot, stack) -> true;
    }

    public MachineItemHandler(int slots, BiPredicate<Integer, ItemStack> validator) {
        super(slots);
        this.validator = validator;
    }

    public void setChangeListener(Runnable changeListener) {
        this.changeListener = changeListener != null ? changeListener : () -> {};
    }

    /**
     * Sets a dynamic slot limit provider that overrides the default 64 limit.
     * <p>
     * The provider receives (slot, candidateStack) and returns the maximum
     * number of items allowed in that slot. When active, {@link #getStackLimit}
     * bypasses {@link ItemStack#getMaxStackSize()} to permit overstacking.
     * <p>
     * Pass {@code null} to restore the default behavior (64 cap).
     * <p>
     * NOTE: This method is intentionally not annotated with {@link lombok.Setter}
     * to prevent accidental exposure via Lombok's {@code @Setter} on the field.
     * Recipe/domain dependencies must not leak into this capability handler.
     *
     * @param provider the limit provider, or null to disable
     */
    public void setDynamicSlotLimit(ToIntBiFunction<Integer, ItemStack> provider) {
        this.dynamicSlotLimit = provider;
    }

    /**
     * Returns the effective slot limit for the given slot and candidate stack.
     * Used by external callers (e.g. {@code RecipeMachineBlockEntity}) to query
     * the current dynamic limit without inserting.
     *
     * @param slot  the slot index
     * @param stack the candidate stack to insert
     * @return the effective slot limit (stack size ceiling)
     */
    public int getEffectiveSlotLimit(int slot, ItemStack stack) {
        if (dynamicSlotLimit != null) {
            int limit = dynamicSlotLimit.applyAsInt(slot, Objects.requireNonNullElse(stack, ItemStack.EMPTY));
            ItemStack existing = getStackInSlot(slot);
            if (!existing.isEmpty()) {
                limit = Math.max(limit, existing.getCount());
            }
            return limit;
        }
        return super.getSlotLimit(slot);
    }

    @Override
    public int getSlotLimit(int slot) {
        if (dynamicSlotLimit != null) {
            ItemStack existing = getStackInSlot(slot);
            int limit = dynamicSlotLimit.applyAsInt(slot, existing.isEmpty() ? ItemStack.EMPTY : existing);
            // Never go below existing count to preserve overstack
            return Math.max(limit, existing.getCount());
        }
        return super.getSlotLimit(slot);
    }

    @Override
    protected int getStackLimit(int slot, ItemStack stack) {
        if (dynamicSlotLimit != null) {
            int limit = dynamicSlotLimit.applyAsInt(slot, Objects.requireNonNullElse(stack, ItemStack.EMPTY));
            ItemStack existing = getStackInSlot(slot);
            if (!existing.isEmpty()) {
                limit = Math.max(limit, existing.getCount());
            }
            return limit;
        }
        return super.getStackLimit(slot, stack);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return validator.test(slot, stack);
    }

    @Override
    protected void onContentsChanged(int slot) {
        super.onContentsChanged(slot);
        changeListener.run();
    }
}
