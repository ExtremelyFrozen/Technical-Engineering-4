package com.modularmc.ten.common.data;

import com.modularmc.ten.TEN;
import com.modularmc.ten.common.item.ChannelConnectorItem;
import com.modularmc.ten.common.item.EnergyUnitItem;
import com.modularmc.ten.common.item.SpannerItem;
import com.modularmc.ten.common.item.upgrades.*;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.LinkedHashMap;
import java.util.function.Supplier;

import static com.modularmc.ten.common.registry.Registration.ITEMS;

public class TENItems {

    public static final LinkedHashMap<String, String> ZH_NAMES = new LinkedHashMap<>();

    // ══════════════════════════════════════════════════════════════════
    // Bulk material variant registration (dusts, ingots, nuggets, etc.)
    // ══════════════════════════════════════════════════════════════════

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

    // ══════════════════════════════════════════════════════════════════
    // Raw Materials
    // ══════════════════════════════════════════════════════════════════

    public static final DeferredHolder<Item, Item> RAW_TIN
            = simpleItem("raw_tin", "粗锡");
    public static final DeferredHolder<Item, Item> RAW_NICKEL
            = simpleItem("raw_nickel", "粗镍");

    // ══════════════════════════════════════════════════════════════════
    // Crafting Components
    // ══════════════════════════════════════════════════════════════════

    public static final DeferredHolder<Item, Item> REDSTONE_CONDUCTOR
            = simpleItem("redstone_conductor", "红石传导元件");
    public static final DeferredHolder<Item, Item> REDSTONE_CONVERTER
            = simpleItem("redstone_converter", "红石转化元件");
    public static final DeferredHolder<Item, Item> REDSTONE_STORER
            = simpleItem("redstone_storer", "红石转存元件");
    public static final DeferredHolder<Item, Item> INDIGO
            = simpleItem("indigo", "靛青");
    public static final DeferredHolder<Item, Item> AZURE_GLASS
            = simpleItem("azure_glass", "琉璃");
    public static final DeferredHolder<Item, Item> BIZARRERIE
            = simpleItem("bizarrerie", "奇异物质");
    public static final DeferredHolder<Item, Item> REDSTONE_AI
            = simpleItem("redstone_ai", "红石智能芯片");
    public static final DeferredHolder<Item, Item> REDSTONE_AI_ADVANCED
            = simpleItem("redstone_ai_advanced", "自律红石智能芯片");
    public static final DeferredHolder<Item, Item> HYDRAULIC_WIDGET
            = simpleItem("hydraulic_widget", "液压组件");
    public static final DeferredHolder<Item, Item> DETECTOR
            = simpleItem("detector", "反射探测仪");

    public static final DeferredHolder<Item, Item> ROYAL_JELLY
            = simpleItem("royal_jelly", "蜂王浆");
    public static final DeferredHolder<Item, Item> SPICY_JELLY
            = simpleItem("spicy_jelly", "香辣蜂王浆");

    // ══════════════════════════════════════════════════════════════════
    // Moulds
    // ══════════════════════════════════════════════════════════════════

    public static final DeferredHolder<Item, Item> MOULD_GEAR
            = mould("mould_gear", "模具-齿轮");
    public static final DeferredHolder<Item, Item> MOULD_PLATE
            = mould("mould_plate", "模具-板");
    public static final DeferredHolder<Item, Item> MOULD_ROD
            = mould("mould_rod", "模具-杆");
    public static final DeferredHolder<Item, Item> MOULD_STRING
            = mould("mould_string", "模具-线");
    public static final DeferredHolder<Item, Item> MOULD_COMPRESSED_SMALL
            = mould("mould_compressed_small", "模具-2x2压缩");
    public static final DeferredHolder<Item, Item> MOULD_COMPRESSED_LARGE
            = mould("mould_compressed_large", "模具-3x3压缩");
    public static final DeferredHolder<Item, Item> MOULD_SPLIT
            = mould("mould_split", "模具-解压缩");
    public static final DeferredHolder<Item, Item> MOULD_COIN
            = mould("mould_coin", "模具-币");
    public static final DeferredHolder<Item, Item> MOULD_DENSE_PLATE
            = mould("mould_dense_plate", "模具-致密板");

    // ══════════════════════════════════════════════════════════════════
    // Tools
    // ══════════════════════════════════════════════════════════════════

    public static final DeferredHolder<Item, SpannerItem> SPANNER
            = tool("spanner", "扳手", () -> new SpannerItem(new Item.Properties().setId(itemKey("spanner"))));
    public static final DeferredHolder<Item, EnergyUnitItem> ENERGY_CAPACITY
            = tool("energy_capacity", "能量单元", () -> new EnergyUnitItem(new Item.Properties().setId(itemKey("energy_capacity"))));
    public static final DeferredHolder<Item, ChannelConnectorItem> CHANNEL_CONNECTOR
            = tool("channel_connector", "频道桥接器", () -> new ChannelConnectorItem(new Item.Properties().setId(itemKey("channel_connector"))));

    // ══════════════════════════════════════════════════════════════════
    // Upgrades
    // ══════════════════════════════════════════════════════════════════

