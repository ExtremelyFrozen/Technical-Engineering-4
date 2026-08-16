# 计划：能量体系重构 — 统一 tick 语义、功率升级与全网络覆盖

## 计划元数据
- 计划ID: `energy-system-refactor_001`
- 计划路径: `plans/plan_energy-system-refactor_001.md`
- 版本状态: **v1.7（执行完成版）— 全部 41 原子任务已执行并通过验证，生命周期收尾完成**
- 触发原因: 初版 — 用户选定方案后首次编纂；v1.1 — 按审查报告逐项修正后复审；v1.2 — 用户指示补完任务设计到可直接逐项派发粒度，修复公式命名不一致，记录工作区真实基线状态；v1.3 — 按三轮需求问诊裁示（A-H）写回：升级重复安装/B_max=19、B_actual四维锁定+运行停滞、取整统一、概率最坏容量、光合昼间判定API、EffectMachine batch+持续时间×B、问诊关闭记录归档；v1.4 — 落实艾琳复审"条件通过"六项修正：① B1 拆分baseFePerTick/totalFePerTick，B_byEnergy用baseFePerTick消除循环依赖；② B2 powerMultiplier接入efficientIn/getActualEfficiency生命周期并sync reset/apply；③ C1 P2-T6补RecipeMachineBlockEntity.java及实际I/O方法（onCookFinish/InputConsumptionPlan/generateItems/generateFluids）；④ C2 概率最坏容量公式清除分母B，按perBatchUnitWorst每slot/tank聚合反算；⑤ D1 P0-T6新增四维B_actual锁定/概率最坏容量的真正RED用例（B=19/B<1/催化剂/fluid/每槽聚合/energyStored/停滞/actual chance不变）；⑥ D2 P3-T1b精确限定Beacon为唯一MobEffectInstance产生者，提取400常量/覆写方法，long中间值+clamp；轻微：P0-T8测试命令列显式过滤防通配遗漏；v1.5 — 终审前最后一轮需求问诊全量裁示写回（N1-N4机械修正+新增非Beacon batch/自定义batch/注能落点/最小守卫）；v1.6 — 终审轻微清理（基线状态更新、Q4重复行删除、P3-T1c Farm行为补完）；v1.7 — 执行完成收尾：全部 41 原子任务执行完毕，验证证据写入
- 建议下一步: **本地 checkpoint 已创建（9ff7070），保留当前分支，不 push/merge。计划作为可复盘事实文件归档。Q4（JEI 总能耗）仍后置非阻塞；ModDev 视觉验证不在 DoD 内；用户工作区未提交文件/未清理**
- 完成日期: 2026-07-27
- 完成 checkpoint: `9ff7070 feat: refactor machine energy and batch processing`
- 验证证据索引: `plans/.evidence/p4_recipe_time_audit.md`、`plans/.evidence/p4_recipe_time_audit_analysis.md`
- 所属工作流: `workflow_mode=standard`

