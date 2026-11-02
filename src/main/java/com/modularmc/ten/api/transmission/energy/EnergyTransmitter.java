package com.modularmc.ten.api.transmission.energy;

import com.modularmc.ten.api.transmission.BufferedTransmitter;
import com.modularmc.ten.api.transmission.ITransmitterProvider;
import com.modularmc.ten.api.transmission.Transmitter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.UUID;

/**
 * 能量传输节点（移植自 TE4-New）：本地 buffer 仅在网络缺位（orphan）时兜底，
 * 常态能量归网络所有；拆网时经 takeShare 拿回均分份额。
 */
public class EnergyTransmitter extends BufferedTransmitter<IEnergyStorage, EnergyNetwork, Long, EnergyTransmitter> {

    private long buffer;

    public EnergyTransmitter(ITransmitterProvider tile, long capacity, long throughput) {
        super(tile, capacity, throughput);
    }

    @Override
    public Long releaseShare() {
        long share = buffer;
        buffer = 0;
        return share;
    }

    @Override
    public @Nullable IEnergyStorage getAcceptor(Direction side, Level level, BlockPos targetPos) {
        return level.getCapability(Capabilities.EnergyStorage.BLOCK, targetPos, side.getOpposite());
    }

    public long getBuffer() {
        return buffer;
    }

    public void setBuffer(long v) {
        buffer = v;
    }

    public long getNetworkEnergy() {
        EnergyNetwork net = getNetwork();
        return net != null ? net.getBuffer() : 0;
    }

    @Override
    public EnergyNetwork createEmptyNetwork(UUID id) {
        return new EnergyNetwork(id);
    }

    @Override
    public EnergyNetwork createNetworkByMerging(Collection<EnergyNetwork> nets) {
        return new EnergyNetwork(nets);
    }

    @Override
    public boolean supportsTransmission(Transmitter<?, ?, ?> other) {
        return other instanceof EnergyTransmitter;
    }

    @Override
    protected boolean isValidAcceptor(Direction side) {
        if (getLevel() == null) {
            return false;
        }
        return getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, getBlockPos().relative(side), side.getOpposite()) != null;
    }

    @Override
    public void takeShare() {
        EnergyNetwork net = getNetwork();
        if (net != null) {
            int size = net.getTransmitters().size();
            if (size > 0) {
                long share = net.getBuffer() / size;
                net.setBuffer(net.getBuffer() - share);
                buffer = share;
            }
        }
    }
}
