package com.modularmc.ten.api.transmission.energy;

import com.modularmc.ten.api.transmission.BufferedNetwork;
import com.modularmc.ten.api.transmission.ConnectionType;
import com.modularmc.ten.api.transmission.ITransmitterProvider;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 能量网络（移植自 TE4-New，int FE 适配）：两阶段传输——
 * 先从 PULL 边收集能源拉入网络缓冲，再向 PUSH/NORMAL 边的受端均分推送。
 * 内部用 long 累计（多线缆聚合），IEnergyStorage 边界处限幅 int。
 */
public class EnergyNetwork extends BufferedNetwork<IEnergyStorage, EnergyNetwork, Long, EnergyTransmitter> {

    private long buffer;
    private long lastMoved;

    public EnergyNetwork(UUID id) {
        super(id);
    }

    public EnergyNetwork(Collection<EnergyNetwork> nets) {
        super(UUID.randomUUID());
        adoptAllAndRegister(nets);
    }

    @Override
    public Long getBuffer() {
        return buffer;
    }

    public void setBuffer(long v) {
        buffer = v;
    }

    /** 上一传输周期实际搬运总量（拉+推），供 BE 活性状态显示。 */
    public long getLastMoved() {
        return lastMoved;
    }

    @Override
    public void absorbBuffer(EnergyTransmitter t) {
        long s = t.releaseShare();
        if (s > 0) {
            buffer = Math.min(buffer + s, capacity);
        }
    }

    @Override
    public void clampBuffer() {
        if (buffer > capacity) {
            buffer = capacity;
        }
    }

    @Override
    protected List<EnergyTransmitter> adoptFrom(EnergyNetwork other) {
        List<EnergyTransmitter> list = super.adoptFrom(other);
        if (other.buffer != 0) {
            buffer = Math.min(buffer + other.buffer, capacity);
            other.buffer = 0;
        }
        return list;
    }

    @Override
    public void onUpdate() {
        if (positionedTransmitters.isEmpty()) {
            return;
        }
        Level level = firstLevel();
        if (level == null) {
            return;
        }
        // 对齐旧版 5 tick 传输节奏（gameTime 全局相位，网络重建不重置相位）
        if (level.getGameTime() % 5 != 0) {
            return;
        }
        lastMoved = 0;
        long throughput = netThroughput();
        if (throughput <= 0) {
            return;
        }

        // 0. PULL：从显式 PULL 模式的边拉能源入缓冲
        Set<Edge> pullSeen = new HashSet<>();
        List<IEnergyStorage> producers = new ArrayList<>();
        for (Map.Entry<BlockPos, EnergyTransmitter> e : positionedTransmitters.entrySet()) {
            BlockPos pos = e.getKey();
            EnergyTransmitter cable = e.getValue();
            for (Direction d : Direction.values()) {
                if (cable.getConnectionTypeRaw(d) != ConnectionType.PULL) {
                    continue;
                }
                BlockPos target = pos.relative(d);
                if (positionedTransmitters.containsKey(target) || level.getBlockEntity(target) instanceof ITransmitterProvider) {
                    continue;
                }
                if (!pullSeen.add(new Edge(target, d.getOpposite()))) {
                    continue;
                }
                IEnergyStorage src = level.getCapability(Capabilities.EnergyStorage.BLOCK, target, d.getOpposite());
                if (src != null && src.canExtract()) {
                    producers.add(src);
                }
            }
        }

        long space = Math.min(capacity - buffer, throughput);
        long totalPulled = 0;
        if (space > 0 && !producers.isEmpty()) {
            long toPull = space;
            List<IEnergyStorage> srcs = new ArrayList<>(producers);
            while (!srcs.isEmpty() && toPull > 0) {
                long share = toPull / srcs.size();
                if (share == 0) {
                    share = toPull;
                }
                Iterator<IEnergyStorage> it = srcs.iterator();
                while (it.hasNext()) {
                    IEnergyStorage src = it.next();
                    int pulled = src.extractEnergy((int) Math.min(share, Integer.MAX_VALUE), false);
                    totalPulled += pulled;
                    toPull -= pulled;
                    if (pulled < share) {
                        it.remove();
                    }
                }
            }
            buffer += totalPulled;
        }
        lastMoved += totalPulled;
        if (buffer <= 0) {
            return;
        }

        // 1. PUSH：向 PUSH/NORMAL 边的受端均分推送，送不出的留在缓冲
        Set<Edge> pushSeen = new HashSet<>();
        List<IEnergyStorage> acceptors = new ArrayList<>();
        for (Map.Entry<BlockPos, EnergyTransmitter> e : positionedTransmitters.entrySet()) {
            BlockPos pos = e.getKey();
            EnergyTransmitter tr = e.getValue();
            for (Direction d : Direction.values()) {
                if (!tr.getConnectionTypeRaw(d).isPushOrNormal()) {
                    continue;
                }
                BlockPos target = pos.relative(d);
                if (positionedTransmitters.containsKey(target) || level.getBlockEntity(target) instanceof ITransmitterProvider) {
                    continue;
                }
                if (!pushSeen.add(new Edge(target, d.getOpposite()))) {
                    continue;
                }
                IEnergyStorage cap = level.getCapability(Capabilities.EnergyStorage.BLOCK, target, d.getOpposite());
                if (cap != null && cap.canReceive() && cap.getEnergyStored() < cap.getMaxEnergyStored()) {
                    acceptors.add(cap);
                }
            }
        }

        long toSend = Math.min(buffer, throughput);
        long totalSent = 0;
        List<IEnergyStorage> needy = new ArrayList<>(acceptors);
        while (!needy.isEmpty() && toSend > 0) {
            long share = toSend / needy.size();
            if (share == 0) {
                share = toSend;
            }
            Iterator<IEnergyStorage> it = needy.iterator();
            while (it.hasNext()) {
                IEnergyStorage a = it.next();
                int sent = a.receiveEnergy((int) Math.min(share, Integer.MAX_VALUE), false);
                totalSent += sent;
                toSend -= sent;
                if (sent < share || a.getEnergyStored() >= a.getMaxEnergyStored()) {
                    it.remove();
                }
            }
        }
        buffer -= totalSent;
        lastMoved += totalSent;
    }

    private @Nullable Level firstLevel() {
        for (EnergyTransmitter t : positionedTransmitters.values()) {
            Level level = t.getLevel();
            if (level != null) {
                return level;
            }
        }
        return null;
    }

    /** 去重的边标识：同一受端被多台线缆相邻时只计一次。 */
    private record Edge(BlockPos pos, Direction side) {}
}
