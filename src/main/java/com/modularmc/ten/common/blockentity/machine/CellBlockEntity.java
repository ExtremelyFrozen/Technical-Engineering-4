package com.modularmc.ten.common.blockentity.machine;

import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;
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
        // 能量单元两个槽都需要玩家放入带 FE 的电池/物品（充放电），必须可进可出（BOTH）。
        // 修复：原 IGNORE 的 canIn()/canOut() 均为 false，导致 GUI 无法放入物品（26.1.2 对齐）。
        return IngredientType.BOTH;
    }

    @Override
    public boolean valid(int slot, ItemStack stack) {
        var energy = stack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM);
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
            root.addChild(TENMachineBlockUIFactory.machineSlotPower(this, 0, 42, 32, false));
            root.addChild(TENMachineBlockUIFactory.machineSlotPower(this, 1, 115, 32, true));
        }, root -> root.addChild(TENMachineBlockUIFactory.energyGaugeModular(this, 81, 18, true)));
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
            var energy0 = stack0.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM);
            if (energy0 != null && energy0.canExtract()) {
                int diff = energy0.extractEnergy(Math.min(maxReceiveEnergy, maxStorageEnergy - energyStorage.getEnergyStored()), false);
                if (diff > 0) {
                    energyStorage.receiveEnergy(diff, false);
                }
            }
        }

        if (stack1.getCount() == 1) {
            var energy1 = stack1.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM);
            if (energy1 != null && energy1.canReceive()) {
                int diff = energy1.receiveEnergy(Math.min(maxExtractEnergy, energyStorage.getEnergyStored()), false);
                if (diff > 0) {
                    energyStorage.extractEnergy(diff, false);
                }
            }
        }

        // 主动能量输出：向相邻能量接收方推送能量（P0-3：相邻机器应能直接获得能量单元的电力）
        doActiveEnergyIo();
    }

    @Override
    public boolean supportsUpgradeSlots() {
        return false;
    }
}
