#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Unit tests for resource_roots.py — dual-root resource resolution with
conflict detection and input sanitization.

Covers: main-only / generated-only / missing / conflict / union / empty /
suffix / recursive / forward-slash keys / Windows backslash normalization /
absolute-path rejection / path traversal rejection / realistic 8+5 scenario.

Usage:
    python -m unittest scripts/test_resource_roots.py  (from project root)
    python scripts/test_resource_roots.py              (from anywhere)
"""

import os
import sys
import tempfile
import unittest

# Ensure the scripts directory is on the path so resource_roots can be imported
_script_dir = os.path.dirname(os.path.abspath(__file__))
if _script_dir not in sys.path:
    sys.path.insert(0, _script_dir)

from resource_roots import (
    RESOURCE_ROOTS,
    resolve_unique_resource,
    collect_resource_files,
)


def _touch(*segments):
    """Create an empty file at the joined path, creating parent dirs."""
    path = os.path.join(*segments)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write("")
    return path


class ResourceRootTestBase(unittest.TestCase):
    """Shared fixture: temporary directory with main + generated resource roots."""

    def setUp(self):
        self._tmp = tempfile.TemporaryDirectory(prefix="rr_test_")
        self.root = self._tmp.name
        os.makedirs(os.path.join(self.root, "src", "main", "resources"))
        os.makedirs(os.path.join(self.root, "src", "generated", "resources"))

    def tearDown(self):
        self._tmp.cleanup()


# ── resolve_unique_resource ─────────────────────────────────────────────


class TestResolveUniqueResource(ResourceRootTestBase):
    """Tests for resolve_unique_resource()."""

    def test_main_only(self):
        """File exists only in main/ resources → returned with owner 'main'."""
        _touch(self.root, "src", "main", "resources", "assets", "test", "lang", "en_us.json")
        owner, fpath = resolve_unique_resource(self.root, "assets/test/lang/en_us.json")
        self.assertEqual(owner, "main")
        self.assertTrue(os.path.exists(fpath))
        norm = os.path.normpath(fpath)
        self.assertIn(
            os.path.join("main", "resources", "assets", "test", "lang", "en_us.json"),
            norm,
        )

    def test_generated_only(self):
        """File exists only in generated/ resources → returned with owner 'generated'."""
        _touch(self.root, "src", "generated", "resources", "assets", "test", "lang", "zh_cn.json")
        owner, fpath = resolve_unique_resource(self.root, "assets/test/lang/zh_cn.json")
        self.assertEqual(owner, "generated")
        self.assertIn("generated", fpath)

    def test_missing_raises_filenotfound(self):
        """No file in either root → FileNotFoundError mentioning roots."""
        with self.assertRaises(FileNotFoundError) as ctx:
            resolve_unique_resource(self.root, "assets/test/lang/missing.json")
        msg = str(ctx.exception)
        self.assertIn("missing.json", msg)
        for r in RESOURCE_ROOTS:
            self.assertIn(r, msg)

    def test_conflict_raises_assertionerror(self):
        """Same relative path in both roots → AssertionError listing both paths."""
        _touch(self.root, "src", "main", "resources", "assets", "test", "conflict.txt")
        _touch(self.root, "src", "generated", "resources", "assets", "test", "conflict.txt")
        with self.assertRaises(AssertionError) as ctx:
            resolve_unique_resource(self.root, "assets/test/conflict.txt")
        msg = str(ctx.exception)
        self.assertIn("conflict", msg.lower())
        self.assertIn("assets/test/conflict.txt", msg)
        self.assertIn("main:", msg)
        self.assertIn("generated:", msg)

    def test_returns_absolute_path(self):
        """Returned path is absolute and exists."""
        _touch(self.root, "src", "main", "resources", "data", "foo.json")
        _, fpath = resolve_unique_resource(self.root, "data/foo.json")
        self.assertTrue(os.path.isabs(fpath))
        self.assertTrue(os.path.exists(fpath))

    def test_none_typeerror(self):
        """None as relative_path raises TypeError (fail-fast)."""
        with self.assertRaises(TypeError):
            resolve_unique_resource(self.root, None)

    def test_absolute_path_rejected(self):
        """Absolute path as relative_path raises ValueError."""
        abs_path = os.path.join(self.root, "src", "main", "resources", "foo.json")
        with self.assertRaises(ValueError) as ctx:
            resolve_unique_resource(self.root, abs_path)
        self.assertIn("Absolute path", str(ctx.exception))

    def test_path_traversal_rejected(self):
        """Path with '..' raises ValueError."""
        with self.assertRaises(ValueError) as ctx:
            resolve_unique_resource(self.root, "assets/../foo.json")
        self.assertIn("Path traversal", str(ctx.exception))

    def test_backslash_normalization(self):
        """Windows backslash in path is normalized to forward slashes."""
        _touch(self.root, "src", "main", "resources", "a", "b", "test.dat")
        owner, fpath = resolve_unique_resource(self.root, "a\\b\\test.dat")
        self.assertEqual(owner, "main")
        self.assertTrue(os.path.exists(fpath))


# ── collect_resource_files ──────────────────────────────────────────────


class TestCollectResourceFiles(ResourceRootTestBase):
    """Tests for collect_resource_files()."""

    def test_union_from_both_roots(self):
        """Files from main + generated are merged without conflict."""
        _touch(self.root, "src", "main", "resources", "items", "a.json")
        _touch(self.root, "src", "generated", "resources", "items", "b.json")
        result = collect_resource_files(self.root, "items", ".json")
        self.assertEqual(len(result), 2)
        self.assertIn("items/a.json", result)
        self.assertIn("items/b.json", result)
        self.assertEqual(result["items/a.json"][0], "main")
        self.assertEqual(result["items/b.json"][0], "generated")

    def test_suffix_filter(self):
        """Suffix filter excludes files with non-matching extensions."""
        _touch(self.root, "src", "main", "resources", "items", "a.json")
        _touch(self.root, "src", "generated", "resources", "items", "b.txt")
        _touch(self.root, "src", "generated", "resources", "items", "c.json")
        result = collect_resource_files(self.root, "items", ".json")
        self.assertEqual(len(result), 2)
        for key in result:
            self.assertTrue(key.endswith(".json"), key)

    def test_no_suffix_filter(self):
        """No suffix → all files returned regardless of extension."""
        _touch(self.root, "src", "main", "resources", "items", "a.json")
        _touch(self.root, "src", "generated", "resources", "items", "b.txt")
        result = collect_resource_files(self.root, "items")
        self.assertEqual(len(result), 2)

    def test_empty_directories(self):
        """Non-existent or empty directory → empty dict (no crash)."""
        self.assertEqual(collect_resource_files(self.root, "nonexistent"), {})
        os.makedirs(os.path.join(self.root, "src", "main", "resources", "empty_dir"))
        self.assertEqual(collect_resource_files(self.root, "empty_dir"), {})

    def test_forward_slash_keys(self):
        """All result keys use forward slashes, never backslashes."""
        _touch(self.root, "src", "main", "resources", "items", "sub", "a.json")
        _touch(self.root, "src", "generated", "resources", "items", "b.json")
        result = collect_resource_files(self.root, "items", ".json")
        for key in result:
            self.assertNotIn("\\", key, f"Key contains backslash: {key!r}")

    def test_conflict_raises(self):
        """Same relative filename in both roots → AssertionError."""
        _touch(self.root, "src", "main", "resources", "items", "shared.json")
        _touch(self.root, "src", "generated", "resources", "items", "shared.json")
        with self.assertRaises(AssertionError) as ctx:
            collect_resource_files(self.root, "items", ".json")
        self.assertIn("conflict", str(ctx.exception).lower())
        self.assertIn("items/shared.json", str(ctx.exception))

    def test_recursive_scan(self):
        """collect_resource_files recurses into subdirectories."""
        _touch(self.root, "src", "main", "resources", "items", "top.json")
        _touch(self.root, "src", "generated", "resources", "items", "sub", "nested.json")
        result = collect_resource_files(self.root, "items", ".json")
        self.assertIn("items/top.json", result)
        self.assertIn("items/sub/nested.json", result)
        self.assertEqual(len(result), 2)

    def test_duplicate_in_same_root_ok(self):
        """Same filename in different subdirs of one root are distinct (different rel paths)."""
        _touch(self.root, "src", "main", "resources", "items", "a.json")
        _touch(self.root, "src", "main", "resources", "items", "sub", "a.json")
        result = collect_resource_files(self.root, "items", ".json")
        self.assertIn("items/a.json", result)
        self.assertIn("items/sub/a.json", result)
        self.assertEqual(len(result), 2)

    def test_path_traversal_rejected(self):
        """relative_dir with '..' raises ValueError."""
        with self.assertRaises(ValueError) as ctx:
            collect_resource_files(self.root, "items/../other")
        self.assertIn("Path traversal", str(ctx.exception))

    def test_absolute_dir_rejected(self):
        """absolute path as relative_dir raises ValueError."""
        with self.assertRaises(ValueError):
            collect_resource_files(self.root, os.path.join(self.root, "src"))


# ── Integration ─────────────────────────────────────────────────────────


class TestIntegration(ResourceRootTestBase):
    """Cross-root integration scenarios — realistic usage patterns."""

    def test_realistic_item_def_scenario(self):
        """Main has 3 items, generated has 5 items, no overlap → 8 total."""
        main_items = ["engine_a", "engine_b", "cable_special"]
        gen_items = ["machine_x", "machine_y", "machine_z", "cell_1", "cell_2"]
        for name in main_items:
            _touch(self.root, "src", "main", "resources",
                   "assets", "testmod", "items", f"{name}.json")
        for name in gen_items:
            _touch(self.root, "src", "generated", "resources",
                   "assets", "testmod", "items", f"{name}.json")
        result = collect_resource_files(
            self.root, "assets/testmod/items", ".json"
        )
        self.assertEqual(len(result), 8)
        main_owned = [k for k, (o, _) in result.items() if o == "main"]
        gen_owned = [k for k, (o, _) in result.items() if o == "generated"]
        self.assertEqual(len(main_owned), 3)
        self.assertEqual(len(gen_owned), 5)
        for name in main_items:
            key = f"assets/testmod/items/{name}.json"
            self.assertEqual(result[key][0], "main")
        for name in gen_items:
            key = f"assets/testmod/items/{name}.json"
            self.assertEqual(result[key][0], "generated")


if __name__ == "__main__":
    unittest.main()
