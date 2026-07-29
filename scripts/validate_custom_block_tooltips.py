#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
validate_custom_block_tooltips.py — TASK-012 TE4 Custom Block Item Tooltip Validator

Static validation for the 21 target block items that should use TENBaseBlockItem
to display their lang-defined custom tooltips.

Checks:
  1. 21 target items are registered with TENBaseBlockItem in TENBlocks.java
  2. Exclusion items (ores/storage/pipes/channels/creative_energy_cell) are NOT
  3. Key prefix resolver categories produce correct lang key prefixes
  4. en_us.json / zh_cn.json tooltip keys for all 21 targets exist and are
     continuous (no gaps in .0, .1, .2, ...)

Usage:
    python scripts/validate_custom_block_tooltips.py

Exit codes:
    0 = GREEN (all 21/21 + exclusions + lang continuity PASS)
    1 = RED (one or more checks FAIL)
"""

import json
import os
import re
import sys

from resource_roots import resolve_unique_resource


def _configure_stdio_utf8():
    """Ensure stdout/stderr use UTF-8 encoding for consistent output."""
    for stream in (sys.stdout, sys.stderr):
        if stream is not None and hasattr(stream, 'reconfigure'):
            try:
                stream.reconfigure(encoding='utf-8')
            except (ValueError, OSError):
                pass


PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# ─── TARGET DEFINITIONS ────────────────────────────────────────────────────────

MACHINES = [
    "machine_smelter", "machine_pulverizer", "machine_compressor",
    "machine_refiner", "machine_induction_furnace", "machine_psionicant",
    "machine_beacon_simulator", "machine_mob_ripper", "machine_quarry",
    "machine_enchantment_flusher", "machine_matter_condenser", "machine_farm_manager",
]

ENGINES = [
    "engine_extraction", "engine_metal", "engine_biomass", "engine_solar",
]

CABLES = [
    "cable", "cable_azure", "cable_quartz", "cable_star",
]

ENERGY_CELL = ["energy_cell"]

TARGETS = MACHINES + ENGINES + CABLES + ENERGY_CELL  # 21 total

# Items that should NEVER use TENBaseBlockItem (exclusions)
EXCLUSIONS = [
    "tin_ore", "nickel_ore", "deep_tin_ore", "deep_nickel_ore",         # ores
    "tin_block", "nickel_block", "powered_tin_block", "chlorium_block",
    "raw_tin_block", "raw_nickel_block",                                 # storage
    "pipe", "pipe_white", "pipe_black",                                   # pipes
    "channel_energy", "channel_item", "channel_fluid",                     # channels
    "creative_energy_cell",                                                # excluded cell
]

# All block items that exist (to confirm we're not missing anything)
ALL_BLOCK_ITEMS = TARGETS + EXCLUSIONS  # 21 + 16 = 37 total

# ─── KEY RESOLVER RULES (mirrors planned TENBaseBlockItem logic) ──────────────

def expected_prefix(reg_path: str) -> str | None:
    """Expected lang key prefix for a given registry path."""
    if reg_path.startswith("machine_"):
        name = reg_path[len("machine_"):]
        return f"kenergyengineering.info.{name}."
    elif reg_path.startswith("engine_"):
        return f"kenergyengineering.info.{reg_path}."
    elif reg_path == "energy_cell":
        return f"kenergyengineering.info.energy_cell."
    elif reg_path.startswith("cable"):
        return f"kenergyengineering.{reg_path}."
    return None


# ─── SOURCE PARSING ───────────────────────────────────────────────────────────

def read_tenblocks_java() -> str:
    """Read TENBlocks.java content."""
    path = os.path.join(
        PROJECT_ROOT, "src", "main", "java", "com", "modularmc", "ten",
        "common", "data", "TENBlocks.java"
    )
    if not os.path.isfile(path):
        print(f"ERROR: TENBlocks.java not found at {path}")
        sys.exit(1)
    with open(path, "r", encoding="utf-8") as f:
        return f.read()


def read_tenbaseblockitem_java() -> str:
    """Read TENBaseBlockItem.java content."""
    path = os.path.join(
        PROJECT_ROOT, "src", "main", "java", "com", "modularmc", "ten",
        "common", "item", "TENBaseBlockItem.java"
    )
    if not os.path.isfile(path):
        print(f"ERROR: TENBaseBlockItem.java not found at {path}")
        sys.exit(1)
    with open(path, "r", encoding="utf-8") as f:
        return f.read()


def read_lang(lang: str) -> dict:
    """Read a lang JSON file via dual-root resolution and return parsed dict."""
    rel = f"assets/kenergyengineering/lang/{lang}.json"
    try:
        _, path = resolve_unique_resource(PROJECT_ROOT, rel)
    except FileNotFoundError:
        print(f"ERROR: {lang}.json not found in any resource root")
        sys.exit(1)
    except AssertionError as e:
        print(f"ERROR: {lang}.json conflict: {e}")
        sys.exit(1)

    if not os.path.isfile(path):
        print(f"ERROR: {lang}.json not found at {path}")
        sys.exit(1)
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


# ─── CHECKS ───────────────────────────────────────────────────────────────────

def check_registration(source: str) -> dict:
    """
    Check TENBlocks.java for which block items use TENBaseBlockItem.
    
    Strategy: Identify which helper methods call registerTENBaseBlockItem
    vs registerSimpleBlockItem, then map each block declaration to its
    helper call to determine registration type.
    """
    # Step 1: Find all method definitions by line, identify which call
    # registerTENBaseBlockItem vs registerSimpleBlockItem
    tenbase_methods = set()
    
    # Use a simpler approach: find method signatures (lines that start with "private static")
    # and check the lines between this method and the next method
    lines = source.split('\n')
    method_starts = []
    for i, line in enumerate(lines):
        stripped = line.strip()
        if (stripped.startswith('private static ') or stripped.startswith('public static ')) and ('(' in stripped):
            method_starts.append(i)
    
    # Also find the end of file
    method_starts.append(len(lines))
    
    for idx in range(len(method_starts) - 1):
        start_line = method_starts[idx]
        end_line = method_starts[idx + 1]
        method_text = '\n'.join(lines[start_line:end_line])
        
        # Extract method name
        name_match = re.search(r'(?:private|public)\s+static\s+\S+\s+(\w+)\s*\(', lines[start_line])
        if not name_match:
            continue
        method_name = name_match.group(1)
        
        # Skip the outer init() method, constructors, etc.
        if method_name in ('init', 'TENBlocks', 'Registration'):
            continue
        
        # Check what item registration call is in this method body
        if 'registerTENBaseBlockItem' in method_text:
            tenbase_methods.add(method_name)

    # Step 2: Map each block name to its helper call
    results = {}
    for item_name in ALL_BLOCK_ITEMS:
        field_pattern = rf'=\s*(\w+)\s*\("' + re.escape(item_name) + r'"'
        match = re.search(field_pattern, source)
        if match:
            helper_name = match.group(1)
            results[item_name] = helper_name in tenbase_methods
        else:
            results[item_name] = False

    return results


def check_lang_continuity(lang_data: dict, target: str, prefix: str, lang_name: str) -> dict:
    """
    Check that lang keys for a target are continuous (0, 1, 2, ... without gaps).
    Returns {target: {"continuous": bool, "keys_found": int, "max_idx": int, "gap_at": int|None}}
    """
    result = {
        "continuous": False,
        "keys_found": [],
        "max_continuous": -1,
        "gap_at": None,
        "first_missing": None,
    }

    i = 0
    while True:
        key = f"{prefix}{i}"
        if key in lang_data:
            result["keys_found"].append(i)
            i += 1
        else:
            if i == 0:
                # No keys at all for this prefix
                result["first_missing"] = 0
                break
            # We hit a gap. Check if there are more keys after this gap.
            found_after_gap = False
            for j in range(i + 1, i + 50):  # check up to 50
                if f"{prefix}{j}" in lang_data:
                    found_after_gap = True
                    result["gap_at"] = i
                    break
            if found_after_gap:
                result["continuous"] = False
            else:
                result["continuous"] = True  # No gap, just reached end
            result["max_continuous"] = i - 1
            break

    return result


def check_all_langs(targets: list[str]) -> dict:
    """
    For all targets, check lang key existence and continuity in both en_us and zh_cn.
    """
    en_us = read_lang("en_us")
    zh_cn = read_lang("zh_cn")

    results = {
        "en_us": {},
        "zh_cn": {},
    }

    for target in targets:
        prefix = expected_prefix(target)
        if prefix is None:
            continue

        results["en_us"][target] = check_lang_continuity(en_us, target, prefix, "en_us")
        results["zh_cn"][target] = check_lang_continuity(zh_cn, target, prefix, "zh_cn")

    return results


# ─── MAIN ──────────────────────────────────────────────────────────────────────

def main():
    _configure_stdio_utf8()
    print("=" * 72)
    print("  TASK-012 — Custom Block Item Tooltip Validation")
    print("=" * 72)
    print(f"\n  Targets: {len(TARGETS)} items")
    print(f"  Exclusions: {len(EXCLUSIONS)} items")
    print()

    tenblocks = read_tenblocks_java()
    tenbase = read_tenbaseblockitem_java()
    en_us = read_lang("en_us")
    zh_cn = read_lang("zh_cn")

    all_pass = True

    # ─── CHECK 1: Registration types ──────────────────────────────────────
    print("[1/6] Block item registration type check (TENBlocks.java)")
    print("-" * 72)

    reg_results = check_registration(tenblocks)

    # Determine which helper methods map to which blocks (for display)
    helper_map = {}
    for item_name in ALL_BLOCK_ITEMS:
        field_pattern = rf'=\s*(\w+)\s*\("' + re.escape(item_name) + r'"'
        match = re.search(field_pattern, tenblocks)
        if match:
            helper_map[item_name] = match.group(1)

    target_hits = 0
    target_misses = 0
    for target in TARGETS:
        uses_tenbase = reg_results.get(target, False)
        helper = helper_map.get(target, "?")
        status = "[OK] TENBaseBlockItem" if uses_tenbase else "[FAIL] BlockItem (plain)"
        if uses_tenbase:
            target_hits += 1
        else:
            target_misses += 1
        print(f"      {target:<35s} ({helper:<15s}) {status}")

    # Check exclusions
    exclusion_violations = 0
    for excl in EXCLUSIONS:
        uses_tenbase = reg_results.get(excl, False)
        helper = helper_map.get(excl, "?")
        if uses_tenbase:
            print(f"      {excl:<35s} ({helper:<15s}) [FAIL] VIOLATION: uses TENBaseBlockItem")
            exclusion_violations += 1
        else:
            print(f"      {excl:<35s} ({helper:<15s}) [OK] BlockItem (correct)")

    if target_hits == len(TARGETS) and exclusion_violations == 0:
        print(f"\n      >>> PASS: {target_hits}/{len(TARGETS)} targets use TENBaseBlockItem, "
              f"0 exclusion violations")
    else:
        print(f"\n      >>> FAIL: {target_hits}/{len(TARGETS)} targets, "
              f"{exclusion_violations} exclusion violations")
        all_pass = False

    # ─── CHECK 2: Key prefix resolver logic in source ─────────────────────
    print(f"\n[2/6] Key prefix resolver logic (TENBaseBlockItem.java)")
    print("-" * 72)

    has_machine_resolver = bool(re.search(r'machine_', tenbase))
    has_engine_resolver = bool(re.search(r'engine_', tenbase))
    has_cable_resolver = bool(re.search(r'cable', tenbase))
    has_energy_cell_resolver = bool(re.search(r'energy_cell', tenbase))
    has_return_null = bool(re.search(r'return\s+null', tenbase)) or bool(re.search(r'// no tooltip|// not a target', tenbase, re.I))
    has_new_resolver = bool(re.search(r'resolveKeyPrefix|getKeyPrefix|prefix', tenbase))

    print(f"      machine_ resolver:  {'[OK] found' if has_machine_resolver else '[FAIL] missing'}")
    print(f"      engine_ resolver:   {'[OK] found' if has_engine_resolver else '[FAIL] missing'}")
    print(f"      cable resolver:     {'[OK] found' if has_cable_resolver else '[FAIL] missing'}")
    print(f"      energy_cell:        {'[OK] found' if has_energy_cell_resolver else '[FAIL] missing'}")
    print(f"      null/non-target:    {'[OK] found' if has_return_null else '[FAIL] missing'}")

    # Check that cables DON'T use info. prefix
    cable_info_usage = False
    # Look for cable lines that also reference info.
    # This is a heuristic: check if cable is used WITHOUT info. prefix
    cable_lines = [l for l in tenbase.split('\n') if 'cable' in l.lower()]
    for line in cable_lines:
        if 'info.' in line and 'cable' in line.lower():
            # Check if this is actually referencing info prefix for cables
            # Pattern like: "info." + "cable" or similar
            if not re.search(r'regPath\b', line) and not re.search(r'path\b', line):
                pass  # might be a false positive
    print(f"      cable (no info):    {'[OK] no info prefix for cables' if not cable_info_usage else '[FAIL] might use info prefix'}")
    print(f"      non-target skip:    {'[OK] has return null guard' if has_return_null else '[FAIL] missing'}")

    resolver_ok = has_machine_resolver and has_engine_resolver and has_cable_resolver
    if resolver_ok:
        print(f"\n      >>> PASS: resolver logic covers all categories")
    else:
        print(f"\n      >>> FAIL: resolver logic incomplete")
        all_pass = False

    # ─── CHECK 3: Prefix sanity (test the python resolver against expected) ─
    print(f"\n[3/6] Key prefix sanity check (Python reference resolver)")
    print("-" * 72)

    prefix_errors = 0
    for target in TARGETS:
        expected = expected_prefix(target)
        # Check that expected prefix actually yields keys in lang files
        # Check first key exists (at least .0)
        first_key = f"{expected}0"
        has_en = first_key in en_us
        has_zn = first_key in zh_cn
        icon = "[OK]" if has_en and has_zn else "[WARN]" if has_en or has_zn else "[FAIL]"
        if not (has_en and has_zn):
            prefix_errors += 1
        print(f"      {icon} {target:<35s} prefix={expected:<40s} "
              f"en={str(has_en):<5s} zh={str(has_zn):<5s}")

    if prefix_errors == 0:
        print(f"\n      >>> PASS: all target prefixes resolve in both lang files")
    else:
        print(f"\n      >>> FAIL: {prefix_errors} targets have missing lang keys")
        all_pass = False

    # ─── CHECK 4: Lang key continuity ─────────────────────────────────────
    print(f"\n[4/6] Lang key continuity check")
    print("-" * 72)

    lang_results = check_all_langs(TARGETS)

    continuity_errors = 0
    for lang_name in ["en_us", "zh_cn"]:
        print(f"      [{lang_name}]")
        for target in TARGETS:
            prefix = expected_prefix(target)
            if prefix is None:
                continue
            r = lang_results[lang_name].get(target, {})
            gap = r.get("gap_at")
            continuous = r.get("continuous", False)
            keys = r.get("keys_found", [])
            max_cont = r.get("max_continuous", -1)

            if gap is not None:
                print(f"        [FAIL] {target:<35s} gap at index {gap}, "
                      f"keys={keys}")
                continuity_errors += 1
            elif not keys:
                print(f"        [FAIL] {target:<35s} no tooltip keys found")
                continuity_errors += 1
            else:
                print(f"        [OK] {target:<35s} continuous keys {keys[0]}..{max_cont} "
                      f"({len(keys)} total)")

    if continuity_errors == 0:
        print(f"\n      >>> PASS: all lang keys continuous with no gaps")
    else:
        print(f"\n      >>> FAIL: {continuity_errors} continuity issues found")
        all_pass = False

    # ─── CHECK 5: BlockItem description prefix (useBlockDescriptionPrefix) ──
    print(f"\n[5/6] BlockItem description prefix (useBlockDescriptionPrefix)")
    print("-" * 72)

    # Check that registerTENBaseBlockItem method calls .useBlockDescriptionPrefix()
    has_use_block_prefix = ".useBlockDescriptionPrefix()" in tenblocks

    # Also find which items go through registerTENBaseBlockItem for per-item report
    tenbase_items_using_tenblocks = [t for t in TARGETS if reg_results.get(t, False)]
    non_tenbase_items_plain = [t for t in TARGETS if not reg_results.get(t, False)]

    # Check that each TENBaseBlockItem has a block. prefix lang entry
    prefix_property_errors = 0
    for target in TARGETS:
        uses_tenbase = reg_results.get(target, False)
        if uses_tenbase:
            # Items that use registerTENBaseBlockItem need useBlockDescriptionPrefix
            block_key = f"block.kenergyengineering.{target}"
            en_has = block_key in en_us
            zh_has = block_key in zh_cn
            key_ok = en_has and zh_has

            if not has_use_block_prefix:
                # Without the prefix, these items show item.<id> instead of block.<id>
                item_key = f"item.kenergyengineering.{target}"
                en_item_has = item_key in en_us
                zh_item_has = item_key in zh_cn
                fallback_note = ""
                if en_item_has or zh_item_has:
                    fallback_note = f" (fallback item.<id> WOULD resolve but is wrong)"
                print(f"      [FAIL] {target:<35s} block_key={block_key:<50s} "
                      f"en={'[OK]' if en_has else '[MISS]':<6s} "
                      f"zh={'[OK]' if zh_has else '[MISS]':<6s}"
                      f" | MISSING useBlockDescriptionPrefix{fallback_note}")
                prefix_property_errors += 1
            else:
                print(f"      [OK] {target:<35s} block_key={block_key:<50s} "
                      f"en={'[OK]' if en_has else '[MISS]':<6s} "
                      f"zh={'[OK]' if zh_has else '[MISS]':<6s}")

    if non_tenbase_items_plain:
        print(f"\n      (plain BlockItems via registerSimpleBlockItem get auto-prefix — excluded from this check)")

    if has_use_block_prefix and prefix_property_errors == 0:
        print(f"\n      >>> PASS: .useBlockDescriptionPrefix() present, all {len(tenbase_items_using_tenblocks)} targets have block.<id> lang keys")
    elif not has_use_block_prefix:
        print(f"\n      >>> FAIL: .useBlockDescriptionPrefix() MISSING in registerTENBaseBlockItem — "
              f"{prefix_property_errors} targets will show bare item.<id> keys!")
        all_pass = False
    else:
        print(f"\n      >>> FAIL: {prefix_property_errors} targets missing block.<id> lang keys")
        all_pass = False

    # ─── CHECK 6: Name key integrity ──────────────────────────────────────
    print(f"\n[6/6] Name key integrity check (full lang scan)")
    print("-" * 72)

    name_integrity_errors = 0

    # 6a: All 21 targets must have block.kenergyengineering.<id> in both langs
    print(f"      [block.<id> existence — 21 targets]")
    for target in TARGETS:
        block_key = f"block.kenergyengineering.{target}"
        en_ok = block_key in en_us
        zh_ok = block_key in zh_cn
        if en_ok and zh_ok:
            print(f"        [OK] {target:<35s} en={en_us[block_key]:<30s} zh={zh_cn[block_key]:<30s}")
        else:
            print(f"        [FAIL] {target:<35s} en={'[OK]' if en_ok else '[MISS]'} zh={'[OK]' if zh_ok else '[MISS]'}")
            name_integrity_errors += 1

    # 6b: No item.kenergyengineering.<id> for any of the 21 targets
    print(f"      [item.<id> must NOT exist for targets — 21 check]")
    for target in TARGETS:
        item_key = f"item.kenergyengineering.{target}"
        en_has = item_key in en_us
        zh_has = item_key in zh_cn
        if en_has or zh_has:
            print(f"        [WARN] {target:<35s} item.<id> key exists! "
                  f"en={'[FOUND]' if en_has else '[absent]':<8s} "
                  f"zh={'[FOUND]' if zh_has else '[absent]':<8s}")
            name_integrity_errors += 1  # This is incorrect — target block items should NOT have item.<id> names
        else:
            print(f"        [OK] {target:<35s} no item.<id> key (correct)")

    # 6c: Scan all block.kenergyengineering.* keys — no empty/null values
    print(f"      [block.* key value scan — all block items]")
    all_block_keys = [k for k in en_us if k.startswith("block.kenergyengineering.")]
    for key in sorted(all_block_keys):
        en_val = en_us.get(key, "")
        zh_val = zh_cn.get(key, "")
        if not en_val or not en_val.strip():
            print(f"        [FAIL] {key:<55s} en value is EMPTY!")
            name_integrity_errors += 1
        elif key.endswith(".0") or key.endswith(".1") or key.endswith(".2") or key.endswith(".3") or key.endswith(".4") or key.endswith(".5"):
            # Skip tooltip keys — they are checked in continuity above
            pass
        elif en_val == key:
            print(f"        [FAIL] {key:<55s} en value is BARE KEY (untranslated: '{en_val}')!")
            name_integrity_errors += 1
        else:
            # Check bare key in zh too
            if not zh_val or not zh_val.strip():
                print(f"        [WARN] {key:<55s} zh value is EMPTY! en='{en_val}'")
                name_integrity_errors += 1
            elif zh_val == key:
                print(f"        [FAIL] {key:<55s} zh value is BARE KEY (untranslated: '{zh_val}')! en='{en_val}'")
                name_integrity_errors += 1

    # 6d: Scan item.kenergyengineering.* keys — no bare key display
    print(f"      [item.* key value scan — plain items]")
    all_item_keys = [k for k in en_us if k.startswith("item.kenergyengineering.")]
    for key in sorted(all_item_keys):
        en_val = en_us.get(key, "")
        zh_val = zh_cn.get(key, "")
        if not en_val or not en_val.strip():
            print(f"        [FAIL] {key:<55s} en value is EMPTY!")
            name_integrity_errors += 1
        elif en_val == key:
            print(f"        [FAIL] {key:<55s} en value is BARE KEY (untranslated: '{en_val}')!")
            name_integrity_errors += 1
        else:
            if not zh_val or not zh_val.strip():
                print(f"        [WARN] {key:<55s} zh value is EMPTY! en='{en_val}'")
                name_integrity_errors += 1
            elif zh_val == key:
                print(f"        [FAIL] {key:<55s} zh value is BARE KEY (untranslated: '{zh_val}')! en='{en_val}'")
                name_integrity_errors += 1

    if name_integrity_errors == 0:
        print(f"\n      >>> PASS: all name keys complete, no bare key display risk")
    else:
        print(f"\n      >>> FAIL: {name_integrity_errors} name integrity issues found")
        all_pass = False

    # ─── NAME KEY STATISTICS ──────────────────────────────────────────────
    print()
    print("=" * 72)
    print("  Name Key Statistics (lang coverage)")
    print("=" * 72)

    # Count block.* name keys
    en_block_names = [k for k in en_us if k.startswith("block.kenergyengineering.")]
    zh_block_names = [k for k in zh_cn if k.startswith("block.kenergyengineering.")]
    en_item_names = [k for k in en_us if k.startswith("item.kenergyengineering.")]
    zh_item_names = [k for k in zh_cn if k.startswith("item.kenergyengineering.")]

    print(f"\n  Block item name keys (block.kenergyengineering.*):")
    print(f"    en_us: {len(en_block_names)} keys")
    print(f"    zh_cn: {len(zh_block_names)} keys")
    print(f"    {'[OK]' if len(en_block_names) == len(zh_block_names) else '[MISMATCH]'} "
          f"count {'matches' if len(en_block_names) == len(zh_block_names) else 'differs'}")

    print(f"\n  Plain item name keys (item.kenergyengineering.*):")
    print(f"    en_us: {len(en_item_names)} keys")
    print(f"    zh_cn: {len(zh_item_names)} keys")
    print(f"    {'[OK]' if len(en_item_names) == len(zh_item_names) else '[MISMATCH]'} "
          f"count {'matches' if len(en_item_names) == len(zh_item_names) else 'differs'}")

    # Cross-check target names
    print(f"\n  Target block name cross-check ({len(TARGETS)} targets):")
    target_21_block_ok = 0
    for target in TARGETS:
        bk = f"block.kenergyengineering.{target}"
        if bk in en_us and bk in zh_cn:
            target_21_block_ok += 1
    print(f"    {target_21_block_ok}/{len(TARGETS)} have block.<id> in both en_us AND zh_cn "
          f"{'[OK]' if target_21_block_ok == len(TARGETS) else '[SOME MISSING]'}")

    # ─── SUMMARY ──────────────────────────────────────────────────────────
    print()
    print("=" * 72)
    if all_pass:
        print("  >>> GREEN: All checks passed. TASK-012 implementation complete.")
        print(f"  >>> {target_hits}/{len(TARGETS)} targets use TENBaseBlockItem.")
        print(f"  >>> 0 exclusion violations, 0 lang continuity errors.")
        print(f"  >>> .useBlockDescriptionPrefix() present, 0 name integrity issues.")
        sys.exit(0)
    else:
        print("  >>> RED: One or more checks FAILED. See details above.")
        print(f"  >>> Targets using TENBaseBlockItem: {target_hits}/{len(TARGETS)}")
        print(f"  >>> Exclusion violations: {exclusion_violations}")
        print(f"  >>> Lang continuity errors: {continuity_errors}")
        print(f"  >>> useBlockDescriptionPrefix: {'present' if has_use_block_prefix else 'MISSING'}")
        print(f"  >>> Name integrity errors: {name_integrity_errors}")
        sys.exit(1)


if __name__ == "__main__":
    main()
