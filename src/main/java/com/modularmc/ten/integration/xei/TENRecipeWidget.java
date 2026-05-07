package com.modularmc.ten.integration.xei;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.recipe.FormsCombinedRecipe;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

public class TENRecipeWidget {

    public static final ResourceLocation GUI = TEN.id("textures/gui/handler.png");
    public static final ResourceLocation JEI_BG = TEN.id("textures/gui/jei_handler2.png");

    public static void drawSlot(GuiGraphics graphics, int x, int y, boolean isOutput) {
        graphics.blit(GUI, x, y, isOutput ? 24 : 0, 0, 24, 24, 256, 256);
    }

    public static void drawProgressArrow(GuiGraphics graphics, int x, int y, double progress) {
        graphics.blit(GUI, x, y, 56, 0, (int) (22 * progress), 16, 256, 256);
    }

    public static void drawEnergy(GuiGraphics graphics, int x, int y, int energyPerTick) {
        graphics.blit(GUI, x, y, 0, 24, 14, 14, 256, 256);
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, energyPerTick + " FE/t", x + 16, y + 3, 0xFF5555, false);
    }

    public static void drawTime(GuiGraphics graphics, int x, int y, int ticks) {
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, String.format("%.1fs", ticks / 20.0), x, y, 0x555555, false);
    }

    public static void drawFluidTank(GuiGraphics graphics, int x, int y, int w, int h, FluidStack stack) {
        if (stack.isEmpty()) return;
        var fluidType = IClientFluidTypeExtensions.of(stack.getFluid());
        int color = fluidType.getTintColor();
        var tex = fluidType.getStillTexture();
        if (tex == null) return;
        graphics.fill(x, y, x + w, y + h, color | 0xFF000000);
        int tileSize = 16;
        for (int i = 0; i < w; i += tileSize) {
            for (int j = 0; j < h; j += tileSize) {
                int tw = Math.min(tileSize, w - i);
                int th = Math.min(tileSize, h - j);
                graphics.blit(tex, x + i, y + j, tw, th, 0, 0, tw, th, tw, th);
            }
        }
    }

    public static List<Component> getInputTooltip(FormsCombinedRecipe recipe) {
        List<Component> tips = new ArrayList<>();
        for (var ing : recipe.allInputItems()) {
            var stack = ing.symbolItem();
            if (!stack.isEmpty()) {
                tips.add(Component.literal("Input: ").append(stack.getHoverName()));
                if (ing.amountOrCount() > 1) tips.add(Component.literal("x" + ing.amountOrCount()));
            }
        }
        return tips;
    }

    public static List<Component> getOutputTooltip(FormsCombinedRecipe recipe) {
        List<Component> tips = new ArrayList<>();
        for (var ing : recipe.allOutputItems()) {
            var stack = ing.symbolItem();
            if (!stack.isEmpty()) {
                tips.add(Component.literal("Output: ").append(stack.getHoverName()));
                if (ing.amountOrCount() > 1) tips.add(Component.literal("x" + ing.amountOrCount()));
                if (ing.chance() < 1.0f) tips.add(Component.literal(String.format("%.0f%%", ing.chance() * 100)));
            }
        }
        return tips;
    }
}
