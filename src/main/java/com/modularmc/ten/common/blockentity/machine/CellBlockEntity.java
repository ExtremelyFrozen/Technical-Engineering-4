package com.modularmc.ten.common.blockentity.machine;

import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;
import com.modularmc.ten.api.capability.CapabilityAdapters;
import com.modularmc.ten.api.option.IngredientType;
import com.modularmc.ten.common.gui.TENMachineBlockUIFactory;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;

public class CellBlockEntity extends CmMachineBlockEntity {

    public CellBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setCapacity(kFE(1000));
        setEfficiency(100);
    }

    @Override
    public int inventorySize() {
        return 2;
    }

    @Override
    public int machineType() {
        return com.modularmc.ten.api.option.MachineType.CELL;
    }

    @Override
    public IngredientType slotType(int slot) {
        return IngredientType.IGNORE;
    }

    @Override
    public boolean valid(int slot, ItemStack stack) {
        var energy = CapabilityAdapters.getEnergy(stack);
        if (energy == null) return false;
        if (slot == 0) {
            return energy.canExtract();
        }
        if (slot == 1) {
            return energy.canReceive() && energy.getEnergyStored() < energy.getMaxEnergyStored();
        }
        return false;
    }

    @Override
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        return buildMachineUI(holder, TENMachineBlockUIFactory.backgroundFor(machineType()), root -> {
            root.addChild(TENMachineBlockUIFactory.machineSlot(this, 0, 42, 32));
            root.addChild(TENMachineBlockUIFactory.machineSlot(this, 1, 115, 32));
        }, root -> root.addChild(TENMachineBlockUIFactory.energyGauge(this, 81, 18, 14, 46, 0, 0, true)));
    }

    @Override
    public void tick() {
        doBaseData();

        // Sync active state based on energy level for block model texture switching
        // Always runs, even when io is disabled, so the block model shows correct texture
        setActive(energyStorage != null && energyStorage.getEnergyStored() > 0);

        if (!signalAllowRun() || energyStorage == null || itemHandler == null) {
            return;
        }

        ItemStack stack0 = itemHandler.getStackInSlot(0);
        ItemStack stack1 = itemHandler.getStackInSlot(1);

        if (stack0.getCount() == 1) {
            var energy0 = CapabilityAdapters.getEnergy(stack0);
            if (energy0 != null && energy0.canExtract()) {
                int diff = energy0.extractEnergy(Math.min(maxReceiveEnergy, maxStorageEnergy - energyStorage.getEnergyStored()), false);
                if (diff > 0) {
                    energyStorage.receiveEnergy(diff, false);
                }
            }
        }

        if (stack1.getCount() == 1) {
            var energy1 = CapabilityAdapters.getEnergy(stack1);
            if (energy1 != null && energy1.canReceive()) {
                int diff = energy1.receiveEnergy(Math.min(maxExtractEnergy, energyStorage.getEnergyStored()), false);
                if (diff > 0) {
                    energyStorage.extractEnergy(diff, false);
                }
            }
        }
    }

    @Override
    public boolean hasUpgrade() {
        return false;
    }

    @Override
    public boolean supportsUpgradeSlots() {
        return false;
    }
}
