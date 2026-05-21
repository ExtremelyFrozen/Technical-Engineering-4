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

/**
 * Energy cable — zero buffer, per-cable independent transfer.
 * <p>
 * Design (simplified Pipez-style): each cable collects all neighbor sources
 * and sinks, then transfers from each source to each sink. No two-phase
 * direction iteration — prevents source↔sink loopback.
 */
public class CableBlockEntity extends CmBlockEntity {

    public CableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected void tick() {
        if (level == null || level.isClientSide()) return;
        int moved = transferOnce();
        setActive(moved > 0);
    }

    /**
     * Two-phase transfer: collect sources & sinks first, then move energy.
     * No direction-based loopback (source != sink positions).
     */
    private int transferOnce() {
        int rate = transferFor(getBlockState());
        int moved = 0;

        // Phase 1: collect all sources and sinks (deduplicated by position)
        Map<BlockPos, IEnergyStorage> sources = new LinkedHashMap<>();
        Map<BlockPos, IEnergyStorage> sinks = new LinkedHashMap<>();

        for (Direction dir : Direction.values()) {
            BlockPos neighbor = worldPosition.relative(dir);
            if (level.getBlockEntity(neighbor) instanceof CableBlockEntity) continue;
            IEnergyStorage cap = TransferNetworks.getEnergy(level, neighbor, dir.getOpposite());
            if (cap == null) continue;
            // Source: canExtract AND has extractable energy right now
            if (cap.canExtract()) {
                int testPull = cap.extractEnergy(rate, true);
                if (testPull > 0) {
                    sources.put(neighbor, cap);
                    continue; // skip adding as sink — prevent loopback
                }
            }
            // Sink: canReceive AND not already a source
            if (cap.canReceive()) sinks.putIfAbsent(neighbor, cap);
        }

        if (sources.isEmpty() || sinks.isEmpty()) return 0;

        // Phase 2: each source → each sink (skip self-loop where source == sink position)
        for (var srcEntry : sources.entrySet()) {
            BlockPos srcPos = srcEntry.getKey();
            IEnergyStorage src = srcEntry.getValue();
            for (var snkEntry : sinks.entrySet()) {
                if (snkEntry.getKey().equals(srcPos)) continue;
                moved += TransferNetworks.moveEnergy(src, snkEntry.getValue(), rate, false);
            }
        }

        return moved;
    }

    /**
     * Exposed capability — dead end. All transfer happens in {@link #transferOnce()}.
     */
    public IEnergyStorage getEnergy(Direction side) {
        int rate = transferFor(getBlockState());
        return new IEnergyStorage() {

            @Override
            public int receiveEnergy(int amt, boolean sim) {
                return 0;
            }

            @Override
            public int extractEnergy(int amt, boolean sim) {
                return 0;
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
                return false;
            }
        };
    }

    public boolean hasUi() {
        return false;
    }

    private static int transferFor(BlockState state) {
        String name = SafeOperationHelper.regNameOf(state.getBlock());
        if ("cable_star".equals(name)) return 200_000;
        if ("cable_azure".equals(name)) return 4_000;
        if ("cable_quartz".equals(name)) return 1_000;
        return 200;
    }

    private void setActive(boolean active) {
        if (level == null) return;
        BlockState state = getBlockState();
        var prop = com.modularmc.ten.common.block.machine.BaseMachineBlock.ACTIVE;
        if (state.hasProperty(prop) && state.getValue(prop) != active) {
            level.setBlock(worldPosition, state.setValue(prop, active), 3);
        }
    }
}
