package com.modularmc.ten.api.transmission;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

/**
 * 传输节点宿主接口（BE 实现，移植自 TE4-New，有裁剪）：
 * New 版的 sendUpdatePacket/payload 同步未移植——连接外观由
 * CableBased.CONNECTION blockstate 驱动，notifyChanges() 走 markDirty
 * （setChanged + sendBlockUpdated）即可刷新模型。
 */
public interface ITransmitterProvider {

    BlockPos getBlockPos();

    @Nullable
    Level getLevel();

    boolean isInvalid();

    /** 状态变化后驱动持久化与 blockstate 刷新（实现为 markDirty）。 */
    void notifyChanges();

    Transmitter<?, ?, ?> getTransmitter();
}
