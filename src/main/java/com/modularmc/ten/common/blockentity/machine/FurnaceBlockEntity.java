package com.modularmc.ten.common.blockentity.machine;

import com.modularmc.ten.api.blockentity.ProcessingMachineBlockEntity;
import com.modularmc.ten.api.option.IngredientType;
import com.modularmc.ten.api.option.MachineType;
import com.modularmc.ten.common.gui.TENMachineBlockUIFactory;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;

import java.util.Optional;

public class FurnaceBlockEntity extends ProcessingMachineBlockEntity {

    public FurnaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setCapacity(kFE(20));
        setEfficiency(15);
    }

    @Override
    public int machineType() {
        return MachineType.FURNACE;
    }

    @Override
    public int inventorySize() {
        return 2;
    }

    @Override
    public IngredientType slotType(int slot) {
        return switch (slot) {
            case 0 -> IngredientType.INPUT;
            case 1 -> IngredientType.OUTPUT;
            default -> IngredientType.IGNORE;
        };
    }

    @Override
    public int baseTickTime() {
        var recipe = getCurrentRecipe();
        return recipe.map(r -> r.value().cookingTime() / 2).orElse(200);
    }

    @Override
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        return buildMachineUI(holder, TENMachineBlockUIFactory.backgroundFor(machineType()), root -> {
            root.addChild(TENMachineBlockUIFactory.machineSlot(this, 0, 43, 20));
            root.addChild(TENMachineBlockUIFactory.machineSlot(this, 1, 115, 34));
        }, root -> {
            root.addChild(TENMachineBlockUIFactory.energyGauge(this, 9, 18, 14, 46, 0, 0, true));
            root.addChild(TENMachineBlockUIFactory.fuelGauge(this, 45, 48, 13, 13, 14, 0, false));
            root.addChild(TENMachineBlockUIFactory.progressGauge(this, 76, 35, 22, 16, 27, 0, false));
        });
    }

    private Optional<RecipeHolder<SmeltingRecipe>> getCurrentRecipe() {
        if (level == null || level.getServer() == null) return Optional.empty();
        ItemStack input = itemHandler.getStackInSlot(0);
        if (input.isEmpty()) return Optional.empty();
        return level.getServer().getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(input), level);
    }

    @Override
    public boolean conditionStart() {
        return getCurrentRecipe().isPresent();
    }

    @Override
    public boolean cooking() {
        var recipeOpt = getCurrentRecipe();
        if (recipeOpt.isEmpty()) return false;

        ItemStack result = recipeOpt.get().value().assemble(new SingleRecipeInput(itemHandler.getStackInSlot(0)));
        ItemStack output = itemHandler.getStackInSlot(1);

        if (output.isEmpty()) return false;
        if (!ItemStack.isSameItem(output, result)) return false;
        return output.getCount() + result.getCount() > output.getMaxStackSize();
    }

    @Override
    public void onCookFinish() {
        var recipeOpt = getCurrentRecipe();
        if (recipeOpt.isEmpty() || level == null) return;

        ItemStack result = recipeOpt.get().value().assemble(new SingleRecipeInput(itemHandler.getStackInSlot(0)));
        ItemStack output = itemHandler.getStackInSlot(1);

        if (output.isEmpty()) {
            itemHandler.setStackInSlot(1, result.copy());
        } else if (ItemStack.isSameItem(output, result)) {
            output.grow(result.getCount());
        }

        itemHandler.getStackInSlot(0).shrink(1);
    }
}
