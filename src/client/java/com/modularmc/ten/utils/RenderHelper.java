package com.modularmc.ten.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public class RenderHelper {

    static Identifier SHADER_BLOCK = Identifier.withDefaultNamespace("textures/atlas/blocks.png");

    public static TextureAtlasSprite getSprite(Identifier rl) {
        return Minecraft.getInstance()
                .getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS)
                .getSprite(rl);
    }

    public static void drawFluidTank(GuiGraphicsExtractor guiGraphics, Fluid fluid, int x, int y, int width, int height) {
        if (fluid == Fluids.EMPTY) return;
        // Phase 4/5: use NeoForge fluid type extensions when available
        int color = -1; // default white
        Identifier stillTexture = Identifier.withDefaultNamespace("block/water_still");

        float red = (float) (color >> 16 & 255) / 255.0F;
        float green = (float) (color >> 8 & 255) / 255.0F;
        float blue = (float) (color & 255) / 255.0F;
        int packedColor = 0xFF000000 | ((int)(red * 255) << 16) | ((int)(green * 255) << 8) | (int)(blue * 255);

        for (int i = 0; i < width; i += 16) {
            for (int j = 0; j < height; j += 16) {
                int dw = Math.min(width - i, 16);
                int dh = Math.min(height - j, 16);
                drawSprite(guiGraphics, getSprite(stillTexture), x + i, y + j, dw, dh, packedColor);
            }
        }
    }

    public static void drawSprite(GuiGraphicsExtractor guiGraphics, TextureAtlasSprite icon, int x, int y, int width, int height) {
        drawSprite(guiGraphics, icon, x, y, width, height, -1);
    }

    private static void drawSprite(GuiGraphicsExtractor guiGraphics, TextureAtlasSprite icon, int x, int y, int width, int height, int color) {
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, icon.atlasLocation(), width, height, 0, 0, x, y, width, height, color);
    }

    public static void renderBackGround(GuiGraphicsExtractor guiGraphics, int i, int j, int textureW, int textureH, Identifier rl) {
        render(guiGraphics, i, j, 176, 166, textureW, textureH, 0, 0, rl);
    }

    public static void renderBackGround(GuiGraphicsExtractor guiGraphics, int w, int h, int i, int j, int textureW, int textureH, Identifier rl) {
        render(guiGraphics, i, j, w, h, textureW, textureH, 0, 0, rl);
    }

    public static void render(GuiGraphicsExtractor guiGraphics, int x, int y, int width, int height, int textureW, int textureH, int xOff, int yOff, Identifier resourceLocation) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, resourceLocation, x, y, (float) xOff, (float) yOff, width, height, textureW, textureH);
    }

    public static void renderString(GuiGraphicsExtractor guiGraphics, int x, int y, int color, Component str) {
        guiGraphics.text(Minecraft.getInstance().font, str, x, y, color, true);
    }

    public static void renderCString(GuiGraphicsExtractor guiGraphics, int x, int y, int color, Component str) {
        Font font = Minecraft.getInstance().font;
        int textWidth = font.width(str);
        guiGraphics.text(font, str, x - textWidth / 2, y, color, true);
    }
}
