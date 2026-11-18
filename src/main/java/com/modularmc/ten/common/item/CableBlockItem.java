package com.modularmc.ten.common.item;

import com.modularmc.ten.common.block.machine.CableTier;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * 线缆方块物品：在默认提示之后追加两行规格（传输速率、能量缓冲）。
 * <p>
 * 只对登记过 {@link CableTier} 的方块生效，管道复用同一个方块基类但不登记，因此不会显示能量行。
 */
public class CableBlockItem extends BlockItem {

    public CableBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        CableTier tier = CableTier.of(getBlock());
        if (tier == null) {
            return;
        }
        lines.add(Component.translatable("kenergyengineering.cable.tooltip.rate", amount(tier.transfer())).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("kenergyengineering.cable.tooltip.buffer", amount(tier.capacity())).withStyle(ChatFormatting.GRAY));
    }

    /**
     * {@code Integer.MAX_VALUE} 是「无限」档（星辉线缆），显示成数字没有意义。
     * 其余按科技模组的习惯用 {@code k} / {@code M} 缩写，四档数值都能整除。
     */
    private static Component amount(int value) {
        if (value == Integer.MAX_VALUE) {
            return Component.translatable("kenergyengineering.cable.tooltip.infinite");
        }
        if (value >= 1_000_000) {
            return Component.literal((value / 1_000_000) + "M");
        }
        if (value >= 1_000) {
            return Component.literal((value / 1_000) + "k");
        }
        return Component.literal(String.valueOf(value));
    }
}
