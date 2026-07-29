// -*- coding: utf-8 -*-
package com.modularmc.ten.integration.jei;

import com.modularmc.ten.TEN;
import com.modularmc.ten.TENConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
 * Layout (116×64):
 * <ul>
 *   <li>Input slot at (11, 23)</li>
 *   <li>Output slot at (87, 23)</li>
 *   <li>Flame/progress icon from GUI_HANDLER at (39, 24)</li>
 *   <li>Cooking time text below the arrow area</li>
 * </ul>
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

    private static final int WIDTH = 116;
    private static final int HEIGHT = 64;

    // Layout positions
    private static final int INPUT_X = 11;
    private static final int INPUT_Y = 23;
    private static final int OUTPUT_X = 87;
    private static final int OUTPUT_Y = 23;
    private static final int ARROW_X = 49;
    private static final int ARROW_Y = 27;
    private static final int ARROW_W = 22;
    private static final int ARROW_U = 27;
    private static final int ARROW_V = 0;
    private static final int TEXT_X = 8;
    private static final int TEXT_Y = 52;

    private final RecipeType<Recipe> recipeType;
    private final Component title;
    private final IDrawable icon;
    private final IDrawable background;
    private final IDrawable inputSlot;
    private final IDrawable arrow;

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
        this.background = helper.createDrawable(TENConstants.JEI_HANDLER_2, 0, 106, WIDTH, HEIGHT);
        this.inputSlot = helper.getSlotDrawable();
        // Arrow from GUI_HANDLER (same progress arrow used by other TEN categories)
        this.arrow = helper.createDrawable(TENConstants.GUI_HANDLER, ARROW_U, ARROW_V, ARROW_W, 16);
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

        // Draw arrow (progress indicator — static in JEI category view)
        arrow.draw(graphics, ARROW_X, ARROW_Y);

        // Draw cooking time text (in ticks, matching vanilla convention)
        var font = Minecraft.getInstance().font;
        Component timeText = Component.translatable("kenergyengineering.jei.duration_short", recipe.cookingTime());
        graphics.text(font, timeText, TEXT_X, TEXT_Y, 0xFF404040, false);
    }
}
