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
        PROGRESS
    }

    public record SlotSpec(SlotKind kind, SlotRole role, int recipeIndex, int x, int y, int width, int height) {

        public static SlotSpec item(SlotRole role, int recipeIndex, int x, int y) {
            return new SlotSpec(SlotKind.ITEM, role, recipeIndex, x, y, 18, 18);
        }

        public static SlotSpec fluid(SlotRole role, int recipeIndex, int x, int y) {
            return new SlotSpec(SlotKind.FLUID, role, recipeIndex, x, y, 18, 50);
        }
    }

    public record DecorationSpec(DecorationKind kind, int x, int y, int width, int height, ResourceLocation bgTexture, ResourceLocation fillTexture) {

        /** 进度箭头：独立背景/填充素材（modular 素材族）。 */
        public static DecorationSpec progress(int x, int y, ResourceLocation bgTexture, ResourceLocation fillTexture) {
            return new DecorationSpec(DecorationKind.PROGRESS, x, y, 22, 16, bgTexture, fillTexture);
        }
    }

    public record Layout(ResourceLocation background, int u, int v, int width, int height,
                         List<SlotSpec> slots, List<DecorationSpec> decorations) {}

    private static final long PROGRESS_CYCLE_MS = 10_000L;

    /**
     * 左下角工作时长标签位置（150×50 面板，对齐熔炼机 TEXT(25,38)）：
     * 默认 (25,38)；compressor/refiner 底部左侧被槽位（25..43 列 / fluid 竖槽 16..34）占用，
     * 右移至槽位边界之后。引擎兼容层不调用本位置表（用户设定：引擎不显示时间）。
     */
    private static final Map<String, int[]> TIME_LABEL_POS = Map.of(
            "compressor", new int[] { 46, 38 },
            "refiner", new int[] { 46, 38 });
    private static final int[] DEFAULT_TIME_POS = { 25, 38 };

    /** 指定分类的左下角时间标签位置 {x, y}。 */
    public static int[] timeLabelPos(String categoryPath) {
        return TIME_LABEL_POS.getOrDefault(categoryPath, DEFAULT_TIME_POS);
    }

    /**
     * 左下角工作消耗时间文本（对齐熔炼机兼容层：duration_short lang 键、深灰 0xFF404040、无阴影）。
     * 由 JEI（TENJeiCategory.draw）与 EMI（TENEmiRecipe addDrawable）共用；引擎兼容层不调用。
     */
    public static void drawDurationText(GuiGraphics graphics, int x, int y, int ticks) {
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font,
                Component.translatable("kenergyengineering.jei.duration_short", ticks),
                x, y, 0xFF404040, false);
    }

    /**
     * 布局规则 v5（移除能量条，组件群居中 + 间距自适应）：
     * <ol>
     * <li>不再绘制能量条（ENERGY_GAUGE 已从 xei 界面移除）</li>
     * <li>组件群中心 = 75 = 面板中心（150×50），左右对称</li>
     * <li>固定宽 = 输入组宽 + 箭头22 + 输出组宽；间隔总量均分到 2 个间隔</li>
     * <li>连续同种槽（多输入/多输出组）算一个整体：组内紧贴</li>
     * <li>垂直方向组件组在面板 50 高内居中：18×18 槽 y=16、箭头 y=17、竖排/2×2 组 y=7、fluid y=0</li>
     * </ol>
     * 底图统一 JEI_RECIPE_BG（LDLib2 modular 素材族），进度箭头按机器映射独立背景/填充素材。
     */
    private static final Layout DEFAULT_LAYOUT = new Layout(
            TENConstants.JEI_RECIPE_BG, 0, 0, 150, 50,
            List.of(
                    SlotSpec.item(SlotRole.INPUT, 0, 22, 16),
                    SlotSpec.item(SlotRole.OUTPUT, 0, 92, 7),
                    SlotSpec.item(SlotRole.OUTPUT, 1, 110, 7),
                    SlotSpec.item(SlotRole.OUTPUT, 2, 92, 25),
                    SlotSpec.item(SlotRole.OUTPUT, 3, 110, 25)),
            List.of(
                    DecorationSpec.progress(55, 17, TENConstants.PROGRESS_ARROW_SMELTER_BG, TENConstants.PROGRESS_ARROW_SMELTER_FILL)));

    private static final Map<String, Layout> LAYOUTS = buildLayouts();

    private static Map<String, Layout> buildLayouts() {
        // 布局规则 v5：150×50 JEI_RECIPE_BG，无能量条，组件居中
        return Map.of(
                "pulverizer", new Layout(
                        TENConstants.JEI_RECIPE_BG, 0, 0, 150, 50,
                        List.of(
                                SlotSpec.item(SlotRole.INPUT, 0, 22, 16),
                                SlotSpec.item(SlotRole.OUTPUT, 0, 92, 7),
                                SlotSpec.item(SlotRole.OUTPUT, 1, 110, 7),
                                SlotSpec.item(SlotRole.OUTPUT, 2, 92, 25),
                                SlotSpec.item(SlotRole.OUTPUT, 3, 110, 25)),
                        List.of(
                                DecorationSpec.progress(55, 17, TENConstants.PROGRESS_ARROW_PULVERIZER_BG, TENConstants.PROGRESS_ARROW_PULVERIZER_FILL))),
                "compressor", new Layout(
                        TENConstants.JEI_RECIPE_BG, 0, 0, 150, 50,
                        List.of(
                                SlotSpec.item(SlotRole.INPUT, 0, 25, 7),
                                SlotSpec.item(SlotRole.INPUT, 1, 25, 25),
                                SlotSpec.item(SlotRole.OUTPUT, 0, 107, 16)),
                        List.of(
                                DecorationSpec.progress(64, 17, TENConstants.PROGRESS_ARROW_COMPRESSOR_BG, TENConstants.PROGRESS_ARROW_COMPRESSOR_FILL))),
                "refiner", new Layout(
                        TENConstants.JEI_RECIPE_BG, 0, 0, 150, 50,
                        List.of(
                                SlotSpec.fluid(SlotRole.INPUT, 0, 16, 0),
                                SlotSpec.item(SlotRole.INPUT, 0, 38, 16),
                                SlotSpec.item(SlotRole.OUTPUT, 0, 94, 16),
                                SlotSpec.fluid(SlotRole.OUTPUT, 0, 116, 0)),
                        List.of(
                                DecorationSpec.progress(64, 17, TENConstants.PROGRESS_ARROW_REFINER_BG, TENConstants.PROGRESS_ARROW_REFINER_FILL))),
                "induction_furnace", new Layout(
                        TENConstants.JEI_RECIPE_BG, 0, 0, 150, 50,
                        List.of(
                                SlotSpec.item(SlotRole.INPUT, 0, 20, 16),
                                SlotSpec.item(SlotRole.INPUT, 1, 38, 16),
                                SlotSpec.item(SlotRole.INPUT, 2, 56, 16),
                                SlotSpec.item(SlotRole.OUTPUT, 0, 112, 16)),
                        List.of(
                                DecorationSpec.progress(82, 17, TENConstants.PROGRESS_ARROW_INDUCTION_FURNACE_BG, TENConstants.PROGRESS_ARROW_INDUCTION_FURNACE_FILL))),
                "psionicant", new Layout(
                        TENConstants.JEI_RECIPE_BG, 0, 0, 150, 50,
                        List.of(
                                SlotSpec.item(SlotRole.INPUT, 0, 23, 16),
                                SlotSpec.item(SlotRole.INPUT, 1, 41, 16),
                                SlotSpec.item(SlotRole.OUTPUT, 0, 109, 16)),
                        List.of(
                                DecorationSpec.progress(73, 17, TENConstants.PROGRESS_ARROW_PSIONICANT_BG, TENConstants.PROGRESS_ARROW_PSIONICANT_FILL))));
    }

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

    /**
     * 在配方布局的所有 ITEM+OUTPUT 槽位上绘制概率/掷骰覆盖层。
     * <ul>
     * <li>左上：概率百分比（如 "40%"，chance &lt; 1 时显示）</li>
     * <li>左下：掷骰次数（如 "R9"，rolls &gt; 1 时显示）</li>
     * </ul>
     * JEI 侧经 TENJeiSlotOverlay 由 JEI 槽位管线绘制（物品之后，z 序正确）；
     * EMI 侧无槽位 overlay 管线，由 TENEmiRecipe 经 addDrawable 调本方法。
     * EMI 渲染机制（1.1.22 源码）：widget 按插入顺序绘制，层级由 Z 深度裁决——
     * DrawableWidget 默认 Z=0 且深度测试可能被关闭，退化为插入顺序时文本先于后绘物品。
     * 对齐 EMI 官方 overlay 惯例（EmiRenderHelper.renderAmount）：绘制内 translate(0,0,300)
     * + enableDepthTest，确保覆盖层稳定位于物品（Z≈150）之上。
     * 视觉坐标对齐 JEI 挂载（X_OFFSET=-1/Y_OFFSET=-1 + CHANCE=-1/ROLLS=11）：
     * chance 绝对 slot.y-2、rolls 绝对 slot.y+10。
     */
    public static void drawSlotOverlays(FormsCombinedRecipe recipe, GuiGraphics graphics, Layout layout) {
        Font font = Minecraft.getInstance().font;
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(0, 0, 300);
        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
        for (var slot : layout.slots()) {
            if (slot.kind() != SlotKind.ITEM || slot.role() != SlotRole.OUTPUT) continue;
            var ingredient = ingredientFor(recipe, slot);
            if (ingredient == null) continue;

            String chanceText = ingredient.chanceOverlayText();
            String rollsText = ingredient.rollsOverlayText();

            // Top-left: chance percentage (e.g., "40%") at 75% scale
            if (chanceText != null) {
                drawScaledText(graphics, font, chanceText, slot.x() + 1, slot.y() - 2);
            }
            // Bottom-left: rolls (e.g., "R9") at 75% scale
            if (rollsText != null) {
                drawScaledText(graphics, font, rollsText, slot.x() + 1, slot.y() + 10);
            }
        }
        com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
        pose.popPose();
    }

    /** 75% 缩放文本绘制（对齐 TENJeiSlotOverlay.TEXT_SCALE，"100%"/"R999" 限宽 18px 槽内）。 */
    private static void drawScaledText(GuiGraphics graphics, Font font, String text, int x, int y) {
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale(0.75f, 0.75f, 1.0f);
        graphics.drawString(font, text, 0, 0, 0xFFFFFFFF, true);
        pose.popPose();
    }

    public static void drawSlot(GuiGraphics graphics, int x, int y, boolean isOutput) {
        graphics.blit(TENConstants.LEGACY_SHEET, x, y, isOutput ? 24 : 0, 0, 24, 24, 256, 256);
    }

    public static void drawDecorations(GuiGraphics graphics, Layout layout) {
        for (var decoration : layout.decorations()) {
            switch (decoration.kind()) {
                case PROGRESS -> drawProgressDecoration(graphics, decoration, progressPercent());
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

    /**
     * @param chance raw probability in [0,1] range
     * @return percentage as integer (0-100), rounded
     */
    public static int chancePercent(double chance) {
        return (int) Math.round(chance * 100.0d);
    }

    /**
     * 进度箭头：独立背景/填充素材（modular 族），填充从左到右裁切。
     */
    private static void drawProgressDecoration(GuiGraphics graphics, DecorationSpec decoration, double percent) {
        RenderHelper.render(graphics, decoration.x(), decoration.y(), decoration.width(), decoration.height(), decoration.width(), decoration.height(), 0, 0, decoration.bgTexture());
        int filledWidth = (int) (percent * decoration.width());
        if (filledWidth > 0) {
            RenderHelper.render(graphics, decoration.x(), decoration.y(), filledWidth, decoration.height(), decoration.width(), decoration.height(), 0, 0, decoration.fillTexture());
        }
    }

    private static double progressPercent() {
        return (System.currentTimeMillis() % PROGRESS_CYCLE_MS) / (double) PROGRESS_CYCLE_MS;
    }

    private static boolean contains(int x, int y, int width, int height, int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
