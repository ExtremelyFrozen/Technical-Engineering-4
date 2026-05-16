package com.modularmc.ten.common.blockentity.machine;

import com.modularmc.ten.api.blockentity.RadiusMachineBlockEntity;
import com.modularmc.ten.api.option.IngredientType;
import com.modularmc.ten.api.option.MachineType;
import com.modularmc.ten.common.item.upgrades.LevelupIce;
import com.modularmc.ten.common.item.upgrades.LevelupMagma;
import com.modularmc.ten.common.item.upgrades.LevelupMineral;
import com.modularmc.ten.utils.ItemNBTHelper;
import com.modularmc.ten.utils.TagHelper;
import com.modularmc.ten.utils.WorkingHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

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
            return stack.getItem() instanceof TieredItem;
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
        switch (mode) {
            case 0, 3 -> mineRandomBlock();
            case 1 -> giveGeneratedLoot(
                    Math.random() < 0.75 ? Items.ICE.getDefaultInstance() : Math.random() < 0.75 ? Items.PACKED_ICE.getDefaultInstance() : Items.BLUE_ICE.getDefaultInstance());
            case 2 -> giveGeneratedLoot(
                    Math.random() < 0.75 ? Items.MAGMA_BLOCK.getDefaultInstance() : Items.MAGMA_CREAM.getDefaultInstance());
            default -> {}
        }
    }

    private void mineRandomBlock() {
        if (level == null || itemHandler == null) {
            return;
        }
        int dx = Mth.nextInt(level.getRandom(), -radius + 1, radius - 1);
        int dz = Mth.nextInt(level.getRandom(), -radius + 1, radius - 1);
        BlockPos target = worldPosition.offset(dx, 0, dz);
        target = target.atY(Mth.randomBetweenInclusive(level.getRandom(), level.getMinBuildHeight(), worldPosition.getY() - 1));
        BlockState state = level.getBlockState(target);
        if (!canBreak(state)) {
            return;
        }
        List<ItemStack> drops = state.getDrops(WorkingHelper.getLootBuilder(level, target, itemHandler.getStackInSlot(0)));
        if (!canFitAll(drops)) {
            return;
        }
        fitAll(drops);
        level.destroyBlock(target, false);
        ItemNBTHelper.damage(itemHandler.getStackInSlot(0), level, 1);
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

    private void giveGeneratedLoot(ItemStack stack) {
        if (canFit(stack)) {
            insertFirstFit(stack.copy());
        }
    }

    private boolean canFit(ItemStack stack) {
        for (int i = 1; i < itemHandler.getSlots(); i++) {
            ItemStack existing = itemHandler.getStackInSlot(i);
            if (existing.isEmpty()) {
                return true;
            }
            if (ItemStack.isSameItem(existing, stack) && existing.getCount() + stack.getCount() <= existing.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    private boolean canFitAll(List<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (!canFit(stack)) {
                return false;
            }
        }
        return true;
    }

    private void fitAll(List<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            insertFirstFit(stack.copy());
        }
    }

    private void insertFirstFit(ItemStack stack) {
        for (int i = 1; i < itemHandler.getSlots(); i++) {
            ItemStack existing = itemHandler.getStackInSlot(i);
            if (existing.isEmpty()) {
                itemHandler.setStackInSlot(i, stack);
                return;
            }
            if (ItemStack.isSameItem(existing, stack)) {
                int room = existing.getMaxStackSize() - existing.getCount();
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
