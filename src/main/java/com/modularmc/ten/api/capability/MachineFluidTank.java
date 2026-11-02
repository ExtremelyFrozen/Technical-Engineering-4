package com.modularmc.ten.api.capability;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.function.Predicate;

/**
 * 机器流体罐能力：记录构造初始容量供批处理 B 缩放恢复基准，
 * 容量运行时可调且不缩存量（stored 超出新容量保持至耗）。
 */
public class MachineFluidTank extends FluidTank {

    private Runnable changeListener = () -> {};

    /** 构造时记录的初始容量，用于批处理（B）批量缩放时恢复基准。 */
    private final int initialCapacity;

    public MachineFluidTank(int capacity) {
        super(capacity);
        this.initialCapacity = capacity;
    }

    public MachineFluidTank(int capacity, Predicate<FluidStack> validator) {
        super(capacity, validator);
        this.initialCapacity = capacity;
    }

    public int getInitialCapacity() {
        return initialCapacity;
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
