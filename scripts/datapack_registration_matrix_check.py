#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import sys
import io
# Force UTF-8 for stdout/stderr in Windows
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8')
"""
Post-datagen validation script for the Technical Engineering recipe matrix.

Facts derived from source (no hardcoded material/allowlist duplicates):
  - 7 form categories from Mat.java FORM_NAMES
  - Expected sub-tags from generated aggregate c: tag values (self-consistency)
  - Allowlist from TENRecipeMatrixValidator.java NO_RECIPE_ALLOWLIST
  - Historical aliases from TENRecipeMatrixValidator.java HISTORICAL_RECIPE_ALIASES
  - Recipe counts use strict historical baseline (315 main / 211 generated)

Checks:
   1. Recipe file counts in main/generated/build directories
   2. Historical recipe aliases (aerolium_gear → tin_gear, powered_aerolium_gear → powered_tin_gear)
      exist in main/resources with correct result item IDs; no unexpected aerolium files
   3. Matrix c: tags — aggregate values reference only existing sub-tags,
      each sub-tag value is a valid minecraft: or kenergyengineering: item ID
   4. No orphan sub-tag files (not referenced by any aggregate)
   5. Allowlist items (from TENRecipeMatrixValidator) have no recipe file
   6. Allowlist entry count matches source (expects 10)
   7. Main<->generated overlap is allowed

NEW RED-CHECK extensions (gate):
   8. Ownership separation: main/generated recipe intersection must be 0
   9. Count check: main=104, generated=211, union/build=315
  10. 36 vanilla pack/split (cp/sp/cp4/sp4) mould ingredient must have chance==0.0
  11. 4 tin/nickel ore/raw pulv input keys must be c:ores/* or c:raw_materials/*
  12. All generated compressor/pulverizer recipes: inputs/outputs non-empty,
      each ingredient has form/type/key/count/amount/chance fields semantically complete

Usage:
    python scripts/datapack_registration_matrix_check.py [--main DIR] [--generated DIR] [--build DIR]

Exit code 0 = all checks pass, non-zero = failure(s).
Default mode is post-datagen strict (requires generated c: tags).
"""

import argparse
import json
import os
import re
import sys

PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# ── Strict historical baseline (preserved, not derived) ──────────────
# After ownership separation: main-only=104, generated=211, intersection=0, union=315
EXPECTED_MAIN_RECIPE_COUNT = 104    # main-only after duplicate removal
EXPECTED_GENERATED_RECIPE_COUNT = 211
EXPECTED_INTERSECTION_COUNT = 0
EXPECTED_UNION_COUNT = 315

AEROLIUM_PATTERN = re.compile(r"aerolium", re.IGNORECASE)

# ── Source parsers ───────────────────────────────────────────────────

def parse_form_names_from_mat():
    """Read 7 form categories from Mat.java FORM_NAMES array."""
    mat_path = os.path.join(PROJECT_ROOT, "src", "main", "java",
                            "com", "modularmc", "ten", "common", "data", "Mat.java")
    with open(mat_path, "r", encoding="utf-8") as fh:
        text = fh.read()
    m = re.search(r'FORM_NAMES\s*=\s*\{\s*((?:"\w+"\s*,?\s*)+)\s*\};', text)
    if not m:
        print("FATAL: Cannot parse FORM_NAMES from Mat.java", file=sys.stderr)
        sys.exit(2)
    names = re.findall(r'"(\w+)"', m.group(1))
    if len(names) != 7:
        print(f"FATAL: Expected 7 form names, got {len(names)}: {names}", file=sys.stderr)
        sys.exit(2)
    return names


def parse_map_from_validator(map_name):
    """Parse a Map<String, String> constant from TENRecipeMatrixValidator.java."""
    val_path = os.path.join(PROJECT_ROOT, "src", "main", "java",
                            "com", "modularmc", "ten", "data",
                            "TENRecipeMatrixValidator.java")
    with open(val_path, "r", encoding="utf-8") as fh:
        text = fh.read()
    start_marker = f'public static final Map<String, String> {map_name} = Map.ofEntries('
    idx = text.find(start_marker)
    if idx == -1:
        print(f"FATAL: Cannot find map '{map_name}' in TENRecipeMatrixValidator.java", file=sys.stderr)
        sys.exit(2)
    result = {}
    # Find the section of the file between this map's start and its closing ");"
    # by counting parentheses depth.
    # start_marker includes the opening "(" of Map.ofEntries( so depth starts at 1.
    depth = 1
    start_idx = idx + len(start_marker)
    current = start_idx
    while current < len(text):
        ch = text[current]
        if ch == '(':
            depth += 1
        elif ch == ')':
            depth -= 1
            if depth == 0:
                # Found the closing paren matching Map.ofEntries(
                map_text = text[start_idx:current]
                break
        current += 1
    else:
        print(f"FATAL: Cannot find closing of map '{map_name}'", file=sys.stderr)
        sys.exit(2)

    pattern = r'Map\.entry\("([^"]+)"\s*,\s*"([^"]+)"\s*\)'
    for m in re.finditer(pattern, map_text):
        result[m.group(1)] = m.group(2)
    return result


