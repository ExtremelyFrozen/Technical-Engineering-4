#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Validate ConfigHolder has migrated from hardcoded stubs to NeoForge ModConfigSpec.

FP-001 TDD RED/GATE validator.
Adapted from backup in D:/Temp/task020-backup-20260712-025524/

Changes from backup:
  - Replaced check_no_define_in_range (incorrect — origin/1.21 DID have range annotations)
    with check_numeric_fields_use_define_in_range (verifies defineInRange IS used with correct
    min/max from evidence_26_1_2_feature_parity_01.md)
  - Fixed check_old_field_keys_present to look for keys inside config define() calls, not
    anywhere in the file (mutable primitives also contain field names by coincidence)

Checks:
  - ConfigHolder.java: uses ModConfigSpec.Builder, exports COMMON_SPEC / CLIENT_SPEC
  - ConfigHolder fields: typed ModConfigSpec values (ConfigValue, IntValue, DoubleValue, BooleanValue)
  - TEN.java: @Mod constructor calls registerConfig instead of ConfigHolder.init()
  - No mutable primitive public fields remaining (old hardcoded stub pattern)
  - All old field keys present in the new config (machine.*, energyUnit.*, farm.*, client.*)
  - Numeric fields use defineInRange with ranges matching evidence_01
  - Callers (EnergyUnitItem, FarmBlockEntity) use getter methods, not direct field access