### 计划修订历史
| 版本 | 日期 | 变更摘要 | 触发 |
|------|------|----------|------|
| v1.0 | 初版 | 初始编纂 | 用户选定方案后首次 |
| v1.1 | 当前 | 艾琳审查阻断修复：S1-Q5关闭(LevelupSyn范围限制)、S2-maxProgress统一公式/附录A、M1-consume→chance()语义、M2-概率产物→genItem()/genFluid()复用rolls、M3-P4验收条件增强；关闭Q3/Q5；轻微项修复(继承链标注/SolarHelper提取/RecipeProgressResetTest更新)；更新复审条件/DoD/风险 | 艾琳审查报告 + 用户裁示 |
| v1.2 | 本版 | ① 核心公式命名一致：powerMultiplier 已含 Syn×0.8，移除外层"光合系数"项；② 新增「当前基线状态」节：计划已批准但实现未开始，compileTestJava 因取消执行留下的测试损坏而失败；③ P0 增加"工作区恢复门(P0-RG)"前置任务；④ P0-P5 每阶段拆为原子任务单（任务ID/目标/文件/核心符号/前置/测试先行/实现动作/验证命令/DoD/回滚/禁止/代理/审查门禁）；⑤ 补齐测试矩阵（映射任务ID与文件）；⑥ Q4 收敛为后置非阻塞建议；⑦ 命令统一 Windows `gradlew.bat` 格式 | 用户指示补完任务设计 + 修复命名不一致 |
| v1.4 | 本版 | ① B1 拆分baseFePerTick/totalFePerTick命名&B_byEnergy用baseFePerTick消除循环依赖；② B2 powerMultiplier接入efficientIn/getActualEfficiency生命周期+sync reset/apply；③ C1 P2-T6补RecipeMachineBlockEntity.java及实际I/O落点；④ C2 概率最坏容量按perBatchUnitWorst每slot/tank聚合反算消除分母B；⑤ D1 P0-T6新增四维B_actual/概率最坏容量真正RED用例(B=19/B<1/催化剂/fluid/每槽聚合/energyStored/停滞/actual chance)；⑥ D2 P3-T1b限定Beacon为唯一MobEffectInstance产生者+400常量/覆写方法+long中间值+clamp；轻微 P0-T8显式过滤防通配遗漏 | 艾琳复审"条件通过"六项修正落实 |
| v1.5 | 本版 | ① 附录A继承链修正+真实baseTickTime(Condenser=1000/Encflu=800/Furnace=cookingTime无/2)；② baseFePerTick最小1守卫(safeRound→Math.max(1,...→B_byEnergy)；③ Syn注能10FE/t落点显式写入(P2-T5/P2-T7/P3-T1a/Processing共性)；④ 非Beacon Effect batch逐类行为(Quarry×B独立挖掘/Farm×B行扫描/MobRip×B随机伤害)拆入P3；⑤ Furnace/Condenser/Encflu逐类batch写入P1/P2原子任务+B_actual约束；⑥ 标准RecipeMachine(5类)与自定义ProcessingMachine(3类)batch路径分离；⑦ P0-RG仅恢复门禁不声称完成 | 终审前最后一轮需求问诊裁示全量写回 |
| v1.6 | 本版 | ① 当前基线状态更新：保留compileTestJava曾因Javadoc/结构错误失败的历史事实，更新状态为「文件结构已完整但未运行fresh compileTestJava，编译状态未验证」；② 删除Q4段落完全重复行；③ P3-T1c Farm B超过剩余行数只处理末尾、多余batch作废、能耗仍按锁定B支付 | 终审轻微清理（艾琳终审条件通过后） |
| v1.7 | 2026-07-27 | **执行完成收尾**：① 全部 41 原子任务/检查点标记完成；② P0-RG～P5-T4 分阶段验证证据写入；③ checkpoint `9ff7070` 创建（本地保留，未 push/merge）；④ Q4 后置非阻塞未实现、ModDev 视觉验证未执行且不在 DoD、用户工作区未提交/未清理等残留边界明确 | 执行完成 → 生命周期收尾 |

---

## 概述

### 目标
将整个能量网络从「累加能量到进度→超量完成」重构为「固定 FE/t 消耗→按 tick 计数→>= 完成」模型。同时将**四款现有**功率升级的效果改为乘法模型（含光合供能抑制）、**正式实现批量处理**（batch 乘法联动 I/O FE/t、能量上限、物品/流体槽位）、统一配方 `time` 语义（仅表示处理 ticks），覆盖处理机、EffectMachine、发电机、线缆、通道、能量单元、NeoForge capability adapter、面配置、GUI/JEI 回归。

### 核心决策
1. **不新增配方字段**：配方 `time` 只表示 ticks；FE/t 由机器 `initialEfficientIn` 决定；总能耗 = ticks × FE/t。不改 `FormsCombinedRecipe`/`Serializer`/`IBaseRecipeCm` 字段边界。
2. **批处理纳入正式实现**：batch = Σ batch_i 加法叠加；批量因子 B = 1 + batch；每 tick 消耗 `FE/t × B` 能量，progress 固定 +1，处理时间仍由 `recipe.time` ticks 决定（不 progress+B）；物品输入 `chance()≤0` 视作催化剂保持单份、`chance()>0` 按 B 倍消耗、流体输入全部按 B 倍消耗、确定性输出 × B、概率产物按 B 次调用现有 `genItem()`/`genFluid()`（每次内部复用现有 rolls 语义）；I/O FE/t、能量上限、物品/流体槽位容量全部 × B 联动，槽位数与 GUI 布局不变。
3. **三种升级可同类型重复安装**：Aug/Power/Shulker 不限同类型重复（每 UpgradeItem 栈 1，共 6 升级槽）；Syn 仍最多 1。理论极值 6×Shulker → Σbatch_i = 18，B_max = 19。
4. **B_actual 启动四维锁定**：按输入容量（物品/流体）、输出容量（确定性+概率最坏）、当前储能取最大可行 B；固定后运行中不动态降 B。能量维度启动时仅看当前 energyStored（非最大值）；若下一 tick 能量不足，当前 tick 停滞：不扣能、不加 progress、maxProgress 不变，墙钟时间延长。
5. **取整规则统一**：FE/t 使用 `Math.round`；容量/吞吐整数截断并做安全钳位；处理 ticks 使用 `Math.ceil` 且 `Math.max(1, ...)`。
6. **概率输出容量预判**：启动锁定时假设所有 B×rolls 试验 100% 成功（按最坏产量反算），动态设置该配方/批量所需输出槽上限；实际完成仍对每 batch 调用 `genItem()`/`genFluid()`，内部 chance 保持原值，不预掷、不缓存、不改概率。空间不足时启动时降低 B 至容纳最坏输出的最大值，运行中不降 B，零丢失/零地面掉落。
7. **光合不外输**：光合供能抑制升级只向本机储能补充 FE（固定 10FE/t，不乘 B、不乘功率倍率），不向网络输出；无光时倍率仍生效。
8. **EffectMachine 应用 batch**：每 tick 能耗 FE/t×B，progress+1；maxProgress/触发间隔不因 B 改变；效果等级/范围不变；每次应用的 MobEffectInstance 持续时间=基础持续时间×B 并做 int 安全钳位；同 tick 不重复 apply B 次。**非Beacon EffectMachine（Quarry/Farm/MobRip）逐类不同**：每完成周期执行 B 次独立操作——Quarry 独立挖掘/产出 B 次、Farm 连续扫描 B 行（按现有行索引推进且不重复/越界）、MobRip 独立随机选择并伤害 B 次（允许目标少时同一实体被多次选中，仍需每次检查存活/有效性）。Beacon 保持 MobEffect duration×B 且每周期只 apply 一次。
9. **存档最小破坏**：旧进度允许重置（progress/maxProgress 语义变化），不做比例迁移；库存和储能按 `readTileData`/`writeTileData` 现有 NBT 字段保留。
10. **锁定 26.1.2**：所有改动基于 NeoForge 26.1.2 API，不混用 1.21.x/26.2.x。
11. **ModDev 不纳入**：ModDev 功能缺失不作为任何阶段的依赖、验收条件或阻塞项；视觉验证以静态分析、集成测试、日志断言和可复现测试替代。
12. **标准 RecipeMachine 与自定义 ProcessingMachine batch 路径分离**：5 个标准 RecipeMachine（Pulverizer/Compressor/Refiner/Indfur/Psionicant）通过 `RecipeMachineBlockEntity.onCookFinish()` + `InputConsumptionPlan` 实现 I/O×B；3 个自定义 ProcessingMachine 直接子类（Furnace/Condenser/Encflu）各自实现独立 batch 逻辑，不共用 `InputConsumptionPlan`。非 Processing 系的 RadiusMachine 子类（Quarry/Farm/MobRip）各自实现 effect-level batch 循环。
13. **非标准 ProcessingMachine 逐类适配**：Furnace 按最大可行 B 消耗 B 份输入并生成 B 份结果；Condenser 保留催化剂单份/既有周期消耗语义，重写旧能量累加 cooking，完成时输出 5mB×B 并按 tank 可用容量反算 B；Encflu 按可行输入组数执行最多 B 组，每组独立安全转移/回滚、产出和 XP fluid，受单槽物品栈、工具不可堆叠、输出槽/tank 容量约束，B_actual 可因此降至 1。三者不得只多耗能无收益。
14. **baseFePerTick 最小 1 守卫**：`baseFePerTick = Math.max(1, safeRound(initialEfficientIn × powerMultiplier))`；`B_byEnergy = floor(energyStored / Math.max(1, baseFePerTick))`；不得使用 `cancelStart` 造成未来 0 功耗机器无法工作，最小 1 FE/t 为已选安全约定。long 中间值与 int clamp 适用所有 ×B 乘法。
15. **Syn 注能当前 tick 检查前注入**：在 ProcessingMachine/EffectMachine 当前 tick 能量检查前注入 10FE/t；`receiveEnergy(10, false)`，满储自然丢弃余量；不乘 B/倍率，不外送；通过公共 helper/共享注入方法避免两份分叉。

---

## 不变量与公式

### 核心模型公式
| 量 | 公式 | 说明 |
|---|---|---|
| `总能耗` | `recipe.time × baseFePerTick × B_actual` | baseFePerTick = `Math.round(initialEfficientIn × powerMultiplier)`；**powerMultiplier 已含 Syn×0.8（若安装光合）**；见下方 powerMultiplier 公式；总能耗 = recipe.time × totalFePerTick |
| `baseFePerTick` | `Math.max(1, Math.round(initialEfficientIn × powerMultiplier))` | 每批单位基础FE/t，含功率升级、不含batch B；存于efficientIn/effAuc，由getActualEfficiency()返回。取整顺序统一：先round(initial×power)得baseFePerTick，Math.max(1, ...)保证非零，最终消耗再×B_actual后round。long中间值+int clamp |
| `totalFePerTick (含 batch)` | `Math.round(baseFePerTick × B_actual)` | 最终每tick消耗（含batch因子）；上限受`maxExtractEnergy`钳位；使用Math.round，非int截断 |
| `maxProgress (RecipeMachine)` | `baseTickTime()` | **RecipeMachine 子类**：`baseTickTime()` 返回当前配方 `recipe.time()`（ticks）|
| `maxProgress (Furnace)` | `baseTickTime()` | **FurnaceBlockEntity**（RecipeMachine 子类）：`baseTickTime()` 返回 `recipe.cookingTime()`，**移除当前 `/2`**；不保留 Furnace 固有 2 倍速度 |
| `maxProgress (EffectMachine)` | `(int)Math.ceil(effectInterval() × 20 × durationMultiplier)` | EffectMachine 无 recipe，由 effectInterval 和升级倍率决定；不因 B 改变 |
| `durationMultiplier` | `Π(1 - reduction_i)` | 各功率组件逐个乘法叠加；下限 1 tick；Furnace 同样受 durationMultiplier 影响（无固有2倍特权） |
| `powerMultiplier` | `Π(1 + increase_i) × (Syn 已安装 ? 0.8 : 1.0)` | 各功率组件逐个乘法叠加；**Syn 0.8 已嵌入 powerMultiplier，不是外层项**；需防 int 溢出 |
| `batch` | `Σ batch_i` | 加法叠加；同类型可重复安装；6 升级槽极值 6×Shulker=18；B_max=19 |
| `maxProgress (通用硬下限)` | `Math.max(1, maxProgress)` | 任何机器 maxProgress 至少 1 tick，通过 `Math.max(1, ...)` 保证 |
| `容量/吞吐整数安全` | `(int)Math.min(rawValue, Integer.MAX_VALUE / 2)` | 所有 ×B 后的容量/吞吐值先 int 截断，然后安全钳位防溢出 |

### 取整规则统一表
| 量 | 取整方法 | 说明 |
|---|---|---|
| baseFePerTick（含升级不含B） | `Math.max(1, Math.round(double))` | 先 round(initialEfficientIn×powerMultiplier) 再 max(1, ...) 保证非零；用于 totalFePerTick 和 B_byEnergy 分母 |
| totalFePerTick（含 batch） | `Math.round(baseFePerTick × B_actual)` | 在 baseFePerTick 基础上再×B后 round；两步 round 顺序固定，全文统一 |
| 机器储能容量（×B后） | `(int)` 截断 + `Math.min(value, Integer.MAX_VALUE/2)` | 安全钳位防溢出 |
| I/O 吞吐（×B后） | `(int)` 截断 + `Math.min(value, Integer.MAX_VALUE/2)` | 同上 |
| 物品/流体槽位容量（×B后） | `(int)` 截断 + `Math.min(value, Integer.MAX_VALUE/2)` | 同上 |
| 处理 ticks（maxProgress/duration） | `Math.ceil(double)` + `Math.max(1, ...)` | 向上取整 + 至少 1 tick |
| 效果持续时间×B | `(int)Math.min(baseDuration × (long)B, Integer.MAX_VALUE)` | long 中间值防溢出后再 clamp 到 int；同 tick 不重复 apply B 次 |

### 批量处理 B 因子
| 量 | 公式 | 说明 |
|---|---|---|
| `B` | `1 + batch` | 批量因子；batch≥0；B≥1；同类型可重复；6×Shulker 得 B_max=19 |
| `B_theory` | `1 + Σbatch_i` | 理论 B（安装所有升级后的总值） |
| `B_actual` | `min(B_theory, B_byItems, B_byFluids, B_byOutput, B_byEnergy)` | **启动时四维锁定**：取输入(物品/流体)、输出(确定性+概率最坏)、当前储能维度下最大可行 B；B<1 时不启动；固定后运行中不动态降 B |
| `B_byItems` | `floor(slotCapacity_avail / inputCount)` | 物品输入维度：可用槽位容量足够容纳 B 倍输入 |
| `B_byFluids` | `floor(tankCapacity_avail / fluidInputCount)` | 流体输入维度：可用流体槽容量足够容纳 B 倍输入 |
| `B_byOutput` | `min(B_byDeterministic, B_byProbWorst)` | 输出维度：确定性 + 概率最坏情况综合 |
| `perBatchUnitWorst` | `Σ(ingredient.rolls × maxAmount × maxCount)` | **每批单位最坏输出（不含B）**：对每个概率输出 ingredient 按 100% chance 假设，计算单batch可能的最大产量。多ingredient/同槽合并/物品与流体需按目标slot/tank分别聚合，不得仅用全局容量总和导致分布误判 |
| `B_byProbWorst` | `minOverSlots( floor(availableSlotCapacity / perBatchUnitWorst_slot) )` | 概率容量维度：对每个输出slot/tank，用可用容量 ÷ 该槽最坏产量（不含B）反算最大B，取全局最小值。若fluid tank不支持动态扩容，按实际tankCapacity反算 |
| `B_byEnergy` | `floor(energyStored / Math.max(1, baseFePerTick))` | **启动时**仅看当前 `energyStored`（非最大值），用baseFePerTick（不含B）避免循环依赖，`Math.max(1, ...)` 保证零功耗机器不除零；启动后运行中停滞时不降 B |
| `物品输入（催化剂，chance()≤0）` | 不变（1 份） | 物品输入 `chance()≤0` 视作不消耗催化剂，每配方周期仅消耗 1 份；不按 B 倍乘 |
| `物品输入（消耗品，chance()>0）` | `originalInputCount × B` | 物品输入 `chance()>0` 按 B 倍消耗；无 consume 字段，由 chance() 值判定 |
| `流体输入` | `originalInputCount × B` | **所有 fluid 输入按 B 倍消耗**；当前无催化剂跳过逻辑，不虚构 fluid consume 字段。未来可在另建语义后扩展，本计划不做 |
| `确定性输出数量` | `originalOutputCount × B` | 主输出/副产出按 B 倍乘 |
| `概率产物` | `rolls × B 次独立试验` | 每 batch 单位调用现有 `ing.genItem()`/`genFluid()`，每次内部复用现有 rolls 语义；总独立试验次数 = B × rolls。禁止直接 random chance 循环导致丢失 rolls |
| **概率输出容量预判** | `maxWorstOutput(B) = perBatchUnitWorst × B_actual` | 启动锁定时用perBatchUnitWorst（不含B）× B_actual验证；实际完成仍走 genItem()/genFluid()，内部 chance 不变 |
| `I/O FE/t（批处理模式）` | `Math.round(baseFePerTick × B_actual)` | 批处理激活时输入/输出能量速率；使用 Math.round，baseFePerTick 不含B |
| `能量上限` | `(int)Math.min(baseEnergyStorage × B_actual, Integer.MAX_VALUE/2)` | 机器储能容量上限；截断+钳位 |
| `物品槽位容量` | `(int)Math.min(slotCapacity × B_actual, Integer.MAX_VALUE/2)` | 每槽容量倍乘；槽位数与 GUI 布局不变（已确认） |
| `流体槽位容量` | `(int)Math.min(slotCapacity × B_actual, Integer.MAX_VALUE/2)` | 同上（已确认） |
| `配方周期总能耗` | `maxProgress × totalFePerTick` | 批处理模式下单周期总能耗 = maxProgress × Math.round(baseFePerTick × B_actual) |
| `Syn 注能 (10FE/t)` | `energyStorage.receiveEnergy(10, false)` | 有效光照时每 tick 在能量检查前注入 10FE/t；满储自然丢弃；不乘 B/倍率；不外送；通过公共 helper 共享 |

> **概率产物机制说明**：每份概率输出在 `onCookFinish()` 中循环 B 次，每次调用现有 `ing.genItem()` / `ing.genFluid()`——这些方法内部已封装 rolls 和 chance 逻辑（含 `random.nextDouble() < chance` 判断）。**B 次循环是 batch 层的外层调用，genItem/genFluid 内部 rolls 语义不变**；总独立试验次数 = B × rolls。禁止在 batch 层直接写 `random.nextDouble() < chance` 循环，否则会丢失 genItem 内部已有的多 rolls 语义。若产物槽位满，剩余概率产物丢弃（复用现有 `cooking()` 满槽暂停逻辑）。
>
> **概率输出容量预判细则**：启动时 `B_actual` 锁定前，对每个概率输出 ingredient，先计算**每批单位最坏产量 perBatchUnitWorst = rolls × maxAmount × maxCount（不含B）**，对每个输出slot/tank分别聚合。然后用 `B_byProbWorst = floor(availSlotCapacity / perBatchUnitWorst_slot)` 反算该槽/罐维度下最大B，取全局最小值。最终验证 `requiredWorst(B) = perBatchUnitWorst × B_actual ≤ slotCapacity`。若流体 tank 不支持启动后动态扩容，则按实际 tank capacity 反算 B 而非虚构扩容 API。实际完成产物仍由 `genItem()`/`genFluid()` 内部的真实 chance 决定，不预掷、不缓存、不改概率。运行中不降 B，确保零丢失/零地面掉落。
>
> **运行停滞语义**：若当前 tick 开始时 `energyStored < totalFePerTick`（即 `Math.round(baseFePerTick × B_actual)`），则该 tick 完全停滞：不 `extractEnergy`、不 `progress++`、maxProgress 不变。停滞仅延长墙钟时间，不惩罚/不降级。energyStored 恢复后自动继续。
> **Syn 注能时序**：在停滞判断**之前**，ProcessingMachine/EffectMachine 每 tick 先执行 `energyStorage.receiveEnergy(10, false)`（仅 Syn 安装 + 有效光照时），然后再检查 `energyStored < totalFePerTick` 决定是否停滞。注能不乘 B/倍率，不外送。

### 功率升级表（四款）

### 功率升级表（四款）

#### 升级乘法公式
| 组件 | time 减少 | FE/t 增加 | batch 加 | `reduction` | `increase` | `batch_i` |
|---|---|---|---|---|---|---|
| 增强组件 (Augmented) | 25% | +30% | 0 | 0.25 | 0.30 | 0 |
| 充能组件 (Charged) | 40% | +50% | +1 | 0.40 | 0.50 | 1 |
| 潜影组件 (Shulker) | 60% | +100% | +3 | 0.60 | 1.00 | 3 |
| 光合供能抑制 (Photosyn) | ×1.5 | ×0.8 | 0 | — | — | 0 |

> 光合供能抑制的 time/FE 效果为独立倍率（非 reduction/increase 语义），嵌入 powerMultiplier 链：`powerMultiplier ×= 0.8`（已纳入 powerMultiplier 公式，非外层额外项）、`durationMultiplier ×= 1.5`。
>
> **安装限制说明**：Aug/Power/Shulker 不限同类型重复安装（每栈1、6升级槽）；Syn 仍最多 1 个（通过 `isPhotosynInstalled()` 守卫）。理论极值 6×Shulker → B=19。

#### 组合示例（powerMultiplier 已含 Syn 0.8）
| 组合 | durationMultiplier 计算 | powerMultiplier 计算（含 Syn 0.8） | B |
|---|---|---|---|
| 仅增强 | 1 × (1-0.25) = 0.75 | 1 × (1+0.30) = 1.30 | 1 |
| 增强+充能+潜影 | 0.75 × 0.60 × 0.40 = 0.18 | 1.30 × 1.50 × 2.00 = 3.90 | 1+0+1+3=5 |
| 增强+潜影+光合 | 0.75 × 0.40 × 1.50 = 0.45 | 1.30 × 2.00 × 0.80 = 2.08 | 1+0+3+0=4 |
| **3×潜影** | 0.40³ = 0.064 | 2.00³ = 8.00 | 1+3+3+3=10 |
| **6×潜影（极值）** | 0.40⁶ = 0.004096 | 2.00⁶ = 64.00 | 1+3×6=19 |
> **同类型可重复安装**：UpgradeItem 每栈 1、共 6 升级槽；Aug/Power/Shulker 不限同类型重复（6×Shulker 得 B=19、Σbatch_i=18）；Syn 仍最多 1（`LevelupSyn.effect()` 中 `isPhotosynInstalled()` 上限守卫）。B_max=19 为理论极值，实际受 B_actual 输入/输出/储能四维限制。测试须覆盖 6×Shulker 溢出钳位和 B_actual 降级路径。

### 光合供能抑制
| 条件 | 效果 |
|---|---|
| 安装数上限 | 1（通过 `isPhotosynInstalled()` 守卫） |
| 可安装机器类型 | 仅 `MACHINE_PROCESS` + `MACHINE_EFFECT`，不扩展发电机（`LevelupSyn.canApply()` 严格复用现有身份检查，禁止泛化为任意 `CmMachine`） |
| time 倍率 | 1.5（独立乘算在 durationMultiplier 链中） |
| FE/t 倍率 | 0.8（独立乘算在 powerMultiplier 链中）；**始终生效，无论是否有光** |
| 有效光照 FE 注入 | 有效光照时，固定向本机储能注入 **10FE/t**，不乘 B、不乘功率倍率、不向网络输出；受本机储能容量限制。**注能时机**：ProcessingMachine/EffectMachine 每 tick 在能量检查和停滞判断前首先执行 `energyStorage.receiveEnergy(10, false)`；满储自然丢弃余量。通过公共 helper 方法共享注入逻辑避免两份分叉 |
| 有效光照判定 | `level.canSeeSky(pos.above()) && !level.isRaining() && isDaytime(level)` |
| 昼间判定接口 | 方案一（标准 Minecraft API）：`level.getDayTime() % 24000L < 12000L`——在 26.1.2 中可用，为最稳定方式。方案二（NeoForge Clock API）：`level.dimensionType().defaultClock()` 获取 Overworld daylight clock，再通过 `((ServerLevel)level).clockManager().getTotalTicks(daylightClock) % 24000L < 12000L` 判定。本计划推荐方案一以确保客户端兼容性；方案二作为备用。详见 P2-T7 任务 |
| 无光时 | 倍率仍生效（duration×1.5 / power×0.8），**不产生内部 FE 注入** |
| 内部 FE 去向 | 仅本机储能，不向网络推送 |
| 外部供电 | 无光时仍可从外部接收能量运行 |

---

## 范围 / 不做项

### 范围内
- `ProcessingMachineBlockEntity` tick 语义重写
- `EffectMachineBlockEntity` tick 语义重写
- `CmMachineBlockEntity` 升级系统改为乘法模型 + batch 因子联动
- `EngineBlockEntity` 发电机 tick 语义检查（发电机产生能量不变，但 energyAllowRun 边界需确认）
- `CableBlockEntity` 网络传输（线缆本身是 buffer，不改 tick 语义但需验证 I/O 速率一致性）
- `ChannelEnergyBlockEntity` 通道传输
- `CellBlockEntity` / `CreativeCellBlockEntity` 能量单元
- `CapabilityAdapters` adapter 验证
- `MachineEnergyStorage` extract/maxExtract 一致性
- 面配置 `energyFaceMode` I/O 速率
- GUI `energyGauge`/`fuelGauge`/`progressGauge` 显示同步
- `TENJeiCategory`/`TENRecipeWidget` JEI 能量信息显示回归
- 手写 DataPack 配方 `time` 审核修订
- DataGen 配方 `TENRecipeGen` 中 `time` 审核修订
- **升级 `UpgradeItem` 四款现有组件效果改造**：
  - `LevelupAug`（增强组件）— 乘法 duration/power + batch+0
  - `LevelupPower`（充能组件）— 乘法 duration/power + batch+1
  - `LevelupShulker`（潜影组件）— 乘法 duration/power + batch+3
   - `LevelupSyn`（光合供能抑制）— 独立倍率 time×1.5 / FE/t×0.8 / batch+0；**canApply() 严格限制 MACHINE_PROCESS + MACHINE_EFFECT，禁止泛化为任意 CmMachine**
- **批处理正式数据模型与调度**：
  - B 因子计算（`1 + Σbatch_i`）
  - **标准 RecipeMachine（5 子类）**：物品输入 `chance()≤0` 视作催化剂保持单份；`chance()>0` 按 B 倍消耗；流体输入全部按 B 倍消耗；确定性输出 ×B；概率产物 B×rolls 独立试验；通过 `InputConsumptionPlan` 实现输入消费 ×B
  - **直接 ProcessingMachine 自定义 batch（Furnace/Condenser/Encflu）**：各自独立实现 batch 逻辑，不共用 InputConsumptionPlan（见 P1-T3a/T3b/T3c）
  - **RadiusMachine effect-level batch（Quarry/Farm/MobRip）**：各自实现 effect 循环 B 次独立操作（见 P3-T1c）
  - 概率产物：每 batch 单位调用现有 `ing.genItem()`/`genFluid()`，每次内部复用现有 rolls 语义；总独立试验次数 = B × rolls；禁止直接 random chance 循环丢 rolls
  - I/O FE/t、能量上限、物品/流体槽位容量 ×B 联动
  - 满槽暂停 + 防止槽位/容量越界
- 旧存档 progress/maxProgress 重置
- 测试（RED/GREEN：单元测试 + 集成契约 + 批处理场景矩阵）

### 范围外
- 不新增配方 `fePerTick` / `energy` 字段
- 不改 `FormsCombinedRecipeSerializer` 序列化格式
- 不改 `IBaseRecipeCm` 接口方法签名
- 不改 `FormsCombinedRecipe.matches`/`matchesExactInputs` 输入匹配逻辑
- 不复制第二套光照算法（提取共享静态方法而非复制）
- **不虚构流体 consume 字段**——流体当前无催化剂跳过逻辑，所有 fluid 输入按 B 倍消耗；未来可在另建语义后扩展，本计划不做
- 不做旧进度比例迁移
- 不处理跨存档版本升级脚本（仅 NBT 向前兼容：读旧 progress 重置为 0）
- 不改 `rolls` 字段语义或序列化格式（概率产物仍走现有 chance/rolls 判断，通过 `genItem()/genFluid()` 封装调用，B 次外层循环）
- **ModDev 功能缺失**不纳入任何阶段的阻塞/验收/依赖
- 不新增物品、注册项、升级子类、配方 JSON、模型 JSON 或语言键（四款升级均为现有组件平级改造）

---

## 当前基线状态（before 实施事实）

> **重要**：本计划 v1.1 已通过审查，但**实际实现尚未开始**。以下为代码仓库真实状态，与计划预期之间有差距，执行前必须先对齐。

### 审查状态
- v1.1 审查通过（轻微项遗留：核心公式命名不一致 → 本版 v1.2 已修正）
- 所有 7 项复审条件已关闭
- 计划已批准但未进入执行

### 工作区实际状态（git status）
| 项目 | 状态 |
|------|------|
| `UpgradeMultiplicativeStackTest.java` | 未跟踪（untracked）——上次取消执行留下的测试文件，存在于工作区但从未提交 |
| `RecipeProgressResetTest.java` | 已修改未暂存（unstaged modified）——上次取消执行留下的增量改动 |
| 其余 5 个新测试文件 | 未跟踪（untracked）——均存在于工作区但从未提交 |
| `compileTestJava` | **历史曾失败**——用户此前运行compileTestJava曾因`UpgradeMultiplicativeStackTest.java` Javadoc/结构错误失败；当前只读检查文件结构已完整，但未运行fresh compileTestJava，编译状态未验证 |

### 关键事实记录
1. **计划 v1.1 已审查通过但从未被执行**：无任何重构代码写入生产源文件（`ProcessingMachineBlockEntity.java` 等未修改）。
2. **compileTestJava 曾因测试文件损坏失败**：失败源是上次取消执行留下的`UpgradeMultiplicativeStackTest.java` Javadoc/结构错误。当前只读检查该文件结构已完整，但未运行fresh compileTestJava，编译状态未验证。**P0-RG须先检查git diff并运行fresh compileTestJava后决定修复或直接建立RED**。
3. **这些未跟踪的测试文件是否可保留作为起点**需由执行代理（米娅）+ 审查（艾琳）在工作区恢复门（P0-RG）中评估决定。可能的行动：（a）修复损坏文件继续使用；（b）删除所有搁置测试文件，从计划 DoD 重新编写。
4. **所有生产代码仍为加法模型**，未应用任何乘法/批处理改造。

### 与计划预期差异
- 计划假设 P0 RED 测试已就绪 → 文件结构已完整但编译状态未验证，需P0-RG先检查git diff并运行fresh compileTestJava后确认
- 计划假设基线测试通过 → compileTestJava状态待P0-RG验证后确定（不再预先假设仍损坏/失败）

---

### 执行完成记录（v1.7 追加）

> 以下为 P0～P5 全部 41 原子任务执行完毕后的验证证据摘要。代码 checkpoint `9ff7070`（本地保留，未 push/merge）。

#### P0 — 基线与契约测试（9 任务）
| 任务 | 状态 | 关键证据 |
|------|------|----------|
| P0-RG | ✅ | `compileTestJava` BUILD SUCCESSFUL；生产代码未动（`git diff -- src/main` 为空） |
| P0-T1 | ✅ | `ProcessingMachineContractTest` RED→GREEN，覆盖 7 断言点 |
| P0-T2 | ✅ | `EffectMachineContractTest` RED→GREEN，覆盖契约测试完整 |
| P0-T3 | ✅ | `UpgradeMultiplicativeStackTest` 乘法叠加公式验证（含 6×Shulker B=19） |
| P0-T4 | ✅ | `BatchFactorTest` 纯函数 B 因子验证 |
| P0-T5 | ✅ | `RecipeProgressResetTest` 更新断言通过 |
| P0-T6 | ✅ | `BatchProcessingContractTest` 全 22 项 RED→GREEN（四维 B_actual/概率最坏容量/B=19/B<1/催化剂/fluid/停滞/actual chance 不变/baseFePerTick 最小 1/Syn 10FE/t） |
| P0-T7 | ✅ | `PhotosynEffectTest` 光合专用契约全部通过 |
| P0-T8 | ✅ | P0 GREEN 基线验证：快照 GREEN + 新断言 RED→GREEN |

#### P1 — 处理机 tick 语义重构（6 任务）
| 任务 | 状态 | 关键证据 |
|------|------|----------|
| P1-T1 | ✅ | `process()` 重写：固定 FE/t 消耗、tick 计数、`progress>=maxProgress`、else 不重置 |
| P1-T2 | ✅ | CmMachineBlockEntity 辅助方法 + energyAllowRun 调整 |
| P1-T3 | ✅ | 子类 conditionStart 统一 + Furnace cookingTime 移除 `/2` |
| P1-T3a | ✅ | Furnace batch：B 份输入消费 + B 份产出 |
| P1-T3b | ✅ | Condenser batch：重写 cooking，旧 `progress+=200*efficiency` 完全删除，输出 5mB×B |
| P1-T3c | ✅ | Encflu batch：B 组独立安全转移/回滚 |

#### P2 — 升级改造 + 批处理实现（8 任务）
| 任务 | 状态 | 关键证据 |
|------|------|----------|
| P2-T1 | ✅ | CmMachineBlockEntity 乘法方法 + efficientIn 生命周期（reset→apply→consume）+ 最小 1 守卫 |
| P2-T2 | ✅ | LevelupAug effect() → `applyDurationReduction(0.25)` + `applyPowerIncrease(0.30)` |
| P2-T3 | ✅ | LevelupPower effect() → + `applyBatch(1)` |
| P2-T4 | ✅ | LevelupShulker effect() → + `applyBatch(3)` |
| P2-T5 | ✅ | LevelupSyn effect() → `applyPhotosyn()` + 安装上限 1 + `canApply()` 仅限 PROCESS+EFFECT |
| P2-T6 | ✅ | 标准 RecipeMachine batch（5 子类：Pulverizer/Compressor/Refiner/Indfur/Psionicant）通过 InputConsumptionPlan ×B |
| P2-T7 | ✅ | `isValidDaytime()` 共享静态方法提取 + 昼间 API 解冻 + `tryInjectPhotosynEnergy()` 公共 helper |
| P2-T8 | ✅ | 极端倍率（6×Shulker+Syn/B=19）不溢出；duration≥1；取整统一验证 |

#### P3 — 全网络覆盖（8 任务）
| 任务 | 状态 | 关键证据 |
|------|------|----------|
| P3-T1a | ✅ | EffectMachineBlockEntity tick 改造（FE/t×B + progress++ + 停滞语义 + Syn 注能） |
| P3-T1b | ✅ | Beacon 效果持续时间×B（`BEACON_BASE_DURATION=400`，long 中间值 + int clamp，同 tick 仅 apply 一次） |
| P3-T1c | ✅ | 非 Beacon EffectMachine batch：Quarry×B 独立挖掘、Farm×B 行扫描（不重复/越界）、MobRip×B 随机伤害 |
| P3-T2 | ✅ | EngineBlockEntity 验证通过 |
| P3-T3 | ✅ | CableBlockEntity 验证通过 |
| P3-T4 | ✅ | ChannelEnergyBlockEntity 验证通过 |
| P3-T5 | ✅ | CellBlockEntity / CreativeCellBlockEntity 验证通过 |
| P3-T6 | ✅ | MachineEnergyStorage / CapabilityAdapters 验证通过 |
| P3-T7 | ✅ | 面配置 I/O 验证通过 |

#### P4 — 配方 time 审核（5 任务）
| 任务 | 状态 | 关键证据 |
|------|------|----------|
| P4-T1 | ✅ | 审计表生成 → 详见 `plans/.evidence/p4_recipe_time_audit.md`（338 行，320 配方全覆盖） |
| P4-T2 | ✅ | 手写配方 time 批量修订完成（有变更清单，旧→新映射） |
| P4-T3 | ✅ | DataGen 配方 time 审核修订完成 |
| P4-T4 | ✅ | `runClientData` 重新生成 + diff 验证通过（仅预期 time/格式变更） |
| P4-T5 | ✅ | 脚本验证 PASS：datapack matrix main102/generated218/intersection0/union320 PASS；orphan cache 789/789/0/0 PASS；无 FE/t 字段新增 |

#### P5 — 存档/GUI/JEI/回归（4 任务）
| 任务 | 状态 | 关键证据 |
|------|------|----------|
| P5-T1 | ✅ | 旧存档重置策略：progress/maxProgress 读旧 NBT 重置为 0，储能/库存/升级保留 |
| P5-T2 | ✅ | GUI 同步验证：progressGauge 显示 ticks 进度；@DescSynced 同步正确 |
| P5-T3 | ✅ | JEI 显示回归：time 显示为 ticks（**Q4 总能耗后置非阻塞，未实现**） |
| P5-T4 | ✅ | **全量回归 BUILD SUCCESSFUL**：`gradlew.bat compileJava compileTestJava cleanTest test --console=plain --no-daemon --max-workers=1 --no-build-cache` → 1239 passed / 0 failed；`git diff --check` 0；艾琳全局门禁条件通过 |

#### 关键验证命令输出摘要
```powershell
# 全量测试（1239 passed / 0 failed）
gradlew.bat compileJava compileTestJava cleanTest test --console=plain --no-daemon --max-workers=1 --no-build-cache

# DataPack 矩阵检查 PASS
python scripts/datapack_registration_matrix_check.py
# → main 102 / generated 218 / intersection 0 / union 320 PASS

# Orphan 缓存检查 PASS
python scripts/generated_orphan_check.py
# → cached 789 / files 789 / orphan 0 / stale 0 PASS

# Git diff --check 无空白错误
git diff --check
# → 0
```

#### 残留 / 范围边界
- **Q4（JEI 总能耗显示）**：后置非阻塞建议，未实现。P5-T3 仅验证 time 显示为 ticks。不阻塞验收。
- **ModDev 视觉验证**：计划明确排除（核心决策 §11），不在任何 DoD/命令/依赖/验收条件中。不做视觉确认。
- **用户工作区未提交文件**：`git status` 显示若干 untracked/modified 文件（`TODO.txt`、`scripts/` 下新脚本、`src/test/java/.../utils/` 等），属于用户既有工作区内容，不在本计划范围内，未提交未清理。
- **checkpoint `9ff7070`**：本地已创建，未 push/merge。用户选择保留当前分支 + 本地 checkpoint。

#### 证据文件引用
- `plans/.evidence/p4_recipe_time_audit.md` — P4-T1 审计表（338 行，320 配方全覆盖）
- `plans/.evidence/p4_recipe_time_audit_analysis.md` — 交叉验证分析（结论：全部一致，无变更需要）

---

### ProcessingMachineBlockEntity (src/main/java/.../api/blockentity/ProcessingMachineBlockEntity.java)
- `maxProgress = baseTickTime() * Math.max(initialEfficientIn, 1)` ← 当前存能量总量
- `energyConsumed = Math.min(getActualEfficiency(), energyStorage.getEnergyStored())`
- `progress += energyConsumed`
- 完成条件：`progress > maxProgress` ← 是 "大于" 不是 ">= "
- 能量不足时：`progress` 不减但也不进 → 但 `else` 分支会把 progress 重置为 0
- `conditionStart()` 由子类实现，目前无 recipe identity 保护

### EffectMachineBlockEntity (src/main/java/.../api/blockentity/EffectMachineBlockEntity.java)
- 同样 `progress += energyConsumed`，`maxProgress = (int)(effectInterval() * 20 * max(initialEfficientIn, 1))`
- 完成条件 `progress > maxProgress`，else 分支重置 progress=0

### EngineBlockEntity (src/main/java/.../api/blockentity/EngineBlockEntity.java)
- 生产方：`energyStorage.receiveEnergy(getActualEfficiency(), false)`，每 tick 固定生产
- 燃料消耗：`fuel = Math.max(fuel - efficientIn, 0)` ← 消耗速率 = efficientIn（FE/t）
- 不需改 tick 语义，但需验证 energyAllowRun 边界

### CmMachineBlockEntity (src/main/java/.../api/blockentity/CmMachineBlockEntity.java)
- `energyAllowRun()`：处理机 `energyStorage.getEnergyStored() >= efficientIn`；发电机 `energyStorage.getEnergyStored() + getActualEfficiency() <= maxStorageEnergy`
- `onUpgradeApply(percent, slotIncrease)`：当前按 `initial* + initial* × percent` 加法叠加
- `resetUpgradeEffects()` 重置为 `initial*` 然后 `applyUpgradeEffects()` 遍历所有升级
- `doBaseData()`：每 tick 调 `resetUpgradeEffects()` + `applyUpgradeEffects()`
- 持久化字段：`progress`, `maxProgress`, `energyStored`, `maxEnergyStored` 等均通过 `@Persisted @DescSynced`

### CableBlockEntity (src/main/java/.../common/blockentity/CableBlockEntity.java)
- 每 5 tick 执行 redistribute：source→sink→cable buffer
- 速率 `transferFor(state)` 控制每 tick 传输量
- 能量存在 `MachineEnergyStorage` 中

### 配方 (IME/JSON)
- 手写 recipes 在 `src/main/resources/data/kenergyengineering/recipe/`（~104 个）
- DataGen 在 `src/generated/resources/data/kenergyengineering/recipe/`（~211 个）
- `time` 字段以 tick 为单位，但没有统一标准——有的机器 baseTickTime 与 recipe time 已耦合

### 升级系统（现有代码真实状态）
- `UpgradeItem` 基类（`common/item/upgrades/UpgradeItem.java`）：`percent` 字段（double，当前值 0.2/0.35/0.75） + `effect(IUpgradableMachine)` + `canApply(IUpgradableMachine)`
- `IUpgradableMachine` 接口（`common/item/upgrades/IUpgradableMachine.java`）：`onUpgradeApply(double percent, int slotIncrease)` — 当前纯加法叠加
- **四款功率升级已全部注册**，无需新建物品/注册项：
  | 中文名 | 注册 ID | 类 | 文件 | 当前 percent | 当前 slotIncrease | 实际语义 |
  |---|---|---|---|---|---|---|
  | 升级：增强组件 | `augmented_levelup` | `LevelupAug` | `upgrades/LevelupAug.java` | 0.20 | 1 | ~+20% 全属性（加法） |
  | 升级：充能组件 | `powered_levelup` | `LevelupPower` | `upgrades/LevelupPower.java` | 0.35 | 2 | ~+35% 全属性（加法） |
  | 升级：潜影组件 | `relic_levelup` | `LevelupShulker` | `upgrades/LevelupShulker.java` | 0.75 | 3 | ~+75% 全属性（加法） |
  | 升级：光合供能抑制 | `photosyn_levelup` | `LevelupSyn` | `upgrades/LevelupSyn.java` | 0.00 | 1 | 当前 effect: `onUpgradeApply(-0.1, 1)`（负 percent 减属性 + 加槽） |
- **`CmMachineBlockEntity.onUpgradeApply()`**（`api/blockentity/CmMachineBlockEntity.java:994`）：
  ```java
  efficientIn = (int)(efficientIn + initialEfficientIn * percent);  // 加法
  maxStorageEnergy = (int)(maxStorageEnergy + initialEnergyStorage * percent);
  // ... 影响全部 8 个参数（efficientIn, maxStorage/Receive/Extract × 能量/物品/流体）
  ```
  - 当前是 `efficientIn += initialEfficientIn * percent` — 加法叠加，且 percent 统一影响所有参数
  - 重构目标：将 `efficientIn` 影响拆为 duration 乘法 + power 乘法，其他 7 个参数保留加法（当前行为不变）
- **配方**：手写合成 JSON 在 `data/kenergyengineering/recipe/vanilla/upgrades/lu/lev_augmented.json`（产出 `augmented_levelup`）、`lev_powered.json`（产出 `powered_levelup`）、`lev_shulker.json`（产出 `relic_levelup`）— 无需新建配方
- **语言键**：
  - `item.kenergyengineering.augmented_levelup` = "升级：增强组件" / "Upgrade: Augmented Kit"
  - `item.kenergyengineering.powered_levelup` = "升级：充能组件" / "Upgrade: Powered Kit"
  - `item.kenergyengineering.relic_levelup` = "升级：潜影组件" / "Upgrade: Shulker Kit"
  - `item.kenergyengineering.photosyn_levelup` = "升级：光合供能抑制" / "Upgrade: Photosynthetic Suppressor"
  - 提示键：`kenergyengineering.augmented_levelup.0` = "机器：额外升级槽+1\n全局性能+20%"（源自 `TENLangHandler.java:104-106`）
- **模型**：`assets/kenergyengineering/models/item/augmented_levelup.json` 等已存在

---

## 阶段规划（原子任务结构）

> 每个任务标注执行代理与审查门禁。默认执行代理：米娅（`猫娘编写官-米娅`）。审查门禁：艾琳（`猫娘审查官-艾琳`）审查通过后进入下一任务。各任务均遵循 TDD 模式：先写 RED 测试 → 实现使 GREEN → 审查 → 合并。
> 
> **通用禁止项（所有任务适用，不再重复）**：
> - 不新增配方 `fePerTick` / `energy` 字段
> - 不改 `FormsCombinedRecipeSerializer` 序列化格式
> - 不改 `IBaseRecipeCm` 接口方法签名
> - 不改 `FormsCombinedRecipe.matches`/`matchesExactInputs` 输入匹配逻辑
> - 不新增物品、注册项、升级子类、配方 JSON、模型 JSON 或语言键（四款升级均为现有组件平级改造）
> - **ModDev 不出现于任何 DoD、验证命令、依赖或验收条件中**
> - 不修改 `TENItems.java` 注册行（141-144）
> - 不虚构流体 consume 字段
> - 命令使用 `gradlew.bat`（Windows），附加 `--console=plain`，完整输出，300s 硬超时，不用 `Select-String` 过滤 FAILED，不默认 `--rerun-tasks`

### P0：基线与契约测试（RED 阶段）

**阶段目标**：建立可编译的工作区基线，为当前行为编写快照测试，为重构目标编写 RED 测试。

**阶段依赖**：无（但需先通过 P0-RG 工作区恢复门）

**允许并行**：P0-T1 至 P0-T7 可并行编写（共享 P0-RG 基底），P0-T8 依赖前序全部完成

---

#### P0-RG：工作区恢复门（前置门禁，非测试任务）

| 字段 | 值 |
|------|-----|
| **任务ID** | `P0-RG` |
| **目标** | 将工作区从「上次取消执行留下的不一致状态」恢复到可编译基线。**本任务只恢复测试文件的可编译性，不实施任何生产代码重构** |
| **目标文件** | `src/test/java/.../common/item/upgrades/UpgradeMultiplicativeStackTest.java`（损坏）、`src/test/java/.../api/blockentity/RecipeProgressResetTest.java`（未提交修改）、其余 5 个未跟踪测试文件 |
| **核心符号** | N/A（不是实现任务） |
| **前置依赖** | 无 |
| **测试先行** | N/A |
| **实现动作** | ① 先检查 `git diff --stat` 确认测试文件变化范围，然后运行 `gradlew.bat compileTestJava --console=plain` 检查当前编译状态。若已通过（BUILD SUCCESSFUL）→ 跳过修复步骤，直接建立RED（测试已可编译，RED=运行时断言失败即可）。若仍失败 → 评估`UpgradeMultiplicativeStackTest.java`损坏程度（Javadoc/结构错误），修复或重建；检查`RecipeProgressResetTest.java`未提交修改决定保留或丢弃；确认其余5个未跟踪测试文件完整性。② 再次运行 `gradlew.bat compileTestJava --console=plain` 确认全部测试源通过编译。**【编译错误不算 RED】**——RED 要求测试编译通过 + 运行时断言失败。**注意：先验证git diff和compileTestJava结果，不得预先断言当前仍损坏/失败** |
| **验证命令** | `gradlew.bat compileTestJava --console=plain`（预期：BUILD SUCCESSFUL） |
| **DoD** | ① `compileTestJava` 成功，无编译错误；② 所有 P0 测试文件存在（新建或恢复）且语法正确；③ 生产代码未做任何改动（`git diff -- src/main` 为空）；④ 未解决的问题已记录为风险/注释 |
| **回滚/失败处理** | 若 `UpgradeMultiplicativeStackTest.java` 不可恢复，删除后从计划 DoD 重新编写；若 `RecipeProgressResetTest.java` 修改有冲突，`git checkout -- src/test/java/.../RecipeProgressResetTest.java` 丢弃后重新实施 |
| **禁止项** | 不修改任何生产代码文件；不运行 `test` 任务（只 `compileTestJava`）；不修复 `compileJava` 错误（若有，暂停并上报指挥官） |
| **执行代理** | 米娅 |
| **审查门禁** | 艾琳验证 `compileTestJava` 输出 + `git diff -- src/main` 为空 |

---

#### P0-T1：ProcessingMachineContractTest 新建 + RED

| 字段 | 值 |
|------|-----|
| **任务ID** | `P0-T1` |
| **目标** | 为 `ProcessingMachineBlockEntity` 新建契约测试，覆盖当前加法行为快照 + P1 目标行为 RED 断言 |
| **目标文件** | `src/test/java/.../api/blockentity/ProcessingMachineContractTest.java`（新建） |
| **核心符号** | `ProcessingMachineBlockEntity.process()`、`maxProgress`、`progress`、`energyAllowRun`、`cooking()` |
| **前置依赖** | P0-RG |
| **测试先行** | 本任务即测试编写：① `progress > maxProgress` 当前完成条件快照（GREEN）；② `progress >= maxProgress` 目标断言（RED）；③ 能量不足时暂停不重置 progress（RED）；④ 输出满时暂停不重置 progress（RED）；⑤ maxProgress 在配方开始时固定（RED）；⑥ FE/t 受 upgrade multiplier 影响（RED）；⑦ duration >= 1 tick（RED） |
| **实现动作** | 仅编写测试文件。包含静态纯函数辅助方法（与 UpgradeMultiplicativeStackTest 类似风格），不 mock Minecraft 环境。使用文件源码扫描验证当前行为 |
| **验证命令** | `gradlew.bat test --tests "com.modularmc.ten.api.blockentity.ProcessingMachineContractTest" --console=plain`（预期：基线快照 GREEN + 目标断言 RED） |
| **DoD** | ① 测试文件编译通过；② 当前行为快照测试全部 GREEN；③ 新行为测试全部 RED（因源码未改）；④ 覆盖 7 个断言点 |
| **回滚/失败处理** | 若编译失败，调试验证语法/导入；若基线快照 RED，说明用户对当前行为的理解与源码不一致，上报指挥官 |
| **禁止项** | 不修改任何生产代码；不 mock 整个 Minecraft 环境 |
| **执行代理** | 米娅 |
| **审查门禁** | 艾琳：验证 RED 断言语义正确 + 覆盖范围完整 |

---

#### P0-T2：EffectMachineContractTest 新建 + RED

| 字段 | 值 |
|------|-----|
| **任务ID** | `P0-T2` |
| **目标** | 为 `EffectMachineBlockEntity` 新建契约测试 |
| **目标文件** | `src/test/java/.../api/blockentity/EffectMachineContractTest.java`（新建） |
| **核心符号** | `EffectMachineBlockEntity`、`effectInterval()`、`maxProgress` |
| **前置依赖** | P0-RG（可与 P0-T1 并行） |
| **测试先行** | 同 P0-T1 模式：`progress > maxProgress` 当前快照（GREEN）、`progress >= maxProgress`（RED）、else 分支不重置（RED）、maxProgress 固定（RED） |
| **验证命令** | `gradlew.bat test --tests "com.modularmc.ten.api.blockentity.EffectMachineContractTest" --console=plain` |
| **DoD** | 同 P0-T1 模式，覆盖 EffectMachine 特有契约 |
| **禁止项** | 不修改生产代码 |
| **执行代理** | 米娅 |
| **审查门禁** | 艾琳 |

---

#### P0-T3：UpgradeMultiplicativeStackTest 新建/修复 + RED

| 字段 | 值 |
|------|-----|
| **任务ID** | `P0-T3` |
| **目标** | 建立四款功率升级的乘法叠加公式测试。若已存在（从取消执行恢复），验证其覆盖完整度 |
| **目标文件** | `src/test/java/.../common/item/upgrades/UpgradeMultiplicativeStackTest.java`（已有未跟踪文件，需评估） |
| **核心符号** | `durationMultiplier()`、`powerMultiplier()`、`batchFactor()`——纯静态函数，已验证 Syn 0.8 在 powerMultiplier 链内 |
| **前置依赖** | P0-RG（可与 P0-T1/P0-T2 并行） |
| **测试先行** | ① 单升级数值验证（Aug/Power/Shulker/Syn 的 duration/power/batch）；② 乘法叠加组合（Aug+Power；Aug+Power+Shulker；全部四款）；③ Syn 独立倍率嵌入链（非外层项）；④ 极端倍率不溢出；⑤ 有效持续时间 >= 1 tick；⑥ 当前加法基线快照（percent 值、`onUpgradeApply` 调用模式）；⑦ **同类型可重复安装验证（3×Power、3×Shulker、6×Shulker B=19）**；⑧ **Syn 上限 1 守卫测试** |
| **验证命令** | `gradlew.bat test --tests "com.modularmc.ten.common.item.upgrades.UpgradeMultiplicativeStackTest" --console=plain` |
| **DoD** | ① 所有单升级数值 test 通过；② 组合验证通过；③ Syn 0.8 在 powerMultiplier 链内（非外层项）断言通过；④ 极端倍率不溢出；⑤ 当前加法模型基线快照全部 GREEN |
| **禁止项** | 不修改生产代码；不重复 P0-T1/T2 覆盖的 ProcessingMachine 契约 |
| **执行代理** | 米娅 |
| **审查门禁** | 艾琳：验证 Syn 0.8 的测试口径与 v1.2 公式一致 |

---

#### P0-T4：BatchFactorTest 新建 + RED

| 字段 | 值 |
|------|-----|
| **任务ID** | `P0-T4` |
| **目标** | 验证 B 因子计算与条件逻辑 |
| **目标文件** | `src/test/java/.../common/item/upgrades/BatchFactorTest.java`（新建/已有未跟踪） |
| **核心符号** | `B = 1 + Σbatch_i`；`chance()>0`→B 倍消耗；`chance()≤0`→催化剂单份 |
| **前置依赖** | P0-RG（可与 P0-T1 并行） |
| **测试先行** | ① 无升级 B=1；② Aug(0) B=1；③ Power(1) B=2；④ Shulker(3) B=4；⑤ Aug+Power+Shulker B=5；⑥ Syn(0) 不影响 B；⑦ `chance()>0` 倍乘语义（纯函数验证）；⑧ `chance()≤0` 催化剂语义；⑨ **同类型重复（2×Power=1+1+1=3→B=4；3×Shulker=1+9=10→B=10）**；⑩ **6×Shulker B=19 极值验证** |
| **验证命令** | `gradlew.bat test --tests "com.modularmc.ten.common.item.upgrades.BatchFactorTest" --console=plain` |
| **DoD** | B 因子计算全部验证（含 6×Shulker B=19）；物品输入分类（催化剂/消耗品）语义测试通过；同类型重复安装 B 正确 |
| **禁止项** | 不 mock 机器实例，用纯函数验证数学逻辑 |
| **执行代理** | 米娅 |
| **审查门禁** | 艾琳 |

---

#### P0-T5：RecipeProgressResetTest 更新 + RED

| 字段 | 值 |
|------|-----|
| **任务ID** | `P0-T5` |
| **目标** | 更新现有 `RecipeProgressResetTest`，断言「能量不足/输出满暂停不清零、配方失效才重置」 |
| **目标文件** | `src/test/java/.../api/blockentity/RecipeProgressResetTest.java`（已有文件，有未提交修改） |
| **核心符号** | `conditionStart()`、progress 保留语义、配方 identity 变化重置 |
| **前置依赖** | P0-RG（需要先决定保留还是丢弃未提交修改） |
| **测试先行** | ① recipe identity 未变 → progress 保留（GREEN 基线）；② recipe 从 A→B → progress 重置 0（RED 目标）；③ recipe→null → progress 重置 0（RED）；④ null→recipe → progress 重置 0（RED）；⑤ 多 tick 同配方 progress 保留（RED）；⑥ 源码验证 else 分支当前无条件 progress=0（GREEN 基线） |
| **验证命令** | `gradlew.bat test --tests "com.modularmc.ten.api.blockentity.RecipeProgressResetTest" --console=plain` |
| **DoD** | 现有 `simulateConditionStart` 测试全部 GREEN；新增 RED 断言覆盖配方 identity 变化场景 |
| **禁止项** | 不修改生产代码；不改已有基线测试 |
| **执行代理** | 米娅 |
| **审查门禁** | 艾琳 |

---

#### P0-T6：BatchProcessingContractTest 新建 + RED

| 字段 | 值 |
|------|-----|
| **任务ID** | `P0-T6` |
| **目标** | 批处理完整契约：I/O FE/t ×B、能量上限 ×B、槽位容量 ×B、概率产物 rolls 独立 |
| **目标文件** | `src/test/java/.../api/blockentity/BatchProcessingContractTest.java`（新建/已有未跟踪） |
| **核心符号** | `getBatchFactor()`、`maxEnergyStorage`、`slotCapacity`、`genItem()`/`genFluid()` |
| **前置依赖** | P0-T4（B 因子数学验证完成） |
| **测试先行** | ① 无功率升级退化为原行为（B=1 无变化）；② B=2 时 FE/t 消耗 ×2、progress+1、maxProgress=recipe.time；③ 确定性输出 ×B；④ 概率产物 B×rolls 独立试验（统计验证非简单概率倍乘）；⑤ 满槽时暂停不丢进度；⑥ 能量不足时暂停不降级 B；⑦ 槽位容量 ×B 但不改变槽位数；⑧ **B_max=19（6×Shulker）溢出钳位测试**；⑨ **B_actual 四维锁定（输入/输出/流体/储能）测试：模拟每维度限制条件，验证 B_actual 按正确维度降级**；⑩ **概率输出最坏容量预判测试（item/fluid 分别按 slot/tank 聚合）**；⑪ **运行停滞语义测试**；⑫ **Math.round/截断/钳位/ceil 取整验证**；⑬ **B=19 时 baseFePerTick 与 totalFePerTick 分离正确（totalFePerTick = Math.round(baseFePerTick × B)）**；⑭ **B<1 不启动：当 B_theory 因维度限制降至 0 时验证机器不启动**；⑮ **催化剂（chance()≤0）输入消耗保持 1 份、消耗品（chance()>0）按 B 倍消耗的纯函数验证**；⑯ **流体输入全部按 B 倍消耗验证**；⑰ **概率最坏容量按每 output slot/tank 分别聚合反算 B，多 ingredient 同槽合并验证**；⑱ **energyStored 维度用 baseFePerTick（不含B）而非 totalFePerTick 反算，验证无循环依赖**；⑲ **fixed lockedB 运行中停滞不降 B 测试**；⑳ **概率产物的实际 chance 不变：验证 genItem/genFluid 内部 chance 未被 batch 层预掷或修改**；㉑ **baseFePerTick 最小 1 守卫：模拟 initialEfficientIn=0/efficiency 降为 0 后 baseFePerTick 仍为 1，B_byEnergy 分母不除零，机器仍可启动**；㉒ **Syn 10FE/t 注入测试：安装 Syn+有效光照时 energyStored 每 tick 增加 10（受容量上限）；无光/雨天/夜间不注入；满储时不报错；不外送；不乘 B** |
| **验证命令** | `gradlew.bat test --tests "com.modularmc.ten.api.blockentity.BatchProcessingContractTest" --console=plain` |
| **DoD** | 批处理契约全部 RED；概率产物独立性通过统计验证；槽位容量边界验证；6×Shulker B=19 溢出测试 RED；概率最坏容量预判测试 RED（按每 slot/tank 聚合）；停滞语义测试 RED；取整验证 RED；B<1 不启动 RED；催化剂/消耗品/流体输入倍乘 RED；B_byEnergy 用 baseFePerTick 无循环依赖 RED；固定 B 运行中不降 B RED；actual chance 不变 RED；baseFePerTick 最小 1 守卫 RED（零功耗机器 baseFePerTick=1 不除零）；Syn 10FE/t 注入 RED（有光/无光/满储/不外送） |
| **禁止项** | 不修改生产代码 |
| **执行代理** | 米娅 |
| **审查门禁** | 艾琳 |

---

#### P0-T7：PhotosynEffectTest 新建 + RED

| 字段 | 值 |
|------|-----|
| **任务ID** | `P0-T7` |
| **目标** | 光合供能抑制专用测试：独立倍率、安装上限、无光不失倍率、不外输 |
| **目标文件** | `src/test/java/.../common/item/upgrades/PhotosynEffectTest.java`（新建/已有未跟踪） |
| **核心符号** | `LevelupSyn`、`applyPhotosyn()`、`canApply()`、光照判定 |
| **前置依赖** | P0-RG（可与 P0-T6 并行，但测试设计联调可能需要 P0-T3 的 Syn 数值常量） |
| **测试先行** | ① duration ×1.5 独立乘算（非 reduction）；② FE/t ×0.8 在 powerMultiplier 链内（非外层项）；③ 安装上限 1 — 第二次 `applyPhotosyn()` 调用返回 false；④ `canApply()` 仅通过 MACHINE_PROCESS 和 MACHINE_EFFECT（读源码验证）；⑤ 无光时倍率仍生效（纯函数验证不依赖光照 mock）；⑥ 光合能量不外输（契约断言）；**⑦ 10FE/t 注入测试：有光时 energyStored 增加 10/t；存量 0 时仍可注入；满储时不报错；无光/雨天/夜间不注入；不向外发送能量** |
| **验证命令** | `gradlew.bat test --tests "com.modularmc.ten.common.item.upgrades.PhotosynEffectTest" --console=plain` |
| **DoD** | 光合专用契约全部 RED；安装上限 1 测试通过；canApply 范围验证 |
| **禁止项** | 不修改生产代码 |
| **执行代理** | 米娅 |
| **审查门禁** | 艾琳：重点验证 Syn 0.8 在 powerMultiplier 链内的测试口径 |

---

#### P0-T8：P0 GREEN 基线验证（当前行为快照回归）

| 字段 | 值 |
|------|-----|
| **任务ID** | `P0-T8` |
| **目标** | 运行全部当前行为快照测试，确认基线 GREEN |
| **目标文件** | 所有 P0 测试文件 |
| **核心符号** | 全部 |
| **前置依赖** | P0-T1 至 P0-T7 全部完成 |
| **测试先行** | 本任务即验证：运行全部 P0 测试，确保当前行为快照（CurrentAdditiveBaseline 等）全部 GREEN，新行为断言全部 RED |
| **验证命令** | `gradlew.bat test --tests "com.modularmc.ten.api.blockentity.ProcessingMachineContractTest" --tests "com.modularmc.ten.api.blockentity.EffectMachineContractTest" --tests "com.modularmc.ten.api.blockentity.RecipeProgressResetTest" --tests "com.modularmc.ten.api.blockentity.BatchProcessingContractTest" --tests "com.modularmc.ten.common.item.upgrades.UpgradeMultiplicativeStackTest" --tests "com.modularmc.ten.common.item.upgrades.BatchFactorTest" --tests "com.modularmc.ten.common.item.upgrades.PhotosynEffectTest" --console=plain`（完整显式列举，避免通配遗漏；仍完整输出/300s硬超时） |
| **DoD** | ① 所有当前行为快照测试 GREEN；② 所有新行为断言 RED（因生产代码未改）；③ 产出测试运行日志作为基线证据 |
| **回滚/失败处理** | 若基线快照 RED，说明源码现状与计划假设不符，暂停并上报指挥官 |
| **禁止项** | 不修改生产代码；不运行全量 `test`（只 P0 范围） |
| **执行代理** | 米娅 |
| **审查门禁** | 艾琳审查测试日志 + 确认 RED 断言语义正确 |

---

### P1：处理机 tick 语义重构（GREEN 阶段）

**阶段目标**：将 `ProcessingMachineBlockEntity.process()` 从能量累计改为固定 FE/t 消耗、tick 计数模型。同时改造 `CmMachineBlockEntity` 基础辅助方法。

**阶段依赖**：P0-T8（P0 GREEN 基线通过）→ P1-T1（核心 process）→ P1-T2（CmMachine 辅助）→ P1-T3（子类验证），可并行 P1-T3 中独立子类

**允许并行**：P1-T3 各子类验证可并行执行

---

#### P1-T1：重写 ProcessingMachineBlockEntity.process() 核心 tick 语义

| 字段 | 值 |
|------|-----|
| **任务ID** | `P1-T1` |
| **目标** | 将 `process()` 从能量累计变更为固定 FE/t 消耗、tick 计数。完成条件从 `progress > maxProgress` 改为 `progress >= maxProgress`。能量不足/输出满时暂停不重置 progress |
| **目标文件** | `src/main/java/.../api/blockentity/ProcessingMachineBlockEntity.java` |
| **核心符号** | `process()`、`progress`、`maxProgress`、`getActualEfficiency()`、`energyStorage.extractEnergy()`、`cooking()` |
| **前置依赖** | P0-T1（ProcessingMachineContractTest RED 就绪） |
| **测试先行** | P0-T1 中 RED 断言：`progress >= maxProgress`、能量不足暂停不重置、输出满暂停不重置、maxProgress 固定、FE/t 受 upgrade 影响、duration >= 1 |
| **实现动作** | ① 重写 `process()`：移除 `progress += energyConsumed`；移除 `progress = 0` else 分支；添加 Syn 注能 `tryInjectPhotosynEnergy()` 在停滞判断前执行；添加 `fePerTick = getActualEfficiency()` 检查；`energyStorage.extractEnergy(fePerTick, false)`；`progress++`；`if (progress >= maxProgress) { onCookFinish(); progress=0; }`；② 保留 `conditionStart()` 返回 false 时不清零 |
| **验证命令** | `gradlew.bat test --tests "com.modularmc.ten.api.blockentity.ProcessingMachineContractTest" --console=plain`（预期全 GREEN） |
| **DoD** | ① 完成条件 `>= maxProgress`；② 能量不足时暂停不重置；③ 输出满时暂停不重置；④ maxProgress 不在 process 中修改；⑤ P0-T1 RED → GREEN；⑥ Syn 注能 `tryInjectPhotosynEnergy()` 在停滞判断前执行 |
| **回滚/失败处理** | `git checkout -- src/main/java/.../ProcessingMachineBlockEntity.java` 回退；确认测试回退后仍为 RED |
| **禁止项** | 不改 `conditionStart()` 实现；不改 tick 调度入口；不改子类 |
| **执行代理** | 米娅 |
| **审查门禁** | 艾琳：验证 process() 伪代码与实现一致 |

---

#### P1-T2：CmMachineBlockEntity 辅助方法 + energyAllowRun 调整

| 字段 | 值 |
|------|-----|
| **任务ID** | `P1-T2` |
| **目标** | 新增 `applyUpgradeEffects()` 中 durationMultiplier/powerMultiplier 字段声明与计算辅助；`energyAllowRun()` 调整；`onUpgradeApply()` 初步重构 |
| **目标文件** | `src/main/java/.../api/blockentity/CmMachineBlockEntity.java` |
| **核心符号** | `energyAllowRun()`、`onUpgradeApply()`、`applyUpgradeEffects()`、`durationMultiplier`、`powerMultiplier`、`batch` |
| **前置依赖** | P1-T1（需要 process 新语义验证 FE/t 传递） |
| **测试先行** | P0-T3（UpgradeMultiplicativeStackTest）验证纯函数公式；P0-T1 验证 FE/t 传递到 process |
| **实现动作** | ① `energyAllowRun()` 处理机检查从 `>= efficientIn` 改为 `>= getActualEfficiency()`；② 在 `CmMachineBlockEntity` 声明 `private double durationMultiplier = 1.0, powerMultiplier = 1.0; private int batch = 0; private boolean photosynInstalled = false;`；③ `applyUpgradeEffects()` 中重置后计算乘法值（此时四个子类仍调旧 `effect()`，方法体准备就绪但 effect() 尚未改——先让旧加法逻辑继续工作）；④ `onUpgradeApply()` 保留旧加法逻辑但标记 `@deprecated` |
| **验证命令** | `gradlew.bat compileJava --console=plain` + `gradlew.bat test --tests "com.modularmc.ten.api.blockentity.ProcessingMachineContractTest" --console=plain` |
| **DoD** | ① 编译通过；② 所有 P0 GREEN 基线仍 GREEN（加法模型未破坏）；③ 新增字段在 `applyUpgradeEffects()` 中正确重置 |
| **回滚/失败处理** | `git checkout -- src/main/java/.../CmMachineBlockEntity.java` |
| **禁止项** | 不改 `effect()` 方法；不改 Levelup*/IUpgradableMachine |
| **执行代理** | 米娅 |
| **审查门禁** | 艾琳：确认字段声明不破坏现有序列化 |

---

#### P1-T3：子类 conditionStart 统一 + Furnace cookingTime 修正

| 字段 | 值 |
|------|-----|
| **任务ID** | `P1-T3` |
| **目标** | 确保所有 RecipeMachine 子类在 `conditionStart()` 中设置 `maxProgress = baseTickTime()`；FurnaceBlockEntity 移除 `/2` |
| **目标文件** | `FurnaceBlockEntity.java`、`PulverizerBlockEntity.java`、`CompressorBlockEntity.java`、`RefinerBlockEntity.java`、`IndfurBlockEntity.java`、`PsionicantBlockEntity.java`、`CondenserBlockEntity.java`、`EncfluBlockEntity.java`、`QuarryBlockEntity.java`、`FarmBlockEntity.java`、`MobRipBlockEntity.java` |
| **核心符号** | `conditionStart()`、`baseTickTime()`、`maxProgress` |
| **前置依赖** | P1-T1（process 使用 maxProgress） |
| **测试先行** | P0-T1/P0-T5 验证 maxProgress 在 conditionStart 固定 |
| **实现动作** | ① 逐个验证各子类 `conditionStart()` 设置 `maxProgress = baseTickTime()`；② FurnaceBlockEntity：`baseTickTime()` 从 `cookingTime() / 2` 改为 `cookingTime()`；③ Quarry/Farm/MobRip：确认特殊逻辑不依赖旧/2 行为 |
| **验证命令** | `gradlew.bat test --tests "com.modularmc.ten.api.blockentity.ProcessingMachineContractTest" --console=plain` + `gradlew.bat compileJava --console=plain` |
| **DoD** | ① 所有 RecipeMachine 子类 conditionStart 统一；② Furnace cookingTime 移除 `/2`；③ 特殊机器（Quarry/Farm/MobRip）未受影响；④ P0 所有测试仍 GREEN |
| **回滚/失败处理** | 单文件 `git checkout` 回退；Furnace /2 回退需确认 |
| **禁止项** | 不改 EffectMachine（归 P3）；不改子类 process() |
| **执行代理** | 米娅 |
| **审查门禁** | 艾琳：确认附录 A 继承链与实际一致 |

---

#### P1-T3a：Furnace batch — B 份输入消费 + B 份产出

| 字段 | 值 |
|------|-----|
| **任务ID** | `P1-T3a` |
| **目标** | FurnaceBlockEntity（直接继承 ProcessingMachine）独立实现 batch 逻辑：按最大可行 B 从输入槽提取 B 份输入，组装 B 份结果至输出槽。B_actual 受输入可用量 / 输出槽容量约束。不得只多耗能无收益 |
| **目标文件** | `src/main/java/.../common/blockentity/machine/FurnaceBlockEntity.java` |
| **核心符号** | `conditionStart()` → `B_actual` 计算（B_byInputItems = floor(inputCount/B_inputPerUnit), B_byOutputSlot = floor(availOutputSlot / resultCount)）；`cooking()` 满槽判断；`onCookFinish()` B 次消费/产出循环；Syn 注能 `tryInjectPhotosynEnergy()` |
| **前置依赖** | P1-T3（conditionStart/cookingTime 修正）、P2-T1（getLockedB 存在） |
| **测试先行** | P0-T1/P0-T6 扩展：新增 Furnace B=2/B=4 batch 输入/产出测试；B_actual 受输入/输出约束 RED |
| **实现动作** | ① `conditionStart()` 中计算 `B_actual`：`B_byInput = Math.min(B_theory, inputCount)`（输入槽单品栈上限约束）；`B_byOutput = floor(availOutputSlotCount / recipeResultCount)`；`B_actual = Math.min(B_byInput, B_byOutput)`；`if (B_actual < 1) cancelStart()`。② `cooking()`：检查 output 槽能否容纳 B×result；满槽暂停。③ `onCookFinish()`：for(i=0; i<B_actual; i++) { 消费 input 1 份; 生成 output 1 份; }（每份独立检查 input 是否耗尽）。④ `totalFePerTick = Math.round(baseFePerTick × B_actual)`。⑤ Syn 注能 `tryInjectPhotosynEnergy()` 在停滞判断前。⑥ `baseFePerTick` 最小 1 守卫已由 P2-T1 efficientIn 生命周期保证 |
| **验证命令** | `gradlew.bat test --tests "com.modularmc.ten.api.blockentity.ProcessingMachineContractTest" --console=plain` |
| **DoD** | ① B_actual 受输入/输出双维约束；② B<1 不启动；③ B 份 input 消费 + B 份 output 产出；④ 中间 input 耗尽提前结束；⑤ 满槽暂停不丢进度；⑥ FE/t×B 消耗正确；⑦ Syn 注能正确 |
| **禁止项** | 不共用 InputConsumptionPlan（非 RecipeMachine）；不改 vanilla RecipeType 查询逻辑 |
| **执行代理** | 米娅 |
| **审查门禁** | 艾琳：验证 B_actual 约束 + 消费/产出循环原子性 + 无收益消耗禁止 |

---

#### P1-T3b：Condenser batch — 重写 cooking + 输出 5mB×B

| 字段 | 值 |
|------|-----|
| **任务ID** | `P1-T3b` |
| **目标** | CondenserBlockEntity（直接继承 ProcessingMachine）保留催化剂单份/既有周期消耗语义，重写旧能量累加 cooking 为新 tick 计数模型。完成时输出 5mB×B 并按 tank 可用容量反算 B。旧 `cooking()` 中 `progress += 200 * getActualEfficiency()` 和输出满 `progress = 0` 必须删除/重写 |
| **目标文件** | `src/main/java/.../common/blockentity/machine/CondenserBlockEntity.java` |
| **核心符号** | `baseTickTime()`=1000（硬编码）；`conditionStart()` 中 B_actual 计算（`B_byTank = floor(availTankCapacity / 5mB)`，`B_byInput = catalyst 存在 ? B_theory : 0`）；`cooking()` 满罐暂停；`onCookFinish()` 输出 5mB×B；Syn 注能 `tryInjectPhotosynEnergy()` |
| **前置依赖** | P1-T3（conditionStart 统一）、P2-T1（getLockedB 存在） |
| **测试先行** | P0-T1/P0-T6 扩展：新增 Condenser B 受 tank 容量约束测试；旧 energy-accumulate 语义 RED（应有对应快照验证）；新 tick 计数 GREEN；5mB×B 输出正确 |
| **实现动作** | ① `conditionStart()` 中：`int B_byInput = itemHandler.getStackInSlot(0).isEmpty() ? 0 : B_theory;`（催化剂存在即有 1 份）；`int B_byTank = tanks.get(0).getCapacity() - tanks.get(0).getFluidAmount()`；`B_actual = Math.min(B_theory, B_byInput, B_byTank / 5)`（每 mB 单位需 5mB 空间）；`if(B_actual<1) cancelStart()`。② 完全**重写 `cooking()`**：移除 `progress += 200 * getActualEfficiency()`；改为满罐检查（新语义输出满停滞不清零，`cooking()` 返回 true 暂停）。③ 完全**重写 `onCookFinish()`**：输出 `new FluidStack(TENFluids.LIQUID_BIZARRERIE_SOURCE.get(), 5 * B_actual)`，SIMULATE 验证后 EXECUTE。④ 催化剂消耗：每周期消耗 1 份（不×B），在 `cooking()` 或 `onCookFinish()` 中 shrink。⑤ `totalFePerTick = Math.round(baseFePerTick × B_actual)`。⑥ Syn 注能 `tryInjectPhotosynEnergy()` 在停滞判断前 |
| **验证命令** | `gradlew.bat test --tests "com.modularmc.ten.api.blockentity.ProcessingMachineContractTest" --console=plain` |
| **DoD** | ① 旧 `progress += 200*efficiency` 完全删除，不残留；② 旧输出满 `progress=0` 重置完全删除，改为停滞不清零；③ catalyst 每周期消耗 1 份（不×B）；④ 输出 5mB×B_actual；⑤ B_actual 受 tank 实际可用容量反算；⑥ B<1 不启动；⑦ FE/t×B 消耗正确；⑧ Syn 注能正确 |
| **禁止项** | 不共用 InputConsumptionPlan；不虚构催化剂 B 倍消耗；不改 tank 扩容 API；不保留旧 energy-accumulate 代码 |
| **执行代理** | 米娅 |
| **审查门禁** | 艾琳：重点审查旧 `progress += 200*efficiency` 残留清理 + 输出满不清零语义 + catalyst 单份周期语义 |

---

#### P1-T3c：Encflu batch — B 组独立安全转移/回滚

| 字段 | 值 |
|------|-----|
| **任务ID** | `P1-T3c` |
| **目标** | EncfluBlockEntity（直接继承 ProcessingMachine）按可行输入组数执行最多 B 组，每组独立安全转移/回滚、产出和 XP fluid。受单槽物品栈、工具不可堆叠、输出槽/tank 容量约束，B_actual 可因此降至 1。不得虚构 FormsIngredient chance 字段 |
| **目标文件** | `src/main/java/.../common/blockentity/machine/EncfluBlockEntity.java` |
| **核心符号** | `baseTickTime()`=800（硬编码）；`conditionStart()` B_actual 计算（`B_byToolSlots = 1`（工具不可堆叠+仅 1 槽输入）→ B_actual 受输出槽/tank 约束）；`cooking()` 满槽/满罐暂停；`onCookFinish()` B 组循环；现有安全转移/回滚扩展到 B 次 |
| **前置依赖** | P1-T3（conditionStart 统一）、P2-T1（getLockedB 存在） |
| **测试先行** | P0-T1/P0-T6 扩展：新增 Encflu B=1（工具不可堆叠）默认值测试；输出槽满时 B=1 不启动；tank 满时暂停 |
| **实现动作** | ① `conditionStart()` 中：`B_byTool = 1`（slot0 工具+slot1 目标+slot2 输出，工具不可堆叠+单槽输入→B 受输入组数天然限制为 1）；`B_byOutput = outputSlot.isEmpty() ? B_theory : 0`；`B_byTank = floor(availTankCapacity / xpPerGroup)`；`B_actual = Math.min(B_theory, B_byTool, B_byOutput, B_byTank)`。② `cooking()`：满槽/满罐暂停。③ `onCookFinish()`：将现有单组安全转移/回滚逻辑包装为可迭代操作；for(i=0; i<B_actual; i++) 每次独立：读取 tool/target 快照 → 验证 → 构建输出 → SIMULATE tank → 保存回滚快照 → EXECUTE → 捕获异常回滚。④ 每组消费按实际源码定义（tool 消耗 enchantment、target shrink 1），不虚构 `FormsIngredient chance` 字段。⑤ `totalFePerTick = Math.round(baseFePerTick × B_actual)`。⑥ Syn 注能 `tryInjectPhotosynEnergy()` 在停滞判断前 |
| **验证命令** | `gradlew.bat test --tests "com.modularmc.ten.api.blockentity.ProcessingMachineContractTest" --console=plain` |
| **DoD** | ① B_actual 受 tool 不可堆叠约束默认 1 但理论上限可由 B_byTank/B_byOutput 进一步限制；② 每组独立安全转移/回滚；③ 异常回滚不破坏物品/流体状态；④ XP fluid 按组累加；⑤ 不虚构 FormsIngredient chance 字段；⑥ B=1 时不退化为无 batch 行为差异（仅验证逻辑正确）；⑦ Syn 注能正确 |
| **禁止项** | 不共用 InputConsumptionPlan；不虚构 FormsIngredient 字段；不改 Encflu 现有安全转移 API（仅扩展为 B 次循环） |
| **执行代理** | 米娅 |
| **审查门禁** | 艾琳：重点验证 B 组循环安全转移/回滚原子性 + 工具不可堆叠约束 + XP fluid 累加正确 |

---

### P2：四款升级效果改造 + 批处理正式实现（GREEN 阶段）

**阶段目标**：将四款功率升级从加法改为乘法 duration/power + batch 加法模型。批处理正式实现：B 因子联动 I/O FE/t、能量上限、物品/流体槽位。同步提取 SolarBlockEntity 共享静态方法。

**设计约束**（保留 v1.1 设计细节，任务中引用不重复）：
- 四款升级映射、B 因子公式、`onCookFinish()` 落点、概率产物 rolls 复用、防越界 B_actual 锁定、新 `effect()` 改造方案、旧加法清理范围——均见「设计附录：P2 详细设计」。各任务实现时参考该附录，不再逐项重复伪代码。
- `powerMultiplier` 已含 Syn×0.8（非外层项）。
- 不改动文件清单：`TENItems.java` 注册行 141-144、所有模型 JSON、语言键、现有配方 JSON、`TENLangHandler.java`、存档中 `percent` 字段名。

**阶段依赖**：P1（需要新 process 语义才能验证 FE/t、duration、batch 效果）

**阶段内顺序**：P2-T1（CmMachine 乘法方法）→ P2-T2~P2-T5（四款 upgrade effect 改造，可并行）→ P2-T6（批处理联动）→ P2-T7（太阳能提取，可并行于 P2-T2~P2-T5）→ P2-T8（极端倍率/边界验证，依赖 P2-T6）

**允许并行**：P2-T2/T3/T4/T5（四款升级 effect）可并行；P2-T7 可并行于 P2-T2~T5

---

#### P2-T1：CmMachineBlockEntity 乘法方法 + 批处理字段

| 字段 | 值 |
|------|-----|
| **任务ID** | `P2-T1` |
| **目标** | 新增 `applyDurationReduction()`、`applyPowerIncrease()`、`applyBatch()`、`applyPhotosyn()`、`isPhotosynInstalled()`、`getBatchFactor()` 方法；改写 `applyUpgradeEffects()` 进入乘法计算模式；支持同类型可重复安装（每栈1/6槽，不限制同类型数量） |
| **目标文件** | `src/main/java/.../api/blockentity/CmMachineBlockEntity.java` |
| **核心符号** | `applyDurationReduction(double)`、`applyPowerIncrease(double)`、`applyBatch(int)`、`applyPhotosyn()`、`isPhotosynInstalled()`、`getBatchFactor()`、`applyUpgradeEffects()` |
| **前置依赖** | P1-T2（字段声明 + 框架） |
| **测试先行** | P0-T3（UpgradeMultiplicativeStackTest 纯函数验证）；P0-T4（BatchFactorTest 纯函数验证） |
| **实现动作** | ① 新增 public setter 方法；② `applyUpgradeEffects()` 改为：`durationMultiplier=1.0; powerMultiplier=1.0; batch=0; photosynInstalled=false;` 然后遍历升级——此时升级 `effect()` 仍为旧实现，遍历后**暂不生效**；③ `getBatchFactor()` 返回 `1 + batch`；④ 旧 `onUpgradeApply()` 标记 `@Deprecated` 保留向前兼容；**⑤ 在 `doBaseData()` 或统一升级重算落点中，在 `applyUpgradeEffects()` 完成后立即计算 `efficientIn = Math.max(1, Math.round(initialEfficientIn * powerMultiplier))`（最小 1 守卫），再赋值 `effAuc = efficientIn`，使 `getActualEfficiency()` 返回含升级但不含 batch 的 baseFePerTick**；⑥ **同步 `resetUpgradeEffects()` 生命周期：在重置时恢复 efficientIn = initialEfficientIn、effAuc = initialEfficientIn，避免每 tick 复乘漂移——`doBaseData()` 每 tick 按 reset→apply→consume 顺序执行，保证 powerMultiplier 仅每 tick 重算一次，不累计** |
| **验证命令** | `gradlew.bat compileJava --console=plain` |
| **DoD** | ① 新方法编译通过；② `applyUpgradeEffects()` 进入乘法框架但不破坏加法基线；③ 所有 P0 测试仍 GREEN（因 effect() 未改，实际乘法未激活）；④ `efficientIn` = `Math.max(1, Math.round(initialEfficientIn × powerMultiplier))` 在 `applyUpgradeEffects()` 后计算；⑤ `effAuc = efficientIn` 赋值；⑥ `resetUpgradeEffects()` 恢复 efficientIn = initialEfficientIn；⑦ `doBaseData()` 按 reset→apply→consume 顺序保证每 tick 单次重算，无累计漂移 |
| **禁止项** | 不改 Levelup* 类；不改 process() 批处理联动（归 P2-T6） |
| **执行代理** | 米娅 |
| **审查门禁** | 艾琳：验证方法签名与设计附录一致 |

---

#### P2-T2：LevelupAug effect() 改造

| 字段 | 值 |
|------|-----|
| **任务ID** | `P2-T2` |
| **目标** | 将 `LevelupAug.effect()` 改为调 `applyDurationReduction(0.25)` + `applyPowerIncrease(0.30)` |
| **目标文件** | `src/main/java/.../common/item/upgrades/LevelupAug.java` |
| **核心符号** | `effect(IUpgradableMachine)`、`applyDurationReduction`、`applyPowerIncrease` |
| **前置依赖** | P2-T1（方法存在） |
| **测试先行** | P0-T3 中 `levelupAug_duration`、`levelupAug_power`、`levelupAug_batch` 应转 GREEN |
| **实现动作** | 将 `effect()` 从 `machine.onUpgradeApply(percent, 1)` 改为 `if (machine instanceof CmMachineBlockEntity cm) { cm.applyDurationReduction(0.25); cm.applyPowerIncrease(0.30); return true; }` |
| **验证命令** | `gradlew.bat test --tests "com.modularmc.ten.common.item.upgrades.UpgradeMultiplicativeStackTest" --console=plain`（部分 GREEN） |
| **DoD** | Aug 乘法数值测试 GREEN；旧加法基线快照应 RED（因 effect() 已改） |
| **禁止项** | 不改注册 ID、配方、模型、语言键；不改 canApply() |
| **执行代理** | 米娅 |
| **审查门禁** | 艾琳 |

---

#### P2-T3：LevelupPower effect() 改造

| 字段 | 值 |
|------|-----|
| **任务ID** | `P2-T3` |
| **目标** | 将 `LevelupPower.effect()` 改为调 `applyDurationReduction(0.40)` + `applyPowerIncrease(0.50)` + `applyBatch(1)` |
| **目标文件** | `src/main/java/.../common/item/upgrades/LevelupPower.java` |
| **前置依赖** | P2-T1 |
| **测试先行** | P0-T3/T4 中 Power 相关测试 |
| **实现动作** | 同 P2-T2 模式 + `cm.applyBatch(1)` |
| **验证命令** | `gradlew.bat test --tests "com.modularmc.ten.common.item.upgrades.*" --console=plain` |
| **禁止项** | 同 P2-T2 |
| **执行代理** | 米娅 |
| **审查门禁** | 艾琳 |

---

#### P2-T4：LevelupShulker effect() 改造

| 字段 | 值 |
|------|-----|
| **任务ID** | `P2-T4` |
| **目标** | 将 `LevelupShulker.effect()` 改为调 `applyDurationReduction(0.60)` + `applyPowerIncrease(1.00)` + `applyBatch(3)` |
| **目标文件** | `src/main/java/.../common/item/upgrades/LevelupShulker.java` |
| **前置依赖** | P2-T1 |
| **测试先行** | P0-T3/T4 中 Shulker 相关测试 |
| **实现动作** | 同 P2-T3 模式 + `cm.applyBatch(3)` |
| **验证命令** | `gradlew.bat test --tests "com.modularmc.ten.common.item.upgrades.*" --console=plain` |
| **禁止项** | 同 P2-T2 |
| **执行代理** | 米娅 |
| **审查门禁** | 艾琳 |

---

#### P2-T5：LevelupSyn effect() 改造

| 字段 | 值 |
|------|-----|
| **任务ID** | `P2-T5` |
| **目标** | 将 `LevelupSyn.effect()` 改为光合独立倍率：`applyPhotosyn()`。上限 1。严格复用现有 `canApply()` 身份检查（仅 PROCESS+EFFECT） |
| **目标文件** | `src/main/java/.../common/item/upgrades/LevelupSyn.java` |
| **核心符号** | `effect()`、`canApply()`、`applyPhotosyn()`、`isPhotosynInstalled()` |
| **前置依赖** | P2-T1 |
| **测试先行** | P0-T7（PhotosynEffectTest）全部应转 GREEN |
| **实现动作** | ① `effect()` 中先调 `canApply()` 检查（现有身份不变）；② `if (cm.isPhotosynInstalled()) return false;` 上限 1；③ `cm.applyPhotosyn()`；④ 移除旧 `onUpgradeApply(-0.1, 1)` 调用；**⑤ 在 P2-T7 提取的共享光照判定和公共注入方法就绪后，Syn 注能逻辑通过 `tryInjectPhotosynEnergy()`（见 P2-T7）在 ProcessingMachine/EffectMachine process() 中调用，不在此任务实现** |
| **验证命令** | `gradlew.bat test --tests "com.modularmc.ten.common.item.upgrades.PhotosynEffectTest" --console=plain` |
| **DoD** | ① `canApply()` 仅限 PROCESS+EFFECT（读源码确认）；② 安装上限 1 生效；③ Syn 0.8 纳入 powerMultiplier 链（非外层项）；④ 无光不失倍率（纯数学验证） |
| **禁止项** | 不泛化 canApply() 到任意 CmMachine；不改注册/配方/模型/语言键 |
| **执行代理** | 米娅 |
| **审查门禁** | 艾琳：重点验证 canApply 身份未泛化 |

---

#### P2-T6：批处理数据模型联动 — 标准 RecipeMachine 路径（process + onCookFinish via InputConsumptionPlan）

> **本任务仅覆盖 5 个标准 RecipeMachine（Pulverizer/Compressor/Refiner/Indfur/Psionicant）的 batch 路径**。3 个直接 ProcessingMachine 子类（Furnace/Condenser/Encflu）的 batch 已在 P1-T3a/T3b/T3c 中各自独立实现。3 个 RadiusMachine 子类（Quarry/Farm/MobRip）的 effect-level batch 已在 P3-T1c 中实现。

| 字段 | 值 |
|------|-----|
| **任务ID** | `P2-T6` |
| **目标** | 在 `ProcessingMachineBlockEntity.process()` 中联动批处理：FE/t×B、progress+1。在 `RecipeMachineBlockEntity.onCookFinish()` 中通过 `InputConsumptionPlan` 实现物品/流体/输出 B 倍逻辑。实现 B_actual 四维锁定与运行停滞语义，概率输出最坏容量预判（item/fluid 分别聚合），取整统一规则。**本任务仅涉及标准 RecipeMachine 的 InputConsumptionPlan ×B 路径** |
| **目标文件** | `src/main/java/.../api/blockentity/ProcessingMachineBlockEntity.java`、`RecipeMachineBlockEntity.java`、`CmMachineBlockEntity.java` |
| **核心符号** | `process()` 中 `int B_actual = getLockedB()`、`baseFePerTick = getActualEfficiency()`（含升级不含B）、`totalFePerTick = Math.round(baseFePerTick * B_actual)`；`RecipeMachineBlockEntity.onCookFinish()` 中：**通过 `InputConsumptionPlan` 处理输入消耗**（按 B 倍调整 plan 中 inputCount）、通过 `currentRecipe.generateItems()`/`generateFluids()` 输出（概率产物已在方法内部处理 rolls×B）；**ProcessingMachineBlockEntity 基类的空 onCookFinish() 不应承载配方 I/O 逻辑**——实际配方的输入消费计划、输出生成与放置均在 RecipeMachineBlockEntity 层面完成。B_actual 四维锁定（输入/输出/流体/储能）；概率最坏容量预判（按每 output slot/tank 聚合 perBatchUnitWorst 反算 B）；运行停滞语义 |
| **前置依赖** | P1-T1（process 新语义）、P2-T1（getBatchFactor 存在 + efficientIn 生命周期）、P2-T2~T5（升级 effect 改造使批处理实际激活） |
| **测试先行** | P0-T6（BatchProcessingContractTest）全部应转 GREEN；新增 B_max=19 溢出测试、6×Shulker 测试、B_actual 四维锁定测试、概率最坏容量测试（每槽聚合）、停滞语义测试 |
| **实现动作** | ① `process()`：`int B = getLockedB(); int totalFePerTick = Math.round(getActualEfficiency() * B); if (energyStored < totalFePerTick) return; /*停滞*/ energyStorage.extractEnergy(totalFePerTick, false); progress++;`；② `conditionStart()` 中四维锁定 B_actual（输入/输出/流体/当前 energyStored 除 baseFePerTick）；B<1 不启动；③ `RecipeMachineBlockEntity.onCookFinish()`：修改 `InputConsumptionPlan.build()` 中 item/fluid inputCount 为 ×B（chance()≤0 的 catalyst 保持 1 份）；`currentRecipe.generateItems()`/`generateFluids()` 内部已封装概率产物 B×rolls 独立试验逻辑——若尚未内置 B 循环则在 `onCookFinish()` 外层 for(i=0;i<B;i++) 循环调用 generateItems/generateFluids；④ 概率输出容量预判：对每个 output slot/tank 计算 perBatchUnitWorst（不含B），用 `B_byProbWorst = floor(availSlotCapacity / perBatchUnitWorst_slot)` 反算，取全局 min；⑤ I/O FE/t→Math.round、容量/吞吐→(int)截断+钳位、ticks→Math.ceil+Math.max(1)；⑥ **ProcessingMachineBlockEntity 的 `onCookFinish()` 保持空方法体，不承载 I/O；recipe I/O 全部在 `RecipeMachineBlockEntity.onCookFinish()` 内** |
| **验证命令** | `gradlew.bat test --tests "com.modularmc.ten.api.blockentity.BatchProcessingContractTest" --console=plain` |
| **DoD** | ① FE/t×B 使用 Math.round 消耗正确；② progress+1 不×B；③ 确定性输出 ×B；④ 概率产物 B×rolls 独立试验，chance 保持原值；⑤ `chance()≤0` 催化剂 1 份；⑥ 流体全部 B 倍；⑦ 满槽/能量不足暂停不丢进度；⑧ B_actual 四维锁定（含概率最坏容量按每 slot/tank 聚合）；⑨ B<1 不启动；⑩ 运行停滞语义（不扣能/不加 progress/maxProgress 不变）；⑪ 极端 B=19（6×Shulker）不溢出；⑫ 取整规则全部正确；⑬ **ProcessingMachineBlockEntity.onCookFinish() 为空，I/O 逻辑在 RecipeMachineBlockEntity** |
| **禁止项** | 不直接写 random chance 循环（必须通过 genItem/genFluid）；不虚构流体 consume 字段；不虚构 fluid tank 扩容 API；不让 ProcessingMachineBlockEntity 基类的 onCookFinish 承载配方 I/O |
| **执行代理** | 米娅 |
| **审查门禁** | 艾琳：重点审查概率产物 rolls 复用逻辑 + B_actual 四维锁定 + 概率最坏容量每槽聚合预判 + 停滞语义 + RecipeMachineBlockEntity I/O 落点正确 |

---

#### P2-T7：光照判定共享静态方法 + 昼间 API 解冻 + Syn 注能公共 helper

| 字段 | 值 |
|------|-----|
| **任务ID** | `P2-T7` |
| **目标** | 从 `SolarBlockEntity` 提取公共静态光照判定方法供 `LevelupSyn` 光照判定复用；**同时解冻当前的昼间判定 TODO**；**新增 `tryInjectPhotosynEnergy()` 公共 helper 方法供 ProcessingMachine/EffectMachine process() 调用** |
| **目标文件** | `src/main/java/.../common/blockentity/machine/SolarBlockEntity.java`（提取+更新）；`UpgradeMathHelper.java` 或 `CmMachineBlockEntity.java`（可选落点） |
| **核心符号** | `isValidDaytime(Level, BlockPos)` — 返回 boolean 用于 Syn 判定；昼间 API：`level.getDayTime() % 24000L < 12000L`；**`tryInjectPhotosynEnergy()` — 共享注入方法，在 process() 停滞判断前调用，避免两份分叉** |
| **前置依赖** | P2-T1（辅助框架完成） |
| **实现动作** | ① 分析 `SolarBlockEntity.matchFuel()` 中光照计算逻辑；② 提取 `isValidDaytime(Level level, BlockPos pos)` 静态方法：`return level.canSeeSky(pos.above()) && !level.isRaining() && level.getDayTime() % 24000L < 12000L;`；③ 更新 `SolarBlockEntity.matchFuel()` 改为调此静态方法（保留向后兼容）；④ 更新 `SolarBlockEntity` 中 TODO 注释为已实现状态；**⑤ 在 CmMachineBlockEntity 或公共 helper 中声明 `protected void tryInjectPhotosynEnergy()`：if(!isPhotosynInstalled()||!isValidDaytime(level,pos)) return; energyStorage.receiveEnergy(10,false);——满储自然丢弃；⑥ 将此方法调用位置记入 ProcessingMachine/EffectMachine 的 process() 伪代码中，停滞判断前执行** |
| **验证命令** | `gradlew.bat compileJava --console=plain` |
| **DoD** | ① `isValidDaytime` 静态方法存在且可测试；② `matchFuel()` 调用它，行为无回归；③ SolarBlockEntity 的昼间 TODO 注释已移除/标记为已实现；④ 昼间 API 使用 `level.getDayTime() % 24000L < 12000L`（已验证在 26.1.2 可用）；**⑤ `tryInjectPhotosynEnergy()` 公共方法存在，被 process() 引用；⑥ 不乘 B/倍率，不外送；⑦ 满储时 receiveEnergy 自然丢弃** |
| **禁止项** | 不修改太阳能发电数值；不复制第二套光照算法；不引入对 ServerLevel 的强依赖（客户端兼容）；不在 LevelupSyn.effect() 中直接实现注能逻辑 |
| **执行代理** | 米娅 |
| **审查门禁** | 艾琳 |

---

#### P2-T8：极端倍率 + 边界合约验证

| 字段 | 值 |
|------|-----|
| **任务ID** | `P2-T8` |
| **目标** | 集成验证：极端组合（6×Shulker+Syn）不溢出、duration>=1 tick、B_max=19 FE/t 不超载、满槽防越界正确、概率最坏容量预判正确、取整统一正确 |
| **目标文件** | 全部 P2 修改文件 |
| **核心符号** | `powerMultiplier` 上限、`maxStorageEnergy`、`maxExtractEnergy`、`Math.round`/`Math.ceil`/钳位 |
| **前置依赖** | P2-T6（批处理激活） |
| **实现动作** | ① 确保 `applyPowerIncrease` 中 clamp 不溢出；② `maxProgress = Math.max(1, ...)` 硬保证；③ 所有 ×B 容量使用 `(int)Math.min(raw, Integer.MAX_VALUE/2)`；④ FE/t 使用 `Math.round`；⑤ 测试验证极端组合（6×Shulker、B_max=19、3×Shulker+Syn）全部 GREEN；⑥ 概率最坏容量（item/fluid 分别）不越界 |
| **验证命令** | `gradlew.bat test --tests "com.modularmc.ten.common.item.upgrades.*" --console=plain` + `gradlew.bat test --tests "com.modularmc.ten.api.blockentity.*" --console=plain` |
| **DoD** | ① 6×Shulker+Syn 仍不溢出；② duration>=1；③ B_max=19 全倍率不超 maxExtractEnergy；④ 满槽 B_actual 降级正确；⑤ 概率最坏容量预判不越界；⑥ Math.round/截断/钳位/ceil/1-tick 全部正确 |
| **禁止项** | 不改注册/模型/配方/语言键 |
| **执行代理** | 米娅 |
| **审查门禁** | 艾琳 |

---

> **设计附录：P2 详细设计（v1.4 更新——任务实现时引用）**
>
> 以下要点从 v1.1 平移保留并更新。完整伪代码见原 v1.1 P2 节。各任务实现时引用此处，避免重复大段代码。
>
> **1. 升级映射表**：
> - Aug: reduction=0.25, increase=0.30, batch=0
> - Power: reduction=0.40, increase=0.50, batch=1
> - Shulker: reduction=0.60, increase=1.00, batch=3
> - Syn: duration×1.5, power×0.8（纳入 powerMultiplier 链）, batch=0
> - **同类型可重复安装**：仅 Syn 上限 1（`isPhotosynInstalled()` 守卫）；Aug/Power/Shulker 不限重复
>
> **2. B 因子计算**：`B = 1 + Σbatch_i`（P2-T1 的 getBatchFactor()）；理论极值 6×Shulker → B_max=19
>
> **3. B_actual 四维锁定（启动时）**：
> ```java
> int B_actual = B_theory;
> // 维度1-物品输入：检查可用物品槽能否容纳 B×inputCount
> B_actual = Math.min(B_actual, maxBByInputItems);
> // 维度2-流体输入：检查可用流体槽能否容纳 B×fluidInputCount
> B_actual = Math.min(B_actual, maxBByInputFluids);
> // 维度3-输出容量：确定性输出×B + 概率输出最坏预设（按perBatchUnitWorst每slot/tank聚合反算，不含B）
> B_actual = Math.min(B_actual, maxBByOutputWorstCase);
> // 维度4-当前储能：仅启动时看 energyStored / baseFePerTick（baseFePerTick不含B，避免循环依赖）
> B_actual = Math.min(B_actual, energyStored / baseFePerTick);
> // 无可行 B 时不启动
> if (B_actual < 1) { cancelStart(); return; }
> // 固定，运行中不降 B
> this.lockedB = B_actual;
> ```
>
> **4. onCookFinish() 落点**：`RecipeMachineBlockEntity.onCookFinish()`（非 ProcessingMachineBlockEntity 基类的空方法）。ProcessingMachineBlockEntity 的空 onCookFinish 不承载任何配方 I/O。
>
> **5. 消耗品输入**：`chance()≤0`→催化剂 1 份；`chance()>0`→count×B；流体全部×B。输入消耗通过 `RecipeMachineBlockEntity.InputConsumptionPlan.build()` 调整 inputCount 实现。
>
> **6. 概率产物**：外层 `for(i=0;i<B;i++)` 套 `currentRecipe.generateItems()/generateFluids()`，每次内部复用 rolls+chance；实际 chance 保持原值，不预掷、不缓存、不改配方概率。
>
> **7. 概率输出容量预判（item/fluid 分别按 slot/tank 聚合）**：
> - 先定义 **perBatchUnitWorst = Σ(ingredient.rolls × maxAmount × maxCount)**（**不含B**），对每个 output slot 分别聚合；多个 ingredient 可产出同 item 时合并计算该 slot 的 perBatchUnitWorst
> - **B_byProbWorst_slot = floor(availableSlotCapacity / perBatchUnitWorst_slot)**
> - 对 fluid tank 同样：perBatchUnitWorst_fluid = Σ(rolls × maxAmount)，B_byProbWorst_tank = floor(tankCapacity / perBatchUnitWorst_fluid)
> - 最终 `B_byProbWorst = min(所有slot的B_byProbWorst_slot, 所有tank的B_byProbWorst_tank)`
> - `B_byOutput = min(B_byDeterministic, B_byProbWorst)`；启动时降低 B 至能容纳最坏输出的最大值；运行中不降 B
>
> **8. 运行停滞语义**：
> ```java
> // 每 tick process() 开头
> int baseFePerTick = getActualEfficiency();          // 含升级不含B
> int totalFePerTick = Math.round(baseFePerTick * lockedB);
> if (energyStored < totalFePerTick) {
>     // 停滞：不扣能、不加 progress、maxProgress 不变
>     return; // 墙钟时间延长
> }
> energyStorage.extractEnergy(totalFePerTick, false);
> progress++;
> ```
>
> **9. EffectMachine batch**：
> - 每 tick 能耗 `totalFePerTick`（含 B），progress+1
> - `maxProgress`/触发间隔不因 B 改变
> - 效果等级/范围不变
> - 每次应用的 `MobEffectInstance` 持续时间=基础持续时间×B（long→int 安全钳位）
> - 同 tick 不重复 apply B 次（只 apply 1 次但 duration×B）
>
> **10. new effect() 模型**：`applyDurationReduction/reduction`、`applyPowerIncrease/increase`、`applyBatch/add`、`applyPhotosyn()` 见 CmMachineBlockEntity 新增方法
>
> **11. efficientIn 生命周期（v1.4 新增，v1.5 补最小守卫）**：
> ```java
> // doBaseData() 每 tick 调用顺序：
> resetUpgradeEffects();    // 恢复 efficientIn = initialEfficientIn, effAuc = initialEfficientIn
> applyUpgradeEffects();    // 遍历升级，累积 powerMultiplier
> // 升级应用后立即重算 efficientIn = Math.max(1, Math.round(initialEfficientIn * powerMultiplier));
> // 最小 1 守卫：保证零功耗机器 baseFePerTick ≥ 1，避免 B_byEnergy 除零和 cancelStart 阻塞
> // effAuc = efficientIn;
> // 同步该值给 getActualEfficiency()，使其返回含升级不含 batch 的 baseFePerTick
> // 最终 process() 中再 × lockedB 得 totalFePerTick
> // 每 tick 严格按 reset→apply→consume 顺序执行，避免复乘漂移
> ```
>
> **12. 旧加法逻辑**：`onUpgradeApply()` 中 `efficientIn` 的加法不再需要；其余 7 参数加法保留
>
> **13. Syn 注能公共 helper**：
> ```java
> // 在 CmMachineBlockEntity 或公共 helper 类中声明共享方法
> protected void tryInjectPhotosynEnergy() {
>     if (!isPhotosynInstalled()) return;
>     if (!isValidDaytime(level, pos)) return; // 复用 P2-T7 共享光照判定
>     energyStorage.receiveEnergy(10, false); // 满储自然丢弃
> }
> // ProcessingMachineBlockEntity.process() 和 EffectMachineBlockEntity.process() 中
> // 在停滞判断之前调用：tryInjectPhotosynEnergy();
> // 不乘 B、不乘功率倍率、不外送
> ```
>
> **14. 取整约定**：FE/t→`Math.round` + 最小 1 守卫；容量/吞吐→`(int)`截断+`Math.min(..., Integer.MAX_VALUE/2)`；ticks→`Math.ceil`+`Math.max(1)`；MobEffectInstance 持续时间×B→`(int)Math.min(baseDuration * (long)B, Integer.MAX_VALUE)`


---

### P3：全网络覆盖（GREEN 阶段）

**阶段目标**：将 P1/P2 的 tick 语义和批处理联动推广到 EffectMachine（含非 Beacon 子类 batch 循环）、发电机、线缆、通道、能量单元、Capability、面配置。

**阶段依赖**：P1（tick 语义基础）→ P3-T1a（EffectMachine tick 语义）→ P3-T1b（EffectMachine batch 联动 — Beacon duration×B）→ P3-T1c（非 Beacon EffectMachine batch — Quarry/Farm/MobRip 逐类 B 次操作）→ P3-T2~P3-T7（验证类任务，可部分并行）

**阶段内顺序**：P3-T1a（EffectMachine tick 语义）→ P3-T1b（Beacon duration×B）→ P3-T1c（Quarry/Farm/MobRip B 次循环）→ P3-T2~P3-T7（验证任务，依赖 P3-T1c 完成后方可验证网络完整性；P3-T2~P3-T7 之间可并行）

---

#### P3-T1a：EffectMachineBlockEntity tick 语义改造

| 字段 | 值 |
|------|-----|
| **任务ID** | `P3-T1a` |
| **目标** | 同 P1 模式改造 EffectMachine 核心 tick 循环：固定扣 FE/t×B、progress++、`progress >= maxProgress`、else 分支不重置。maxProgress/触发间隔不因 B 改变 |
| **目标文件** | `src/main/java/.../api/blockentity/EffectMachineBlockEntity.java` |
| **核心符号** | `process()`、`effectInterval()`、`maxProgress = (int)Math.ceil(effectInterval * 20 * durationMultiplier)`；FE/t 消耗取 `Math.round(getActualEfficiency() × B)` 即 totalFePerTick；运行停滞：`if (energyStored < totalFePerTick) return;` |
| **前置依赖** | P1-T1（process 模式）、P2-T6（批处理 FE/t×B） |
| **测试先行** | P0-T2（EffectMachineContractTest）全部应转 GREEN；新增 batch FE/t 消耗测试 |
| **实现动作** | ① 同 P1-T1 模式：移除 energyConsumed 累加；固定 `int B = getLockedB(); int totalFePerTick = Math.round(getActualEfficiency() * B);`；**② Syn 注能：`tryInjectPhotosynEnergy()` 在停滞判断前执行**；`if (energyStored < totalFePerTick) return;` 停滞；`energyStorage.extractEnergy(totalFePerTick, false); progress++;`；③ `maxProgress` 在 recipe 开始时固定，公式 `(int)Math.ceil(effectInterval() * 20 * durationMultiplier)`，**不因 B 改变**；④ else 分支 `progress = 0` 移除；⑤ 无 recipe 时 maxProgress = 0；⑥ 停滞语义：不扣能、不加 progress、maxProgress 不变 |
| **验证命令** | `gradlew.bat test --tests "com.modularmc.ten.api.blockentity.EffectMachineContractTest" --console=plain` |
| **DoD** | ① EffectMachine 契约测试全 GREEN；② completion `>=`；③ 能量不足暂停不重置；④ maxProgress 固定且不因 B 改变；⑤ FE/t×B 使用 Math.round 消耗正确；⑥ 停滞语义正确；⑦ Syn 注能 `tryInjectPhotosynEnergy()` 在停滞判断前执行 |
| **禁止项** | 不改 EffectMachine 的 effect 调度逻辑；不改效果等级/范围 |
| **执行代理** | 米娅 |
| **审查门禁** | 艾琳 |

---

#### P3-T1b：EffectMachine batch 联动 — 效果持续时间×B

| 字段 | 值 |
|------|-----|
| **任务ID** | `P3-T1b` |
| **目标** | EffectMachine 应用 batch 时：效果等级/范围不变；每次 apply 的 `MobEffectInstance` 持续时间 = 基础持续时间 × B（long 中间值 + int 安全钳位）；同 tick 不重复 apply B 次。确保 Power/Shulker batch 在 EffectMachine 有实际价值 |
| **目标文件** | `src/main/java/.../api/blockentity/EffectMachineBlockEntity.java`（基类覆盖方法 `getEffectBaseDuration()`）、`src/main/java/.../common/blockentity/machine/BeaconBlockEntity.java`（唯一实际产生 MobEffectInstance 的子类，提取硬编码 400 为命名常量） |
| **核心符号** | `applyEffects()`、`MobEffectInstance`、`getBatchFactor()`、`getEffectBaseDuration()`（覆写方法，Beacon 返回 `BEACON_BASE_DURATION = 400`）、`(int)Math.min(baseDuration * (long)B, Integer.MAX_VALUE)` |
| **前置依赖** | P3-T1a（tick 语义就绪） |
| **测试先行** | P0-T2 扩展：新增 batch 持续时间×B 测试。**只读核对所有 EffectMachine 子类**——继承链：`EffectMachineBlockEntity` → `RadiusMachineBlockEntity` → `BeaconBlockEntity`（MobEffectInstance 唯一产生者）、`QuarryBlockEntity`（挖矿/产冰，非效果）、`FarmBlockEntity`（作物扫描，非效果）、`MobRipBlockEntity`（实体伤害，非效果）。仅 `BeaconBlockEntity.applyEffect()` 创建 `MobEffectInstance`（硬编码 400 ticks）。因此持续时间×B 逻辑限定为 Beacon 子类专属，不虚构所有 EffectMachine 共享基础时长。测试覆盖：Beacon 基础 400ticks × B=4 → 1600ticks，long 乘后 clamp 不溢出；B=19 → 7600ticks 仍为合法 int；非 Beacon 子类不影响 |
| **实现动作** | ① 在 `EffectMachineBlockEntity` 中添加 `protected int getEffectBaseDuration() { return 0; }` 作为覆写点（默认 0 表示子类不产生计时效果）；② 在 `BeaconBlockEntity` 中：提取硬编码 `private static final int BEACON_BASE_DURATION = 400;` 并 `@Override protected int getEffectBaseDuration() { return BEACON_BASE_DURATION; }`；③ 在 `BeaconBlockEntity.applyEffect()` 中获取 `int B = getLockedB();`；④ 构造 `MobEffectInstance` 时改为：`new MobEffectInstance(effect.getEffect(), (int)Math.min(BEACON_BASE_DURATION * (long)B, Integer.MAX_VALUE), amplifier, true, true)`——使用 long 乘法避免 int 溢出再 clamp 到 int；⑤ 同 tick 仅 apply 1 次效果（duration×B 替代重复 apply 次数）；⑥ 效果等级/范围不因 B 改变；⑦ Quarry/Farm/MobRip 的 `applyEffect()` 不涉及 duration，无需改动 |
| **验证命令** | `gradlew.bat test --tests "com.modularmc.ten.api.blockentity.EffectMachineContractTest" --console=plain` + `gradlew.bat test --tests "com.modularmc.ten.common.blockentity.machine.BeaconBlockEntityTest" --console=plain`（若存在）或 `gradlew.bat test --tests "com.modularmc.ten.api.blockentity.*Batch*" --console=plain` |
| **DoD** | ① 基础持续时间作为命名常量/可覆写方法抽取；② 持续时间 = 基础值 × B，使用 `(long)B` 中间乘法 + `(int)Math.min(..., Integer.MAX_VALUE)` 安全钳位；③ 同 tick 不重复 apply B 次；④ 效果等级/范围不变；⑤ 仅 Beacon 子类绑定 duration×B；Quarry/Farm/MobRip 不受影响；⑥ Power/Shulker batch 在 EffectMachine 有实际价值（更长时间效果）|
| **禁止项** | 不修改 `MobEffectInstance` 类；不改效果注册/等级；不改 maxProgress/触发间隔；不虚构所有 EffectMachine 共享的基础持续时间 |
| **执行代理** | 米娅 |
| **审查门禁** | 艾琳：验证子类继承链正确 + Beacon 为唯一 MobEffectInstance 产生者 + long 中间值钳位正确 |

---

#### P3-T1c：非 Beacon EffectMachine batch — Quarry/Farm/MobRip 逐类 B 次操作

| 字段 | 值 |
|------|-----|
| **任务ID** | `P3-T1c` |
| **目标** | 为非 Beacon EffectMachine 子类（Quarry/Farm/MobRip）实现 batch 实际价值：每周期执行 B 次独立操作。FE/t×B、progress+1、maxProgress 不因 B 变。三类行为不同，各自实现独立 batch 循环 |
| **目标文件** | `src/main/java/.../common/blockentity/machine/QuarryBlockEntity.java`、`FarmBlockEntity.java`、`MobRipBlockEntity.java`（各自扩展 `applyEffect()` 或 `process()` 中的 effect 触发点） |
| **核心符号** | `applyEffect()`、`getLockedB()`、`progress`、`maxProgress` |
| **前置依赖** | P3-T1a（tick 语义就绪）、P2-T6（B_actual 锁定） |
| **测试先行** | P0-T2 扩展：新增 Quarry batch×B、Farm row×B、MobRip damage×B 测试；新增防越界测试（Farm row 不越界、MobRip 无效目标跳过）；RED 断言当前无 batch 循环 |
| **实现动作** | ① **QuarryBlockEntity**：在 `applyEffect()` 完成周期逻辑后，外层 for(i=0; i<getLockedB(); i++) 循环独立调用挖掘/产出逻辑（每 B 次独立 `WorkingHelper.getDrops`/`fill`/`output`）。注意：B 次共享同一 radius/mode 设置，各自独立检查 `conditionStart()` 所需资源。② **FarmBlockEntity**：在 `applyEffect()` 完成现有单行扫描后，改为连续扫描 B 行。每次迭代按 `currentRowIndex` 推进，调用现有行扫描逻辑；每段迭代更新 `currentRowIndex`，确保不重复扫描同一行/不越界（`currentRowIndex = (currentRowIndex + 1) % xRowOrder.length`）。若 B > 本周期剩余行数，只处理到数组末尾；未执行的多余batch份额本周期作废，不顺延、不回绕。能耗仍按启动锁定的 `FE/t×B` 支付，保持 `totalFePerTick = Math.round(baseFePerTick × lockedB)` 不变，不因实际执行行数少而动态降B。③ **MobRipBlockEntity**：在 `applyEffect()` 中改为 for(i=0; i<getLockedB(); i++) 每次独立随机选取 `AABB` 内 `LivingEntity`，检查存活/有效性后施加伤害/产出。允许目标少时同一实体被多次选中，每次仍需重新检查存活/有效性。④ 三种机器每 tick `totalFePerTick = Math.round(getActualEfficiency() × getLockedB())`，停滞语义同 P3-T1a。⑤ Syn 注能 `tryInjectPhotosynEnergy()` 在停滞判断前执行。 |
| **验证命令** | `gradlew.bat test --tests "com.modularmc.ten.api.blockentity.EffectMachineContractTest" --console=plain` |
| **DoD** | ① Quarry B 次循环独立挖掘/产出，progress+1 不变；② Farm B 行连续扫描，不重复/不越界，`currentRowIndex` 正确推进；③ MobRip B 次独立随机伤害，无效目标跳过，允许重复选中；④ FE/t×B 消耗正确；⑤ progress+1、maxProgress 不变；⑥ Syn 注能在停滞判断前注入 |
| **禁止项** | 不改 Beacon 持续时间语义（已归 P3-T1b）；不改效果等级/范围；不改 `effectInterval()`/`maxProgress` 公式 |
| **执行代理** | 米娅 |
| **审查门禁** | 艾琳：逐类验证 batch 循环不破坏原有单次语义，防越界/防无效目标逻辑正确 |

---

#### P3-T2：EngineBlockEntity 验证

| 字段 | 值 |
|------|-----|
| **任务ID** | `P3-T2` |
| **目标** | 验证发电机在 upgrade 后 energyAllowRun 边界、燃料消耗速率正确 |
| **目标文件** | `src/main/java/.../api/blockentity/EngineBlockEntity.java` + 子类 |
| **核心符号** | `energyAllowRun()`、`fuel`、`efficientIn` |
| **前置依赖** | P2-T2~T5（升级 effect 改造完成） |
| **实现动作** | ① 验证 `energyAllowRun()` 在 upgrade 后 `<= maxStorageEnergy` 正确；② 验证 `fuel = Math.max(fuel - efficientIn, 0)` 速率正确；③ 子类独立验证 |
| **验证命令** | `gradlew.bat compileJava --console=plain` + 全量测试 |
| **DoD** | 发电机生产量不变；upgrade 后消耗速率正确；energyAllowRun 边界无溢出 |
| **禁止项** | 不改发电机 tick 生产逻辑 |
| **执行代理** | 米娅 |
| **审查门禁** | 艾琳 |

---

#### P3-T3~P3-T7：网络组件验证（可并行）

这些任务是验证/回归任务，不涉及大规模生产改造。每个任务都标识目标文件和验证点。合并为一个区块以节省篇幅，但执行时每个作为独立任务单。

| 任务ID | 目标 | 目标文件 | 验证要点 | 依赖 |
|--------|------|----------|----------|------|
| **P3-T3** | CableBlockEntity 验证 | `CableBlockEntity.java` | `redistribute()` 速率限制`transferFor` 与 `maxExtractEnergy`/`maxReceiveEnergy` 一致；`fillNetwork`/`drainNetwork` 无回归 | P3-T1c |
| **P3-T4** | ChannelEnergyBlockEntity 验证 | `ChannelEnergyBlockEntity.java`、`AbstractChannelBlockEntity.java` | `maxReceiveEnergy`/`maxExtractEnergy` 与 upgrade 后值一致；`TransferNetworks.moveEnergy` 限制正确 | P3-T1c |
| **P3-T5** | CellBlockEntity/CreativeCellBlockEntity 验证 | `CellBlockEntity.java`、`CreativeCellBlockEntity.java` | 储能上限与 I/O 速率一致；充放电速率限制正确 | P3-T1c |
| **P3-T6** | MachineEnergyStorage/CapabilityAdapters 验证 | `MachineEnergyStorage.java`、`CapabilityAdapters.java` | extract/maxExtract 一致性；`setMaxReceive`/`setMaxExtract` 在 upgrade 后正确调用；`getEnergyStorage(side)` 使用 `maxReceiveEnergy`/`maxExtractEnergy` | P3-T1c |
| **P3-T7** | 面配置 I/O 验证 | `CmMachineBlockEntity.java`（面配置部分） | `energyFaceData` 与 upgrade 后速率一致；`canReceiveEnergy`/`canExtractEnergy` 正确 | P3-T1c |

**P3-T3~P3-T7 通用属性**：
- **测试先行**：P0 契约已覆盖基本行为，本阶段为集成回归
- **验证命令**：`gradlew.bat test --tests "com.modularmc.ten.*" --console=plain`
- **DoD**：各组件验证 PASS；全量测试 GREEN
- **执行代理**：米娅
- **审查门禁**：艾琳
- **禁止项**：除非必要，不改动这些文件的生产逻辑（仅验证/适配）

---

### P4：配方 time 审核 + 批量修订（审核 + 数据阶段）

**阶段目标**：统一所有配方 `time` 字段语义为 ticks。一次批量修订，需独立 diff/验证/检查点。**未有平衡依据的配方保持原值而非猜测**。

**阶段依赖**：P1（理解新 tick 语义后判断 time 值）→ P4-T1（审计表）→ P4-T2（手写修订）→ P4-T3（DataGen 审核）→ P4-T4（重生成 + diff）→ P4-T5（脚本验证）

**阶段内顺序**：严格串行——审计表 → 手写修订 → DataGen 修订 → 重生成 → 验证

---

#### P4-T1：配方 time 审计表生成

| 字段 | 值 |
|------|-----|
| **任务ID** | `P4-T1` |
| **目标** | 遍历所有配方 JSON + DataGen time 字面量，输出当前 time 值清单，标注来源（手写/DataGen/目录/配方类型） |
| **目标文件** | 审计表输出（建议 `plans/.evidence/` 或独立文档）；遍历范围：`src/main/resources/data/kenergyengineering/recipe/` + `src/generated/resources/data/kenergyengineering/recipe/` + `TENRecipeGen.java` + `TENVanillaPackGen.java` |
| **核心符号** | `time` 字段、`cookingTime` 字段 |
| **前置依赖** | P1-T3（子类统一后确认 baseTickTime 契约） |
| **实现动作** | ① 编写/运行脚本遍历所有 recipe JSON，提取 `time`/`cookingTime` 值；② 标注每个配方的机器类型和源码来源（手写 vs DataGen）；③ 输出审计表：文件路径、当前值、机器类型、是否需修订、修订依据 |
| **验证命令** | 审计表输出可审查（人工确认每一项合理） |
| **DoD** | ① 审计表完整覆盖所有配方；② 每个 time 值标注机器类型；③ 标注是否需要修订及依据 |
| **禁止项** | 不修改任何文件 |
| **执行代理** | 米娅 |
| **审查门禁** | 艾琳 + 用户（若涉及平衡判断） |

---

#### P4-T2：手写配方 time 批量修订

| 字段 | 值 |
|------|-----|
| **任务ID** | `P4-T2` |
| **目标** | 根据审计表修订手写配方 time 值，仅修改明确有依据的项 |
| **目标文件** | `src/main/resources/data/kenergyengineering/recipe/**/*.json`（手写） |
| **核心符号** | `time` 字段 |
| **前置依赖** | P4-T1（审计表就绪） |
| **实现动作** | ① 按审计表逐个修改；② 每项记录旧值→新值；③ **无依据保持原值**；④ 修改后独立的 commit |
| **验证命令** | `gradlew.bat compileJava --console=plain` + JSON schema 验证 |
| **DoD** | ① 手写配方 time 语义为 ticks；② 有变更清单（旧→新映射）；③ 独立 commit |
| **禁止项** | 不新增 FE/t 字段；不改 DataGen |
| **执行代理** | 米娅 |
| **审查门禁** | 艾琳：审查变更清单确认无不合理修改 |

---

#### P4-T3：DataGen 配方 time 审核修订

| 字段 | 值 |
|------|-----|
| **任务ID** | `P4-T3` |
| **目标** | 审核 `TENRecipeGen.java` 中 time 字面量，确保语义为 ticks；同步修正 `TENVanillaPackGen.java` |
| **目标文件** | `src/main/java/.../data/TENRecipeGen.java`、`TENVanillaPackGen.java` |
| **核心符号** | `time` 字面量、`buildCompress()`、`buildPulv()`、`buildSmelting()`、`buildBlasting()`、`buildRawBlockPulv()` |
| **前置依赖** | P4-T1（审计表） |
| **实现动作** | ① 审核所有硬编码 time 值；② 若有动态计算逻辑，确认最终值单位正确；③ 修改后独立 commit |
| **验证命令** | `gradlew.bat compileJava --console=plain` |
| **DoD** | DataGen time 字面量语义为 ticks；动态计算逻辑确认单位正确 |
| **禁止项** | 不新增 FE/t 字段 |
| **执行代理** | 米娅 |
| **审查门禁** | 艾琳 |

---

#### P4-T4：DataGen 重新生成 + diff 验证

| 字段 | 值 |
|------|-----|
| **任务ID** | `P4-T4` |
| **目标** | 运行 `runClientData` 重新生成配方 JSON，验证 `git diff src/generated/resources` 仅含预期 time/格式变更 |
| **目标文件** | `src/generated/resources/data/kenergyengineering/recipe/` |
| **前置依赖** | P4-T3（DataGen 源码修订完成） |
| **实现动作** | ① `gradlew.bat runClientData --console=plain`；② `git diff --stat src/generated/resources`；③ 检查 diff 是否仅含 time 值/格式变更；④ 若非预期 diff 则 stop 排查 |
| **验证命令** | `gradlew.bat runClientData --console=plain`（300s 硬超时）+ `git diff --exit-code -- src/generated/resources`（仅 time/格式） |
| **DoD** | ① DataGen 运行成功；② diff 仅预期 time/格式变化；③ 无新增/删除配方 |
| **禁止项** | 不手动编辑 generated 文件 |
| **执行代理** | 米娅 |
| **审查门禁** | 艾琳：审查 diff |

---

#### P4-T5：脚本验证 + 检查点

| 字段 | 值 |
|------|-----|
| **任务ID** | `P4-T5` |
| **目标** | 运行完整验证套件，确认 P4 所有验收条件通过 |
| **目标文件** | 全部配方文件 |
| **前置依赖** | P4-T4（重生成完成） |
| **实现动作** | ① 运行 `python scripts/datapack_registration_matrix_check.py`；② `python scripts/generated_orphan_check.py`；③ 验证所有 time/cookingTime > 0；④ 确认无 FE/t 字段新增 |
| **验证命令** | `python scripts/datapack_registration_matrix_check.py` + `python scripts/generated_orphan_check.py` + `git diff --stat -- src/generated/resources` |
| **DoD** | ① 两脚本 PASS；② 所有 time>0 ticks；③ 无 FE/t 字段新增；④ 手写/DataGen 均有独立 commit 记录 |
| **禁止项** | 不修改生产代码 |
| **执行代理** | 米娅 |
| **审查门禁** | 艾琳 |

---

### P5：旧进度重置、GUI 同步、全量回归（收尾阶段）

**阶段目标**：存档兼容性、客户端同步、JEI 显示回归、全量测试回归。

**阶段依赖**：P1-P4 全部完成

**阶段内顺序**：P5-T1（存档）→ P5-T2（GUI）可并行 P5-T3（JEI）→ P5-T4（全量回归，依赖全部前序）

---

#### P5-T1：旧存档重置策略

| 字段 | 值 |
|------|-----|
| **任务ID** | `P5-T1` |
| **目标** | progress/maxProgress 不做比例迁移，读旧 NBT 后重置为 0。储能/库存/升级保留。 |
| **目标文件** | `src/main/java/.../api/blockentity/CmMachineBlockEntity.java`（`readTileData()`） |
| **核心符号** | `readTileData()`、`progress`、`maxProgress` |
| **前置依赖** | P1-T2（CmMachine 字段声明） |
| **实现动作** | ① `readTileData()` 中读旧 progress/maxProgress；② 若 maxProgress 异常大（旧语义能量值），重置 progress=0, maxProgress=0；③ try-catch 保护防止 NPE；④ 新 progress 在下一轮 recipe 开始时由 conditionStart 重新设定 |
| **验证命令** | `gradlew.bat compileJava --console=plain` |
| **DoD** | ① 旧存档加载不报错；② progress/maxProgress 重置；③ 储能/库存/升级保留；④ 面配置/红石模式保留 |
| **禁止项** | 不修改存档格式；不做比例迁移 |
| **执行代理** | 米娅 |
| **审查门禁** | 艾琳 |

---

#### P5-T2：GUI 同步验证

| 字段 | 值 |
|------|-----|
| **任务ID** | `P5-T2` |
| **目标** | 验证 progressGauge 显示 ticks 进度；energyGauge/fuelGauge 语义不变；@DescSynced 同步正确 |
| **目标文件** | `TENMachineBlockUIFactory.java`（验证，不改）、`CmMachineBlockEntity.java`（`doBaseData()`） |
| **核心符号** | `progressGauge`、`@DescSynced` |
| **前置依赖** | P1-T1（process 新语义）、P2-T6（批处理能量联动） |
| **实现动作** | ① 验证 `doBaseData()` 写入 progress/maxProgress 给 @DescSynced；② 验证 GUI progress 箭头显示 `progress / maxProgress` 正确 |
| **验证命令** | 无自动化测试（GUI 需人工或 ModDev）→ 以 `compileJava` + 静态分析替代 |
| **DoD** | progressGauge 显示 ticks 进度；GUI 无编译/链接错误 |
| **禁止项** | 不依赖 ModDev；不改 GUI 布局 |
| **执行代理** | 米娅 |
| **审查门禁** | 艾琳 |

---

#### P5-T3：JEI 显示回归（Q4 后置建议，不阻塞主计划）

| 字段 | 值 |
|------|-----|
| **任务ID** | `P5-T3` |
| **目标** | JEI 配方页面 time 显示为 ticks（标注 "ticks"），总能耗显示为后置非阻塞项 |
| **目标文件** | `TENJeiCategory.java`、`TENRecipeWidget.java`、`TENJeiPlugin.java` |
| **核心符号** | `drawJei`、time 文本、能量文本 |
| **前置依赖** | P4-T4（time 统一为 ticks） |
| **实现动作** | ① 验证 JEI 显示 recipe time 为 ticks；② 若有能量消耗信息，基于 recipe time + 机器 FE/t 计算总能耗显示；③ **总能耗显示为 Q4 后置建议，不实现也在验收范围内** |
| **验证命令** | `gradlew.bat compileJava --console=plain` |
| **DoD** | JEI time 信息显示为 ticks；总能耗显示（若实现）不破坏现有布局 |
| **禁止项** | 不依赖 ModDev 验证 JEI 显示 |
| **执行代理** | 米娅 |
| **审查门禁** | 艾琳 |

---

#### P5-T4：全量回归

| 字段 | 值 |
|------|-----|
| **任务ID** | `P5-T4` |
| **目标** | 全量编译、测试、DataGen、构建通过 |
| **目标文件** | 所有 |
| **前置依赖** | P5-T1、P5-T2、P5-T3 |
| **实现动作** | 依次运行：① `gradlew.bat compileJava --console=plain`（300s 超时）；② `gradlew.bat test --console=plain`（300s 超时）；③ `gradlew.bat runClientData --console=plain`；④ `python scripts/datapack_registration_matrix_check.py`；⑤ `python scripts/generated_orphan_check.py`；⑥ `gradlew.bat build --console=plain` |
| **验证命令** | `gradlew.bat compileJava --console=plain` → `gradlew.bat test --console=plain` → `gradlew.bat runClientData --console=plain` → python 脚本 → `gradlew.bat build --console=plain` |
| **DoD** | ① 编译无错误；② 测试全部 GREEN；③ DataGen 成功；④ python 脚本 PASS；⑤ build 成功；⑥ 不依赖 ModDev |
| **禁止项** | 不改任何生产代码 |
| **执行代理** | 米娅 |
| **审查门禁** | 艾琳 + 最终审查 |

---

## 依赖链总结（含原子任务映射）

```
P0-RG(工作区恢复) ──→ P0-T1~T8(契约测试 RED) ──→ P1-T1~T3(处理机 tick)
                                                          │
                                                          ├─→ P1-T3a(Furnace batch)
                                                          ├─→ P1-T3b(Condenser batch)
                                                          └─→ P1-T3c(Encflu batch)
                                                          │
                                                          ↓
                                                     P2-T1~T8(升级+批处理)
                                                          │
                                                          ├─→ P2-T6标准RecipeMachine batch
                                                          │   (P1-T3a/b/c为自定义ProcessingMachine batch)
                                                          ↓
                                                     P3-T1a/T1b/T1c(EffectMachine)
                                                          │
                                                          ↓
                                                     P3-T2~T7(全网络)
                                                          │
                                                          ↓
                                                     P4-T1~T5(配方time)
                                                          │
                                                          ↓
                                                     P5-T1~T4(存档/GUI/JEI/回归)
```

### 串行依赖链
- `P0-RG → P0-T1~T8 → P1-T1 → P1-T2 → P1-T3 → P1-T3a/T3b/T3c（可并行于 P2-T1 前或与 P2-T1 并行）→ P2-T1 → P2-T2~T5 → P2-T6 → P2-T8 → P3-T1a → P3-T1b → P3-T1c → P3-T2~T7 → P4-T1 → P4-T2 → P4-T3 → P4-T4 → P4-T5 → P5-T1 → P5-T2/T3 → P5-T4`
- P3-T3~T7 之间可并行
- P2-T7（太阳能提取）可并行于 P2-T2~T5
- P0-T1~T7 之间可并行（RED 测试编写）
- P5-T2（GUI）与 P5-T3（JEI）可并行
- **P1-T3a/T3b/T3c（自定义 batch）可并行于 P2-T1～T5，最迟在 P2-T6 前完成**（因 P2-T6 仅标准 RecipeMachine batch，与自定义解耦）

### 允许并行标注
| 阶段 | 并行任务 | 说明 |
|------|----------|------|
| P0 | P0-T1~T7 | 测试文件编写无代码依赖 |
| P0 | P0-T8 | 依赖前序全部完成 |
| P1 | P1-T3 各子类 | 各子类 conditionStart 独立验证 |
| P1 | **P1-T3a/T3b/T3c** | **Furnace/Condenser/Encflu batch 独立实现，可并行于 P2-T1~T5** |
| P2 | P2-T2~T5 | 四款升级 effect 改造互不依赖 |
| P2 | P2-T7 | 可并行于 P2-T2~T5 |
| P2 | **P2-T6（标准 RecipeMachine）** | **与 P1-T3a/b/c 解耦，仅覆盖 5 个标准子类** |
| P3 | P3-T3~T7 | 各网络组件独立验证 |
| P3 | P3-T1a→T1b→T1c | 串行（先 tick 语义，后 Beacon duration，再非 Beacon batch） |
| P4 | P4-T1~T5 | 严格串行 |
| P5 | P5-T2/P5-T3 | GUI 与 JEI 可并行 |

---

## 关键文件清单（按影响面排序）

| 优先级 | 文件 | 阶段 | 改动量 |
|---|---|---|---|
| 🔴 | `src/main/java/.../api/blockentity/ProcessingMachineBlockEntity.java` | P1+P2 | 重写 process()+ 批处理联动 |
| 🔴 | `src/main/java/.../api/blockentity/EffectMachineBlockEntity.java` | P3-T1a+T1b+T1c | 同 P1 模式 + batch 联动（FE/t×B + Beacon duration×B + Quarry/Farm/MobRip B 次循环） |
| 🔴 | `src/main/java/.../api/blockentity/CmMachineBlockEntity.java` | P1/P2 | onUpgradeApply、applyUpgradeEffects、energyAllowRun、batch因子、光合标记 |
| 🟡 | `src/main/java/.../common/item/upgrades/LevelupAug.java` | P2 | 修改 effect() 调乘法接口 |
| 🟡 | `src/main/java/.../common/item/upgrades/LevelupPower.java` | P2 | 修改 effect() 调乘法接口 + applyBatch(1) |
| 🟡 | `src/main/java/.../common/item/upgrades/LevelupShulker.java` | P2 | 修改 effect() 调乘法接口 + applyBatch(3) |
| 🟡 | `src/main/java/.../common/item/upgrades/LevelupSyn.java` | P2 | 重写 effect() → applyPhotosyn() (安装上限1, 独立倍率) |
| 🟡 | `src/main/java/.../common/item/upgrades/IUpgradableMachine.java` | P2 | 可选接口扩展 |
| 🟡 | `src/main/java/.../common/item/upgrades/UpgradeMathHelper.java` | P2 | 可选新建—纯 static 辅助（不注册） |
| 🟡 | `src/main/java/.../common/blockentity/machine/SolarBlockEntity.java` | P2 | **提取共享静态方法** `computeSolarFuel(BlockState, Level, BlockPos)` 供 LevelupSyn 光照判定复用；而非调用非静态 `matchFuel()` |
| 🟡 | `src/main/java/.../common/blockentity/CableBlockEntity.java` | P3-T3 | 验证 |
| 🟡 | `src/main/java/.../common/blockentity/channel/ChannelEnergyBlockEntity.java` | P3-T4 | 验证 |
| 🟡 | `src/main/java/.../common/blockentity/channel/AbstractChannelBlockEntity.java` | P3-T4 | 验证 |
| 🟡 | `src/main/java/.../common/blockentity/machine/CellBlockEntity.java` | P3-T5 | 验证 |
| 🟡 | `src/main/java/.../api/capability/CapabilityAdapters.java` | P3-T6 | 验证 |
| 🟡 | `src/main/java/.../api/capability/MachineEnergyStorage.java` | P3-T6 | 验证 |
| 🟡 | `src/main/resources/data/.../recipe/**/*.json` | P4 | 审核 + 修订 time |
| 🟡 | `src/main/java/.../data/TENRecipeGen.java` | P4 | 审核 + 修订 time |
| 🟢 | `src/main/java/.../api/blockentity/EngineBlockEntity.java` + 子类 | P3-T2 | 验证 |
| 🟢 | `src/main/java/.../common/blockentity/machine/FurnaceBlockEntity.java` | P1-T3a | 自定义 batch：B 份输入/产出 |
| 🟢 | `src/main/java/.../common/blockentity/machine/CondenserBlockEntity.java` | P1-T3b | 自定义 batch：重写 cooking，5mB×B |
| 🟢 | `src/main/java/.../common/blockentity/machine/EncfluBlockEntity.java` | P1-T3c | 自定义 batch：B 组安全回滚 |
| 🟢 | `src/main/java/.../common/blockentity/machine/QuarryBlockEntity.java` | P3-T1c | 非 Beacon EffectMachine batch：×B 独立挖掘 |
| 🟢 | `src/main/java/.../common/blockentity/machine/FarmBlockEntity.java` | P3-T1c | 非 Beacon EffectMachine batch：×B 行扫描 |
| 🟢 | `src/main/java/.../common/blockentity/machine/MobRipBlockEntity.java` | P3-T1c | 非 Beacon EffectMachine batch：×B 随机伤害 |
| 🟢 | `src/client/java/.../integration/jei/*.java` | P5-T3 | JEI 显示回归 |
| 🟢 | `src/client/java/.../integration/xei/TENRecipeWidget.java` | P5-T3 | time 显示 |
| 🟢 | `src/test/java/.../` | P0-T1~T8 | 测试文件（任务ID映射见检查点清单） |

---

## 风险与回退

### 风险登记

| ID | 风险 | 概率 | 影响 | 缓解 |
|---|---|---|---|---|---|---|---|
| R1 | 升级乘法极端倍率导致 int 溢出 | 中 | 高 | 测试验证极限组合（6×潜影+光合 = power+6300%）；在 `applyPowerIncrease` 中 clamp 到 Integer.MAX_VALUE/2；容量×B 使用 `Math.min(Integer.MAX_VALUE/2, ...)` |
| R2 | duration 下限无法达到 1 tick | 低 | 中 | `maxProgress = Math.max(1, ...)` 硬保证 |
| R3 | FE/t 数值超过 capacity/maxExtract 限制 | 中 | 中 | `doBaseData` 中写回 `energyStorage.setMaxExtract(maxExtractEnergy)`；`process()` 中 `totalFePerTick = Math.round(min(getActualEfficiency(), maxExtractEnergy) × B)` 并钳位 |
| R4 | 内部 extract/maxExtract 不一致 | 低 | 高 | 验证 `CmMachineBlockEntity.getEnergyStorage(side)` 中 extractEnergy 使用 `min(maxExtract, maxExtractEnergy)` |
| R5 | 面配置升级后不匹配 | 低 | 中 | `resetUpgradeEffects` 重置所有值，`applyUpgradeEffects` 覆盖 |
| R6 | 客户端 progress gauge 显示错误 | 低 | 中 | `@DescSynced` 自动同步；验证 doBaseData 写入 |
| R7 | 旧存档 progress/maxProgress 读入后 NPE 或异常 | 低 | 高 | `readTileData` 中检查边界，溢出时重置 |
| R8 | 光合供能抑制与机器升级顺序导致倍率应用顺序不一致 | 低 | 中 | 统一在 `applyUpgradeEffects()` 中顺序遍历，光合单独处理；`canApply()` 严格限 PROCESS+EFFECT |
| R9 | 发电机 `energyAllowRun` 在 upgrade 后容量溢出判断错误 | 中 | 中 | 保留 `<= maxStorageEnergy` 逻辑，验证 upgrade 后 maxStorageEnergy 正确 |
| R10 | 测试缺口：EffectMachine 无现有契约测试 | 高 | 中 | P0 中新建 EffectMachineContractTest，覆盖完整 |
| R11 | B_max=19 导致 ×B 后容量/吞吐溢出或 FE/t 超出网络承载 | 中 | 高 | conditionStart 时 B_actual 四维锁定；容量使用 `Math.min(Integer.MAX_VALUE/2, ...)`；FE/t 使用 `Math.round` 并钳位 |
| R12 | 概率产物 B 次 genItem()/genFluid() 调用在极端情况下性能开销 | 低 | 低 | B 上限由升级组合决定（理论 max=19），每次调用内部已有 rolls 循环，无需额外优化 |
| R13 | 概率输出最坏容量预判与实际产出不一致导致槽位不足 | 低 | 中 | 启动时按 100% chance 最坏预判，已保证零丢失/零地面掉落；若实际槽位仍不足，复用 `cooking()` 满槽暂停逻辑 |
| R14 | 新 tick 模型下能耗放大：批处理总能耗 = maxProgress × FE/t × B，相比无批处理放大 B 倍，可能导致机器/网络能耗预期偏差 | 中 | 中 | 文档明确总能耗公式；测试验证极端 B=19 时全周期能耗正确；确保 cable/energy cell 速率上限足够 |
| R15 | Furnace 移除 `/2` 后 speed 回归 | 中 | 中 | 设计上已确认；P0 契约测试和 P4 配方 time 审核确保 balance 经审计表确认 |
| R16 | EffectMachine batch 持续时间×B 后被钳位截断导致效果时间异常 | 低 | 低 | `(int)Math.min(baseDuration * (long)B, Integer.MAX_VALUE)` 安全钳位 |
| R17 | B_actual 启动四维锁定中，概率流体输出不可动态扩容导致 B 被过度限制 | 中 | 中 | 计划明确使用实际 tankCapacity 反算 B，不虚构扩容 API；若 tank 容量不足，B 自然降低，运行中不降 B 确保稳定 |
| R18 | baseFePerTick 最小 1 守卫遗漏导致零功耗机器除零/B_byEnergy 分母为 0 | 低 | 高 | 公式 Math.max(1, ...) 硬保证；P2-T1 efficientIn 生命周期中实现；P2-T6 process() 中使用 baseFePerTick 前已守卫；P0-T6 新增 RED 测试；审查门禁 S12 关闭 |
| R19 | Syn 10FE/t 注入在 process() 中重复实现（ProcessingMachine/EffectMachine 两份）导致分叉 | 中 | 低 | 公共 helper `tryInjectPhotosynEnergy()` 统一实现（P2-T7），两份 process() 中仅调用；审查门禁 S13 验证 |
| R20 | Condenser 旧 progress+=200*efficiency 残留，新 tick 计数不生效 | 中 | 高 | P1-T3b 明确要求完全重写 cooking() 和 onCookFinish()；旧代码必须删除；审查门禁 S14 验证残留清理 |
| R21 | Encflu B 组循环中部分组完成后异常回滚导致不一致状态 | 中 | 中 | 现有安全转移/回滚已封装 try-catch，扩展为 B 次循环时每组装独立快照+回滚；P1-T3c 实现动作明确；测试覆盖异常路径 |
| R22 | Farm B 行扫描越过 `xRowOrder` 数组边界 | 低 | 中 | 实现动作限 B ≤ 剩余行数，不越界不回绕；P3-T1c DoD 验证 |

### 回退策略

1. **单阶段回退**：每阶段有独立 DoD 和验证命令；若某阶段测试失败，只回退该阶段的文件变更。
2. **原子提交**：每阶段改为完成后，建议 git commit 打 tag（如 `energy-p0`, `energy-p1`），可 `git revert` 单阶段。
3. **配方修改回退**：P4 中对手写配方的 `time` 修改单独 commit，与代码修改分离。
4. **存档兼容性**：若旧存档加载出错，在 `CmMachineBlockEntity.readTileData()` 加 try-catch 保护，progress/maxProgress 读取失败时默认重置为 0。
5. **全量回退**：`git log --oneline` 找到本计划首个 commit，`git revert` 范围。

### Question 闸门

> **已关闭决策记录**：以下决策全部锁定，写回计划对应节（详见各节引用），**所有问诊已关闭**。Q4 仍单独后置。
>
> #### 历史决策关闭（v1.0–v1.2）
> | 编号 | 决策 | 关闭原因 | 写回位置 |
> |------|------|----------|----------|
> | Q1 | 批处理 tick 模型 | 用户确认 | 核心决策§2、B 因子表 |
> | Q6 | 槽位倍乘模型 | 用户确认 | B 因子表 |
> | Q2 | 旧存档 progress 重置为 0 | 确认为可接受行为 | P5-T1 |
> | Q3 | 手写配方 time 批量修订授权 | 用户授权一次修订 | P4 验收条件 |
> | Q5 | Syn 仅限 PROCESS+EFFECT | 用户裁示关闭 | 光合节、P2-T5 |
>
> #### 三轮需求问诊裁示全量写回（v1.3 — 2026-07-23）
> | 裁示 | 类别 | 写回位置 | 说明 |
> |------|------|----------|------|
> | **A. 同类型可重复安装** | 升级系统 | 核心决策§3、升级组合示例表、P2 附录§1 | Aug/Power/Shulker 不限同类型重复（每栈1/6槽）；Syn 仍最多1；B_max=19 |
> | **B. B_actual 四维锁定 + 运行停滞** | 批处理 | 核心决策§4、B 因子表、P2 附录§3/§8 | 四维(输入/输出/流体/储能)启动锁定；运行中停滞不扣能/不加 progress |
> | **C. 取整规则统一** | 公式 | 核心模型公式表 + 取整规则统一表、P2 附录§12 | FE/t→Math.round；容量截断+钳位；ticks→Math.ceil+Math.max(1) |
> | **D. 概率输出容量预判** | 批处理 | 核心决策§6、B 因子表概率预判节、P2 附录§7 | 启动假设100%最坏预判；item/fluid 分别；实际 chance 保持原值，不预掷/不缓存 |
> | **E. 光合昼间判定解冻 + 10FE/t 注入** | 光合 | 光合节、P2-T7 | 10FE/t 固定注入（不乘B/功率）；昼间 API 使用 `level.getDayTime() % 24000L < 12000L` |
> | **F. EffectMachine batch 联动** | EffectMachine | 核心决策§8、P3-T1a/T1b、P2 附录§9 | FE/t×B/progress+1/maxProgress 不变/持续时间×B(钳位)/同tick不重复apply |
> | **G. 已有裁示保持** | 总体 | 全篇 | 机器FE/t+配方ticks/四升级数值/加法batch/Syn仅PROCESS+EFFECT/Furnace无/2/P4审计/ModDev排除/Q4后置 |
> | **H. 工作区基线状态记录** | 基线 | 当前基线状态节、P0-RG | compileTestJava 失败仅记录恢复门，不计入交付 |
>
> #### v1.5 终审前最后裁示全量写回（2026-07-24）
> | 裁示 | 类别 | 写回位置 | 说明 |
> |------|------|----------|------|
> | **I. 附录A机械修正** | 附录A | 附录A全表、P1-T3、P2-T6 | 准确区分5标准RecipeMachine和3直接ProcessingMachine；写真实baseTickTime(Condenser=1000/Encflu=800/Furnace=cookingTime无/2)；清理"标准RecipeMachine含Condenser/Encflu"和"FE×B加速周期"错误 |
> | **J. baseFePerTick最小1守卫** | 公式 | 核心决策§14、公式表、取整表、P2-T1/T6、P0-T6 | safeRound→Math.max(1,...)→B_byEnergy；long中间值+int clamp；不得cancelStart造成0功耗阻塞 |
> | **K. Syn注能当前tick检查前** | 光合 | 核心决策§15、光合节、P2-T5/T7/P3-T1a/Processing共性 | receiveEnergy(10,false)在停滞判断前；满储丢弃；不乘B/倍率；不外送；公共helper共享 |
> | **L. 非Beacon EffectMachine batch** | EffectMachine | P3-T1c（新增） | Quarry×B独立挖掘/产出B次；Farm×B连续不重复行扫描；MobRip×B随机伤害(允许重复选中) |
> | **M. Furnace/Condenser/Encflu逐类batch** | 批处理 | P1-T3a/P1-T3b/P1-T3c（新增） | Furnace按最大可行B消费B输入/产出B结果；Condenser保留催化剂单份、重写cooking、输出5mB×B、tank容量反算B；Encflu按可行组数B组、独立转移/回滚、槽/tank约束 |
> | **N. 标准RecipeMachine与自定义batch路径分离** | 批处理 | P2-T6（标准）× vs P1-T3a/b/c（自定义） | 5标准RecipeMachine用InputConsumptionPlan×B；3自定义ProcessingMachine各自独立batch路径；RadiusMachine子类(effect-level)各自的batch循环 |
>
> #### 未决项
> 1. **Q4（后置非阻塞建议——不阻塞主计划）**：JEI 总能耗显示（ticks × FE/t）属于 UXD 增强项，不在本计划 DoD 内。P5-T3 仅验证 time 显示为 ticks；总能耗显示除非用户后续明确要求，否则不实现。即使不实现 Q4，P5 验收条件依然 PASS。

---

## 复审条件（审查关闭清单 — 7项 + v1.3 + v1.4 艾琳6项 + v1.5 终审前8项）

> 以下条件对应艾琳审查报告 + 用户裁示，全部关闭后方可进入执行。v1.3 增加 S3 覆盖三轮问诊裁示写回验收。v1.4 增加 S4-S10 覆盖艾琳复审"条件通过"六项修正。v1.5 增加 S11-S18 覆盖最后一轮裁示（附录A修正、最小守卫、Syn注能、Condenser重写、非Beacon batch、自定义batch、路径分离、P0-RG门禁）。

| 编号 | 审查项 | 关闭证据 | 状态 |
|------|--------|----------|------|
| **S1** | Q5关闭：光合供能抑制安装范围限 MACHINE_PROCESS + MACHINE_EFFECT；LevelupSyn.canApply 身份不变 | 光合节「可安装机器类型」行已标注；LevelupSyn 伪代码含 `canApply()` 检查；Q5 闸门关闭 | ✅ |
| **S2** | maxProgress 统一公式：RecipeMachine→`baseTickTime()`=recipe.time；Furnace→`cookingTime()`移除/2；EffectMachine→`effectInterval×20×durationMultiplier`；附录A所有子类明确 | 核心模型公式表三行分列 + 附录A完整清单 + P1-T1/P3-T1a伪代码同步更新 | ✅ |
| **M1** | consume误写：修正为 item `chance()≤0`→催化剂单份、`chance()>0`→B倍消耗；fluid全部B倍消耗；不虚构fluid consume字段 | B因子表、核心决策、范围内、P2伪代码、DoD全部修正 | ✅ |
| **M2** | 概率产物：每batch单位调`ing.genItem()`/`genFluid()`，复用内部rolls语义；禁直接random chance循环 | B因子表、概率产物说明、onCookFinish伪代码全部修正，含"禁止直接random chance"标注 | ✅ |
| **M3** | P4 time验收增强：1)所有time>0且ticks；2)资源清单+源映射完整；3)DataGen diff仅预期time/格式；4)批量修订独立checkpoint+变更清单；5)审计表+明确规则，无依据保持原值；不新增FE/t | P4节验收条件1-5完整列出，含脚本化验收描述 | ✅ |
| **轻微** | RecipeMachine继承链+onCookFinish落点标注；SolarBlockEntity光照判定提取共享helper；RecipeProgressResetTest更新为"能量不足/输出满暂停不清零、配方失效才重置" | 附录A继承链标注 + onCookFinish落点说明 + SolarBlockEntity表项更新"共享静态方法" + P0测试清单标注"需更新" | ✅ |
| **闸门** | Q3关闭（一次批量修订已授权）；Q4不阻塞保留后置question但不依赖ModDev；Q5关闭 | Question闸门节全部更新 | ✅ |
| **S3 (v1.3)** | 三轮问诊裁示A-H全部锁入计划：A.同类型重复安装/B_max=19；B.B_actual四维锁定+停滞语义；C.取整统一；D.概率最坏容量；E.光合昼间API+10FE/t；F.EffectMachine batch；G.已有裁示保持；H.P0-RG | 核心决策·公式表·升级表·B因子表·光合节·P2附录·P3-T1a/T1b·风险表·检查点·Question闸门全部更新；旧口径残留清理确认 | ✅ |
| **S4 (v1.4 B1)** | B_byEnergy 循环依赖：fePerTick 已含 B 导致循环。改为 baseFePerTick = `Math.round(initialEfficientIn×powerMultiplier)`（不含B），`B_byEnergy = floor(energyStored/baseFePerTick)`，`totalFePerTick = Math.round(baseFePerTick×B_actual)`。Math.round 顺序全文统一 | 核心模型公式表新增 baseFePerTick/totalFePerTick 行；B因子表 B_byEnergy 分母改为 baseFePerTick；P2附录§3/§8/§11 全部同步；命名严格分离"每批单位baseFePerTick"与"总FE/t" | ✅ |
| **S5 (v1.4 B2)** | powerMultiplier 未接入 getActualEfficiency/efficientIn。需在 P2-T1 中写明 `efficientIn = Math.round(initialEfficientIn×powerMultiplier)`、`effAuc=efficientIn`，使 getActualEfficiency 返回 baseFePerTick（含升级不含B）；每tick reset→apply→consume 生命周期 | P2-T1 实现动作⑤⑥新增 efficientIn 重算 + resetUpgradeEffects 恢复 + doBaseData 顺序保证；P2附录§11 新增 efficientIn 生命周期伪代码 | ✅ |
| **S6 (v1.4 C1)** | P2-T6 目标文件漏 RecipeMachineBlockEntity.java，实际 I/O 在其 onCookFinish/InputConsumptionPlan/generateItems/generateFluids | P2-T6 目标文件加入 RecipeMachineBlockEntity.java；核心符号/实现动作/DoD 补充真实 I/O 方法引用；明确 ProcessingMachineBlockEntity.onCookFinish() 为空不承载配方 I/O | ✅ |
| **S7 (v1.4 C2)** | 概率最坏容量公式分母含 B 导致循环。应定义每批单位最大产出 perBatchUnitWorst（不含B），再 `B_byProbWorst = floor(availableOutputCapacity/perBatchUnitWorst)`，多 ingredient/同槽合并需按 slot/tank 聚合 | B因子表新增 perBatchUnitWorst/B_byProbWorst/requiredWorst(B) 行；概率输出容量预判细则重写为按每 slot/tank 聚合反算；P2附录§7 重写为 perBatchUnitWorst 聚合逻辑 | ✅ |
| **S8 (v1.4 D1)** | P0-T6 需新增四维 B_actual 锁定和概率最坏容量的真正 RED 测试（非仅数学 B=1/2/5）；覆盖 B=19、B<1 不启动、催化剂/consume input/fluid、每目标槽/罐聚合、energyStored、固定 B 运行停滞、actual chance 不变 | P0-T6 测试先行扩展至 20 项（①~⑳），含 B=19 溢出钳位、B<1 不启动、催化剂/消耗品/流体倍乘、每槽聚合反算、energyStored baseFePerTick 无循环依赖、固定 B 不降、actual chance 不变等 RED 断言 | ✅ |
| **S9 (v1.4 D2)** | EffectMachine 基础持续时间需基于现有子类事实显式化。只读核对：仅 BeaconBlockEntity 产生 MobEffectInstance（硬编码 400 ticks），Quarry/Farm/MobRip 的 applyEffect 不涉及 duration。任务限定 Beacon，抽取命名常量/可覆写方法，400×B 用 long 中间值并 clamp | P3-T1b 重写：目标文件含 BeaconBlockEntity；新增 `getEffectBaseDuration()` 覆写点 + `BEACON_BASE_DURATION=400` 常量；`(int)Math.min(400L*B, Integer.MAX_VALUE)` 安全钳位；禁止虚构所有 EffectMachine 共享基础时长 | ✅ |
| **S10 (v1.4 轻微)** | P0-T8 测试命令避免通配匹配遗漏测试；仍完整输出/300s/--console=plain | P0-T8 验证命令改为完整显式列举 7 个测试类全限定名，不使用 `*` 通配符 | ✅ |
| **S11 (v1.5 N1)** | 附录A继承链准确：5标准RecipeMachine(Pulverizer/Compressor/Refiner/Indfur/Psionicant)与3直接ProcessingMachine(Furnace/Condenser/Encflu)分开；真实baseTickTime：Condenser=1000/Encflu=800/Furnace=cookingTime(无/2)；清理"标准RecipeMachine含Condenser/Encflu"和"FE×B加速周期"错误 | 附录A全表已修正、baseTickTime列来源准确、注释清理 | ✅ |
| **S12 (v1.5 N2)** | baseFePerTick最小1守卫：`Math.max(1, Math.round(...))`用于baseFePerTick和B_byEnergy分母；long中间值+int clamp；不cancelStart导致0功耗阻塞 | 核心决策§14、公式表baseFePerTick/B_byEnergy行、取整表、P2附录§11伪代码、P2-T1/T6实现动作、P0-T6测试扩展、风险表R18全部同步 | ✅ |
| **S13 (v1.5 N3)** | Syn注能当前tick检查前：receiveEnergy(10,false)在停滞判断前；满储丢弃；不乘B/倍率；不外送；公共helper共享避免分叉 | 核心决策§15、光合节、B因子表、停滞语义节、P2附录§13、P2-T5/T7/P3-T1a/P3-T1c任务实现动作 | ✅ |
| **S14 (v1.5 N4)** | Condenser.cooking旧progress+=200*efficiency和输出满reset删除/重写；新语义输出满停滞不清零 | P1-T3b实现动作明确重写cooking()和onCookFinish() | ✅ |
| **S15 (v1.5 非Beacon batch)** | Quarry×B独立挖掘/产出B次、Farm×B连续不重复行扫描、MobRip×B随机伤害(允许重复选中)；每类有目标符号、B次循环、防越界/失效目标行为、RED、命令、DoD | P3-T1c新增原子任务完整 | ✅ |
| **S16 (v1.5 自定义batch)** | Furnace/Condenser/Encflu逐类batch写入P1-T3a/T3b/T3c；明确B_actual输入/输出/tank限制、消费/产出、回滚原子性和测试；Condenser催化剂按既有单份周期语义不×B；Encflu每组消费按实际源码定义 | P1-T3a/T3b/T3c原子任务完整 | ✅ |
| **S17 (v1.5 路径分离)** | 标准RecipeMachine(5类)与自定义ProcessingMachine(3类)batch路径分开；P2-T6仅标准RecipeMachine用InputConsumptionPlan×B；自定义各自独立路径 | P2-T6范围标注+附录A+决策§12+并行表 | ✅ |
| **S18 (v1.5 P0-RG)** | P0-RG仅工作区恢复门，禁止声称执行完成 | P0-RG DoD明确只恢复编译不实施重构、验收汇总标注 | ✅ |

