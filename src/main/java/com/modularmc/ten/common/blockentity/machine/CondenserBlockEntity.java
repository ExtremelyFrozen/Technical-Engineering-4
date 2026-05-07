package com.modularmc.ten.common.blockentity.machine;

import com.modularmc.ten.api.blockentity.ProcessingMachineBlockEntity;
import com.modularmc.ten.api.option.IngredientType;
import com.modularmc.ten.api.option.MachineType;
import com.modularmc.ten.common.data.TENFluids;
import com.modularmc.ten.utils.TagHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public class CondenserBlockEntity extends ProcessingMachineBlockEntity {

    public CondenserBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setCapacity(kFE(20));
        setEfficiency(30);
        tanks.add(new com.modularmc.ten.api.capability.MachineFluidTank(1000));
    }

    @Override
    public int machineType() {
        return MachineType.MATTER_CONDENSER;
    }

    @Override
    public int inventorySize() {
        return 1;
    }

    @Override
    public IngredientType slotType(int slot) {
        return IngredientType.INPUT;
    }

    @Override
    public boolean valid(int slot, ItemStack stack) {
        return TagHelper.containsItem(stack.getItem(), TagHelper.keyItem("kenergyengineering:catalyst"));
    }

    @Override
    public IngredientType tankType(int tank) {
        return IngredientType.OUTPUT;
    }

    @Override
    public boolean valid(int slot, FluidStack stack) {
        return true;
    }

    @Override
    public int baseTickTime() {
        return 1000;
    }

    @Override
    public boolean conditionStart() {
        return !itemHandler.getStackInSlot(0).isEmpty();
    }

    @Override
    public boolean cooking() {
        FluidStack produced = new FluidStack((net.minecraft.world.level.material.Fluid) TENFluids.LIQUID_BIZARRERIE.getSource(), 5);
        if (tanks.isEmpty() || tanks.get(0).fill(produced, IFluidHandler.FluidAction.SIMULATE) < produced.getAmount()) {
            data.set(PROGRESS, 0);
            return true;
        }

        ItemStack catalyst = itemHandler.getStackInSlot(0);
        if (valid(0, catalyst) && getAliveTime() % 20 == 0) {
            catalyst.shrink(1);
            data.translate(PROGRESS, 200 * getActualEfficiency());
        }
        return false;
    }

    @Override
    public void onCookFinish() {
        if (!tanks.isEmpty()) {
            tanks.get(0).fill(new FluidStack((net.minecraft.world.level.material.Fluid) TENFluids.LIQUID_BIZARRERIE.getSource(), 5), IFluidHandler.FluidAction.EXECUTE);
        }
    }
}
