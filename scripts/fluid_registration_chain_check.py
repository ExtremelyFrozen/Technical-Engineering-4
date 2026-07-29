#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import sys
import io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8')
"""
Fluid Registration Chain Check — RED/GREEN TDD script for TEN fluids.

RED asserts (will fail initially):
  1. Registration has FLUID_TYPES DeferredRegister
  2. TENFluids has 5 independent FluidType holders
  3. Each BaseFlowingFluid.Properties uses its own type (not DEFAULT)
  4. Each LiquidBlock passes SOURCE (not FLOWING)
  5. Each FluidType has descriptionId mapped to fluid.kenergyengineering.<id>
  6. Client has FluidModel registration for all 5 source+flowing pairs
  7. Bucket chain passes SOURCE (unchanged)
"""

import os
import re
import sys
import json

from resource_roots import resolve_unique_resource

PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC_MAIN = os.path.join(PROJECT_ROOT, "src", "main")
SRC_CLIENT = os.path.join(PROJECT_ROOT, "src", "client")
GENERATED = os.path.join(PROJECT_ROOT, "src", "generated")

# ── helpers ──────────────────────────────────────────────────────────
def read_java(path):
    with open(path, "r", encoding="utf-8") as f:
        return f.read()

def glob_java(pattern_start, base=SRC_MAIN):
    matches = []
    for root, dirs, files in os.walk(base):
        for f in files:
            if f.endswith(".java") and f.startswith(pattern_start):
                matches.append(os.path.join(root, f))
    return matches

# ── assertion helpers ────────────────────────────────────────────────
REDS = []
GREENS = []

def RED(msg, evidence=""):
    REDS.append(f"❌ RED: {msg}\n   Evidence: {evidence}")

def GREEN(msg):
    GREENS.append(f"✅ GREEN: {msg}")

def check_file(filepath, description):
    if not os.path.isfile(filepath):
        RED(f"{description} — file not found", f"missing: {filepath}")
        return None
    GREEN(f"{description} — found")
    return read_java(filepath)

def _resolve_lang(filename):
    """Resolve a lang file using dual-root lookup with friendly error messages.

    0 owner → friendly RED (non-blocking for missing lang).
    2 owner → conflict RED (blocking — must resolve before proceed).
    """
    rel = f"assets/kenergyengineering/lang/{filename}"
    try:
        _, path = resolve_unique_resource(PROJECT_ROOT, rel)
        return path
    except FileNotFoundError:
        RED(f"Lang file {filename} not found in any resource root",
            f"searched main and generated for '{rel}' — 0 owners, blocking")
        return None
    except AssertionError as e:
        RED(f"Lang file {filename} exists in multiple roots — conflict",
            f"blocking: {e}")
        return None

# ═══════════════════════════════════════════════════════════════════════
# 1. Registration.java — FLUID_TYPES DeferredRegister
# ═══════════════════════════════════════════════════════════════════════
reg_file = os.path.join(SRC_MAIN, "java", "com", "modularmc", "ten", "common", "registry", "Registration.java")
reg = check_file(reg_file, "Registration.java")
if reg:
    # Check FLUID_TYPES DeferredRegister field exists
    if "FLUID_TYPES" in reg:
        GREEN("Registration.java: FLUID_TYPES field declared")
    else:
        RED("Registration.java: missing FLUID_TYPES DeferredRegister field", "expected DeferredRegister<FluidType> FLUID_TYPES")

    # Check it's registered in register() method
    if "FLUID_TYPES.register(modBus)" in reg or "FLUID_TYPES.register" in reg:
        GREEN("Registration.java: FLUID_TYPES registered on mod bus")
    else:
        RED("Registration.java: FLUID_TYPES not registered in register()", "missing 'FLUID_TYPES.register(modBus)'")

    # Check import for FluidType registry
    if "import net.neoforged.neoforge.registries.NeoForgeRegistries;" in reg or "import net.neoforged.neoforge.fluids.FluidType" in reg:
        GREEN("Registration.java: has FluidType-related imports")
    else:
        RED("Registration.java: missing FluidType imports", "check import section")

