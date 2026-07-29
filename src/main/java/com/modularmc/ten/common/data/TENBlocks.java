package com.modularmc.ten.common.data;

import com.modularmc.ten.TEN;
import com.modularmc.ten.common.block.machine.BaseMachineBlock;
import com.modularmc.ten.common.block.machine.CableBased;
import com.modularmc.ten.common.block.machine.ChannelBlock;
import com.modularmc.ten.common.block.machine.EngineBlock;
import com.modularmc.ten.common.block.machine.HorizontalMachineBlock;
import com.modularmc.ten.common.item.TENBaseBlockItem;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import static com.modularmc.ten.common.registry.Registration.BLOCKS;
import static com.modularmc.ten.common.registry.Registration.ITEMS;

public class TENBlocks {

    public static final LinkedHashMap<String, String> ZH_NAMES = new LinkedHashMap<>();
    public static final LinkedHashMap<String, String> EN_NAMES = new LinkedHashMap<>();

    // ═════════════════════════════════════════════════════════════════════
    // Semantic block collections — populated by helper methods for use by
    // TENTagProvider, SpannerItem, and other consumers.
    // Each collection preserves registration order.
    // ═════════════════════════════════════════════════════════════════════

    /** Ore blocks (tin_ore, nickel_ore, deep_tin_ore, deep_nickel_ore). */
    private static final List<DeferredHolder<Block, Block>> ORES = new ArrayList<>(4);
    /** Storage blocks (tin_block, nickel_block, powered_tin_block, chlorium_block). */
    private static final List<DeferredHolder<Block, Block>> STORAGE_BLOCKS = new ArrayList<>(4);
    /** Raw storage blocks (raw_tin_block, raw_nickel_block). */
    private static final List<DeferredHolder<Block, Block>> RAW_STORAGE_BLOCKS = new ArrayList<>(2);
    /** Machine blocks (all 12 machines). */
    private static final List<DeferredHolder<Block, ?>> MACHINES = new ArrayList<>(12);
    /** Engine blocks (all 4 engines). */
    private static final List<DeferredHolder<Block, ?>> ENGINES = new ArrayList<>(4);
    /** Cable blocks (all 4 cables). */
    private static final List<DeferredHolder<Block, ?>> CABLES = new ArrayList<>(4);
    /** Pipe blocks (all 3 pipes). */
    private static final List<DeferredHolder<Block, ?>> PIPES = new ArrayList<>(3);
    /** Cell blocks (energy_cell, creative_energy_cell). */
    private static final List<DeferredHolder<Block, ?>> CELLS = new ArrayList<>(2);
    /** Channel blocks (channel_energy, channel_item, channel_fluid). */
    private static final List<DeferredHolder<Block, ?>> CHANNELS = new ArrayList<>(3);
    /**
     * All functional (machine tag) blocks: machines + engines + cables + pipes + cells + channels.
     * Populated after each category helper via addAll.
     */
    private static final List<DeferredHolder<Block, ?>> ALL_FUNCTIONAL = new ArrayList<>(28);

    // ── Read-only accessors ─────────────────────────────────────────────

    public static List<DeferredHolder<Block, Block>> getOres() {
        return Collections.unmodifiableList(ORES);
    }

    public static List<DeferredHolder<Block, Block>> getStorageBlocks() {
        return Collections.unmodifiableList(STORAGE_BLOCKS);
    }

    public static List<DeferredHolder<Block, Block>> getRawStorageBlocks() {
        return Collections.unmodifiableList(RAW_STORAGE_BLOCKS);
    }

    public static List<DeferredHolder<Block, ?>> getMachines() {
        return Collections.unmodifiableList(MACHINES);
    }

    public static List<DeferredHolder<Block, ?>> getEngines() {
        return Collections.unmodifiableList(ENGINES);
    }

    public static List<DeferredHolder<Block, ?>> getCables() {
        return Collections.unmodifiableList(CABLES);
    }

    public static List<DeferredHolder<Block, ?>> getPipes() {
        return Collections.unmodifiableList(PIPES);
    }

    public static List<DeferredHolder<Block, ?>> getCells() {
        return Collections.unmodifiableList(CELLS);
    }

    public static List<DeferredHolder<Block, ?>> getChannels() {
        return Collections.unmodifiableList(CHANNELS);
    }

    /**
     * All blocks that should appear in the {@code kenergyengineering:machines} and
     * {@code kenergyengineering:wrench_dismantleable} tags.
     */
    public static List<DeferredHolder<Block, ?>> getAllFunctional() {
        return Collections.unmodifiableList(ALL_FUNCTIONAL);
    }

