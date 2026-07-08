package com.modularmc.ten.common.item;

import com.modularmc.ten.utils.ComponentHelper;
import com.modularmc.ten.utils.SafeOperationHelper;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class TENBaseBlockItem extends BlockItem {

    public TENBaseBlockItem(Block b, Properties prp) {
        super(b, prp);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        // TODO: 26.1.2 - Re-implement tooltip logic using new Consumer<Component> API
        // Old code used: List<Component> tooltip directly with tooltip.addAll(list)
        // New API uses Consumer<Component> tooltip with tooltip.accept(component)
        String path = SafeOperationHelper.regNameOf(this);

        for (int i = 0; true; i++) {
            String k = ComponentHelper.getKey(path + "." + i);
            Component ttc = ComponentHelper.translated(ComponentHelper.GOLD, k);
            if (ttc.getString().equals(k)) break;
            tooltip.accept(ttc);
        }
    }
}
