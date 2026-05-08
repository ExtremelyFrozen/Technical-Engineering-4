package com.modularmc.ten.client.gui.element;

import com.modularmc.ten.client.gui.CmContainerMachine;
import com.modularmc.ten.utils.ComponentHelper;
import com.modularmc.ten.utils.DisplayHelper;
import com.modularmc.ten.utils.RenderHelper;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

public class ElementFluid extends ElementBase {

    double percent;
    boolean displayValue;
    int tankId;
    FluidStack stack;
    int value, maxValue;

    public ElementFluid(int x, int y, int w, int h, int xOff, int yOff, ResourceLocation rl, int id) {
        super(x, y, w, h, xOff, yOff, rl);
        tankId = id;
    }

    public ElementFluid(int x, int y, int w, int h, int xOff, int yOff, ResourceLocation rl, int id, boolean dv) {
        super(x, y, w, h, xOff, yOff, rl);
        tankId = id;
        displayValue = dv;
    }

    public void update(int fluidId, int amount, int capacity) {
        Fluid fluid = BuiltInRegistries.FLUID.byId(fluidId);
        stack = new FluidStack(fluid, amount);
        value = amount;
        maxValue = capacity;
        percent = maxValue > 0 ? (double) value / maxValue : 0;
    }

    public void update(FluidStack s, int max) {
        stack = s.copy();
        value = s.getAmount();
        maxValue = max;
        percent = max > 0 ? (double) value / max : 0;
    }

    public void update(CmContainerMachine container) {
        if (container == null || container.machine == null || tankId < 0 || tankId >= container.machine.tanks.size()) {
            stack = FluidStack.EMPTY;
            value = 0;
            maxValue = 0;
            percent = 0;
            return;
        }
        var tank = container.machine.tanks.get(tankId);
        update(tank.getFluid(), tank.getCapacity());
    }

    @Override
    public void draw(GuiGraphics guiGraphics) {
        int h = (int) ((height - 2) * (1 - percent));
        RenderHelper.render(guiGraphics, x, y, width, height, textureW, textureH, xOff, yOff, resourceLocation);
        if (stack != null && !stack.isEmpty()) {
            RenderHelper.drawFluidTank(guiGraphics, stack.getFluid(), x + 1, y + 1 + h, width - 2, height - h - 2);
        }
    }

    @Override
    public void addToolTip(List<Component> tooltips) {
        if (stack != null && !stack.isEmpty()) {
            tooltips.add(stack.getHoverName());
        }
        if (!displayValue) {
            tooltips.add(ComponentHelper.make((int) (percent * 100) + "%"));
        } else {
            tooltips.add(DisplayHelper.joinmB(value, maxValue));
        }
    }

    public void setValue(int v, int mv) {
        value = v;
        maxValue = mv;
    }

    public void setPer(double per) {
        percent = per;
    }
}
