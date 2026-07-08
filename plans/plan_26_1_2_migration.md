# Plan: 26.1.2 Migration — Kenergy Engineering Retechnicalized

> authority: `skills/编排沙盘/templates/plans/正式计划模板.md`
> 本计划由草稿 `plans/.draft_plan_26_1_2_migration.md` v0.3 经审查通过后，由猫娘规划师-缇娅按正式计划模板重建。

## 计划元数据

- **计划ID**: `26_1_2_migration`
- **计划路径**: `plans/plan_26_1_2_migration.md`
- **草稿路径**: `plans/.draft_plan_26_1_2_migration.md`
- **workflow_mode**: standard
- **版本状态**: `已完成（初步目标）`
- **创建日期**: 2026-07-08
- **创建者**: 猫娘规划师-缇娅
- **触发原因**: 草稿 v0.3 经猫娘审查官-艾琳审查通过，由规划师转换为正式计划
- **审查结论**: 审查通过
- **建议下一步**: 保留分支，后续扩展按新任务处理
- **目标基线**: NeoForge 26.1.2 / Minecraft 1.21.1 / Java 25
- **当前 HEAD**: `25b0daa feat: migrate project to 26.1.2 runtime baseline`
- **远程分支**: `origin/26.1.2`
- **收官选择**: `保留分支`
- **验证证据**: `docs/closure_verification.md`
- **最终提交**: `25b0daa`

## 1. 目标

> **初步目标（本计划覆盖）**: 游戏启动后 Mod 被正确加载 + 注册内容完整（方块/物品/流体/BE/配方/数据资源/GUI 入口不缺失）。
>
> **最终目标（后续扩展）**: 在 NeoForge 26.1.2 上实现与旧版本功能齐平的稳定运行，包括 LDLib2 GUI 完整渲染、datagen、GameTest 全部通过。

> **范围边界说明**: 本计划涵盖从依赖确认到启动验证的完整链路。GUI 完整美观和全功能平衡属于「最终目标」的后续扩展阶段，**不阻塞初步目标**，除非 GUI 代码直接导致编译失败或启动崩溃。

## 2. 范围

### 在范围内

- Gradle 构建迁移（Java 25 toolchain、NeoForge 26.1.2 MDG、版本目录调整）
- 核心编译修复（Mixin API 变更、FML API 变更、Capability → Resource/Transaction 迁移）
- Registrate（1.21.1 兼容版本）适配
- Configuration（3.1.1-neoforge）API 适配
- JEI/EMI 编译接口适配
- LDLib2 26.1.x GUI API 迁移（`com.lowdragmc.lowdraglib2 → com.lowdragmc.ldlib2` 包迁移 + GUI 构建 DSL 更新）— **仅到 GUI 入口可打开的程度，不要求完整美观**
- BE 存储系统迁移（`@Persisted`/`@DescSynced`/`FieldManagedStorage` → LDLib2 26.1.x `ValueInput`/`ValueOutput` 范式）
- Network（`RegisterPayloadHandlersEvent` → NeoForge 26.1 网络 API）
- 数据生成（datagen）配方/模型/语言文件可运行
- 内容完整性验证（注册项完整、资源不缺失）

### 不在范围内（后续扩展）

- GUI 完整美观与全功能平衡（仅到入口可打开为止，不阻塞初步目标）
- 模块化重构（除非阻塞编译）
- Minecraft 版本内容兼容性适配（1.21.1 → 26.1.2 差异）
- 性能优化（迁移稳定后再做）
- BE 面向客户端同步彻底重构（过渡期保留兼容层）
- 新功能开发
- 测试覆盖率提升（现有测试能通过即可）

## 3. 执行阶段

### Phase 0 — 依赖版本与构建基线门禁

**阶段目标**: 验证所有依赖在 26.1.2 上可解析，构建流水线就绪。所有不确定依赖必须通过外部证据采集消除假设。

---

#### TASK-001: 外部证据采集与假设验证

| 属性 | 内容 |
|------|------|
| **task_id** | TASK-001 |
| **phase** | Phase 0 |
| **scope** | NeoForge 26.1.2 changelog / LDLib2 Maven repo / Registrate GitHub / Configuration docs / Parchment MC version matrix / Mixin GitHub issues / Java 25 JPMS notes |
| **目标** | 系统采集所有不确定依赖的兼容性证据，消除假设，将结论写回依赖清单证据列 |
| **输入** | 依赖版本确认清单（§4，当前仅标记 ❓ 版本） |
| **输出** | 依赖清单「证据来源/验证命令/状态」三列全部填充；如证据超出表格容量，以 `docs/deps_evidence_26.1.2.md` 补充 |
| **依赖** | 无（可独立并行） |
| **DoD** | □ 每项依赖的「证据来源」列填写了具体 URL 或文档路径<br>□ 每项依赖的「验证命令」列填写了可重复执行的 gradle/curl 命令<br>□ 每项依赖的「状态」列确认（confirmed / failed / unknown_with_ticket）<br>□ 状态为 confirmed 的项，验证命令已执行且输出内容匹配预期<br>□ 状态为 unknown 的项，已创建后续 TASK-002 ~ TASK-005 中的对应实验验证 |
| **验收要点** | - [ ] 运行 `.\gradlew.bat --refresh-dependencies dependencies > deps_check.txt 2>&1`，输出中无 `Could not resolve`<br>- [ ] Parchment 26.1.2 兼容版本已从 https://parchmentmc.org/docs/getting-started 的 MC 版本矩阵确认<br>- [ ] Mixin 0.8.7 Java 25 兼容性已从 Mixin GitHub Issues `is:issue java 25` 确认；如无确认记录，标记 unknown<br>- [ ] NeoForge 26.1.2 Changelog（https://github.com/neoforged/NeoForge/releases/tag/26.1.2）已确认 Capability → Resource 移除状态、FML API 变更、PayloadRegistrar 包路径<br>- [ ] Java 25 `--add-opens` 需求已从 NeoForge 26.1 发布说明确认<br>- [ ] 降级路径 A 条件（LDLib2 可解析判定）中涉及的依赖状态已填入证据列<br>- [ ] 核心依赖坐标（NeoForge、Minecraft、Parchment、Mixin、Registrate 等 Phase 0 可判定的项）状态已填入证据列 |
| **回退** | 证据不可得 → 标注 `unknown: 需实验验证`，移交对应 TASK 实验判定；不阻塞 Phase 0 其他 TASK 执行 |

---

#### TASK-002: Java 25 环境确认

| 属性 | 内容 |
|------|------|
| **task_id** | TASK-002 |
| **phase** | Phase 0 |
| **scope** | 系统 JDK 安装 / `JAVA_HOME` 环境变量 / `gradle.properties` toolchain 配置 |
| **目标** | 确认 JDK 25 已安装且可被 Gradle toolchain 正确解析 |
| **输入** | 系统环境变量 `JAVA_HOME`；文件 `gradle.properties`（toolchain 设置） |
| **输出** | `java -version` 输出日志；`gradlew -version` 确认 toolchain 检测 |
| **依赖** | 无 |
| **DoD** | □ `java -version` 输出包含 `25` 开头的版本号<br>□ `$env:JAVA_HOME`（PowerShell）或 `echo %JAVA_HOME%`（CMD）指向 JDK 25 安装路径<br>□ `.\gradlew.bat -version` 输出中 toolchain 检测不报错<br>□ `gradle.properties` 中 `javaVersion` = 25（或对应 JDK 25 toolchain 格式） |
| **验收要点** | - [ ] 执行 `java -version 2>&1` 输出含 `openjdk version "25"`<br>- [ ] 执行 `.\gradlew.bat -version 2>&1` 无 toolchain 相关 warning<br>- [ ] 打开 `gradle.properties` 目视确认 toolchain Java 版本号正确 |
| **回退** | JDK 25 未安装 → 通过 SDKMAN / Adoptium 安装后重验 |

---

#### TASK-003: 版本目录审核与依赖版本确认

| 属性 | 内容 |
|------|------|
| **task_id** | TASK-003 |
| **phase** | Phase 0 |
| **scope** | `gradle/libs.versions.toml`、`gradle/forge.versions.toml`、`build.gradle`、`settings.gradle` |
| **目标** | 审核并更新版本目录中所有依赖版本号使其与 NeoForge 26.1.2 兼容 |
| **输入** | TASK-001 采集的证据；当前 `libs.versions.toml` 内容 |
| **输出** | 更新后的 `libs.versions.toml`（Parchment/modDevGradle/Mixin 版本已调整） |
| **依赖** | TASK-001（证据采集完成）；TASK-002（Java 环境确认） |
| **DoD** | □ NeoForge 版本 = `26.1.2`，Minecraft 版本 = `1.21.1`（与 26.1.2 对应）<br>□ Parchment 版本更新到 26.1.2 兼容版本（如不兼容 → 注释 parchment 配置）<br>□ modDevGradle 版本 = `2.0.200+` 或已验证兼容的版本<br>□ Mixin 版本已根据 Java 25 兼容性确认结果更新<br>□ `settings.gradle` 中 pluginManagement 版本与 libs.versions.toml 一致 |
| **验收要点** | - [ ] 执行 `.\gradlew.bat --refresh-dependencies dependencies > deps_version_check.txt 2>&1` 无 `Could not find` 版本解析错误<br>- [ ] `grep "neoforge" libs.versions.toml` 输出 NeoForge = `26.1.2`<br>- [ ] `grep "parchment" libs.versions.toml` 输出版本号已更新或已注释 |
| **回退** | 版本确认超时 → 使用已知旧版 + 测试编译（不阻塞） |

