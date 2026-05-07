package com.modularmc.ten.integration.xei;

import com.modularmc.ten.TENConstants;
import com.modularmc.ten.api.recipe.FormsCombinedIngredient;
import com.modularmc.ten.api.recipe.FormsCombinedRecipe;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;
import java.util.Map;

public final class TENRecipeWidget {

    public enum SlotKind {
        ITEM,
        FLUID
    }

    public enum SlotRole {
        INPUT,
        OUTPUT
    }

    public record SlotSpec(SlotKind kind, SlotRole role, int recipeIndex, int x, int y, int width, int height) {

        public static SlotSpec item(SlotRole role, int recipeIndex, int x, int y) {
            return new SlotSpec(SlotKind.ITEM, role, recipeIndex, x, y, 18, 18);
        }

        public static SlotSpec fluid(SlotRole role, int recipeIndex, int x, int y) {
            return new SlotSpec(SlotKind.FLUID, role, recipeIndex, x, y, 18, 50);
        }
    }

    public record Layout(ResourceLocation background, int u, int v, int width, int height,
                         int arrowX, int arrowY, int energyX, int energyY, int timeX, int timeY,
                         List<SlotSpec> slots) {}

    private static final int DEFAULT_ENERGY_PER_TICK = 15;

    private static final Layout DEFAULT_LAYOUT = new Layout(
            TENConstants.JEI_HANDLER_2, 0, 0, 160, 80,
            65, 30, 5, 60, 80, 65,
            List.of(
                    SlotSpec.item(SlotRole.INPUT, 0, 10, 10),
                    SlotSpec.item(SlotRole.OUTPUT, 0, 90, 10),
                    SlotSpec.item(SlotRole.OUTPUT, 1, 112, 10),
                    SlotSpec.item(SlotRole.OUTPUT, 2, 90, 32),
                    SlotSpec.item(SlotRole.OUTPUT, 3, 112, 32)));

    private static final Map<String, Layout> LAYOUTS = Map.of(
            "pulverizer", new Layout(
                    TENConstants.JEI_HANDLER_1, 0, 161, 150, 50,
                    73, 19, 6, 2, 80, 36,
                    List.of(
                            SlotSpec.item(SlotRole.INPUT, 0, 40, 4),
                            SlotSpec.item(SlotRole.OUTPUT, 0, 109, 9),
                            SlotSpec.item(SlotRole.OUTPUT, 1, 127, 9),
                            SlotSpec.item(SlotRole.OUTPUT, 2, 109, 27),
                            SlotSpec.item(SlotRole.OUTPUT, 3, 127, 27))),
            "compressor", new Layout(
                    TENConstants.JEI_HANDLER_1, 0, 102, 150, 58,
                    73, 22, 6, 5, 80, 42,
                    List.of(
                            SlotSpec.item(SlotRole.INPUT, 0, 40, 2),
                            SlotSpec.item(SlotRole.INPUT, 1, 40, 38),
                            SlotSpec.item(SlotRole.OUTPUT, 0, 112, 21))),
            "refiner", new Layout(
                    TENConstants.JEI_HANDLER_2, 0, 51, 170, 54,
                    81, 19, 6, 2, 94, 40,
                    List.of(
                            SlotSpec.fluid(SlotRole.INPUT, 0, 34, 1),
                            SlotSpec.item(SlotRole.INPUT, 0, 55, 18),
                            SlotSpec.item(SlotRole.OUTPUT, 0, 114, 18),
                            SlotSpec.fluid(SlotRole.OUTPUT, 0, 140, 1))),
            "induction_furnace", new Layout(
                    TENConstants.JEI_HANDLER_2, 0, 0, 150, 50,
                    90, 19, 6, 2, 97, 36,
                    List.of(
                            SlotSpec.item(SlotRole.INPUT, 0, 30, 4),
                            SlotSpec.item(SlotRole.INPUT, 1, 48, 4),
                            SlotSpec.item(SlotRole.INPUT, 2, 66, 4),
                            SlotSpec.item(SlotRole.OUTPUT, 0, 124, 18))),
            "psionicant", new Layout(
                    TENConstants.JEI_HANDLER_1, 0, 51, 150, 50,
                    73, 19, 6, 2, 80, 36,
                    List.of(
                            SlotSpec.item(SlotRole.INPUT, 0, 31, 4),
                            SlotSpec.item(SlotRole.INPUT, 1, 49, 4),
                            SlotSpec.item(SlotRole.OUTPUT, 0, 112, 18))));

