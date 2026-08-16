# Plan: 26.1.2 Feature Parity — Restore Deleted/Regressed Functionality vs 1.21.1

> authority: `skills/编排沙盘/templates/plans/正式计划模板.md`
> 本计划由草稿 `plans/.draft_plan_26_1_2_feature_parity.md` 经二次审查（条件通过）后，由猫娘规划师-缇娅按正式计划模板重建。

---

## 计划元数据

- **计划ID**: `26_1_2_feature_parity`
- **草稿路径**: `plans/.draft_plan_26_1_2_feature_parity.md`
- **计划路径**: `plans/plan_26_1_2_feature_parity.md`
- **版本状态**: `已批准`
- **触发原因**: 审查修订 — 二次审查（条件通过）→ 三次审查（条件通过），修复后可安全执行
- **当前 HEAD**: `7b83906`（FP-002 `feat(fp-002): restore item/fluid block capability…` 已本地 commit，**未推送**）
  - `origin/feat/26.1.2-datagen-migration` 仍为 `114887c`（FP-001 已推送）
  - 本地 ahead 1；且存在取消返工遗留的未提交 tracked 文件（`dependencies.gradle` M、`gradle/scripts/moddevgradle.gradle` M）+ 测试配置增量
  - ⚠️ **执行前必须运行 `git status --porcelain` 确认当前实际状态，不得依赖本文件硬编码**
- **对照基线**: `origin/1.21` = `406d105`
- **创建日期**: 2026-07-12
- **修订日期**: 2026-07-12（激活记录）；2026-07-12（FP-001 完成）；2026-07-12（新增全局正常游戏启动硬门禁）；2026-07-12（三次审查修复：worktree 归因、ModDev 探针标准化、FP-002 暂停隔离）
- **创建者**: 猫娘规划师-缇娅
- **审查者**: 猫娘审查官-艾琳
- **执行状态**: `进行中(standard)` — FP-001 已提交推送但正常客户端启动门禁待追补；FP-002 已本地 commit `7b83906` 但**暂停隔离**，在 FP-001 门禁通过前不得 push/继续；工作区存在取消返工遗留的未提交变更
- **审查结论**: 计划级「条件通过」（三次审查）— 需按审查修复项更新后方可安全执行。阻断项：HEAD/远程事实不匹配、git stash 归因不安全、ModDev 探针不正确、FP-002 未隔离。
- **建议下一步**: 执行前先 `git status` 确认实际事实 → 追补 FP-001 正常客户端启动验证（隔离 worktree 方式）→ 若通过则恢复 FP-002 审查返工；若失败则保持 FP-002 隔离
- **next_hop**: `git status（确认当前事实）→ 追补 FP-001 客户端启动门禁（worktree 隔离模式）→ 若通过则恢复 FP-002 审查返工`

---

## 概述

### 目标

在 NeoForge 26.1.2 / Minecraft 1.21.1 项目（已回退到保留旧 plans 的 clean tree）上，逐个恢复对比 1.21.1 (`origin/1.21` = `406d105`) 确认的 8 个核心功能缺口，实现功能等价。不扩大范围、不引入工程治理偏航。

### 基线信息

| 属性 | 值 |
|------|-----|
| 当前 HEAD | `7b83906`（FP-002 已本地 commit，**未推送**） |
| 远程 origin | `origin/feat/26.1.2-datagen-migration` = `114887c`（FP-001 已推送） |
| 本地 ahead | 1（FP-002 `7b83906` 未推送；存在取消返工遗留未提交 tracked 变更） |
| ⚠️ 执行前 | 必须运行 `git status --porcelain` 获取当前实际文件状态，不得硬编码 |
| 对照分支 | `origin/1.21` = `406d105` |
| 前瞻变更 | 从 `48fbb30` 开始，每个 FP 独立 commit + push（FP-002 暂停中，不得 push） |
| 未追踪资产 | `registry_dump.json`（诊断产物，不提交不删除） |
| TASK020 备份 | `D:\Temp\task020-backup-20260712-025524\validate_mod_config_spec.py.bak`（仅 FP-001 输入，只参考可靠检查+修正 range，不纳入 repo） |

### 范围边界

**在范围内**：
1. ConfigHolder 硬编码 → NeoForge ModConfigSpec 替换（真实 ranges/defaults，COMMON/CLIENT，TOML 持久化）
2. Item/Fluid block capability 边界恢复（反向 ResourceHandler adapter → 注册，恢复 1.21 外部交互）
3. PipeBlockEntity filter ValueInput/ValueOutput 持久化恢复
4. SolarBlockEntity day-only 判断恢复（level.isDay + skylight 等价）
5. 材料 variants 加入 creative tab
6. CuriosIntegration 恢复（官方 Curios 15.0.0-beta.2 + 26.1.2 可用，ModList 门控安全加载）
7. EMI 缺口证据化标记为外部阻塞，JEI 替代
8. 最终验证：compileJava/jar/GameTest（若需修测试兼容则修）+ server/client runtime 功能矩阵

**明确排除**：
- CI/publish/docs 版本治理
- dev mod 便利性改进
- jar 命名 / loader / MDG
- 完整内部 Resource/Transaction 重构（除非恢复功能所必需）
- FluidModel warning
- 纯存根/技术债清理（除非直接阻塞功能恢复）
- 旧计划/历史修改
- registry_dump.json 的提交或删除
- 旧 TASK020 备份文件纳入 repo

---

## 全局执行策略

### 超时与进程管理（每个任务强制执行字段）

| 字段 | 用途 | 适用 |
|------|------|------|
| `timeout_seconds` | 任务级硬超时，超时 → kill 并标记失败回流 | 每个 FP |
| `progress_interval_seconds` | 前台执行时状态汇报间隔 | 每个 FP |
| `graceful_shutdown_seconds` | 超时后先发停止信号等待时间，超后再强杀 | 后台/长时间任务 |
| `cleanup_delay_seconds` | 任务完成/失败后清理临时资源前的等待 | 每个 FP |

