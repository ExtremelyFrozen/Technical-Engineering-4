package com.modularmc.ten.data;

import com.modularmc.ten.TEN;
import com.modularmc.ten.common.data.Mat;
import com.modularmc.ten.common.data.TENBlocks;
import com.modularmc.ten.common.data.TENItems;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Generates all block, item, and fluid tags for the mod, including
 * minecraft-namespace tags (needs_iron_tool, mineable/pickaxe, fluid/water),
 * mod-namespace tags (machines, wrench_dismantleable, moulds, etc.), and
 * common (c:) tags.
 * <p>
 * <b>Source of truth:</b> Block and item tags that reflect the mod's own
 * registrations are generated from the semantic collections in
 * {@link TENBlocks} and {@link TENItems}, not from hardcoded ID lists.
 * This ensures tag membership automatically stays in sync with registration.
 * <p>
 * <b>Ownership separation:</b>
 * <ul>
 * <li><b>Generated owns:</b> All tags that directly mirror mod registrations:
 * kenergyengineering:machines, kenergyengineering: moulds (item),
 * kenergyengineering:wrench_dismantleable, mineable/pickaxe,
 * needs_iron_tool, c: item form tags (dusts, ingots, nuggets, plates,
 * gears, rods, wires), c:ores block+item, c:storage_blocks block+item,
 * c:raw_materials item, kenergyengineering:mats/* (business classification),
 * kenergyengineering:catalyst, kenergyengineering:common_ingots,
 * kenergyengineering:uncommon_ingots, kenergyengineering:valuable_ingots.
 * </li>
 * <li><b>Main owns:</b> Hand-written business tags that are not simple
 * registration mirrors: c:gems/*, c:mushrooms, c:obsidians, c:stones,
 * c:sands, c:gravels, c:netherracks, c:cobblestones, c:raw_materials/iron,
 * c:raw_materials/gold, c:raw_materials/copper, and the aggregate
 * c:ores/tin, c:ores/nickel (for vanilla copper we reference
 * {@code #minecraft:copper_ores} in main).</li>
 * <li><b>Intersection must be 0</b> — no tag file appears in both
 * main and generated.</li>
 * </ul>
 * <p>
 * All tags use {@code "replace": false} (additive) to avoid overwriting
 * vanilla or other mods' tag contributions.
 * <p>
 * Uses a custom {@link DataProvider} because:
 * <ul>
 * <li>The standard {@code TagsProvider} API varies across NeoForge versions</li>
 * <li>Writing to {@code minecraft} and {@code c} namespaces is easier with direct JSON generation</li>
 * <li>Consistent with existing {@link TENModelProvider} and {@link TENRecipeGen}</li>
 * </ul>
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
        generateCTagItemTags(cache, futures);
        // Block c: tags for ores and storage_blocks
        generateCBlockTags(cache, futures);
        // Item c: tags for ores, storage_blocks, and raw_materials (block items)
        generateCBlockItemTags(cache, futures);
        generateCRawMaterialsItemTags(cache, futures);
        generateFluidTags(cache, futures);
        generateMinecraftTags(cache, futures);

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    // ═══════════════════════════════════════════════════════════════════
    // Helpers: convert DeferredHolder to registry ID string
    // ═══════════════════════════════════════════════════════════════════

    private static String blockId(net.neoforged.neoforge.registries.DeferredHolder<?, ?> holder) {
        return holder.getId().toString();
    }

    // Vanilla raw storage blocks that need c:storage_blocks/raw_* tags
    private static final String[][] VANILLA_RAW_BLOCKS = {
            { "iron", "minecraft:raw_iron_block" },
            { "gold", "minecraft:raw_gold_block" },
            { "copper", "minecraft:raw_copper_block" },
    };

    // ═══════════════════════════════════════════════════════════════════
    // Block tags — kenergyengineering namespace
    // ═══════════════════════════════════════════════════════════════════

    private void generateBlockTags(CachedOutput cache, List<CompletableFuture<?>> futures) {
        var tagDir = output.getOutputFolder()
                .resolve("data/" + TEN.MOD_ID + "/tags/block");

        // machines tag — all functional blocks from TENBlocks collections
        writeTag(cache, tagDir, futures, "machines.json",
                TENBlocks.getAllFunctional().stream()
                        .map(TENTagProvider::blockId)
                        .toList(),
                false);

        // wrench_dismantleable tag — same set as machines (independent list for future exclusion)
        writeTag(cache, tagDir, futures, "wrench_dismantleable.json",
                TENBlocks.getAllFunctional().stream()
                        .map(TENTagProvider::blockId)
                        .toList(),
                false);

        // quarry_valids — tag references to common tags (unchanged from hand-written)
        writeTag(cache, tagDir, futures, "quarry_valids.json", list(
                "#c:ores",
                "#c:stones",
                "#c:sands",
                "#c:gravels",
                "#c:netherracks",
                "#c:obsidians",
                "#c:cobblestones"), false);
    }

    // ═══════════════════════════════════════════════════════════════════
    // Item tags — kenergyengineering namespace
    // ═══════════════════════════════════════════════════════════════════

    private void generateItemTags(CachedOutput cache, List<CompletableFuture<?>> futures) {
        var tagDir = output.getOutputFolder()
                .resolve("data/" + TEN.MOD_ID + "/tags/item");

        // moulds tag — generated from TENItems mould collection
        writeTag(cache, tagDir, futures, "moulds.json",
                TENItems.getMouldHolders().stream()
                        .map(h -> h.getId().toString())
                        .toList(),
                false);

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
                "#c:mushrooms"), false);

        // common_ingots — common tier ingots
        writeTag(cache, tagDir, futures, "common_ingots.json", list(
                "minecraft:iron_ingot",
                "minecraft:gold_ingot",
                "#c:ingots/tin",
                "#c:ingots/copper",
                "#c:ingots/nickel"), false);

        // uncommon_ingots — uncommon tier ingots
        writeTag(cache, tagDir, futures, "uncommon_ingots.json", list(
                "#c:ingots/chlorium",
                "#c:ingots/powered_tin"), false);

        // valuable_ingots — valuable tier ingots
        writeTag(cache, tagDir, futures, "valuable_ingots.json", list(
                "minecraft:netherite_ingot"), false);

        // Material sub-tags (mats/) — dynamically generated from Mat with both dust form and ingot.
        // Covers the 9 materials: Iron, Gold, Copper, Tin, Nickel, Powered Tin, Chlorium, Mushrium, Netherite.
        Path matsDir = tagDir.resolve("mats");
        for (Mat mat : Mat.values()) {
            if (mat.hasForm("dust") && mat.hasIngot) {
                writeTag(cache, matsDir, futures, mat.id + ".json",
                        list("#c:ingots/" + mat.id, "#c:dusts/" + mat.id), false);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Common (c:) item tags — generated from Mat registered-forms matrix
    // ═══════════════════════════════════════════════════════════════════

    private static String pluralize(String category) {
        return category + "s";
    }

    private void generateCTagItemTags(CachedOutput cache, List<CompletableFuture<?>> futures) {
        Path cTagDir = output.getOutputFolder().resolve("data/c/tags/item");

        for (String category : Mat.formNames()) {
            Map<String, Mat> formMats = new LinkedHashMap<>();
            for (Mat mat : Mat.values()) {
                if (mat.hasForm(category) && (mat.isRegistered(category) || mat.itemId(category).startsWith("minecraft:"))) {
                    formMats.put(mat.id, mat);
                }
            }

            if (formMats.isEmpty()) {
                continue;
            }

            String catPlural = pluralize(category);

            Path subDir = cTagDir.resolve(catPlural);
            var aggregateValues = new ArrayList<String>();

            for (Map.Entry<String, Mat> entry : formMats.entrySet()) {
                String matId = entry.getKey();
                Mat mat = entry.getValue();
                String itemId = mat.itemId(category);

                writeTag(cache, subDir, futures, matId + ".json",
                        list(itemId), false);

                aggregateValues.add("#c:" + catPlural + "/" + matId);
            }

            writeTag(cache, cTagDir, futures, catPlural + ".json",
                    aggregateValues, false);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Common (c:) block tags — ores and storage_blocks
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Generate {@code c:tags/block/ores/<mat>.json} and {@code c:tags/block/storage_blocks/<mat>.json}
     * sub-tags with aggregate tags, based on {@link Mat} metadata.
     * <p>
     * Only materials with {@code hasOre} or {@code hasBlock} produce tags here.
     * Vanilla copper ore/block is managed via {@code #minecraft:copper_ores} in main.
     */
    private void generateCBlockTags(CachedOutput cache, List<CompletableFuture<?>> futures) {
        Path cBlockDir = output.getOutputFolder().resolve("data/c/tags/block");

        // ── ores ───────────────────────────────────────────────────────
        Path oresDir = cBlockDir.resolve("ores");
        var oreAggregate = new ArrayList<String>();
        for (Mat mat : Mat.values()) {
            if (!mat.hasOre) continue;
            String blockId = mat.itemId("ore"); // kenergyengineering:tin_ore
            String deepId = mat.hasDeepOre ? mat.itemId("deep_ore") : null;
            var values = new ArrayList<String>();
            values.add(blockId);
            if (deepId != null) values.add(deepId);
            writeTag(cache, oresDir, futures, mat.id + ".json", values, false);
            oreAggregate.add("#c:ores/" + mat.id);
        }
        if (!oreAggregate.isEmpty()) {
            // Add copper ore reference (vanilla) for NeoForge convention
            oreAggregate.add("#c:ores/copper");
            writeTag(cache, cBlockDir, futures, "ores.json", oreAggregate, false);
        }

        // ── storage_blocks ─────────────────────────────────────────────
        Path storageDir = cBlockDir.resolve("storage_blocks");
        var storageAggregate = new ArrayList<String>();

        for (Mat mat : Mat.values()) {
            if (!mat.hasBlock) continue;
            String blockId = mat.itemId("block"); // kenergyengineering:tin_block
            writeTag(cache, storageDir, futures, mat.id + ".json", list(blockId), false);
            storageAggregate.add("#c:storage_blocks/" + mat.id);

            // Raw storage blocks (raw_tin_block, raw_nickel_block)
            if (mat.hasRaw) {
                String rawBlockId = mat.itemId("raw_block"); // kenergyengineering:raw_tin_block
                writeTag(cache, storageDir, futures, "raw_" + mat.id + ".json", list(rawBlockId), false);
                storageAggregate.add("#c:storage_blocks/raw_" + mat.id);
            }
        }

        // Vanilla raw storage blocks (iron, gold, copper) — block tags
        for (String[] entry : VANILLA_RAW_BLOCKS) {
            String matId = entry[0];
            String blockId = entry[1];
            writeTag(cache, storageDir, futures, "raw_" + matId + ".json", list(blockId), false);
            storageAggregate.add("#c:storage_blocks/raw_" + matId);
        }

        if (!storageAggregate.isEmpty()) {
            writeTag(cache, cBlockDir, futures, "storage_blocks.json", storageAggregate, false);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Common (c:) item tags — for ores and storage_blocks (block items)
    // ═══════════════════════════════════════════════════════════════════

    private void generateCBlockItemTags(CachedOutput cache, List<CompletableFuture<?>> futures) {
        Path cItemDir = output.getOutputFolder().resolve("data/c/tags/item");

        // ── ores (item) ────────────────────────────────────────────────
        Path oresItemDir = cItemDir.resolve("ores");
        var oreItemAggregate = new ArrayList<String>();
        for (Mat mat : Mat.values()) {
            if (!mat.hasOre) continue;
            // Item form for ore block: same ID as the block item
            String itemId = mat.itemId("ore"); // kenergyengineering:tin_ore
            writeTag(cache, oresItemDir, futures, mat.id + ".json", list(itemId), false);
            oreItemAggregate.add("#c:ores/" + mat.id);
        }
        if (!oreItemAggregate.isEmpty()) {
            // Add copper ore item reference (vanilla block item) for NeoForge convention
            writeTag(cache, oresItemDir, futures, "copper.json", list("minecraft:copper_ore"), false);
            oreItemAggregate.add("#c:ores/copper");
            writeTag(cache, cItemDir, futures, "ores.json", oreItemAggregate, false);
        }

        // ── storage_blocks (item) ──────────────────────────────────────
        Path storageItemDir = cItemDir.resolve("storage_blocks");
        var storageItemAggregate = new ArrayList<String>();
        for (Mat mat : Mat.values()) {
            if (!mat.hasBlock) continue;
            String itemId = mat.itemId("block");
            writeTag(cache, storageItemDir, futures, mat.id + ".json", list(itemId), false);
            storageItemAggregate.add("#c:storage_blocks/" + mat.id);

            if (mat.hasRaw) {
                String rawItemId = mat.itemId("raw_block");
                writeTag(cache, storageItemDir, futures, "raw_" + mat.id + ".json", list(rawItemId), false);
                storageItemAggregate.add("#c:storage_blocks/raw_" + mat.id);
            }
        }

        // Vanilla raw storage blocks (iron, gold, copper) — item tags
        for (String[] entry : VANILLA_RAW_BLOCKS) {
            String matId = entry[0];
            String blockId = entry[1];
            writeTag(cache, storageItemDir, futures, "raw_" + matId + ".json", list(blockId), false);
            storageItemAggregate.add("#c:storage_blocks/raw_" + matId);
        }

        if (!storageItemAggregate.isEmpty()) {
            writeTag(cache, cItemDir, futures, "storage_blocks.json", storageItemAggregate, false);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Common (c:) raw_materials item tags
    // ═══════════════════════════════════════════════════════════════════

    private void generateCRawMaterialsItemTags(CachedOutput cache, List<CompletableFuture<?>> futures) {
        Path cItemDir = output.getOutputFolder().resolve("data/c/tags/item");
        Path rawDir = cItemDir.resolve("raw_materials");
        var rawAggregate = new ArrayList<String>();

        for (Mat mat : Mat.values()) {
            if (!mat.hasRaw) continue;
            String itemId = mat.itemId("raw"); // kenergyengineering:raw_tin
            writeTag(cache, rawDir, futures, mat.id + ".json", list(itemId), false);
            rawAggregate.add("#c:raw_materials/" + mat.id);
        }
        if (!rawAggregate.isEmpty()) {
            // Add vanilla raw materials (iron, gold, copper) for NeoForge convention
            String[][] VANILLA_RAW_MATERIALS = {
                    { "iron", "minecraft:raw_iron" },
                    { "gold", "minecraft:raw_gold" },
                    { "copper", "minecraft:raw_copper" },
            };
            for (String[] entry : VANILLA_RAW_MATERIALS) {
                String matId = entry[0];
                String itemId = entry[1];
                writeTag(cache, rawDir, futures, matId + ".json", list(itemId), false);
                rawAggregate.add("#c:raw_materials/" + matId);
            }

            writeTag(cache, cItemDir, futures, "raw_materials.json", rawAggregate, false);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Fluid tags — kenergyengineering namespace (none currently)
    // ═══════════════════════════════════════════════════════════════════

    private void generateFluidTags(CachedOutput cache, List<CompletableFuture<?>> futures) {
        // No kenergyengineering:tags/fluid/ entries in baseline.
    }

    // ═══════════════════════════════════════════════════════════════════
    // Minecraft namespace tags — generated from TENBlocks collections
    // ═══════════════════════════════════════════════════════════════════

    private void generateMinecraftTags(CachedOutput cache, List<CompletableFuture<?>> futures) {
        var mcBlockDir = output.getOutputFolder().resolve("data/minecraft/tags/block");

        // ── block/needs_iron_tool ─────────────────────────────────────
        // Ores + storage blocks + raw storage blocks (not functional blocks)
        writeTag(cache, mcBlockDir, futures, "needs_iron_tool.json",
                TENBlocks.getAllNeedsIronTool().stream()
                        .map(TENTagProvider::blockId)
                        .toList(),
                false);

        // ── block/mineable/pickaxe ─────────────────────────────────────
        // Ores + storage + raw storage + all functional blocks
        writeTag(cache, mcBlockDir.resolve("mineable"), futures, "pickaxe.json",
                TENBlocks.getAllMineablePickaxe().stream()
                        .map(TENTagProvider::blockId)
                        .toList(),
                false);

        // ── fluid/water ────────────────────────────────────────────────
        writeTag(cache, output.getOutputFolder().resolve("data/minecraft/tags/fluid"),
                futures, "water.json", list(
                        "kenergyengineering:liquid_xp",
                        "kenergyengineering:liquid_xp_flowing",
                        "kenergyengineering:liquid_bizarrerie",
                        "kenergyengineering:liquid_bizarrerie_flowing"),
                false);
    }

    // ═══════════════════════════════════════════════════════════════════
    // Utilities
    // ═══════════════════════════════════════════════════════════════════

    @SafeVarargs
    private static <T> List<T> list(T... elements) {
        return new ArrayList<>(List.of(elements));
    }

    private void writeTag(CachedOutput cache, Path dir, List<CompletableFuture<?>> futures,
                          String fileName, List<String> values) {
        writeTag(cache, dir, futures, fileName, values, false);
    }

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