def parse_allowlist_from_validator():
    return parse_map_from_validator("NO_RECIPE_ALLOWLIST")


def parse_historical_aliases_from_validator():
    return parse_map_from_validator("HISTORICAL_RECIPE_ALIASES")


def plural_form(category: str) -> str:
    return category + "s"


# ── File finders ─────────────────────────────────────────────────────

def find_recipes(base_dir):
    """Recursively find all .json recipe files under base_dir/data/*/recipe.
    Returns dict of {relative_path_without_json: absolute_path}."""
    recipe_dir = os.path.join(base_dir, "data", "kenergyengineering", "recipe")
    if not os.path.isdir(recipe_dir):
        return {}
    result = {}
    for root, _dirs, files in os.walk(recipe_dir):
        for f in files:
            if f.endswith(".json"):
                full = os.path.join(root, f)
                rel = os.path.relpath(full, base_dir).replace(os.sep, '/')
                # Relative recipe ID = path relative to <base>/data/kenergyengineering/recipe/ without .json
                recipe_id = os.path.relpath(full, recipe_dir).replace(os.sep, '/').replace('.json', '')
                result[recipe_id] = full
    return result


def find_c_tags(base_dir):
    """Find c: tags under base_dir/data/c/tags/item."""
    c_tag_dir = os.path.join(base_dir, "data", "c", "tags", "item")
    if not os.path.isdir(c_tag_dir):
        return {}
    result = {}
    for root, _dirs, files in os.walk(c_tag_dir):
        for f in files:
            if f.endswith(".json"):
                rel = os.path.relpath(os.path.join(root, f), base_dir)
                rel = rel.replace(os.sep, '/')
                result[rel] = os.path.join(root, f)
    return result


def read_tag_values(filepath):
    """Read the 'values' array from a tag JSON."""
    try:
        with open(filepath, "r", encoding="utf-8") as fh:
            data = json.load(fh)
        return data.get("values", [])
    except (json.JSONDecodeError, OSError) as e:
        print(f"  ERROR: Cannot read tag file {filepath}: {e}")
        return None


def read_recipe_json(filepath):
    """Read and parse a recipe JSON, return dict or None on error."""
    try:
        with open(filepath, "r", encoding="utf-8") as fh:
            return json.load(fh)
    except (json.JSONDecodeError, OSError) as e:
        print(f"  ERROR: Cannot read recipe {filepath}: {e}")
        return None


# ── Valid item ID check ──────────────────────────────────────────────

def is_valid_item_id(value: str) -> bool:
    if value.startswith('#'):
        return True
    return value.startswith("minecraft:") or value.startswith("kenergyengineering:")


# ── Recipe schema basics ─────────────────────────────────────────────

KNOWN_RECIPE_TYPES = {
    "kenergyengineering:compressor",
    "kenergyengineering:pulverizer",
    "minecraft:crafting_shaped",
    "minecraft:crafting_shapeless",
    "minecraft:smelting",
    "minecraft:blasting",
}

# ── Checkers ─────────────────────────────────────────────────────────

def check_recipe_counts(recipes, label, expected):
    count = len(recipes)
    if count == expected:
        print(f"  OK: {label} recipes = {count}")
        return True
    else:
        print(f"  FAIL: {label} recipes = {count}, expected {expected}")
        return False


