package com.modularmc.ten.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.modularmc.ten.TEN;
import com.modularmc.ten.network.ToggleEnergyUnitPayload;

import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.util.Lazy;
import net.neoforged.neoforge.network.PacketDistributor;

import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = TEN.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class TENKeybinds {

    public static final String KEY_CATEGORY = "key.categories." + TEN.MOD_ID;
    public static final String KEY_TOGGLE_CHARGE = "key." + TEN.MOD_ID + ".toggle_charge";

    public static final Lazy<KeyMapping> TOGGLE_CHARGE = Lazy.of(() -> new KeyMapping(
            KEY_TOGGLE_CHARGE,
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            KEY_CATEGORY
    ));

    @SubscribeEvent
    public static void registerBindings(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_CHARGE.get());
    }
}