**具体值**见各任务定义。前台执行优先；若需后台运行，CIM 从 descendant 叶节点→根节点回收。所有 long-running 任务（>30s）必须设资源闸门（单核/限内存），不得写过度 .ps1 包装。

### 代理边界

- **规划师（缇娅）**：唯一负责写 `plans/.evidence/` 文件；evidence_01 和 evidence_07 已就绪
- **执行代理（米娅等）**：不写 `plans/.evidence/`；TDD RED 摘要输出到 `build/reports/feature-parity/`；FP-008 矩阵输出到 `build/reports/feature-parity/`
- **规划师后续按需归档**：执行回执中带回摘要，规划师审查后决定是否归档到 `plans/.evidence/`

### 逐任务正常游戏启动硬门禁

> **本门禁为全局强制要求，应用于 FP-001~FP-008，不替代各任务原有 DoD 和验收要点，而是作为标记完成、commit/push 或进入下一任务的前置条件。**

#### 动机

此前 FP-001 完成时仅依赖 compileJava + 定向验证 + 审查，未做完整正常客户端启动验证。实际 ModDev GameTestServer 两次卡在模组加载阶段不等同于正常客户端就绪。为确保每个 FP 不引入运行时崩溃/启动故障，新增本门禁。

#### 最小验收标准

每个 FP 在完成定向验证/compile/审查后，**必须**额外执行正常客户端启动验证，全部满足后方可标记完成：

1. **启动入口**：使用项目标准 client run 入口（优先现有 ModDev `runClient` 配置或等效 gradle task）。不得以 GameTestServer、adapterTestServer、数据生成器或任何仅载入 mod 但不渲染/不联网的模式替代。

2. **ModDev 就绪探针**（权威端点，见 `skill://moddev-usage`）：
   - **首选**：`GET http://127.0.0.1:47812/api/v1/status`
   - **降级**：若首选失败，读取 `<gradleProject>/build/moddevmcp/game-instances.json`，遍历各 instance 的 `baseUrl` + `/api/v1/status` 逐个探测
   - **继续条件**：`serviceReady=true`（ModDev 服务报告就绪）
   - **需要 live game state 时**：`gameReady=true`（游戏进入活跃 play state）
   - **客户端门禁检查**：`connectedSides` 数组包含 `"client"`
   - 不使用 `26375/v1/health` 或其他非权威端点

3. **稳定窗口**：在条件 2 满足后，执行 30 秒观测窗口，操作规范：
   - **探测频率**：每 5 秒调用一次 `/api/v1/status`，记录每次响应的 `serviceReady`/`gameReady`/`connectedSides`
   - **日志扫描**：每次探测同时读取 `latest.log`，仅扫描本次启动后新写入的行（通过时间戳或行号增量判断）
   - **失败条件**（任意一条即判定启动验证不通过）：
     - 任一次探测发现 `serviceReady`、`gameReady` 或 `connectedSides` 含 `"client"` 条件从满足变为不满足
     - `latest.log` 中出现 `FATAL` 级别日志
     - `latest.log` 中出现 `ModLoadingException`
     - `latest.log` 中出现未处理异常堆栈（`at net.minecraft.`、`at com.modularmc.ten.` 等，除非已在基线例外清单中注明）
   - **基线例外**：初始为空 `[]`。仅可依据**证据文件**（`plans/.evidence/`）追加条目，不得在执行现场临时忽略。例外条目包含：异常全类名、首次发现 FP、证据来源索引、指挥官-莉莉丝确认日期。
   - **窗口完成**：30 秒内无任何失败条件触发 → 稳定窗口通过。

4. **证据留存**：每任务启动验证结果保存到：
   - `build/reports/feature-parity/fpXXX-game-startup.txt`（纯文本摘要，包含探针观测时间戳、`serviceReady`/`gameReady`/`connectedSides` 值、稳定窗口起止时间、latest.log 关键检查行摘要、基线例外引用）
   - 若启动失败，该文件应包含失败原因及诊断线索

5. **失败处理与归因（隔离 worktree，严禁 git stash/reset/clean 主工作区）**：
   - 启动验证失败 → 该 FP **保持"进行中"状态**，不得标记完成，不得 commit/push
   - 执行根因诊断：先区分是**本任务代码故障**还是**基线/环境问题**
   - **归因方法**（全程在隔离 worktree 中进行，不触碰主工作区）：
     1. 以当前 FP 的基线 commit 创建隔离 worktree（`git worktree add ../<plan>-baseline-verify <baseline-commit>`）
        - FP-001 追补：基线 = `114887c`（FP-001 已推送 commit）
        - FP-002：基线 = `114887c`（FP-001 通过门禁的 commit）
        - 后续 FP：基线 =「前一任务通过全部门禁的 commit」
     2. 在隔离 worktree 中执行完整启动验证流程（同条件 1‑4）
     3. **若基线验证通过** → 主工作区本 FP 代码引入故障，按 Red-Green 流程修复
     4. **若基线验证同样失败** → 环境/非本任务问题，记录证据后上报指挥官-莉莉丝
     5. **若要判断更早基线是否已引入故障**（如 `114887c` 本身有问题）：在 `48fbb30` 另建/切换隔离 worktree（`git worktree add ../<plan>-prior-baseline 48fbb30`），执行同一验证流程
   - **worktree 保护规则**：
     - worktree 从指定 commit 创建，不复制主工作区未跟踪资产（`plans/` 等保护资产不受影响）
     - 禁止在 worktree 或主工作区中执行 `git stash`、`git reset`、`git clean`
     - worktree 使用完毕后 `git worktree remove <path>` 清理，不残留
   - **确认为本任务代码故障后**，按 Red-Green 流程修复，不得绕过门禁