# ═══════════════════════════════════════════════════════════════════════
# 2. TENFluids.java — 5 independent FluidType holders
# ═══════════════════════════════════════════════════════════════════════
fluids_file = os.path.join(SRC_MAIN, "java", "com", "modularmc", "ten", "common", "data", "TENFluids.java")
fluids = check_file(fluids_file, "TENFluids.java")
if fluids:
    # 2a. No DEFAULT_FLUID_TYPE
    if "DEFAULT_FLUID_TYPE" in fluids:
        RED("TENFluids.java: still uses DEFAULT_FLUID_TYPE", "remove shared type, use per-fluid types")
    else:
        GREEN("TENFluids.java: no shared DEFAULT_FLUID_TYPE")

    # 2b. Each fluid type has its own FluidType holder
    expected_types = [
        ("royal_jelly", "LIQUID_ROYAL_JELLY_FLUID_TYPE"),
        ("spicy_jelly", "LIQUID_SPICY_JELLY_FLUID_TYPE"),
        ("honey", "LIQUID_HONEY_FLUID_TYPE"),
        ("xp", "LIQUID_XP_FLUID_TYPE"),
        ("bizarrerie", "LIQUID_BIZARRERIE_FLUID_TYPE"),
    ]
    for fluid_name, type_field in expected_types:
        if type_field in fluids:
            GREEN(f"TENFluids.java: {type_field} declared")
        else:
            RED(f"TENFluids.java: missing {type_field}", f"expected DeferredHolder<FluidType, FluidType> {type_field}")

        # Check descriptionId is set (allowing dynamic TEN.MOD_ID construction)
        desc_pattern = f"liquid_{fluid_name}"
        if f"fluid.kenergyengineering.liquid_{fluid_name}" in fluids or (desc_pattern in fluids and "descriptionId" in fluids):
            GREEN(f"TENFluids.java: {fluid_name} has descriptionId for liquid_{fluid_name}")
        else:
            RED(f"TENFluids.java: {fluid_name} missing descriptionId", f"expected descriptionId pointing to fluid.kenergyengineering.liquid_{fluid_name}")

    # 2c. Each BaseFlowingFluid.Properties uses its own type (not DEFAULT)
    if "DEFAULT_FLUID_TYPE" not in fluids:
        # verify each getXxxProps uses a type-specific field
        for fluid_name, type_field in expected_types:
            props_method = f"get{fluid_name.split('_')[0].title()}{fluid_name.split('_')[1].title()}Props" if '_' in fluid_name else f"get{fluid_name.title()}Props"
            # simpler: just grep for the pattern
            pattern = f"BaseFlowingFluid.Properties({type_field}"
            if type_field in fluids:
                # all properties reference the type field
                GREEN(f"TENFluids.java: {fluid_name} Properties uses {type_field}")
            else:
                RED(f"TENFluids.java: {fluid_name} Properties doesn't use its own type", f"expected ref to {type_field}")

    # 2d. LiquidBlock passes SOURCE (not FLOWING)
    for fluid_name, _ in expected_types:
        source_name = f"LIQUID_{fluid_name.upper()}_SOURCE"
        # Check LiquidBlock constructor uses source
        block_pattern = f"LiquidBlock({source_name}.get()"
        if block_pattern in fluids:
            GREEN(f"TENFluids.java: liquid_{fluid_name} block uses SOURCE")
        else:
            RED(f"TENFluids.java: liquid_{fluid_name} block might use FLOWING instead of SOURCE",
                f"expected 'LiquidBlock({source_name}.get()'")

    # 2e. Bucket chain still uses SOURCE (unchanged)
    for fluid_name, _ in expected_types:
        source_name = f"LIQUID_{fluid_name.upper()}_SOURCE"
        bucket_use = f"{source_name}.get()" in fluids
        if bucket_use:
            GREEN(f"TENFluids.java: {fluid_name} bucket uses SOURCE (unchanged)")
        else:
            RED(f"TENFluids.java: {fluid_name} bucket chain may have changed", f"expected {source_name}.get() in bucket")

    # 2f. All 5 source/flowing/block/bucket IDs preserved
    for fluid_name, _ in expected_types:
        for suffix in ["_SOURCE", "_FLOWING", "_BLOCK", "_BUCKET"]:
            field = f"LIQUID_{fluid_name.upper()}{suffix}"
            if field in fluids:
                GREEN(f"TENFluids.java: {field} preserved")
            else:
                RED(f"TENFluids.java: {field} missing — ID changed?", f"expected {field}")

    # ── NEW RED: 2g. Each get*Props chains .block(LIQUID_*_BLOCK) ──────────
    for fluid_name, _ in expected_types:
        block_field = f"LIQUID_{fluid_name.upper()}_BLOCK"
        props_method = f"get{fluid_name.split('_')[0].title()}{fluid_name.split('_')[1].title()}Props" if '_' in fluid_name else f"get{fluid_name.title()}Props"
        # Check .block() call referencing the block field in the method body
        block_call = f".block({block_field}"
        if block_call in fluids:
            GREEN(f"TENFluids.java: {props_method} chains .block({block_field})")
        else:
            RED(f"TENFluids.java: {props_method} missing .block({block_field})",
                f"add '.block({block_field})' after new BaseFlowingFluid.Properties(...)")

    # ── NEW RED: 2h. Each get*Props chains .bucket(LIQUID_*_BUCKET) ────────
    for fluid_name, _ in expected_types:
        bucket_field = f"LIQUID_{fluid_name.upper()}_BUCKET"
        props_method = f"get{fluid_name.split('_')[0].title()}{fluid_name.split('_')[1].title()}Props" if '_' in fluid_name else f"get{fluid_name.title()}Props"
        bucket_call = f".bucket({bucket_field}"
        if bucket_call in fluids:
            GREEN(f"TENFluids.java: {props_method} chains .bucket({bucket_field})")
        else:
            RED(f"TENFluids.java: {props_method} missing .bucket({bucket_field})",
                f"add '.bucket({bucket_field})' after new BaseFlowingFluid.Properties(...)")

    # ── NEW RED: 2i. liquidBlockProps has replaceable/liquid/pushReaction ──
    if "liquidBlockProps" in fluids:
        # Check .replaceable()
        if ".replaceable()" in fluids:
            GREEN("TENFluids.java: liquidBlockProps has .replaceable()")
        else:
            RED("TENFluids.java: liquidBlockProps missing .replaceable()",
                "add .replaceable() to the BlockBehaviour.Properties chain")
        # Check .liquid()
        if ".liquid()" in fluids:
            GREEN("TENFluids.java: liquidBlockProps has .liquid()")
        else:
            RED("TENFluids.java: liquidBlockProps missing .liquid()",
                "add .liquid() to the BlockBehaviour.Properties chain")
        # Check .pushReaction(PushReaction.DESTROY)
        if "pushReaction(PushReaction.DESTROY)" in fluids:
            GREEN("TENFluids.java: liquidBlockProps has pushReaction(PushReaction.DESTROY)")
        else:
            RED("TENFluids.java: liquidBlockProps missing pushReaction(PushReaction.DESTROY)",
                "add .pushReaction(PushReaction.DESTROY) to the BlockBehaviour.Properties chain")
    else:
        RED("TENFluids.java: liquidBlockProps method not found", "expected static method")

    # ── NEW RED: 2j. TENFluids imports PushReaction ────────────────────────
    if "import net.minecraft.world.level.material.PushReaction" in fluids:
        GREEN("TENFluids.java: imports PushReaction")
    else:
        RED("TENFluids.java: missing PushReaction import",
            "add 'import net.minecraft.world.level.material.PushReaction;'")

