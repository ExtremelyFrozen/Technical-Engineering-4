package com.modularmc.ten.common.item;

import com.modularmc.ten.utils.ComponentHelper;
import com.modularmc.ten.utils.SafeOperationHelper;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

public class TENBaseBlockItem extends BlockItem {

    public TENBaseBlockItem(Block b, Properties prp) {
        super(b, prp);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        List<Component> list = new ArrayList<>();
        String path = SafeOperationHelper.regNameOf(this);

        for (int i = 0; true; i++) {
            String k = ComponentHelper.getKey(path + "." + i);
            Component ttc = ComponentHelper.translated(ComponentHelper.GOLD, k);
            if (ttc.getString().equals(k)) break;
            list.add(ttc);
        }

        // 直接显示所有提示，不再需要 Shift
        tooltip.addAll(list);
    }

    @Override
    public String getDescriptionId() {
        return super.getDescriptionId();
    }
}
