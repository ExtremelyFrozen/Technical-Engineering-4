package com.modularmc.ten.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import com.modularmc.ten.TEN;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Generates all block, item, and fluid tags for the mod, including
 * minecraft-namespace tags (needs_iron_tool, mineable/pickaxe, fluid/water).
 * <p>
 * Uses a custom {@link DataProvider} (mirror/baseline approach) because:
 * <ul>
 *   <li>The standard {@code TagsProvider} API varies across NeoForge versions</li>
 *   <li>Writing to {@code minecraft} namespace is easier with direct JSON generation</li>
 *   <li>Consistent with existing {@link TENModelProvider} and {@link TENRecipeGen}</li>
 * </ul>
 * <p>
 * TODO: Migrate to vanilla {@code BlockTagsProvider} / {@code ItemTagsProvider} /
 * {@code FluidTagsProvider} once the API is stable in NeoForge 26.x.
 */
public class TENTagProvider implements DataProvider {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private final PackOutput output;

    public TENTagProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        var futures = new ArrayList<CompletableFuture<?>>();

        generateBlockTags(cache, futures);
        generateItemTags(cache, futures);
        generateFluidTags(cache, futures);
        generateMinecraftTags(cache, futures);

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    // ═══════════════════════════════════════════════════════════════════
    // Block tags — kenergyengineering namespace
    // ═══════════════════════════════════════════════════════════════════

    private void generateBlockTags(CachedOutput cache, List<CompletableFuture<?>> futures) {
        var tagDir = output.getOutputFolder()
                .resolve("data/" + TEN.MOD_ID + "/tags/block");

        // machines tag — all machines, engines, cables, pipes, cells, channels
        writeTag(cache, tagDir, futures, "machines.json", list(
                "kenergyengineering:machine_smelter",
                "kenergyengineering:machine_pulverizer",
                "kenergyengineering:machine_compressor",
                "kenergyengineering:machine_refiner",
                "kenergyengineering:machine_induction_furnace",
                "kenergyengineering:machine_psionicant",
                "kenergyengineering:machine_beacon_simulator",
                "kenergyengineering:machine_mob_ripper",
                "kenergyengineering:machine_quarry",
                "kenergyengineering:machine_enchantment_flusher",
                "kenergyengineering:machine_matter_condenser",
                "kenergyengineering:machine_farm_manager",
                "kenergyengineering:engine_extraction",
                "kenergyengineering:engine_metal",
                "kenergyengineering:engine_biomass",
                "kenergyengineering:engine_solar",
                "kenergyengineering:cable",
                "kenergyengineering:cable_quartz",
                "kenergyengineering:cable_azure",
                "kenergyengineering:cable_star",
                "kenergyengineering:pipe",
                "kenergyengineering:pipe_white",
                "kenergyengineering:pipe_black",
                "kenergyengineering:energy_cell",
                "kenergyengineering:creative_energy_cell",
                "kenergyengineering:channel_energy",
                "kenergyengineering:channel_item",
                "kenergyengineering:channel_fluid"
        ));

        // quarry_valids — tag references to common tags
        writeTag(cache, tagDir, futures, "quarry_valids.json", list(
                "#c:ores",
                "#c:stones",
                "#c:sands",
                "#c:gravels",
                "#c:netherracks",
                "#c:obsidians",
                "#c:cobblestones"
        ), false);
    }

    // ═══════════════════════════════════════════════════════════════════
    // Item tags — kenergyengineering namespace
    // ═══════════════════════════════════════════════════════════════════

    private void generateItemTags(CachedOutput cache, List<CompletableFuture<?>> futures) {
        var tagDir = output.getOutputFolder()
                .resolve("data/" + TEN.MOD_ID + "/tags/item");

        // catalyst — pulverizer catalysts
        writeTag(cache, tagDir, futures, "catalyst.json", list(
                "#c:dusts/nickel",
                "#c:dusts/iron",
                "#c:dusts/copper",
                "#c:dusts/gold",
                "minecraft:nether_wart",
                "minecraft:chorus_fruit",
                "minecraft:blaze_powder",
                "minecraft:redstone",
                "minecraft:glowstone_dust",
                "#c:mushrooms"
        ), false);

        // common_ingots — common tier ingots
        writeTag(cache, tagDir, futures, "common_ingots.json", list(
                "minecraft:iron_ingot",
                "minecraft:gold_ingot",
                "#c:ingots/tin",
                "#c:ingots/copper",
                "#c:ingots/nickel"
        ), false);

        // uncommon_ingots — uncommon tier ingots
        writeTag(cache, tagDir, futures, "uncommon_ingots.json", list(
                "#c:ingots/chlorium",
                "#c:ingots/powered_tin"
        ), false);

        // valuable_ingots — valuable tier ingots
        writeTag(cache, tagDir, futures, "valuable_ingots.json", list(
                "minecraft:netherite_ingot"
        ), false);

        // moulds — basic moulds
        writeTag(cache, tagDir, futures, "moulds.json", list(
                "kenergyengineering:mould_gear",
                "kenergyengineering:mould_plate",
                "kenergyengineering:mould_rod",
                "kenergyengineering:mould_string"
        ), false);

        // Material sub-tags (mats/)
        Path matsDir = tagDir.resolve("mats");
        // tin: #c:ingots/tin, #c:dusts/tin
        writeTag(cache, matsDir, futures, "tin.json", list("#c:ingots/tin", "#c:dusts/tin"), false);
        // nickel: #c:ingots/nickel, #c:dusts/nickel
        writeTag(cache, matsDir, futures, "nickel.json", list("#c:ingots/nickel", "#c:dusts/nickel"), false);
        // powered_tin: #c:ingots/powered_tin, #c:dusts/powered_tin
        writeTag(cache, matsDir, futures, "powered_tin.json", list("#c:ingots/powered_tin", "#c:dusts/powered_tin"), false);
        // iron: #c:ingots/iron, #c:dusts/iron
        writeTag(cache, matsDir, futures, "iron.json", list("#c:ingots/iron", "#c:dusts/iron"), false);
        // gold: #c:ingots/gold, #c:dusts/gold
        writeTag(cache, matsDir, futures, "gold.json", list("#c:ingots/gold", "#c:dusts/gold"), false);
        // copper: #c:ingots/copper, #c:dusts/copper
        writeTag(cache, matsDir, futures, "copper.json", list("#c:ingots/copper", "#c:dusts/copper"), false);
        // chlorium: #c:ingots/chlorium, #c:dusts/chlorium
        writeTag(cache, matsDir, futures, "chlorium.json", list("#c:ingots/chlorium", "#c:dusts/chlorium"), false);
    }

