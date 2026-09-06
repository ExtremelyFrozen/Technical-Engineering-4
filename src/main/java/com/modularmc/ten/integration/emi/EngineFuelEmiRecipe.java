package com.modularmc.ten.integration.emi;

import com.modularmc.ten.TENConstants;
import com.modularmc.ten.common.blockentity.EngineFuelRecipe;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;

import java.util.List;

/**
 * 引擎燃料三类（extractor/metalizer/biomass）的 EMI 配方包装（26.1.2 JEI 对齐）：
 * 燃料输入槽（ITEM_SLOT_SMALL 底图）+ 燃料表（FUEL_GAUGE_BG/FILL）+ 右侧三行信息文本。
 * 布局常量与 {@code EngineFuelCategory} 逐坐标一致。
 */
public class EngineFuelEmiRecipe implements EmiRecipe {

    // 布局常量（与 EngineFuelCategory v5 逐坐标一致）
    private static final int INPUT_SLOT_X = 10;
    private static final int INPUT_SLOT_Y = 16;
    private static final int FUEL_GAUGE_X = 37;
    private static final int FUEL_GAUGE_Y = 19;
    private static final int FUEL_GAUGE_W = 13;
    private static final int FUEL_GAUGE_H = 13;
    private static final int TEXT_X = 58;
    private static final int TEXT_LINE1_Y = 8;
    private static final int TEXT_LINE2_Y = 21;
    private static final int TEXT_LINE3_Y = 34;

    private final EmiRecipeCategory category;
    private final ResourceLocation id;
    private final EngineFuelRecipe recipe;

    public EngineFuelEmiRecipe(EmiRecipeCategory category, ResourceLocation id, EngineFuelRecipe recipe) {
        this.category = category;
        this.id = id;
        this.recipe = recipe;
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return category;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return recipe.ingredients().stream().map(EmiStack::of).map(s -> (EmiIngredient) s).toList();
    }

    @Override
    public List<EmiStack> getOutputs() {
        return List.of();
    }

    @Override
    public int getDisplayWidth() {
        return 150;
    }

    @Override
    public int getDisplayHeight() {
        return 50;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        // EMI customBackground 硬编码 256 图集采样，独立尺寸文件需真实 texW/texH
        widgets.addTexture(TENConstants.ITEM_SLOT_SMALL, INPUT_SLOT_X, INPUT_SLOT_Y, 18, 18, 0, 0, 18, 18, 18, 18);
        widgets.addSlot(EmiStack.of(recipe.ingredients().get(0)), INPUT_SLOT_X, INPUT_SLOT_Y)
                .drawBack(false)
                .recipeContext(this);

        widgets.addTexture(TENConstants.FUEL_GAUGE_BG, FUEL_GAUGE_X, FUEL_GAUGE_Y, FUEL_GAUGE_W, FUEL_GAUGE_H, 0, 0, FUEL_GAUGE_W, FUEL_GAUGE_H, FUEL_GAUGE_W, FUEL_GAUGE_H);
        widgets.addTexture(TENConstants.FUEL_GAUGE_FILL, FUEL_GAUGE_X, FUEL_GAUGE_Y, FUEL_GAUGE_W, FUEL_GAUGE_H, 0, 0, FUEL_GAUGE_W, FUEL_GAUGE_H, FUEL_GAUGE_W, FUEL_GAUGE_H);

        int textColor = 0xFF404040;
        widgets.addText(Component.translatable("kenergyengineering.jei.base_rate_short", recipe.baseRate()), TEXT_X, TEXT_LINE1_Y, textColor, false);
        widgets.addText(Component.translatable("kenergyengineering.jei.duration_short", recipe.durationTicks()), TEXT_X, TEXT_LINE2_Y, textColor, false);
        widgets.addText(Component.translatable("kenergyengineering.jei.total_short", recipe.baseGeneratedEnergy()), TEXT_X, TEXT_LINE3_Y, textColor, false);
    }
}
