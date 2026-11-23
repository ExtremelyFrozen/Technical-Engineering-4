package com.modularmc.ten.common.blockentity.machine;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.modularmc.ten.TEN;
import com.modularmc.ten.api.blockentity.RecipeMachineBlockEntity;
import com.modularmc.ten.api.blockentity.SlotInfo;
import com.modularmc.ten.api.capability.MachineFluidTank;
import com.modularmc.ten.api.option.IMachineType;
import com.modularmc.ten.api.option.IngredientType;
import com.modularmc.ten.api.option.MachineType;
import com.modularmc.ten.api.recipe.FormsCombinedIngredient;
import com.modularmc.ten.api.recipe.FormsCombinedRecipe;
import com.modularmc.ten.common.data.TENDataComponents;
import com.modularmc.ten.common.data.TENRecipeTypes;
import com.modularmc.ten.common.gui.TENMachineBlockUIFactory;
import com.modularmc.ten.utils.SafeOperationHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.List;
import java.util.function.DoubleSupplier;

public class DistillatorBlockEntity extends RecipeMachineBlockEntity {

    public DistillatorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, new SlotInfo(0, 0, 0, 1, 0, 0, 1, 2));
        setCapacity(kFE(20));
        setEfficiency(15);
        tanks.add(new MachineFluidTank(4000));
        tanks.add(new MachineFluidTank(4000));
        tanks.add(new MachineFluidTank(4000));
    }

    @Override
    public IMachineType machineType() {
        return MachineType.DISTILLATOR;
    }

    @Override
    public int inventorySize() {
        return 2;
    }

    @Override
    public IngredientType slotType(int slot) {
        return IngredientType.OUTPUT;
    }

    @Override
    public IngredientType tankType(int tank) {
        return tank == 0 ? IngredientType.INPUT : IngredientType.OUTPUT;
    }

    @Override
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        return buildMachineUI(holder, TENMachineBlockUIFactory.backgroundFor(machineType()), root -> {
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 0, 112, 25));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 1, 112, 43));
        }, root -> {
            root.addChild(TENMachineBlockUIFactory.energyGaugeModular(this, 8, 18, true));
            root.addChild(TENMachineBlockUIFactory.progressGaugeModular(this, 85, 33, true));
            root.addChild(TENMachineBlockUIFactory.fluidGaugeModular(this, 31, 17, 18, 50, 0));
            root.addChild(TENMachineBlockUIFactory.fluidGaugeModular(this, 133, 17, 18, 50, 1));
            root.addChild(TENMachineBlockUIFactory.fluidGaugeModular(this, 153, 17, 18, 50, 2));
        });
    }

    @Override
    protected boolean canFitOutputsForBatch(int B) {
        if (currentRecipe == null) return true;
        var itemOutputs = currentRecipe.allOutputItems();
        boolean allFit = true;

        // Item output capacity check using simulated slot state
        if (!itemOutputs.isEmpty() && itemHandler != null) {
            int outSlotCount = slotInfo.o2() - slotInfo.o1() + 1;
            ItemStack[] simulated = new ItemStack[outSlotCount];
            for (int i = 0; i < outSlotCount; i++) {
                ItemStack existing = itemHandler.getStackInSlot(slotInfo.o1() + i);
                simulated[i] = existing.isEmpty() ? ItemStack.EMPTY : existing.copy();
            }

            // Process each output ingredient
            for (var ing : itemOutputs) {
                if (ing.isAllowAll()) continue;
                if (ing.chance() <= 0) continue;
                ItemStack sym = ing.symbolItem();
                if (sym.isEmpty()) continue;
                Item item = sym.getItem();

                // Worst-case per batch unit
                int perUnitWorst = Math.multiplyExact(ing.amountOrCount(), ing.rolls());
                if (perUnitWorst <= 0) continue;
                long totalWorst = (long) perUnitWorst * B;
                if (totalWorst > Integer.MAX_VALUE) return false;

                int remaining = (int) totalWorst;
                // Use candidate B explicitly for slot limit — not global lockedB
                int limitPerSlot = computeItemSlotLimit(item, B);

                for (int i = 0; i < outSlotCount && remaining > 0; i++) {
                    ItemStack slot = simulated[i];
                    if (!slot.isEmpty() && slot.getItem() != item) continue;

                    int existingCount = slot.isEmpty() ? 0 : slot.getCount();
                    int slotLimit = Math.max(limitPerSlot, existingCount);
                    int space = slotLimit - existingCount;
                    int toAdd = Math.min(space, remaining);

                    if (toAdd > 0) {
                        if (slot.isEmpty()) {
                            simulated[i] = new ItemStack(item, toAdd);
                        } else {
                            slot.grow(toAdd);
                        }
                        remaining -= toAdd;
                    }
                }

                if (remaining > 0) {
                    allFit = false;
                    break;
                }
            }
        }

        if (!allFit) return false;

        TENDataComponents.FluidQuality quality = readInputQuality();
        int fluidIdx = 0;
        // Fluid output capacity check
        for (var ing : currentRecipe.allOutputFluids()) {
            if (ing.isAllowAll()) continue;
            int amountPerBatch = ing.amountOrCount();
            if (amountPerBatch <= 0) continue;

            long totalNeeded = (long) (amountPerBatch * B * quality.fluidMul(fluidIdx++));
            if (totalNeeded > Integer.MAX_VALUE) return false;

            FluidStack needed = ing.symbolFluid();
            if (needed.isEmpty()) continue;
            needed.setAmount((int) totalNeeded);

            boolean fits = false;
            for (int i = slotInfo.fo1(); i <= slotInfo.fo2() && i < tanks.size(); i++) {
                if (tankType(i).canOut()) {
                    // Check if this one tank can hold the full amount
                    if (tanks.get(i).fill(needed, IFluidHandler.FluidAction.SIMULATE) >= needed.getAmount()) {
                        fits = true;
                        break;
                    }
                }
            }
            if (!fits) return false;
        }

        return true;
    }

    @Override
    protected boolean collectBatchOutputs(List<ItemStack> itemCollector, List<FluidStack> fluidCollector, int B) {
        if (currentRecipe == null) return false;

        TENDataComponents.FluidQuality quality = readInputQuality();
        RandomSource random = SafeOperationHelper.getRandom();
        DoubleSupplier rng = random::nextDouble;

        int itemIdx = 0;
        int fluidIdx = 0;
        try {
            for (var ing : currentRecipe.output()) {
                if (ing.isAllowAll()) continue;

                if ("item".equals(ing.form())) {
                    if (!collectItem(ing, B, quality.itemMul(itemIdx++), rng, itemCollector)) {
                        return false;
                    }
                } else if ("fluid".equals(ing.form())) {
                    if (!collectFluid(ing, B, quality.fluidMul(fluidIdx++), rng, fluidCollector)) {
                        return false;
                    }
                }
            }
            return true;
        } catch (Exception e) {
            TEN.LOGGER.error("Distillator collectBatchOutputs failed: recipe={}", currentRecipe != null ? currentRecipe.getId() : "null", e);
            return false;
        }
    }

    /** 输入罐（tank 0）上的倍率组件；缺省 DEFAULT。 */
    private TENDataComponents.FluidQuality readInputQuality() {
        FluidStack input = tanks.getFirst().getFluid();
        if (input.isEmpty()) return TENDataComponents.FluidQuality.DEFAULT;
        return input.getOrDefault(TENDataComponents.QUALITY.get(), TENDataComponents.FluidQuality.DEFAULT);
    }

    private boolean collectItem(FormsCombinedIngredient ing, int B, float mult, DoubleSupplier rng, List<ItemStack> collector) {
        if (mult <= 0f) return true;

        if (ing.chance() >= 1.0d - 1e-12) {
            ItemStack base = ing.symbolItem();
            if (base.isEmpty()) return true;
            long total = Math.round((double) base.getCount() * B * mult);
            if (total > 64L * Math.max(1, B)) return false;
            if (total > 0) collector.add(new ItemStack(base.getItem(), (int) total));
            return true;
        }

        float gate = Math.min(1f, mult);
        for (int b = 0; b < B; b++) {
            if (gate < 1f && rng.getAsDouble() >= gate) continue;
            ItemStack result = ing.genItem(rng);
            if (result.isEmpty()) continue;
            if (result.getCount() > ABSOLUTE_MAX_STACK) return false;
            collector.add(result);
        }
        return true;
    }

    /** 流体产物同物品口径。 */
    private boolean collectFluid(FormsCombinedIngredient ing, int B, float mult, DoubleSupplier rng, List<FluidStack> collector) {
        if (mult <= 0f) return true;

        if (ing.chance() >= 1.0d - 1e-12) {
            FluidStack base = ing.symbolFluid();
            if (base.isEmpty()) return true;
            long total = Math.round((double) base.getAmount() * B * mult);
            if (total > Integer.MAX_VALUE) return false;
            if (total > 0) collector.add(new FluidStack(base.getFluid(), (int) total));
            return true;
        }

        float gate = Math.min(1f, mult);
        for (int b = 0; b < B; b++) {
            if (gate < 1f && rng.getAsDouble() >= gate) continue;
            FluidStack result = ing.genFluid(rng.getAsDouble());
            if (result.isEmpty()) continue;
            if (result.getAmount() < 0) return false;
            collector.add(result);
        }
        return true;
    }

    @Override
    public FormsCombinedRecipe findRecipe() {
        var recipes = level.getRecipeManager().getAllRecipesFor(TENRecipeTypes.DISTILLATOR_T.get());
        for (var holder : recipes) {
            var recipe = holder.value();
            if (recipe instanceof FormsCombinedRecipe r && r.matches(itemHandler, tanks, this::slotType, this::tankType)) {
                r.recipeType = TENRecipeTypes.DISTILLATOR_T.get();
                r.serializer = TENRecipeTypes.DISTILLATOR_S.get();
                return r.assignId(holder.id());
            }
        }
        return null;
    }
}
