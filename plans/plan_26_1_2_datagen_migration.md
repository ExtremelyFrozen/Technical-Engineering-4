# Plan: 全面修复和迁移变构数据生成 — 26.1.2 Datagen Restore

> authority: `skills/编排沙盘/templates/plans/正式计划模板.md`
> 本计划由草稿 `plans/.draft_plan_26_1_2_datagen_migration.md` v0.2 经审查通过后，由猫娘规划师-缇娅按正式计划模板重建。

## 计划元数据

- **计划ID**: `26_1_2_datagen_migration`
- **计划路径**: `plans/plan_26_1_2_datagen_migration.md`
- **草稿路径**: `plans/.draft_plan_26_1_2_datagen_migration.md`
- **workflow_mode**: standard
- **版本状态**: `已批准`
- **创建日期**: 2026-07-09
- **创建者**: 猫娘规划师-缇娅
- **触发原因**: 此前勘探确认当前 datagen 只生成 recipe + zh_cn lang，未生成 blockstates/models/loot_table/tags。`TENModels.java` 已变成存根，旧 Registrate/BlockStateProvider 生成链被移除。当前 blockstates/models/loot/tags 是从旧 generated 迁移到 `src/main/resources` 的静态资源快照。用户怀疑 blockstates 问题源于 datagen 迁移未完成，希望全面修复和迁移"变构数据生成"。
- **审查结论**: 审查通过
- **建议下一步**: 进入执行
- **目标基线**: NeoForge 26.1.2 / Minecraft 1.21.1 / Java 25
- **当前 HEAD**: `a343a27 fix: resolve 26.1.2 resource data issues`
- **远程分支**: `origin/feat/26.1.2-datagen-migration`
- **本地分支**: `feat/26.1.2-datagen-migration`
- **工作区状态**: clean（上一计划已提交推送）

## 1. 目标

在 NeoForge/Minecraft 26.1.2 下恢复/迁移完整 datagen，至少覆盖：
- blockstates（方块状态 JSON）
- block models（方块模型 JSON）
- item models（物品模型 JSON）
- loot tables（战利品表 JSON）
- tags（标签 JSON：block / item / fluid）

同时确保现有 recipe / lang 继续可运行，不退化。

**最终状态**：`runClientData` 能够产出 blockstates / models / loot_tables / tags 目录且内容完整；`runClient` 游戏中无 `Missing model` / `Invalid blockstate` / `Missing loot_table` / `Missing tag` 等资源相关错误。

## 2. 范围

### 在范围内

- 现状盘点与黄金样本冻结（当前 `main/resources` 作为 golden baseline）
- 检索/确认 26.1.2 NeoForge datagen Provider API（BlockStateProvider / ItemModelProvider / LootTableProvider / TagsProvider）
- 重建 BlockStateProvider + block model 生成
- 重建 ItemModelProvider
- 重建 LootTableProvider
- 重建 TagsProvider（block / item / fluid）
- 统一 `DataGenerators.java` 注册所有 Provider
- `sourceSets` / `processResources` 策略确认（`generated/resources` vs `main/resources` 覆盖关系）
- 运行 `runClientData`，对比 generated 与 golden baseline
- 运行 `runClient` 资源零错误验证
- 按阶段提交与推送策略
- 每个可验证节点即时推送

### 不在范围内

- ❌ JEI/EMI 集成或功能恢复
- ❌ GUI 完整美观
- ❌ Capability → Resource/Transaction 功能迁移
- ❌ 游戏平衡性调整
- ❌ 新功能开发（仅恢复 datagen 覆盖）
- ❌ 代码重构（仅新增 Provider 类，不改现有注册逻辑）
- ❌ 性能优化
- ❌ 测试覆盖率提升

### 允许的回退/降级原则

- 如某 Provider 在 26.1.2 API 下暂不可实现（例如 TagOutput 格式不兼容、LootTable 注册变更过大），允许**保留静态 `main/resources` 并记录技术债**
- 每个可验证节点（TASK 完成后）需要即时推送，不累积未推送变更
- 如回退发生，需在计划中更新对应 TASK 状态并记录原因

## 3. 执行 TASK

---

### TASK-001: 现状盘点与黄金样本冻结

