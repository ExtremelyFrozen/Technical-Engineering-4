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
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 0, 43, 16));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 1, 61, 16));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 2, 43, 34));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 3, 61, 34));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 4, 43, 52));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 5, 61, 52));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 6, 97, 16));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 7, 115, 16));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 8, 97, 34));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 9, 115, 34));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 10, 97, 52));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 11, 115, 52));
        }, root -> {
            root.addChild(TENMachineBlockUIFactory.energyGaugeModular(this, 8, 18, true));
            root.addChild(TENMachineBlockUIFactory.progressGaugeWide(this, 48, 74, true));
            root.addChild(TENMachineBlockUIFactory.rangeDisplayToggleButton(this));
        });
    }

    @Override
    public boolean cooking() {
        // 纯容量谓词：输出槽（6..11）无法容纳本周期收割产出时停滞（保留 progress）。
        // 每周期扫描 B 行；单格最多 2 件（1 种子 + 1 产物），阈值 2*B 保证产出放得下。
        if (itemHandler == null) {
            return false;
        }
        int B = getLockedBatchSize();
        int units = 0;
        for (int i = 6; i < itemHandler.getSlots(); i++) {
            ItemStack existing = itemHandler.getStackInSlot(i);
            units += existing.isEmpty() ? itemHandler.getSlotLimit(i) : Math.max(0, itemHandler.getSlotLimit(i) - existing.getCount());
        }
        return units < 2L * B;
    }

    /**
     * 批量升级时按 lockedB 动态扩展输出槽上限（6..11），保留既有超堆叠（26.1.2 对齐）。
     * 在每个 applyEffect 周期开始时调用。
     */
    private void installDynamicOutputLimit() {
        if (itemHandler == null) return;
        int B = getLockedBatchSize();
        int cap = (int) Math.min(64L * Math.max(1, B), Integer.MAX_VALUE);
        itemHandler.setDynamicSlotLimit((slot, candidate) -> {
            if (slot < 6 || slot >= inventorySize()) return 64; // 非输出槽
            ItemStack existing = itemHandler.getStackInSlot(slot);
            return existing.isEmpty() ? cap : Math.max(cap, existing.getCount());
        });
    }

    /**
     * 生效范围：机器背面（2*radius+1)²（宽轴 ±radius、深轴从背面第一格起，
     * Y=自身一层），与 buildXOffsets/scanRow 的动态列数 × 动态深度一致（26.1.2 对齐）。
     */
    @Override
    public List<net.minecraft.world.phys.AABB> getRangeBoxes() {
        int r = Math.max(0, radius);
        Direction facing = getFacing();
        int mx = worldPosition.getX(), my = worldPosition.getY(), mz = worldPosition.getZ();
        int depth = 2 * r + 1;
        int minX, maxX, minZ, maxZ;
        switch (facing) {
            case NORTH -> {
                minX = mx - r;
                maxX = mx + r + 1;
                minZ = mz + 1;
                maxZ = mz + 1 + depth;
            } // 背面=+Z
            case SOUTH -> {
                minX = mx - r;
                maxX = mx + r + 1;
                minZ = mz - depth;
                maxZ = mz;
            }      // 背面=-Z
            case EAST -> {
                minX = mx - depth;
                maxX = mx;
                minZ = mz - r;
                maxZ = mz + r + 1;
            }      // 背面=-X
            default -> {
                minX = mx + 1;
                maxX = mx + 1 + depth;
                minZ = mz - r;
                maxZ = mz + r + 1;
            } // 背面=+X (WEST)
        }
        return List.of(new net.minecraft.world.phys.AABB(minX, my, minZ, maxX, my + 1, maxZ));
    }

    @Override
    public void applyEffect() {
        if (level == null) return;

        // 批量升级时按 B 动态扩展输出槽上限（26.1.2 对齐）
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

        // P3-T1c 对齐：每周期连续扫描 B 行（能耗已按 B 放大，产出必须同步兑现）
        int B = getLockedBatchSize();

        for (int i = 0; i < B && currentRowIndex < xRowOrder.length; i++) {
            int xOffset = xRowOrder[currentRowIndex];
            int maturityCount = scanRow(xOffset);
            xRowMaturity[currentRowIndex] = maturityCount;
            currentRowIndex++;
        }

        // If all rows processed, sort by maturity and restart
        if (currentRowIndex >= xRowOrder.length) {
            sortRowsByMaturity();
            currentRowIndex = 0;
        }
    }

    private int[] buildXOffsets() {
        // 动态方形：宽度轴 2*radius+1 列（-radius..+radius），随范围升级（LevelupRg）扩展（26.1.2 对齐）
        int size = 2 * radius + 1;
        int[] offsets = new int[size];
        for (int i = 0; i < size; i++) {
            offsets[i] = -radius + i;
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

        // 背面方形：深轴从背面第一格起 2*radius+1 格（不含机器所在行，26.1.2 对齐）
        int depth = 2 * radius + 1;
        for (int d = 1; d <= depth; d++) {
            int dx, dz;
            // widthOffset: axis perpendicular to facing
            // d: depth axis (opposite of facing = behind)
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
        var config = ConfigHolder.farm();
        if (config.bushCrops() == null || config.bushCrops().isEmpty()) return false;
        String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        for (String id : config.bushCrops()) {
            if (id.equals(blockId)) return true;
        }
        return false;
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

    private boolean canFitAll(List<ItemStack> drops) {
        // 快照模拟：与 fitAll 提交逻辑完全一致，多件掉落不竞争同一空槽（26.1.2 对齐）
        int outputStart = 6;
        int slotCount = itemHandler.getSlots() - outputStart;
        ItemStack[] simulated = copyOutputSlots(outputStart, slotCount);
        for (ItemStack stack : drops) {
            ItemStack remaining = simulateInsert(simulated, stack.copy(), outputStart);
            if (!remaining.isEmpty()) {
                return false;
            }
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

    /** 模拟向槽位数组插入单组物品，就地更新；返回剩余（空=全部插入）。逻辑与 fitAll 提交阶段一致。 */
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

    private void fitAll(List<ItemStack> drops) {
        // 快照 + 模拟 + 原子提交（26.1.2 对齐）：模拟失败 fail-fast，绝不部分写入
        int outputStart = 6;
        int slotCount = itemHandler.getSlots() - outputStart;
        ItemStack[] snapshot = copyOutputSlots(outputStart, slotCount);

        for (ItemStack stack : drops) {
            ItemStack remaining = simulateInsert(snapshot, stack.copy(), outputStart);
            if (!remaining.isEmpty()) {
                throw new IllegalStateException(
                        "Farm cannot fit all drops: " + stack + " has " + remaining.getCount() + " remaining. " + "canFitAll pre-check should have prevented this.");
            }
        }

        for (int i = 0; i < slotCount; i++) {
            itemHandler.setStackInSlot(outputStart + i, snapshot[i]);
        }
    }

    @Override
    public double effectInterval() {
        return 0.25;
    }
}