---

#### TASK-004: LDLib2 依赖验证与可解析判定

| 属性 | 内容 |
|------|------|
| **task_id** | TASK-004 |
| **phase** | Phase 0 |
| **scope** | LDLib2 Maven artifact 坐标与仓库配置（`gradle/forge.versions.toml`、`build.gradle` repositories） |
| **目标** | 判断 LDLib2 `2.2.8` 在 26.1.2 上是否可解析，输出主路径/降级路径 A 决策 |
| **输入** | TASK-001 中 LDLib2 证据采集结果；当前 `forge.versions.toml` 中 `forge.ldlib2` 版本 |
| **输出** | 解析结果判定记录（`ldlib2_resolution_result.txt`）；repositories 配置调整（如 Maven URL 变更） |
| **依赖** | TASK-003（版本目录已更新） |
| **DoD** | □ `.\gradlew.bat --refresh-dependencies dependencies 2>&1 | grep "ldlib2"` 标记的 resolve 状态已确认<br>□ 如果可解析：LDLib2 版本坐标已更新为 26.1.x 兼容坐标（`com.lowdragmc.ldlib2:ldlib2-neoforge-1.21.1:2.2.8` 或更高）<br>□ 如果不可解析：已记录不可解析原因，判定进入降级路径 A，并创建降级路径入口标记<br>□ 判定结果已同步到 TASK-001 输出的依赖清单状态列 |
| **验收要点** | - [ ] 执行 `.\gradlew.bat dependencies --refresh-dependencies > ldlib2_resolve.txt 2>&1`<br>- [ ] `select-string "ldlib2" ldlib2_resolve.txt` 输出包含 `--- ldlib2` 且无 `FAILED`<br>- [ ] 或者 `select-string "Could not resolve" ldlib2_resolve.txt` 无 LDLib2 相关行 |
| **回退** | 解析超时 → 尝试替代 LDLib2 Maven 仓库 URL；全部失败 → 直接进入降级路径 A |

---

#### TASK-005: 第三方依赖验证

| 属性 | 内容 |
|------|------|
| **task_id** | TASK-005 |
| **phase** | Phase 0 |
| **scope** | Registrate / Configuration / JEI / EMI / Jade / Sodium / Iris / ModernUI / ModernFix 的 Maven/Modrinth/CurseMaven 配置 |
| **目标** | 验证所有第三方依赖在 26.1.2 上可解析，确认仓库 URL 有效 |
| **输入** | TASK-001 证据；当前 `forge.versions.toml` 中第三方版本坐标 |
| **输出** | 第三方依赖解析报告（`deps_thirdparty_check.txt`） |
| **依赖** | TASK-003（版本目录已更新） |
| **DoD** | □ 所有第三方依赖在 `dependencies --refresh-dependencies` 中无 `Could not resolve`<br>□ Registrate `MC1.21-1.3.0+67` 可解析<br>□ Configuration `3.1.1-neoforge`（repsy.io）可解析<br>□ JEI `19.25.1.328` + EMI `1.1.22+1.21.1` 可解析<br>□ Jade/Sodium/Iris/ModernUI/ModernFix 通过 Modrinth Maven 可解析 |
| **验收要点** | - [ ] 执行 `.\gradlew.bat dependencies --refresh-dependencies > deps_thirdparty.txt 2>&1`<br>- [ ] `select-string "Could not resolve" deps_thirdparty.txt` 输出为空 |
| **回退** | 单个依赖不可解析 → 注释该依赖行（如 ModernUX 可选依赖）并标记 issue；核心依赖不可解析 → 评估替换方案 |

---

#### TASK-006: 构建基线验证

| 属性 | 内容 |
|------|------|
| **task_id** | TASK-006 |
| **phase** | Phase 0 |
| **scope** | 完整 Gradle 构建基线（dependencies + IDE 同步） |
| **目标** | 确认整个构建基线无解析错误，IDE 可同步 |
| **输入** | TASK-002 ~ TASK-005 全部完成 |
| **输出** | `deps_final_check.txt`（无错误）；IDE 项目文件更新 |
| **依赖** | TASK-002, TASK-003, TASK-004, TASK-005 |
| **DoD** | □ `.\gradlew.bat --refresh-dependencies dependencies 2>&1` 输出中 `Could not resolve`/`Could not find` 计数为 0<br>□ `.\gradlew.bat idea` 或 `eclipse` 成功执行<br>□ IDE（IntelliJ/Eclipse）打开后无红色编译错误提示<br>□ 所有依赖状态已在 §4 清单中同步更新 |
| **验收要点** | - [ ] 执行完整基线命令并捕获退出码：`.\gradlew.bat --refresh-dependencies dependencies 2>&1; $LASTEXITCODE -eq 0`<br>- [ ] IDE 项目文件生成且无错误 |
| **回退** | IDE 同步失败 → 跳过，使用纯命令行开发环境；依赖解析异常 → 逐个排除后回退到 TASK-001 重新验证 |

### Phase 1 — 最小编译通过（光源模式）

**阶段目标**: 使用最大降级策略，让项目在 26.1.2 上编译通过（核心注册 + BE + tick 可编译，GUI 显示可牺牲）。

**原则**: 先注释/禁用阻塞代码，再逐步恢复。每个 TASK 可独立回退。

---

#### TASK-010: Mixin + Init API 兼容性修复

| 属性 | 内容 |
|------|------|
| **task_id** | TASK-010 |
| **phase** | Phase 1 |
| **scope** | `kenergyengineering.mixins.json`；`TemplateMixinConfig.java`；`TEN.java`（`@Mod` 构造）；所有 Mixin 目标类 |
| **目标** | 确保 Mixin 框架在 Java 25 上正常工作，NeoForge 26.1 init API 变更不影响 mod 构造 |
| **输入** | TASK-001 中 Mixin Java 25 兼容性证据；NeoForge 26.1.2 API diff |
| **输出** | `kenergyengineering.mixins.json` 中 `compatibilityLevel` = `JAVA_25`；`TEN.java` 构造器适配 NeoForge 26.1 签名 |
| **依赖** | TASK-001（证据）；TASK-003（版本目录已更新 Mixin 版本） |
| **DoD** | □ `kenergyengineering.mixins.json` 中 `compatibilityLevel` 已改为 `JAVA_25`<br>□ `TEN.java` 中 `FMLModContainer` 构造调用适配 26.1.2 签名（grep `new FMLModContainer` 确认无编译错误）<br>□ `@Mixin(KeyboardHandler.class)` 目标类在 26.1.2 MojMap 中存在（执行 `ugrep "class KeyboardHandler"` 在 jar 内确认）<br>□ Mixin 0.8.7 如果 Java 25 不兼容，已在 `build.gradle` 中替换 Mixin 版本为 `0.9.0+` |
| **验收要点** | - [ ] 编译 `.\gradlew.bat :compileJava 2>&1 | findstr "mixin"` 无 Mixin 相关编译错误<br>- [ ] `ugrep "KeyboardHandler" src/main/java` 确认目标类引用可用 |
| **回退** | Mixin 0.8.7 Java 25 不兼容且无替代版本 → 临时禁用所有 Mixin（使用 `-Dmixin.env.disableRefmap` 标记 issue） |

---

#### TASK-011: NeoForge Capability → Resource/Transaction 迁移

| 属性 | 内容 |
|------|------|
| **task_id** | TASK-011 |
| **phase** | Phase 1 |
| **scope** | `CommonProxy.java`；`TENCapabilities.java`（如有）；所有引用 `Capabilities.*.BLOCK` 的文件 |
| **目标** | 根据 NeoForge 26.1.2 实际 API（TASK-001 已确认）迁移 Capability 注册到新 Resource/Transaction 机制，或仅更新 import 路径 |
| **输入** | TASK-001 输出的 Capability 移除状态结论 |
| **输出** | 更新后的 `CommonProxy.java`（Capability 注册代码）；移除/替换的废弃 API 导入 |
| **依赖** | TASK-001（证据确认移除了哪个 API） |
| **DoD** | □ 已验证 `Capabilities.EnergyStorage.BLOCK`/`ItemHandler.BLOCK`/`FluidHandler.BLOCK` 在 26.1.2 上的可用性<br>□ **如已移除**：使用 NeoForge 26.1 Resource API 重新注册（`registerBlockEntity` + `IEnergyStorage` 类型标记）<br>□ **如保留**：更新所有 import 包路径（`net.neoforged.neoforge.capabilities` 新路径）<br>□ `grep -r "RegisterCapabilitiesEvent" src/` 确认无残留 |
| **验收要点** | - [ ] `.\gradlew.bat :compileJava 2>&1 | findstr "Capabilities"` 无 Capability 相关编译错误<br>- [ ] `ugrep "Capabilities\.\w+\.BLOCK" src/` 输出行均已更新 API |
| **回退** | Resource API 文档不足 → 回退到 `@Deprecated` 但可编译的 Capability 存根 |

---

#### TASK-012: Configuration + Registrate + Network 适配