| 属性 | 内容 |
|------|------|
| **task_id** | TASK-001 |
| **phase** | Phase 1 — 盘点 |
| **scope** | `src/main/resources/assets/kenergyengineering/`（blockstates / models / textures）；`src/main/resources/data/kenergyengineering/`（loot_tables / tags）；`src/main/resources/data/minecraft/tags/`（引用 `kenergyengineering` 的 tag 文件）；`src/generated/resources/`；`src/main/java/com/modularmc/ten/data/` |
| **目标** | 冻结当前 `main/resources` 中的 blockstates/models/loot/tags 作为 golden baseline；记录当前 generated 覆盖状态；盘点源码中 Provider 注册情况 |
| **输入** | 当前工作区源码与资源 |
| **输出** | `docs/datagen_baseline_report.md` — 包含：<br>1. `main/resources` 各目录文件计数与清单<br>2. `generated/resources` 各目录文件计数与清单<br>3. `DataGenerators.java` 已注册 Provider 列表<br>4. `TENModels.java` 存根确认<br>5. 所有注册方块/物品/流体/标签清单（来自 Java 注册类）<br>6. 当前 missing model / blockstate 的 runClient 日志证据（复用上一计划已有日志或重新采集）<br>7. `data/minecraft/tags/` 下引用 `kenergyengineering` 的 tag 文件清单<br>8. runClient 日志中当前 `Missing texture` WARN 计数与清单（作为 texture baseline） |
| **依赖** | 无（独立执行） |
| **DoD** | □ `docs/datagen_baseline_report.md` 已生成，含文件计数和清单<br>□ 所有 `main/resources` 下的 blockstates / models / loot_tables / tags 已被记录为 golden baseline<br>□ `data/minecraft/tags/` 中引用 `kenergyengineering` 的 tag 文件已列入 baseline 范围<br>□ 与 Java 注册类（TENBlocks / TENItems / TENFluids / TENTags）逐一比对，产出覆盖矩阵<br>□ runClient 日志中当前资源错误已记录为新计划的基线<br>□ runClient 日志中 `Missing texture` WARN 计数与清单已采集（texture baseline）<br>□ 盘点结果已提交（`git add -A && git commit -m "chore: freeze datagen baseline snapshot"`）并推送（首次推送需 `git push -u origin feat/26.1.2-datagen-migration`）<br>□ 证据文件（如有外部引用）落盘到 `plans/.evidence/evidence_26_1_2_datagen_migration_001.md` |
| **验收要点** | - [ ] baseline report 存在且内容完整<br>- [ ] 已标记 commit 且推送<br>- [ ] 注册项清单与资源清单比对已记录差异 |
| **回退** | 日志采集困难 → 直接使用文件系统清单作为 baseline，不依赖 runClient 日志 |

**预计工作量**: 1h

---

### TASK-002: 确认 Gradle 版本坐标 → 检索对应 NeoForge datagen Provider API

| 属性 | 内容 |
|------|------|
| **task_id** | TASK-002 |
| **phase** | Phase 1 — 证据采集 |
| **scope** | `build.gradle` / `gradle.properties`（读取实际 NeoForge/Minecraft 版本坐标）；NeoForge MDK / GitHub / Maven javadoc / 官方示例（确认实际版本的 API） |
| **目标** | ① 读取 Gradle 配置，确认真实 NeoForge 与 Minecraft 版本坐标；② 基于确认的版本，检索 BlockStateProvider / ItemModelProvider / LootTableProvider / TagsProvider / LanguageProvider 的包路径、注册方式、API 签名 |
| **输入** | TASK-001 盘点报告；当前 `DataGenerators.java`；`build.gradle`；`gradle.properties` |
| **输出** | `plans/.evidence/evidence_26_1_2_datagen_migration_002.md` — 包含：<br>1. Gradle 配置中读取的 NeoForge 版本、Minecraft 版本、MCP 映射版本<br>2. 每个 Provider 的完整类名与包路径<br>3. `GatherDataEvent` 中注册方式（`generator.addProvider(true, ...)` 或 `event.addProvider(...)`）<br>4. BlockStateProvider 示例（最简单的 cube_all）<br>5. ItemModelProvider 示例（最简单的 generated item）<br>6. LootTableProvider 注册方式与子 provider 注册<br>7. TagsProvider 示例（block tag）<br>8. LanguageProvider 的 `addBlock`/`addItem` 方法确认<br>9. `runClientData` 任务的配置与 Gradle task 确认 |
| **依赖** | TASK-001（盘点完成，了解当前注册方式后更有针对性） |
| **DoD** | □ 从 Gradle 配置读取并记录了实际 NeoForge/Minecraft 版本坐标（不假设 `26.1.2` 就是实际值）<br>□ 证据文件包含所有 9 项内容的明确结论<br>□ 每个结论附来源链接或代码片段<br>□ 如果有 `@OnlyIn` / Dist 限制，已特别标注<br>□ 如果某个 Provider 在确认版本中不可用/已弃用，已标注替代方案或回退策略<br>□ 证据文件已落盘到 `plans/.evidence/evidence_26_1_2_datagen_migration_002.md` |
| **验收要点** | - [ ] 证据文件中的版本坐标与 `build.gradle` 一致，非硬编码假设<br>- [ ] 证据文件可被后续 TASK 作为唯一参考源<br>- [ ] 无未解决的 API 不确定性（所有 `?` 都已消除） |
| **回退** | 文档不足以确认 API → 从 neo MDK 示例项目 clone 参考；或从 Maven 下载 jar 反编译 |

**预计工作量**: 1.5~2h（含 Gradle 版本确认 + API 检索与验证）

---

### TASK-003: 重建 BlockStateProvider + Block Model 生成

