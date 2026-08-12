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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Supplier;

import static com.modularmc.ten.common.registry.Registration.ITEMS;

public class TENItems {

    public static final LinkedHashMap<String, String> ZH_NAMES = new LinkedHashMap<>();
    public static final LinkedHashMap<String, String> EN_NAMES = new LinkedHashMap<>();

    /**
     * Ordered holder list for all registered mould items, populated by
     * the {@code mould()} helper. Read-only access via {@link #getMouldHolders()}.
     * Used by {@link com.modularmc.ten.data.TENTagProvider} to generate the
     * {@code kenergyengineering:moulds} tag.
     */
    private static final List<DeferredHolder<Item, Item>> MOULD_HOLDERS = new ArrayList<>(9);

    /**
     * Ordered holder list for all material variant items registered via
     * {@link #registerVariants(String, String, String, Mat...)}.
     * Preserves category order (dust -> ingot -> nugget -> plate -> gear -> rod -> wire)
     * and per-call material argument order.
     * Populated during class initialization, exposed read-only via {@link #getMaterialVariantHolders()}.
     */
    private static final List<DeferredHolder<Item, Item>> MATERIAL_VARIANT_HOLDERS = new ArrayList<>(79);

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

    public static final DeferredHolder<Item, Item> RAW_TIN = simpleItem("raw_tin", "Raw Tin", "粗锡");
    public static final DeferredHolder<Item, Item> RAW_NICKEL = simpleItem("raw_nickel", "Raw Nickel", "粗镍");

    // ══════════════════════════════════════════════════════════════════
    // Crafting Components
    // ══════════════════════════════════════════════════════════════════

    public static final DeferredHolder<Item, Item> REDSTONE_CONDUCTOR = simpleItem("redstone_conductor", "Redstone Conductor", "红石传导元件");
    public static final DeferredHolder<Item, Item> REDSTONE_CONVERTER = simpleItem("redstone_converter", "Redstone Converter", "红石转化元件");
    public static final DeferredHolder<Item, Item> REDSTONE_STORER = simpleItem("redstone_storer", "Redstone Storer", "红石转存元件");
    public static final DeferredHolder<Item, Item> INDIGO = simpleItem("indigo", "Indigo", "靛青");
    public static final DeferredHolder<Item, Item> AZURE_GLASS = simpleItem("azure_glass", "Azure Glass", "琉璃");
    public static final DeferredHolder<Item, Item> BIZARRERIE = simpleItem("bizarrerie", "Bizarrerie", "奇异物质");
    public static final DeferredHolder<Item, Item> REDSTONE_AI = simpleItem("redstone_ai", "Redstone AI", "红石智能芯片");
    public static final DeferredHolder<Item, Item> REDSTONE_AI_ADVANCED = simpleItem("redstone_ai_advanced", "Advanced Redstone AI", "自律红石智能芯片");
    public static final DeferredHolder<Item, Item> HYDRAULIC_WIDGET = simpleItem("hydraulic_widget", "Hydraulic Widget", "液压组件");
    public static final DeferredHolder<Item, Item> DETECTOR = simpleItem("detector", "Detector", "反射探测仪");

    public static final DeferredHolder<Item, Item> ROYAL_JELLY = simpleItem("royal_jelly", "Royal Jelly", "蜂王浆");
    public static final DeferredHolder<Item, Item> SPICY_JELLY = simpleItem("spicy_jelly", "Spicy Jelly", "香辣蜂王浆");

    // ══════════════════════════════════════════════════════════════════
    // Moulds
    // ══════════════════════════════════════════════════════════════════