6. **adapterTestServer 说明**：此运行配置（如有，如 `runAdapterTestServer`）**仅可用于行为验证**（如 FP-002 的 capability 交互测试），**绝不能替代**本小节定义的正常客户端启动硬门禁。adapterTestServer 不是 client run 入口，无渲染/无 GUI 侧，无法满足 `connectedSides` 含 `"client"` 的条件。

#### 证据命名规范

| 任务 | 证据路径 |
|------|---------|
| FP-001 | `build/reports/feature-parity/fp001-game-startup.txt` |
| FP-002 | `build/reports/feature-parity/fp002-game-startup.txt` |
| FP-003 | `build/reports/feature-parity/fp003-game-startup.txt` |
| FP-004 | `build/reports/feature-parity/fp004-game-startup.txt` |
| FP-005 | `build/reports/feature-parity/fp005-game-startup.txt` |
| FP-006 | `build/reports/feature-parity/fp006-game-startup.txt` |
| FP-007 | `build/reports/feature-parity/fp007-game-startup.txt` |
| FP-008 | `build/reports/feature-parity/fp008-game-startup.txt`（或合并到 FP-008 已有验证报告） |

#### 与 GameTest 的关系

- FP-001~FP-007：本门禁**替代** GameTest 作为运行时验证手段（项目 GameTest 基线存在已知编译问题，不做硬门禁）
- FP-008：最终验证中既要求 GameTest（设硬超时），也要求正常客户端启动验证（作为功能矩阵的一部分）
- 两个维度互相独立，不可互替：GameTest 验证功能正确性，本门禁验证启动无崩溃

### 前置步骤：清理基线（受控丢弃旧 FP-001 未提交实现）

> **目的**：在执行 FP-001 TDD RED 前，确保 tracked 与 untracked 状态均回到干净的 48fbb30 基线，
> 且**仅**清除确认为旧 FP-001 实现的文件，绝不误伤 `plans/` 资产或其他用户文件。

**操作语义**（严格执行顺序）：

1. **还原 tracked FP-001 改动**：执行 `git checkout 48fbb30 -- <涉及文件路径>` 或 `git restore --source=48fbb30 -- <涉及文件路径>`，
   将 FP-001 范围内所有 tracked 变更（`ConfigHolder.java`、`TEN.java` 等）恢复到 48fbb30 的版本。
   - 不执行 `git reset --hard`（会丢失未跟踪文件状态信息）。
   - 如已处于 48fbb30（无 tracked 变更），此步跳过。

2. **建立精确删除清单**：执行前通过 `git status --porcelain` 核对，仅删除以下确认为旧 FP-001 实现的未跟踪文件：
   - `src/main/java/com/modularmc/ten/config/TENConfig.java`（旧 FP-001 新建，未提交）
   - `src/main/java/com/modularmc/ten/config/ConfigValidator.java`（旧 FP-001 新建，未提交）
   - `scripts/fp001_validate_config_migration.py`（旧 FP-001 验证脚本，未提交）
   - 执行代理在操作前应列出实际存在的未跟踪文件清单，与上表交叉核对后逐文件删除。

