#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Shared resource root resolution for validation scripts — main + generated
dual-root resource discovery with conflict detection.

Provides:
  - resolve_unique_resource(project_root, relative_path)
      → (owner_root, full_path) for a file that exists in exactly one root.
  - collect_resource_files(project_root, relative_dir, suffix)
      → {relative_path: (owner_root, full_path)} deduplicated across both roots.

Consumed by:
  - validate_brand_localization.py
  - validate_custom_block_tooltips.py
  - validate_item_definitions.py

No external dependencies — stdlib only.
"""

import os
import sys


# Roots are searched in priority order — 'main' first for user overrides,
# then 'generated' for data-gen output.
RESOURCE_ROOTS = ("main", "generated")


# ── Path helpers ────────────────────────────────────────────────────────


def _root_dir(project_root, root_key):
    """Return absolute path to a resource root directory."""
    return os.path.join(project_root, "src", root_key, "resources")


def _sanitize_relative_path(path):
    """Normalize and validate a relative resource path.

    Converts backslashes to forward slashes (Windows compatibility).
    Rejects absolute paths and '..' directory traversal (fail-fast).

    Args:
        path: Relative path string.

    Returns:
        Normalized path with forward slashes.

    Raises:
        TypeError: If path is not a string.
        ValueError: If path is absolute or contains '..' traversal.
    """
    if not isinstance(path, str):
        raise TypeError(
            f"relative_path must be a string, got {type(path).__name__}"
        )
    if os.path.isabs(path):
        raise ValueError(f"Absolute path not allowed: '{path}'")
    norm = path.replace("\\", "/")
    if ".." in norm.split("/"):
        raise ValueError(f"Path traversal not allowed: '{path}'")
    return norm


# ── Public API ──────────────────────────────────────────────────────────


def resolve_unique_resource(project_root, relative_path):
    """
    Find a resource that exists in exactly one root directory.

    Args:
        project_root: Absolute path to the project root directory.
        relative_path: Resource path relative to any resources/ root,
                       using forward slashes (e.g.
                       'assets/kenergyengineering/lang/en_us.json').

    Returns:
        (root_key, full_path) tuple where root_key is 'main' or 'generated'.

    Raises:
        TypeError:     relative_path is not a string.
        ValueError:    relative_path is absolute or contains '..'.
        FileNotFoundError: The resource does not exist in any root.
        AssertionError:   The resource exists in more than one root.
    """
    rel = _sanitize_relative_path(relative_path)
    found = []
    for root_key in RESOURCE_ROOTS:
        candidate = os.path.join(_root_dir(project_root, root_key), rel)
        if os.path.exists(candidate):
            found.append((root_key, candidate))

    if not found:
        raise FileNotFoundError(
            f"Resource not found: '{rel}' "
            f"(searched {', '.join(RESOURCE_ROOTS)})"
        )
    if len(found) > 1:
        lines = [f"  {k}: {p}" for k, p in found]
        raise AssertionError(
            f"Resource conflict: '{rel}' exists in multiple roots:\n"
            + "\n".join(lines)
        )
    return found[0]


def collect_resource_files(project_root, relative_dir, suffix=None):
    """
    Collect resource files from both roots, deduplicated by relative path.

    Args:
        project_root: Absolute path to the project root directory.
        relative_dir: Directory relative to any resources/ root
                      (e.g. 'assets/kenergyengineering/items').
        suffix:       Optional file suffix filter (e.g. '.json').

    Returns:
        dict mapping unique relative path (str, forward slashes) to
        (owner_root, full_path) tuple.

    Raises:
        TypeError:     relative_dir is not a string.
        ValueError:    relative_dir is absolute or contains '..'.
        AssertionError: If the same relative path exists in both roots.
    """
    rel_dir = _sanitize_relative_path(relative_dir)
    result = {}
    for root_key in RESOURCE_ROOTS:
        base = os.path.join(_root_dir(project_root, root_key), rel_dir)
        if not os.path.isdir(base):
            continue
        for dirpath, _dirs, filenames in os.walk(base):
            for fname in filenames:
                if suffix is not None and not fname.endswith(suffix):
                    continue
                full = os.path.join(dirpath, fname)
                rel = os.path.relpath(full, _root_dir(project_root, root_key))
                rel = rel.replace(os.sep, '/')
                if rel in result:
                    prev_root, _ = result[rel]
                    raise AssertionError(
                        f"Resource conflict: '{rel}' exists in both "
                        f"'{prev_root}' and '{root_key}'"
                    )
                result[rel] = (root_key, full)
    return result