---

## 检查点清单（按任务ID）

```
[✓] P0-RG: 工作区恢复门 — compileTestJava 通过，生产代码未动
[✓] P0-T1: ProcessingMachineContractTest 新建 + RED
[✓] P0-T2: EffectMachineContractTest 新建 + RED
[✓] P0-T3: UpgradeMultiplicativeStackTest 新建/修复 + RED
[✓] P0-T4: BatchFactorTest 新建 + RED
[✓] P0-T5: RecipeProgressResetTest 更新 + RED
[✓] P0-T6: BatchProcessingContractTest 新建 + RED
[✓] P0-T7: PhotosynEffectTest 新建 + RED
[✓] P0-T8: P0 GREEN 基线验证（当前行为快照 GREEN + 目标断言 RED）
[✓] P1-T1: ProcessingMachineBlockEntity.process() 重写
[✓] P1-T2: CmMachineBlockEntity 辅助方法 + energyAllowRun 调整
[✓] P1-T3: 子类 conditionStart 统一 + Furnace cookingTime 修正
[✓] P1-T3a: Furnace batch — B 份输入消费 + B 份产出
[✓] P1-T3b: Condenser batch — 重写 cooking + 输出 5mB×B
[✓] P1-T3c: Encflu batch — B 组独立安全转移/回滚
[✓] P2-T1: CmMachineBlockEntity 乘法方法 + 批处理字段
[✓] P2-T2: LevelupAug effect() 改造
[✓] P2-T3: LevelupPower effect() 改造
[✓] P2-T4: LevelupShulker effect() 改造
[✓] P2-T5: LevelupSyn effect() 改造（canApply 严格限 PROCESS+EFFECT）
[✓] P2-T6: 标准 RecipeMachine batch（InputConsumptionPlan ×B，仅 Pulverizer/Compressor/Refiner/Indfur/Psionicant）
[✓] P2-T7: 光照判定共享静态方法 + 昼间 API 解冻 + Syn 注能公共 helper
[✓] P2-T8: 极端倍率 + 边界合约验证
[✓] P3-T1a: EffectMachineBlockEntity tick 改造（FE/t×B + progress++ + 停滞语义 + Syn 注能）
[✓] P3-T1b: EffectMachine batch 联动 — Beacon 效果持续时间×B（long钳位/仅apply一次）
[✓] P3-T1c: 非Beacon EffectMachine batch — Quarry×B/Farm×B行/MobRip×B伤害（逐类独立循环）
[✓] P3-T2: EngineBlockEntity 验证
[✓] P3-T3: CableBlockEntity 验证
[✓] P3-T4: ChannelEnergyBlockEntity 验证
[✓] P3-T5: CellBlockEntity/CreativeCellBlockEntity 验证
[✓] P3-T6: MachineEnergyStorage/CapabilityAdapters 验证
[✓] P3-T7: 面配置 I/O 验证
[✓] P4-T1: 配方 time 审计表生成
[✓] P4-T2: 手写配方 time 批量修订
[✓] P4-T3: DataGen 配方 time 审核修订
[✓] P4-T4: DataGen 重新生成 + diff 验证
[✓] P4-T5: 脚本验证 + 检查点（无 FE/t 字段新增）
[✓] P5-T1: 旧存档重置策略
[✓] P5-T2: GUI 同步验证
[✓] P5-T3: JEI 显示回归（Q4 后置建议）
[✓] P5-T4: 全量回归（编译/测试/DataGen/build PASS）
```

