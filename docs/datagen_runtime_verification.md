# Datagen Runtime Verification — TASK-010 + TASK-011 (Item Definition Layer — Round 2)

**Date:** 2026-07-10 (updated with Round 2 fix + fresh client verification)
**Branch:** `feat/26.1.2-datagen-migration`
**Previous Commit (baseline):** `1981fba` — 162 missing item models
**Round 1 (TASK-010):** `14b66c4` — `TENModelProvider` item definition code added (existence fix)
**Round 2 (TASK-011):** Current — model reference path fix (non-block/bucket items referenced wrong paths)
**Command (datagen):** `gradlew.bat runClientData --no-daemon` (✅ BUILD SUCCESSFUL, 10m18s)
**Command (validation):** `python scripts/validate_item_definitions.py` (✅ 161/161 pass, all model refs resolve)
**Command (client):** `gradlew.bat runClient --no-daemon` (✅ loaded, 0 missing item models)
**Client Verification Evidence:**
  - `run/client/logs/latest.log` (Jul 10 06:17–06:29) — **0 missing item models** ✅ (Round 2)
  - `run/client/logs/2026-07-09-5.log.gz` (22:00–22:07) — **0 missing item models** ✅ (Round 1)
  - `run/client/logs/2026-07-10-1.log.gz` (01:26) — 162 missing item models ❌ (BEFORE any fix)
  - `scripts/validate_item_definitions.py` v2 — static model reference resolution: **161/161 resolve** ✅

---

---

## 0. Cross-Validation with Hoarding

**Reference project:** `Hoarding-26.1.2` — a working NeoForge 26.1.2 mod with correct item definitions.

### Key finding: 26.1.2 item model chain requires `items/<id>.json` layer

In NeoForge 26.1.2 / Minecraft 1.21.5, the item model resolution chain changed:
- **Old (pre-26.1.2):** `assets/<modid>/models/item/<id>.json` → loaded directly as item model
- **New (26.1.2+):** `assets/<modid>/items/<id>.json` → defines `{"model":{"type":"minecraft:model","model":"<model_location>"}}` → references block or item model

Hoarding generates `items/<id>.json` for every block (→ `hoarding:block/<id>`) and standalone items (→ `hoarding:item/<id>`), e.g.:
```json
{"model":{"type":"minecraft:model","model":"hoarding:block/flint_block"}}
```

### What was NOT changed (across both rounds)
- `blockstates/` variants/multipart format — left as-is (confirmed compatible with Hoarding)
- `models/item/*.json` — retained (161 files, still required as model targets)
- JEI/EMI/GUI/Capability — untouched
- FluidModel — not handled (separate issue)

---

## 1. Summary

| Category | Status |
|---|---|
| Mod Loading | ✅ 5 mods loaded (Kenergy Engineering 4.1.0, ldlib2, Minecraft, NeoForge, Test Framework) |
| Datagen (`runClientData`) | ✅ BUILD SUCCESSFUL — 696 files cached, 161 item definitions regenerated |
| Item Definitions (`items/*.json`) | ✅ **161 files** — all present, valid JSON, model refs resolve |
| Static Model Resolution (`validate_item_definitions.py`) | ✅ **161/161** model references resolve to actual model files |
| `models/item/*.json` Preservation | ✅ 161 files retained, none deleted |
| World Creation | ✅ Integrated server started, world generated, player joined |
| Crashes | ❌ None |
| Fatal Errors | ❌ None |
| Minecraft-side ERROR lines | **0** |
| **Missing Item Model (kenergyengineering)** | ✅ **0** — CONFIRMED via `latest.log` (Jul 10 06:29) |
| WARN lines (post-fix, known remaining) | FluidModel: 10, ldlib2 variant: 7, vanilla commands: 10, sprite mip: 2 |

---

## 2. Warning Breakdown

