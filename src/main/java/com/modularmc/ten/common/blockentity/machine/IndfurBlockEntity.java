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
            // 3 输入横排（30/48/66，组宽 54），y=32 中线 41 与能量条对齐；输出大槽
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 0, 30, 32));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 1, 48, 32));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 2, 66, 32));
            root.addChild(TENMachineBlockUIFactory.machineSlotLarge(this, 3, 122, 28));
        }, root -> {
            root.addChild(TENMachineBlockUIFactory.energyGaugeModular(this, 8, 18, true));
            root.addChild(TENMachineBlockUIFactory.progressGaugeModular(this, 92, 33, false));
        });
    }

    @Override
    public FormsCombinedRecipe findRecipe() {
        if (level == null) return null;
        var recipes = level.getRecipeManager().getAllRecipesFor(TENRecipeTypes.INDUCTION_FURNACE_T.get());
        for (var holder : recipes) {
            var recipe = holder.value();
            // 1.21.1 的 matches 已内联严格语义（occupied == required），与 26.1.2 的
            // matchesExactInputs 等价（多余输入槽占用时不匹配）
            if (recipe instanceof FormsCombinedRecipe r && r.matches(itemHandler, tanks, this::slotType, this::tankType)) {
                r.recipeType = TENRecipeTypes.INDUCTION_FURNACE_T.get();
                r.serializer = TENRecipeTypes.INDUCTION_FURNACE_S.get();
                return r.assignId(holder.id());
            }
        }
        return null;
    }

    @Override
    protected boolean revalidateInputs() {
        // Indfur 使用严格 exact-input 匹配重验（26.1.2 对齐，与 findRecipe 同源）
        if (currentRecipe == null || itemHandler == null) return false;
        return currentRecipe.matches(itemHandler, tanks, this::slotType, this::tankType);
    }
}
