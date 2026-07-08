package com.modularmc.ten.utils;

import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.Collection;
import java.util.Objects;

public class SafeOperationHelper {

    public static String regNameOf(Block block) {
        return Objects.requireNonNull(BuiltInRegistries.BLOCK.getKey(block)).getPath();
    }

    public static String regNameOf(Item item) {
        return Objects.requireNonNull(BuiltInRegistries.ITEM.getKey(item)).getPath();
    }

    static RandomSource random = RandomSource.create();

    @SuppressWarnings("unchecked")
    public static <T> T randomInCollection(Collection<T> col) {
        if (col == null) return null;
        if (col.isEmpty()) return null;
        Object[] items = col.toArray();
        return (T) Util.getRandom(items, random);
    }

    public static int safeInt(Integer i) {
        if (i == null) return 0;
        return i;
    }
}
