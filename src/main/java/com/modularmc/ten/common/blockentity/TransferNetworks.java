package com.modularmc.ten.common.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public final class TransferNetworks {

    private static final Comparator<BlockPos> POS_COMPARATOR = Comparator
            .comparingInt((BlockPos pos) -> pos.getX())
            .thenComparingInt(pos -> pos.getY())
            .thenComparingInt(pos -> pos.getZ());

    private TransferNetworks() {}

    public interface NetworkNodePredicate {

        boolean test(Level level, BlockPos pos);
    }

    public static Set<BlockPos> collectConnected(Level level, BlockPos start, NetworkNodePredicate predicate) {
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        if (!predicate.test(level, start)) {
            return visited;
        }
        visited.add(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            BlockPos current = queue.removeFirst();
            for (Direction direction : Direction.values()) {
                BlockPos next = current.relative(direction);
                if (visited.contains(next) || !predicate.test(level, next)) {
                    continue;
                }
                visited.add(next);
                queue.addLast(next);
            }
        }
        return visited;
    }

    public static boolean isRoot(Set<BlockPos> network, BlockPos self) {
        BlockPos min = network.stream().min(POS_COMPARATOR).orElse(self);
        return self.equals(min);
    }

    @Nullable
    public static IEnergyStorage getEnergy(Level level, BlockPos pos, @Nullable Direction side) {
        return level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, side);
    }

    @Nullable
    public static IItemHandler getItems(Level level, BlockPos pos, @Nullable Direction side) {
        return level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
    }

    @Nullable
    public static IFluidHandler getFluids(Level level, BlockPos pos, @Nullable Direction side) {
        return level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side);
    }

    public static int moveEnergy(IEnergyStorage from, IEnergyStorage to, int limit, boolean simulate) {
        if (limit <= 0 || !from.canExtract() || !to.canReceive()) {
            return 0;
        }
        int extracted = from.extractEnergy(limit, true);
        if (extracted <= 0) {
            return 0;
        }
        int accepted = to.receiveEnergy(extracted, true);
        int moved = Math.min(extracted, accepted);
        if (moved <= 0) {
            return 0;
        }
        if (!simulate) {
            int drained = from.extractEnergy(moved, false);
            to.receiveEnergy(drained, false);
        }
        return moved;
    }

    public static int moveFluid(IFluidHandler from, IFluidHandler to, int limit, boolean simulate) {
        if (limit <= 0) {
            return 0;
        }
        FluidStack drained = from.drain(limit, IFluidHandler.FluidAction.SIMULATE);
        if (drained.isEmpty()) {
            return 0;
        }
        int accepted = to.fill(drained, IFluidHandler.FluidAction.SIMULATE);
        int moved = Math.min(drained.getAmount(), accepted);
        if (moved <= 0) {
            return 0;
        }
        if (!simulate) {
            FluidStack extracted = from.drain(moved, IFluidHandler.FluidAction.EXECUTE);
            to.fill(extracted, IFluidHandler.FluidAction.EXECUTE);
        }
        return moved;
    }

    public static int moveItems(IItemHandler from, IItemHandler to, int limit, Predicate<ItemStack> filter, boolean simulate) {
        if (limit <= 0) {
            return 0;
        }
        for (int slot = 0; slot < from.getSlots(); slot++) {
            ItemStack extractedSim = from.extractItem(slot, limit, true);
            if (extractedSim.isEmpty() || !filter.test(extractedSim)) {
                continue;
            }
            ItemStack remaining = insertItem(to, extractedSim.copy(), true);
            int moved = extractedSim.getCount() - remaining.getCount();
            if (moved <= 0) {
                continue;
            }
            if (!simulate) {
                ItemStack extracted = from.extractItem(slot, moved, false);
                ItemStack leftover = insertItem(to, extracted.copy(), false);
                moved = extracted.getCount() - leftover.getCount();
            }
            return moved;
        }
        return 0;
    }

    public static ItemStack insertItem(IItemHandler handler, ItemStack stack, boolean simulate) {
        ItemStack remaining = stack;
        for (int slot = 0; slot < handler.getSlots() && !remaining.isEmpty(); slot++) {
            remaining = handler.insertItem(slot, remaining, simulate);
        }
        return remaining;
    }
}
