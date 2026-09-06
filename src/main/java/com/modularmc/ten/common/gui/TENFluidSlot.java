package com.modularmc.ten.common.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

import com.lowdragmc.lowdraglib2.gui.ui.elements.FluidSlot;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * TEN fluid slot that hides temperature and gas/liquid state from the built-in LDLib2 tooltip.
 */
public class TENFluidSlot extends FluidSlot {

    private static final Set<String> HIDDEN_TOOLTIP_KEYS = Set.of(
            "ldlib.fluid.temperature",
            "ldlib.fluid.state_gas",
            "ldlib.fluid.state_liquid");

    @Override
    public List<Component> getFullTooltipTexts() {
        List<Component> original = super.getFullTooltipTexts();
        List<Component> filtered = new ArrayList<>(original.size());
        for (Component component : original) {
            if (!isHiddenTooltip(component)) {
                filtered.add(component);
            }
        }
        return filtered;
    }

    private static boolean isHiddenTooltip(Component component) {
        if (component.getContents() instanceof TranslatableContents contents) {
            return HIDDEN_TOOLTIP_KEYS.contains(contents.getKey());
        }
        return false;
    }
}
