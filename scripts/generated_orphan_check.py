#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Full-tree orphan/stale generated resource check via HashCache.

Scans ALL files under src/generated/resources/ (excluding .cache/**) and
compares them against NeoForge DataProvider .cache records.

Checks:
  1. ORPHAN: generated files that exist on disk but are NOT recorded in any
     .cache file → they came from a removed provider, a manual edit, or a
     previous datagen run whose cache was lost.
  2. STALE: .cache entries whose referenced file no longer exists on disk →
     the file was deleted manually but cache still references it.
  3. CACHE INTEGRITY: reports total cache entries vs total non-cache files.

Usage:
    python scripts/generated_orphan_check.py [--generated DIR]

Exit code 0 = clean, 1 = orphan/stale found, 2 = fatal (no cache).
"""

import argparse
import os
import re
import sys

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')
    sys.stderr.reconfigure(encoding='utf-8')

PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# ── Cache parsers ──────────────────────────────────────────────────────

CACHE_LINE_RE = re.compile(
    r'^(?P<hash>[a-f0-9]{40}|[a-f0-9]{20})\s+(?P<path>\S+)$'
)


def parse_cache_files(cache_dir):
    """
    Read all files under *cache_dir*.
    Returns a dict: {relative_path: set_of_cache_filenames}.
    """
    if not os.path.isdir(cache_dir):
        print(f"  WARNING: Cache directory not found: {cache_dir}")
        return {}

    cache_entries = {}  # rel_path -> [cache_filenames (relative to cache_dir)]

    for root, _dirs, files in os.walk(cache_dir):
        for fname in sorted(files):
            fpath = os.path.join(root, fname)
            cache_rel = os.path.relpath(fpath, cache_dir).replace(os.sep, "/")
            with open(fpath, "r", encoding="utf-8") as fh:
                for line in fh:
                    line = line.rstrip("\n")
                    # Skip comment / header lines
                    if line.startswith("//") or not line.strip():
                        continue
                    m = CACHE_LINE_RE.match(line)
                    if m:
                        rel_path = m.group("path")
                        # Normalise – NeoForge cache uses forward slashes
                        rel_path = rel_path.replace("\\", "/")
                        cache_entries.setdefault(rel_path, []).append(cache_rel)

    return cache_entries


# ── File scanner ───────────────────────────────────────────────────────

def find_all_generated_files(base_dir):
    """
    Walk *base_dir* and collect relative paths for ALL files,
    except those under .cache/.
    Returns a set of relative paths (forward-slash separated).
    """
    if not os.path.isdir(base_dir):
        return set()

    result = set()
    for dirpath, dirnames, filenames in os.walk(base_dir):
        # Compute relative path of current directory
        rel_dir = os.path.relpath(dirpath, base_dir).replace(os.sep, "/")
        # Skip .cache directory and its contents
        if rel_dir == ".cache" or rel_dir.startswith(".cache/"):
            # Prune traversal by modifying dirnames in-place
            dirnames[:] = [d for d in dirnames if d != ".cache"]
            continue
        # Also filter out .cache from subdirectories at current level
        dirnames[:] = [d for d in dirnames if d != ".cache"]

        for f in filenames:
            if rel_dir == ".":
                full_rel = f
            else:
                full_rel = f"{rel_dir}/{f}"
            result.add(full_rel)

    return result


# ── Main ───────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(
        description="Full-tree orphan/stale generated resource check via HashCache."
    )
    parser.add_argument(
        "--generated",
        default=os.path.join(PROJECT_ROOT, "src", "generated", "resources"),
        help="Path to generated resources directory (default: src/generated/resources)",
    )
    args = parser.parse_args()

    generated_dir = os.path.abspath(args.generated)
    cache_dir = os.path.join(generated_dir, ".cache")

    print("=" * 60)
    print("TEN Generated Resource Full-Tree Orphan/Stale Check")
    print("=" * 60)
    print(f"  Generated dir : {generated_dir}")
    print(f"  Cache dir     : {cache_dir}")

    # ── 1. Parse cache ────────────────────────────────────────────────
    print("\n[1] Parsing cache files …")
    cache_entries = parse_cache_files(cache_dir)
    if not cache_entries:
        print("  FATAL: No cache entries parsed — cannot validate.")
        print("  Cache format may be unparseable or directory missing.")
        sys.exit(2)

    print(f"  Total unique paths in cache : {len(cache_entries)}")

    # ── 2. Scan all generated files (excl .cache) ─────────────────────
    print("\n[2] Scanning all generated files (excluding .cache) …")
    actual_files = find_all_generated_files(generated_dir)
    print(f"  Total non-cache files       : {len(actual_files)}")

    # ── 3. Orphan check ───────────────────────────────────────────────
    print("\n[3] Orphan check (file on disk but not in cache) …")
    orphan_paths = sorted(actual_files - set(cache_entries.keys()))

    if orphan_paths:
        print(f"  FAIL: {len(orphan_paths)} orphan file(s) found:")
        for p in orphan_paths:
            print(f"    {p}")
    else:
        print("  OK: No orphan files found")

    # ── 4. Stale check ────────────────────────────────────────────────
    print("\n[4] Stale check (cache entry but no file on disk) …")
    stale_paths = sorted(set(cache_entries.keys()) - actual_files)

    if stale_paths:
        print(f"  FAIL: {len(stale_paths)} stale cache entry(ies) found:")
        for p in stale_paths:
            print(f"    {p}")
    else:
        print("  OK: No stale cache entries found")

    # ── 5. Cache count integrity ──────────────────────────────────────
    print("\n[5] Cache count integrity …")
    if len(cache_entries) == len(actual_files):
        print(f"  OK: Cache entries ({len(cache_entries)}) == "
              f"Non-cache files ({len(actual_files)})")
    else:
        print(f"  NOTE: Cache entries ({len(cache_entries)}) != "
              f"Non-cache files ({len(actual_files)})")
        print(f"  (This is expected if cache covers providers while files include "
              f"non-provider artifacts; orphan/stale checks above are authoritative.)")

    # ── Summary ────────────────────────────────────────────────────────
    print("\n" + "=" * 60)
    exit_code = 0
    if orphan_paths:
        exit_code = 1
    if stale_paths:
        exit_code = 1

    if exit_code == 0:
        print("RESULT: ALL CHECKS PASSED (GREEN)")
    else:
        print("RESULT: CHECK(S) FAILED (RED) — see above")
    print("=" * 60)

    return exit_code


if __name__ == "__main__":
    sys.exit(main())
