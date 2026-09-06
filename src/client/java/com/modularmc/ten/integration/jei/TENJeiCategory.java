package com.modularmc.ten.integration.jei;

import com.modularmc.ten.TENConstants;
import com.modularmc.ten.api.recipe.FormsCombinedIngredient;
import com.modularmc.ten.api.recipe.FormsCombinedRecipe;
import com.modularmc.ten.integration.xei.TENRecipeWidget;
import com.modularmc.ten.utils.RenderHelper;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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

/**
 * 增强版 JEI 机器配方分类，适配 1.21.1：
 * <ul>
 *   <li>使用 modular 素材（ITEM_SLOT_SMALL/FLUID_SLOT）替代默认 slot drawable</li>
 *   <li>集成 TENJeiSlotOverlay（chance/rolls 文本覆盖）</li>
 *   <li>使用 ingredient.tooltipKind() 分类注册 tooltip</li>
 *   <li>取消 getBackground()，改用 draw 中渲染底图</li>
 * </ul>
 */
public class TENJeiCategory implements IRecipeCategory<FormsCombinedRecipe> {

    private final RecipeType<FormsCombinedRecipe> recipeType;
    private final Component title;
    private final IDrawable icon;
    private final IDrawable inputSlot;
    private final IDrawable fluidSlot;
    private final TENRecipeWidget.Layout layout;

    public TENJeiCategory(IGuiHelper helper, ResourceLocation categoryId, RecipeType<FormsCombinedRecipe> type, ItemStack iconStack) {
        this.recipeType = type;
        this.layout = TENRecipeWidget.layout(categoryId);
        this.title = TENRecipeWidget.titleJei(categoryId);
        this.icon = helper.createDrawableItemStack(iconStack);
        // 26.1.2 对齐：槽位底图用 LDLib2 modular 素材族（旧版手工 blit 已弃用）
        this.inputSlot = helper.drawableBuilder(TENConstants.ITEM_SLOT_SMALL, 0, 0, 18, 18).setTextureSize(18, 18).build();
        this.fluidSlot = helper.drawableBuilder(TENConstants.FLUID_SLOT, 0, 0, 18, 50).setTextureSize(18, 50).build();
    }

    @Override
    public RecipeType<FormsCombinedRecipe> getRecipeType() { return recipeType; }

    @Override
    public Component getTitle() { return title; }

    @Override
    public int getWidth() { return layout.width(); }

    @Override
    public int getHeight() { return layout.height(); }

    @Override
    public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, FormsCombinedRecipe recipe, IFocusGroup focuses) {
        for (var slot : layout.slots()) {
            var ingredient = TENRecipeWidget.ingredientFor(recipe, slot);
            if (slot.kind() == TENRecipeWidget.SlotKind.ITEM) {
                var jeiSlot = builder.addSlot(toJeiRole(slot.role()), slot.x() + 2, slot.y())
                        .setBackground(inputSlot, -1, -1);
                if (ingredient != null) {
                    var itemStacks = ingredient.itemStacks().stream().filter(s -> !s.isEmpty()).toList();
                    if (!itemStacks.isEmpty()) jeiSlot.addItemStacks(itemStacks);
                    registerSlotTooltips(jeiSlot, slot, ingredient);
                    attachSlotOverlay(jeiSlot, slot, ingredient);
                }
            } else if (slot.kind() == TENRecipeWidget.SlotKind.FLUID) {
                var jeiSlot = builder.addSlot(toJeiRole(slot.role()), slot.x() + 1, slot.y() + 1)
                        .setBackground(fluidSlot, -1, -1);
                int capacity = ingredient != null ? Math.max(1, TENRecipeWidget.fluidCapacity(ingredient)) : 1;
                jeiSlot.setFluidRenderer(capacity, true, slot.width() - 2, slot.height() - 2);
                if (ingredient != null) {
                    var fluidStacks = ingredient.fluidStacks().stream().filter(s -> !s.isEmpty()).toList();
                    if (!fluidStacks.isEmpty()) jeiSlot.addIngredients(NeoForgeTypes.FLUID_STACK, fluidStacks);
                    registerSlotTooltips(jeiSlot, slot, ingredient);
                }
            }
        }
    }

    private static void attachSlotOverlay(
            mezz.jei.api.gui.builder.IRecipeSlotBuilder jeiSlot,
            TENRecipeWidget.SlotSpec slot,
            FormsCombinedIngredient ingredient
    ) {
        if (!TENJeiSlotOverlay.shouldHaveOverlay(true, slot.role() == TENRecipeWidget.SlotRole.OUTPUT, ingredient)) return;
        var overlay = TENJeiSlotOverlay.create(ingredient);
        if (overlay != null) {
            jeiSlot.setOverlay(overlay, TENJeiSlotOverlay.X_OFFSET, TENJeiSlotOverlay.Y_OFFSET);
        }
    }

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
            case NONE -> {}
        }
    }

    @Override
    public void draw(FormsCombinedRecipe recipe, IRecipeSlotsView slotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        RenderHelper.render(graphics, 0, 0, layout.width(), layout.height(), 256, 256, layout.u(), layout.v(), layout.background());
        TENRecipeWidget.drawJei(recipe, graphics, layout);
        // 左下角工作消耗时间（对齐熔炼机兼容层样式；引擎兼容层不调用）
        var timePos = TENRecipeWidget.timeLabelPos(recipeType.getUid().getPath());
        TENRecipeWidget.drawDurationText(graphics, timePos[0], timePos[1], recipe.time());
    }

    private static RecipeIngredientRole toJeiRole(TENRecipeWidget.SlotRole role) {
        return role == TENRecipeWidget.SlotRole.INPUT ? RecipeIngredientRole.INPUT : RecipeIngredientRole.OUTPUT;
    }
}