| 属性 | 内容 |
|------|------|
| **task_id** | TASK-012 |
| **phase** | Phase 1 |
| **scope** | `ConfigHolder.java`（Configuration）；`TENRegistrate.java`（Registrate）；`ToggleEnergyUnitPayload.java`（Network）；`TENNetwork.java` |
| **目标** | 适配第三方库 API 变更，确保 Configuration/Registrate/Network 模块可编译 |
| **输入** | TASK-001 中 Configuration/Registrate 兼容性证据；当前源文件 |
| **输出** | 更新后的 ConfigHolder/Registrate/Payload 源文件 |
| **依赖** | TASK-005（第三方依赖可解析） |
| **DoD** | □ Configuration: `ConfigHolder.java` 中 `Configuration.registerConfig` 和 `ConfigFormats.YAML` 调用已适配新签名（如有变）；`grep "Configuration.registerConfig" src/` 确认无编译错误<br>□ Registrate: `TENRegistrate.java` 中 `AbstractRegistrate` 构造已适配；`DeferredHolder` 类型参数一致<br>□ Network: `ToggleEnergyUnitPayload.java` 中 `CustomPacketPayload` 接口方法 `type()` 签名正确；`PayloadRegistrar.playToServer` 参数列表适配 26.1.2<br>□ 三方件适配相互独立，任意一项可单独回退 |
| **验收要点** | - [ ] `.\gradlew.bat :compileJava 2>&1 | findstr "Configuration\|Registrate\|Payload"` 无相关编译错误<br>- [ ] `ugrep "dev\\.toma\\.configuration" src/` 确认 import 路径正确 |
| **回退** | Configuration 适配失败 → 注释 ConfigHolder 中配置初始化，保留硬编码默认值（加 `// HACK: fallback` 注释） |

---

#### TASK-013: 最小编译验证

| 属性 | 内容 |
|------|------|
| **task_id** | TASK-013 |
| **phase** | Phase 1 |
| **scope** | 全项目源文件（`compileJava` 任务） |
| **目标** | 执行 `compileJava` 确认零编译错误；如错误 > 50，先注释最大阻塞块 |
| **输入** | TASK-010, TASK-011, TASK-012 变更后的源码 |
| **输出** | `compileJava` 成功日志（`compile_phase1.log`）或错误分类记录 |
| **依赖** | TASK-010, TASK-011, TASK-012（前序所有 Phase 1 适配完成） |
| **DoD** | □ `.\gradlew.bat :compileJava 2>&1` 退出码为 0（零错误）<br>□ 如果错误 > 50：已注释最大阻塞块（GUI 工厂/LDLib2 类引用/网络 handler），并记录到 `docs/phase1_deferred_items.md`<br>□ 核心注册（方块/物品/BE）代码无编译错误 |
| **验收要点** | - [ ] `.\gradlew.bat :compileJava 2>&1; $LASTEXITCODE` 返回 0<br>- [ ] 如有注释的阻塞块，`grep "HACK: deferred" src/` 记录了每个被延迟项的原因和恢复条件 |
| **回退** | 编译无法归零 → 继续注释非核心文件（机器 GUI、EMI/JEI 集成、部分网络 handler），直到 `compileJava` 通过 |

### Phase 2 — 核心 API 迁移

**阶段目标**: 全面迁移资源注册、BE 存储、数据生成和网络 API，使 `compileJava` + `runData` 通过。

**datagen 边界**: Phase 2 仅确保 `runData` 可运行不崩溃、产出非空；产出内容完整性（所有注册项的资源文件是否齐全）交由 Phase 4 核验。

---

#### TASK-020: Registrate 数据生成适配

| 属性 | 内容 |
|------|------|
| **task_id** | TASK-020 |
| **phase** | Phase 2 |
| **scope** | `TENRegistrate.java`；`TENDatagen.java`；所有 Provider 类（BlockStates/ItemModels/Lang/Recipes） |
| **目标** | 确保 Registrate 数据生成框架在 NeoForge 26.1.2 上可执行 |
| **输入** | TASK-012 已确认 Registrate 可编译 |
| **输出** | 更新后的 Registrate Provider 代码；首次 `runData` 日志 |
| **依赖** | TASK-012（Registrate 基本编译通过） |
| **DoD** | □ `ProviderType.LANG` 在 Registrate 1.21.1 上有效（编译 + 运行时均无 `NoSuchMethodError`）<br>□ `.\gradlew.bat :runData 2>&1` 退出码为 0<br>□ `src/generated/resources` 目录有新的生成文件（非空）<br>□ 未出现 `Duplicate entry` 或 `Missing mapping` datagen 错误 |
| **验收要点** | - [ ] `.\gradlew.bat :runData 2>&1` 返回 0<br>- [ ] `Get-ChildItem -Path src/generated/resources -Recurse -ErrorAction SilentlyContinue \| Measure-Object \| Select-Object -ExpandProperty Count` 结果 > 0 |
| **回退** | Registrate datagen 与 NeoForge 26.1 不兼容 → 切换为手动 Provider（BlockStates/ItemModels 手写），标记 `// FALLBACK: manual` |

---

#### TASK-021: LDLib2 SyncData 体系迁移（核心高工作量）

| 属性 | 内容 |
|------|------|
| **task_id** | TASK-021 |
| **phase** | Phase 2 |
| **scope** | `CmBlockEntity.java`；`CmMachineBlockEntity.java`；`FieldManagedStorage` 引用文件；所有 `@Persisted`/`@DescSynced`/`@RPCMethod` 注解使用处 |
| **目标** | 将 LDLib2 SyncData 体系适配到 26.1.x API（包路径迁移 + 泛型签名适配 + ValueIO 范式演进） |
| **输入** | LDLib2 26.1.x jar 反编译/文档（猫娘检索员-诺雅协助） |
| **输出** | 更新后的 BE 源文件；LDLib2 SyncData 适配日志 |
| **依赖** | TASK-004（LDLib2 可解析判定）；TASK-013（Phase 1 编译通过） |
| **DoD** | □ `ISyncPersistRPCBlockEntity` 接口引用已更新到 26.1.x 新路径（`com.lowdragmc.ldlib2.syncdata.holder...`）或已替换为新接口名<br>□ `FieldManagedStorage` → `ManagedStorage`（如已更名）迁移完成<br>□ `@Persisted` + `@DescSynced` 注解已知签名（保留或已更新）；`grep "Persisted\|DescSynced" src/` 确认无旧包路径残留<br>□ `@RPCMethod` 可编译且 `RPCSender` API 签名正确<br>□ 如果 `ValueInput`/`ValueOutput` 是新范式：核心字段（`energyStored`、`progress` 等）已包装为 `ValueInput<Integer>`，NBT 读写已适配新 `ValueIO` 接口<br>□ 或已选择降级路径 B：保持反射兼容层，标记 `// LDLIB2-HACK: compatibility shim` |
| **验收要点** | - [ ] `.\gradlew.bat :compileJava 2>&1 | findstr "ldlib2\|LDLib\|Persisted\|DescSynced\|ValueInput"` 无相关错误<br>- [ ] `ugrep "com\\.lowdragmc\\.lowdraglib2" src/` 无残留旧包路径（或降级路径 B 允许的例外已注释）<br>- [ ] `.\gradlew.bat :runData 2>&1` 未因 BE 序列化异常中断 |
| **回退** | 新范式过度复杂 → 执行降级路径 B（反射兼容层 + 手动 NBT 读写 + 手动 CustomPacketPayload 同步） |

---

#### TASK-022: BlockUIMenuType + JEI/EMI + 事件总线适配

| 属性 | 内容 |
|------|------|
| **task_id** | TASK-022 |
| **phase** | Phase 2 |
| **scope** | `BaseMachineBlock.java`（BlockUIMenuType）；`com.modularmc.ten.integration.jei`；`com.modularmc.ten.integration.emi`；事件总线消费者 |
| **目标** | 适配 BlockUIMenuType 接口、JEI/EMI 编译接口、NeoForge 事件总线变更 |
| **输入** | TASK-001 中 EventBus/BlockUIMenuType 证据；TASK-005 第三方依赖已解析 |
| **输出** | 更新后的 BlockUIHolder 实现、JEI/EMI 集成代码、事件监听器 |
| **依赖** | TASK-001（事件总线变更证据）；TASK-013（Phase 1 编译通过） |
| **DoD** | □ `BlockUIMenuType.BlockUI`/`BlockUIHolder` 接口签名已在 `BaseMachineBlock.java` 中适配（`grep "implements BlockUI" src/` 无编译错误）<br>□ JEI 模块（`com.modularmc.ten.integration.jei`）在 26.1.2 上可编译；`IRecipeCategory`、`IGuiHelper` 调用适配 JEI 19.x API<br>□ EMI 模块（`com.modularmc.ten.integration.emi`）可编译<br>□ `RegisterEvent`/`RegisterCapabilitiesEvent` 可用性确认（如已弃用则替换为替代事件）<br>□ 上述三项适配互不阻塞，可独立暂缓 |
| **验收要点** | - [ ] `.\gradlew.bat :compileJava 2>&1 | findstr "BlockUIMenuType\|BlockUI\|jei\|emi"` 无相关错误<br>- [ ] `ugrep -l "RegisterCapabilitiesEvent" src/main/java` 所有引用均已更新 |
| **回退** | JEI 不可编译 → 注释 `jei` 包，记录 issue；EMI 同理；BlockUIMenuType 不可编译 → 回退到 `BlockEntityWithoutLevelRenderer` 占位 |

