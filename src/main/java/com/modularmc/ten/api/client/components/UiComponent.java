package com.modularmc.ten.api.client.components;

import com.modularmc.ten.api.client.gui.EnhancedGuiGraphics;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class UiComponent {

    public boolean hovering;
    protected int semanticX;
    protected int semanticY;
    protected int x;
    protected int y;
    protected int width;
    protected int height;
    protected boolean visible = true;
    protected List<UiComponent> children = new ArrayList<>();
    protected @Nullable SoundEvent soundOnClicked;
    protected @Nullable UiTag tag;

    public UiComponent(int x, int y, int w, int h) {
        this.x = semanticX = x;
        this.y = semanticY = y;
        this.width = w;
        this.height = h;
    }

    public UiComponent addChild(UiComponent child) {
        children.add(child);
        return this;
    }

    public UiComponent removeChild(UiComponent child) {
        children.remove(child);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T extends UiComponent> T withClickSound(@Nullable SoundEvent soundOnClicked) {
        this.soundOnClicked = soundOnClicked;
        return (T) this;
    }

    public void onRescaled(int i, int j) {
        x = semanticX + i;
        y = semanticY + j;

        for (UiComponent child : children) {
            child.onRescaled(x, y);
        }
    }

    public void onRender(EnhancedGuiGraphics g, float pt) {
        for (UiComponent child : children) {
            if (child.isVisible()) {
                child.onRender(g, pt);
            }
        }
    }

    public void onTick() {
        for (UiComponent child : children) {
            child.onTick();
        }
    }

    public void onCollectingTooltips(List<Component> tooltips, int mx, int my) {
        for (UiComponent child : children) {
            if (child.isVisible() && child.hovering) {
                child.onCollectingTooltips(tooltips, mx, my);
            }
        }
    }

    public void onMouseClicked(int mouseX, int mouseY, int button) {
        for (UiComponent child : children) {
            if (child.isVisible() && child.hovering) {
                child.onMouseClicked(mouseX, mouseY, button);
            }
        }

        if (soundOnClicked != null) {
            Minecraft mc = Minecraft.getInstance();
            mc.getSoundManager().play(SimpleSoundInstance.forUI(soundOnClicked, 1.0F));
        }
    }

    public boolean onMouseScrolled(double mx, double my, double delta) {
        for (UiComponent child : children) {
            if (child.isVisible() && child.isMouseHovering((int) mx, (int) my)) {
                if (child.onMouseScrolled(mx, my, delta)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void onMouseMotion(int mouseX, int mouseY) {
        hovering = isMouseHovering(mouseX, mouseY);

        for (UiComponent child : children) {
            child.onMouseMotion(mouseX, mouseY);
        }
    }

    public boolean isMouseHovering(int mouseX, int mouseY) {
        boolean flag = mouseX >= x && mouseY >= y && mouseX <= x + width && mouseY <= y + height;

        if (flag) {
            return true;
        }

        for (UiComponent child : children) {
            if (child.isMouseHovering(mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean v) {
        this.visible = v;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getSemanticX() {
        return semanticX;
    }

    public void setSemanticX(int semanticX) {
        int delta = semanticX - this.semanticX;
        this.semanticX = semanticX;
        this.x += delta;
    }

    public int getSemanticY() {
        return semanticY;
    }

    public void setSemanticY(int semanticY) {
        int delta = semanticY - this.semanticY;
        this.semanticY = semanticY;
        this.y += delta;
    }

    @SuppressWarnings("unchecked")
    public <T extends UiComponent> T setSemanticPosition(int x, int y) {
        setSemanticX(x);
        setSemanticY(y);
        return (T) this;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public Rect2i getBounds() {
        return new Rect2i(x, y, width, height);
    }

    public List<Rect2i> getTakeUp() {
        if (children.isEmpty()) {
            return List.of(getBounds());
        }
        List<Rect2i> takeUp = new ArrayList<>();
        takeUp.add(getBounds());

        for (UiComponent child : children) {
            takeUp.addAll(child.getTakeUp());
        }
        return takeUp;
    }

    @SuppressWarnings("unchecked")
    public <T extends UiComponent> T tagged(@Nullable UiTag tag) {
        this.tag = tag;
        return (T) this;
    }

    public @Nullable UiTag getTag() {
        return tag;
    }
}
