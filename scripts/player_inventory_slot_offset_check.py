#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
player_inventory_slot_offset_check.py — TEN player inventory slot offset TDD validation

RED phase (pre-implementation):
  Asserts that addPlayerInventory container uses x=7 and hotbar marginTop=4.
  Fails initially (current x=8, marginTop=5); passes (GREEN) after implementation.

Checks:
  1. addPlayerInventory uses absolute(…, 7, 83, 162, 58) — x=7 (was 8)
  2. addPlayerInventory calls inventory.hotbar.getLayout().marginTop(4.0f)
  3. Main inventory rows have no extra margin/padding changes
  4. Both callers (CmMachineBlockEntity + PipeBlockEntity) still use addPlayerInventory
  5. Machine slot code untouched (machineSlot, upgradeSlot, itemSlot params unchanged)
  6. EMPTY background texture still applied
  7. Container width/height unchanged at 162, 58
  8. No changes to background texture references

Usage:
    python scripts/player_inventory_slot_offset_check.py
    python scripts/player_inventory_slot_offset_check.py --green   # after implementation

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
FACTORY_PATH = os.path.join(PROJECT_ROOT, "src", "main", "java", "com", "modularmc", "ten", "common", "gui", "TENMachineBlockUIFactory.java")
MACHINE_PATH = os.path.join(PROJECT_ROOT, "src", "main", "java", "com", "modularmc", "ten", "api", "blockentity", "CmMachineBlockEntity.java")
PIPE_PATH = os.path.join(PROJECT_ROOT, "src", "main", "java", "com", "modularmc", "ten", "common", "blockentity", "PipeBlockEntity.java")


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
        return True
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()
    ok = re.search(pattern, content) is None
    print(f"  [{'PASS' if ok else 'FAIL'}] {label}: {'absent' if ok else 'FOUND (should not exist)'}")
    return ok


def extract_add_player_inventory(content: str) -> str:
    """Extract the body of addPlayerInventory method."""
    # Match from "public static void addPlayerInventory" to the closing brace
    match = re.search(
        r'public static void addPlayerInventory\s*\(UIElement\s+root\)\s*\{'
        r'(.*?)^\s*\}',
        content, re.MULTILINE | re.DOTALL
    )
    if match:
        return match.group(1)
    return ""


def check_container_position() -> list[str]:
    """Group 1: Container absolute position — x=7"""
    failures = []
    print("\n--- Group 1: Container absolute position ---")

    if not os.path.isfile(FACTORY_PATH):
        failures.append("Factory file not found")
        return failures

    with open(FACTORY_PATH, "r", encoding="utf-8") as f:
        content = f.read()

    method_body = extract_add_player_inventory(content)

    # Check x=7 (not 8)
    if re.search(r'absolute\(\s*new\s+InventorySlots\(\)\s*,\s*7\s*,\s*83\s*,\s*162\s*,\s*58\s*\)', method_body):
        print(f"  [PASS] addPlayerInventory uses absolute(x=7, y=83, w=162, h=58)")
    else:
        print(f"  [FAIL] addPlayerInventory does NOT use x=7, y=83, w=162, h=58")
        failures.append("Container absolute position is not x=7, y=83, w=162, h=58")

    return failures


def check_hotbar_margin() -> list[str]:
    """Group 2: Hotbar marginTop = 4"""
    failures = []
    print("\n--- Group 2: Hotbar marginTop = 4 ---")

    if not os.path.isfile(FACTORY_PATH):
        failures.append("Factory file not found")
        return failures

    with open(FACTORY_PATH, "r", encoding="utf-8") as f:
        content = f.read()

    method_body = extract_add_player_inventory(content)

    # Check hotbar marginTop is set to 4 (or equivalent)
    # Pattern 1: inventory.hotbar.getLayout().marginTop(4.0f)
    # Pattern 2: inventory.hotbar.getLayout().marginTop(4)
    if re.search(r'inventory\.hotbar\.getLayout\(\)\.marginTop\(\s*4\s*(?:\.0f?)?\s*\)', method_body):
        print(f"  [PASS] hotbar marginTop set to 4 via inventory.hotbar.getLayout().marginTop(4)")
    else:
        # Check for any marginTop call targeting hotbar
        if re.search(r'hotbar.*marginTop|marginTop.*hotbar', method_body):
            print(f"  [FAIL] hotbar marginTop found but not exactly 4")
            failures.append("hotbar marginTop target found but value is not 4")
        else:
            print(f"  [FAIL] hotbar marginTop override not found in addPlayerInventory")
            failures.append("hotbar marginTop override missing in addPlayerInventory")

    return failures