    /**
     * All blocks that require correct tool for drops and need mineable/pickaxe tag:
     * ores + storage blocks + raw storage blocks + all functional blocks.
     */
    public static List<DeferredHolder<Block, ?>> getAllMineablePickaxe() {
        var result = new ArrayList<>(ALL_FUNCTIONAL);
        result.addAll(0, ORES);
        result.addAll(STORAGE_BLOCKS);
        result.addAll(RAW_STORAGE_BLOCKS);
        return Collections.unmodifiableList(result);
    }

    /**
     * All blocks that require iron tier or better:
     * ores + storage blocks + raw storage blocks (functional blocks keep vanilla stone tier).
     */
    public static List<DeferredHolder<Block, ?>> getAllNeedsIronTool() {
        var result = new ArrayList<>(ORES);
        result.addAll(STORAGE_BLOCKS);
        result.addAll(RAW_STORAGE_BLOCKS);
        return Collections.unmodifiableList(result);
    }

    // ═══════════════════════════════════════════════════
    // Ores
    // ═══════════════════════════════════════════════════

    public static final DeferredHolder<Block, Block> TIN_ORE = ore("tin_ore", "Tin Ore", "锡矿石", 3, MapColor.STONE);
    public static final DeferredHolder<Block, Block> NICKEL_ORE = ore("nickel_ore", "Nickel Ore", "镍矿石", 4, MapColor.STONE);
    public static final DeferredHolder<Block, Block> DEEP_TIN_ORE = ore("deep_tin_ore", "Deep Tin Ore", "深层锡矿石", 4, MapColor.DEEPSLATE);
    public static final DeferredHolder<Block, Block> DEEP_NICKEL_ORE = ore("deep_nickel_ore", "Deep Nickel Ore", "深层镍矿石", 5, MapColor.DEEPSLATE);

    // ═══════════════════════════════════════════════════
    // Storage Blocks
    // ═══════════════════════════════════════════════════

    public static final DeferredHolder<Block, Block> TIN_BLOCK = storage("tin_block", "Block of Tin", "锡块", 4);
    public static final DeferredHolder<Block, Block> NICKEL_BLOCK = storage("nickel_block", "Block of Nickel", "镍块", 5);
    public static final DeferredHolder<Block, Block> POWERED_TIN_BLOCK = storage("powered_tin_block", "Block of Powered Tin", "充能锡块", 5.5f);
    public static final DeferredHolder<Block, Block> CHLORIUM_BLOCK = storage("chlorium_block", "Block of Chlorium", "叶绿块", 5);
    public static final DeferredHolder<Block, Block> RAW_TIN_BLOCK = rawStorage("raw_tin_block", "Raw Tin Block", "粗锡块", 3);
    public static final DeferredHolder<Block, Block> RAW_NICKEL_BLOCK = rawStorage("raw_nickel_block", "Raw Nickel Block", "粗镍块", 4);

    // ═══════════════════════════════════════════════════
    // Machines
    // ═══════════════════════════════════════════════════

    public static final DeferredHolder<Block, HorizontalMachineBlock> MACHINE_SMELTER = machine("machine_smelter", "Smelter", "熔炼机");
    public static final DeferredHolder<Block, HorizontalMachineBlock> MACHINE_PULVERIZER = machine("machine_pulverizer", "Pulverizer", "粉碎机");
    public static final DeferredHolder<Block, HorizontalMachineBlock> MACHINE_COMPRESSOR = machine("machine_compressor", "Compressor", "压缩机");
    public static final DeferredHolder<Block, HorizontalMachineBlock> MACHINE_REFINER = machine("machine_refiner", "Refiner", "精炼机");
    public static final DeferredHolder<Block, HorizontalMachineBlock> MACHINE_INDUCTION_FURNACE = machine("machine_induction_furnace", "Induction Furnace", "感应炉");
    public static final DeferredHolder<Block, HorizontalMachineBlock> MACHINE_PSIONICANT = machine("machine_psionicant", "Psionicant", "灵能处理器");
    public static final DeferredHolder<Block, HorizontalMachineBlock> MACHINE_BEACON = machine("machine_beacon_simulator", "Beacon Simulator", "信标模拟机");
    public static final DeferredHolder<Block, HorizontalMachineBlock> MACHINE_MOB_RIPPER = machine("machine_mob_ripper", "Mob Ripper", "生物啃噬者");
    public static final DeferredHolder<Block, HorizontalMachineBlock> MACHINE_QUARRY = machine("machine_quarry", "Quarry", "采矿场");
    public static final DeferredHolder<Block, HorizontalMachineBlock> MACHINE_ENCHFLU = machine("machine_enchantment_flusher", "Enchantment Flusher", "祛魔机");
    public static final DeferredHolder<Block, HorizontalMachineBlock> MACHINE_CONDENSER = machine("machine_matter_condenser", "Matter Condenser", "物质结晶器");
    public static final DeferredHolder<Block, HorizontalMachineBlock> MACHINE_FARM = machine("machine_farm_manager", "Farm Manager", "农场管理机");

