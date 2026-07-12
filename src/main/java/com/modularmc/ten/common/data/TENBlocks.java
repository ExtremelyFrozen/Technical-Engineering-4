package com.modularmc.ten.common.data;

import com.modularmc.ten.TEN;
import com.modularmc.ten.common.block.machine.BaseMachineBlock;
import com.modularmc.ten.common.block.machine.CableBased;
import com.modularmc.ten.common.block.machine.DirectionalMachineBlock;
import com.modularmc.ten.common.block.machine.EngineBlock;
import com.modularmc.ten.common.block.machine.HorizontalMachineBlock;
import com.modularmc.ten.common.item.TENBaseBlockItem;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.LinkedHashMap;

import static com.modularmc.ten.common.registry.Registration.BLOCKS;
import static com.modularmc.ten.common.registry.Registration.ITEMS;

public class TENBlocks {

    public static final LinkedHashMap<String, String> ZH_NAMES = new LinkedHashMap<>();

    // ═══════════════════════════════════════════════════
    // Ores
    // ═══════════════════════════════════════════════════

    public static final DeferredHolder<Block, Block> TIN_ORE = ore("tin_ore", "锡矿石", 3, MapColor.STONE);
    public static final DeferredHolder<Block, Block> NICKEL_ORE = ore("nickel_ore", "镍矿石", 4, MapColor.STONE);
    public static final DeferredHolder<Block, Block> DEEP_TIN_ORE = ore("deep_tin_ore", "深层锡矿石", 4, MapColor.DEEPSLATE);
    public static final DeferredHolder<Block, Block> DEEP_NICKEL_ORE = ore("deep_nickel_ore", "深层镍矿石", 5, MapColor.DEEPSLATE);

    // ═══════════════════════════════════════════════════
    // Storage Blocks
    // ═══════════════════════════════════════════════════

    public static final DeferredHolder<Block, Block> TIN_BLOCK = storage("tin_block", "锡块", 4);
    public static final DeferredHolder<Block, Block> NICKEL_BLOCK = storage("nickel_block", "镍块", 5);
    public static final DeferredHolder<Block, Block> POWERED_TIN_BLOCK = storage("powered_tin_block", "充能锡块", 5.5f);
    public static final DeferredHolder<Block, Block> CHLORIUM_BLOCK = storage("chlorium_block", "叶绿块", 5);
    public static final DeferredHolder<Block, Block> RAW_TIN_BLOCK = rawStorage("raw_tin_block", "粗锡块", 3);
    public static final DeferredHolder<Block, Block> RAW_NICKEL_BLOCK = rawStorage("raw_nickel_block", "粗镍块", 4);

    // ═══════════════════════════════════════════════════
    // Machines
    // ═══════════════════════════════════════════════════

    public static final DeferredHolder<Block, HorizontalMachineBlock> MACHINE_SMELTER
            = machine("machine_smelter", "熔炼机");
    public static final DeferredHolder<Block, HorizontalMachineBlock> MACHINE_PULVERIZER
            = machine("machine_pulverizer", "粉碎机");
    public static final DeferredHolder<Block, HorizontalMachineBlock> MACHINE_COMPRESSOR
            = machine("machine_compressor", "压缩机");
    public static final DeferredHolder<Block, HorizontalMachineBlock> MACHINE_REFINER
            = machine("machine_refiner", "精炼机");
    public static final DeferredHolder<Block, HorizontalMachineBlock> MACHINE_INDUCTION_FURNACE
            = machine("machine_induction_furnace", "感应炉");
    public static final DeferredHolder<Block, HorizontalMachineBlock> MACHINE_PSIONICANT
            = machine("machine_psionicant", "灵能处理器");
    public static final DeferredHolder<Block, HorizontalMachineBlock> MACHINE_BEACON
            = machine("machine_beacon_simulator", "信标模拟机");
    public static final DeferredHolder<Block, HorizontalMachineBlock> MACHINE_MOB_RIPPER
            = machine("machine_mob_ripper", "生物啃噬者");
    public static final DeferredHolder<Block, HorizontalMachineBlock> MACHINE_QUARRY
            = machine("machine_quarry", "采矿场");
    public static final DeferredHolder<Block, HorizontalMachineBlock> MACHINE_ENCHFLU
            = machine("machine_enchantment_flusher", "祛魔机");
    public static final DeferredHolder<Block, HorizontalMachineBlock> MACHINE_CONDENSER
            = machine("machine_matter_condenser", "物质结晶器");
    public static final DeferredHolder<Block, HorizontalMachineBlock> MACHINE_FARM
            = machine("machine_farm_manager", "农场管理机");

