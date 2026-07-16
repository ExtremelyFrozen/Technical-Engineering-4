# 贡献指南

欢迎参与 **Technical-Engineering 4 (Kenergy Engineering: Retechnicalized)** 的开发。  
本文档概述了项目的结构、编码约定和贡献流程，帮助新贡献者快速上手。

---

## 目录

- [项目概览](#项目概览)
- [技术栈](#技术栈)
- [环境准备](#环境准备)
- [包结构](#包结构)
- [代码风格与格式化](#代码风格与格式化)
- [核心实现约定](#核心实现约定)
  - [Registrate 注册](#registrate-注册)
  - [方块继承体系](#方块继承体系)
  - [VoxelShape 自定义](#voxelshape-自定义)
  - [BlockEntity 约定](#blockentity-约定)
  - [数据生成（DataGen）](#数据生成-datagen)
  - [本地化](#本地化)
- [依赖管理](#依赖管理)
- [审查清单](#审查清单)
- [提交规范](#提交规范)

---

## 项目概览

Technical-Engineering 4 是一个 **NeoForge 1.21.1** 科技模组，继承自 TE3 的玩法理念，使用现代化的注册和数据生成流程。

- **Mod ID**: `kenergyengineering`
- **包根**: `com.modularmc.ten`
- **语言**: Java 21（部分工具脚本使用 Kotlin）
- **构建系统**: Gradle + ModDevGradle
- **注册框架**: Registrate（通过自定义 `TENRegistrate` 封装）
- **代码简化**: Lombok
- **代码格式化**: Spotless（Eclipse 格式化配置）

---

## 技术栈

| 组件 | 版本 / 说明 |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.219 |
| Java | 21 |
| Gradle | ModDevGradle 2.0.141 |
| Registrate | MC1.21-1.3.0+67（通过 `TENRegistrate` 封装） |
| Parchment | 2024.11.17（可选的映射层） |
| Lombok | 1.18.38 |
| Spotless | 8.4.0 |
| 配方查看器 | JEI + EMI（可选，二选一加载） |

---

## 环境准备

1. **JDK 21** — 确保 `JAVA_HOME` 指向 JDK 21。
2. **克隆仓库**：
   ```bash
   git clone https://github.com/ModularMCLib/Technical-Engineering-4.git
   cd Technical-Engineering-4
   ```
3. **导入 IDE** — 作为 Gradle 项目导入（IntelliJ IDEA 推荐）。
4. **生成运行配置**：
   ```bash
   ./gradlew idePostSync
   ```
5. **运行客户端**：
   ```bash
   ./gradlew runClient
   ```

> 数据生成由 Registrate 自动触发。如需手动运行数据生成器：
> ```bash
> ./gradlew runData
> ```

---

## 包结构

```
src/main/java/com/modularmc/ten/
├── TEN.java                        # Mod 主入口
├── TENConstants.java               # 常量定义
├── api/
│   ├── blockentity/                # API 层 BlockEntity 接口与抽象（CmBlockEntity, CmMachineBlockEntity）
│   ├── capability/                 # 能力（Capability）封装
│   ├── option/                     # 机器配置选项
│   ├── recipe/                     # 配方 API
│   ├── registry/registrate/        # TENRegistrate（Registrate 自定义子类）
│   └── wrapper/                    # 包装器
├── client/
│   └── gui/                        # 客户端 GUI
│       ├── element/                # GUI 元素组件
│       └── screen/                 # Screen 实现
├── common/
│   ├── block/                      # 方块类
│   │   └── machine/                # 机器方块继承体系
│   ├── blockentity/                # 方块实体
│   │   └── machine/                # 各机器 BE 实现
│   ├── data/                       # 注册声明（TENBlocks, TENItems, TENBlockEntities 等）
│   ├── item/                       # 物品
│   │   └── upgrades/               # 升级组件物品
│   ├── network/                    # 网络
│   │   └── packet/                 # 网络包
│   └── registry/                   # 注册入口
│       └── Registration.java       # TENRegistrate 单例
├── config/                         # 配置
├── core/
│   └── mixin/                      # Mixin
│       └── dev/                    # 开发环境专用 mixin
├── data/                           # 数据生成编排
│   ├── DataGenerators.java         # GatherDataEvent 订阅
│   ├── TENDataGen.java             # DataGen 初始化
│   └── lang/                       # 语言提供器
│       └── TENLangHandler.java     # 中英文翻译入口
├── integration/                    # 模组集成
│   ├── emi/                        # EMI 配方集成
│   ├── jei/                        # JEI 配方集成
│   └── xei/                        # 通用跨平台 REI 集成（TODO）
└── utils/                          # 工具类
```

`src/generated/resources/` 包含 Registrate 自动产出的资源文件——**不要手动编辑**。

---

## 代码风格与格式化

### 格式化工具

项目使用 **Spotless**（Eclipse 格式化器）进行自动格式化。提交前运行：

```bash
./gradlew spotlessApply
```

CI 会执行 `spotlessCheck`，未格式化的代码将被拒绝。

### 关键格式化规则

| 规则 | 值 |
|---|---|
| 缩进 | 4 空格（Tab 转空格） |
| 大括号 | `end_of_line`（同行） |
| 行宽 | 100000（实际不断行，由开发者控制） |
| 编码 | UTF-8 |
| 文件末尾 | 强制换行 |
| 末尾空格 | 删除 |
| import 顺序 | `com.modularmc` → `net` → 空行 → `java` → `javax` → `#`（静态导入） |

### 命名约定

| 元素 | 风格 | 示例 |
|---|---|---|
| 类 | PascalCase | `BaseMachineBlock`, `PulverizerBlockEntity` |
| 方法 | camelCase | `updateConnections()`, `getStateForPlacement()` |
| 字段 | camelCase | `currentTab`, `CORE_SHAPE` |
| 常量/static final | UPPER_SNAKE_CASE | `ZH_NAMES`, `GUI_HANDLER` |
| 包 | 全小写 | `com.modularmc.ten.common.block.machine` |
| 注册名 | snake_case | `machine_smelter`, `tin_ingot` |

### 注释惯例

- **公有 API 方法**使用 Javadoc。
- **内部实现**使用行注释（`//`）。
- 不要在 getter/setter 或显而易见的代码上写注释。
- `@ApiStatus.Internal` 标记不应被外部调用的 API。
- 使用 `// @formatter:off` / `// @formatter:on` 包围需要保留特殊格式的代码段。

---

## 核心实现约定

### Registrate 注册

所有方块、物品、方块实体、流体均通过 `TENRegistrate` 注册。  
注册入口在 `com.modularmc.ten.common.registry.Registration`：

```java
public static final TENRegistrate REGISTRATE = TENRegistrate.create(TEN.MOD_ID, false);
```

**方块注册模式**（在 `TENBlocks` 中）：

```java
// 简单方块 —— 使用 private helper 方法
public static final BlockEntry<Block> TIN_ORE = ore("tin_ore", "Tin Ore", "锡矿石", 3, MapColor.STONE);

// 机器方块 —— 通过 helper 统一 blockstate / model
public static final BlockEntry<HorizontalMachineBlock> MACHINE_SMELTER =
        machine("machine_smelter", "Smelter", "熔炼机");

// 需要自定义 blockstate 的方块 —— 展开链式调用
public static final BlockEntry<HorizontalMachineBlock> CELL = REGISTRATE
        .block("energy_cell", HorizontalMachineBlock::new)
        .lang("Energy Cell")
        .blockstate((ctx, prov) -> TENModels.energyCellBlockstate(prov, ctx.getEntry()))
        .item().model((ctx, prov) -> prov.blockItem(ctx::getEntry)).build()
        .register();
```

**关键规则**：

1. **始终使用 `REGISTRATE` 静态导入**：`import static com.modularmc.ten.common.registry.Registration.REGISTRATE;`
2. **英文名**通过 `.lang()` 设置，**中文名**存入 `ZH_NAMES` LinkedHashMap（DataGen 自动提取）。
3. **方块同时附着物品**：通过 `.item()` 链生成物品形式；不想生成物品时不要调用 `.item()`。
4. **自定义模型**通过 `.blockstate()` 和 `.item().model()` 设置。
5. **`register()` 调用必须在链末尾**，否则条目不会被提交。

**Helper 方法职责**（在 `TENBlocks`、`TENItems` 中）：

- `ore()` — 矿石方块（需铁镐及以上，不同的 MapColor）
- `storage()` / `rawStorage()` — 存储方块
- `machine()` — 水平朝向机器
- `engine()` — 发电机（水平朝向，不同纹理映射）
- `cable()` — 线缆/管道（multipart blockstate）
- `item()` — 简单物品
- `upgrade()` — 升级组件（自定义 Item 子类）

> 新增方块时，先检查现有 helper 是否能复用。如果不能，考虑新增 helper 而非到处展开完整链式调用。

### 方块继承体系

```
Block
└── BaseMachineBlock (implements EntityBlock)
    ├── HorizontalMachineBlock (FACING: 水平方向)
    │   ├── (具体机器方块：Smelter, Pulverizer, Compressor…)
    │   └── CableBased (implements SimpleWaterloggedBlock, 含六向 CONNECTION)
    │       ├── (线缆：Glass Energy Cable, Quartz Cable…)
    │       └── (管道：Item Pipe…)
    └── DirectionalMachineBlock (FACING: 六方向)
        └── (频道：Channel Energy/Item/Fluid)
```

- `BaseMachineBlock` — 提供 `ACTIVE` 状态属性、GUI 打开逻辑、Ticker 分发、流体交互、掉落物处理。
- `HorizontalMachineBlock` — 加入 `FACING`（仅水平），提供 `getStateForPlacement`。
- `DirectionalMachineBlock` — 加入 `FACING`（六方向），使用 `getClickedFace().getOpposite()`。
- `CableBased` — 继承 `HorizontalMachineBlock`，加入 `WATERLOGGED` + 六向 `CONNECTION` 属性，使用 `CORE_SHAPE`（`Block.box(3, 3, 3, 13, 13, 13)`）作为碰撞箱。

### VoxelShape 自定义

- 线缆/管道的核心碰撞箱在 `CableBased` 中定义为 `CORE_SHAPE`。
- 需要非标准形状的方块，覆写 `getShape()` / `getCollisionShape()`。
- 如果形状涉及方向，在 blockstate 中用不同的 ModelFile 而非运行时计算。

### BlockEntity 约定

BlockEntity 通过 `TENBlockEntities` 注册：

```java
public static final BlockEntityEntry<FurnaceBlockEntity> FURNACE = REGISTRATE
        .blockEntity("machine_smelter", FurnaceBlockEntity::new)
        .validBlocks(TENBlocks.MACHINE_SMELTER)
        .register();
```

- 命名与对应方块注册名一致（如 `machine_smelter` ↔ `MACHINE_SMELTER`）。
- 一个 BE 类可以对应多个方块（如 `CableBlockEntity` 对应全部 4 种线缆）。
- 实现 `CmMachineBlockEntity` 或 `CmBlockEntity` 接口以获得通用功能（tick、面配置、能力暴露等）。
- Capability 注册在 `CommonProxy.registerCapabilities()` 中统一完成。

### 数据生成（DataGen）

#### 模型（TENModels）

模型 helper 位于 `com.modularmc.ten.common.data.TENModels`：

| 方法 | 用途 |
|---|---|
| `cubeAll()` | 简单六面贴图方块 |
| `machine()` / `machineActive()` | 机器方块模型（带/不带 active 贴图） |
| `engine()` / `engineActive()` | 发电机模型 |
| `energyCellBlockstate()` | 能量单元 blockstate |
| `channelBlockstate()` | 频道 blockstate（六方向，active 切换） |
| `cableMultipart()` | 线缆/管道 multipart blockstate（核心 + 六向连接 + active 切换） |

#### 语言提供器

- **英文**由 Registrate 的 `.lang()` 自动生成。
- **中文**在 DataGenerators 的 `LanguageProvider` 中产出：
  1. 从 `TENBlocks.ZH_NAMES`、`TENItems.ZH_NAMES`、`TENFluids.ZH_NAMES` 提取方块/物品/流体翻译。
  2. 从 `TENLangHandler.ZH_ENTRIES` 提取额外键（频道、升级说明、机器介绍、进度等）。
  3. 从手写 `zh_cn.json` 文件补全（存在时加载，不存在的键跳过）。
- 中文翻译键覆盖英文，**不要在中文化 JSON 中放置英文 fallback**。

#### DataGen 注册

在 `TENDataGen.init()` 中注册额外的 DataGen Provider：

```java
REGISTRATE.addDataGenerator(ProviderType.LANG, TENLangHandler::init);
```

不要创建新的 `GatherDataEvent` 订阅者——在已有 `DataGenerators` 类中扩展即可。

### 本地化

- **英文**通过 `.lang("English Name")` 直接在注册链中设置。
- **中文**：
  - 方块/物品/流体：在静态 `ZH_NAMES` LinkedHashMap 中添加条目（`put("注册名", "中文")`）。
  - 非注册条目（频道键、GUI 文本、进度等）：在 `TENLangHandler` 的对应 `addXxx()` 方法中添加。
  - `TENLangHandler` 中的 `add(provider, suffix, en, cn)` 方法自动补全 `kenergyengineering.` 前缀。
  - `addRaw(provider, fullKey, en, cn)` 用于完整键。
- 新增中文条目时，同时提供英文原文作为第二个参数。
- 确保中文字符串文件的编码为 **UTF-8**。

---

## 依赖管理

项目使用 **Version Catalog** 双文件管理依赖：

| 文件 | 内容 |
|---|---|
| `gradle/libs.versions.toml` | 核心依赖：Minecraft、NeoForge、ModDevGradle、Spotless、Lombok、Mixin |
| `gradle/forge.versions.toml` | NeoForge 生态依赖：Registrate、Configuration、JEI、EMI、Jade、Sodium、Iris、ModernFix、Spark |

`settings.gradle` 中注册了 `forge` catalog：

```groovy
dependencyResolutionManagement {
    versionCatalogs {
        forge {
            from(files("gradle/forge.versions.toml"))
        }
    }
}
```

在 `build.gradle` 中分别引用：

```groovy
// 从 libs.versions.toml
libs.neoForge
libs.modDevGradle

// 从 forge.versions.toml
forge.registrate
forge.jei
```

**添加依赖时**：

1. 先判断依赖属于核心（`libs.versions.toml`）还是生态（`forge.versions.toml`）。
2. 在新文件中添加 version 和 library 声明。
3. 在 `build.gradle` 或 `dependencies.gradle` 中使用 catalog 引用。
4. **不要在 `build.gradle` 中硬编码版本号**。

---

## 审查清单

提交 PR 前，对照以下清单检查：

### 正确性
- [ ] 新方块/物品是否已在 `TENBlocks` / `TENItems` 中注册并调用了 `register()`？
- [ ] 中文名是否已加入对应的 `ZH_NAMES` / `ZH_ENTRIES`？
- [ ] BlockEntity 是否已注册且 `validBlocks` 正确指向对应方块？
- [ ] 新机器的方块类是否位于正确的继承层级？
- [ ] 涉及 Capability 暴露时，`CommonProxy.registerCapabilities()` 中是否已覆盖？

### 风格
- [ ] 运行 `./gradlew spotlessApply` 后无 diff？(`./gradlew spotlessCheck`)
- [ ] import 顺序正确（com.modularmc → net → java → javax → #）？
- [ ] 没有硬编码的依赖版本号？
- [ ] 无 `System.out` / `printStackTrace`（使用 `TEN.LOGGER`）？

### 资源
- [ ] DataGen 是否覆盖了新方块的 blockstate 和模型？
- [ ] 如果使用 `duplicatesStrategy = DuplicatesStrategy.FAIL`，是否有路径冲突？
- [ ] 需要更新 `CHANGELOG.md`？

### CI
- [ ] 本地能否正常编译：`./gradlew build`？
- [ ] 可选适配器弱验证是否通过：`./gradlew runAdapterTestServer`？

---

## 提交规范

- 使用 **中文或英文** 写提交信息，保持项目内一致。
- 格式：
  ```
  简短标题（不超过 50 字）
  
  可选详细说明，解释变更原因和影响。
  ```
- 关联 Issue：`Closes #123` 或 `Refs #456`。
- 保持提交粒度合理：一个提交对应一个逻辑变更。
- 提交前运行 `./gradlew spotlessApply` 确保格式化通过。

---

*最后更新：2026-05-14*
