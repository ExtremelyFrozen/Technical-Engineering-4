package com.modularmc.ten.client;

import com.modularmc.ten.TEN;
import com.modularmc.ten.network.ToggleEnergyUnitPayload;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = TEN.MOD_ID, value = Dist.CLIENT)
public class TENKeyHandler {

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (TENKeybinds.TOGGLE_CHARGE.get().consumeClick()) {
            PacketDistributor.sendToServer(new ToggleEnergyUnitPayload());
        }
    }
}
