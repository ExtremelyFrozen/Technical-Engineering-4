package com.modularmc.ten.api.transmission.item;

import com.modularmc.ten.api.transmission.ITransmitterProvider;
import com.modularmc.ten.api.transmission.Transmitter;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.Collection;
import java.util.UUID;

/**
 * 物品传输节点（移植自 TE4-New，即时搬运语义）：无缓冲、无运输实体。
 * 过滤由宿主 BE 覆写 isItemAllowed 提供（旧版白/黑名单变体语义，默认全放行）。
 */
public class ItemTransmitter extends Transmitter<IItemHandler, ItemNetwork, ItemTransmitter> {

    public ItemTransmitter(ITransmitterProvider tile) {
        super(tile);
    }

    public boolean isItemAllowed(ItemStack stack) {
        return true;
    }

    @Override
    public void takeShare() {
        // 非缓冲节点，拆网无份额
    }

    @Override
    public ItemNetwork createEmptyNetwork(UUID id) {
        return new ItemNetwork(id);
    }

    @Override
    public ItemNetwork createNetworkByMerging(Collection<ItemNetwork> nets) {
        return new ItemNetwork(nets);
    }

    @Override
    public boolean supportsTransmission(Transmitter<?, ?, ?> other) {
        return other instanceof ItemTransmitter;
    }

    @Override
    protected boolean isValidAcceptor(Direction side) {
        if (getLevel() == null) {
            return false;
        }
        return getLevel().getCapability(Capabilities.ItemHandler.BLOCK, getBlockPos().relative(side), side.getOpposite()) != null;
    }
}
