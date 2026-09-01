package com.modularmc.ten.common.blockentity.machine;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.blockentity.RadiusMachineBlockEntity;
import com.modularmc.ten.api.option.IngredientType;
import com.modularmc.ten.api.option.MachineType;
import com.modularmc.ten.common.gui.TENMachineBlockUIFactory;
import com.modularmc.ten.utils.ItemNBTHelper;
import com.modularmc.ten.utils.WorkingHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
 * 套用采矿场 GUI：槽 0 工具（镐），槽 1..12 输出。沿面向方向 B 格（深度）×宽轴 ±(radius-1)。
 */
public class BlockBreakerBlockEntity extends RadiusMachineBlockEntity {

    public BlockBreakerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setCapacity(kFE(20));
        setEfficiency(10);
        initialRadius = 1;
        radius = 1;
    }

    @Override public int machineType() { return MachineType.BLOCK_BREAKER; }
    @Override public int inventorySize() { return 13; }

    @Override
    public IngredientType slotType(int slot) {
        return slot == 0 ? IngredientType.INPUT : IngredientType.OUTPUT;
    }

    @Override
    public boolean valid(int slot, ItemStack stack) {
        return slot == 0 ? stack.has(DataComponents.TOOL) : true;
    }

    @Override public IngredientType tankType(int tank) { return IngredientType.IGNORE; }
    @Override public boolean valid(int slot, FluidStack stack) { return true; }

    @Override
    public List<net.minecraft.world.phys.AABB> getRangeBoxes() {
        int b = Math.max(1, getTheoreticalBatchSize());
        int w = Math.max(1, radius);
        Direction facing = getFacing();
        int mx = worldPosition.getX(), my = worldPosition.getY(), mz = worldPosition.getZ();
        int x1, y1, z1, x2, y2, z2;
        switch (facing) {
            case NORTH -> { x1 = mx - (w - 1); x2 = mx + w; y1 = my; y2 = my + 1; z1 = mz - b; z2 = mz; }
            case SOUTH -> { x1 = mx - (w - 1); x2 = mx + w; y1 = my; y2 = my + 1; z1 = mz + 1; z2 = mz + 1 + b; }
            case EAST  -> { x1 = mx + 1; x2 = mx + 1 + b; y1 = my; y2 = my + 1; z1 = mz - (w - 1); z2 = mz + w; }
            default    -> { x1 = mx - b; x2 = mx; y1 = my; y2 = my + 1; z1 = mz - (w - 1); z2 = mz + w; }
        }
        return List.of(new net.minecraft.world.phys.AABB(x1, y1, z1, x2, y2, z2));
    }

    @Override
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        return buildMachineUI(holder, TEN.id("textures/gui/machine_gui.png"), root -> {
            root.addChild(TENMachineBlockUIFactory.machineSlot(this, 0, 43, 34));
            root.addChild(TENMachineBlockUIFactory.machineSlot(this, 1, 79, 16));
            root.addChild(TENMachineBlockUIFactory.machineSlot(this, 2, 97, 16));
            root.addChild(TENMachineBlockUIFactory.machineSlot(this, 3, 115, 16));
            root.addChild(TENMachineBlockUIFactory.machineSlot(this, 4, 133, 16));
            root.addChild(TENMachineBlockUIFactory.machineSlot(this, 5, 79, 34));
            root.addChild(TENMachineBlockUIFactory.machineSlot(this, 6, 97, 34));
            root.addChild(TENMachineBlockUIFactory.machineSlot(this, 7, 115, 34));
            root.addChild(TENMachineBlockUIFactory.machineSlot(this, 8, 133, 34));
            root.addChild(TENMachineBlockUIFactory.machineSlot(this, 9, 79, 52));
            root.addChild(TENMachineBlockUIFactory.machineSlot(this, 10, 97, 52));
            root.addChild(TENMachineBlockUIFactory.machineSlot(this, 11, 115, 52));
            root.addChild(TENMachineBlockUIFactory.machineSlot(this, 12, 133, 52));
        }, root -> {
            root.addChild(TENMachineBlockUIFactory.energyGauge(this, 8, 18, 14, 46, 0, 0, true));
            root.addChild(TENMachineBlockUIFactory.progressGauge(this, 48, 74, 80, 5, 97, 0, false));
        });
    }

    @Override
    public void applyEffect() {
        if (level == null || itemHandler == null) return;
        installDynamicOutputLimit();
        ItemStack tool = itemHandler.getStackInSlot(0);
        if (tool.isEmpty()) return;
        int B = getLockedBatchSize();
        int w = Math.max(1, radius);
        for (int i = 1; i <= B; i++) {
            for (int j = -(w - 1); j <= (w - 1); j++) {
                if (tool.isEmpty()) break;
                BlockPos target = offsetTarget(i, j);
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
    }

    private BlockPos offsetTarget(int i, int j) {
        return switch (getFacing()) {
            case NORTH -> worldPosition.offset(j, 0, -i);
            case SOUTH -> worldPosition.offset(j, 0, i);
            case EAST -> worldPosition.offset(i, 0, j);
            default -> worldPosition.offset(-i, 0, j);
        };
    }

    @Override public double effectInterval() { return 3; }

    @Override
    public boolean conditionStart() {
        if (level == null || itemHandler == null) return false;
        ItemStack tool = itemHandler.getStackInSlot(0);
        if (tool.isEmpty()) return false;
        int B = getTheoreticalBatchSize();
        int w = Math.max(1, radius);
        for (int i = 1; i <= B; i++) {
            for (int j = -(w - 1); j <= (w - 1); j++) {
                BlockState state = level.getBlockState(offsetTarget(i, j));
                if (!state.isAir() && !state.hasBlockEntity() && tool.isCorrectToolForDrops(state)) return true;
            }
        }
        return false;
    }

    @Override
    public boolean cooking() {
        if (itemHandler == null) return false;
        int B = getLockedBatchSize();
        int units = 0;
        for (int i = 1; i < itemHandler.getSlots(); i++) {
            ItemStack existing = itemHandler.getStackInSlot(i);
            units += existing.isEmpty() ? itemHandler.getSlotLimit(i) : Math.max(0, itemHandler.getSlotLimit(i) - existing.getCount());
        }
        return units < B;
    }

    private void installDynamicOutputLimit() {
        if (itemHandler == null) return;
        int B = getLockedBatchSize();
        int cap = (int) Math.min(64L * Math.max(1, B), Integer.MAX_VALUE);
        itemHandler.setDynamicSlotLimit((slot, candidate) -> {
            if (slot < 1 || slot >= inventorySize()) return 64;
            ItemStack existing = itemHandler.getStackInSlot(slot);
            return existing.isEmpty() ? cap : Math.max(cap, existing.getCount());
        });
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
                if (stack.getCount() <= limit) { slots[j] = stack; stack = ItemStack.EMPTY; }
                else { ItemStack fill = stack.copy(); fill.setCount(limit); slots[j] = fill; stack.shrink(limit); }
            } else if (ItemStack.isSameItem(slots[j], stack)) {
                int room = itemHandler.getSlotLimit(outputStart + j) - slots[j].getCount();
                int moved = Math.min(room, stack.getCount());
                if (moved > 0) { slots[j].grow(moved); stack.shrink(moved); }
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