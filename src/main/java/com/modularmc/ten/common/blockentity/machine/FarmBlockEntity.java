package com.modularmc.ten.common.blockentity.machine;

import com.modularmc.ten.api.blockentity.RadiusMachineBlockEntity;
import com.modularmc.ten.api.option.IngredientType;
import com.modularmc.ten.utils.WorkingHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

public class FarmBlockEntity extends RadiusMachineBlockEntity {

    public FarmBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setCapacity(kFE(20));
        setEfficiency(10);
        initialRadius = 4;
        radius = 4;
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
        if (slot <= 5) {
            return stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof CropBlock;
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
    public void applyEffect() {
        if (level == null) return;
        WorkingHelper.runInFlat(radius, worldPosition, pos -> {
            BlockPos below = pos.below();
            BlockState state = level.getBlockState(pos);
            BlockState belowState = level.getBlockState(below);

            if (state.getBlock() instanceof CropBlock crop) {
                int age = state.getValue(CropBlock.AGE);
                if (age >= crop.getMaxAge()) {
                    var lootBuilder = WorkingHelper.getLootBuilder(level, worldPosition, ItemStack.EMPTY);
                    List<ItemStack> drops = state.getDrops(lootBuilder);
                    if (canFitAll(drops)) {
                        fitAll(drops);
                        level.destroyBlock(pos, false);
                        return true;
                    }
                }
            }

            if (belowState.is(Blocks.FARMLAND) && state.isAir()) {
                ItemStack seed = getSeed();
                if (!seed.isEmpty() && seed.getItem() instanceof BlockItem bi) {
                    Block plantBlock = bi.getBlock();
                    if (plantBlock instanceof CropBlock) {
                        level.setBlock(pos, plantBlock.defaultBlockState(), 3);
                        seed.shrink(1);
                        return true;
                    }
                }
            }
            return false;
        });
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
        return 20;
    }
}
