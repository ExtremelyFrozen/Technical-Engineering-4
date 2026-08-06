package com.modularmc.ten.common.item.upgrades;

import com.modularmc.ten.common.item.TENBaseItem;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.List;
import java.util.function.Consumer;

public abstract class UpgradeItem extends TENBaseItem {

    double percent;

    public UpgradeItem(double per) {
        super(new Properties().stacksTo(1));
        percent = per;
    }

    public UpgradeItem(double per, Properties properties) {
        super(properties.stacksTo(1));
        percent = per;
    }

    /**
     * Check if this upgrade can be applied to the given machine.
     * <p>
     * This is the compatibility check — separated from {@link #effect(IUpgradableMachine)}
     * so that both GUI validation and quick-install can determine applicability
     * without executing potentially side-effectful effect code.
     * <p>
     * Subclasses that restrict machine type or radius MUST override this method
     * and move the type/radius check here, keeping only actual stat/behavior
     * modifications in {@code effect}.
     *
     * @param machine the target machine
     * @return true if this upgrade is compatible
     */
    public boolean canApply(IUpgradableMachine machine) {
        return true;
    }

    public boolean effect(IUpgradableMachine machine) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        List<Component> lines = UpgradeTooltipFormatter.format(this);
        for (int i = 0; i < lines.size(); i++) {
            Component line = lines.get(i);
            if (i == 0) {
                // Title: GOLD + BOLD (formatter already applies style)
                builder.accept(line);
            } else {
                // Subsequent lines: GOLD only (formatter already applies style)
                builder.accept(line);
            }
        }
    }
}
