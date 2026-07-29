#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Validate FP-002: Item/Fluid reverse ResourceHandler adapters.

Checks:
  - ItemHandlerResourceAdapter.java exists with ResourceHandler<ItemResource> signature
  - ItemHandlerResourceAdapter implements all ResourceHandler methods
  - ItemHandlerResourceAdapter delegates to internal IItemHandler (legacy pattern)
  - FluidHandlerResourceAdapter.java exists, extends SnapshotJournal<FluidStack[]>, implements ResourceHandler<FluidResource>
  - FluidHandlerResourceAdapter implements all ResourceHandler methods
  - FluidHandlerResourceAdapter uses List<MachineFluidTank>, no IFluidHandler field
  - FluidHandlerResourceAdapter uses per-index (no handler-wide fill/drain), updateSnapshots, resource.matches
  - FluidHandlerResourceAdapter has onRootCommit override
  - CommonProxy.registerCapabilities registers both with correct adapter patterns
  - No old handler exposed at capability boundary
  - Transaction-aware insert/extract
  - Empty/null resource guards

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
    path = os.path.join(CAP_DIR, "ItemHandlerResourceAdapter.java")
    text = read(path)
    if text is None:
        acc("ItemHandlerResourceAdapter.java not found")
        return None
    return text


def check_item_adapter_implements_resource_handler(acc):
    text = check_item_adapter_exists(acc)
    if text is None: return
    if "ResourceHandler<ItemResource>" not in text:
        acc("ItemHandlerResourceAdapter: missing 'implements ResourceHandler<ItemResource>'")
    if "class ItemHandlerResourceAdapter" not in text:
        acc("ItemHandlerResourceAdapter: class ItemHandlerResourceAdapter not found")


def check_item_adapter_methods(acc):
    text = check_item_adapter_exists(acc)
    if text is None: return
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
    text = check_item_adapter_exists(acc)
    if text is None: return
    if not re.search(r'IItemHandler\s+\w+', text):
        acc("ItemHandlerResourceAdapter: no IItemHandler field found")


def check_fluid_adapter_exists(acc):
    path = os.path.join(CAP_DIR, "FluidHandlerResourceAdapter.java")
    text = read(path)
    if text is None:
        acc("FluidHandlerResourceAdapter.java not found")
        return None
    return text


def check_fluid_adapter_extends_snapshot_journal(acc):
    text = check_fluid_adapter_exists(acc)
    if text is None: return
    if "extends SnapshotJournal<FluidStack[]>" not in text:
        acc("FluidHandlerResourceAdapter: missing 'extends SnapshotJournal<FluidStack[]>'")
    if "implements ResourceHandler<FluidResource>" not in text:
        acc("FluidHandlerResourceAdapter: missing 'implements ResourceHandler<FluidResource>'")
    if "class FluidHandlerResourceAdapter" not in text:
        acc("FluidHandlerResourceAdapter: class not found")


def check_fluid_adapter_methods(acc):
    text = check_fluid_adapter_exists(acc)
    if text is None: return
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


def check_fluid_adapter_no_ifluidhandler(acc):
    """FluidHandlerResourceAdapter must NOT have IFluidHandler field; uses List<MachineFluidTank>."""
    text = check_fluid_adapter_exists(acc)
    if text is None: return
    if re.search(r'IFluidHandler\s+\w+\s*;', text):
        acc("FluidHandlerResourceAdapter: still has IFluidHandler field (should use List<MachineFluidTank>)")
    if 'List<MachineFluidTank>' not in text:
        acc("FluidHandlerResourceAdapter: missing List<MachineFluidTank> field")


def check_fluid_adapter_no_handler_wide(acc):
    """Must not use handler-wide fill/drain."""
    text = check_fluid_adapter_exists(acc)
    if text is None: return
    if re.search(r'(handler\.fill|handler\.drain)\s*\(', text):
        acc("FluidHandlerResourceAdapter: uses handler-wide fill/drain (should be per-index setFluid)")


def check_fluid_adapter_update_snapshots(acc):
    """Must call updateSnapshots in insert/extract."""
    text = check_fluid_adapter_exists(acc)
    if text is None: return
    if 'updateSnapshots(transaction)' not in text:
        acc("FluidHandlerResourceAdapter: missing updateSnapshots(transaction) calls")


def check_fluid_adapter_resource_matches(acc):
    """Must use resource.matches(current) for component-aware comparison."""
    text = check_fluid_adapter_exists(acc)
    if text is None: return
    if 'resource.matches(current)' not in text:
        acc("FluidHandlerResourceAdapter: missing resource.matches(current) for component-aware comparison")
    if re.search(r'current\.is\(resource\.getFluid\(\)\)', text):
        acc("FluidHandlerResourceAdapter: still uses current.is(resource.getFluid()) type-only comparison")