| 属性 | 内容 |
|------|------|
| **task_id** | TASK-003 |
| **phase** | Phase 2 — Provider 实现 |
| **scope** | 新建 `src/main/java/com/modularmc/ten/data/datagen/TENBlockStateProvider.java`；更新 `DataGenerators.java` |
| **目标** | 创建 BlockStateProvider，为所有注册方块生成 blockstate JSON + block model JSON |
| **输入** | TASK-001 的 block 注册清单；TASK-002 的 API 证据 |
| **输出** | `TENBlockStateProvider.java`（实现）<br>更新后的 `DataGenerators.java`（注册该 Provider）<br>首次 `runClientData` 产出 |
| **依赖** | TASK-001（block 清单）；TASK-002（API 确认） |
| **DoD** | □ `TENBlockStateProvider.java` 扩展 `BlockStateProvider`（或 neo 等效类）<br>□ 覆盖 `TENBlocks.java` 中所有注册方块（~15 个基础方块 + ~13 台机器 + 流体块 + 电缆等）<br>□ 简单方块（矿石/储块）：`simpleBlock()` 生成正确<br>□ 机器方块（有 active/idle 状态）：使用 `horizontalBlock()` + variant map 区分 active 状态<br>□ 电缆/管道（可能需自定义模型）：正确生成对应的 blockstate variant<br>□ 流体块：正确生成 blockstate<br>□ 生成的 blockstate JSON 与 TASK-001 baseline 中 `main/resources` 的手写文件格式兼容<br>□ `DataGenerators.java` 中已注册该 Provider<br>□ `runClientData` 运行不报错，生成的 blockstates + models/block 目录非空<br>□ 首次生成的产出已提交并推送 |
| **验收要点** | - [ ] `runClientData` 生成 `src/generated/resources/assets/kenergyengineering/blockstates/` 下文件 > 0<br>- [ ] `models/block/` 下文件 > 0<br>- [ ] `git diff --stat` 确认新增 Provider 代码 + 生成资源范围合理 |
| **回退** | 某类方块（如电缆）blockstate 过于复杂无法通过 Provider 生成 → 保留 `main/resources` 中的手写文件，Provider 只覆盖 `#` 条件跳过（使用 `excludedBlocks` 列表记录技术债） |

**预计工作量**: 3~4h（含调试）

---

### TASK-004: 重建 ItemModelProvider

| 属性 | 内容 |
|------|------|
| **task_id** | TASK-004 |
| **phase** | Phase 2 — Provider 实现 |
| **scope** | 新建 `src/main/java/com/modularmc/ten/data/datagen/TENItemModelProvider.java`；更新 `DataGenerators.java` |
| **目标** | 创建 ItemModelProvider，为所有注册物品生成 item model JSON |
| **输入** | TASK-001 的 item/block 注册清单；TASK-002 的 API 证据；`TENModels.java` 存根（可复用或替换） |
| **输出** | `TENItemModelProvider.java`（实现）<br>更新后的 `DataGenerators.java`（注册该 Provider）<br>`runClientData` 产出 |
| **依赖** | TASK-001（item 清单）；TASK-002（API 确认） |
| **DoD** | □ `TENItemModelProvider.java` 扩展 `ItemModelProvider`（或 neo 等效类）<br>□ 覆盖 `TENItems.java` 中所有注册物品（~70+ 种材料/升级/模具等）<br>□ 简单物品（材料类）：使用 `"item/generated"` parent + 正确 texture 引用<br>□ 方块物品（block item）：使用对应 block model 作为 parent<br>□ 桶物品（fluid bucket）：使用 `"item/generated"` + bucket texture<br>□ 特殊物品（如需要自定义模型）：Provider 中按注册名分发<br>□ 生成的 item model JSON 与 TASK-001 baseline 格式兼容<br>□ `DataGenerators.java` 中已注册该 Provider<br>□ **与 TASK-003 并行执行**（无交叉依赖）<br>□ 生成的产出已提交并推送 |
| **验收要点** | - [ ] `runClientData` 生成 `models/item/` 下文件 > 0<br>- [ ] 与 TASK-001 baseline 中 `main/resources/models/item/` 对比，注册物品覆盖完整 |
| **回退** | 纹理路径确认困难 → 标记 `"layer0": "kenergyengineering:item/missing"` 并记录缺失纹理清单；不阻塞其他物品生成 |

**预计工作量**: 2~3h

---

### TASK-005: 重建 LootTableProvider