### 最终验收汇总
- ✅ 所有 41 原子任务 DoD 通过，审查门禁通过
- ✅ 全量 `gradlew.bat compileJava compileTestJava cleanTest test --console=plain --no-daemon --max-workers=1 --no-build-cache` → **BUILD SUCCESSFUL，1239 passed / 0 failed**
- ✅ DataPack 矩阵检查：main 102 / generated 218 / intersection 0 / union 320 **PASS**
- ✅ Orphan 缓存检查：cached 789 / files 789 / orphan 0 / stale 0 **PASS**
- ✅ `git diff --check` 0
- ✅ 艾琳全局门禁条件通过（P0-P5 分阶段审查 + 最终全局审查）
- ✅ 无新增注册项、配方字段、模型、语言键
- ✅ ModDev 未出现在任何 DoD/命令/依赖/验收条件中
- ✅ Q4（JEI 总能耗）已被明确归为后置非阻塞建议，不验收
- ✅ 取整规则统一（FE/t→Math.round+最小1守卫 / 容量截断+钳位 / ticks→Math.ceil+Math.max(1)）全部验证
- ✅ 概率输出最坏容量预判（item/fluid 分别按 slot/tank 聚合）零丢失/零地面掉落验证
- ✅ baseFePerTick（含升级不含B）与 totalFePerTick（再乘B）命名严格分离，全文一致
- ✅ powerMultiplier 已接入 efficientIn/effAuc 生命周期（P2-T1），每tick reset→apply→consume 保证无漂移
- ✅ P2-T6 配方 I/O 明确落点于 RecipeMachineBlockEntity.onCookFinish/InputConsumptionPlan/generateItems/generateFluids
- ✅ B_byEnergy 使用 baseFePerTick（不含B）避免循环依赖
- ✅ P0-T6 含四维 B_actual 锁定、概率最坏容量每槽聚合、B=19/B<1/催化剂/fluid/停滞/actual chance 不变等 22 项 RED 测试
- ✅ **附录A继承链准确区分5标准RecipeMachine和3直接ProcessingMachine（Condenser=1000/Encflu=800/Furnace=cookingTime无/2）**
- ✅ **baseFePerTick 最小 1 守卫已同步公式/伪代码/P2-T1/T6/测试/风险/DoD**
- ✅ **Syn 注能 10FE/t 已显式落点于 P2-T5/P2-T7/P3-T1a/Processing共性任务，公共 helper 避免分叉**
- ✅ **非Beacon EffectMachine batch（Quarry×B/Farm×B行/MobRip×B伤害）已写入 P3-T1c**
- ✅ **Furnace/Condenser/Encflu 逐类 batch 已写入 P1-T3a/T3b/T3c，各自独立路径**
- ✅ **标准 RecipeMachine 与自定义 ProcessingMachine batch 路径已分离（P2-T6 仅覆盖 5 标准子类）**
- ✅ **P0-RG 仅工作区恢复门，不声称执行完成**
- ✅ **本地 checkpoint 已创建：`9ff7070 feat: refactor machine energy and batch processing`（未 push/merge）**
- ➡️ **建议下一步：保留当前分支 + 本地 checkpoint。计划作为可复盘事实文件归档。**

