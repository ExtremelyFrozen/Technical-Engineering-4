package com.modularmc.ten.api.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 生产端数据包配方：一份燃料在某台引擎上的运行参数。
 *
 * <p>
 * 只声明两件事——<b>持续时间</b>（{@link #duration()}，tick）与
 * <b>FE/t 修正倍率</b>（{@link #rateMultiplier()}，相对引擎额定功率）。绝对产出率不在这里：
 * 它由引擎的 {@code BASE_GENERATION_RATE} 乘上修正倍率算出，机器与配方不各自声明一份绝对量。
 *
 * <p>
 * 一份燃料的总能量 = {@code round(额定 × m) × duration}，与升级、并行运行都无关；
 * 升级只抬高产出率，于是持续时间按倍率缩短、总能量不变。
 *
 * <p>
 * {@code engines} 留空表示所有引擎通用；填了就只在列出的引擎上生效
 * （取值为 {@code extractor} / {@code metalizer} / {@code biomass}）。
 */
public class EngineFuelRecipe implements Recipe<SingleRecipeInput> {

    private final Ingredient ingredient;
    private final List<String> engines;
    private final int duration;
    private final double rateMultiplier;

    public EngineFuelRecipe(Ingredient ingredient, List<String> engines, int duration, double rateMultiplier) {
        if (ingredient == null || ingredient.isEmpty()) {
            throw new IllegalArgumentException("engine fuel ingredient must not be empty");
        }
        if (duration <= 0) {
            throw new IllegalArgumentException("engine fuel duration must be positive: " + duration);
        }
        if (!Double.isFinite(rateMultiplier) || rateMultiplier <= 0) {
            throw new IllegalArgumentException("engine fuel rate multiplier must be positive: " + rateMultiplier);
        }
        this.ingredient = ingredient;
        this.engines = List.copyOf(engines);
        this.duration = duration;
        this.rateMultiplier = rateMultiplier;
    }

    public Ingredient ingredient() {
        return ingredient;
    }

    /** 适用引擎 id 列表；空表示全部引擎。 */
    public List<String> engines() {
        return engines;
    }

    /** 持续时间（tick），即额定功率下这份燃料的燃烧时长。 */
    public int duration() {
        return duration;
    }

    /** 对引擎额定 FE/t 的修正倍率。 */
    public double rateMultiplier() {
        return rateMultiplier;
    }

    public boolean appliesTo(String engineId) {
        return engines.isEmpty() || engines.contains(engineId);
    }

    public boolean test(ItemStack stack) {
        return !stack.isEmpty() && ingredient.test(stack);
    }

    /** 一份燃料（额定功率、无升级）的总能量 FE。 */
    public int energy(int engineRate) {
        return (int) Math.round(engineRate * rateMultiplier) * duration;
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return com.modularmc.ten.common.data.TENRecipeTypes.ENGINE_FUEL_S.get();
    }

    @Override
    public RecipeType<?> getType() {
        return com.modularmc.ten.common.data.TENRecipeTypes.ENGINE_FUEL_T.get();
    }
}