| Warning Type | Count | Scope | Severity | Status |
|---|---|---|---|---|
| Missing item model | **0** | kenergyengineering items | — | ✅ **RESOLVED** |
| Missing FluidModel | 10 | kenergyengineering fluid blocks (still+flowing for 5 fluids) | ⚠️ Medium | ⏳ Known, separate issue |
| Missing model for variant | 7 | ldlib2:test block (6 facing dirs + renderer_block) | ⚠️ Low | 🔗 External dep (ldlib2) |
| Ambiguous command arguments | 10 | Vanilla commands (teleport, time, waypoint) | ℹ️ Vanilla | 🔵 Vanilla behavior |
| SpriteLoader mip level | 2 | testframework:white (4x4) + blocks atlas | ℹ️ Cosmetic | 🔵 Cosmetic |

### 2.1 Missing Item Models — ✅ CONFIRMED RESOLVED (Both Rounds)

**Status:** ✅ **Zero missing item models confirmed** via runtime client logs:

| Log File | Timestamp | Missing Item Models | Round | Result |
|---|---|---|---|---|
| `latest.log` | Jul 10 06:29 | **0** | **R2** ✅ | **After Round 2 fix** |
| `2026-07-09-5.log.gz` | Jul 9 22:07 | **0** | R1 ✅ | After Round 1 fix |
| `2026-07-09-8.log.gz` | Jul 10 02:43 | **0** | R1 ✅ | After Round 1 fix |
| `2026-07-10-1.log.gz` | Jul 10 01:26 | 162 | — ❌ | Before any fix |

**Previous count (before any fix):** 162 (161 kenergyengineering + 1 ldlib2:test)
**Post-Round-2 count:** **0**

### Round 2 Fix Details (Current)

**Problem discovered during code review (审查官艾琳):**
After Round 1 fixed item definition *existence*, the item definition `model` field was incorrectly reusing the *texture path* from `models/item/*.json`'s `layer0`, instead of pointing to the actual *model file location*.

**Before (Round 1 — WRONG):**
| TE4 Item | Item Definition | Model Reference (wrong) | Actual Model File |
|---|---|---|---|
| `iron_dust` (item) | `items/iron_dust.json` | → `kenergyengineering:item/material/dust/iron_dust` ❌ | `models/item/iron_dust.json` |
| `augmented_levelup` (item) | `items/augmented_levelup.json` | → `kenergyengineering:item/upgrade/augmented_levelup` ❌ | `models/item/augmented_levelup.json` |
| `mould_gear` (item) | `items/mould_gear.json` | → `kenergyengineering:item/mold/model_gear` ❌ | `models/item/mould_gear.json` |
| `redstone_conductor` (item) | `items/redstone_conductor.json` | → `kenergyengineering:item/crafting/redstone_conductor` ❌ | `models/item/redstone_conductor.json` |
| `liquid_xp_bucket` (bucket) | `items/liquid_xp_bucket.json` | → `kenergyengineering:block/liquid_xp_bucket` ❌ | `models/item/liquid_xp_bucket.json` |

**After (Round 2 — CORRECT):**
| TE4 Item | Item Definition | Model Reference (correct) | Actual Model File |
|---|---|---|---|
| `iron_dust` (item) | `items/iron_dust.json` | → `kenergyengineering:item/iron_dust` ✅ | `models/item/iron_dust.json` |
| `augmented_levelup` (item) | `items/augmented_levelup.json` | → `kenergyengineering:item/augmented_levelup` ✅ | `models/item/augmented_levelup.json` |
| `mould_gear` (item) | `items/mould_gear.json` | → `kenergyengineering:item/mould_gear` ✅ | `models/item/mould_gear.json` |
| `redstone_conductor` (item) | `items/redstone_conductor.json` | → `kenergyengineering:item/redstone_conductor` ✅ | `models/item/redstone_conductor.json` |
| `liquid_xp_bucket` (bucket) | `items/liquid_xp_bucket.json` | → `kenergyengineering:item/liquid_xp_bucket` ✅ | `models/item/liquid_xp_bucket.json` |
| `tin_ore` (block, unchanged) | `items/tin_ore.json` | → `kenergyengineering:block/tin_ore` ✅ | `models/block/tin_ore.json` (via parent) |

