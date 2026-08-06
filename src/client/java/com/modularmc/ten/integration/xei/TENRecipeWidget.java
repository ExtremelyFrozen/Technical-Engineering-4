package com.modularmc.ten.integration.xei;

import com.modularmc.ten.TENConstants;
import com.modularmc.ten.api.recipe.FormsCombinedIngredient;
import com.modularmc.ten.api.recipe.FormsCombinedRecipe;
import com.modularmc.ten.utils.RenderHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

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

    public record DecorationSpec(DecorationKind kind, int x, int y, int width, int height, Identifier background, Identifier fill) {

        public static DecorationSpec progress(int x, int y, Identifier background, Identifier fill) {
            return new DecorationSpec(DecorationKind.PROGRESS, x, y, 22, 16, background, fill);
        }

        public static DecorationSpec energyGauge(int x, int y) {
            return new DecorationSpec(DecorationKind.BURN_LEFT, x, y, 14, 46, TENConstants.ENERGY_GAUGE_BG, TENConstants.ENERGY_GAUGE_FILL);
        }
    }

    public record Layout(Identifier background, int u, int v, int width, int height,
                         List<SlotSpec> slots, List<DecorationSpec> decorations) {}

    private static final int DEFAULT_ENERGY_PER_TICK = 15;
    private static final long PROGRESS_CYCLE_MS = 10_000L;
    private static final long BURN_CYCLE_MS = 25_000L;

    /** 进度箭头素材映射（smelter→progress_arrow_smelter、pulverizer→progress_arrow_pulverizer、compressor→progress_arrow_compressor、refiner→progress_arrow_refiner、induction_furnace→progress_arrow_induction_furnace、psionicant→progress_arrow_psionicant）。集中一处，改映射只动此处。 */
    private record ProgressArrow(Identifier background, Identifier fill) {}

    private static ProgressArrow progressArrow(String machine) {
        return switch (machine) {
            case "smelter" -> new ProgressArrow(TENConstants.PROGRESS_ARROW_SMELTER_BG, TENConstants.PROGRESS_ARROW_SMELTER_FILL);
            case "pulverizer" -> new ProgressArrow(TENConstants.PROGRESS_ARROW_PULVERIZER_BG, TENConstants.PROGRESS_ARROW_PULVERIZER_FILL);
            case "compressor" -> new ProgressArrow(TENConstants.PROGRESS_ARROW_COMPRESSOR_BG, TENConstants.PROGRESS_ARROW_COMPRESSOR_FILL);
            case "refiner" -> new ProgressArrow(TENConstants.PROGRESS_ARROW_REFINER_BG, TENConstants.PROGRESS_ARROW_REFINER_FILL);
            case "induction_furnace" -> new ProgressArrow(TENConstants.PROGRESS_ARROW_INDUCTION_FURNACE_BG, TENConstants.PROGRESS_ARROW_INDUCTION_FURNACE_FILL);
            case "psionicant" -> new ProgressArrow(TENConstants.PROGRESS_ARROW_PSIONICANT_BG, TENConstants.PROGRESS_ARROW_PSIONICANT_FILL);
            default -> new ProgressArrow(TENConstants.PROGRESS_ARROW_SMELTER_BG, TENConstants.PROGRESS_ARROW_SMELTER_FILL);
        };
    }

    /**
     * 布局规则 v4（用户最终确认，组件群居中 + 间距自适应）：
     * <ol>
     *   <li>能量条（14×46）左缘 x=8，垂直居中 y=2，右缘 x=22</li>
     *   <li>组件群左缘 = 8（能量条左缘）、右缘 = 142、总宽 134 → 组件群中心 = 75 = 面板中心（150×50）</li>
     *   <li>固定宽 = 能量条14 + 输入组宽 + 箭头22 + 输出组宽；间隔总量 = 134 − 固定宽，均分到 3 个间隔
     *       （能量条→输入组、输入组→箭头、箭头→输出组），取整保证中心精确 75、间隔 ≥4、无重叠</li>
     *   <li>连续同种槽（多输入/多输出组）算一个整体：组内紧贴</li>
     *   <li>垂直方向组件组在面板 50 高内居中：18×18 槽 y=16、箭头 y=17、竖排/2×2 组 y=7、fluid y=0</li>
     *   <li>refiner 例外：布局饱满，保持 v3 现状（能量条 8、fluid 26、item 48、箭头 74、item 104、fluid 126）</li>
     * </ol>
     */
    private static final Layout DEFAULT_LAYOUT = new Layout(
            TENConstants.JEI_HANDLER_MODULAR, 0, 0, 150, 50,
            List.of(
                    SlotSpec.item(SlotRole.INPUT, 0, 37, 16),
                    SlotSpec.item(SlotRole.OUTPUT, 0, 106, 7),
                    SlotSpec.item(SlotRole.OUTPUT, 1, 124, 7),
                    SlotSpec.item(SlotRole.OUTPUT, 2, 106, 25),
                    SlotSpec.item(SlotRole.OUTPUT, 3, 124, 25)),
            List.of(
                    DecorationSpec.progress(70, 17, TENConstants.PROGRESS_ARROW_SMELTER_BG, TENConstants.PROGRESS_ARROW_SMELTER_FILL),
                    DecorationSpec.energyGauge(8, 2)));

    private static final Map<String, Layout> LAYOUTS = buildLayouts();

    private static Map<String, Layout> buildLayouts() {
        var pulverizerArrow = progressArrow("pulverizer");
        var compressorArrow = progressArrow("compressor");
        var refinerArrow = progressArrow("refiner");
        var inductionFurnaceArrow = progressArrow("induction_furnace");
        var psionicantArrow = progressArrow("psionicant");
        return Map.of(
                "pulverizer", new Layout(
                        TENConstants.JEI_HANDLER_MODULAR, 0, 0, 150, 50,
                        List.of(
                                SlotSpec.item(SlotRole.INPUT, 0, 37, 16),
                                SlotSpec.item(SlotRole.OUTPUT, 0, 106, 7),
                                SlotSpec.item(SlotRole.OUTPUT, 1, 124, 7),
                                SlotSpec.item(SlotRole.OUTPUT, 2, 106, 25),
                                SlotSpec.item(SlotRole.OUTPUT, 3, 124, 25)),
                        List.of(
                                DecorationSpec.progress(70, 17, pulverizerArrow.background(), pulverizerArrow.fill()),
                                DecorationSpec.energyGauge(8, 2))),
                "compressor", new Layout(
                        TENConstants.JEI_HANDLER_MODULAR, 0, 0, 150, 50,
                        List.of(
                                SlotSpec.item(SlotRole.INPUT, 0, 43, 7),
                                SlotSpec.item(SlotRole.INPUT, 1, 43, 25),
                                SlotSpec.item(SlotRole.OUTPUT, 0, 124, 16)),
                        List.of(
                                DecorationSpec.progress(82, 17, compressorArrow.background(), compressorArrow.fill()),
                                DecorationSpec.energyGauge(8, 2))),
                "refiner", new Layout(
                        TENConstants.JEI_HANDLER_MODULAR, 0, 0, 150, 50,
                        List.of(
                                SlotSpec.fluid(SlotRole.INPUT, 0, 26, 0),
                                SlotSpec.item(SlotRole.INPUT, 0, 48, 16),
                                SlotSpec.item(SlotRole.OUTPUT, 0, 104, 16),
                                SlotSpec.fluid(SlotRole.OUTPUT, 0, 126, 0)),
                        List.of(
                                DecorationSpec.progress(74, 17, refinerArrow.background(), refinerArrow.fill()),
                                DecorationSpec.energyGauge(8, 2))),
                "induction_furnace", new Layout(
                        TENConstants.JEI_HANDLER_MODULAR, 0, 0, 150, 50,
                        List.of(
                                SlotSpec.item(SlotRole.INPUT, 0, 31, 16),
                                SlotSpec.item(SlotRole.INPUT, 1, 49, 16),
                                SlotSpec.item(SlotRole.INPUT, 2, 67, 16),
                                SlotSpec.item(SlotRole.OUTPUT, 0, 124, 16)),
                        List.of(
                                DecorationSpec.progress(93, 17, inductionFurnaceArrow.background(), inductionFurnaceArrow.fill()),
                                DecorationSpec.energyGauge(8, 2))),
                "psionicant", new Layout(
                        TENConstants.JEI_HANDLER_MODULAR, 0, 0, 150, 50,
                        List.of(
                                SlotSpec.item(SlotRole.INPUT, 0, 37, 16),
                                SlotSpec.item(SlotRole.INPUT, 1, 55, 16),
                                SlotSpec.item(SlotRole.OUTPUT, 0, 124, 16)),
                        List.of(
                                DecorationSpec.progress(88, 17, psionicantArrow.background(), psionicantArrow.fill()),
                                DecorationSpec.energyGauge(8, 2))));
    }

    private TENRecipeWidget() {}

    public static Layout layout(Identifier categoryId) {
        return LAYOUTS.getOrDefault(categoryId.getPath(), DEFAULT_LAYOUT);
    }

    public static Component titleEmi(Identifier categoryId) {
        return Component.translatable("emi.category." + categoryId.getNamespace() + "." + categoryId.getPath());
    }

    public static Component titleJei(Identifier categoryId) {
        return Component.translatable(categoryId.getNamespace() + ".machine_" + categoryId.getPath());
    }

    public static void drawJei(FormsCombinedRecipe recipe, GuiGraphicsExtractor graphics, Layout layout) {
        drawDecorations(graphics, layout);
    }

    public static void drawSlot(GuiGraphicsExtractor graphics, int x, int y, boolean isOutput) {
        RenderHelper.render(graphics, x, y, 18, 18, 18, 18, 0, 0, TENConstants.ITEM_SLOT_SMALL);
    }

    public static void drawDecorations(GuiGraphicsExtractor graphics, Layout layout) {
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

    public static void drawTime(GuiGraphicsExtractor graphics, int x, int y, int ticks) {
        Font font = Minecraft.getInstance().font;
        graphics.text(font, String.format("%.1fs", ticks / 20.0d), x, y, 0x555555, false);
    }

    public static void drawFluidTank(GuiGraphicsExtractor graphics, int x, int y, int w, int h, FluidStack stack) {
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

    /**
     * @param chance raw probability in [0,1] range
     * @return percentage as integer (0-100), rounded
     */
    public static int chancePercent(double chance) {
        return (int) Math.round(chance * 100.0d);
    }

    /**
     * Draw slot overlay text (chance percentage and rolls) for JEI item output
     * slots. Only affects ITEM + OUTPUT slots; chance >= 1.0 and rolls <= 1
     * produce no overlay. Text is rendered in the white with a dark shadow for
     * readability over item icons.
     * <ul>
     *   <li>Top-left: chance percentage like {@code "40%"} (when chance &lt; 1)</li>
     *   <li>Bottom-left: rolls like {@code "R9"} (when rolls &gt; 1)</li>
     * </ul>
     * The right-bottom ItemStack count rendered by JEI is not modified.
     */
    public static void drawSlotOverlays(FormsCombinedRecipe recipe, GuiGraphicsExtractor graphics, Layout layout) {
        Font font = Minecraft.getInstance().font;
        for (var slot : layout.slots()) {
            if (slot.kind() != SlotKind.ITEM || slot.role() != SlotRole.OUTPUT) continue;
            var ingredient = ingredientFor(recipe, slot);
            if (ingredient == null) continue;

            String chanceText = ingredient.chanceOverlayText();
            String rollsText = ingredient.rollsOverlayText();

            // Top-left: chance percentage (e.g., "40%")
            if (chanceText != null) {
                graphics.text(font, chanceText, slot.x() + 1, slot.y() + 1, 0xFFFFFFFF, true);
            }
            // Bottom-left: rolls (e.g., "R9")
            if (rollsText != null) {
                graphics.text(font, rollsText, slot.x() + 1, slot.y() + 10, 0xFFFFFFFF, true);
            }
        }
    }

    /** @deprecated Use {@link #drawSlotOverlays(FormsCombinedRecipe, GuiGraphicsExtractor, Layout)} */
    @Deprecated(forRemoval = false)
    public static void drawSlotOverlay(GuiGraphicsExtractor graphics, int x, int y, @Nullable FormsCombinedIngredient ingredient) {
        if (ingredient == null) return;
        Font font = Minecraft.getInstance().font;
        String chanceText = ingredient.chanceOverlayText();
        String rollsText = ingredient.rollsOverlayText();
        if (chanceText != null) {
            graphics.text(font, chanceText, x + 1, y + 1, 0xFFFFFFFF, true);
        }
        if (rollsText != null) {
            graphics.text(font, rollsText, x + 1, y + 10, 0xFFFFFFFF, true);
        }
    }

    private static void drawProgressDecoration(GuiGraphicsExtractor graphics, DecorationSpec decoration, double percent) {
        RenderHelper.render(graphics, decoration.x(), decoration.y(), decoration.width(), decoration.height(), decoration.width(), decoration.height(), 0, 0, decoration.background());
        int filledWidth = (int) (percent * decoration.width());
        if (filledWidth > 0) {
            RenderHelper.render(graphics, decoration.x(), decoration.y(), filledWidth, decoration.height(), decoration.width(), decoration.height(), 0, 0, decoration.fill());
        }
    }

    private static void drawBurnLeftDecoration(GuiGraphicsExtractor graphics, DecorationSpec decoration, double percent) {
        RenderHelper.render(graphics, decoration.x(), decoration.y(), decoration.width(), decoration.height(), decoration.width(), decoration.height(), 0, 0, decoration.background());
        int hiddenHeight = (int) (decoration.height() * (1.0d - percent));
        int visibleHeight = decoration.height() - hiddenHeight;
        if (visibleHeight > 0) {
            RenderHelper.render(graphics, decoration.x(), decoration.y() + hiddenHeight, decoration.width(), visibleHeight, decoration.width(), decoration.height(), 0, hiddenHeight, decoration.fill());
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
