package com.modularmc.ten.common.blockentity.machine;

import com.modularmc.ten.api.blockentity.RadiusMachineBlockEntity;
import com.modularmc.ten.api.option.IngredientType;
import com.modularmc.ten.api.option.MachineType;
import com.modularmc.ten.common.block.machine.HorizontalMachineBlock;
import com.modularmc.ten.common.gui.TENMachineBlockUIFactory;
import com.modularmc.ten.config.ConfigHolder;
import com.modularmc.ten.utils.WorkingHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.neoforged.neoforge.fluids.FluidStack;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;

import java.util.Arrays;
import java.util.List;

public class FarmBlockEntity extends RadiusMachineBlockEntity {

    public FarmBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setCapacity(kFE(20));
        setEfficiency(10);
        initialRadius = 4;
        radius = 4;
    }

    @Persisted
    @DescSynced
    public int currentRowIndex = 0;

    @Persisted
    @DescSynced
    public int[] xRowOrder = new int[0];

    @Persisted
    @DescSynced
    public int[] xRowMaturity = new int[0];

    @Override
    public int machineType() {
        return MachineType.FARM;
    }

    @Override
    public int inventorySize() {
        return 12;
    }

    @Override
    public IngredientType slotType(int slot) {
        return slot <= 5 ? IngredientType.INPUT : IngredientType.OUTPUT;
    }

    @Override
    public boolean valid(int slot, ItemStack stack) {
        return true;
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
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        return buildMachineUI(holder, TENMachineBlockUIFactory.backgroundFor(machineType()), root -> {
            root.addChild(TENMachineBlockUIFactory.machineSlot(this, 0, 43, 16));
            root.addChild(TENMachineBlockUIFactory.machineSlot(this, 1, 61, 16));
            root.addChild(TENMachineBlockUIFactory.machineSlot(this, 2, 43, 34));
            root.addChild(TENMachineBlockUIFactory.machineSlot(this, 3, 61, 34));
            root.addChild(TENMachineBlockUIFactory.machineSlot(this, 4, 43, 52));
            root.addChild(TENMachineBlockUIFactory.machineSlot(this, 5, 61, 52));
            root.addChild(TENMachineBlockUIFactory.machineSlot(this, 6, 97, 16));
            root.addChild(TENMachineBlockUIFactory.machineSlot(this, 7, 115, 16));
            root.addChild(TENMachineBlockUIFactory.machineSlot(this, 8, 97, 34));
            root.addChild(TENMachineBlockUIFactory.machineSlot(this, 9, 115, 34));
            root.addChild(TENMachineBlockUIFactory.machineSlot(this, 10, 97, 52));
            root.addChild(TENMachineBlockUIFactory.machineSlot(this, 11, 115, 52));
        }, root -> {
            root.addChild(TENMachineBlockUIFactory.energyGauge(this, 9, 18, 14, 46, 0, 0, true));
            root.addChild(TENMachineBlockUIFactory.progressGauge(this, 48, 73, 80, 5, 97, 0, true));
        });
    }

    @Override
    public void applyEffect() {
        if (level == null) return;

        // P3-T1c: Install dynamic output slot limit for slots 6-11 based on lockedB
        installDynamicOutputLimit();

        // Build X offsets for current radius
        int[] allOffsets = buildXOffsets();
        if (allOffsets.length == 0) return;

        // Validate or rebuild row order if radius changed
        if (xRowOrder.length != allOffsets.length) {
            xRowOrder = allOffsets;
            xRowMaturity = new int[allOffsets.length];
            currentRowIndex = 0;
        }

        if (currentRowIndex < 0 || currentRowIndex >= xRowOrder.length) {
            currentRowIndex = 0;
        }

        // P3-T1c: Scan B consecutive rows per cycle
        int B = getLockedBatchSize();
        int rowsScanned = 0;

        for (int i = 0; i < B && currentRowIndex < xRowOrder.length; i++) {
            int xOffset = xRowOrder[currentRowIndex];
            int maturityCount = scanRow(xOffset);
            xRowMaturity[currentRowIndex] = maturityCount;
            currentRowIndex++;
            rowsScanned++;
        }

        // If all rows processed, sort by maturity and restart
        if (currentRowIndex >= xRowOrder.length) {
            sortRowsByMaturity();
            currentRowIndex = 0;
        }

        // NOTE: If B > remaining rows (rowsScanned < B), excess batch is forfeited.
        // Energy is still charged at full B (totalFePerTick = round(baseFePerTick * lockedB)).
        // No wrap-around, no carry-over of excess to next cycle.
    }

    private int[] buildXOffsets() {
        // 9x9 square: 9 positions along the width axis (perpendicular to facing)
        int[] offsets = new int[9];
        for (int i = 0; i < 9; i++) {
            offsets[i] = -4 + i;
        }
        return offsets;
    }

    private void sortRowsByMaturity() {
        int n = xRowOrder.length;
        Integer[] indices = new Integer[n];
        for (int i = 0; i < n; i++) indices[i] = i;
        Arrays.sort(indices, (a, b) -> Integer.compare(xRowMaturity[b], xRowMaturity[a]));
        int[] newOrder = new int[n];
        int[] newMaturity = new int[n];
        for (int i = 0; i < n; i++) {
            newOrder[i] = xRowOrder[indices[i]];
            newMaturity[i] = xRowMaturity[indices[i]];
        }
        xRowOrder = newOrder;
        xRowMaturity = newMaturity;
    }

    private int scanRow(int widthOffset) {
        if (level == null) return 0;

        BlockState state = getBlockState();
        Direction facing = state.getValue(HorizontalMachineBlock.FACING);

        int mx = worldPosition.getX();
        int my = worldPosition.getY();
        int mz = worldPosition.getZ();
        int maturity = 0;

        // Scan 9 blocks along the depth axis (back direction)
        for (int d = 0; d < 9; d++) {
            int dx, dz;
            switch (facing) {
                case NORTH -> {
                    dx = widthOffset;
                    dz = d;
                }
                case SOUTH -> {
                    dx = widthOffset;
                    dz = -d;
                }
                case EAST -> {
                    dx = -d;
                    dz = widthOffset;
                }
                case WEST -> {
                    dx = d;
                    dz = widthOffset;
                }
                default -> {
                    dx = widthOffset;
                    dz = d;
                }
            }
            BlockPos pos = new BlockPos(mx + dx, my, mz + dz);
            if (pos.equals(worldPosition)) continue;
            BlockState scanState = level.getBlockState(pos);
            BlockPos below = pos.below();
            BlockState belowState = level.getBlockState(below);
            var ageProp = findAgeProperty(scanState);

            // Tier 1: Standard CropBlock
            if (scanState.getBlock() instanceof CropBlock crop) {
                int age = scanState.getValue(CropBlock.AGE);
                int maxAge = crop.getMaxAge();
                if (age >= maxAge) {
                    var lootBuilder = WorkingHelper.getLootBuilder(level, worldPosition, ItemStack.EMPTY);
                    List<ItemStack> drops = scanState.getDrops(lootBuilder);
                    if (canFitAll(drops)) {
                        fitAll(drops);
                        level.setBlock(pos, scanState.setValue(CropBlock.AGE, 1), 3);
                    }
                } else if (age >= maxAge - 1) {
                    maturity++;
                }
                continue;
            }

            // Tier 2: Bush/regrowable crops
            if (ageProp != null && !(scanState.getBlock() instanceof StemBlock)) {
                int age = scanState.getValue(ageProp);
                int maxAge = ageProp.getPossibleValues().stream().max(Integer::compare).orElse(0);
                if (age >= maxAge) {
                    var lootBuilder = WorkingHelper.getLootBuilder(level, worldPosition, ItemStack.EMPTY);
                    List<ItemStack> drops = scanState.getDrops(lootBuilder);
                    if (canFitAll(drops)) {
                        fitAll(drops);
                        level.setBlock(pos, scanState.setValue(ageProp, Math.max(0, maxAge - 1)), 3);
                    }
                } else if (age >= maxAge - 1) {
                    maturity++;
                }
                continue;
            }

            // Tier 3: Config list override
            if (ageProp == null && isBushCrop(scanState)) {
                var lootBuilder = WorkingHelper.getLootBuilder(level, worldPosition, ItemStack.EMPTY);
                List<ItemStack> drops = scanState.getDrops(lootBuilder);
                if (canFitAll(drops)) {
                    fitAll(drops);
                    level.destroyBlock(pos, false);
                }
                continue;
            }

            // Auto-plant on empty farmland from seed slots
            if (belowState.is(Blocks.FARMLAND) && scanState.isAir()) {
                ItemStack seed = getSeed();
                if (!seed.isEmpty() && seed.getItem() instanceof BlockItem bi) {
                    Block plantBlock = bi.getBlock();
                    if (plantBlock instanceof CropBlock) {
                        level.setBlock(pos, plantBlock.defaultBlockState(), 3);
                        seed.shrink(1);
                    }
                }
            }
        }
        return maturity;
    }

    private static boolean isBushCrop(BlockState state) {
        var bushCrops = ConfigHolder.bushCrops();
        if (bushCrops.isEmpty()) return false;
        String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        return bushCrops.contains(blockId);
    }

    private static IntegerProperty findAgeProperty(BlockState state) {
        var prop = state.getBlock().getStateDefinition().getProperty("age");
        return prop instanceof IntegerProperty ip ? ip : null;
    }

    private ItemStack getSeed() {
        for (int i = 0; i <= 5; i++) {
            ItemStack s = itemHandler.getStackInSlot(i);
            if (!s.isEmpty()) return s;
        }
        return ItemStack.EMPTY;
    }

    private boolean canFitAll(List<ItemStack> stacks) {
        // Simulate accumulation into a local ItemStack[] copy of output slots.
        // This matches the exact same logic as fitAll's commit, ensuring the
        // pre-check is accurate and multiple different drops don't compete for
        // the same empty slot.
        int outputStart = 6;
        int slotCount = itemHandler.getSlots() - outputStart;
        ItemStack[] simulated = copyOutputSlots(outputStart, slotCount);
        for (ItemStack stack : stacks) {
            ItemStack remaining = simulateInsert(simulated, stack.copy(), outputStart);
            if (!remaining.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Snapshot output slots into a new ItemStack[] array (deep copy).
     */
    private ItemStack[] copyOutputSlots(int start, int count) {
        ItemStack[] copy = new ItemStack[count];
        for (int i = 0; i < count; i++) {
            ItemStack s = itemHandler.getStackInSlot(start + i);
            copy[i] = s.isEmpty() ? ItemStack.EMPTY : s.copy();
        }
        return copy;
    }

    /**
     * Simulate inserting a single stack into the slot array, updating it in-place.
     * Returns the remaining stack (empty if fully inserted).
     * Logic matches the commit-phase insert exactly: respects slot limits,
     * handles same-item merge, and spills across slots.
     */
    private ItemStack simulateInsert(ItemStack[] slots, ItemStack stack, int outputStart) {
        for (int j = 0; j < slots.length && !stack.isEmpty(); j++) {
            if (slots[j].isEmpty()) {
                int slotLimit = itemHandler.getSlotLimit(outputStart + j);
                if (stack.getCount() <= slotLimit) {
                    slots[j] = stack;
                    stack = ItemStack.EMPTY;
                } else {
                    ItemStack fill = stack.copy();
                    fill.setCount(slotLimit);
                    slots[j] = fill;
                    stack.shrink(slotLimit);
                }
            } else if (ItemStack.isSameItem(slots[j], stack)) {
                int slotLimit = itemHandler.getSlotLimit(outputStart + j);
                int room = slotLimit - slots[j].getCount();
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
        // 1. Snapshot output slots
        int outputStart = 6;
        int slotCount = itemHandler.getSlots() - outputStart;
        ItemStack[] snapshot = copyOutputSlots(outputStart, slotCount);

        // 2. Simulate insertion on snapshot
        for (ItemStack stack : stacks) {
            ItemStack remaining = simulateInsert(snapshot, stack.copy(), outputStart);
            if (!remaining.isEmpty()) {
                // 3. Fail-fast: partial state must never reach the handler
                throw new IllegalStateException(
                        "Farm cannot fit all drops: " + stack + " has " + remaining.getCount() + " remaining. " + "canFitAll pre-check should have prevented this.");
            }
        }

        // 4. Commit: write snapshot atomically to the real handler
        for (int i = 0; i < slotCount; i++) {
            itemHandler.setStackInSlot(outputStart + i, snapshot[i]);
        }
    }

    private void insertFirstFit(ItemStack stack) {
        // NOTE: This method is kept for legacy callers but should not be
        // reached from canFitAll+fitAll flow (which uses snapshot+commit).
        // Fail-fast if stack doesn't fully fit.
        for (int i = 6; i < itemHandler.getSlots(); i++) {
            ItemStack existing = itemHandler.getStackInSlot(i);
            if (existing.isEmpty()) {
                int slotLimit = itemHandler.getSlotLimit(i);
                if (stack.getCount() <= slotLimit) {
                    itemHandler.setStackInSlot(i, stack);
                    return;
                } else {
                    ItemStack fill = stack.copy();
                    fill.setCount(slotLimit);
                    stack.shrink(slotLimit);
                    itemHandler.setStackInSlot(i, fill);
                    continue;
                }
            }
            if (ItemStack.isSameItem(existing, stack)) {
                int slotLimit = itemHandler.getSlotLimit(i);
                int room = slotLimit - existing.getCount();
                int moved = Math.min(room, stack.getCount());
                if (moved > 0) {
                    existing.grow(moved);
                    stack.shrink(moved);
                    if (stack.isEmpty()) {
                        return;
                    }
                }
            }
        }
        // Fail-fast: prevent silent item loss
        if (!stack.isEmpty()) {
            throw new IllegalStateException(
                    "Farm insertFirstFit cannot fit " + stack + ": no available slot. " + "canFitAll pre-check should have prevented this.");
        }
    }

    /**
     * Install dynamic slot limit on output slots (6..11) based on lockedB.
     * Limit = min(lockedB + 63, 99) per slot, preserving existing overstack.
     * Called at the start of each applyEffect cycle.
     */
    private void installDynamicOutputLimit() {
        if (itemHandler == null) return;
        int B = getLockedBatchSize();
        itemHandler.setDynamicSlotLimit((slot, candidate) -> {
            if (slot < 6 || slot >= inventorySize()) return 64; // Not output
            int limit = Math.min(B + 63, 99);
            // Never go below existing count
            ItemStack existing = itemHandler.getStackInSlot(slot);
            if (!existing.isEmpty()) {
                limit = Math.max(limit, existing.getCount());
            }
            return limit;
        });
    }

    @Override
    public double effectInterval() {
        return 0.25;
    }
}
