# Evidence: runClient 日志资源错误分类清单

> **计划ID**: `26_1_2_resource_fix`
> **TASK**: TASK-A — 复现并提取 runClient 日志中的资源错误清单
> **日志源**: `build/resource_fix_log.txt`
> **运行时间**: 2026-07-09 22:08:40 ~ 22:18:11 (10m 6s)
> **构建结果**: BUILD SUCCESSFUL
> **创建日期**: 2026-07-09
> **创建者**: 猫娘编写官-米娅

---

## 运行概要

- 游戏成功启动并进入主菜单
- 自动进入世界（新的世界）
- 约 1 分钟后玩家手动退出（客户端窗口关闭）
- 服务器正常停止
- **BUILD SUCCESSFUL** — 无阻塞性崩溃

---

## 错误分类统计

| # | 问题类别 | 严重度 | 数量 | 行号范围 |
|---|---------|--------|------|---------|
| 1 | 配方解析 ERROR | **ERROR** | **133** | L132–L264 |
| 2 | Tag 引用缺失 ERROR | **ERROR** | **2** | L127–L131 |
| 3 | Missing FluidModel WARN | **WARN** | **10** | L92–L101 |
| 4 | Missing item model WARN (kenergyengineering) | **WARN** | **161** | L268–L428 |
| 5 | Missing model variant WARN (ldlib2 test block) | **WARN** | **7** | L102–L108 |
| 6 | 音效缺失 WARN | **WARN** | **1** | L86 |
| 7 | Missing translation / untranslated | — | **0** | — |
| 8 | Texture not found / Missing texture | — | **0** | — |
| 9 | JSON 解析错误（非配方） | — | **0** | — |

---

## 1. 配方解析 ERROR — 133 个

**错误特征**: `Couldn't parse data file 'kenergyengineering:vanilla/...'`  
**影响**: 配方不加载，相关合成不可用

### 子类 A: `neoforge:ingredient_type` 缺失 — 约 116 个

**错误消息特征**: `No key neoforge:ingredient_type in MapLike[{...}]`

**根因**: 配方中 ingredient 字段使用了简单 `{"item": "..."}` / `{"tag": "..."}` 格式，但 NeoForge 26.1.2 要求这些配方显式指定 `neoforge:ingredient_type`。

**失败文件清单（按子目录分组）**:

- **`vanilla/burning/`** (26+ 文件):
  - `iron_ingot_fd`, `iron_ingot_fd_blast`
  - `tin_ingot`, `tin_ingot_fd`, `tin_ingot_fd_blast`, `tin_ingot_blast`, `tin_ingot_raw`, `tin_ingot_raw_blast`, `tin_ingot_deep`, `tin_ingot_deep_blast`
  - `nickel_ingot`, `nickel_ingot_fd`, `nickel_ingot_fd_blast`, `nickel_ingot_blast`, `nickel_ingot_raw`, `nickel_ingot_raw_blast`, `nickel_ingot_deep`, `nickel_ingot_deep_blast`
  - `copper_ingot_fd`, `copper_ingot_fd_blast`
  - `gold_ingot_fd`, `gold_ingot_fd_blast`
  - `chlorium_ingot_fd`, `chlorium_ingot_fd_blast`
  - `mushrium_ingot_fd`, `mushrium_ingot_fd_blast`
  - `powered_tin_ingot_fd`, `powered_tin_ingot_fd_blast`
  - `netherite_ingot_fd`, `netherite_ingot_fd_blast`

- **`vanilla/material/`** (15+ 文件):
  - `iron_gear`, `gold_gear`, `copper_gear`, `tin_gear`, `nickel_gear`, `powered_tin_gear`, `chlorium_gear`
  - `netherite_gear`, `diamond_gear`, `emerald_gear`, `lapis_gear`, `quartz_gear`, `amethyst_gear`, `redstone_gear`, `mushrium_gear`
  - `aerolium_gear`, `powered_aerolium_gear`

