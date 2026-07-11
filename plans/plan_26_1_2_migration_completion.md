## 计划元数据

- 计划ID: `26_1_2_migration_completion`
- 草稿路径: `plans/.draft_plan_26_1_2_migration_completion.md`
- 计划路径: `plans/plan_26_1_2_migration_completion.md`
- 版本状态: `已批准`
- 创建日期: 2026-07-11
- 创建者: 猫娘规划师-缇娅
- 触发原因: 审计发现16项未完成迁移残项，用户要求"逐个攻破"切换到任务模式；草稿第2版经增量审查通过
- **审查结论**: 审查通过
- **通过范围**: 草稿第2版全部34 TASK、P0-P9阶段结构、依赖链、DoD、验证矩阵（后经计划调整 +2 → 36，再 +2 → 38 TASK）
- **残留风险（非阻断）**: ugrep/rg混用、风险矩阵R09/R10重复、TASK-092依赖精度、TASK-090未显式覆盖所有P8——均在本次正式转换中消除
- **版本基线修正声明**: 初始草稿/旧计划中目标基线标注 `Minecraft 1.21.1` 属历史误判——Mojang 已采用年号制，`26.1.2` 是真实版本而非 `1.21.1` 别名。旧计划/草稿只读保留不作静默改写；当前 formal plan 及 evidence 为权威版本。
- **计划调整记录**: 
  - `REVIEW-TASK-010-20260711` (2026-07-11): 执行扫描发现 publish.yml 环境 JAVA:'21' 及 CONTRIBUTING.md 三处 Java 21 残留，非初始审计遗漏而是执行中发现。纳入 P1 为 TASK-010A、TASK-010B。TASK 总数 34→36，P1 TASK 数 2→4。
  - `PLAN-CORRECTION-VERSION-20260711` (2026-07-11): 官方外部裁决 Minecraft 26.1.2 为真实版本（Mojang 年号制）；NeoForge 26.1.2.78 前三段目标 MC 26.1.2；Java 25。修正基线 `Minecraft 1.21.1` → `Minecraft 26.1.2`。同时执行扫描发现 jar 双重版本命名、publish MC_VERSION、publish-on-release 版本语义、多文档版本引用残项。纳入 P1 为 TASK-010C、TASK-010D。TASK 总数 36→38，P1 TASK 数 4→6。证据: `plans/.evidence/evidence_26_1_2_migration_completion_02.md`。
- 执行状态: P0 ✅ · TASK-010 ✅ · TASK-010A ✅ · TASK-010B ✅ · TASK-010C ✅（2026-07-11）
- 建议下一步: 进入 TASK-010D 发布目标游戏版本与 glob 对齐
- next_hop: 执行 TASK-010D — 更新 publish.yml MC_VERSION + glob 对齐
- 目标基线: NeoForge 26.1.2.78 / Minecraft 26.1.2（Mojang 年号制，非 1.21.1 别名）/ Java 25
- 当前 HEAD: `7fe63cb` fix: remove duplicate Minecraft version from jar names
- 本地分支: `feat/26.1.2-datagen-migration`
- 工作区状态: clean — TASK-010C 已提交
- 前置计划: `plan_26_1_2_migration.md`（已完成初步目标，只读保留）、`plan_26_1_2_datagen_migration.md`（13/13 完成，只读保留）、`plan_26_1_2_resource_fix.md`（历史参考）

## 执行状态

| TASK | 阶段 | 状态 | 完成日期 | 审查人 | 证据 |
|------|------|------|---------|--------|------|
| TASK-000 | P0 | ✅ 已完成 | 2026-07-11 | 艾琳 | 8aafdc3, ce6f653 |
| TASK-001 | P0 | ✅ 已完成 | 2026-07-11 | 艾琳 | 66f2ff5 |
| TASK-002 | P0 | ✅ 已完成 | 2026-07-11 | 艾琳 | e4de678 |
| TASK-010 | P1 | ✅ 已完成 | 2026-07-11 | 艾琳 | 2358866 |
| TASK-010A | P1 | ✅ 已完成 | 2026-07-11 | 艾琳 | c57c8fb |
| TASK-010B | P1 | ✅ 已完成 | 2026-07-11 | 艾琳 | 458c7bb |
| TASK-010C | P1 | ✅ 已完成 | 2026-07-11 | 艾琳 | 7fe63cb |
| TASK-010D | P1 | 🔄 **当前任务** | — | — | — |
| TASK-011+ | P1-P9 | ⏳ 待执行 | — | — | — |

## 计划正文

# 计划: 26.1.2 迁移完成 — 残项逐个攻破

## 概述

**目标**: 在 NeoForge 26.1.2 上实现与旧版本功能齐平的稳定运行。审计确认127旧Java生产文件全部有语义对位，无ID丢失。本计划以16项已知残项清单为执行对象，逐项攻破。

**范围边界（不做）**:
- ❌ GUI 完整美观/模块化重构/性能优化/新功能开发
- ❌ EMI 伪恢复（官方无26.1.2，非官方fork不采用）
- ❌ CI/publishing mod_url/分支过滤器等迁移前已存在问题（归为backlog）
- ❌ 批量资源清理/盲删generated-only配方
- ❌ 强制升Mixin/ModDevGradle版本（仅核验不盲升）
- ❌ 不force push、不删旧计划、不盲删资源、不提交registry_dump

**约束原则**:
1. 每项完成验证后再下一项，禁止合为大提交
2. DoD 可量化可验证：静态验证 → compile → GameTest → runClientData diff → clean build → runClient人工
3. en_us/zh_cn 维护，en_ud 不改
4. 旧计划只读；registry_dump 不提交
5. 每一项必须明确降级/回退路径

## 阶段总览

| 阶段 | 名称 | TASK数 | 关键依赖链 |
|------|------|--------|-----------|
| P0 | 工作区检查点 —— 安全收口 | 3 | 无（最先执行） |
| P1 | 工具链修复 | 6 | P0 |
| P2 | Configuration 迁移 | 2 | P0 |
| P3 | Transfer 边界恢复 | 5 | P0（含新 TASK-031A BE accessor） |
| P4 | Transfer 内部迁移 | 6 | P3 |
| P5 | 持久化恢复 | 1 | P4 |
| P6 | 用户功能恢复 | 4 | P0（大部分独立） |
| P7 | FluidModel 修复 | 1 | P0（独立探索） |
| P8 | 存根清理 & TODOs | 4 | 最小化：080A/080B 仅 P0，081/082 仅 P2 |
| P9 | 全量验证 & 收官 | 6 | P8（含新 TASK-090A test sourceSet） |

**总计**: **38 TASK**，按阶段顺序执行，P6/P7/P8(080A/080B) 可与 P3-P5 并行（依赖标记为独立时）。

## 任务清单

---

### Phase 0 — 工作区检查点（安全收口）

> ⚠️ **关键约束**：当前 workspace 有 modified 文件（dependencies.gradle、gradle/forge.versions.toml）和 untracked 文件（docs/runclient_development_environment.md、registry_dump.json、scripts/validate_runclient_mods.py）。五个 clientLocalRuntime 开发模组已验证启动到主菜单（validator 9/9）。**首个执行节点必须先安全收口这几个开发环境交付文件，不能让后续覆盖。**

#### TASK-000: registry_dump.json 精确排除 + 暂存未提交文件

- task_id: `TASK-000`
- phase: P0
- scope:
  - `.gitignore`（追加 registry_dump.json）
  - `registry_dump.json`（审计诊断产物，不得提交）
  - `dependencies.gradle`（当前修改内容）
  - `gradle/forge.versions.toml`（当前修改内容）
  - `docs/runclient_development_environment.md`
  - `scripts/validate_runclient_mods.py`
- **关键约束（审查裁决）**: registry_dump.json 为本轮审计生成诊断产物且不得提交。执行时先核查无引用（`rg registry_dump src/`），再精确 `.gitignore` 排除或手动删除。不批量删除未知用户文件。
- 目标: 确保 registry_dump.json 不被提交；当前 dirty/untracked 开发环境文件准备就绪进入 checkpoint commit
- DoD（已全部通过 ✓）:
  - ☑ **registry_dump.json 处理**:
    - `rg "registry_dump" src/` → 无引用依赖（源码零匹配）
    - `.gitignore` 末尾追加 `/registry_dump.json`（根目录精确规则）
    - `git status` → registry_dump.json 不再出现在 Untracked 列表
  - ☑ `dependencies.gradle`、`forge.versions.toml` 的修改内容已确认属于开发环境启动验证（五个 clientLocalRuntime 模组接入），功能正确
  - ☑ 开发环境文档 `docs/runclient_development_environment.md` 已验证内容准确
  - ☑ `scripts/validate_runclient_mods.py` 已验证可执行
- 验收要点（已通过）:
  - [✓] `rg "registry_dump" src/` 无项目引用
  - [✓] `git status`：registry_dump.json 不在 untracked 或 modified 列表
  - [✓] `.gitignore` 末尾追加 `/registry_dump.json` 一行
- 输入: 当前工作区所有 dirty/untracked 文件
- 输出: 干净的 staged 准备状态
- 依赖: 无
- 风险/回退: `.gitignore` 修改意外 → `git checkout -- .gitignore` 恢复
- **执行记录**:
  - 完成日期: 2026-07-11
  - 审查人: 艾琳（审查通过）
  - Commit A: `8aafdc3` — `docs: add 26.1.2 migration completion plan`（草案+正式计划）
  - Commit B: `ce6f653` — `chore: ignore migration registry dump`（`.gitignore` 根规则 `/registry_dump.json`）
  - Push: `13bb9fc..ce6f653` → 远程分支已同步
  - 验证摘要:
    - `rg "registry_dump" src/` → 无引用 ✅
    - 根路径被 ignore（子目录不误匹配）✅
    - 文件仍存在（未误删）✅
    - 旧计划未修改 ✅
    - 工作区已清理至仅剩 TASK-001 四文件 ✅

#### TASK-001: Checkpoint commit — 开发环境基线

- task_id: `TASK-001`
- phase: P0
- scope: 同 TASK-000 文件
- 目标: 提交当前已验证的开发环境状态，建立后续迁移操作的干净基线
- DoD（已全部通过 ✓）:
  - ☑ `git add` 包含：dependencies.gradle、gradle/forge.versions.toml、docs/runclient_development_environment.md、scripts/validate_runclient_mods.py、`.gitignore`
  - ☑ 提交信息: `feat: establish 26.1.2 dev environment baseline`
  - ☑ `git push` 到远程（`-u` 首次成功）
  - ☑ `git status` → `nothing to commit, working tree clean`
- 验收要点（已通过）:
  - [✓] `git log --oneline -1` → `66f2ff5 feat: establish 26.1.2 dev environment baseline`
  - [✓] `git diff HEAD` → 空输出
  - [✓] 推送后远程分支同步（`ce6f653..66f2ff5`）
- 输入: TASK-000 的 clean staging
- 输出: checkpoint commit
- 依赖: TASK-000
- 风险/回退: 推送被拒 → `git pull --rebase` 后重推
- **执行记录**:
  - 完成日期: 2026-07-11
  - 审查人: 艾琳（审查通过）
  - Commit: `66f2ff5` — `feat: establish 26.1.2 dev environment baseline`
  - Push: `ce6f653..66f2ff5` → 远程分支已同步
  - 验证摘要:
    - validator 9/9 exit0 ✅
    - `compileJava`/`jar` 成功 ✅
    - 五 dev mods 仅 clientRuntime，jar 无泄漏 ✅
    - `runClient` 十 mod 到主菜单，TEN 0 ERROR/FATAL ✅
    - Missing item model 0 / FluidModel 10 归入 TASK-070 ✅
    - 用户确认 JEI/Jade/ModernUI 三项正常 ✅
    - `git status` → clean ✅

#### TASK-002: 编译器警告门禁基线确认

- task_id: `TASK-002`
- phase: P0
- scope: `build.gradle`（`options.compilerArgs << "-Xlint:-removal"`）；`src/main/java` 全量
- 目标: 记录当前编译输出基线（含 deprecation/removal 警告数量），为后续 P4 移除 `-Xlint:-removal` 提供对比
- DoD（已全部通过 ✓）:
  - ☑ 运行 `.\gradlew.bat :compileJava *> compile_warn_baseline.log` → exit0 BUILD SUCCESSFUL 23s
  - ☑ 运行 `.\gradlew.bat :compileTestJava *>> compile_warn_baseline.log` → exit1（30 GameTest API errors，归入 TASK-090A）
  - ☑ 静态旧 Transfer 约 20 生产文件 / 80 真实调用已分类标注
  - ☑ `docs/compile_warning_baseline_26.1.2.md` 已写入并提交
