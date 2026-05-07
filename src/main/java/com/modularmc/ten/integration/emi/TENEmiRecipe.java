package com.modularmc.ten.integration.emi;

import com.modularmc.ten.api.recipe.FormsCombinedRecipe;
import com.modularmc.ten.integration.xei.TENRecipeWidget;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;

import java.util.List;
import java.util.stream.Collectors;

public class TENEmiRecipe implements EmiRecipe {

    private final EmiRecipeCategory category;
    private final FormsCombinedRecipe recipe;
    private final ResourceLocation id;
    private final List<EmiIngredient> inputs;
    private final List<EmiStack> outputs;

    public TENEmiRecipe(EmiRecipeCategory category, FormsCombinedRecipe recipe) {
        this.category = category;
        this.recipe = recipe;
        this.id = recipe.getId();
        this.inputs = recipe.allInputItems().stream()
                .filter(ing -> !ing.symbolItem().isEmpty())
                .map(ing -> EmiStack.of(ing.symbolItem()))
                .collect(Collectors.toList());
        this.outputs = recipe.allOutputItems().stream()
                .filter(ing -> !ing.symbolItem().isEmpty())
                .map(ing -> {
                    var stack = EmiStack.of(ing.symbolItem());
                    if (ing.chance() < 1.0f) {
                        stack.setChance((float) ing.chance());
                    }
                    return stack;
                })
                .collect(Collectors.toList());
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
        return 160;
    }

    @Override
    public int getDisplayHeight() {
        return 80;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addTexture(TENRecipeWidget.JEI_BG, 0, 0, 160, 80, 0, 0, 160, 80, 256, 256);

        int x = 10, y = 10;
        for (int i = 0; i < inputs.size(); i++) {
            widgets.addSlot(inputs.get(i), x, y + i * 22);
        }

        x = 90;
        for (int i = 0; i < outputs.size(); i++) {
            widgets.addSlot(outputs.get(i), x + (i % 2) * 22, y + (i / 2) * 22).recipeContext(this);
        }

        widgets.addFillingArrow(65, 30, 2000);

        widgets.addText(Component.literal("15 FE/t"), 5, 60, 0xFF5555, true);
        widgets.addText(Component.literal(String.format("%.1fs", recipe.time() / 20.0)), 80, 65, 0x555555, true);
    }
}