---

#### TASK-023: 编译 + Datagen 联合验证

| 属性 | 内容 |
|------|------|
| **task_id** | TASK-023 |
| **phase** | Phase 2 |
| **scope** | 全项目 `compileJava` + `runData` |
| **目标** | 确认 Phase 2 全部适配完成后编译零错误 + datagen 完整可运行 |
| **输入** | TASK-020, TASK-021, TASK-022 合并后的源码 |
| **输出** | `compile_phase2.log` + `rundata_phase2.log` |
| **依赖** | TASK-020, TASK-021, TASK-022 |
| **DoD** | □ `.\gradlew.bat :compileJava 2>&1` 退出码 0（零编译错误）<br>□ `.\gradlew.bat :runData 2>&1` 退出码 0（datagen 完整运行）<br>□ 生成资源中 key 覆盖全部注册项（对比见 Phase 4，此处仅验证 datagen 不崩溃）<br>□ 所有 Phase 2 适配项均无 deprecation warning（已处理或已记录） |
| **验收要点** | - [ ] `.\gradlew.bat :compileJava :runData 2>&1; $LASTEXITCODE` 返回 0<br>- [ ] `select-string "error" compile_phase2.log` 输出为空<br>- [ ] `select-string "Exception\|Error\|FAILED" rundata_phase2.log` 输出为空 |
| **回退** | 联合验证失败 → 回退到单项验证（先确保 `compileJava` 通过，不阻塞 Phase 3） |

### Phase 3 — LDLib2 GUI / 同步验证

**阶段目标**: 确保 GUI 框架可用、方块界面可打开（即使布局风格改变，不要求 GUI 完整美观）。

---

#### TASK-030: GUI 包路径批量迁移

| 属性 | 内容 |
|------|------|
| **task_id** | TASK-030 |
| **phase** | Phase 3 |
| **scope** | 所有包含 `com.lowdragmc.lowdraglib2` 的源文件 |
| **目标** | 将所有 LDLib2 旧包路径 `com.lowdragmc.lowdraglib2` 批量替换为新路径 `com.lowdragmc.ldlib2`，并更新所有 GUI 相关 import |
| **输入** | LDLib2 26.1.x jar 确认新包路径存在（TASK-001/诺雅协助） |
| **输出** | 全局 import 已更新的源文件列表 |
| **依赖** | TASK-004（LDLib2 已可解析）；TASK-023（Phase 2 编译通过） |
| **DoD** | □ `ugrep -r "com\\.lowdragmc\\.lowdraglib2" src/main/java` 无残留旧包路径<br>□ `ugrep -r "com\\.lowdragmc\\.ldlib2" src/main/java` 覆盖所有原旧路径引用<br>□ `.\gradlew.bat :compileJava 2>&1 | findstr "lowdraglib2\|ldlib2"` 无 LDLib2 相关编译错误（包路径不存在的情况已排除）<br>□ 批量替换通过 `文本替换` 工具执行，差分可审查 |
| **验收要点** | - [ ] `ugrep "lowdraglib2" src/main/java` 输出为空<br>- [ ] `ugrep "ldlib2" src/main/java` 输出行数 > 旧路径行数<br>- [ ] `.\gradlew.bat :compileJava 2>&1 | findstr "package.*ldlib2"` 无 package-not-found 错误 |
| **回退** | 新包路径不存在 → 执行 LDLib2 降级路径 A（注释 LDLib2 GUI 相关依赖） |

---

#### TASK-031: GUI 组件 API 适配

| 属性 | 内容 |
|------|------|
| **task_id** | TASK-031 |
| **phase** | Phase 3 |
| **scope** | 所有引用 `ModularUI`/`UI`/`UIElement`/`ItemSlot`/`FluidSlot`/`ProgressBar`/`Label`/`IGuiTexture`/`SpriteTexture`/`FillDirection`/`HoverTooltips`/`StylesheetManager`/`SupplierDataSource` 的文件；`TENMachineBlockUIFactory.java` |
| **目标** | 适配 GUI 组件 API 变更（构造器/Builder/方法签名），使 GUI 编译通过 |
| **输入** | LDLib2 26.1.x jar / 源码参考（来自 Maven） |
| **输出** | 更新后的 GUI 工厂和组件引用代码 |
| **依赖** | TASK-030（包路径已迁移） |
| **DoD** | □ `ModularUI`/`UI`/`UIElement` 构造函数或 Builder 调用适配 26.1.x 签名<br>□ `ItemSlot`/`FluidSlot`/`ProgressBar`/`Label` 等 widget 创建代码中无 `cannot find symbol` 编译错误<br>□ `IGuiTexture`/`SpriteTexture` API 调用适配（`grep "IGuiTexture\|SpriteTexture" -A2 src/` 确认签名匹配）<br>□ `FillDirection`/`HoverTooltips` 包路径已更新<br>□ `SupplierDataSource` 如被移除 → 替换为 `SyncData` 新机制或 `LazyData` 回退<br>□ 如果单文件改动 > 30 个 → 执行降级判定（走降级路径 C） |
| **验收要点** | - [ ] `.\gradlew.bat :compileJava 2>&1 | findstr "ModularUI\|UIElement\|ItemSlot\|FluidSlot\|ProgressBar\|IGuiTexture"` 无相关错误<br>- [ ] `ugrep "SupplierDataSource" src/` 无残留（或已替换并标记 `// LDLIB2-HACK`） |
| **回退** | GUI API 大面积不兼容（> 30 文件）→ 执行降级路径 C（占位 GUI + 仅文字显示，图形元素可选） |

---

#### TASK-032: GUI 显示 + 同步数据绑定验证

| 属性 | 内容 |
|------|------|
| **task_id** | TASK-032 |
| **phase** | Phase 3 |
| **scope** | `CmMachineBlockEntity` 中 `@RPCMethod` 方法（`rpcCycleFaceMode`、`rpcSetRedstoneMode` 等）；`RPCSender` 调用处；GUI 运行时 |
| **目标** | 验证 GUI 框架在运行时可打开方块界面，同步数据绑定在客户端可接收更新 |
| **输入** | TASK-031 适配后的 GUI 代码 |
| **输出** | `runClient` GUI 显示截图/日志 |
| **依赖** | TASK-031（GUI 组件可编译）；TASK-023（datagen 已生成必要资源） |
| **DoD** | □ `@RPCMethod` 方法签名适配 26.1.x（`grep "@RPCMethod" src/` 确认无编译错误）<br>□ `RPCSender.ofServer()` API 调用适配新签名<br>□ 客户端启动后放置任意一台代表性机器方块 → 右键打开 GUI：<br>　　• GUI 标题可见<br>　　• 至少一个 `ItemSlot` 或 `FluidSlot` 渲染实例可见<br>　　• 无 missing texture 紫黑块<br>　　• 服务端日志无 GUI 打开异常（`NullPointerException`/`ClassCastException`）<br>□ GUI 截图保存到 `docs/migration/26.1.2/gui_smoke.png`<br>□ 同步数据验证：若 GUI 有能量/进度等动态文本字段，等待 5 秒后截图/日志确认数值更新；若该机器无动态字段，记录 N/A<br>□ GUI 元素不要求完整美观，仅验证入口可用 + 不崩溃 |
| **验收要点** | - [ ] `.\gradlew.bat :runClient 2>&1` 启动后，在游戏中放置一台机器并右键 → GUI 打开不抛 `NullPointerException` / `ClassCastException`<br>- [ ] GUI 中机器能量槽/进度条显示数据（即使纹理偏移） |
| **回退** | GUI 运行时崩溃 → 执行降级路径 C（占位 GUI 仅显示文字状态）；同步数据绑定失败 → 回退到手动 `CustomPacketPayload` 同步（降级路径 B 延续） |

### Phase 4 — 内容完整性与 datagen

**阶段目标**: 验证所有注册项完整、资源文件齐全、数据生成覆盖完整。**本阶段 DoD 以 §6 内容完整性检查清单全部核验为最终通过标准**。

**datagen 边界**: Phase 4 在 Phase 2（runData 可运行）基础上，要求 datagen 产出与注册类声明逐一比对，确保无遗漏。

---

#### TASK-040: 注册项清查（对照 §6 检查清单）

