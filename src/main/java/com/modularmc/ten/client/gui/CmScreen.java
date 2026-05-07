package com.modularmc.ten.client.gui;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;
import com.modularmc.ten.client.gui.element.ElementBase;
import com.modularmc.ten.utils.RenderHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class CmScreen<T extends CmContainerMachine> extends AbstractContainerScreen<T> {

    public static final ResourceLocation HANDLER = TEN.id("textures/gui/handler.png");
    public final ResourceLocation BG;
    protected int texH = 256, texW = 256;
    public int xSize = 176, ySize = 166;
    protected final ArrayList<ElementBase> widgets = new ArrayList<>();
    protected final List<Component> tooltips = new LinkedList<>();
    public T container;

    public CmScreen(T container, Inventory inv, Component title, String path, int textureW, int textureH) {
        super(container, inv, title);
        BG = TEN.id(path);
        texH = textureH;
        texW = textureW;
        this.container = container;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        for (var w : widgets) if (w.isVisible()) w.draw(guiGraphics);
        for (var w : widgets) if (w.isVisible()) w.update();
        for (var w : widgets) if (w.isVisible()) w.hangingEvent(true, mouseX, mouseY);

        ElementBase element = getElementFromLocation(mouseX, mouseY);
        if (element != null) element.addToolTip(tooltips);

        if (!tooltips.isEmpty()) {
            guiGraphics.renderComponentTooltip(Minecraft.getInstance().font, tooltips, mouseX, mouseY);
        }
        super.renderTooltip(guiGraphics, mouseX, mouseY);
        tooltips.clear();
    }

    boolean init;

    public void addWidgets() {}

    @Override
    protected void init() {
        this.imageWidth = xSize;
        this.imageHeight = ySize;
        super.init();
        if (!init) addWidgets();
        init = true;
        updateIJ();
    }

    protected void updateIJ() {
        int i = getGuiLeft();
        int j = getGuiTop();
        for (var e : widgets) e.updateLocWhenFrameResize(i, j);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        int i = getGuiLeft() + getExtras();
        int j = getGuiTop();
        RenderHelper.renderBackGround(guiGraphics, i, j, texW, texH, BG);
    }

    public int getExtras() {
        return 0;
    }

    public ElementBase getElementFromLocation(int mouseX, int mouseY) {
        for (var e : widgets) {
            if (e.checkInstr(mouseX, mouseY) && e.isVisible()) return e;
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (var e : widgets) {
                if (e.checkInstr((int) mouseX, (int) mouseY) && e.isVisible()) {
                    e.onMouseClicked((int) mouseX, (int) mouseY);
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public double pFuel() {
        if (container.data == null) return 0;
        var d = container.data;
        return d.get(CmMachineBlockEntity.MAX_FUEL) != 0 ? (double) d.get(CmMachineBlockEntity.FUEL) / d.get(CmMachineBlockEntity.MAX_FUEL) : 0;
    }

    public double pProgress() {
        if (container.data == null) return 0;
        var d = container.data;
        return d.get(CmMachineBlockEntity.MAX_PROGRESS) != 0 ? (double) d.get(CmMachineBlockEntity.PROGRESS) / d.get(CmMachineBlockEntity.MAX_PROGRESS) : 0;
    }

    public double pEnergy() {
        if (container.data == null) return 0;
        var d = container.data;
        return d.get(CmMachineBlockEntity.MAX_ENERGY) != 0 ? (double) d.get(CmMachineBlockEntity.ENERGY) / d.get(CmMachineBlockEntity.MAX_ENERGY) : 0;
    }

    public int energy() {
        if (container.data == null) return 0;
        return container.data.get(CmMachineBlockEntity.ENERGY);
    }

    public int maxEnergy() {
        if (container.data == null) return 0;
        return container.data.get(CmMachineBlockEntity.MAX_ENERGY);
    }
}
