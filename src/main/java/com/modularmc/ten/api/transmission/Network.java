package com.modularmc.ten.api.transmission;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 传输网络基类（移植自 TE4-New）：服务端权威图结构，成员按 BlockPos 索引。
 * 拓扑变更（加入/合并/分裂）由 {@link TransmitterNetworkRegistry} 在 level tick
 * 内集中延迟处理，防 chunk 加载顺序问题。
 * 裁剪：客户端网络镜像未移植（本工程渲染走 blockstate，无客户端网络需求）。
 */
public abstract class Network<AC, NET extends Network<AC, NET, T>, T extends Transmitter<AC, NET, T>> {

    public final Map<BlockPos, T> positionedTransmitters = new HashMap<>();
    public final Set<T> transmittersToAdd = new HashSet<>();
    private final UUID uuid;

    protected Network(UUID id) {
        this.uuid = id;
    }

    public UUID getUUID() {
        return uuid;
    }

    @SuppressWarnings("unchecked")
    NET self() {
        return (NET) this;
    }

    /** 将待加入成员正式收编：逐台改挂网络归属并登记，随后回调新增钩子。 */
    public void commit() {
        if (transmittersToAdd.isEmpty()) {
            return;
        }
        List<T> toUpdate = new ArrayList<>();
        for (T t : transmittersToAdd) {
            if (t != null && t.isValid()) {
                for (Direction d : Direction.values()) {
                    acceptorChanged(t, d);
                }
                if (t.setNetwork(self(), false)) {
                    toUpdate.add(t);
                }
                addTransmitterFromCommit(t);
            }
        }
        transmittersToAdd.clear();
        if (!toUpdate.isEmpty()) {
            onTransmittersAdded(toUpdate);
            toUpdate.forEach(Transmitter::requestsUpdate);
        }
    }

    protected void onTransmittersAdded(List<T> added) {}

    void addNewTransmitters(Collection<T> ts) {
        transmittersToAdd.addAll(ts);
    }

    protected void addTransmitterFromCommit(T t) {
        positionedTransmitters.put(t.getBlockPos(), t);
    }

    public @Nullable T getTransmitter(BlockPos pos) {
        return positionedTransmitters.get(pos);
    }

    public Collection<T> getTransmitters() {
        return positionedTransmitters.values();
    }

    public int size() {
        return positionedTransmitters.size();
    }

    public boolean isEmpty() {
        return positionedTransmitters.isEmpty();
    }

    public void addTransmitter(T t) {
        positionedTransmitters.put(t.getBlockPos(), t);
    }

    public void removeTransmitter(T t) {
        if (getTransmitter(t.getBlockPos()) == t) {
            positionedTransmitters.remove(t.getBlockPos());
        }
        if (isEmpty()) {
            deregister();
        }
    }

    /**
     * 网络失效（成员拆除）：清无效成员 → 有效成员取回缓冲份额
     * （{@link Transmitter#validateAndTakeShare}，BufferedNetwork 子类保证无损分流）
     * → 逐台重新入队组网 → 注销自身。
     */
    public void invalidate(@Nullable T trigger) {
        removeInvalid(trigger);
        for (T t : getTransmitters()) {
            if (t.isValid()) {
                t.validateAndTakeShare();
                t.setNetwork(null, false);
                TransmitterNetworkRegistry.join(t);
            }
        }
        deregister();
    }

    protected void removeInvalid(@Nullable T trigger) {
        getTransmitters().removeIf(t -> !t.isValid());
    }

    protected List<T> adoptFrom(NET other) {
        List<T> toUpdate = new ArrayList<>();
        for (Map.Entry<BlockPos, T> e : other.positionedTransmitters.entrySet()) {
            T t = e.getValue();
            positionedTransmitters.put(e.getKey(), t);
            if (t.setNetwork(self(), false)) {
                toUpdate.add(t);
            }
        }
        transmittersToAdd.addAll(other.transmittersToAdd);
        return toUpdate;
    }

    protected void adoptAllAndRegister(Collection<NET> nets) {
        for (NET n : nets) {
            if (n != null && n != this) {
                adoptFrom(n);
                n.deregister();
            }
        }
        register();
    }

    public void register() {
        TransmitterNetworkRegistry.registerNetwork(this);
    }

    public void deregister() {
        positionedTransmitters.clear();
        transmittersToAdd.clear();
        TransmitterNetworkRegistry.removeNetwork(this);
    }

    /** 连接模式/对端能力变化钩子（子类按需失效缓存）。 */
    public void acceptorChanged(T t, Direction side) {}

    /** 每 level tick 的网络行为入口（传输逻辑），由注册表驱动。 */
    public void onUpdate() {}

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Network<?, ?, ?> other && uuid.equals(other.uuid);
    }
}
