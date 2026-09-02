package com.modularmc.ten.common.blockentity.machine;

import com.modularmc.ten.api.blockentity.EngineBlockEntity;
import com.modularmc.ten.api.option.IngredientType;
import com.modularmc.ten.api.option.MachineType;
import com.modularmc.ten.common.blockentity.MatchFuel;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

public class MetalizerBlockEntity extends EngineBlockEntity {

    /** 引擎基础产能（FE/t），与 setEfficiency 一致，供 JEI 燃料页显示。 */
    public static final int BASE_GENERATION_RATE = 80;

    public MetalizerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setCapacity(kFE(80));
        setEfficiency(80);
    }

    @Override
    public int machineType() {
        return MachineType.ENGINE_METAL;
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
        return MatchFuel.matchMetal(stack, true) > 0;
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
        return MatchFuel.matchMetal(stack, simulate);
    }
}
