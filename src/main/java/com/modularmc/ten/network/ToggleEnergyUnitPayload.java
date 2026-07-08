package com.modularmc.ten.network;

import com.modularmc.ten.TEN;
import com.modularmc.ten.common.item.EnergyUnitItem;
import com.modularmc.ten.component.EnergyUnitData;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public record ToggleEnergyUnitPayload() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ToggleEnergyUnitPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(TEN.MOD_ID, "toggle_energy_unit"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleEnergyUnitPayload> CODEC = StreamCodec.unit(new ToggleEnergyUnitPayload());

    @Override
    public Type<ToggleEnergyUnitPayload> type() {
        return TYPE;
    }

    public static void handle(ToggleEnergyUnitPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            // Find energy unit in inventory
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.getItem() instanceof EnergyUnitItem) {
                    EnergyUnitData data = EnergyUnitData.of(stack);
                    data.setCharging(!data.isCharging());
                    data.save(stack);
                    break;
                }
            }
        });
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(TYPE, CODEC, ToggleEnergyUnitPayload::handle);
    }
}