# ═══════════════════════════════════════════════════════════════════════
# 3. Client-side FluidModel registration class
# ═══════════════════════════════════════════════════════════════════════
client_base = os.path.join(SRC_CLIENT, "java", "com", "modularmc", "ten")
client_fluid_model = None

# Search for any client fluid model file
if os.path.isdir(client_base):
    for root, dirs, files in os.walk(client_base):
        for f in files:
            if f.endswith(".java") and ("FluidModel" in f or "fluid" in f.lower()):
                client_fluid_model = os.path.join(root, f)
                break

if client_fluid_model:
    GREEN(f"Client fluid model class found: {os.path.basename(client_fluid_model)}")
    model_code = read_java(client_fluid_model)

    # Check for RegisterFluidModelsEvent or equivalent
    if "RegisterFluidModelsEvent" in model_code or "register" in model_code.lower():
        GREEN("Client fluid model class has registration event listener")
    else:
        RED("Client fluid model class may not have proper event registration",
            "expected RegisterFluidModelsEvent listener")

    # Check texture references (dynamic construction via TEN.id() is fine)
    for fluid_name, _ in expected_types:
        if f"liquid_{fluid_name}" in model_code:
            GREEN(f"Client fluid model references block/fluid/liquid_{fluid_name}")
        else:
            RED(f"Client fluid model missing texture ref for liquid_{fluid_name}",
                f"expected 'block/fluid/liquid_{fluid_name}' or dynamic ref")

    # Check flowing textures (dynamic construction via baseName + "_flowing" is fine)
    for fluid_name, _ in expected_types:
        flow_str = f"liquid_{fluid_name}_flowing"
        flow_part = "_flowing"
        if flow_str in model_code or (flow_part in model_code and f"liquid_{fluid_name}" in model_code):
            GREEN(f"Client fluid model references flowing texture for {fluid_name}")
        else:
            RED(f"Client fluid model missing flowing texture ref for {fluid_name}",
                f"expected '{flow_str}' or dynamic ref")

    # NEW: Check FluidModel.Unbaked 3rd param (overlay) uses still, NOT flowingMat
    # FluidModel.Unbaked(still, flowingMat, overlay, tint)
    # overlay should be still (the still texture), not flowingMat
    overlay_check = re.findall(r'FluidModel\.Unbaked\((\w+),\s*(\w+),\s*(\w+),', model_code)
    for match in overlay_check:
        still_var, flowing_var, overlay_var = match
        if overlay_var == flowing_var:
            RED(f"Client fluid model: FluidModel.Unbaked overlay={overlay_var} (should be {still_var})",
                f"3rd param of FluidModel.Unbaked should be '{still_var}' (still texture), not '{flowing_var}' (flowing)")
        elif overlay_var == still_var:
            GREEN(f"Client fluid model: FluidModel.Unbaked overlay={overlay_var} (still) ✓")
        else:
            RED(f"Client fluid model: FluidModel.Unbaked overlay={overlay_var} (unexpected)",
                f"expected '{still_var}' (still)")
    if not overlay_check:
        RED("Client fluid model: cannot find FluidModel.Unbaked constructor call",
            "expected new FluidModel.Unbaked(still, flowing, overlay, tint)")
