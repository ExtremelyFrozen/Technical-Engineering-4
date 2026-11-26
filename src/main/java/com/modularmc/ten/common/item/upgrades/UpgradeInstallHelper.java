// -*- coding: utf-8 -*-
package com.modularmc.ten.common.item.upgrades;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import org.jetbrains.annotations.Nullable;

import java.util.function.BiPredicate;

/**
 * 升级安装的纯逻辑辅助。
 * <p>
 * 方法无状态，可在无游戏环境时测试；供
 * {@link com.modularmc.ten.common.block.machine.BaseMachineBlock} 的普通右键快速安装使用。
 */
public final class UpgradeInstallHelper {

    private UpgradeInstallHelper() {}

    /**
     * Find the first empty slot in the given item handler.
     *
     * @param handler the upgrade item handler (nullable)
     * @return slot index, or -1 if null or all slots occupied
     */
    public static int findFirstEmptySlot(@Nullable IItemHandler handler) {
        if (handler == null) return -1;
        for (int i = 0; i < handler.getSlots(); i++) {
            if (handler.getStackInSlot(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Attempt to install a single upgrade item into the first empty slot.
     * <p>
     * Only performs the insertion if:
     * <ul>
     * <li>handler is not null</li>
     * <li>stack is not empty</li>
     * <li>the stack passes the validator at the target slot</li>
     * <li>there is an empty slot available</li>
     * <li>the full stack (count=1) can be inserted without remainder</li>
     * </ul>
     *
     * @param handler   the upgrade handler
     * @param stack     the upgrade item stack (only 1 will be inserted)
     * @param validator slot+stack validator (e.g. machine.validUpgrade)
     * @return true if the item was fully inserted
     */
    public static boolean tryInstall(IItemHandler handler, ItemStack stack,
                                     BiPredicate<Integer, ItemStack> validator) {
        if (handler == null || stack.isEmpty()) return false;
        int slot = findFirstEmptySlot(handler);
        if (slot < 0) return false;
        if (!validator.test(slot, stack)) return false;

        ItemStack toInsert = stack.copyWithCount(1);
        ItemStack remainder = handler.insertItem(slot, toInsert, false);
        return remainder.isEmpty();
    }

    /**
     * Convenience overload without explicit validator (uses handler's own {@code isItemValid}).
     */
    public static boolean tryInstall(IItemHandler handler, ItemStack stack) {
        if (handler == null || stack.isEmpty()) return false;
        int slot = findFirstEmptySlot(handler);
        if (slot < 0) return false;
        if (!handler.isItemValid(slot, stack)) return false;

        ItemStack toInsert = stack.copyWithCount(1);
        ItemStack remainder = handler.insertItem(slot, toInsert, false);
        return remainder.isEmpty();
    }
}