    public static final DeferredHolder<Item, Item> MOULD_GEAR = mould("mould_gear", "Gear Mould", "模具-齿轮");
    public static final DeferredHolder<Item, Item> MOULD_PLATE = mould("mould_plate", "Plate Mould", "模具-板");
    public static final DeferredHolder<Item, Item> MOULD_ROD = mould("mould_rod", "Rod Mould", "模具-杆");
    public static final DeferredHolder<Item, Item> MOULD_STRING = mould("mould_string", "String Mould", "模具-线");
    public static final DeferredHolder<Item, Item> MOULD_COMPRESSED_SMALL = mould("mould_compressed_small", "Compressed Small Mould", "模具-2x2压缩");
    public static final DeferredHolder<Item, Item> MOULD_COMPRESSED_LARGE = mould("mould_compressed_large", "Compressed Large Mould", "模具-3x3压缩");
    public static final DeferredHolder<Item, Item> MOULD_SPLIT = mould("mould_split", "Split Mould", "模具-解压缩");
    public static final DeferredHolder<Item, Item> MOULD_COIN = mould("mould_coin", "Coin Mould", "模具-币");
    public static final DeferredHolder<Item, Item> MOULD_DENSE_PLATE = mould("mould_dense_plate", "Dense Plate Mould", "模具-致密板");

    // ══════════════════════════════════════════════════════════════════
    // Tools
    // ══════════════════════════════════════════════════════════════════

    public static final DeferredHolder<Item, SpannerItem> SPANNER = tool("spanner", "Spanner", "扳手", () -> new SpannerItem(new Item.Properties().setId(itemKey("spanner"))));
    public static final DeferredHolder<Item, EnergyUnitItem> ENERGY_CAPACITY = tool("energy_capacity", "Energy Unit", "能量单元", () -> new EnergyUnitItem(new Item.Properties().setId(itemKey("energy_capacity"))));
    public static final DeferredHolder<Item, ChannelConnectorItem> CHANNEL_CONNECTOR = tool("channel_connector", "Channel Connector", "频道连接器", () -> new ChannelConnectorItem(new Item.Properties().setId(itemKey("channel_connector"))));

    // ══════════════════════════════════════════════════════════════════
    // Upgrades
    // ══════════════════════════════════════════════════════════════════

    public static final DeferredHolder<Item, UpgradeItem> AUGMENTED_LEVELUP = upgrade("augmented_levelup", "Upgrade: Augmented Kit", "升级：增强组件", () -> new LevelupAug(new Item.Properties().setId(itemKey("augmented_levelup"))));
    public static final DeferredHolder<Item, UpgradeItem> POWERED_LEVELUP = upgrade("powered_levelup", "Upgrade: Powered Kit", "升级：充能组件", () -> new LevelupPower(new Item.Properties().setId(itemKey("powered_levelup"))));
    public static final DeferredHolder<Item, UpgradeItem> RELIC_LEVELUP = upgrade("relic_levelup", "Upgrade: Shulker Kit", "升级：潜影组件", () -> new LevelupShulker(new Item.Properties().setId(itemKey("relic_levelup"))));
    public static final DeferredHolder<Item, UpgradeItem> PHOTOSYN_LEVELUP = upgrade("photosyn_levelup", "Upgrade: Photosynthetic Power", "升级：光合供能抑制", () -> new LevelupSyn(new Item.Properties().setId(itemKey("photosyn_levelup"))));
    public static final DeferredHolder<Item, UpgradeItem> RANGE_LEVELUP = upgrade("range_levelup", "Upgrade: Range Expansion", "升级：作用范围扩展", () -> new LevelupRg(new Item.Properties().setId(itemKey("range_levelup"))));
    public static final DeferredHolder<Item, UpgradeItem> SMOKE_LEVELUP = upgrade("smoke_levelup", "Upgrade: Smoking Professor", "升级：食物专业烟熏", () -> new LevelupSmoke(new Item.Properties().setId(itemKey("smoke_levelup"))));
    public static final DeferredHolder<Item, UpgradeItem> BLAST_LEVELUP = upgrade("blast_levelup", "Upgrade: Blasting Professor", "升级：矿物专业冶炼", () -> new LevelupBlast(new Item.Properties().setId(itemKey("blast_levelup"))));
    public static final DeferredHolder<Item, UpgradeItem> POTION_LEVELUP = upgrade("potion_levelup", "Upgrade: Potion Effect Extraction", "升级：药水效能榨取", () -> new LevelupPotion(new Item.Properties().setId(itemKey("potion_levelup"))));
    public static final DeferredHolder<Item, UpgradeItem> STREAM_LEVELUP = upgrade("stream_levelup", "Upgrade: Starlight Energy Deliverance", "升级：星辉能量传输", () -> new LevelupStream(new Item.Properties().setId(itemKey("stream_levelup"))));
    public static final DeferredHolder<Item, UpgradeItem> KNOWLEDGE_LEVELUP = upgrade("knowledge_levelup", "Upgrade: Knowledge Expansion", "升级：知识储备扩充", () -> new LevelupKnow(new Item.Properties().setId(itemKey("knowledge_levelup"))));
    public static final DeferredHolder<Item, UpgradeItem> ICE_LEVELUP = upgrade("ice_levelup", "Upgrade: Frozen Soil Drilling", "升级：地下冻土开采", () -> new LevelupIce(new Item.Properties().setId(itemKey("ice_levelup"))));
    public static final DeferredHolder<Item, UpgradeItem> MAGMA_LEVELUP = upgrade("magma_levelup", "Upgrade: Mantle Drilling", "升级：地幔深层钻井", () -> new LevelupMagma(new Item.Properties().setId(itemKey("magma_levelup"))));
    public static final DeferredHolder<Item, UpgradeItem> MINERAL_LEVELUP = upgrade("mineral_levelup", "Upgrade: Mineral Detection", "升级：矿物探测仪器", () -> new LevelupMineral(new Item.Properties().setId(itemKey("mineral_levelup"))));

