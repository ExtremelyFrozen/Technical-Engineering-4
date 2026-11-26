package com.modularmc.ten.common.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

import com.lowdragmc.lowdraglib2.gui.ui.elements.FluidSlot;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** 隐藏 LDLib2 内置流体 tooltip 中的温度与气液状态。 */
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
     * 渲染顺序：底图（原生 {@code drawBackground}）→ 流体柱 → 覆盖层 → hover。
     * <p>
     * 覆盖层必须位于流体之上：空槽时叠在槽底图上，有流体时叠在流体上；hover 高亮位于最顶层。
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
