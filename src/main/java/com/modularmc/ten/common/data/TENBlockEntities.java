package com.modularmc.ten.common.data;

import com.modularmc.ten.common.blockentity.CableBlockEntity;
import com.modularmc.ten.common.blockentity.PipeBlockEntity;
import com.modularmc.ten.common.blockentity.channel.ChannelEnergyBlockEntity;
import com.modularmc.ten.common.blockentity.channel.ChannelFluidBlockEntity;
import com.modularmc.ten.common.blockentity.channel.ChannelItemBlockEntity;
import com.modularmc.ten.common.blockentity.machine.*;

import com.tterrag.registrate.util.entry.BlockEntityEntry;

import static com.modularmc.ten.common.registry.Registration.REGISTRATE;

public class TENBlockEntities {

    public static final BlockEntityEntry<CableBlockEntity> CABLE = REGISTRATE
            .blockEntity("cable", CableBlockEntity::new)
            .validBlocks(TENBlocks.CABLE, TENBlocks.CABLE_QUARTZ, TENBlocks.CABLE_AZURE, TENBlocks.CABLE_STAR)
            .register();

    public static final BlockEntityEntry<PipeBlockEntity> PIPE = REGISTRATE
            .blockEntity("pipe", PipeBlockEntity::new)
            .validBlocks(TENBlocks.PIPE, TENBlocks.PIPE_WHITE, TENBlocks.PIPE_BLACK)
            .register();

    public static final BlockEntityEntry<ChannelEnergyBlockEntity> CHANNEL_ENERGY = REGISTRATE
            .blockEntity("channel_energy", ChannelEnergyBlockEntity::new)
            .validBlocks(TENBlocks.CHANNEL_ENERGY)
            .register();

    public static final BlockEntityEntry<ChannelItemBlockEntity> CHANNEL_ITEM = REGISTRATE
            .blockEntity("channel_item", ChannelItemBlockEntity::new)
            .validBlocks(TENBlocks.CHANNEL_ITEM)
            .register();

    public static final BlockEntityEntry<ChannelFluidBlockEntity> CHANNEL_FLUID = REGISTRATE
            .blockEntity("channel_fluid", ChannelFluidBlockEntity::new)
            .validBlocks(TENBlocks.CHANNEL_FLUID)
            .register();

    // Processing machines
    public static final BlockEntityEntry<FurnaceBlockEntity> FURNACE = REGISTRATE
            .blockEntity("machine_smelter", FurnaceBlockEntity::new)
            .validBlocks(TENBlocks.MACHINE_SMELTER)
            .register();
    public static final BlockEntityEntry<PulverizerBlockEntity> PULVERIZER = REGISTRATE
            .blockEntity("machine_pulverizer", PulverizerBlockEntity::new)
            .validBlocks(TENBlocks.MACHINE_PULVERIZER)
            .register();
    public static final BlockEntityEntry<CompressorBlockEntity> COMPRESSOR = REGISTRATE
            .blockEntity("machine_compressor", CompressorBlockEntity::new)
            .validBlocks(TENBlocks.MACHINE_COMPRESSOR)
            .register();
    public static final BlockEntityEntry<RefinerBlockEntity> REFINER = REGISTRATE
            .blockEntity("machine_refiner", RefinerBlockEntity::new)
            .validBlocks(TENBlocks.MACHINE_REFINER)
            .register();
    public static final BlockEntityEntry<IndfurBlockEntity> INDUCTION_FURNACE = REGISTRATE
            .blockEntity("machine_induction_furnace", IndfurBlockEntity::new)
            .validBlocks(TENBlocks.MACHINE_INDUCTION_FURNACE)
            .register();
    public static final BlockEntityEntry<PsionicantBlockEntity> PSIONICANT = REGISTRATE
            .blockEntity("machine_psionicant", PsionicantBlockEntity::new)
            .validBlocks(TENBlocks.MACHINE_PSIONICANT)
            .register();
    public static final BlockEntityEntry<CondenserBlockEntity> CONDENSER = REGISTRATE
            .blockEntity("machine_matter_condenser", CondenserBlockEntity::new)
            .validBlocks(TENBlocks.MACHINE_CONDENSER)
            .register();
    public static final BlockEntityEntry<EncfluBlockEntity> ENCHFLU = REGISTRATE
            .blockEntity("machine_enchantment_flusher", EncfluBlockEntity::new)
            .validBlocks(TENBlocks.MACHINE_ENCHFLU)
            .register();