3. **严格禁止**：
   - `git clean -fd` 或任何通配/批量删除命令。
   - 删除 `plans/` 目录下任何文件（含正式计划、草稿、证据）。
   - 删除 `registry_dump.json`（已声明为保留诊断产物）。
   - 删除 `D:\Temp\` 下备份文件（不在 repo 内，不涉及）。

4. **保留资产**（显式保护，不允许删除）：
   - `plans/plan_26_1_2_feature_parity.md`（本正式计划）
   - `plans/.draft_plan_26_1_2_feature_parity.md`（草稿历史）
   - `plans/.evidence/` 下全部证据文件
   - 项目内其他非 FP-001 未跟踪文件（如有，执行代理列出并确认非 FP-001 归属后保留）

5. **清理后验证**（必须全部通过才能进入 RED）：
   - □ `git diff 48fbb30 --stat` 确认 tracked 文件无差异（输出为空或仅 `plans/` 等非 FP-001 文件差异）。
   - □ 上述 3 个旧 FP-001 未跟踪文件已不存在（`Test-Path` 返回 `False`）。
   - □ `plans/` 全路径文件完整（`Test-Path` 确认正式计划、草稿、证据目录存在）。
   - □ 验证通过后，进入 **FP-001 TDD RED** 阶段。

**失败回退**：若误删了保护资产，立即停止执行并上报指挥官-莉莉丝；从 git 或备份恢复后重试。

---

## 任务清单

任务按 **严格串行（逐个攻破）** 执行。每个 FP 独立：
- TDD RED → 实装 → compileGreen → 运行时验证 → 回退预案 → 审查 → **正常客户端启动硬门禁** → commit + push
- 不跳过任何环节
- GameTest 不做 FP-001~007 的硬门禁（仅在 FP-008 最终验证中作为验证手段）
- **正常客户端启动硬门禁**：全局强制要求，定义见「逐任务正常游戏启动硬门禁」小节；每个 FP 必须在通过该门禁后才可标记完成并 commit/push

---

### FP-001: ConfigHolder → NeoForge ModConfigSpec

> **状态**: ⚠️ 功能代码已提交推送（`114887c` → origin），但新增正常游戏启动硬门禁待追补 — 不可虚报完全闭环

- **task_id**: `FP-001`
- **目标**: 将 `ConfigHolder` 从硬编码存根替换为 NeoForge `ModConfigSpec` 实现，包含 COMMON + CLIENT 两侧，TOML 持久化
- **scope**:
  - `src/main/java/com/modularmc/ten/config/ConfigHolder.java`（重建，委托 ModConfigSpec）
  - 新建 `src/main/java/com/modularmc/ten/config/TENConfig.java`（COMMON + CLIENT 双 Spec 定义）
  - `src/main/java/com/modularmc/ten/TEN.java`（注册 ConfigSpec 到 mod bus）
  - 新建 `src/main/java/com/modularmc/ten/config/ConfigValidator.java`（运行时加载值校验）
  - 排除：旧 validator 备份不纳入 repo，仅作为参考输入
- **输入**:
  - `plans/.evidence/evidence_26_1_2_feature_parity_01.md`（逐字段注解引用 + 范围/默认值/迁移映射，证据源为 `git show origin/1.21:.../ConfigHolder.java`）
  - 当前 `ConfigHolder.java`（硬编码存根，保留默认值参考）
  - `D:\Temp\task020-backup-20260712-025524\validate_mod_config_spec.py.bak`（仅参考可靠检查模式，全部 range 检查必须改为与 evidence_01 中注解 min/max 一致，不沿用旧"no defineInRange"规则）
- **依赖**: 无（FP-001 是后续基础）
- **参考来源**: `plans/.evidence/evidence_26_1_2_feature_parity_01.md`
- **耗时控制**: `timeout_seconds=600` · `progress_interval=60` · `graceful_shutdown=30` · `cleanup_delay=10`
- **DoD**:
  - □ `ConfigHolder` 改为委托 `ModConfigSpec`，移除所有 hardcoded final 字段
  - □ 所有数值字段使用 `defineInRange`，范围/默认值与 evidence_01 完全一致：

    | 字段 | 原注解 | ModConfigSpec 映射 |
    |------|--------|-------------------|
    | energyMultiplier | `@Configurable.DecimalRange(min=0.01, max=100.0)` | `defineInRange("energyMultiplier", 1.0, 0.01, 100.0)` |
    | baseEnergyCapacity | `@Configurable.Range(min=1000, max=1000000000)` | `defineInRange("baseEnergyCapacity", 10000, 1000, 1000000000)` |
    | maxEnergy | `@Configurable.Range(min=1000, max=100000000)` | `defineInRange("maxEnergy", 400000, 1000, 100000000)` |
    | chargeRate | `@Configurable.Range(min=1, max=100000)` | `defineInRange("chargeRate", 2000, 1, 100000)` |
    | inputRate | `@Configurable.Range(min=1, max=100000)` | `defineInRange("inputRate", 2000, 1, 100000)` |
    | outputRate | `@Configurable.Range(min=1, max=100000)` | `defineInRange("outputRate", 2000, 1, 100000)` |

  - □ Boolean/list 字段使用 `define` / `defineList`（无 range）
  - □ COMMON spec 包含 `machine` + `energyUnit` + `farm` 区块；CLIENT spec 包含 `client` 区块
  - □ TEN.java 在 `onCommonSetup` 中注册 COMMON，`onClientSetup` 中注册 CLIENT
  - □ 首次启动在 `run/config/` 生成 `te4-config.toml` + `te4-client.toml`，可热重载
  - □ `ConfigValidator.java` 运行时断言加载值在注解范围内（从备份重建，range 检查使用 evidence_01 的 min/max）
  - □ **TDD RED**：使用项目已有 Python 验证脚本（从备份重构），RED 阶段实际运行输出到控制台，同时保存摘要到 `build/reports/feature-parity/fp001-tdd-red.txt`（构建临时路径，非 `plans/.evidence/`）。执行代理不写 `plans/.evidence`，执行回执返回摘要，规划师后续按需归档。RED 断言：
    - 当前 hardcoded 值不在文件中 → 失败（RED）
    - 预期 ModConfigSpec 加载值与默认一致 → 通过（GREEN）
  - □ **不引入 JUnit 测试框架**
  - □ **GameTest 不做硬门禁**（FP-008 才要求）
  - □ **（⬆ 新门禁追补）正常客户端启动验证通过** — 按本计划「逐任务正常游戏启动硬门禁」小节执行完整验证（ModDev 权威端点 `http://127.0.0.1:47812/api/v1/status`、30s 稳定窗口、latest.log 扫描、worktree 归因）；证据 → `build/reports/feature-parity/fp001-game-startup.txt`  ← **当前未完成，需追补**
  - □ compileJava 通过，无 deprecation warning 新增
  - □ 旧 ConfigHolder 调用处全部适配为新 API
- **验收要点**:
  - [x] `.\gradlew.bat :compileJava 2>&1` 通过 — BUILD SUCCESSFUL，零 error，无新增 deprecation warning
  - [x] `te4-config.toml` 和 `te4-client.toml` 生成在 `run/config/`，内容包含所有定义的键
  - [x] 修改 toml 值后重启，ConfigHolder 读取新值
  - [x] 设定越界值 → 截断到 range 边界或 spec 默认值
  - [x] 键名与 origin/1.21 完全兼容（对照 evidence_01 字段列表）
