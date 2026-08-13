package com.modularmc.ten.common.blockentity;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * 管道独特升级类型（封闭稳定集合）：拉取/推入/速度/扩写/末影。
 * <p>
 * 升级物、上限与名称 lang 键为领域常量，核心逻辑只消费本枚举，
 * 不感知原版物品与 i18n 前缀细节。
 */
public enum PipeUpgradeType {

    /** 拉取：粘性活塞，最多 1 级。 */
    PULL(Items.STICKY_PISTON, 1, "pipe.upgrade.pull"),
    /** 推入：活塞，最多 1 级。 */
    PUSH(Items.PISTON, 1, "pipe.upgrade.push"),
    /** 速度：糖，最多 9 级。 */
    SPEED(Items.SUGAR, 9, "pipe.upgrade.speed"),
    /** 扩写（过滤页数）：书，最多 63 级。 */
    PAGE(Items.BOOK, 63, "pipe.upgrade.page"),
    /** 末影（跨维度）：末影珍珠，最多 8 级。 */
    ENDER(Items.ENDER_PEARL, 8, "pipe.upgrade.ender");

    private final Item material;
    private final int maxLevel;
    private final String nameKey;

    PipeUpgradeType(Item material, int maxLevel, String nameKey) {
        this.material = material;
        this.maxLevel = maxLevel;
        this.nameKey = nameKey;
    }

    /** 触发本升级所需的物品（右键手持判定）。 */
    public Item material() {
        return material;
    }

    /** 本升级的等级上限。 */
    public int maxLevel() {
        return maxLevel;
    }

    /** 升级名称 lang 键后缀（消费处经 {@code ComponentHelper.getKey} 补全前缀）。 */
    public String nameKey() {
        return nameKey;
    }

    /** 由手持物品反查升级类型；非升级物返回 {@code null}。 */
    public static PipeUpgradeType fromItem(Item item) {
        for (PipeUpgradeType type : values()) {
            if (type.material == item) {
                return type;
            }
        }
        return null;
    }
}
