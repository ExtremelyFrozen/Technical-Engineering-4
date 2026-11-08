package com.modularmc.ten.api.capability;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

import lombok.Setter;

import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.ToIntBiFunction;

/**
 * 机器物品槽能力：槽位级插入/提取校验（回调注入），支持批处理动态堆叠上限
 * （{@link #setDynamicSlotLimit}），并自行实现超 99 堆叠的 NBT 读写。
 */
@Setter
public class MachineItemHandler extends ItemStackHandler {

    /**
     * 超堆叠数量的持久化键。vanilla {@code ItemStack.CODEC} 的 count 是
     * {@code intRange(1, 99)}，超出即编码抛错；这里以 count=1 走 vanilla 编码，
     * 真实数量单独存 int（键名与 vanilla 的 {@code count} 不冲突）。
     */
    public static final String BIG_COUNT_KEY = "BigCount";

    private BiPredicate<Integer, ItemStack> validator;
    private Runnable changeListener = () -> {};
    private ToIntBiFunction<Integer, ItemStack> dynamicSlotLimit;

    /** 以 count=1 走 vanilla 编码，真实数量写入 {@link #BIG_COUNT_KEY}。 */
    public static Tag saveStack(ItemStack stack, HolderLookup.Provider registries) {
        if (stack.isEmpty()) return new CompoundTag();
        CompoundTag tag = (CompoundTag) stack.copyWithCount(1).save(registries);
        if (stack.getCount() != 1) tag.putInt(BIG_COUNT_KEY, stack.getCount());
        return tag;
    }

    /** vanilla 解析后用 {@link #BIG_COUNT_KEY} 覆盖数量（无该键即 ≤99 的旧数据）。 */
    public static ItemStack readStack(CompoundTag tag, HolderLookup.Provider registries) {
        ItemStack stack = ItemStack.parseOptional(registries, tag);
        int big = tag.getInt(BIG_COUNT_KEY);
        if (!stack.isEmpty() && big > 0) stack.setCount(big);
        return stack;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        ListTag nbtTagList = new ListTag();
        for (int i = 0; i < getSlots(); i++) {
            ItemStack stack = getStackInSlot(i);
            if (stack.isEmpty()) continue;
            CompoundTag itemTag = (CompoundTag) saveStack(stack, provider);
            itemTag.putInt("Slot", i);
            nbtTagList.add(itemTag);
        }
        CompoundTag nbt = new CompoundTag();
        nbt.put("Items", nbtTagList);
        nbt.putInt("Size", getSlots());
        return nbt;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        setSize(nbt.contains("Size", Tag.TAG_INT) ? nbt.getInt("Size") : getSlots());
        ListTag tagList = nbt.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < tagList.size(); i++) {
            CompoundTag itemTags = tagList.getCompound(i);
            int slot = itemTags.getInt("Slot");
            if (slot >= 0 && slot < getSlots()) {
                stacks.set(slot, readStack(itemTags, provider));
            }
        }
        onLoad();
    }

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

    /**
     * 提取：以槽内实际数量为上限，不再被物品自身堆叠上限（通常 64）截断。
     * <p>
     * 父类实现取 {@code Math.min(amount, existing.getMaxStackSize())}，而本 handler 允许
     * 批处理超堆叠（单槽上限 64×B）。若沿用父类，超堆叠槽一次最多只能交出 64 个，
     * 所有「先插入目标、再按插入量扣槽」的调用方都会凭空多出物品；
     * 反之忽略返回值、只调一次的调用方（扳手拆卸）会把剩余部分留在即将移除的 BE 里，
     * 随后被方块移除逻辑撒到地上而不是进背包。
     * 这里与 {@link #getSlotLimit(int)} 的超堆叠语义对齐：上限 = 槽内现有数量。
     */
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0) return ItemStack.EMPTY;

        validateSlotIndex(slot);

        ItemStack existing = stacks.get(slot);
        if (existing.isEmpty()) return ItemStack.EMPTY;

        int toExtract = Math.min(amount, existing.getCount());
        if (toExtract <= 0) return ItemStack.EMPTY;

        if (simulate) {
            return existing.copyWithCount(toExtract);
        }

        if (toExtract >= existing.getCount()) {
            stacks.set(slot, ItemStack.EMPTY);
            onContentsChanged(slot);
            return existing;
        }

        // 超堆叠：不能 whole-copy，先取出副本再收缩原栈
        ItemStack extracted = existing.copyWithCount(toExtract);
        existing.shrink(toExtract);
        onContentsChanged(slot);
        return extracted;
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