**Code changes in `TENModelProvider.java`:**
1. **Non-block item definitions (lines 261–272):** Simplified from complex switch-based `modelRef` logic to a flat `modId + ":item/" + name`. The item definition's `model` field now points to the model file location (`kenergyengineering:item/<name>`), keeping texture paths inside `models/item/*.json` separate.
2. **Bucket item definitions (lines 274–281):** Changed from `modId + ":block/" + bucketName` to `modId + ":item/" + bucketName`, since bucket models live at `models/item/<bucket_name>.json`.
3. **Block item definitions (line 256–258):** Unchanged — still correctly reference `modId + ":block/" + name`.

**Separation of concerns enforced:** The item definition's `model` field is now the *model file location* only. Texture paths (e.g., `kenergyengineering:item/material/dust/iron_dust`) remain inside `models/item/*.json` as `layer0` values and are no longer duplicated into item definitions.

**Validation script upgrade (`validate_item_definitions.py` v2):**
- Added `resolve_model_path()` function that converts model references (e.g., `kenergyengineering:block/tin_ore`) to filesystem paths and checks they exist in either `src/generated/resources/` or `src/main/resources/`.
- Extended validation output to include unresolved model reference count.
- Script now runs as `GREEN` only when all 161 item definitions exist, are valid JSON, and all model references resolve to actual files.

### 2.2 Missing FluidModels

| Fluid | Still | Flowing |
|---|---|---|
| `kenergyengineering:liquid_royal_jelly` | ❌ | ❌ |
| `kenergyengineering:liquid_spicy_jelly` | ❌ | ❌ |
| `kenergyengineering:liquid_honey` | ❌ | ❌ |
| `kenergyengineering:liquid_xp` | ❌ | ❌ |
| `kenergyengineering:liquid_bizarrerie` | ❌ | ❌ |

Expected: fluids need explicit model registration or custom renderers in NeoForge 1.21.

### 2.3 Other Categories — Clean

| Check | Result |
|---|---|
| Missing texture | 0 |
| Missing translation | 0 |
| Missing tag | 0 |
| Missing recipe | 0 |
| Missing sound | 0 (known pre-existing: 1 starlight.ogg) |
| Invalid blockstate | 0 |

All datagen-provided categories (language, tags, recipes, loot tables) pass without warnings.

---

## 3. Loaded Mods

| Mod ID | Version |
|---|---|
| `kenergyengineering` | 4.1.0 |
| `ldlib2` | 26.1.2.28 (was 26.1.2.27 — upgraded for tooltip fix TASK-011) |
| `minecraft` | 26.1.2 |
| `neoforge` | 26.1.2.78 |
| `testframework` | 26.1.2.78 |

---

## 4. Recipes & Advancements

- 1830 recipes loaded (✅)
- 1617 advancements loaded (44 after player join) (✅)
- 0 recipe priority overrides (✅, expected)

---

## 5. Known Limitations

1. **Item definitions fixed (Round 2)** ✅ — All 161 kenergyengineering item definitions generated with correct model references. **Three independent `runClient` executions confirm zero missing item model warnings at runtime** (Round 1: logs 5 and 8; Round 2: `latest.log` Jul 10 06:29). Static validation (`validate_item_definitions.py` v2) confirms **161/161 model references resolve**.
2. **Missing FluidModels (10)** — Fluids lack block models in current datagen. May be acceptable if fluids use custom rendering (ldlib2 renderer). **Recommended as follow-up task** — independent of item definition fix, involves `FluidModel` registration in NeoForge 26.1.2.
3. **Missing sound asset (1)** — `kenergyengineering:sounds/starlight.ogg` referenced in sound definitions but file does not exist. Pre-existing.
4. **ldlib2:test variant warnings (7)** — External dependency, not in TE4 scope.
5. **Dev environment only** — This verification was run via `runClientData` and `runClient` in a NeoForge dev workspace. Some warnings may not appear in a production build.
6. **No gameplay test** — Verification covers datagen output validation only. Machine functionality, GUIs, and block interactions were not tested.