    // ═══════════════════════════════════════════════════════════════════
    // Fluid tags — kenergyengineering namespace (none currently)
    // ═══════════════════════════════════════════════════════════════════

    private void generateFluidTags(CachedOutput cache, List<CompletableFuture<?>> futures) {
        // No kenergyengineering:tags/fluid/ entries in baseline.
        // Fluid tags go to minecraft namespace (see generateMinecraftTags).
    }

    // ═══════════════════════════════════════════════════════════════════
    // Minecraft namespace tags — baseline mirror
    // ═══════════════════════════════════════════════════════════════════

    private void generateMinecraftTags(CachedOutput cache, List<CompletableFuture<?>> futures) {
        // ── block/needs_iron_tool ─────────────────────────────────────
        // All ores and storage blocks require iron tier or better.
        writeTag(cache, output.getOutputFolder().resolve("data/minecraft/tags/block"),
                futures, "needs_iron_tool.json", list(
                        "kenergyengineering:tin_ore",
                        "kenergyengineering:nickel_ore",
                        "kenergyengineering:deep_tin_ore",
                        "kenergyengineering:deep_nickel_ore",
                        "kenergyengineering:tin_block",
                        "kenergyengineering:nickel_block",
                        "kenergyengineering:powered_tin_block",
                        "kenergyengineering:chlorium_block",
                        "kenergyengineering:raw_tin_block",
                        "kenergyengineering:raw_nickel_block"
                ));

        // ── block/mineable/pickaxe ─────────────────────────────────────
        // Same set of blocks is pickaxe-mineable.
        writeTag(cache, output.getOutputFolder().resolve("data/minecraft/tags/block/mineable"),
                futures, "pickaxe.json", list(
                        "kenergyengineering:tin_ore",
                        "kenergyengineering:nickel_ore",
                        "kenergyengineering:deep_tin_ore",
                        "kenergyengineering:deep_nickel_ore",
                        "kenergyengineering:tin_block",
                        "kenergyengineering:nickel_block",
                        "kenergyengineering:powered_tin_block",
                        "kenergyengineering:chlorium_block",
                        "kenergyengineering:raw_tin_block",
                        "kenergyengineering:raw_nickel_block"
                ));

        // ── fluid/water ────────────────────────────────────────────────
        // Liquid XP and Liquid Bizarrerie are tagged as water-like fluids.
        writeTag(cache, output.getOutputFolder().resolve("data/minecraft/tags/fluid"),
                futures, "water.json", list(
                        "kenergyengineering:liquid_xp",
                        "kenergyengineering:liquid_xp_flowing",
                        "kenergyengineering:liquid_bizarrerie",
                        "kenergyengineering:liquid_bizarrerie_flowing"
                ), false);
    }

    // ═══════════════════════════════════════════════════════════════════
    // Utilities
    // ═══════════════════════════════════════════════════════════════════

    /** Create a mutable list from elements. */
    @SafeVarargs
    private static <T> List<T> list(T... elements) {
        return new ArrayList<>(List.of(elements));
    }

    /**
     * Write a tag JSON file with {@code "replace": true} (default).
     * Matches the vanilla convention for mod-owned tags.
     */
    private void writeTag(CachedOutput cache, Path dir, List<CompletableFuture<?>> futures,
                          String fileName, List<String> values) {
        writeTag(cache, dir, futures, fileName, values, true);
    }

    /**
     * Write a tag JSON file with explicit {@code replace} value.
     *
     * @param replace whether to set {@code "replace": true}
     */
    private void writeTag(CachedOutput cache, Path dir, List<CompletableFuture<?>> futures,
                          String fileName, List<String> values, boolean replace) {
        var json = new JsonObject();
        json.addProperty("replace", replace);

        var array = new JsonArray();
        for (var value : values) {
            array.add(value);
        }
        json.add("values", array);

        writeJson(cache, dir.resolve(fileName), json, futures);
    }

    private void writeJson(CachedOutput cache, Path file, JsonElement json, List<CompletableFuture<?>> futures) {
        futures.add(DataProvider.saveStable(cache, json, file));
    }

    @Override
    public String getName() {
        return "TEN Tag Provider";
    }
}
