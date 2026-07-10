#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Item Definition Validation Script — NeoForge 26.1.2 (v2)

Verifies that all registered kenergyengineering items have:
1. Item definition files at assets/kenergyengineering/items/<id>.json
2. Valid JSON structure: {"model": {"type": "minecraft:model", "model": "<loc>"}}
3. The model reference actually resolves to an existing model file in
   src/generated/resources/ or src/main/resources/.

In Minecraft 1.21.5 / NeoForge 26.1.2, the runtime model lookup requires
an item definition file at assets/<namespace>/items/<id>.json with format:
    {"model": {"type": "minecraft:model", "model": "<model_location>"}}

Without these files, items report "Missing item model" in the log even if
models/item/<id>.json exists.

v2 changes:
- Added model file resolution check (Round 2 fix: non-block/bucket items
  now reference kenergyengineering:item/<id> instead of texture sub-paths)

Usage:
    python scripts/validate_item_definitions.py
    python scripts/validate_item_definitions.py --items-dir <path>
"""

import json
import os
import sys


def _configure_stdio_utf8():
    """Ensure stdout/stderr use UTF-8 encoding for consistent output."""
    for stream in (sys.stdout, sys.stderr):
        if stream is not None and hasattr(stream, 'reconfigure'):
            try:
                stream.reconfigure(encoding='utf-8')
            except (ValueError, OSError):
                pass


# ── Project paths ──────────────────────────────────────────────────────

PROJECT_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
GENERATED_MODELS_DIR = os.path.join(PROJECT_DIR, "src", "generated", "resources", "assets", "kenergyengineering", "models")
MAIN_MODELS_DIR = os.path.join(PROJECT_DIR, "src", "main", "resources", "assets", "kenergyengineering", "models")


# ── Expected item names (mirrors TENModelProvider constants) ──────────────

CUBE_ALL_NAMES = [
    "tin_ore", "nickel_ore", "deep_tin_ore", "deep_nickel_ore",
    "tin_block", "nickel_block", "powered_tin_block", "chlorium_block",
    "raw_tin_block", "raw_nickel_block",
]

MACHINE_NAMES = [
    "machine_smelter", "machine_pulverizer", "machine_compressor", "machine_refiner",
    "machine_induction_furnace", "machine_psionicant", "machine_beacon_simulator",
    "machine_mob_ripper", "machine_quarry", "machine_enchantment_flusher",
    "machine_matter_condenser", "machine_farm_manager",
]

ENGINE_NAMES = [
    "engine_extraction", "engine_metal", "engine_biomass", "engine_solar",
]

CELL_NAMES = ["energy_cell", "creative_energy_cell"]

CABLE_NAMES = ["cable", "cable_quartz", "cable_azure", "cable_star"]

PIPE_NAMES = ["pipe", "pipe_white", "pipe_black"]

CHANNEL_NAMES = ["channel_energy", "channel_item", "channel_fluid"]

LIQUID_NAMES = [
    "liquid_xp", "liquid_bizarrerie", "liquid_honey",
    "liquid_royal_jelly", "liquid_spicy_jelly",
]

BLOCK_NAMES = set(CUBE_ALL_NAMES + MACHINE_NAMES + ENGINE_NAMES
                  + CABLE_NAMES + PIPE_NAMES + CELL_NAMES + CHANNEL_NAMES)

# Items registered in TENItems (from ZH_NAMES entries)
NON_BLOCK_ITEM_NAMES = [
    # Raw materials
    "raw_tin", "raw_nickel",
    # Crafting components
    "redstone_conductor", "redstone_converter", "redstone_storer",
    "indigo", "azure_glass", "bizarrerie", "redstone_ai",
    "redstone_ai_advanced", "hydraulic_widget", "detector",
    "royal_jelly", "spicy_jelly",
    # Moulds
    "mould_gear", "mould_plate", "mould_rod", "mould_string",
    "mould_compressed_small", "mould_compressed_large", "mould_split",
    "mould_coin", "mould_dense_plate",
    # Tools
    "spanner", "energy_capacity", "channel_connector",
    # Upgrades
    "augmented_levelup", "powered_levelup", "relic_levelup",
    "photosyn_levelup", "range_levelup", "smoke_levelup",
    "blast_levelup", "potion_levelup", "stream_levelup",
    "knowledge_levelup", "ice_levelup", "magma_levelup",
    "mineral_levelup",
    # Material variants (dusts)
    "iron_dust", "gold_dust", "copper_dust", "tin_dust", "nickel_dust",
    "powered_tin_dust", "chlorium_dust", "netherite_dust",
    "diamond_dust", "emerald_dust", "lapis_dust", "quartz_dust",
    "amethyst_dust", "mushrium_dust", "starlight_dust",
    # Material variants (ingots)
    "tin_ingot", "nickel_ingot", "powered_tin_ingot", "chlorium_ingot",
    "mushrium_ingot",
    # Material variants (nuggets)
    "tin_nugget", "nickel_nugget", "powered_tin_nugget", "chlorium_nugget",
    "copper_nugget", "netherite_nugget", "diamond_nugget", "emerald_nugget",
    "lapis_nugget", "quartz_nugget", "mushrium_nugget",
    # Material variants (plates)
    "iron_plate", "gold_plate", "copper_plate", "tin_plate", "nickel_plate",
    "powered_tin_plate", "chlorium_plate", "netherite_plate",
    "diamond_plate", "emerald_plate", "lapis_plate", "quartz_plate",
    "amethyst_plate", "redstone_plate", "mushrium_plate",
    # Material variants (gears)
    "iron_gear", "gold_gear", "copper_gear", "tin_gear", "nickel_gear",
    "powered_tin_gear", "chlorium_gear", "netherite_gear",
    "diamond_gear", "emerald_gear", "lapis_gear", "quartz_gear",
    "amethyst_gear", "redstone_gear", "mushrium_gear",
    # Material variants (rods)
    "iron_rod", "gold_rod", "copper_rod", "tin_rod", "nickel_rod",
    "powered_tin_rod", "chlorium_rod", "netherite_rod", "mushrium_rod",
    # Material variants (wires)
    "iron_wire", "gold_wire", "copper_wire", "tin_wire", "nickel_wire",
    "powered_tin_wire", "chlorium_wire", "netherite_wire", "mushrium_wire",
]

# Bucket items (from TENFluids)
BUCKET_NAMES = [f"{liquid}_bucket" for liquid in LIQUID_NAMES]

ALL_ITEM_NAMES = set(BLOCK_NAMES) | set(NON_BLOCK_ITEM_NAMES) | set(BUCKET_NAMES)


def resolve_model_path(model_location: str) -> str | None:
    """
    Convert a model location like 'kenergyengineering:block/tin_ore'
    to the actual filesystem path, returning the first existing match
    from generated or main resources, or None if not found.

    Model location format: <mod_id>:<category>/<name>
    Example: kenergyengineering:block/tin_ore → models/block/tin_ore.json
             kenergyengineering:item/iron_dust → models/item/iron_dust.json

    The models/ directory contains per-category subdirectories (block/, item/).
    """
    if ":" not in model_location:
        return None

    _, path = model_location.split(":", 1)
    # path is like "block/tin_ore" → models/block/tin_ore.json
    rel_path = f"{path}.json"

    for base_dir in (GENERATED_MODELS_DIR, MAIN_MODELS_DIR):
        candidate = os.path.join(base_dir, rel_path)
        if os.path.exists(candidate):
            return candidate

    return None


def validate_items_dir(items_dir: str) -> tuple[list[str], list[str], list[str]]:
    """
    Check that all expected item definition files exist, are valid JSON,
    and their model references resolve to actual model files.
    Returns (missing, invalid_format, unresolved_model).
    """
    missing = []
    invalid = []
    unresolved = []

    for name in sorted(ALL_ITEM_NAMES):
        file_path = os.path.join(items_dir, f"{name}.json")
        if not os.path.exists(file_path):
            missing.append(name)
            continue

        try:
            with open(file_path, "r", encoding="utf-8") as f:
                data = json.load(f)

            # Validate structure
            if "model" not in data:
                invalid.append(f"{name}: missing 'model' key")
                continue

            model_obj = data["model"]
            if not isinstance(model_obj, dict):
                invalid.append(f"{name}: 'model' is not an object")
                continue

            if model_obj.get("type") != "minecraft:model":
                invalid.append(f"{name}: model.type is not 'minecraft:model'")

            model_ref = model_obj.get("model")
            if not model_ref:
                invalid.append(f"{name}: model.model is missing or empty")
                continue

            # Resolve model reference to actual file
            resolved = resolve_model_path(model_ref)
            if resolved is None:
                unresolved.append(f"{name}: model='{model_ref}' → NOT FOUND")
            else:
                # Verify it's actually a JSON file (not just exists)
                if not resolved.endswith(".json"):
                    unresolved.append(f"{name}: model='{model_ref}' → not a JSON file")

        except json.JSONDecodeError as e:
            invalid.append(f"{name}: {e}")

    return missing, invalid, unresolved


def main():
    _configure_stdio_utf8()
    script_dir = os.path.dirname(os.path.abspath(__file__))
    project_dir = os.path.dirname(script_dir)

    items_dir = os.path.join(
        project_dir,
        "src", "generated", "resources",
        "assets", "kenergyengineering", "items",
    )

    # Allow override via --items-dir
    if "--items-dir" in sys.argv:
        idx = sys.argv.index("--items-dir")
        if idx + 1 < len(sys.argv):
            items_dir = sys.argv[idx + 1]

    print(f"Item definitions directory: {items_dir}")
    print(f"Expected items: {len(ALL_ITEM_NAMES)}")
    print(f"Model search paths:")
    print(f"  Generated: {GENERATED_MODELS_DIR}")
    print(f"  Main:      {MAIN_MODELS_DIR}")
    print()

    if not os.path.isdir(items_dir):
        print(f"[FAIL] Items directory does not exist!")
        print(f"   Expected {len(ALL_ITEM_NAMES)} items, 0 present.")
        print(f"   Status: RED - No item definitions found")
        sys.exit(1)

    existing_items = {
        f.replace(".json", "")
        for f in os.listdir(items_dir)
        if f.endswith(".json")
    }

    missing, invalid, unresolved = validate_items_dir(items_dir)

    # Stats
    present = len(existing_items)
    expected = len(ALL_ITEM_NAMES)
    total_issues = len(missing) + len(invalid) + len(unresolved)

    print(f"Present:       {present}")
    print(f"Missing:       {len(missing)}")
    print(f"Format issues: {len(invalid)}")
    print(f"Model refs:    {len(unresolved)} unresolved")
    print()

    if missing:
        print("[FAIL] Missing item definitions:")
        for name in sorted(missing):
            print(f"   - {name}")
        print()

    if invalid:
        print("[FAIL] Invalid item definitions:")
        for msg in invalid:
            print(f"   - {msg}")
        print()

    if unresolved:
        print("[FAIL] Unresolvable model references:")
        for msg in unresolved:
            print(f"   - {msg}")
        print()

    # Summary
    if total_issues == 0:
        print(f"[PASS] All {present}/{expected} item definitions present, valid, "
              f"and model references resolve. Status: GREEN")
        return 0
    elif total_issues == len(unresolved) and not missing and not invalid:
        print(f"[WARN] All items present but {len(unresolved)} model references "
              f"cannot be resolved.")
        print("   Status: YELLOW - Fix model references")
        return 2
    elif not missing and not unresolved:
        print(f"[WARN] All items present and resolvable, but {len(invalid)} "
              f"have format issues.")
        print("   Status: YELLOW - Needs format fixes")
        return 2
    else:
        print(f"[FAIL] {len(missing)} missing, {len(invalid)} invalid, "
              f"{len(unresolved)} unresolved ({present}/{expected} present).")
        print("   Status: RED - Items incomplete")
        return 1


if __name__ == "__main__":
    sys.exit(main())
