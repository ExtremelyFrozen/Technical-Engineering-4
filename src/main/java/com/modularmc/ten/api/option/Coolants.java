package com.modularmc.ten.api.option;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;

/**
 * 冷却剂注册中心（可拓展）。
 * <p>
 * 冷却器通过本中心查询「冷却剂物品 → {@link Coolant} 数据」，而非硬编码 if-else。
 * 新增冷却剂只需调用一次 {@link #register(Item, Coolant)}；支持本模组或其它来源
 * 的任意物品，天然可拓展。
 */
public final class Coolants {

    /** 冷却剂物品 → 冷却数据映射。 */
    private static final Map<Item, Coolant> REGISTRY = new HashMap<>();

    private Coolants() {}

    static {
        // 内置三种冷却剂（原版冰系，数值递增）。
        // 冰：每 10s 减少正面机器 1s(20tick) 耗时，作用 64 次。
        // 浮冰：每 8s 减少 2s(40tick)，作用 64 次。
        // 蓝冰（干冰）：每 5s 减少 4s(80tick)，作用 64 次。
        register(Items.ICE, new Coolant(10, 20, 64));
        register(Items.PACKED_ICE, new Coolant(8, 40, 64));
        register(Items.BLUE_ICE, new Coolant(5, 80, 64));
    }

    /**
     * 注册一种冷却剂物品及其数据。同一物品重复注册时后者覆盖前者。
     *
     * @param item    冷却剂物品
     * @param coolant 冷却数据
     */
    public static void register(Item item, Coolant coolant) {
        if (item == null) {
            throw new IllegalArgumentException("coolant item must not be null");
        }
        REGISTRY.put(item, coolant);
    }

    /**
     * 查询物品是否为已注册的冷却剂。
     */
    public static boolean isCoolant(ItemStack stack) {
        return !stack.isEmpty() && REGISTRY.containsKey(stack.getItem());
    }

    /**
     * 查询冷却剂数据；非冷却剂物品返回 {@code null}。
     */
    public static Coolant get(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        return REGISTRY.get(stack.getItem());
    }
}