    // ═══════════════════════════════════════════════════
    // Engines
    // ═══════════════════════════════════════════════════

    public static final DeferredHolder<Block, EngineBlock> ENGINE_EXTRACTION
            = engine("engine_extraction", "萃取引擎");
    public static final DeferredHolder<Block, EngineBlock> ENGINE_METAL
            = engine("engine_metal", "金属引擎");
    public static final DeferredHolder<Block, EngineBlock> ENGINE_BIOMASS
            = engine("engine_biomass", "生物质引擎");
    public static final DeferredHolder<Block, EngineBlock> ENGINE_SOLAR
            = engine("engine_solar", "光合引擎");

    // ═══════════════════════════════════════════════════
    // Cables
    // ═══════════════════════════════════════════════════

    public static final DeferredHolder<Block, CableBased> CABLE
            = cable("cable", "玻璃能量线缆");
    public static final DeferredHolder<Block, CableBased> CABLE_QUARTZ
            = cable("cable_quartz", "石英能量线缆");
    public static final DeferredHolder<Block, CableBased> CABLE_AZURE
            = cable("cable_azure", "蔚蓝能量线缆");
    public static final DeferredHolder<Block, CableBased> CABLE_STAR
            = cable("cable_star", "星辉能量线缆");

    // ═══════════════════════════════════════════════════
    // Pipes
    // ═══════════════════════════════════════════════════

    public static final DeferredHolder<Block, CableBased> PIPE
            = pipe("pipe", "物品管道");
    public static final DeferredHolder<Block, CableBased> PIPE_WHITE
            = pipe("pipe_white", "限定物品管道");
    public static final DeferredHolder<Block, CableBased> PIPE_BLACK
            = pipe("pipe_black", "排除物品管道");

    // ═══════════════════════════════════════════════════
    // Cells
    // ═══════════════════════════════════════════════════

    public static final DeferredHolder<Block, HorizontalMachineBlock> CELL
            = tooltipCell("energy_cell", "能量单元");
    public static final DeferredHolder<Block, HorizontalMachineBlock> CREATIVE_CELL
            = cell("creative_energy_cell", "创造能量单元");

    // ═══════════════════════════════════════════════════
    // Channels
    // ═══════════════════════════════════════════════════

    public static final DeferredHolder<Block, DirectionalMachineBlock> CHANNEL_ENERGY
            = channel("channel_energy", "能量频道");
    public static final DeferredHolder<Block, DirectionalMachineBlock> CHANNEL_ITEM
            = channel("channel_item", "物品频道");
    public static final DeferredHolder<Block, DirectionalMachineBlock> CHANNEL_FLUID
            = channel("channel_fluid", "流体频道");

    // ═══════════════════════════════════════════════════
    // Helpers — block registration & BlockItem
    // ═══════════════════════════════════════════════════

    private static DeferredBlock<Block> ore(String name, String cn, double hardness, MapColor color) {
        ZH_NAMES.put(name, cn);
        boolean isDeep = color == MapColor.DEEPSLATE;
        var holder = BLOCKS.registerBlock(name, Block::new, props -> props
                .mapColor(color)
                .strength((float) hardness)
                .requiresCorrectToolForDrops()
                .sound(isDeep ? SoundType.DEEPSLATE : SoundType.STONE));
        ITEMS.registerSimpleBlockItem(name, holder);
        return holder;
    }

