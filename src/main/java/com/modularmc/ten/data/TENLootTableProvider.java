package com.modularmc.ten.data;

import com.modularmc.ten.TEN;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Generates block loot tables for all mod blocks.
 * <p>
 * All 38 blocks in the baseline use a simple self-drop pattern
 * with survives_explosion condition. Fluid blocks ({@code liquid_*})
 * are excluded — they use {@code noLootTable()} in their properties.
 * <p>
 * This is a custom {@link DataProvider} that writes JSON directly,
 * consistent with the project's existing {@link TENModelProvider}
 * and {@link TENRecipeGen} approach.
 */
public class TENLootTableProvider implements DataProvider {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private final PackOutput output;

    public TENLootTableProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        var lootDir = output.getOutputFolder()
                .resolve("data/" + TEN.MOD_ID + "/loot_table/blocks");
        var futures = new ArrayList<CompletableFuture<?>>();

        // ── Ores (4) ──────────────────────────────────────────────────
        selfDropBlock(cache, lootDir, futures, "tin_ore");
        selfDropBlock(cache, lootDir, futures, "nickel_ore");
        selfDropBlock(cache, lootDir, futures, "deep_tin_ore");
        selfDropBlock(cache, lootDir, futures, "deep_nickel_ore");

        // ── Storage blocks (6) ────────────────────────────────────────
        selfDropBlock(cache, lootDir, futures, "tin_block");
        selfDropBlock(cache, lootDir, futures, "nickel_block");
        selfDropBlock(cache, lootDir, futures, "powered_tin_block");
        selfDropBlock(cache, lootDir, futures, "chlorium_block");
        selfDropBlock(cache, lootDir, futures, "raw_tin_block");
        selfDropBlock(cache, lootDir, futures, "raw_nickel_block");

        // ── Machines (12) ─────────────────────────────────────────────
        selfDropBlock(cache, lootDir, futures, "machine_smelter");
        selfDropBlock(cache, lootDir, futures, "machine_pulverizer");
        selfDropBlock(cache, lootDir, futures, "machine_compressor");
        selfDropBlock(cache, lootDir, futures, "machine_refiner");
        selfDropBlock(cache, lootDir, futures, "machine_induction_furnace");
        selfDropBlock(cache, lootDir, futures, "machine_psionicant");
        selfDropBlock(cache, lootDir, futures, "machine_beacon_simulator");
        selfDropBlock(cache, lootDir, futures, "machine_mob_ripper");
        selfDropBlock(cache, lootDir, futures, "machine_quarry");
        selfDropBlock(cache, lootDir, futures, "machine_enchantment_flusher");
        selfDropBlock(cache, lootDir, futures, "machine_matter_condenser");
        selfDropBlock(cache, lootDir, futures, "machine_farm_manager");

        // ── Engines (4) ───────────────────────────────────────────────
        selfDropBlock(cache, lootDir, futures, "engine_extraction");
        selfDropBlock(cache, lootDir, futures, "engine_metal");
        selfDropBlock(cache, lootDir, futures, "engine_biomass");
        selfDropBlock(cache, lootDir, futures, "engine_solar");

        // ── Cables (4) ────────────────────────────────────────────────
        selfDropBlock(cache, lootDir, futures, "cable");
        selfDropBlock(cache, lootDir, futures, "cable_quartz");
        selfDropBlock(cache, lootDir, futures, "cable_azure");
        selfDropBlock(cache, lootDir, futures, "cable_star");

        // ── Pipes (3) ─────────────────────────────────────────────────
        selfDropBlock(cache, lootDir, futures, "pipe");
        selfDropBlock(cache, lootDir, futures, "pipe_white");
        selfDropBlock(cache, lootDir, futures, "pipe_black");

        // ── Energy cells (2) ──────────────────────────────────────────
        selfDropBlock(cache, lootDir, futures, "energy_cell");
        selfDropBlock(cache, lootDir, futures, "creative_energy_cell");

        // ── Channels (3) ──────────────────────────────────────────────
        selfDropBlock(cache, lootDir, futures, "channel_energy");
        selfDropBlock(cache, lootDir, futures, "channel_item");
        selfDropBlock(cache, lootDir, futures, "channel_fluid");

        // Note: Fluid blocks (liquid_*) are excluded — they use
        // .noLootTable() in their block properties and have no baseline
        // loot tables.

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    /**
     * Generate a self-drop loot table for a block (single item, survives explosion).
     * <p>
     * Matches the baseline pattern:
     *
     * <pre>
     * {
     *   "type": "minecraft:block",
     *   "pools": [{
     *     "bonus_rolls": 0.0,
     *     "conditions": [{"condition": "minecraft:survives_explosion"}],
     *     "entries": [{"type": "minecraft:item", "name": "kenergyengineering:&lt;name&gt;"}],
     *     "rolls": 1.0
     *   }],
     *   "random_sequence": "kenergyengineering:blocks/&lt;name&gt;"
     * }
     * </pre>
     */
    private void selfDropBlock(CachedOutput cache, Path dir, List<CompletableFuture<?>> futures, String name) {
        var json = new JsonObject();
        json.addProperty("type", "minecraft:block");

        var pool = new JsonObject();
        pool.addProperty("bonus_rolls", 0.0);

        var conditions = new JsonArray();
        var condition = new JsonObject();
        condition.addProperty("condition", "minecraft:survives_explosion");
        conditions.add(condition);
        pool.add("conditions", conditions);

        var entries = new JsonArray();
        var entry = new JsonObject();
        entry.addProperty("type", "minecraft:item");
        entry.addProperty("name", TEN.MOD_ID + ":" + name);
        entries.add(entry);
        pool.add("entries", entries);

        pool.addProperty("rolls", 1.0);

        var pools = new JsonArray();
        pools.add(pool);
        json.add("pools", pools);

        json.addProperty("random_sequence", TEN.MOD_ID + ":blocks/" + name);

        writeJson(cache, dir.resolve(name + ".json"), json, futures);
    }

    private void writeJson(CachedOutput cache, Path file, JsonElement json, List<CompletableFuture<?>> futures) {
        futures.add(DataProvider.saveStable(cache, json, file));
    }

    @Override
    public String getName() {
        return "TEN Loot Table Provider";
    }
}