| 属性 | 内容 |
|------|------|
| **task_id** | TASK-040 |
| **phase** | Phase 4 |
| **scope** | `TENBlocks.java`、`TENItems.java`、`TENFluids.java`、`TENBlockEntities.java`、`TENRecipeTypes.java`、`TENCreativeModeTabs.java` |
| **目标** | 使用 `模组扫查` 技能统计所有注册项数量，与 §6 检查清单中的预期项逐行核对 |
| **输入** | §6 内容完整性检查清单 |
| **输出** | 注册项统计报告（`docs/content_inventory_report.md`）；§6 Checkbox 逐项打钩 |
| **依赖** | TASK-023（Phase 2 联合验证通过，所有注册代码可编译） |
| **DoD** | □ 使用 `模组扫查` 扫描注册项，与 `TENBlocks.java` 中 `RegistryObject` 声明逐一比对，记录差异项<br>□ 使用 `模组扫查` 扫描物品注册项，与 `TENItems.java` 声明逐一比对，记录差异项<br>□ 使用 `模组扫查` 扫描流体注册项，与 `TENFluids.java` 声明逐一比对，记录差异项<br>□ 使用 `模组扫查` 扫描 BE 注册项，与 `TENBlockEntities.java` 声明逐一比对，记录差异项<br>□ 使用 `模组扫查` 扫描配方序列化器，与 `TENRecipeTypes.java` 声明逐一比对，记录差异项<br>□ 创造标签页与 `TENCreativeModeTabs.java` 注册一致<br>□ §6 检查清单中所有 `[ ]` 标记已核验为 `[x]` 或已记录差异原因 |
| **验收要点** | - [ ] 执行 `模组扫查` 技能 → 输出注册项统计 JSON 或表格<br>- [ ] 统计结果与注册类声明逐一比对，差异项已记录到 `docs/content_gaps.md`（如有）<br>- [ ] `grep "TENBlocks\|TENItems\|TENFluids\|TENBlockEntities\|TENRecipeTypes" src/main/java -c` 确认注册方法与实际注册行为一致 |
| **回退** | 注册项统计 < 预期 → 在 `docs/content_gaps.md` 记录缺失项，不阻塞 Phase 5（除非缺失项导致编译失败或启动崩溃） |

---

#### TASK-041: 资源文件 + 语言文件 + 配方完整性核验

| 属性 | 内容 |
|------|------|
| **task_id** | TASK-041 |
| **phase** | Phase 4 |
| **scope** | `src/main/resources/assets/kenergyengineering/`；`src/main/resources/data/kenergyengineering/`；`src/generated/resources/` |
| **目标** | 确保所有纹理/模型/配方/LootTable/语言文件存在且引用完整，无 dangling 引用 |
| **输入** | TASK-040 注册项清单 |
| **输出** | 资源完整性报告（`docs/resource_integrity_report.md`） |
| **依赖** | TASK-040（注册项清单已生成） |
| **DoD** | □ 使用 `模组扫查` 统计纹理文件，与 §6 GUI 纹理列表逐项核对（不存在即记录缺失）<br>□ `Get-ChildItem src/generated/resources/assets/kenergyengineering/ -Recurse` 所有生成资源无冲突覆盖<br>□ `ugrep -r "kenergyengineering:" src/main/resources/data/kenergyengineering/recipes/` 配方中引用的所有 ID 对应注册项存在<br>□ 语言文件覆盖：`模组扫查` 对比 `en_us` 翻译覆盖率 ≥ 90%<br>□ `src/generated/resources/` 与 `src/main/resources/` 无文件名冲突 |
| **验收要点** | - [ ] 执行 `模组扫查` 资源引用完整性检查<br>- [ ] `select-string "missing\|not found\|dangling" rundata_phase4.log` 输出为空<br>- [ ] `Get-ChildItem src/main/resources/assets/kenergyengineering/textures/gui/*.png \| Select-Object Name` 与 §6 GUI 纹理列表逐项比对 |
| **回退** | 个别纹理缺失 → 复制占位纹理（1×1 透明 PNG）并记录 issue；配方引用缺失 → 注释该配方或添加 `conditions: ["kenergyengineering:loaded"]` |

---

#### TASK-042: 数据修补与 Issue 登记

| 属性 | 内容 |
|------|------|
| **task_id** | TASK-042 |
| **phase** | Phase 4 |
| **scope** | TASK-040/041 发现的所有缺失/异常项 |
| **目标** | 对 Phase 4 清查中发现的所有缺失进行修补；无法立即修补的登记 issue，确保不阻塞启动 |
| **输入** | TASK-040 注册项统计报告；TASK-041 资源完整性报告 |
| **输出** | 修补后的资源文件；`docs/content_gaps.md`（未修补项清单 + 阻塞判断） |
| **依赖** | TASK-040, TASK-041 |
| **DoD** | □ 所有可自动修复的缺失（缺少的 lang key、模型文件、配方）已在 datagen 中补充完毕<br>□ 所有手动修复的缺失（纹理文件、数据资源）已补充 `src/main/resources/`<br>□ 无法立即修复的项已登记到 `docs/content_gaps.md`，每条标注：[ID] 位置 | 缺失类型 | 阻塞启动? (Y/N) | 修复计划<br>□ 阻塞启动的缺失项数为 0（无法修补的阻塞项 → 标注 `BLOCKER` 并上报指挥官决策）<br>□ **§6 内容完整性检查清单全部核验通过**（作为本 TASK 最终关闭条件） |
| **验收要点** | - [ ] `.\gradlew.bat :runData 2>&1` 再次确认修补后无新警告<br>- [ ] `.\gradlew.bat :compileJava 2>&1` 确认修补不引入新编译错误<br>- [ ] `docs/content_gaps.md` 不存在（全部已修复）或仅有 `BLOCKER: N` 的项 |
| **回退** | 阻塞项无法修补且影响启动 → 上报猫娘指挥官-莉莉丝决定是否将此项移出范围 |

### Phase 5 — 游戏启动 / 运行时验证

**阶段目标**: Mod 在客户端启动后正确加载，注册内容在创造模式标签页中可见，服务端与 GameTest 通过。

---

#### TASK-050: 客户端启动测试

| 属性 | 内容 |
|------|------|
| **task_id** | TASK-050 |
| **phase** | Phase 5 |
| **scope** | `runClient` 任务 + 客户端日志 |
| **目标** | 确认 Minecraft 客户端可正常启动，Mod 被正确加载，无明显运行时异常 |
| **输入** | Phase 1-4 全部完成的源码 |
| **输出** | `client_startup.log`（含 `[kenergyengineering]` 过滤行） |
| **依赖** | TASK-042（Phase 4 确保无阻塞缺失项） |
| **DoD** | □ `.\gradlew.bat :runClient 2>&1` 启动后 Minecraft 主界面可达（无黑屏/闪退）<br>□ Mod 列表页面显示 `Kenergy Engineering` 状态为绿色（已正确加载）<br>□ 日志中 `[kenergyengineering]` 初始化行正常打印（无 `ClassNotFoundException`/`NoClassDefFoundError`/`ClassCastException`/`Missing mapping`）<br>□ `select-string "Exception\|Error\|Failed" client_startup.log \| select-string -NotMatch "Handshake\|Connection refused"` 无异常模式 |
| **验收要点** | - [ ] 执行 `.\gradlew.bat :runClient 2>&1 > client_startup.log 2>&1`，在 client_startup.log 中 grep `kenergyengineering` 确认 mod 初始化<br>- [ ] `select-string "ERROR\|FATAL\|Caused by" client_startup.log` 无 `kenergyengineering` 包路径的相关异常<br>- [ ] 日志中确认 `All registries loaded` 类型消息出现在 mod 加载完成时 |
| **回退** | 客户端崩溃 → 执行降级路径 D（git bisect 定位，24h 内无法修复则回退提交）；JVM 参数缺失 → 在 `build.gradle` 的 `runClient` 配置中添加 `--add-opens` 参数 |

---

#### TASK-051: 注册内容游戏内验证

| 属性 | 内容 |
|------|------|
| **task_id** | TASK-051 |
| **phase** | Phase 5 |
| **scope** | 游戏中创造模式标签页 + `/kenergyengineering` 命令 |
| **目标** | 在游戏中实地验证所有注册项可访问、放置、打开 GUI |
| **输入** | TASK-050（客户端已可启动） |
| **输出** | 验证截图/日志（`docs/ingame_verification.md`） |
| **依赖** | TASK-050（客户端可启动+mod 加载） |
| **DoD** | □ 创造模式搜索 `kenergyengineering` → 创造标签页 `Kenergy Engineering` 存在<br>□ 机器方块（至少 1 种）→ 放置后右键 → GUI 入口可打开（标题显示，槽位可见）<br>□ 矿石方块 → 放置后纹理显示正常（无紫黑贴图）<br>□ 物品（能量单元/升级）→ 持有在手中，NBT 数据存在<br>□ 流体 → 桶装后放置，流体纹理可渲染<br>□ `/kenergyengineering` 命令（如已注册）→ `/help kenergyengineering` 可查<br>□ 资源包/数据包无 `Missing model`/`Invalid blockstate` 错误 |
| **验收要点** | - [ ] 客户端启动后，`/give @p kenergyengineering:与预期一致的物品ID` 返回物品<br>- [ ] 创造模式搜索栏输入 `kenergyengineering` 结果不为空<br>- [ ] 至少一种机器右键打开 GUI 无控制台报错 |
| **回退** | 个别类别的项不能正常工作 → 记录到 `docs/phase5_known_issues.md`，不阻塞初步目标；GUI 完全不可用 → 回退到 TASK-031/032 的降级路径 C |

---

#### TASK-052: 服务端启动 + GameTest 运行