- **回退**: git reset --hard HEAD~1；ConfigHolder.java 恢复为存根
- **审查要求**: 猫娘审查官-艾琳审查 range 是否与 evidence_01 完全一致
- **commit**: `feat(fp-001): replace ConfigHolder stub with NeoForge ModConfigSpec`
- **完成证明**:
  - TDD RED: `build/reports/feature-parity/fp001-tdd-red.txt` — 16 errors，均为预期失败（hardcoded 值未迁移），符合 RED 阶段预期
  - GREEN 验证: Python 验证脚本 10/10 PASS，确认 ModConfigSpec 加载值与默认一致
  - `.\gradlew.bat :compileJava`: BUILD SUCCESSFUL，zero errors，无新增 deprecation warning
  - `.\gradlew.bat :test`: 因基线已存在的 GameTest/GameTestHolder 编译问题失败，按计划口径（FP-001~FP-007 不做 :test 硬门禁）不阻塞 FP-001 完成
  - 审查结论: 审查官「通过」— range 与 evidence_01 完全一致，无实现偏航
  - 提交推送: `114887c` → `origin/feat/26.1.2-datagen-migration`，本地/远程 0 ahead 0 behind
  - ⚠️ **注意**：以上完成证明仅涵盖原有 DoD（compileJava + 定向验证 + 审查）。根据计划修订新增的全局「逐任务正常游戏启动硬门禁」，FP-001 **尚未**通过正常客户端启动验证。此门禁为新增全局要求，FP-001 需追补启动验证后方可视为真正闭环。

---

### FP-002: Item/Fluid Block Capability 边界恢复

> **状态**: ⏸️ **暂停隔离** — FP-002 已本地 commit `7b83906` 但**未推送**；因 FP-001 正常客户端启动门禁待追补，FP-002 暂停。
> - **隔离规则**：FP-001 门禁验证在隔离 worktree 中进行，不触碰主工作区 FP-002 文件。FP-002 不得 push、不得继续返工、不得在主工作区回退。
> - **若 FP-001 门禁失败**：保持 FP-002 `7b83906` 原状隔离，不在主工作区执行任何回退。FP-002 任一回退/修改需等待指挥官-莉莉丝裁决。
> - **adapterTestServer**：此任务中可选的 GameTestServer 运行仅用于验证 item/fluid capability 行为，**绝不能替代**「逐任务正常游戏启动硬门禁」定义的正常客户端启动验证。

- **task_id**: `FP-002`
- **目标**: 在 `CommonProxy` 中恢复 Item/Fluid block capability 注册，通过反向 `ResourceHandler` adapter 实现，恢复 1.21 外部管道/频道交互
- **scope**:
  - `src/main/java/com/modularmc/ten/common/CommonProxy.java`（恢复 registerCapabilities 中 item/fluid 注册行）
  - `src/main/java/com/modularmc/ten/api/capability/CapabilityAdapters.java`（现状：已有 new→old 查找适配器 getItems/getFluids，**缺 old→new 反向 adapter**）
  - 新建 `src/main/java/com/modularmc/ten/api/capability/ItemHandlerResourceAdapter.java`（`IItemHandler` → `ResourceHandler<ItemResource>` 反向适配器）
  - 新建 `src/main/java/com/modularmc/ten/api/capability/FluidHandlerResourceAdapter.java`（`IFluidHandler` → `ResourceHandler<FluidResource>` 反向适配器）
  - 排除 `api/transfer/GenericTransfer.java`（此路径不存在于项目，不涉及）
- **输入**:
  - `origin/1.21` 的 `CommonProxy.registerCapabilities` 使用 `Capabilities.ItemHandler.BLOCK` / `Capabilities.FluidHandler.BLOCK` 注册
  - 当前 26.1.2 需用 `Capabilities.Item.BLOCK` / `Capabilities.Fluid.BLOCK` + `ResourceHandler<>` 类型
  - `CapabilityAdapters` 现有只读侧（getItems/getFluids）正常工作，职责为 new→old lookup
- **依赖**: FP-001（Config 需先就绪，capability 注册不依赖 config 值但串行顺序要求）
- **耗时控制**: `timeout_seconds=900` · `progress_interval=60` · `graceful_shutdown=30` · `cleanup_delay=10`
- **DoD**:
  - □ `ItemHandlerResourceAdapter` 实现 `ResourceHandler<ItemResource>`，内部委托给 `IItemHandler`
  - □ `FluidHandlerResourceAdapter` 实现 `ResourceHandler<FluidResource>`，内部委托给 `IFluidHandler`
  - □ `CommonProxy.registerCapabilities` 恢复：
    - `event.registerBlock(Capabilities.Item.BLOCK, (level, pos, state, be, side) -> new ItemHandlerResourceAdapter(...))`
    - `event.registerBlock(Capabilities.Fluid.BLOCK, (level, pos, state, be, side) -> new FluidHandlerResourceAdapter(...))`
  - □ compileJava 通过
  - □ 运行时验证：管道物品传输恢复（GameTest 或手动）
  - □ **不做完整 Resource/Transaction 内部重构**，adapter 层保持最小转发
  - □ **重要**：上述「运行时验证」项使用 adapterTestServer GameTest 仅为行为验证，**不等同于**全局正常客户端启动硬门禁；FP-002 仍需通过「逐任务正常游戏启动硬门禁」定义的客户端完整验证后方可标记完成
- **验收要点**:
  - [ ] `.\gradlew.bat :compileJava 2>&1` 通过
  - [ ] 运行服中放箱子+管道→物品能传输
  - [ ] 流体管道类似测试
  - [ ] 注册代码量不超过 origin/1.21 + 两个 adapter 类
- **回退**: git reset --hard HEAD~1；注释回 capability 注册（⚠️ 仅在 FP-001 门禁通过并恢复 FP-002 工作时可用；在 FP-001 门禁失败/待定期间，严禁在主工作区对 FP-002 执行任何回退操作）
- **审查要求**: 艾琳审查 adapter 最小性（不引入内部重构偏航）
- **commit**: `feat(fp-002): restore item/fluid block capability with IItemHandler/IFluidHandler→ResourceHandler reverse adapters`

---

### FP-003: PipeBlockEntity Filter ValueInput/ValueOutput 持久化

