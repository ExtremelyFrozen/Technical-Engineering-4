package com.modularmc.ten.common.blockentity.machine;

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
            root.addChild(TENMachineBlockUIFactory.machineSlot(this, 0, 43, 15));
            root.addChild(TENMachineBlockUIFactory.machineSlot(this, 1, 43, 51));
            root.addChild(TENMachineBlockUIFactory.machineSlot(this, 2, 115, 34));
        }, root -> {
            root.addChild(TENMachineBlockUIFactory.energyGauge(this, 9, 18, 14, 46, 0, 0, true));
            root.addChild(TENMachineBlockUIFactory.fuelGauge(this, 45, 36, 13, 13, 14, 0, false));
            root.addChild(TENMachineBlockUIFactory.progressGauge(this, 76, 35, 22, 16, 27, 127, true));
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
        return tool.isEnchanted() && (target.isEnchantable() || target.is(Items.BOOK)) && output.isEmpty();
    }

    @Override
    public boolean cooking() {
        return false;
    }

    @Override
    public void onCookFinish() {
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
            // Output slot occupied — nothing to do
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

        // Build reduced target (shrunk by 1)
        ItemStack targetAfter = target.copy();
        targetAfter.shrink(1);

        // ════════════════════════════════════════════════════════════
        // Phase 4: Re-validate fluid tank has space (SIMULATE)
        // ════════════════════════════════════════════════════════════
        if (!tanks.isEmpty()) {
            int filled = tanks.get(0).fill(xpFluid, IFluidHandler.FluidAction.SIMULATE);
            if (filled < xpAmount) {
                // Tank cannot accept full XP amount — abort
                return;
            }
        }

        // ════════════════════════════════════════════════════════════
        // Phase 5: Save snapshots for rollback
        // ════════════════════════════════════════════════════════════
        ItemStack slot0Snapshot = itemHandler.getStackInSlot(0).copy();
        ItemStack slot1Snapshot = itemHandler.getStackInSlot(1).copy();
        ItemStack slot2Snapshot = itemHandler.getStackInSlot(2).copy();
        // Fluid tank snapshot: save current fluid stack
        FluidStack tankSnapshot = tanks.isEmpty() ? FluidStack.EMPTY : tanks.get(0).getFluid().copy();

        try {
            // ════════════════════════════════════════════════════════
            // Phase 6: Execute consumption FIRST (consume before output)
            // ════════════════════════════════════════════════════════
            // Submit stripped tool back to slot 0 (consumption: enchantments removed)
            itemHandler.setStackInSlot(0, strippedTool);
            // Submit reduced target back to slot 1 (consumption: shrink by 1)
            itemHandler.setStackInSlot(1, targetAfter);

            // ════════════════════════════════════════════════════════
            // Phase 7: Execute output AFTER consumption
            // ════════════════════════════════════════════════════════
            // Place enchanted output in slot 2
            itemHandler.setStackInSlot(2, output);

            // Fill XP fluid tank
            if (!tanks.isEmpty()) {
                tanks.get(0).fill(xpFluid, IFluidHandler.FluidAction.EXECUTE);
            }
        } catch (Exception e) {
            // ── Rollback on any unexpected failure ──
            itemHandler.setStackInSlot(0, slot0Snapshot);
            itemHandler.setStackInSlot(1, slot1Snapshot);
            itemHandler.setStackInSlot(2, slot2Snapshot);
            if (!tanks.isEmpty()) {
                // Restore tank to snapshot: drain all, then fill back
                tanks.get(0).drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
                if (!tankSnapshot.isEmpty()) {
                    tanks.get(0).fill(tankSnapshot, IFluidHandler.FluidAction.EXECUTE);
                }
            }
            // Fail-fast: log and rethrow
            throw new RuntimeException("Encflu onCookFinish failed and rolled back", e);
        }
    }
}
