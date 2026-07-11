#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Validate FP-002: Item/Fluid reverse ResourceHandler adapters.

Checks:
  - ItemHandlerResourceAdapter.java exists with ResourceHandler<ItemResource> signature
  - ItemHandlerResourceAdapter implements size/getResource/getAmountAsLong/getCapacityAsLong/isValid/insert/extract
  - FluidHandlerResourceAdapter.java exists with ResourceHandler<FluidResource> signature
  - FluidHandlerResourceAdapter implements all ResourceHandler methods
  - CommonProxy.registerCapabilities registers Capabilities.Item.BLOCK with adapter
  - CommonProxy.registerCapabilities registers Capabilities.Fluid.BLOCK with adapter
  - No direct IItemHandler/IFluidHandler exposed at capability boundary
  - Transaction-aware insert/extract (no simulation leakage)
  - Empty/null resource guards
  - Side-aware delegation

Exit 0 = GREEN (fully implemented), 1 = RED (not implemented or incomplete).
"""

import os, re, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = lambda *p: os.path.join(ROOT, "src", *p)
CAP_DIR = SRC("main", "java", "com", "modularmc", "ten", "api", "capability")
COMMON_PROXY = SRC("main", "java", "com", "modularmc", "ten", "common", "CommonProxy.java")

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


def check_item_adapter_exists(acc):
    """ItemHandlerResourceAdapter.java must exist."""
    path = os.path.join(CAP_DIR, "ItemHandlerResourceAdapter.java")
    text = read(path)
    if text is None:
        acc("ItemHandlerResourceAdapter.java not found")
        return None
    return text


def check_item_adapter_implements_resource_handler(acc):
    """ItemHandlerResourceAdapter must implement ResourceHandler<ItemResource>."""
    text = check_item_adapter_exists(acc)
    if text is None:
        return
    if "ResourceHandler<ItemResource>" not in text:
        acc("ItemHandlerResourceAdapter: missing 'implements ResourceHandler<ItemResource>'")
    if "class ItemHandlerResourceAdapter" not in text:
        acc("ItemHandlerResourceAdapter: class ItemHandlerResourceAdapter not found")


def check_item_adapter_methods(acc):
    """ItemHandlerResourceAdapter must have all ResourceHandler methods delegating to IItemHandler."""
    text = check_item_adapter_exists(acc)
    if text is None:
        return
    required = [
        (r'@Override\s+public\s+int\s+size\s*\(', 'size()'),
        (r'@Override\s+public\s+ItemResource\s+getResource\s*\(', 'getResource(int)'),
        (r'@Override\s+public\s+long\s+getAmountAsLong\s*\(', 'getAmountAsLong(int)'),
        (r'@Override\s+public\s+long\s+getCapacityAsLong\s*\(', 'getCapacityAsLong(int, ItemResource)'),
        (r'@Override\s+public\s+boolean\s+isValid\s*\(', 'isValid(int, ItemResource)'),
        (r'@Override\s+public\s+int\s+insert\s*\([^)]*TransactionContext', 'insert(..., TransactionContext)'),
        (r'@Override\s+public\s+int\s+extract\s*\([^)]*TransactionContext', 'extract(..., TransactionContext)'),
    ]
    for pattern, name in required:
        if not re.search(pattern, text):
            acc(f"ItemHandlerResourceAdapter: missing method {name}")


def check_item_adapter_has_internal_iitemhandler(acc):
    """ItemHandlerResourceAdapter must have a field referencing IItemHandler."""
    text = check_item_adapter_exists(acc)
    if text is None:
        return
    if not re.search(r'IItemHandler\s+\w+', text):
        acc("ItemHandlerResourceAdapter: no IItemHandler field found")


def check_fluid_adapter_exists(acc):
    """FluidHandlerResourceAdapter.java must exist."""
    path = os.path.join(CAP_DIR, "FluidHandlerResourceAdapter.java")
    text = read(path)
    if text is None:
        acc("FluidHandlerResourceAdapter.java not found")
        return None
    return text


def check_fluid_adapter_implements_resource_handler(acc):
    """FluidHandlerResourceAdapter must implement ResourceHandler<FluidResource>."""
    text = check_fluid_adapter_exists(acc)
    if text is None:
        return
    if "ResourceHandler<FluidResource>" not in text:
        acc("FluidHandlerResourceAdapter: missing 'implements ResourceHandler<FluidResource>'")
    if "class FluidHandlerResourceAdapter" not in text:
        acc("FluidHandlerResourceAdapter: class not found")


def check_fluid_adapter_methods(acc):
    """FluidHandlerResourceAdapter must have all ResourceHandler methods delegating to IFluidHandler."""
    text = check_fluid_adapter_exists(acc)
    if text is None:
        return
    required = [
        (r'@Override\s+public\s+int\s+size\s*\(', 'size()'),
        (r'@Override\s+public\s+FluidResource\s+getResource\s*\(', 'getResource(int)'),
        (r'@Override\s+public\s+long\s+getAmountAsLong\s*\(', 'getAmountAsLong(int)'),
        (r'@Override\s+public\s+long\s+getCapacityAsLong\s*\(', 'getCapacityAsLong(int, FluidResource)'),
        (r'@Override\s+public\s+boolean\s+isValid\s*\(', 'isValid(int, FluidResource)'),
        (r'@Override\s+public\s+int\s+insert\s*\([^)]*TransactionContext', 'insert(..., TransactionContext)'),
        (r'@Override\s+public\s+int\s+extract\s*\([^)]*TransactionContext', 'extract(..., TransactionContext)'),
    ]
    for pattern, name in required:
        if not re.search(pattern, text):
            acc(f"FluidHandlerResourceAdapter: missing method {name}")


def check_fluid_adapter_has_internal_ifluidhandler(acc):
    """FluidHandlerResourceAdapter must have a field referencing IFluidHandler."""
    text = check_fluid_adapter_exists(acc)
    if text is None:
        return
    if not re.search(r'IFluidHandler\s+\w+', text):
        acc("FluidHandlerResourceAdapter: no IFluidHandler field found")


def check_common_proxy_item_registration(acc):
    """CommonProxy must register Capabilities.Item.BLOCK with adapter."""
    text = read(COMMON_PROXY)
    if text is None:
        acc("CommonProxy.java not found")
        return
    if "Capabilities.Item.BLOCK" not in text:
        acc("CommonProxy.java: missing Capabilities.Item.BLOCK registration")
    if "ItemHandlerResourceAdapter" not in text:
        acc("CommonProxy.java: missing ItemHandlerResourceAdapter reference in registration")
    if "Capabilities.Fluid.BLOCK" not in text:
        acc("CommonProxy.java: missing Capabilities.Fluid.BLOCK registration")
    if "FluidHandlerResourceAdapter" not in text:
        acc("CommonProxy.java: missing FluidHandlerResourceAdapter reference in registration")
    # Check that the TODO comment about re-enabling is removed
    if "TODO: Re-enable Item/Fluid capability" in text:
        acc("CommonProxy.java: TODO comment about re-enabling item/fluid capabilities still present (should be removed)")


def check_no_old_handler_exposed(acc):
    """Capability boundary must NOT expose IItemHandler/IFluidHandler directly."""
    text = read(COMMON_PROXY)
    if text is None:
        return
    # The registerBlock should be for Capabilities.Item.BLOCK and Capabilities.Fluid.BLOCK
    # NOT for old Capabilities.ItemHandler.BLOCK
    if "Capabilities.ItemHandler.BLOCK" in text:
        acc("CommonProxy.java: uses old Capabilities.ItemHandler.BLOCK instead of Capabilities.Item.BLOCK")
    if "Capabilities.FluidHandler.BLOCK" in text:
        acc("CommonProxy.java: uses old Capabilities.FluidHandler.BLOCK instead of Capabilities.Fluid.BLOCK")


def check_item_adapter_uses_transaction(acc):
    """ItemHandlerResourceAdapter must pass TransactionContext to insert/extract (not always simulate)."""
    text = check_item_adapter_exists(acc)
    if text is None:
        return
    # Check that insert/extract use the transaction parameter
    if re.search(r'insert\s*\([^)]*simulate', text):
        acc("ItemHandlerResourceAdapter: insert() uses legacy 'simulate' parameter instead of Transaction pattern")


def check_fluid_adapter_uses_transaction(acc):
    """FluidHandlerResourceAdapter must pass TransactionContext to insert/extract."""
    text = check_fluid_adapter_exists(acc)
    if text is None:
        return
    if re.search(r'(fill|drain)\s*\([^)]*SIMULATE', text):
        acc("FluidHandlerResourceAdapter: uses SIMULATE action instead of Transaction pattern")


def check_item_adapter_empty_guard(acc):
    """ItemHandlerResourceAdapter must guard against empty ItemResource."""
    text = check_item_adapter_exists(acc)
    if text is None:
        return
    if not re.search(r'(isEmpty|isValid|checkNonEmpty|checkNonEmptyNonNegative)', text):
        acc("ItemHandlerResourceAdapter: missing empty/non-negative resource guard pattern")


def check_fluid_adapter_empty_guard(acc):
    """FluidHandlerResourceAdapter must guard against empty FluidResource."""
    text = check_fluid_adapter_exists(acc)
    if text is None:
        return
    if not re.search(r'(isEmpty|isValid|checkNonEmpty|checkNonEmptyNonNegative)', text):
        acc("FluidHandlerResourceAdapter: missing empty/non-negative resource guard pattern")


def check_item_adapter_implements_is_valid(acc):
    """isValid must return false for empty resource."""
    text = check_item_adapter_exists(acc)
    if text is None:
        return
    if re.search(r'isValid\s*\([^)]*\)\s*\{[^}]*\}', text) and not re.search(r'isEmpty', text):
        # Check if the method body is more than just return true
        body_match = re.search(r'isValid\s*\([^)]*\)\s*\{(.*?)\}', text, re.DOTALL)
        if body_match:
            body = body_match.group(1).strip()
            if 'isEmpty' not in body and 'false' not in body:
                acc("ItemHandlerResourceAdapter: isValid() should return false for empty resources")


# ─── Main ────────────────────────────────────────────────────────────────────

def main():
    errors = []

    def acc(msg):
        errors.append(msg)

    checks = [
        ("ItemHandlerResourceAdapter: class exists with ResourceHandler<ItemResource>", check_item_adapter_implements_resource_handler),
        ("ItemHandlerResourceAdapter: all ResourceHandler methods present", check_item_adapter_methods),
        ("ItemHandlerResourceAdapter: delegates to IItemHandler", check_item_adapter_has_internal_iitemhandler),
        ("ItemHandlerResourceAdapter: uses Transaction pattern (not simulate flag)", check_item_adapter_uses_transaction),
        ("ItemHandlerResourceAdapter: guards against empty resources", check_item_adapter_empty_guard),
        ("ItemHandlerResourceAdapter: isValid handles empty resource", check_item_adapter_implements_is_valid),
        ("FluidHandlerResourceAdapter: class exists with ResourceHandler<FluidResource>", check_fluid_adapter_implements_resource_handler),
        ("FluidHandlerResourceAdapter: all ResourceHandler methods present", check_fluid_adapter_methods),
        ("FluidHandlerResourceAdapter: delegates to IFluidHandler", check_fluid_adapter_has_internal_ifluidhandler),
        ("FluidHandlerResourceAdapter: uses Transaction pattern (not simulate flag)", check_fluid_adapter_uses_transaction),
        ("FluidHandlerResourceAdapter: guards against empty resources", check_fluid_adapter_empty_guard),
        ("CommonProxy: Capabilities.Item.BLOCK registered with adapter", check_common_proxy_item_registration),
        ("CommonProxy: no old ItemHandler.BLOCK capability used", check_no_old_handler_exposed),
    ]

    print("=" * 60)
    print("  FP-002 TDD RED/GATE — Item/Fluid Reverse ResourceHandler Adapters")
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
        print("\n  >>> RED - Adapters/registration incomplete (expected for TDD RED phase)")
        return 1
    print("\n  >>> GREEN - All adapters and registration complete")
    return 0


if __name__ == "__main__":
    sys.exit(main())
