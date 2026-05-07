package com.modularmc.ten.common.blockentity.machine;

import com.modularmc.ten.api.blockentity.EngineBlockEntity;
import com.modularmc.ten.api.option.IngredientType;
import com.modularmc.ten.common.blockentity.MatchFuel;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

public class ExtractorBlockEntity extends EngineBlockEntity {

    public ExtractorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setCapacity(kFE(60));
        setEfficiency(30);
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
        return MatchFuel.matchFuel(stack, true) > 0;
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
        return MatchFuel.matchFuel(stack, simulate);
    }
}
