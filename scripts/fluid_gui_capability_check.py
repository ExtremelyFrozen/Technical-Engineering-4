#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
fluid_gui_capability_check.py — Static assertions for fluid capability rewrite.
Contract: GUI uses ResourceHandler bind, BE caches adapters, CommonProxy delegates.

GREEN = all assertions pass (exit 0). RED = any assertion fails (exit 1).
"""

import os
import re
import sys

PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

for s in (sys.stdout, sys.stderr):
    if s is not None and hasattr(s, 'reconfigure'):
        try:
            s.reconfigure(encoding='utf-8')
        except (ValueError, OSError):
            pass


def read_file(path):
    with open(path, 'rb') as f:
        return f.read().decode('utf-8', errors='replace')


def fail(errors, name, msg):
    errors.append(f"[FAIL-{name}] {msg}")


# ── A: Adapter core remains unchanged (already validated) ────────────
def check_adapter_core(errors):
    path = os.path.join(PROJECT_ROOT, 'src', 'main', 'java', 'com', 'modularmc', 'ten',
                        'api', 'capability', 'FluidHandlerResourceAdapter.java')
    content = read_file(path)
    if 'extends SnapshotJournal<FluidStack[]>' not in content:
        fail(errors, "A1", "Adapter does not extend SnapshotJournal<FluidStack[]>")
    if 'implements ResourceHandler<FluidResource>' not in content:
        fail(errors, "A2", "Adapter does not implement ResourceHandler<FluidResource>")
    if 'onRootCommit(FluidStack[] originalState)' not in content:
        fail(errors, "A3", "onRootCommit override not found")
    if 'resource.matches(current)' not in content:
        fail(errors, "A4", "resource.matches(current) not found")


# ── B: CmMachineBlockEntity caches ResourceHandler, restores protected ──
def check_be_fluid_resource_handler(errors):
    path = os.path.join(PROJECT_ROOT, 'src', 'main', 'java', 'com', 'modularmc', 'ten',
                        'api', 'blockentity', 'CmMachineBlockEntity.java')
    content = read_file(path)

    # Must have getFluidResourceHandler method
    if 'getFluidResourceHandler' not in content:
        fail(errors, "B1", "CmMachineBlockEntity missing getFluidResourceHandler method")

    # Must have caching fields (unsided + sided)
    if 'ResourceHandler<FluidResource>' not in content:
        fail(errors, "B2", "CmMachineBlockEntity missing ResourceHandler<FluidResource> caching field")
    if 'fluidResourceHandler' not in content and 'fluidHandlerResource' not in content:
        fail(errors, "B3", "CmMachineBlockEntity missing fluid resource handler caching field name")

    # canReceiveFluid/canExtractFluid must be protected (not public)
    if 'protected boolean canReceiveFluid' not in content:
        fail(errors, "B4", "canReceiveFluid not restored to protected")
    if 'protected boolean canExtractFluid' not in content:
        fail(errors, "B5", "canExtractFluid not restored to protected")

    # markFluidCapabilityChanged must NOT be present (removed)
    if 'markFluidCapabilityChanged' in content:
        fail(errors, "B6", "markFluidCapabilityChanged hook still present (should be removed)")

    # Old getFluidHandler must still exist
    if 'public IFluidHandler getFluidHandler' not in content:
        fail(errors, "B7", "Old getFluidHandler removed (must keep for internal compat)")


# ── C: TENMachineBlockUIFactory uses ResourceHandler bind + built-in tooltips ──
def check_gui_uses_resource_handler_bind(errors):
    path = os.path.join(PROJECT_ROOT, 'src', 'main', 'java', 'com', 'modularmc', 'ten',
                        'common', 'gui', 'TENMachineBlockUIFactory.java')
    content = read_file(path)

    # Must NOT bind with getFluidHandler (the deprecated IFluidHandler path)
    if 'bind(machine.getFluidHandler(' in content:
        fail(errors, "C1", "TENMachineBlockUIFactory still uses deprecated bind(getFluidHandler)")
    if 'bind(machine.getFluidHandler' in content:
        fail(errors, "C2", "TENMachineBlockUIFactory still uses deprecated IFluidHandler bind")

    # Must bind with getFluidResourceHandler (the ResourceHandler path)
    if 'getFluidResourceHandler(' not in content:
        fail(errors, "C3", "TENMachineBlockUIFactory missing getFluidResourceHandler call")
    if 'bind(' not in content[content.rfind('getFluidResourceHandler'):content.rfind('getFluidResourceHandler')+200]:
        if 'bind(handler' not in content:
            fail(errors, "C3b", "TENMachineBlockUIFactory missing bind(handler,tankIndex) after getFluidResourceHandler")

    # Behavior-chain check (C4/C5): production code uses var + getFluidResourceHandler + slot.bind,
    # so no explicit ResourceHandler/FluidResource imports are required.
    # C4: Verify slot.bind(handler, tankIndex) exists — proves ResourceHandler bind pattern
    if 'slot.bind(handler, tankIndex)' not in content and 'slot.bind(handler,' not in content:
        fail(errors, "C4", "TENMachineBlockUIFactory missing slot.bind(handler,tankIndex) — ResourceHandler bind pattern")
    # C5: Verify no deprecated IFluidHandler type used in the factory
    if 'IFluidHandler' in content:
        fail(errors, "C5", "TENMachineBlockUIFactory still references deprecated IFluidHandler type")

    # Tooltip contract: showFluidTooltips must be true (enable LDLib2 built-in)
    if '.showFluidTooltips(false)' in content:
        fail(errors, "C6", "showFluidTooltips is false (should be true for LDLib2 built-in tooltip)")
    if '.showFluidTooltips(true)' not in content:
        fail(errors, "C7", "showFluidTooltips(true) not found")

    # Must NOT have custom HOVER_TOOLTIPS listener on FluidSlot
    if re.search(r'slot\.addEventListener\(UIEvents\.HOVER_TOOLTIPS', content):
        fail(errors, "C8", "FluidSlot has custom HOVER_TOOLTIPS listener (should use LDLib2 built-in)")

    # Must NOT have fluidTooltip helper method
    if 'fluidTooltip(' in content:
        fail(errors, "C9", "fluidTooltip helper still present (should be removed, LDLib2 built-in suffices)")

    # Must NOT have showValue parameter in fluidGauge/fluidGaugeWithBackground/fluidGaugeBase
    if 'boolean showValue' in content:
        fail(errors, "C10", "showValue parameter still present in fluidGauge method signatures (should be removed)")


# ── D: CommonProxy delegates to machine.getFluidResourceHandler ──────
def check_commonproxy_delegates(errors):
    path = os.path.join(PROJECT_ROOT, 'src', 'main', 'java', 'com', 'modularmc', 'ten',
                        'common', 'CommonProxy.java')
    content = read_file(path)

    # Must NOT directly construct adapter
    if 'new FluidHandlerResourceAdapter(' in content:
        fail(errors, "D1", "CommonProxy still directly constructs FluidHandlerResourceAdapter (must delegate)")

    # Must call machine.getFluidResourceHandler(side)
    if 'machine.getFluidResourceHandler(' not in content:
        fail(errors, "D2", "CommonProxy missing machine.getFluidResourceHandler(side) call")

    # Should not import FluidHandlerResourceAdapter
    if 'import com.modularmc.ten.api.capability.FluidHandlerResourceAdapter;' in content:
        fail(errors, "D3", "CommonProxy still imports FluidHandlerResourceAdapter (should remove)")


# ── E: GUI fluid gauge matrix unchanged ───────────────────────────────
def check_gui_fluid_gauge_mapping(errors):
    path = os.path.join(PROJECT_ROOT, 'src', 'main', 'java', 'com', 'modularmc', 'ten',
                        'common', 'gui', 'TENMachineBlockUIFactory.java')
    content = read_file(path)
    if 'fluidGauge(CmMachineBlockEntity machine, int x, int y, int width, int height, int tankIndex' not in content:
        fail(errors, "E0", "fluidGauge method signature unexpected (should not have showValue)")

    machines = {
        'RefinerBlockEntity.java': [(37, 17, 18, 50, 0), (143, 17, 18, 50, 1)],
        'CondenserBlockEntity.java': [(143, 17, 18, 50, 0)],
        'ChannelFluidBlockEntity.java': [(7, 17, 18, 50, 0), (25, 17, 18, 50, 1)],
    }
    use_with_background = ['ChannelFluidBlockEntity.java']

    for machine_file, expected_gauges in machines.items():
        be_path = os.path.join(PROJECT_ROOT, 'src', 'main', 'java', 'com', 'modularmc', 'ten',
                               'common', 'blockentity')
        gauge_fn = 'fluidGaugeWithBackground' if machine_file in use_with_background else 'fluidGauge'
        found_file = False
        for root, dirs, files in os.walk(be_path):
            if machine_file in files:
                found_file = True
                full_path = os.path.join(root, machine_file)
                machine_content = read_file(full_path)
                for gauge in expected_gauges:
                    gs = f'{gauge_fn}(this, {gauge[0]}, {gauge[1]}, {gauge[2]}, {gauge[3]}, {gauge[4]})'
                    if gs not in machine_content:
                        alt_fn = 'fluidGaugeWithBackground' if gauge_fn == 'fluidGauge' else 'fluidGauge'
                        alt_s = f'{alt_fn}(this, {gauge[0]}, {gauge[1]}, {gauge[2]}, {gauge[3]}, {gauge[4]})'
                        if alt_s not in machine_content:
                            fail(errors, "E", f"{machine_file}: gauge not found")
                break
        if not found_file:
            fail(errors, "E", f"{machine_file} not found")


# ── F: GameTest coverage (unchanged core) ─────────────────────────────
def check_gametest_coverage(errors):
    path = os.path.join(PROJECT_ROOT, 'src', 'extra', 'java', 'com', 'modularmc', 'ten',
                        'adapterTest', 'FluidHandlerIndexGameTest.java')
    content = read_file(path)
    tests = [
        'extractDrainsCorrectTank', 'insertFillsCorrectTank',
        'abortRestoresState', 'commitPreservesChanges',
        'readWorksWithoutWritePermission',
        'insertBlockedByPermission', 'extractBlockedByPermission',
        'commitCallbackFiresOnce', 'abortDoesNotFireCallback',
        'emptyResourceReturnsCapacity', 'insertRespectsCapacity',
        'componentMatchUsedOnInsert',
    ]
    for method in tests:
        if f'public void {method}(' not in content:
            fail(errors, "F", f"Missing GameTest method: {method}")


# ── G: getFluidHandler exists for internal compat ─────────────────────
def check_old_fluid_handler_exists(errors):
    path = os.path.join(PROJECT_ROOT, 'src', 'main', 'java', 'com', 'modularmc', 'ten',
                        'api', 'blockentity', 'CmMachineBlockEntity.java')
    content = read_file(path)
    if 'createCombinedFluidHandler' not in content:
        fail(errors, "G", "createCombinedFluidHandler removed (must keep for internal compat)")
    if 'fillFluidRange' not in content and 'fillFluid' not in content:
        fail(errors, "G2", "fillFluidRange removed (must keep)")


# ── MAIN ────────────────────────────────────────────────────────────────
def main():
    errors = []

    print("=" * 60)
    print("Fluid Capability LDLib2 ResourceHandler Bind Check")
    print("=" * 60)

    checks = [
        ("A: Adapter core unchanged", check_adapter_core),
        ("B: BE caches ResourceHandler, protected restored", check_be_fluid_resource_handler),
        ("C: GUI uses ResourceHandler bind", check_gui_uses_resource_handler_bind),
        ("D: CommonProxy delegates to BE", check_commonproxy_delegates),
        ("E: GUI fluid gauge matrix", check_gui_fluid_gauge_mapping),
        ("F: GameTest coverage", check_gametest_coverage),
        ("G: Old fluid compat methods kept", check_old_fluid_handler_exists),
    ]

    for name, fn in checks:
        before = len(errors)
        fn(errors)
        nerr = len(errors) - before
        status = "PASS" if nerr == 0 else f"FAIL ({nerr})"
        print(f"  [{name}] {status}")

    print(f"\n  Total: {len(errors)} error(s)")
    if errors:
        for e in errors:
            print(f"    - {e}")
        print("\n  >>> RED")
        return 1
    print("\n  >>> GREEN")
    return 0


if __name__ == '__main__':
    sys.exit(main())
