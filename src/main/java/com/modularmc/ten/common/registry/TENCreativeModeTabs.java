package com.modularmc.ten.common.registry;

import com.modularmc.ten.TEN;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.tterrag.registrate.util.entry.RegistryEntry;

import static com.modularmc.ten.common.registry.Registration.REGISTRATE;

public class TENCreativeModeTabs {

    public static RegistryEntry<CreativeModeTab, CreativeModeTab> BLOCK_TAB = REGISTRATE
            .defaultCreativeTab("block",
                    builder -> builder.icon(() -> new ItemStack(Items.STONE))
                            .title(REGISTRATE.addLang("itemGroup", TEN.id("block"), "Technical Engineering - Blocks"))
                            .build())
            .register();

    public static RegistryEntry<CreativeModeTab, CreativeModeTab> MACHINE_TAB = REGISTRATE
            .defaultCreativeTab("machine",
                    builder -> builder.icon(() -> new ItemStack(Items.FURNACE))
                            .title(REGISTRATE.addLang("itemGroup", TEN.id("machine"), "Technical Engineering - Machines"))
                            .build())
            .register();

    public static RegistryEntry<CreativeModeTab, CreativeModeTab> ITEM_TAB = REGISTRATE
            .defaultCreativeTab("item",
                    builder -> builder.icon(() -> new ItemStack(Items.IRON_INGOT))
                            .title(REGISTRATE.addLang("itemGroup", TEN.id("item"), "Technical Engineering - Items"))
                            .build())
            .register();

    public static RegistryEntry<CreativeModeTab, CreativeModeTab> TOOL_TAB = REGISTRATE
            .defaultCreativeTab("tool",
                    builder -> builder.icon(() -> new ItemStack(Items.IRON_PICKAXE))
                            .title(REGISTRATE.addLang("itemGroup", TEN.id("tool"), "Technical Engineering - Tools"))
                            .build())
            .register();

    public static void init() {}
}