| 属性 | 内容 |
|------|------|
| **task_id** | TASK-005 |
| **phase** | Phase 2 — Provider 实现 |
| **scope** | 新建 `src/main/java/com/modularmc/ten/data/datagen/TENLootTableProvider.java`；更新 `DataGenerators.java` |
| **目标** | 创建 LootTableProvider，为所有注册方块生成 loot table JSON |
| **输入** | TASK-001 的 block 注册清单；TASK-002 的 API 证据 |
| **输出** | `TENLootTableProvider.java`（实现）<br>更新后的 `DataGenerators.java`（注册该 Provider）<br>`runClientData` 产出 |
| **依赖** | TASK-001（block 清单）；TASK-002（API 确认） |
| **DoD** | □ `TENLootTableProvider.java` 使用 neo 的 LootTableProvider API（或 SubProviderEntry + BlockLootSubProvider）<br>□ 覆盖所有注册方块（包括矿石方块 → 带矿物掉落、机器方块 → 掉落自身、流体块 → 不掉落或掉落桶）<br>□ 对应 TENTags 中 `mineable` 标签的方块有正确的挖掘条件<br>□ `DataGenerators.java` 中已注册该 Provider<br>□ `runClientData` 运行不报错，生成的 loot_tables 目录非空<br>□ 生成的 loot table JSON 语法校验通过（使用 `Get-ChildItem -Recurse -Filter *.json src/generated/resources/data/kenergyengineering/loot_table/ | ForEach-Object { try { ConvertFrom-Json -InputObject (Get-Content -Raw $_.FullName) } catch { Write-Error "$($_.FullName): invalid JSON" } }`）<br>□ 生成的产出已提交并推送 |
| **验收要点** | - [ ] `runClientData` 生成 `data/kenergyengineering/loot_table/blocks/` 下文件 > 0<br>- [ ] runClient 无 `Missing loot_table` 错误 |
| **回退** | LootTableProvider 在 26.1.2 上 API 不兼容（如 SubProviderEntry 类路径变更） → 使用简单方案：对所有方块生成 `self_drop` loot table（掉落自身），不区分挖掘等级/精准采集；记录技术债 |

**预计工作量**: 2~3h

---

### TASK-006: 重建 TagsProvider

| 属性 | 内容 |
|------|------|
| **task_id** | TASK-006 |
| **phase** | Phase 2 — Provider 实现 |
| **scope** | 新建 `src/main/java/com/modularmc/ten/data/datagen/TENTagsProvider.java`；更新 `DataGenerators.java`；参考 `TENTags.java` |
| **目标** | 创建 TagsProvider（Blocks / Items / Fluids），为所有注册项生成 tag JSON |
| **输入** | TASK-001 的注册清单；TASK-002 的 API 证据；`TENTags.java`（tag 常量定义） |
| **输出** | `TENTagsProvider.java`（实现，含 BlockTags / ItemTags / FluidTags 子类或统一类）<br>更新后的 `DataGenerators.java`（注册该 Provider）<br>`runClientData` 产出 |
| **依赖** | TASK-001（tag 定义清单）；TASK-002（API 确认） |
| **DoD** | □ `TENTagsProvider.java` 扩展 `TagsProvider`（或 neo 等效类）或使用 `TagsProvider` 的 block/item/fluid 变体<br>□ Block tags 覆盖：`mineable/pickaxe`、`mineable/shovel`、`needs_stone_tool`、`needs_iron_tool`、`needs_diamond_tool` 等<br>□ Item tags 覆盖：如 `c:ingots`、`c:dusts`、`c:gears`、`c:storage_blocks`、`c:ores` 等<br>□ Fluid tags 覆盖：`minecraft:water` 中包括模组流体<br>□ 与 `main/resources/data/minecraft/tags/` 和 `data/kenergyengineering/tags/` 现有静态文件不冲突（如冲突则移除静态版本，依赖 datagen）<br>□ `DataGenerators.java` 中已注册该 Provider<br>□ `runClientData` 运行不报错，生成的 tags 目录非空<br>□ 生成的产出已提交并推送 |
| **验收要点** | - [ ] `runClientData` 生成 `data/kenergyengineering/tags/` + `data/minecraft/tags/` 下文件 > 0<br>- [ ] runClient 无 `Missing tag` 错误 |
| **回退** | TagsProvider API 在 26.1.2 不兼容 → 保留 `main/resources` 中手写 tag 文件，在 `DataGenerators.java` 中标记 `// TAGS_DEFERRED: manual`；记录技术债 |

**预计工作量**: 2~3h

---

### TASK-007: 统一 DataGenerators.java 注册与 sourceSets/processResources 策略

