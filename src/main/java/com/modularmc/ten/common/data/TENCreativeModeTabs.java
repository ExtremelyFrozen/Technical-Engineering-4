package com.modularmc.ten.common.data;

import com.modularmc.ten.TEN;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import com.tterrag.registrate.util.entry.RegistryEntry;

import static com.modularmc.ten.common.data.TENBlocks.MACHINE_SMELTER;
import static com.modularmc.ten.common.data.TENBlocks.TIN_BLOCK;
import static com.modularmc.ten.common.data.TENItems.MOULD_GEAR;
import static com.modularmc.ten.common.data.TENItems.TIN_INGOT;
import static com.modularmc.ten.common.registry.Registration.REGISTRATE;

public class TENCreativeModeTabs {

    public static RegistryEntry<CreativeModeTab, CreativeModeTab> BLOCK_TAB;
    public static RegistryEntry<CreativeModeTab, CreativeModeTab> MACHINE_TAB;
    public static RegistryEntry<CreativeModeTab, CreativeModeTab> ITEM_TAB;
    public static RegistryEntry<CreativeModeTab, CreativeModeTab> TOOL_TAB;

    static {
        BLOCK_TAB = REGISTRATE
                .defaultCreativeTab("block",
                        builder -> builder.icon(TIN_BLOCK::asStack)
                                .title(REGISTRATE.addLang("itemGroup", TEN.id("block"), "Technical Engineering - Blocks"))
                                .displayItems((params, output) -> {
                                    var tab = BLOCK_TAB;
                                    if (tab != null) {
                                        for (var entry : REGISTRATE.getAll(Registries.ITEM)) {
                                            if (REGISTRATE.isInCreativeTab(entry, tab)) {
                                                output.accept(new ItemStack(entry.get()));
                                            }
                                        }
                                    }
                                })
                                .build())
                .register();

        MACHINE_TAB = REGISTRATE
                .defaultCreativeTab("machine",
                        builder -> builder.icon(MACHINE_SMELTER::asStack)
                                .title(REGISTRATE.addLang("itemGroup", TEN.id("machine"), "Technical Engineering - Machines"))
                                .displayItems((params, output) -> {
                                    var tab = MACHINE_TAB;
                                    if (tab != null) {
                                        for (var entry : REGISTRATE.getAll(Registries.ITEM)) {
                                            if (REGISTRATE.isInCreativeTab(entry, tab)) {
                                                output.accept(new ItemStack(entry.get()));
                                            }
                                        }
                                    }
                                })
                                .build())
                .register();

        ITEM_TAB = REGISTRATE
                .defaultCreativeTab("item",
                        builder -> builder.icon(TIN_INGOT::asStack)
                                .title(REGISTRATE.addLang("itemGroup", TEN.id("item"), "Technical Engineering - Items"))
                                .displayItems((params, output) -> {
                                    var tab = ITEM_TAB;
                                    if (tab != null) {
                                        for (var entry : REGISTRATE.getAll(Registries.ITEM)) {
                                            if (REGISTRATE.isInCreativeTab(entry, tab)) {
                                                output.accept(new ItemStack(entry.get()));
                                            }
                                        }
                                    }
                                })
                                .build())
                .register();

        TOOL_TAB = REGISTRATE
                .defaultCreativeTab("tool",
                        builder -> builder.icon(MOULD_GEAR::asStack)
                                .title(REGISTRATE.addLang("itemGroup", TEN.id("tool"), "Technical Engineering - Tools"))
                                .displayItems((params, output) -> {
                                    var tab = TOOL_TAB;
                                    if (tab != null) {
                                        for (var entry : REGISTRATE.getAll(Registries.ITEM)) {
                                            if (REGISTRATE.isInCreativeTab(entry, tab)) {
                                                output.accept(new ItemStack(entry.get()));
                                            }
                                        }
                                    }
                                })
                                .build())
                .register();
    }

    public static void init() {}
}
