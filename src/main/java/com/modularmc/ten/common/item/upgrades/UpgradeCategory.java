package com.modularmc.ten.common.item.upgrades;

/**
 * 升级三分类。
 * <ul>
 * <li>{@link #TIER} 整合组件：提供全局倍率，整机仅一个；</li>
 * <li>{@link #MODIFIER} 增益组件：提供效率 / 耗能 / 其他参数增量，可叠加；</li>
 * <li>{@link #SPECIALIZATION} 独特组件：提供独特效果，整机仅一个生效。</li>
 * </ul>
 * 这里的分类只用于结算与拦截，不写进 tooltip：整合组件与独特组件的「仅一个」由
 * {@code CmMachineBlockEntity#validUpgrade} 在放入时直接拒绝，而不是提示玩家自行避让。
 * 结算规则由 {@code CmMachineBlockEntity.applyUpgradeEffects()} 统一实现。
 */
public enum UpgradeCategory {
    TIER,
    MODIFIER,
    SPECIALIZATION
}
