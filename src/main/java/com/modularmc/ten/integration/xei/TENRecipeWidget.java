package com.modularmc.ten.integration.xei;

import com.modularmc.ten.TENConstants;
import com.modularmc.ten.api.recipe.FormsCombinedIngredient;
import com.modularmc.ten.api.recipe.FormsCombinedRecipe;
import com.modularmc.ten.utils.RenderHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
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

    public enum DecorationKind {
        PROGRESS,
        BURN_LEFT
    }

    public record SlotSpec(SlotKind kind, SlotRole role, int recipeIndex, int x, int y, int width, int height) {

        public static SlotSpec item(SlotRole role, int recipeIndex, int x, int y) {
            return new SlotSpec(SlotKind.ITEM, role, recipeIndex, x, y, 18, 18);
        }

        public static SlotSpec fluid(SlotRole role, int recipeIndex, int x, int y) {
            return new SlotSpec(SlotKind.FLUID, role, recipeIndex, x, y, 18, 50);
        }
    }

    public record DecorationSpec(DecorationKind kind, int x, int y, int width, int height, int u, int v) {

        public static DecorationSpec progress(int x, int y) {
            return new DecorationSpec(DecorationKind.PROGRESS, x, y, 22, 16, 27, 0);
        }

        public static DecorationSpec burnLeft(int x, int y, int width, int height, int u, int v) {
            return new DecorationSpec(DecorationKind.BURN_LEFT, x, y, width, height, u, v);
        }
    }

    public record Layout(ResourceLocation background, int u, int v, int width, int height,
                         List<SlotSpec> slots, List<DecorationSpec> decorations) {}

    private static final int DEFAULT_ENERGY_PER_TICK = 15;
    private static final long PROGRESS_CYCLE_MS = 10_000L;
    private static final long BURN_CYCLE_MS = 25_000L;

    private static final Layout DEFAULT_LAYOUT = new Layout(
            TENConstants.JEI_HANDLER_2, 0, 0, 160, 80,
            List.of(
                    SlotSpec.item(SlotRole.INPUT, 0, 10, 10),
                    SlotSpec.item(SlotRole.OUTPUT, 0, 90, 10),
                    SlotSpec.item(SlotRole.OUTPUT, 1, 112, 10),
                    SlotSpec.item(SlotRole.OUTPUT, 2, 90, 32),
                    SlotSpec.item(SlotRole.OUTPUT, 3, 112, 32)),
            List.of(
                    DecorationSpec.progress(65, 30),
                    DecorationSpec.burnLeft(5, 12, 14, 46, 0, 0)));

    private static final Map<String, Layout> LAYOUTS = Map.of(
            "pulverizer", new Layout(
                    TENConstants.JEI_HANDLER_1, 0, 161, 150, 50,
                    List.of(
                            SlotSpec.item(SlotRole.INPUT, 0, 40, 4),
                            SlotSpec.item(SlotRole.OUTPUT, 0, 109, 9),
                            SlotSpec.item(SlotRole.OUTPUT, 1, 127, 9),
                            SlotSpec.item(SlotRole.OUTPUT, 2, 109, 27),
                            SlotSpec.item(SlotRole.OUTPUT, 3, 127, 27)),
                    List.of(
                            DecorationSpec.progress(73, 19),
                            DecorationSpec.burnLeft(6, 2, 14, 46, 0, 0),
                            DecorationSpec.burnLeft(42, 32, 13, 13, 14, 0))),
            "compressor", new Layout(
                    TENConstants.JEI_HANDLER_1, 0, 102, 150, 58,
                    List.of(
                            SlotSpec.item(SlotRole.INPUT, 0, 40, 2),
                            SlotSpec.item(SlotRole.INPUT, 1, 40, 38),
                            SlotSpec.item(SlotRole.OUTPUT, 0, 112, 21)),
                    List.of(
                            DecorationSpec.progress(73, 22),
                            DecorationSpec.burnLeft(6, 5, 14, 46, 0, 0),
                            DecorationSpec.burnLeft(42, 23, 13, 13, 14, 0))),
            "refiner", new Layout(
                    TENConstants.JEI_HANDLER_2, 0, 51, 170, 54,
                    List.of(
                            SlotSpec.fluid(SlotRole.INPUT, 0, 34, 1),
                            SlotSpec.item(SlotRole.INPUT, 0, 55, 18),
                            SlotSpec.item(SlotRole.OUTPUT, 0, 114, 18),
                            SlotSpec.fluid(SlotRole.OUTPUT, 0, 140, 1)),
                    List.of(
                            DecorationSpec.progress(81, 19),
                            DecorationSpec.burnLeft(6, 2, 14, 46, 0, 0),
                            DecorationSpec.burnLeft(57, 40, 13, 13, 14, 0))),
            "induction_furnace", new Layout(
                    TENConstants.JEI_HANDLER_2, 0, 0, 150, 50,
                    List.of(
                            SlotSpec.item(SlotRole.INPUT, 0, 30, 4),
                            SlotSpec.item(SlotRole.INPUT, 1, 48, 4),
                            SlotSpec.item(SlotRole.INPUT, 2, 66, 4),
                            SlotSpec.item(SlotRole.OUTPUT, 0, 124, 18)),
                    List.of(
                            DecorationSpec.progress(90, 19),
                            DecorationSpec.burnLeft(6, 2, 14, 46, 0, 0),
                            DecorationSpec.burnLeft(51, 32, 13, 13, 14, 0))),
            "psionicant", new Layout(
                    TENConstants.JEI_HANDLER_1, 0, 51, 150, 50,
                    List.of(
                            SlotSpec.item(SlotRole.INPUT, 0, 31, 4),
                            SlotSpec.item(SlotRole.INPUT, 1, 49, 4),
                            SlotSpec.item(SlotRole.OUTPUT, 0, 112, 18)),
                    List.of(
                            DecorationSpec.progress(73, 19),
                            DecorationSpec.burnLeft(6, 2, 14, 46, 0, 0),
                            DecorationSpec.burnLeft(42, 32, 13, 13, 14, 0))));

    private TENRecipeWidget() {}

    public static Layout layout(ResourceLocation categoryId) {
        return LAYOUTS.getOrDefault(categoryId.getPath(), DEFAULT_LAYOUT);
    }

    public static Component titleEmi(ResourceLocation categoryId) {
        return Component.translatable("emi.category." + categoryId.getNamespace() + "." + categoryId.getPath());
    }

    public static Component titleJei(ResourceLocation categoryId) {
        return Component.translatable(categoryId.getNamespace() + ".machine_" + categoryId.getPath());
    }

    public static void drawJei(FormsCombinedRecipe recipe, GuiGraphics graphics, Layout layout) {
        drawDecorations(graphics, layout);
    }

    public static void drawSlot(GuiGraphics graphics, int x, int y, boolean isOutput) {
        graphics.blit(TENConstants.GUI_HANDLER, x, y, isOutput ? 24 : 0, 0, 24, 24, 256, 256);
    }

    public static void drawDecorations(GuiGraphics graphics, Layout layout) {
        for (var decoration : layout.decorations()) {
            switch (decoration.kind()) {
                case PROGRESS -> drawProgressDecoration(graphics, decoration, progressPercent());
                case BURN_LEFT -> drawBurnLeftDecoration(graphics, decoration, burnPercent());
            }
        }
    }

    public static List<Component> decorationTooltips(FormsCombinedRecipe recipe, Layout layout, int mouseX, int mouseY) {
        List<Component> tooltips = new ArrayList<>();
        for (var decoration : layout.decorations()) {
            if (!contains(decoration.x(), decoration.y(), decoration.width(), decoration.height(), mouseX, mouseY)) {
                continue;
            }
            switch (decoration.kind()) {
                case PROGRESS -> {
                    tooltips.add(Component.literal((int) (progressPercent() * 100.0d) + "%"));
                    tooltips.add(Component.literal(String.format("%.1fs", recipe.time() / 20.0d)));
                }
                case BURN_LEFT -> tooltips.add(Component.literal(DEFAULT_ENERGY_PER_TICK + " FE/t"));
            }
        }
        return tooltips;
    }

    public static void drawTime(GuiGraphics graphics, int x, int y, int ticks) {
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, String.format("%.1fs", ticks / 20.0d), x, y, 0x555555, false);
    }

    public static void drawFluidTank(GuiGraphics graphics, int x, int y, int w, int h, FluidStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        RenderHelper.drawFluidTank(graphics, stack.getFluid(), x, y, w, h);
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

    private static void drawProgressDecoration(GuiGraphics graphics, DecorationSpec decoration, double percent) {
        RenderHelper.render(graphics, decoration.x(), decoration.y(), decoration.width(), decoration.height(), 256, 256, decoration.u(), decoration.v(), TENConstants.GUI_HANDLER);
        int filledWidth = (int) (percent * decoration.width());
        if (filledWidth > 0) {
            RenderHelper.render(graphics, decoration.x(), decoration.y(), filledWidth, decoration.height(), 256, 256, decoration.u(), decoration.v() + decoration.height(), TENConstants.GUI_HANDLER);
        }
    }

    private static void drawBurnLeftDecoration(GuiGraphics graphics, DecorationSpec decoration, double percent) {
        RenderHelper.render(graphics, decoration.x(), decoration.y(), decoration.width(), decoration.height(), 256, 256, decoration.u(), decoration.v(), TENConstants.GUI_HANDLER);
        int hiddenHeight = (int) (decoration.height() * (1.0d - percent));
        int visibleHeight = decoration.height() - hiddenHeight;
        if (visibleHeight > 0) {
            RenderHelper.render(
                    graphics,
                    decoration.x(),
                    decoration.y() + hiddenHeight,
                    decoration.width(),
                    visibleHeight,
                    256,
                    256,
                    decoration.u(),
                    decoration.v() + decoration.height() + hiddenHeight,
                    TENConstants.GUI_HANDLER);
        }
    }

    private static double progressPercent() {
        return (System.currentTimeMillis() % PROGRESS_CYCLE_MS) / (double) PROGRESS_CYCLE_MS;
    }

    private static double burnPercent() {
        return 1.0d - ((System.currentTimeMillis() % BURN_CYCLE_MS) / (double) BURN_CYCLE_MS);
    }

    private static boolean contains(int x, int y, int width, int height, int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
