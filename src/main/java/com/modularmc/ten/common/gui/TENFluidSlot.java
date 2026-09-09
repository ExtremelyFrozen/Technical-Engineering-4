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

    /**
     * 覆写渲染顺序（对齐本 mod 覆盖层需求）：底图（原生 drawBackground）→ 流体柱 → 覆盖层 → hover。
     * <p>
     * ldlib2 原生 {@code drawBackgroundAdditional} 顺序是 slotOverlay 先于 fluid：有流体时覆盖层
     * 被完全遮住，玻璃质感失效。本覆写将流体提到覆盖层之下——空槽时覆盖层叠槽、
     * 有流体时覆盖层叠在流体上（用户需求 2026-09）。
     * 复刻自 LDLib2 1.21 分支 FluidSlot#drawBackgroundAdditional（仅调换 overlay/fluid 顺序）。
     */
    @Override
    public void drawBackgroundAdditional(com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext guiContext) {
        var renderedFluid = getValue();
        var hovered = isHover() || isSelfOrChildHover();
        var contentX = getContentX();
        var contentY = getContentY();
        var contentWidth = getContentWidth();
        var contentHeight = getContentHeight();

        if (!renderedFluid.isEmpty()) {
            drawFluid(guiContext, renderedFluid, contentX, contentY, contentWidth, contentHeight);
        }
        // 覆盖层常驻（空槽叠槽、有流体叠流体）；hover 高亮最顶层
        drawSlotOverlay(guiContext, contentX, contentY, contentWidth, contentHeight);
        if (hovered) {
            drawHover(guiContext, contentX, contentY, contentWidth, contentHeight);
        }
    }

    private static boolean isHiddenTooltip(Component component) {
        if (component.getContents() instanceof TranslatableContents contents) {
            return HIDDEN_TOOLTIP_KEYS.contains(contents.getKey());
        }
        return false;
    }
}
