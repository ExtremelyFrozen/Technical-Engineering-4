package com.modularmc.ten.common.blockentity;

import com.modularmc.ten.api.blockentity.CmBlockEntity;
import com.modularmc.ten.utils.SafeOperationHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Energy cable — zero buffer, per-cable independent transfer.
 * <p>
 * Design (Pipez-style): each cable independently pushes and pulls energy
 * to/from its direct neighbors every tick. No network scanning, no root
 * election, no inter-cable coordination. Reliable and predictable.
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
     * One pass: pull from each non-cable neighbor that canExtract,
     * immediately push to first non-cable neighbor that canReceive.
     * <p>
     * All simulation before execution — no double-deduction.
     */
    private int transferOnce() {
        int rate = transferFor(getBlockState());
        int moved = 0;

        for (Direction inDir : Direction.values()) {
            BlockPos sourcePos = worldPosition.relative(inDir);
            if (level.getBlockEntity(sourcePos) instanceof CableBlockEntity) continue;

            IEnergyStorage source = TransferNetworks.getEnergy(level, sourcePos, inDir.getOpposite());
            if (source == null || !source.canExtract()) continue;

            // Simulate pull
            int pulled = source.extractEnergy(rate, true);
            if (pulled <= 0) continue;

            // Find a sink and simulate push
            int remaining = pulled;
            for (Direction outDir : Direction.values()) {
                if (remaining <= 0) break;

                BlockPos sinkPos = worldPosition.relative(outDir);
                if (sinkPos.equals(sourcePos)) continue;
                if (level.getBlockEntity(sinkPos) instanceof CableBlockEntity) continue;

                IEnergyStorage sink = TransferNetworks.getEnergy(level, sinkPos, outDir.getOpposite());
                if (sink == null || !sink.canReceive()) continue;

                int canAccept = sink.receiveEnergy(remaining, true);
                if (canAccept <= 0) continue;

                // Execute: pull from source, push to sink
                int drained = source.extractEnergy(canAccept, false);
                if (drained > 0) {
                    int accepted = sink.receiveEnergy(drained, false);
                    remaining -= accepted;
                    moved += accepted;
                }
            }

            // If we simulated a partial pull but didn't execute it,
            // we just skip — the simulated extract doesn't change state
        }

        return moved;
    }

    /**
     * Exposed capability — cables are dead ends. All transfer happens in
     * {@link #transferOnce()} via neighbor capability access.
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