else:
    RED("Client fluid model registration class not found",
        f"expected file in {client_base}")

# ═══════════════════════════════════════════════════════════════════════
# 4. Lang keys — fluid.* must exist in en_us AND zh_cn
# ═══════════════════════════════════════════════════════════════════════
def check_lang(lang_path, lang_name):
    if not os.path.isfile(lang_path):
        RED(f"Lang file {lang_name} not found", f"missing: {lang_path}")
        return
    GREEN(f"Lang file {lang_name} found")
    with open(lang_path, "r", encoding="utf-8") as f:
        try:
            data = json.load(f)
        except json.JSONDecodeError as e:
            RED(f"Lang file {lang_name} is not valid JSON", str(e))
            return
    for fluid_name, _ in expected_types:
        key = f"fluid.kenergyengineering.liquid_{fluid_name}"
        if key in data:
            GREEN(f"{lang_name}: '{key}' = '{data[key]}'")
        else:
            RED(f"{lang_name}: missing '{key}' lang key",
                "add entry for fluid.kenergyengineering." + fluid_name)

lang_en_path = _resolve_lang("en_us.json")
if lang_en_path:
    check_lang(lang_en_path, "en_us.json")
lang_zh_path = _resolve_lang("zh_cn.json")
if lang_zh_path:
    check_lang(lang_zh_path, "zh_cn.json")

# ═══════════════════════════════════════════════════════════════════════
# 5. Texture files exist
# ═══════════════════════════════════════════════════════════════════════
tex_dir = os.path.join(SRC_MAIN, "resources", "assets", "kenergyengineering", "textures", "block", "fluid")
if os.path.isdir(tex_dir):
    GREEN("Fluid textures directory exists")
else:
    RED("Fluid textures directory missing", f"expected: {tex_dir}")

for fluid_name, _ in expected_types:
    still = os.path.join(tex_dir, f"liquid_{fluid_name}.png")
    flowing = os.path.join(tex_dir, f"liquid_{fluid_name}_flowing.png")
    if os.path.isfile(still):
        GREEN(f"Texture: liquid_{fluid_name}.png")
    else:
        RED(f"Texture: missing liquid_{fluid_name}.png")
    if os.path.isfile(flowing):
        GREEN(f"Texture: liquid_{fluid_name}_flowing.png")
    else:
        RED(f"Texture: missing liquid_{fluid_name}_flowing.png")

# ═══════════════════════════════════════════════════════════════════════
# ═══════════════════════════════════════════════════════════════════════
# RESULTS
# ═══════════════════════════════════════════════════════════════════════
print("=" * 60)
print("Fluid Registration Chain Check — RESULTS")
print("=" * 60)
print()

for g in GREENS:
    print(g)
print()
for r in REDS:
    print(r)
print()

if not REDS:
    print("✨ ALL GREEN — fluid registration chain complete!")
    sys.exit(0)
else:
    print(f"❌ {len(REDS)} RED assertion(s) — fix before proceeding")
    sys.exit(1)