    public static final DeferredHolder<Item, UpgradeItem> AUGMENTED_LEVELUP
            = upgrade("augmented_levelup", "升级：增强组件", () -> new LevelupAug(new Item.Properties().setId(itemKey("augmented_levelup"))));
    public static final DeferredHolder<Item, UpgradeItem> POWERED_LEVELUP
            = upgrade("powered_levelup", "升级：充能组件", () -> new LevelupPower(new Item.Properties().setId(itemKey("powered_levelup"))));
    public static final DeferredHolder<Item, UpgradeItem> RELIC_LEVELUP
            = upgrade("relic_levelup", "升级：潜影组件", () -> new LevelupShulker(new Item.Properties().setId(itemKey("relic_levelup"))));
    public static final DeferredHolder<Item, UpgradeItem> PHOTOSYN_LEVELUP
            = upgrade("photosyn_levelup", "升级：光合供能抑制", () -> new LevelupSyn(new Item.Properties().setId(itemKey("photosyn_levelup"))));
    public static final DeferredHolder<Item, UpgradeItem> RANGE_LEVELUP
            = upgrade("range_levelup", "升级：作用范围扩展", () -> new LevelupRg(new Item.Properties().setId(itemKey("range_levelup"))));
    public static final DeferredHolder<Item, UpgradeItem> SMOKE_LEVELUP
            = upgrade("smoke_levelup", "升级：食物专业烟熏", () -> new LevelupSmoke(new Item.Properties().setId(itemKey("smoke_levelup"))));
    public static final DeferredHolder<Item, UpgradeItem> BLAST_LEVELUP
            = upgrade("blast_levelup", "升级：矿物专业冶炼", () -> new LevelupBlast(new Item.Properties().setId(itemKey("blast_levelup"))));
    public static final DeferredHolder<Item, UpgradeItem> POTION_LEVELUP
            = upgrade("potion_levelup", "升级：药水效能榨取", () -> new LevelupPotion(new Item.Properties().setId(itemKey("potion_levelup"))));
    public static final DeferredHolder<Item, UpgradeItem> STREAM_LEVELUP
            = upgrade("stream_levelup", "升级：星辉能量传输", () -> new LevelupStream(new Item.Properties().setId(itemKey("stream_levelup"))));
    public static final DeferredHolder<Item, UpgradeItem> KNOWLEDGE_LEVELUP
            = upgrade("knowledge_levelup", "升级：知识储备扩充", () -> new LevelupKnow(new Item.Properties().setId(itemKey("knowledge_levelup"))));
    public static final DeferredHolder<Item, UpgradeItem> ICE_LEVELUP
            = upgrade("ice_levelup", "升级：地下冻土开采", () -> new LevelupIce(new Item.Properties().setId(itemKey("ice_levelup"))));
    public static final DeferredHolder<Item, UpgradeItem> MAGMA_LEVELUP
            = upgrade("magma_levelup", "升级：地幔深层钻井", () -> new LevelupMagma(new Item.Properties().setId(itemKey("magma_levelup"))));
    public static final DeferredHolder<Item, UpgradeItem> MINERAL_LEVELUP
            = upgrade("mineral_levelup", "升级：矿物探测仪器", () -> new LevelupMineral(new Item.Properties().setId(itemKey("mineral_levelup"))));

    // ══════════════════════════════════════════════════════════════════
    // Helpers
    // ══════════════════════════════════════════════════════════════════

    private static ResourceKey<Item> itemKey(String name) {
        return ResourceKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath(TEN.MOD_ID, name));
    }

    /** Register a simple {@link Item} with default properties. */
    private static DeferredHolder<Item, Item> simpleItem(String name, String cn) {
        ZH_NAMES.put(name, cn);
        var key = itemKey(name);
        return ITEMS.register(name, () -> new Item(new Item.Properties().setId(key)));
    }

    /** Register a mould item (simple Item, textured). */
    private static DeferredHolder<Item, Item> mould(String name, String cn) {
        ZH_NAMES.put(name, cn);
        var key = itemKey(name);
        return ITEMS.register(name, () -> new Item(new Item.Properties().setId(key)));
    }

    /** Register a tool item with custom class. */
    private static <T extends Item> DeferredHolder<Item, T> tool(String name, String cn, Supplier<T> factory) {
        ZH_NAMES.put(name, cn);
        return ITEMS.register(name, factory);
    }

    /** Register an upgrade item. */
    private static <T extends UpgradeItem> DeferredHolder<Item, T> upgrade(String name, String cn, Supplier<T> factory) {
        ZH_NAMES.put(name, cn);
        return ITEMS.register(name, factory);
    }

    /**
     * Bulk-register material variant items from {@link Mat} entries.
     * Each variant becomes {@code <material>_<category>} (e.g. {@code tin_ingot}).
     */
    @SafeVarargs
    private static void registerVariants(String category, String categoryEn, String categoryCn,
                                          Mat... materials) {
        for (Mat mat : materials) {
            String name = mat.id + "_" + category;
            mat.markRegistered(category);
            ZH_NAMES.put(name, mat.cn + categoryCn);
            var key = itemKey(name);
            ITEMS.register(name, () -> new Item(new Item.Properties().setId(key)));
        }
    }

    /**
     * Dummy init to trigger static initializers (kept for compatibility).
     */
    public static void init() {}
}