- 验收要点（已通过）:
  - [✓] `compile_warn_baseline.log` 存在且包含分类统计
  - [✓] 旧传输 API 相关警告计数已明确标注
- 输入: 当前 build.gradle 编译配置
- 输出: `docs/compile_warning_baseline_26.1.2.md`
- 依赖: TASK-001
- 风险/回退: 编译失败 → 先记录失败原因，待后续阶段修复
- **执行记录**:
  - 完成日期: 2026-07-11
  - 审查人: 艾琳（两轮审查最终通过）
  - Commit: `e4de678` — `docs: record 26.1.2 compiler warning baseline`
  - Push: `66f2ff5..e4de678` → 远程分支已同步
  - 验证摘要:
    - `compileJava` rerun → exit0 BUILD SUCCESSFUL 23s ✅
    - `compileTestJava` → exit1（30 GameTest API errors，已归入 TASK-090A）✅
    - 静态旧 Transfer 约 20 生产文件 / 80 真实调用已分类 ✅
    - `compile_warn_baseline.log` 与基线文档双文件已提交 ✅

---

### Phase 1 — 工具链修复

> 旧 plan 未覆盖的 CI/元数据/文档/构建/发布问题，修正 JDK 版本、MC 版本基线及发布链残项。含执行扫描发现的新增残项（TASK-010A~010D）。

#### TASK-010: CI JDK 版本修正 21→25

- task_id: `TASK-010`
- phase: P1
- scope:
  - `.github/actions/build_setup/action.yml`（`java-version: 21` → `25`）
  - `mise.toml`（`java = "21"` → `"25"`）
- 目标: 使 CI 构建环境和本地 mise 工具链与 JDK 25 一致；build toolchain 已为 25 且 `auto-download=false`，CI 必须匹配
- DoD（已全部通过 ✓）:
  - ☑ `action.yml` 中 `java-version: 21` → `25`
  - ☑ `mise.toml` 中 `java = "21"` → `"25"`
  - ☑ tomllib/YAML 结构验证通过；`git diff` 仅含上述两处变更 + formal plan 增量
  - ☑ `.\gradlew.bat :compileJava` no-daemon rerun → exit0 1m5s（不退化）
- 验收要点（已通过）:
  - [✓] `grep "java-version" .github/actions/build_setup/action.yml` → `25`
  - [✓] `grep 'java =' mise.toml` → `java = "25"`
  - [✓] 变更后 `.\gradlew.bat :compileJava` exit0
- 输入: `.github/actions/build_setup/action.yml`、`mise.toml`
- 输出: 上述两文件的 JDK 版本修正
- 依赖: P0
- 风险/回退: CI 配置语法错误 → `git revert` 回退
- **执行记录**:
  - 完成日期: 2026-07-11
  - 审查人: 艾琳（条件通过 + 计划任务审查通过）
  - Commit: `2358866` — `fix: align CI JDK and mise toolchain to JDK 25`
  - Push: `e4de678..2358866` → 远程分支已同步
  - 验证摘要:
    - `action.yml` java-version 25 ✅
    - `mise.toml` java = "25" ✅
    - tomllib/YAML 结构正确 ✅
    - `compileJava` no-daemon rerun → exit0 1m5s ✅
    - worktree clean ✅

#### TASK-010A: 发布元数据 Java 21→25（publish.yml）

- task_id: `TASK-010A`
- phase: P1
- scope:
  - `.github/workflows/publish.yml`（第99、132行环境 `JAVA: '21'` → `'25'`）
- 目标: 将 mc-publish action 的 java 参数从 21 更正为 25，反映当前构件 major 69 / JDK 25，无 `--release` 降级
- **关键约束（审查裁决）**:
  - 仅改 `env.JAVA` 值（两处共享 env + 实际 input 引用），不改 workflow 分支/发布目标/secret
  - Mixin `JAVA_21` 明确非残项不做
  - 先 RED 断言确认当前值含 `21`，再 GREEN 改为 `25`，再 RED 确认无残留 `'21'`
  - `rg` 全 workflow 文件核验无错误 21 残留
- DoD（已全部通过 ✓）:
  - ☑ **RED 断言**：`grep "JAVA.*21"` → 第99、132行匹配
  - ☑ **GREEN 修改**：两处 `JAVA: '21'` → `JAVA: '25'`
  - ☑ **RED 再断言**：`rg "JAVA.*'21'"` → 无输出；`rg "'21'"` → 仅 version 等非 JAVA 引用保留
  - ☑ **YAML parse**：`python -c "import yaml; yaml.safe_load(open(...))"` → exit0
  - ☑ **diff 审查**：`git diff` 仅 publish.yml + formal plan + evidence 02（scope 内仅版本变更）
- 验收要点（已通过）:
  - [✓] `grep "JAVA.*'25'" .github/workflows/publish.yml` → 两行（第99、132行）
  - [✓] `rg "'21'" .github/workflows/publish.yml` → 无 JAVA 引用残留（非 JAVA 的 `21` 确认评估）
  - [✓] `git diff` 仅 version 数值变更
- 输入: `.github/workflows/publish.yml`
- 输出: JDK 版本修正后的 publish.yml
- 依赖: TASK-010（build_setup 与 mise 先对齐，再修正发布元数据）
- 风险/回退: YAML 缩进损坏 → `git checkout -- .github/workflows/publish.yml` 恢复
- **执行记录**:
  - 完成日期: 2026-07-11
  - 审查人: 艾琳（版本纠错增量审查通过）
  - Commit: `c57c8fb` — `fix: align publish Java metadata with Minecraft 26.1.2`
  - Push: `2358866..c57c8fb` → 远程分支已同步
  - 验证摘要:
    - publish.yml JAVA: '25' × 2 ✅
    - `rg "JAVA.*'21'"` → 无输出 ✅
    - YAML parse exit0 ✅
    - worktree clean ✅
- 输入: `.github/workflows/publish.yml`
- 输出: JDK 版本修正后的 publish.yml
- 依赖: TASK-010（build_setup 与 mise 先对齐，再修正发布元数据）
- 风险/回退: YAML 缩进损坏 → `git checkout -- .github/workflows/publish.yml` 恢复

#### TASK-010B: 活跃开发文档与源码注释版本基线修正

- task_id: `TASK-010B`
- phase: P1
- scope:
  - `CONTRIBUTING.md`（7 处：Minecraft/NeoForge/Java/Registrate 旧版本引用）
  - `docs/closure_verification.md`（1 处 MC 1.21.1）
  - `docs/compile_warning_baseline_26.1.2.md`（1 处 MC 1.21.1）
  - `src/main/java/com/modularmc/ten/common/blockentity/CmBlockEntity.java`（1 处过期注释）
- 目标: 消除活跃开发文档与源码注释中所有指向 Minecraft 1.21.1 / NeoForge 26.1.2 错误语义的版本引用，与当前目标基线 `Minecraft 26.1.2 / NeoForge 26.1.2.78 / Java 25` 一致
- **关键约束（审查裁决）**:
  - 仅改版本号与平台名，保留技术描述/API 调用签名/历史变更日志不动
  - CONTRIBUTING.md 分类每处引用：Minecraft 版本 / NeoForge 版本 / Java 版本 / Registrate 引用，分别修正
  - 旧 plans/drafts/evidence/deps/logs/README 兄弟目录排除
  - 核对 `build.gradle` toolchain（JDK 25）、构件 major=69、NeoForge 26.1.2.78 jar 元数据
  - `CmBlockEntity.java` 注释中的过期 MC 版本引用仅更新版本号，不改代码逻辑
- DoD（已全部通过 ✓）:
  - ☑ **CONTRIBUTING.md**：7 处修正 — MC 26.1.2 / NeoForge 26.1.2.78 / Java 25 / DeferredRegister
  - ☑ **docs/closure_verification.md**：`1.21.1` → `26.1.2`
  - ☑ **docs/compile_warning_baseline_26.1.2.md**：`1.21.1` → `26.1.2`
  - ☑ **CmBlockEntity.java**：过期注释版本已更新
  - ☑ **RED 验证**：`rg "1\.21\.1" CONTRIBUTING.md docs/ src/main/java/ --include '*.java' --include '*.md'` → scope 内无残留（仅旧 plans 合法保留）
  - ☑ **compileJava** no-daemon fresh → exit0 30s（不退化）
  - ☑ **diff 审查**：`git diff` 仅版本号/平台名数值变更
- 验收要点（已通过）:
  - [✓] 贡献者文档 CONTRIBUTING.md 再无 `1.21.1` / `Java 21` 描述
  - [✓] `rg "1\.21\.1" CONTRIBUTING.md docs/closure_verification.md docs/compile_warning_baseline_26.1.2.md` → 无输出
  - [✓] `CmBlockEntity.java` 注释版本已更新
  - [✓] `git diff` 仅 scope 内文件且仅版本信息变更
- 输入: CONTRIBUTING.md、docs/closure_verification.md、docs/compile_warning_baseline_26.1.2.md、CmBlockEntity.java
- 输出: 各文件版本引用已修正
- 依赖: TASK-010A（顺序执行链：010 → 010A → 010B → 010C → 010D → 011）
- 风险/回退: 文档文字错误 → `git checkout -- <文件>` 恢复
- **执行记录**:
  - 完成日期: 2026-07-11
  - 审查人: 艾琳（审查通过）
  - Commit: `458c7bb` — `docs: align active version references with Minecraft 26.1.2`（5 文件含 formal plan）
  - Push: `c57c8fb..458c7bb` → 远程分支已同步
  - 验证摘要:
    - 四 scope 旧 `1.21.1` 等模式 → 0 残留 ✅
    - CONTRIBUTING → NeoForge 26.1.2.78 / MC 26.1.2 / Java 25 / DeferredRegister ✅
    - `compileJava` no-daemon fresh → exit0 30s ✅
    - worktree clean ✅
- 输入: CONTRIBUTING.md、docs/closure_verification.md、docs/compile_warning_baseline_26.1.2.md、CmBlockEntity.java
- 输出: 各文件版本引用已修正
- 依赖: TASK-010A（顺序执行链：010 → 010A → 010B → 010C → 010D → 011）
- 风险/回退: 文档文字错误 → `git checkout -- <文件>` 恢复

#### TASK-010C: JAR 命名双重版本修正

- task_id: `TASK-010C`
- phase: P1
- scope:
  - `gradle/scripts/jars.gradle`（优先 —— 修正 `archivesName` 或 version 拼接逻辑）
  - `build.gradle`（必要时核实 `version` / `archivesBaseName` 配置）
- 目标: 消除当前构建产出 `kenergyengineering-26.1.2-26.1.2-4.1.0.jar` 中的重复 `26.1.2`，修正为 `kenergyengineering-26.1.2-4.1.0.jar`（主 jar）。slim/sources jar 命名规则自动继承修正。
- **关键约束（审查裁决）**:
  - 先 RED：`.\gradlew.bat clean :build` 产出 fresh jar，`ls build/libs/` 确认当前重复命名模式
  - 目标：`archivesName = "${mod_id}-${libs.versions.minecraft.get()}"` 或等效，使主 jar 精确为 `${mod_id}-${mc_version}-${mod_version}.jar`
  - 不改 `settings.gradle` 除非验证必须
  - JAR 内容/manifest 不变（`jar { ... }` 配置不调整）
  - slim/sources 前缀自动继承新命名规则
- DoD（已全部通过 ✓）:
  - ☑ **RED 断言**：fresh build → 确认旧重复 `kenergyengineering-26.1.2-26.1.2-4.1.0.jar`
  - ☑ **根因定位**：`jars.gradle` version 拼接逻辑含多余 MC 版本段
  - ☑ **GREEN 修正**：`archivesName` 配置 → 主 jar `kenergyengineering-26.1.2-4.1.0.jar`
  - ☑ **GREEN 验证**：fresh clean build → 主 jar + slim + sources 三文件正确，旧重复 0
  - ☑ **metadata**：jar 内容/manifest 不变；LDLib2/devmod 隔离正常
  - ☑ **diff 审查**：`git diff` 仅 `jars.gradle` 命名规则变更
