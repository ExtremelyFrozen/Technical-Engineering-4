package com.modularmc.ten.common.data;

import com.modularmc.ten.common.item.upgrades.*;

import net.minecraft.world.item.Item;

import com.tterrag.registrate.util.entry.ItemEntry;
import com.tterrag.registrate.util.nullness.NonNullFunction;

import static com.modularmc.ten.common.registry.Registration.REGISTRATE;

public class TENItems {

    static {
        REGISTRATE.creativeModeTab(() -> TENCreativeModeTabs.ITEM_TAB);
    }

    // Materials - Dusts
    public static final ItemEntry<Item> IRON_DUST = item("iron_dust", "Iron Dust");
    public static final ItemEntry<Item> GOLD_DUST = item("gold_dust", "Gold Dust");
    public static final ItemEntry<Item> COPPER_DUST = item("copper_dust", "Copper Dust");
    public static final ItemEntry<Item> TIN_DUST = item("tin_dust", "Tin Dust");
    public static final ItemEntry<Item> NICKEL_DUST = item("nickel_dust", "Nickel Dust");
    public static final ItemEntry<Item> POWERED_TIN_DUST = item("powered_tin_dust", "Powered Tin Dust");
    public static final ItemEntry<Item> CHLORIUM_DUST = item("chlorium_dust", "Chlorium Dust");

    // Materials - Ingots
    public static final ItemEntry<Item> TIN_INGOT = item("tin_ingot", "Tin Ingot");
    public static final ItemEntry<Item> NICKEL_INGOT = item("nickel_ingot", "Nickel Ingot");
    public static final ItemEntry<Item> POWERED_TIN_INGOT = item("powered_tin_ingot", "Powered Tin Ingot");
    public static final ItemEntry<Item> CHLORIUM_INGOT = item("chlorium_ingot", "Chlorium Ingot");

    // Materials - Nuggets
    public static final ItemEntry<Item> TIN_NUGGET = item("tin_nugget", "Tin Nugget");
    public static final ItemEntry<Item> NICKEL_NUGGET = item("nickel_nugget", "Nickel Nugget");
    public static final ItemEntry<Item> POWERED_TIN_NUGGET = item("powered_tin_nugget", "Powered Tin Nugget");
    public static final ItemEntry<Item> CHLORIUM_NUGGET = item("chlorium_nugget", "Chlorium Nugget");

    // Materials - Plates
    public static final ItemEntry<Item> IRON_PLATE = item("iron_plate", "Iron Plate");
    public static final ItemEntry<Item> GOLD_PLATE = item("gold_plate", "Gold Plate");
    public static final ItemEntry<Item> COPPER_PLATE = item("copper_plate", "Copper Plate");
    public static final ItemEntry<Item> TIN_PLATE = item("tin_plate", "Tin Plate");
    public static final ItemEntry<Item> NICKEL_PLATE = item("nickel_plate", "Nickel Plate");
    public static final ItemEntry<Item> POWERED_TIN_PLATE = item("powered_tin_plate", "Powered Tin Plate");
    public static final ItemEntry<Item> CHLORIUM_PLATE = item("chlorium_plate", "Chlorium Plate");

    // Materials - Gears
    public static final ItemEntry<Item> IRON_GEAR = item("iron_gear", "Iron Gear");
    public static final ItemEntry<Item> GOLD_GEAR = item("gold_gear", "Gold Gear");
    public static final ItemEntry<Item> COPPER_GEAR = item("copper_gear", "Copper Gear");
    public static final ItemEntry<Item> TIN_GEAR = item("tin_gear", "Tin Gear");
    public static final ItemEntry<Item> NICKEL_GEAR = item("nickel_gear", "Nickel Gear");
    public static final ItemEntry<Item> POWERED_TIN_GEAR = item("powered_tin_gear", "Powered Tin Gear");
    public static final ItemEntry<Item> CHLORIUM_GEAR = item("chlorium_gear", "Chlorium Gear");

    // Raw Materials
    public static final ItemEntry<Item> RAW_TIN = item("raw_tin", "Raw Tin");
    public static final ItemEntry<Item> RAW_NICKEL = item("raw_nickel", "Raw Nickel");

    // Crafting Components
    public static final ItemEntry<Item> REDSTONE_CONDUCTOR = item("redstone_conductor", "Redstone Conductor");
    public static final ItemEntry<Item> REDSTONE_CONVERTER = item("redstone_converter", "Redstone Converter");
    public static final ItemEntry<Item> REDSTONE_STORER = item("redstone_storer", "Redstone Storer");
    public static final ItemEntry<Item> INDIGO = item("indigo", "Indigo");
    public static final ItemEntry<Item> AZURE_GLASS = item("azure_glass", "Azure Glass");
    public static final ItemEntry<Item> BIZARRERIE = item("bizarrerie", "Bizarrerie");
    public static final ItemEntry<Item> REDSTONE_AI = item("redstone_ai", "Redstone AI");
    public static final ItemEntry<Item> REDSTONE_AI_ADVANCED = item("redstone_ai_advanced", "Advanced Redstone AI");
    public static final ItemEntry<Item> HYDRAULIC_WIDGET = item("hydraulic_widget", "Hydraulic Widget");
    public static final ItemEntry<Item> DETECTOR = item("detector", "Detector");

