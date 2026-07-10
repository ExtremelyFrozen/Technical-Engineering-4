package com.modularmc.ten.common.item;

import com.modularmc.ten.utils.ComponentHelper;
import com.modularmc.ten.utils.SafeOperationHelper;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

public class TENBaseBlockItem extends BlockItem {

    public TENBaseBlockItem(Block b, Properties prp) {
        super(b, prp);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);

        String path = SafeOperationHelper.regNameOf(this);
        String prefix = resolveKeyPrefix(path);
        if (prefix == null) return;

        for (int i = 0; true; i++) {
            String k = prefix + i;
            Component ttc = ComponentHelper.translated(ComponentHelper.GOLD, k);
            if (ttc.getString().equals(k)) break;

            // Support \n newlines (same pattern as TENBaseItem)
            String text = ttc.getString();
            int nl = text.indexOf('\n');
            if (nl >= 0) {
                String[] lines = text.split("\n", -1);
                for (String line : lines) {
                    tooltip.accept(Component.literal(line).withStyle(ttc.getStyle()));
                }
            } else {
                tooltip.accept(ttc);
            }
        }
    }

    /**
     * Resolve the lang key prefix for tooltip lookup based on block registry name.
     *
     * <ul>
     *   <li>machine_x → kenergyengineering.info.x.&lt;n&gt;</li>
     *   <li>engine_x → kenergyengineering.info.engine_x.&lt;n&gt;</li>
     *   <li>energy_cell → kenergyengineering.info.energy_cell.&lt;n&gt;</li>
     *   <li>cable / cable_* → kenergyengineering.&lt;id&gt;.&lt;n&gt; (direct, no info.)</li>
     *   <li>Anything else → null (no tooltip)</li>
     * </ul>
     */
    private static String resolveKeyPrefix(String regPath) {
        if (regPath.startsWith("machine_")) {
            String name = regPath.substring("machine_".length());
            return ComponentHelper.getKey("info." + name + ".");
        }
        if (regPath.startsWith("engine_")) {
            return ComponentHelper.getKey("info." + regPath + ".");
        }
        if (regPath.equals("energy_cell")) {
            return ComponentHelper.getKey("info.energy_cell.");
        }
        if (regPath.startsWith("cable")) {
            return ComponentHelper.getKey(regPath + ".");
        }
        return null; // non-target: no tooltip
    }
}