---

## 附录 A：处理机子类清单与 baseTickTime 契约（v1.5 修正）

> **`baseTickTime()` 契约**：返回配方处理的**基础 ticks 数**，不含 `durationMultiplier` 升级倍率。`maxProgress = baseTickTime()`（RecipeMachine 及其子类，以及其他 ProcessingMachine 子类）或 `maxProgress = (int)Math.ceil(effectInterval * 20 * durationMultiplier)`（EffectMachine）。
> 
> **三类机器的分类**：
> - **标准 RecipeMachine（5 个）**：继承 `RecipeMachineBlockEntity`（→ `ProcessingMachineBlockEntity`），使用 `FormsCombinedRecipe`、`InputConsumptionPlan`、`onCookFinish()` 标准 I/O 路径。batch 通过 `InputConsumptionPlan` ×B 实现。
> - **直接 ProcessingMachine 自定义 batch（3 个）**：直接继承 `ProcessingMachineBlockEntity`（非 `RecipeMachineBlockEntity`），各自实现独立 `cooking()`/`onCookFinish()` batch 逻辑，不共用 `InputConsumptionPlan`。
> - **RadiusMachine（4 个）**：继承 `EffectMachineBlockEntity`（→ `CmMachineBlockEntity`），效果/区域类机器，无配方 time，各自独立进度逻辑。
>
> **Furnace 特殊规则**：`baseTickTime()` 返回 `recipe.cookingTime()` 纯 ticks，移除当前 `/2`。Furnace 不保留固有 2 倍速度，`durationMultiplier` 由升级系统统一作用。

