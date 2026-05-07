package com.modularmc.ten.common.blockentity.machine;

import com.modularmc.ten.api.blockentity.EngineBlockEntity;
import com.modularmc.ten.api.option.IngredientType;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

public class SolarBlockEntity extends EngineBlockEntity {

    public SolarBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setCapacity(kFE(80));
        setEfficiency(10);
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
    public int matchFuel(ItemStack stack, boolean simulate) {
        if (level != null && level.canSeeSky(worldPosition.above()) && level.isDay() && !level.isRaining()) {
            return 600;
        }
        return 0;
    }
}
