package com.modularmc.ten.integration.jei;

import com.modularmc.ten.TEN;
import com.modularmc.ten.TENConstants;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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

/**
 * JEI category for the three Smelter recipe pages (Smelting/Blasting/Smoking).
 * <p>
 * 1.21.1 适配：GuiGraphics 替代 GuiGraphicsExtractor，ResourceLocation 替代 Identifier，
 * Ingredient.items() 替代 getValues()。
 */
public class SmelterJeiCategory implements IRecipeCategory<SmelterJeiCategory.Recipe> {

    public record Recipe(
            List<ItemStack> inputs,
            ItemStack output,
            int cookingTime
    ) {
        public static Recipe of(Ingredient ingredient, ItemStack output, int cookingTime) {
            if (output.isEmpty()) return null;
            var inputs = List.of(ingredient.getItems());
            var nonEmpty = inputs.stream().filter(s -> !s.isEmpty()).toList();
            if (nonEmpty.isEmpty()) return null;
            return new Recipe(nonEmpty, output.copy(), cookingTime);
        }
    }

    private static final int WIDTH = 150;
    private static final int HEIGHT = 50;
    private static final long PROGRESS_CYCLE_MS = 10_000L;

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

    public SmelterJeiCategory(IGuiHelper helper, ResourceLocation uid, RecipeType<Recipe> type, ItemStack iconStack) {
        this.recipeType = type;
        this.title = Component.translatable(TEN.MOD_ID + ".jei.category." + uid.getPath());
        this.icon = helper.createDrawableItemStack(iconStack);
        this.background = helper.createDrawable(TENConstants.JEI_RECIPE_BG, 0, 0, WIDTH, HEIGHT);
        this.inputSlot = helper.drawableBuilder(TENConstants.ITEM_SLOT_SMALL, 0, 0, 18, 18).setTextureSize(18, 18).build();
    }

    @Override
    public RecipeType<Recipe> getRecipeType() { return recipeType; }

    @Override
    public Component getTitle() { return title; }

    @Override
    public int getWidth() { return WIDTH; }

    @Override
    public int getHeight() { return HEIGHT; }

    @Override
    public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Recipe recipe, IFocusGroup focuses) {
        var inputSlotBuilder = builder.addSlot(RecipeIngredientRole.INPUT, INPUT_X, INPUT_Y)
                .setBackground(inputSlot, -1, -1);
        for (ItemStack stack : recipe.inputs()) {
            if (!stack.isEmpty()) inputSlotBuilder.addItemStack(stack);
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, OUTPUT_Y)
                .setBackground(inputSlot, -1, -1)
                .addItemStack(recipe.output());
    }

    @Override
    public void draw(Recipe recipe, IRecipeSlotsView slotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        background.draw(graphics);

        // Draw progress arrow background + animated fill
        var pose = graphics.pose();
        pose.pushPose();
        // 1.21.1 blit signature: blit(ResourceLocation, x, y, u, v, width, height, textureWidth, textureHeight)
        graphics.blit(TENConstants.PROGRESS_ARROW_SMELTER_BG, ARROW_X, ARROW_Y, 0, 0, ARROW_W, ARROW_H, ARROW_W, ARROW_H);
        int filledWidth = (int) (progressPercent() * ARROW_W);
        if (filledWidth > 0) {
            graphics.blit(TENConstants.PROGRESS_ARROW_SMELTER_FILL, ARROW_X, ARROW_Y, 0, 0, filledWidth, ARROW_H, ARROW_W, ARROW_H);
        }
        pose.popPose();

        // Cooking time text
        var font = Minecraft.getInstance().font;
        Component timeText = Component.translatable("kenergyengineering.jei.duration_short", recipe.cookingTime());
        graphics.drawString(font, timeText, TEXT_X, TEXT_Y, 0xFF404040, false);
    }

    private static double progressPercent() {
        return (System.currentTimeMillis() % PROGRESS_CYCLE_MS) / (double) PROGRESS_CYCLE_MS;
    }
}