    // Effect machines
    public static final BlockEntityEntry<BeaconBlockEntity> BEACON = REGISTRATE
            .blockEntity("machine_beacon_simulator", BeaconBlockEntity::new)
            .validBlocks(TENBlocks.MACHINE_BEACON)
            .register();
    public static final BlockEntityEntry<MobRipBlockEntity> MOB_RIP = REGISTRATE
            .blockEntity("machine_mob_ripper", MobRipBlockEntity::new)
            .validBlocks(TENBlocks.MACHINE_MOB_RIPPER)
            .register();
    public static final BlockEntityEntry<FarmBlockEntity> FARM = REGISTRATE
            .blockEntity("machine_farm_manager", FarmBlockEntity::new)
            .validBlocks(TENBlocks.MACHINE_FARM)
            .register();
    public static final BlockEntityEntry<QuarryBlockEntity> QUARRY = REGISTRATE
            .blockEntity("machine_quarry", QuarryBlockEntity::new)
            .validBlocks(TENBlocks.MACHINE_QUARRY)
            .register();

    // 三台环境作业机器
    public static final BlockEntityEntry<BlockBreakerBlockEntity> BLOCK_BREAKER = REGISTRATE
            .blockEntity("machine_block_breaker", BlockBreakerBlockEntity::new)
            .validBlocks(TENBlocks.MACHINE_BLOCK_BREAKER)
            .register();
    public static final BlockEntityEntry<BlockFormerBlockEntity> BLOCK_FORMER = REGISTRATE
            .blockEntity("machine_block_former", BlockFormerBlockEntity::new)
            .validBlocks(TENBlocks.MACHINE_BLOCK_FORMER)
            .register();
    public static final BlockEntityEntry<CoolerBlockEntity> COOLER = REGISTRATE
            .blockEntity("machine_cooler", CoolerBlockEntity::new)
            .validBlocks(TENBlocks.MACHINE_COOLER)
            .register();

    // Engines
    public static final BlockEntityEntry<SolarBlockEntity> SOLAR = REGISTRATE
            .blockEntity("engine_solar", SolarBlockEntity::new)
            .validBlocks(TENBlocks.ENGINE_SOLAR)
            .register();
    public static final BlockEntityEntry<ExtractorBlockEntity> EXTRACTOR = REGISTRATE
            .blockEntity("engine_extraction", ExtractorBlockEntity::new)
            .validBlocks(TENBlocks.ENGINE_EXTRACTION)
            .register();
    public static final BlockEntityEntry<BiomassBlockEntity> BIOMASS = REGISTRATE
            .blockEntity("engine_biomass", BiomassBlockEntity::new)
            .validBlocks(TENBlocks.ENGINE_BIOMASS)
            .register();
    public static final BlockEntityEntry<MetalizerBlockEntity> METALIZER = REGISTRATE
            .blockEntity("engine_metal", MetalizerBlockEntity::new)
            .validBlocks(TENBlocks.ENGINE_METAL)
            .register();

    public static final BlockEntityEntry<CellBlockEntity> CELL = REGISTRATE
            .blockEntity("energy_cell", CellBlockEntity::new)
            .validBlocks(TENBlocks.CELL)
            .register();

    public static final BlockEntityEntry<CreativeCellBlockEntity> CREATIVE_CELL = REGISTRATE
            .blockEntity("creative_energy_cell", CreativeCellBlockEntity::new)
            .validBlocks(TENBlocks.CREATIVE_CELL)
            .register();

    public static void init() {}
}
