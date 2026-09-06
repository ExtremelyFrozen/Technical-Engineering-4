package com.modularmc.ten.api.client.components;

import com.modularmc.ten.api.client.gui.EnhancedGuiGraphics;
import com.modularmc.ten.api.client.gui.TextureRegion;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class Panel extends UiComponent {

    public static final int LEFT = 0;
    public static final int RIGHT = 1;
    public static final int EXPANDING_SPEED = 3;
    public static final int MIN_WIDTH = 19;
    public static final int MIN_HEIGHT = 19;
    private static final int BORDER = 4; // border thickness
    private final TextureRegion tabBg;
    public boolean open;
    public int side;
    protected boolean fullyOpen;
    protected int minWidth = MIN_WIDTH;
    protected int maxWidth;
    protected int minHeight = MIN_HEIGHT;
    protected int maxHeight;
    private Runnable beforeOpen;

    protected Panel(TextureRegion tabBg, int expandedW, int expandedH, int side) {
        super(0, 0, expandedW, expandedH);
        this.tabBg = tabBg;
        this.side = side;
        this.maxWidth = expandedW;
        this.maxHeight = expandedH;
        this.width = minWidth;
        this.height = minHeight;
    }

    protected @Nullable TextureRegion getIcon() {
        return null;
    }

    protected @Nullable Component getTitle() {
        return null;
    }

    public Panel minSize(int w, int h) {
        minWidth = w;
        minHeight = h;
        width = w;
        height = h;
        return this;
    }

    public void updateSize() {
        if (open && width < maxWidth) {
            width += EXPANDING_SPEED;
        } else if (!open && width > minWidth) {
            width -= EXPANDING_SPEED;
        }
        width = Math.clamp(width, minWidth, maxWidth);

        if (open && height < maxHeight) {
            height += EXPANDING_SPEED;
        } else if (!open && height > minHeight) {
            height -= EXPANDING_SPEED;
        }
        height = Math.clamp(height, minHeight, maxHeight);

        if (!fullyOpen && open && width == maxWidth && height == maxHeight) {
            setFullyOpen();
        }
    }

    public void setFullyOpen() {
        open = true;
        fullyOpen = true;
        width = maxWidth;
        height = maxHeight;
    }

    public void toggleOpen() {
        if (open) {
            open = false;
            fullyOpen = false;
        } else {
            if (beforeOpen != null) {
                beforeOpen.run();
            }
            open = true;
        }
    }

    void setBeforeOpen(Runnable cb) {
        this.beforeOpen = cb;
    }

    public int effectiveX() {
        return side == LEFT ? x - width + minWidth : x;
    }

    @Override
    public void onRescaled(int guiX, int guiY) {
        x = semanticX + guiX;
        y = semanticY + guiY;
        for (UiComponent child : children) {
            child.onRescaled(effectiveX(), y);
        }
    }

    @Override
    public void onRender(EnhancedGuiGraphics g, float pt) {
        int ex = effectiveX();
        drawBackground(g, ex, y);
        if (fullyOpen) {
            renderBody(g, ex, y, maxWidth, maxHeight);
            for (UiComponent child : children) {
                if (child.isVisible()) {
                    child.onRender(g, pt);
                }
            }
        }
        drawForeground(g, ex, y);
    }

    @Override
    public void onCollectingTooltips(List<Component> tooltips, int mx, int my) {
        super.onCollectingTooltips(tooltips, mx, my);

        if (isHoveringUpper(mx, my)) {
            tooltips.add(getTitle());
        }
    }

    @Override
    public void onMouseClicked(int mx, int my, int btn) {
        int ex = effectiveX();

        if (isHoveringUpper(mx, my)) {
            toggleOpen();
            return;
        }

        if (fullyOpen) {
            super.onMouseClicked(mx, my, btn);
        }
    }

    @Override
    public boolean isMouseHovering(int mx, int my) {
        int ex = effectiveX();
        return mx >= ex && my >= y && mx <= ex + width && my <= y + height;
    }

    @Override
    public List<Rect2i> getTakeUp() {
        return List.of(new Rect2i(effectiveX(), y, width, height));
    }

    private boolean isHoveringUpper(int mx, int my) {
        int ex = effectiveX();
        return mx >= ex && my >= y && mx <= ex + width && my <= y + minHeight;
    }

    protected void drawBackground(EnhancedGuiGraphics g, int px, int py) {
        if (tabBg == null) {
            return;
        }
        GuiGraphics gg = g.inner();
        var res = tabBg.texture().resource();
        int texW = tabBg.texture().width();
        int texH = tabBg.texture().height();
        int u0 = tabBg.u(), v0 = tabBg.v();
        int rgnW = tabBg.width(), rgnH = tabBg.height();
        int w = width, h = height;
        int innerW = rgnW - 2 * BORDER, innerH = rgnH - 2 * BORDER;
        int midW = w - 2 * BORDER, midH = h - 2 * BORDER;

        // corners
        gg.blit(res, px, py, BORDER, BORDER, (float) u0, (float) v0, BORDER, BORDER, texW, texH);                    // TL
        gg.blit(res, px + w - BORDER, py, BORDER, BORDER, (float) (u0 + rgnW - BORDER), (float) v0, BORDER, BORDER, texW, texH);       // TR
        gg.blit(res, px, py + h - BORDER, BORDER, BORDER, (float) u0, (float) (v0 + rgnH - BORDER), BORDER, BORDER, texW, texH);       // BL
        gg.blit(res, px + w - BORDER, py + h - BORDER, BORDER, BORDER, (float) (u0 + rgnW - BORDER), (float) (v0 + rgnH - BORDER), BORDER, BORDER, texW, texH); // BR

        // edges
        gg.blit(res, px + BORDER, py, midW, BORDER, (float) (u0 + BORDER), (float) v0, innerW, BORDER, texW, texH);         // T
        gg.blit(res, px + BORDER, py + h - BORDER, midW, BORDER, (float) (u0 + BORDER), (float) (v0 + rgnH - BORDER), innerW, BORDER, texW, texH); // B
        gg.blit(res, px, py + BORDER, BORDER, midH, (float) u0, (float) (v0 + BORDER), BORDER, innerH, texW, texH);         // L
        gg.blit(res, px + w - BORDER, py + BORDER, BORDER, midH, (float) (u0 + rgnW - BORDER), (float) (v0 + BORDER), BORDER, innerH, texW, texH); // R

        // center
        gg.blit(res, px + BORDER, py + BORDER, midW, midH, (float) (u0 + BORDER), (float) (v0 + BORDER), innerW, innerH, texW, texH);
    }

    protected void drawForeground(EnhancedGuiGraphics g, int px, int py) {
        TextureRegion icon = getIcon();
        if (icon != null) {
            int iconX = px + sideOffset();
            int iconY = py + 2;
            g.draw(icon, iconX, iconY);
        }

        if (fullyOpen) {
            Component title = getTitle();
            if (title != null) {
                g.drawString(title, px + sideOffset() + 14, py + 4, 0x404040, false);
            }
        }
    }

    protected void renderBody(EnhancedGuiGraphics g, int px, int py, int pw, int ph) {}

    protected int sideOffset() {
        return 4;
    }
}