- **`vanilla/machine/`** (18+ 文件):
  - `machine_compressor`, `machine_smelter`, `machine_pulverizer`, `machine_refiner`, `machine_induction_furnace`
  - `machine_psionicant`, `machine_beacon_simulator`, `machine_mob_ripper`, `machine_quarry`, `machine_enchantment_flusher`, `machine_matter_condenser`, `machine_farm_manager`
  - `engine_extraction`, `engine_metal`, `engine_biomass`, `engine_solar`
  - `pipe_gold`, `cell_glass`, `cable_glass`, `channel_item`, `channel_energy`, `channel_fluid`, `channel_connector`

- **`vanilla/compress/`** (~14+ 文件):
  - `pb_tin`, `pb_tin_raw`, `pb_nickel`, `pb_nickel_raw`, `pb_powered_tin`, `pb_chlorium`, `pb_iron`, `pb_gold`, `pb_copper`, `pb_netherite`, `pb_mushrium`
  - 各种 `nugget/` 子文件

- **`vanilla/upgrades/`** (12+ 文件):
  - `lev_potion`, `lev_range`, `lev_magma`, `lev_smoke`, `lev_ice`, `lev_mineral`, `lev_knowledge`, `lev_stream`, `lev_blast`, `lev_photosyn`
  - `lu/lev_powered`, `lu/lev_augmented`, `lu/lev_shulker`

- **`vanilla/bizarrerie/`** (7 文件):
  - `biz_emer`, `biz_gold`, `biz_iron`, `biz_netherite`, `biz_redstone`, `biz_quartz`, `biz_diam`, `biz_lapis`

- **`vanilla/` 其他**:
  - `mould_rod`, `mould_plate`, `mould_string`, `mould_gear`
  - `detector`, `hydraulic_widget`, `redstone_conductor`, `redstone_converter`, `redstone_storer`, `redstone_ai`

### 子类 B: `List is too short: 0` (空 ingredient 列表) — 17 个

**错误消息**: `List is too short: 0, expected range [1-9]`

**根因**: 配方中 ingredient 或 result 列表为空数组 `[]`。

**失败文件**:
- `vanilla/compress/nugget/rb_mushrium`
- `vanilla/compress/nugget/rb_chlorium`
- `vanilla/compress/nugget/rb_powered_tin`
- `vanilla/compress/nugget/rb_tin`
- `vanilla/compress/nugget/rb_gold`
- `vanilla/compress/nugget/rb_netherite`
- `vanilla/compress/nugget/rb_copper`
- `vanilla/compress/nugget/rb_iron`
- `vanilla/compress/nugget/rb_nickel`
- `vanilla/compress/rb_powered_tin`
- `vanilla/compress/rb_chlorium`
- `vanilla/compress/rb_nickel`
- `vanilla/compress/rb_tin`
- `vanilla/compress/rb_tin_raw`
- `vanilla/compress/rb_nickel_raw`
- `vanilla/powered_tin_dust`
- `vanilla/chlorium_dust`

**建议修复**: 这些配方可能是需要注册到某个压缩/解压处理器而非标准 crafting table。需确认这些配方是否基于自定义 recipe type——若为自定义 type，ingredient 格式可能不同。建议检查这些 JSON 的 `type` 字段。

### TASK 归属: TASK-B

---

## 2. Tag 引用缺失 ERROR — 2 个

### 错误 1: `minecraft:water` tag
```
Couldn't load tag minecraft:water as it is missing following references:
  kenergyengineering:flowing_liquid_xp (from mod/kenergyengineering)
  kenergyengineering:flowing_liquid_bizarrerie (from mod/kenergyengineering)
```

**根因**: `data/minecraft/tags/fluid/water.json` 中使用了 `flowing_liquid_xp` 和 `flowing_liquid_bizarrerie`，但实际的注册名为 `liquid_xp_flowing` 和 `liquid_bizarrerie_flowing`。

### 错误 2: `minecraft:supports_sugar_cane_adjacently` tag
```
Couldn't load tag minecraft:supports_sugar_cane_adjacently as it is missing following references:
  #minecraft:water (from vanilla)
```