| 属性 | 内容 |
|------|------|
| **task_id** | TASK-052 |
| **phase** | Phase 5 |
| **scope** | `runServer` 任务；`runGameTestServer` 任务 |
| **目标** | 确认服务端可启动不崩溃，GameTest 全部通过或已知失败分类 |
| **输入** | TASK-050/TASK-051 已通过的源码 |
| **输出** | `server_startup.log` + `gametest_results.log` |
| **依赖** | TASK-050（客户端可启动，共享源码基础）；TASK-051（注册内容游戏内验证，与 TASK-052 互不阻塞可并行） |
| **DoD** | □ `.\gradlew.bat :runServer 2>&1` 启动完成，`Done (1.xxxs)!` 消息出现<br>□ 服务端日志无 `NullPointerException`/`ClassCastException` 涉及 `kenergyengineering` 包<br>□ 服务端存档加载（`/save-on` + `^C` 停止后重启）无崩溃<br>□ `.\gradlew.bat :runGameTestServer 2>&1` 运行完成<br>□ GameTest 结果：`Passed: X` > 0，`Failed: 0`；或失败已分类为「迁移引入」vs「原有问题」 |
| **验收要点** | - [ ] `select-string "Done" server_startup.log` 输出包含 Completed 时间<br>- [ ] `select-string "FAILED\|Error" gametest_results.log` 输出为空，或仅有已知问题分类记录<br>- [ ] `select-string "kenergyengineering" server_startup.log \| select-string "Exception"` 为空 |
| **回退** | 服务端启动崩溃 → 检查 `--add-opens` JVM 参数 + Mod 构造器日志；GameTest 失败 > 原有数量 → 记录新增失败列表，标记 `REG: regression` |

### Phase 6 — 收口与验证

**阶段目标**: 完成迁移、清理临时修改、执行完整收口验证，确认通过后再推送。

---

#### TASK-060: 代码清理

| 属性 | 内容 |
|------|------|
| **task_id** | TASK-060 |
| **phase** | Phase 6 |
| **scope** | 全项目源文件 |
| **目标** | 移除所有迁移过程中引入的临时标记，清理死代码/废弃 import |
| **输入** | Phase 1-5 全部完成的源码 |
| **输出** | 清理后的源码 |
| **依赖** | TASK-052（GameTest 全部确认） |
| **DoD** | □ `ugrep "// TODO: 26.1.2 migration\|// FIXME: 26.1.2" src/` 输出为空（全部已处理或已为已知 issue）<br>□ `ugrep "// HACK:.*[dD]eferred\|// FALLBACK:" src/` 输出全部审查，确认无必须清理项<br>□ 降级路径中使用的临时兼容层已移除（除非标记 `// KEEP: compatibility shim`）<br>□ IDE 中无 unused import 警告（可通过 `.\gradlew.bat checkstyleMain 2>&1` 确认） |
| **验收要点** | - [ ] `ugrep "TODO.*26\.1\.2" src/main/java` 输出为空<br>- [ ] `.\gradlew.bat :compileJava 2>&1` 清理后编译不失败 |
| **回退** | 清理引入新错误 → 回退本地更改，逐步清理 |

---

#### TASK-061: 文档更新

| 属性 | 内容 |
|------|------|
| **task_id** | TASK-061 |
| **phase** | Phase 6 |
| **scope** | `CHANGELOG.md`（如有）；`README.md`；`docs/` 下的临时文档 |
| **目标** | 更新版本信息文档，归档迁移过程中的临时记录文档 |
| **输入** | TASK-060 清理后的源码 |
| **输出** | 更新后的 `CHANGELOG.md`/`README.md`；已归档的临时文档 |
| **依赖** | TASK-060 |
| **DoD** | □ `CHANGELOG.md`（如存在）已添加 26.1.2 迁移条目<br>□ `README.md`（如存在）中版本信息已更新<br>□ `docs/phase1_deferred_items.md`、`docs/content_gaps.md` 等临时文档已归档到 `docs/migration/26.1.2/` 目录或标记为 `_archived` |
| **验收要点** | - [ ] `grep "26.1.2\|migrat" CHANGELOG.md` 有对应条目（如果文件存在） |
| **回退** | 文档更新非阻塞；可延至推送前补全 |

---

#### TASK-062: 构建验证最终（clean build）

| 属性 | 内容 |
|------|------|
| **task_id** | TASK-062 |
| **phase** | Phase 6 |
| **scope** | 完整 `clean build` 任务 |
| **目标** | 确认完整构建（含资源处理、重映射）无错误，产出 jar |
| **输入** | TASK-060/TASK-061 完成后的源码 |
| **输出** | `clean_build.log`；构建产出 jar |
| **依赖** | TASK-060（代码清理通过） |
| **DoD** | □ `.\gradlew.bat clean :build 2>&1` 退出码为 0<br>□ 输出 jar 存在于 `build/libs/` 目录（文件名含 `kenergyengineering`）<br>□ jar 文件大小 > 100KB（确保有内容，非空 jar）<br>□ `select-string "BUILD SUCCESSFUL" clean_build.log` 匹配 |
| **验收要点** | - [ ] `.\gradlew.bat clean :build 2>&1; $LASTEXITCODE` 返回 0<br>- [ ] `Get-ChildItem build/libs/*.jar` 存在且大小 > 100KB |
| **回退** | Build 失败 → 检查 `clean` 缓存冲突或资源重映射错误 |

---

#### TASK-063: 收口验证（完整验证命令集 + 证据记录）

| 属性 | 内容 |
|------|------|
| **task_id** | TASK-063 |
| **phase** | Phase 6 |
| **scope** | 全项目验证命令集（编译 + datagen + 启动 + GameTest） |
| **目标** | **在声称完成前**，运行完整的验证命令集并记录每一阶段的证据。**证据先于断言**——只有所有命令退出码为 0 + 日志无异常，才可标记初步目标完成。 |
| **输入** | TASK-062 构建成功的源码 |
| **输出** | `docs/closure_verification.md`（完整验证证据报告） |
| **依赖** | TASK-062（clean build 通过） |
| **DoD** | □ 运行 `.\gradlew.bat :compileJava :runData 2>&1` → 退出码 0，日志无 ERROR<br>□ 运行 `.\gradlew.bat :runClient 2>&1`（超时 60s 后自动退出）→ 日志中 `[kenergyengineering]` 初始化行正常，无 `Exception`/`Error` 异常模式<br>□ 运行 `.\gradlew.bat :runServer 2>&1`（超时 30s 后自动退出）→ `Done` 消息出现<br>□ 运行 `.\gradlew.bat :runGameTestServer 2>&1` → 全部通过或已知失败已分类<br>□ 回归核验：`git diff --stat` 确认变更文件列表合理（无意外修改）<br>□ §6 内容完整性检查清单所有 checkbox 为 `[x]`<br>□ 依赖版本确认清单所有「状态」列 = `confirmed`（或 `failed_with_ticket` 且不阻塞初步目标）<br>□ 上述全部证据已写入 `docs/closure_verification.md`，包含：每项命令的退出码、日志路径、关键输出片段、异常计数 |
| **验收要点** | - [ ] `.\gradlew.bat :compileJava :runData 2>&1; $LASTEXITCODE` → 0<br>- [ ] `runClient` 日志中 `select-string "kenergyengineering"` 无 `Exception\|Error`<br>- [ ] `runServer` 日志中 `select-string "Done"` 匹配<br>- [ ] `runGameTestServer` 结果中 `Failed: 0`（或已知分类记录）<br>- [ ] `docs/closure_verification.md` 存在且包含所有证据记录<br>- [ ] **完成标准**：上述全部验证通过 → 标记 **初步目标完成** |
| **回退** | 验证不通过 → 修复具体失败的 TASK 重新验证；无法在时限内完成 → 上报猫娘指挥官-莉莉丝，缩小范围或接受部分完成 |

---

#### TASK-064: 提交与推送

| 属性 | 内容 |
|------|------|
| **task_id** | TASK-064 |
| **phase** | Phase 6 |
| **scope** | Git 工作树 |
| **目标** | 执行完整收口验证后，引用 `分支收官` 技能，由猫娘指挥官-莉莉丝收集用户选择后执行最终收尾；不假定自动 merge |
| **输入** | TASK-063 验证通过的源码 |
| **输出** | 根据用户选择：推送远程分支 / 创建 PR / 仅本地完成 |
| **依赖** | TASK-063（收口验证通过） |
| **DoD** | □ 收口验证（TASK-063）全部通过，`docs/closure_verification.md` 已生成且证据完整<br>□ 调用 `分支收官` 技能，由猫娘指挥官-莉莉丝询问用户选择（推送远程 / 创建 PR / 本地暂存）<br>□ 根据用户选择执行对应收尾动作（不假定自动推送或 merge）<br>□ 操作完成后 `git status` 显示干净，无意外修改 |
| **验收要点** | - [ ] `git status` 显示 `nothing to commit, working tree clean`（提交后）<br>- [ ] `git push origin 26.1.2 2>&1` 无 rejected/failed 输出 |
| **回退** | 推送被拒绝 → `git pull --rebase origin 26.1.2` 解决冲突后重推 |

#### 收尾记录（TASK-064 执行结果）

| 属性 | 内容 |
|------|------|
| **执行状态** | ✅ 已完成（初步目标） |
| **最终提交** | `25b0daa feat: migrate project to 26.1.2 runtime baseline` |
| **远程分支** | `origin/26.1.2` |
| **收官选择** | 保留分支（不创建 PR、不合并、不清理分支） |
| **验证证据** | `docs/closure_verification.md` |
| **分支策略** | 独立版本分支 `26.1.2` 长期保留，后续扩展按新任务处理 |
| **系统状态** | `git status` → nothing to commit, working tree clean |