| 属性 | 内容 |
|------|------|
| **task_id** | TASK-007 |
| **phase** | Phase 3 — 整合 |
| **scope** | `DataGenerators.java`；`build.gradle`（runClientData / sourceSets / processResources 配置） |
| **目标** | 确保所有 Provider 在 `DataGenerators.java` 中正确注册；确认 `generated/resources` 与 `main/resources` 的覆盖关系正确（generated 优先或 main 优先取决于策略）；确保 `processResources` 不覆盖 generated 文件 |
| **输入** | TASK-003~006 新增的 Provider 类；当前 `build.gradle` |
| **输出** | 更新后的 `DataGenerators.java`（统一注册）<br>更新后的 `build.gradle`（如需要调整 sourceSets 或 runClientData 配置）<br>策略说明文档（`docs/datagen_resource_strategy.md`） |
| **依赖** | TASK-003, TASK-004, TASK-005, TASK-006（所有 Provider 已实现） |
| **DoD** | □ `DataGenerators.java` 中已注册所有 Provider：BlockState / ItemModel / LootTable / Tags + 原有的 Recipe + Lang(zh_cn)<br>□ Provider 注册顺序已考虑依赖（如 Tags 需要先于 Recipe 引用）<br>□ `build.gradle` 中 `runClientData` 配置正确：`ideConfig = true`，`source = sourceSets.main`<br>□ sourceSets 策略确认：`src/generated/resources` 作为 `main` sourceSet 的资源目录，且 `processResources` 从 generated 复制但不覆盖 main 中的同名文件（或相反，取决于策略）<br>□ `docs/datagen_resource_strategy.md` 记录：哪个目录优先、冲突时如何处理、手动维护文件列表<br>□ `runClientData` 全量运行通过（所有 Provider 一起运行无错误）<br>□ 整合后的代码已提交并推送 |
| **验收要点** | - [ ] `.\gradlew.bat :runClientData 2>&1` 退出码 0<br>- [ ] `DataGenerators.java` 中所有 Provider 行数 = 预期数<br>- [ ] 策略文档存在且清晰 |
| **回退** | sourceSets 配置不当导致 processResources 吞掉 generated → 回退到简单策略：手动 copy generated → main（不自动合并），在 README 中说明工作流 |

**预计工作量**: 1.5~2h（含调试 sourceSets）

---

### TASK-008: 运行 runClientData，比对 generated 与 golden baseline

| 属性 | 内容 |
|------|------|
| **task_id** | TASK-008 |
| **phase** | Phase 4 — 验证与比对 |
| **scope** | `runClientData` 全量产出 vs `docs/datagen_baseline_report.md`（TASK-001） |
| **目标** | 全量运行 datagen，对比 generated 产出与 golden baseline 的差异，记录每项差异是否可接受/需修复 |
| **输入** | TASK-007 整合后的全量 Provider；TASK-001 的 baseline 报告 |
| **输出** | `docs/datagen_diff_report.md` — 包含：<br>1. 新增文件（generated 有但 baseline 无）<br>2. 缺失文件（baseline 有但 generated 无）<br>3. 内容差异（同名文件，json diff）<br>4. 每条差异的可接受判定与理由<br>5. 需修复项清单 |
| **依赖** | TASK-007（所有 Provider 已注册并整合） |
| **DoD** | □ `.\gradlew.bat clean runClientData 2>&1` 退出码 0，无 ERROR<br>□ diff 报告已生成，包含以下三条命令产出：<br>　• 文件级新增/缺失：`$generated = Get-ChildItem -Path src/generated/resources -Recurse -File; $baseline = Get-ChildItem -Path src/main/resources -Recurse -File; Compare-Object $baseline $generated -Property Name, DirectoryName | Where-Object { $_.DirectoryName -match 'kenergyengineering' }`<br>　• JSON 内容差异（使用 `git diff --no-index`）：`$genFiles = Get-ChildItem -Path src/generated/resources -Recurse -File -Filter *.json; $mainFiles = Get-ChildItem -Path src/main/resources -Recurse -File -Filter *.json; foreach ($f in $genFiles) { $rel = $f.FullName.Replace('src\generated\resources\', ''); $m = "src/main/resources/$rel"; if (Test-Path $m) { git diff --no-index -- "$m" "$($f.FullName)" --ignore-space-change } }`<br>　• 目录结构对比：`diff (Get-ChildItem -Path src/generated/resources/assets/kenergyengineering -Recurse -Directory | ForEach-Object { $_.FullName.Replace('src\generated\resources\', '') }) (Get-ChildItem -Path src/main/resources/assets/kenergyengineering -Recurse -Directory | ForEach-Object { $_.FullName.Replace('src\main\resources\', '') })`<br>□ 每条差异已记录并标记：`accept`（可接受）/ `fix`（需修复）/ `regression`（回退）<br>□ accept 判定理由明确：如"格式差异但语义等价" / "baseline 中的手写模型已过时，generated 更准确"<br>□ fix 项已分配对应 TASK-009 子任务<br>□ regression 项已记录阻断判断（是否阻塞推进）<br>□ 本次比对结果已提交并推送 |
| **验收要点** | - [ ] diff 报告包含所有 5 项内容<br>- [ ] 所有差异有明确的 accept/fix/regression 标签<br>- [ ] 无阻塞性 regression |
| **回退** | diff 报告过长（>200 差异）→ 仅关注 missing 和 blockstate/model 关键差异；机械格式差异（如 indent 风格）标记 accept |

**预计工作量**: 2~3h（含比对分析）

---

### TASK-009: runClient 资源验证