- **task_id**: `FP-003`
- **目标**: 恢复 `PipeBlockEntity` 中 filter inventory 的 NBT 持久化，使过滤器配置在服务器重启后保留
- **scope**:
  - `src/main/java/com/modularmc/ten/common/blockentity/PipeBlockEntity.java`（`readTileData(ValueInput)` / `writeTileData(ValueOutput)` 恢复 filter NBT 读写）
  - 涉及：`filterInventory` 的序列化（`MachineItemHandler`，已有 `serializeNBT`/`deserializeNBT`）
- **输入**:
  - 当前 `PipeBlockEntity.readTileData`/`writeTileData` 为 TODO 空块
  - `origin/1.21` 使用 `CompoundTag` + `HolderLookup.Provider`，键名 `"filter"`
- **依赖**: FP-002（串行顺序）
- **耗时控制**: `timeout_seconds=300` · `progress_interval=30` · `graceful_shutdown=15` · `cleanup_delay=5`
- **DoD**:
  - □ `writeTileData(ValueOutput)` 中写入 `filterInventory.serializeNBT()` 到 ValueOutput（以兼容键名 `"filter"`）
  - □ `readTileData(ValueInput)` 中读取并恢复 `filterInventory.deserializeNBT()`
  - □ 键名与 origin/1.21 兼容（使用旧键名 `"filter"`）
  - □ compileJava 通过
  - □ 运行时验证：设置 filter → 重启 → filter 保留
- **验收要点**:
  - [ ] `.\gradlew.bat :compileJava 2>&1` 通过
  - [ ] 运行服中：管道 UI 设 filter → 重启 → filter 配置保留
  - [ ] NBT dump 检查 filter 数据存在
  - [ ] 旧存档 filter 键名兼容（读旧 NBT `"FilterInventory"` 或 `"filter"`）
- **回退**: git reset --hard HEAD~1；保留 TODO 空块
- **审查要求**: 检查 NBT 键名是否兼容 origin/1.21
- **commit**: `feat(fp-003): restore PipeBlockEntity filter NBT persistence in readTileData/writeTileData`

---

### FP-004: SolarBlockEntity Day-Only 判断恢复

- **task_id**: `FP-004`
- **目标**: 恢复太阳能引擎在夜晚不发电的行为（目前全天发电）
- **scope**:
  - `src/main/java/com/modularmc/ten/common/blockentity/machine/SolarBlockEntity.java`（`matchFuel` 中添加 day-only 检查）
- **输入**:
  - 当前代码：`level.canSeeSky() && !level.isRaining()` → 无时间判断
  - `origin/1.21` 逻辑：`level.getDayTime() % 24000L < 12000L`
- **依赖**: FP-003（串行顺序）
- **耗时控制**: `timeout_seconds=120` · `progress_interval=15` · `graceful_shutdown=10` · `cleanup_delay=5`
- **DoD**:
  - □ `SolarBlockEntity.matchFuel()` 在现有 `canSeeSky && !isRaining` 基础上添加 day-only 检查
  - □ 实现：`level.isDay()`（NeoForge 26.1 可用）或 `level.getDayTime() % 24000L < 12000L`
  - □ compileJava 通过
  - □ 运行时验证：白天发电 600，夜晚 0
- **验收要点**:
  - [ ] `.\gradlew.bat :compileJava 2>&1` 通过
  - [ ] 运行服中放置太阳能引擎，`/time set night` 确认停止发电
  - [ ] `/time set day` 确认恢复发电
- **回退**: git reset --hard HEAD~1；保留当前无 day-only 版本
- **审查要求**: 无特别
- **commit**: `feat(fp-004): restore day-only generation check in SolarBlockEntity`

---

### FP-005: Material Variants Creative Tab

- **task_id**: `FP-005`
- **目标**: 将所有已注册的 material variants 加入 creative mode tab，通过动态枚举而非硬编码数字
- **scope**:
  - `src/main/java/com/modularmc/ten/common/data/TENCreativeModeTabs.java`（`displayItems` 中添加 variants）
  - `src/main/java/com/modularmc/ten/common/data/Mat.java`（暴露 variants 集合枚举方法）
- **输入**:
  - `TENItems.java` static 块中 `registerVariants()` 注册的全部材料集合
  - `origin/1.21` 的 Registrate 自动归类效果
- **依赖**: FP-004（串行顺序）
- **耗时控制**: `timeout_seconds=300` · `progress_interval=30` · `graceful_shutdown=15` · `cleanup_delay=5`
- **DoD**:
  - □ `Mat.java`（或 `TENItems.java`）提供方法枚举所有已注册的 variant 物品（通过 `DeferredHolder`/`RegistryObject` 集合）
  - □ `ITEM_TAB` 的 `displayItems` 中遍历上述集合加入 tab（按 dust/ingot/nugget/plate/gear/rod/wire 分组或原始顺序）
  - □ **不使用硬数字**（删除"79"/"约79"等表述），DoD 使用 registry 集合动态枚举
  - □ DoD 验证：`registry/DeferredHolder` 集合与 tab entries 动态对比，无遗漏
  - □ compileJava 通过
  - □ 运行时验证：creative tab 中可见所有材料变体
- **验收要点**:
  - [ ] `.\gradlew.bat :compileJava 2>&1` 通过
  - [ ] 创造模式物品栏中能看到所有材料变体
  - [ ] 对比 `registerVariants` 全部注册项与 tab 可见项，100% 覆盖
  - [ ] 未来新增材料自动出现在 tab 中（无需修改 tab 代码）
- **回退**: git reset --hard HEAD~1；注释掉 displayItems 新增行
- **审查要求**: 检查是否所有 registerVariants 产物都通过动态枚举加入
- **commit**: `feat(fp-005): add all material variants to creative tab via dynamic registry enumeration`

---

### FP-006: CuriosIntegration 恢复（ModList 门控安全加载）

