package com.modularmc.ten.common.data;

import com.modularmc.ten.TEN;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import com.tterrag.registrate.util.entry.RegistryEntry;

import static com.modularmc.ten.common.data.TENBlocks.MACHINE_SMELTER;
import static com.modularmc.ten.common.data.TENBlocks.TIN_BLOCK;
import static com.modularmc.ten.common.data.TENItems.MOULD_GEAR;
import static com.modularmc.ten.common.data.TENItems.REDSTONE_AI_ADVANCED;
import static com.modularmc.ten.common.registry.Registration.REGISTRATE;

public class TENCreativeModeTabs {

    public static final java.util.LinkedHashMap<String, String> ZH_NAMES = new java.util.LinkedHashMap<>();

    public static RegistryEntry<CreativeModeTab, CreativeModeTab> BLOCK_TAB;
    public static RegistryEntry<CreativeModeTab, CreativeModeTab> MACHINE_TAB;
    public static RegistryEntry<CreativeModeTab, CreativeModeTab> ITEM_TAB;
    public static RegistryEntry<CreativeModeTab, CreativeModeTab> TOOL_TAB;

    static {
        BLOCK_TAB = REGISTRATE
                .defaultCreativeTab("block",
                        builder -> builder.icon(TIN_BLOCK::asStack)
                                .title(REGISTRATE.addLang("itemGroup", TEN.id("block"), "Kenergy Engineering: Retechnicalized - Blocks"))
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
                                .title(REGISTRATE.addLang("itemGroup", TEN.id("machine"), "Kenergy Engineering: Retechnicalized - Machines"))
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
                        builder -> builder.icon(REDSTONE_AI_ADVANCED::asStack)
                                .title(REGISTRATE.addLang("itemGroup", TEN.id("item"), "Kenergy Engineering: Retechnicalized - Items"))
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
                                .title(REGISTRATE.addLang("itemGroup", TEN.id("tool"), "Kenergy Engineering: Retechnicalized - Tools"))
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

    static {
        ZH_NAMES.put("itemGroup.kenergyengineering.block", "科能工程:再技术化 | 方块");
        ZH_NAMES.put("itemGroup.kenergyengineering.machine", "科能工程:再技术化 | 机器");
        ZH_NAMES.put("itemGroup.kenergyengineering.item", "科能工程:再技术化 | 物品");
        ZH_NAMES.put("itemGroup.kenergyengineering.tool", "科能工程:再技术化 | 工具");
    }

    public static void init() {}
}