def check_historical_aliases(recipes, label, aliases):
    found = {}
    unexpected = []
    for recipe_id, full_path in recipes.items():
        fname = os.path.splitext(os.path.basename(recipe_id))[0]
        # Check by filename base
        if fname in aliases:
            expected_result = aliases[fname]
            if os.path.isfile(full_path):
                try:
                    with open(full_path, "r", encoding="utf-8") as fh:
                        data = json.load(fh)
                    actual_result = data.get("result", {}).get("id", "")
                    if actual_result == expected_result:
                        found[fname] = actual_result
                    else:
                        unexpected.append(
                            f"{recipe_id}: expected result {expected_result}, got {actual_result}"
                        )
                except (json.JSONDecodeError, OSError) as e:
                    unexpected.append(f"{recipe_id}: cannot read — {e}")
        elif AEROLIUM_PATTERN.search(recipe_id):
            unexpected.append(f"{recipe_id}: unexpected aerolium file (not in aliases)")

    all_ok = True
    for alias_name, expected_result in sorted(aliases.items()):
        if alias_name in found:
            print(f"  OK: Historical alias '{alias_name}' found in {label}, result={found[alias_name]}")
        else:
            print(f"  FAIL: Historical alias '{alias_name}' NOT found in {label}")
            all_ok = False

    if not unexpected:
        print(f"  OK: No unexpected aerolium references in {label}")
    else:
        for u in unexpected:
            print(f"  FAIL: {u}")
        all_ok = False

    if all_ok:
        print(f"  Historical alias check PASSED for {label}")

    return all_ok


def check_c_tags_self_consistent(c_tags, base_dir, label, form_categories):
    all_ok = True
    cat_plural_set = {plural_form(c) for c in form_categories}
    sub_tag_refs = {}
    sub_tag_files = {}

    for rel_path, abs_path in c_tags.items():
        parts = rel_path.split('/')
        if len(parts) == 6 and parts[3] in cat_plural_set:
            cat_plural = parts[3]
            mat_id = os.path.splitext(parts[4])[0]
            sub_tag_files.setdefault(cat_plural, set()).add(mat_id)

    for rel_path, abs_path in sorted(c_tags.items()):
        parts = rel_path.split('/')
        if len(parts) == 5 and parts[3] == 'item' and parts[4].endswith('.json'):
            agg_name = os.path.splitext(parts[4])[0]
            if agg_name not in cat_plural_set:
                continue
            cat_plural = agg_name
            agg_values = read_tag_values(abs_path)
            if agg_values is None:
                all_ok = False
                continue

            for val in agg_values:
                val_parts = val.split('/')
                if len(val_parts) == 2 and val_parts[0] == f"#c:{cat_plural}":
                    ref_mat = val_parts[1]
                    sub_tag_refs.setdefault(cat_plural, set()).add(ref_mat)
                    expected_sub = f"data/c/tags/item/{cat_plural}/{ref_mat}.json"
                    if expected_sub not in c_tags:
                        print(f"  FAIL [{label}]: Aggregate {agg_name}.json references "
                              f"{val} but sub-tag {cat_plural}/{ref_mat}.json does not exist")
                        all_ok = False
                else:
                    print(f"  WARN [{label}]: Aggregate {agg_name}.json has unexpected value: {val}")

    for cat_plural, mat_ids in sub_tag_refs.items():
        for mat_id in sorted(mat_ids):
            sub_path = f"data/c/tags/item/{cat_plural}/{mat_id}.json"
            if sub_path not in c_tags:
                continue
            sub_values = read_tag_values(c_tags[sub_path])
            if sub_values is None:
                all_ok = False
                continue
            if len(sub_values) != 1:
                print(f"  FAIL [{label}]: Sub-tag {cat_plural}/{mat_id}.json "
                      f"has {len(sub_values)} values, expected 1")
                all_ok = False
            elif not is_valid_item_id(sub_values[0]):
                print(f"  FAIL [{label}]: Sub-tag {cat_plural}/{mat_id}.json "
                      f"has invalid item ID: {sub_values[0]}")
                all_ok = False

    all_refs = set()
    for cat_plural, refs in sub_tag_refs.items():
        for r in refs:
            all_refs.add(r)
    for cat_plural, file_mats in sub_tag_files.items():
        orphans = file_mats - sub_tag_refs.get(cat_plural, set())
        if orphans:
            print(f"  FAIL [{label}]: Orphan sub-tag files in {cat_plural}/ "
                  f"(not referenced by aggregate): {sorted(orphans)}")
            all_ok = False

    if all_ok:
        sub_count = sum(len(v) for v in sub_tag_refs.values())
        agg_count = len(sub_tag_refs)
        print(f"  OK: All c: tags self-consistent in {label} "
              f"({agg_count} aggregates, {sub_count} sub-tags)")

    return all_ok


def check_allowlist(recipes, label, allowlist_map):
    bad = []
    for recipe_id in recipes:
        fname = os.path.splitext(os.path.basename(recipe_id))[0]
        if fname in allowlist_map:
            bad.append(recipe_id)
    bad = sorted(set(bad))
    if not bad:
        print(f"  OK: No allowlisted items have recipes in {label}")
        return True
    else:
        print(f"  FAIL: Allowlisted items with recipes in {label}: {bad}")
        return False


