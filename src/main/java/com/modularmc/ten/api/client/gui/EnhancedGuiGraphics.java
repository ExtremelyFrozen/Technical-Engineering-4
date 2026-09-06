package com.modularmc.ten.api.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.List;
import java.util.function.IntSupplier;

@OnlyIn(Dist.CLIENT)
public final class EnhancedGuiGraphics {

    private static final ResourceLocation BLOCK_ATLAS = ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png");

    private final GuiGraphics inner;
    private final Font font;

    public EnhancedGuiGraphics(GuiGraphics inner) {
        this.inner = inner;
        this.font = Minecraft.getInstance().font;
    }

    public Font font() {
        return font;
    }

    public GuiGraphics inner() {
        return inner;
    }

    public void draw(@Nullable TextureRegion region, int x, int y, int w, int h, int u, int v, int srcW, int srcH) {
        if (region == null) {
            return;
        }
        Texture tex = region.texture();
        inner.blit(tex.resource(), x, y, w, h, (float) region.u() + u, (float) region.v() + v, srcW, srcH, tex.width(), tex.height());
    }

    public void draw(@Nullable TextureRegion region, int x, int y, int w, int h) {
        if (region == null) {
            return;
        }
        Texture tex = region.texture();
        inner.blit(tex.resource(), x, y, w, h, (float) region.u(), (float) region.v(), region.width(), region.height(), tex.width(), tex.height());
    }

    public void draw(@Nullable TextureRegion region, int x, int y) {
        if (region == null) {
            return;
        }
        draw(region, x, y, region.width(), region.height());
    }

    public void draw(@Nullable ResourceLocation resource, int x, int y, int w, int h) {
        if (resource == null) {
            return;
        }
        Texture tex = Texture.of(resource);
        if (tex != null) {
            inner.blit(tex.resource(), x, y, w, h, 0f, 0f, tex.width(), tex.height(), tex.width(), tex.height());
        }
    }

    public void draw(@Nullable Texture texture, int x, int y, int w, int h) {
        if (texture == null) {
            return;
        }
        inner.blit(texture.resource(), x, y, w, h, 0f, 0f, texture.width(), texture.height(), texture.width(), texture.height());
    }

    public void draw(@Nullable Texture texture, int x, int y) {
        if (texture == null) {
            return;
        }
        draw(texture, x, y, texture.width(), texture.height());
    }

    public void drawSprite(@Nullable TextureAtlasSprite sprite, int x, int y, int width, int height) {
        if (sprite == null) {
            return;
        }
        float minU = sprite.getU0(), maxU = sprite.getU1();
        float minV = sprite.getV0(), maxV = sprite.getV1();
        float u = minU + (maxU - minU) * width / 16F;
        float v = minV + (maxV - minV) * height / 16F;

        Matrix4f matrix = inner.pose().last().pose();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.addVertex(matrix, (float) x, (float) (y + height), 0.0F).setUv(minU, v);
        buffer.addVertex(matrix, (float) (x + width), (float) (y + height), 0.0F).setUv(u, v);
        buffer.addVertex(matrix, (float) (x + width), (float) y, 0.0F).setUv(u, minV);
        buffer.addVertex(matrix, (float) x, (float) y, 0.0F).setUv(minU, minV);
        MeshData mesh = buffer.build();
        if (mesh != null) {
            BufferUploader.drawWithShader(mesh);
        }
    }

    public void drawFluid(Fluid fluid, int x, int y, int width, int height, boolean flowing, IntSupplier tinter) {
        if (fluid == Fluids.EMPTY) {
            return;
        }

        IClientFluidTypeExtensions fluidType = IClientFluidTypeExtensions.of(fluid);
        int color = tinter.getAsInt();
        ResourceLocation stillTexture = flowing ? fluidType.getFlowingTexture() : fluidType.getStillTexture();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor((float) (color >> 16 & 255) / 255.0F, (float) (color >> 8 & 255) / 255.0F, (float) (color & 255) / 255.0F, 1.0F);
        RenderSystem.setShaderTexture(0, BLOCK_ATLAS);

        TextureAtlasSprite sprite = ClientUtil.getBlockSprite(stillTexture);
        for (int i = 0; i < width; i += 16) {
            for (int j = 0; j < height; j += 16) {
                drawSprite(sprite, x + i, y + j, Math.min(width - i, 16), Math.min(height - j, 16));
            }
        }
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    public void drawString(Component text, int x, int y, int color, boolean shadow) {
        inner.drawString(font, text, x, y, color, shadow);
    }

    public void drawBrokenString(FormattedText text, int x, int y, int color, int maxWidth, boolean shadow) {
        List<FormattedCharSequence> comps = font.split(text, maxWidth);
        for (int l = 0; l < comps.size(); l++) {
            FormattedCharSequence fcs = comps.get(l);
            inner.drawString(font, fcs, x, y + l * 9, color, shadow);
        }
    }

    public void drawCenteredString(Component text, int x, int y, int color, boolean shadow) {
        inner.drawString(font, text, x - font.width(text) / 2, y, color, shadow);
    }

    public void drawString(FormattedCharSequence text, int x, int y, int color, boolean shadow) {
        inner.drawString(font, text, x, y, color, shadow);
    }

    public void drawCenteredString(FormattedCharSequence text, int x, int y, int color, boolean shadow) {
        inner.drawString(font, text, x - font.width(text) / 2, y, color, shadow);
    }

    public int stringWidth(FormattedText text) {
        return font.width(text);
    }

    public int fontLineHeight() {
        return font.lineHeight;
    }
}