**根因**: 这是级联错误 — 因为 `minecraft:water` tag 加载失败，导致依赖于它的 `supports_sugar_cane_adjacently` tag 也失败。

### TASK 归属: TASK-C

---

## 3. Missing FluidModel — 10 个 (5 流体 × 2 变体)

所有 5 种流体及其 `_flowing` 变体都缺失 FluidModel：

| # | 流体名 | Still | Flowing |
|---|--------|-------|---------|
| 1 | `kenergyengineering:liquid_royal_jelly` | ✓ | ✓ |
| 2 | `kenergyengineering:liquid_spicy_jelly` | ✓ | ✓ |
| 3 | `kenergyengineering:liquid_honey` | ✓ | ✓ |
| 4 | `kenergyengineering:liquid_xp` | ✓ | ✓ |
| 5 | `kenergyengineering:liquid_bizarrerie` | ✓ | ✓ |

**根因**: 缺少 `blockstates/fluid/` 注册或 fluids 的模型/blockstate JSON 文件。需确认现有文件：
- `blockstates/liquid_*.json` (5 个)
- `models/block/liquid_*.json` (5 个)
- `models/block/fluid/liquid_*.json` (5 个)
- `textures/block/liquid_*.png` + 流动版
- `textures/fluid/liquid_*.png` + 流动版
- 是否缺少 `models/item/liquid_*_bucket.json`

### TASK 归属: TASK-E

---

## 4. Missing Item Model — 161 个 (kenergyengineering)

全部 161 个注册物品都没有物品模型文件。以下是按类别分组清单：

### 方块物品 (Blocks as Items) — ~36 个
```
tin_ore, nickel_ore, deep_tin_ore, deep_nickel_ore
tin_block, nickel_block, powered_tin_block, chlorium_block
raw_tin_block, raw_nickel_block
machine_smelter, machine_pulverizer, machine_compressor, machine_refiner
machine_induction_furnace, machine_psionicant, machine_beacon_simulator
machine_mob_ripper, machine_quarry, machine_enchantment_flusher
machine_matter_condenser, machine_farm_manager
engine_extraction, engine_metal, engine_biomass, engine_solar
cable, cable_quartz, cable_azure, cable_star
pipe, pipe_white, pipe_black
energy_cell, creative_energy_cell
channel_energy, channel_item, channel_fluid
```

### 流体桶 — 5 个
```
liquid_royal_jelly_bucket, liquid_spicy_jelly_bucket, liquid_honey_bucket
liquid_xp_bucket, liquid_bizarrerie_bucket
```

### 粉尘 (Dusts) — 15 个
```
iron_dust, gold_dust, copper_dust, tin_dust, nickel_dust
powered_tin_dust, chlorium_dust, netherite_dust, diamond_dust
emerald_dust, lapis_dust, quartz_dust, amethyst_dust
mushrium_dust, starlight_dust
```

### 锭 (Ingots) — 5 个
```
tin_ingot, nickel_ingot, powered_tin_ingot, chlorium_ingot, mushrium_ingot
```

### 粒 (Nuggets) — 14 个
```
tin_nugget, nickel_nugget, powered_tin_nugget, chlorium_nugget
copper_nugget, netherite_nugget, diamond_nugget, emerald_nugget
lapis_nugget, quartz_nugget, mushrium_nugget
(plus 3 more standard -> gold/iron nuggets may already exist)
```

### 板 (Plates) — 16 个
```
iron_plate, gold_plate, copper_plate, tin_plate, nickel_plate
powered_tin_plate, chlorium_plate, netherite_plate, diamond_plate
emerald_plate, lapis_plate, quartz_plate, amethyst_plate
redstone_plate, mushrium_plate
```

### 齿轮 (Gears) — 15 个
```
iron_gear, gold_gear, copper_gear, tin_gear, nickel_gear
powered_tin_gear, chlorium_gear, netherite_gear, diamond_gear
emerald_gear, lapis_gear, quartz_gear, amethyst_gear
redstone_gear, mushrium_gear
```

