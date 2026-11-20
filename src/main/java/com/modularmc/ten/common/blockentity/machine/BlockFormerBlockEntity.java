package com.modularmc.ten.common.blockentity.machine;

import com.modularmc.ten.api.blockentity.RadiusMachineBlockEntity;
import com.modularmc.ten.api.option.IngredientType;
import com.modularmc.ten.api.option.MachineType;
import com.modularmc.ten.common.gui.TENMachineBlockUIFactory;

import net.minecraft.core.BlockPos;
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
 * 槽 0 当前放置物块（输入），槽 1..12 候选栏。
 * 工作范围由面板的 XYZ 范围与 XYZ 相对偏移决定（世界轴向三轴盒子），不再随并行运行 B 变化；
 * 范围升级只抬高可调节半径上限。
 */
public class BlockFormerBlockEntity extends RadiusMachineBlockEntity {

    public BlockFormerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setCapacity(kFE(20));
        setEfficiency(10);
        initialRadius = 1;
        radius = 1;
    }

    /** 本机改用面板范围：可调节半径基准从 ADJUST_BASE_CAP 起步，范围升级每件翻倍。 */
    @Override
    protected void resetUpgradeEffects() {
        super.resetUpgradeEffects();
        adjustRangeCap = ADJUST_BASE_CAP;
    }

    @Override
    public boolean supportsRangeAdjust() {
        return true;
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
        // 线框与实际遍历共用同一个工作盒：中心 = 机器位置 + XYZ 偏移，半径 = range/2 方块、每轴方块数 = range。
        return List.of(adjustedRangeBox());
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
        var bounds = adjustedBounds();
        int scanned = 0;
        for (BlockPos cursor : BlockPos.betweenClosed(bounds.min(), bounds.max())) {
            // betweenClosed 复用 MutableBlockPos：写操作前取不可变拷贝，避免引用被后续迭代改写
            BlockPos target = cursor.immutable();
            if (++scanned > ADJUST_SCAN_LIMIT) break;
            if (target.equals(worldPosition)) continue;
            ItemStack stack = itemHandler.getStackInSlot(0);
            if (!(stack.getItem() instanceof BlockItem blockItem)) break;
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

    @Override
    public double effectInterval() {
        return 5;
    }

    @Override
    public boolean conditionStart() {
        if (level == null || itemHandler == null) return false;
        ItemStack stack = itemHandler.getStackInSlot(0);
        if (!(stack.getItem() instanceof BlockItem blockItem)) return false;
        var bounds = adjustedBounds();
        int scanned = 0;
        for (BlockPos cursor : BlockPos.betweenClosed(bounds.min(), bounds.max())) {
            // betweenClosed 复用 MutableBlockPos：写操作前取不可变拷贝，避免引用被后续迭代改写
            BlockPos target = cursor.immutable();
            if (++scanned > ADJUST_SCAN_LIMIT) break;
            if (target.equals(worldPosition)) continue;
            BlockState targetState = level.getBlockState(target);
            if ((targetState.isAir() || targetState.canBeReplaced()) && blockItem.getBlock().defaultBlockState().canSurvive(level, target)) return true;
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