| 属性 | 内容 |
|------|------|
| **task_id** | TASK-009 |
| **phase** | Phase 4 — 运行时验证 |
| **scope** | `runClient` 任务 + 游戏内创作品页验证 |
| **目标** | 启动 Minecraft 客户端，确认所有资源错误归零或降至可接受范围 |
| **输入** | TASK-008 整合后的全量 datagen 产出 + `main/resources` 静态资源 |
| **输出** | `docs/datagen_runtime_verification.md` — 包含：<br>1. `runClient` 日志中资源相关错误/警告计数<br>2. 每类错误的详细清单与处理判定<br>3. 游戏内验证：创造模式物品栏中所有物品显示正常模型<br>4. 方块放置后纹理正确，无紫黑块<br>5. 流体显示正常<br>6. `Missing texture` 对比 TASK-001 texture baseline：增量清单与判定 |
| **依赖** | TASK-008（generated vs baseline 差异已确认并修复） |
| **DoD** | □ `runClient` 日志中：<br>　• `Missing model` / `Invalid blockstate` ERROR 计数 = 0<br>　• `Missing loot_table` ERROR 计数 = 0<br>　• `Missing tag` ERROR 计数 = 0<br>　• `Missing texture` WARN：无非预期新增（对比 TASK-001 texture baseline）；最终目标为 0；如 baseline 非 0 则清单中每条必须解释原因并归档到 `docs/datagen_runtime_verification.md`<br>　• `Missing translation` 不归零（语言文件在上一计划已修复）<br>□ 游戏内验证通过：<br>　• 所有注册方块可在创造模式中找到<br>　• 方块放置后纹理正确<br>　• 所有注册物品模型显示正常<br>　• 流体方块/桶渲染正常<br>□ 验证报告已生成并提交推送<br>□ **本 TASK 完成后立即执行即时推送** |
| **验收要点** | - [ ] `runClient` 日志中 `select-string "ERROR"` 无 kenergyengineering 资源相关行<br>- [ ] 游戏内截图或操作记录显示所有物品/方块模型正常 |
| **回退** | 剩余错误无法归零 → 分类记录到已知问题清单，标记是否为阻塞项；非阻塞项允许延后修复 |

**预计工作量**: 2~3h（含日志分析 + 游戏内验证）

---

### TASK-010: 按阶段提交与即时推送策略

| 属性 | 内容 |
|------|------|
| **task_id** | TASK-010 |
| **phase** | Phase 5 — 收口 |
| **scope** | 全工作区 Git 提交历史 |
| **目标** | 确保每阶段成果已按约定提交并推送至 `origin/feat/26.1.2-datagen-migration`；生成可追溯的 commit 链 |
| **输入** | TASK-001~TASK-009 所有变更 |
| **输出** | 提交历史链 + 最终状态确认 |
| **依赖** | TASK-009（运行验证通过） |
| **DoD** | □ 提交历史链已形成（从 TASK-001 的 baseline 冻结到最终验证报告）：<br>　1. `chore: freeze datagen baseline snapshot`（TASK-001）<br>　2. `feat: add BlockStateProvider and block model generation`（TASK-003）<br>　3. `feat: add ItemModelProvider for item model generation`（TASK-004）<br>　4. `feat: add LootTableProvider for block loot tables`（TASK-005）<br>　5. `feat: add TagsProvider for block/item/fluid tags`（TASK-006）<br>　6. `feat: unify DataGenerators registration and sourceSets strategy`（TASK-007）<br>　7. `docs: datagen diff report vs golden baseline`（TASK-008）<br>　8. `feat: runtime resource validation pass`（TASK-009）<br>　9. 或以上按实际执行合并/拆分<br>□ 每个提交对应可独立验证的节点<br>□ 每个提交后已执行 `git push origin feat/26.1.2-datagen-migration`（首次推送使用 `git push -u origin feat/26.1.2-datagen-migration` 建立跟踪）<br>□ 最终 `git status` 显示 clean<br>□ 最终 `git log --oneline origin/feat/26.1.2-datagen-migration` 可追溯全部变更 |
| **验收要点** | - [ ] `git log --oneline origin/feat/26.1.2-datagen-migration -10` 输出包含本计划所有阶段提交<br>- [ ] `git status` → nothing to commit, working tree clean<br>- [ ] `git push` 无 rejected |
| **回退** | 提交历史不理想 → 在最终阶段使用 `git rebase -i` 整理（需在推送前执行）；已推送的分支不 rebase，接受已有历史 |

**预计工作量**: 0.5h（不含前期推送，仅最终确认）

---

## 4. 依赖链与执行顺序

```text
TASK-001 (现状盘点与基线冻结, 无依赖)
  └── TASK-002 (NeoForge API 检索, 建议依赖 001)
        ├── TASK-003 (BlockStateProvider, 依赖 001+002)
        ├── TASK-004 (ItemModelProvider, 依赖 001+002)  ← 可与 003 并行
        ├── TASK-005 (LootTableProvider, 依赖 001+002)  ← 可与 003/004 并行
        └── TASK-006 (TagsProvider, 依赖 001+002)       ← 可与 003/004/005 并行
              └── TASK-007 (DataGenerators 整合, 依赖 003~006)
                    └── TASK-008 (Diff 比对, 依赖 007)
                          └── TASK-009 (runClient 验证, 依赖 008)
                                └── TASK-010 (提交与推送确认, 依赖 009)
```

