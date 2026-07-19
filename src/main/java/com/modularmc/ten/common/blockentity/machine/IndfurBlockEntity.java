package com.modularmc.ten.common.blockentity.machine;

import com.modularmc.ten.api.blockentity.RecipeMachineBlockEntity;
import com.modularmc.ten.api.blockentity.SlotInfo;
import com.modularmc.ten.api.option.IngredientType;
import com.modularmc.ten.api.option.MachineType;
import com.modularmc.ten.api.recipe.FormsCombinedRecipe;
import com.modularmc.ten.common.data.TENRecipeTypes;
import com.modularmc.ten.common.gui.TENMachineBlockUIFactory;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;

public class IndfurBlockEntity extends RecipeMachineBlockEntity {

    public IndfurBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, new SlotInfo(0, 2, 3, 3, 0, -1, 0, -1));
        setCapacity(kFE(20));
        setEfficiency(15);
    }

    @Override
    public int machineType() {
        return MachineType.INDUCTION_FURNACE;
    }

    @Override
    public int inventorySize() {
        return 4;
    }

    @Override
    public IngredientType slotType(int slot) {
        if (slot <= 2) return IngredientType.INPUT;
        if (slot == 3) return IngredientType.OUTPUT;
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
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        return buildMachineUI(holder, TENMachineBlockUIFactory.backgroundFor(machineType()), root -> {
            root.addChild(TENMachineBlockUIFactory.machineSlot(this, 0, 33, 20));
            root.addChild(TENMachineBlockUIFactory.machineSlot(this, 1, 51, 20));
            root.addChild(TENMachineBlockUIFactory.machineSlot(this, 2, 69, 20));
            root.addChild(TENMachineBlockUIFactory.machineSlot(this, 3, 127, 34));
        }, root -> {
            root.addChild(TENMachineBlockUIFactory.energyGauge(this, 9, 18, 14, 46, 0, 0, true));
            root.addChild(TENMachineBlockUIFactory.fuelGauge(this, 54, 48, 13, 13, 14, 0, false));
            root.addChild(TENMachineBlockUIFactory.progressGauge(this, 93, 35, 22, 16, 27, 0, false));
        });
    }

    @Override
    public FormsCombinedRecipe findRecipe() {
        if (level == null) return null;
        var recipes = level.getServer().getRecipeManager().recipeMap().byType(TENRecipeTypes.INDUCTION_FURNACE_T.get());
        for (var holder : recipes) {
            var recipe = holder.value();
            if (recipe instanceof FormsCombinedRecipe r && r.matchesExactInputs(itemHandler, tanks, this::slotType, this::tankType)) {
                r.recipeType = TENRecipeTypes.INDUCTION_FURNACE_T.get();
                r.serializer = TENRecipeTypes.INDUCTION_FURNACE_S.get();
                return r;
            }
        }
        return null;
    }

    @Override
    protected boolean revalidateInputs() {
        // Indfur uses strict exact-input matching — same as findRecipe
        if (currentRecipe == null || itemHandler == null) return false;
        return currentRecipe.matchesExactInputs(itemHandler, tanks, this::slotType, this::tankType);
    }
}