- **task_id**: `FP-006`
- **目标**: 恢复能量单元在 Curios 饰品栏中的充电功能（官方 Curios 15.0.0-beta.2 + 26.1.2 可用），带 ModList 门控安全加载
- **scope**:
  - `src/main/java/com/modularmc/ten/common/item/CuriosIntegration.java`（从空类恢复完整实现）
  - `src/main/java/com/modularmc/ten/common/item/EnergyUnitHandler.java`（恢复 Curios 相关引用）
  - `dependencies.gradle`（取消 Curios 依赖注释，更新版本）
- **输入**:
  - `origin/1.21` 的 CuriosIntegration 完整实现
  - 官方 Curios `15.0.0-beta.2` 的 Modrinth/GitHub 兼容性确认
- **依赖**: FP-005（串行顺序）
- **耗时控制**: `timeout_seconds=600` · `progress_interval=60` · `graceful_shutdown=30` · `cleanup_delay=10`
- **DoD**:
  - □ `dependencies.gradle` 中取消 `compileOnly` 注释，版本为 Curios `15.0.0-beta.2`（26.1.2 兼容版）
  - □ 可选加载安全门控：`CuriosIntegration` 内部使用 `ModList.get().isLoaded("curios")` 做运行时检测，Curios API 引用完全隔离在 `CuriosIntegration` 类内部
  - □ Curios 类型不泄漏到 `CuriosIntegration` 之外的公共方法签名或 static initializer 中
  - □ `EnergyUnitHandler` 对接 Curios 的充电逻辑仅在 `isLoaded("curios")` 为 true 时执行
  - □ **不依赖 `@Optional` 注解**（未经核验）
  - □ 无 Curios 环境启动：不抛出 `NoClassDefFoundError` 或 `ClassNotFoundException`（compileOnly 确保编译期有类，运行时无 jar 时不会加载引用类）
  - □ compileJava 通过（curios 为 compileOnly，运行时可选）
  - □ 运行时验证：装 Curios mod → 能量单元放饰品栏可充能；无 Curios → 不崩溃
- **验收要点**:
  - [ ] `.\gradlew.bat :compileJava 2>&1` 通过
  - [ ] 有 Curios mod 的运行服中，能量单元可放饰品栏并充电
  - [ ] 无 Curios mod 的运行服正常（无 `NoClassDefFoundError`、无 `ExceptionInInitializerError`）
  - [ ] 日志中包含 `Curios detected` 或类似门控信息（可选调试日志）
- **回退**: git reset --hard HEAD~1；恢复依赖注释和空存根
- **审查要求**: 检查 Curios 依赖坐标与 26.1.2 兼容；检查门控实现无类型泄漏
- **commit**: `feat(fp-006): restore CuriosIntegration with energy unit curio slot charging`

---

### FP-007: EMI 缺口证据化关闭

- **task_id**: `FP-007`
- **目标**: 确认 EMI 官方无 26.1.2 版本，将缺口证据化标记为外部阻塞，不采用非官方 fork，保留未来恢复锚点
- **scope**:
  - `src/main/java/com/modularmc/ten/integration/emi/TENEmiPlugin.java`（添加 BLOCKED 注释 + 状态标记）
  - `src/main/java/com/modularmc/ten/integration/emi/TENEmiRecipe.java`（添加 BLOCKED 注释 + 状态标记）
  - `plans/.evidence/evidence_26_1_2_feature_parity_07.md`（阻塞证据文件，**唯一路径**，无 `docs/` 路径二义）
- **输入**:
  - `plans/.evidence/evidence_26_1_2_feature_parity_07.md`（EMI Modrinth/GitHub/Maven 检索记录，日期 2026-07-12）
  - 用户确认：EMI 官方无 26.1.2，不能伪实现
- **依赖**: FP-006（串行顺序）
- **参考来源**: `plans/.evidence/evidence_26_1_2_feature_parity_07.md`
- **耗时控制**: `timeout_seconds=60` · `progress_interval=15` · `graceful_shutdown=5` · `cleanup_delay=5`
- **DoD**:
  - □ 证据文件 `evidence_26_1_2_feature_parity_07.md` 已包含：
    - EMI 最新版对应 NeoForge 版本
    - 与 26.1.2 不兼容的具体原因
    - Modrinth / GitHub / Maven 检索 URL
    - 检索日期
  - □ `TENEmiPlugin.java` 添加注释：`// BLOCKED: EMI has no stable release for 26.1.2. See plans/.evidence/evidence_26_1_2_feature_parity_07.md`
  - □ `TENEmiRecipe.java` 同上
  - □ **不删除类文件、不 rename、不修改访问修饰符**（保留未来恢复锚点）
  - □ JEI 替代已生效（`dependencies.gradle` 中 `compileOnly(forge.bundles.jei)` 已启用）
  - □ **不执行任何功能代码修改**
  - □ compileJava 通过（空类不引用任何 EMI API，不影响编译）
- **验收要点**:
  - [ ] `.\gradlew.bat :compileJava 2>&1` 通过
  - [ ] 证据文件被外部队验证可读
  - [ ] JEI 集成正常工作
  - [ ] EMI 空存根保留 + BLOCKED 注释到位
- **回退**: git reset --hard HEAD~1；但证据文件不损失
- **审查要求**: 验证证据文件 URL 可访问、日期有效
- **commit**: `docs(fp-007): evidence-close EMI integration as externally blocked, keep future anchor`

---

### FP-008: 最终验证

- **task_id**: `FP-008`
- **目标**: 验证所有 8 个功能缺口已恢复，compile + runtime 功能矩阵完整
- **scope**:
  - compileJava / jar
  - GameTest（设硬超时，失败直接回流）
  - 运行服功能场景验证
