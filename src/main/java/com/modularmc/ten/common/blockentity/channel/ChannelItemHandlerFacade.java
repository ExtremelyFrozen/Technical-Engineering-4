package com.modularmc.ten.common.blockentity.channel;

import com.modularmc.ten.common.channel.SharedStorage;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/**
 * 物品 UI 门面 handler（零 tick 传输：接入后槽位直接绑定共享存储）。
 * <p>
 * UI 槽位在构建时绑定此门面（对象引用恒定）；每次读写经 {@link #resolve()}
 * 动态解析：
 * <ul>
 * <li>服务端接入态 → 频道共享 ItemHandler（成员共享，容量=64×成员数动态）</li>
 * <li>其余情况（未接入 / 客户端镜像）→ 本地缓冲 itemHandler（断开回流源）</li>
 * </ul>
 * 由此 join/leave 切换后无需重建 UI，槽位自动指向新后端。
 */
public final class ChannelItemHandlerFacade implements IItemHandlerModifiable {

    private final AbstractChannelBlockEntity channel;

    public ChannelItemHandlerFacade(AbstractChannelBlockEntity channel) {
        this.channel = channel;
    }

    private IItemHandlerModifiable resolve() {
        if (channel.getLevel() != null && !channel.getLevel().isClientSide()) {
            SharedStorage shared = channel.sharedStorage();
            if (shared != null) {
                return shared.getItemHandler();
            }
        }
        return channel.itemHandler;
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        resolve().setStackInSlot(slot, stack);
    }

    @Override
    public int getSlots() {
        return resolve().getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return resolve().getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return resolve().insertItem(slot, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return resolve().extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        if (channel.getLevel() != null && !channel.getLevel().isClientSide()) {
            return resolve().getSlotLimit(slot);
        }
        // 客户端接入态：本地缓冲无动态上限，按 @DescSynced 同步的成员数推算共享槽位上限
        if (channel.isJoined()) {
            return SharedStorage.BASE_ITEM_STACK * Math.max(1, channel.joinedMemberCount());
        }
        return resolve().getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return resolve().isItemValid(slot, stack);
    }
}