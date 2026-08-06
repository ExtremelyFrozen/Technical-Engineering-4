// -*- coding: utf-8 -*-
package com.modularmc.ten.integration.jei;

import com.modularmc.ten.TEN;
import com.modularmc.ten.TENConstants;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.gui.widgets.IRecipeWidget;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

/**
 * JEI category for the three Smelter recipe pages (Smelting/Blasting/Smoking).
 * <p>
 * A single reusable category implementation with three independent instances,
 * each backed by a different {@link RecipeType} UID, title, and icon.
 * <p>
 * Layout (150×50, modular JEI_HANDLER_MODULAR top layer at UV 0,0), per
 * 布局规则 v4（组件群居中 + 间距自适应）：能量条左缘 x=8，组件群左缘=8、右缘=142、
 * 总宽 134、中心 75=面板中心；固定宽 72（能量条14+输入18+箭头22+输出18），间隔总量
 * 62 均分为 21/21/20（能量条→输入、输入→箭头、箭头→输出），垂直居中：
 * <ul>
 *   <li>Energy gauge (ENERGY_GAUGE_BG/FILL) at (8, 2), 14×46 — vertically centered</li>
 *   <li>Input slot (ITEM_SLOT_SMALL) at (43, 16) — 18×18, vertically centered, gap 21px from gauge right edge 22</li>
 *   <li>Output slot at (124, 16) — arrow right edge 104 + 20</li>
 *   <li>Arrow group (PROGRESS_ARROW_SMELTER_BG/FILL) at (82, 17) — 22×16, input right edge 61 + 21</li>
 *   <li>Cooking time text at (43, 38) — under input slot, left-aligned</li>
 * </ul>
 * 组件群右缘 = OUTPUT_X+18 = 142，中心 = (8+142)/2 = 75 = 面板中心。
 */
public class SmelterJeiCategory implements IRecipeCategory<SmelterJeiCategory.Recipe> {

    /**
     * Payload record for the three smelter JEI recipe categories.
     * <p>
     * Wraps a vanilla cooking recipe into a client-safe form: preserves all
     * ingredient candidates via {@link Ingredient#getValues()}, a copied output
     * stack, and the original cooking time.
     * <p>
     * Client-only — not serialized, not registered as a recipe type.
     */
    public record Recipe(
            List<ItemStack> inputs,
            ItemStack output,
            int cookingTime
    ) {
        /**
         * Create a Recipe from a vanilla Ingredient, output, and time.
         * Ingredient items are resolved via {@link Ingredient#getValues()} to
         * avoid the deprecated {@link Ingredient#items()} method.
         *
         * @return a new Recipe, or null if output is empty
         */
        public static Recipe of(Ingredient ingredient, ItemStack output, int cookingTime) {
            if (output.isEmpty()) return null;
            var inputs = StreamSupport.stream(
                    Spliterators.spliteratorUnknownSize(
                            ingredient.getValues().iterator(),
                            Spliterator.ORDERED),
                    false)
                    .map(holder -> new ItemStack(holder))
                    .filter(s -> !s.isEmpty())
                    .toList();
            if (inputs.isEmpty()) return null;
            return new Recipe(inputs, output.copy(), cookingTime);
        }
    }

    private static final int WIDTH = 150;
    private static final int HEIGHT = 50;

    // Energy gauge (14×46 vertical bar, modular ENERGY_GAUGE_BG/FILL) —
    // 规则 v4：左缘 x=8（距面板左缘 8px），垂直居中 y = (50-46)/2 = 2；
    // 右缘 8+14=22；组件群左缘=8、右缘=142、中心 75=面板中心。
    private static final int BURN_LEFT_X = 8;
    private static final int BURN_LEFT_Y = 2;
    private static final int BURN_LEFT_W = 14;
    private static final int BURN_LEFT_H = 46;
    private static final long BURN_CYCLE_MS = 25_000L;
    // Progress arrow animation — 10s cycle growing left→right, matching
    // TENRecipeWidget.progressPercent() on the XEI side.
    private static final long PROGRESS_CYCLE_MS = 10_000L;