    private TENRecipeWidget() {}

    public static Layout layout(ResourceLocation categoryId) {
        return LAYOUTS.getOrDefault(categoryId.getPath(), DEFAULT_LAYOUT);
    }

    public static Component title(ResourceLocation categoryId) {
        return Component.translatable("emi.category." + categoryId.getNamespace() + "." + categoryId.getPath());
    }

    public static void drawJei(FormsCombinedRecipe recipe, GuiGraphics graphics, Layout layout) {
        double progress = (System.currentTimeMillis() / 1000.0d) % 2.0d / 2.0d;
        drawProgressArrow(graphics, layout.arrowX(), layout.arrowY(), progress);
        drawEnergy(graphics, layout.energyX(), layout.energyY(), DEFAULT_ENERGY_PER_TICK);
        drawTime(graphics, layout.timeX(), layout.timeY(), recipe.time());
    }

    public static void drawSlot(GuiGraphics graphics, int x, int y, boolean isOutput) {
        graphics.blit(TENConstants.GUI_HANDLER, x, y, isOutput ? 24 : 0, 0, 24, 24, 256, 256);
    }

    public static void drawProgressArrow(GuiGraphics graphics, int x, int y, double progress) {
        graphics.blit(TENConstants.GUI_HANDLER, x, y, 56, 0, (int) (22 * progress), 16, 256, 256);
    }

    public static void drawEnergy(GuiGraphics graphics, int x, int y, int energyPerTick) {
        graphics.blit(TENConstants.GUI_HANDLER, x, y, 0, 24, 14, 14, 256, 256);
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, energyPerTick + " FE/t", x + 16, y + 3, 0xFF5555, false);
    }

    public static void drawTime(GuiGraphics graphics, int x, int y, int ticks) {
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, String.format("%.1fs", ticks / 20.0d), x, y, 0x555555, false);
    }

    public static void drawFluidTank(GuiGraphics graphics, int x, int y, int w, int h, FluidStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        var fluidType = IClientFluidTypeExtensions.of(stack.getFluid());
        int color = fluidType.getTintColor();
        var texture = fluidType.getStillTexture();
        if (texture == null) {
            return;
        }
        graphics.fill(x, y, x + w, y + h, color | 0xFF000000);
        int tileSize = 16;
        for (int offsetX = 0; offsetX < w; offsetX += tileSize) {
            for (int offsetY = 0; offsetY < h; offsetY += tileSize) {
                int tileWidth = Math.min(tileSize, w - offsetX);
                int tileHeight = Math.min(tileSize, h - offsetY);
                graphics.blit(texture, x + offsetX, y + offsetY, tileWidth, tileHeight, 0, 0, tileWidth, tileHeight, tileWidth, tileHeight);
            }
        }
    }

    public static FormsCombinedIngredient ingredientFor(FormsCombinedRecipe recipe, SlotSpec slot) {
        List<FormsCombinedIngredient> pool = switch (slot.role()) {
            case INPUT -> slot.kind() == SlotKind.ITEM ? recipe.allInputItems() : recipe.allInputFluids();
            case OUTPUT -> slot.kind() == SlotKind.ITEM ? recipe.allOutputItems() : recipe.allOutputFluids();
        };
        return slot.recipeIndex() >= 0 && slot.recipeIndex() < pool.size() ? pool.get(slot.recipeIndex()) : null;
    }

    public static int fluidCapacity(FormsCombinedIngredient ingredient) {
        return ingredient.fluidStacks().stream()
                .mapToInt(FluidStack::getAmount)
                .max()
                .orElse(Math.max(1, ingredient.amountOrCount()));
    }

    public static String formatChance(double chance) {
        return String.format("%.0f%% chance", chance * 100.0d);
    }
}