## 4. 依赖版本确认清单

> **使用说明**: Phase 0 的 TASK-001 负责填充本清单的「证据来源」「验证命令」「状态」三列。状态列允许 `unknown`，
> 但必须已在 Phase 0 创建对应 TASK 进行实验验证。所有状态最终应在 Phase 6 TASK-063 收口验证前达到 `confirmed`。

| 依赖 | 当前版本 | 目标版本 | 来源文件 | 证据来源 | 验证命令 | 状态 | 归属 TASK |
|------|---------|---------|---------|---------|---------|------|---------|
| NeoForge | 26.1.2 | 26.1.2.78 | `libs.versions.toml` | ✅ NeoForge maven: `net.neoforged:neoforge:26.1.2.78` | `.\gradlew.bat dependencies \| grep neoforge` | confirmed | TASK-003 |
| Minecraft | 1.21.1 | 1.21.1 | `libs.versions.toml` | ✅ 与 NeoForge 26.1.2 对应 | `.\gradlew.bat dependencies \| grep minecraft` | confirmed | TASK-003 |
| Parchment | 2024.11.17 | 无 26.1.2 兼容版本 | `libs.versions.toml` | https://parchmentmc.org/docs/getting-started → MC 26.1.2 无对应 | 已注释禁用，使用 Mojang 参数名 | disabled | TASK-001 |
| modDevGradle | 2.0.141 | 2.0.141（待验证） | `libs.versions.toml` | https://github.com/neoforged/moddevgradle/releases | 检查 26.1.2 兼容说明 | unknown | TASK-001 |
| LDLib2 | 2.2.8 | 26.1.2.27 | `forge.versions.toml` | ✅ 已检索确认：`ldlib2-neoforge-26.1:26.1.2.27` | `.\gradlew.bat dependencies --refresh-dependencies \| grep ldlib2` | confirmed | TASK-004 |
| Registrate | MC1.21-1.3.0+67 | ❌ 无 26.1.2 版本 | `forge.versions.toml` | ❌ GitHub releases/Modrinth 无 26.1.2 兼容版本 | 已注释依赖，需迁移 NeoForge DeferredRegister | failed | TASK-001 |
| Configuration | 3.1.1-neoforge | ❌ 无 26.1.2 坐标 | `forge.versions.toml` | ❌ repsy.io / GitHub 无 26.1.2 发布 | 已注释依赖，需迁移 ModConfigSpec | failed | TASK-001 |
| JEI | 19.25.1.328 | 29.13.0.42 | `forge.versions.toml` | ✅ 已检索确认：`jei-26.1.2-neoforge:29.13.0.42` | `.\gradlew.bat dependencies \| grep jei` | confirmed | TASK-005 |
| EMI | 1.1.22+1.21.1 | ❓Phase 4/5 恢复 | `forge.versions.toml` | ❓可选 dep，坐标不确定 26.1.2 版本 | 已注释禁用，Phase 4/5 恢复 | deferred | TASK-005 |
| Jade | 15.10.0+neoforge | ❓Phase 4/5 恢复 | 外部 Maven | ❓可选 dep，坐标不确定 26.1.2 版本 | 已注释禁用，Phase 4/5 恢复 | deferred | TASK-005 |
| Sodium | 0.6.13-neoforge | ❓Phase 4/5 恢复 | Modrinth | ❓可选 dep，坐标不确定 26.1.2 版本 | 已注释禁用，Phase 4/5 恢复 | deferred | TASK-005 |
| Iris | 1.8.8+1.21.1-neoforge | ❓Phase 4/5 恢复 | Modrinth | ❓可选 dep，坐标不确定 26.1.2 版本 | 已注释禁用，Phase 4/5 恢复 | deferred | TASK-005 |
| ModernUI | 3.12.0.2 | ❓Phase 4/5 恢复 | Modrinth | ❓可选 dep，坐标不确定 26.1.2 版本 | 已注释禁用，Phase 4/5 恢复 | deferred | TASK-005 |
| ModernFix | 5.24.3+mc1.21.1 | ❓Phase 4/5 恢复 | Modrinth | ❓可选 dep，坐标不确定 26.1.2 版本 | 已注释禁用，Phase 4/5 恢复 | deferred | TASK-005 |
| Spark | 最新 | ❓Phase 4/5 恢复 | Curse Maven | ❓可选 dep，坐标不确定 26.1.2 版本 | 已注释禁用，Phase 4/5 恢复 | deferred | TASK-005 |
| Mixin | 0.8.7 | ❓Java 25 兼容性 | `libs.versions.toml` | https://github.com/SpongePowered/Mixin/issues | GitHub Issues `is:issue "java 25"` | unknown | TASK-001 |
| Lombok | 1.18.38 | ✅ Java 25 | `build.gradle` | https://projectlombok.org/changelog | 版本号确认 | confirmed | TASK-001 |

**状态列说明**: `confirmed` = 已通过验证命令确认，输出匹配预期；`unknown` = 尚未验证，归属 TASK 负责执行。
**门禁规则**: Phase 0 中若 LDLib2 或其他关键依赖不可解析 → 立即进入降级路径 A。
**证据归档**: TASK-001 完成后所有 `unknown` 必须变更为 `confirmed` 或 `failed_with_ticket`。

---

## 5. API 迁移清单

### LDLib2 包路径迁移（预计高工作量）

| 旧路径 | 新路径（推测） | 涉及文件数 |
|--------|-------------|-----------|
| `com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType` | `com.lowdragmc.ldlib2.gui.factory.BlockUIMenuType` | 2+ |
| `com.lowdragmc.lowdraglib2.gui.ui.*` | `com.lowdragmc.ldlib2.gui.ui.*` | 1（GUI 工厂）+ 各 BE |
| `com.lowdragmc.lowdraglib2.gui.texture.*` | `com.lowdragmc.ldlib2.gui.texture.*` | 1 |
| `com.lowdragmc.lowdraglib2.syncdata.*` | `com.lowdragmc.ldlib2.syncdata.*` | ~5（BE 体系） |
| `com.lowdragmc.lowdraglib2.syncdata.holder.blockentity.ISyncPersistRPCBlockEntity` | ❓可能合并/更名 | ~8 |
| `com.lowdragmc.lowdraglib2.syncdata.storage.FieldManagedStorage` | `ManagedStorage`? | ~8 |
| `dev.vfyjxf.taffy.*` | ❓可能独立或并入 LDLib2 | ~1 |

### NeoForge API 迁移

| API | 旧（1.21） | 新（26.1） | 优先级 |
|-----|-----------|-----------|--------|
| Capability 注册 | `RegisterCapabilitiesEvent` | ❓可能移除 → Resource/Transaction | 高 |
| 网络 | `RegisterPayloadHandlersEvent` | 同（可能包路径变） | 高 |
| 事件总线 | `IEventBus` / `@SubscribeEvent` | 同 | 中 |

### 第三方库

| 库 | 关注点 | 工作量估计 |
|----|-------|-----------|
| Registrate | `AbstractRegistrate` 构造、`ProviderType` | 低 |
| Configuration | `Configuration.registerConfig` 签名 | 低 |
| JEI 19.x | `IRecipeCategory`、`IGuiHelper` API 变化 | 中 |
| EMI | `EmiRecipe`、`EmiStack` API 变化 | 中 |

---

## 6. 内容完整性检查清单

### 核验方法

本清单不预设静态预期数量。**所有注册项核验以 `模组扫查` 技能对以下注册类声明的扫描结果为基准**，逐项比对后记录差异。

| 注册分类 | 来源类 | 核验方式 |
|---------|-------|---------|
| 方块 | `TENBlocks.java` | 模组扫查输出 ↔ 类中 `RegistryObject` 声明逐一比对 |
| 物品 | `TENItems.java` | 同上 |
| 流体（Block + Fluid + BucketItem） | `TENFluids.java` | 同上 |
| BlockEntity | `TENBlockEntities.java` | 每个机器方块对应 BE + Cable/Pipe BE |
| 配方类型 | `TENRecipeTypes.java` | 每个机器配方序列化器 + 配方类型注册 |

> **执行方式**: 在 Phase 4 TASK-040 中运行 `模组扫查`，输出扫描结果 JSON/表格，与上表来源类逐行比对，差异项记录到 `docs/content_gaps.md`。

### 注册项（Phase 4 执行核验后打钩）

**方块**（与 `TENBlocks.java` 声明比对）：
- [ ] 机器方块（每个 MachineType 对应一个）
- [ ] 矿石方块
- [ ] 金属存储块
- [ ] 原料存储块
- [ ] 线缆/管道块
- [ ] 差异项已记录到 `docs/content_gaps.md`（如有）

**物品**（与 `TENItems.java` 声明比对）：
- [ ] 能量单元（Energy Unit）
- [ ] 升级物品
- [ ] 材料/部件
- [ ] 桶装流体形式
- [ ] 差异项已记录（如有）

**流体**（与 `TENFluids.java` 声明比对）：
- [ ] 每种流体对应 `Block` + `Fluid` + `BucketItem`
- [ ] 差异项已记录（如有）

**BlockEntity**（与 `TENBlockEntities.java` 声明比对）：
- [ ] 每个机器方块对应 BE 类型
- [ ] `CableBlockEntity`
- [ ] `PipeBlockEntity`
- [ ] 差异项已记录（如有）

