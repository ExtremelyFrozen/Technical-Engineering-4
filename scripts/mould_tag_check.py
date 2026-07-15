#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import sys
import io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8')
"""
Post-datagen validation script for the TEN mould tag ownership & consistency.

Checks:
   1. Generated resources must contain data/kenergyengineering/tags/item/moulds.json
      - Must exist
      - replace must be false
      - Must contain exactly 9 mould item IDs (all 9 registered moulds)
   2. Main resources must NOT contain data/kenergyengineering/tags/item/moulds.json
      (i.e. Generated owns the moulds tag exclusively)
   3. TENTagProvider.java must write a moulds tag (consuming TENItems mould collection)
   4. TENTags class must expose a TagKey<Item> MOULDS
   5. CompressorBlockEntity.valid() must use TENTags.MOULDS (not hardcoded holder chain)
   6. Recipe mould inputs remain as static specific IDs (not tag-based)

Exit code 0 = all checks pass (GREEN), non-zero = failure(s) (RED).
"""

import argparse
import json
import os
import re
import sys

PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

EXPECTED_MOULD_IDS = [
    "kenergyengineering:mould_gear",
    "kenergyengineering:mould_plate",
    "kenergyengineering:mould_rod",
    "kenergyengineering:mould_string",
    "kenergyengineering:mould_compressed_small",
    "kenergyengineering:mould_compressed_large",
    "kenergyengineering:mould_split",
    "kenergyengineering:mould_coin",
    "kenergyengineering:mould_dense_plate",
]

EXPECTED_COUNT = len(EXPECTED_MOULD_IDS)


def read_json(path):
    try:
        with open(path, "r", encoding="utf-8") as fh:
            return json.load(fh)
    except (json.JSONDecodeError, OSError):
        return None


def read_file(path):
    try:
        with open(path, "r", encoding="utf-8") as fh:
            return fh.read()
    except OSError:
        return None


def check_generated_moulds_json(gen_dir):
    """Check generated moulds.json has replace=false and exactly 9 expected mould IDs."""
    tag_path = os.path.join(gen_dir, "data", "kenergyengineering", "tags", "item", "moulds.json")
    if not os.path.isfile(tag_path):
        print(f"  FAIL: Generated moulds.json not found at {tag_path}")
        return False

    data = read_json(tag_path)
    if data is None:
        print(f"  FAIL: Generated moulds.json is not valid JSON")
        return False

    all_ok = True
    replace = data.get("replace", None)
    if replace is False:
        print(f"  OK: Generated moulds.json replace=false")
    else:
        print(f"  FAIL: Generated moulds.json replace={replace}, expected false")
        all_ok = False

    values = data.get("values", [])
    if not isinstance(values, list):
        print(f"  FAIL: Generated moulds.json 'values' is not a list")
        return False

    if len(values) == EXPECTED_COUNT:
        print(f"  OK: Generated moulds.json has {EXPECTED_COUNT} values")
    else:
        print(f"  FAIL: Generated moulds.json has {len(values)} values, expected {EXPECTED_COUNT}")
        all_ok = False

    values_set = set(values)
    expected_set = set(EXPECTED_MOULD_IDS)
    missing = expected_set - values_set
    extra = values_set - expected_set

    if missing:
        print(f"  FAIL: Generated moulds.json missing IDs: {sorted(missing)}")
        all_ok = False
    else:
        print(f"  OK: Generated moulds.json contains all expected IDs")

    if extra:
        print(f"  FAIL: Generated moulds.json has unexpected IDs: {sorted(extra)}")
        all_ok = False
    else:
        print(f"  OK: Generated moulds.json has no unexpected IDs")

    tag_refs = [v for v in values if v.startswith("#")]
    if tag_refs:
        print(f"  FAIL: Generated moulds.json contains tag references: {tag_refs}")
        all_ok = False
    else:
        print(f"  OK: Generated moulds.json contains only item IDs")

    return all_ok


def check_main_no_moulds(main_dir):
    """Main resources must not contain moulds.json."""
    tag_path = os.path.join(main_dir, "data", "kenergyengineering", "tags", "item", "moulds.json")
    if os.path.isfile(tag_path):
        print(f"  FAIL: Main moulds.json exists (should be removed)")
        return False
    else:
        print(f"  OK: Main moulds.json does not exist (generated owns it)")
        return True


def check_tag_provider_has_moulds():
    """TENTagProvider.java must write moulds.json."""
    provider_path = os.path.join(
        PROJECT_ROOT, "src", "main", "java",
        "com", "modularmc", "ten", "data", "TENTagProvider.java"
    )
    content = read_file(provider_path)
    if content is None:
        print(f"  FAIL: Cannot read TENTagProvider.java")
        return False

    if re.search(r'writeTag\([^)]*,\s*"moulds\.json"', content):
        print(f"  OK: TENTagProvider.java has writeTag for moulds.json")
        return True
    else:
        print(f"  FAIL: TENTagProvider.java does not write moulds.json")
        return False


