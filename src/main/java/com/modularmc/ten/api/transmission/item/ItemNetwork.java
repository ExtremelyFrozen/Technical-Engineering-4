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
 * 物品网络（移植自 TE4-New，按目标工程语义裁剪）：成员图持久维护，
 * 每 5 tick 由网络对象自身执行一轮「source → 即时搬运到 sink」（网络单例驱动，无重复搬运）。
 * New 版的逐跳运输（TransitEntry/RouteFinder/运输动画）未移植——
 * 本工程管道不透明且保留即时传送语义；搬运结构直译旧版 processNetwork。
 * 连接模式门控（对齐 TE4-New 基准）：源边 ==PULL 才主动从设备抽取（默认 NORMAL 不主动抽，
 * 须扳手显式切 PULL）；受端边 isPushOrNormal（NORMAL/PUSH 接收投放，PULL 面拒投防抽投冲突）。
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
        // 对齐旧版 5 tick 传输节奏（用 gameTime 全局相位，网络重建时相位不重置）
        if (level.getGameTime() % 5 != 0) {
            return;
        }
        lastMoved = false;

        boolean moved = false;
        // 对齐旧版 5 tick 传输节奏（用 gameTime 全局相位，网络重建时相位不重置）——见上。
        // 受端（投放）门控 isPushOrNormal 语义：NORMAL/PUSH 面接收推送；
        // 源端（主动抽取）门控 ==PULL 对齐 TE4-New 基准——默认 NORMAL 不主动抽，
        // 须扳手显式切 PULL 才从该面设备抽物品（见 DuctInteractions 四态循环）。
        outer:
        for (Map.Entry<BlockPos, ItemTransmitter> sourceEntry : positionedTransmitters.entrySet()) {
            ItemTransmitter sourcePipe = sourceEntry.getValue();
            for (Direction sourceDir : Direction.values()) {
                // 源边：仅显式 PULL 面才主动从设备抽物品（TE4-New 基准；NORMAL 不主动抽）
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
