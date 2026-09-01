package com.modularmc.ten.common.data;

import com.modularmc.ten.common.block.machine.CableBased;
import com.modularmc.ten.common.block.machine.ChannelBlock;
import com.modularmc.ten.common.block.machine.DirectionalMachineBlock;
import com.modularmc.ten.common.block.machine.HorizontalMachineBlock;

import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

import com.tterrag.registrate.util.entry.BlockEntry;

import static com.modularmc.ten.common.registry.Registration.REGISTRATE;

public class TENBlocks {

    public static final java.util.LinkedHashMap<String, String> ZH_NAMES = new java.util.LinkedHashMap<>();
    public static final java.util.LinkedHashMap<String, String> EN_NAMES = new java.util.LinkedHashMap<>();

    static {
        REGISTRATE.creativeModeTab(() -> TENCreativeModeTabs.BLOCK_TAB);
    }

    // === Ores ===
    public static final BlockEntry<Block> TIN_ORE = ore("tin_ore", "Tin Ore", "锡矿石", 3, MapColor.STONE);
    public static final BlockEntry<Block> NICKEL_ORE = ore("nickel_ore", "Nickel Ore", "镍矿石", 4, MapColor.STONE);
    public static final BlockEntry<Block> DEEP_TIN_ORE = ore("deep_tin_ore", "Deep Tin Ore", "深层锡矿石", 4, MapColor.DEEPSLATE);
    public static final BlockEntry<Block> DEEP_NICKEL_ORE = ore("deep_nickel_ore", "Deep Nickel Ore", "深层镍矿石", 5, MapColor.DEEPSLATE);

    // === Storage Blocks ===
    public static final BlockEntry<Block> TIN_BLOCK = storage("tin_block", "Block of Tin", "锡块", 4);
    public static final BlockEntry<Block> NICKEL_BLOCK = storage("nickel_block", "Block of Nickel", "镍块", 5);
    public static final BlockEntry<Block> POWERED_TIN_BLOCK = storage("powered_tin_block", "Block of Powered Tin", "充能锡块", 5.5f);
    public static final BlockEntry<Block> CHLORIUM_BLOCK = storage("chlorium_block", "Block of Chlorium", "叶绿块", 5);
    public static final BlockEntry<Block> RAW_TIN_BLOCK = rawStorage("raw_tin_block", "Raw Tin Block", "粗锡块", 3);
    public static final BlockEntry<Block> RAW_NICKEL_BLOCK = rawStorage("raw_nickel_block", "Raw Nickel Block", "粗镍块", 4);

    // === Machines ===
    static {
        REGISTRATE.creativeModeTab(() -> TENCreativeModeTabs.MACHINE_TAB);
    }
    public static final BlockEntry<HorizontalMachineBlock> MACHINE_SMELTER = machine("machine_smelter", "Smelter", "熔炼机");
    public static final BlockEntry<HorizontalMachineBlock> MACHINE_PULVERIZER = machine("machine_pulverizer", "Pulverizer", "粉碎机");
    public static final BlockEntry<HorizontalMachineBlock> MACHINE_COMPRESSOR = machine("machine_compressor", "Compressor", "压缩机");
    public static final BlockEntry<HorizontalMachineBlock> MACHINE_REFINER = machine("machine_refiner", "Refiner", "精炼机");
    public static final BlockEntry<HorizontalMachineBlock> MACHINE_INDUCTION_FURNACE = machine("machine_induction_furnace", "Induction Furnace", "感应炉");
    public static final BlockEntry<HorizontalMachineBlock> MACHINE_PSIONICANT = machine("machine_psionicant", "Psionicant", "灵能处理器");
    public static final BlockEntry<HorizontalMachineBlock> MACHINE_BEACON = machine("machine_beacon_simulator", "Beacon Simulator", "信标模拟机");
    public static final BlockEntry<HorizontalMachineBlock> MACHINE_MOB_RIPPER = machine("machine_mob_ripper", "Mob Ripper", "生物啃噬者");
    public static final BlockEntry<HorizontalMachineBlock> MACHINE_QUARRY = machine("machine_quarry", "Quarry", "采矿场");
    public static final BlockEntry<HorizontalMachineBlock> MACHINE_ENCHFLU = machine("machine_enchantment_flusher", "Enchantment Flusher", "祛魔机");
    public static final BlockEntry<HorizontalMachineBlock> MACHINE_CONDENSER = machine("machine_matter_condenser", "Matter Condenser", "物质结晶器");
    public static final BlockEntry<HorizontalMachineBlock> MACHINE_FARM = machine("machine_farm_manager", "Farm Manager", "农场管理机");
    public static final BlockEntry<HorizontalMachineBlock> MACHINE_BLOCK_BREAKER = machine("machine_block_breaker", "Block Breaker", "方块破坏器");
    public static final BlockEntry<HorizontalMachineBlock> MACHINE_BLOCK_FORMER = machine("machine_block_former", "Block Former", "方块成型器");
    public static final BlockEntry<HorizontalMachineBlock> MACHINE_COOLER = machine("machine_cooler", "Cooler", "冷却器");

