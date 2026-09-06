package com.modularmc.ten.api.client.components;

import com.modularmc.ten.api.client.gui.EnhancedGuiGraphics;

import java.util.ArrayList;
import java.util.List;

public class PanelLayout extends UiComponent {

    private final List<Panel> panels = new ArrayList<>();
    private int panelGap;

    public PanelLayout(int x, int topY) {
        super(x, topY, 1, 1);
    }

    public PanelLayout panelGap(int gap) {
        this.panelGap = gap;
        return this;
    }

    public void addPanel(Panel panel) {
        panels.add(panel);
        addChild(panel);
        recomputeLayout();
    }

    public List<Panel> panels() {
        return panels;
    }

    @Override
    public void onRender(EnhancedGuiGraphics g, float pt) {
        recomputeLayout();
        for (Panel p : panels) {
            p.updateSize();
        }
        super.onRender(g, pt);
    }

    private void recomputeLayout() {
        int cy = 0;
        for (Panel p : panels) {
            p.setSemanticX(0);
            p.setSemanticY(cy);
            cy += p.height;
            if (panels.indexOf(p) < panels.size() - 1) {
                cy += panelGap;
            }
        }
        height = cy;
    }
}
