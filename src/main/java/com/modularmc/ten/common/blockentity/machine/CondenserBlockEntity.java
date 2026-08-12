package com.modularmc.ten.common.blockentity.machine;

import com.modularmc.ten.api.blockentity.ProcessingMachineBlockEntity;
import com.modularmc.ten.api.option.IngredientType;
import com.modularmc.ten.api.option.MachineType;
import com.modularmc.ten.common.data.TENFluids;
import com.modularmc.ten.common.gui.TENMachineBlockUIFactory;
import com.modularmc.ten.utils.TagHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;

public class CondenserBlockEntity extends ProcessingMachineBlockEntity {

    /**
     * Counter tracking successful processing ticks for periodic catalyst
     * consumption. Every 20 processing ticks, one catalyst is consumed.
     * Initialized to 20 so the first processing tick immediately consumes
     * (matching the old {@code getAliveTime() % 20 == 0} behavior where
     * tick 0 triggered consumption).
     */
    private int catalystTickCounter = 20;

    public CondenserBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setCapacity(kFE(20));
        setEfficiency(30);
        tanks.add(new com.modularmc.ten.api.capability.MachineFluidTank(1000));
    }

    @Override
    public int machineType() {
        return MachineType.MATTER_CONDENSER;
    }

    @Override
    public int inventorySize() {
        return 1;
    }

    @Override
    public IngredientType slotType(int slot) {
        return IngredientType.INPUT;
    }

    @Override
    public boolean valid(int slot, ItemStack stack) {
        return TagHelper.containsItem(stack.getItem(), TagHelper.keyItem("kenergyengineering:catalyst"));
    }

    @Override
    public IngredientType tankType(int tank) {
        return IngredientType.OUTPUT;
    }

    @Override
    public boolean valid(int slot, FluidStack stack) {
        return true;
    }

    @Override
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        return buildMachineUI(holder, TENMachineBlockUIFactory.backgroundFor(machineType()), root -> {
            // 输入槽 (79,32)：中心 x=88 与宽进度条竖中线 (48+40) 对齐（翻新时 79→39 左移导致偏离，恢复）；
            // y=32 中心 41 符合主区中线，与流体槽 (143,17) 中心 42 协调
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 0, 79, 32));
        }, root -> {
            root.addChild(TENMachineBlockUIFactory.energyGaugeModular(this, 8, 18, true));
            root.addChild(TENMachineBlockUIFactory.progressGaugeWide(this, 48, 57, true));
            root.addChild(TENMachineBlockUIFactory.fluidGaugeModular(this, 143, 17, 18, 50, 0));
        });
    }

    @Override
    public int baseTickTime() {
        return 1000;
    }

    @Override
    public boolean conditionStart() {
        // ── P1-T3b: Compute and lock B_actual ──
        int B_theory = 1 + batch;

        // B_byInput: catalyst must be present for any processing
        boolean hasCatalyst = !itemHandler.getStackInSlot(0).isEmpty();

        // If catalyst is gone and we had a lock, clear it
        if (!hasCatalyst) {
            clearLockedBatch();
            return false;
        }

        // Only compute and lock B when no lock exists yet
        if (!hasLockedBatch()) {
            // Lock maxProgress with durationMultiplier captured at operation start
            maxProgress = Math.max(1, (int) Math.ceil(baseTickTime() * durationMultiplier));
            lockMaxProgressForNewOperation(maxProgress);

            int B_byInput = hasCatalyst ? B_theory : 0;

            // B_byTank: floor(availTankCapacity / 5mB)
            int B_byTank = 0;
            if (!tanks.isEmpty()) {
                int availTank = tanks.get(0).getCapacity() - tanks.get(0).getFluidAmount();
                B_byTank = availTank / 5;
            }

            // B_byEnergy: how many ticks can current energy sustain?
            int baseFe = Math.max(1, getActualEfficiency());
            int B_byEnergy = energyStorage != null ? energyStorage.getEnergyStored() / baseFe : 0;

            // Lock B_actual — validateAndLockB clears lock on failure
            if (!validateAndLockB(B_theory, B_byInput, Integer.MAX_VALUE, B_byTank, B_byEnergy)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean cooking() {
        // Pure capacity predicate: only check if output tank has space for B×5mB.
        // No progress read/write, no catalyst consumption, no side effects.
        // Progress is preserved when output is full (P1 target semantics).
        int B = getLockedBatchSize();
        FluidStack produced = new FluidStack(TENFluids.LIQUID_BIZARRERIE_SOURCE.get(), 5 * B);
        if (tanks.isEmpty() || tanks.get(0).fill(produced, IFluidHandler.FluidAction.SIMULATE) < produced.getAmount()) {
            return true; // tank full → block processing
        }
        return false;
    }

    @Override
    protected void onProcessTick() {
        // Catalyst consumption: 1 unit every 20 successful processing ticks.
        // Only consumed when this tick actually processes (energy consumed,
        // progress advanced). Not consumed on stalled/blocked ticks.
        // Single catalyst per cycle (not multiplied by B), matching old semantics.
        catalystTickCounter++;
        if (catalystTickCounter >= 20) {
            catalystTickCounter = 0;
            ItemStack catalyst = itemHandler.getStackInSlot(0);
            if (!catalyst.isEmpty()) {
                catalyst.shrink(1);
            }
        }
    }

    @Override
    public void onCookFinish() {
        if (!tanks.isEmpty()) {
            int B = getLockedBatchSize();
            FluidStack produced = new FluidStack(TENFluids.LIQUID_BIZARRERIE_SOURCE.get(), 5 * B);
            int filled = tanks.get(0).fill(produced, IFluidHandler.FluidAction.SIMULATE);
            if (filled >= produced.getAmount()) {
                tanks.get(0).fill(produced, IFluidHandler.FluidAction.EXECUTE);
            }
        }
        clearLockedBatch();
    }
}
