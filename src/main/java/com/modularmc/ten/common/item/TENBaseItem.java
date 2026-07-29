package com.modularmc.ten.common.item;

import com.modularmc.ten.utils.ComponentHelper;
import com.modularmc.ten.utils.SafeOperationHelper;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class TENBaseItem extends Item {

    public TENBaseItem() {
        this(new Properties().stacksTo(64));
    }

    public TENBaseItem(Properties prp) {
        super(prp);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        String path = SafeOperationHelper.regNameOf(this);

        for (int i = 0; true; i++) {
            String k = ComponentHelper.getKey(path + "." + i);
            Component ttc = ComponentHelper.translated(ComponentHelper.GOLD, k);
            if (ttc.getString().equals(k)) break;

            // All lines for base items: GOLD only (no BOLD — that's UpgradeItem's job)
            builder.accept(ttc);
        }
    }
}