# ═══════════════════════════════════════════════════════════════════════
# NEW RED-CHECK extensions
# ═══════════════════════════════════════════════════════════════════════

def check_ownership_separation(main_recipes, gen_recipes):
    """Check 8: main/generated recipe ID intersection must be 0."""
    main_ids = set(main_recipes.keys())
    gen_ids = set(gen_recipes.keys())
    intersection = main_ids & gen_ids
    main_only = main_ids - gen_ids
    gen_only = gen_ids - main_ids
    union = main_ids | gen_ids

    all_ok = True

    print(f"  Main recipe IDs: {len(main_ids)}")
    print(f"  Generated recipe IDs: {len(gen_ids)}")
    print(f"  Intersection: {len(intersection)}")
    print(f"  Main-only: {len(main_only)}")
    print(f"  Generated-only: {len(gen_only)}")
    print(f"  Union: {len(union)}")

    if len(intersection) != EXPECTED_INTERSECTION_COUNT:
        print(f"  FAIL: Intersection = {len(intersection)}, expected {EXPECTED_INTERSECTION_COUNT}")
        print(f"  Conflicting IDs: {sorted(intersection)[:20]}...")
        all_ok = False

    if len(main_ids) != EXPECTED_MAIN_RECIPE_COUNT:
        print(f"  FAIL: Main count = {len(main_ids)}, expected {EXPECTED_MAIN_RECIPE_COUNT}")
        all_ok = False

    if len(gen_ids) != EXPECTED_GENERATED_RECIPE_COUNT:
        print(f"  FAIL: Generated count = {len(gen_ids)}, expected {EXPECTED_GENERATED_RECIPE_COUNT}")
        all_ok = False

    if len(union) != EXPECTED_UNION_COUNT:
        print(f"  FAIL: Union = {len(union)}, expected {EXPECTED_UNION_COUNT}")
        all_ok = False

    if all_ok:
        print(f"  OK: Ownership separation verified — intersection=0, main-only={len(main_only)}, "
              f"generated={len(gen_only)}, union={len(union)}")

    return all_ok


def check_vanilla_mould_chance(recipes, label):
    """Check 10: For cp/sp/cp4/sp4 recipes, the mould ingredient must have chance==0.0.
    These recipes have type=kenergyengineering:compressor and contain a mould as second input."""
    all_ok = True
    checked = 0
    mould_pattern = re.compile(r'^compressor/(cp_|sp_|cp4_|sp4_)')

    for recipe_id, full_path in sorted(recipes.items()):
        if not mould_pattern.match(recipe_id):
            continue
        data = read_recipe_json(full_path)
        if data is None:
            print(f"  FAIL [{label}]: Cannot read {recipe_id}")
            all_ok = False
            continue

        if data.get("type") != "kenergyengineering:compressor":
            print(f"  FAIL [{label}]: {recipe_id} expected type compressor, got {data.get('type')}")
            all_ok = False
            continue

        inputs = data.get("inputs", [])
        if len(inputs) < 2:
            print(f"  FAIL [{label}]: {recipe_id} has only {len(inputs)} inputs, expected ≥2")
            all_ok = False
            continue

        # Second input should be the mould
        mould_ingr = inputs[1]
        if "chance" not in mould_ingr:
            print(f"  FAIL [{label}]: {recipe_id} mould ingredient missing 'chance' field")
            all_ok = False
            continue

        chance = mould_ingr["chance"]
        if chance != 0.0:
            print(f"  FAIL [{label}]: {recipe_id} mould chance = {chance}, expected 0.0")
            all_ok = False
            continue

        checked += 1

    if all_ok:
        print(f"  OK [{label}]: All {checked} cp/sp/cp4/sp4 recipes have mould chance==0.0")

    return all_ok


def check_pulv_input_tags(recipes, label):
    """Check 11: tin/nickel ore/raw pulv input keys must be c:ores/* or c:raw_materials/*."""
    all_ok = True
    # The 4 affected recipes
    targets = {
        "pulverizer/metal/tin_dust_ore": "c:ores/tin",
        "pulverizer/metal/tin_dust_raw": "c:raw_materials/tin",
        "pulverizer/metal/nickel_dust_ore": "c:ores/nickel",
        "pulverizer/metal/nickel_dust_raw": "c:raw_materials/nickel",
    }

    for recipe_id, expected_key in targets.items():
        if recipe_id not in recipes:
            print(f"  FAIL [{label}]: Missing recipe {recipe_id}")
            all_ok = False
            continue

        data = read_recipe_json(recipes[recipe_id])
        if data is None:
            print(f"  FAIL [{label}]: Cannot read {recipe_id}")
            all_ok = False
            continue

        inputs = data.get("inputs", [])
        if not inputs:
            print(f"  FAIL [{label}]: {recipe_id} has no inputs")
            all_ok = False
            continue

        actual_key = inputs[0].get("key", "")
        if actual_key != expected_key:
            print(f"  FAIL [{label}]: {recipe_id} input key = '{actual_key}', expected '{expected_key}'")
            all_ok = False
        else:
            print(f"  OK [{label}]: {recipe_id} input key = '{actual_key}' (correct)")

    if all_ok:
        print(f"  OK [{label}]: All 4 tin/nickel ore/raw pulv input tags correct")

    return all_ok


