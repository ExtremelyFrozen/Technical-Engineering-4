# Compiler Warning Baseline — 26.1.2

## 元数据

| 字段 | 值 |
|------|-----|
| 创建日期 | 2026-07-11 |
| 项目 HEAD | `66f2ff5` feat: establish 26.1.2 dev environment baseline |
| 本地分支 | `feat/26.1.2-datagen-migration` |
| 目标基线 | NeoForge 26.1.2 / Minecraft 26.1.2 / Java 25 |
| 计划归属 | `plans/plan_26_1_2_migration_completion.md` — TASK-002 |
| 下一站 | P4 TASK-045 移除 `-Xlint:-removal` 抑制 |

---

## 1. 当前 suppression 事实

`build.gradle` 第 88 行存在全局抑制：

```groovy
tasks.withType(JavaCompile).configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs << "-Xlint:-removal"
}
```

**效果**：所有 JavaCompile 任务（含 main/test/client/extra）的 `removal` 类编译器警告被静默。
**范围**：整个项目，未按 sourceSet 或 package 做选择性抑制。

---

## 2. 编译命令与结果

### 2.1 `:compileJava`

| 项目 | 值 |
|------|-----|
| 命令 | `.\gradlew.bat :compileJava --rerun-tasks` |
| Exit Code | 0（BUILD SUCCESSFUL） |
| 任务数 | 3 actionable: 3 executed |
| 持续时间 | 23s |

**编译输出摘要**：

```
> Task :compileJava
. 某些输入文件使用或覆盖了已过时的 API.
. 有关详细信息, 请使用 -Xlint:deprecation 重新编译.
. 某些输入文件使用或覆盖了标记为待删除的已过时 API.
. 有关详细信息, 请使用 -Xlint:removal 重新编译.
. 某些输入文件使用了未经检查或不安全的操作.
. 有关详细信息, 请使用 -Xlint:unchecked 重新编译.
```

**警告分类（编译器仅给出定性提示，无精确行数）**：

| 类别 | 编译器是否输出 | 是否被抑制 | 精确计数 |
|------|---------------|-----------|---------|
| deprecation | 摘要行存在 | 否（`-Xlint:-deprecation` 未设） | 未列出（需 `-Xlint:deprecation`） |
| removal | 摘要行存在 | 是（被 `-Xlint:-removal` 抑制明细） | 未列出（需 `-Xlint:removal`） |
| unchecked | 摘要行存在 | 否（`-Xlint:-unchecked` 未设） | 未列出（需 `-Xlint:unchecked`） |

> **关键注意**：编译器仍输出摘要行"某些输入文件使用或覆盖了标记为待删除的已过时 API"，证明存在 removal 相关使用。当前抑制仅隐藏了每个使用点的具体位置，**不应误认为零 removal 警告**。

### 2.2 `:compileTestJava`

| 项目 | 值 |
|------|-----|
| 命令 | `.\gradlew.bat :compileTestJava --rerun-tasks` |
| Exit Code | 1（BUILD FAILED） |
| 任务数 | 9 actionable: 9 executed |
| 持续时间 | 38s |

**编译失败根因**：

2 个测试文件因缺少 GameTest API 依赖而编译失败，共计 **30 个错误**：

1. `src/test/java/com/modularmc/ten/test/logic/NetworkLogicGameTest.java`
   - `import net.minecraft.gametest.framework.GameTest` — 找不到符号
   - `import net.neoforged.neoforge.gametest.GameTestHolder` — 找不到符号
   - `import net.neoforged.neoforge.gametest.PrefixGameTestTemplate` — 找不到符号
   - 以及 14 个 `@GameTest` / `@PrefixGameTestTemplate` 注解使用错误

2. `src/test/java/com/modularmc/ten/test/ui/MachineBlockUITest.java`
   - 同样 3 个 import 找不到符号
   - 以及 10 个注解使用错误

**归属计划**：测试 sourceSet 恢复由 **TASK-090A** 覆盖，不在 TASK-002 范围内。此处如实记录。

> 注意：`compileTestJava` 的 deprecation/removal 警告因编译提前失败而无法观测。

### 2.3 编译日志文件

计划 DoD 要求根目录 `compile_warn_baseline.log` 作为编译证据持久化。

