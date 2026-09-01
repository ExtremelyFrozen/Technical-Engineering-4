package com.modularmc.ten.common.data;

import com.modularmc.ten.common.item.ChannelConnectorItem;
import com.modularmc.ten.common.item.EnergyUnitItem;
import com.modularmc.ten.common.item.SpannerItem;
import com.modularmc.ten.common.item.upgrades.*;

import net.minecraft.world.item.Item;

import com.tterrag.registrate.util.entry.ItemEntry;
import com.tterrag.registrate.util.nullness.NonNullFunction;

import static com.modularmc.ten.common.registry.Registration.REGISTRATE;

public class TENItems {

    public static final java.util.LinkedHashMap<String, String> ZH_NAMES = new java.util.LinkedHashMap<>();

    static {
        REGISTRATE.creativeModeTab(() -> TENCreativeModeTabs.ITEM_TAB);
    }

    // === Bulk material variant registration ===
    static {
        // --- Dusts ---
        registerVariants("dust", "Dust", "粉",
                Mat.IRON, Mat.GOLD, Mat.COPPER, Mat.TIN, Mat.NICKEL, Mat.POWERED_TIN, Mat.CHLORIUM,
                Mat.NETHERITE, Mat.DIAMOND, Mat.EMERALD, Mat.LAPIS, Mat.QUARTZ, Mat.AMETHYST,
                Mat.MUSHRIUM, Mat.STARLIGHT);

        // --- Ingots ---
        registerVariants("ingot", "Ingot", "锭",
                Mat.TIN, Mat.NICKEL, Mat.POWERED_TIN, Mat.CHLORIUM,
                Mat.MUSHRIUM);

        // --- Nuggets ---
        registerVariants("nugget", "Nugget", "粒",
                Mat.TIN, Mat.NICKEL, Mat.POWERED_TIN, Mat.CHLORIUM,
                Mat.COPPER,
                Mat.NETHERITE, Mat.DIAMOND, Mat.EMERALD, Mat.LAPIS, Mat.QUARTZ,
                Mat.MUSHRIUM);

        // --- Plates ---
        registerVariants("plate", "Plate", "板",
                Mat.IRON, Mat.GOLD, Mat.COPPER, Mat.TIN, Mat.NICKEL, Mat.POWERED_TIN, Mat.CHLORIUM,
                Mat.NETHERITE, Mat.DIAMOND, Mat.EMERALD, Mat.LAPIS, Mat.QUARTZ, Mat.AMETHYST, Mat.REDSTONE,
                Mat.MUSHRIUM);

        // --- Gears ---
        registerVariants("gear", "Gear", "齿轮",
                Mat.IRON, Mat.GOLD, Mat.COPPER, Mat.TIN, Mat.NICKEL, Mat.POWERED_TIN, Mat.CHLORIUM,
                Mat.NETHERITE, Mat.DIAMOND, Mat.EMERALD, Mat.LAPIS, Mat.QUARTZ, Mat.AMETHYST, Mat.REDSTONE,
                Mat.MUSHRIUM);

        // --- Rods ---
        registerVariants("rod", "Rod", "杆",
                Mat.IRON, Mat.GOLD, Mat.COPPER, Mat.TIN, Mat.NICKEL, Mat.POWERED_TIN, Mat.CHLORIUM,
                Mat.NETHERITE, Mat.MUSHRIUM);

        // --- Wires ---
        registerVariants("wire", "Wire", "线",
                Mat.IRON, Mat.GOLD, Mat.COPPER, Mat.TIN, Mat.NICKEL, Mat.POWERED_TIN, Mat.CHLORIUM,
                Mat.NETHERITE, Mat.MUSHRIUM);
    }

    // Raw Materials
    public static final ItemEntry<Item> RAW_TIN = texturedItem("raw_tin", "Raw Tin", "粗锡", "item/material/raw/raw_tin");
    public static final ItemEntry<Item> RAW_NICKEL = texturedItem("raw_nickel", "Raw Nickel", "粗镍", "item/material/raw/raw_nickel");

