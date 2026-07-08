package com.modularmc.ten.common.item;

import com.modularmc.ten.component.EnergyUnitData;
import com.modularmc.ten.config.ConfigHolder;
import com.modularmc.ten.utils.ComponentHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

public class EnergyUnitItem extends TENBaseItem {

    public EnergyUnitItem(Properties properties) {
        super(properties.stacksTo(1).durability(0));
    }

    // ───── Config access ─────
    public static int maxEnergy() {
        return ConfigHolder.INSTANCE.energyUnit.maxEnergy;
    }

    public static int inputRate() {
        return ConfigHolder.INSTANCE.energyUnit.inputRate;
    }

    public static int outputRate() {
        return ConfigHolder.INSTANCE.energyUnit.outputRate;
    }

    public static int chargeRate() {
        return ConfigHolder.INSTANCE.energyUnit.chargeRate;
    }

    public static boolean chargingDefault() {
        return ConfigHolder.INSTANCE.energyUnit.chargingDefault;
    }

    // ───── Use: toggle charging mode ─────
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            EnergyUnitData data = EnergyUnitData.of(stack);
            data.setCharging(!data.isCharging());
            data.save(stack);
            String status = data.isCharging() ? ComponentHelper.getKey("energy_capacity.charging_on") : ComponentHelper.getKey("energy_capacity.charging_off");
            player.sendSystemMessage(ComponentHelper.translated(ChatFormatting.GREEN, status));
        }
        return InteractionResult.SUCCESS;
    }

    // ───── Tooltip ─────
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, builder, flag);
        EnergyUnitData data = EnergyUnitData.of(stack);
        builder.accept(Component.literal("FE: " + data.getEnergy() + " / " + maxEnergy())
                .withStyle(ChatFormatting.GOLD));
        builder.accept(Component.literal(data.isCharging() ? "[充能中]" : "[已关闭]")
                .withStyle(data.isCharging() ? ChatFormatting.GREEN : ChatFormatting.RED));
    }
}
