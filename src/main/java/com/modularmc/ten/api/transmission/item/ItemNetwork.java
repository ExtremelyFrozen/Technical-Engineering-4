package com.modularmc.ten.api.transmission.item;

import com.modularmc.ten.api.transmission.ConnectionType;
import com.modularmc.ten.api.transmission.ITransmitterProvider;
import com.modularmc.ten.api.transmission.Network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * 物品网络：成员图持久维护，由网络对象统一执行 source → sink 即时搬运，每 5 tick 使用 gameTime 全局相位。
 * 源边仅 PULL 主动从设备抽取；受端边仅 PUSH/NORMAL 接收投放，PULL 面拒投以避免抽投冲突。
 */
public class ItemNetwork extends Network<IItemHandler, ItemNetwork, ItemTransmitter> {

    private boolean lastMoved;

    public ItemNetwork(UUID id) {
        super(id);
    }

    public ItemNetwork(Collection<ItemNetwork> nets) {
        super(UUID.randomUUID());
        adoptAllAndRegister(nets);
    }

    /** 上一传输周期是否发生搬运（BE 活性状态显示用）。 */
    public boolean hasLastMoved() {
        return lastMoved;
    }

    @Override
    public void onUpdate() {
        Level level = firstLevel();
        if (level == null) {
            return;
        }
        // 按 gameTime 的全局 5 tick 相位执行，网络重建不重置相位。
        if (level.getGameTime() % 5 != 0) {
            return;
        }
        lastMoved = false;

        boolean moved = false;
        outer:
        for (Map.Entry<BlockPos, ItemTransmitter> sourceEntry : positionedTransmitters.entrySet()) {
            ItemTransmitter sourcePipe = sourceEntry.getValue();
            for (Direction sourceDir : Direction.values()) {
                // 源边：仅显式 PULL 面才主动从设备抽物品，NORMAL 不主动抽。
                if (sourcePipe.getConnectionTypeRaw(sourceDir) != ConnectionType.PULL) {
                    continue;
                }
                BlockPos sourcePos = sourceEntry.getKey().relative(sourceDir);
                if (positionedTransmitters.containsKey(sourcePos) || level.getBlockEntity(sourcePos) instanceof ITransmitterProvider) {
                    continue;
                }
                IItemHandler source = level.getCapability(Capabilities.ItemHandler.BLOCK, sourcePos, sourceDir.getOpposite());
                if (source == null) {
                    continue;
                }
                for (Map.Entry<BlockPos, ItemTransmitter> sinkEntry : positionedTransmitters.entrySet()) {
                    ItemTransmitter sinkPipe = sinkEntry.getValue();
                    for (Direction sinkDir : Direction.values()) {
                        // 受端边：管道向该设备投放物品，须为 PUSH/NORMAL 模式（与能量层语义一致）
                        if (!sinkPipe.getConnectionTypeRaw(sinkDir).isPushOrNormal()) {
                            continue;
                        }
                        BlockPos sinkPos = sinkEntry.getKey().relative(sinkDir);
                        if (sinkPos.equals(sourcePos) || positionedTransmitters.containsKey(sinkPos) || level.getBlockEntity(sinkPos) instanceof ITransmitterProvider) {
                            continue;
                        }
                        IItemHandler sink = level.getCapability(Capabilities.ItemHandler.BLOCK, sinkPos, sinkDir.getOpposite());
                        if (sink == null) {
                            continue;
                        }
                        if (TransferUtil.moveItems(source, sink, 1, sourcePipe::isItemAllowed) > 0) {
                            moved = true;
                            break outer;
                        }
                    }
                }
            }
        }
        lastMoved = moved;
    }

    private @Nullable Level firstLevel() {
        for (ItemTransmitter t : positionedTransmitters.values()) {
            Level level = t.getLevel();
            if (level != null) {
                return level;
            }
        }
        return null;
    }
}