    // Crafting Components
    public static final ItemEntry<Item> REDSTONE_CONDUCTOR = texturedItem("redstone_conductor", "Redstone Conductor", "红石传导元件", "item/crafting/redstone_conductor");
    public static final ItemEntry<Item> REDSTONE_CONVERTER = texturedItem("redstone_converter", "Redstone Converter", "红石转化元件", "item/crafting/redstone_converter");
    public static final ItemEntry<Item> REDSTONE_STORER = texturedItem("redstone_storer", "Redstone Storer", "红石转存元件", "item/crafting/redstone_storer");
    public static final ItemEntry<Item> INDIGO = texturedItem("indigo", "Indigo", "靛青", "item/crafting/indigo");
    public static final ItemEntry<Item> AZURE_GLASS = texturedItem("azure_glass", "Azure Glass", "琉璃", "item/crafting/azure_glass");
    public static final ItemEntry<Item> BIZARRERIE = texturedItem("bizarrerie", "Bizarrerie", "奇异物质", "item/crafting/bizarrerie");
    public static final ItemEntry<Item> REDSTONE_AI = texturedItem("redstone_ai", "Redstone AI", "红石智能芯片", "item/crafting/redstone_ai");
    public static final ItemEntry<Item> REDSTONE_AI_ADVANCED = texturedItem("redstone_ai_advanced", "Advanced Redstone AI", "自律红石智能芯片", "item/crafting/redstone_ai_advanced");
    public static final ItemEntry<Item> HYDRAULIC_WIDGET = texturedItem("hydraulic_widget", "Hydraulic Widget", "液压组件", "item/crafting/hydraulic_widget");
    public static final ItemEntry<Item> DETECTOR = texturedItem("detector", "Detector", "反射探测仪", "item/crafting/detector");

    public static final ItemEntry<Item> ROYAL_JELLY = texturedItem("royal_jelly", "Royal Jelly", "蜂王浆", "item/royal_jelly");
    public static final ItemEntry<Item> SPICY_JELLY = texturedItem("spicy_jelly", "Spicy Jelly", "香辣蜂王浆", "item/spicy_jelly");

    // === Moulds ===
    static {
        REGISTRATE.creativeModeTab(() -> TENCreativeModeTabs.TOOL_TAB);
    }

    private static ItemEntry<Item> mould(String name, String englishName, String cn, String texturePath) {
        ZH_NAMES.put(name, cn);
        return REGISTRATE.item(name, Item::new)
                .lang(englishName)
                .model((ctx, prov) -> prov.generated(ctx::getEntry, prov.modLoc(texturePath)))
                .register();
    }

    public static final ItemEntry<Item> MOULD_GEAR = mould("mould_gear", "Gear Mould", "模具-齿轮", "item/mold/model_gear");
    public static final ItemEntry<Item> MOULD_PLATE = mould("mould_plate", "Plate Mould", "模具-板", "item/mold/model_plate");
    public static final ItemEntry<Item> MOULD_ROD = mould("mould_rod", "Rod Mould", "模具-杆", "item/mold/model_rod");
    public static final ItemEntry<Item> MOULD_STRING = mould("mould_string", "String Mould", "模具-线", "item/mold/model_string");
    public static final ItemEntry<Item> MOULD_COMPRESSED_SMALL = mould("mould_compressed_small", "Compressed-small Mould", "模具-2x2压缩", "item/mold/compressed_small");
    public static final ItemEntry<Item> MOULD_COMPRESSED_LARGE = mould("mould_compressed_large", "Compressed-large Mould", "模具-3x3压缩", "item/mold/compressed_large");
    public static final ItemEntry<Item> MOULD_SPLIT = mould("mould_split", "Split Mould", "模具-解压缩", "item/mold/split");
    public static final ItemEntry<Item> MOULD_COIN = mould("mould_coin", "Coin Mould", "模具-币", "item/mold/coin");
    public static final ItemEntry<Item> MOULD_DENSE_PLATE = mould("mould_dense_plate", "Dense Plate Mould", "模具-致密板", "item/mold/dense_plate");

    // === Tools ===
    private static <T extends Item> ItemEntry<T> tool(String name, String englishName, String cn,
                                                      NonNullFunction<Item.Properties, T> factory, String texturePath) {
        ZH_NAMES.put(name, cn);
        return REGISTRATE.item(name, factory)
                .lang(englishName)
                .model((ctx, prov) -> prov.generated(ctx::getEntry, prov.modLoc(texturePath)))
                .register();
    }

