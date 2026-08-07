package com.modularmc.ten.common.blockentity.machine;

import com.modularmc.ten.api.blockentity.ProcessingMachineBlockEntity;
import com.modularmc.ten.api.capability.MachineFluidTank;
import com.modularmc.ten.api.option.IngredientType;
import com.modularmc.ten.api.option.MachineType;
import com.modularmc.ten.common.data.TENFluids;
import com.modularmc.ten.common.gui.TENMachineBlockUIFactory;
import com.modularmc.ten.common.item.upgrades.IUpgradableMachine;
import com.modularmc.ten.common.item.upgrades.LevelupKnow;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;

import java.util.Objects;
import java.util.Optional;

public class FurnaceBlockEntity extends ProcessingMachineBlockEntity {

    /** Capacity for the XP fluid output tank (always present). */
    private static final int XP_TANK_CAPACITY = 4000;

    /**
     * XP fluid conversion: ticks per mB (0.1 mB/tick).
     * Formula: max(1, round(cookingTime / XP_FLUID_TICKS_PER_MB))
     */
    private static final int XP_FLUID_TICKS_PER_MB = 10;

    private ResourceKey<Recipe<?>> lastRecipeId;

    public FurnaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setCapacity(kFE(20));
        setEfficiency(15);
        tanks.add(new MachineFluidTank(XP_TANK_CAPACITY)); // XP output tank (always present)
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
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 0, 39, 32));
            root.addChild(TENMachineBlockUIFactory.machineSlotLarge(this, 1, 113, 28));
        }, root -> {
            root.addChild(TENMachineBlockUIFactory.energyGaugeModular(this, 8, 18, true));
            root.addChild(TENMachineBlockUIFactory.progressGaugeModular(this, 74, 33, false));
            // XP fluid gauge — always visible, no dependency on Knowledge.
            // Tank is always present; Knowledge only controls XP production.
            // 契约：createXpFluidSlot(143,17,18,50) — 输出大槽(113,28)右缘139 右侧 x=143（间隔4）、主区垂直对齐 y=17（中线42≈41.5）
            root.addChild(TENMachineBlockUIFactory.createXpFluidSlot(this, 143, 17, 18, 50));
        });
    }

    /**
     * Get the current recipe using the active recipe mode.
     * <p>
     * Recipe mode is determined by installed Blast/Smoke upgrades:
     * <ul>
     * <li>{@link IUpgradableMachine#RECIPE_MODE_SMELTING} → {@link RecipeType#SMELTING}</li>
     * <li>{@link IUpgradableMachine#RECIPE_MODE_BLASTING} → {@link RecipeType#BLASTING}</li>
     * <li>{@link IUpgradableMachine#RECIPE_MODE_SMOKING} → {@link RecipeType#SMOKING}</li>
     * </ul>
     * Default is SMELTING when no mode-switching upgrade is installed.
     */
    @SuppressWarnings("unchecked")
    private Optional<RecipeHolder<AbstractCookingRecipe>> getCurrentRecipe() {
        if (level == null || level.getServer() == null) return Optional.empty();
        ItemStack input = itemHandler.getStackInSlot(0);
        if (input.isEmpty()) return Optional.empty();
        var recipeInput = new SingleRecipeInput(input);

        return switch (recipeMode) {
            case IUpgradableMachine.RECIPE_MODE_BLASTING -> (Optional) level.getServer().getRecipeManager()
                    .getRecipeFor(RecipeType.BLASTING, recipeInput, level);
            case IUpgradableMachine.RECIPE_MODE_SMOKING -> (Optional) level.getServer().getRecipeManager()
                    .getRecipeFor(RecipeType.SMOKING, recipeInput, level);
            default -> (Optional) level.getServer().getRecipeManager()
                    .getRecipeFor(RecipeType.SMELTING, recipeInput, level);
        };
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

        ResourceKey<Recipe<?>> newId = recipeOpt.get().id();
        boolean identityChanged = !Objects.equals(lastRecipeId, newId);

        if (identityChanged) {
            progress = 0;
            clearLockedBatch();
        }
        lastRecipeId = newId;

        if (identityChanged || !hasLockedBatch()) {
            maxProgress = Math.max(1, (int) Math.ceil(baseTickTime() * durationMultiplier));
            lockMaxProgressForNewOperation(maxProgress);

            int B_theory = 1 + batch;
            ItemStack input = itemHandler.getStackInSlot(0);
            ItemStack result = recipeOpt.get().value().assemble(new SingleRecipeInput(input));
            int inputCount = input.getCount();
            int B_byInput = inputCount;

            ItemStack existingOutput = itemHandler.getStackInSlot(1);
            int availOutputSpace;
            if (existingOutput.isEmpty()) {
                availOutputSpace = itemHandler.getSlotLimit(1);
            } else if (ItemStack.isSameItem(existingOutput, result)) {
                availOutputSpace = existingOutput.getMaxStackSize() - existingOutput.getCount();
            } else {
                availOutputSpace = 0;
            }
            int resultCount = result.getCount();
            int B_byOutput = resultCount > 0 ? availOutputSpace / resultCount : 0;

            int baseFe = Math.max(1, getActualEfficiency());
            int B_byEnergy = energyStorage != null ? energyStorage.getEnergyStored() / baseFe : 0;

            // P3: Knowledge gates XP production — when installed, constrain B by XP tank space.
            // The tank itself is always present; only the production rate is gated.
            int B_byXpFluid = Integer.MAX_VALUE;
            if (hasUpgrade(LevelupKnow.class)) {
                AbstractCookingRecipe recipe = recipeOpt.get().value();
                int xpPerUnit = calculateXpFluidPerUnit(recipe);
                if (xpPerUnit > 0 && !tanks.isEmpty()) {
                    int availXp = tanks.get(0).getCapacity() - tanks.get(0).getFluidAmount();
                    B_byXpFluid = availXp / xpPerUnit;
                } else if (xpPerUnit > 0) {
                    B_byXpFluid = 0;
                }
            }

            int B_byFluid = B_byXpFluid;

            if (!validateAndLockB(B_theory, B_byInput, B_byFluid, B_byOutput, B_byEnergy)) {
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
        if (output.getCount() + result.getCount() * B > output.getMaxStackSize()) return true;

        // P3: Knowledge gates XP tank space check (production path only)
        if (hasUpgrade(LevelupKnow.class)) {
            AbstractCookingRecipe recipe = recipeOpt.get().value();
            int xpPerUnit = calculateXpFluidPerUnit(recipe);
            if (xpPerUnit > 0 && !tanks.isEmpty()) {
                FluidStack xpFluid = new FluidStack(TENFluids.LIQUID_XP_SOURCE.get(), xpPerUnit);
                int filled = tanks.get(0).fill(xpFluid, IFluidHandler.FluidAction.SIMULATE);
                if (filled < xpPerUnit) return true;
            }
        }

        return false;
    }

    @Override
    public void onCookFinish() {
        var recipeOpt = getCurrentRecipe();
        if (recipeOpt.isEmpty() || level == null) return;

        ResourceKey<Recipe<?>> currentId = recipeOpt.get().id();
        if (!Objects.equals(lastRecipeId, currentId)) {
            clearLockedBatch();
            return;
        }

        int B = getLockedBatchSize();
        AbstractCookingRecipe recipe = recipeOpt.get().value();
        ItemStack result = recipe.assemble(new SingleRecipeInput(itemHandler.getStackInSlot(0)));
        ItemStack input = itemHandler.getStackInSlot(0);
        ItemStack output = itemHandler.getStackInSlot(1);

        if (input.getCount() < B) {
            clearLockedBatch();
            return;
        }

        long totalResult = (long) result.getCount() * B;
        if (totalResult > Integer.MAX_VALUE) {
            clearLockedBatch();
            return;
        }
        int resultCount = (int) totalResult;

        if (!output.isEmpty()) {
            if (!ItemStack.isSameItem(output, result)) {
                clearLockedBatch();
                return;
            }
            int outputLimit = Math.min(itemHandler.getSlotLimit(1), output.getMaxStackSize());
            if (output.getCount() + resultCount > outputLimit) {
                return;
            }
        }

        // P3: Knowledge gates XP production — pre-validate XP tank space
        boolean hasKnowledge = hasUpgrade(LevelupKnow.class);
        int xpFluidTotal = 0;
        if (hasKnowledge) {
            int xpPerUnit = calculateXpFluidPerUnit(recipe);
            if (xpPerUnit > 0) {
                long totalXpLong = (long) xpPerUnit * B;
                if (totalXpLong > Integer.MAX_VALUE) {
                    clearLockedBatch();
                    return;
                }
                xpFluidTotal = (int) totalXpLong;
                if (!tanks.isEmpty()) {
                    FluidStack totalXpFluid = new FluidStack(TENFluids.LIQUID_XP_SOURCE.get(), xpFluidTotal);
                    int filled = tanks.get(0).fill(totalXpFluid, IFluidHandler.FluidAction.SIMULATE);
                    if (filled < xpFluidTotal) {
                        return;
                    }
                } else {
                    clearLockedBatch();
                    return;
                }
            }
        }

        // ── Atomic execution with rollback ──
        ItemStack inputSnapshot = itemHandler.getStackInSlot(0).copy();
        ItemStack outputSnapshot = itemHandler.getStackInSlot(1).copy();
        FluidStack tankSnapshot = (hasKnowledge && !tanks.isEmpty()) ? tanks.get(0).getFluid().copy() : FluidStack.EMPTY;

        try {
            itemHandler.extractItem(0, B, false);

            if (output.isEmpty()) {
                ItemStack batchResult = result.copy();
                batchResult.setCount(resultCount);
                itemHandler.setStackInSlot(1, batchResult);
            } else {
                output.grow(resultCount);
            }

            if (hasKnowledge && xpFluidTotal > 0 && !tanks.isEmpty()) {
                FluidStack totalXpFluid = new FluidStack(TENFluids.LIQUID_XP_SOURCE.get(), xpFluidTotal);
                tanks.get(0).fill(totalXpFluid, IFluidHandler.FluidAction.EXECUTE);
            }
        } catch (Exception e) {
            itemHandler.setStackInSlot(0, inputSnapshot);
            itemHandler.setStackInSlot(1, outputSnapshot);
            if (hasKnowledge && !tanks.isEmpty()) {
                tanks.get(0).drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
                if (!tankSnapshot.isEmpty()) {
                    tanks.get(0).fill(tankSnapshot, IFluidHandler.FluidAction.EXECUTE);
                }
            }
            throw new RuntimeException("Furnace onCookFinish failed and rolled back", e);
        }

        clearLockedBatch();
    }

    /**
     * Calculate the XP fluid amount (in mB) produced per recipe unit.
     * <p>
     * Formula: max(1, round(cookingTime / {@link #XP_FLUID_TICKS_PER_MB}))
     * where XP_FLUID_TICKS_PER_MB = 10 (0.1 mB per tick).
     * <p>
     * For a standard 200-tick smelting recipe: max(1, round(200/10)) = 20 mB.
     * Positive cookingTime always produces at least 1 mB.
     * Used by conditionStart, cooking, and onCookFinish — shared formula ensures consistency.
     */
    private static int calculateXpFluidPerUnit(AbstractCookingRecipe recipe) {
        int ticks = recipe.cookingTime();
        if (ticks <= 0) return 0;
        return Math.max(1, (int) Math.round((double) ticks / XP_FLUID_TICKS_PER_MB));
    }

    @Override
    public IngredientType tankType(int tank) {
        return IngredientType.OUTPUT;
    }

    @Override
    public boolean hasFaceCapabilityFluid(net.minecraft.core.Direction side) {
        // Tank and capability are always present — Knowledge gates only production logic.
        // Ensures residual XP fluid remains accessible after Knowledge is uninstalled.
        return true;
    }
}
