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
 * Uses the modular {@link TENConstants#JEI_HANDLER_MODULAR} upper layer
 * (150x50) background and the {@link TENConstants#FUEL_GAUGE_BG} /
 * {@link TENConstants#FUEL_GAUGE_FILL} 13x13 fuel gauge (this page is the
 * only JEI-side consumer of the fuel_gauge sprites; machine GUIs reuse the
 * same sprites via fuelGaugeModular in TENMachineBlockUIFactory).
 * <p>
 * Layout (150x50):
 * <ul>
 *   <li>Fuel input slot (ITEM_SLOT_SMALL 18x18) at (10, 16), vertically centered</li>
 *   <li>Fuel gauge (13x13) at (37, 19), vertically centered</li>
 *   <li>Three compact value lines starting at x=58: y=8, y=21, y=34</li>
 * </ul>
 * Detailed fuel budget, base rate, duration and total energy are shown
 * in rich tooltips on the input slot (localized via lang keys).
 */
public class EngineFuelCategory implements IRecipeCategory<EngineFuelRecipe> {

    private static final int WIDTH = 150;
    private static final int HEIGHT = 50;

    // Layout constants
    private static final int INPUT_SLOT_X = 10;
    // Slot is 18x18; (50 - 18) / 2 = 16 keeps it vertically centered in 50px height
    private static final int INPUT_SLOT_Y = 16;
    private static final int FUEL_GAUGE_X = 37;
    // Gauge is 13x13; (50 - 13) / 2 = 18.5 -> 19 keeps it vertically centered
    private static final int FUEL_GAUGE_Y = 19;
    private static final int FUEL_GAUGE_W = 13;
    private static final int FUEL_GAUGE_H = 13;
    // Fuel gauge sprites are full 13x13, no UV offset needed
    private static final int FUEL_GAUGE_U = 0;
    private static final int FUEL_GAUGE_V = 0;

    // Text position — compact info lines at right of fuel gauge
    private static final int TEXT_X = 58;
    private static final int TEXT_LINE1_Y = 8;
    private static final int TEXT_LINE2_Y = 21;
    private static final int TEXT_LINE3_Y = 34;

    private final RecipeType<EngineFuelRecipe> recipeType;
    private final Component title;
    private final IDrawable icon;
    private final IDrawable background;
    private final IDrawable inputSlot;
    private final IDrawable fuelGaugeBg;
    private final IDrawable fuelGaugeFill;

    public EngineFuelCategory(IGuiHelper helper, Identifier categoryId,
                              RecipeType<EngineFuelRecipe> type, ItemStack iconStack,
                              Component title) {
        this.recipeType = type;
        this.title = title;
        this.icon = helper.createDrawableItemStack(iconStack);
        // Modular background upper layer (150x50)
        this.background = helper.createDrawable(TENConstants.JEI_HANDLER_MODULAR, 0, 0, WIDTH, HEIGHT);
        // NOTE: createDrawable 按 256×256 atlas 语义解释 UV；18×18 槽位与 13×13 燃料表素材
        // 必须经 drawableBuilder().setTextureSize(实际尺寸) 声明，否则只渲染左上角区域。
        this.inputSlot = helper.drawableBuilder(TENConstants.ITEM_SLOT_SMALL, 0, 0, 18, 18).setTextureSize(18, 18).build();
        // Fuel gauge: background frame + full fill (static recipe preview, no runtime level)
        this.fuelGaugeBg = helper.drawableBuilder(TENConstants.FUEL_GAUGE_BG, FUEL_GAUGE_U, FUEL_GAUGE_V, FUEL_GAUGE_W, FUEL_GAUGE_H).setTextureSize(FUEL_GAUGE_W, FUEL_GAUGE_H).build();
        this.fuelGaugeFill = helper.drawableBuilder(TENConstants.FUEL_GAUGE_FILL, FUEL_GAUGE_U, FUEL_GAUGE_V, FUEL_GAUGE_W, FUEL_GAUGE_H).setTextureSize(FUEL_GAUGE_W, FUEL_GAUGE_H).build();
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
        // Draw background (JEI_HANDLER_MODULAR upper layer 150x50)
        background.draw(graphics);

        // Draw fuel gauge (fuel_gauge background + full fill)
        fuelGaugeBg.draw(graphics, FUEL_GAUGE_X, FUEL_GAUGE_Y);
        fuelGaugeFill.draw(graphics, FUEL_GAUGE_X, FUEL_GAUGE_Y);

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
