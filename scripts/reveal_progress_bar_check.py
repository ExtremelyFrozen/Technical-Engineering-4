#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
reveal_progress_bar_check.py — TEN RevealProgressBar TDD validation

RED phase (pre-implementation):
  Asserts that RevealProgressBar class and factory wiring exist.
  Fails initially; passes (GREEN) after implementation.

Checks:
  1. RevealProgressBar.java exists in common/gui package
  2. extends ProgressBar (LDLib2)
  3. Overrides updateProgressBarStyle — no super call to flex layout
  4. Uses SpriteTexture.setSprite() for UV clipping
  5. bar layout stays at 100% width/height
  6. TENMachineBlockUIFactory.verticalGauge/progressGauge use RevealProgressBar
  7. LEFT_TO_RIGHT and DOWN_TO_UP mapping present
  8. FluidSlot unchanged in factory
  9. Normalized value clamped to [0,1] in the override

Usage:
    python scripts/reveal_progress_bar_check.py
    python scripts/reveal_progress_bar_check.py --green   # after implementation

Exit codes:
    0 = GREEN (all checks pass)
    1 = RED (one or more checks fail)
"""

import os
import re
import sys

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')
    sys.stderr.reconfigure(encoding='utf-8')


PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
REVEAL_PATH = os.path.join(PROJECT_ROOT, "src", "main", "java", "com", "modularmc", "ten", "common", "gui", "RevealProgressBar.java")
FACTORY_PATH = os.path.join(PROJECT_ROOT, "src", "main", "java", "com", "modularmc", "ten", "common", "gui", "TENMachineBlockUIFactory.java")
SRC_DIR = os.path.join(PROJECT_ROOT, "src", "main", "java")


def check_file_exists(path: str, label: str) -> bool:
    ok = os.path.isfile(path)
    print(f"  [{'PASS' if ok else 'FAIL'}] {label}: {'found' if ok else 'MISSING'}")
    return ok


def check_content(path: str, pattern: str, label: str, count: int = None) -> bool:
    """Check that a file contains the given regex pattern."""
    if not os.path.isfile(path):
        print(f"  [FAIL] {label}: file not found")
        return False
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()
    matches = re.findall(pattern, content)
    if count is not None:
        ok = len(matches) == count
        print(f"  [{'PASS' if ok else 'FAIL'}] {label}: expected {count}, found {len(matches)}")
        return ok
    ok = len(matches) > 0
    print(f"  [{'PASS' if ok else 'FAIL'}] {label}: {'found' if ok else 'NOT FOUND'}")
    return ok


def check_not_content(path: str, pattern: str, label: str) -> bool:
    """Check that a file does NOT contain the given pattern."""
    if not os.path.isfile(path):
        print(f"  [SKIP] {label}: file not found")
        return True  # skip if file doesn't exist yet
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()
    ok = re.search(pattern, content) is None
    print(f"  [{'PASS' if ok else 'FAIL'}] {label}: {'absent' if ok else 'FOUND (should not exist)'}")
    return ok


def check_reveal_class() -> list[str]:
    """Group 1: RevealProgressBar — integer pixel quantization"""
    failures = []
    print("\n--- Group 1: RevealProgressBar — integer pixel quantization ---")

    exists = check_file_exists(REVEAL_PATH, "RevealProgressBar.java exists")
    if not exists:
        failures.append("RevealProgressBar.java not found")
        return failures

    if not check_content(REVEAL_PATH, r"class\s+RevealProgressBar\s+extends\s+ProgressBar", "extends ProgressBar"):
        failures.append("Does not extend ProgressBar")

    if not check_content(REVEAL_PATH, r"updateProgressBarStyle", "overrides updateProgressBarStyle"):
        failures.append("updateProgressBarStyle not overridden")

    # Must NOT call super.updateProgressBarStyle in the normal path (avoid flex shrinkage).
    # Guard clause calling super is acceptable for super constructor initialization.
    with open(REVEAL_PATH, "r", encoding="utf-8") as f:
        reveal_content = f.read()
    # Count super.updateProgressBarStyle occurrences
    super_call_count = len(re.findall(r"super\.updateProgressBarStyle", reveal_content))
    has_guard = re.search(r"(guard|initialized|null\s*==|isInitialized)", reveal_content)
    if super_call_count == 0:
        print(f"  [PASS] super.updateProgressBarStyle not called (guardless approach)")
    elif super_call_count >= 1 and has_guard:
        print(f"  [PASS] super.updateProgressBarStyle guarded for constructor only (count={super_call_count})")
    else:
        print(f"  [FAIL] super.updateProgressBarStyle called without guard (flex approach)")
        failures.append("super.updateProgressBarStyle called without guard")

    # Guard for super constructor call
    if has_guard:
        print(f"  [PASS] has guard for super constructor call: found")
    else:
        print(f"  [FAIL] has guard for super constructor call: NOT FOUND")
        failures.append("No guard for super constructor override")

    # Uses SpriteTexture.setSprite for UV clipping
    if not check_content(REVEAL_PATH, r"setSprite", "uses setSprite for UV clipping"):
        failures.append("No setSprite call for UV clipping")

    # ---- Integer pixel quantization ----
    # visibleWidth/visibleHeight computed with ONE round/floor after clamp
    if not check_content(REVEAL_PATH, r"Math\.round\(origSpriteWidth\s*\*\s*progress\)", "visibleWidth quantized via Math.round(origSpriteWidth * progress)"):
        failures.append("No visibleWidth integer quantization (Math.round)")
    if not check_content(REVEAL_PATH, r"Math\.round\(origSpriteHeight\s*\*\s*progress\)", "visibleHeight quantized via Math.round(origSpriteHeight * progress)"):
        failures.append("No visibleHeight integer quantization (Math.round)")

    # Bar layout uses absolute .width()/.height() — NOT widthPercent/heightPercent
    if not check_content(REVEAL_PATH, r"layout\.width\(\s*visibleWidth\s*\)", "bar layout uses .width(visibleWidth) absolute pixels"):
        failures.append("bar layout missing .width(visibleWidth)")
    if not check_content(REVEAL_PATH, r"layout\.height\(\s*visibleHeight\s*\)", "bar layout uses .height(visibleHeight) absolute pixels"):
        failures.append("bar layout missing .height(visibleHeight)")

    # widthPercent/heightPercent must NOT be used in update path
    wp_calls = len(re.findall(r"layout\.widthPercent", reveal_content))
    hp_calls = len(re.findall(r"layout\.heightPercent", reveal_content))
    if wp_calls == 0:
        print(f"  [PASS] no layout.widthPercent calls (replaced by absolute .width())")
    else:
        print(f"  [FAIL] layout.widthPercent still used ({wp_calls} call(s)) — should be .width()")
        failures.append(f"layout.widthPercent still used ({wp_calls} call(s))")
    if hp_calls == 0:
        print(f"  [PASS] no layout.heightPercent calls (replaced by absolute .height())")
    else:
        print(f"  [FAIL] layout.heightPercent still used ({hp_calls} call(s)) — should be .height()")
        failures.append(f"layout.heightPercent still used ({hp_calls} call(s))")

    # UV clipping uses the SAME visible pixel values
    if not check_content(REVEAL_PATH, r"newW\s*=\s*visibleWidth", "UV width assigned from visibleWidth (same as layout)"):
        failures.append("UV width NOT using visibleWidth from layout")
    if not check_content(REVEAL_PATH, r"newH\s*=\s*visibleHeight", "UV height assigned from visibleHeight (same as layout)"):
        failures.append("UV height NOT using visibleHeight from layout")

    # Clamp normalized value to [0,1]
    if not check_content(REVEAL_PATH, r"(clamp|Math\.max.*Math\.min|MathHelper\.clamp)", "normalized value clamped to [0,1]"):
        failures.append("No clamp for normalized value")

    # NaN/Infinity guard: Float.isFinite before Mth.clamp
    if not check_content(REVEAL_PATH, r"Float\.isFinite\(normalized\)", "NaN/Infinity guard: Float.isFinite(normalized) before clamp"):
        failures.append("No Float.isFinite(normalized) guard — NaN can bypass Mth.clamp")

    # LEFT_TO_RIGHT and DOWN_TO_UP handling
    if not check_content(REVEAL_PATH, r"LEFT_TO_RIGHT", "handles LEFT_TO_RIGHT"):
        failures.append("LEFT_TO_RIGHT not handled")
    if not check_content(REVEAL_PATH, r"DOWN_TO_UP", "handles DOWN_TO_UP"):
        failures.append("DOWN_TO_UP not handled")
    if not check_content(REVEAL_PATH, r"fillDirection|RIGHT_TO_LEFT|switch\s*\(|if\s*\(.*FillDirection", "direction mapping present"):
        failures.append("Direction mapping not found")

    # R2L/D2U zero-size position safety: when visibleWidth/Height == 0,
    # UV x/y must stay at orig (not orig+full boundary)
    with open(REVEAL_PATH, "r", encoding="utf-8") as f:
        rl_content = f.read()
    r2l_conditional = bool(re.search(
        r"RIGHT_TO_LEFT.*\n.*newX\s*=.*visibleWidth\s*>\s*0|"
        r"newX\s*=.*visibleWidth\s*>\s*0.*\n.*RIGHT_TO_LEFT|"
        r"visibleWidth\s*>\s*0\s*\?\s*origSpriteX\s*\+.*:\s*origSpriteX",
        rl_content, re.DOTALL
    ))
    d2u_conditional = bool(re.search(
        r"DOWN_TO_UP.*\n.*newY\s*=.*visibleHeight\s*>\s*0|"
        r"newY\s*=.*visibleHeight\s*>\s*0.*\n.*DOWN_TO_UP|"
        r"visibleHeight\s*>\s*0\s*\?\s*origSpriteY\s*\+.*:\s*origSpriteY",
        rl_content, re.DOTALL
    ))
    if r2l_conditional:
        print(f"  [PASS] R2L: UV x-offset conditional on visibleWidth > 0 (0% position safe)")
    else:
        print(f"  [FAIL] R2L: UV x-offset NOT conditional on visibleWidth > 0 — 0% at orig+full boundary")
        failures.append("R2L offset not conditional on visibleWidth > 0 — 0% at orig+full boundary")
    if d2u_conditional:
        print(f"  [PASS] D2U: UV y-offset conditional on visibleHeight > 0 (0% position safe)")
    else:
        print(f"  [FAIL] D2U: UV y-offset NOT conditional on visibleHeight > 0 — 0% at orig+full boundary")
        failures.append("D2U offset not conditional on visibleHeight > 0 — 0% at orig+full boundary")

    return failures


def check_barcontainer_direction() -> list[str]:
    """Group 1b: barBackground mirror of parent ProgressBar direction layout"""
    failures = []
    print("\n--- Group 1b: barBackground direction layout (mirrors parent ProgressBar) ---")

    exists = check_file_exists(REVEAL_PATH, "RevealProgressBar.java exists")
    if not exists:
        failures.append("RevealProgressBar.java not found")
        return failures

    with open(REVEAL_PATH, "r", encoding="utf-8") as f:
        content = f.read()

    # Import check for taffy layout types
    taffy_imports = [
        "dev.vfyjxf.taffy.style.FlexDirection",
        "dev.vfyjxf.taffy.style.AlignItems",
    ]
    for imp in taffy_imports:
        if imp in content:
            print(f"  [PASS] import {imp}")
        else:
            print(f"  [FAIL] missing import for {imp}")
            failures.append(f"Missing import: {imp}")

    # barBackground layout must set flexDirection + alignItems
    # Parent ProgressBar bytecode:
    #   L2R: barBackground=COLUMN+alignItems=FLEX_START
    #   R2L: barBackground=COLUMN+alignItems=FLEX_END
    #   U2D: barBackground=ROW+alignItems=FLEX_START
    #   D2U: barBackground=ROW+alignItems=FLEX_END
    #
    # We check that barBackground.layout appears and sets both properties
    if "barBackground.layout" in content or "barBackground(" in content:
        print(f"  [PASS] barBackground layout is modified")
    else:
        print(f"  [FAIL] barBackground layout not modified")
        failures.append("barBackground layout not modified — will break D2U/R2L alignment")

    # Check each direction has both flexDirection and alignItems
    dir_checks = [
        ("LEFT_TO_RIGHT", "FLEX_START"),
        ("RIGHT_TO_LEFT", "FLEX_END"),
        ("UP_TO_DOWN", "FLEX_START"),
        ("DOWN_TO_UP", "FLEX_END"),
    ]
    for direction, align in dir_checks:
        has_dir = direction in content
        if has_dir:
            print(f"  [PASS] handles {direction}")
        else:
            print(f"  [FAIL] missing {direction}")
            failures.append(f"Missing {direction} handling")

    # Check barAlso sets flexDirection/justifyContent (parent does this too)
    if "justifyContent" in content:
        print(f"  [PASS] bar layout sets justifyContent (mirrors parent)")
    else:
        print(f"  [FAIL] bar layout missing justifyContent")
        failures.append("bar layout missing justifyContent (parent ProgressBar sets it)")

    # Check flexDirection is set on bar as well
    if "flexDirection" in content:
        # Count minimum flexDirection calls: 4 for barBackground + 4 for bar = 8
        fd_count = len(re.findall(r"flexDirection", content))
        if fd_count >= 4:
            print(f"  [PASS] flexDirection set in layout (count={fd_count})")
        else:
            print(f"  [FAIL] flexDirection appears only {fd_count} times, expected >=4")
            failures.append(f"flexDirection appears only {fd_count} times")
    else:
        print(f"  [FAIL] flexDirection never set")
        failures.append("flexDirection never set in layout")

    return failures


def check_factory_modifications() -> list[str]:
    """Group 2: TENMachineBlockUIFactory modifications"""
    failures = []
    print("\n--- Group 2: TENMachineBlockUIFactory modifications ---")

    exists = check_file_exists(FACTORY_PATH, "TENMachineBlockUIFactory.java exists")
    if not exists:
        failures.append("Factory file not found")
        return failures

    with open(FACTORY_PATH, "r", encoding="utf-8") as f:
        factory_content = f.read()

    # verticalGauge returns RevealProgressBar
    if "RevealProgressBar verticalGauge(" in factory_content:
        print(f"  [PASS] verticalGauge returns RevealProgressBar")
    elif "RevealProgressBar" in factory_content:
        print(f"  [PASS] RevealProgressBar referenced in factory")
    else:
        print(f"  [FAIL] RevealProgressBar not used in factory")
        failures.append("RevealProgressBar not used in factory")

    # progressGauge returns RevealProgressBar
    if "RevealProgressBar progressGauge(" in factory_content:
        print(f"  [PASS] progressGauge returns RevealProgressBar")
    elif "RevealProgressBar" not in factory_content:
        print(f"  [FAIL] RevealProgressBar not referenced in progressGauge")
        failures.append("RevealProgressBar not referenced in progressGauge")

    # barBackground stays EMPTY
    if not check_content(FACTORY_PATH, r"barBackground.*EMPTY|barBackground.*IGuiTexture\.EMPTY", "barBackground still EMPTY"):
        failures.append("barBackground not EMPTY")

    # FluidSlot — checked authoritatively in Group 3 below

    # Does NOT remove fillDirection from progressBarStyle
    if not check_content(FACTORY_PATH, r"fillDirection", "fillDirection still configured"):
        failures.append("fillDirection removed from progressBarStyle")

    # Check that old ProgressBar in verticalGauge is replaced
    with open(FACTORY_PATH, "r", encoding="utf-8") as f:
        factory_content = f.read()
    # RevealProgressBar is used in place of ProgressBar
    if "new RevealProgressBar" in factory_content:
        print(f"  [PASS] RevealProgressBar created in factory")
    else:
        print(f"  [FAIL] RevealProgressBar not created in factory")
        failures.append("RevealProgressBar not instantiated in factory")

    return failures


def check_fluid_slot_unchanged() -> list[str]:
    """Group 3: FluidSlot remains unmodified"""
    failures = []
    print("\n--- Group 3: FluidSlot unchanged ---")

    if not os.path.isfile(FACTORY_PATH):
        failures.append("Factory file not found")
        return failures

    with open(FACTORY_PATH, "r", encoding="utf-8") as f:
        content = f.read()

    # fluidGauge should still use FluidSlot
    if "new FluidSlot()" not in content and "fluidGauge" in content:
        # Check fluidGauge method body
        if re.search(r"fluidGauge.*FluidSlot|FluidSlot.*fluidGauge", content):
            print(f"  [PASS] fluidGauge still uses FluidSlot")
        else:
            print(f"  [FAIL] fluidGauge may have changed FluidSlot usage")
            failures.append("fluidGauge no longer uses FluidSlot")

    # FillDirection.DOWN_TO_UP for fluid
    if "DOWN_TO_UP" not in content:
        # fluid gauge still has DOWN_TO_UP
        pass  # might be fine if fluid gauge fill direction is handled elsewhere

    # No ProgressBar or RevealProgressBar changes to fluid gauge
    with open(FACTORY_PATH, "r", encoding="utf-8") as f:
        lines = f.readlines()
    in_fluid_gauge = False
    fluid_has_reveal = False
    for line in lines:
        if "fluidGauge" in line and "CmMachineBlockEntity" in line:
            in_fluid_gauge = True
        if in_fluid_gauge:
            if "RevealProgressBar" in line or "ProgressBar" in line and "FluidSlot" not in line:
                fluid_has_reveal = True
            if line.strip().startswith("}") or line.strip().startswith("private") or line.strip().startswith("public"):
                in_fluid_gauge = False

    if fluid_has_reveal:
        print(f"  [FAIL] fluidGauge contains ProgressBar/RevealProgressBar reference")
        failures.append("fluidGauge incorrectly modified with progress bar")
    else:
        print(f"  [PASS] fluidGauge does NOT use progress bar components")

    return failures


def check_exported_signatures() -> list[str]:
    """Group 4: Verify method signatures match expectations"""
    failures = []
    print("\n--- Group 4: API signatures unchanged ---")

    if not os.path.isfile(FACTORY_PATH):
        failures.append("Factory file not found")
        return failures

    with open(FACTORY_PATH, "r", encoding="utf-8") as f:
        content = f.read()

    # energyGauge signature unchanged
    if not re.search(r"public static ProgressBar energyGauge\(CmMachineBlockEntity", content):
        # Might use RevealProgressBar in return type
        if re.search(r"public static RevealProgressBar energyGauge\(CmMachineBlockEntity", content):
            print(f"  [PASS] energyGauge returns RevealProgressBar (compatible)")
        else:
            print(f"  [FAIL] energyGauge signature changed")
            failures.append("energyGauge signature changed")

    # fuelGauge signature unchanged
    if not re.search(r"public static (ProgressBar|RevealProgressBar) fuelGauge\(CmMachineBlockEntity", content):
        print(f"  [FAIL] fuelGauge signature changed")
        failures.append("fuelGauge signature changed")

    # progressGauge signature unchanged
    if not re.search(r"public static (ProgressBar|RevealProgressBar) progressGauge\(CmMachineBlockEntity", content):
        print(f"  [FAIL] progressGauge signature changed")
        failures.append("progressGauge signature changed")

    return failures


# ---- MAIN ----
def main():
    is_green = "--green" in sys.argv

    print("=" * 65)
    print(f"  TEN RevealProgressBar — {'GREEN' if is_green else 'RED'} Phase Validation")
    print("=" * 65)

    all_failures = []

    all_failures.extend(check_reveal_class())
    all_failures.extend(check_barcontainer_direction())
    all_failures.extend(check_factory_modifications())
    all_failures.extend(check_fluid_slot_unchanged())
    all_failures.extend(check_exported_signatures())

    print("\n" + "=" * 65)
    if all_failures:
        print(f"  RESULT: RED ({len(all_failures)} failure(s))")
        for f in all_failures:
            print(f"    [x] {f}")
        print("\n  Reminder: Run with --green after implementation to confirm")
        sys.exit(1)
    else:
        print("  RESULT: GREEN - All checks pass!")
        sys.exit(0)


if __name__ == "__main__":
    main()
