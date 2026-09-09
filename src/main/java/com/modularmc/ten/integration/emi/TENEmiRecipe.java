package com.modularmc.ten.integration.emi;

import com.modularmc.ten.TEN;
import com.modularmc.ten.TENConstants;
import com.modularmc.ten.api.recipe.FormsCombinedRecipe;
import com.modularmc.ten.integration.xei.TENRecipeWidget;

import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidStack;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;

import java.util.ArrayList;
import java.util.List;

public class TENEmiRecipe implements EmiRecipe {

    private final EmiRecipeCategory category;
    private final FormsCombinedRecipe recipe;
    private final ResourceLocation id;
    private final ResourceLocation categoryId;
    private final TENRecipeWidget.Layout layout;
    private final List<EmiIngredient> inputs;
    private final List<EmiStack> outputs;

    public TENEmiRecipe(ResourceLocation categoryId, EmiRecipeCategory category, FormsCombinedRecipe recipe, ResourceLocation holderId) {
        this.category = category;
        this.recipe = recipe;
        // id 用 RecipeHolder 的注册 id：serializer Codec 解码不携带 id，recipe.getId() 本身为 null
        // （见 FormsCombinedRecipe.assignId 注释）；机器侧 findRecipe 已回填，此处不再依赖它
        this.id = holderId != null ? holderId : recipe.getId();
        this.categoryId = categoryId;
        this.layout = TENRecipeWidget.layout(categoryId);
        this.inputs = emiInputs(recipe);
        this.outputs = emiOutputs(recipe);
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
        return inputs;
    }

    @Override
    public List<EmiStack> getOutputs() {
        return outputs;
    }

    @Override
    public int getDisplayWidth() {
        return layout.width();
    }

    @Override
    public int getDisplayHeight() {
        return layout.height();
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        // 26.1.2 对齐：底图为 modular 素材族 jei_handler.png（256×256 画布，上 150×50 面板），按 UV 归一化裁切
        widgets.addTexture(
                layout.background(),
                0,
                0,
                layout.width(),
                layout.height(),
                layout.u(),
                layout.v(),
                layout.width(),
                layout.height(),
                256,
                256);

        for (var slot : layout.slots()) {
            var ingredient = TENRecipeWidget.ingredientFor(recipe, slot);
            // 空槽不隐藏（用户设定）：槽位与底图全部渲染；无配料/空配料 → 显示为空槽，
            // 物品与 tooltip 仅在配料存在时填充/挂载
            if (slot.kind() == TENRecipeWidget.SlotKind.ITEM) {
                // EMI customBackground 内部硬编码 256×256 图集采样（SlotWidget.drawBackground →
                // blit(texture, ..., 256, 256)），而 modular 槽框是独立尺寸文件（18×18）→
                // 按 256 图集 UV 采样会越界错切。改用 fluid 槽同款独立 addTexture（真实 texW/texH）。
                widgets.addTexture(TENConstants.ITEM_SLOT_SMALL,
                        slot.x(), slot.y(), 18, 18,
                        0, 0, 18, 18, 18, 18);
                // drawBack(false)：EMI 自带槽框（默认绘制）会覆盖先插入的 modular 底图
                var emiSlot = widgets.addSlot(
                        ingredient != null ? toEmiIngredient(ingredient) : EmiStack.EMPTY,
                        slot.x(), slot.y())
                        .drawBack(false);
                if (ingredient != null) {
                    attachSlotTooltips(emiSlot, slot, ingredient);
                }
            } else {
                // TankWidget.backgroundTexture 有上游 bug（super 传字段 textureId 而非参数 id），
                // customBackground 纹理会被丢弃 → 槽保持 drawBack(false)，底图用独立 addTexture 绘制
                widgets.addTexture(TENConstants.FLUID_SLOT,
                        slot.x() - 1, slot.y() - 1, slot.width(), slot.height(),
                        0, 0, slot.width(), slot.height(), slot.width(), slot.height());
                var emiTank = widgets.addTank(
                        ingredient != null ? toEmiIngredient(ingredient) : EmiStack.EMPTY,
                        slot.x() - 1,
                        slot.y() - 1,
                        slot.width(),
                        slot.height(),
                        ingredient != null ? Math.max(1, TENRecipeWidget.fluidCapacity(ingredient)) : 1)
                        .drawBack(false);
                // 覆盖层在 tank 之后加入（EMI 按加入顺序绘制，后加者在上）——空槽叠槽、有流体叠在流体上，
                // 与 GUI 侧 fluidGaugeModular 层序对齐
                widgets.addTexture(TENConstants.FLUID_SLOT_OVERLAY,
                        slot.x() - 1, slot.y() - 1, slot.width(), slot.height(),
                        0, 0, slot.width(), slot.height(), slot.width(), slot.height());
                if (ingredient != null) {
                    attachSlotTooltips(emiTank, slot, ingredient);
                }
            }
        }

        widgets.addDrawable(0, 0, layout.width(), layout.height(), (draw, mouseX, mouseY, delta) -> {
            TENRecipeWidget.drawDecorations(draw, layout);
            // 概率/掷骰覆盖层（chance% 左上 + R9 左下，75% 缩放）：在槽位物品之后绘制（z 序正确）
            TENRecipeWidget.drawSlotOverlays(recipe, draw, layout);
            // 左下角工作消耗时间（对齐熔炼机兼容层样式；引擎兼容层不调用）
            var timePos = TENRecipeWidget.timeLabelPos(categoryId.getPath());
            TENRecipeWidget.drawDurationText(draw, timePos[0], timePos[1], recipe.time());
        });
        for (var decoration : layout.decorations()) {
            widgets.addTooltip(
                    (mouseX, mouseY) -> TENRecipeWidget.decorationTooltips(recipe, layout, mouseX, mouseY).stream()
                            .map(Component::getVisualOrderText)
                            .map(ClientTooltipComponent::create)
                            .toList(),
                    decoration.x(),
                    decoration.y(),
                    decoration.width(),
                    decoration.height());
        }
    }