### 标准 RecipeMachine（使用 InputConsumptionPlan ×B 实现 batch）

| BE 类 | 文件 | 继承链 | machineType | baseTickTime | 备注 |
|---|---|---|---|---|---|
| `PulverizerBlockEntity` | `.../machine/PulverizerBlockEntity.java` | ProcessingMachine→RecipeMachine | PULVERIZER | `currentRecipe.time()` | 标准 RecipeMachine，batch 走 InputConsumptionPlan×B |
| `CompressorBlockEntity` | `.../machine/CompressorBlockEntity.java` | ProcessingMachine→RecipeMachine | COMPRESSOR | `currentRecipe.time()` | 同上 |
| `RefinerBlockEntity` | `.../machine/RefinerBlockEntity.java` | ProcessingMachine→RecipeMachine | REFINER | `currentRecipe.time()` | 同上 |
| `IndfurBlockEntity` | `.../machine/IndfurBlockEntity.java` | ProcessingMachine→RecipeMachine | INDUCTION_FURNACE | `currentRecipe.time()` | 同上 |
| `PsionicantBlockEntity` | `.../machine/PsionicantBlockEntity.java` | ProcessingMachine→RecipeMachine | PSIONICANT | `currentRecipe.time()` | 同上 |

### 直接 ProcessingMachine 自定义 batch（Furnace/Condenser/Encflu，各自独立 batch 逻辑）