def check_main_inventory_unchanged() -> list[str]:
    """Group 3: Main inventory internal margins unchanged"""
    failures = []
    print("\n--- Group 3: Main inventory internal layout unchanged ---")

    with open(FACTORY_PATH, "r", encoding="utf-8") as f:
        content = f.read()

    # The main rows should NOT have any marginTop/marginLeft overrides
    # Check that we don't have marginTop calls on rows[0-2] or similar
    body = extract_add_player_inventory(content)

    suspicious = re.findall(r'(rows\[|rows\s\[|__inventory_main__).*(?:marginTop|marginLeft|marginBottom|marginRight)', body)
    if suspicious:
        print(f"  [FAIL] Main inventory rows have margin overrides: {suspicious}")
        failures.append("Main inventory rows should not have margin overrides")
    else:
        print(f"  [PASS] No margin overrides on main inventory rows")

    # Verify rows[0-2] are not individually targeted
    if re.search(r'rows\s*\[', body):
        print(f"  [FAIL] Main inventory rows[] indexed directly")
        failures.append("Main inventory rows[] should not be directly indexed")
    else:
        print(f"  [PASS] No direct rows[] indexing")

    return failures


def check_callers_still_use_public_method() -> list[str]:
    """Group 4: All callers still use addPlayerInventory"""
    failures = []
    print("\n--- Group 4: All callers still use public addPlayerInventory ---")

    # Check CmMachineBlockEntity
    if os.path.isfile(MACHINE_PATH):
        with open(MACHINE_PATH, "r", encoding="utf-8") as f:
            machine_content = f.read()
        if re.search(r'TENMachineBlockUIFactory\.addPlayerInventory', machine_content):
            print(f"  [PASS] CmMachineBlockEntity calls TENMachineBlockUIFactory.addPlayerInventory()")
        else:
            print(f"  [FAIL] CmMachineBlockEntity does NOT call addPlayerInventory()")
            failures.append("CmMachineBlockEntity missing addPlayerInventory() call")
    else:
        failures.append("CmMachineBlockEntity.java not found")

    # Check PipeBlockEntity
    if os.path.isfile(PIPE_PATH):
        with open(PIPE_PATH, "r", encoding="utf-8") as f:
            pipe_content = f.read()
        if re.search(r'TENMachineBlockUIFactory\.addPlayerInventory', pipe_content):
            print(f"  [PASS] PipeBlockEntity calls TENMachineBlockUIFactory.addPlayerInventory()")
        else:
            print(f"  [FAIL] PipeBlockEntity does NOT call addPlayerInventory()")
            failures.append("PipeBlockEntity missing addPlayerInventory() call")
    else:
        failures.append("PipeBlockEntity.java not found")

    # Verify exactly 2 callers (no inlining, no extra callers)
    all_callers = set()
    for filepath, label in [(FACTORY_PATH, "factory"), (MACHINE_PATH, "machine"), (PIPE_PATH, "pipe")]:
        if os.path.isfile(filepath):
            with open(filepath, "r", encoding="utf-8") as f:
                c = f.read()
            calls = re.findall(r'addPlayerInventory\s*\(', c)
            for _ in calls:
                all_callers.add(label)

    expected = {"factory", "machine", "pipe"}
    if all_callers == expected:
        print(f"  [PASS] addPlayerInventory defined in factory, called from machine + pipe ({len(all_callers) - 1} callers)")
    else:
        print(f"  [WARN] addPlayerInventory callers: {all_callers} (expected {expected})")

    return failures


