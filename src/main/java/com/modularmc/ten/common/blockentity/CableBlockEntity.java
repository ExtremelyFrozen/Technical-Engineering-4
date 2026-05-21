package com.modularmc.ten.common.blockentity;

import com.modularmc.ten.api.blockentity.CmBlockEntity;
import com.modularmc.ten.utils.SafeOperationHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.HashSet;
import java.util.Set;

/**
 * Energy cable with tiny tick-buffer — Pipez-style design.
 * <p>
 * Each tick:
 * <ol>
 * <li>Pull from connected generators → buffer (up to transfer rate)</li>
 * <li>Push buffer → connected consumers (skip generator positions)</li>
 * </ol>
 * The buffer acts as a one-way valve: energy flows source→cable→sink,
 * never backwards.
 * <p>
 * No energy capability registered — cables are invisible to capability queries.
 */
public class CableBlockEntity extends CmBlockEntity {

    private int tickBuffer = 0;

    public CableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected void tick() {
        if (level == null || level.isClientSide()) return;
        int moved = transferOnce();
        setActive(moved > 0);
    }

    private int transferOnce() {
        int rate = transferFor(getBlockState());
        int moved = 0;
        Set<BlockPos> pulledFrom = new HashSet<>();

        // Phase 1: Pull from sources into buffer (track source positions)
        if (tickBuffer < rate) {
            int space = rate - tickBuffer;
            for (Direction dir : Direction.values()) {
                if (space <= 0) break;
                BlockPos neighbor = worldPosition.relative(dir);
                if (level.getBlockEntity(neighbor) instanceof CableBlockEntity) continue;
                IEnergyStorage source = TransferNetworks.getEnergy(level, neighbor, dir.getOpposite());
                if (source == null || !source.canExtract()) continue;
                int pulled = source.extractEnergy(space, false);
                if (pulled > 0) {
                    pulledFrom.add(neighbor);
                    tickBuffer += pulled;
                    space -= pulled;
                    moved += pulled;
                }
            }
        }

        // Phase 2: Push buffer to consumers (skip source positions)
        if (tickBuffer > 0) {
            for (Direction dir : Direction.values()) {
                if (tickBuffer <= 0) break;
                BlockPos neighbor = worldPosition.relative(dir);
                if (level.getBlockEntity(neighbor) instanceof CableBlockEntity) continue;
                if (pulledFrom.contains(neighbor)) continue; // Skip positions we just pulled from
                IEnergyStorage sink = TransferNetworks.getEnergy(level, neighbor, dir.getOpposite());
                if (sink == null || !sink.canReceive()) continue;
                int accepted = sink.receiveEnergy(tickBuffer, false);
                tickBuffer -= accepted;
                moved += accepted;
            }
        }

        return moved;
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