| BE 类 | 文件 | 继承链 | machineType | baseTickTime | 备注 |
|---|---|---|---|---|---|
| `FurnaceBlockEntity` | `.../machine/FurnaceBlockEntity.java` | ProcessingMachine（直） | FURNACE | `recipe.cookingTime()`（移除 `/2`） | 使用原版熔炉配方；无固有 2 倍速度；直接继承 ProcessingMachine→独立 onCookFinish；batch 独立实现 |
| `CondenserBlockEntity` | `.../machine/CondenserBlockEntity.java` | ProcessingMachine（直） | MATTER_CONDENSER | **1000**（硬编码，非 `recipe.time()`） | 直接继承 ProcessingMachine；催化剂单份周期语义；旧 `cooking()` 用 `progress += 200*efficiency` 需在 P1-T3b 重写；输出 5mB×B |
| `EncfluBlockEntity` | `.../machine/EncfluBlockEntity.java` | ProcessingMachine（直） | ENCHANTMENT_FLUSHER | **800**（硬编码，非 `recipe.time()`） | 直接继承 ProcessingMachine；自包含 onCookFinish 安全转移/回滚；batch 按可行输入组数执行 |

### RadiusMachine 子类（EffectMachine 线，无配方 time，各自独立进度）

| BE 类 | 文件 | 继承链 | machineType | baseTickTime / maxProgress | 备注 |
|---|---|---|---|---|---|
| `BeaconBlockEntity` | `.../machine/BeaconBlockEntity.java` | CmMachine→EffectMachine→RadiusMachine | BEACON | `maxProgress = Math.ceil(effectInterval×20×durationMultiplier)` | 唯一产生 MobEffectInstance；duration×B，仅 apply 一次 |
| `QuarryBlockEntity` | `.../machine/QuarryBlockEntity.java` | CmMachine→EffectMachine→RadiusMachine | QUARRY | N/A（效果机器，独立进度） | 每周期完成 B 次独立挖掘/产出；mode/radius 控制范围 |
| `FarmBlockEntity` | `.../machine/FarmBlockEntity.java` | CmMachine→EffectMachine→RadiusMachine | FARM | N/A（效果机器，独立进度） | 每周期持续扫描 B 行（按 `currentRowIndex` 推进，不重复/越界） |
| `MobRipBlockEntity` | `.../machine/MobRipBlockEntity.java` | CmMachine→EffectMachine→RadiusMachine | MOB_RIPPER | N/A（效果机器，独立进度） | 每周期 B 次独立随机选择并伤害（可重复选中，每次检查存活/有效性） |

