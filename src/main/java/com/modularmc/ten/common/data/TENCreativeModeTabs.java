package com.modularmc.ten.common.data;

import com.modularmc.ten.TEN;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.LinkedHashMap;

import static com.modularmc.ten.common.registry.Registration.CREATIVE_TABS;

public class TENCreativeModeTabs {

    public static final LinkedHashMap<String, String> ZH_NAMES = new LinkedHashMap<>();
    public static final LinkedHashMap<String, String> EN_NAMES = new LinkedHashMap<>();

    // ── Tab entries ────────────────────────────────────────────────
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BLOCK_TAB = CREATIVE_TABS.register("block", () -> CreativeModeTab.builder()
            .icon(() -> new ItemStack(TENBlocks.TIN_BLOCK.get()))
            .title(Component.translatable("itemGroup." + TEN.MOD_ID + ".block"))
            .displayItems((params, output) -> {
                // Storage blocks
                output.accept(TENBlocks.TIN_BLOCK.get());
                output.accept(TENBlocks.NICKEL_BLOCK.get());
                output.accept(TENBlocks.POWERED_TIN_BLOCK.get());
                output.accept(TENBlocks.CHLORIUM_BLOCK.get());
                output.accept(TENBlocks.RAW_TIN_BLOCK.get());
                output.accept(TENBlocks.RAW_NICKEL_BLOCK.get());
                // Ores
                output.accept(TENBlocks.TIN_ORE.get());
                output.accept(TENBlocks.NICKEL_ORE.get());
                output.accept(TENBlocks.DEEP_TIN_ORE.get());
                output.accept(TENBlocks.DEEP_NICKEL_ORE.get());
            })
            .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MACHINE_TAB = CREATIVE_TABS.register("machine", () -> CreativeModeTab.builder()
            .icon(() -> new ItemStack(TENBlocks.MACHINE_SMELTER.get()))
            .title(Component.translatable("itemGroup." + TEN.MOD_ID + ".machine"))
            .displayItems((params, output) -> {
                // Machines
                output.accept(TENBlocks.MACHINE_SMELTER.get());
                output.accept(TENBlocks.MACHINE_PULVERIZER.get());
                output.accept(TENBlocks.MACHINE_COMPRESSOR.get());
                output.accept(TENBlocks.MACHINE_REFINER.get());
                output.accept(TENBlocks.MACHINE_INDUCTION_FURNACE.get());
                output.accept(TENBlocks.MACHINE_PSIONICANT.get());
                output.accept(TENBlocks.MACHINE_BEACON.get());
                output.accept(TENBlocks.MACHINE_MOB_RIPPER.get());
                output.accept(TENBlocks.MACHINE_QUARRY.get());
                output.accept(TENBlocks.MACHINE_ENCHFLU.get());
                output.accept(TENBlocks.MACHINE_CONDENSER.get());
                output.accept(TENBlocks.MACHINE_FARM.get());
                // Engines
                output.accept(TENBlocks.ENGINE_EXTRACTION.get());
                output.accept(TENBlocks.ENGINE_METAL.get());
                output.accept(TENBlocks.ENGINE_BIOMASS.get());
                output.accept(TENBlocks.ENGINE_SOLAR.get());
                // Cables
                output.accept(TENBlocks.CABLE.get());
                output.accept(TENBlocks.CABLE_QUARTZ.get());
                output.accept(TENBlocks.CABLE_AZURE.get());
                output.accept(TENBlocks.CABLE_STAR.get());
                // Pipes
                output.accept(TENBlocks.PIPE.get());
                output.accept(TENBlocks.PIPE_WHITE.get());
                output.accept(TENBlocks.PIPE_BLACK.get());
                // Cells
                output.accept(TENBlocks.CELL.get());
                output.accept(TENBlocks.CREATIVE_CELL.get());
                // Channels
                output.accept(TENBlocks.CHANNEL_ENERGY.get());
                output.accept(TENBlocks.CHANNEL_ITEM.get());
                output.accept(TENBlocks.CHANNEL_FLUID.get());
            })
            .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ITEM_TAB = CREATIVE_TABS.register("item", () -> CreativeModeTab.builder()
            .icon(() -> new ItemStack(TENItems.REDSTONE_AI_ADVANCED.get()))
            .title(Component.translatable("itemGroup." + TEN.MOD_ID + ".item"))
            .displayItems((params, output) -> {
                // Raw materials
                output.accept(TENItems.RAW_TIN.get());
                output.accept(TENItems.RAW_NICKEL.get());
                // Crafting components
                output.accept(TENItems.REDSTONE_CONDUCTOR.get());
                output.accept(TENItems.REDSTONE_CONVERTER.get());
                output.accept(TENItems.REDSTONE_STORER.get());
                output.accept(TENItems.INDIGO.get());
                output.accept(TENItems.AZURE_GLASS.get());
                output.accept(TENItems.BIZARRERIE.get());
                output.accept(TENItems.REDSTONE_AI.get());
                output.accept(TENItems.REDSTONE_AI_ADVANCED.get());
                output.accept(TENItems.HYDRAULIC_WIDGET.get());
                output.accept(TENItems.DETECTOR.get());
                output.accept(TENItems.ROYAL_JELLY.get());
                output.accept(TENItems.SPICY_JELLY.get());
                // Fluid buckets
                output.accept(TENFluids.LIQUID_ROYAL_JELLY_BUCKET.get());
                output.accept(TENFluids.LIQUID_SPICY_JELLY_BUCKET.get());
                output.accept(TENFluids.LIQUID_HONEY_BUCKET.get());
                output.accept(TENFluids.LIQUID_XP_BUCKET.get());
                output.accept(TENFluids.LIQUID_BIZARRERIE_BUCKET.get());
                // Material variants (dusts/ingots/nuggets/plates/gears/rods/wires)
                for (var holder : TENItems.getMaterialVariantHolders()) {
                    output.accept(holder.get());
                }
            })
            .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TOOL_TAB = CREATIVE_TABS.register("tool", () -> CreativeModeTab.builder()
            .icon(() -> new ItemStack(TENItems.MOULD_GEAR.get()))
            .title(Component.translatable("itemGroup." + TEN.MOD_ID + ".tool"))
            .displayItems((params, output) -> {
                // Moulds
                output.accept(TENItems.MOULD_GEAR.get());
                output.accept(TENItems.MOULD_PLATE.get());
                output.accept(TENItems.MOULD_ROD.get());
                output.accept(TENItems.MOULD_STRING.get());
                output.accept(TENItems.MOULD_COMPRESSED_SMALL.get());
                output.accept(TENItems.MOULD_COMPRESSED_LARGE.get());
                output.accept(TENItems.MOULD_SPLIT.get());
                output.accept(TENItems.MOULD_COIN.get());
                output.accept(TENItems.MOULD_DENSE_PLATE.get());
                // Tools
                output.accept(TENItems.SPANNER.get());
                output.accept(TENItems.ENERGY_CAPACITY.get());
                output.accept(TENItems.CHANNEL_CONNECTOR.get());
                // Upgrades
                output.accept(TENItems.AUGMENTED_LEVELUP.get());
                output.accept(TENItems.POWERED_LEVELUP.get());
                output.accept(TENItems.RELIC_LEVELUP.get());
                output.accept(TENItems.PHOTOSYN_LEVELUP.get());
                output.accept(TENItems.RANGE_LEVELUP.get());
                output.accept(TENItems.SMOKE_LEVELUP.get());
                output.accept(TENItems.BLAST_LEVELUP.get());
                output.accept(TENItems.POTION_LEVELUP.get());
                output.accept(TENItems.STREAM_LEVELUP.get());
                output.accept(TENItems.KNOWLEDGE_LEVELUP.get());
                output.accept(TENItems.ICE_LEVELUP.get());
                output.accept(TENItems.MAGMA_LEVELUP.get());
                output.accept(TENItems.MINERAL_LEVELUP.get());
            })
            .build());

    // ── Title translations ────────────────────────────────────────
    static {
        ZH_NAMES.put("itemGroup." + TEN.MOD_ID + ".block", "科能工程:再技术化 | 方块");
        ZH_NAMES.put("itemGroup." + TEN.MOD_ID + ".machine", "科能工程:再技术化 | 机器");
        ZH_NAMES.put("itemGroup." + TEN.MOD_ID + ".item", "科能工程:再技术化 | 物品");
        ZH_NAMES.put("itemGroup." + TEN.MOD_ID + ".tool", "科能工程:再技术化 | 工具");
        EN_NAMES.put("itemGroup." + TEN.MOD_ID + ".block", "Kenergy Engineering: Retechnicalized - Blocks");
        EN_NAMES.put("itemGroup." + TEN.MOD_ID + ".machine", "Kenergy Engineering: Retechnicalized - Machines");
        EN_NAMES.put("itemGroup." + TEN.MOD_ID + ".item", "Kenergy Engineering: Retechnicalized - Items");
        EN_NAMES.put("itemGroup." + TEN.MOD_ID + ".tool", "Kenergy Engineering: Retechnicalized - Tools");
    }

    public static void init() {}
}
