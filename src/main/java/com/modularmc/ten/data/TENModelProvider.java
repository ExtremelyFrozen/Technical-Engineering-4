package com.modularmc.ten.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.modularmc.ten.TEN;
import com.modularmc.ten.common.data.TENBlocks;
import com.modularmc.ten.common.data.TENFluids;
import com.modularmc.ten.common.data.TENItems;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Model/blockstate generator for Technical Engineering 4.
 * <p>
 * This provider generates blockstate JSONs, block model JSONs, and item model JSONs
 * for all registered blocks and items. Complex models (cables, channels, energy cells)
 * use embedded JSON templates rather than the Vanilla {@code BlockModelGenerators} API,
 * which lacks support for multipart and custom-element models.
 * <p>
 * TODO(optimize): Migrate per-block-type model generation to Vanilla API where feasible.
 */
public class TENModelProvider implements DataProvider {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private final PackOutput output;
    private final String modId;

    public TENModelProvider(PackOutput output, String modId) {
        this.output = output;
        this.modId = modId;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        var futures = new ArrayList<CompletableFuture<?>>();

        // ── Blockstates ───────────────────────────────────────────────
        var bsPath = output.getOutputFolder().resolve("assets/" + modId + "/blockstates");

        // 1. Simple blocks: ores, storage
        simpleBlockstate(cache, bsPath, futures, "tin_ore");
        simpleBlockstate(cache, bsPath, futures, "nickel_ore");
        simpleBlockstate(cache, bsPath, futures, "deep_tin_ore");
        simpleBlockstate(cache, bsPath, futures, "deep_nickel_ore");
        simpleBlockstate(cache, bsPath, futures, "tin_block");
        simpleBlockstate(cache, bsPath, futures, "nickel_block");
        simpleBlockstate(cache, bsPath, futures, "powered_tin_block");
        simpleBlockstate(cache, bsPath, futures, "chlorium_block");
        simpleBlockstate(cache, bsPath, futures, "raw_tin_block");
        simpleBlockstate(cache, bsPath, futures, "raw_nickel_block");

        // 2. Machines (horizontal + active)
        for (var name : MACHINE_NAMES) {
            horizontalActiveBlockstate(cache, bsPath, futures, name);
        }

        // 3. Energy cells (same pattern as machines but with empty/active models)
        for (var name : CELL_NAMES) {
            horizontalActiveBlockstate(cache, bsPath, futures, name, "_empty");
        }

        // 4. Cables (multipart) — embedded template
        for (var name : CABLE_NAMES) {
            cableBlockstate(cache, bsPath, futures, name);
        }

        // 5. Pipes (same as cables)
        for (var name : PIPE_NAMES) {
            cableBlockstate(cache, bsPath, futures, name);
        }

        // 6. Channels (6-direction + active)
        for (var name : CHANNEL_NAMES) {
            directionalActiveBlockstate(cache, bsPath, futures, name);
        }

        // 7. Liquid blocks
        for (var name : LIQUID_NAMES) {
            simpleBlockstate(cache, bsPath, futures, name);
        }

        // ── Block Models ──────────────────────────────────────────────
        var blockModelPath = output.getOutputFolder().resolve("assets/" + modId + "/models/block");

        // Simple cube_all blocks
        for (var name : CUBE_ALL_NAMES) {
            cubeAllModel(cache, blockModelPath, futures, name);
        }

        // Machine blocks (cube with per-face textures)
        for (var name : MACHINE_NAMES) {
            machineModel(cache, blockModelPath, futures, name, false);
            machineModel(cache, blockModelPath, futures, name, true);
        }
        // Energy cells — complex model, use embedded template
        for (var name : CELL_NAMES) {
            parentModel(cache, blockModelPath, futures, name, modId + ":block/" + name);
            parentModel(cache, blockModelPath, futures, name + "_empty", modId + ":block/" + name + "_empty");
        }

        // Cables — simple parent to sub-model
        for (var name : CABLE_NAMES) {
            if (name.equals("cable")) {
                parentModel(cache, blockModelPath, futures, name, modId + ":block/cable/cable_core");
            } else {
                parentModel(cache, blockModelPath, futures, name, modId + ":block/cable/cable_" + name.replace("cable_", "") + "_core");
            }
        }

        // Pipes — same as cables
        for (var name : PIPE_NAMES) {
            parentModel(cache, blockModelPath, futures, name, modId + ":block/cable/cable_core");
        }

        // Channels — simple parent to sub-model
        for (var name : CHANNEL_NAMES) {
            parentModel(cache, blockModelPath, futures, name, modId + ":block/channel/" + name);
            parentModel(cache, blockModelPath, futures, name + "_active", modId + ":block/channel/" + name + "_active");
        }

        // Liquid blocks — particle-only model (no cube_all, fluid uses custom renderer)
        for (var name : LIQUID_NAMES) {
            liquidModel(cache, blockModelPath, futures, name);
        }

        // ── Item Models ───────────────────────────────────────────────
        var itemModelPath = output.getOutputFolder().resolve("assets/" + modId + "/models/item");

        // Collect all block item names (items that are also blocks)
        var blockNames = new HashSet<>(List.of(
                "tin_ore", "nickel_ore", "deep_tin_ore", "deep_nickel_ore",
                "tin_block", "nickel_block", "powered_tin_block", "chlorium_block",
                "raw_tin_block", "raw_nickel_block"
        ));
        blockNames.addAll(List.of(MACHINE_NAMES));
        blockNames.addAll(List.of(CABLE_NAMES));
        blockNames.addAll(List.of(PIPE_NAMES));
        blockNames.addAll(List.of(CELL_NAMES));
        blockNames.addAll(List.of(CHANNEL_NAMES));
        // Liquid blocks do not have BlockItems and have no item model in baseline
        // blockNames.addAll(List.of(LIQUID_NAMES));

        // Generate block-item models (parent = block model)
        for (var name : blockNames) {
            blockItemModel(cache, itemModelPath, futures, name);
        }

        // Generate non-block item models
        for (var entry : TENItems.ZH_NAMES.entrySet()) {
            var name = entry.getKey();
            if (blockNames.contains(name)) continue;

            if (name.startsWith("mould_")) {
                // Mould items — baseline uses "mold" directory.
                // gear/plate/rod/string use "model_" prefix in filename, others use bare name.
                var suffix = name.replace("mould_", "");
                var mouldName = switch (suffix) {
                    case "gear", "plate", "rod", "string" -> "model_" + suffix;
                    default -> suffix;
                };
                generatedItemModel(cache, itemModelPath, futures, name,
                        modId + ":item/mold/" + mouldName);
            } else if (name.endsWith("_levelup")) {
                // Upgrade items
                generatedItemModel(cache, itemModelPath, futures, name,
                        modId + ":item/upgrade/" + name);
            } else if (name.contains("_dust") || name.contains("_ingot") || name.contains("_nugget")
                    || name.contains("_plate") || name.contains("_gear") || name.contains("_rod")
                    || name.contains("_wire")) {
                // Material variant items: determine category
                String category = null;
                if (name.contains("_dust")) category = "dust";
                else if (name.contains("_ingot")) category = "ingot";
                else if (name.contains("_nugget")) category = "nugget";
                else if (name.contains("_plate")) category = "plate";
                else if (name.contains("_gear")) category = "gear";
                else if (name.contains("_rod")) category = "rod";
                else if (name.contains("_wire")) category = "wire";
                // raw materials are handled below as "other"
                if (category != null) {
                    generatedItemModel(cache, itemModelPath, futures, name,
                            modId + ":item/material/" + category + "/" + name);
                } else {
                    generatedItemModel(cache, itemModelPath, futures, name,
                            modId + ":item/" + name);
                }
            } else if (name.equals("raw_tin") || name.equals("raw_nickel")
                    || name.equals("royal_jelly") || name.equals("spicy_jelly")) {
                // Raw materials and special items
                generatedItemModel(cache, itemModelPath, futures, name,
                        modId + ":item/" + name);
            } else {
                // Default: crafting components and tools
                // Check if it's a crafting component
                var craftingComponents = Set.of(
                        "redstone_conductor", "redstone_converter", "redstone_storer",
                        "indigo", "azure_glass", "bizarrerie", "redstone_ai",
                        "redstone_ai_advanced", "hydraulic_widget", "detector"
                );
                if (craftingComponents.contains(name)) {
                    generatedItemModel(cache, itemModelPath, futures, name,
                            modId + ":item/crafting/" + name);
                } else {
                    generatedItemModel(cache, itemModelPath, futures, name,
                            modId + ":item/" + name);
                }
            }
        }

        // Generate bucket item models for fluid buckets (registered in TENFluids, not TENItems)
        for (var liquidName : LIQUID_NAMES) {
            var bucketName = liquidName + "_bucket";
            generatedItemModel(cache, itemModelPath, futures, bucketName,
                    modId + ":block/" + bucketName);
        }

        // ── Item Definitions (26.1.2: assets/<namespace>/items/<id>.json) ──
        // NeoForge 26.1.2 / Minecraft 1.21.5 changed item model lookup:
        // instead of resolving models/item/<id>.json directly, the runtime now
        // requires an item definition at items/<id>.json with the format:
        //   {"model": {"type": "minecraft:model", "model": "<model_location>"}}
        // See: https://minecraft.wiki/w/Model#Item_models
        var itemDefPath = output.getOutputFolder().resolve("assets/" + modId + "/items");

        // Block items → reference block model directly
        for (var name : blockNames) {
            itemDefinition(cache, itemDefPath, futures, name, modId + ":block/" + name);
        }

        // Non-block items → reference flat item model path
        // Item model files are always at models/item/<name>.json,
        // referenced as kenergyengineering:item/<name>.
        // NOTE: texture paths inside those model JSONs may differ (e.g.
        // "kenergyengineering:item/material/dust/iron_dust" for layer0),
        // but the item definition's model field must point to the model
        // file location, not the texture path.
        for (var entry : TENItems.ZH_NAMES.entrySet()) {
            var name = entry.getKey();
            if (blockNames.contains(name)) continue;
            itemDefinition(cache, itemDefPath, futures, name, modId + ":item/" + name);
        }

        // Bucket item definitions — reference item model, not block model
        // Bucket models are at models/item/<bucket_name>.json, so the
        // definition must point to kenergyengineering:item/<bucket_name>.
        for (var liquidName : LIQUID_NAMES) {
            var bucketName = liquidName + "_bucket";
            itemDefinition(cache, itemDefPath, futures, bucketName,
                    modId + ":item/" + bucketName);
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    // ═══════════════════════════════════════════════════════════════════
    // Block list constants
    // ═══════════════════════════════════════════════════════════════════

    private static final String[] CUBE_ALL_NAMES = {
            "tin_ore", "nickel_ore", "deep_tin_ore", "deep_nickel_ore",
            "tin_block", "nickel_block", "powered_tin_block", "chlorium_block",
            "raw_tin_block", "raw_nickel_block"
    };

    private static final String[] MACHINE_NAMES = {
            "machine_smelter", "machine_pulverizer", "machine_compressor", "machine_refiner",
            "machine_induction_furnace", "machine_psionicant", "machine_beacon_simulator",
            "machine_mob_ripper", "machine_quarry", "machine_enchantment_flusher",
            "machine_matter_condenser", "machine_farm_manager"
    };

    private static final String[] CELL_NAMES = {
            "energy_cell", "creative_energy_cell"
    };

    private static final String[] CABLE_NAMES = {
            "cable", "cable_quartz", "cable_azure", "cable_star"
    };

    private static final String[] PIPE_NAMES = {
            "pipe", "pipe_white", "pipe_black"
    };

    private static final String[] CHANNEL_NAMES = {
            "channel_energy", "channel_item", "channel_fluid"
    };

    private static final String[] LIQUID_NAMES = {
            "liquid_xp", "liquid_bizarrerie", "liquid_honey", "liquid_royal_jelly", "liquid_spicy_jelly"
    };

    // ═══════════════════════════════════════════════════════════════════
    // Blockstate generation
    // ═══════════════════════════════════════════════════════════════════

    /** Simple variant: {"": {"model": "modid:block/<name>"}} */
    private void simpleBlockstate(CachedOutput cache, Path dir, List<CompletableFuture<?>> futures, String name) {
        var json = new JsonObject();
        var variants = new JsonObject();
        var model = new JsonObject();
        model.addProperty("model", modId + ":block/" + name);
        variants.add("", model);
        json.add("variants", variants);
        writeJson(cache, dir.resolve(name + ".json"), json, futures);
    }

    /** Horizontal variant blockstate with active=true/false. Machine pattern. */
    private void horizontalActiveBlockstate(CachedOutput cache, Path dir, List<CompletableFuture<?>> futures, String name) {
        horizontalActiveBlockstate(cache, dir, futures, name, "_active");
    }

    /** Horizontal variant blockstate with active=true/false and custom active suffix. */
    private void horizontalActiveBlockstate(CachedOutput cache, Path dir, List<CompletableFuture<?>> futures,
                                             String name, String activeSuffix) {
        var json = new JsonObject();
        var variants = new JsonObject();
        var facingValues = List.of("north", "east", "south", "west");
        var yRotations = Map.of("north", 0, "east", 90, "south", 180, "west", 270);

        for (var active : List.of("false", "true")) {
            var modelName = active.equals("true") ? name + activeSuffix : name;
            for (var facing : facingValues) {
                var key = "active=" + active + ",facing=" + facing;
                var entry = new JsonObject();
                entry.addProperty("model", modId + ":block/" + modelName);
                int y = yRotations.get(facing);
                if (y != 0) entry.addProperty("y", y);
                variants.add(key, entry);
            }
        }
        json.add("variants", variants);
        writeJson(cache, dir.resolve(name + ".json"), json, futures);
    }

    /** Directional variant blockstate (6 directions) with active=true/false. */
    private void directionalActiveBlockstate(CachedOutput cache, Path dir, List<CompletableFuture<?>> futures, String name) {
        var json = new JsonObject();
        var variants = new JsonObject();
        var dirMap = new HashMap<String, int[]>();
        dirMap.put("north", new int[]{0, 0});
        dirMap.put("east", new int[]{0, 90});
        dirMap.put("south", new int[]{0, 180});
        dirMap.put("west", new int[]{0, 270});
        dirMap.put("up", new int[]{270, 0});
        dirMap.put("down", new int[]{90, 0});

        for (var active : List.of("false", "true")) {
            var modelName = active.equals("true") ? name + "_active" : name;
            for (var entry : dirMap.entrySet()) {
                var facing = entry.getKey();
                var rot = entry.getValue();
                var key = "active=" + active + ",facing=" + facing;
                var obj = new JsonObject();
                obj.addProperty("model", modId + ":block/" + modelName);
                if (rot[0] != 0) obj.addProperty("x", rot[0]);
                if (rot[1] != 0) obj.addProperty("y", rot[1]);
                variants.add(key, obj);
            }
        }
        json.add("variants", variants);
        writeJson(cache, dir.resolve(name + ".json"), json, futures);
    }

    /**
     * Cable/pipe multipart blockstate — generated once per base name.
     * References sub-models in {@code block/cable/} directory (baseline mirror).
     */
    private void cableBlockstate(CachedOutput cache, Path dir, List<CompletableFuture<?>> futures, String name) {
        // Determine cable variant prefix
        String prefix = "cable";
        if (name.startsWith("cable_")) {
            prefix = "cable_" + name.replace("cable_", "");
        }

        var json = new JsonObject();
        var multipart = new JsonArray();

        // Core
        multipart.add(multipartEntry(modId + ":block/cable/" + prefix + "_core", "active", "false"));
        multipart.add(multipartEntry(modId + ":block/cable/" + prefix + "_core_active", "active", "true"));

        // Six directions: down, up, north, south, west, east
        var dirs = List.of(
                new Dir("down", 90, 0),
                new Dir("up", 270, 0),
                new Dir("north", 0, 0),
                new Dir("south", 0, 180),
                new Dir("west", 0, 270),
                new Dir("east", 0, 90)
        );

        for (var d : dirs) {
            // Part connections (value=1)
            multipart.add(multipartEntryRotated(modId + ":block/cable/" + prefix + "_part",
                    "active", "false", d.value(), "1", d.x(), d.y()));
            multipart.add(multipartEntryRotated(modId + ":block/cable/" + prefix + "_part_active",
                    "active", "true", d.value(), "1", d.x(), d.y()));
            // Connect connections (value=2)
            multipart.add(multipartEntryRotated(modId + ":block/cable/" + prefix + "_connect",
                    "active", "false", d.value(), "2", d.x(), d.y()));
            multipart.add(multipartEntryRotated(modId + ":block/cable/" + prefix + "_connect_active",
                    "active", "true", d.value(), "2", d.x(), d.y()));
        }

        json.add("multipart", multipart);
        writeJson(cache, dir.resolve(name + ".json"), json, futures);
    }

    private record Dir(String value, int x, int y) {}

    private JsonObject multipartEntry(String model, String condKey, String condValue) {
        var entry = new JsonObject();
        var apply = new JsonObject();
        apply.addProperty("model", model);
        entry.add("apply", apply);
        var when = new JsonObject();
        when.addProperty(condKey, condValue);
        entry.add("when", when);
        return entry;
    }

    private JsonObject multipartEntryRotated(String model, String condKey, String condValue,
                                              String dirProp, String dirValue, int x, int y) {
        var entry = new JsonObject();
        var apply = new JsonObject();
        apply.addProperty("model", model);
        if (x != 0) apply.addProperty("x", x);
        if (y != 0) apply.addProperty("y", y);
        entry.add("apply", apply);
        var when = new JsonObject();
        when.addProperty(condKey, condValue);
        when.addProperty(dirProp, dirValue);
        entry.add("when", when);
        return entry;
    }

    // ═══════════════════════════════════════════════════════════════════
    // Block model generation
    // ═══════════════════════════════════════════════════════════════════

    /** Generate a cube_all model. */
    private void cubeAllModel(CachedOutput cache, Path dir, List<CompletableFuture<?>> futures, String name) {
        var json = new JsonObject();
        json.addProperty("parent", "minecraft:block/cube_all");
        var textures = new JsonObject();
        textures.addProperty("all", modId + ":block/" + name);
        json.add("textures", textures);
        writeJson(cache, dir.resolve(name + ".json"), json, futures);
    }

    /**
     * Generate a machine block model using {@code minecraft:block/cube} parent
     * with standard machine face textures.
     */
    private void machineModel(CachedOutput cache, Path dir, List<CompletableFuture<?>> futures,
                               String name, boolean active) {
        var activeSuffix = active ? "_active" : "";
        var json = new JsonObject();
        json.addProperty("parent", "minecraft:block/cube");
        var textures = new JsonObject();
        textures.addProperty("down", modId + ":block/machine_bottom");
        textures.addProperty("up", modId + ":block/machine_top");
        // North face is the front — use the specific machine texture
        textures.addProperty("north", modId + ":block/" + name + activeSuffix);
        textures.addProperty("south", modId + ":block/machine_side");
        textures.addProperty("east", modId + ":block/machine_side_connection");
        textures.addProperty("west", modId + ":block/machine_side_connection_flipped");
        textures.addProperty("particle", modId + ":block/machine_side");
        json.add("textures", textures);
        writeJson(cache, dir.resolve(name + activeSuffix + ".json"), json, futures);
    }

    /** Generate a liquid block model — particle-only (fluid uses custom renderer). */
    private void liquidModel(CachedOutput cache, Path dir, List<CompletableFuture<?>> futures, String name) {
        var json = new JsonObject();
        var textures = new JsonObject();
        textures.addProperty("particle", modId + ":block/" + name);
        json.add("textures", textures);
        writeJson(cache, dir.resolve(name + ".json"), json, futures);
    }

    /** Simple parent-reference block model. */
    private void parentModel(CachedOutput cache, Path dir, List<CompletableFuture<?>> futures,
                              String name, String parent) {
        var json = new JsonObject();
        json.addProperty("parent", parent);
        writeJson(cache, dir.resolve(name + ".json"), json, futures);
    }

    // ═══════════════════════════════════════════════════════════════════
    // Item model generation
    // ═══════════════════════════════════════════════════════════════════

    /** Item model that copies a block model reference. */
    private void blockItemModel(CachedOutput cache, Path dir, List<CompletableFuture<?>> futures, String name) {
        var json = new JsonObject();
        json.addProperty("parent", modId + ":block/" + name);
        writeJson(cache, dir.resolve(name + ".json"), json, futures);
    }

    /** Item model using {@code item/generated} with a single layer. */
    private void generatedItemModel(CachedOutput cache, Path dir, List<CompletableFuture<?>> futures,
                                      String name, String texturePath) {
        var json = new JsonObject();
        json.addProperty("parent", "minecraft:item/generated");
        var textures = new JsonObject();
        textures.addProperty("layer0", texturePath);
        json.add("textures", textures);
        writeJson(cache, dir.resolve(name + ".json"), json, futures);
    }

    // ═══════════════════════════════════════════════════════════════════
    // Item definition generation (26.1.2)
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Generate an item definition JSON for NeoForge 26.1.2 / Minecraft 1.21.5.
     * <p>
     * File: {@code assets/<modId>/items/<name>.json}
     * <p>
     * This is the new item model definition format required at runtime.
     * Without it, items report "Missing item model" even if
     * {@code models/item/<name>.json} exists, because the model lookup chain
     * in 26.1.2 first reads the item definition ({@code items/<id>.json}),
     * then resolves the model location from it.
     * <p>
     * Format reference:
     * <pre>{@code
     * {
     *   "model": {
     *     "type": "minecraft:model",
     *     "model": "<namespace>:<model_path>"
     *   }
     * }
     * }</pre>
     */
    private void itemDefinition(CachedOutput cache, Path dir, List<CompletableFuture<?>> futures,
                                 String name, String modelLocation) {
        var outer = new JsonObject();
        var inner = new JsonObject();
        inner.addProperty("type", "minecraft:model");
        inner.addProperty("model", modelLocation);
        outer.add("model", inner);
        writeJson(cache, dir.resolve(name + ".json"), outer, futures);
    }

    // ═══════════════════════════════════════════════════════════════════
    // JSON writing utility
    // ═══════════════════════════════════════════════════════════════════

    private void writeJson(CachedOutput cache, Path file, JsonElement json, List<CompletableFuture<?>> futures) {
        futures.add(DataProvider.saveStable(cache, json, file));
    }

    @Override
    public String getName() {
        return "TEN Model Provider";
    }
}
