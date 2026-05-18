package com.modularmc.ten.client.gui;

import com.modularmc.ten.TEN;
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
        int i = getGuiLeft();
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

    /** 燃料进度：基于 @DescSynced 的 machine 字段 */
    public double pFuel() {
        var m = container.machine;
        if (m == null) return 0;
        return m.maxFuel != 0 ? (double) m.fuel / m.maxFuel : 0;
    }

    /** 加工进度：基于 @DescSynced 的 machine 字段 */
    public double pProgress() {
        var m = container.machine;
        if (m == null) return 0;
        return m.maxProgress != 0 ? (double) m.progress / m.maxProgress : 0;
    }

    /** 能量比例：基于 @DescSynced 的 machine 字段 */
    public double pEnergy() {
        var m = container.machine;
        if (m == null) return 0;
        return m.maxEnergyStored != 0 ? (double) m.energyStored / m.maxEnergyStored : 0;
    }

    /** 当前能量 */
    public int energy() {
        var m = container.machine;
        if (m == null) return 0;
        return m.energyStored;
    }

    /** 最大能量 */
    public int maxEnergy() {
        var m = container.machine;
        if (m == null) return 0;
        return m.maxEnergyStored;
    }
}
