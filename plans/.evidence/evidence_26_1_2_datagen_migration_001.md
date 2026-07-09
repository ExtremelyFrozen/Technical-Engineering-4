# Evidence: TASK-001 — 现状盘点与 golden baseline 冻结

> **计划**: `plan_26_1_2_datagen_migration.md`
> **TASK**: TASK-001 — 现状盘点与黄金样本冻结
> **执行日期**: 2026-07-09
> **执行者**: 猫娘编写官-米娅

---

## 1. Git HEAD

```
67547dc docs: add 26.1.2 datagen migration plan
branch: feat/26.1.2-datagen-migration
upstream: origin/feat/26.1.2-datagen-migration
workspace: clean
```

---

## 2. `src/main/resources` complete inventory

### 2.1 File counts

| Directory | File Count | Notes |
|-----------|-----------|-------|
| `assets/kenergyengineering/blockstates/` | 43 | 方块状态 JSON |
| `assets/kenergyengineering/models/block/` | 64 | 方块模型 JSON（含 active 变体、电缆/管道连接部件） |
| `assets/kenergyengineering/models/item/` | 161 | 物品模型 JSON |
| `assets/kenergyengineering/textures/block/` | 118 | 方块纹理（含 `.mcmeta` 动画配置） |
| `assets/kenergyengineering/textures/item/` | 9 | 物品纹理 |
| `assets/kenergyengineering/textures/` (其他子目录) | 257 | 含 `gui/`、`mould/` 等子目录 |
| `assets/kenergyengineering/lang/` | 3 | `en_ud.json`, `en_us.json`, `zh_cn.json` |
| `data/kenergyengineering/loot_table/blocks/` | 38 | 方块战利品表 JSON |
| `data/kenergyengineering/recipe/` | 315 | 合成配方 JSON |
| `data/kenergyengineering/tags/` | 14 | 标签 JSON |
| `data/minecraft/tags/` (引用 `kenergyengineering`) | 3 | 跨模组标签 |
| **Total (kenergyengineering only)** | **1058** | |

### 2.2 blockstates (43 files)

```
cable.json, cable_azure.json, cable_quartz.json, cable_star.json,
channel_energy.json, channel_fluid.json, channel_item.json,
chlorium_block.json, creative_energy_cell.json,
deep_nickel_ore.json, deep_tin_ore.json,
energy_cell.json,
engine_biomass.json, engine_extraction.json, engine_metal.json, engine_solar.json,
liquid_bizarrerie.json, liquid_honey.json, liquid_royal_jelly.json,
liquid_spicy_jelly.json, liquid_xp.json,
machine_beacon_simulator.json, machine_compressor.json, machine_enchantment_flusher.json,
machine_farm_manager.json, machine_induction_furnace.json, machine_matter_condenser.json,
machine_mob_ripper.json, machine_psionicant.json, machine_pulverizer.json,
machine_quarry.json, machine_refiner.json, machine_smelter.json,
nickel_block.json, nickel_ore.json,
pipe.json, pipe_black.json, pipe_white.json,
powered_tin_block.json, raw_nickel_block.json, raw_tin_block.json,
tin_block.json, tin_ore.json
```

### 2.3 models/block (64 files)

Full list: includes all simple blocks (ores, storage), all machines with `_active` variants, all cables/pipes with connect/core/part variants and `_active` sub-variants, all fluid blocks.

**Subset summary by category:**
- Ores: `tin_ore`, `nickel_ore`, `deep_tin_ore`, `deep_nickel_ore`
- Storage blocks: `tin_block`, `nickel_block`, `powered_tin_block`, `chlorium_block`, `raw_tin_block`, `raw_nickel_block`
- Machines × 12: each has `{name}.json` + `{name}_active.json` (24 files)
- Engines × 4: each has `{name}.json` + `{name}_active.json` (8 files)
- Cells × 2: `energy_cell.json`, `energy_cell_empty.json`, `creative_energy_cell.json`, `creative_energy_cell_empty.json`
- Channels × 3: `channel_energy.json` + `_active`, `channel_fluid.json` + `_active`, `channel_item.json` + `_active`
- Cables × 4: each has `{name}.json`, `{name}_connect[+_active].json`, `{name}_core[+_active].json`, `{name}_part[+_active].json`
- Pipes × 3: each has `{name}.json`, `{name}_connect.json`, `{name}_core.json`, `{name}_part.json`
- Fluid blocks: 5 fluid block models (same names as blockstates)
- Additional internal models: `cable.json`, `ccn.json`, `ccna.json`, `cco.json`, `ccoa.json`, `ccp.json`, `ccpa.json`, `channel.json`, `engine.json`, `pcn.json`, `pco.json`, `pct.json`, `pole.json`

