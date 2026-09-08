package com.modularmc.ten.api.transmission;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;

/**
 * 缓冲型网络基类（移植自 TE4-New）：网络级容量 = 成员容量之和，
 * 全网吞吐 = 各成员吞吐的最小值（木桶效应）。
 * 裁剪：内容液位 scale（无内容渲染）与 chunk 追踪（New 版无消费点的死数据）未移植。
 */
public abstract class BufferedNetwork<AC, NET extends BufferedNetwork<AC, NET, BUF, T>, BUF, T extends BufferedTransmitter<AC, NET, BUF, T>>
                                     extends Network<AC, NET, T> {

    protected long capacity;
    private long cachedThroughput = -1;

    protected BufferedNetwork(UUID id) {
        super(id);
    }

    public abstract @Nullable BUF getBuffer();

    /** 新成员入网时上缴其本地缓冲。 */
    public abstract void absorbBuffer(T transmitter);

    /** 容量/成员变化后收敛缓冲，防越界。 */
    public abstract void clampBuffer();

    protected void updateCapacity(T transmitter) {
        long c = transmitter.getCapacity();
        capacity = capacity > Long.MAX_VALUE - c ? Long.MAX_VALUE : capacity + c;
    }

    protected void updateCapacity() {
        long sum = 0;
        for (T t : getTransmitters()) {
            long c = t.getCapacity();
            sum = sum > Long.MAX_VALUE - c ? Long.MAX_VALUE : sum + c;
        }
        capacity = sum;
    }

    public long getCapacity() {
        return capacity;
    }

    public long netThroughput() {
        if (cachedThroughput < 0) {
            long min = Long.MAX_VALUE;
            for (T t : getTransmitters()) {
                min = Math.min(min, t.getThroughput());
            }
            cachedThroughput = min;
        }
        return cachedThroughput;
    }

    protected void invalidateThroughput() {
        cachedThroughput = -1;
    }

    @Override
    protected void onTransmittersAdded(List<T> added) {
        clampBuffer();
        invalidateThroughput();
    }

    @Override
    protected void addTransmitterFromCommit(T t) {
        super.addTransmitterFromCommit(t);
        updateCapacity(t);
        absorbBuffer(t);
        invalidateThroughput();
    }

    @Override
    protected void removeInvalid(@Nullable T trigger) {
        super.removeInvalid(trigger);
        clampBuffer();
        invalidateThroughput();
    }

    @Override
    protected List<T> adoptFrom(NET other) {
        List<T> list = super.adoptFrom(other);
        updateCapacity();
        invalidateThroughput();
        return list;
    }

    /**
     * 通用均分分发：收集网络边界上的唯一 acceptor（跳过网络成员与其它 transmitter 邻位），
     * 第一轮各取均分份额，第二轮把余量塞给还能收的，返回实际送出量。
     */
    protected <H extends AC> long emitToAcceptors(long amount, BiFunction<H, Long, Long> sendFn) {
        List<H> acceptors = new ArrayList<>();
        Level level = null;
        for (Map.Entry<BlockPos, T> e : positionedTransmitters.entrySet()) {
            BlockPos pos = e.getKey();
            T transmitter = e.getValue();
            if (level == null) {
                level = transmitter.getLevel();
            }
            if (level == null) {
                continue;
            }
            for (Direction d : Direction.values()) {
                BlockPos target = pos.relative(d);
                if (positionedTransmitters.containsKey(target)) {
                    continue;
                }
                if (level.getBlockEntity(target) instanceof ITransmitterProvider) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                H cap = (H) transmitter.getAcceptor(d, level, target);
                if (cap != null) {
                    acceptors.add(cap);
                }
            }
        }
        if (acceptors.isEmpty() || amount <= 0) {
            return 0;
        }

        int n = acceptors.size();
        long share = amount / n;
        long totalSent = 0;

        for (H a : acceptors) {
            totalSent += sendFn.apply(a, share);
        }

        long leftover = amount - totalSent;
        if (leftover > 0) {
            for (H a : acceptors) {
                long sent = sendFn.apply(a, leftover);
                totalSent += sent;
                leftover -= sent;
                if (leftover <= 0) {
                    break;
                }
            }
        }
        return totalSent;
    }
}
