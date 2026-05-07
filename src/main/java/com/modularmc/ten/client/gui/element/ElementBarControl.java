package com.modularmc.ten.client.gui.element;

import com.modularmc.ten.utils.ComponentHelper;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class ElementBarControl extends ElementImage {

    public boolean show;
    public ElementButton panel;

    public ElementBarControl(int xr, int y, int w, int h, int xOff, int yOff, ResourceLocation rl) {
        super(xr, y, w, h, xOff, yOff, rl);
        panel = new ElementButton(-61, y, 60, 85, 91, 40, rl, () -> show = !show) {

            @Override
            public boolean checkInstr(int mouseX, int mouseY) {
                return mouseX >= x + width - 10 && mouseX <= x + width && mouseY >= y && mouseY <= y + 10;
            }
        };
    }

    @Override
    public void updateLocWhenFrameResize(int i, int j) {
        super.updateLocWhenFrameResize(i, j);
        panel.updateLocWhenFrameResize(i, j);
    }

    @Override
    public boolean checkInstr(int mouseX, int mouseY) {
        return show ? panel.checkInstr(mouseX, mouseY) : super.checkInstr(mouseX, mouseY);
    }

    @Override
    public void onMouseClicked(int mouseX, int mouseY) {
        show = !show;
    }

    @Override
    public void draw(GuiGraphics guiGraphics) {
        if (show) panel.draw(guiGraphics);
        else super.draw(guiGraphics);
    }

    @Override
    public void addToolTip(List<Component> tooltips) {
        tooltips.add(ComponentHelper.translated(ComponentHelper.GOLD, "kenergyengineering.info.bar_control"));
    }
}
