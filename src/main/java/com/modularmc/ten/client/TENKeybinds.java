package com.modularmc.ten.client;

import com.modularmc.ten.TEN;

import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.util.Lazy;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = TEN.MOD_ID, value = Dist.CLIENT)
public class TENKeybinds {

    /** 键名定义在 {@link TEN#KEY_CATEGORY} / {@link TEN#KEY_TOGGLE_CHARGE}，datagen 侧要复用同一份常量做校验。 */
    public static final String KEY_CATEGORY = TEN.KEY_CATEGORY;
    public static final String KEY_TOGGLE_CHARGE = TEN.KEY_TOGGLE_CHARGE;

    public static final Lazy<KeyMapping> TOGGLE_CHARGE = Lazy.of(() -> new KeyMapping(
            KEY_TOGGLE_CHARGE,
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            KEY_CATEGORY));

    @SubscribeEvent
    public static void registerBindings(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_CHARGE.get());
    }
}