> **继承链总结**：
> - **标准 RecipeMachine（5）**：`CmMachineBlockEntity` → `ProcessingMachineBlockEntity` → `RecipeMachineBlockEntity` → 各子类（Pulverizer/Compressor/Refiner/Indfur/Psionicant）。使用 `FormsCombinedRecipe` + `InputConsumptionPlan` batch 路径。
> - **直接 ProcessingMachine 自定义 batch（3）**：`CmMachineBlockEntity` → `ProcessingMachineBlockEntity` → 各子类（Furnace/Condenser/Encflu）。各自独立 cooking/onCookFinish，不共用 InputConsumptionPlan。
> - **RadiusMachine（4）**：`CmMachineBlockEntity` → `EffectMachineBlockEntity` → `RadiusMachineBlockEntity` → 各子类（Beacon/Quarry/Farm/MobRip）。其中 Beacon 唯一产生 MobEffectInstance；Quarry/Farm/MobRip 各自独立 effect 逻辑。无配方 time 概念。

---

## 附录 B：关键命令行参考（Windows 格式，全部 gradlew.bat + --console=plain）

```powershell
# 编译测试源（仅编译，不运行）
gradlew.bat compileTestJava --console=plain

# 编译生产源
gradlew.bat compileJava --console=plain

# 运行全部测试
gradlew.bat test --console=plain

# 运行指定测试
gradlew.bat test --tests "com.modularmc.ten.api.blockentity.ProcessingMachineContractTest" --console=plain

# DataGen 重新生成
gradlew.bat runClientData --console=plain

# 验证脚本
python scripts/datapack_registration_matrix_check.py
python scripts/generated_orphan_check.py

# 检查 generated 是否与预期一致
git diff --exit-code -- src/generated/resources

# 完整构建
gradlew.bat build --console=plain
```
