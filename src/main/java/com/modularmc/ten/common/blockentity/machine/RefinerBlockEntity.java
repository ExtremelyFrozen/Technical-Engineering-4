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
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        return buildMachineUI(holder, TENMachineBlockUIFactory.backgroundFor(machineType()), root -> {
            root.addChild(TENMachineBlockUIFactory.machineSlot(this, 0, 58, 34));
            root.addChild(TENMachineBlockUIFactory.machineSlot(this, 1, 117, 34));
        }, root -> {
            root.addChild(TENMachineBlockUIFactory.energyGauge(this, 9, 18, 14, 46, 0, 0, true));
            root.addChild(TENMachineBlockUIFactory.energyGauge(this, 60, 56, 13, 13, 14, 0, false));
            root.addChild(TENMachineBlockUIFactory.progressGauge(this, 84, 35, 22, 16, 27, 159, false));
            root.addChild(TENMachineBlockUIFactory.fluidGauge(this, 37, 17, 18, 50, 0, true));
            root.addChild(TENMachineBlockUIFactory.fluidGauge(this, 143, 17, 18, 50, 1, true));
        });
    }

    @Override
    public FormsCombinedRecipe findRecipe() {
        if (level == null) return null;
        var recipes = level.getServer().getRecipeManager().recipeMap().byType(TENRecipeTypes.REFINER_T.get());
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
