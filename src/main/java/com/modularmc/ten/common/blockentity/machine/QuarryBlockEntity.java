package com.modularmc.ten.common.blockentity.machine;

import com.modularmc.ten.api.blockentity.RadiusMachineBlockEntity;
import com.modularmc.ten.api.option.IngredientType;
import com.modularmc.ten.api.option.MachineType;
import com.modularmc.ten.common.gui.TENMachineBlockUIFactory;
import com.modularmc.ten.common.item.upgrades.LevelupIce;
import com.modularmc.ten.common.item.upgrades.LevelupMagma;
import com.modularmc.ten.common.item.upgrades.LevelupMineral;
import com.modularmc.ten.utils.ItemNBTHelper;
import com.modularmc.ten.utils.TagHelper;
import com.modularmc.ten.utils.WorkingHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;

import java.util.List;

public class QuarryBlockEntity extends RadiusMachineBlockEntity {

    private int mode;

    public QuarryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setCapacity(kFE(20));
        setEfficiency(10);
        initialRadius = 3;
        radius = 3;
    }

    @Override
    public int machineType() {
        return MachineType.QUARRY;
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
        if (slot == 0) {
            return stack.has(DataComponents.TOOL);
        }
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
        });
    }

    @Override
    public void tick() {
        super.tick();
        if (getAliveTime() % 10 == 0) {
            updateMode();
        }
    }

    private void updateMode() {
        if (upgradeHandler == null) {
            mode = 0;
            return;
        }
        if (hasUpgrade(LevelupIce.class)) {
            mode = 1;
            return;
        }
        if (hasUpgrade(LevelupMagma.class)) {
            mode = 2;
            return;
        }
        if (hasUpgrade(LevelupMineral.class)) {
            mode = 3;
            return;
        }
        mode = 0;
    }

    @Override
    public void applyEffect() {
        if (level == null || itemHandler == null) {
            return;
        }
        // P3-T1c: Install dynamic output slot limit based on locked batch size
        installDynamicOutputLimit();
        // conditionStart is already checked in outer process() — no need to repeat.
        // Only truly fatal conditions (tool broken) break the loop.
        int B = getLockedBatchSize();
        for (int i = 0; i < B; i++) {
            if (itemHandler.getStackInSlot(0).isEmpty()) break; // Tool broken/empty → fatal → break
            // Per-operation miss (no drops, can't break, capacity full) → continue
            executeSingleOperation();
        }
    }

    @Override
    public boolean cooking() {
        // Pure capacity predicate (P1 contract): block processing when output slots
        // (1..12) cannot accommodate this cycle's B output units. Each operation
        // produces at most 1 output unit (mode 1/2: single item; mode 0/3: one
        // block's drops). No progress read/write, no side effects — progress is
        // preserved while output is full (stall semantics, Deviation #2 fix).
        if (itemHandler == null) {
            return false;
        }
        int B = getLockedBatchSize();
        int units = 0;
        for (int i = 1; i < itemHandler.getSlots(); i++) {
            ItemStack existing = itemHandler.getStackInSlot(i);
            units += existing.isEmpty() ? itemHandler.getSlotLimit(i) : Math.max(0, itemHandler.getSlotLimit(i) - existing.getCount());
        }
        return units < B;
    }

    /**
     * Install dynamic slot limit on output slots (1..12) based on lockedB.
     * Limit = min(lockedB + 63, 99) per slot, preserving existing overstack.
     */
    private void installDynamicOutputLimit() {
        if (itemHandler == null) return;
        int B = getLockedBatchSize();
        itemHandler.setDynamicSlotLimit((slot, candidate) -> {
            if (slot < 1 || slot >= inventorySize()) return 64; // Not output
            int limit = Math.min(B + 63, 99);
            // Never go below existing count
            ItemStack existing = itemHandler.getStackInSlot(slot);
            if (!existing.isEmpty()) {
                limit = Math.max(limit, existing.getCount());
            }
            return limit;
        });
    }

    /**
     * Execute one unit of the current mode's operation.
     *
     * @return true if operation was executed, false if cannot continue
     */
    private boolean executeSingleOperation() {
        return switch (mode) {
            case 0, 3 -> mineRandomBlock();
            case 1 -> giveGeneratedLoot(
                    Math.random() < 0.75 ? Items.ICE.getDefaultInstance() : Math.random() < 0.75 ? Items.PACKED_ICE.getDefaultInstance() : Items.BLUE_ICE.getDefaultInstance());
            case 2 -> giveGeneratedLoot(
                    Math.random() < 0.75 ? Items.MAGMA_BLOCK.getDefaultInstance() : Items.MAGMA_CREAM.getDefaultInstance());
            default -> false;
        };
    }

    private boolean mineRandomBlock() {
        if (level == null || itemHandler == null) {
            return false;
        }
        // Deviation #4 fix: horizontal bounds are -radius..+radius (span 2*radius+1),
        // matching Beacon/MobRip (AABB.inflate(radius)) and Farm (offsets -radius..+radius).
        // radius=3 → 7×7 (was ±(radius-1) → 5×5, one smaller than nominal).
        int dx = Mth.nextInt(level.getRandom(), -radius, radius);
        int dz = Mth.nextInt(level.getRandom(), -radius, radius);
        BlockPos target = worldPosition.offset(dx, 0, dz);
        // Vertical semantics unchanged: full column below the machine, minY..(y-1).
        target = target.atY(Mth.randomBetweenInclusive(level.getRandom(), level.getMinY(), worldPosition.getY() - 1));
        BlockState state = level.getBlockState(target);
        if (!canBreak(state)) {
            return false;
        }
        List<ItemStack> drops = state.getDrops(WorkingHelper.getLootBuilder(level, target, itemHandler.getStackInSlot(0)));
        // P3-T1c: Empty drops → return false, do NOT destroy block or waste durability
        if (drops.isEmpty()) {
            return false;
        }
        if (!canFitAll(drops)) {
            return false;
        }
        fitAll(drops);
        level.destroyBlock(target, false);
        ItemNBTHelper.damage(itemHandler.getStackInSlot(0), level, 1);
        return true;
    }

    private boolean canBreak(BlockState state) {
        ItemStack tool = itemHandler.getStackInSlot(0);
        if (tool.isEmpty()) {
            return false;
        }
        if (mode == 0) {
            return TagHelper.containsBlock(state.getBlock(), TagHelper.keyBlock("kenergyengineering:quarry_valids")) && tool.isCorrectToolForDrops(state);
        }
        if (mode == 3) {
            return TagHelper.containsBlock(state.getBlock(), TagHelper.keyBlock("c:ores")) && tool.isCorrectToolForDrops(state);
        }
        return false;
    }

    private boolean giveGeneratedLoot(ItemStack stack) {
        if (canFit(stack)) {
            insertFirstFit(stack.copy());
            return true;
        }
        return false;
    }

    private boolean canFit(ItemStack stack) {
        for (int i = 1; i < itemHandler.getSlots(); i++) {
            ItemStack existing = itemHandler.getStackInSlot(i);
            if (existing.isEmpty()) {
                return true;
            }
            // Use dynamic slot limit instead of hard-coded maxStackSize
            int slotLimit = itemHandler.getSlotLimit(i);
            if (ItemStack.isSameItem(existing, stack) && existing.getCount() + stack.getCount() <= slotLimit) {
                return true;
            }
        }
        return false;
    }

    /**
     * P3-T1c: Check if all drops can fit in output slots using simulated
     * accumulation into a local snapshot copy. This matches the exact same
     * logic as fitAll's commit, ensuring multiple different drops don't
     * compete for the same empty slot (unlike the per-item canFit approach).
     */
    private boolean canFitAll(List<ItemStack> stacks) {
        int outputStart = 1;
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

    /**
     * P3-T1c: Fit all drops using snapshot+simulate+commit pattern.
     * <ol>
     * <li>Snapshot output slots into local array</li>
     * <li>Simulate all drops on snapshot</li>
     * <li>Fail-fast if any remaining (pre-condition violated)</li>
     * <li>Commit snapshot atomically to real handler</li>
     * </ol>
     */
    private void fitAll(List<ItemStack> stacks) {
        // 1. Snapshot output slots
        int outputStart = 1;
        int slotCount = itemHandler.getSlots() - outputStart;
        ItemStack[] snapshot = copyOutputSlots(outputStart, slotCount);

        // 2. Simulate insertion on snapshot
        for (ItemStack stack : stacks) {
            ItemStack remaining = simulateInsert(snapshot, stack.copy(), outputStart);
            if (!remaining.isEmpty()) {
                // 3. Fail-fast: partial state must never reach the handler
                throw new IllegalStateException(
                        "Quarry cannot fit all drops: " + stack + " has " + remaining.getCount() + " remaining. " + "canFitAll pre-check should have prevented this.");
            }
        }

        // 4. Commit: write snapshot atomically to the real handler
        for (int i = 0; i < slotCount; i++) {
            itemHandler.setStackInSlot(outputStart + i, snapshot[i]);
        }
    }

    /**
     * Insert a single stack into the first available output slot.
     * Used by {@link #giveGeneratedLoot} for single-item generated loot.
     * <p>
     * NOTE: This method should not be reached from the canFitAll+fitAll flow
     * (which uses snapshot+commit). Fail-fast if stack doesn't fully fit.
     */
    private void insertFirstFit(ItemStack stack) {
        for (int i = 1; i < itemHandler.getSlots(); i++) {
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
                    "Quarry insertFirstFit cannot fit " + stack + ": no available slot. " + "canFitAll pre-check should have prevented this.");
        }
    }

    @Override
    public double effectInterval() {
        return switch (mode) {
            case 0 -> 3;
            case 1 -> 15;
            case 2 -> 30;
            case 3 -> 0.4;
            default -> Integer.MAX_VALUE;
        };
    }
}