    public static final ItemEntry<SpannerItem> SPANNER = tool("spanner", "Spanner", "扳手", SpannerItem::new, "item/spanner");
    public static final ItemEntry<EnergyUnitItem> ENERGY_CAPACITY = tool("energy_capacity", "Energy Module", "能量模块", EnergyUnitItem::new, "item/energy_capacity");
    public static final ItemEntry<ChannelConnectorItem> CHANNEL_CONNECTOR = tool("channel_connector", "Channel Connector", "频道连接器", ChannelConnectorItem::new, "item/channel_connector");
    // Upgrades
    public static final ItemEntry<? extends UpgradeItem> AUGMENTED_LEVELUP = upgrade("augmented_levelup", "Upgrade: Augmented Kit", "升级：增强组件", p -> new LevelupAug());
    public static final ItemEntry<? extends UpgradeItem> POWERED_LEVELUP = upgrade("powered_levelup", "Upgrade: Powered Kit", "升级：充能组件", p -> new LevelupPower());
    public static final ItemEntry<? extends UpgradeItem> RELIC_LEVELUP = upgrade("relic_levelup", "Upgrade: Shulker Kit", "升级：潜影组件", p -> new LevelupShulker());
    public static final ItemEntry<? extends UpgradeItem> PHOTOSYN_LEVELUP = upgrade("photosyn_levelup", "Upgrade: Photosynthetic Power", "升级：光合供能抑制", p -> new LevelupSyn());
    public static final ItemEntry<? extends UpgradeItem> RANGE_LEVELUP = upgrade("range_levelup", "Upgrade: Range Expansion", "升级：作用范围扩展", p -> new LevelupRg());
    public static final ItemEntry<? extends UpgradeItem> SMOKE_LEVELUP = upgrade("smoke_levelup", "Upgrade: Smoking Professor", "升级：食物专业烟熏", p -> new LevelupSmoke());
    public static final ItemEntry<? extends UpgradeItem> BLAST_LEVELUP = upgrade("blast_levelup", "Upgrade: Blasting Professor", "升级：矿物专业冶炼", p -> new LevelupBlast());
    public static final ItemEntry<? extends UpgradeItem> POTION_LEVELUP = upgrade("potion_levelup", "Upgrade: Potion Effect Extraction", "升级：药水效能榨取", p -> new LevelupPotion());
    public static final ItemEntry<? extends UpgradeItem> STREAM_LEVELUP = upgrade("stream_levelup", "Upgrade: Starlight Energy Deliverance", "升级：星辉能量传输", p -> new LevelupStream());
    public static final ItemEntry<? extends UpgradeItem> KNOWLEDGE_LEVELUP = upgrade("knowledge_levelup", "Upgrade: Knowledge Expansion", "升级：知识储备扩充", p -> new LevelupKnow());
    public static final ItemEntry<? extends UpgradeItem> ICE_LEVELUP = upgrade("ice_levelup", "Upgrade: Frozen Soil Drilling", "升级：地下冻土开采", p -> new LevelupIce());
    public static final ItemEntry<? extends UpgradeItem> MAGMA_LEVELUP = upgrade("magma_levelup", "Upgrade: Mantle Drilling", "升级：地幔深层钻井", p -> new LevelupMagma());
    public static final ItemEntry<? extends UpgradeItem> MINERAL_LEVELUP = upgrade("mineral_levelup", "Upgrade: Mineral Detection", "升级：矿物探测仪器", p -> new LevelupMineral());

    private static ItemEntry<Item> item(String name, String englishName, String cn) {
        ZH_NAMES.put(name, cn);
        return REGISTRATE.item(name, Item::new)
                .lang(englishName)
                .register();
    }

    private static ItemEntry<Item> texturedItem(String name, String englishName, String cn, String texturePath) {
        ZH_NAMES.put(name, cn);
        return REGISTRATE.item(name, Item::new)
                .lang(englishName)
                .model((ctx, prov) -> prov.generated(ctx::getEntry, prov.modLoc(texturePath)))
                .register();
    }

    private static <T extends UpgradeItem> ItemEntry<T> upgrade(String name, String englishName, String cn, NonNullFunction<Item.Properties, T> factory) {
        ZH_NAMES.put(name, cn);
        return REGISTRATE.item(name, factory)
                .lang(englishName)
                .model((ctx, prov) -> prov.generated(ctx::getEntry, prov.modLoc("item/upgrade/" + name)))
                .register();
    }

    @SafeVarargs
    private static void registerVariants(String category, String categoryEn, String categoryCn,
                                         NonNullFunction<Item.Properties, ? extends Item> factory,
                                         Mat... materials) {
        for (Mat mat : materials) {
            String name = mat.id + "_" + category;
            mat.markRegistered(category);
            String englishName = mat.englishName(categoryEn);
            String chineseName = mat.cn + categoryCn;
            String texturePath = "item/material/" + category + "/" + name;

            ZH_NAMES.put(name, chineseName);
            REGISTRATE.item(name, factory)
                    .lang(englishName)
                    .model((ctx, prov) -> prov.generated(ctx::getEntry, prov.modLoc(texturePath)))
                    .register();
        }
    }

    private static void registerVariants(String category, String categoryEn, String categoryCn,
                                         Mat... materials) {
        registerVariants(category, categoryEn, categoryCn, Item::new, materials);
    }

    public static void init() {}
}