def check_fluid_adapter_on_root_commit(acc):
    """Must override onRootCommit to fire commitCallback once."""
    text = check_fluid_adapter_exists(acc)
    if text is None: return
    if 'onRootCommit(FluidStack[] originalState)' not in text:
        acc("FluidHandlerResourceAdapter: missing onRootCommit(FluidStack[]) override")


def check_fluid_adapter_capacity_empty(acc):
    """getCapacityAsLong must return general capacity even for empty resource."""
    text = check_fluid_adapter_exists(acc)
    if text is None: return
    # Find getCapacityAsLong
    cap_match = re.search(r'getCapacityAsLong\s*\([^}]*\}', text, re.DOTALL)
    if cap_match:
        body = cap_match.group(0)
        if 'resource == null || resource.isEmpty()' in body or 'resource.isEmpty()' in body:
            idx = body.find('resource.isEmpty()')
            snippet = body[idx:idx+100]
            if 'return 0' in snippet:
                acc("FluidHandlerResourceAdapter: getCapacityAsLong returns 0 for empty resource (should return general tank capacity)")


def check_common_proxy_fluid_registration(acc):
    """CommonProxy must delegate to machine.getFluidResourceHandler, not construct adapter directly."""
    text = read(COMMON_PROXY)
    if text is None:
        acc("CommonProxy.java not found")
        return
    if "Capabilities.Fluid.BLOCK" not in text:
        acc("CommonProxy.java: missing Capabilities.Fluid.BLOCK registration")
    # Must NOT construct FluidHandlerResourceAdapter directly
    if "new FluidHandlerResourceAdapter(" in text:
        acc("CommonProxy.java: still directly constructs FluidHandlerResourceAdapter (should delegate to BE)")
    # Must call machine.getFluidResourceHandler(side)
    if "machine.getFluidResourceHandler(" not in text:
        acc("CommonProxy.java: missing machine.getFluidResourceHandler(side) delegation")
    # Should not import FluidHandlerResourceAdapter directly
    if "import com.modularmc.ten.api.capability.FluidHandlerResourceAdapter;" in text:
        acc("CommonProxy.java: still imports FluidHandlerResourceAdapter directly")


def check_no_old_handler_exposed(acc):
    """Capability boundary must NOT expose IItemHandler/IFluidHandler directly."""
    text = read(COMMON_PROXY)
    if text is None: return
    if "Capabilities.ItemHandler.BLOCK" in text:
        acc("CommonProxy.java: uses old Capabilities.ItemHandler.BLOCK instead of Capabilities.Item.BLOCK")
    if "Capabilities.FluidHandler.BLOCK" in text:
        acc("CommonProxy.java: uses old Capabilities.FluidHandler.BLOCK instead of Capabilities.Fluid.BLOCK")


def check_item_adapter_uses_transaction(acc):
    text = check_item_adapter_exists(acc)
    if text is None: return
    if re.search(r'insert\s*\([^)]*simulate', text):
        acc("ItemHandlerResourceAdapter: insert() uses legacy 'simulate' parameter instead of Transaction pattern")


def check_fluid_adapter_empty_guard(acc):
    text = check_fluid_adapter_exists(acc)
    if text is None: return
    if not re.search(r'(isEmpty|checkNonEmpty|checkNonEmptyNonNegative)', text):
        acc("FluidHandlerResourceAdapter: missing empty/non-negative resource guard pattern")


def check_item_adapter_empty_guard(acc):
    text = check_item_adapter_exists(acc)
    if text is None: return
    if not re.search(r'(isEmpty|isValid|checkNonEmpty|checkNonEmptyNonNegative)', text):
        acc("ItemHandlerResourceAdapter: missing empty/non-negative resource guard pattern")


# ─── BE-level checks ────────────────────────────────────────────────

def check_be_has_fluid_resource_handler(acc):
    """CmMachineBlockEntity must have getFluidResourceHandler method with caching."""
    path = os.path.join(CAP_DIR, "..", "blockentity", "CmMachineBlockEntity.java")
    text = read(os.path.normpath(path))
    if text is None:
        acc("CmMachineBlockEntity.java not found")
        return
    if "getFluidResourceHandler" not in text:
        acc("CmMachineBlockEntity: missing getFluidResourceHandler method")
    if "ResourceHandler<FluidResource>" not in text:
        acc("CmMachineBlockEntity: missing ResourceHandler<FluidResource> caching field")
    if "public boolean canReceiveFluid" in text or "protected boolean canReceiveFluid" not in text:
        acc("CmMachineBlockEntity: canReceiveFluid not restored to protected")
    if "public boolean canExtractFluid" in text or "protected boolean canExtractFluid" not in text:
        acc("CmMachineBlockEntity: canExtractFluid not restored to protected")
    if "markFluidCapabilityChanged" in text:
        acc("CmMachineBlockEntity: markFluidCapabilityChanged still present (should be removed)")
    if "getFluidHandler" not in text:
        acc("CmMachineBlockEntity: getFluidHandler removed (must keep for internal compat)")