    // Layout positions — 18×18 slots vertically centered within the 50px
    // background (y = (50-18)/2 = 16), 规则 v4：组件群居中 + 间距自适应。
    // 组件群左缘=8（能量条左缘）、右缘=OUTPUT_X+18=142、总宽 134、中心 75=面板中心。
    // 固定宽 = 14+18+22+18 = 72，间隔总量 = 134−72 = 62，均分为 21/21/20：
    // 能量条右缘 22 → 输入 43（+21）；输入右缘 61 → 箭头 82（+21）；
    // 箭头右缘 104 → 输出 124（+20）。Time text aligns with the input slot
    // at (43, 38), bottom 38+9=47 < 50.
    private static final int INPUT_X = 43;
    private static final int INPUT_Y = 16;
    private static final int OUTPUT_X = 124;
    private static final int OUTPUT_Y = 16;
    private static final int ARROW_X = 82;
    private static final int ARROW_Y = 17;
    private static final int ARROW_W = 22;
    private static final int ARROW_H = 16;
    private static final int TEXT_X = 43;
    private static final int TEXT_Y = 38;

    private final RecipeType<Recipe> recipeType;
    private final Component title;
    private final IDrawable icon;
    private final IDrawable background;
    private final IDrawable inputSlot;

    /**
     * @param helper    JEI gui helper
     * @param uid       unique category identifier (e.g. {@code smelter_smelting})
     * @param type      the JEI recipe type for this category
     * @param iconStack the item stack used as the category icon
     */
    public SmelterJeiCategory(IGuiHelper helper, Identifier uid, RecipeType<Recipe> type, ItemStack iconStack) {
        this.recipeType = type;
        this.title = Component.translatable(TEN.MOD_ID + ".jei.category." + uid.getPath());
        this.icon = helper.createDrawableItemStack(iconStack);
        // Modular top layer (150×50) of jei_handler.png at UV 0,0
        this.background = helper.createDrawable(TENConstants.JEI_HANDLER_MODULAR, 0, 0, WIDTH, HEIGHT);
        // Modular 18×18 slot background replacing JEI's default slot drawable.
        // NOTE: createDrawable 按 256×256 atlas 语义解释 UV；18×18 素材必须声明实际尺寸
        // （setTextureSize），否则只渲染纹理左上角区域并被放大。
        this.inputSlot = helper.drawableBuilder(TENConstants.ITEM_SLOT_SMALL, 0, 0, 18, 18).setTextureSize(18, 18).build();
    }

