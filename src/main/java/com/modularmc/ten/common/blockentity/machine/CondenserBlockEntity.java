package com.modularmc.ten.common.blockentity.machine;

import com.modularmc.ten.api.blockentity.ProcessingMachineBlockEntity;
import com.modularmc.ten.api.option.IngredientType;
import com.modularmc.ten.api.option.MachineType;
import com.modularmc.ten.common.data.TENFluids;
import com.modularmc.ten.common.gui.TENMachineBlockUIFactory;
import com.modularmc.ten.utils.TagHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;

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
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        return buildMachineUI(holder, TENMachineBlockUIFactory.backgroundFor(machineType()), root -> {
            root.addChild(TENMachineBlockUIFactory.machineSlot(this, 0, 79, 32));
        }, root -> {
            root.addChild(TENMachineBlockUIFactory.energyGauge(this, 9, 18, 14, 46, 0, 0, true));
            root.addChild(TENMachineBlockUIFactory.progressGauge(this, 48, 57, 80, 5, 97, 0, true));
            root.addChild(TENMachineBlockUIFactory.fluidGauge(this, 143, 17, 18, 50, 0));
        });
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
        FluidStack produced = new FluidStack(TENFluids.LIQUID_BIZARRERIE_SOURCE.get(), 5);
        if (tanks.isEmpty() || tanks.get(0).fill(produced, IFluidHandler.FluidAction.SIMULATE) < produced.getAmount()) {
            progress = 0;
            return true;
        }

        ItemStack catalyst = itemHandler.getStackInSlot(0);
        if (valid(0, catalyst) && getAliveTime() % 20 == 0) {
            catalyst.shrink(1);
            progress += 200 * getActualEfficiency();
        }
        return false;
    }

    @Override
    public void onCookFinish() {
        if (!tanks.isEmpty()) {
            tanks.get(0).fill(new FluidStack(TENFluids.LIQUID_BIZARRERIE_SOURCE.get(), 5), IFluidHandler.FluidAction.EXECUTE);
        }
    }
}
