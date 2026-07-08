package com.modularmc.ten.common.registry;

import com.modularmc.ten.TEN;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Central registry hub for Technical Engineering 4 (26.1.2 migration).
 * <p>
 * Provides NeoForge native {@link DeferredRegister} instances replacing
 * the previous Registrate-based {@code TENRegistrate}.
 * All mod registrations go through these registers.
 */
public class Registration {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(TEN.MOD_ID);
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(TEN.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, TEN.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TEN.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TEN.MOD_ID);

    /**
     * Register all DeferredRegisters on the mod event bus.
     * Must be called exactly once during mod construction.
     */
    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        FLUIDS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        CREATIVE_TABS.register(modBus);
    }

    private Registration() {}
}
