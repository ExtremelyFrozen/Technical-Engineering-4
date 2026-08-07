package com.modularmc.ten.common.blockentity.machine;

import com.modularmc.ten.TENConstants;
import com.modularmc.ten.api.blockentity.ProcessingMachineBlockEntity;
import com.modularmc.ten.api.option.IngredientType;
import com.modularmc.ten.api.option.MachineType;
import com.modularmc.ten.common.data.TENFluids;
import com.modularmc.ten.common.gui.TENMachineBlockUIFactory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;

public class EncfluBlockEntity extends ProcessingMachineBlockEntity {

    public EncfluBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setCapacity(kFE(20));
        setEfficiency(100);
        tanks.add(new com.modularmc.ten.api.capability.MachineFluidTank(1000));
    }

    @Override
    public int machineType() {
        return MachineType.ENCHANTMENT_FLUSHER;
    }

    @Override
    public int inventorySize() {
        return 3;
    }

    @Override
    public IngredientType slotType(int slot) {
        if (slot == 1) {
            return IngredientType.INPUT;
        }
        if (slot == 0) {
            ItemStack stack = itemHandler.getStackInSlot(slot);
            if (!stack.isEmpty() && !stack.isEnchanted()) {
                return IngredientType.OUTPUT;
            }
            return IngredientType.INPUT;
        }
        if (slot == 2) {
            return IngredientType.OUTPUT;
        }
        return IngredientType.IGNORE;
    }

    @Override
    public boolean valid(int slot, ItemStack stack) {
        if (slot == 1) {
            return stack.isEnchantable() || stack.is(Items.BOOK);
        }
        if (slot == 0) {
            return stack.isEnchanted();
        }
        return false;
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
            // 2 输入竖排（39,14 / 39,50，块 14~68 中线 41 与能量条对齐），输出大槽
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 0, 39, 14));
            root.addChild(TENMachineBlockUIFactory.machineSlotModular(this, 1, 39, 50));
            root.addChild(TENMachineBlockUIFactory.machineSlotLarge(this, 2, 113, 28));
        }, root -> {
            root.addChild(TENMachineBlockUIFactory.energyGaugeModular(this, 8, 18, true));
            // mini 装饰（静态整件 8x54）：紧贴输入槽右缘，标示上下输入槽流向（目标在下），不承载进度
            root.addChild(TENMachineBlockUIFactory.verticalProgressMini(39, 14, 18, TENConstants.PROGRESS_ARROW_MINI_ENCFLU_BG, false));
            // 进度箭头（modular smelter 素材 22x16）：encflu 映射 PROGRESS_ARROW_SMELTER 素材族
            root.addChild(TENMachineBlockUIFactory.progressGaugeModular(this, 76, 33, true));
        });
    }

    @Override
    public int baseTickTime() {
        return 800;
    }

    @Override
    public boolean conditionStart() {
        ItemStack tool = itemHandler.getStackInSlot(0);
        ItemStack target = itemHandler.getStackInSlot(1);
        ItemStack output = itemHandler.getStackInSlot(2);

        // Basic input validation — if inputs are invalid, clear lock and stop
        if (!tool.isEnchanted() || !(target.isEnchantable() || target.is(Items.BOOK)) || !output.isEmpty()) {
            clearLockedBatch();
            return false;
        }

        // Only compute and lock B + maxProgress when no lock exists yet
        if (!hasLockedBatch()) {
            // Lock maxProgress with durationMultiplier captured at operation start
            maxProgress = Math.max(1, (int) Math.ceil(baseTickTime() * durationMultiplier));
            lockMaxProgressForNewOperation(maxProgress);

            // ── P1-T3c: Compute and lock B_actual ──
            int B_theory = 1 + batch;

            // B_byTool: tool slot can only support 1 unit (non-stackable, single slot)
            int B_byTool = 1;

            // B_byOutput: output slot must be empty
            int B_byOutput = output.isEmpty() ? B_theory : 0;

            // B_byTank: floor(availTankCapacity / xpPerGroup)
            ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(tool);
            int xpPerGroup = enchantments.isEmpty() ? 25 : Math.max(1, enchantments.size() * 25);
            int B_byTank = 0;
            if (!tanks.isEmpty()) {
                int availTank = tanks.get(0).getCapacity() - tanks.get(0).getFluidAmount();
                B_byTank = xpPerGroup > 0 ? availTank / xpPerGroup : 0;
            }

            // B_byEnergy: how many ticks can current energy sustain?
            int baseFe = Math.max(1, getActualEfficiency());
            int B_byEnergy = energyStorage != null ? energyStorage.getEnergyStored() / baseFe : 0;

            // Lock B_actual — validateAndLockB clears lock on failure
            // B_byTool=1 ensures actual B=1 (tool is non-stackable, no batch dup)
            if (!validateAndLockB(B_theory, B_byTool, Integer.MAX_VALUE, Math.min(B_byOutput, B_byTank), B_byEnergy)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean cooking() {
        // Check output slot and tank capacity before allowing progress
        ItemStack output = itemHandler.getStackInSlot(2);
        if (!output.isEmpty()) return true; // output slot occupied → block

        ItemStack tool = itemHandler.getStackInSlot(0);
        if (!tool.isEnchanted()) return true; // tool depleted → block

        // Check XP tank has space for at least one group
        if (!tanks.isEmpty()) {
            ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(tool);
            int xpPerGroup = enchantments.isEmpty() ? 25 : Math.max(1, enchantments.size() * 25);
            FluidStack xpFluid = new FluidStack(TENFluids.LIQUID_XP_SOURCE.get(), xpPerGroup);
            int filled = tanks.get(0).fill(xpFluid, IFluidHandler.FluidAction.SIMULATE);
            if (filled < xpPerGroup) return true; // tank full → block
        }

        return false;
    }

    @Override
    public void onCookFinish() {
        // ════════════════════════════════════════════════════════════
        // Phase 0: Determine batch size
        // ════════════════════════════════════════════════════════════
        int B = getLockedBatchSize();

        // ════════════════════════════════════════════════════════════
        // Phase 1: Read current state from handlers (copies)
        // ════════════════════════════════════════════════════════════
        ItemStack tool = itemHandler.getStackInSlot(0).copy();
        ItemStack target = itemHandler.getStackInSlot(1).copy();
        ItemStack currentOutput = itemHandler.getStackInSlot(2).copy();

        if (tool.isEmpty() || target.isEmpty()) {
            return;
        }

        ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(tool);
        if (enchantments.isEmpty()) {
            return;
        }

        // ════════════════════════════════════════════════════════════
        // Phase 2: Re-validate output slot is empty
        // ════════════════════════════════════════════════════════════
        if (!currentOutput.isEmpty()) {
            return;
        }

        // ════════════════════════════════════════════════════════════
        // Phase 3: Build outputs (side-effect-free planning)
        // ════════════════════════════════════════════════════════════
        ItemStack output;
        if (target.is(Items.BOOK)) {
            output = Items.ENCHANTED_BOOK.getDefaultInstance();
        } else {
            output = target.copyWithCount(1);
        }
        EnchantmentHelper.setEnchantments(output, enchantments);

        int xpAmount = Math.max(1, enchantments.size() * 25);
        FluidStack xpFluid = new FluidStack(TENFluids.LIQUID_XP_SOURCE.get(), xpAmount);

        // Build stripped tool (enchantments removed, item stays)
        ItemStack strippedTool = tool.copy();
        strippedTool.remove(DataComponents.ENCHANTMENTS);
        strippedTool.remove(DataComponents.STORED_ENCHANTMENTS);

        // Build reduced target (shrunk by B)
        ItemStack targetAfter = target.copy();
        targetAfter.shrink(B); // consume B targets

        // ════════════════════════════════════════════════════════════
        // Phase 4: Re-validate fluid tank has space (SIMULATE) for B×XP
        // ════════════════════════════════════════════════════════════
        if (!tanks.isEmpty()) {
            int totalXp = xpAmount * B;
            FluidStack totalXpFluid = new FluidStack(TENFluids.LIQUID_XP_SOURCE.get(), totalXp);
            int filled = tanks.get(0).fill(totalXpFluid, IFluidHandler.FluidAction.SIMULATE);
            if (filled < totalXp) {
                return;
            }
        }

        // ════════════════════════════════════════════════════════════
        // Phase 5: Save snapshots for rollback
        // ════════════════════════════════════════════════════════════
        ItemStack slot0Snapshot = itemHandler.getStackInSlot(0).copy();
        ItemStack slot1Snapshot = itemHandler.getStackInSlot(1).copy();
        ItemStack slot2Snapshot = itemHandler.getStackInSlot(2).copy();
        FluidStack tankSnapshot = tanks.isEmpty() ? FluidStack.EMPTY : tanks.get(0).getFluid().copy();

        // Ensure target doesn't go below 0
        if (targetAfter.getCount() < 0) {
            targetAfter = ItemStack.EMPTY;
        }

        try {
            // ════════════════════════════════════════════════════════
            // Phase 6: Execute consumption FIRST (consume before output)
            // ════════════════════════════════════════════════════════
            // Strip tool enchantments once (works for any B — tool only stripped once)
            itemHandler.setStackInSlot(0, strippedTool);
            // Consume B targets
            itemHandler.setStackInSlot(1, targetAfter);

            // ════════════════════════════════════════════════════════
            // Phase 7: Execute output AFTER consumption
            // ════════════════════════════════════════════════════════
            // Place enchanted output in slot 2 (one output for the batch)
            ItemStack batchOutput = output.copyWithCount(B);
            itemHandler.setStackInSlot(2, batchOutput);

            // Fill XP fluid tank (B × XP)
            if (!tanks.isEmpty()) {
                FluidStack totalXpFluid = new FluidStack(TENFluids.LIQUID_XP_SOURCE.get(), xpAmount * B);
                tanks.get(0).fill(totalXpFluid, IFluidHandler.FluidAction.EXECUTE);
            }

            // Clear lock after successful completion
            clearLockedBatch();
        } catch (Exception e) {
            // ── Rollback on any unexpected failure ──
            itemHandler.setStackInSlot(0, slot0Snapshot);
            itemHandler.setStackInSlot(1, slot1Snapshot);
            itemHandler.setStackInSlot(2, slot2Snapshot);
            if (!tanks.isEmpty()) {
                tanks.get(0).drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
                if (!tankSnapshot.isEmpty()) {
                    tanks.get(0).fill(tankSnapshot, IFluidHandler.FluidAction.EXECUTE);
                }
            }
            throw new RuntimeException("Encflu onCookFinish failed and rolled back", e);
        }
    }
}