    @Override
    public RecipeType<Recipe> getRecipeType() {
        return recipeType;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Recipe recipe, IFocusGroup focuses) {
        // Input slot — all ingredient candidates preserved
        var inputSlotBuilder = builder.addSlot(RecipeIngredientRole.INPUT, INPUT_X, INPUT_Y)
                .setBackground(inputSlot, -1, -1);
        for (ItemStack stack : recipe.inputs()) {
            if (!stack.isEmpty()) {
                inputSlotBuilder.addItemStack(stack);
            }
        }

        // Output slot — exact result
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, OUTPUT_Y)
                .setBackground(inputSlot, -1, -1)
                .addItemStack(recipe.output());
    }

    @Override
    public void draw(Recipe recipe, IRecipeSlotsView slotsView, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        // Draw background
        background.draw(graphics);

        // Draw burnLeft energy gauge (background + animated fill)
        drawBurnLeft(graphics);

        // Draw progress arrow group (background + animated fill)
        drawProgress(graphics);

        // Draw cooking time text (in ticks, matching vanilla convention)
        var font = Minecraft.getInstance().font;
        Component timeText = Component.translatable("kenergyengineering.jei.duration_short", recipe.cookingTime());
        graphics.text(font, timeText, TEXT_X, TEXT_Y, 0xFF404040, false);
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, Recipe recipe, IFocusGroup focuses) {
        builder.addWidget(new IRecipeWidget() {
            @Override
            public ScreenPosition getPosition() {
                return new ScreenPosition(BURN_LEFT_X, BURN_LEFT_Y);
            }

            @Override
            public void drawWidget(GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
                // No additional rendering — gauge is drawn in draw() alongside
                // background / arrow / time text for correct z-order.
            }

            @Override
            public void getTooltip(ITooltipBuilder tooltipBuilder, double mouseX, double mouseY) {
                if (mouseX >= BURN_LEFT_X && mouseX < BURN_LEFT_X + BURN_LEFT_W
                        && mouseY >= BURN_LEFT_Y && mouseY < BURN_LEFT_Y + BURN_LEFT_H) {
                    tooltipBuilder.add(Component.translatable("kenergyengineering.jei.base_rate_short", 15));
                }
            }
        });
    }

    /**
     * Draws the 22×16 progress arrow group from modular
     * PROGRESS_ARROW_SMELTER_BG/FILL with an animated fill that grows left→right
     * over a 10s cycle, matching {@code TENRecipeWidget.drawProgressDecoration}
     * on the XEI side so all three smelter pages share the same arrow behavior.
     */
    private void drawProgress(GuiGraphicsExtractor graphics) {
        // Background (empty state) — PROGRESS_ARROW_SMELTER_BG is a full 22×16 texture
        graphics.blit(RenderPipelines.GUI_TEXTURED, TENConstants.PROGRESS_ARROW_SMELTER_BG,
                ARROW_X, ARROW_Y, 0.0F, 0.0F,
                ARROW_W, ARROW_H, ARROW_W, ARROW_H);

        // Animated fill (growing left → right) — PROGRESS_ARROW_SMELTER_FILL is a
        // full 22×16 texture; crop the not-yet-filled right portion via width.
        int filledWidth = (int) (progressPercent() * ARROW_W);
        if (filledWidth > 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TENConstants.PROGRESS_ARROW_SMELTER_FILL,
                    ARROW_X, ARROW_Y, 0.0F, 0.0F,
                    filledWidth, ARROW_H, ARROW_W, ARROW_H);
        }
    }

    /**
     * @return a value in [0, 1] increasing over a 10-second cycle (0 → 1),
     *         simulating smelting progress for visual animation.
     */
    private static double progressPercent() {
        return (System.currentTimeMillis() % PROGRESS_CYCLE_MS) / (double) PROGRESS_CYCLE_MS;
    }

    /**
     * Draws the 14×46 vertical energy gauge from modular ENERGY_GAUGE_BG/FILL
     * with animated fill (depleting top-to-bottom over a 25s cycle), matching
     * the machine decoration style used by
     * {@code TENRecipeWidget.drawBurnLeftDecoration}.
     */
    private void drawBurnLeft(GuiGraphicsExtractor graphics) {
        // Gauge background (empty state) — ENERGY_GAUGE_BG is a full 14×46 texture
        graphics.blit(RenderPipelines.GUI_TEXTURED, TENConstants.ENERGY_GAUGE_BG,
                BURN_LEFT_X, BURN_LEFT_Y, 0.0F, 0.0F,
                BURN_LEFT_W, BURN_LEFT_H, BURN_LEFT_W, BURN_LEFT_H);

        // Animated fill (depleting from top → bottom) — ENERGY_GAUGE_FILL is a
        // full 14×46 texture; crop the top-hidden portion via UV v offset.
        double percent = burnPercent();
        int hiddenHeight = (int) (BURN_LEFT_H * (1.0d - percent));
        int visibleHeight = BURN_LEFT_H - hiddenHeight;
        if (visibleHeight > 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TENConstants.ENERGY_GAUGE_FILL,
                    BURN_LEFT_X, BURN_LEFT_Y + hiddenHeight,
                    0.0F, (float) hiddenHeight,
                    BURN_LEFT_W, visibleHeight, BURN_LEFT_W, BURN_LEFT_H);
        }
    }

    /**
     * @return a value in [0, 1] decreasing over a 25-second cycle (1 → 0),
     *         simulating energy depletion for visual animation.
     */
    private static double burnPercent() {
        return 1.0d - ((System.currentTimeMillis() % BURN_CYCLE_MS) / (double) BURN_CYCLE_MS);
    }
}
