package com.modularmc.ten.common.blockentity.machine;

import com.modularmc.ten.api.blockentity.IUpgradableMachine;
import com.modularmc.ten.api.blockentity.ProcessingMachineBlockEntity;
import com.modularmc.ten.api.capability.MachineFluidTank;
import com.modularmc.ten.api.option.IngredientType;
import com.modularmc.ten.api.option.MachineType;
import com.modularmc.ten.common.data.TENFluids;
import com.modularmc.ten.common.gui.TENMachineBlockUIFactory;
import com.modularmc.ten.common.item.upgrades.LevelupKnow;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
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

    /** XP 流体输出罐容量（恒存在）。 */
    private static final int XP_TANK_CAPACITY = 4000;

    /**
     * XP 流体换算：每 mB 对应 tick 数（0.1 mB/tick）。
     * 公式：max(1, round(cookingTime / XP_FLUID_TICKS_PER_MB))
     */
    private static final int XP_FLUID_TICKS_PER_MB = 10;

    /** 上一周期的配方身份（处理链锁定期间禁止漂移到其他配方）。 */
    private ResourceLocation lastRecipeId;

    public FurnaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setCapacity(kFE(20));
        setEfficiency(15);
        tanks.add(new MachineFluidTank(XP_TANK_CAPACITY)); // XP 输出罐（恒存在）
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

    /** XP 输出罐：可经桶/管道取出（canOut），禁注入（canIn）。 */
    @Override
    public IngredientType tankType(int tank) {
        return IngredientType.OUTPUT;
    }

    /** 基础周期 tick：配方 cookingTime 全值（旧版误除 2 导致处理速度快一倍，勿回退）。 */
    @Override
    public int baseTickTime() {
        var recipe = getCurrentRecipe();
        return recipe.map(r -> r.value().getCookingTime()).orElse(200);
    }

    @Override
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        return buildMachineUI(holder, TENMachineBlockUIFactory.backgroundFor(machineType()), root -> {
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 0, 39, 32));
            root.addChild(TENMachineBlockUIFactory.machineSlotLarge(this, 1, 113, 28));
        }, root -> {
            root.addChild(TENMachineBlockUIFactory.energyGaugeModular(this, 8, 18, true));
            root.addChild(TENMachineBlockUIFactory.progressGaugeModular(this, 74, 33, false));
            // 契约：createXpFluidSlot(143,17,18,50) — 输出大槽(113,28)右缘139 右侧 x=143（间隔4）、主区垂直对齐 y=17（中线42≈41.5）
            root.addChild(TENMachineBlockUIFactory.createXpFluidSlot(this, 143, 17, 18, 50));
        });
    }

    /**
     * 按当前配方模式取配方：
     * recipeMode 由 LevelupBlast/LevelupSmoke 升级决定（First-wins 语义，见 setRecipeMode）：
     * SMELTING（默认）/ BLASTING / SMOKING。
     */
    @SuppressWarnings("unchecked")
    private Optional<RecipeHolder<AbstractCookingRecipe>> getCurrentRecipe() {
        if (level == null) return Optional.empty();
        ItemStack input = itemHandler.getStackInSlot(0);
        if (input.isEmpty()) return Optional.empty();
        var recipeInput = new SingleRecipeInput(input);

        return switch (recipeMode) {
            case IUpgradableMachine.RECIPE_MODE_BLASTING -> (Optional) level.getRecipeManager()
                    .getRecipeFor(RecipeType.BLASTING, recipeInput, level);
            case IUpgradableMachine.RECIPE_MODE_SMOKING -> (Optional) level.getRecipeManager()
                    .getRecipeFor(RecipeType.SMOKING, recipeInput, level);
            default -> (Optional) level.getRecipeManager()
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

        ResourceLocation newId = recipeOpt.get().id();
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
            var registry = level.registryAccess();
            ItemStack result = recipeOpt.get().value().getResultItem(registry);
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

            // Knowledge 门控 XP 产出：安装时 B 受 XP 罐剩余空间约束（罐恒存在，仅生产被门控）
            int B_byXpFluid = Integer.MAX_VALUE;
            if (hasUpgrade(LevelupKnow.class)) {
                AbstractCookingRecipe recipe = recipeOpt.get().value();
                int xpPerUnit = calculateXpFluidPerUnit(recipe.getCookingTime());
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

        var registry = level != null ? level.registryAccess() : null;
        if (registry == null) return false;

        int B = getLockedBatchSize();
        ItemStack result = recipeOpt.get().value().getResultItem(registry);
        ItemStack output = itemHandler.getStackInSlot(1);

        if (output.isEmpty()) return false;
        if (!ItemStack.isSameItem(output, result)) return true;
        if (output.getCount() + result.getCount() * B > output.getMaxStackSize()) return true;

        // Knowledge 门控 XP 罐空间检查（仅生产路径）
        if (hasUpgrade(LevelupKnow.class)) {
            AbstractCookingRecipe recipe = recipeOpt.get().value();
            int xpPerUnit = calculateXpFluidPerUnit(recipe.getCookingTime());
            if (xpPerUnit > 0 && !tanks.isEmpty()) {
                FluidStack xpFluid = xpFluidStack(xpPerUnit);
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

        ResourceLocation currentId = recipeOpt.get().id();
        if (!Objects.equals(lastRecipeId, currentId)) {
            clearLockedBatch();
            return;
        }

        int B = getLockedBatchSize();
        AbstractCookingRecipe recipe = recipeOpt.get().value();
        var registry = level.registryAccess();
        ItemStack result = recipe.getResultItem(registry);
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

        // Knowledge 门控 XP 产出：执行前预检 XP 罐空间（不足则本周期停滞不消耗输入）
        boolean hasKnowledge = hasUpgrade(LevelupKnow.class);
        int xpFluidTotal = 0;
        if (hasKnowledge) {
            int xpPerUnit = calculateXpFluidPerUnit(recipe.getCookingTime());
            if (xpPerUnit > 0) {
                long totalXpLong = (long) xpPerUnit * B;
                if (totalXpLong > Integer.MAX_VALUE) {
                    clearLockedBatch();
                    return;
                }
                xpFluidTotal = (int) totalXpLong;
                if (!tanks.isEmpty()) {
                    FluidStack totalXpFluid = xpFluidStack(xpFluidTotal);
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

        // ── 原子执行 + 回滚 ──
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
                tanks.get(0).fill(xpFluidStack(xpFluidTotal), IFluidHandler.FluidAction.EXECUTE);
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

    /** 每单位产物产出的 XP mB：max(1, round(cookingTime / 10))。 */
    private static int calculateXpFluidPerUnit(int cookingTime) {
        if (cookingTime <= 0) return 0;
        return Math.max(1, (int) Math.round((double) cookingTime / XP_FLUID_TICKS_PER_MB));
    }

    private static FluidStack xpFluidStack(int amount) {
        return new FluidStack((net.minecraft.world.level.material.Fluid) TENFluids.LIQUID_XP.getSource(), amount);
    }

    @Override
    public boolean hasFaceCapabilityFluid(net.minecraft.core.Direction side) {
        // 罐与 capability 恒存在——Knowledge 仅门控生产逻辑。
        // 保证卸下 Knowledge 后残留 XP 流体仍可取出。
        return true;
    }
}