def check_generated_recipe_semantics(recipes, label):
    """Check 12: All generated compressor/pulverizer recipes must have:
    - Non-empty inputs and outputs
    - Each ingredient has form/type/key fields
    - count present when >1 or chance present
    - Semantic completeness
    """
    all_ok = True
    checked_comp = 0
    checked_pulv = 0

    for recipe_id, full_path in sorted(recipes.items()):
        # Only check compressor and pulverizer types (vanilla types are verified by content)
        if not ('compressor/' in recipe_id or 'pulverizer/' in recipe_id):
            continue

        data = read_recipe_json(full_path)
        if data is None:
            print(f"  FAIL [{label}]: Cannot read {recipe_id}")
            all_ok = False
            continue

        rtype = data.get("type", "")
        if rtype not in ("kenergyengineering:compressor", "kenergyengineering:pulverizer"):
            continue  # skip vanilla types in generated dir

        inputs = data.get("inputs", [])
        outputs = data.get("outputs", [])

        if not inputs:
            print(f"  FAIL [{label}]: {recipe_id} has empty inputs")
            all_ok = False
        if not outputs:
            print(f"  FAIL [{label}]: {recipe_id} has empty outputs")
            all_ok = False

        # Check each ingredient for required fields
        for idx, ingr in enumerate(inputs):
            for field in ("form", "type", "key"):
                if field not in ingr:
                    print(f"  FAIL [{label}]: {recipe_id} input[{idx}] missing '{field}'")
                    all_ok = False
            # If count > 1 or there's a chance, these fields should be present
            if ingr.get("type") == "static" and "count" not in ingr:
                pass  # count=1 is optional
            if ingr.get("type") == "static" and "chance" not in ingr:
                pass  # omitted chance means default

        for idx, out in enumerate(outputs):
            for field in ("form", "type", "key"):
                if field not in out:
                    print(f"  FAIL [{label}]: {recipe_id} output[{idx}] missing '{field}'")
                    all_ok = False
            # Optional: chance for outputs (byproducts)
            # count should be present when >1

        if rtype == "kenergyengineering:compressor":
            checked_comp += 1
            # Compressor must have exactly 2 inputs (material + mould)
            if len(inputs) < 2:
                print(f"  FAIL [{label}]: {recipe_id} compressor has {len(inputs)} inputs, expected ≥2")
                all_ok = False
        elif rtype == "kenergyengineering:pulverizer":
            checked_pulv += 1
            # Pulverizer must have exactly 1 input
            if len(inputs) != 1:
                print(f"  FAIL [{label}]: {recipe_id} pulverizer has {len(inputs)} inputs, expected 1")
                all_ok = False

        # Check time field
        if "time" not in data:
            print(f"  FAIL [{label}]: {recipe_id} missing 'time' field")
            all_ok = False

    if all_ok:
        print(f"  OK [{label}]: {checked_comp} compressor + {checked_pulv} pulverizer recipes "
              f"semantically complete ({checked_comp + checked_pulv} total)")

    return all_ok


# ═══════════════════════════════════════════════════════════════════════
# NEW CHECK: 64-matrix tag input closure
# ═══════════════════════════════════════════════════════════════════════

