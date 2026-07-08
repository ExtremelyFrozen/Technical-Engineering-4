package com.modularmc.ten.common.data;

import com.modularmc.ten.common.blockentity.CableBlockEntity;
import com.modularmc.ten.common.blockentity.PipeBlockEntity;
import com.modularmc.ten.common.blockentity.channel.ChannelEnergyBlockEntity;
import com.modularmc.ten.common.blockentity.channel.ChannelFluidBlockEntity;
import com.modularmc.ten.common.blockentity.channel.ChannelItemBlockEntity;
import com.modularmc.ten.common.blockentity.machine.*;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.function.Function;

import static com.modularmc.ten.common.registry.Registration.BLOCK_ENTITIES;

public class TENBlockEntities {

    /**
     * Helper to create a BlockEntityType with a factory that first captures
     * the type reference (for BEs whose constructor takes BlockEntityType).
     */
    private static <T extends BlockEntity> BlockEntityType<T> beType(
            Function<BlockEntityType<T>, BlockEntityType.BlockEntitySupplier<T>> factoryBuilder,
            Block... blocks) {
        BlockEntityType<T>[] ref = new BlockEntityType[1];
        BlockEntityType<T> type = new BlockEntityType<>((pos, state) -> factoryBuilder.apply(ref[0]).create(pos, state), blocks);
        ref[0] = type;
        return type;
    }

    // ── Cables ────────────────────────────────────────────────
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CableBlockEntity>> CABLE
            = BLOCK_ENTITIES.register("cable",
            () -> beType(type -> (pos, state) -> new CableBlockEntity(type, pos, state),
                    TENBlocks.CABLE.get(), TENBlocks.CABLE_QUARTZ.get(),
                    TENBlocks.CABLE_AZURE.get(), TENBlocks.CABLE_STAR.get()));

    // ── Pipes ─────────────────────────────────────────────────
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PipeBlockEntity>> PIPE
            = BLOCK_ENTITIES.register("pipe",
            () -> beType(type -> (pos, state) -> new PipeBlockEntity(type, pos, state),
                    TENBlocks.PIPE.get(), TENBlocks.PIPE_WHITE.get(), TENBlocks.PIPE_BLACK.get()));

