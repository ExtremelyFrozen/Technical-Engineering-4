package com.modularmc.ten.api.option;

import com.modularmc.ten.utils.TagHelper;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 冷却剂查询（数据驱动）。
 * <p>
 * 准入与数值都来自物品标签，与 {@code MatchFuel} 的「金属燃料按品位标签分档」同构：
 * <ul>
 * <li>{@code c:coolants} —— 准入。不在该标签内的物品不能当冷却剂。</li>
 * <li>{@code c:coolants/basic|standard|premium} —— 分档，决定周期/推进量/次数。</li>
 * </ul>
 * 数据包或第三方只需往标签里加物品，无需改代码。{@link #register(Item, Coolant)}
 * 保留为代码侧显式覆盖，优先级高于标签。
 */
public final class Coolants {

    /** 冷却剂准入标签。 */
    public static final TagKey<Item> COOLANTS = TagHelper.keyItem("c:coolants");

    /** 分档标签 → 数值。列表顺序即匹配优先级，先命中先取。 */
    private static final List<Map.Entry<TagKey<Item>, Coolant>> TIERS = List.of(
            Map.entry(TagHelper.keyItem("c:coolants/premium"), new Coolant(5, 80, 64)),
            Map.entry(TagHelper.keyItem("c:coolants/standard"), new Coolant(8, 40, 64)),
            Map.entry(TagHelper.keyItem("c:coolants/basic"), new Coolant(10, 20, 64)));

    /** 已准入但未分档时的兜底数值（等同 basic 档）。 */
    private static final Coolant FALLBACK = new Coolant(10, 20, 64);

    /** 代码侧显式注册：物品 → 冷却数据，覆盖标签结果。 */
    private static final Map<Item, Coolant> EXPLICIT = new HashMap<>();

    private Coolants() {}

    /**
     * 查询冷却剂数据；非冷却剂返回 {@code null}。
     */
    public static Coolant get(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        Coolant explicit = EXPLICIT.get(stack.getItem());
        if (explicit != null) {
            return explicit;
        }
        if (!stack.is(COOLANTS)) {
            return null;
        }
        for (var tier : TIERS) {
            if (stack.is(tier.getKey())) {
                return tier.getValue();
            }
        }
        return FALLBACK;
    }

    /**
     * 查询物品是否为已注册的冷却剂。
     */
    public static boolean isCoolant(ItemStack stack) {
        return get(stack) != null;
    }

    /**
     * 代码侧注册一种冷却剂。同一物品重复注册时后者覆盖前者。
     */
    public static void register(Item item, Coolant coolant) {
        if (item == null) {
            throw new IllegalArgumentException("coolant item must not be null");
        }
        if (coolant == null) {
            throw new IllegalArgumentException("coolant data must not be null");
        }
        EXPLICIT.put(item, coolant);
    }
}
