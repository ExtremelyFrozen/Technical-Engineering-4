package com.modularmc.ten.integration.jei;

import com.modularmc.ten.TENConstants;
import com.modularmc.ten.common.blockentity.EngineFuelRecipe;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

/**
 * JEI category for engine fuel recipes (Extractor, Metalizer, Biomass).
 * <p>
 * Reuses existing {@link TENConstants#JEI_HANDLER_1} background and
 * {@link TENConstants#GUI_HANDLER} burn icon — no new textures.
 * <p>
 * Layout (150x50):
 * <ul>
 *   <li>Fuel input slot at (10, 17)</li>
 *   <li>Burn icon (GUI_HANDLER u=14, v=26, 13x13) at (37, 19)</li>
 *   <li>Three compact value lines starting at x≈58: y=8, y=21, y=34</li>
 * </ul>
 * Detailed fuel budget, base rate, duration and total energy are shown
 * in rich tooltips on the input slot (localized via lang keys).
 */
public class EngineFuelCategory implements IRecipeCategory<EngineFuelRecipe> {

    private static final int WIDTH = 150;
    private static final int HEIGHT = 50;

    // Layout constants
    private static final int INPUT_SLOT_X = 10;
    private static final int INPUT_SLOT_Y = 17;
    private static final int BURN_ICON_X = 37;
    private static final int BURN_ICON_Y = 19;
    private static final int BURN_ICON_W = 13;
    private static final int BURN_ICON_H = 13;
    // Burn icon UV on GUI_HANDLER: u=14, v=26 (engine burning icon area)
    private static final int BURN_U = 14;
    private static final int BURN_V = 26;

    // Text position — compact info lines at right of burn icon
    private static final int TEXT_X = 58;
    private static final int TEXT_LINE1_Y = 8;
    private static final int TEXT_LINE2_Y = 21;
    private static final int TEXT_LINE3_Y = 34;

    private final RecipeType<EngineFuelRecipe> recipeType;
    private final Component title;
    private final IDrawable icon;
    private final IDrawable background;
    private final IDrawable inputSlot;
    private final IDrawable burnIcon;

    public EngineFuelCategory(IGuiHelper helper, Identifier categoryId,
                              RecipeType<EngineFuelRecipe> type, ItemStack iconStack,
                              Component title) {
        this.recipeType = type;
        this.title = title;
        this.icon = helper.createDrawableItemStack(iconStack);
        this.background = helper.createDrawable(TENConstants.JEI_HANDLER_1, 0, 0, WIDTH, HEIGHT);
        this.inputSlot = helper.getSlotDrawable();
        // Burn icon from GUI_HANDLER: engine burning icon at u=14, v=26 (13x13)
        this.burnIcon = helper.createDrawable(TENConstants.GUI_HANDLER, BURN_U, BURN_V, BURN_ICON_W, BURN_ICON_H);
    }

    @Override
    public RecipeType<EngineFuelRecipe> getRecipeType() {
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
    public void setRecipe(IRecipeLayoutBuilder builder, EngineFuelRecipe recipe, IFocusGroup focuses) {
        // Input slot — the fuel item (count always 1)
        var slot = builder.addSlot(RecipeIngredientRole.INPUT, INPUT_SLOT_X, INPUT_SLOT_Y)
                .setBackground(inputSlot, -1, -1)
                .addItemStacks(recipe.ingredients());

        // Rich tooltip: detailed fuel info
        slot.addRichTooltipCallback((view, tooltip) -> {
            tooltip.add(Component.translatable("kenergyengineering.jei.base_output", recipe.fuelBudget()));
            tooltip.add(Component.translatable("kenergyengineering.jei.base_rate", recipe.baseRate()));
            tooltip.add(Component.translatable("kenergyengineering.jei.duration_ticks", recipe.durationTicks()));
            tooltip.add(Component.translatable("kenergyengineering.jei.total_energy", recipe.baseGeneratedEnergy()));
        });
    }

    @Override
    public void draw(EngineFuelRecipe recipe, IRecipeSlotsView slotsView, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        // Draw background (JEI_HANDLER_1)
        background.draw(graphics);

        // Draw burn icon (GUI_HANDLER)
        burnIcon.draw(graphics, BURN_ICON_X, BURN_ICON_Y);

        // Compact value lines using GuiGraphicsExtractor.text()
        var font = Minecraft.getInstance().font;
        int textColor = 0xFF404040;

        // Line 1: %s FE/t  (base generation rate)
        graphics.text(font, Component.translatable("kenergyengineering.jei.base_rate_short", recipe.baseRate()),
                TEXT_X, TEXT_LINE1_Y, textColor, false);

        // Line 2: %s ticks (duration)
        graphics.text(font, Component.translatable("kenergyengineering.jei.duration_short", recipe.durationTicks()),
                TEXT_X, TEXT_LINE2_Y, textColor, false);

        // Line 3: %s FE (total generated energy)
        graphics.text(font, Component.translatable("kenergyengineering.jei.total_short", recipe.baseGeneratedEnergy()),
                TEXT_X, TEXT_LINE3_Y, textColor, false);
    }
}