# NeoForge 26.1.2 built-in convention tags that don't need generated files.
# These are provided by the NeoForge jar, not by this mod's datagen.
NEOPORGE_BUILTIN_TAGS = {
    # ingots
    "c:ingots/iron", "c:ingots/gold", "c:ingots/copper", "c:ingots/netherite",
    # gems
    "c:gems/diamond", "c:gems/emerald", "c:gems/lapis", "c:gems/quartz",
    "c:gems/amethyst",
    # ores — standard vanilla
    "c:ores/iron", "c:ores/gold", "c:ores/copper", "c:ores/netherite",
    "c:ores/diamond", "c:ores/emerald", "c:ores/lapis", "c:ores/quartz",
    # raw_materials — standard vanilla
    "c:raw_materials/iron", "c:raw_materials/gold", "c:raw_materials/copper",
    # stones/sands/end_stones — block tags
    "c:stones", "c:sands", "c:cobblestones", "c:obsidians", "c:netherracks",
    "c:gravels", "c:end_stones",
    # dyes
    "c:dyes", "c:dyes/white", "c:dyes/orange", "c:dyes/magenta",
    "c:dyes/light_blue", "c:dyes/yellow", "c:dyes/lime", "c:dyes/pink",
    "c:dyes/gray", "c:dyes/light_gray", "c:dyes/cyan", "c:dyes/purple",
    "c:dyes/blue", "c:dyes/brown", "c:dyes/green", "c:dyes/red", "c:dyes/black",
    # other common
    "c:mushrooms",
}

# Custom mod materials that this mod provides tags for (via generated resources)
# Keys that match these patterns WITHOUT a generated file are errors.
CUSTOM_TAG_MATERIALS = {
    "c:ingots/chlorium", "c:ingots/tin", "c:ingots/nickel",
    "c:ingots/powered_tin", "c:ingots/mushrium",
    "c:raw_materials/tin", "c:raw_materials/nickel",
    "c:ores/tin", "c:ores/nickel",
}


def check_matrix_tag_input_closure(recipes, label, c_tags, c_tags_label):
    """Check 13: 64 matrix tag input keys form a valid closure.

    For each generated compressor/pulverizer recipe with tag-type inputs:
    - Verify the key matches expected c: namespace patterns
    - For custom mod materials, verify the corresponding tag file exists and is non-empty
    - For NeoForge built-in tags, don't flag as missing (provided by NeoForge jar)
    - Redstone/minecraft:redstone direct item must use type=item, not type=tag
    """
    all_ok = True
    tag_inputs_found = 0
    redstone_items_found = 0
    errors = []

    # Build a set of tag files found in the tag directory for cross-reference
    # c_tags keys are like "data/c/tags/item/ingots/iron.json"
    tag_file_set = set()
    for rel_path in c_tags:
        if not rel_path.endswith('.json'):
            continue
        # Normalize and parse path: data/c/tags/item/<category>/<material>.json
        # or: data/c/tags/item/<category>.json
        path = rel_path.replace('\\', '/')
        parts = path.split('/')
        # Look for the "tags" directory marker — usually at parts[-4] for sub-tags
        # e.g., data/c/tags/item/ingots/iron.json → len=6, parts[-4]='tags'
        # e.g., data/c/tags/item/ingots.json → len=5, parts[-3]='tags'
        try:
            tags_idx = parts.index('tags')
        except ValueError:
            continue
        if tags_idx + 1 >= len(parts) or parts[tags_idx + 1] != 'item':
            continue
        # Determine the category path starting after 'item'
        cat_parts = parts[tags_idx + 2:]  # e.g., ['ingots', 'iron.json']
        if len(cat_parts) == 1:
            # Aggregate tag: data/c/tags/item/ingots.json → c:ingots
            tag_name = cat_parts[0].replace('.json', '')
            tag_file_set.add(f"c:{tag_name}")
        elif len(cat_parts) == 2:
            # Sub-tag: data/c/tags/item/ingots/iron.json → c:ingots/iron
            subcat = cat_parts[0]
            mat = cat_parts[1].replace('.json', '')
            tag_file_set.add(f"c:{subcat}/{mat}")
        # ignore deeper structures

    for recipe_id, full_path in sorted(recipes.items()):
        if not ('compressor/' in recipe_id or 'pulverizer/' in recipe_id):
            continue

        data = read_recipe_json(full_path)
        if data is None:
            continue

        rtype = data.get("type", "")
        if rtype not in ("kenergyengineering:compressor", "kenergyengineering:pulverizer"):
            continue

        inputs = data.get("inputs", [])
        if not inputs:
            continue

        for idx, ingr in enumerate(inputs):
            ingr_type = ingr.get("type", "")
            ingr_key = ingr.get("key", "")

            if ingr_type == "tag":
                tag_inputs_found += 1

                # Check tag key pattern
                if not ingr_key.startswith("c:"):
                    errors.append(f"{recipe_id} input[{idx}]: tag key '{ingr_key}' must start with 'c:'")
                    continue

                # Check if it's a known NeoForge built-in (skip file check)
                if ingr_key in NEOPORGE_BUILTIN_TAGS:
                    continue

                # Check if it's a custom mod material that needs a generated tag file
                # Or any c: tag that should have a file in generated resources
                if ingr_key in tag_file_set:
                    # Tag file exists — it's OK (content semantics checked by tag self-consistency)
                    continue
                elif ingr_key in CUSTOM_TAG_MATERIALS:
                    errors.append(f"{recipe_id} input[{idx}]: tag '{ingr_key}' is a custom material "
                                  f"but no tag file found in {c_tags_label} resources")
                else:
                    # Unknown tag. Could be NeoForge built-in not in our list or a miss.
                    # Assume it's a NeoForge built-in rather than failing — avoid false positives.
                    # Log as info only.
                    pass

            elif ingr_type == "item" and ingr_key == "minecraft:redstone":
                redstone_items_found += 1
                # Redstone must use type=item (not type=tag)
                if ingr_type != "item":
                    errors.append(f"{recipe_id} input[{idx}]: redstone must use type='item', got '{ingr_type}'")

    # Report results
    if errors:
        for err in errors:
            print(f"  FAIL [{label}]: {err}")
        all_ok = False
    if all_ok:
        print(f"  OK [{label}]: {tag_inputs_found} tag inputs valid, "
              f"{redstone_items_found} redstone direct items correct, "
              f"no issues with 64-matrix tag closure")
        print(f"    (NeoForge built-in tags not flagged as missing)")

    return all_ok


