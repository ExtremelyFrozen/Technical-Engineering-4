package com.modularmc.ten.common.data;

import com.modularmc.ten.common.blockentity.CableBlockEntity;
import com.modularmc.ten.common.blockentity.PipeBlockEntity;
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

    public static void init() {}
}
