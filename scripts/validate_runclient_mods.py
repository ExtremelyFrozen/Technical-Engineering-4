#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Validate runClient dev mods — standard clientLocalRuntime only.
9 checks: version catalog (exact), no EMI runtime, clientLocalRuntime
unconditional (5 mods), no jar leak, JEI compileOnly, no impl/api runtime,
no dup renderNurse, no force/strictly, no stale icyllis.modernui coord.
Exit 0 = GREEN, 1 = RED.
"""

import os, sys, re, tomllib

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
for s in (sys.stdout, sys.stderr):
    if s is not None and hasattr(s, 'reconfigure'):
        try: s.reconfigure(encoding='utf-8')
        except (ValueError, OSError): pass

VERSIONS = {"jei":"29.13.0.42","jade":"26.1.8+neoforge",
            "modernfix":"5.27.18+mc26.1.2","ferritecore":"9.0.0-neoforge",
            "modernui":"pDpDBt4H"}
CLR = ["jade","jei","modernfix","ferritecore","modernui"]
FI = ["jade","modernfix","ferritecore","jei-neoforge-impl","modernui"]
FJ = ["jade","modernfix","ferritecore","jei","modernui"]

def read(path):
    if not os.path.exists(path): return None
    with open(path, "r", encoding="utf-8") as f: return f.read()

def check_version_catalog(acc):
    text = read(os.path.join(ROOT, "gradle", "forge.versions.toml"))
    if text is None: acc[0]("forge.versions.toml not found"); return
    try: cfg = tomllib.loads(text)
    except Exception as e: acc[0](f"TOML parse: {e}"); return
    v, l, b = cfg.get("versions",{}), cfg.get("libraries",{}), cfg.get("bundles",{})
    for k, ev in VERSIONS.items():
        av = v.get(k)
        if av is None: acc[0](f"versions.{k} missing")
        elif av != ev: acc[0](f"versions.{k}={av!r}, expected {ev!r}")
    for k in ["jei-common-api","jei-neoforge-api","jei-neoforge-impl","jade","modernfix","ferritecore","modernui"]:
        if k not in l: acc[0](f"libraries.{k} missing")
    if "emi" in l: acc[0]("EMI alias present (should be commented)")
    if "jei" not in b: acc[0]("bundles.jei missing")
    def chk(key, mod):
        lib = l.get(key)
        if lib and lib.get("module") != mod: acc[0](f"libraries.{key} module={lib['module']!r}, expected {mod!r}")
    chk("jade","maven.modrinth:jade"); chk("modernfix","maven.modrinth:modernfix")
    chk("ferritecore","maven.modrinth:ferrite-core")
    chk("modernui","maven.modrinth:3sjzyvGR")
    # 禁止旧 icyllis.modernui:ModernUI-NeoForge 坐标
    for k, v in l.items():
        if isinstance(v, dict) and "module" in v:
            if "icyllis.modernui" in v["module"]:
                acc[0](f"libraries.{k}: stale icyllis.modernui module={v['module']!r}")

def check_no_emi_runtime(acc):
    dep = read(os.path.join(ROOT,"dependencies.gradle"))
    if dep is None: return
    for ln in dep.split("\n"):
        s = ln.strip()
        if s.startswith("//") or s.startswith("/*") or s.startswith("*"): continue
        if re.search(r'(localRuntime|clientLocalRuntime|runtimeOnly)\s*\(.*emi',ln,re.I):
            acc[0](f"EMI in runtime: {s}")

def check_five_clr(acc):
    dep = read(os.path.join(ROOT,"dependencies.gradle"))
    if dep is None: return
    lines = dep.split("\n")
    clr_lines = [(i, ln) for i, ln in enumerate(lines)
                 if re.search(r'clientLocalRuntime\s*\(',ln) and not ln.strip().startswith("//")]
    found = set()
    for i, ln in clr_lines:
        if re.search(r'(force|strictly)\s*\(?',ln,re.I):
            acc[0](f"Line {i+1}: force/strictly: {ln.strip()}")
        for mod in CLR:
            if re.search(mod,ln,re.I): found.add(mod)
    for mod in CLR:
        if mod not in found: acc[0](f"clientLocalRuntime for '{mod}' not found")
    for i, ln in enumerate(lines):
        if re.search(r'if\s*\(.*(strictBool|findProperty|enableDev)',ln):
            for j in range(i+1,min(i+8,len(lines))):
                if re.search(r'clientLocalRuntime\s*\(',lines[j]):
                    acc[0](f"Line {j+1}: conditional (gated by if on line {i+1})")



def check_no_leak(acc):
    dep = read(os.path.join(ROOT,"dependencies.gradle"))
    if dep is None: return
    for kw in FI:
        for m in re.finditer(rf'implementation\s*\([^)]*{re.escape(kw)}',dep,re.I):
            if not m.group(0).strip().startswith("//"): acc[0](f"'{kw}' in impl: {m.group(0).strip()}")
    for kw in FJ:
        for m in re.finditer(rf'jarJar\s*\([^)]*{re.escape(kw)}',dep,re.I):
            if not m.group(0).strip().startswith("//"): acc[0](f"'{kw}' in jarJar: {m.group(0).strip()}")
    if "compileOnly(forge.bundles.jei)" not in dep: acc[0]("JEI compileOnly missing")

def check_jei_compile_only(acc):
    dep = read(os.path.join(ROOT,"dependencies.gradle"))
    if dep is None: return
    for ln in dep.split("\n"):
        if re.search(r'compileOnly.*jei',ln,re.I) and re.search(r'(if\s*\(|enableDev)',ln):
            acc[0](f"JEI compileOnly conditional: {ln.strip()}")

def check_no_impl_api(acc):
    dep = read(os.path.join(ROOT,"dependencies.gradle"))
    if dep is None: return
    for kw in CLR:
        for m in re.finditer(rf'(implementation|api)\s*\([^)]*{kw}',dep,re.I):
            if not m.group(0).strip().startswith("//"): acc[0](f"'{kw}' in impl/api: {m.group(0).strip()}")

def check_no_dup_render_nurse(acc):
    dep = read(os.path.join(ROOT,"dependencies.gradle"))
    if dep is None: acc[0]("dependencies.gradle not found"); return
    c = len(re.findall(r'renderNurseCfg\s*\(',dep))
    if c == 0: acc[0]("renderNurseCfg not found (expect 1)")
    elif c > 1: acc[0](f"renderNurseCfg {c}x (expect 1)")

def check_no_force_strictly(acc):
    dep = read(os.path.join(ROOT,"dependencies.gradle"))
    if dep is None: return
    for kw in CLR:
        for m in re.finditer(rf'(force|strictly)\s*\(?[^)]*{kw}',dep,re.I):
            if not m.group(0).strip().startswith("//"): acc[0](f"force/strictly for '{kw}': {m.group(0).strip()}")

def check_no_old_modernui_coord(acc):
    """禁止旧 icyllis.modernui:ModernUI-NeoForge 坐标用作依赖声明（忽略文档注释）。"""
    import glob as pyglob
    # 只检查可能含依赖声明的文件，不检查 docs/（文档需说明迁移原因）
    for pat in ("**/forge.versions.toml", "**/dependencies.gradle", "**/repositories.gradle"):
        for fp in pyglob.glob(os.path.join(ROOT, pat), recursive=True):
            text = read(fp)
            if not text: continue
            # TOML module 声明中的 icyllis.modernui
            for m in re.finditer(r'module\s*=\s*"[^"]*icyllis\.modernui[^"]*"', text):
                acc[0].append(f"Stale icyllis.modernui module coordinate in {os.path.relpath(fp, ROOT)}: {m.group()}")
            # Gradle 依赖坐标: 不在注释行中的 icyllis.modernui:ModernUI
            for m in re.finditer(r'^[^#*/]*icyllis\.modernui:ModernUI', text, re.M):
                acc[0].append(f"Stale icyllis.modernui Gradle coordinate in {os.path.relpath(fp, ROOT)}: {m.group().strip()}")

def main():
    errors = []; acc = (errors, [])
    checks = [
        ("1. Version catalog — exact, no EMI", check_version_catalog),
        ("2. EMI not in runtime", check_no_emi_runtime),
        ("3. clientLocalRuntime unconditional (5 mods)", check_five_clr),
        ("4. No jarJar/implementation leak", check_no_leak),
        ("5. JEI compileOnly unconditional", check_jei_compile_only),
        ("6. No implementation/api for runtime", check_no_impl_api),
        ("7. No duplicate renderNurseCfg", check_no_dup_render_nurse),
        ("8. No force/strictly on runtime mods", check_no_force_strictly),
        ("9. No stale icyllis.modernui coordinates", check_no_old_modernui_coord),
    ]
    print("="*60)
    print("  RunClient Dev Mods Validation (standard clientLocalRuntime)")
    print("="*60)
    for name, fn in checks:
        before = len(errors); fn(acc)
        n = len(errors)-before
        print(f"  {name}: {'✅ PASS' if n==0 else f'❌ FAIL ({n})'}")
    print(f"\n  Total: {len(errors)} error(s)")
    if errors:
        for e in errors[:15]: print(f"    • {e}")
        if len(errors) > 15: print(f"    … and {len(errors)-15} more")
        print("  >>> RED — Issues found"); return 1
    print("  >>> GREEN — All checks pass"); return 0

if __name__ == "__main__":
    sys.exit(main())
