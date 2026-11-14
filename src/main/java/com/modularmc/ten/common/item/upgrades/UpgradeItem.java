package com.modularmc.ten.common.item.upgrades;

import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;
import com.modularmc.ten.api.blockentity.IUpgradableMachine;
import com.modularmc.ten.common.item.TENBaseItem;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public abstract class UpgradeItem extends TENBaseItem {

    double percent;

    private UpgradeCategory category = UpgradeCategory.MODIFIER;
    private String family = "";
    private int tier = 0;
    private int maxPerMachine = CmMachineBlockEntity.MAX_UPGRADE_SLOTS;
    private UpgradeEffect modifiers = UpgradeEffect.NONE;
    /** 增益组件多级变体的档位（1 起），决定 tooltip 的本地化路径。 */
    private int variant = 1;

    public UpgradeItem(double per) {
        super(new Properties().stacksTo(1));
        percent = per;
    }

    public UpgradeItem(double per, Properties properties) {
        super(properties.stacksTo(1));
        percent = per;
    }

    public final UpgradeCategory category() {
        return category;
    }

    public final String family() {
        return family;
    }

    public final int tier() {
        return tier;
    }

    public final int maxPerMachine() {
        return maxPerMachine;
    }

    public final UpgradeEffect modifiers() {
        return modifiers;
    }

    public final int variant() {
        return variant;
    }

    protected final UpgradeItem variant(int level) {
        this.variant = level;
        return this;
    }

    /** 整合组件：声明所属族与档位。整机只允许装一张，由 {@code validUpgrade} 在放入时拒绝。 */
    protected final UpgradeItem asTier(String family, int tier, UpgradeEffect modifiers) {
        this.category = UpgradeCategory.TIER;
        this.family = family;
        this.tier = tier;
        this.modifiers = modifiers;
        return this;
    }

    /** 增益组件：声明线性叠加的系数。 */
    protected final UpgradeItem asModifier(UpgradeEffect modifiers) {
        this.category = UpgradeCategory.MODIFIER;
        this.modifiers = modifiers;
        return this;
    }

    /** 独特组件：整机仅一个生效，模式 marker 仍由 {@link #effect} 写入。 */
    protected final UpgradeItem asSpecialization() {
        this.category = UpgradeCategory.SPECIALIZATION;
        this.maxPerMachine = 1;
        return this;
    }

    protected final UpgradeItem limitedTo(int max) {
        this.maxPerMachine = max;
        return this;
    }

    /**
     * 兼容性检查：只判能不能装，不写任何数值。
     * 真正生效的卡由 {@code applyUpgradeEffects()} 按三类别规则筛选。
     */
    public boolean canApply(IUpgradableMachine machine) {
        return true;
    }

    /**
     * 副作用入口：独特组件的模式 marker、范围类的半径 / 上限、光合注能标记等。
     * 数值系数一律走 {@link #modifiers()}，不要在这里重复乘。
     */
    public boolean effect(IUpgradableMachine machine) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        List<Component> lines = UpgradeTooltipFormatter.format(this);
        tooltip.addAll(lines);
    }
}
