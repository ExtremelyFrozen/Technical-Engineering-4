package com.modularmc.ten.api.transmission;

import com.modularmc.ten.TEN;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 传输网络注册表（移植自 TE4-New）：服务端权威网络池 + 延迟组网队列。
 * 组网/拆网由 {@link #onLevelTick} 在 level tick 末集中处理，避免
 * BE 加载顺序与放置/拆除事件中的并发修改问题。
 * 裁剪：客户端网络镜像与流体冲突特效未移植（本工程无流体管道）。
 */
@EventBusSubscriber(modid = TEN.MOD_ID)
public final class TransmitterNetworkRegistry {

    private static final Set<Network<?, ?, ?>> networks = new HashSet<>();
    private static final Set<Transmitter<?, ?, ?>> pendingJoins = new HashSet<>();
    private static final Set<Transmitter<?, ?, ?>> pendingRemovals = new HashSet<>();
    private static int pruneCounter;

    private TransmitterNetworkRegistry() {}

    /** game bus 自动注册：每个服务端 level tick 末驱动拆网→组网→网络行为→清理。 */
    @SubscribeEvent
    public static void onLevelTickEvent(LevelTickEvent.Post event) {
        onLevelTick(event.getLevel());
    }

    public static void onLevelTick(Level level) {
        if (level.isClientSide()) {
            return;
        }
        processPendingRemovals();
        processPendingJoins();
        for (Network<?, ?, ?> net : networks) {
            net.onUpdate();
        }
        if (++pruneCounter >= 100) {
            pruneCounter = 0;
            networks.removeIf(Network::isEmpty);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void processPendingJoins() {
        if (pendingJoins.isEmpty()) {
            return;
        }
        Set<Transmitter<?, ?, ?>> batch = new HashSet<>(pendingJoins);
        pendingJoins.clear();

        for (Transmitter start : batch) {
            if (!start.isValid() || start.isRemote()) {
                continue;
            }
            Level world = start.getLevel();
            if (world == null) {
                continue;
            }

            Set<Transmitter> connected = new HashSet<>();
            Set<Network> foundNetworks = new HashSet<>();
            Set<BlockPos> visited = new HashSet<>();
            Deque<BlockPos> queue = new ArrayDeque<>();

            queue.add(start.getBlockPos());
            while (!queue.isEmpty()) {
                BlockPos pos = queue.pollFirst();
                if (!visited.add(pos)) {
                    continue;
                }
                if (!(world.getBlockEntity(pos) instanceof ITransmitterProvider tb)) {
                    continue;
                }
                Transmitter t = tb.getTransmitter();
                if (!t.isValid() || !start.supportsTransmission(t)) {
                    continue;
                }

                Network net = t.getNetwork();
                if (net != null) {
                    foundNetworks.add(net);
                    continue;
                }
                connected.add(t);

                for (Direction d : Direction.values()) {
                    BlockPos next = pos.relative(d);
                    if (!visited.contains(next) && world.getBlockEntity(next) instanceof ITransmitterProvider ntb && t.isValidTransmitterBasic(ntb, d)) {
                        queue.addLast(next);
                    }
                }
            }

            Network network;
            if (foundNetworks.isEmpty()) {
                network = start.createEmptyNetwork(UUID.randomUUID());
                network.register();
            } else if (foundNetworks.size() == 1) {
                network = foundNetworks.iterator().next();
            } else {
                network = start.createNetworkByMerging(foundNetworks);
            }

            for (Transmitter t : connected) {
                Network oldNet = t.getNetwork();
                if (oldNet != null && oldNet != network) {
                    oldNet.removeTransmitter(t);
                    t.setNetwork(null, false);
                }
            }

            network.addNewTransmitters(connected);
            network.commit();
            for (Transmitter t : connected) {
                t.refreshConnections();
            }
        }
    }

    private static void processPendingRemovals() {
        if (pendingRemovals.isEmpty()) {
            return;
        }
        Set<Transmitter<?, ?, ?>> batch = new HashSet<>(pendingRemovals);
        pendingRemovals.clear();

        for (Transmitter removed : batch) {
            if (removed.isRemote()) {
                continue;
            }
            Network oldNet = removed.getNetwork();
            if (oldNet != null) {
                // 缓冲无损分流由 Network.invalidate() 内 takeShare 分配到剩余成员
                oldNet.invalidate(removed);
            }
            removed.setNetwork(null, false);
        }
    }

    public static void registerNetwork(Network<?, ?, ?> net) {
        networks.add(net);
    }

    public static void removeNetwork(Network<?, ?, ?> net) {
        networks.remove(net);
    }

    public static void join(Transmitter<?, ?, ?> t) {
        if (!t.isRemote()) {
            pendingJoins.add(t);
        }
    }

    public static void remove(Transmitter<?, ?, ?> removed) {
        if (!removed.isRemote()) {
            pendingRemovals.add(removed);
        }
    }

    public static int networkCount() {
        return networks.size();
    }
}
