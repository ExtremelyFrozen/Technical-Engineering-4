package com.modularmc.ten.common.item;

import com.modularmc.ten.utils.ComponentHelper;
import com.modularmc.ten.utils.SafeOperationHelper;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class TENBaseItem extends Item {

    public TENBaseItem() {
        this(new Properties().stacksTo(64));
    }

    public TENBaseItem(Properties prp) {
        super(prp);
    }

    @Override
    public String getDescriptionId() {
        return super.getDescriptionId();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String path = SafeOperationHelper.regNameOf(this);

        for (int i = 0; true; i++) {
            String k = ComponentHelper.getKey(path + "." + i);
            Component ttc = ComponentHelper.translated(ComponentHelper.GOLD, k);
            if (ttc.getString().equals(k)) break;

            // 支持 \n 换行
            String text = ttc.getString();
            int nl = text.indexOf('\n');
            if (nl >= 0) {
                String[] lines = text.split("\n", -1);
                for (String line : lines) {
                    tooltip.add(Component.literal(line).withStyle(ttc.getStyle()));
                }
            } else {
                tooltip.add(ttc);
            }
        }
    }
}