**配方类型**（与 `TENRecipeTypes.java` 声明比对）：
- [ ] 每个机器配方序列化器
- [ ] 配方类型注册
- [ ] 差异项已记录（如有）

### 资源完整性

**GUI 纹理**（`assets/kenergyengineering/textures/gui/`，与目录实际文件逐一核对）：
- [ ] `handler.png`
- [ ] `one_to_one.png`
- [ ] `pulverizer.png`
- [ ] `compressor.png`
- [ ] `one_to_one_fluid.png`
- [ ] `three_to_one.png`
- [ ] `two_to_one.png`
- [ ] `matter_condenser.png`
- [ ] `enchantment_flusher.png`
- [ ] `beacon_simulator.png`
- [ ] `mob_ripper.png`
- [ ] `quarry.png`
- [ ] `farm_manager.png`
- [ ] `energy_cell.png`
- [ ] `engine_solar.png`
- [ ] `engine.png`

**数据资源**：
- [ ] `data/kenergyengineering/recipes/` — 所有配方
- [ ] `data/kenergyengineering/advancements/` — 进度
- [ ] `data/kenergyengineering/loot_tables/` — 战利品表
- [ ] `data/kenergyengineering/tags/` — 标签

**语言文件**：
- [ ] `en_us` 完整翻译
- [ ] `zh_cn` 完整翻译（如有）

---

## 7. 风险与回退

### 风险矩阵

| 风险编号 | 风险描述 | 概率 | 影响 | 触发阶段 | 缓解措施 |
|---------|---------|------|------|---------|---------|
| R01 | LDLib2 26.1.2 artifact 不可用（未发布/版本号变更） | 高 | 高 | Phase 0 | 降级路径 A |
| R02 | LDLib2 26.1.x Beta API 不稳定，Bean 注解/ValueIO 范式与现有代码不兼容 | 中 | 高 | Phase 2-3 | 降级路径 B |
| R03 | NeoForge 26.1.2 移除 Capability 事件，强制 Resource/Transaction 迁移 | 中 | 高 | Phase 1 | 预留 3-5 天迁移时间 |
| R04 | Mixin 0.8.7 在 Java 25 上抛出 JVM 兼容异常 | 低 | 高 | Phase 0-1 | 升 Mixin 到 0.9.0+ |
| R05 | Parchment 26.1.2 无对应映射 | 低 | 中 | Phase 0 | 临时移除 Parchment |
| R06 | Registrate 1.3.0+67 与 NeoForge 26.1 不兼容 | 低-中 | 中 | Phase 1-2 | 回退到手动注册 + 移除 Registrate |
| R07 | GUI 重构工作量远超预期（> 30 文件） | 中 | 中 | Phase 3 | 降级路径 C |
| R08 | `hurtServer` / `JAVA_25` 反射访问限制阻塞部分 Mod 运行 | 中 | 中 | Phase 5 | 添加 `--add-opens` JVM 参数 |
| R09 | 内容完整性缺失（配方/模型未生成） | 低-中 | 低 | Phase 4 | 分批修复，不阻塞整体迁移 |
| R10 | GameTest 失败率过高 | 低 | 中 | Phase 5 | 按已知/新增分类处理 |

### 回退策略

#### 降级路径 A（LDLib2 不可用 → 最小核心模式）

**条件**: Phase 0 中 LDLib2 解析失败，或 26.1.x 版本不可用。

**操作**:
1. 注释 `dependencies.gradle` 中所有 LDLib2 依赖行
2. 注释/排除 `@Mod` 构造器中与 LDLib2 GUI 相关代码
3. 创建 `BE 兼容层`: 将 `ISyncPersistRPCBlockEntity` 替换为自定义手动 NBT 读写（回退到 `loadAdditional`/`saveAdditional` 传统路径）
4. 禁用所有机器 GUI（使用 `BlockUIMenuType` 降级为无 GUI 或 HUD-only）
5. 建立一个单独文件 `/docs/LDLIB2_GUI_DEPENDENCY.md` 记录待恢复项
6. 目标：**核心注册 + 机器 tick + 能量逻辑可运行**

**验证方式**: `compileJava` 通过，`runClient` 可启动（无 GUI 可接受）

---

#### 降级路径 B（LDLib2 SyncData API 不兼容 → 兼容层）

**条件**: Phase 2 中 `@Persisted`/`@DescSynced`/`FieldManagedStorage` 与 26.1.x 严重不兼容。

**操作**:
1. 保留旧版注解在新包路径的映射（如果仅包路径变化 → 全局替换 import）
2. 如果 `ValueInput`/`ValueOutput` 是新范式但过于复杂：
   - 为 `CmMachineBlockEntity` 手工实现 NBT 序列化（回退到手动读写）
   - 为 `@DescSynced` 替换为 `CustomPacketPayload` 手动同步
3. 记录改造点，标注 `// HACK: 26.1.2 compatibility layer`

**验证方式**: BE 数据可持久化保存/加载，客户端能看到基本 GUI 数据

---

#### 降级路径 C（GUI 迁移量过大 → 暂缓 GUI 重构）

**条件**: Phase 3 发现 LDLib2 GUI API 与旧版完全不兼容，需要全面重写 `TENMachineBlockUIFactory.java`。

**操作**:
1. 为每个机器创建"占位 GUI"（仅显示标题 + 机器状态文字，不渲染 LDLib2 组件）
2. 将所有 GUI 迁移任务推迟到 Phase 6+（创建 issue 跟踪）
3. 继续推进 Phase 4-5，不阻塞整体编译通过

**验证方式**: `runClient` 可打开 GUI（显示文字内容即可，图形元素可选）

---

#### 降级路径 D（游戏启动失败 → 逐步回退提交）

**条件**: Phase 5 中游戏启动崩溃，且 24h 内无法定位根因。

**操作**:
1. `git stash` 或回退到上一已知可启动提交
2. 使用二分法定位崩溃 commit
3. 创建独立分支 `fix/crash-YYYYMMDD` 隔离调试
4. 主分支继续推进其他可运行功能

---

## 8. 提交与推送策略

### 提交策略

| 阶段 | 提交次数 | 提交信息模式 | 说明 |
|------|---------|------------|------|
| Phase 0 | 1 | `chore: update version catalogs for 26.1.2` | 版本目录 + 依赖配置变更 |
| Phase 1 | 1-2 | `fix: [area] 26.1.2 compilation fixes` | 最小编译修复 |
| Phase 2 | 2-3 | `migrate: [api-name] 26.1.2` | 核心 API 迁移 |
| Phase 3 | 1-2 | `migrate: ldlib2 gui 26.1.x` | GUI 适配 |
| Phase 4 | 1 | `fix: datagen and content integrity` | 内容完整性 |
| Phase 5 | 0-1 | `fix: runtime issues` | 运行时修复 |
| Phase 6 | 1 | `migrate: complete 26.1.2 migration` | 收口 |

### 推送策略

- 每个 Phase 完成后推送（保持远程按阶段可追踪）
- 推送前确保 `compileJava` 通过 + 快速启动验证
- 不 push 半成品（除非紧急备份）

### 分支管理

- 主线: `26.1.2`
- 紧急修复: `fix/<issue>-<date>` → merge 到 `26.1.2`
- 暂缓功能: 不创建分支，使用 `§6 内容完整性检查清单` 跟踪

---

## 9. 工作量估算

| Phase | 预计文件变更 | 预计耗时 | 依赖 |
|-------|------------|---------|------|
| Phase 0 | 3-5 | 1-2h | 无 |
| Phase 1 | 15-25 | 4-8h | Phase 0 |
| Phase 2 | 20-35 | 8-16h | Phase 1 |
| Phase 3 | 15-25 | 4-8h | Phase 2 |
| Phase 4 | 5-15 | 2-4h | Phase 2 |
| Phase 5 | 0-10 | 2-4h | Phase 2-4 |
| Phase 6 | 3-5 | 1h | Phase 5 |

**总计预计**: 22-43h（含风险缓冲约 30-60h）
**实际耗时取决于 LDLib2 兼容程度和降级深度**。

---

## 10. 协作需求

| 角色 | 阶段 | 具体任务 |
|------|------|---------|
| 猫娘指挥官-莉莉丝 | Phase 0 | 确认优先级：哪条降级路径优先 |
| 猫娘勘探员-露娜 | Phase 2 | 探索 LDLib2 26.1.x 实际 API 签名 |
| 猫娘检索员-诺雅 | Phase 0/2 | 检索 NeoForge 26.1.2 changelog，确认 Capability 移除状态 |
| 猫娘审查官-艾琳 | Phase 0 后 | 审查本计划草稿 |
| 猫娘编写官-米娅 | Phase 1-6 | 执行代码迁移 |
| 猫娘挑刺官-薇拉 | Phase 5 | 挑刺运行时代码问题 |

---

## 附录: 环境信息

```
工作目录: E:\GitHub\Technical-Engineering-4\Technical-Engineering-4-26.1.2
Java:     需要 >= 25 (当前 toolchain 已设置)
OS:       Windows
NeoForge: 26.1.2
Git HEAD: 25b0daa feat: migrate project to 26.1.2 runtime baseline
远程:     origin/26.1.2
```
