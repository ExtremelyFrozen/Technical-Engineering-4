package com.modularmc.ten.api.capability;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

import lombok.Setter;

import java.util.function.BiPredicate;

@Setter
public class MachineItemHandler extends ItemStackHandler {

    private BiPredicate<Integer, ItemStack> validator;
    private Runnable changeListener = () -> {};

    public MachineItemHandler(int slots) {
        super(slots);
        this.validator = (slot, stack) -> true;
    }

    public MachineItemHandler(int slots, BiPredicate<Integer, ItemStack> validator) {
        super(slots);
        this.validator = validator;
    }

    public void setChangeListener(Runnable changeListener) {
        this.changeListener = changeListener != null ? changeListener : () -> {};
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return validator.test(slot, stack);
    }

    @Override
    protected void onContentsChanged(int slot) {
        super.onContentsChanged(slot);
        changeListener.run();
    }
}
