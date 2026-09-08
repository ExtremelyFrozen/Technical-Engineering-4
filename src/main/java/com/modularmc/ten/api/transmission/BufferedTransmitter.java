package com.modularmc.ten.api.transmission;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

/**
 * 缓冲型传输节点基类（移植自 TE4-New）：容量与吞吐由宿主配置注入，
 * 本地缓冲仅在网络缺位（orphan）时兜底，常态缓冲归网络所有。
 */
public abstract class BufferedTransmitter<AC, NET extends BufferedNetwork<AC, NET, BUF, T>, BUF, T extends BufferedTransmitter<AC, NET, BUF, T>>
                                         extends Transmitter<AC, NET, T> {

    public long bufferCapacity;
    public long throughput;

    protected BufferedTransmitter(ITransmitterProvider tile, long bufferCapacity, long throughput) {
        super(tile);
        this.bufferCapacity = bufferCapacity;
        this.throughput = throughput;
    }

    public long getCapacity() {
        return bufferCapacity;
    }

    public long getThroughput() {
        return throughput;
    }

    /** 拆网时上缴本地缓冲份额。 */
    public abstract BUF releaseShare();

    /** 获取本方向对端 acceptor 能力（无则 null）。 */
    public abstract @Nullable AC getAcceptor(Direction side, Level level, BlockPos targetPos);
}
