package com.modularmc.ten.api.recipe;

import com.modularmc.ten.utils.TagHelper;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.List;

public class FormsCombinedRecipe implements RandRecipe {

    protected ResourceLocation regName;
    protected ResourceLocation id;
    protected List<FormsCombinedIngredient> input;
    protected List<FormsCombinedIngredient> output;
    protected int time;

    public RecipeSerializer<?> serializer;
    public RecipeType<?> recipeType;

    public FormsCombinedRecipe(ResourceLocation regName, ResourceLocation id,
                               List<FormsCombinedIngredient> input,
                               List<FormsCombinedIngredient> output, int time) {
        this.regName = regName;
        this.id = id;
        this.input = input;
        this.output = output;
        this.time = time;
    }

    public List<FormsCombinedIngredient> allOutputFluids() {
        return output.stream().filter(ing -> "fluid".equals(ing.form)).toList();
    }

    public List<FormsCombinedIngredient> allInputFluids() {
        return input.stream().filter(ing -> "fluid".equals(ing.form)).toList();
    }

    public List<FormsCombinedIngredient> allOutputItems() {
        return output.stream().filter(ing -> "item".equals(ing.form)).toList();
    }

    public List<FormsCombinedIngredient> allInputItems() {
        return input.stream().filter(ing -> "item".equals(ing.form)).toList();
    }

    @Override
    public boolean matches(RecipeInput inv, Level level) {
        return true; // custom matching using machines
    }

    public boolean matches(IItemHandler inv, List<? extends IFluidHandler> tanks,
                           FormsCombinedIngredient.IngredientTypeGetter slotType,
                           FormsCombinedIngredient.IngredientTypeGetter tankType) {
        for (var i : input) {
            if (!i.check(slotType, tankType, inv, tanks)) return false;
        }
        return true;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        for (var i : input) {
            if (i != null && "item".equals(i.form)) list.add(i.toOriginStackIngredients());
        }
        return list;
    }

    @Override
    public int inputLimit(ItemStack stack) {
        for (var ing : input) {
            if (ing.matchItems.contains(stack.getItem())) return ing.amountOrCount;
            if (ing.ifTagItem != null && TagHelper.containsItem(stack.getItem(), ing.ifTagItem)) return ing.amountOrCount;
        }
        return 0;
    }

    @Override
    public int inputLimit(FluidStack stack) {
        for (var ing : input) {
            if (ing.matchFluids.contains(stack.getFluid())) return ing.amountOrCount;
            if (ing.ifTagFluid != null && TagHelper.containsFluid(stack.getFluid(), ing.ifTagFluid)) return ing.amountOrCount;
        }
        return 0;
    }

    @Override
    public int time() {
        return time;
    }

    @Override
    public ItemStack assemble(RecipeInput inv, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    public ResourceLocation getId() {
        return id;
    }

    public RecipeType<?> getType() {
        return recipeType;
    }

    public RecipeSerializer<?> getSerializer() {
        return serializer;
    }

    public List<FormsCombinedIngredient> output() {
        return output;
    }

    public List<FormsCombinedIngredient> input() {
        return input;
    }
}
