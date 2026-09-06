package com.modularmc.ten.api.client.components;

import com.modularmc.ten.api.client.gui.EnhancedGuiGraphics;
import com.modularmc.ten.api.client.gui.TextureRegion;

import org.jetbrains.annotations.Nullable;

public class Button extends UiComponent {

    protected @Nullable Runnable action;
    protected boolean pressed;
    protected boolean enablePressed;
    protected @Nullable TextureRegion idleTex;
    protected @Nullable TextureRegion hoveringTex;
    protected @Nullable TextureRegion pressedTex;

    public Button(int x, int y, int w, int h) {
        super(x, y, w, h);
    }

    public Button withTexture(@Nullable TextureRegion idleTex, @Nullable TextureRegion hoveringTex, @Nullable TextureRegion pressedTex) {
        this.idleTex = idleTex;
        this.hoveringTex = hoveringTex;
        this.pressedTex = pressedTex;
        return this;
    }

    public Button setAction(@Nullable Runnable action) {
        this.action = action;
        return this;
    }

    public Button setEnablePressed(boolean enablePressed) {
        this.enablePressed = enablePressed;
        return this;
    }

    public boolean isPressed() {
        return pressed;
    }

    @Override
    public void onRender(EnhancedGuiGraphics g, float pt) {
        if (pressed) {
            g.draw(pressedTex, x, y, width, height);
        } else {
            if (hovering) {
                g.draw(hoveringTex, x, y, width, height);
            } else {
                g.draw(idleTex, x, y, width, height);
            }
        }

        super.onRender(g, pt);
    }

    @Override
    public void onMouseClicked(int mouseX, int mouseY, int button) {
        super.onMouseClicked(mouseX, mouseY, button);

        if (action != null) {
            action.run();
        }

        if (enablePressed) {
            pressed = !pressed;
        }
    }
}
