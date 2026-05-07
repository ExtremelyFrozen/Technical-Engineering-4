package com.modularmc.ten.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

import org.lwjgl.opengl.GL11;

public class RenderHelper {

    static ResourceLocation SHADER_BLOCK = ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png");

    public static TextureAtlasSprite getSprite(ResourceLocation rl) {
        return Minecraft.getInstance()
                .getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS)
                .getSprite(rl);
    }

    public static void drawFluidTank(PoseStack matrixStack, Fluid fluid, int x, int y, int width, int height) {
        if (fluid == Fluids.EMPTY) return;

        var fluidType = IClientFluidTypeExtensions.of(fluid);
        int color = fluidType.getTintColor();
        ResourceLocation stillTexture = fluidType.getStillTexture();

        if (stillTexture == null) return;

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);

        float red = (float) (color >> 16 & 255) / 255.0F;
        float green = (float) (color >> 8 & 255) / 255.0F;
        float blue = (float) (color & 255) / 255.0F;
        RenderSystem.setShaderColor(red, green, blue, 1.0F);
        RenderSystem.setShaderTexture(0, SHADER_BLOCK);

        for (int i = 0; i < width; i += 16) {
            for (int j = 0; j < height; j += 16) {
                int drawWidth = Math.min(width - i, 16);
                int drawHeight = Math.min(height - j, 16);
                drawSprite(matrixStack, getSprite(stillTexture), x + i, y + j, drawWidth, drawHeight);
            }
        }

        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    private static void drawSprite(PoseStack matrixStack, TextureAtlasSprite icon, int x, int y, int width, int height) {
        float minU = icon.getU0();
        float maxU = icon.getU1();
        float minV = icon.getV0();
        float maxV = icon.getV1();

        float u = minU + (maxU - minU) * width / 16F;
        float v = minV + (maxV - minV) * height / 16F;

        var matrix = matrixStack.last().pose();

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.addVertex(matrix, (float) x, (float) (y + height), 0.0F).setUv(minU, v);
        buffer.addVertex(matrix, (float) (x + width), (float) (y + height), 0.0F).setUv(u, v);
        buffer.addVertex(matrix, (float) (x + width), (float) y, 0.0F).setUv(u, minV);
        buffer.addVertex(matrix, (float) x, (float) y, 0.0F).setUv(minU, minV);
        BufferUploader.drawWithShader(buffer.build());
    }

    public static void bindTexture(ResourceLocation resourceLocation) {
        RenderSystem.setShaderTexture(0, resourceLocation);
    }

    public static void renderBackGround(PoseStack matrixStack, int i, int j, int textureW, int textureH, ResourceLocation resourceLocation) {
        bindTexture(resourceLocation);
        render(matrixStack, i, j, 176, 166, textureW, textureH, 0, 0, resourceLocation);
    }

    public static void renderBackGround(PoseStack matrixStack, int w, int h, int i, int j, int textureW, int textureH, ResourceLocation resourceLocation) {
        bindTexture(resourceLocation);
        render(matrixStack, i, j, w, h, textureW, textureH, 0, 0, resourceLocation);
    }

    public static void render(PoseStack matrixStack, int x, int y, int width, int height, int textureW, int textureH, int xOff, int yOff, ResourceLocation resourceLocation) {
        bindTexture(resourceLocation);
        float minU = (float) xOff / (float) textureW;
        float maxU = (float) (xOff + width) / (float) textureW;
        float minV = (float) yOff / (float) textureH;
        float maxV = (float) (yOff + height) / (float) textureH;

        var matrix = matrixStack.last().pose();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.addVertex(matrix, (float) x, (float) (y + height), 0.0F).setUv(minU, maxV);
        buffer.addVertex(matrix, (float) (x + width), (float) (y + height), 0.0F).setUv(maxU, maxV);
        buffer.addVertex(matrix, (float) (x + width), (float) y, 0.0F).setUv(maxU, minV);
        buffer.addVertex(matrix, (float) x, (float) y, 0.0F).setUv(minU, minV);
        BufferUploader.drawWithShader(buffer.build());
    }

    public static void renderString(PoseStack matrixStack, int x, int y, int color, Component str) {
        Font font = Minecraft.getInstance().font;
        font.drawInBatch(str, (float) x, (float) y, color, true, matrixStack.last().pose(),
                MultiBufferSource.immediate(new ByteBufferBuilder(256)),
                Font.DisplayMode.NORMAL, 0, 15728880);
    }

    public static void renderCString(PoseStack matrixStack, int x, int y, int color, Component str) {
        Font font = Minecraft.getInstance().font;
        int width = font.width(str);
        font.drawInBatch(str, (float) (x - width / 2.0), (float) y, color, true, matrixStack.last().pose(),
                MultiBufferSource.immediate(new ByteBufferBuilder(256)),
                Font.DisplayMode.NORMAL, 0, 15728880);
    }
}
