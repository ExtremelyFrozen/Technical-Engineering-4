package com.modularmc.ten.common.blockentity;

import com.modularmc.ten.api.recipe.EngineFuelRecipe;
import com.modularmc.ten.common.blockentity.MatchFuel.FuelProfile;
import com.modularmc.ten.common.data.TENRecipeTypes;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 引擎燃料的唯一查询入口：从数据包配方 {@code kenergyengineering:engine_fuel} 里
 * 找出「这台引擎 + 这个物品」对应的一份燃料参数。
 *
 * <p>
 * 匹配优先级：精确物品优先于标签（按 ingredient 解析出的物品数升序），同优先级取先注册者。
 * 数据包没有覆盖的物品再回退到原版燃烧时间（{@code getBurnTime}），
 * 这条兜底只为兼容「别的模组给物品加了燃烧时间」的情况，不参与本模组的燃料定义。
 */
public final class EngineFuels {

    /** 引擎 id：与数据包 {@code engine_fuel} 配方的 {@code engines} 字段对应。 */
    public static final String EXTRACTOR = "extractor";
    public static final String METALIZER = "metalizer";
    public static final String BIOMASS = "biomass";
    public static final String SOLAR = "solar";

    /** 兜底口径的能量密度（FE / 燃烧 tick），与旧版萃取引擎一致。 */
    private static final int FALLBACK_DENSITY = 20;

    /** 缓存按 RecipeManager 实例失效：重载数据包会换一个新实例。 */
    private static final Map<RecipeManager, List<EngineFuelRecipe>> CACHE = new WeakHashMap<>();
    private static List<EngineFuelRecipe> lastResolved = List.of();

    private EngineFuels() {}

    /**
     * @param level      当前世界（可为 null；null 时复用上次索引）
     * @param engineId   引擎 id，见各引擎的 {@code fuelEngineId()}
     * @param engineRate 引擎额定 FE/t
     * @param stack      燃料物品（不修改）
     * @return 燃料参数；不是本引擎的燃料时返回 {@link FuelProfile#EMPTY}
     */
    public static FuelProfile profile(Level level, String engineId, int engineRate, ItemStack stack) {
        return profile(level == null ? null : level.getRecipeManager(), engineId, engineRate, stack);
    }

    /** 供 JEI/EMI 等自带 RecipeManager 的展示层使用。 */
    public static FuelProfile profile(RecipeManager manager, String engineId, int engineRate, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return FuelProfile.EMPTY;
        for (EngineFuelRecipe recipe : recipes(manager)) {
            if (!recipe.appliesTo(engineId)) continue;
            if (!recipe.test(stack)) continue;
            int rate = rateOf(engineRate, recipe.rateMultiplier());
            return new FuelProfile(rate, recipe.duration(), recipe.rateMultiplier());
        }
        // 兜底只给萃取引擎：它历史上就吃「任何有原版燃烧时间的物品」。
        // 金属引擎与生物质引擎的燃料是白名单（锭 / 植物标签），不能被兜底放进来。
        return EXTRACTOR.equals(engineId) ? vanillaBurnFallback(engineRate, stack) : FuelProfile.EMPTY;
    }

    /** 引擎额定 × 配方修正倍率 = 该配方在这台引擎上的实际 FE/t。 */
    public static int rateOf(int engineRate, double rateMultiplier) {
        return Math.max(1, (int) Math.round(engineRate * rateMultiplier));
    }

    /**
     * 该物品是否被任何一份「引擎燃料」配方收录（不看引擎）。
     * 供 JEI/EMI 之类的展示层判断，不参与运行时结算。
     */
    public static boolean definedByData(RecipeManager manager, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        for (EngineFuelRecipe recipe : recipes(manager)) {
            if (recipe.test(stack)) return true;
        }
        return false;
    }

    private static List<EngineFuelRecipe> recipes(RecipeManager manager) {
        if (manager == null) return lastResolved;
        // 单人游戏里服务端与客户端各有一个 RecipeManager，两边都会来查：
        // 按实例分别缓存，避免两边互相把对方的缓存挤掉、每 tick 重建一次索引。
        return CACHE.computeIfAbsent(manager, m -> {
            List<EngineFuelRecipe> list = new ArrayList<>();
            for (RecipeHolder<EngineFuelRecipe> holder : m.getAllRecipesFor(TENRecipeTypes.ENGINE_FUEL_T.get())) {
                list.add(holder.value());
            }
            // 精确物品（解析出 1 个物品）排在标签前面，避免同名标签盖掉专门的条目
            list.sort(Comparator.comparingInt(r -> r.ingredient().getItems().length));
            lastResolved = List.copyOf(list);
            return lastResolved;
        });
    }

    /** 兼容兜底：别的模组给物品加了原版燃烧时间时，按旧口径（20 FE / 燃烧 tick）折算。 */
    private static FuelProfile vanillaBurnFallback(int engineRate, ItemStack stack) {
        int ticks = stack.getBurnTime(RecipeType.SMELTING);
        if (ticks <= 0) return FuelProfile.EMPTY;
        return FuelProfile.of(FALLBACK_DENSITY * ticks, engineRate, 1.0);
    }
}
