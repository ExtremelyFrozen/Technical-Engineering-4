package com.modularmc.ten.integration.emi;

import com.modularmc.ten.TENConstants;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;

import java.util.List;

/**
 * 熔炼机三页（smelting/blasting/smoking）的 EMI 配方包装（26.1.2 JEI 对齐）：
 * 输入槽 + 输出槽（ITEM_SLOT_SMALL 底图）+ 进度箭头动画 + 熔炼时间文本。
 * <p>
 * 布局常量与 {@code SmelterJeiCategory} v5 逐坐标一致。自持 {@link RecipeData}
 * （main 源集可用，不依赖 client 源集的 JEI 类别）。
 */
public class SmelterEmiRecipe implements EmiRecipe {

    /** 布局常量（与 SmelterJeiCategory 一致）。 */
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
    private static final long PROGRESS_CYCLE_MS = 10_000L;

    /** 熔炼配方轻量记录（vanilla 烹饪配方采集后的展示数据）。 */
    public record RecipeData(List<ItemStack> inputs, ItemStack output, int cookingTime) {}

    private final EmiRecipeCategory category;
    private final ResourceLocation id;
    private final RecipeData recipe;

    public SmelterEmiRecipe(EmiRecipeCategory category, ResourceLocation id, RecipeData recipe) {
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
        return recipe.inputs().stream().map(EmiStack::of).map(s -> (EmiIngredient) s).toList();
    }

    @Override
    public List<EmiStack> getOutputs() {
        return List.of(EmiStack.of(recipe.output()));
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
        // 布局与 SmelterJeiCategory v5 一致：输入(25,16) 箭头(64,17) 输出(107,16) 时间(25,38)
        // EMI customBackground 硬编码 256 图集采样，独立尺寸文件需真实 texW/texH
        widgets.addTexture(TENConstants.ITEM_SLOT_SMALL, INPUT_X, INPUT_Y, 18, 18, 0, 0, 18, 18, 18, 18);
        widgets.addSlot(EmiIngredient.of(
                recipe.inputs().stream().map(EmiStack::of).toList()),
                INPUT_X, INPUT_Y)
                .drawBack(false);
        widgets.addTexture(TENConstants.ITEM_SLOT_SMALL, OUTPUT_X, OUTPUT_Y, 18, 18, 0, 0, 18, 18, 18, 18);
        widgets.addSlot(EmiStack.of(recipe.output()), OUTPUT_X, OUTPUT_Y)
                .drawBack(false)
                .recipeContext(this);

        // 进度箭头：背景 + 动画填充（horizontal=true, endToStart=false → 从左向右按时间裁切，10s 周期与 JEI 同步）
        widgets.addTexture(TENConstants.PROGRESS_ARROW_SMELTER_BG, ARROW_X, ARROW_Y, ARROW_W, ARROW_H, 0, 0, ARROW_W, ARROW_H, ARROW_W, ARROW_H);
        widgets.addAnimatedTexture(TENConstants.PROGRESS_ARROW_SMELTER_FILL,
                ARROW_X, ARROW_Y,
                ARROW_W, ARROW_H,
                0, 0,
                ARROW_W, ARROW_H,
                ARROW_W, ARROW_H,
                (int) PROGRESS_CYCLE_MS,
                true, false, false);

        widgets.addText(
                Component.translatable("kenergyengineering.jei.duration_short", recipe.cookingTime()),
                TEXT_X, TEXT_Y, 0xFF404040, false);
    }
}
