package com.modularmc.ten.common.blockentity.machine;

import com.modularmc.ten.api.blockentity.ProcessingMachineBlockEntity;
import com.modularmc.ten.api.option.IngredientType;
import com.modularmc.ten.api.option.MachineType;
import com.modularmc.ten.common.gui.TENMachineBlockUIFactory;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;

import java.util.Objects;
import java.util.Optional;

public class FurnaceBlockEntity extends ProcessingMachineBlockEntity {

    private ResourceKey<Recipe<?>> lastRecipeId;

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
        return recipe.map(r -> r.value().cookingTime()).orElse(200);
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
        var recipeOpt = getCurrentRecipe();
        if (recipeOpt.isEmpty()) {
            lastRecipeId = null;
            progress = 0;
            clearLockedBatch();
            return false;
        }

        // P1-S3: track recipe identity via stable recipe ResourceKey.
        // Progress is preserved when the same recipe continues,
        // and reset to 0 when recipe identity changes.
        ResourceKey<Recipe<?>> newId = recipeOpt.get().id();
        boolean identityChanged = !Objects.equals(lastRecipeId, newId);

        if (identityChanged) {
            progress = 0;
            clearLockedBatch();
        }
        lastRecipeId = newId;

        // Only compute and lock maxProgress + B when identity changed or no lock exists yet
        if (identityChanged || !hasLockedBatch()) {
            // Lock maxProgress with durationMultiplier captured at operation start
            maxProgress = Math.max(1, (int) Math.ceil(baseTickTime() * durationMultiplier));
            lockMaxProgressForNewOperation(maxProgress);

            // ── P1-T3a: Compute and lock B_actual ──
            int B_theory = 1 + batch;
            ItemStack input = itemHandler.getStackInSlot(0);
            ItemStack result = recipeOpt.get().value().assemble(new SingleRecipeInput(input));
            int inputCount = input.getCount();

            // B_byInput: how many batch units can the input support? (1 input per unit)
            int B_byInput = inputCount;

            // B_byOutput: how many batch units can the output slot support?
            ItemStack existingOutput = itemHandler.getStackInSlot(1);
            int availOutputSpace;
            if (existingOutput.isEmpty()) {
                // Use real slot limit instead of hardcoded 64
                availOutputSpace = itemHandler.getSlotLimit(1);
            } else if (ItemStack.isSameItem(existingOutput, result)) {
                availOutputSpace = existingOutput.getMaxStackSize() - existingOutput.getCount();
            } else {
                availOutputSpace = 0; // wrong item in output
            }
            int resultCount = result.getCount();
            int B_byOutput = resultCount > 0 ? availOutputSpace / resultCount : 0;

            // B_byEnergy: how many ticks can current energy sustain?
            int baseFe = Math.max(1, getActualEfficiency());
            int B_byEnergy = energyStorage != null ? energyStorage.getEnergyStored() / baseFe : 0;

            // Lock B_actual — validateAndLockB clears lock on failure
            if (!validateAndLockB(B_theory, B_byInput, Integer.MAX_VALUE, B_byOutput, B_byEnergy)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean cooking() {
        var recipeOpt = getCurrentRecipe();
        if (recipeOpt.isEmpty()) return false;

        int B = getLockedBatchSize();
        ItemStack result = recipeOpt.get().value().assemble(new SingleRecipeInput(itemHandler.getStackInSlot(0)));
        ItemStack output = itemHandler.getStackInSlot(1);

        if (output.isEmpty()) return false;
        if (!ItemStack.isSameItem(output, result)) return true;
        return output.getCount() + result.getCount() * B > output.getMaxStackSize();
    }

    @Override
    public void onCookFinish() {
        var recipeOpt = getCurrentRecipe();
        if (recipeOpt.isEmpty() || level == null) return;

        // S3: verify recipe identity still matches what was locked
        ResourceKey<Recipe<?>> currentId = recipeOpt.get().id();
        if (!Objects.equals(lastRecipeId, currentId)) {
            // Recipe changed since lock was set — don't consume
            clearLockedBatch();
            return;
        }

        int B = getLockedBatchSize();
        ItemStack result = recipeOpt.get().value().assemble(new SingleRecipeInput(itemHandler.getStackInSlot(0)));
        ItemStack input = itemHandler.getStackInSlot(0);
        ItemStack output = itemHandler.getStackInSlot(1);

        // ── Pre-validation before any mutation ──

        // 1. Input must have at least B items
        if (input.getCount() < B) {
            clearLockedBatch();
            return;
        }

        // 2. Output must be able to accommodate B×result (long for overflow safety)
        long totalResult = (long) result.getCount() * B;
        if (totalResult > Integer.MAX_VALUE) {
            clearLockedBatch();
            return;
        }
        int resultCount = (int) totalResult;

        if (!output.isEmpty()) {
            if (!ItemStack.isSameItem(output, result)) {
                // Wrong item in output — cannot place result
                clearLockedBatch();
                return;
            }
            int outputLimit = Math.min(itemHandler.getSlotLimit(1), output.getMaxStackSize());
            if (output.getCount() + resultCount > outputLimit) {
                // Output would overflow
                return; // keep lock — waiting for space
            }
        }

        // ── Atomic execution with rollback ──
        // Save snapshots for rollback
        ItemStack inputSnapshot = itemHandler.getStackInSlot(0).copy();
        ItemStack outputSnapshot = itemHandler.getStackInSlot(1).copy();

        try {
            // Consume B inputs at once
            itemHandler.extractItem(0, B, false);

            // Place batch result in output
            if (output.isEmpty()) {
                ItemStack batchResult = result.copy();
                batchResult.setCount(resultCount);
                itemHandler.setStackInSlot(1, batchResult);
            } else {
                output.grow(resultCount);
            }
        } catch (Exception e) {
            // Rollback on any failure
            itemHandler.setStackInSlot(0, inputSnapshot);
            itemHandler.setStackInSlot(1, outputSnapshot);
            throw new RuntimeException("Furnace onCookFinish failed and rolled back", e);
        }

        // Clear lock after successful completion
        clearLockedBatch();
    }
}