- 验收要点（已通过）:
  - [✓] `.\gradlew.bat clean :build` → 主 jar `kenergyengineering-26.1.2-4.1.0.jar`（无重复）
  - [✓] slim/sources jar 命名前缀一致且未损坏
  - [✓] jar 内容与修正前一致（仅命名变）
  - [✓] `git diff` 仅 jars.gradle 且仅命名变更
- 输入: `gradle/scripts/jars.gradle`、`build.gradle`
- 输出: 修正后的 jar 命名（fresh build 验证）
- 依赖: TASK-010B（顺序链：010B → 010C）
- 风险/回退: archivesName 变更导致 slim jar 未生成 → 恢复 jars.gradle，改用 `build.gradle` 中 `version` 拼接
- **执行记录**:
  - 完成日期: 2026-07-11
  - 审查人: 艾琳（审查通过）
  - Commit: `7fe63cb` — `fix: remove duplicate Minecraft version from jar names`
  - Push: `458c7bb..7fe63cb` → 远程分支已同步
  - 验证摘要:
    - fresh clean build → exit0 ✅
    - 主 jar + slim + sources 三文件正确 ✅
    - 旧重复 `*-26.1.2-26.1.2-*` → 0 ✅
    - metadata / LDLib2 / devmod 隔离正常 ✅
    - worktree clean ✅
- 输入: `gradle/scripts/jars.gradle`、`build.gradle`
- 输出: 修正后的 jar 命名（fresh build 验证）
- 依赖: TASK-010B（顺序链：010B → 010C）
- 风险/回退: archivesName 变更导致 slim jar 未生成 → 恢复 jars.gradle，改用 `build.gradle` 中 `version` 拼接

#### TASK-010D: 发布目标游戏版本与 glob 对齐

- task_id: `TASK-010D`
- phase: P1
- scope:
  - `.github/workflows/publish.yml`（两处 `MC_VERSION: '1.21.1'` → `'26.1.2'`；`files: '**/build/libs/*.jar'` 的实际 glob 模式确认匹配修正后命名）
  - `.github/workflows/publish-on-release.yml`（版本发布语义的 job name / tag condition / version input — 先读取数据流确定具体字段；仅改版本数值，不改 workflow 触发分支策略/发布目标/secret）
- 目标: 将发布工作流的游戏版本参数从 1.21.1 更新至 26.1.2，确保 TASK-010C 修正后的 jar 命名能命中发布 glob
- **关键约束（审查裁决）**:
  - TASK-010A 已处理 JAVA 版本，TASK-010D 仅处理游戏版本（MC_VERSION）和相关 glob
  - 先读 publish.yml 确定 MC_VERSION 两处具体行 + files/name/version 实际引用模式
  - 先读 publish-on-release.yml 确定版本语义字段（job name、tag condition、version input）
  - 不运行真实发布、不碰 secret、不改 workflow 触发分支
  - DoD 含 PowerShell glob 模拟：基于 TASK-010C fresh artifacts 验证主 jar 恰好 1 个、slim/sources 正确排除
- DoD:
  - ☐ **publish.yml MC_VERSION**：
    - `grep -n "MC_VERSION" .github/workflows/publish.yml` → 确认两处
    - RED：当前值 `'1.21.1'`；GREEN：→ `'26.1.2'`
    - RED 再断言：`rg "MC_VERSION.*1\.21\.1" .github/workflows/publish.yml` → 无输出
  - ☐ **publish.yml glob 对齐**：
    - 基于 TASK-010C fresh `build/libs/` 产物，PowerShell glob 模拟：
      `Get-ChildItem build/libs/*.jar | Where-Object Name -notmatch 'slim|sources'`
    - 确认主 jar 恰好 1 个、slim/sources 不被发布 glob 误匹配
  - ☐ **publish-on-release.yml 版本字段**：
    - `grep -n "1\.21\|1\.21\.1\|26\.1" .github/workflows/publish-on-release.yml` → 确认版本语义字段
    - 仅改版本发布语义字段：job name、tag condition、version input
    - 不改 `on:` 触发分支策略、不改 `secrets:`、不改发布目标
  - ☐ **YAML parse**：`python -c "import yaml; yaml.safe_load(open(...))"` 或人工目视核对
  - ☐ **JAR metadata**：`jar tf build/libs/kenergyengineering-26.1.2-4.1.0.jar META-INF/neoforge.mods.toml` 中 `minecraft_version_range` 确认含 `[26.1.2,27)` 供 mc-publish auto-detect（不作为硬性 blocking，记录 evidence）
  - ☐ **diff 审查**：`git diff .github/workflows/` 仅版本字段与 glob 对齐变更
  - ☐ **提交信息**: `fix: update publish workflows MC_VERSION to 26.1.2 and align artifact glob`
- 验收要点:
  - [ ] `rg "MC_VERSION.*1\.21\.1" .github/workflows/` → 无输出
  - [ ] `rg "1\.21\.1\|1\.21\b" .github/workflows/publish-on-release.yml` → 无版本发布语义残留
  - [ ] PowerShell glob 模拟：主 jar 恰好 1 个匹配，slim/sources 排除
  - [ ] `git diff .github/workflows/` 仅版本/glob 变更
- 输入: `.github/workflows/publish.yml`、`.github/workflows/publish-on-release.yml`；TASK-010C 的 fresh artifacts
- 输出: 修正后的发布工作流文件 + glob 对齐验证
- 依赖: TASK-010C（需要修正后的 jar 命名发布 glob 才能对齐）
- 风险/回退: publish-on-release.yml 版本字段识别不完整 → 记录发现的额外字段，单独 commit；不做完整发布测试

#### TASK-011: libs.versions.toml TODO 清理（交叉验证）

- task_id: `TASK-011`
- phase: P1
- scope: `gradle/libs.versions.toml`
- 目标: 核验 `loader = "4"` 和 `modDevGradle = "2.0.141"` 的 TODO 标记，使用三重证据交叉确认后清理 TODO
- **关键约束（审查裁决）**: 禁止仅凭 `compileJava` 通过就断言推荐版本。必须用以下证据交叉核对:
  1. **loader 版本**: 生成/运行时元数据：`build/generated/sources/modMetadata/META-INF/neoforge.mods.toml` 中 `${loader_version}` 展开值；`runClient` 日志 `ModLauncher` 行显示的 fml loader 版本；NeoForge jar 内 `META-INF/neoforge.mods.toml` 的 `loaderVersion`。三者一致则确认，否则取多数或报 issue。
  2. **modDevGradle**: 官方 NeoForge Gradle plugin portal、GitHub Releases（`https://github.com/neoforged/moddevgradle/releases`）检查 2.0.141 兼容声明；`.\gradlew.bat :tasks` 运行时无 modDevGradle 版本警告；官方示例项目（如有）使用的版本。
- DoD:
  - □ **loader 版本核查（三重证据）**:
    - 证据1: 读取 `build/generated/sources/modMetadata/META-INF/neoforge.mods.toml` 中 `loaderVersion` 值
    - 证据2: `runClient` 日志提取 `ModLauncher`/`fml` 版本号
    - 证据3: 直接解压 NeoForge jar 检查 `META-INF/neoforge.mods.toml` 中 `loaderVersion`
    - 结论: 如 `4` 正确 → 移除 TODO 注释；如不正确 → 更新为三者一致的版本
  - □ **modDevGradle 版本核查**:
    - 检查 `https://github.com/neoforged/moddevgradle/releases` 确认 2.0.141 的 NeoForge 26.x 兼容性
    - 运行 `.\gradlew.bat :tasks` 确认无 modDevGradle 版本警告/弃用通知
    - 结论: 如兼容 → 移除 TODO 注释并记录证据来源 URL；如需更新 → 更新版本并 `compileJava` + `runClient` 通过验证；如证据不足或 2.0.141 兼容但不推荐 → 保留 2.0.141 并移除误导 TODO
  - □ 变更仅含版本号更新（如有）和 TODO 移除/替换为证据注释，不改其他内容
  - □ 将交叉验证结论写入 `docs/deps_version_verification_26.1.2.md`
  - □ 提交信息: `chore: clean up libs.versions.toml TODOs after cross-verification`
- 验收要点:
  - [ ] `rg "TODO" gradle/libs.versions.toml` 无 loader/modDevGradle 相关 TODO 残留
  - [ ] `.\gradlew.bat :compileJava` 通过
  - [ ] `docs/deps_version_verification_26.1.2.md` 存在且包含证据来源 URL 和交叉验证结论
