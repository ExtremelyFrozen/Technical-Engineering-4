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

import java.util.ArrayList;
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
        // Strict exact matching: occupied input slot count must equal required ingredient count.
        // ALLOW_ALL 占位（serializer 对不足槽位的 air 填充）不计入需求，否则 2 输入配方会被
        // 迫要求放满全部机器槽位（"还需要输入一个空气"）。
        int occupiedSlots = 0;
        List<Integer> occupiedSlotIndices = new ArrayList<>();
        for (int i = 0; i < inv.getSlots(); i++) {
            if (slotType.get(i).canIn() && !inv.getStackInSlot(i).isEmpty()) {
                occupiedSlots++;
                occupiedSlotIndices.add(i);
            }
        }
        List<FormsCombinedIngredient> itemIngredients = new ArrayList<>();
        for (var ing : input) {
            if ("item".equals(ing.form) && !ing.isAllowAll()) itemIngredients.add(ing);
        }
        if (occupiedSlots != itemIngredients.size()) return false;

        // 一一分配：每个 item ingredient 独占一个匹配槽（含催化剂 chance=0），回溯求解。
        // 防串配方：两个 ingredient 的匹配集有交集时（如两个 tag 均含同一物品），
        // 不允许共用同一槽——各自必须落在独立槽位且数量充足。
        if (!assignItemIngredients(itemIngredients, 0, occupiedSlotIndices, inv, new boolean[occupiedSlotIndices.size()])) {
            return false;
        }

        for (var i : input) {
            if (!i.check(slotType, tankType, inv, tanks)) return false;
        }
        return true;
    }

    /**
     * Backtracking assignment of item ingredients to occupied input slots.
     * Ingredient {@code idx} must find a distinct slot whose stack count covers
     * {@code amountOrCount} and whose item is contained by the ingredient.
     */
    private static boolean assignItemIngredients(List<FormsCombinedIngredient> ingredients, int idx,
                                                 List<Integer> slots, IItemHandler inv, boolean[] used) {
        if (idx == ingredients.size()) return true;
        var ing = ingredients.get(idx);
        for (int s = 0; s < slots.size(); s++) {
            if (used[s]) continue;
            ItemStack stack = inv.getStackInSlot(slots.get(s));
            if (stack.getCount() < ing.amountOrCount()) continue;
            if (!ing.contains(stack.getItem())) continue;
            used[s] = true;
            if (assignItemIngredients(ingredients, idx + 1, slots, inv, used)) return true;
            used[s] = false;
        }
        return false;
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

    /**
     * 回填配方 id：1.21 Codec 体系下 serializer 解码时拿不到 id（由 RecipeManager 经
     * RecipeHolder 从文件路径提供）→ {@link FormsCombinedRecipeSerializer} 只能传 null。
     * 所有从 RecipeManager 取出本类配方的调用侧（findRecipe/EMI 集成）须以 holder.id() 回填，
     * 否则 {@link #getId()} 为 null → 机器配方身份变更检测失效（切配方不重置进度）、EMI id 缺失。
     */
    public FormsCombinedRecipe assignId(ResourceLocation holderId) {
        this.id = holderId;
        return this;
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