def check_gui_uses_resource_handler_bind(acc):
    """TENMachineBlockUIFactory must use ResourceHandler bind, not deprecated IFluidHandler bind.

    Behavior-chain assertions (not import checks — the compiler infers types
    from getFluidResourceHandler return and FluidSlot.bind overload resolution).
    """
    gui_path = SRC("main", "java", "com", "modularmc", "ten", "common", "gui", "TENMachineBlockUIFactory.java")
    text = read(gui_path)
    if text is None:
        acc("TENMachineBlockUIFactory.java not found")
        return
    # 1. Must NOT use deprecated bind(getFluidHandler) — IFluidHandler path
    if "bind(machine.getFluidHandler(" in text:
        acc("TENMachineBlockUIFactory: still uses deprecated bind(getFluidHandler)")
    # 2. Must call getFluidResourceHandler (behavior, not import)
    if "getFluidResourceHandler(" not in text:
        acc("TENMachineBlockUIFactory: missing getFluidResourceHandler call")
    # 3. Must bind the result via slot.bind(handler, tankIndex) — allowing var format
    if not re.search(r'\.bind\(\s*\w+\s*,\s*\w+\s*\)', text):
        acc("TENMachineBlockUIFactory: missing slot.bind(handler, tankIndex) call")
    # 4. No deprecated IFluidHandler type used anywhere in this file
    if re.search(r'\bIFluidHandler\b', text):
        acc("TENMachineBlockUIFactory: references deprecated IFluidHandler type")


# ─── Main ────────────────────────────────────────────────────────────────────

def main():
    errors = []

    def acc(msg):
        errors.append(msg)

    checks = [
        ("ItemHandlerResourceAdapter: class exists with ResourceHandler<ItemResource>",
         check_item_adapter_implements_resource_handler),
        ("ItemHandlerResourceAdapter: all ResourceHandler methods present",
         check_item_adapter_methods),
        ("ItemHandlerResourceAdapter: delegates to IItemHandler",
         check_item_adapter_has_internal_iitemhandler),
        ("ItemHandlerResourceAdapter: uses Transaction pattern (not simulate flag)",
         check_item_adapter_uses_transaction),
        ("ItemHandlerResourceAdapter: guards against empty resources",
         check_item_adapter_empty_guard),
        ("FluidHandlerResourceAdapter: extends SnapshotJournal + implements ResourceHandler<FluidResource>",
         check_fluid_adapter_extends_snapshot_journal),
        ("FluidHandlerResourceAdapter: all ResourceHandler methods present",
         check_fluid_adapter_methods),
        ("FluidHandlerResourceAdapter: uses List<MachineFluidTank> (no IFluidHandler)",
         check_fluid_adapter_no_ifluidhandler),
        ("FluidHandlerResourceAdapter: no handler-wide fill/drain (per-index only)",
         check_fluid_adapter_no_handler_wide),
        ("FluidHandlerResourceAdapter: calls updateSnapshots(transaction)",
         check_fluid_adapter_update_snapshots),
        ("FluidHandlerResourceAdapter: uses resource.matches(current) for component check",
         check_fluid_adapter_resource_matches),
        ("FluidHandlerResourceAdapter: overrides onRootCommit(originalState)",
         check_fluid_adapter_on_root_commit),
        ("FluidHandlerResourceAdapter: getCapacityAsLong handles empty resource",
         check_fluid_adapter_capacity_empty),
        ("FluidHandlerResourceAdapter: guards against empty resources",
         check_fluid_adapter_empty_guard),
        ("CommonProxy: Capabilities registered with adapters",
         check_common_proxy_fluid_registration),
        ("CommonProxy: no old ItemHandler.BLOCK capability used",
         check_no_old_handler_exposed),
        ("CmMachineBlockEntity: getFluidResourceHandler with caching + protected restored",
         check_be_has_fluid_resource_handler),
        ("TENMachineBlockUIFactory: ResourceHandler bind (not deprecated IFluidHandler)",
         check_gui_uses_resource_handler_bind),
    ]

    print("=" * 60)
    print("  FP-002 TDD GATE — Item/Fluid Reverse ResourceHandler Adapters")
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
        print("\n  >>> RED")
        return 1
    print("\n  >>> GREEN - All adapters and registration complete")
    return 0


if __name__ == "__main__":
    sys.exit(main())
