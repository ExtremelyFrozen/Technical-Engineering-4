package com.modularmc.ten.integration.emi;

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
    private final TENRecipeWidget.Layout layout;
    private final List<EmiIngredient> inputs;
    private final List<EmiStack> outputs;

    public TENEmiRecipe(ResourceLocation categoryId, EmiRecipeCategory category, FormsCombinedRecipe recipe) {
        this.category = category;
        this.recipe = recipe;
        this.id = recipe.getId();
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
            if (ingredient == null) {
                continue;
            }
            var emiIngredient = toEmiIngredient(ingredient);
            if (emiIngredient.isEmpty()) {
                continue;
            }
            if (slot.kind() == TENRecipeWidget.SlotKind.ITEM) {
                var emiSlot = widgets.addSlot(emiIngredient, slot.x(), slot.y())
                        .drawBack(false);
                if (slot.role() == TENRecipeWidget.SlotRole.OUTPUT) {
                    emiSlot.recipeContext(this);
                }
                if (slot.role() == TENRecipeWidget.SlotRole.OUTPUT && ingredient.chance() < 1.0d) {
                    emiSlot.appendTooltip(Component.literal(TENRecipeWidget.formatChance(ingredient.chance())));
                }
            } else {
                var emiTank = widgets.addTank(
                        emiIngredient,
                        slot.x(),
                        slot.y(),
                        slot.width(),
                        slot.height(),
                        Math.max(1, TENRecipeWidget.fluidCapacity(ingredient)))
                        .drawBack(false);
                if (slot.role() == TENRecipeWidget.SlotRole.OUTPUT) {
                    emiTank.recipeContext(this);
                }
                if (slot.role() == TENRecipeWidget.SlotRole.OUTPUT && ingredient.chance() < 1.0d) {
                    emiTank.appendTooltip(Component.literal(TENRecipeWidget.formatChance(ingredient.chance())));
                }
            }
        }

        widgets.addDrawable(0, 0, layout.width(), layout.height(), (draw, mouseX, mouseY, delta) -> TENRecipeWidget.drawDecorations(draw, layout));
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