### 杆 (Rods) — 10 个
```
iron_rod, gold_rod, copper_rod, tin_rod, nickel_rod
powered_tin_rod, chlorium_rod, netherite_rod, mushrium_rod
```

### 线 (Wires) — 10 个
```
iron_wire, gold_wire, copper_wire, tin_wire, nickel_wire
powered_tin_wire, chlorium_wire, netherite_wire, mushrium_wire
```

### 模具 (Moulds) — 9 个
```
mould_gear, mould_plate, mould_rod, mould_string
mould_compressed_small, mould_compressed_large
mould_split, mould_coin, mould_dense_plate
```

### 杂项物品 — ~25 个
```
raw_tin, raw_nickel
redstone_conductor, redstone_converter, redstone_storer, redstone_ai, redstone_ai_advanced
indigo, azure_glass, bizarrerie
hydraulic_widget, detector
royal_jelly, spicy_jelly
spanner, energy_capacity, channel_connector
augmented_levelup, powered_levelup, relic_levelup
photosyn_levelup, range_levelup, smoke_levelup, blast_levelup
potion_levelup, stream_levelup, knowledge_levelup, ice_levelup, magma_levelup, mineral_levelup
```

### TASK 归属: TASK-D

---

## 5. Missing Model Variant (ldlib2 test block) — 7 个

```
Block{ldlib2:test}[facing=north/east/south/west/up/down]
Block{ldlib2:renderer_block}
```

**状态**: 这是 `ldlib2` 依赖的测试方块，**不在本计划范围内**。可忽略。

---

## 6. 音效缺失 — 1 个

```
File kenergyengineering:sounds/starlight.ogg does not exist,
cannot add it to event kenergyengineering:starlight
```

**根因**: `assets/kenergyengineering/sounds.json` 中引用了 `kenergyengineering:starlight`，但对应的 `assets/kenergyengineering/sounds/starlight.ogg` 文件不存在。

**建议修复选项**:
1. 从旧分支检出 `starlight.ogg`
2. 修正 `sounds.json` 路径
3. 移除无效 sound event

### TASK 归属: TASK-F

---

## 7. Missing Translation — 0 个

**未发现 Missing translation 警告。**

说明语言文件没有报 JSON 解析错误。但游戏中物品可能仍不显示中文名称（需手动验证），建议在 TASK-H 中检查 `en_us.json` / `zh_cn.json` 的覆盖完整性。

### TASK 归属: TASK-H

---

## 8. Texture Not Found / Missing Texture — 0 个

**未发现纹理缺失类错误。**

---

## 9. 附加观察: 游戏中无名称显示

日志中虽无 `Missing translation` 警告，但根据计划描述所述"所有注册物品无 tooltip/名称"。建议在 TASK-H 中：
1. 检查 `en_us.json` 和 `zh_cn.json` 中是否覆盖了所有 `item.*` / `block.*` key
2. 检查 Java 注册侧 `descriptionId` 设置（可能 Item 注册时未正确设置 name）
3. 若 `en_us.json` 中有 key 但名称仍不显示，则可能是代码侧问题

---

## 错误汇总: 按 TASK 归类

| TASK | 问题类型 | 数量 | 优先级 |
|------|---------|------|--------|
| **TASK-B** | 配方解析 ERROR (ingredient_type) | ~116 | 🔴 高 |
| **TASK-B** | 配方解析 ERROR (空列表) | 17 | 🟡 中 |
| **TASK-C** | Tag 引用缺失 ERROR | 2 | 🟡 高 |
| **TASK-D** | Missing item model WARN | 161 | 🟢 中 |
| **TASK-E** | Missing FluidModel WARN | 10 | 🟢 中 |
| **TASK-F** | 音效缺失 WARN | 1 | 🟢 中 |
| **TASK-H** | 语言文件完整性验证 | (需手动验证) | 🟡 高 |
| **—** | ldlib2 test block (超出范围) | 7 | 🔘 忽略 |

---

## 日志文件

日志原文: `build/resource_fix_log.txt` (117,880 bytes, 486 行)
