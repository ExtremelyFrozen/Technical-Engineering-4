package com.modularmc.ten.common.blockentity.machine;

import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;
import com.modularmc.ten.api.option.IngredientType;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class CellBlockEntity extends CmMachineBlockEntity {

    public CellBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setCapacity(kFE(100));
        setEfficiency(100);
    }

    @Override
    public int inventorySize() {
        return 0;
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
        return false;
    }

    @Override
    public void tick() {
        doBaseData();
        if (energyStorage != null) {
            data.set(com.modularmc.ten.api.blockentity.CmMachineBlockEntity.ENERGY, energyStorage.getEnergyStored());
        }
    }

    @Override
    public boolean hasUpgrade() {
        return false;
    }
}
