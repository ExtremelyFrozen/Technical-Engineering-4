package com.modularmc.ten.common.blockentity.machine;

import com.modularmc.ten.api.blockentity.RadiusMachineBlockEntity;
import com.modularmc.ten.api.option.IngredientType;
import com.modularmc.ten.api.option.MachineType;
import com.modularmc.ten.common.gui.TENMachineBlockUIFactory;
import com.modularmc.ten.utils.ItemNBTHelper;
import com.modularmc.ten.utils.WorkingHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;

import java.util.List;

/**
 * 方块破坏器（Block Breaker）。
 * 套用采矿场 GUI：槽 0 工具（镐），槽 1..12 输出。
 * 工作范围由面板的 XYZ 范围与 XYZ 相对偏移决定（世界轴向三轴盒子），不再随批处理 B 变化；
 * 范围升级只抬高可调节半径上限。
 */
public class BlockBreakerBlockEntity extends RadiusMachineBlockEntity {

    public BlockBreakerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
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
        return MachineType.BLOCK_BREAKER;
    }

    @Override
    public int inventorySize() {
        return 13;
    }

    @Override
    public IngredientType slotType(int slot) {
        return slot == 0 ? IngredientType.INPUT : IngredientType.OUTPUT;
    }

    @Override
    public boolean valid(int slot, ItemStack stack) {
        return slot == 0 ? stack.has(DataComponents.TOOL) : true;
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
            // 采矿场式布局：槽0 工具 + 槽1..12 输出
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
            root.addChild(TENMachineBlockUIFactory.useEnchantmentsToggleButton(this, 48, 48));
        });
    }

    @Override
    public void applyEffect() {
        if (level == null || itemHandler == null) return;
        ItemStack tool = itemHandler.getStackInSlot(0);
        if (tool.isEmpty()) return;
        var bounds = adjustedBounds();
        int scanned = 0;
        for (BlockPos cursor : BlockPos.betweenClosed(bounds.min(), bounds.max())) {
            // betweenClosed 复用 MutableBlockPos：写操作前取不可变拷贝，避免引用被后续迭代改写
            BlockPos target = cursor.immutable();
            if (++scanned > ADJUST_SCAN_LIMIT || tool.isEmpty()) break;
            if (target.equals(worldPosition)) continue;
            BlockState state = level.getBlockState(target);
            if (state.isAir() || state.hasBlockEntity()) continue;
            if (!tool.isCorrectToolForDrops(state)) continue;
            List<ItemStack> drops = state.getDrops(WorkingHelper.getLootBuilder(level, target, effectiveToolForDrops(tool)));
            if (drops.isEmpty()) continue;
            if (!canFitAll(drops)) break;
            fitAll(drops);
            level.destroyBlock(target, false);
            ItemNBTHelper.damage(tool, level, 1);
        }
    }

    @Override
    public double effectInterval() {
        return 3;
    }

    @Override
    public boolean conditionStart() {
        if (level == null || itemHandler == null) return false;
        ItemStack tool = itemHandler.getStackInSlot(0);
        if (tool.isEmpty()) return false;
        var bounds = adjustedBounds();
        int scanned = 0;
        for (BlockPos cursor : BlockPos.betweenClosed(bounds.min(), bounds.max())) {
            // betweenClosed 复用 MutableBlockPos：写操作前取不可变拷贝，避免引用被后续迭代改写
            BlockPos target = cursor.immutable();
            if (++scanned > ADJUST_SCAN_LIMIT) break;
            if (target.equals(worldPosition)) continue;
            BlockState state = level.getBlockState(target);
            if (!state.isAir() && !state.hasBlockEntity() && tool.isCorrectToolForDrops(state)) return true;
        }
        return false;
    }

    @Override
    public boolean cooking() {
        if (itemHandler == null) return false;
        // 范围不再由批处理决定：只有输出槽全满才阻塞；单格掉落放不下由 canFitAll 兜底。
        for (int i = 1; i < itemHandler.getSlots(); i++) {
            ItemStack existing = itemHandler.getStackInSlot(i);
            if (existing.isEmpty() || existing.getCount() < itemHandler.getSlotLimit(i)) return false;
        }
        return true;
    }

    // ─── 输出槽快照+模拟+提交（防丢失） ───

    private boolean canFitAll(List<ItemStack> stacks) {
        int outputStart = 1, slotCount = itemHandler.getSlots() - outputStart;
        ItemStack[] simulated = copyOutputSlots(outputStart, slotCount);
        for (ItemStack stack : stacks) {
            if (!simulateInsert(simulated, stack.copy(), outputStart).isEmpty()) return false;
        }
        return true;
    }

    private ItemStack[] copyOutputSlots(int start, int count) {
        ItemStack[] copy = new ItemStack[count];
        for (int i = 0; i < count; i++) {
            ItemStack s = itemHandler.getStackInSlot(start + i);
            copy[i] = s.isEmpty() ? ItemStack.EMPTY : s.copy();
        }
        return copy;
    }

    private ItemStack simulateInsert(ItemStack[] slots, ItemStack stack, int outputStart) {
        for (int j = 0; j < slots.length && !stack.isEmpty(); j++) {
            if (slots[j].isEmpty()) {
                int limit = itemHandler.getSlotLimit(outputStart + j);
                if (stack.getCount() <= limit) {
                    slots[j] = stack;
                    stack = ItemStack.EMPTY;
                } else {
                    ItemStack fill = stack.copy();
                    fill.setCount(limit);
                    slots[j] = fill;
                    stack.shrink(limit);
                }
            } else if (ItemStack.isSameItem(slots[j], stack)) {
                int room = itemHandler.getSlotLimit(outputStart + j) - slots[j].getCount();
                int moved = Math.min(room, stack.getCount());
                if (moved > 0) {
                    slots[j].grow(moved);
                    stack.shrink(moved);
                }
            }
        }
        return stack;
    }

    private void fitAll(List<ItemStack> stacks) {
        int outputStart = 1, slotCount = itemHandler.getSlots() - outputStart;
        ItemStack[] snapshot = copyOutputSlots(outputStart, slotCount);
        for (ItemStack stack : stacks) {
            ItemStack remaining = simulateInsert(snapshot, stack.copy(), outputStart);
            if (!remaining.isEmpty()) throw new IllegalStateException("BlockBreaker cannot fit all drops");
        }
        for (int i = 0; i < slotCount; i++) itemHandler.setStackInSlot(outputStart + i, snapshot[i]);
    }
}