    // === Engines ===
    public static final BlockEntry<HorizontalMachineBlock> ENGINE_EXTRACTION = engine("engine_extraction", "Extraction Engine", "萃取引擎");
    public static final BlockEntry<HorizontalMachineBlock> ENGINE_METAL = engine("engine_metal", "Metal Engine", "金属引擎");
    public static final BlockEntry<HorizontalMachineBlock> ENGINE_BIOMASS = engine("engine_biomass", "Biomass Engine", "生物质引擎");
    public static final BlockEntry<HorizontalMachineBlock> ENGINE_SOLAR = engine("engine_solar", "Solar Engine", "光合引擎");

    // === Cables ===
    public static final BlockEntry<CableBased> CABLE = cable("cable", "Glass Energy Cable", "玻璃能量线缆");
    public static final BlockEntry<CableBased> CABLE_QUARTZ = cable("cable_quartz", "Quartz Energy Cable", "石英能量线缆");
    public static final BlockEntry<CableBased> CABLE_AZURE = cable("cable_azure", "Azure Energy Cable", "蔚蓝能量线缆");
    public static final BlockEntry<CableBased> CABLE_STAR = cable("cable_star", "Starlight Cable", "星辉能量线缆");

    // === Pipes ===
    public static final BlockEntry<CableBased> PIPE = cable("pipe", "Item Pipe", "物品管道");
    public static final BlockEntry<CableBased> PIPE_WHITE = cable("pipe_white", "Conditional Item Pipe", "限定物品管道");
    public static final BlockEntry<CableBased> PIPE_BLACK = cable("pipe_black", "Exceptional Item Pipe", "排除物品管道");

    // === Cell ===
    public static final BlockEntry<HorizontalMachineBlock> CELL = cell("energy_cell", "Energy Cell", "能量单元");
    public static final BlockEntry<HorizontalMachineBlock> CREATIVE_CELL = cell("creative_energy_cell", "Creative Energy Cell", "创造能量单元");

    // === Channels ===
    public static final BlockEntry<ChannelBlock> CHANNEL_ENERGY = REGISTRATE
            .block("channel_energy", ChannelBlock::new)
            .lang("Energy Channel")
            .tag(TENTags.MACHINES)
            .blockstate((ctx, prov) -> TENModels.channelBlockstate(prov, ctx.getEntry(), "channel_energy"))
            .item().model((ctx, prov) -> prov.blockItem(ctx::getEntry)).build().register();
    public static final BlockEntry<ChannelBlock> CHANNEL_ITEM = REGISTRATE
            .block("channel_item", ChannelBlock::new)
            .lang("Item Channel")
            .tag(TENTags.MACHINES)
            .blockstate((ctx, prov) -> TENModels.channelBlockstate(prov, ctx.getEntry(), "channel_item"))
            .item().model((ctx, prov) -> prov.blockItem(ctx::getEntry)).build().register();
    public static final BlockEntry<ChannelBlock> CHANNEL_FLUID = REGISTRATE
            .block("channel_fluid", ChannelBlock::new)
            .lang("Fluid Channel")
            .tag(TENTags.MACHINES)
            .blockstate((ctx, prov) -> TENModels.channelBlockstate(prov, ctx.getEntry(), "channel_fluid"))
            .item().model((ctx, prov) -> prov.blockItem(ctx::getEntry)).build().register();

    static {
        ZH_NAMES.put("channel_energy", "能量频道");
        EN_NAMES.put("channel_energy", "Energy Channel");
        ZH_NAMES.put("channel_item", "物品频道");
        EN_NAMES.put("channel_item", "Item Channel");
        ZH_NAMES.put("channel_fluid", "流体频道");
        EN_NAMES.put("channel_fluid", "Fluid Channel");
    }

    // === Helpers ===

    private static BlockEntry<Block> ore(String n, String englishName, String cn, double h, MapColor c) {
        ZH_NAMES.put(n, cn);
        EN_NAMES.put(n, englishName);
        return REGISTRATE.block(n, Block::new)
                .lang(englishName)
                .initialProperties(() -> c == MapColor.DEEPSLATE ? Blocks.DEEPSLATE : Blocks.STONE)
                .properties(p -> p.mapColor(c).strength((float) h).requiresCorrectToolForDrops()
                        .sound(c == MapColor.DEEPSLATE ? SoundType.DEEPSLATE : SoundType.STONE))
                .tag(BlockTags.NEEDS_IRON_TOOL, BlockTags.MINEABLE_WITH_PICKAXE)
                .blockstate((ctx, prov) -> prov.simpleBlock(ctx.getEntry(), TENModels.cubeAll(prov, n)))
                .simpleItem().register();
    }

