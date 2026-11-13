package com.modularmc.ten.common.block.machine;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.Map;

/**
 * 线缆的传输规格：能量缓冲（FE）与传输速率（FE/t）。
 * <p>
 * 数值原先写在 {@code CableBlockEntity} 的私有静态方法里，靠注册名字符串判断。物品 tooltip 拿不到
 * BlockState，所以提到这里，方块实体与物品提示共用同一份。管道不登记，{@link #of} 返回 null。
 * <p>
 * 键用 {@link ResourceKey} 而不是 {@link Block} 实例：登记发生在 {@code TENBlocks} 的静态初始化里，
 * 那时 {@code BlockEntry.get()} 还没有绑定注册表，取值会抛「Trying to access unbound value」。
 */
public record CableTier(int capacity, int transfer) {

    private static final Map<ResourceKey<?>, CableTier> BY_KEY = new HashMap<>();

    public static void register(ResourceKey<?> key, int capacity, int transfer) {
        BY_KEY.put(key, new CableTier(capacity, transfer));
    }

    /** 未登记的方块（管道等）返回 null。 */
    public static CableTier of(Block block) {
        return BY_KEY.get(block.builtInRegistryHolder().key());
    }
}
