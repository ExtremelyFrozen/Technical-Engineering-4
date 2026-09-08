package com.modularmc.ten.api.transmission.item;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.function.Predicate;

/**
 * 物品搬运工具（沿袭旧版 TransferNetworks.moveItems 语义）：
 * 遍历源槽 → SIMULATE 探测双方可移量 → 实际执行，返回实际搬运数。
 */
public final class TransferUtil {

    private TransferUtil() {}

    public static int moveItems(IItemHandler from, IItemHandler to, int limit, Predicate<ItemStack> filter) {
        if (limit <= 0) {
            return 0;
        }
        for (int s = 0; s < from.getSlots(); s++) {
            ItemStack stack = from.getStackInSlot(s);
            if (stack.isEmpty() || !filter.test(stack)) {
                continue;
            }
            ItemStack extracted = from.extractItem(s, limit, true);
            if (extracted.isEmpty()) {
                continue;
            }
            int movable = insertUpTo(to, extracted, true);
            if (movable <= 0) {
                continue;
            }
            ItemStack actual = from.extractItem(s, movable, false);
            int moved = insertUpTo(to, actual, false);
            if (moved > 0) {
                return moved;
            }
        }
        return 0;
    }

    /** 全槽遍历插入（旧版 TransferNetworks.insertItem 语义），返回剩余。 */
    public static ItemStack insertItem(IItemHandler to, ItemStack stack, boolean simulate) {
        ItemStack remaining = stack.copy();
        for (int s = 0; s < to.getSlots() && !remaining.isEmpty(); s++) {
            remaining = to.insertItem(s, remaining, simulate);
        }
        return remaining;
    }

    /** 尝试向 handler 各槽插入 stack（simulate 或实际），返回成功插入的数量。 */
    private static int insertUpTo(IItemHandler to, ItemStack stack, boolean simulate) {
        int remaining = stack.getCount();
        for (int s = 0; s < to.getSlots() && remaining > 0; s++) {
            ItemStack part = stack.copyWithCount(remaining);
            ItemStack rest = to.insertItem(s, part, simulate);
            remaining = rest.getCount();
        }
        return stack.getCount() - remaining;
    }
}
