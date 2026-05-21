package com.modularmc.ten.common.blockentity;

import com.modularmc.ten.api.blockentity.CmBlockEntity;
import com.modularmc.ten.utils.SafeOperationHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Energy cable with zero internal buffer.
 * <p>
 * Design (inspired by EnderIO conduits):
 * <ul>
 * <li>No internal energy storage — cables are pure pass-through</li>
 * <li>Root cable (lowest coordinate in network) performs transfer each tick</li>
 * <li>Pulls from generators → pushes directly to consumers, no buffer in between</li>
 * <li>Each tick processes one source→sink pair to keep it simple and fair</li>
 * </ul>
 */
public class CableBlockEntity extends CmBlockEntity {

    public CableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /**
     * Returns a capability wrapper that treats the cable as a dead-end.
     * Real transfer happens in {@link #redistribute()} on the root cable.
     */
    public IEnergyStorage getEnergy(Direction side) {
        int rate = transferFor(getBlockState());
        return new IEnergyStorage() {

            @Override
            public int receiveEnergy(int amount, boolean simulate) {
                if (amount <= 0 || !canReceive()) return 0;
                // On-demand forward to consumers
                return forwardToConsumers(rate, amount, simulate);
            }

            @Override
            public int extractEnergy(int amount, boolean simulate) {
                return 0; // Cables do not store energy
            }

            @Override
            public int getEnergyStored() {
                return 0;
            }

            @Override
            public int getMaxEnergyStored() {
                return rate;
            }

            @Override
            public boolean canExtract() {
                return false;
            }

            @Override
            public boolean canReceive() {
                return true;
            }
        };
    }

    public boolean hasUi() {
        return false;
    }

    // ────────── Tick: root cable orchestrates transfer ──────────

    @Override
    protected void tick() {
        if (level == null || level.isClientSide() || getAliveTime() % 5 != 0) {
            return;
        }
        if (!isNetworkRoot()) {
            return;
        }
        int moved = redistribute();
        setActive(moved > 0);
    }

    private boolean isNetworkRoot() {
        Set<BlockPos> network = TransferNetworks.collectConnected(level, worldPosition,
                (lvl, pos) -> lvl.getBlockEntity(pos) instanceof CableBlockEntity);
        return TransferNetworks.isRoot(network, worldPosition);
    }

    /**
     * Pull energy from all connected generators and push to all connected consumers.
     * No intermediate buffer — energy passes through in the same tick.
     */
    private int redistribute() {
        Set<BlockPos> network = TransferNetworks.collectConnected(level, worldPosition,
                (lvl, pos) -> lvl.getBlockEntity(pos) instanceof CableBlockEntity);
        if (network.isEmpty()) return 0;

        int rate = transferFor(getBlockState());
        int moved = 0;

        // Collect sources (canExtract) and sinks (canReceive), deduplicated by position
        Map<BlockPos, IEnergyStorage> sources = new LinkedHashMap<>();
        Map<BlockPos, IEnergyStorage> sinks = new LinkedHashMap<>();

        for (BlockPos cablePos : network) {
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = cablePos.relative(dir);
                if (network.contains(neighbor)) continue;
                IEnergyStorage cap = TransferNetworks.getEnergy(level, neighbor, dir.getOpposite());
                if (cap == null) continue;
                if (cap.canExtract()) sources.putIfAbsent(neighbor, cap);
                if (cap.canReceive()) sinks.putIfAbsent(neighbor, cap);
            }
        }

        if (sources.isEmpty() || sinks.isEmpty()) return 0;

        // Move energy: each source → each sink (TransferNetworks.moveEnergy handles simulation)
        for (var sourceEntry : sources.entrySet()) {
            BlockPos sourcePos = sourceEntry.getKey();
            IEnergyStorage source = sourceEntry.getValue();
            for (var sinkEntry : sinks.entrySet()) {
                if (sinkEntry.getKey().equals(sourcePos)) continue; // Skip self
                moved += TransferNetworks.moveEnergy(source, sinkEntry.getValue(), rate, false);
            }
        }

        return moved;
    }

    // ────────── On-demand forwarding when energy enters the cable ──────────

    /**
     * Forward incoming energy directly to connected consumers.
     * Called from {@link #getEnergy(Direction)} when a generator pushes into the cable.
     */
    private int forwardToConsumers(int rate, int amount, boolean simulate) {
        Set<BlockPos> network = TransferNetworks.collectConnected(level, worldPosition,
                (lvl, pos) -> lvl.getBlockEntity(pos) instanceof CableBlockEntity);
        if (network.isEmpty()) return 0;

        int toDistribute = Math.min(amount, rate);
        int totalAccepted = 0;

        Map<BlockPos, IEnergyStorage> seenConsumers = new LinkedHashMap<>();
        for (BlockPos cablePos : network) {
            for (Direction dir : Direction.values()) {
                if (toDistribute <= 0) break;
                BlockPos neighbor = cablePos.relative(dir);
                if (network.contains(neighbor) || seenConsumers.containsKey(neighbor)) continue;
                IEnergyStorage cap = TransferNetworks.getEnergy(level, neighbor, dir.getOpposite());
                if (cap == null || !cap.canReceive()) continue;
                seenConsumers.put(neighbor, cap);
                int accepted = cap.receiveEnergy(toDistribute, simulate);
                totalAccepted += accepted;
                toDistribute -= accepted;
            }
        }
        return totalAccepted;
    }

    // ────────── Rate / tier lookup ──────────

    private static int transferFor(BlockState state) {
        String name = SafeOperationHelper.regNameOf(state.getBlock());
        if ("cable_star".equals(name)) return 200_000;
        if ("cable_azure".equals(name)) return 4_000;
        if ("cable_quartz".equals(name)) return 1_000;
        return 200;
    }

    // ────────── Visual active state ──────────

    private void setActive(boolean active) {
        if (level == null) return;
        BlockState state = getBlockState();
        var prop = com.modularmc.ten.common.block.machine.BaseMachineBlock.ACTIVE;
        if (state.hasProperty(prop) && state.getValue(prop) != active) {
            level.setBlock(worldPosition, state.setValue(prop, active), 3);
        }
    }
}