    /**
     * 按 {@link FormsCombinedIngredient#tooltipKind(boolean)} 分类注册槽位 tooltip
     * （对齐 26.1.2 JEI registerSlotTooltips：CHANCE_ONLY / CHANCE_WITH_ROLLS / NOT_CONSUMED）。
     * 与 JEI 侧共用同一 lang 键（jei_addition_chance / jei_addition_chance_rolls / not_consumed）。
     */
    private void attachSlotTooltips(dev.emi.emi.api.widget.SlotWidget slot,
                                    TENRecipeWidget.SlotSpec slotSpec,
                                    com.modularmc.ten.api.recipe.FormsCombinedIngredient ingredient) {
        boolean isOutput = slotSpec.role() == TENRecipeWidget.SlotRole.OUTPUT;
        if (isOutput) {
            slot.recipeContext(this);
        }
        switch (ingredient.tooltipKind(isOutput)) {
            case CHANCE_ONLY -> slot.appendTooltip(Component.translatable(
                    TEN.MOD_ID + ".jei_addition_chance",
                    TENRecipeWidget.chancePercent(ingredient.chance())));
            case CHANCE_WITH_ROLLS -> slot.appendTooltip(Component.translatable(
                    TEN.MOD_ID + ".jei_addition_chance_rolls",
                    TENRecipeWidget.chancePercent(ingredient.chance()),
                    ingredient.rolls()));
            case NOT_CONSUMED -> slot.appendTooltip(Component.translatable(TEN.MOD_ID + ".not_consumed"));
            case NONE -> { /* no tooltip */ }
        }
    }

    private static List<EmiIngredient> emiInputs(FormsCombinedRecipe recipe) {
        List<EmiIngredient> inputs = new ArrayList<>();
        for (var ingredient : recipe.input()) {
            var emiIngredient = toEmiIngredient(ingredient);
            if (!emiIngredient.isEmpty()) {
                inputs.add(emiIngredient);
            }
        }
        return inputs;
    }

    private static List<EmiStack> emiOutputs(FormsCombinedRecipe recipe) {
        List<EmiStack> outputs = new ArrayList<>();
        for (var ingredient : recipe.output()) {
            var emiStack = toPrimaryEmiStack(ingredient);
            if (!emiStack.isEmpty()) {
                outputs.add(emiStack);
            }
        }
        return outputs;
    }

    private static EmiIngredient toEmiIngredient(com.modularmc.ten.api.recipe.FormsCombinedIngredient ingredient) {
        if (ingredient.form().equals("fluid")) {
            List<EmiIngredient> fluidOptions = ingredient.fluidStacks().stream()
                    .filter(stack -> !stack.isEmpty())
                    .map(stack -> EmiStack.of(stack.getFluid(), stack.getComponentsPatch(), stack.getAmount()))
                    .map(emiStack -> (EmiIngredient) emiStack)
                    .toList();
            return EmiIngredient.of(fluidOptions, Math.max(1, ingredient.amountOrCount()));
        }
        List<EmiIngredient> itemOptions = ingredient.itemStacks().stream()
                .filter(stack -> !stack.isEmpty())
                .map(EmiStack::of)
                .map(emiStack -> (EmiIngredient) emiStack)
                .toList();
        return EmiIngredient.of(itemOptions, Math.max(1, ingredient.amountOrCount()));
    }

    private static EmiStack toPrimaryEmiStack(com.modularmc.ten.api.recipe.FormsCombinedIngredient ingredient) {
        if (ingredient.form().equals("fluid")) {
            FluidStack stack = ingredient.symbolFluid();
            if (stack.isEmpty()) {
                return EmiStack.EMPTY;
            }
            var emiStack = EmiStack.of(stack.getFluid(), stack.getComponentsPatch(), stack.getAmount());
            if (ingredient.chance() < 1.0d) {
                emiStack.setChance((float) ingredient.chance());
            }
            return emiStack;
        }
        var stack = ingredient.symbolItem();
        if (stack.isEmpty()) {
            return EmiStack.EMPTY;
        }
        var emiStack = EmiStack.of(stack);
        if (ingredient.chance() < 1.0d) {
            emiStack.setChance((float) ingredient.chance());
        }
        return emiStack;
    }
}