**并行策略**:
- **Phase 1 串行**: TASK-001 → TASK-002（002 依赖 001 的盘点结果以确定检索范围）
- **Phase 2 全并行**: TASK-003/004/005/006 各自为独立的 Provider 类，无代码交叉依赖，可使用 skill://多线调度 并发启动
  - 每个并行线包含：实现 Provider → 单次 `runClientData` 验证 → 各自提交并推送
  - 注意：如果多个并行线同时修改 `DataGenerators.java` 会导致冲突，建议策略为：
    a) 各并行线在自己 Provider 文件中独立开发，暂不改 `DataGenerators.java`
    b) TASK-007 统一注册所有 Provider
    或：
    a) 各并行线顺序执行（串行化 Phase 2），每完成一个 Provider 即注册并推送
  - 推荐策略：TASK-003/004/005/006 顺序执行而非全并行，避免 `DataGenerators.java` 多线冲突；每个 Provider 完成后即推送，形成可追溯的增量提交链
- **Phase 3 串行**: TASK-007（依赖所有 Phase 2 Provider 就绪后统一整合）
- **Phase 4 串行**: TASK-008 → TASK-009
- **Phase 5 收口**: TASK-010（最终确认）

**即时推送节点**（每个 TASK 标记 ✅ 后立即推送至 `origin/feat/26.1.2-datagen-migration`）:
- TASK-001 ✅ → `git push -u origin feat/26.1.2-datagen-migration`（首次推送，建立跟踪）
- TASK-003 ✅ → `git push origin feat/26.1.2-datagen-migration`
- TASK-004 ✅ → `git push origin feat/26.1.2-datagen-migration`
- TASK-005 ✅ → `git push origin feat/26.1.2-datagen-migration`
- TASK-006 ✅ → `git push origin feat/26.1.2-datagen-migration`
- TASK-007 ✅ → `git push origin feat/26.1.2-datagen-migration`
- TASK-008 ✅ → `git push origin feat/26.1.2-datagen-migration`
- TASK-009 ✅ → `git push origin feat/26.1.2-datagen-migration`
- TASK-010 ✅ → `git push origin feat/26.1.2-datagen-migration`（最终确认推送无 rejected）

## 5. 工作量评估与排序

| TASK | 预估工时 | 阶段 | 并行组 | 优先级 |
|:----:|:--------:|:----:|:------:|:------:|
| TASK-001 | 1h | Phase 1 | — | 🔴 最高（基线） |
| TASK-002 | 1.5~2h | Phase 1 | 串行于 001 | 🔴 最高（API 确认） |
| TASK-003 | 3~4h | Phase 2 | A（BlockState） | 🟡 高 |
| TASK-004 | 2~3h | Phase 2 | B（ItemModel） | 🟡 高 |
| TASK-005 | 2~3h | Phase 2 | C（LootTable） | 🟡 高 |
| TASK-006 | 2~3h | Phase 2 | D（Tags） | 🟡 高 |
| TASK-007 | 1.5~2h | Phase 3 | — | 🟡 高（整合） |
| TASK-008 | 2~3h | Phase 4 | — | 🟡 高（比对） |
| TASK-009 | 2~3h | Phase 4 | — | 🟡 高（验证） |
| TASK-010 | 0.5h | Phase 5 | — | 🟢 中（收口） |
| **合计** | **18~25.5h** | — | — | — |

## 6. 风险与降级方案

| # | 风险 | 概率 | 影响 | 降级方案 |
|---|------|:----:|:----:|---------|
| 1 | NeoForge 26.1.2 的某个 Provider API 与预期不一致（如 BlockStateProvider 包路径变更、LootTableProvider 注册方式变化） | 中 | 中 | TASK-002 中尽早暴露；API 变更过大则该 Provider 降级为保留静态资源，记录技术债 |
| 2 | `runClientData` 生成的资源与 `main/resources` 现有手写文件冲突（同名覆盖） | 高 | 高 | TASK-007 中确认 sourceSets 策略；冲突时以手写文件优先（`main/resources` 覆盖 generated），或迁移手写文件到临时目录归档 |
| 3 | 生成的 blockstate/models 与手写版本格式不兼容导致运行时报错 | 中 | 高 | TASK-008 diff 比对中重点检查格式兼容性；如不兼容则调整 Provider 输出格式或降级为保留手写文件 |
| 4 | TagsProvider 生成的 tag 与手写 tag 重复/冲突 | 中 | 中 | 同风险 2：手写优先；或删除手写 tag 全权委托 datagen |
| 5 | `processResources` 吞噬 generated 文件或遗漏 generated 目录 | 低 | 高 | TASK-007 中验证 `processResources` 行为；使用 `build/generated/resources` 做中间验证 |
| 6 | 运行 `runClientData` 需要下载大量依赖（neo MDG 缓存）导致耗时过长 | 低 | 低 | 首次运行预计 5~15min；后续增量运行快；不在 DoD 中设时间约束 |
| 7 | 某 Provider 生成的资源数量与注册项不匹配（遗漏部分方块/物品） | 中 | 中 | TASK-008 diff 可暴露遗漏；补全对应 Provider 中的 mapping |
| 8 | runClient 中仍有非 datagen 相关的资源错误（如纹理文件本身缺失） | 中 | 低 | 属于上一计划已修复/已知问题；本计划只关注 datagen 可生成的部分 |
| 9 | 即时推送策略在某个 TASK 产生破坏性变更后被发现 | 低 | 高 | 每个推送前执行 `runClientData` 快速验证；破坏性变更 revert 后重推 |

