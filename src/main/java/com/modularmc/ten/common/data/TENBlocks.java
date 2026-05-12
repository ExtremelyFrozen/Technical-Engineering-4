package com.modularmc.ten.common.data;

import com.modularmc.ten.common.block.machine.CableBased;
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

    static {
        REGISTRATE.creativeModeTab(() -> TENCreativeModeTabs.BLOCK_TAB);
    }

    // === Ores ===
    public static final BlockEntry<Block> TIN_ORE = ore("tin_ore", "Tin Ore", 3, MapColor.STONE);
    public static final BlockEntry<Block> NICKEL_ORE = ore("nickel_ore", "Nickel Ore", 4, MapColor.STONE);
    public static final BlockEntry<Block> DEEP_TIN_ORE = ore("deep_tin_ore", "Deep Tin Ore", 4, MapColor.DEEPSLATE);
    public static final BlockEntry<Block> DEEP_NICKEL_ORE = ore("deep_nickel_ore", "Deep Nickel Ore", 5, MapColor.DEEPSLATE);

    // === Storage Blocks ===
    public static final BlockEntry<Block> TIN_BLOCK = storage("tin_block", "Block of Tin", 4);
    public static final BlockEntry<Block> NICKEL_BLOCK = storage("nickel_block", "Block of Nickel", 5);
    public static final BlockEntry<Block> POWERED_TIN_BLOCK = storage("powered_tin_block", "Block of Powered Tin", 5.5f);
    public static final BlockEntry<Block> CHLORIUM_BLOCK = storage("chlorium_block", "Block of Chlorium", 5);
    public static final BlockEntry<Block> RAW_TIN_BLOCK = rawStorage("raw_tin_block", "Raw Tin Block", 3);
    public static final BlockEntry<Block> RAW_NICKEL_BLOCK = rawStorage("raw_nickel_block", "Raw Nickel Block", 4);

    // === Machines ===
    static {
        REGISTRATE.creativeModeTab(() -> TENCreativeModeTabs.MACHINE_TAB);
    }
    public static final BlockEntry<HorizontalMachineBlock> MACHINE_SMELTER = machine("machine_smelter", "Smelter");
    public static final BlockEntry<HorizontalMachineBlock> MACHINE_PULVERIZER = machine("machine_pulverizer", "Pulverizer");
    public static final BlockEntry<HorizontalMachineBlock> MACHINE_COMPRESSOR = machine("machine_compressor", "Compressor");
    public static final BlockEntry<HorizontalMachineBlock> MACHINE_REFINER = machine("machine_refiner", "Refiner");
    public static final BlockEntry<HorizontalMachineBlock> MACHINE_INDUCTION_FURNACE = machine("machine_induction_furnace", "Induction Furnace");
    public static final BlockEntry<HorizontalMachineBlock> MACHINE_PSIONICANT = machine("machine_psionicant", "Psionicant");
    public static final BlockEntry<HorizontalMachineBlock> MACHINE_BEACON = machine("machine_beacon_simulator", "Beacon Simulator");
    public static final BlockEntry<HorizontalMachineBlock> MACHINE_MOB_RIPPER = machine("machine_mob_ripper", "Mob Ripper");
    public static final BlockEntry<HorizontalMachineBlock> MACHINE_QUARRY = machine("machine_quarry", "Quarry");
    public static final BlockEntry<HorizontalMachineBlock> MACHINE_ENCHFLU = machine("machine_enchantment_flusher", "Enchantment Flusher");
    public static final BlockEntry<HorizontalMachineBlock> MACHINE_CONDENSER = machine("machine_matter_condenser", "Matter Condenser");
    public static final BlockEntry<HorizontalMachineBlock> MACHINE_FARM = machine("machine_farm_manager", "Farm Manager");

    // === Engines ===
    public static final BlockEntry<HorizontalMachineBlock> ENGINE_EXTRACTION = engine("engine_extraction", "Extraction Engine");
    public static final BlockEntry<HorizontalMachineBlock> ENGINE_METAL = engine("engine_metal", "Metal Engine");
    public static final BlockEntry<HorizontalMachineBlock> ENGINE_BIOMASS = engine("engine_biomass", "Biomass Engine");
    public static final BlockEntry<HorizontalMachineBlock> ENGINE_SOLAR = engine("engine_solar", "Solar Engine");

    // === Cables ===
    public static final BlockEntry<CableBased> CABLE = cable("cable", "Glass Energy Cable");
    public static final BlockEntry<CableBased> CABLE_QUARTZ = cable("cable_quartz", "Quartz Energy Cable");
    public static final BlockEntry<CableBased> CABLE_AZURE = cable("cable_azure", "Azure Energy Cable");
    public static final BlockEntry<CableBased> CABLE_STAR = cable("cable_star", "Starlight Cable");

    // === Pipes ===
    public static final BlockEntry<CableBased> PIPE = cable("pipe", "Item Pipe");
    public static final BlockEntry<CableBased> PIPE_WHITE = cable("pipe_white", "Conditional Item Pipe");
    public static final BlockEntry<CableBased> PIPE_BLACK = cable("pipe_black", "Exceptional Item Pipe");

    // === Cell ===
    public static final BlockEntry<HorizontalMachineBlock> CELL = REGISTRATE
            .block("energy_cell", HorizontalMachineBlock::new)
            .lang("Energy Cell")
            .blockstate((ctx, prov) -> TENModels.energyCellBlockstate(prov, ctx.getEntry()))
            .item().model((ctx, prov) -> prov.blockItem(ctx::getEntry)).build().register();

    // === Channels ===
    public static final BlockEntry<DirectionalMachineBlock> CHANNEL_ENERGY = REGISTRATE
            .block("channel_energy", DirectionalMachineBlock::new)
            .lang("Energy Channel")
            .blockstate((ctx, prov) -> TENModels.channelBlockstate(prov, ctx.getEntry(), "channel_energy"))
            .item().model((ctx, prov) -> prov.blockItem(ctx::getEntry)).build().register();
    public static final BlockEntry<DirectionalMachineBlock> CHANNEL_ITEM = REGISTRATE
            .block("channel_item", DirectionalMachineBlock::new)
            .lang("Item Channel")
            .blockstate((ctx, prov) -> TENModels.channelBlockstate(prov, ctx.getEntry(), "channel_item"))
            .item().model((ctx, prov) -> prov.blockItem(ctx::getEntry)).build().register();
    public static final BlockEntry<DirectionalMachineBlock> CHANNEL_FLUID = REGISTRATE
            .block("channel_fluid", DirectionalMachineBlock::new)
            .lang("Fluid Channel")
            .blockstate((ctx, prov) -> TENModels.channelBlockstate(prov, ctx.getEntry(), "channel_fluid"))
            .item().model((ctx, prov) -> prov.blockItem(ctx::getEntry)).build().register();

    // === Helpers ===

    private static BlockEntry<Block> ore(String n, String englishName, double h, MapColor c) {
        return REGISTRATE.block(n, Block::new)
                .lang(englishName)
                .initialProperties(() -> c == MapColor.DEEPSLATE ? Blocks.DEEPSLATE : Blocks.STONE)
                .properties(p -> p.mapColor(c).strength((float) h).requiresCorrectToolForDrops()
                        .sound(c == MapColor.DEEPSLATE ? SoundType.DEEPSLATE : SoundType.STONE))
                .tag(BlockTags.NEEDS_IRON_TOOL, BlockTags.MINEABLE_WITH_PICKAXE)
                .blockstate((ctx, prov) -> prov.simpleBlock(ctx.getEntry(), TENModels.cubeAll(prov, n)))
                .simpleItem().register();
    }

    private static BlockEntry<Block> storage(String n, String englishName, float h) {
        return REGISTRATE.block(n, Block::new)
                .lang(englishName)
                .initialProperties(() -> Blocks.IRON_BLOCK)
                .properties(p -> p.mapColor(MapColor.METAL).strength(h).requiresCorrectToolForDrops().sound(SoundType.METAL))
                .tag(BlockTags.NEEDS_IRON_TOOL, BlockTags.MINEABLE_WITH_PICKAXE)
                .blockstate((ctx, prov) -> prov.simpleBlock(ctx.getEntry(), TENModels.cubeAll(prov, n)))
                .simpleItem().register();
    }

    private static BlockEntry<Block> rawStorage(String n, String englishName, float h) {
        return REGISTRATE.block(n, Block::new)
                .lang(englishName)
                .initialProperties(() -> Blocks.IRON_BLOCK)
                .properties(p -> p.mapColor(MapColor.STONE).strength(h).requiresCorrectToolForDrops().sound(SoundType.STONE))
                .tag(BlockTags.NEEDS_IRON_TOOL, BlockTags.MINEABLE_WITH_PICKAXE)
                .blockstate((ctx, prov) -> prov.simpleBlock(ctx.getEntry(), TENModels.cubeAll(prov, n)))
                .simpleItem().register();
    }

    private static BlockEntry<HorizontalMachineBlock> machine(String n, String englishName) {
        return REGISTRATE.block(n, HorizontalMachineBlock::new)
                .lang(englishName)
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

    private static BlockEntry<HorizontalMachineBlock> engine(String n, String englishName) {
        return REGISTRATE.block(n, HorizontalMachineBlock::new)
                .lang(englishName)
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

    private static BlockEntry<CableBased> cable(String n, String englishName) {
        return REGISTRATE.block(n, CableBased::new)
                .lang(englishName)
                .blockstate((ctx, prov) -> TENModels.cableMultipart(prov, ctx.getEntry(), n))
                .item().model((ctx, prov) -> prov.blockItem(ctx::getEntry)).build().register();
    }

    public static void init() {}
}
