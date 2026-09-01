package com.modularmc.ten.common.blockentity.machine;

import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;
import com.modularmc.ten.api.option.IngredientType;
import com.modularmc.ten.api.option.MachineType;
import com.modularmc.ten.common.gui.TENMachineBlockUIFactory;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;

public class CreativeCellBlockEntity extends CmMachineBlockEntity {

    public CreativeCellBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setCapacity(Integer.MAX_VALUE);
    }

    @Override
    public int machineType() {
        return MachineType.CREATIVE_CELL;
    }

    @Override
    public int inventorySize() {
        return 2;
    }

    @Override
    public IngredientType slotType(int slot) {
        return IngredientType.IGNORE;
    }

    @Override
    public boolean valid(int slot, ItemStack stack) {
        var energy = stack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM);
        if (energy == null) return false;
        return energy.canReceive() && energy.getEnergyStored() < energy.getMaxEnergyStored();
    }

    @Override
    public IngredientType tankType(int tank) {
        return IngredientType.IGNORE;
    }

    @Override
    public boolean valid(int slot, FluidStack stack) {
        return false;
    }

    @Override
    public boolean hasUpgrade() {
        return false;
    }

    @Override
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        return buildMachineUI(holder, TENMachineBlockUIFactory.backgroundFor(machineType()), root -> {
            root.addChild(TENMachineBlockUIFactory.machineSlot(this, 0, 42, 32));
            root.addChild(TENMachineBlockUIFactory.machineSlot(this, 1, 115, 32));
        }, root -> {
            root.addChild(TENMachineBlockUIFactory.energyGauge(this, 81, 18, 14, 46, 0, 0, true));
        });
    }

    @Override
    public void tick() {
        doBaseData();
        if (energyStorage == null) return;
        // Always full — extractEnergy can drain but this refills every tick
        energyStorage.setEnergy(maxStorageEnergy);
        setActive(true);

        if (!signalAllowRun() || itemHandler == null || itemHandler.getSlots() < 2) return;

        for (int slot = 0; slot < 2; slot++) {
            ItemStack stack = itemHandler.getStackInSlot(slot);
            if (stack.getCount() != 1) continue;
            var energy = stack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM);
            if (energy == null || !energy.canReceive()) continue;
            int accepted = energy.receiveEnergy(Integer.MAX_VALUE, false);
            if (accepted > 0) {
                energyStorage.extractEnergy(accepted, false);
            }
        }

        // 主动能量输出：向相邻能量接收方推送能量（P0-3：创造单元无限供电相邻机器）
        doActiveEnergyIo();
    }
}
