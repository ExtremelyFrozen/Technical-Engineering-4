#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Rapid dual-language (en_us/zh_cn) brand & psionicant gate.

Checks: old brand absence, Java authority, lang brand keys, key category,
psionicant exact values, numbering continuity, generated zh_cn consistency.
Does NOT read en_ud or call external scripts.  Exit 0 = GREEN, 1 = RED.
"""

import json, os, re, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# ── UTF-8 stdio ──────────────────────────────────────────────────────
for s in (sys.stdout, sys.stderr):
    if s is not None and hasattr(s, 'reconfigure'):
        try: s.reconfigure(encoding='utf-8')
        except (ValueError, OSError): pass

# ── Constants ────────────────────────────────────────────────────────
EN_FORMAL = "Kenergy Engineering: Retechnicalized"
ZH_FORMAL = "科能工程:再技术化"
KEY_SHORT = "Kenergy Engineering"

FORBIDDEN = [
    re.compile('科能工程3'),
    re.compile(r'Technical Engineering\s+3'),
    re.compile(r'Technical Engineering\s+III', re.IGNORECASE),
    re.compile('科能工程\uff1a'),           # fullwidth colon
    re.compile(r'Kenergy Engineering\uff1a'),  # fullwidth colon
]

SRC = lambda *p: os.path.join(ROOT, "src", *p)
MAIN_LANG = lambda f: SRC("main", "resources", "assets", "kenergyengineering", "lang", f)
GEN_LANG = lambda f: SRC("generated", "resources", "assets", "kenergyengineering", "lang", f)
JAVA = lambda *p: SRC("main", "java", "com", "modularmc", "ten", *p)

ITEM_GROUP_KEYS = [
    "itemGroup.kenergyengineering.block", "itemGroup.kenergyengineering.machine",
    "itemGroup.kenergyengineering.item", "itemGroup.kenergyengineering.tool",
]

PSI = {
    "kenergyengineering.info.psionicant.0":
        ("Transforms specific pairs of materials into new items.",
         "将特定的两种材料转化为新的物品。"),
    "kenergyengineering.info.psionicant.1":
        ("Each recipe requires its own material pairing.",
         "每种产物都需要对应的材料组合。"),
}


def load_json(path):
    """Strict JSON parse with duplicate-key detection."""
    def _hook(pairs):
        seen = {}
        for k, v in pairs:
            if k in seen:
                raise ValueError(f"Duplicate key: '{k}'")
            seen[k] = v
        return seen
    with open(path, "r", encoding="utf-8") as f:
        return json.loads(f.read(), object_pairs_hook=_hook)


def read(path):
    """Read file as str, returning None if missing."""
    if not os.path.exists(path):
        return None
    with open(path, "r", encoding="utf-8") as f:
        return f.read()


# ── Checks ───────────────────────────────────────────────────────────

def check_old_brand(acc):
    """No old brand in Java / properties / lang files."""
    files = [JAVA("TEN.java"), JAVA("data/lang/TENLangHandler.java"),
             JAVA("common/data/TENCreativeModeTabs.java"),
             os.path.join(ROOT, "gradle.properties"),
             MAIN_LANG("en_us.json"), MAIN_LANG("zh_cn.json"),
             GEN_LANG("zh_cn.json"),
             os.path.join(ROOT, "README.md"), os.path.join(ROOT, "CONTRIBUTING.md")]
    for fp in files:
        text = read(fp)
        if text is None:
            continue
        # README / CONTRIBUTING: check exact brand positions only
        base = os.path.basename(fp).lower()
        if base == "readme.md":
            first = text.split("\n")[0].strip()
            if EN_FORMAL not in first and any(pat.search(first) for pat in FORBIDDEN):
                acc(f"README.md:L1 has old brand")
            continue
        if base == "contributing.md":
            for i, line in enumerate(text.split("\n"), 1):
                if "Kenergy Engineering" in line or "Technical Engineering" in line:
                    if any(pat.search(line) for pat in FORBIDDEN):
                        acc(f"CONTRIBUTING.md:{i} has old brand")
            continue
        # Full scan for other files
        for i, line in enumerate(text.split("\n"), 1):
            for pat in FORBIDDEN:
                if pat.search(line):
                    acc(f"{os.path.relpath(fp, ROOT)}:{i}: {pat.pattern!r}")


def check_java_authority(acc):
    """TEN.java MOD_NAME half-width colon; TENLangHandler adv.root + psionicant;
    TENCreativeModeTabs ZH_NAMES; gradle.properties mod_name with colon."""
    # TEN.java
    text = read(JAVA("TEN.java"))
    if text and EN_FORMAL not in text:
        acc("TEN.java: MOD_NAME != new brand")
    # TENLangHandler
    text = read(JAVA("data/lang/TENLangHandler.java"))
    if text:
        if EN_FORMAL not in text:
            acc("TENLangHandler.java: en adv.root != new brand")
        if ZH_FORMAL not in text:
            acc("TENLangHandler.java: zh adv.root != new brand")
        if "info.psionicant.0" not in text or "info.psionicant.1" not in text:
            acc("TENLangHandler.java: missing psionicant.0/.1")
        if "Explore..." in text or "探索..." in text:
            acc("TENLangHandler.java: psionicant placeholder remains")
    # TENCreativeModeTabs
    text = read(JAVA("common/data/TENCreativeModeTabs.java"))
    if text and text.count("科能工程:再技术化") < 4:
        acc("TENCreativeModeTabs.java: fewer than 4 new-brand ZH_NAMES")
    # gradle.properties
    text = read(os.path.join(ROOT, "gradle.properties"))
    if text:
        for line in text.split("\n"):
            m = re.match(r'^mod_name\s*=\s*(.+)', line.strip())
            if m and m.group(1) != EN_FORMAL:
                acc(f"gradle.properties: mod_name = {m.group(1)!r}")


def check_lang_brand(acc):
    """en_us / zh_cn itemGroup + adv.root use new brand."""
    for label, path in [("en_us", MAIN_LANG("en_us.json")),
                        ("zh_cn (main)", MAIN_LANG("zh_cn.json")),
                        ("zh_cn (generated)", GEN_LANG("zh_cn.json"))]:
        data = load_json(path)
        for k in ITEM_GROUP_KEYS:
            v = data.get(k, "")
            for pat in FORBIDDEN:
                if pat.search(v):
                    acc(f"{label}: {k} old brand: {pat.pattern!r}")
        ar = data.get("kenergyengineering.adv.root", "")
        for pat in FORBIDDEN:
            if pat.search(ar):
                acc(f"{label}: adv.root old brand: {pat.pattern!r}")


def check_key_category(acc):
    """key.categories.kenergyengineering remains 'Kenergy Engineering'."""
    for lang in ("en_us", "zh_cn"):
        path = MAIN_LANG(f"{lang}.json")
        if os.path.exists(path):
            v = load_json(path).get("kenergyengineering.key.categories.kenergyengineering", "")
            if v != KEY_SHORT:
                acc(f"{lang}: key category = {v!r}")


def check_psionicant(acc):
    """Exact bilingual values, no placeholders, continuous numbering."""
    for label, path, is_en in [("en_us", MAIN_LANG("en_us.json"), True),
                               ("zh_cn (main)", MAIN_LANG("zh_cn.json"), False),
                               ("zh_cn (generated)", GEN_LANG("zh_cn.json"), False)]:
        data = load_json(path)
        idxs = set()
        for key, (en_val, zh_val) in PSI.items():
            val = data.get(key)
            exp = en_val if is_en else zh_val
            if val is None:
                acc(f"{label}: missing '{key}'")
            elif val in ("Explore...", "探索..."):
                acc(f"{label}: '{key}' placeholder")
            elif val != exp:
                acc(f"{label}: '{key}' unexpected: {val!r}")
            if (m := re.match(r'kenergyengineering\.info\.psionicant\.(\d+)', key)):
                idxs.add(int(m.group(1)))
        if idxs:
            if 0 not in idxs: acc(f"{label}: missing psionicant.0")
            if 1 not in idxs: acc(f"{label}: missing psionicant.1")


def check_generated_consistency(acc):
    """generated zh_cn psionicant values match main zh_cn."""
    main = load_json(MAIN_LANG("zh_cn.json"))
    gen = load_json(GEN_LANG("zh_cn.json"))
    for key, (_, zh_val) in PSI.items():
        if gen.get(key) != zh_val:
            acc(f"generated zh_cn: '{key}' = {gen.get(key)!r}, expected {zh_val!r}")
        if main.get(key) != zh_val:
            acc(f"main zh_cn: '{key}' = {main.get(key)!r}, expected {zh_val!r}")


# ── Main ─────────────────────────────────────────────────────────────

def main():
    errors = []
    def acc(msg): errors.append(msg)

    checks = [
        ("Old brand absence", check_old_brand),
        ("Java authoritative sources", check_java_authority),
        ("Lang brand keys", check_lang_brand),
        ("Key category", check_key_category),
        ("Psionicant exact values", check_psionicant),
        ("Generated zh_cn consistency", check_generated_consistency),
    ]

    print("=" * 60)
    print("  Brand Localization & Psionicant Gate (en_us/zh_cn only)")
    print("=" * 60)

    for i, (name, fn) in enumerate(checks, 1):
        before = len(errors)
        fn(acc)
        nerr = len(errors) - before
        status = "✅ PASS" if nerr == 0 else f"❌ FAIL ({nerr})"
        print(f"  [{i}] {name}: {status}")

    print(f"\n  Total: {len(errors)} error(s)")
    if errors:
        for e in errors[:10]:
            print(f"    • {e}")
        if len(errors) > 10:
            print(f"    … and {len(errors) - 10} more")
        print("  >>> RED — Issues found")
        return 1
    print("  >>> GREEN — All checks pass")
    return 0


if __name__ == "__main__":
    sys.exit(main())
