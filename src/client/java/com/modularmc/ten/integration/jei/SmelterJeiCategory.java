// -*- coding: utf-8 -*-
package com.modularmc.ten.integration.jei;

import com.modularmc.ten.TEN;
import com.modularmc.ten.TENConstants;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
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
 * 布局规则 v5（移除能量条，组件群居中）：组件群左缘=25、右缘=125、总宽 100、
 * 中心 75=面板中心；固定宽 58（输入18+箭头22+输出18），间隔总量 42 均分为
 * 21/21（输入→箭头、箭头→输出），垂直居中：
 * <ul>
 *   <li>Input slot (ITEM_SLOT_SMALL) at (25, 16) — 18×18, vertically centered</li>
 *   <li>Arrow group (PROGRESS_ARROW_SMELTER_BG/FILL) at (64, 17) — 22×16, input right edge 43 + 21</li>
 *   <li>Output slot at (107, 16) — arrow right edge 86 + 21</li>
 *   <li>Cooking time text at (25, 38) — under input slot, left-aligned</li>
 * </ul>
 * 组件群右缘 = OUTPUT_X+18 = 125，中心 = (25+125)/2 = 75 = 面板中心。
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

    // Progress arrow animation — 10s cycle growing left→right, matching
    // TENRecipeWidget.progressPercent() on the XEI side.
    private static final long PROGRESS_CYCLE_MS = 10_000L;

    // Layout positions — 18×18 slots vertically centered within the 50px
    // background (y = (50-18)/2 = 16), 规则 v5：移除能量条，组件群居中。
    // 组件群左缘=25、右缘=OUTPUT_X+18=125、总宽 100、中心 75=面板中心。
    // 固定宽 = 18+22+18 = 58，间隔总量 = 100−58 = 42，均分为 21/21：
    // 输入右缘 43 → 箭头 64（+21）；箭头右缘 86 → 输出 107（+21）。
    // Time text aligns with the input slot at (25, 38), bottom 38+9=47 < 50.
    private static final int INPUT_X = 25;
    private static final int INPUT_Y = 16;
    private static final int OUTPUT_X = 107;
    private static final int OUTPUT_Y = 16;
    private static final int ARROW_X = 64;
    private static final int ARROW_Y = 17;
    private static final int ARROW_W = 22;
    private static final int ARROW_H = 16;
    private static final int TEXT_X = 25;
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

        // Draw progress arrow group (background + animated fill)
        drawProgress(graphics);

        // Draw cooking time text (in ticks, matching vanilla convention)
        var font = Minecraft.getInstance().font;
        Component timeText = Component.translatable("kenergyengineering.jei.duration_short", recipe.cookingTime());
        graphics.text(font, timeText, TEXT_X, TEXT_Y, 0xFF404040, false);
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
}
