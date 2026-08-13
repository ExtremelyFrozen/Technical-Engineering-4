package com.modularmc.ten.common.blockentity;

import com.modularmc.ten.api.capability.CapabilityAdapters;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

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

    /**
     * root 前剪枝（sound，不完备）：是否存在按 POS_COMPARATOR（x→y→z，与 isRoot 最小判定同序）
     * 比 self 更小的相邻网络节点。相邻管道必属同一网络，故存在更小邻居 ⇒ self 必非网络最小者
     * （root），可免 BFS 直接返回。不会误剪 root（root 无更小邻居）；非 root 但无更小邻居的节点
     * 仍需 BFS 后由 isRoot 判定（剪枝不完备但 sound，用于 PipeBlockEntity.tick 的局部短路）。
     */
    public static boolean hasSmallerNeighbor(Level level, BlockPos self, NetworkNodePredicate predicate) {
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = self.relative(direction);
            if (predicate.test(level, neighbor) && POS_COMPARATOR.compare(neighbor, self) < 0) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static IEnergyStorage getEnergy(Level level, BlockPos pos, @Nullable Direction side) {
        return CapabilityAdapters.getEnergy(level, pos, side);
    }

    @Nullable
    public static IItemHandler getItems(Level level, BlockPos pos, @Nullable Direction side) {
        return CapabilityAdapters.getItems(level, pos, side);
    }

    @Nullable
    public static IFluidHandler getFluids(Level level, BlockPos pos, @Nullable Direction side) {
        return CapabilityAdapters.getFluids(level, pos, side);
    }

    public static ItemStack insertItem(IItemHandler handler, ItemStack stack, boolean simulate) {
        ItemStack remaining = stack;
        for (int slot = 0; slot < handler.getSlots() && !remaining.isEmpty(); slot++) {
            remaining = handler.insertItem(slot, remaining, simulate);
        }
        return remaining;
    }
}
