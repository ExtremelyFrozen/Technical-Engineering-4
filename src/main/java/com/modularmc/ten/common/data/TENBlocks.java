package com.modularmc.ten.common.data;

import com.modularmc.ten.core.block.machine.CableBased;
import com.modularmc.ten.core.block.machine.HorizontalMachineBlock;

import com.tterrag.registrate.util.entry.BlockEntry;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

import static com.modularmc.ten.common.registry.Registration.REGISTRATE;

public class TENBlocks {

    static {
        REGISTRATE.creativeModeTab(() -> TENCreativeModeTabs.BLOCK_TAB);
    }

    // Ores
    public static final BlockEntry<Block> TIN_ORE = REGISTRATE
            .block("tin_ore", Block::new)
            .initialProperties(() -> Blocks.STONE)
            .properties(p -> p.mapColor(MapColor.STONE).strength(3, 3).requiresCorrectToolForDrops().sound(SoundType.STONE))
            .tag(BlockTags.NEEDS_IRON_TOOL, BlockTags.MINEABLE_WITH_PICKAXE)
            .simpleItem()
            .register();
    public static final BlockEntry<Block> NICKEL_ORE = REGISTRATE
            .block("nickel_ore", Block::new)
            .initialProperties(() -> Blocks.STONE)
            .properties(p -> p.mapColor(MapColor.STONE).strength(4, 4).requiresCorrectToolForDrops().sound(SoundType.STONE))
            .tag(BlockTags.NEEDS_IRON_TOOL, BlockTags.MINEABLE_WITH_PICKAXE)
            .simpleItem()
            .register();
    public static final BlockEntry<Block> DEEP_TIN_ORE = REGISTRATE
            .block("deep_tin_ore", Block::new)
            .initialProperties(() -> Blocks.DEEPSLATE)
            .properties(p -> p.mapColor(MapColor.DEEPSLATE).strength(4, 4).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE))
            .tag(BlockTags.NEEDS_IRON_TOOL, BlockTags.MINEABLE_WITH_PICKAXE)
            .simpleItem()
            .register();
    public static final BlockEntry<Block> DEEP_NICKEL_ORE = REGISTRATE
            .block("deep_nickel_ore", Block::new)
            .initialProperties(() -> Blocks.DEEPSLATE)
            .properties(p -> p.mapColor(MapColor.DEEPSLATE).strength(5, 5).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE))
            .tag(BlockTags.NEEDS_IRON_TOOL, BlockTags.MINEABLE_WITH_PICKAXE)
            .simpleItem()
            .register();

    // Storage Blocks
    public static final BlockEntry<Block> TIN_BLOCK = REGISTRATE
            .block("tin_block", Block::new)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(p -> p.mapColor(MapColor.METAL).strength(4, 4).requiresCorrectToolForDrops().sound(SoundType.METAL))
            .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_IRON_TOOL)
            .simpleItem()
            .register();
    public static final BlockEntry<Block> NICKEL_BLOCK = REGISTRATE
            .block("nickel_block", Block::new)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(p -> p.mapColor(MapColor.METAL).strength(5, 5).requiresCorrectToolForDrops().sound(SoundType.METAL))
            .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_IRON_TOOL)
            .simpleItem()
            .register();
    public static final BlockEntry<Block> POWERED_TIN_BLOCK = REGISTRATE
            .block("powered_tin_block", Block::new)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(p -> p.mapColor(MapColor.COLOR_LIGHT_BLUE).strength(5.5f, 5.5f).requiresCorrectToolForDrops().sound(SoundType.METAL))
            .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_IRON_TOOL)
            .simpleItem()
            .register();
    public static final BlockEntry<Block> CHLORIUM_BLOCK = REGISTRATE
            .block("chlorium_block", Block::new)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(p -> p.mapColor(MapColor.COLOR_CYAN).strength(5, 5).requiresCorrectToolForDrops().sound(SoundType.METAL))
            .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_IRON_TOOL)
            .simpleItem()
            .register();
    public static final BlockEntry<Block> RAW_TIN_BLOCK = REGISTRATE
            .block("raw_tin_block", Block::new)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(p -> p.mapColor(MapColor.STONE).strength(3, 3).requiresCorrectToolForDrops().sound(SoundType.STONE))
            .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_IRON_TOOL)
            .simpleItem()
            .register();
    public static final BlockEntry<Block> RAW_NICKEL_BLOCK = REGISTRATE
            .block("raw_nickel_block", Block::new)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(p -> p.mapColor(MapColor.STONE).strength(4, 4).requiresCorrectToolForDrops().sound(SoundType.STONE))
            .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_IRON_TOOL)
            .simpleItem()
            .register();

    // Machines
    static {
        REGISTRATE.creativeModeTab(() -> TENCreativeModeTabs.MACHINE_TAB);
    }
    public static final BlockEntry<HorizontalMachineBlock> MACHINE_SMELTER = REGISTRATE
            .block("machine_smelter", HorizontalMachineBlock::new).simpleItem().register();
    public static final BlockEntry<HorizontalMachineBlock> MACHINE_PULVERIZER = REGISTRATE
            .block("machine_pulverizer", HorizontalMachineBlock::new).simpleItem().register();
    public static final BlockEntry<HorizontalMachineBlock> MACHINE_COMPRESSOR = REGISTRATE
            .block("machine_compressor", HorizontalMachineBlock::new).simpleItem().register();
    public static final BlockEntry<HorizontalMachineBlock> MACHINE_REFINER = REGISTRATE
            .block("machine_refiner", HorizontalMachineBlock::new).simpleItem().register();
    public static final BlockEntry<HorizontalMachineBlock> MACHINE_INDUCTION_FURNACE = REGISTRATE
            .block("machine_induction_furnace", HorizontalMachineBlock::new).simpleItem().register();
    public static final BlockEntry<HorizontalMachineBlock> MACHINE_PSIONICANT = REGISTRATE
            .block("machine_psionicant", HorizontalMachineBlock::new).simpleItem().register();
    public static final BlockEntry<HorizontalMachineBlock> MACHINE_BEACON = REGISTRATE
            .block("machine_beacon_simulator", HorizontalMachineBlock::new).simpleItem().register();
    public static final BlockEntry<HorizontalMachineBlock> MACHINE_MOB_RIPPER = REGISTRATE
            .block("machine_mob_ripper", HorizontalMachineBlock::new).simpleItem().register();
    public static final BlockEntry<HorizontalMachineBlock> MACHINE_QUARRY = REGISTRATE
            .block("machine_quarry", HorizontalMachineBlock::new).simpleItem().register();
    public static final BlockEntry<HorizontalMachineBlock> MACHINE_ENCHFLU = REGISTRATE
            .block("machine_enchantment_flusher", HorizontalMachineBlock::new).simpleItem().register();
    public static final BlockEntry<HorizontalMachineBlock> MACHINE_CONDENSER = REGISTRATE
            .block("machine_matter_condenser", HorizontalMachineBlock::new).simpleItem().register();
    public static final BlockEntry<HorizontalMachineBlock> MACHINE_FARM = REGISTRATE
            .block("machine_farm_manager", HorizontalMachineBlock::new).simpleItem().register();

    // Engines
    public static final BlockEntry<HorizontalMachineBlock> ENGINE_EXTRACTION = REGISTRATE
            .block("engine_extraction", HorizontalMachineBlock::new).simpleItem().register();
    public static final BlockEntry<HorizontalMachineBlock> ENGINE_METAL = REGISTRATE
            .block("engine_metal", HorizontalMachineBlock::new).simpleItem().register();
    public static final BlockEntry<HorizontalMachineBlock> ENGINE_BIOMASS = REGISTRATE
            .block("engine_biomass", HorizontalMachineBlock::new).simpleItem().register();
    public static final BlockEntry<HorizontalMachineBlock> ENGINE_SOLAR = REGISTRATE
            .block("engine_solar", HorizontalMachineBlock::new).simpleItem().register();

    // Cables
    public static final BlockEntry<CableBased> CABLE = REGISTRATE
            .block("cable", CableBased::new).simpleItem().register();
    public static final BlockEntry<CableBased> CABLE_QUARTZ = REGISTRATE
            .block("cable_quartz", CableBased::new).simpleItem().register();
    public static final BlockEntry<CableBased> CABLE_AZURE = REGISTRATE
            .block("cable_azure", CableBased::new).simpleItem().register();
    public static final BlockEntry<CableBased> CABLE_STAR = REGISTRATE
            .block("cable_star", CableBased::new).simpleItem().register();

    // Pipes
    public static final BlockEntry<CableBased> PIPE = REGISTRATE
            .block("pipe", CableBased::new).simpleItem().register();
    public static final BlockEntry<CableBased> PIPE_WHITE = REGISTRATE
            .block("pipe_white", CableBased::new).simpleItem().register();
    public static final BlockEntry<CableBased> PIPE_BLACK = REGISTRATE
            .block("pipe_black", CableBased::new).simpleItem().register();

    // Cell (texture: cell_frame.png)
    public static final BlockEntry<HorizontalMachineBlock> CELL = REGISTRATE
            .block("cell", HorizontalMachineBlock::new)
            .blockstate((ctx, prov) -> prov.simpleBlock(ctx.getEntry(), prov.models().cubeAll("cell", prov.modLoc("block/cell_frame"))))
            .item().model((ctx, prov) -> prov.generated(ctx::getEntry, prov.modLoc("block/cell_frame"))).build()
            .register();

    // Channels
    public static final BlockEntry<HorizontalMachineBlock> CHANNEL_ENERGY = REGISTRATE
            .block("channel_energy", HorizontalMachineBlock::new).simpleItem().register();
    public static final BlockEntry<HorizontalMachineBlock> CHANNEL_ITEM = REGISTRATE
            .block("channel_item", HorizontalMachineBlock::new).simpleItem().register();
    public static final BlockEntry<HorizontalMachineBlock> CHANNEL_FLUID = REGISTRATE
            .block("channel_fluid", HorizontalMachineBlock::new).simpleItem().register();

    public static void init() {}
}
