package com.modularmc.ten.common.blockentity.machine;

import com.modularmc.ten.api.blockentity.RecipeMachineBlockEntity;
import com.modularmc.ten.api.blockentity.SlotInfo;
import com.modularmc.ten.api.option.IngredientType;
import com.modularmc.ten.api.option.MachineType;
import com.modularmc.ten.api.recipe.FormsCombinedRecipe;
import com.modularmc.ten.common.data.TENRecipeTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

public class RefinerBlockEntity extends RecipeMachineBlockEntity {

    public RefinerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, new SlotInfo(0, 0, 1, 1, 0, 0, 1, 1));
        setCapacity(kFE(20));
        setEfficiency(15);
        tanks.add(new com.modularmc.ten.api.capability.MachineFluidTank(6000));
        tanks.add(new com.modularmc.ten.api.capability.MachineFluidTank(6000));
    }

    @Override
    public int machineType() {
        return MachineType.REFINER;
    }

    @Override
    public int inventorySize() {
        return 2;
    }

    @Override
    public IngredientType slotType(int slot) {
        if (slot == 0) return IngredientType.INPUT;
        if (slot == 1) return IngredientType.OUTPUT;
        return IngredientType.IGNORE;
    }

    @Override
    public boolean valid(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public IngredientType tankType(int tank) {
        if (tank == 0) return IngredientType.INPUT;
        if (tank == 1) return IngredientType.OUTPUT;
        return IngredientType.IGNORE;
    }

    @Override
    public boolean valid(int slot, FluidStack stack) {
        return true;
    }

    @Override
    public FormsCombinedRecipe findRecipe() {
        if (level == null) return null;
        var recipes = level.getRecipeManager().getAllRecipesFor(TENRecipeTypes.REFINER_T.get());
        for (var holder : recipes) {
            var recipe = holder.value();
            if (recipe instanceof FormsCombinedRecipe r && r.matches(itemHandler, tanks, this::slotType, this::tankType)) {
                r.recipeType = TENRecipeTypes.REFINER_T.get();
                r.serializer = TENRecipeTypes.REFINER_S.get();
                return r;
            }
        }
        return null;
    }
}
