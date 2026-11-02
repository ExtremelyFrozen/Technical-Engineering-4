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
            // 输入小槽（58,32，中线 41 与能量条对齐），输出大槽（116,28），双流体槽（31/151, y=17 保持）
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 0, 58, 32));
            root.addChild(TENMachineBlockUIFactory.machineSlotLarge(this, 1, 116, 28));
        }, root -> {
            root.addChild(TENMachineBlockUIFactory.energyGaugeModular(this, 8, 18, true));
            root.addChild(TENMachineBlockUIFactory.progressGaugeModular(this, 85, 33, false));
            root.addChild(TENMachineBlockUIFactory.fluidGaugeModular(this, 31, 17, 18, 50, 0));
            root.addChild(TENMachineBlockUIFactory.fluidGaugeModular(this, 151, 17, 18, 50, 1));
        });
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
                return r.assignId(holder.id());
            }
        }
        return null;
    }
}