    // ═══════════════════════════════════════════════════
    // Engines
    // ═══════════════════════════════════════════════════

    public static final DeferredHolder<Block, EngineBlock> ENGINE_EXTRACTION = engine("engine_extraction", "Extraction Engine", "萃取引擎");
    public static final DeferredHolder<Block, EngineBlock> ENGINE_METAL = engine("engine_metal", "Metal Engine", "金属引擎");
    public static final DeferredHolder<Block, EngineBlock> ENGINE_BIOMASS = engine("engine_biomass", "Biomass Engine", "生物质引擎");
    public static final DeferredHolder<Block, EngineBlock> ENGINE_SOLAR = engine("engine_solar", "Solar Engine", "光合引擎");

    // ═══════════════════════════════════════════════════
    // Cables
    // ═══════════════════════════════════════════════════

    public static final DeferredHolder<Block, CableBased> CABLE = cable("cable", "Glass Energy Cable", "玻璃能量线缆");
    public static final DeferredHolder<Block, CableBased> CABLE_QUARTZ = cable("cable_quartz", "Quartz Energy Cable", "石英能量线缆");
    public static final DeferredHolder<Block, CableBased> CABLE_AZURE = cable("cable_azure", "Azure Energy Cable", "蔚蓝能量线缆");
    public static final DeferredHolder<Block, CableBased> CABLE_STAR = cable("cable_star", "Starlight Cable", "星辉能量线缆");

    // ═══════════════════════════════════════════════════
    // Pipes
    // ═══════════════════════════════════════════════════

    public static final DeferredHolder<Block, CableBased> PIPE = pipe("pipe", "Item Pipe", "物品管道");
    public static final DeferredHolder<Block, CableBased> PIPE_WHITE = pipe("pipe_white", "Conditional Item Pipe", "限定物品管道");
    public static final DeferredHolder<Block, CableBased> PIPE_BLACK = pipe("pipe_black", "Exceptional Item Pipe", "排除物品管道");

    // ═══════════════════════════════════════════════════
    // Cells
    // ═══════════════════════════════════════════════════

    public static final DeferredHolder<Block, HorizontalMachineBlock> CELL = tooltipCell("energy_cell", "Energy Cell", "能量单元");
    public static final DeferredHolder<Block, HorizontalMachineBlock> CREATIVE_CELL = cell("creative_energy_cell", "Creative Energy Cell", "创造能量单元");

    // ═══════════════════════════════════════════════════
    // Channels
    // ═══════════════════════════════════════════════════

    public static final DeferredHolder<Block, ChannelBlock> CHANNEL_ENERGY = channel("channel_energy", "Energy Channel", "能量频道");
    public static final DeferredHolder<Block, ChannelBlock> CHANNEL_ITEM = channel("channel_item", "Item Channel", "物品频道");
    public static final DeferredHolder<Block, ChannelBlock> CHANNEL_FLUID = channel("channel_fluid", "Fluid Channel", "流体频道");

    // ═══════════════════════════════════════════════════
    // Helpers — block registration & BlockItem
    // ═══════════════════════════════════════════════════

    private static DeferredBlock<Block> ore(String name, String en, String cn, double hardness, MapColor color) {
        EN_NAMES.put(name, en);
        ZH_NAMES.put(name, cn);
        boolean isDeep = color == MapColor.DEEPSLATE;
        var holder = BLOCKS.registerBlock(name, Block::new, props -> props
                .mapColor(color)
                .strength((float) hardness)
                .requiresCorrectToolForDrops()
                .sound(isDeep ? SoundType.DEEPSLATE : SoundType.STONE));
        ITEMS.registerSimpleBlockItem(name, holder);
        ORES.add(holder);
        return holder;
    }