def check_tentags_has_moulds():
    """TENTags class must expose a TagKey<Item> MOULDS."""
    tags_path = os.path.join(
        PROJECT_ROOT, "src", "main", "java",
        "com", "modularmc", "ten", "common", "data", "TENTags.java"
    )
    content = read_file(tags_path)
    if content is None:
        print(f"  FAIL: Cannot read TENTags.java")
        return False

    if re.search(r'TagKey<Item>\s+MOULDS\s*=', content):
        print(f"  OK: TENTags.java declares TagKey<Item> MOULDS")
        return True
    else:
        print(f"  FAIL: TENTags.java does not have TagKey<Item> MOULDS")
        return False


def check_compressor_uses_tag():
    """CompressorBlockEntity.valid() must use TENTags.MOULDS."""
    be_path = os.path.join(
        PROJECT_ROOT, "src", "main", "java",
        "com", "modularmc", "ten", "common", "blockentity", "machine",
        "CompressorBlockEntity.java"
    )
    content = read_file(be_path)
    if content is None:
        print(f"  FAIL: Cannot read CompressorBlockEntity.java")
        return False

    if re.search(r'TENTags\.MOULDS', content):
        print(f"  OK: CompressorBlockEntity.valid() uses TENTags.MOULDS")
    else:
        print(f"  FAIL: CompressorBlockEntity does not reference TENTags.MOULDS")
        return False

    if re.search(r'MOULD_GEAR\.get\(\)\s*\|\|\s*.*MOULD_PLATE\.get\(\)', content):
        print(f"  FAIL: CompressorBlockEntity still has hardcoded mould chain")
        return False
    else:
        print(f"  OK: No hardcoded mould holder chain")
        return True


def check_recipe_moulds_static(main_dir, gen_dir):
    """Recipe mould inputs must use static item IDs, not tags."""
    all_ok = True
    checked = 0

    for base_dir, label in [(main_dir, "main"), (gen_dir, "generated")]:
        recipe_root = os.path.join(base_dir, "data", "kenergyengineering", "recipe")
        if not os.path.isdir(recipe_root):
            continue
        for root, _dirs, files in os.walk(recipe_root):
            for f in files:
                if not f.endswith(".json"):
                    continue
                path = os.path.join(root, f)
                data = read_json(path)
                if data is None or data.get("type") != "kenergyengineering:compressor":
                    continue
                inputs = data.get("inputs", [])
                for idx, ingr in enumerate(inputs):
                    key = ingr.get("key", "")
                    if "mould" in key:
                        if key.startswith("#"):
                            print(f"  FAIL [{label}]: {root}/{f} input[{idx}] uses tag: {key}")
                            all_ok = False
                        else:
                            checked += 1

    if all_ok:
        print(f"  OK: All {checked} mould recipe inputs use static item IDs")
    return all_ok


def main():
    parser = argparse.ArgumentParser(
        description="Validate the TEN mould tag ownership & consistency."
    )
    parser.add_argument("--main", default=os.path.join(PROJECT_ROOT, "src", "main", "resources"))
    parser.add_argument("--generated", default=os.path.join(PROJECT_ROOT, "src", "generated", "resources"))
    parser.add_argument("--build", default=None)
    parser.add_argument("--pre-datagen", action="store_true", default=False)
    args = parser.parse_args()

    exit_code = 0

    print("=" * 60)
    print("TEN Mould Tag Ownership & Consistency Check v2 (generated-owned)")
    print("=" * 60)

    print("\n[1] Generated moulds.json (replace=false, exactly 9 IDs)")
    gen_exists = os.path.isdir(args.generated)
    if gen_exists:
        if not check_generated_moulds_json(args.generated):
            exit_code = 1
    elif args.pre_datagen:
        print("  SKIP: generated dir not found (pre-datagen)")
    else:
        print("  FAIL: generated dir not found")
        exit_code = 1

    print("\n[2] Main resources must NOT contain moulds.json")
    if not check_main_no_moulds(args.main):
        exit_code = 1

    print("\n[3] TENTagProvider must write moulds.json")
    if not check_tag_provider_has_moulds():
        exit_code = 1

    print("\n[4] TENTags class exposes TagKey<Item> MOULDS")
    if not check_tentags_has_moulds():
        exit_code = 1

    print("\n[5] CompressorBlockEntity.valid() uses TENTags.MOULDS")
    if not check_compressor_uses_tag():
        exit_code = 1

    print("\n[6] Recipe mould inputs remain static")
    if not check_recipe_moulds_static(args.main, args.generated):
        exit_code = 1

    if args.build:
        print(f"\n[Build] Checking {args.build}")
        build_tag = os.path.join(args.build, "data", "kenergyengineering", "tags", "item", "moulds.json")
        if os.path.isfile(build_tag):
            print(f"  INFO: Build moulds.json exists")
        else:
            print(f"  INFO: Build moulds.json not found")

    print("\n" + "=" * 60)
    if exit_code == 0:
        print("RESULT: ALL MOULD TAG CHECKS PASSED (GREEN)")
    else:
        print("RESULT: MOULD TAG CHECK(S) FAILED (RED)")
    print("=" * 60)

    return exit_code


if __name__ == "__main__":
    sys.exit(main())
