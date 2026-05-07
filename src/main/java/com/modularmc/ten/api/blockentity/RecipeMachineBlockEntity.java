package com.modularmc.ten.api.blockentity;

import com.modularmc.ten.api.recipe.FormsCombinedIngredient;
import com.modularmc.ten.api.recipe.FormsCombinedRecipe;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public abstract class RecipeMachineBlockEntity extends ProcessingMachineBlockEntity {

    public SlotInfo slotInfo;
    protected FormsCombinedRecipe currentRecipe;

    public RecipeMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, SlotInfo info) {
        super(type, pos, state);
        this.slotInfo = info;
    }

    public abstract FormsCombinedRecipe findRecipe();

    @Override
    public int baseTickTime() {
        return currentRecipe != null ? currentRecipe.time() : 200;
    }

    @Override
    public boolean conditionStart() {
        currentRecipe = findRecipe();
        return currentRecipe != null;
    }

    @Override
    public boolean cooking() {
        if (currentRecipe == null) return false;

        var outputItems = currentRecipe.allOutputItems();
        var outputFluids = currentRecipe.allOutputFluids();

        for (var ing : outputItems) {
            if (!canFitOutput(ing)) return true;
        }
        for (var ing : outputFluids) {
            if (!canFitFluidOutput(ing)) return true;
        }
        return false;
    }

    @Override
    public void onCookFinish() {
        if (currentRecipe == null) return;

        var items = currentRecipe.generateItems();
        var fluids = currentRecipe.generateFluids();

        for (ItemStack s : items) {
            giveOutput(s, slotInfo.o1(), slotInfo.o2());
        }
        for (FluidStack s : fluids) {
            giveFluidOutput(s);
        }
        shrinkInputs();
    }

    protected boolean canFitOutput(FormsCombinedIngredient ing) {
        ItemStack stack = ing.symbolItem();
        if (stack.isEmpty()) return true;
        for (int i = slotInfo.o1(); i <= slotInfo.o2(); i++) {
            ItemStack existing = itemHandler.getStackInSlot(i);
            if (existing.isEmpty()) return true;
            if (ItemStack.isSameItem(existing, stack) && existing.getCount() + stack.getCount() <= existing.getMaxStackSize())
                return true;
        }
        return false;
    }

    protected boolean canFitFluidOutput(FormsCombinedIngredient ing) {
        FluidStack stack = ing.symbolFluid();
        if (stack.isEmpty()) return true;
        for (var tank : tanks) {
            if (tank.fill(stack, IFluidHandler.FluidAction.SIMULATE) >= stack.getAmount()) return true;
        }
        return false;
    }

    protected void giveOutput(ItemStack stack, int start, int end) {
        for (int i = start; i <= end && !stack.isEmpty(); i++) {
            ItemStack existing = itemHandler.getStackInSlot(i);
            if (existing.isEmpty()) {
                itemHandler.setStackInSlot(i, stack.copy());
                stack.setCount(0);
            } else if (ItemStack.isSameItem(existing, stack)) {
                int space = existing.getMaxStackSize() - existing.getCount();
                int toAdd = Math.min(space, stack.getCount());
                existing.grow(toAdd);
                stack.shrink(toAdd);
            }
        }
    }

    protected void giveFluidOutput(FluidStack stack) {
        for (var tank : tanks) {
            int filled = tank.fill(stack, IFluidHandler.FluidAction.EXECUTE);
            stack.shrink(filled);
            if (stack.isEmpty()) break;
        }
    }

    protected void shrinkInputs() {
        if (currentRecipe == null) return;
        for (var ing : currentRecipe.allInputItems()) {
            int needed = ing.amountOrCount();
            for (int i = slotInfo.i1(); i <= slotInfo.i2() && needed > 0; i++) {
                ItemStack slot = itemHandler.getStackInSlot(i);
                if (ing.contains(slot.getItem())) {
                    int toRemove = Math.min(needed, slot.getCount());
                    slot.shrink(toRemove);
                    needed -= toRemove;
                }
            }
        }
        for (var ing : currentRecipe.allInputFluids()) {
            int needed = ing.amountOrCount();
            for (var tank : tanks) {
                FluidStack fluid = tank.getFluid();
                if (ing.contains(fluid.getFluid())) {
                    int toRemove = Math.min(needed, fluid.getAmount());
                    fluid.shrink(toRemove);
                    needed -= toRemove;
                    if (needed <= 0) break;
                }
            }
        }
    }
}