## 7. 非目标（重申）

以下内容**明确不包含**在本计划中：

- ❌ JEI/EMI 集成或功能恢复
- ❌ GUI 完整美观
- ❌ Capability → Resource/Transaction 功能迁移
- ❌ 游戏平衡性调整
- ❌ 新功能开发
- ❌ 代码重构（除新增 Provider 类外，不改现有注册逻辑）
- ❌ 性能优化
- ❌ 测试覆盖率提升
- ❌ 英文语言文件 Provider 添加（en_us 数据已有但未注册 Provider — 视为低优先级，可后续补充）

## 8. 完成标准（汇总 DoD）

- [ ] TASK-001: baseline 报告已生成（含 `data/minecraft/tags/` 引用本 mod 的 tag + `Missing texture` baseline），基线已冻结并推送至 `origin/feat/26.1.2-datagen-migration`
- [ ] TASK-002: Gradle 版本坐标已确认，对应 NeoForge API 证据已采集落盘
- [ ] TASK-003: BlockStateProvider 实现，runClientData 产出 blockstate + block model
- [ ] TASK-004: ItemModelProvider 实现，runClientData 产出 item model
- [ ] TASK-005: LootTableProvider 实现，runClientData 产出 loot_table，JSON 语法校验通过
- [ ] TASK-006: TagsProvider 实现，runClientData 产出 tag
- [ ] TASK-007: DataGenerators.java 统一注册 + sourceSets 策略已确认
- [ ] TASK-008: generated vs golden baseline diff 报告已生成（含文件级差异 + JSON 内容 diff），差异已分类
- [ ] TASK-009: runClient 资源错误归零（Missing model/blockstate/loot_table/tag = 0）；Missing texture 无非预期新增，对比 baseline 已解释和归档
- [ ] TASK-010: 全阶段提交已推送至 `origin/feat/26.1.2-datagen-migration`，历史可追溯
- [ ] 每阶段推送已完成，无累积未推送变更
- [ ] 如遇 Provider 暂不可实现，已降级保留静态资源并记录技术债

## 9. 验证命令参考

```powershell
# 运行数据生成
.\gradlew.bat runClientData --no-daemon

# 运行客户端（验证运行时资源）
.\gradlew.bat runClient --no-daemon

# 全量 clean + 数据生成（用于 diff 比对）
.\gradlew.bat clean runClientData --no-daemon

# 查看 generated 资源计数
Get-ChildItem -Path src/generated/resources -Recurse -File | Group-Object DirectoryName | Format-Table Count, Name

# JSON 语法校验（以 loot_table 为例，可替换为任意目录）
Get-ChildItem -Recurse -Filter *.json src/generated/resources/data/kenergyengineering/loot_table/ | ForEach-Object { try { ConvertFrom-Json -InputObject (Get-Content -Raw $_.FullName) } catch { Write-Error "$($_.FullName): invalid JSON" } }

# 文件级新增/缺失对比（generated vs main）
$g = Get-ChildItem -Path src/generated/resources -Recurse -File; $m = Get-ChildItem -Path src/main/resources -Recurse -File; Compare-Object $m $g -Property Name, DirectoryName | Where-Object { $_.DirectoryName -match 'kenergyengineering' } | Format-Table -AutoSize

# JSON 内容 diff（generated vs main 同路径文件）
$genFiles = Get-ChildItem -Path src/generated/resources -Recurse -File -Filter *.json; foreach ($f in $genFiles) { $rel = $f.FullName.Replace('src\generated\resources\', ''); $mainPath = "src/main/resources/$rel"; if (Test-Path $mainPath) { git diff --no-index --ignore-space-change -- "$mainPath" "$($f.FullName)" } }

# 查看推送状态
git status
git log --oneline origin/feat/26.1.2-datagen-migration -10

# 首次推送（TASK-001 完成后首次执行）
git push -u origin feat/26.1.2-datagen-migration

# 后续推送
git push origin feat/26.1.2-datagen-migration
```