    // ── Channels ──────────────────────────────────────────────
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChannelEnergyBlockEntity>> CHANNEL_ENERGY
            = BLOCK_ENTITIES.register("channel_energy",
            () -> beType(type -> (pos, state) -> new ChannelEnergyBlockEntity(type, pos, state),
                    TENBlocks.CHANNEL_ENERGY.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChannelItemBlockEntity>> CHANNEL_ITEM
            = BLOCK_ENTITIES.register("channel_item",
            () -> beType(type -> (pos, state) -> new ChannelItemBlockEntity(type, pos, state),
                    TENBlocks.CHANNEL_ITEM.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChannelFluidBlockEntity>> CHANNEL_FLUID
            = BLOCK_ENTITIES.register("channel_fluid",
            () -> beType(type -> (pos, state) -> new ChannelFluidBlockEntity(type, pos, state),
                    TENBlocks.CHANNEL_FLUID.get()));

    // ── Processing machines ───────────────────────────────────
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FurnaceBlockEntity>> FURNACE
            = BLOCK_ENTITIES.register("machine_smelter",
            () -> beType(type -> (pos, state) -> new FurnaceBlockEntity(type, pos, state),
                    TENBlocks.MACHINE_SMELTER.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PulverizerBlockEntity>> PULVERIZER
            = BLOCK_ENTITIES.register("machine_pulverizer",
            () -> beType(type -> (pos, state) -> new PulverizerBlockEntity(type, pos, state),
                    TENBlocks.MACHINE_PULVERIZER.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CompressorBlockEntity>> COMPRESSOR
            = BLOCK_ENTITIES.register("machine_compressor",
            () -> beType(type -> (pos, state) -> new CompressorBlockEntity(type, pos, state),
                    TENBlocks.MACHINE_COMPRESSOR.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RefinerBlockEntity>> REFINER
            = BLOCK_ENTITIES.register("machine_refiner",
            () -> beType(type -> (pos, state) -> new RefinerBlockEntity(type, pos, state),
                    TENBlocks.MACHINE_REFINER.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IndfurBlockEntity>> INDUCTION_FURNACE
            = BLOCK_ENTITIES.register("machine_induction_furnace",
            () -> beType(type -> (pos, state) -> new IndfurBlockEntity(type, pos, state),
                    TENBlocks.MACHINE_INDUCTION_FURNACE.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PsionicantBlockEntity>> PSIONICANT
            = BLOCK_ENTITIES.register("machine_psionicant",
            () -> beType(type -> (pos, state) -> new PsionicantBlockEntity(type, pos, state),
                    TENBlocks.MACHINE_PSIONICANT.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CondenserBlockEntity>> CONDENSER
            = BLOCK_ENTITIES.register("machine_matter_condenser",
            () -> beType(type -> (pos, state) -> new CondenserBlockEntity(type, pos, state),
                    TENBlocks.MACHINE_CONDENSER.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EncfluBlockEntity>> ENCHFLU
            = BLOCK_ENTITIES.register("machine_enchantment_flusher",
            () -> beType(type -> (pos, state) -> new EncfluBlockEntity(type, pos, state),
                    TENBlocks.MACHINE_ENCHFLU.get()));

    // ── Effect machines ────────────────────────────────────────
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BeaconBlockEntity>> BEACON
            = BLOCK_ENTITIES.register("machine_beacon_simulator",
            () -> beType(type -> (pos, state) -> new BeaconBlockEntity(type, pos, state),
                    TENBlocks.MACHINE_BEACON.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MobRipBlockEntity>> MOB_RIP
            = BLOCK_ENTITIES.register("machine_mob_ripper",
            () -> beType(type -> (pos, state) -> new MobRipBlockEntity(type, pos, state),
                    TENBlocks.MACHINE_MOB_RIPPER.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FarmBlockEntity>> FARM
            = BLOCK_ENTITIES.register("machine_farm_manager",
            () -> beType(type -> (pos, state) -> new FarmBlockEntity(type, pos, state),
                    TENBlocks.MACHINE_FARM.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<QuarryBlockEntity>> QUARRY
            = BLOCK_ENTITIES.register("machine_quarry",
            () -> beType(type -> (pos, state) -> new QuarryBlockEntity(type, pos, state),
                    TENBlocks.MACHINE_QUARRY.get()));

    // ── Engines ────────────────────────────────────────────────
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SolarBlockEntity>> SOLAR
            = BLOCK_ENTITIES.register("engine_solar",
            () -> beType(type -> (pos, state) -> new SolarBlockEntity(type, pos, state),
                    TENBlocks.ENGINE_SOLAR.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ExtractorBlockEntity>> EXTRACTOR
            = BLOCK_ENTITIES.register("engine_extraction",
            () -> beType(type -> (pos, state) -> new ExtractorBlockEntity(type, pos, state),
                    TENBlocks.ENGINE_EXTRACTION.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BiomassBlockEntity>> BIOMASS
            = BLOCK_ENTITIES.register("engine_biomass",
            () -> beType(type -> (pos, state) -> new BiomassBlockEntity(type, pos, state),
                    TENBlocks.ENGINE_BIOMASS.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MetalizerBlockEntity>> METALIZER
            = BLOCK_ENTITIES.register("engine_metal",
            () -> beType(type -> (pos, state) -> new MetalizerBlockEntity(type, pos, state),
                    TENBlocks.ENGINE_METAL.get()));

    // ── Cells ──────────────────────────────────────────────────
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CellBlockEntity>> CELL
            = BLOCK_ENTITIES.register("energy_cell",
            () -> beType(type -> (pos, state) -> new CellBlockEntity(type, pos, state),
                    TENBlocks.CELL.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CreativeCellBlockEntity>> CREATIVE_CELL
            = BLOCK_ENTITIES.register("creative_energy_cell",
            () -> beType(type -> (pos, state) -> new CreativeCellBlockEntity(type, pos, state),
                    TENBlocks.CREATIVE_CELL.get()));

    public static void init() {}
}