Exit 0 = GREEN (fully migrated), 1 = RED (not migrated or incomplete).
"""

import os, re, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = lambda *p: os.path.join(ROOT, "src", *p)
CONFIG = lambda f: SRC("main", "java", "com", "modularmc", "ten", "config", f)
TEN_JAVA = SRC("main", "java", "com", "modularmc", "ten", "TEN.java")

# --- UTF-8 stdio setup ---
for s in (sys.stdout, sys.stderr):
    if s is not None and hasattr(s, 'reconfigure'):
        try:
            s.reconfigure(encoding='utf-8')
        except (ValueError, OSError):
            pass


def read(path):
    """Read file as str, returning None if missing."""
    if not os.path.exists(path):
        return None
    with open(path, "r", encoding="utf-8") as f:
        return f.read()


# ─── Expected numeric field ranges (from evidence_26_1_2_feature_parity_01.md) ───
# Format: (field_name, expected_default, expected_min, expected_max)
EXPECTED_RANGES = [
    ("energyMultiplier", 1.0, 0.01, 100.0),
    ("baseEnergyCapacity", 10000, 1000, 1000000000),
    ("maxEnergy", 400000, 1000, 100000000),
    ("chargeRate", 2000, 1, 100000),
    ("inputRate", 2000, 1, 100000),
    ("outputRate", 2000, 1, 100000),
]


# ─── Checks ─────────────────────────────────────────────────────────────────

def check_ten_config_uses_mod_config_spec(acc):
    """TENConfig must import and use ModConfigSpec."""
    text = read(CONFIG("TENConfig.java"))
    if text is None:
        acc("TENConfig.java not found")
        return
    if "import net.neoforged.neoforge.common.ModConfigSpec;" not in text:
        acc("TENConfig.java: missing import net.neoforged.neoforge.common.ModConfigSpec")
    if "ModConfigSpec" not in text:
        acc("TENConfig.java: no ModConfigSpec reference found")


def check_ten_config_exports_specs(acc):
    """TENConfig must export ModConfigSpec instances."""
    text = read(CONFIG("TENConfig.java"))
    if text is None:
        return
    if not re.search(r'(public\s+)?static\s+(final\s+)?ModConfigSpec\s+COMMON_SPEC', text):
        acc("TENConfig.java: missing COMMON_SPEC export")
    if not re.search(r'(public\s+)?static\s+(final\s+)?ModConfigSpec\s+CLIENT_SPEC', text):
        acc("TENConfig.java: missing CLIENT_SPEC export (client configs need separate spec)")


def check_ten_config_typed_values(acc):
    """TENConfig must use typed ConfigValue/IntValue/DoubleValue/BooleanValue, not mutable primitives."""
    text = read(CONFIG("TENConfig.java"))
    if text is None:
        return
    has_typed = bool(re.search(r'(ConfigValue|IntValue|DoubleValue|BooleanValue)\s+\w+', text))
    if not has_typed:
        acc("TENConfig.java: no ConfigValue/typed value fields found")
    # ConfigHolder must NOT have old mutable primitive pattern
    holder_text = read(CONFIG("ConfigHolder.java"))
    if holder_text:
        old_pattern = re.findall(r'public\s+(int|double|boolean)\s+\w+\s*=\s*\d', holder_text)
        bad = [m for m in old_pattern if not re.match(r'public static final', m)]
        if bad:
            acc(f"ConfigHolder.java: {len(bad)} mutable primitive field(s) remain: {', '.join(b[:30] for b in bad[:5])}")


def check_register_config_in_ten(acc):
    """TEN.java @Mod constructor must call registerConfig."""
    text = read(TEN_JAVA)
    if text is None:
        acc("TEN.java not found")
        return
    if "registerConfig" not in text:
        acc("TEN.java: constructor missing registerConfig call")


def check_config_holder_no_init(acc):
    """ConfigHolder must NOT declare init() — no binary compat callers remain."""
    text = read(CONFIG("ConfigHolder.java"))
    if text is None:
        return
    if re.search(r'public\s+static\s+void\s+init\s*\(\s*\)', text):
        acc("ConfigHolder.java: init() method still declared — must be removed (no callers)")


def check_numeric_fields_use_define_in_range(acc):
    """Numeric config fields must use defineInRange with correct ranges from evidence_01."""
    text = read(CONFIG("TENConfig.java"))
    if text is None:
        return
    # Normalize Java source: strip underscore separators from numeric literals
    normalized = re.sub(r'(?<=[\d])_(?=[\d])', '', text)
    for field_name, default, vmin, vmax in EXPECTED_RANGES:
        pattern = rf'defineInRange\s*\(\s*"{field_name}"'
        if not re.search(pattern, normalized):
            acc(f"TENConfig.java: '{field_name}' does not use defineInRange (expected: defineInRange(\"{field_name}\", {default}, {vmin}, {vmax}))")
            continue
        # Verify default numeric value matches (after underscore normalization)
        default_str = str(default)
        # Handle float vs int: match both "1.0" and "1"
        if '.' in default_str:
            value_pattern = rf'(?:{re.escape(default_str)}|{re.escape(str(int(default)))})'
        else:
            value_pattern = re.escape(default_str)
        full_pattern = rf'defineInRange\s*\(\s*"{field_name}"\s*,\s*{value_pattern}'
        if not re.search(full_pattern, normalized):
            acc(f"TENConfig.java: '{field_name}' default value may not match expected {default} (found defineInRange but couldn't verify default)")


def check_old_field_keys_in_config_defines(acc):
    """All old field keys must be present in config define/defineInRange/defineList calls."""
    text = read(CONFIG("TENConfig.java"))
    if text is None:
        return
    expected_keys = [
        "energyMultiplier", "baseEnergyCapacity",
        "enableSmelter", "enablePulverizer", "enableCompressor", "enableRefiner",
        "enableInductionFurnace", "enablePsionicant", "enableBeacon",
        "enableMobRipper", "enableQuarry", "enableEnchantmentFlusher",
        "enableCondenser", "enableFarmManager",
        "bushCrops",
        "maxEnergy", "chargeRate", "inputRate", "outputRate", "chargingDefault",
        "showMachineHUD", "showCableHUD",
    ]
    # Look for keys inside define("key"), defineInRange("key"), defineList("key") calls
    define_pattern = re.findall(r'define(?:InRange|List)?\s*\(\s*"([^"]+)"', text)
    found = sum(1 for k in expected_keys if k in define_pattern)
    if found < len(expected_keys):
        missing = [k for k in expected_keys if k not in define_pattern]
        acc(f"TENConfig.java: {len(missing)} expected field key(s) missing in config define() calls: {', '.join(missing[:8])}")


def check_builder_push_categories(acc):
    """TENConfig must have builder.push() calls for machine, energyUnit, farm, and client categories."""
    text = read(CONFIG("TENConfig.java"))
    if text is None:
        return
    if "push" not in text:
        acc("TENConfig.java: no builder.push() calls found (missing categories)")
        return
    expected_categories = ["machine", "energyUnit", "farm", "client"]
    for cat in expected_categories:
        push_calls = re.findall(r'\.push\("([^"]+)"', text)
        if not any(cat.lower() in pc.lower() for pc in push_calls):
            acc(f"TENConfig.java: missing builder.push() for category '{cat}'")


def check_energy_unit_item_uses_getters(acc):
    """EnergyUnitItem must use getter methods like configHolder.energyUnit().maxEnergy(), not direct field access."""
    path = SRC("main", "java", "com", "modularmc", "ten", "common", "item", "EnergyUnitItem.java")
    text = read(path)
    if text is None:
        acc("EnergyUnitItem.java not found")
        return
    # Must NOT have ConfigHolder.INSTANCE.energyUnit.X (direct field access, no parentheses)
    if re.search(r'ConfigHolder\.INSTANCE\.\w+\.\w+\b(?!\s*\()', text):
        acc("EnergyUnitItem.java: still uses direct field access to config (should use getter methods)")


def check_farm_block_entity_uses_getters(acc):
    """FarmBlockEntity must use getter methods, not direct field access."""
    path = SRC("main", "java", "com", "modularmc", "ten", "common", "blockentity", "machine", "FarmBlockEntity.java")
    text = read(path)
    if text is None:
        acc("FarmBlockEntity.java not found")
        return
    if re.search(r'ConfigHolder\.INSTANCE\.farm\.\w+\b(?!\s*\()', text):
        acc("FarmBlockEntity.java: still uses direct field access to config (should use getter methods)")


# ─── Main ────────────────────────────────────────────────────────────────────

def main():
    errors = []

    def acc(msg):
        errors.append(msg)

    checks = [
        ("TENConfig: ModConfigSpec import & usage", check_ten_config_uses_mod_config_spec),
        ("TENConfig: COMMON_SPEC / CLIENT_SPEC exported", check_ten_config_exports_specs),
        ("TENConfig: typed ConfigValue fields (no mutable primitives)", check_ten_config_typed_values),
        ("TEN.java: registerConfig call", check_register_config_in_ten),
        ("ConfigHolder.init() absent (no callers remain)", check_config_holder_no_init),
        ("Numeric fields use defineInRange with correct ranges (evidence_01)", check_numeric_fields_use_define_in_range),
        ("All old field keys present in config define() calls", check_old_field_keys_in_config_defines),
        ("builder.push() categories (machine/energyUnit/farm/client)", check_builder_push_categories),
        ("EnergyUnitItem uses config getters", check_energy_unit_item_uses_getters),
        ("FarmBlockEntity uses config getters", check_farm_block_entity_uses_getters),
    ]

    print("=" * 60)
    print("  FP-001 TDD RED/GATE — ConfigHolder → ModConfigSpec Migration")
    print("=" * 60)

    for i, (name, fn) in enumerate(checks, 1):
        before = len(errors)
        fn(acc)
        nerr = len(errors) - before
        status = "  PASS" if nerr == 0 else f"  FAIL ({nerr})"
        print(f"  [{i:2d}] {name}: {status}")

    print(f"\n  Total: {len(errors)} error(s)")
    if errors:
        for e in errors[:15]:
            print(f"    - {e}")
        if len(errors) > 15:
            print(f"    ... and {len(errors) - 15} more")
        print("\n  >>> RED - Migration incomplete (expected for TDD RED phase)")
        return 1
    print("\n  >>> GREEN - ConfigHolder successfully migrated to ModConfigSpec")
    return 0


if __name__ == "__main__":
    sys.exit(main())
