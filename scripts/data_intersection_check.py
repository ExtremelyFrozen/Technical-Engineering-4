#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import sys
import io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8')
"""
Validate that src/main/resources/data and src/generated/resources/data have
zero file-path intersection.

Files under data/ are generated (tags, loot tables, recipes, etc.) and should
not overlap with hand-written main resource data files. The assets/ directory
is intentionally exempt — item definition and model files overlap by design.

Exit code 0 = clean (GREEN), non-zero = intersection found (RED).
"""

import argparse
import os
import sys

PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def find_relative_paths(base_dir, subdir):
    """Find all file paths under base_dir/subdir, returned relative to base_dir,
    using forward slashes."""
    target = os.path.join(base_dir, subdir)
    if not os.path.isdir(target):
        return set()
    result = set()
    for dirpath, _dirs, filenames in os.walk(target):
        for f in filenames:
            full = os.path.join(dirpath, f)
            rel = os.path.relpath(full, base_dir).replace(os.sep, '/')
            result.add(rel)
    return result


def main():
    parser = argparse.ArgumentParser(
        description="Validate main/generated data directory intersection = 0."
    )
    parser.add_argument("--main", default=os.path.join(PROJECT_ROOT, "src", "main", "resources"))
    parser.add_argument("--generated", default=os.path.join(PROJECT_ROOT, "src", "generated", "resources"))
    args = parser.parse_args()

    exit_code = 0

    print("=" * 60)
    print("Main/Generated Data Directory Intersection Check")
    print("=" * 60)
    print(f"  Main:      {args.main}")
    print(f"  Generated: {args.generated}")

    # Only scan data/ subdirectory, not assets/
    main_data = find_relative_paths(args.main, "data")
    gen_data = find_relative_paths(args.generated, "data")

    print(f"\n  Main data files:      {len(main_data)}")
    print(f"  Generated data files: {len(gen_data)}")

    intersection = sorted(main_data & gen_data)

    print(f"\n  Intersection count:   {len(intersection)}")
    print()

    if intersection:
        print("  FAIL: Files in BOTH main and generated data/:")
        for p in intersection:
            print(f"    - {p}")
        exit_code = 1
    else:
        print("  OK: No overlapping files between main and generated data/")

    print("\n" + "=" * 60)
    if exit_code == 0:
        print("RESULT: DATA INTERSECTION CHECK PASSED (GREEN)")
    else:
        print("RESULT: DATA INTERSECTION CHECK FAILED (RED) — see above")
    print("=" * 60)

    return exit_code


if __name__ == "__main__":
    sys.exit(main())