---

## 6. TASK-011: LDLib2 26.1.2.27 → 26.1.2.28 Container Tooltip Fix

**Date:** 2026-07-10
**Branch:** `feat/26.1.2-datagen-migration`
**Commit:** 13652c6 + unstaged changes (not yet committed)

### Root Cause

LDLib2 `AbstractContainerScreenMixin.ldlib2$renderTooltips` (`@Inject(method="extractTooltip", at=@At("HEAD"), cancellable=true)`) unconditionally called `ci.cancel()` when `getMenu() instanceof IItemSlotHolderMenu`, suppressing ALL tooltip rendering (both vanilla and mod items) in any container screen.

### Fix

Upstream commit [3744f67e](https://github.com/Low-Drag-MC/LDLib2/commit/3744f67e39920ded7a742aa654cdadaf4f07fd8a) added an `isItemSlot(this.hoveredSlot)` guard — only cancel tooltip rendering for LDLib-managed slots, letting vanilla slots render normally.

### Bytecode Evidence (`javap -c -p`)

**26.1.2.27 (BUGGY):**
```
private void ldlib2$renderTooltips(GuiGraphicsExtractor, int, int, CallbackInfo);
  Code:
    0: aload_0
    1: invokevirtual getMenu:()AbstractContainerMenu;
    4: instanceof IItemSlotHolderMenu
    7: ifeq 15           ← skip if NOT IItemSlotHolderMenu
   10: aload 4
   12: invokevirtual ci.cancel:()V    ← CANCEL unconditionally!
   15: return
```

**26.1.2.28 (FIXED):**
```
private void ldlib2$renderTooltips(GuiGraphicsExtractor, int, int, CallbackInfo);
  Code:
    0: aload_0
    1: invokevirtual getMenu:()AbstractContainerMenu;
    4: astore 6
    6: aload 6
    8: instanceof IItemSlotHolderMenu
   11: ifeq 40
   14: aload 6
   16: checkcast IItemSlotHolderMenu
   19: astore 5
   21: aload 5
   23: aload_0
   24: getfield hoveredSlot:()Slot;
   27: invokeinterface IItemSlotHolderMenu.isItemSlot:(Slot;)Z  ← NEW guard!
   32: ifeq 40           ← skip cancel if NOT LDLib-managed slot
   35: aload 4
   37: invokevirtual ci.cancel:()V    ← Only cancel for LDLib-owned slots
   40: return
```

### Changes Made

- `gradle/forge.versions.toml`: `ldlib2` from `26.1.2.27` → `26.1.2.28`
- `scripts/validate_ldlib_tooltip_fix.py`: New regression check
- `docs/datagen_runtime_verification.md`: This update

### Verification Results

| Check | Result |
|---|---|
| Version string | ✅ `ldlib2 = "26.1.2.28"` in `forge.versions.toml` |
| Dependency resolution | ✅ `ldlib2-neoforge-26.1:26.1.2.28` resolved in all configurations |
| Jar hash | `fc2b6d81999d632a6dc02c4f42849dd2ef9047a4` |
| Mixin class found | ✅ `AbstractContainerScreenMixin.class` present |
| `isItemSlot` check in `ldlib2$renderTooltips` | ✅ CONFIRMED in bytecode |
| Regression script (RED: 26.1.2.27) | ❌ FAILED (expected — version mismatch + no isItemSlot in bytecode) |
| Regression script (GREEN: 26.1.2.28) | ✅ PASSED (version match + isItemSlot confirmed in bytecode) |
| `compileJava` | ✅ BUILD SUCCESSFUL |
| `runClient` startup | ✅ Loaded, 0 ERROR/FATAL, 0 missing item models |
| Running LDLib2 version in client | ✅ `LowDragLib2 26.1.2.28 (ldlib2)` confirmed in `latest.log` |
| **Visual tooltip verification** | ⏳ **Pending human confirmation** — cannot automate visual GUI inspection |

### Residual Risk

| Risk | Mitigation |
|---|---|
| 26.1.2.28 introduces new regressions | No ldlib2-related errors in `latest.log`. If found, revert to 26.1.2.27 and document in `docs/known_issues.md` |
| Visual tooltip not confirmed | Bytecode evidence confirms fix logic. Upstream commit [3744f67e](https://github.com/Low-Drag-MC/LDLib2/commit/3744f67e39920ded7a742aa654cdadaf4f07fd8a) independently verified. Human to visually confirm: open inventory/chest/container screen, verify vanilla items (dirt, diamond) and TE4 items show tooltip on hover. |
| FluidModel 10 WARN still present | Pre-existing, not in scope of TASK-011 |

---

## 7. Conclusion

**Item definition layer fix (Round 2): ✅ CONFIRMED.**

Round 2 fixed a subtle but critical issue: item definition `model` references were incorrectly pointing to *texture paths* (e.g., `kenergyengineering:item/material/dust/iron_dust`) or *block paths* for buckets (`kenergyengineering:block/liquid_xp_bucket`), instead of the actual *model file locations* (`kenergyengineering:item/iron_dust`, `kenergyengineering:item/liquid_xp_bucket`).

**Fix summary:**
- `TENModelProvider.java`: Simplified non-block item definitions to use flat `modId + ":item/" + name` model references; fixed bucket definitions to use `:item/` instead of `:block/`; block items unchanged.
- `validate_item_definitions.py` v2: Added model file resolution checking — statically verifies that every item definition's model reference resolves to an actual `models/block/*.json` or `models/item/*.json` file.
- `runClient` (Jul 10 06:29): **0 missing item models, 0 ERRORs, 0 FATALs.**

Remaining warnings (all pre-existing, none blocking):

- **Missing FluidModels (10):** Independent issue — recommended as separate follow-up task.
- **ldlib2:test variant warnings (7):** External dependency, not in TE4 scope.
- **Vanilla command ambiguity (10):** Vanilla behavior, not actionable.
- **SpriteLoader mip level (2):** Cosmetic, not actionable.

**Item definition layer is complete and correct.**

---

## 8. TASK-012: TE4 Custom Block Item Tooltip Restoration (Round 1 + Round 2)

**Date:** 2026-07-10 (Round 1) / 2026-07-10 (Round 2 — bare key fix)
**Branch:** `feat/26.1.2-datagen-migration`
**Base:** Commit 756ff21 (TASK-011 LDLib2 26.1.2.28 upgrade)

### Root Cause (Original)

21 block items with existing lang tooltip data (12 machines, 4 engines, energy_cell, 4 cables) were registered as plain `BlockItem` via `ITEMS.registerSimpleBlockItem()`. The existing `TENBaseBlockItem` class (which extends `BlockItem`) was not used. Additionally, `machine_pulverizer` tooltip lang keys were numbered `.0, .1, .2, .4` (gap at index 3), causing the iteration loop to terminate early after finding no `.3` key.

### Root Cause (Round 2 — Bare Key Regression)

After Round 1, all 21 target block items correctly used `TENBaseBlockItem` with custom tooltips. However, a visual regression was found: **all 21 items displayed their registry lang key as the item name** (e.g., `item.kenergyengineering.machine_smelter`) instead of the translated name (e.g., "Smelter" / "熔炼机").

**Root cause:** `TENBlocks.registerTENBaseBlockItem()` was creating `Item.Properties` with only `.setId(key)`, missing the `.useBlockDescriptionPrefix()` call required by NeoForge 26.1.2 for `BlockItem` subclasses. Without this flag, the `BlockItem` uses `item.<modid>.<path>` as its description ID, but the lang files only define `block.<modid>.<path>` keys. Items registered via `registerSimpleBlockItem()` (ores, storage, pipes, channels, creative_energy_cell) were unaffected because NeoForge's implementation automatically adds `.useBlockDescriptionPrefix()`.

**Before (Round 1 — WRONG):**
```java
ITEMS.register(name, () -> new TENBaseBlockItem(holder.get(), new Item.Properties().setId(key)));
// Item description ID → "item.kenergyengineering.<id>" — NO matching lang key → bare key display
```

**After (Round 2 — CORRECT):**
```java
ITEMS.register(name, () -> new TENBaseBlockItem(holder.get(), new Item.Properties().useBlockDescriptionPrefix().setId(key)));
// Item description ID → "block.kenergyengineering.<id>" — matches existing lang keys → translated name
```

### Changes Made

| File | Change |
|---|---|
| `TENBaseBlockItem.java` | New `resolveKeyPrefix()` method: machine_x → `info.x.`, engine_x → `info.engine_x.`, energy_cell → `info.energy_cell.`, cable/cable_x → direct key `<id>.`, others → null (no tooltip) |
| `TENBlocks.java` | **Round 1:** 21 targets switched to `registerTENBaseBlockItem()` helper → `TENBaseBlockItem`; pipes/channels/ores/storage/creative_energy_cell remain plain `BlockItem` |
| `TENBlocks.java` | **Round 2:** Added `.useBlockDescriptionPrefix()` to `Item.Properties` chain in `registerTENBaseBlockItem()` — 1 line changed |
| `TENLangHandler.java` | `info.pulverizer.4` → `.3` (authoritative datagen source) |
| `en_us.json` | `info.pulverizer.4` → `.3` |
| `zh_cn.json` (main + generated) | `info.pulverizer.4` → `.3` |
| `en_ud.json` | `info.pulverizer.4` → `.3` |
| `scripts/validate_custom_block_tooltips.py` | New static validation script (Round 1) + extended with checks 5/6 for bare key detection (Round 2) |

### Static Validation Results (`validate_custom_block_tooltips.py`)

| Check | Result |
|---|---|
| Registration: 21 targets use TENBaseBlockItem | ✅ 21/21 PASS |
| Registration: exclusions not switched | ✅ 0 violations (ores/storage/pipes/channels/creative_energy_cell all plain BlockItem) |
| Key resolver: machine_/engine_/cable/energy_cell coverage | ✅ All 4 categories covered |
| Key resolver: null return for non-targets | ✅ Present |
| Cable resolver: no `info.` prefix | ✅ Correct (direct key) |
| Lang prefix sanity: all 21 targets resolve in en_us | ✅ 21/21 |
| Lang prefix sanity: all 21 targets resolve in zh_cn | ✅ 21/21 |
| Lang continuity: en_us keys continuous (0..N) | ✅ All continuous |
| Lang continuity: zh_cn keys continuous (0..N) | ✅ All continuous |
| Pulverizer gap fix (was .4, now .3) | ✅ Continuous 0..3 (4 keys) |
| **BlockItem description prefix** (Round 2) | ✅ `.useBlockDescriptionPrefix()` present — 21/21 targets use block prefix |
| **Name key integrity** (Round 2) | ✅ 21/21 block.<id> exist in en_us+zh_cn; 0 item.<id> for targets; 0 empty/bare values |
| **Name key statistics** (Round 2) | `block.*`: 43 en + 43 zh; `item.*`: 123 en + 123 zh — counts match |

### Compilation

| Round | Command | Result |
|---|---|---|
| Round 1 | `.\gradlew.bat compileJava --no-daemon` | ✅ BUILD SUCCESSFUL (41s) |
| Round 2 | `.\gradlew.bat compileJava --no-daemon` | ✅ BUILD SUCCESSFUL (38s) |

### Runtime Log Analysis (runClient Jul 10 22:38–22:51)

| Category | Count | Status |
|---|---|---|
| ERROR/FATAL (kenergyengineering) | **0** | ✅ |
| Missing item model (kenergyengineering) | **0** | ✅ (preserved from TASK-011) |
| Missing FluidModel | 10 | ⚠️ Pre-existing (5 fluids × still+flowing) |
| ldlib2:test variant/model warnings | 7+1 | ℹ️ External dep, pre-existing |
| Vanilla command ambiguity | 10 | ℹ️ Vanilla behavior |
| SpriteLoader mip level | 2 | ℹ️ Cosmetic |

### Round 2 Verification — Bare Key Fix

**RED phase (before fix):**
- Check 5/6 `useBlockDescriptionPrefix`: **MISSING** — all 21 targets [FAIL]
- Check 6/6 name integrity: ✅ PASS (keys exist, just not being looked up)
- Confirmed: `registerTENBaseBlockItem` had `new Item.Properties().setId(key)` without `.useBlockDescriptionPrefix()`

**GREEN phase (after fix):**
- Check 5/6 `useBlockDescriptionPrefix`: **present** — 21/21 [OK]
- Check 6/6 name integrity: ✅ PASS — 21 block.<id> keys in both langs, no item.<id> for targets
- Name statistics: 43 `block.*` keys (en=zh), 123 `item.*` keys (en=zh), no mismatches
- `compileJava`: ✅ BUILD SUCCESSFUL

**Key evidence:** The lang files already contained all 21 `block.kenergyengineering.<id>` translation keys. The only issue was that `Item.Properties` was missing `.useBlockDescriptionPrefix()`. With the fix, the BlockItem now correctly resolves to `block.kenergyengineering.<id>` and displays the translated name.

### Visual Verification

⏳ **Pending human confirmation (both rounds).** The following items should display:
1. **Correct translated name** (via `block.kenergyengineering.<id>`) instead of bare key — fixed in Round 2
2. **Custom tooltips** on hover in creative mode inventory / JEI / container screens — fixed in Round 1

- **Machines (12):** smelter (3 lines), pulverizer (4 lines), compressor (2 lines), refiner (2 lines), induction_furnace (2 lines), psionicant (1 line), beacon_simulator (4 lines), mob_ripper (2 lines), quarry (3 lines), enchantment_flusher (3 lines), matter_condenser (3 lines), farm_manager (3 lines)
- **Engines (4):** extraction (2 lines), metal (4 lines), biomass (2 lines), solar (2 lines)
- **Energy cell:** 2 lines (identical to `info.cell.*` content)
- **Cables (4):** cable ("Transfer: 1 kFE"), cable_azure ("Transfer: 100 kFE"), cable_quartz ("Transfer: 10 kFE"), cable_star ("Transfer: Infinite FE")

**Command to reproduce:** Open creative inventory → Machines tab → Hover over each machine/engine/cell/cable. Verify:
1. Item name is translated (not a bare key)
2. Gold-colored tooltip text appears

### Regression Risks

| Risk | Mitigation |
|---|---|
| All block items showing tooltip (false positive) | Key resolver returns null for non-target prefixes — safe by design |
| Cable showing `info.` prefix tooltip | Cable branch returns direct key without `info.` — verified by static check |
| Non-target block items affected | All exclusions confirmed as `BlockItem` (not `TENBaseBlockItem`) |
| Bare key regression in new BlockItem subclasses | Validation script check 5/6 now detects missing `.useBlockDescriptionPrefix()` — future regression caught at CI time |
| FluidModel 10 WARN still present | Pre-existing, not in scope of TASK-012 |

---

## 9. TASK-013: Script UTF-8 Precautions (Preventative Encoding Standardization)

**Date:** 2026-07-10
**Branch:** `feat/26.1.2-datagen-migration`
**Base:** TASK-012 uncommitted changes preserved

### Changes Made

| File | Change | Lines |
|---|---|---|
| `scripts/validate_item_definitions.py` | Added `_configure_stdio_utf8()` function + call in `main()` | +8 lines |
| `scripts/validate_ldlib_tooltip_fix.py` | Added `_configure_stdio_utf8()` function + call in `main()`; fixed 2× `subprocess.run(..., text=True)` → added `encoding='utf-8', errors='replace'` | +12 lines |
| `scripts/validate_custom_block_tooltips.py` | Added `_configure_stdio_utf8()` function + call in `main()` | +8 lines |
| `run_client.bat` | Replaced `@gradlew runClient` with `@echo off` + `chcp 65001 >nul` + `call gradlew.bat runClient` — **本地开发入口，仅当前机器有效，不进入 commit** | 3 lines (UTF-8 no BOM) |

**交付说明：** 三个 Python 脚本（`validate_item_definitions.py`、`validate_ldlib_tooltip_fix.py`、`validate_custom_block_tooltips.py`）作为仓库交付，纳入 commit；`run_client.bat` 为本地开发便利脚本，UTF-8 编码修正仅当前机器有效，不纳入版本控制。

### Verification Results

| Check | Status | Evidence |
|---|---|---|
| **UTF-8 decoding** — all 4 target files | ✅ All decodable | `first3=23 21 2F` (Python: `#!`) / `40 65 63` (bat: `@ec`) — all clean |
| **BOM check** — Python files | ✅ No BOM | No `EF BB BF` prefix in any script |
| **BOM check** — `run_client.bat` | ✅ No BOM (correct) | First 3 bytes = `40 65 63` (`@ec` from `@echo off`) |
| **`validate_item_definitions.py`** | ✅ PASS (exit 0) | 161/161 items present, valid, model refs resolve (GREEN) |
| **`validate_ldlib_tooltip_fix.py`** | ✅ PASS (exit 0) | ldlib2 26.1.2.28 — version match + isItemSlot confirmed in bytecode |
| **`validate_custom_block_tooltips.py`** | ✅ PASS (exit 0) | 21/21 targets use TENBaseBlockItem, 0 lang continuity errors (GREEN) |
| **Pipe test (reconfigure)** — `validate_item_definitions.py` | ✅ No crash | Output piped through `2>&1 | Select-Object -First 3`, first lines emitted correctly |
| **Pipe test (reconfigure)** — `validate_ldlib_tooltip_fix.py` | ✅ No crash | Same — `reconfigure()` does not raise when piped |
| **Pipe test (reconfigure)** — `validate_custom_block_tooltips.py` | ✅ No crash | Same — `reconfigure()` does not raise when piped |
| **Static: `_configure_stdio_utf8` defined** | ✅ All 3 scripts | Function defined after imports, called at top of `main()` |
| **Static: `_configure_stdio_utf8` calls `reconfigure(encoding='utf-8')`** | ✅ All 3 | Pattern: `hasattr(stream, 'reconfigure')` → `try/except (ValueError, OSError): pass` |
| **Static: `subprocess.run` encoding** | ✅ Both calls fixed | 全部两处调用均添加 `encoding='utf-8', errors='replace'` |
| **Static: `run_client.bat` content** | ✅ Correct | `@echo off` + `chcp 65001 >nul` + `call gradlew.bat runClient` |
| **TASK-012 uncommitted changes** | ✅ Preserved | All 9 TASK-012 files still in `git diff --name-only` output |

### CI / PowerShell Scan

| Area | Status |
|---|---|
| `.github/workflows/` (12 files) | ✅ No Chinese output — contains only English labels and GitHub Actions expressions |
| `.ps1` files | ✅ None exist in project |
| Project .gitignore | `run_client.bat` listed — file is local dev artifact, changes recorded on disk only |

### Static Code Compliance

- `_configure_stdio_utf8()`: uses `hasattr(stream, 'reconfigure')` guard, catches only `(ValueError, OSError)`, does not swallow other exceptions ✅
- All I/O boundaries in `validate_ldlib_tooltip_fix.py` already explicit: `open()` calls use `encoding="utf-8"`, zipfile operations are binary ✅
- No business logic touched, no Chinese test text added ✅
- No `JAVA_TOOL_OPTIONS` set in `run_client.bat` ✅

### Residual Risk

| Risk | Status |
|---|---|
| Python < 3.7 without `stream.reconfigure()` | ✅ `hasattr` guard handles gracefully — function is no-op |
| `reconfigure()` fails on non-text streams (pipes, redirected output) | ✅ `ValueError` caught — no crash |
| Future `text=True` subprocess added without explicit encoding | ⚠️ Low — code review / linting should catch |
| `run_client.bat` gitignored — version-controlled copy not maintained | ⚠️ Low — file is local dev convenience, not part of CI/CD |
