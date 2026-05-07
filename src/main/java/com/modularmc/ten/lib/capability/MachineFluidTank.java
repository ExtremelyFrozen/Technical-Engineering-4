package com.modularmc.ten.lib.capability;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.function.Predicate;

public class MachineFluidTank extends FluidTank {

    public MachineFluidTank(int capacity) {
        super(capacity);
    }

    public MachineFluidTank(int capacity, Predicate<FluidStack> validator) {
        super(capacity, validator);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return super.drain(maxDrain, action);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return super.fill(resource, action);
    }
}