    private static DeferredBlock<Block> storage(String name, String cn, float hardness) {
        ZH_NAMES.put(name, cn);
        var holder = BLOCKS.registerBlock(name, Block::new, props -> props
                .mapColor(MapColor.METAL)
                .strength(hardness)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL));
        ITEMS.registerSimpleBlockItem(name, holder);
        return holder;
    }

    private static DeferredBlock<Block> rawStorage(String name, String cn, float hardness) {
        ZH_NAMES.put(name, cn);
        var holder = BLOCKS.registerBlock(name, Block::new, props -> props
                .mapColor(MapColor.STONE)
                .strength(hardness)
                .requiresCorrectToolForDrops()
                .sound(SoundType.STONE));
        ITEMS.registerSimpleBlockItem(name, holder);
        return holder;
    }

    private static DeferredBlock<HorizontalMachineBlock> machine(String name, String cn) {
        ZH_NAMES.put(name, cn);
        var holder = BLOCKS.registerBlock(name, HorizontalMachineBlock::new, props -> props
                .strength(4.0f, 8.0f)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL));
        registerTENBaseBlockItem(name, holder);
        return holder;
    }

    private static DeferredBlock<EngineBlock> engine(String name, String cn) {
        ZH_NAMES.put(name, cn);
        var holder = BLOCKS.<EngineBlock>registerBlock(name, EngineBlock::new, props -> props
                .mapColor(MapColor.METAL)
                .strength(4.0f, 8.0f)
                .requiresCorrectToolForDrops()
                .noOcclusion()
                .sound(SoundType.METAL)
                .lightLevel(state -> state.getValue(BaseMachineBlock.ACTIVE) ? 6 : 0));
        registerTENBaseBlockItem(name, holder);
        return holder;
    }

    private static DeferredBlock<CableBased> cable(String name, String cn) {
        ZH_NAMES.put(name, cn);
        var holder = BLOCKS.registerBlock(name, CableBased::new, props -> props
                .strength(1.5f)
                .noOcclusion()
                .sound(SoundType.GLASS));
        registerTENBaseBlockItem(name, holder);
        return holder;
    }

    private static DeferredBlock<CableBased> pipe(String name, String cn) {
        ZH_NAMES.put(name, cn);
        var holder = BLOCKS.registerBlock(name, CableBased::new, props -> props
                .strength(1.5f)
                .noOcclusion()
                .sound(SoundType.GLASS));
        ITEMS.registerSimpleBlockItem(name, holder);
        return holder;
    }

    private static DeferredBlock<HorizontalMachineBlock> tooltipCell(String name, String cn) {
        ZH_NAMES.put(name, cn);
        var holder = BLOCKS.registerBlock(name, HorizontalMachineBlock::new, props -> props
                .strength(5.0f, 10.0f)
                .requiresCorrectToolForDrops()
                .noOcclusion()
                .sound(SoundType.METAL));
        registerTENBaseBlockItem(name, holder);
        return holder;
    }

    private static DeferredBlock<HorizontalMachineBlock> cell(String name, String cn) {
        ZH_NAMES.put(name, cn);
        var holder = BLOCKS.registerBlock(name, HorizontalMachineBlock::new, props -> props
                .strength(5.0f, 10.0f)
                .requiresCorrectToolForDrops()
                .noOcclusion()
                .sound(SoundType.METAL));
        ITEMS.registerSimpleBlockItem(name, holder);
        return holder;
    }

    private static DeferredBlock<DirectionalMachineBlock> channel(String name, String cn) {
        ZH_NAMES.put(name, cn);
        var holder = BLOCKS.registerBlock(name, DirectionalMachineBlock::new, props -> props
                .strength(3.0f, 6.0f)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL));
        ITEMS.registerSimpleBlockItem(name, holder);
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