    private static DeferredBlock<Block> storage(String name, String en, String cn, float hardness) {
        EN_NAMES.put(name, en);
        ZH_NAMES.put(name, cn);
        var holder = BLOCKS.registerBlock(name, Block::new, props -> props
                .mapColor(MapColor.METAL)
                .strength(hardness)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL));
        ITEMS.registerSimpleBlockItem(name, holder);
        STORAGE_BLOCKS.add(holder);
        return holder;
    }

    private static DeferredBlock<Block> rawStorage(String name, String en, String cn, float hardness) {
        EN_NAMES.put(name, en);
        ZH_NAMES.put(name, cn);
        var holder = BLOCKS.registerBlock(name, Block::new, props -> props
                .mapColor(MapColor.STONE)
                .strength(hardness)
                .requiresCorrectToolForDrops()
                .sound(SoundType.STONE));
        ITEMS.registerSimpleBlockItem(name, holder);
        RAW_STORAGE_BLOCKS.add(holder);
        return holder;
    }

    private static DeferredBlock<HorizontalMachineBlock> machine(String name, String en, String cn) {
        EN_NAMES.put(name, en);
        ZH_NAMES.put(name, cn);
        var holder = BLOCKS.registerBlock(name, HorizontalMachineBlock::new, props -> props
                .strength(4.0f, 8.0f)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL));
        registerTENBaseBlockItem(name, holder);
        MACHINES.add(holder);
        ALL_FUNCTIONAL.add(holder);
        return holder;
    }

    private static DeferredBlock<EngineBlock> engine(String name, String en, String cn) {
        EN_NAMES.put(name, en);
        ZH_NAMES.put(name, cn);
        var holder = BLOCKS.<EngineBlock>registerBlock(name, EngineBlock::new, props -> props
                .mapColor(MapColor.METAL)
                .strength(4.0f, 8.0f)
                .requiresCorrectToolForDrops()
                .noOcclusion()
                .sound(SoundType.METAL)
                .lightLevel(state -> state.getValue(BaseMachineBlock.ACTIVE) ? 6 : 0));
        registerTENBaseBlockItem(name, holder);
        ENGINES.add(holder);
        ALL_FUNCTIONAL.add(holder);
        return holder;
    }

    private static DeferredBlock<CableBased> cable(String name, String en, String cn) {
        EN_NAMES.put(name, en);
        ZH_NAMES.put(name, cn);
        var holder = BLOCKS.registerBlock(name, CableBased::new, props -> props
                .strength(1.5f)
                .noOcclusion()
                .sound(SoundType.GLASS));
        registerTENBaseBlockItem(name, holder);
        CABLES.add(holder);
        ALL_FUNCTIONAL.add(holder);
        return holder;
    }

    private static DeferredBlock<CableBased> pipe(String name, String en, String cn) {
        EN_NAMES.put(name, en);
        ZH_NAMES.put(name, cn);
        var holder = BLOCKS.registerBlock(name, CableBased::new, props -> props
                .strength(1.5f)
                .noOcclusion()
                .sound(SoundType.GLASS));
        ITEMS.registerSimpleBlockItem(name, holder);
        PIPES.add(holder);
        ALL_FUNCTIONAL.add(holder);
        return holder;
    }

    private static DeferredBlock<HorizontalMachineBlock> tooltipCell(String name, String en, String cn) {
        EN_NAMES.put(name, en);
        ZH_NAMES.put(name, cn);
        var holder = BLOCKS.registerBlock(name, HorizontalMachineBlock::new, props -> props
                .strength(5.0f, 10.0f)
                .requiresCorrectToolForDrops()
                .noOcclusion()
                .sound(SoundType.METAL));
        registerTENBaseBlockItem(name, holder);
        CELLS.add(holder);
        ALL_FUNCTIONAL.add(holder);
        return holder;
    }

    private static DeferredBlock<HorizontalMachineBlock> cell(String name, String en, String cn) {
        EN_NAMES.put(name, en);
        ZH_NAMES.put(name, cn);
        var holder = BLOCKS.registerBlock(name, HorizontalMachineBlock::new, props -> props
                .strength(5.0f, 10.0f)
                .requiresCorrectToolForDrops()
                .noOcclusion()
                .sound(SoundType.METAL));
        ITEMS.registerSimpleBlockItem(name, holder);
        CELLS.add(holder);
        ALL_FUNCTIONAL.add(holder);
        return holder;
    }

    private static DeferredBlock<ChannelBlock> channel(String name, String en, String cn) {
        EN_NAMES.put(name, en);
        ZH_NAMES.put(name, cn);
        var holder = BLOCKS.registerBlock(name, ChannelBlock::new, props -> props
                .strength(3.0f, 6.0f)
                .requiresCorrectToolForDrops()
                .noOcclusion()
                .sound(SoundType.METAL));
        ITEMS.registerSimpleBlockItem(name, holder);
        CHANNELS.add(holder);
        ALL_FUNCTIONAL.add(holder);
        return holder;
    }

    /**
     * Register a TENBaseBlockItem (with custom tooltip) for the given block.
     * Follows the 26.1.2 pattern of explicitly setting the item's ResourceKey.
     */
    private static <T extends Block> void registerTENBaseBlockItem(String name, DeferredHolder<Block, T> holder) {
        var key = ResourceKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath(TEN.MOD_ID, name));
        ITEMS.register(name, () -> new TENBaseBlockItem(holder.get(), new Item.Properties().useBlockDescriptionPrefix().setId(key)));
    }

    /**
     * Dummy init to trigger static initializers (kept for compatibility with old call pattern).
     */
    public static void init() {}
}