def check_machine_slots_untouched() -> list[str]:
    """Group 5: Machine slot code unchanged"""
    failures = []
    print("\n--- Group 5: Machine slot code unchanged ---")

    with open(FACTORY_PATH, "r", encoding="utf-8") as f:
        content = f.read()
    factory_body = content

    # Check machineSlot method still uses the same signature
    if re.search(r'public static ItemSlot machineSlot\(CmMachineBlockEntity\s+machine,\s*int\s+index,\s*int\s+x,\s*int\s+y\)', factory_body):
        print(f"  [PASS] machineSlot signature unchanged")
    else:
        print(f"  [FAIL] machineSlot signature changed")
        failures.append("machineSlot signature changed")

    # Check upgradeSlot still uses the same signature
    if re.search(r'private static ItemSlot upgradeSlot\(CmMachineBlockEntity\s+machine,\s*int\s+index,\s*int\s+x,\s*int\s+y\)', factory_body):
        print(f"  [PASS] upgradeSlot signature unchanged")
    else:
        print(f"  [FAIL] upgradeSlot signature changed")
        failures.append("upgradeSlot signature changed")

    # Check itemSlot still uses the same signature
    if re.search(r'private static ItemSlot itemSlot\(Slot\s+slot,\s*int\s+x,\s*int\s+y,\s*boolean\s+isPlayerSlot\)', factory_body):
        print(f"  [PASS] itemSlot signature unchanged")
    else:
        print(f"  [FAIL] itemSlot signature changed")
        failures.append("itemSlot signature changed")

    # Check that x/y parameters in machineSlot/upgradeSlot are NOT modified through
    # (they take x,y as parameters and pass them to absolute(new ItemSlot(...), x, y, 18, 18))
    if re.search(r'private static ItemSlot itemSlot\b', factory_body):
        # extract itemSlot body
        item_match = re.search(
            r'private static ItemSlot itemSlot\(Slot slot,\s*int x,\s*int y,\s*boolean isPlayerSlot\).*?\{'
            r'(.*?)^\s+\}',
            factory_body, re.MULTILINE | re.DOTALL
        )
        if item_match:
            item_body = item_match.group(1)
            if re.search(r'absolute\(\s*new\s+ItemSlot\(slot\)\s*,\s*x\s*,\s*y\s*,\s*18\s*,\s*18\s*\)', item_body):
                print(f"  [PASS] itemSlot passes x,y directly to absolute(...)")
            else:
                print(f"  [FAIL] itemSlot does NOT pass x,y directly")
                failures.append("itemSlot no longer passes x,y directly")

    return failures


def check_background_texture() -> list[str]:
    """Group 6: Background textures unchanged"""
    failures = []
    print("\n--- Group 6: Background textures ---")

    with open(FACTORY_PATH, "r", encoding="utf-8") as f:
        content = f.read()

    body = extract_add_player_inventory(content)

    # EMPTY background still used
    if re.search(r'backgroundTexture\(\s*IGuiTexture\.EMPTY\s*\)', body):
        print(f"  [PASS] EMPTY background texture applied to player slots")
    else:
        print(f"  [FAIL] EMPTY background texture NOT found for player slots")
        failures.append("EMPTY background texture removed from player slots")

    # slotOverlay still EMPTY
    if re.search(r'slotOverlay\(\s*IGuiTexture\.EMPTY\s*\)', body):
        print(f"  [PASS] EMPTY slot overlay applied to player slots")
    else:
        print(f"  [FAIL] EMPTY slot overlay NOT found for player slots")
        failures.append("EMPTY slot overlay removed from player slots")

    # showSlotOverlayOnlyEmpty(false) still present
    if re.search(r'showSlotOverlayOnlyEmpty\(\s*false\s*\)', body):
        print(f"  [PASS] showSlotOverlayOnlyEmpty(false) still configured")
    else:
        print(f"  [FAIL] showSlotOverlayOnlyEmpty(false) missing")
        failures.append("showSlotOverlayOnlyEmpty(false) missing")

    return failures


def check_container_dimensions() -> list[str]:
    """Group 7: Container width/height unchanged"""
    failures = []
    print("\n--- Group 7: Container dimensions ---")

    with open(FACTORY_PATH, "r", encoding="utf-8") as f:
        content = f.read()

    body = extract_add_player_inventory(content)

    # Width still 162
    if re.search(r'absolute\(\s*new\s+InventorySlots\(\)\s*,\s*\d+\s*,\s*\d+\s*,\s*162\s*,\s*\d+\s*\)', body):
        print(f"  [PASS] Container width unchanged: 162")
    else:
        print(f"  [FAIL] Container width changed from 162")
        failures.append("Container width changed")

    # Height still 58
    if re.search(r'absolute\(\s*new\s+InventorySlots\(\)\s*,\s*\d+\s*,\s*\d+\s*,\s*\d+\s*,\s*58\s*\)', body):
        print(f"  [PASS] Container height unchanged: 58")
    else:
        print(f"  [FAIL] Container height changed from 58")

    return failures


# ---- MAIN ----
def main():
    is_green = "--green" in sys.argv

    print("=" * 65)
    print(f"  TEN Player Inventory Slot Offset — {'GREEN' if is_green else 'RED'} Phase Validation")
    print("=" * 65)

    all_failures = []

    all_failures.extend(check_container_position())
    all_failures.extend(check_hotbar_margin())
    all_failures.extend(check_main_inventory_unchanged())
    all_failures.extend(check_callers_still_use_public_method())
    all_failures.extend(check_machine_slots_untouched())
    all_failures.extend(check_background_texture())
    all_failures.extend(check_container_dimensions())

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
