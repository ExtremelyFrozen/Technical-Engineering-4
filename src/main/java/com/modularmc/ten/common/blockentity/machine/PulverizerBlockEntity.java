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

public class PulverizerBlockEntity extends RecipeMachineBlockEntity {

    public PulverizerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, new SlotInfo(0, 0, 1, 4, 0, -1, 0, -1));
        setCapacity(kFE(20));
        setEfficiency(15);
    }

    @Override
    public int machineType() {
        return MachineType.PULVERIZER;
    }

    @Override
    public int inventorySize() {
        return 5;
    }

    @Override
    public IngredientType slotType(int slot) {
        if (slot == 0) return IngredientType.INPUT;
        if (slot >= 1 && slot <= 4) return IngredientType.OUTPUT;
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
            // 2x2 输出块作为整体（104~142 x 25~61），输入 x=36, y=32（中线 41 与能量条对齐）
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 0, 36, 32));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 1, 104, 25));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 2, 122, 25));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 3, 104, 43));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 4, 122, 43));
        }, root -> {
            root.addChild(TENMachineBlockUIFactory.energyGaugeModular(this, 8, 18, true));
            root.addChild(TENMachineBlockUIFactory.progressGaugeModular(this, 68, 33, false));
        });
    }

    @Override
    public FormsCombinedRecipe findRecipe() {
        if (level == null) return null;
        var recipes = level.getServer().getRecipeManager().recipeMap().byType(TENRecipeTypes.PULVERIZER_T.get());
        for (var holder : recipes) {
            var recipe = holder.value();
            if (recipe instanceof FormsCombinedRecipe r && r.matches(itemHandler, tanks, this::slotType, this::tankType)) {
                r.recipeType = TENRecipeTypes.PULVERIZER_T.get();
                r.serializer = TENRecipeTypes.PULVERIZER_S.get();
                return r;
            }
        }
        return null;
    }
}