- **输入**: FP-001 ~ FP-007 已完成并合入
- **依赖**: FP-001 ~ FP-007（全部前置）
- **耗时控制**: `timeout_seconds=1800` · `progress_interval=120` · `graceful_shutdown=60` · `cleanup_delay=30`
  - GameTest/run 子项目独立硬超时 `600s`，超时 → 标记失败，不跳过
- **DoD**:
  - □ `.\gradlew.bat compileJava 2>&1` 通过（零 error）
  - □ `.\gradlew.bat jar 2>&1` 通过
  - □ GameTest 运行（`runGameTestServer`）：硬超时 600s，失败时输出摘要并回流，不标记通过
  - □ 现有测试通过；若有因修改引起的新失败则修复测试兼容性（不修测试逻辑）
  - □ 功能矩阵验证（运行服中手动或自动化）：

    | 功能 | 验证方式 | 预期结果 |
    |------|----------|---------|
    | Config TOML 持久化 | 修改 toml 重启 | 值生效 |
    | Item Capability | 管道连接箱子 | 物品传输 |
    | Fluid Capability | 管道连接储罐 | 流体传输 |
    | Pipe Filter 持久化 | 设 filter 重启 | filter 保留 |
    | Solar Day-Only | /time 切换 | 夜晚停、白天启 |
    | Creative Tab Variants | 开创造 | 所有材料可见 |
    | Curios Integration | 装 Curios mod | 饰品充电 |
    | EMI 关闭 | 检查日志 | 无 EMI error |

  - □ 所有未通过项已记录 blocking 原因
  - □ 矩阵结果输出到控制台或 `build/reports/feature-parity/fp008-verification.md`（构建临时路径），执行代理不写 `plans/.evidence/`；规划师后续按需归档
- **验收要点**:
  - [ ] compile + jar 通过
  - [ ] GameTest 全部通过（无超时跳过）
  - [ ] 功能矩阵 8 项全部 pass
  - [ ] 无新 warning 或仅限已知 warning
- **回退**: 各自失败回流到对应 FP commit
- **审查要求**: 猫娘审查官-艾琳审查功能矩阵结果
- **commit**: `verif(fp-008): final feature parity verification — all 8 gaps restored`

---

## 依赖链

```text
FP-001 (Config) ─────────────────────────────────────────┐
                                                         │
FP-002 (Capability) ─────────────────────────────────────┤
                                                         │
FP-003 (Pipe Filter) ────────────────────────────────────┤
                                                         │
FP-004 (Solar Day-Only) ─────────────────────────────────┤
                                                         │
FP-005 (CreativeTab Variants) ───────────────────────────┤
                                                         │
FP-006 (CuriosIntegration) ──────────────────────────────┤
                                                         │
FP-007 (EMI Evidence) ───────────────────────────────────┤
                                                         │
FP-008 (Final Verification) ◄────────────────────────────┘
```

**严格串行**：前一任务未通过全部门禁（含正常客户端启动硬门禁）前不开始下一任务。
FP-002 已例外性提前本地 commit（`7b83906`，未推送），但现已**暂停隔离**，在 FP-001 门禁通过前不得继续返工或 push。

---

## 风险与回退

| 风险 | 概率 | 影响 | 缓解措施 | 回退 |
|------|------|------|----------|------|
| Config range 与 origin/1.21 不一致 | 低 | 高 | 证据文件逐字段引用 git show 原文，validator 检查 | git reset FP-001 |
| Curios 15.0.0-beta.2 实际不兼容 26.1.2 | 中 | 中 | compileOnly + ModList 门控 + 类型隔离 | 保留注释，维持空存根 |
| GameTest 因 API 变更运行失败 | 中 | 低 | 仅修测试兼容性代码，不修测试逻辑 | 回退测试修改 |
| Pipe filter NBT 格式与旧存档不兼容 | 低 | 中 | 使用 origin/1.21 相同键名+复合键 | 兼容层 fallback |
| 长时间任务超时 | 低 | 中 | 硬超时 + 进程回收（叶→根） | 对应 FP commit 回退 |
| 资源耗尽（编译/运行服） | 低 | 中 | 资源闸门（单核/限内存），前台优先 | kill 后清理重试 |

---

## 排除范围（明确不做的重申）

以下内容在任何 FP 中**不**涉及，除非直接阻塞功能恢复：
- CI/publish/docs 版本治理
- dev mod 便利性改进
- jar 命名 / loader 配置 / MDG 更新
- 完整内部 Resource/Transaction 重构
- FluidModel warning 清理
- 纯存根/技术债清理（EMI 存根仅添加 BLOCKED 注释，不删除）
- 旧计划文件修改
- `registry_dump.json` 的提交或删除
- 旧 TASK020 备份文件的 repo 纳入

---

## 计划生命周期备注

- 本计划在 `48fbb30` tree 上开始，FP-001 ~ FP-008 各自独立 commit。
  - 已提交：`114887c`（FP-001，已推送 origin）、`7b83906`（FP-002，本地未推送，暂停隔离）
  - 后续 FP：以「前一任务通过全部门禁（含客户端启动硬门禁）的 commit」为基线
- 每个 FP 可在审查不通过时单独回退，不影响其他 FP。
  - FP-002 例外：在 FP-001 门禁失败/待定期间，禁止在主工作区回退 FP-002；仅可在 FP-001 门禁通过并正式恢复 FP-002 工作后使用回退。
- **当前状态**：FP-001 功能代码已提交推送但正常客户端启动门禁待追补；FP-002 本地 commit `7b83906` 暂停隔离中。追补 FP-001 门禁在隔离 worktree 进行，不危及 FP-002 工作。
- 草稿 `plans/.draft_plan_26_1_2_feature_parity.md` 保留为历史记录，不自动删除。
- 证据文件 `plans/.evidence/evidence_26_1_2_feature_parity_01.md`、`evidence_26_1_2_feature_parity_07.md` 保留。