    private static BlockEntry<Block> storage(String n, String englishName, String cn, float h) {
        ZH_NAMES.put(n, cn);
        EN_NAMES.put(n, englishName);
        return REGISTRATE.block(n, Block::new)
                .lang(englishName)
                .initialProperties(() -> Blocks.IRON_BLOCK)
                .properties(p -> p.mapColor(MapColor.METAL).strength(h).requiresCorrectToolForDrops().sound(SoundType.METAL))
                .tag(BlockTags.NEEDS_IRON_TOOL, BlockTags.MINEABLE_WITH_PICKAXE)
                .blockstate((ctx, prov) -> prov.simpleBlock(ctx.getEntry(), TENModels.cubeAll(prov, n)))
                .simpleItem().register();
    }

    private static BlockEntry<Block> rawStorage(String n, String englishName, String cn, float h) {
        ZH_NAMES.put(n, cn);
        EN_NAMES.put(n, englishName);
        return REGISTRATE.block(n, Block::new)
                .lang(englishName)
                .initialProperties(() -> Blocks.IRON_BLOCK)
                .properties(p -> p.mapColor(MapColor.STONE).strength(h).requiresCorrectToolForDrops().sound(SoundType.STONE))
                .tag(BlockTags.NEEDS_IRON_TOOL, BlockTags.MINEABLE_WITH_PICKAXE)
                .blockstate((ctx, prov) -> prov.simpleBlock(ctx.getEntry(), TENModels.cubeAll(prov, n)))
                .simpleItem().register();
    }

    private static BlockEntry<HorizontalMachineBlock> machine(String n, String englishName, String cn) {
        ZH_NAMES.put(n, cn);
        EN_NAMES.put(n, englishName);
        return REGISTRATE.block(n, HorizontalMachineBlock::new)
                .lang(englishName)
                .tag(TENTags.MACHINES)
                .blockstate((ctx, prov) -> {
                    var normal = TENModels.machine(prov, n);
                    var active = TENModels.machineActive(prov, n);
                    var builder = prov.getVariantBuilder(ctx.getEntry());
                    for (var dir : Direction.Plane.HORIZONTAL) {
                        int y = switch (dir) {
                            case NORTH -> 0;
                            case EAST -> 90;
                            case SOUTH -> 180;
                            case WEST -> 270;
                            default -> 0;
                        };
                        builder.partialState()
                                .with(HorizontalMachineBlock.FACING, dir)
                                .with(HorizontalMachineBlock.ACTIVE, false)
                                .modelForState().modelFile(normal).rotationY(y).addModel()
                                .partialState()
                                .with(HorizontalMachineBlock.FACING, dir)
                                .with(HorizontalMachineBlock.ACTIVE, true)
                                .modelForState().modelFile(active).rotationY(y).addModel();
                    }
                })
                .item().model((ctx, prov) -> prov.blockItem(ctx::getEntry)).build().register();
    }

    private static BlockEntry<HorizontalMachineBlock> engine(String n, String englishName, String cn) {
        ZH_NAMES.put(n, cn);
        EN_NAMES.put(n, englishName);
        return REGISTRATE.block(n, HorizontalMachineBlock::new)
                .lang(englishName)
                .tag(TENTags.MACHINES)
                .blockstate((ctx, prov) -> {
                    var normal = TENModels.engine(prov, n);
                    var active = TENModels.engineActive(prov, n);
                    var builder = prov.getVariantBuilder(ctx.getEntry());
                    for (var dir : Direction.Plane.HORIZONTAL) {
                        int y = switch (dir) {
                            case NORTH -> 0;
                            case EAST -> 90;
                            case SOUTH -> 180;
                            case WEST -> 270;
                            default -> 0;
                        };
                        builder.partialState()
                                .with(HorizontalMachineBlock.FACING, dir)
                                .with(HorizontalMachineBlock.ACTIVE, false)
                                .modelForState().modelFile(normal).rotationY(y).addModel()
                                .partialState()
                                .with(HorizontalMachineBlock.FACING, dir)
                                .with(HorizontalMachineBlock.ACTIVE, true)
                                .modelForState().modelFile(active).rotationY(y).addModel();
                    }
                })
                .item().model((ctx, prov) -> prov.blockItem(ctx::getEntry)).build().register();
    }

    private static BlockEntry<CableBased> cable(String n, String englishName, String cn) {
        ZH_NAMES.put(n, cn);
        EN_NAMES.put(n, englishName);
        return REGISTRATE.block(n, CableBased::new)
                .lang(englishName)
                .blockstate((ctx, prov) -> TENModels.cableMultipart(prov, ctx.getEntry(), n))
                .tag(TENTags.MACHINES)
                .item().model((ctx, prov) -> prov.blockItem(ctx::getEntry)).build().register();
    }

    private static BlockEntry<HorizontalMachineBlock> cell(String n, String englishName, String cn) {
        ZH_NAMES.put(n, cn);
        EN_NAMES.put(n, englishName);
        return REGISTRATE.block(n, HorizontalMachineBlock::new)
                .lang(englishName)
                .tag(TENTags.MACHINES)
                .properties(p -> p.noOcclusion())
                .blockstate((ctx, prov) -> TENModels.cellBlockstate(prov, ctx.getEntry(), n))
                .item().model((ctx, prov) -> prov.blockItem(ctx::getEntry)).build().register();
    }

    public static void init() {}
}
