package com.modularmc.ten.common.data;

import com.modularmc.ten.common.item.upgrades.*;

import net.minecraft.world.item.Item;

import com.tterrag.registrate.util.entry.ItemEntry;

import static com.modularmc.ten.common.registry.Registration.REGISTRATE;

public class TENItems {

    static {
        REGISTRATE.creativeModeTab(() -> TENCreativeModeTabs.ITEM_TAB);
    }

    // Materials - Dusts
    public static final ItemEntry<Item> IRON_DUST = REGISTRATE
            .item("iron_dust", Item::new).register();
    public static final ItemEntry<Item> GOLD_DUST = REGISTRATE
            .item("gold_dust", Item::new).register();
    public static final ItemEntry<Item> COPPER_DUST = REGISTRATE
            .item("copper_dust", Item::new).register();
    public static final ItemEntry<Item> TIN_DUST = REGISTRATE
            .item("tin_dust", Item::new).register();
    public static final ItemEntry<Item> NICKEL_DUST = REGISTRATE
            .item("nickel_dust", Item::new).register();
    public static final ItemEntry<Item> POWERED_TIN_DUST = REGISTRATE
            .item("powered_tin_dust", Item::new).register();
    public static final ItemEntry<Item> CHLORIUM_DUST = REGISTRATE
            .item("chlorium_dust", Item::new).register();

    // Materials - Ingots
    public static final ItemEntry<Item> TIN_INGOT = REGISTRATE
            .item("tin_ingot", Item::new).register();
    public static final ItemEntry<Item> NICKEL_INGOT = REGISTRATE
            .item("nickel_ingot", Item::new).register();
    public static final ItemEntry<Item> POWERED_TIN_INGOT = REGISTRATE
            .item("powered_tin_ingot", Item::new).register();
    public static final ItemEntry<Item> CHLORIUM_INGOT = REGISTRATE
            .item("chlorium_ingot", Item::new).register();

    // Materials - Nuggets
    public static final ItemEntry<Item> TIN_NUGGET = REGISTRATE
            .item("tin_nugget", Item::new).register();
    public static final ItemEntry<Item> NICKEL_NUGGET = REGISTRATE
            .item("nickel_nugget", Item::new).register();
    public static final ItemEntry<Item> POWERED_TIN_NUGGET = REGISTRATE
            .item("powered_tin_nugget", Item::new).register();
    public static final ItemEntry<Item> CHLORIUM_NUGGET = REGISTRATE
            .item("chlorium_nugget", Item::new).register();

    // Materials - Plates
    public static final ItemEntry<Item> IRON_PLATE = REGISTRATE
            .item("iron_plate", Item::new).register();
    public static final ItemEntry<Item> GOLD_PLATE = REGISTRATE
            .item("gold_plate", Item::new).register();
    public static final ItemEntry<Item> COPPER_PLATE = REGISTRATE
            .item("copper_plate", Item::new).register();
    public static final ItemEntry<Item> TIN_PLATE = REGISTRATE
            .item("tin_plate", Item::new).register();
    public static final ItemEntry<Item> NICKEL_PLATE = REGISTRATE
            .item("nickel_plate", Item::new).register();
    public static final ItemEntry<Item> POWERED_TIN_PLATE = REGISTRATE
            .item("powered_tin_plate", Item::new).register();
    public static final ItemEntry<Item> CHLORIUM_PLATE = REGISTRATE
            .item("chlorium_plate", Item::new).register();

    // Materials - Gears
    public static final ItemEntry<Item> IRON_GEAR = REGISTRATE
            .item("iron_gear", Item::new).register();
    public static final ItemEntry<Item> GOLD_GEAR = REGISTRATE
            .item("gold_gear", Item::new).register();
    public static final ItemEntry<Item> COPPER_GEAR = REGISTRATE
            .item("copper_gear", Item::new).register();
    public static final ItemEntry<Item> TIN_GEAR = REGISTRATE
            .item("tin_gear", Item::new).register();
    public static final ItemEntry<Item> NICKEL_GEAR = REGISTRATE
            .item("nickel_gear", Item::new).register();
    public static final ItemEntry<Item> POWERED_TIN_GEAR = REGISTRATE
            .item("powered_tin_gear", Item::new).register();
    public static final ItemEntry<Item> CHLORIUM_GEAR = REGISTRATE
            .item("chlorium_gear", Item::new).register();

    // Raw Materials
    public static final ItemEntry<Item> RAW_TIN = REGISTRATE
            .item("raw_tin", Item::new).register();
    public static final ItemEntry<Item> RAW_NICKEL = REGISTRATE
            .item("raw_nickel", Item::new).register();

