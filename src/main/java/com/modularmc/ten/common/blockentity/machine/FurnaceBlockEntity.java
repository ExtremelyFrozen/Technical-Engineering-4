package com.modularmc.ten.common.blockentity.machine;

import com.modularmc.ten.api.blockentity.ProcessingMachineBlockEntity;
import com.modularmc.ten.api.option.IngredientType;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public class FurnaceBlockEntity extends ProcessingMachineBlockEntity {

    public FurnaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setCapacity(kFE(20));
        setEfficiency(15);
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
        return recipe.map(r -> r.value().getCookingTime() / 2).orElse(200);
    }

    private Optional<RecipeHolder<SmeltingRecipe>> getCurrentRecipe() {
        if (level == null) return Optional.empty();
        ItemStack input = itemHandler.getStackInSlot(0);
        if (input.isEmpty()) return Optional.empty();
        return level.getRecipeManager()
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

        var registry = level != null ? level.registryAccess() : null;
        if (registry == null) return false;

        ItemStack result = recipeOpt.get().value().getResultItem(registry);
        ItemStack output = itemHandler.getStackInSlot(1);

        if (output.isEmpty()) return false;
        if (!ItemStack.isSameItem(output, result)) return false;
        return output.getCount() + result.getCount() > output.getMaxStackSize();
    }

    @Override
    public void onCookFinish() {
        var recipeOpt = getCurrentRecipe();
        if (recipeOpt.isEmpty() || level == null) return;

        var registry = level.registryAccess();
        ItemStack result = recipeOpt.get().value().getResultItem(registry);
        ItemStack output = itemHandler.getStackInSlot(1);

        if (output.isEmpty()) {
            itemHandler.setStackInSlot(1, result.copy());
        } else if (ItemStack.isSameItem(output, result)) {
            output.grow(result.getCount());
        }

        itemHandler.getStackInSlot(0).shrink(1);
    }
}