| 项目 | 值 |
|------|-----|
| 路径 | `compile_warn_baseline.log`（项目根目录） |
| 大小 | 13,727 字节（~13.4 KB） |
| 编码 | UTF-8（无 BOM） |
| ANSI 清理 | 是 — Gradle 彩色输出中的 ANSI 控制字符已剥离，保留原始证据文本 |

**日志内容结构**：

| 行范围 | 内容 |
|--------|------|
| 1–48 | `:compileJava --rerun-tasks` 完整输出，含 `BUILD SUCCESSFUL` + exit 0 |
| 51–234 | `:compileTestJava --rerun-tasks` 完整输出，含 `BUILD FAILED` + 30 错误 + exit 1 |

**关键证据行**：
- `compile_warn_baseline.log:45` — `BUILD SUCCESSFUL in 23s`
- `compile_warn_baseline.log:48` — `EXIT CODE: 0`
- `compile_warn_baseline.log:225` — `30 个错误`
- `compile_warn_baseline.log:233` — `BUILD FAILED in 38s`
- `compile_warn_baseline.log:234` — `EXIT CODE: 1`

> **注意**：日志包含本地绝对路径（`E:\GitHub\Technical-Engineering-4\Technical-Engineering-4-26.1.2\`），属于 Gradle 编译输出的正常行为。如需提交，建议审查路径泄露风险或考虑路径脱敏。

**日志归属判断**：计划 DoD（TASK-002）要求 `compile_warn_baseline.log` 存在且包含分类统计。日志作为编译证据，建议纳入 TASK-002 提交工件。若审查认定路径泄露不可接受，可移除后重新生成（路径来源于 Gradle 标准输出，不影响证据效力）。

---

## 3. 静态 API 使用矩阵

> 统计方法：
> - 工具：`rg` (ripgrep) 等价搜索，限定 `src/` 目录，排除 `.git/`、`build/`
> - 分类：import = `import` 语句；comment = javadoc/行注释/TODO；real_call = 类型引用、变量声明、方法参数、匿名类实现、静态方法调用等
> - 第三方、build、generated 目录不计
> - 区分 production（`src/main/java/` + `src/client/java/`）与 test（`src/test/java/`）

### 3.1 总览

| API | 总匹配 | import | comment | real_call | 文件数 | 生产/测试 |
|-----|--------|--------|---------|-----------|--------|----------|
| `IItemHandler` | 28 | 6 | 5 | 17 | 6 (prod) | 28/0 |
| `IItemHandlerModifiable` | 0 | 0 | 0 | 0 | 0 | — |
| `ItemStackHandler` | 3 | 1 | 1 | 1 | 2 (prod) | 3/0 |
| `IFluidHandler` | 38 | 7 | 4 | 27 | 8 (prod) + 1 (test) | 37/1 |
| `FluidTank` (neo) | 4 | 1 | 0 | 3 | 1 (prod) | 4/0 |
| `IEnergyStorage` | 26 | 5 | 4 | 17 | 6 (prod) | 26/0 |
| `EnergyStorage` (neo) | 3 | 1 | 0 | 2 | 1 (prod) | 3/0 |
| `SlotItemHandler` | 3 | 1 | 0 | 2 | 1 (prod) | 3/0 |
| `InvWrapper` | 2 | 1 | 0 | 1 | 1 (prod) | 2/0 |
| `FluidUtil` | 2 | 1 | 0 | 1 | 1 (prod) | 2/0 |
| `FluidAction.SIMULATE` | 4 | — | — | 4 | 3 (prod) | 4/0 |
| `FluidAction.EXECUTE` | 5 | — | — | 5 | 4 (prod) + 1 (test) | 4/1 |
| `serializeNBT` / `deserializeNBT` | 1 | 0 | 1 | 0 | 1 (prod) | 1/0 |
| **合计** | **119** | **24** | **15** | **80** | **~20 文件** | **117/2** |

### 3.2 生产源码详细分布

#### IItemHandler（6 文件，17 real_calls）

| 文件 | imports | comments | real_calls | 说明 |
|------|---------|----------|------------|------|
| `TransferNetworks.java` | 1 | 0 | 4 | 返回值、方法参数 |
| `PipeBlockEntity.java` | 1 | 0 | 6 | 匿名实现、局部变量引用 |
| `FormsCombinedRecipe.java` | 1 | 0 | 1 | 方法参数类型 |
| `FormsCombinedIngredient.java` | 1 | 0 | 1 | 方法参数类型 |
| `CapabilityAdapters.java` | 1 | 4 | 4 | import + javadoc + `IItemHandler.of()` 桥接 |
| `CmMachineBlockEntity.java` | 1 | 0 | 2 | 返回类型 + 匿名实现 |

#### ItemStackHandler（2 文件，1 real_call）

| 文件 | imports | comments | real_calls | 说明 |
|------|---------|----------|------------|------|
| `MachineItemHandler.java` | 1 | 0 | 1 | `extends ItemStackHandler` |
| `PipeBlockEntity.java` | 0 | 1 | 0 | TODO 注释 |

#### IFluidHandler（8 生产文件，27 real_calls）

| 文件 | imports | comments | real_calls | 说明 |
|------|---------|----------|------------|------|
| `CapabilityAdapters.java` | 1 | 4 | 4 | `IFluidHandler.of()` 桥接 |
| `CmMachineBlockEntity.java` | 1 | 0 | 8 | 返回类型 + 匿名实现 ×2 + 方法参数 |
| `TransferNetworks.java` | 1 | 0 | 7 | 方法参数 + drain/fill 调用 |
| `RecipeMachineBlockEntity.java` | 1 | 0 | 2 | fill 调用 |
| `CondenserBlockEntity.java` | 1 | 0 | 2 | fill 调用 |
| `EncfluBlockEntity.java` | 1 | 0 | 1 | FluidAction.EXECUTE 使用 |
| `FormsCombinedRecipe.java` | 1 | 0 | 1 | 方法参数 |
| `FormsCombinedIngredient.java` | 1 | 0 | 1 | 方法参数 |

#### FluidTank — `net.neoforged.neoforge.fluids.capability.templates.FluidTank`（1 文件，3 real_calls）

| 文件 | real_calls | 说明 |
|------|-----------|------|
| `MachineFluidTank.java` | 3 | `extends FluidTank` + 2 个构造器 super 调用 |

#### IEnergyStorage（6 文件，17 real_calls）

| 文件 | imports | comments | real_calls | 说明 |
|------|---------|----------|------------|------|
| `CapabilityAdapters.java` | 1 | 4 | 5 | `IEnergyStorage.of()` 桥接 |
| `CmMachineBlockEntity.java` | 1 | 0 | 2 | 返回类型 + 匿名实现 |
| `CableBlockEntity.java` | 1 | 0 | 5 | 返回类型 + 局部变量 + getEnergy 调用 |
| `TransferNetworks.java` | 1 | 0 | 3 | 方法参数 |
| `EnergyUnitHandler.java` | 1 | 0 | 2 | 局部变量 |
| `CommonProxy.java` | 0 | 0 | 1 | 匿名实现 |

#### EnergyStorage — `net.neoforged.neoforge.energy.EnergyStorage`（1 文件，2 real_calls）

| 文件 | real_calls | 说明 |
|------|-----------|------|
| `MachineEnergyStorage.java` | 2 | `extends EnergyStorage` + 构造器 super 调用 |

#### SlotItemHandler（1 文件，2 real_calls）

| 文件 | real_calls | 说明 |
|------|-----------|------|
| `TENMachineBlockUIFactory.java` | 2 | `new SlotItemHandler(...)` ×2 |

#### InvWrapper（1 文件，1 real_call）

| 文件 | real_calls | 说明 |
|------|-----------|------|
| `ItemNBTHelper.java` | 1 | `new InvWrapper(inv)` |

#### FluidUtil（1 文件，1 real_call）

| 文件 | real_calls | 说明 |
|------|-----------|------|
| `BaseMachineBlock.java` | 1 | `FluidUtil.interactWithFluidHandler(...)` |

### 3.3 测试源码分布

| 文件 | API 使用 | 行数 | 类型 |
|------|---------|------|------|
| `NetworkLogicGameTest.java` | `IFluidHandler.FluidAction.EXECUTE` | 1 | 完整限定名，drain 调用 |

### 3.4 使用模式分析

- **直接实现/继承**（最高迁移成本）：`CmMachineBlockEntity` 的 2 个匿名 `IEnergyStorage`/`IFluidHandler`、`PipeBlockEntity` 的匿名 `IItemHandler`、`MachineItemHandler extends ItemStackHandler`、`MachineFluidTank extends FluidTank`、`MachineEnergyStorage extends EnergyStorage`
- **桥接层**（可保留）：`CapabilityAdapters.java` 中 `IItemHandler.of()` / `IFluidHandler.of()` / `IEnergyStorage.of()` — 这些是 NeoForge 提供的反向兼容桥接
- **方法签名引用**（需更新接口）：`TransferNetworks` 的 `moveItems(IItemHandler, IItemHandler)`、`moveFluid(IFluidHandler, IFluidHandler)`、`moveEnergy(IEnergyStorage, IEnergyStorage)`
- **传输调用**（需迁移 API）：`fill(FluidAction)`, `drain(FluidAction)`, `insertItem`, `extractItem`
- **GUI 绑定**（低风险）：`SlotItemHandler` 构造器
- **工具类**（低风险）：`InvWrapper`, `FluidUtil`
- **NBT 序列化**：当前仅有注释引用 `serializeNBT/deserializeNBT`，无实际调用

### 3.5 统计局限

1. 仅统计源码文本匹配，未区分**编译期实际触发 deprecation/removal 警告**的使用
2. 部分 `IEnergyStorage` 等引用在 javadoc `{@link}` 中属于注释分类，编译器不产生警告
3. `client/` sourceSet 的 4 文件包含 `FluidTank` 方法名（`drawFluidTank`），但并非 import 旧 API——已排除
4. 无法区分来自 NeoForge bundle 的 deprecation 与来自 Minecraft/第三方库的 deprecation
5. 统计数据基于 **rg 文本匹配**，不经过编译器的 deprecation 标记判定

---

## 4. 兼容层退出计划（TASK-040~045）

按计划 P4 阶段分步迁移：

| TASK | 迁移内容 | 关键接口 | 预期文件数 |
|------|---------|---------|-----------|
| TASK-040 | 能量存储 `IEnergyStorage` → `EnergyHandler` | `IEnergyStorage`, `EnergyStorage` | ~6 |
| TASK-041 | 物品存储 `IItemHandler` → `ItemStacksResourceHandler` | `IItemHandler`, `ItemStackHandler`, `SlotItemHandler`, `InvWrapper` | ~7 |
| TASK-042 | 流体存储 `IFluidHandler` → `FluidStacksResourceHandler` | `IFluidHandler`, `FluidTank`, `FluidUtil`, `FluidAction` | ~9 |
| TASK-043 | Transaction 原子事务迁移 | `FluidAction.SIMULATE/EXECUTE` → `Transaction` | ~4 |
| TASK-044 | Recipe/UI/util 引用更新 | 间接引用 | ~3 |
| TASK-045 | 移除 `-Xlint:-removal` + 零 removal 门禁 | `build.gradle` | 全局 |

**TASK-045 DoD**：
- `build.gradle` 中 `-Xlint:-removal` 行已移除
- `.\gradlew.bat :compileJava` 零 removal 警告（可在 `-Xlint:removal` 开启状态下验证）
- 与 TASK-002 基线对比：removal 警告数量降至 0

---

## 5. 总结

| 项目 | 状态 |
|------|------|
| `:compileJava` exit code 0 ✅ | 通过（有 deprecation/removal 使用但被 `-Xlint:-removal` 抑制） |
| `:compileTestJava` exit code 1 ❌ | 30 个 GameTest 符号找不到错误，归属 TASK-090A |
| 静态 API 矩阵 | 约 **20 个生产文件**、**80 处真实调用** |
| 当前抑制行 | `build.gradle:88 — `-Xlint:-removal`` |
| 文档位置 | `docs/compile_warning_baseline_26.1.2.md` |

**关键风险**：当前 suppression 隐藏了 removal 警告的具体位置，但编译器摘要行已确认存在 removal 使用。P4 迁移完成前**不得**移除 suppression，否则编译将因大量 removal 警告而难以定位新增问题。

**下一步**：进入 P1 TASK-010 CI JDK 版本修正。