- 参考来源: NeoForge jar 元数据；modDevGradle GitHub Releases (https://github.com/neoforged/moddevgradle/releases)
- 输入: `gradle/libs.versions.toml`、生成元数据、NeoForge jar
- 输出: 清理后的 `gradle/libs.versions.toml` + `docs/deps_version_verification_26.1.2.md`
- 依赖: TASK-010D（顺序执行链：010 → 010A → 010B → 010C → 010D → 011）
- 风险/回退: 版本更新导致编译失败 → 恢复原版本号，保留 TODO 并记录结论

---

### Phase 2 — Configuration 迁移

> `ConfigHolder` 当前为硬编码存根（javadoc 明确 TODO）。需迁移到 NeoForge `ModConfigSpec`，保持旧字段/默认值/范围，`@Mod` 容器 `registerConfig`。

#### TASK-020: ConfigHolder → ModConfigSpec 迁移

- task_id: `TASK-020`
- phase: P2
- scope:
  - `src/main/java/com/modularmc/ten/config/ConfigHolder.java`（重写）
  - `src/main/java/com/modularmc/ten/TEN.java`（`@Mod` 构造器添加 `registerConfig`）
  - 所有引用 `ConfigHolder.INSTANCE.*` 的文件（更新访问路径或保持兼容）

- 目标: 用 NeoForge `ModConfigSpec` 替代硬编码存根，保持旧字段名/默认值/范围。在 `@Mod` 构造器中调用 `ModLoadingContext.registerConfig`。移除 `ConfigHolder.init()` 调用（`ModConfigSpec` 自动注册）。
- DoD:
  - □ `ConfigHolder.java` 重构为使用 `ModConfigSpec.Builder` 定义配置，导出 `ModConfigSpec` 实例
  - □ 保持旧字段名（`machine.energyMultiplier`、`energyUnit.maxEnergy` 等）和默认值一致
  - □ `TEN.java` 构造器调用 `ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, spec, specInstance)`
  - □ `ConfigHolder.init()` 保留空方法或移除，不影响已有调用（`CommonProxy.init()` 和 `TEN.java`）
  - □ 所有引用 `ConfigHolder.INSTANCE.machine.energyMultiplier` 等字段的代码继续可编译（字段名不变）
  - □ `.\gradlew.bat :compileJava` 零错误
  - □ `.\gradlew.bat :runClient` 启动后确认配置可加载、可修改（通过 mods 菜单 config 页面）
  - □ 提交信息: `feat: migrate ConfigHolder to NeoForge ModConfigSpec`
- 验收要点:
  - [ ] 编译通过后，`runClient` 启动日志无 `ConfigHolder` 相关异常
  - [ ] 游戏内 Mods 菜单 → Kenergy Engineering → Config，看到 Machine/EnergyUnit/Client/Farm 四组配置
  - [ ] 修改某项配置值 → 重启后值持久化
  - [ ] `rg "ConfigHolder\.init" src/` 确认已移除或仅为空方法
- 参考来源: NeoForge 26.1.2 ModConfigSpec 文档／源码
- 输入: `ConfigHolder.java`、`TEN.java`、`CommonProxy.java`
- 输出: 重构后的配置系统
- 依赖: P0
- 风险/回退: ModConfigSpec API 不兼容 → 保留存根，记录 issue，不阻塞其他阶段

#### TASK-021: neoforge.mods.toml + resources.gradle configuration 清理

- task_id: `TASK-021`
- phase: P2
- scope:
  - `src/main/templates/META-INF/neoforge.mods.toml`（移除 configuration 依赖节）
  - `gradle/scripts/resources.gradle`（移除 configurationVersion placeholder）
  - `gradle.properties`（如有 `configuration_version` 键）
- 目标: ModConfigSpec 已完成（TASK-020），清理旧的 configuration 引用
- DoD:
  - □ `neoforge.mods.toml` 中 `[[dependencies.${mod_id}]]` 块（`modId = "configuration"`）已移除
  - □ `resources.gradle` 中 `var configurationVersion = "0.0.0"` 及其 `replaceProperties` 引用已移除
  - □ `.\gradlew.bat :processResources` 通过
  - □ `.\gradlew.bat :compileJava` 通过
  - □ 提交信息: `chore: remove stale configuration dependency references`
- 验收要点:
  - [ ] `grep "configuration" src/main/templates/META-INF/neoforge.mods.toml` 无输出
  - [ ] `grep "configurationVersion" gradle/scripts/resources.gradle` 无输出
- 输入: TASK-020 完成
- 输出: 清理后的模板和 gradle 文件
- 依赖: TASK-020（ModConfigSpec 就位后才有条件移除旧引用）
- 风险/回退: 模板语法错误 → 复原文件，保留 configuration 引用但不影响功能

---

### Phase 3 — Transfer 边界恢复

> ⚠️ **关键约束**：`CommonProxy.java` 中 `Capabilities.Item.BLOCK` 与 `Capabilities.Fluid.BLOCK` 注册被注释。`CapabilityAdapters.getItems/getFluids` 虽已实现查询端适配，但缺少**注册端适配**（`IItemHandler → ResourceHandler<ItemResource>` 和 `IFluidHandler → ResourceHandler<FluidResource>` 转换器）。当前只有 Energy 适配完整。
>
> 本阶段只做**边界适配**：让外部能力查询可通过 Capabilities.Item.BLOCK / Capabilities.Fluid.BLOCK 查询到内部旧接口数据。内部旧接口实现（IItemHandler、IFluidHandler）暂不改动。

#### TASK-030: 实现 Item ResourceHandler 注册端适配器

- task_id: `TASK-030`
- phase: P3
- scope:
  - `src/main/java/com/modularmc/ten/api/capability/CapabilityAdapters.java`（新增 `asItemResourceHandler`、`asFluidResourceHandler`）
- 目标: 实现 `IItemHandler → ResourceHandler<ItemResource>` 适配器，使 BE 可注册 `Capabilities.Item.BLOCK`
- DoD:
  - □ `CapabilityAdapters` 新增 `static ResourceHandler<ItemResource> asItemResourceHandler(IItemHandler handler)`:
    - 实现 `getResource()`、`getAmount()`、`getCapacity()` 委托到对应 `IItemHandler` 方法
    - 实现 `insert(ItemResource, TransactionContext)` 委托到 `IItemHandler.insertItem(...)`
    - 实现 `extract(ItemResource, TransactionContext)` 委托到 `IItemHandler.extractItem(...)`
    - 实现 `getContents()` 返回迭代器
    - 注意：`TransactionContext` 传入但旧 IItemHandler 不支持原子事务回滚，`insertItem/extractItem` 直接操作。需在 javadoc 标注"boundary adapter: simulate ignored, operates directly"
  - □ `asItemResourceHandler(null)` 返回 `null`（安全）
  - □ 单元测试/编译验证：适配器类可编译
- 验收要点:
  - [ ] `.\gradlew.bat :compileJava` 零错误
  - [ ] `rg "ResourceHandler.*ItemResource" src/main/java/com/modularmc/ten/api/capability/CapabilityAdapters.java` 匹配新方法
- 输入: 当前 `CapabilityAdapters.java`
- 输出: 增强的 `CapabilityAdapters.java`
- 依赖: P0
- 风险/回退: ResourceHandler API 理解偏差 → 先提交草稿版本供审查

#### TASK-031: 实现 Fluid ResourceHandler 注册端适配器

- task_id: `TASK-031`
- phase: P3
- scope: 同 TASK-030（`CapabilityAdapters.java`）
- 目标: 实现 `IFluidHandler → ResourceHandler<FluidResource>` 适配器
- DoD:
  - □ `CapabilityAdapters` 新增 `static ResourceHandler<FluidResource> asFluidResourceHandler(IFluidHandler handler)`:
    - 实现 `getResource()`、`getAmount()`、`getCapacity()`
    - 实现 `insert(FluidResource, TransactionContext)` 委托到 `IFluidHandler.fill(...)`
    - 实现 `extract(FluidResource, TransactionContext)` 委托到 `IFluidHandler.drain(...)`
    - 实现 `getContents()` 返回迭代器
    - javadoc 标注原子事务限制说明
  - □ `asFluidResourceHandler(null)` 返回 `null`
  - □ 编译验证通过
- 验收要点:
  - [ ] `.\gradlew.bat :compileJava` 零错误
- 输入: 同 TASK-030
- 输出: 增强的 `CapabilityAdapters.java`
- 依赖: TASK-030（可并行，但推荐顺序执行以独立验证）
- 风险/回退: 同 TASK-030

#### TASK-031A: 盘点/实现 BE item/fluid accessor 委托

- task_id: `TASK-031A`
- phase: P3
- scope:
  - `src/main/java/com/modularmc/ten/common/blockentity/`（所有 BE 类）
  - `src/main/java/com/modularmc/ten/api/blockentity/CmMachineBlockEntity.java`（基类）
  - `src/main/java/com/modularmc/ten/common/blockentity/machine/`（各机器 BE）
  - `src/main/java/com/modularmc/ten/common/blockentity/PipeBlockEntity.java`
  - `src/main/java/com/modularmc/ten/common/blockentity/CableBlockEntity.java`
- 目标: 按源码勘探确定真正提供存储的 BE，为每种实现 `getIItemHandler(Direction)` / `getIFluidHandler(Direction)` 委托方法；禁止给无存储 BE 返回伪空 handler；能力 provider 对不适用侧返回 `null` 安全降级
- DoD:
  - □ **勘探清单**：列出所有 `BaseMachineBlock` 子类对应的 BE，分类：
    - 有物品存储（`inventory` 字段 / `MachineItemHandler`）的 BE → 实现 `getIItemHandler(Direction)` 返回真实 handler
    - 有流体存储（`tank` / `fluidTank` 字段）的 BE → 实现 `getIFluidHandler(Direction)` 返回真实 handler
    - 仅有能量无 item/fluid 的 BE（如 CableBlockEntity）→ `getIItemHandler`/`getIFluidHandler` 返回 `null`
    - 管道（PipeBlockEntity）→ `getIItemHandler` 返回 `filterInventory` 或 pipe transfer handler
  - □ 对无存储的 BE，accessor 返回 `null`（不创建空 IItemHandler 伪实现）
  - □ 所有新增方法标注 `@Nullable`
  - □ `.\gradlew.bat :compileJava` 零错误
  - □ 提交信息: `feat: add item/fluid accessor methods to BE classes for capability registration`
- 验收要点:
  - [ ] `.\gradlew.bat :compileJava` 零错误
  - [ ] `rg "getIItemHandler\|getIFluidHandler" src/main/java/com/modularmc/ten/common/blockentity/` 覆盖所有有存储的 BE
  - [ ] 无存储 BE 返回 `null` 的 accessor 已标注 `@Nullable`
- 输入: 各 BE 源码
- 输出: 新增 accessor 方法的 BE 文件
- 依赖: TASK-030, TASK-031（适配器可用，BE accessor 返回旧接口类型）
- 风险/回退: 勘探遗漏 → 编译时 type mismatch 发现后补；无存储 BE 返回 null 不阻塞注册

#### TASK-032: 注册 Capabilities.Item.BLOCK / Capabilities.Fluid.BLOCK

- task_id: `TASK-032`
- phase: P3
- scope:
  - `CommonProxy.java`（`registerCapabilities` 方法）
- 目标: 恢复 `CommonProxy` 中 Item/Fluid 能力注册，使外部 transfer 机制可查询。**不包含 BE accessor 实现**（已由 TASK-031A 完成）
- DoD:
  - □ `CommonProxy.registerCapabilities` 中 `event.registerBlock(Capabilities.Item.BLOCK, ...)` 已插入，为每个 BaseMachineBlock 注册 `CapabilityAdapters.asItemResourceHandler(blockEntity.getIItemHandler(side))`
  - □ 同样注册 `Capabilities.Fluid.BLOCK` 使用 `asFluidResourceHandler(blockEntity.getIFluidHandler(side))`
  - □ 对返回 `null` 的 accessor（无存储 BE），capability provider 安全返回 `null`（lambda 中 `if (handler == null) return null`）
  - □ 移除 `// TODO: Re-enable Item/Fluid capability registration` 注释
  - □ `.\gradlew.bat :compileJava` 零错误
  - □ 提交信息: `feat: restore item/fluid capability registration via ResourceHandler adapters`
- 验收要点:
  - [ ] `.\gradlew.bat :compileJava` 零错误
  - [ ] `rg "Capabilities\.Item\.BLOCK\|Capabilities\.Fluid\.BLOCK" src/main/java/com/modularmc/ten/common/CommonProxy.java` 显示已注册（非注释）
- 输入: TASK-030/031 完成的 `CapabilityAdapters.java`；TASK-031A 完成的 BE accessor；`CommonProxy.java`
- 输出: 更新后的 `CommonProxy.java`
- 依赖: TASK-030, TASK-031, TASK-031A
- 风险/回退: 单个 BE accessor 未实现 → 其 capability 返回 null（安全降级），不影响其他 BE 的能力注册

#### TASK-033: 边界适配验证 — 编译 + level.getCapability 验证

- task_id: `TASK-033`
- phase: P3
- scope:
  - `src/main/java/com/modularmc/ten/api/capability/CapabilityAdapters.java`
  - `src/test/java/`（新增边界适配器单元测试，若 test sourceSet 已恢复）
- **关键约束（审查裁决）**: 不使用 `/neoforge capabilities` 命令（不存在）或 `/data get block`（不能证明 capability 存在）。采用自动适配器测试/GameTest 中 `level.getCapability(Capabilities.Item/Fluid.BLOCK, pos, side)`。Jade 仅人工辅助，不作为唯一证据。
- 目标: 验证 item/fluid 边界适配器正确工作——`level.getCapability(Capabilities.Item.BLOCK, pos, side)` 可查询到已注册内容
- DoD:
  - □ `.\gradlew.bat :compileJava` 零错误
  - □ **单元验证**（如 test sourceSet 已可用）: 新增测试方法 `testCapabilityRegistration`，在 GameTest 环境中对已注册的机器方块调用 `level.getCapability(Capabilities.Item.BLOCK, pos, side)` 和 `level.getCapability(Capabilities.Fluid.BLOCK, pos, side)`，断言返回非 null
  - □ **人工验证辅助**: `runClient` 启动后，放入一台机器方块（如 Smelter），用 Jade 指向确认不报 capability 错误；日志中无 `Capabilities.Item.BLOCK` 相关 `NullPointerException`
  - □ 提交（含 TASK-030~032 + TASK-031A）: 按 `git add -p` 分块提交确保每项独立
- 验收要点:
  - [ ] `.\gradlew.bat :compileJava` 零错误
  - [ ] `.\gradlew.bat :runClient` 启动日志 `rg "kenergyengineering.*Capabilities"` 无 ERROR/NPE
  - [ ] **最终门禁**: P9 TASK-091 的 GameTest 必须包含 `level.getCapability(Capabilities.Item.BLOCK)` 和 `Capabilities.Fluid.BLOCK` 的通过用例
- 输入: TASK-030, TASK-031, TASK-031A, TASK-032
- 输出: 验证报告（写入 `docs/phase3_boundary_validation.md`）
- 依赖: TASK-032
- 风险/回退: test sourceSet 尚未恢复 → 仅做人工验证 + compile 验证；最终门禁由 P9 TASK-090A + TASK-091 确保

---

### Phase 4 — Transfer 内部迁移

> ⚠️ **第二阶段**：在边界适配就绪后，将**内部**Item/Fluid/Energy 存储从旧 `IItemHandler`/`IFluidHandler`/`IEnergyStorage` 接口迁移到 NeoForge 26.1.2 的 `ItemStacksResourceHandler`/`FluidStacksResourceHandler`/`EnergyHandler` 和 `Transaction` 原子事务。按 storage → machine → network → recipes/UI/util 顺序。

#### TASK-040: 内部能量存储迁移 IEnergyStorage → EnergyHandler

- task_id: `TASK-040`
- phase: P4
- scope:
  - `src/main/java/com/modularmc/ten/api/capability/`（能量存储基类）
  - 所有实现 `IEnergyStorage` 的内部类（`CmMachineBlockEntity` 能量槽、`CableBlockEntity` 能量缓存、`EnergyUnitItem` 等）
  - `CapabilityAdapters.java`（更新 `asEnergyHandler` 或移除封装）
- 目标: 将内部能量存储从 `IEnergyStorage` 迁移到 NeoForge `EnergyHandler`，使用 `Transaction` API
- DoD:
  - □ 核心存储类（`EnergyStorage` 包装/子类）扩展 `EnergyHandler` 而非实现 `IEnergyStorage`
  - □ `insert/extract` 方法签名改为使用 `TransactionContext` 参数
  - □ `IEnergyStorage.of(handler)` 反向兼容桥接（供仍用旧接口的调用方使用）可保留
  - □ 所有 `CapabilityAdapters.getEnergy()` 调用者确认无编译错误
  - □ `.\gradlew.bat :compileJava` 零错误
  - □ 提交信息: `migrate: internal energy storage to EnergyHandler + Transaction`
- 验收要点:
  - [ ] `rg "implements IEnergyStorage\|extends IEnergyStorage" src/main/java/` 无内部类实现（仅保留 `EnergyUnitItem` 中的匿名类作为遗留适配层，加 `// LEGACY-ADAPTER` 注释）
  - [ ] `.\gradlew.bat :compileJava` 零错误
- 输入: 当前能量存储实现
- 输出: 迁移后的能量存储代码
- 依赖: TASK-033（边界适配已验证）
- 风险/回退: Transaction API 使用不当 → 保留旧接口 + `@Deprecated` 兼容桥接，不阻塞

#### TASK-041: 内部物品存储迁移 IItemHandler → ItemStacksResourceHandler

- task_id: `TASK-041`
- phase: P4
- scope:
  - 所有 `MachineItemHandler` / `ItemStackHandler` 引用内部类（`PipeBlockEntity.filterInventory`、各 BE `inventory` 字段）
  - `IItemHandler` 接口使用处（`TransferNetworks.moveItems` 等方法）
  - `Capabilities.Item.BLOCK` 注册端（更新适配器使用新接口）
- 目标: 将内部物品存储基础设施从 `IItemHandler`/`ItemStackHandler` 迁移到 `ItemStacksResourceHandler`/`ResourceHandler<ItemResource>`
- DoD:
  - □ `MachineItemHandler` 重构为基于 `ItemStacksResourceHandler` 实现
  - □ 各 BE 的 `getItemHandler(Direction)` 返回 `ResourceHandler<ItemResource>` 而非 `IItemHandler`
  - □ `TransferNetworks.moveItems` 等网络传输方法更新为新 API
  - □ 仍需要 `IItemHandler` 的地方保留 `IItemHandler.of(resourceHandler)` 桥接
  - □ `.\gradlew.bat :compileJava` 零错误
  - □ 提交信息: `migrate: internal item storage to ItemStacksResourceHandler`
- 验收要点:
  - [ ] `rg "new ItemStackHandler\|extends ItemStackHandler" src/main/java/` 无创建旧实例的代码（桥接层除外）
  - [ ] `.\gradlew.bat :compileJava` 零错误
- 输入: 当前物品存储实现
- 输出: 迁移后的物品存储代码
- 依赖: TASK-040（推荐顺序：先能量后物品）
- 风险/回退: ItemStacksResourceHandler API 不兼容 → 保留旧机器物品存储 + `// LEGACY-ADAPTER`，标记后续

#### TASK-042: 内部流体存储迁移 IFluidHandler → FluidStacksResourceHandler

- task_id: `TASK-042`
- phase: P4
- scope:
  - 各 BE `tank`/`fluidTank`/`IFluidHandler` 实现
  - `FluidTank` 基类引用
  - `Capabilities.Fluid.BLOCK` 注册端
- 目标: 将内部流体存储从 `IFluidHandler`/`FluidTank` 迁移到 `FluidStacksResourceHandler`/`ResourceHandler<FluidResource>`
- DoD:
  - □ 流体 tank 类重构为基于 `FluidStacksResourceHandler` 实现
  - □ 各 BE 的 `getFluidHandler(Direction)` 返回 `ResourceHandler<FluidResource>`
  - □ 仍需 `IFluidHandler` 处保留 `IFluidHandler.of(resourceHandler)` 桥接
  - □ `.\gradlew.bat :compileJava` 零错误
  - □ 提交信息: `migrate: internal fluid storage to FluidStacksResourceHandler`
- 验收要点:
  - [ ] `rg "IFluidHandler\|FluidTank" src/main/java/` 无新实现（桥接除外）
  - [ ] `.\gradlew.bat :compileJava` 零错误
- 输入: 当前流体存储实现
- 输出: 迁移后的流体存储代码
- 依赖: TASK-041（推荐顺序：物品先于流体）
- 风险/回退: 同 TASK-041

#### TASK-043: Transaction 原子事务迁移

- task_id: `TASK-043`
- phase: P4
- scope:
  - `TransferNetworks.java`（`moveItems`、`moveFluids` 等方法）
  - `CapabilityAdapters.java` 中适配器的 Transaction 处理
  - 机器 tick 中涉及 transfer 的逻辑
- 目标: 在传输链路上游使用 `Transaction` 实现原子操作，替代旧 `simulate` 模式
- DoD:
  - □ `TransferNetworks.moveItems` 使用 `Transaction.open(level)` / `transaction.commit()` 模式，失败则 `transaction.abort()`
  - □ 至少覆盖能量/物品/流体的主传输路径
  - □ 已实现的边界适配器 javadoc 更新，注明原子事务支持状态
  - □ `.\gradlew.bat :compileJava` 零错误
  - □ 提交信息: `migrate: adopt Transaction for atomic transfer operations`
- 验收要点:
  - [ ] `grep "Transaction\." src/main/java/com/modularmc/ten/common/blockentity/TransferNetworks.java` 输出行使用新 API
  - [ ] `.\gradlew.bat :compileJava` 零错误
- 输入: TASK-040~042 完成
- 输出: 使用 Transaction 的传输网络代码
- 依赖: TASK-040, TASK-041, TASK-042
- 风险/回退: Transaction 使用不当造成死锁 → 回退到旧 simulate 模式，标记 `// TRANSACTION-TODO`

#### TASK-044: Recipe/UI/util 传输引用更新

- task_id: `TASK-044`
- phase: P4
- scope: 非核心存储但引用 item/fluid handler 的代码（recipe 匹配、JEI 集成、GUI slot 绑定等）
- 目标: 更新所有间接引用以匹配新 API 签名
- DoD:
  - □ recipe 序列化/反序列化中 handler 引用更新
  - □ JEI 集成中 `IItemHandler`/`IFluidHandler` 引用更新（如适用）
  - □ GUI slot 绑定代码更新（如适用）
  - □ `.\gradlew.bat :compileJava` 零错误
  - □ 提交信息: `migrate: update recipe/UI/util transfer references`
- 验收要点:
  - [ ] `.\gradlew.bat :compileJava` 零错误
- 输入: TASK-043
- 输出: 更新后的引用代码
- 依赖: TASK-043（或至少 TASK-040~042）
- 风险/回退: 单个引用遗漏 → 让编译器定位，逐文件修复

#### TASK-045: 移除 -Xlint:-removal + 警告门禁

- task_id: `TASK-045`
- phase: P4
- scope:
  - `build.gradle`（`options.compilerArgs << "-Xlint:-removal"`）
  - 全项目
- 目标: 移除 suppression 配置，建立零 `removal` 警告门禁
- DoD:
  - □ `build.gradle` 中的 `-Xlint:-removal` 已移除
  - □ `.\gradlew.bat :compileJava` 零 `removal` 警告（可在 `-Xlint:removal` 开启状态下验证）
  - □ 运行 `.\gradlew.bat :compileJava *> compile_warn_classified.log; rg "removal|deprecation" compile_warn_classified.log` 分类统计
  - □ 与 P0 TASK-002 基线对比：removal 警告数量降至 0
  - □ 提交信息: `chore: remove -Xlint:-removal suppression, zero removal warnings`
- 验收要点:
  - [ ] `grep "Xlint.*removal" build.gradle` 无输出
  - [ ] `.\gradlew.bat :compileJava -Xlint:removal *> compile_xlint.log; rg "removal" compile_xlint.log | rg "kenergyengineering"` 无输出
- 输入: P0 TASK-002 基线；TASK-040~044 完成
- 输出: 移除 suppression 的 `build.gradle`
- 依赖: TASK-044（所有旧 API 引用已迁移）
- 风险/回退: 仍有 removal 警告 → 记录剩余项并选择性保留 suppression（需加具体类路径限制）

---

### Phase 5 — 持久化恢复

> `PipeBlockEntity` 过滤槽 NBT 持久化被注释（`readTileData`/`writeTileData` 中的 TODO）。需恢复 `ValueInput`/`ValueOutput` 序列化路径。

#### TASK-050: PipeBlockEntity 过滤器 NBT 持久化恢复

- task_id: `TASK-050`
- phase: P5
- scope:
  - `src/main/java/com/modularmc/ten/common/blockentity/PipeBlockEntity.java`（`readTileData`/`writeTileData`/`filterInventory`）
- 目标: 恢复管道过滤器物品槽的 NBT 持久化，确保存档保存/重载后过滤配置不丢失
- DoD:
  - □ `readTileData(ValueInput input)` 中反序列化 `filterInventory` 的 9 个槽位内容
  - □ `writeTileData(ValueOutput output)` 中序列化 `filterInventory` 的 9 个槽位内容
  - □ 使用 `ValueInput`/`ValueOutput` 的 NBT 读写 API（而非直接 `CompoundTag`），与 LDLib2 SyncData 体系一致
  - □ 移除 `// TODO: Re-enable filter inventory NBT persistence` 注释
  - □ 测试步骤:
    1. 放入白/黑管，在 GUI 中设置过滤物品
    2. 保存游戏并退出
    3. 重新进入 → 打开管道 GUI → 过滤物品仍存在
    4. `nbt` 命令或存档文件检查确认 NBT 已持久化
  - □ `.\gradlew.bat :compileJava` 零错误
  - □ 提交信息: `fix: restore pipe filter NBT persistence via ValueIO`
- 验收要点:
  - [ ] `runClient` 中设置过滤 → 存档退出 → 重进 → 过滤槽内容恢复
  - [ ] `rg "TODO.*filter inventory NBT" src/main/java/com/modularmc/ten/common/blockentity/PipeBlockEntity.java` 无输出
- 输入: 当前 `PipeBlockEntity.java`
- 输出: 恢复持久化的 `PipeBlockEntity.java`
- 依赖: P4（ValueInput/ValueOutput 体系在 TASK-040~042 中已确认稳定）
- 风险/回退: ValueIO API 不支持槽序列化 → 回退到标准 `CompoundTag` 手动 NBT 读写，标注 `// HACK: fallback NBT`

---

### Phase 6 — 用户功能恢复

> 四项用户可感知的功能欠账：Solar 昼夜检查、Creative 材料变体、Curios 集成、EMI 关闭。

#### TASK-060: SolarBlockEntity day-only 检查恢复

- task_id: `TASK-060`
- phase: P6
- scope:
  - `src/main/java/com/modularmc/ten/common/blockentity/machine/SolarBlockEntity.java`（`matchFuel`）
- 目标: 恢复太阳能引擎仅在白天和有天光的维度工作的行为
- DoD:
  - □ `matchFuel` 中加入 `level.isDay() && level.dimensionType().hasSkyLight()` 检查
  - □ 移除 `// TODO: 26.1.2 - Re-add day-only check` 注释
  - □ 测试:
    - 白天地表 → matchFuel 返回 600（正常工作）
    - 夜晚地表 → matchFuel 返回 0（不工作）
    - 下界/末地（无天光）→ 始终返回 0
    - 雨天 → matchFuel 仍返回 600（仅旧代码屏蔽雨天，不改）
  - □ `.\gradlew.bat :compileJava` 零错误
  - □ 提交信息: `fix: restore day-only check in solar generator`
- 验收要点:
  - [ ] 放置太阳能引擎：白天产生 RF/tick，夜晚不产生
  - [ ] 在下界放置：始终不产生
  - [ ] `rg "TODO.*day-only" src/main/java/` 无输出
- 输入: `SolarBlockEntity.java`
- 输出: 修复后的 `SolarBlockEntity.java`
- 依赖: P0（独立，可并行于其他阶段）
- 风险/回退: `level.isDay()` 在 26.1.2 上行为异常 → 使用 `level.getDayTime() % 24000L < 12000L` 回退

#### TASK-061: TENCreativeModeTabs 材料变体加入

- task_id: `TASK-061`
- phase: P6
- scope:
  - `src/main/java/com/modularmc/ten/common/data/TENCreativeModeTabs.java`（`ITEM_TAB.displayItems`）
- 目标: 将 `registerVariants()` 动态注册的材料变体（dust/ingot/nugget/plate/gear/rod/wire）加入物品创造标签页
- DoD:
  - □ 在 `ITEM_TAB.displayItems` 的 lambda 中遍历注册的所有材料变体 `DeferredHolder` 集合并 `output.accept()` 每个
  - □ 变体集合来自 `TENItems` 中已知命名模式（如 `DUST_TIN.get()`、`INGOT_TIN.get()` 等），或通过 `Registration.ITEMS.getEntries()` 过滤
  - □ 无重复添加（使用 `HashSet` 或条件判断）
  - □ 移除 `// TODO(26.1.2): Material variants...` 注释
  - □ `.\gradlew.bat :compileJava` 零错误
  - □ `.\gradlew.bat :runClientData` 生成对应语言文件条目
  - □ 提交信息: `feat: add material variants to creative tab`
- 验收要点:
  - [ ] 创造模式 → ITEM 标签页 → 目视确认 dust/ingot/nugget/plate/gear/rod/wire 条目存在
  - [ ] 所有变体无 duplicate ID 警告
  - [ ] `rg "TODO.*Material variants" src/main/java/` 无输出
- 输入: `TENCreativeModeTabs.java`、`TENItems.java`（材料注册）
- 输出: 更新后的 `TENCreativeModeTabs.java`
- 依赖: P0（独立）
- 风险/回退: 材料变体集合获取方式不明确 → `/give` 命令可访问，创造标签添加不阻塞最终验证

#### TASK-062: Curios API 集成恢复（类加载安全设计）

- task_id: `TASK-062`
- phase: P6
- scope:
  - `gradle/forge.versions.toml`（添加 Curios 坐标）
  - `dependencies.gradle`（恢复 `compileOnly` + `clientLocalRuntime` 配置）
  - `src/main/java/com/modularmc/ten/common/item/CuriosIntegration.java`（**新文件** — Curios API 调用隔离类）
  - `src/main/java/com/modularmc/ten/common/item/EnergyUnitHandler.java`（通过 `CuriosIntegration` 委托，不直接引用任何 Curios 类型）
  - `src/main/templates/META-INF/neoforge.mods.toml`（如有必要添加 Curios optional 依赖声明）
- **关键约束（审查裁决）**: 不得依赖 `@Optional` 注解做类加载安全。采用可执行设计：
  1. **`CuriosIntegration` 独立类**：所有 Curios API 引用（`CuriosApi`、`ICurio` 等）仅出现在此文件中。
  2. **`ModList.get().isLoaded("curios")` 门控**：`EnergyUnitHandler` 在调用 `CuriosIntegration` 前检查。
  3. **类型隔离**：`EnergyUnitHandler` 的公共方法签名、字段、静态初始化中不得出现任何 Curios 类型。
  4. **无 Curios 环境必须通过**：`compileOnly` 配置下 `runClient`/`runServer` 启动通过。
- 目标: 恢复能量单元对 Curios API 饰品格子的充电支持，使用官方 Modrinth 构件，确保类加载安全
- DoD:
  - □ 确认 Curios 15.0.0-beta.2+26.1.2 构件可用（Modrinth version 68gxflop），添加至 `forge.versions.toml`
  - □ `dependencies.gradle` 恢复 `compileOnly("maven.modrinth:curios:15.0.0-beta.2+26.1.2")` + `clientLocalRuntime("maven.modrinth:curios:15.0.0-beta.2+26.1.2")`（optional）
  - □ 创建 `CuriosIntegration.java`:
    - 仅在此文件中导入 Curios API 类型
    - 提供 `static void collectCurios(Player player, List<ItemStack> targets)` 方法
    - 内部使用 `CuriosApi.getCuriosInventory(player)` 遍历饰品槽
  - □ `EnergyUnitHandler.java` 修改:
    - 移除旧注释 `// TODO: Re-enable Curios integration` 和 `// collectCurios(player, targets);`
    - 在 `getChargeTargets` 方法末尾插入:
      ```
      if (net.neoforged.fml.ModList.get().isLoaded("curios")) {
          CuriosIntegration.collectCurios(player, targets);
      }
      ```
    - 无 Curios 类型泄漏到 `EnergyUnitHandler` 的方法签名、字段或静态初始化
  - □ `compileOnly` 配置下 `.\gradlew.bat :compileJava` 零错误
  - □ `clientLocalRuntime` 未配置时 `.\gradlew.bat :runClient` 启动无 Curios 相关 `ClassNotFoundException`/`NoClassDefFoundError`
  - □ `clientLocalRuntime` 配置后启动，将能量单元放入 Curios 饰品槽 → 能量随时间自动恢复
  - □ mods 元数据：如有必要在 `neoforge.mods.toml` 添加 `[[dependencies.${mod_id}]]` 块 `modId="curios" type="optional"`
  - □ 提交信息: `feat: restore Curios integration for energy unit charging (class-loading safe)`
- 验收要点:
  - [ ] `runClient` 无 Curios 环境 → mod 正常加载，无 `ClassNotFoundException`
  - [ ] `runClient` 有 Curios 环境 → Curios 模组已加载，饰品槽能量单元可自动充电
  - [ ] `rg "import.*curios" src/main/java/com/modularmc/ten/common/item/EnergyUnitHandler.java` 无输出（import 隔离）
  - [ ] `rg "import.*curios" src/main/java/com/modularmc/ten/common/item/CuriosIntegration.java` 有输出（隔离正确）
  - [ ] `rg "TODO.*Curios" src/main/java/` 无输出
- 参考来源: Modrinth Curios 15.0.0-beta.2+26.1.2 (version 68gxflop)
- 输入: `EnergyUnitHandler.java`、`forge.versions.toml`、`dependencies.gradle`
- 输出: `CuriosIntegration.java`（新文件） + 修改后的 `EnergyUnitHandler.java`
- 依赖: P0（独立，依赖外部构件）
- 风险/回退: Curios 26.1.2 构件与当前依赖不兼容 → 暂缓本 TASK，`ModList.get().isLoaded("curios")` 天然跳过不崩溃；记录 issue，不阻塞其他阶段

#### TASK-063: EMI 关闭证据化处理

- task_id: `TASK-063`
- phase: P6
- scope:
  - `src/main/java/com/modularmc/ten/integration/emi/TENEmiPlugin.java`
  - `src/main/java/com/modularmc/ten/integration/emi/TENEmiRecipe.java`
- 目标: EMI 官方最高仅 1.21.1 无 26.1.2 构件；非官方 fork 不采用。将误导空存根替换为明确关闭说明
- DoD:
  - □ `TENEmiPlugin.java` 更新 javadoc：注明 "EMI has no official 26.1.2 NeoForge release. Not re-enabled. Use JEI as the only recipe viewer."
  - □ `TENEmiRecipe.java` 同
  - □ 移除 `TODO(26.1.2): Re-enable when EMI dependency is available` → 替换为关闭说明
  - □ 在 `docs/migration/26.1.2/` 下创建 `emi_closure_note.md`，记录：
    - EMI 官方 Modrinth 最高版本（1.21.1）
    - 检查日期
    - 替代方案（JEI）
    - 如 future EMI 发布 26.1.2 版本：在有 CI 验证的 PR 中重新评估
  - □ 提交信息: `docs: close EMI integration with evidence of no 26.1.2 support`
- 验收要点:
  - [ ] `grep "TODO.*Re-enable when EMI" src/main/java/` 无输出
  - [ ] `docs/migration/26.1.2/emi_closure_note.md` 存在且包含检查证据
- 输入: EMI 存根文件；Modrinth API 检查结果
- 输出: 关闭注释 + 证据文档
- 依赖: P0（独立）
- 风险/回退: 将来 EMI 发布 26.1.2 时需重新开放 → 已记录在证据文档中

---

### Phase 7 — FluidModel 修复

#### TASK-070: FluidModel 10 WARN 排查与修复

- task_id: `TASK-070`
- phase: P7
- scope:
  - `src/main/java/com/modularmc/ten/data/TENModelProvider.java`（`LIQUID_NAMES` 液体块模型生成）
  - `src/main/resources/assets/kenergyengineering/models/block/`（液体块模型文件）
  - 运行时 client 日志（FluidModel WARN）
- 目标: 排查 5 流体 × still/flowing = ~10 WARN 的根源，确认 26.1.2 正确模型契约，修复至日志零 TEN FluidModel WARN
- DoD:
  - □ 收集当前 runClient 日志中所有 `kenergyengineering` 相关的 `FluidModel` WARN，精确计数（8/10 差异）
  - □ 确认 26.1.2 `LiquidBlock` 的正确模型要求：对比 vanilla 液体模型（water/lava）的 blockstate + model JSON 格式
  - □ 检查 `LIQUID_NAMES` 生成的模型（当前为 particle-only JSON）是否符合 26.1.2 契约
  - □ 修复：为每个流体生成正确的 still/flowing model + blockstate JSON（如需要 separate still/flowing 变体）
  - □ `.\gradlew.bat :runClientData` 重新生成
  - □ `.\gradlew.bat :runClient` 启动 → 日志搜索 `kenergyengineering.*FluidModel` 无 WARN
  - □ 流体方块在游戏中放置后纹理正常（非紫黑）
  - □ 提交信息: `fix: resolve FluidModel warnings for all 5 fluids`
- 验收要点:
  - [ ] `runClient` 日志 `rg "FluidModel.*kenergyengineering"` 无输出
  - [ ] 游戏中放置每种流体（桶装倾倒）→ 流体纹理正确渲染
- 输入: `TENModelProvider.java`、`LIQUID_NAMES` 液体块注册
- 输出: 修复后的模型生成代码
- 依赖: P0（独立探索任务）
- 风险/回退: 26.1.2 fluid model 契约不兼容 → 保留当前 particle-only 模型，记录 evidence 不阻塞；若为零 blocking 则延后

---

### Phase 8 — 存根清理 & TODOs

> **依赖最小化原则**：每个存根文件只要求其自身可独立编译/删除的前置条件，不捆绑无关依赖。TemplateMixinConfig 和 KeyboardHandlerMixin 独立于 Transfer/Config/Solar/FluidModel 等阶段。

#### TASK-080A: 废弃迁移存根清理（TENModels / TENDataGen / TENRegistrate）

- task_id: `TASK-080A`
- phase: P8
- scope:
  - `src/main/java/com/modularmc/ten/common/data/TENModels.java`（空存根）
  - `src/main/java/com/modularmc/ten/data/TENDataGen.java`（空存根）
  - `src/main/java/com/modularmc/ten/api/registry/registrate/TENRegistrate.java`（废弃存根）
- 目标: 按规则 "有引用则迁移，无引用则删除，仍合理则改为有证据说明" 处理每个存根
- DoD:
  - □ `TENModels.java`:
    - `rg "TENModels" src/main/java/ --include '*.java'` 检查引用
    - 无引用 → 删除文件；有引用且仍为 TODO → 保留但更新 javadoc 加证据说明
  - □ `TENDataGen.java`:
    - `rg "TENDataGen" src/main/java/ --include '*.java'` 检查引用
    - `CommonProxy.init()` 中调用 `TENDataGen.init()` → 保留空方法，添加 "Decommissioned — kept for ABI" 注释
  - □ `TENRegistrate.java`:
    - `rg "TENRegistrate" src/main/java/ --include '*.java'` 检查引用
    - 无引用（已迁移至 `Registration`）→ 删除文件
    - 有引用 → 保留 `@Deprecated(forRemoval=true)` 并更新 javadoc
  - □ `.\gradlew.bat :compileJava` 零错误
  - □ 提交信息: `chore: clean up expired migration stubs (TENModels/TENDataGen/TENRegistrate)`
- 验收要点:
  - [ ] 各文件按检查结果处理，编译通过
  - [ ] `.\gradlew.bat :compileJava` 零错误
- 输入: 各存根文件；`rg` 引用检查结果
- 输出: 清理后的源码
- 依赖: **仅 P0**（独立于 Transfer/Config/Solar 等功能阶段）。这些存根不提供运行时功能，删除或保留不影响编译。
- 风险/回退: 删除后编译失败 → `git checkout --` 恢复单个文件

#### TASK-080B: 无引用 Mixin 存根清理（TemplateMixinConfig / KeyboardHandlerMixin）

- task_id: `TASK-080B`
- phase: P8
- scope:
  - `src/main/java/com/modularmc/ten/core/mixin/TemplateMixinConfig.java`（空实现模板）
  - `src/main/java/com/modularmc/ten/core/mixin/dev/client/KeyboardHandlerMixin.java`（空 mixin）
  - `src/main/resources/kenergyengineering.mixins.json`（检查引用）
- 目标: 检查两个空类是否有实际引用，无引用则删除
- DoD:
  - □ `TemplateMixinConfig.java`:
    - `rg "TemplateMixinConfig" src/ --include '*.java' --include '*.json'` 检查引用
    - 无引用（非 mixin plugin 配置）→ 删除文件
    - 有引用 → 保留并更新 javadoc
  - □ `KeyboardHandlerMixin.java`:
    - `rg "KeyboardHandlerMixin" src/main/resources/kenergyengineering.mixins.json` 检查 mixin 配置引用
    - `rg "KeyboardHandlerMixin" src/ --include '*.java'` 检查源码引用
    - 无引用 → 删除文件；有引用 → 保留（可能用于调试工具）
  - □ `.\gradlew.bat :compileJava` 零错误
  - □ 提交信息: `chore: clean up unused mixin stubs (TemplateMixinConfig/KeyboardHandlerMixin)`
- 验收要点:
  - [ ] 无引用的文件已删除，编译通过
  - [ ] `.\gradlew.bat :compileJava` 零错误
- 输入: 存根文件；mixin JSON 配置
- 输出: 清理后的源码
- 依赖: **仅 P0**。这两个文件与 Transfer/Config/Solar/FluidModel 等功能阶段完全无关。
- 风险/回退: 删除后编译失败 → `git checkout --` 恢复

#### TASK-081: DataGenerators.addProvider 临时注释审查

- task_id: `TASK-081`
- phase: P8
- scope:
  - `src/main/java/com/modularmc/ten/data/DataGenerators.java`
- 目标: 审查 `// TODO: 26.1.2 - Convert to new datagen API. event.includeClient()/includeServer() removed.` 注释，确认当前 `addProvider(true, ...)` 使用正确
- DoD:
  - □ 确认 `GatherDataEvent.Client` 的 `generator.addProvider(boolean, DataProvider)` 签名 26.1.2 仍支持 boolean 参数（查阅 NeoForge 源码或运行 `runClientData` 验证）
  - □ 如支持且当前实现正确 → 将 TODO 改为确认注释
  - □ 如不支持 → 更新为正确 API，测试 `runClientData` 通过
  - □ `.\gradlew.bat :runClientData` 运行通过
  - □ 提交信息: `docs: confirm DataGenerators.addProvider API usage`
- 验收要点:
  - [ ] `rg "TODO.*Convert to new datagen API" src/main/java/` 无输出
  - [ ] `.\gradlew.bat :runClientData` 退出码 0
- 输入: `DataGenerators.java`
- 输出: 更新后的 `DataGenerators.java`
- 依赖: **P2（Config 完成）** — 仅需 mod 编译通过即可运行 datagen，不依赖 Transfer/Solar/Curios 等功能阶段
- 风险/回退: API 不兼容 → 保留 TODO，记录 evidence

#### TASK-082: TENTagProvider 迁移 TODO 评估

- task_id: `TASK-082`
- phase: P8
- scope:
  - `src/main/java/com/modularmc/ten/data/TENTagProvider.java`（`// TODO: Migrate to vanilla BlockTagsProvider`）
  - `TENModelProvider.java`（`// TODO(optimize): Migrate per-block-type model generation to Vanilla API`）
- 目标: 核验 26.1.2 标准 TagsProvider API 是否稳定，评估迁移可行性
- DoD:
  - □ 查阅 NeoForge 26.1.2 `BlockTagsProvider`/`ItemTagsProvider`/`FluidTagsProvider` API 文档
  - □ 对比当前 `TENTagProvider` 输出与标准 API 输出 diff（执行 `runClientData` 后对比 generated JSON）
  - □ 结论写回 TODO 处：
    - 如 API 稳定且 diff 可控 → 将 TODO 改为具体迁移计划
    - 如 API 不稳定或 diff 不可控 → 保留现状，TODO 改为 `// Blocked: standard TagsProvider API outputs incompatible format - revisit when NeoForge stabilizes`
  - □ 同样处理 `TENModelProvider` 中的 optimize TODO
  - □ 提交信息: `docs: assess standard TagsProvider/ModelProvider API migration feasibility`
- 验收要点:
  - [ ] TODO 注释已更新为具体证据说明（而非模糊待办）
  - [ ] `.\gradlew.bat :runClientData` 通过
- 输入: `TENTagProvider.java`、`TENModelProvider.java`；NeoForge API 证据
- 输出: 更新后的 TODO 注释
- 依赖: **P2（Config 完成，datagen 可运行）**。无需等待 Transfer/Solar/FluidModel 等功能阶段。
- 风险/回退: 评估只写文档不改逻辑，无回退风险

---

### Phase 9 — 全量验证 & 收官

> 最终阶段：fresh clean build、GameTest 运行、runClientData diff、runClient 人工场景、完整验证矩阵。

#### TASK-090: clean build + jar 隔离验证

- task_id: `TASK-090`
- phase: P9
- scope: 全项目
- 目标: 在已迁移代码上执行 fresh `clean build`，产出 jar 并验证隔离性
- **关键约束**: 必须等 P8 全部 TASK（080A、080B、081、082）完成后执行，确保存根/注释/评估已就位后再做 clean build。
- DoD:
  - □ `.\gradlew.bat clean :build *> clean_build.log`；`$LASTEXITCODE` 0
  - □ `build/libs/` 产出 `kenergyengineering-*.jar`，大小 > 100KB
  - □ jar 内 `META-INF/neoforge.mods.toml` 内容正确（无 configuration 引用、无残留占位符）
  - □ 将 jar 放入纯 26.1.2 客户端 mods 文件夹测试加载（不含开发环境模组）→ 可正常启动
  - □ 提交信息: `build: clean build verification`
- 验收要点:
  - [ ] `.\gradlew.bat clean :build` 退出码 0
  - [ ] jar 文件存在且有效
- 输入: 所有 Phase 1-8 完成（080A、080B、081、082 均已通过）
- 输出: 构建成功
- 依赖: **TASK-080A, TASK-080B, TASK-081, TASK-082**（所有 P8 任务）
- 风险/回退: build 失败 → 定位具体错误并修复

#### TASK-090A: 恢复 test sourceSet + 修复现有 GameTest 编译

- task_id: `TASK-090A`
- phase: P9
- scope:
  - `build.gradle`（`sourceSets { test { ... } }` + test dependencies）
  - `src/test/java/`（现有 GameTest 代码）
  - `src/main/java/`（测试引用的生产 API）
- 目标: GameTest test sourceSet 当前未加入 mod 定义——这是明确未完成的迁移。恢复 `sourceSet(sourceSets.test)` 配置，迁移已有 test 源码到 26.1.2 API，使 `compileTestJava` 通过。
- **关键约束（审查裁决）**:
  - 拆为独立前置 TASK，不合并到 GameTest 覆盖补写
  - 若遇真实 API 阻塞则回流计划，不得标完成绕过
  - `Failed: 0` 是验收硬条件（已知框架 bug 除外，需 evidence）
- DoD:
  - □ **审查 `build.gradle`**：确认 neoForve 块的 `sourceSet(sourceSets.test)` 存在且正确；如缺失则恢复
  - □ **审查 `dependencies.gradle`**：确认 testImplementation/testRuntimeOnly 依赖（如 `testframework`）完整
  - □ **修复 test 源码编译错误**：逐一修复现有 `src/test/java/` 中因 26.1.2 API 变更导致的编译错误
  - □ `.\gradlew.bat :compileTestJava *> compile_test.log`；`$LASTEXITCODE` 0
  - □ `.\gradlew.bat :compileJava :compileTestJava *> compile_all.log`；`$LASTEXITCODE` 0
  - □ 提交信息: `fix: restore test sourceSet and fix GameTest compilation for 26.1.2`
- 验收要点:
  - [ ] `.\gradlew.bat :compileJava :compileTestJava` 退出码 0
  - [ ] 测试编译日志中无 `kenergyengineering` 相关的 `symbol not found` 或 `cannot find symbol` 错误
- 输入: `build.gradle`、`dependencies.gradle`、`src/test/java/`
- 输出: 可编译的 test sourceSet
- 依赖: TASK-090（clean build 验证同基线）
- 风险/回退: API 阻塞 → 记录具体阻塞类/方法、NeoForge ticket 引用，回流计划；不得跳过本 TASK

#### TASK-091: GameTest 覆盖补全与运行

- task_id: `TASK-091`
- phase: P9
- scope:
  - `src/test/java/`（GameTest 源代码，已有测试）
  - `.\gradlew.bat :runGameTestServer`
- 目标: 审查现有 GameTest 覆盖，补关键 transfer/persistence/config/solar 测试，运行通过
- DoD:
  - □ 审查现有 GameTest 覆盖范围（`src/test/java/` 目录）
  - □ 如覆盖不足，补写至少以下场景:
    - 一台机器能量输入/输出（transfer sanity）
    - 管道过滤器设置→保存→重载（persistence）
    - 太阳能引擎白天 vs 夜晚能量产出（solar day-only）
    - ModConfigSpec 加载（不崩溃）
  - □ 新增测试用例调用 `level.getCapability(Capabilities.Item.BLOCK, pos, side)` / `Capabilities.Fluid.BLOCK` 验证边界适配器
  - □ `.\gradlew.bat :runGameTestServer *> gametest.log`；`$LASTEXITCODE` 0，`Passed: X` > 0，`Failed: 0`
  - □ 提交信息: `test: add transfer/persistence/solar GameTests`
- 验收要点:
  - [ ] `runGameTestServer` 结果中 `Passed: X` > 0
  - [ ] `Failed: 0`（已知框架 bug 需证据说明）
- 输出: 新增/更新 GameTest
- 依赖: TASK-090A（test sourceSet 可编译）
- 风险/回退: GameTest 运行框架不兼容 → 记录具体异常和 NeoForge 版本，回流计划；不得标完成绕过

#### TASK-092: runClientData diff 验证

- task_id: `TASK-092`
- phase: P9
- scope:
  - `.\gradlew.bat :runClientData`
  - `src/generated/resources/` 与 `src/main/resources/` 对比
- 目标: 验证 datagen 完整运行且 generated 输出合理。必须等 DataGenerators 确认（081）、TagsProvider 评估（082）、clean build（090）就位后执行，确保 datagen 输出基于最终代码。
- DoD:
  - □ `.\gradlew.bat :runClientData *> rundata.log`；`$LASTEXITCODE` 0
  - □ 检查 `src/generated/resources/` 目录非空
  - □ 将 generated 配方与 `src/main/resources/data/kenergyengineering/recipes/` 中 104 main-only 配方对比，确认无意外覆盖（`DuplicatesStrategy.EXCLUDE` 行为）
  - □ 记录 generated-only 配方列表
  - □ 提交信息: `test: validate datagen output`
- 验收要点:
  - [ ] `runClientData` 退出码 0
  - [ ] 无意外覆盖 main resources
- 输入: TASK-081, TASK-082, TASK-090 全部完成后的源码
- 输出: datagen 验证报告
- 依赖: **TASK-081, TASK-082, TASK-090**（datagen 输出正确性依赖 DataGenerators 确认、TagsProvider 评估、clean build 后状态）
- 风险/回退: datagen 产生新差异 → 审查 diff 后决定是否接受

#### TASK-093: runClient 人工场景验证

- task_id: `TASK-093`
- phase: P9
- scope: `runClient` 人工交互
- 目标: 在游戏中验证所有迁移功能的工作状态
- DoD:
  - □ `.\gradlew.bat :runClient` 启动到主菜单
  - □ Mods 页面 → Kenergy Engineering `[Loaded]` 绿色，无错误标记
  - □ **配置**: Mods 菜单 → Kenergy Engineering → Config → 修改一项 → 重启确认持久化
  - □ **物品外接**: 用能量单元对其他容器充电（使用 Curios 饰品槽 → 自动充电）
  - □ **流体外接**: 用桶接流体 → 管道传输确认
  - □ **管道重载**: 设置过滤器 → 存档退出 → 重进 → 过滤器保留
  - □ **太阳能昼夜**: 放置太阳能引擎 → 白天工作；等待夜晚（或 `/time set night`）→ 停止工作
  - □ **创造栏**: 全部 4 个标签页 → 材料变体条目存在且无重复
  - □ **JEI**: JEI 可打开，显示配方
  - □ **Jade**: 指向机器显示基本信息
  - □ **FluidModel**: 放置流体 → 纹理正确，日志无 WARN
  - □ 提交 `docs/migration/26.1.2/smoke_test_report.md` 包含截图/日志
- 验收要点:
  - [ ] 上述每项测试通过
  - [ ] `smoke_test_report.md` 存在
- 输入: TASK-090 构建成功的 jar
- 输出: 人工测试报告
- 依赖: TASK-092
- 风险/回退: 单项未通过 → 记录 issue，不阻塞整体（需判断是否 blocking）

#### TASK-094: 收口验证矩阵 + 收官

- task_id: `TASK-094`
- phase: P9
- scope: 完整验证命令集
- **关键约束（审查裁决）**: runClient 不伪造 timeout。改为通过本地 ignored `run_client.bat` 交互启动，按验收矩阵人工检查后正常关闭。若需自动日志冒烟，用明确 PowerShell `Start-Process`/`Stop-Process` 且只停止本次 PID，不能全局 kill。
- 目标: 运行全部验证命令并记录证据，调用分支收官技能，完成迁移
- DoD:
  - □ 运行以下命令集并记录退出码/日志:
    1. `.\gradlew.bat :compileJava :runClientData *> compile_rundata.log` → 退出码 0
    2. `.\gradlew.bat clean :build *> clean_build.log` → 退出码 0
    3. `.\gradlew.bat :runGameTestServer *> gametest.log` → `Failed: 0`
  - □ **runClient 交互式验证**:
    - 通过 `.\run_client.bat` 或 `.\gradlew.bat :runClient` 手动启动
    - 执行 TASK-093 验收矩阵全部场景
    - 完成后正常关闭客户端（按 Esc → 保存并退出 / 关闭窗口）
    - 记录本次启动的最新日志时间戳
    - 日志搜索: `rg "ERROR|FATAL" latest.log` 无 `kenergyengineering` 相关行
    - **如用自动启动**: `$proc = Start-Process -PassThru -FilePath ".\gradlew.bat" -ArgumentList ":runClient"; ... Stop-Process -Id $proc.Id`（仅停止本次 PID）
  - □ 回归核验: `git diff --stat` 确认变更文件列表合理
  - □ 编写 `docs/closure_verification_complete.md`（完整验证证据报告）
  - □ 调用 `分支收官` 技能，由猫娘指挥官-莉莉丝询问用户选择
  - □ 根据用户选择执行收尾动作
- 验收要点:
  - [ ] compile/runClientData/build/GameTest 所有命令退出码 0
  - [ ] runClient 日志 `rg "ERROR|FATAL"` 无 `kenergyengineering` 相关
  - [ ] `docs/closure_verification_complete.md` 存在且证据完整
  - [ ] 收官动作已根据用户选择执行
- 输入: 前序全部 TASK 完成
- 输出: 收口验证报告 + 收官动作
- 依赖: TASK-091, TASK-092, TASK-093（即 P9 全部前序任务）
- 风险/回退: 验证失败 → 定位修复后重新验证

---

## 风险与回退

### 关键风险矩阵

| 编号 | 风险 | 阶段 | 概率 | 影响 | 缓解/回退 |
|------|------|------|------|------|-----------|
| R01 | ResourceHandler API 26.1.2 文档不足，适配器实现困难 | P3-P4 | 中 | 高 | 降级：保留 `IItemHandler.of()` 桥接，不强制内部迁移完成 |
| R02 | Transaction 原子事务与旧 simulate 模型冲突 | P4 | 中 | 中 | 保留 simulate 回退路径，标注 `// TRANSACTION-TODO` |
| R03 | Curios 构件 26.1.2 实际不可用 | P6 | 低 | 低 | 恢复注释，记录 evidence 关闭 Curios |
| R04 | FluidModel WARN 为 NeoForge 内部行为不可消除 | P7 | 低 | 低 | 确认非 blocking 后记录 evidence 关闭 |
| R05 | TagsProvider 标准 API 输出 diff 不可控 | P8 | 低 | 低 | 保留自定义 DataProvider，TODO 标注 |
| R06 | clean build 失败 | P9 | 低 | 高 | 逐项排查，必要时拆分提交 |
| R07 | GameTest test sourceSet API 阻塞 | P9 | 低 | 高 | 记录具体阻塞 API 和 NeoForge ticket，回流计划；不得跳过 |
| R08 | Curios 构件 26.1.2 不可用或 class-loading 设计缺陷 | P6 | 低 | 低 | `ModList.get().isLoaded("curios")` 天然跳过不崩溃；记录 issue |

### 降级总原则

- 所有 TASK 可独立回退（`git checkout -- <file>` 或 `git revert <commit>`）
- 任何 TASK 无法在合理时间内完成 → 记录 evidence 关闭，不阻塞后续
- 最终验收允许已知非 blocking 项存在（须在 closure report 中明确列出）

---

## 提交与推送策略

| 范围 | 提交次数 | 提交信息模式 | 说明 |
|------|---------|------------|------|
| P0 checkpoint | 1 | `feat: establish 26.1.2 dev environment baseline` | 开发环境基线 |
| P0 warn baseline | 1 | `docs: record compile warning baseline for -Xlint:-removal` | 警告基线 |
| P1 toolchain | 1 | `fix: align CI JDK and mise toolchain to JDK 25` | CI 工具链 |
| P1 publish | 1 | `fix: align publish workflow JDK to 25` | 发布元数据 (010A) |
| P1 contributing | 1 | `docs: align version references to MC 26.1.2 / NeoForge 26.1.2.78` | 文档与注释基线 (010B) |
| P1 jar naming | 1 | `fix: correct jar naming to avoid duplicate MC version` | jar 命名 (010C) |
| P1 publish mc | 1 | `fix: update publish workflows MC_VERSION to 26.1.2 and align artifact glob` | 发布版本 (010D) |
| P1 versions | 1 | `chore: clean up libs.versions.toml TODOs after cross-verification` | 版本目录 |
| P2 config | 1 | `feat: migrate ConfigHolder to NeoForge ModConfigSpec` | 配置系统 |
| P2 toml | 1 | `chore: remove stale configuration dependency references` | 模板清理 |
| P3 adapters | 1-2 | `feat: implement ResourceHandler adapters for item/fluid` | 适配器 (030/031) |
| P3 BE accessors | 1 | `feat: add item/fluid accessor methods to BE classes` | BE 委托 (031A) |
| P3 registration | 1 | `feat: restore item/fluid capability registration` | 能力注册 (032) |
| P4 energy | 1 | `migrate: internal energy storage to EnergyHandler + Transaction` | 能量迁移 |
| P4 item | 1 | `migrate: internal item storage to ItemStacksResourceHandler` | 物品迁移 |
| P4 fluid | 1 | `migrate: internal fluid storage to FluidStacksResourceHandler` | 流体迁移 |
| P4 transaction | 1 | `migrate: adopt Transaction for atomic transfer operations` | 事务迁移 |
| P4 recipes/UI | 1 | `migrate: update recipe/UI/util transfer references` | 引用更新 |
| P4 lint | 1 | `chore: remove -Xlint:-removal suppression, zero removal warnings` | 警告门禁 |
| P5 persistence | 1 | `fix: restore pipe filter NBT persistence via ValueIO` | 管道持久化 |
| P6 solar | 1 | `fix: restore day-only check in solar generator` | 太阳能 |
| P6 creative | 1 | `feat: add material variants to creative tab` | 创造栏 |
| P6 curios | 1 | `feat: restore Curios integration (class-loading safe)` | Curios |
| P6 emi | 1 | `docs: close EMI integration with evidence of no 26.1.2 support` | EMI 关闭 |
| P7 fluid model | 1 | `fix: resolve FluidModel warnings for all 5 fluids` | FluidModel |
| P8 stubs A | 1 | `chore: clean up expired stubs (TENModels/TENDataGen/TENRegistrate)` | 存根清理 A |
| P8 stubs B | 1 | `chore: clean up unused mixin stubs (TemplateMixinConfig/KeyboardHandler)` | 存根清理 B |
| P8 datagen | 1 | `docs: confirm DataGenerators.addProvider API usage` | datagen 审查 |
| P8 tags | 1 | `docs: assess standard TagsProvider/ModelProvider API migration feasibility` | 标签评估 |
| P9 test sourceSet | 1 | `fix: restore test sourceSet and fix GameTest compilation` | test 恢复 |
| P9 game test | 1 | `test: add transfer/persistence/solar GameTests` | GameTest |
| P9 closure | 1 | `migrate: complete 26.1.2 migration completion` | 收官 |

**总预计提交**: ~32-34 次

---

## 验证矩阵（总表）

| 验证项 | 类型 | 命令/方法 | 关联 TASK |
|--------|------|-----------|-----------|
| 编译零错误 | 静态 | `.\gradlew.bat :compileJava` | 全体 |
| test 编译零错误 | 静态 | `.\gradlew.bat :compileTestJava` | TASK-090A |
| JDK 版本统一 | 静态 | `grep "java-version" .github/actions/build_setup/action.yml` → `25` | TASK-010 |
| 发布元数据 JAVA | 静态 | `grep "JAVA.*'25'" .github/workflows/publish.yml` → 2行 | TASK-010A |
| 文档版本一致 | 静态 | `rg "1\.21\.1" CONTRIBUTING.md docs/ src/main/java/` → 仅旧 plans 保留 | TASK-010B |
| jar 命名无重复 | 构建 | `ls build/libs/kenergyengineering-*.jar` → 主 jar 精确 `kenergyengineering-26.1.2-4.1.0.jar` | TASK-010C |
| 发布 MC_VERSION | 静态 | `rg "MC_VERSION.*1\.21\.1" .github/workflows/` → 无输出 | TASK-010D |
| 发布 glob 对齐 | 构建 | PowerShell `Get-ChildItem build/libs/*.jar | Where Name -notmatch 'slim|sources'` → 1 item | TASK-010D |
| 零 removal 警告 | 静态 | `.\gradlew.bat :compileJava -Xlint:removal *> compile_xlint.log; rg "removal" compile_xlint.log` | TASK-045 |
| datagen 完整执行 | 生成 | `.\gradlew.bat :runClientData` | TASK-081,092 |
| clean build | 构建 | `.\gradlew.bat clean :build` | TASK-090 |
| GameTest 通过 | 运行时 | `.\gradlew.bat :runGameTestServer` | TASK-090A,091 |
| capability 边界验证 | 运行时 | `level.getCapability(Capabilities.Item/Fluid.BLOCK, pos, side)` in GameTest | TASK-033,091 |
| 客户端启动 | 运行时 | `.\gradlew.bat :runClient`（交互式） | TASK-093 |
| 配置加载/持久化 | 人工 | Mods → Config 菜单 | TASK-020,093 |
| 管道过滤器持久化 | 人工 | 设置→存档→重载 | TASK-050,093 |
| 太阳能昼夜 | 人工 | `/time set night` 验证 | TASK-060,093 |
| 创造栏材料变体 | 人工 | 创造模式 ITEM 标签页 | TASK-061,093 |
| Curios 自动充电 | 人工 | 饰品槽能量单元（有 Curios 环境） | TASK-062,093 |
| JEI 配方显示 | 人工 | 打开 JEI | TASK-093 |
| FluidModel 零 WARN | 日志 | `rg "FluidModel.*kenergyengineering"` | TASK-070,093 |
| jar 隔离加载 | 构建 | 放入纯客户端 mods | TASK-090 |

---

## 协作需求

| 角色 | 阶段 | 具体任务 |
|------|------|---------|
| 猫娘指挥官-莉莉丝 | P0,P9 | checkpoint 确认、收官选择 |
| 猫娘检索员-诺雅 | P2-P4,P6 | NeoForge API/Curios/Modrinth 外部资料检索 |
| 猫娘编写官-米娅 | P0-P9 | 执行落地（默认执行者） |
| 猫娘勘探员-露娜 | P7 | FluidModel 26.1.2 契约本地勘探 |
