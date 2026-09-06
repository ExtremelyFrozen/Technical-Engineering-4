package com.modularmc.ten.common.blockentity.machine;

import com.modularmc.ten.api.blockentity.RadiusMachineBlockEntity;
import com.modularmc.ten.api.option.IngredientType;
import com.modularmc.ten.api.option.MachineType;
import com.modularmc.ten.common.gui.TENMachineBlockUIFactory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;

import java.util.List;

/**
 * 方块成型器（Block Former）。
 * 槽 0 当前放置物块（输入），槽 1..12 候选栏。沿面向方向 B 格（深度）×宽轴 ±(radius-1) 放置。
 */
public class BlockFormerBlockEntity extends RadiusMachineBlockEntity {

    public BlockFormerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setCapacity(kFE(20));
        setEfficiency(10);
        initialRadius = 1;
        radius = 1;
    }

    @Override
    public int machineType() {
        return MachineType.BLOCK_FORMER;
    }

    @Override
    public int inventorySize() {
        return 13;
    }

    @Override
    public IngredientType slotType(int slot) {
        return slot == 0 ? IngredientType.INPUT : IngredientType.BOTH;
    }

    @Override
    public boolean valid(int slot, ItemStack stack) {
        return stack.getItem() instanceof BlockItem;
    }

    @Override
    public IngredientType tankType(int tank) {
        return IngredientType.IGNORE;
    }

    @Override
    public boolean valid(int slot, FluidStack stack) {
        return true;
    }

    @Override
    public List<net.minecraft.world.phys.AABB> getRangeBoxes() {
        int b = Math.max(1, getTheoreticalBatchSize());
        int w = Math.max(1, radius);
        Direction facing = getFacing();
        int mx = worldPosition.getX(), my = worldPosition.getY(), mz = worldPosition.getZ();
        int x1, y1, z1, x2, y2, z2;
        switch (facing) {
            case NORTH -> {
                x1 = mx - (w - 1);
                x2 = mx + w;
                y1 = my;
                y2 = my + 1;
                z1 = mz - b;
                z2 = mz;
            }
            case SOUTH -> {
                x1 = mx - (w - 1);
                x2 = mx + w;
                y1 = my;
                y2 = my + 1;
                z1 = mz + 1;
                z2 = mz + 1 + b;
            }
            case EAST -> {
                x1 = mx + 1;
                x2 = mx + 1 + b;
                y1 = my;
                y2 = my + 1;
                z1 = mz - (w - 1);
                z2 = mz + w;
            }
            default -> {
                x1 = mx - b;
                x2 = mx;
                y1 = my;
                y2 = my + 1;
                z1 = mz - (w - 1);
                z2 = mz + w;
            }
        }
        return List.of(new net.minecraft.world.phys.AABB(x1, y1, z1, x2, y2, z2));
    }

    @Override
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        return buildMachineUI(holder, TENMachineBlockUIFactory.backgroundFor(machineType()), root -> {
            // 采矿场式布局，但槽位语义为：槽0 输入（当前放置）、槽1..12 候选栏
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 0, 43, 34));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 1, 79, 16));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 2, 97, 16));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 3, 115, 16));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 4, 133, 16));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 5, 79, 34));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 6, 97, 34));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 7, 115, 34));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 8, 133, 34));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 9, 79, 52));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 10, 97, 52));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 11, 115, 52));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 12, 133, 52));
        }, root -> {
            root.addChild(TENMachineBlockUIFactory.energyGaugeModular(this, 8, 18, true));
            root.addChild(TENMachineBlockUIFactory.progressGaugeWide(this, 48, 74, true));
            root.addChild(TENMachineBlockUIFactory.rangeDisplayToggleButton(this));
        });
    }

    @Override
    public void applyEffect() {
        if (level == null || itemHandler == null) return;
        int B = getLockedBatchSize();
        int w = Math.max(1, radius);
        for (int i = 1; i <= B; i++) {
            for (int j = -(w - 1); j <= (w - 1); j++) {
                ItemStack stack = itemHandler.getStackInSlot(0);
                if (!(stack.getItem() instanceof BlockItem blockItem)) break;
                BlockPos target = offsetTarget(i, j);
                BlockState targetState = level.getBlockState(target);
                if (!targetState.isAir() && !targetState.canBeReplaced()) continue;
                BlockState placeState = blockItem.getBlock().defaultBlockState();
                if (!placeState.canSurvive(level, target)) continue;
                level.setBlock(target, placeState, Block.UPDATE_ALL);
                stack.shrink(1);
                if (stack.isEmpty()) refillFromCandidates();
                setChanged();
            }
        }
    }

    private BlockPos offsetTarget(int i, int j) {
        return switch (getFacing()) {
            case NORTH -> worldPosition.offset(j, 0, -i);
            case SOUTH -> worldPosition.offset(j, 0, i);
            case EAST -> worldPosition.offset(i, 0, j);
            default -> worldPosition.offset(-i, 0, j);
        };
    }

    @Override
    public double effectInterval() {
        return 5;
    }

    @Override
    public boolean conditionStart() {
        if (level == null || itemHandler == null) return false;
        ItemStack stack = itemHandler.getStackInSlot(0);
        if (!(stack.getItem() instanceof BlockItem blockItem)) return false;
        int B = getTheoreticalBatchSize();
        int w = Math.max(1, radius);
        for (int i = 1; i <= B; i++) {
            for (int j = -(w - 1); j <= (w - 1); j++) {
                BlockPos target = offsetTarget(i, j);
                BlockState targetState = level.getBlockState(target);
                if ((targetState.isAir() || targetState.canBeReplaced()) && blockItem.getBlock().defaultBlockState().canSurvive(level, target)) return true;
            }
        }
        return false;
    }

    private void refillFromCandidates() {
        for (int i = 1; i < itemHandler.getSlots(); i++) {
            ItemStack cand = itemHandler.getStackInSlot(i);
            if (cand.isEmpty()) continue;
            ItemStack move = cand.copy();
            move.setCount(1);
            itemHandler.setStackInSlot(0, move);
            cand.shrink(1);
            itemHandler.setStackInSlot(i, cand);
            return;
        }
    }
}