    // Crafting Components
    public static final ItemEntry<Item> REDSTONE_CONDUCTOR = REGISTRATE
            .item("redstone_conductor", Item::new).register();
    public static final ItemEntry<Item> REDSTONE_CONVERTER = REGISTRATE
            .item("redstone_converter", Item::new).register();
    public static final ItemEntry<Item> REDSTONE_STORER = REGISTRATE
            .item("redstone_storer", Item::new).register();
    public static final ItemEntry<Item> INDIGO = REGISTRATE
            .item("indigo", Item::new).register();
    public static final ItemEntry<Item> AZURE_GLASS = REGISTRATE
            .item("azure_glass", Item::new).register();
    public static final ItemEntry<Item> BIZARRERIE = REGISTRATE
            .item("bizarrerie", Item::new).register();
    public static final ItemEntry<Item> REDSTONE_AI = REGISTRATE
            .item("redstone_ai", Item::new).register();
    public static final ItemEntry<Item> REDSTONE_AI_ADVANCED = REGISTRATE
            .item("redstone_ai_advanced", Item::new).register();
    public static final ItemEntry<Item> HYDRAULIC_WIDGET = REGISTRATE
            .item("hydraulic_widget", Item::new).register();
    public static final ItemEntry<Item> DETECTOR = REGISTRATE
            .item("detector", Item::new).register();

    // Moulds (use manual models - texture names differ from item names)
    static {
        REGISTRATE.creativeModeTab(() -> TENCreativeModeTabs.TOOL_TAB);
    }
    public static final ItemEntry<Item> MOULD_GEAR = REGISTRATE
            .item("mould_gear", Item::new)
            .model((ctx, prov) -> prov.generated(ctx::getEntry, prov.modLoc("item/model_gear")))
            .register();
    public static final ItemEntry<Item> MOULD_PLATE = REGISTRATE
            .item("mould_plate", Item::new)
            .model((ctx, prov) -> prov.generated(ctx::getEntry, prov.modLoc("item/model_plate")))
            .register();
    public static final ItemEntry<Item> MOULD_ROD = REGISTRATE
            .item("mould_rod", Item::new)
            .model((ctx, prov) -> prov.generated(ctx::getEntry, prov.modLoc("item/model_rod")))
            .register();
    public static final ItemEntry<Item> MOULD_STRING = REGISTRATE
            .item("mould_string", Item::new)
            .model((ctx, prov) -> prov.generated(ctx::getEntry, prov.modLoc("item/model_string")))
            .register();

    // Upgrades
    public static final ItemEntry<? extends UpgradeItem> AUGMENTED_LEVELUP = REGISTRATE
            .item("augmented_levelup", p -> new LevelupAug()).register();
    public static final ItemEntry<? extends UpgradeItem> POWERED_LEVELUP = REGISTRATE
            .item("powered_levelup", p -> new LevelupPower()).register();
    public static final ItemEntry<? extends UpgradeItem> RELIC_LEVELUP = REGISTRATE
            .item("relic_levelup", p -> new LevelupShulker()).register();
    public static final ItemEntry<? extends UpgradeItem> PHOTOSYN_LEVELUP = REGISTRATE
            .item("photosyn_levelup", p -> new LevelupSyn()).register();
    public static final ItemEntry<? extends UpgradeItem> RANGE_LEVELUP = REGISTRATE
            .item("range_levelup", p -> new LevelupRg()).register();
    public static final ItemEntry<? extends UpgradeItem> SMOKE_LEVELUP = REGISTRATE
            .item("smoke_levelup", p -> new LevelupSmoke()).register();
    public static final ItemEntry<? extends UpgradeItem> BLAST_LEVELUP = REGISTRATE
            .item("blast_levelup", p -> new LevelupBlast()).register();
    public static final ItemEntry<? extends UpgradeItem> POTION_LEVELUP = REGISTRATE
            .item("potion_levelup", p -> new LevelupPotion()).register();
    public static final ItemEntry<? extends UpgradeItem> STREAM_LEVELUP = REGISTRATE
            .item("stream_levelup", p -> new LevelupStream()).register();
    public static final ItemEntry<? extends UpgradeItem> KNOWLEDGE_LEVELUP = REGISTRATE
            .item("knowledge_levelup", p -> new LevelupKnow()).register();
    public static final ItemEntry<? extends UpgradeItem> ICE_LEVELUP = REGISTRATE
            .item("ice_levelup", p -> new LevelupIce()).register();
    public static final ItemEntry<? extends UpgradeItem> MAGMA_LEVELUP = REGISTRATE
            .item("magma_levelup", p -> new LevelupMagma()).register();
    public static final ItemEntry<? extends UpgradeItem> MINERAL_LEVELUP = REGISTRATE
            .item("mineral_levelup", p -> new LevelupMineral()).register();

    public static void init() {}
}