    // Moulds (use manual models - texture names differ from item names)
    static {
        REGISTRATE.creativeModeTab(() -> TENCreativeModeTabs.TOOL_TAB);
    }
    public static final ItemEntry<Item> MOULD_GEAR = REGISTRATE
            .item("mould_gear", Item::new)
            .lang("Gear Mould")
            .model((ctx, prov) -> prov.generated(ctx::getEntry, prov.modLoc("item/model_gear")))
            .register();
    public static final ItemEntry<Item> MOULD_PLATE = REGISTRATE
            .item("mould_plate", Item::new)
            .lang("Plate Mould")
            .model((ctx, prov) -> prov.generated(ctx::getEntry, prov.modLoc("item/model_plate")))
            .register();
    public static final ItemEntry<Item> MOULD_ROD = REGISTRATE
            .item("mould_rod", Item::new)
            .lang("Rod Mould")
            .model((ctx, prov) -> prov.generated(ctx::getEntry, prov.modLoc("item/model_rod")))
            .register();
    public static final ItemEntry<Item> MOULD_STRING = REGISTRATE
            .item("mould_string", Item::new)
            .lang("String Mould")
            .model((ctx, prov) -> prov.generated(ctx::getEntry, prov.modLoc("item/model_string")))
            .register();

    // Upgrades
    public static final ItemEntry<? extends UpgradeItem> AUGMENTED_LEVELUP = upgrade("augmented_levelup", "Upgrade: Augmented Kit", p -> new LevelupAug());
    public static final ItemEntry<? extends UpgradeItem> POWERED_LEVELUP = upgrade("powered_levelup", "Upgrade: Powered Kit", p -> new LevelupPower());
    public static final ItemEntry<? extends UpgradeItem> RELIC_LEVELUP = upgrade("relic_levelup", "Upgrade: Shulker Kit", p -> new LevelupShulker());
    public static final ItemEntry<? extends UpgradeItem> PHOTOSYN_LEVELUP = upgrade("photosyn_levelup", "Upgrade: Photosynthetic Power", p -> new LevelupSyn());
    public static final ItemEntry<? extends UpgradeItem> RANGE_LEVELUP = upgrade("range_levelup", "Upgrade: Range Expansion", p -> new LevelupRg());
    public static final ItemEntry<? extends UpgradeItem> SMOKE_LEVELUP = upgrade("smoke_levelup", "Upgrade: Smoking Professor", p -> new LevelupSmoke());
    public static final ItemEntry<? extends UpgradeItem> BLAST_LEVELUP = upgrade("blast_levelup", "Upgrade: Blasting Professor", p -> new LevelupBlast());
    public static final ItemEntry<? extends UpgradeItem> POTION_LEVELUP = upgrade("potion_levelup", "Upgrade: Potion Effect Extraction", p -> new LevelupPotion());
    public static final ItemEntry<? extends UpgradeItem> STREAM_LEVELUP = upgrade("stream_levelup", "Upgrade: Starlight Energy Deliverance", p -> new LevelupStream());
    public static final ItemEntry<? extends UpgradeItem> KNOWLEDGE_LEVELUP = upgrade("knowledge_levelup", "Upgrade: Knowledge Expansion", p -> new LevelupKnow());
    public static final ItemEntry<? extends UpgradeItem> ICE_LEVELUP = upgrade("ice_levelup", "Upgrade: Frozen Soil Drilling", p -> new LevelupIce());
    public static final ItemEntry<? extends UpgradeItem> MAGMA_LEVELUP = upgrade("magma_levelup", "Upgrade: Mantle Drilling", p -> new LevelupMagma());
    public static final ItemEntry<? extends UpgradeItem> MINERAL_LEVELUP = upgrade("mineral_levelup", "Upgrade: Mineral Detection", p -> new LevelupMineral());

    private static ItemEntry<Item> item(String name, String englishName) {
        return REGISTRATE.item(name, Item::new)
                .lang(englishName)
                .register();
    }

    private static <T extends UpgradeItem> ItemEntry<T> upgrade(String name, String englishName, NonNullFunction<Item.Properties, T> factory) {
        return REGISTRATE.item(name, factory)
                .lang(englishName)
                .register();
    }

    public static void init() {}
}
