package com.modularmc.ten.common.blockentity.machine;

import com.modularmc.ten.TENConstants;
import com.modularmc.ten.api.blockentity.RecipeMachineBlockEntity;
import com.modularmc.ten.api.blockentity.SlotInfo;
import com.modularmc.ten.api.option.IngredientType;
import com.modularmc.ten.api.option.MachineType;
import com.modularmc.ten.api.recipe.FormsCombinedRecipe;
import com.modularmc.ten.common.data.TENRecipeTypes;
import com.modularmc.ten.common.data.TENTags;
import com.modularmc.ten.common.gui.TENMachineBlockUIFactory;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;

public class CompressorBlockEntity extends RecipeMachineBlockEntity {

    public CompressorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, new SlotInfo(0, 1, 2, 2, 0, -1, 0, -1));
        setCapacity(kFE(20));
        setEfficiency(15);
    }

    @Override
    public int machineType() {
        return MachineType.COMPRESSOR;
    }

    @Override
    public int inventorySize() {
        return 3;
    }

    @Override
    public IngredientType slotType(int slot) {
        if (slot <= 1) return IngredientType.INPUT;
        if (slot == 2) return IngredientType.OUTPUT;
        return IngredientType.IGNORE;
    }

    @Override
    public boolean valid(int slot, ItemStack stack) {
        boolean mould = stack.is(TENTags.MOULDS);
        if (slot == 0) {
            return !mould;
        }
        if (slot == 1) {
            return mould;
        }
        return false;
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
            // 2 输入竖排（39,14 / 39,50，块 14~68 中线 41 与能量条对齐），输出大槽
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 0, 39, 14));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 1, 39, 50));
            root.addChild(TENMachineBlockUIFactory.machineSlotLarge(this, 2, 113, 28));
        }, root -> {
            root.addChild(TENMachineBlockUIFactory.energyGaugeModular(this, 8, 18, true));
            // mini 装饰（静态整件 8x54）：紧贴输入槽右缘，标示上下输入槽流向（目标在上），不承载进度
            root.addChild(TENMachineBlockUIFactory.verticalProgressMini(39, 14, 18, TENConstants.PROGRESS_ARROW_MINI_COMPRESSOR_BG, true));
            // 进度箭头（modular 素材族 22x16）：进度显示由原进度条承担，与 mini 装饰水平错开
            root.addChild(TENMachineBlockUIFactory.progressGaugeModular(this, 74, 33, false));
        });
    }

    @Override
    public FormsCombinedRecipe findRecipe() {
        if (level == null) return null;
        var recipes = level.getServer().getRecipeManager().recipeMap().byType(TENRecipeTypes.COMPRESSOR_T.get());
        for (var holder : recipes) {
            var recipe = holder.value();
            if (recipe instanceof FormsCombinedRecipe r && r.matches(itemHandler, tanks, this::slotType, this::tankType)) {
                r.recipeType = TENRecipeTypes.COMPRESSOR_T.get();
                r.serializer = TENRecipeTypes.COMPRESSOR_S.get();
                return r;
            }
        }
        return null;
    }
}