    // ══════════════════════════════════════════════════════════════════
    // Helpers
    // ══════════════════════════════════════════════════════════════════

    private static ResourceKey<Item> itemKey(String name) {
        return ResourceKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath(TEN.MOD_ID, name));
    }

    /** Register a simple {@link Item} with default properties. */
    private static DeferredHolder<Item, Item> simpleItem(String name, String en, String cn) {
        ZH_NAMES.put(name, cn);
        EN_NAMES.put(name, en);
        var key = itemKey(name);
        return ITEMS.register(name, () -> new Item(new Item.Properties().setId(key)));
    }

    /** Register a mould item (simple Item, textured), adding to MOULD_HOLDERS. */
    private static DeferredHolder<Item, Item> mould(String name, String en, String cn) {
        ZH_NAMES.put(name, cn);
        EN_NAMES.put(name, en);
        var key = itemKey(name);
        var holder = ITEMS.register(name, () -> new Item(new Item.Properties().setId(key)));
        MOULD_HOLDERS.add(holder);
        return holder;
    }

    /** Register a tool item with custom class. */
    private static <T extends Item> DeferredHolder<Item, T> tool(String name, String en, String cn, Supplier<T> factory) {
        ZH_NAMES.put(name, cn);
        EN_NAMES.put(name, en);
        return ITEMS.register(name, factory);
    }

    /** Register an upgrade item. */
    private static <T extends UpgradeItem> DeferredHolder<Item, T> upgrade(String name, String en, String cn, Supplier<T> factory) {
        ZH_NAMES.put(name, cn);
        EN_NAMES.put(name, en);
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
            EN_NAMES.put(name, mat.englishName() + " " + categoryEn);
            var key = itemKey(name);
            var holder = ITEMS.register(name, () -> new Item(new Item.Properties().setId(key)));
            MATERIAL_VARIANT_HOLDERS.add(holder);
        }
    }

    /**
     * Returns an unmodifiable view of all material variant holders,
     * in the exact registration order (category then material argument order).
     * <p>
     * Used by {@link TENCreativeModeTabs} to populate the ITEM_TAB.
     */
    public static List<DeferredHolder<Item, Item>> getMaterialVariantHolders() {
        return Collections.unmodifiableList(MATERIAL_VARIANT_HOLDERS);
    }

    /**
     * Returns an unmodifiable view of all mould item holders,
     * in registration order.
     * <p>
     * Used by {@link com.modularmc.ten.data.TENTagProvider} to generate
     * the {@code kenergyengineering:moulds} tag.
     */
    public static List<DeferredHolder<Item, Item>> getMouldHolders() {
        return Collections.unmodifiableList(MOULD_HOLDERS);
    }

    /**
     * Dummy init to trigger static initializers (kept for compatibility).
     */
    public static void init() {}
}
