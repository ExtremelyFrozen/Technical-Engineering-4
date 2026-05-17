package com.modularmc.ten.common.blockentity.machine;

import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;
import com.modularmc.ten.api.option.IngredientType;
import com.modularmc.ten.api.option.MachineType;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

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
        return 0;
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
    public void tick() {
        doBaseData();
        if (energyStorage == null) return;
        // Always full — extractEnergy can drain but this refills every tick
        energyStorage.setEnergy(maxStorageEnergy);
    }
}
