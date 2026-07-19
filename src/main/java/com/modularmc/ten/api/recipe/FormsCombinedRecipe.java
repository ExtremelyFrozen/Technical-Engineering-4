package com.modularmc.ten.api.recipe;

import com.modularmc.ten.utils.TagHelper;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.List;

public class FormsCombinedRecipe implements RandRecipe {

    protected Identifier regName;
    protected Identifier id;
    protected List<FormsCombinedIngredient> input;
    protected List<FormsCombinedIngredient> output;
    protected int time;

    public RecipeSerializer<? extends Recipe<RecipeInput>> serializer;
    public RecipeType<? extends Recipe<RecipeInput>> recipeType;

    public FormsCombinedRecipe(Identifier regName, Identifier id,
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

    /**
     * Count 'item' form input ingredients that are real (non-ALLOW_ALL).
     * ALLOW_ALL entries are serialization padding and must not inflate the required count.
     */
    private int countRequiredItemInputs() {
        int count = 0;
        for (var ing : input) {
            if ("item".equals(ing.form) && !ing.ALLOW_ALL) count++;
        }
        return count;
    }

    /**
     * Count occupied input slots in the item handler.
     */
    private int countOccupiedInputSlots(IItemHandler inv,
                                        FormsCombinedIngredient.IngredientTypeGetter slotType) {
        int occupied = 0;
        for (int i = 0; i < inv.getSlots(); i++) {
            if (slotType.get(i).canIn() && !inv.getStackInSlot(i).isEmpty()) {
                occupied++;
            }
        }
        return occupied;
    }

    /**
     * General matches: allows underfilled serialization slots.
     * Strategy: at least the number of real (non-ALLOW_ALL) item inputs must
     * occupy input slots; extra occupied slots are permitted (machine-chosen).
     * Strict exact matching is a separate method for machines that require it.
     */
    public boolean matches(IItemHandler inv, List<? extends IFluidHandler> tanks,
                           FormsCombinedIngredient.IngredientTypeGetter slotType,
                           FormsCombinedIngredient.IngredientTypeGetter tankType) {
        int occupied = countOccupiedInputSlots(inv, slotType);
        int required = countRequiredItemInputs();
        if (occupied < required) return false;
        return ingredientsMatch(inv, tanks, slotType, tankType);
    }

    /**
     * Strict matching: only when the exact number of occupied input slots
     * matches the real item ingredient count. Used by induction furnace.
     * Strategy: machine explicitly requires exact fill; extra items rejected.
     */
    public boolean matchesExactInputs(IItemHandler inv, List<? extends IFluidHandler> tanks,
                                      FormsCombinedIngredient.IngredientTypeGetter slotType,
                                      FormsCombinedIngredient.IngredientTypeGetter tankType) {
        int occupied = countOccupiedInputSlots(inv, slotType);
        int required = countRequiredItemInputs();
        if (occupied != required) return false;
        return ingredientsMatch(inv, tanks, slotType, tankType);
    }

    /**
     * Shared ingredient check: runs every recipe ingredient against the
     * current inventory/tank state. Callers verify occupied/required counts
     * before invoking this.
     */
    private boolean ingredientsMatch(IItemHandler inv, List<? extends IFluidHandler> tanks,
                                     FormsCombinedIngredient.IngredientTypeGetter slotType,
                                     FormsCombinedIngredient.IngredientTypeGetter tankType) {
        for (var i : input) {
            if (!i.check(slotType, tankType, inv, tanks)) return false;
        }
        return true;
    }

    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        for (var i : input) {
            if (i != null && "item".equals(i.form)) list.add(i.toOriginStackIngredients());
        }
        return list;
    }

    public int inputLimit(ItemStack stack) {
        for (var ing : input) {
            // For tag-type ingredients: dynamic tag check directly (matchItems is intentionally empty).
            // For static/direct-item: use the cached matchItems.
            if (ing.ifTagItem != null) {
                if (TagHelper.containsItem(stack.getItem(), ing.ifTagItem)) return ing.amountOrCount;
            } else if (ing.matchItems.contains(stack.getItem())) {
                return ing.amountOrCount;
            }
        }
        return 0;
    }

    @Override
    public int inputLimit(FluidStack stack) {
        for (var ing : input) {
            // Same pattern: prioritize dynamic tag check for tag-type ingredients.
            if (ing.ifTagFluid != null) {
                if (TagHelper.containsFluid(stack.getFluid(), ing.ifTagFluid)) return ing.amountOrCount;
            } else if (ing.matchFluids.contains(stack.getFluid())) {
                return ing.amountOrCount;
            }
        }
        return 0;
    }

    @Override
    public int time() {
        return time;
    }

    @Override
    public ItemStack assemble(RecipeInput inv) {
        return ItemStack.EMPTY;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public boolean showNotification() {
        return true;
    }

    public Identifier getId() {
        return id;
    }

    @Override
    public RecipeType<? extends Recipe<RecipeInput>> getType() {
        return recipeType;
    }

    @Override
    public RecipeSerializer<? extends Recipe<RecipeInput>> getSerializer() {
        return serializer;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return new RecipeBookCategory();
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    public List<FormsCombinedIngredient> output() {
        return output;
    }

    public List<FormsCombinedIngredient> input() {
        return input;
    }
}
