package com.modularmc.ten.integration.jei;

import com.modularmc.ten.TENConstants;
import com.modularmc.ten.api.recipe.FormsCombinedIngredient;
import com.modularmc.ten.api.recipe.FormsCombinedRecipe;
import com.modularmc.ten.integration.xei.TENRecipeWidget;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

public class TENJeiCategory implements IRecipeCategory<FormsCombinedRecipe> {

    private final RecipeType<FormsCombinedRecipe> recipeType;
    private final Component title;
    private final IDrawable icon;
    private final IDrawable inputSlot;
    private final IDrawable fluidSlot;
    private final TENRecipeWidget.Layout layout;

    public TENJeiCategory(IGuiHelper helper, Identifier categoryId, RecipeType<FormsCombinedRecipe> type, ItemStack iconStack) {
        this.recipeType = type;
        this.layout = TENRecipeWidget.layout(categoryId);
        this.title = TENRecipeWidget.titleJei(categoryId);
        this.icon = helper.createDrawableItemStack(iconStack);
        this.inputSlot = helper.getSlotDrawable();
        this.fluidSlot = helper.createDrawable(TENConstants.GUI_HANDLER, 0, 92, 18, 50);
    }

    @Override
    public RecipeType<FormsCombinedRecipe> getRecipeType() {
        return recipeType;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public int getWidth() {
        return layout.width();
    }

    @Override
    public int getHeight() {
        return layout.height();
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, FormsCombinedRecipe recipe, IFocusGroup focuses) {
        for (var slot : layout.slots()) {
            var ingredient = TENRecipeWidget.ingredientFor(recipe, slot);
            if (slot.kind() == TENRecipeWidget.SlotKind.ITEM) {
                var jeiSlot = builder.addSlot(toJeiRole(slot.role()), slot.x() + 2, slot.y())
                        .setBackground(inputSlot, -1, -1);
                if (ingredient != null) {
                    var itemStacks = ingredient.itemStacks();
                    var filteredItemStacks = itemStacks.stream().filter(s -> !s.isEmpty()).toList();
                    if (!filteredItemStacks.isEmpty()) {
                        jeiSlot.addItemStacks(filteredItemStacks);
                    }
                    registerSlotTooltips(jeiSlot, slot, ingredient);
                    attachSlotOverlay(jeiSlot, slot, ingredient);
                }
            } else if (slot.kind() == TENRecipeWidget.SlotKind.FLUID) {
                var jeiSlot = builder.addSlot(toJeiRole(slot.role()), slot.x() + 1, slot.y() + 1)
                        .setBackground(fluidSlot, -1, -1);
                int capacity = ingredient != null ? Math.max(1, TENRecipeWidget.fluidCapacity(ingredient)) : 1;
                jeiSlot.setFluidRenderer(capacity, true, slot.width() - 2, slot.height() - 2);
                if (ingredient != null) {
                    var fluidStacks = ingredient.fluidStacks();
                    var filteredFluidStacks = fluidStacks.stream().filter(s -> !s.isEmpty()).toList();
                    if (!filteredFluidStacks.isEmpty()) {
                        jeiSlot.addIngredients(NeoForgeTypes.FLUID_STACK, filteredFluidStacks);
                    }
                    registerSlotTooltips(jeiSlot, slot, ingredient);
                }
            } else {
                // unsupported kind: no slot registered
            }
        }
    }

    /**
     * Attaches a {@link TENJeiSlotOverlay} to the slot builder if the slot
     * qualifies (ITEM + OUTPUT + chance < 1 or rolls > 1).
     * <p>
     * The overlay is drawn by JEI's slot rendering pipeline — after the
     * item stack — guaranteeing proper z-order.
     * <p>
     * Offsets are chosen so the text appears at the same screen position
     * as the old {@code drawSlotOverlays} method but with correct layering:
     * <ul>
     *   <li>Top-left: chance percentage ({@code -1, (Y_OFFSET)} relative to slot origin)</li>
     *   <li>Bottom-left: rolls text ({@code -1, (Y_OFFSET + 9)})</li>
     * </ul>
     * Both text lines are shifted together by changing
     * {@link TENJeiSlotOverlay#Y_OFFSET}.
     * The bottom-right ItemStack count rendered by JEI is not modified.
     */
    private static void attachSlotOverlay(
            mezz.jei.api.gui.builder.IRecipeSlotBuilder jeiSlot,
            TENRecipeWidget.SlotSpec slot,
            FormsCombinedIngredient ingredient
    ) {
        if (!TENJeiSlotOverlay.shouldHaveOverlay(true, slot.role() == TENRecipeWidget.SlotRole.OUTPUT, ingredient)) {
            return;
        }
        var overlay = TENJeiSlotOverlay.create(ingredient);
        if (overlay != null) {
            // Offsets: text relative to slot origin (slot.x+2, slot.y)
            jeiSlot.setOverlay(overlay, TENJeiSlotOverlay.X_OFFSET, TENJeiSlotOverlay.Y_OFFSET);
        }
    }

    /**
     * Registers rich tooltip callbacks for chance/rolls/not-consumed on a
     * single slot. Delegates classification to
     * {@link FormsCombinedIngredient#tooltipKind(boolean)} so the decision
     * logic is testable without JEI runtime.
     */
    private static void registerSlotTooltips(
            mezz.jei.api.gui.builder.IRecipeSlotBuilder jeiSlot,
            TENRecipeWidget.SlotSpec slot,
            FormsCombinedIngredient ingredient
    ) {
        boolean isOutput = slot.role() == TENRecipeWidget.SlotRole.OUTPUT;
        switch (ingredient.tooltipKind(isOutput)) {
            case CHANCE_ONLY -> jeiSlot.addRichTooltipCallback(
                    (view, tooltip) -> tooltip.add(Component.translatable(
                            "kenergyengineering.jei_addition_chance",
                            TENRecipeWidget.chancePercent(ingredient.chance()))));
            case CHANCE_WITH_ROLLS -> jeiSlot.addRichTooltipCallback(
                    (view, tooltip) -> tooltip.add(Component.translatable(
                            "kenergyengineering.jei_addition_chance_rolls",
                            TENRecipeWidget.chancePercent(ingredient.chance()),
                            ingredient.rolls())));
            case NOT_CONSUMED -> jeiSlot.addRichTooltipCallback(
                    (view, tooltip) -> tooltip.add(Component.translatable(
                            "kenergyengineering.not_consumed")));
            case NONE -> { /* no callback */ }
        }
    }

    @Override
    public void draw(FormsCombinedRecipe recipe, IRecipeSlotsView slotsView, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        TENRecipeWidget.drawJei(recipe, graphics, layout);
        // Slot overlays (chance/rolls text) are now attached via
        // IRecipeSlotBuilder.setOverlay() in setRecipe() — drawn by JEI
        // after slot items for correct z-order.
    }

    private static RecipeIngredientRole toJeiRole(TENRecipeWidget.SlotRole role) {
        return role == TENRecipeWidget.SlotRole.INPUT ? RecipeIngredientRole.INPUT : RecipeIngredientRole.OUTPUT;
    }
}