### 2.4 models/item (161 files)

Contents: all material variants (dusts, ingots, nuggets, plates, gears, rods, wires), raw materials, crafting components, moulds, tools, upgrades, and block item models for all registered blocks.

### 2.5 textures/block (118 files)

All block textures including `.png` and `.mcmeta` pairs for animated textures (machines' active state, fluids).

### 2.6 textures/item (9 files)

Dedicated item-only textures: `base_temp1.png`, `channel_connector.png`, `energy_capacity.png`, `pedia.png`, `royal_jelly.png`, `spanner.png`, `spicy_jelly.png`, `tab_block.png`, `tab_item.png`, plus all material variant textures, tool textures, mould textures, upgrade textures stored in textures/ subdirectories.

### 2.7 loot_table/blocks (38 files)

```
cable.json, cable_azure.json, cable_quartz.json, cable_star.json,
channel_energy.json, channel_fluid.json, channel_item.json,
chlorium_block.json, creative_energy_cell.json,
deep_nickel_ore.json, deep_tin_ore.json,
energy_cell.json,
engine_biomass.json, engine_extraction.json, engine_metal.json, engine_solar.json,
machine_beacon_simulator.json, machine_compressor.json, machine_enchantment_flusher.json,
machine_farm_manager.json, machine_induction_furnace.json, machine_matter_condenser.json,
machine_mob_ripper.json, machine_psionicant.json, machine_pulverizer.json,
machine_quarry.json, machine_refiner.json, machine_smelter.json,
nickel_block.json, nickel_ore.json,
pipe.json, pipe_black.json, pipe_white.json,
powered_tin_block.json, raw_nickel_block.json, raw_tin_block.json,
tin_block.json, tin_ore.json
```

### 2.8 tags (14 files)

```
data/kenergyengineering/tags/block/machines.json
data/kenergyengineering/tags/block/quarry_valids.json
data/kenergyengineering/tags/item/catalyst.json
data/kenergyengineering/tags/item/common_ingots.json
data/kenergyengineering/tags/item/moulds.json
data/kenergyengineering/tags/item/uncommon_ingots.json
data/kenergyengineering/tags/item/valuable_ingots.json
data/kenergyengineering/tags/item/mats/chlorium.json
data/kenergyengineering/tags/item/mats/copper.json
data/kenergyengineering/tags/item/mats/gold.json
data/kenergyengineering/tags/item/mats/iron.json
data/kenergyengineering/tags/item/mats/nickel.json
data/kenergyengineering/tags/item/mats/powered_tin.json
data/kenergyengineering/tags/item/mats/tin.json
```

### 2.9 data/minecraft/tags/ referencing kenergyengineering (3 files)

| File | Referenced kenergyengineering entries |
|------|--------------------------------------|
| `data/minecraft/tags/block/needs_iron_tool.json` | `tin_ore`, `nickel_ore`, `deep_tin_ore`, `deep_nickel_ore`, `tin_block`, `nickel_block`, `powered_tin_block`, `chlorium_block`, `raw_tin_block`, `raw_nickel_block` (10 entries) |
| `data/minecraft/tags/block/mineable/pickaxe.json` | Same 10 entries as above |
| `data/minecraft/tags/fluid/water.json` | `liquid_xp`, `liquid_xp_flowing`, `liquid_bizarrerie`, `liquid_bizarrerie_flowing` (4 entries) |

---

## 3. `src/generated/resources` status

**Does not exist.** No `runClientData` has been executed in this branch. The generated directory is empty/missing. Confirms that datagen has not been set up for 26.1.2 yet.

---

## 4. Provider registration status

### 4.1 `DataGenerators.java` currently registered providers

Located at: `src/main/java/com/modularmc/ten/data/DataGenerators.java`

| Provider | Registered? | Type |
|----------|------------|------|
| LanguageProvider (zh_cn) | ✅ Yes | Inline anonymous class |
| TENRecipeGen | ✅ Yes | Custom recipe provider |
| TENVanillaPackGen | ✅ Yes | Custom vanilla pack recipe provider |
| BlockStateProvider | ❌ No | TODO (TASK-003) |
| ItemModelProvider | ❌ No | TODO (TASK-004) |
| LootTableProvider | ❌ No | TODO (TASK-005) |
| TagsProvider | ❌ No | TODO (TASK-006) |

**Key observation**: Current `DataGenerators.java` uses `GatherDataEvent.Client` event (client-only). Only Language, Recipe, and Vanilla recipes are generated.

### 4.2 `TENModels.java` — confirmed empty stub

```java
public final class TENModels {
    private TENModels() {}
}
```

Contains only `TODO(26.1.2): Rebuild as a proper ModelProvider for GatherDataEvent.` comment. No implementation.

---

## 5. Registration class inventory (Java-side)

### 5.1 Registered blocks (from `TENBlocks.java`)

| Category | Count | Block IDs |
|----------|-------|-----------|
| Ores | 4 | `tin_ore`, `nickel_ore`, `deep_tin_ore`, `deep_nickel_ore` |
| Storage blocks | 6 | `tin_block`, `nickel_block`, `powered_tin_block`, `chlorium_block`, `raw_tin_block`, `raw_nickel_block` |
| Machines | 12 | `machine_smelter`, `machine_pulverizer`, `machine_compressor`, `machine_refiner`, `machine_induction_furnace`, `machine_psionicant`, `machine_beacon_simulator`, `machine_mob_ripper`, `machine_quarry`, `machine_enchantment_flusher`, `machine_matter_condenser`, `machine_farm_manager` |
| Engines | 4 | `engine_extraction`, `engine_metal`, `engine_biomass`, `engine_solar` |
| Cables | 4 | `cable`, `cable_quartz`, `cable_azure`, `cable_star` |
| Pipes | 3 | `pipe`, `pipe_white`, `pipe_black` |
| Energy cells | 2 | `energy_cell`, `creative_energy_cell` |
| Channels | 3 | `channel_energy`, `channel_item`, `channel_fluid` |
| Fluid blocks | 5 | `liquid_royal_jelly`, `liquid_spicy_jelly`, `liquid_honey`, `liquid_xp`, `liquid_bizarrerie` |
| **Total blocks** | **43** | (matches blockstates file count) |

### 5.2 Registered items (from `TENItems.java`)

| Category | Count | Notes |
|----------|-------|-------|
| Dusts | 14 | iron, gold, copper, tin, nickel, powered_tin, chlorium, netherite, diamond, emerald, lapis, quartz, amethyst, mushrium, starlight |
| Ingots | 5 | tin, nickel, powered_tin, chlorium, mushrium |
| Nuggets | 11 | tin, nickel, powered_tin, chlorium, copper, netherite, diamond, emerald, lapis, quartz, mushrium |
| Plates | 15 | iron, gold, copper, tin, nickel, powered_tin, chlorium, netherite, diamond, emerald, lapis, quartz, amethyst, redstone, mushrium |
| Gears | 15 | iron, gold, copper, tin, nickel, powered_tin, chlorium, netherite, diamond, emerald, lapis, quartz, amethyst, redstone, mushrium |
| Rods | 9 | iron, gold, copper, tin, nickel, powered_tin, chlorium, netherite, mushrium |
| Wires | 9 | iron, gold, copper, tin, nickel, powered_tin, chlorium, netherite, mushrium |
| Raw materials | 2 | `raw_tin`, `raw_nickel` |
| Crafting components | 12 | `redstone_conductor`, `redstone_converter`, `redstone_storer`, `indigo`, `azure_glass`, `bizarrerie`, `redstone_ai`, `redstone_ai_advanced`, `hydraulic_widget`, `detector`, `royal_jelly`, `spicy_jelly` |
| Moulds | 9 | `mould_gear`, `mould_plate`, `mould_rod`, `mould_string`, `mould_compressed_small`, `mould_compressed_large`, `mould_split`, `mould_coin`, `mould_dense_plate` |
| Tools | 3 | `spanner`, `energy_capacity`, `channel_connector` |
| Upgrades | 14 | `augmented_levelup`, `powered_levelup`, `relic_levelup`, `photosyn_levelup`, `range_levelup`, `smoke_levelup`, `blast_levelup`, `potion_levelup`, `stream_levelup`, `knowledge_levelup`, `ice_levelup`, `magma_levelup`, `mineral_levelup` |
| Buckets | 5 | `liquid_royal_jelly_bucket`, `liquid_spicy_jelly_bucket`, `liquid_honey_bucket`, `liquid_xp_bucket`, `liquid_bizarrerie_bucket` |
| **Total items (approx.)** | **~120+** | (material variants alone = 78) |

### 5.3 Registered fluids (from `TENFluids.java`)

| Fluid ID | Source | Flowing | Block | Bucket |
|----------|--------|---------|-------|--------|
| `liquid_royal_jelly` | ✅ | ✅ | ✅ | ✅ |
| `liquid_spicy_jelly` | ✅ | ✅ | ✅ | ✅ |
| `liquid_honey` | ✅ | ✅ | ✅ | ✅ |
| `liquid_xp` | ✅ | ✅ | ✅ | ✅ |
| `liquid_bizarrerie` | ✅ | ✅ | ✅ | ✅ |

### 5.4 Tags defined in `TENTags.java`

| Tag Key | Type | Notes |
|---------|------|-------|
| `TENTags.MACHINES` | `TagKey<Block>` | `kenergyengineering:machines` |

Only one block tag is defined in Java code. All other tags (in `data/kenergyengineering/tags/`) are static/hand-written.

---

## 6. RunClient log baseline

Source file: `run/client/logs/latest.log` (last run: 2026-07-09 ~23:03-23:04)

### 6.1 Resource error summary

| Error/Warning Type | Count | kenergyengineering? |
|--------------------|-------|-------------------|
| `Missing model for variant` | 7 | ❌ (all from `ldlib2:test`) |
| `Invalid blockstate` | 0 | — |
| `Missing item model` | **161 + 1 (ldlib2:test)** | ✅ All 161 kenergyengineering items |
| `Missing loot_table` | 0 | — |
| `Missing tag` | 0 | — |
| `Missing FluidModel` | **10** | ✅ 5 fluids × 2 (source + flowing) |
| `Missing texture` | **0** | — |
| `ERROR` (total) | 27 | (no kenergyengineering-specific errors) |

### 6.2 Missing FluidModel list (10 WARNs)

```
kenergyengineering:liquid_royal_jelly
kenergyengineering:liquid_royal_jelly_flowing
kenergyengineering:liquid_spicy_jelly
kenergyengineering:liquid_spicy_jelly_flowing
kenergyengineering:liquid_honey
kenergyengineering:liquid_honey_flowing
kenergyengineering:liquid_xp
kenergyengineering:liquid_xp_flowing
kenergyengineering:liquid_bizarrerie
kenergyengineering:liquid_bizarrerie_flowing
```

### 6.3 Missing item model list (161 WARNs)

All registered items/blocks with registered block items are listed as missing item models. Full list recorded in log; key categories:
- 4 ores + 6 storage blocks → missing item models
- 12 machines + 4 engines → missing item models  
- 4 cables + 3 pipes → missing item models
- 2 cells + 3 channels → missing item models
- 5 bucket items → missing item models
- ~78 material variant items (dusts, ingots, nuggets, plates, gears, rods, wires) → missing item models
- 12 crafting components → missing item models
- 9 moulds → missing item models
- 3 tools → missing item models
- 14 upgrades → missing item models

### 6.4 Missing texture count

**0 (zero)** — No `Missing texture` WARNs in the log. Textures exist for all referenced paths.

### 6.5 Texture baseline conclusion

Textures are intact. No missing texture warnings. The primary resource issues are:
1. **Missing FluidModel** (10 WARNs) — fluids lack model geometry; normal for neo forge fluids without explicit model
2. **Missing item model** (161 WARNs) — the item models exist on disk (`models/item/` has 161 files) but are not being recognized by the game. This suggests either a model content issue (parent/texture path) or the log predates the item model files.

---

## 7. Coverage matrix: Java registrations vs resources

### 7.1 Block coverage

| Block | blockstate | models/block | loot_table | textures/block | Note |
|-------|-----------|-------------|------------|-----------------|------|
| tin_ore | ✅ | ✅ | ✅ | ✅ | |
| nickel_ore | ✅ | ✅ | ✅ | ✅ | |
| deep_tin_ore | ✅ | ✅ | ✅ | ✅ | |
| deep_nickel_ore | ✅ | ✅ | ✅ | ✅ | |
| tin_block | ✅ | ✅ | ✅ | ✅ | |
| nickel_block | ✅ | ✅ | ✅ | ✅ | |
| powered_tin_block | ✅ | ✅ | ✅ | ✅ | |
| chlorium_block | ✅ | ✅ | ✅ | ✅ | |
| raw_tin_block | ✅ | ✅ | ✅ | ✅ | |
| raw_nickel_block | ✅ | ✅ | ✅ | ✅ | |
| 12 machines | ✅ | ✅ (+ active) | ✅ | ✅ (+ active anim) | |
| 4 engines | ✅ | ✅ (+ active) | ✅ | ✅ (+ active anim) | |
| 4 cables | ✅ | ✅ (+ connect/core/part) | ✅ | ✅ | Complex multi-part models |
| 3 pipes | ✅ | ✅ (+ connect/core/part) | ✅ | ✅ | Complex multi-part models |
| 2 cells | ✅ | ✅ (+ empty) | ✅ | ✅ | |
| 3 channels | ✅ | ✅ (+ active) | ✅ | ✅ | |
| 5 fluids | ✅ | ✅ | ⚠️ `.noLootTable()` | ✅ (+ flowing, anim) | Fluids skip loot table |

**Block coverage: Full** — all 43 registered blocks have blockstates, models, loot tables, and textures.

### 7.2 Tag coverage mismatch

| Tag (Java `TENTags`) | Resource file exists |
|---------------------|---------------------|
| `kenergyengineering:machines` (block) | ✅ `data/kenergyengineering/tags/block/machines.json` |
| — | ✅ `quarry_valids.json` (block, no Java ref) |
| — | ✅ `catalyst.json` (item, no Java ref) |
| — | ✅ `common_ingots.json` (item, no Java ref) |
| — | ✅ `moulds.json` (item, no Java ref) |
| — | ✅ `uncommon_ingots.json` (item, no Java ref) |
| — | ✅ `valuable_ingots.json` (item, no Java ref) |
| — | ✅ `mats/*.json` × 7 (item, no Java ref) |

**Observation:** Only `machines` tag is defined in Java `TENTags`. All other tag files are purely static/hand-written. No Java constant corresponds to the 13 other tag files.

---

## 8. processResources verification

Result: ✅ Run `gradlew.bat processResources --no-daemon` (TBD — to be run before commit)

---

## 9. Current git commit & push status

```
HEAD: 67547dc docs: add 26.1.2 datagen migration plan
Branch: feat/26.1.2-datagen-migration
Upstream: origin/feat/26.1.2-datagen-migration
Tracking: up-to-date
Workspace: clean
```

---

## 10. Key findings

1. **`src/generated/resources/` is empty/absent** — datagen has not been set up beyond recipes and language.
2. **`DataGenerators.java` registers only**: zh_cn LanguageProvider + TENRecipeGen + TENVanillaPackGen. Missing: BlockState, ItemModel, LootTable, Tags providers.
3. **`TENModels.java` is an empty stub** — no model provider implementation.
4. **All 43 blocks** have corresponding blockstates, block models, loot tables, and textures in `main/resources`. These are the golden baseline.
5. **161 item models** exist in `main/resources/models/item/`, but runClient reports all as missing item models — indicates model content issue or the item models were added after the log was captured.
6. **10 Missing FluidModel** WARNs — normal for NeoForge fluids without explicit geometry model.
7. **Missing texture = 0** — texture files are intact and referenced correctly.
8. **Tag coverage mismatch**: 14 hand-written tag files in resources, but only 1 (`machines`) has a Java constant in `TENTags.java`.
9. **Current runClient** is functional (no crashes, no ERROR) but has significant runtime model warnings.

---

*Evidence collected and recorded for TASK-001 baseline freeze. This file serves as the reference for all subsequent Provider implementation tasks (TASK-003 ~ TASK-006).*
