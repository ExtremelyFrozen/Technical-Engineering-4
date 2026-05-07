package com.modularmc.ten.api.capability;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

import lombok.Setter;

import java.util.function.BiPredicate;

@Setter
public class MachineItemHandler extends ItemStackHandler {

    private BiPredicate<Integer, ItemStack> validator;

    public MachineItemHandler(int slots) {
        super(slots);
        this.validator = (slot, stack) -> true;
    }

    public MachineItemHandler(int slots, BiPredicate<Integer, ItemStack> validator) {
        super(slots);
        this.validator = validator;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return validator.test(slot, stack);
    }
}