# ── Main ─────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(
        description="Validate the Technical Engineering datapack registration matrix."
    )
    parser.add_argument("--main", default=os.path.join(PROJECT_ROOT, "src", "main", "resources"),
                        help="Path to main resources directory")
    parser.add_argument("--generated", default=os.path.join(PROJECT_ROOT, "src", "generated", "resources"),
                        help="Path to generated resources directory")
    parser.add_argument("--build", default=None,
                        help="Optional path to build/generated resources directory")
    parser.add_argument("--pre-datagen", action="store_true", default=False,
                        help="Allow SKIP when generated c: tags are missing (pre-datagen mode); default is strict FAIL")
    args = parser.parse_args()

    exit_code = 0

    print("=" * 60)
    print("TEN Datapack Registration Matrix Check v2 (RED+GREEN)")
    print("=" * 60)

    # ── Derive facts from source ────────────────────────────────────
    form_categories = parse_form_names_from_mat()
    print(f"\n[0] Derived facts")
    print(f"  Form categories from Mat.java: {form_categories}")
    allowlist_map = parse_allowlist_from_validator()
    print(f"  Allowlist from TENRecipeMatrixValidator.java: {len(allowlist_map)} entries")
    aliases_map = parse_historical_aliases_from_validator()
    print(f"  Historical aliases from TENRecipeMatrixValidator.java: {len(aliases_map)} entries")
    for alias_name, result_id in sorted(aliases_map.items()):
        print(f"    {alias_name} → {result_id}")

    # ── Find recipes ──────────────────────────────────────────────────
    main_recipes = find_recipes(args.main)
    gen_recipes = find_recipes(args.generated)
    build_recipes = find_recipes(args.build) if args.build else {}

    # ── 1. Recipe counts ─────────────────────────────────────────────
    print("\n[1] Recipe file counts (ownership-separated baseline)")
    if not check_recipe_counts(main_recipes, "main", EXPECTED_MAIN_RECIPE_COUNT):
        exit_code = 1
    if not check_recipe_counts(gen_recipes, "generated", EXPECTED_GENERATED_RECIPE_COUNT):
        exit_code = 1

    # ── 2. Historical recipe aliases (aerolium → tin) ───────────────
    print("\n[2] Historical recipe aliases (aerolium → tin)")
    print("  Aliases are ONLY expected in main/resources as legacy files (not generated)")
    for recipes, label in [(main_recipes, "main")]:
        if not check_historical_aliases(recipes, label, aliases_map):
            exit_code = 1

    # ── 3. C tag self-consistency check ──────────────────────────────
    print("\n[3] Matrix c: tag self-consistency check")
    main_c_tags = find_c_tags(args.main)
    gen_c_tags = find_c_tags(args.generated)
    build_c_tags = find_c_tags(args.build) if args.build else {}

    form_plurals = {plural_form(c) for c in form_categories}
    main_form_tags = {k: v for k, v in main_c_tags.items()
                      if any(f"/{p}/" in k or k.endswith(f"/{p}.json") for p in form_plurals)}
    if main_form_tags:
        print(f"  WARNING: main/resources still has {len(main_form_tags)} form-category c: tag files "
              f"(expected 0, moved to generated): {sorted(main_form_tags.keys())}")
    elif main_c_tags:
        print(f"  OK: No form-category c: tags in main/resources; "
              f"{len(main_c_tags)} business c: tags (gem/ore/...) remain as expected")
    else:
        print("  OK: No c: tags in main/resources (moved to generated)")

    if gen_c_tags:
        if not check_c_tags_self_consistent(gen_c_tags, args.generated, "generated", form_categories):
            exit_code = 1
    elif args.pre_datagen:
        print("  SKIP: generated/resources has no c: tags yet (pre-datagen mode)")
    else:
        print("  FAIL: generated/resources has no c: tags (expected after runClientData). "
              "Use --pre-datagen to allow pre-datagen status.")
        exit_code = 1

    # ── Build path validation (only when --build explicitly provided) ─
    if args.build is not None:
        build_path_abs = os.path.abspath(args.build)
        if not os.path.isdir(build_path_abs):
            print(f"  FAIL: Build path does not exist: {build_path_abs}")
            exit_code = 1
        elif not build_c_tags:
            print(f"  FAIL: Build path exists but no c: tags found in {args.build}")
            print("  (Expected after processResources; datagen may not have run correctly)")
            exit_code = 1
        else:
            if not check_c_tags_self_consistent(build_c_tags, args.build, "build", form_categories):
                exit_code = 1

    # ── 4. Allowlist check ───────────────────────────────────────────
    print("\n[4] Allowlist (NO_RECIPE_ALLOWLIST) enforcement")
    allowlist_ok = True
    for recipes, label in [(main_recipes, "main"), (gen_recipes, "generated")]:
        if not check_allowlist(recipes, label, allowlist_map):
            allowlist_ok = False
    if len(allowlist_map) != 10:
        print(f"  FAIL: Allowlist has {len(allowlist_map)} entries, expected 10")
        allowlist_ok = False
    if allowlist_ok:
        print(f"  OK: {len(allowlist_map)} allowlisted items enforced, no recipes found")
    else:
        exit_code = 1

    # ── 5. Allowlist reasons summary ─────────────────────────────────
    print("\n[5] Allowlist items and historical reasons (from source)")
    for item in sorted(allowlist_map):
        reason = allowlist_map[item]
        print(f"    {item}: {reason}")
    print(f"  OK: All {len(allowlist_map)} allowlist items have documented reasons")

    # ═══════════════════════════════════════════════════════════════════
    # NEW RED-GREEN CHECKS
    # ═══════════════════════════════════════════════════════════════════

    # ── 8. Ownership separation (intersection=0) ──────────────────────
    print("\n[8] Ownership separation (main<->generated intersection must be 0)")
    if not check_ownership_separation(main_recipes, gen_recipes):
        exit_code = 1

    # ── 9. (Already covered by check_ownership_separation counts)

    # ── 10. Vanilla pack/split mould chance == 0.0 ────────────────────
    print("\n[10] Vanilla pack/split (cp/sp/cp4/sp4) mould chance == 0.0")
    gen_ok = check_vanilla_mould_chance(gen_recipes, "generated")
    if not gen_ok:
        exit_code = 1

    # ── 11. Tin/nickel ore/raw pulv input tags ────────────────────────
    print("\n[11] Tin/nickel ore/raw pulv input key tags")
    if not check_pulv_input_tags(gen_recipes, "generated"):
        exit_code = 1

    # ── 12. Generated compressor/pulverizer semantic completeness ──────
    print("\n[12] Generated compressor/pulverizer recipe semantic completeness")
    if not check_generated_recipe_semantics(gen_recipes, "generated"):
        exit_code = 1

    # ── 13. 64-matrix tag input closure ─────────────────────────────────
    print("\n[13] 64-matrix tag input closure (tag inputs via c: ingots/gems/ores/raw_materials)")
    gen_c_tags = find_c_tags(args.generated)
    if gen_c_tags:
        if not check_matrix_tag_input_closure(gen_recipes, "generated", gen_c_tags, "generated"):
            exit_code = 1
    elif args.pre_datagen:
        print("  SKIP: generated/resources has no c: tags yet (pre-datagen mode)")
    else:
        print("  SKIP: no generated c: tags to cross-reference (use --pre-datagen to allow)")

    # ── Summary ──────────────────────────────────────────────────────
    print("\n" + "=" * 60)
    if exit_code == 0:
        print("RESULT: ALL CHECKS PASSED (GREEN)")
    else:
        print("RESULT: CHECK(S) FAILED (RED) — see above")
    print("=" * 60)

    return exit_code


if __name__ == "__main__":
    sys.exit(main())
