package com.modularmc.ten.api.transmission.energy;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.energy.IEnergyStorage;

import org.jetbrains.annotations.Nullable;

/**
 * 线缆对外能量能力适配层（移植自 TE4-New）：
 * 收发均受所在边连接模式门控（收须 isPullOrNormal、取须 isPushOrNormal）
 * 与节点吞吐限流；有网络时读写网络缓冲，orphan 时读写本地兜底缓冲。
 * NOTE: side == null 时模式判定为 NONE（全部拒绝）——注册能力时须提供具体方向。
 */
public class TransmitterEnergyStorage implements IEnergyStorage {

    private final EnergyTransmitter transmitter;
    private final @Nullable Direction side;

    public TransmitterEnergyStorage(EnergyTransmitter transmitter, @Nullable Direction side) {
        this.transmitter = transmitter;
        this.side = side;
    }

    private @Nullable EnergyNetwork net() {
        return transmitter.getNetwork();
    }

    private long buf() {
        EnergyNetwork n = net();
        return n != null ? n.getBuffer() : transmitter.getBuffer();
    }

    private long cap() {
        EnergyNetwork n = net();
        return n != null ? n.getCapacity() : transmitter.getCapacity();
    }

    @Override
    public int receiveEnergy(int max, boolean sim) {
        if (!transmitter.getConnectionTypeRaw(side).isPullOrNormal()) {
            return 0;
        }
        max = (int) Math.min(max, transmitter.getThroughput());
        long space = cap() - buf();
        int toAdd = (int) Math.min(Math.min(space, max), Integer.MAX_VALUE);
        if (toAdd > 0 && !sim) {
            EnergyNetwork n = net();
            if (n != null) {
                n.setBuffer(n.getBuffer() + toAdd);
            } else {
                transmitter.setBuffer(transmitter.getBuffer() + toAdd);
            }
        }
        return toAdd;
    }

    @Override
    public int extractEnergy(int max, boolean sim) {
        if (!transmitter.getConnectionTypeRaw(side).isPushOrNormal()) {
            return 0;
        }
        max = (int) Math.min(max, transmitter.getThroughput());
        long cur = buf();
        int toExtract = (int) Math.min(Math.min(cur, max), Integer.MAX_VALUE);
        if (toExtract > 0 && !sim) {
            EnergyNetwork n = net();
            if (n != null) {
                n.setBuffer(n.getBuffer() - toExtract);
            } else {
                transmitter.setBuffer(transmitter.getBuffer() - toExtract);
            }
        }
        return toExtract;
    }

    @Override
    public int getEnergyStored() {
        return (int) Math.min(buf(), Integer.MAX_VALUE);
    }

    @Override
    public int getMaxEnergyStored() {
        return (int) Math.min(cap(), Integer.MAX_VALUE);
    }

    @Override
    public boolean canExtract() {
        return transmitter.getConnectionTypeRaw(side).isPushOrNormal() && buf() > 0;
    }

    @Override
    public boolean canReceive() {
        return transmitter.getConnectionTypeRaw(side).isPullOrNormal() && buf() < cap();
    }
}
