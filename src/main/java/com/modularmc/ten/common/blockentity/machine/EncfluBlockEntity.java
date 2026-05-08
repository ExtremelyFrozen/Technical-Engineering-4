package com.modularmc.ten.common.blockentity.machine;

import com.modularmc.ten.api.blockentity.ProcessingMachineBlockEntity;
import com.modularmc.ten.api.option.IngredientType;
import com.modularmc.ten.api.option.MachineType;
import com.modularmc.ten.common.data.TENFluids;

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
        ItemStack tool = itemHandler.getStackInSlot(0);
        ItemStack target = itemHandler.getStackInSlot(1);
        if (tool.isEmpty() || target.isEmpty()) {
            return;
        }

        ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(tool);
        if (enchantments.isEmpty()) {
            return;
        }

        ItemStack output;
        if (target.is(Items.BOOK)) {
            output = Items.ENCHANTED_BOOK.getDefaultInstance();
        } else {
            output = target.copyWithCount(1);
        }
        EnchantmentHelper.setEnchantments(output, enchantments);
        itemHandler.setStackInSlot(2, output);

        target.shrink(1);
        tool.remove(DataComponents.ENCHANTMENTS);
        tool.remove(DataComponents.STORED_ENCHANTMENTS);

        if (!tanks.isEmpty()) {
            int amount = Math.max(1, enchantments.size() * 25);
            tanks.get(0).fill(
                    new FluidStack((net.minecraft.world.level.material.Fluid) TENFluids.LIQUID_XP.getSource(), amount),
                    IFluidHandler.FluidAction.EXECUTE);
        }
    }
}
