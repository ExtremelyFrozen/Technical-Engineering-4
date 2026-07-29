#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Recipe Matching Policy Check — RED/GREEN gate for FormsCombinedRecipe.

Checks:
  a) General matches() uses occupied >= required (not ==) and excludes ALLOW_ALL from count
  b) A distinct strict method (matchesExactInputs or equally clear name) exists
  c) Only IndfurBlockEntity calls the strict method; all others call general matches()
  d) Based on real recipe data, underfilled refiner and induction furnace recipes are
     recognized by the correct policy
  e) Output padding EMPTY guards still exist (symbolItem/symbolFluid EMPTY,
     canFitOutput early return, giveOutput empty stack protection)

Exit code 0 = GREEN (all checks pass), non-zero = RED (one or more failures).
"""

import json
import os
import re
import sys

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')
    sys.stderr.reconfigure(encoding='utf-8')

PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC_MAIN = os.path.join(PROJECT_ROOT, "src", "main")
SRC_GENERATED = os.path.join(PROJECT_ROOT, "src", "generated")

# ── File helpers ───────────────────────────────────────────────────────

def read_file(path):
    try:
        with open(path, "r", encoding="utf-8") as f:
            return f.read()
    except OSError:
        return None


def read_json(path):
    try:
        with open(path, "r", encoding="utf-8") as f:
            return json.load(f)
    except (json.JSONDecodeError, OSError):
        return None


def walk_recipes(base_dir, recipe_subdir):
    """Walk a recipe subdirectory and yield (rel_path, data) for each .json."""
    for candidate in [
        os.path.join(base_dir, "resources", "data", "kenergyengineering", "recipe", recipe_subdir),
        os.path.join(base_dir, "data", "kenergyengineering", "recipe", recipe_subdir),
    ]:
        if os.path.isdir(candidate):
            recipe_root = candidate
            break
    else:
        return
    for root, _dirs, files in os.walk(recipe_root):
        for f in files:
            if not f.endswith(".json"):
                continue
            path = os.path.join(root, f)
            data = read_json(path)
            if data is not None:
                rel = os.path.relpath(path, recipe_root).replace(os.sep, "/")
                yield rel, data


# ── Java source analysis ──────────────────────────────────────────────

def get_java_source(rel_path):
    path = os.path.join(SRC_MAIN, "java", *rel_path.split("/"))
    return read_file(path)


# ── Check a: general matches() ────────────────────────────────────────

def check_a_general_matches_uses_ge():
    """
    Check that FormsCombinedRecipe.matches() uses occupied < required
    rejection gate (semantic: >=), and that countRequiredItemInputs()
    excludes ALLOW_ALL via !ing.ALLOW_ALL in its body.
    """
    print("[a] General matches() policy (occupied >= required, ALLOW_ALL excluded)")
    source = get_java_source("com/modularmc/ten/api/recipe/FormsCombinedRecipe.java")
    if source is None:
        print("  FAIL: Cannot read FormsCombinedRecipe.java")
        return False

    # ── ALLOW_ALL exclusion: verify the helper body directly ────────
    helper_match = re.search(
        r'private int countRequiredItemInputs\(\)\s*\{([^}]+)\}',
        source
    )
    if not helper_match:
        print("  FAIL: countRequiredItemInputs() helper not found")
        return False
    helper_body = helper_match.group(1)
    if '!ing.ALLOW_ALL' in helper_body:
        print("  OK: countRequiredItemInputs body excludes ALLOW_ALL via !ing.ALLOW_ALL")
    else:
        print("  FAIL: countRequiredItemInputs body missing !ing.ALLOW_ALL")
        return False

    # ── matches body: must use occupied < required, no exact gate ───
    m = re.search(
        r'public boolean matches\(IItemHandler inv,\s*List<\? extends IFluidHandler> tanks,'
        r'\s*FormsCombinedIngredient\.IngredientTypeGetter slotType,'
        r'\s*FormsCombinedIngredient\.IngredientTypeGetter tankType\)\s*\{',
        source
    )
    if not m:
        print("  FAIL: Cannot find matches(IItemHandler, ...) method")
        return False

    body_start = m.end()
    brace_depth = 0
    body_end = body_start
    for i in range(body_start, len(source)):
        if source[i] == '{':
            brace_depth += 1
        elif source[i] == '}':
            brace_depth -= 1
            if brace_depth == -1:
                body_end = i
                break

    body = source[body_start:body_end]

    if re.search(r'occupied\s*<\s*required', body):
        print("  OK: General matches uses occupied < required (semantic: >=)")
    else:
        print("  FAIL: General matches does not use occupied < required")
        return False

    if re.search(r'occupied\s*(==|!=)\s*required', body):
        print("  FAIL: General matches has exact equality gate (occupied ==/!= required)")
        return False
    else:
        print("  OK: No exact equality gate in general matches")

    return True


# ── Check b: strict method exists ─────────────────────────────────────

def check_b_strict_method_exists():
    """Check that a dedicated strict method exists with a clear name."""
    print("[b] Strict method (matchesExactInputs) exists")
    source = get_java_source("com/modularmc/ten/api/recipe/FormsCombinedRecipe.java")
    if source is None:
        print("  FAIL: Cannot read FormsCombinedRecipe.java")
        return False

    # Look for a method named matchesExactInputs or similar
    # Accept matchesExactInputs or matchesExact or matchesStrict
    strict_method = re.search(
        r'public boolean matches(ExactInputs|Exact|Strict)\(IItemHandler inv,',
        source
    )
    if strict_method:
        name = strict_method.group(1)
        print(f"  OK: matches{name} method exists")
        return True
    else:
        print("  FAIL: No strict matching method (matchesExactInputs / matchesExact / matchesStrict) found")
        return False


# ── Check c: caller discipline ────────────────────────────────────────

def check_c_caller_discipline():
    """
    Check that only IndfurBlockEntity calls the strict method,
    and all others call the general matches.
    """
    print("[c] Caller discipline (strict only in IndfurBlockEntity)")

    # Find all BlockEntities that use matches
    be_dir = os.path.join(SRC_MAIN, "java", "com", "modularmc", "ten", "common", "blockentity", "machine")
    if not os.path.isdir(be_dir):
        print("  FAIL: BlockEntity machine directory not found")
        return False

    all_ok = True

    for fname in sorted(os.listdir(be_dir)):
        if not fname.endswith("BlockEntity.java"):
            continue
        path = os.path.join(be_dir, fname)
        content = read_file(path)
        if content is None:
            continue

        # Check for strict method call
        if re.search(r'\.matchesExact(Inputs|Strict)?\(', content):
            if fname == "IndfurBlockEntity.java":
                print(f"  OK: {fname} calls strict method (expected)")
            else:
                print(f"  FAIL: {fname} calls strict method (should use general matches)")
                all_ok = False
        # Check for general matches call
        elif re.search(r'\.matches\(itemHandler', content):
            if fname == "IndfurBlockEntity.java":
                print(f"  FAIL: {fname} calls general matches (should use strict method)")
                all_ok = False
            else:
                print(f"  OK: {fname} calls general matches (expected)")

    return all_ok


# ── Check d: real recipe recognition ──────────────────────────────────

SERIALIZER_CAPACITY = {
    "refiner": 2,
    "induction_furnace": 3,
}

# Regression anchors: known underfilled samples that must exist.
# Format: (label, rel_path_under_recipe_root)
REGRESSION_ANCHORS = {
    "refiner": ("main", "lava.json"),
    "induction_furnace": ("main", "indigo.json"),
}


def check_d_recipe_recognition():
    """
    Verify real refiner and induction furnace recipes against matching policy.
    Asserts:
      1. Each machine type has at least one recipe
      2. Each has at least one underfilled recipe (len(inputs) < sizeIn)
      3. All recipe input counts ≤ their serializer capacity
      4. Regression anchor paths are parseable
    Returns False on empty set, over-capacity, or missing anchors.
    """
    print("[d] Real recipe recognition by matching policy")
    all_ok = True

    for machine_type, size_in in SERIALIZER_CAPACITY.items():
        print(f"\n  --- {machine_type} (sizeIn={size_in}) ---")
        recipes = []
        underfilled = []
        over_capacity = []

        for base_dir, label in [(SRC_MAIN, "main"), (SRC_GENERATED, "generated")]:
            for rel_path, data in walk_recipes(base_dir, machine_type):
                recipes.append((label, rel_path, data))
                inputs = data.get("inputs", [])
                if len(inputs) < size_in:
                    underfilled.append((label, rel_path))
                elif len(inputs) > size_in:
                    over_capacity.append((label, rel_path, len(inputs)))

        # Assert 1: non-empty set
        if len(recipes) == 0:
            print(f"  FAIL: No recipes found for {machine_type}")
            all_ok = False
            continue
        else:
            print(f"  PASS: {len(recipes)} recipe(s) found")

        # Assert 2: at least one underfilled sample
        if len(underfilled) > 0:
            print(f"  PASS: {len(underfilled)} underfilled recipe(s) (inputs < sizeIn={size_in})")
            for label, path in underfilled:
                print(f"    {label}/{machine_type}/{path}")
        else:
            print(f"  FAIL: No underfilled recipes (len(inputs) < {size_in}) — all fill capacity exactly")
            all_ok = False

        # Assert 3: no recipe exceeds capacity
        if len(over_capacity) > 0:
            print(f"  FAIL: {len(over_capacity)} recipe(s) exceed capacity {size_in}:")
            for label, path, count in over_capacity:
                print(f"    {label}/{machine_type}/{path}: {count} inputs > {size_in}")
            all_ok = False
        else:
            print(f"  PASS: No recipe exceeds capacity {size_in}")

        # Assert 4: regression anchor exists and parses
        anchor = REGRESSION_ANCHORS.get(machine_type)
        if anchor:
            anchor_label, anchor_rel = anchor
            found = any(
                label == anchor_label and rel == anchor_rel
                for label, rel, _ in recipes
            )
            if found:
                print(f"  PASS: Regression anchor {anchor_label}/{machine_type}/{anchor_rel} present and parseable")
            else:
                print(f"  FAIL: Regression anchor {anchor_label}/{machine_type}/{anchor_rel} not found")
                all_ok = False

    return all_ok


# ── Check e: output padding EMPTY guards ─────────────────────────────

def check_e_output_guards():
    """Verify that output padding via ALLOW_ALL still has working EMPTY guards."""
    print("[e] Output padding EMPTY guards")

    # Check FormsCombinedIngredient.symbolItem() returns EMPTY for ALLOW_ALL
    ing_source = get_java_source("com/modularmc/ten/api/recipe/FormsCombinedIngredient.java")
    if ing_source is None:
        print("  FAIL: Cannot read FormsCombinedIngredient.java")
        return False

    all_ok = True

    # Check 1: symbolItem() has EMPTY return for ALLOW_ALL
    if re.search(r'ALLOW_ALL.*return.*ItemStack\.EMPTY', ing_source) or \
       re.search(r'itemStacks\(\).*ALLOW_ALL.*ItemStack\.EMPTY', ing_source):
        print("  OK: symbolItem() returns EMPTY for ALLOW_ALL")
    else:
        # Check less rigidly — the function returns itemStacks() which for ALLOW_ALL returns List.of(ItemStack.EMPTY)
        if re.search(r'if \(ALLOW_ALL\) return List\.of\(ItemStack\.EMPTY\)', ing_source):
            print("  OK: itemStacks() returns EMPTY for ALLOW_ALL (used by symbolItem)")
        else:
            print("  FAIL: symbolItem() does not return EMPTY for ALLOW_ALL")
            all_ok = False

    # Check 2: symbolFluid() has EMPTY return for ALLOW_ALL
    if re.search(r'fluidStacks\(\).*ALLOW_ALL.*FluidStack\.EMPTY', ing_source) or \
       re.search(r'if \(ALLOW_ALL\) return List\.of\(FluidStack\.EMPTY\)', ing_source):
        print("  OK: fluidStacks() returns EMPTY for ALLOW_ALL (used by symbolFluid)")
    else:
        print("  FAIL: fluidStacks() does not return EMPTY for ALLOW_ALL")
        all_ok = False

    # Check 3: canFitOutput() has early return for EMPTY stack
    be_source = get_java_source("com/modularmc/ten/api/blockentity/RecipeMachineBlockEntity.java")
    if be_source is None:
        print("  FAIL: Cannot read RecipeMachineBlockEntity.java")
        return False

    if re.search(r'if \(stack\.isEmpty\(\)\) return true', be_source):
        print("  OK: canFitOutput() has early return for EMPTY stack")
    else:
        print("  FAIL: canFitOutput() missing early return for EMPTY stack")
        all_ok = False

    # Check 4: giveOutput() handles empty stack
    if re.search(r'if \(existing\.isEmpty\(\)\)', be_source):
        print("  OK: giveOutput() handles empty stack (existing.isEmpty)")
    else:
        print("  FAIL: giveOutput() missing empty stack protection")
        all_ok = False

    return all_ok


# ── Main ──────────────────────────────────────────────────────────────

def main():
    print("=" * 60)
    print("TEN Recipe Matching Policy Check — RED/GREEN Gate")
    print("=" * 60)

    checks = [
        ("a", "General matches uses >= and excludes ALLOW_ALL", check_a_general_matches_uses_ge),
        ("b", "Strict method matchesExactInputs exists", check_b_strict_method_exists),
        ("c", "Caller discipline — strict only in IndfurBlockEntity", check_c_caller_discipline),
        ("d", "Real recipe recognition — non-empty, underfilled, within capacity, anchors", check_d_recipe_recognition),
        ("e", "Output padding EMPTY guards intact", check_e_output_guards),
    ]

    results = {}
    for label, desc, func in checks:
        print(f"\n{'─' * 50}")
        print(f"CHECK {label}: {desc}")
        print(f"{'─' * 50}")
        try:
            ok = func()
            results[label] = ok
        except Exception as e:
            print(f"  ERROR: {e}")
            results[label] = False

    print("\n" + "=" * 60)
    print("SUMMARY")
    print("=" * 60)
    failed = []
    for label in sorted(results):
        status = "GREEN" if results[label] else "RED"
        desc = next(d for l, d, _ in checks if l == label)
        print(f"  [{status}] Check {label}: {desc}")
        if not results[label]:
            failed.append(label)

    if failed:
        print(f"\nRESULT: RED — {len(failed)} check(s) failed: {', '.join(failed)}")
        print("Expected failures before GREEN implementation:")
        if 'a' in failed:
            print("  - (a) General matches still uses == and does not exclude ALLOW_ALL")
        if 'b' in failed:
            print("  - (b) matchesExactInputs not yet added")
        if 'c' in failed:
            print("  - (c) IndfurBlockEntity still calls general matches instead of strict")
        sys.exit(1)
    else:
        print("\nRESULT: ALL CHECKS PASSED (GREEN)")
        sys.exit(0)


if __name__ == "__main__":
    main()
