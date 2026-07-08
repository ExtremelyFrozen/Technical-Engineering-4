package com.modularmc.ten.api.capability;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.function.Predicate;

public class MachineFluidTank extends FluidTank {

    private Runnable changeListener = () -> {};

    public MachineFluidTank(int capacity) {
        super(capacity);
    }

    public MachineFluidTank(int capacity, Predicate<FluidStack> validator) {
        super(capacity, validator);
    }

    public void setChangeListener(Runnable changeListener) {
        this.changeListener = changeListener != null ? changeListener : () -> {};
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        FluidStack drained = super.drain(maxDrain, action);
        if (!drained.isEmpty() && action.execute()) {
            changeListener.run();
        }
        return drained;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        int filled = super.fill(resource, action);
        if (filled > 0 && action.execute()) {
            changeListener.run();
        }
        return filled;
    }
}
