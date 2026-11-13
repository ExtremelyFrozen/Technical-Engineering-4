package com.modularmc.ten.common.blockentity.machine;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.modularmc.ten.api.blockentity.EngineBlockEntity;
import com.modularmc.ten.api.option.IngredientType;
import com.modularmc.ten.api.option.MachineType;
import com.modularmc.ten.common.gui.TENMachineBlockUIFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

public class RecyclerBlockEntity extends EngineBlockEntity {

    public RecyclerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setCapacity(kFE(80));
        setEfficiency(5);
        tanks.add(new com.modularmc.ten.api.capability.MachineFluidTank(8000));
    }

    @Override
    public int machineType() {
        return MachineType.ENGINE_RECYCLER;
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
        return true;
    }

    @Override
    public IngredientType tankType(int tank) {
        return IngredientType.INPUT;
    }

    @Override
    public boolean valid(int slot, FluidStack stack) {
        return true;
    }
//
//    @Override
//    public boolean hasFaceCapabilityFluid(Direction side) {
//        return true;
//    }

    @Override
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        return buildMachineUI(holder, TENMachineBlockUIFactory.backgroundFor(machineType()), root -> {
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 0, 43, 36));
            root.addChild(TENMachineBlockUIFactory.fluidGaugeModular(this, 61, 21, 18, 50, 0));
        }, root -> {
            root.addChild(TENMachineBlockUIFactory.energyGaugeModular(this, 117, 22, true));
            root.addChild(TENMachineBlockUIFactory.fuelGaugeModular(this, 81, 39, false));
        });
    }

    @Override
    public void process() {
        if (!signalAllowRun() || !energyAllowRun()) return;

        ItemStack fuelStack = ItemStack.EMPTY;
        if (itemHandler != null && itemHandler.getSlots() > 0) {
            fuelStack = itemHandler.getStackInSlot(0);
        }

        int fuelVal = matchFuel(fuelStack, false);
        //持续消耗，FE只是副产物
        if (fuel > 0) {
            energyStorage.receiveEnergy(getActualEfficiency(), false);
            fuel = Math.max(fuel - efficientIn, 0);
            setActive(true);
        }
        else {
            if (fuelVal > 0) {
                fuel = fuelVal;
                maxFuel = fuelVal;
                setActive(true);
            } else {
                setActive(false);
            }
        }
    }

    //持续消耗物品和流体
    @Override
    public int matchFuel(ItemStack stack, boolean simulate) {
        int t = 0;
        if (!stack.isEmpty()) {
            t += 100;
            stack.setCount(0);
        }
        if (tanks.getFirst().getFluidAmount() > 0) {
            t += 100;
            tanks.getFirst().setFluid(FluidStack.EMPTY);
        }
        return t;
    }
}
