package com.modularmc.ten.common.blockentity;

import com.modularmc.ten.common.blockentity.MatchFuel.FuelProfile;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Shared recipe type for Extractor, Metalizer, and Biomass engine fuel in JEI.
 * <p>
 * 生产端口径（规范 v2）：产能只由引擎声明，配方只给「持续时间 + FE/t 修正倍率」。
 * 因此一个条目同时带三个数：
 * <ul>
 * <li>{@link #baseRate()} —— 引擎额定 FE/t（{@code BASE_GENERATION_RATE}）</li>
 * <li>{@link #rateMultiplier()} —— 配方对额定的修正倍率</li>
 * <li>{@link #rate()} —— 实际 FE/t = {@code round(baseRate × rateMultiplier)}</li>
 * </ul>
 * 总能量 = {@code rate × durationTicks}，其中 durationTicks 由配方直接声明，
 * 不再由引擎反推，所以显示出来的持续时间就是真实燃烧时间。
 */
public final class EngineFuelDisplay {

    private final int baseRate;
    private final int rate;
    private final int durationTicks;
    private final double rateMultiplier;
    private final int count;
    private final List<ItemStack> ingredients;

    public EngineFuelDisplay(ItemStack stack, FuelProfile profile, int baseRate) {
        this(stack, profile, baseRate, 1);
    }

    public EngineFuelDisplay(ItemStack stack, FuelProfile profile, int baseRate, int count) {
        if (stack == null || stack.isEmpty()) throw new IllegalArgumentException("stack must be non-empty");
        if (profile == null || profile.isEmpty()) throw new IllegalArgumentException("profile must be non-empty");
        if (baseRate <= 0) throw new IllegalArgumentException("baseRate must be positive: " + baseRate);
        if (count <= 0) throw new IllegalArgumentException("count must be positive: " + count);
        this.baseRate = baseRate;
        this.rate = profile.rate();
        this.durationTicks = profile.durationTicks();
        this.rateMultiplier = profile.rateMultiplier();
        this.count = count;
        ItemStack copy = stack.copy();
        copy.setCount(count);
        this.ingredients = Collections.singletonList(copy);
    }

    /** 引擎额定产能 FE/t（机器的固有属性）。 */
    public int baseRate() {
        return baseRate;
    }

    /** 配方实际产出率 FE/t = {@code round(baseRate × rateMultiplier)}。 */
    public int rate() {
        return rate;
    }

    /** 配方对引擎额定的 FE/t 修正倍率。 */
    public double rateMultiplier() {
        return rateMultiplier;
    }

    /** 持续时间（tick），即该燃料在额定功率下的真实燃烧时长。 */
    public int durationTicks() {
        return durationTicks;
    }

    /** 一份燃料的总能量（FE）。 */
    public int baseGeneratedEnergy() {
        return rate * durationTicks;
    }

    /** 一份燃料的总能量（FE），与 {@link #baseGeneratedEnergy()} 同值，保留旧调用名。 */
    public int fuelBudget() {
        return baseGeneratedEnergy();
    }

    /** Number of items consumed (always 1). */
    public int count() {
        return count;
    }

    /**
     * Returns a freshly copied list of ingredient stacks.
     */
    public List<ItemStack> ingredients() {
        List<ItemStack> result = new ArrayList<>(ingredients.size());
        for (ItemStack s : ingredients) {
            result.add(s.copy());
        }
        return result;
    }
}
