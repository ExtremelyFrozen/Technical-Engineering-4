package com.modularmc.ten.common.blockentity.machine;

import com.modularmc.ten.api.blockentity.RadiusMachineBlockEntity;
import com.modularmc.ten.api.option.IngredientType;
import com.modularmc.ten.api.option.MachineType;
import com.modularmc.ten.common.gui.TENMachineBlockUIFactory;
import com.modularmc.ten.utils.WorkingHelper;
import com.modularmc.ten.config.ConfigHolder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.neoforged.neoforge.fluids.FluidStack;

import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;

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

        // Process current X-row
        int xOffset = xRowOrder[currentRowIndex];
        int maturityCount = scanRow(xOffset);
        xRowMaturity[currentRowIndex] = maturityCount;

        currentRowIndex++;

        // If all rows processed, sort by maturity and restart
        if (currentRowIndex >= xRowOrder.length) {
            sortRowsByMaturity();
            currentRowIndex = 0;
        }
    }

    private int[] buildXOffsets() {
        int rr = radius % 2 == 0 ? radius - 1 : radius;
        int count = radius + rr;
        int[] offsets = new int[count];
        for (int i = 0; i < count; i++) {
            offsets[i] = -rr + i;
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

    private int scanRow(int xOffset) {
        int cx = worldPosition.getX() + xOffset;
        int y = worldPosition.getY();
        int cz = worldPosition.getZ();
        int rr = radius % 2 == 0 ? radius - 1 : radius;
        int maturity = 0;

        for (int k = -rr; k < radius; k++) {
            BlockPos pos = new BlockPos(cx, y, cz + k);
            if (!worldPosition.closerThan(pos, radius)) continue;
            BlockState state = level.getBlockState(pos);
            BlockPos below = pos.below();
            BlockState belowState = level.getBlockState(below);
            var ageProp = findAgeProperty(state);

            // Tier 1: Standard CropBlock
            if (state.getBlock() instanceof CropBlock crop) {
                int age = state.getValue(CropBlock.AGE);
                int maxAge = crop.getMaxAge();
                if (age >= maxAge) {
                    var lootBuilder = WorkingHelper.getLootBuilder(level, worldPosition, ItemStack.EMPTY);
                    List<ItemStack> drops = state.getDrops(lootBuilder);
                    if (canFitAll(drops)) {
                        fitAll(drops);
                        level.setBlock(pos, state.setValue(CropBlock.AGE, 1), 3);
                    }
                } else if (age >= maxAge - 1) {
                    maturity++;
                }
                continue;
            }

            // Tier 2: Bush/regrowable crops
            if (ageProp != null && !(state.getBlock() instanceof StemBlock)) {
                int age = state.getValue(ageProp);
                int maxAge = ageProp.getPossibleValues().stream().max(Integer::compare).orElse(0);
                if (age >= maxAge) {
                    var lootBuilder = WorkingHelper.getLootBuilder(level, worldPosition, ItemStack.EMPTY);
                    List<ItemStack> drops = state.getDrops(lootBuilder);
                    if (canFitAll(drops)) {
                        fitAll(drops);
                        level.setBlock(pos, state.setValue(ageProp, Math.max(0, maxAge - 1)), 3);
                    }
                } else if (age >= maxAge - 1) {
                    maturity++;
                }
                continue;
            }

            // Tier 3: Config list override
            if (ageProp == null && isBushCrop(state)) {
                var lootBuilder = WorkingHelper.getLootBuilder(level, worldPosition, ItemStack.EMPTY);
                List<ItemStack> drops = state.getDrops(lootBuilder);
                if (canFitAll(drops)) {
                    fitAll(drops);
                    level.destroyBlock(pos, false);
                }
                continue;
            }

            // Auto-plant on empty farmland from seed slots
            if (belowState.is(Blocks.FARMLAND) && state.isAir()) {
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
        var config = ConfigHolder.INSTANCE.farm;
        if (config.bushCrops == null || config.bushCrops.length == 0) return false;
        String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        for (String id : config.bushCrops) {
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
        for (ItemStack drop : drops) {
            boolean fit = false;
            for (int i = 6; i < itemHandler.getSlots(); i++) {
                ItemStack existing = itemHandler.getStackInSlot(i);
                if (existing.isEmpty()) {
                    fit = true;
                    break;
                }
                if (ItemStack.isSameItem(existing, drop) && existing.getCount() + drop.getCount() <= existing.getMaxStackSize()) {
                    fit = true;
                    break;
                }
            }
            if (!fit) return false;
        }
        return true;
    }

    private void fitAll(List<ItemStack> drops) {
        for (ItemStack drop : drops) {
            for (int i = 6; i < itemHandler.getSlots(); i++) {
                ItemStack existing = itemHandler.getStackInSlot(i);
                if (existing.isEmpty()) {
                    itemHandler.setStackInSlot(i, drop.copy());
                    break;
                } else if (ItemStack.isSameItem(existing, drop)) {
                    existing.grow(drop.getCount());
                    break;
                }
            }
        }
    }

    @Override
    public double effectInterval() {
        return 0.25;
    }
}
