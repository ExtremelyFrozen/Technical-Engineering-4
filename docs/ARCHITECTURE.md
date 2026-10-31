# Kenergy Engineering Retechnicalized 架构手册

给接手这套代码的人：这页讲代码怎么组织、东西放在哪、改哪里。
玩法自述在 `../README.md`，注册与编码约定在 `../CONTRIBUTING.md`，待发改动在 `../CHANGELOG_UNRELEASED.md`，
发版流程在 `RELEASE.md`，未完成事项在 `TODO.md`。本手册不重复它们。

版本基线：Minecraft 1.21.1 / NeoForge 21.1.243 / Java 21 / LDLib2 2.2.44。
取数口径是 `gradle/libs.versions.toml`、`gradle/forge.versions.toml` 与 `gradle.properties`，构建脚本里的版本目录才是权威。

三条工程约束：

- **生成资源不手改**：`src/generated/resources/` 由 DataGen 产出。模型、blockstate、语言、配方要改生成器。
- **客户端边界**：客户端类放 `src/client/java`；`src/main` 里的 `client` 子包必须带 `Dist.CLIENT` 防护。
  可选集成的类型名不得漏进总是加载的类。
- **同步在主线程**：`CmBlockEntity` 关闭了 LDLib2 的异步 sync 线程，改为主线程 tick 末尾 `passivelySync()`。
  不要恢复异步线程。

---

## 1. 源集与包地图

### 源集

| 源集 | 位置 | 说明 |
|---|---|---|
| `main` | `src/main/java` | 公共与服务端：方块、方块实体、注册、配方、GUI 工厂、网络 |
| `client` | `src/client/java` | 只在客户端加载：客户端初始化、BER、JEI/Jade |
| `test` | `src/test/java` | NeoForge GameTest，走 `runGameTestServer`，不是 JUnit |
| `extra` / `clientExtra` | 构建脚本声明的运行源集 | 服务端 / 客户端运行配置分别挂这两个源集；当前没有独立源文件 |
| `generated` | `src/generated/resources` | DataGen 输出，构建时并入 main 资源 |

`src/main/templates/META-INF/neoforge.mods.toml` 是模组元数据模板，构建期展开；不要改构建产物。

### 包地图

| 包 | 文件数 | 职责 |
|---|---:|---|
| （根包） | 2 | 模组入口 `TEN`、常量 `TENConstants` |
| `api/blockentity` | 10 | BE 抽象体系：同步基座、机器基类、处理/效果/引擎/配方/范围机器 |
| `api/capability` | 3 | 能量、物品、流体能力封装 |
| `api/option` | 7 | 面配置、机器类型、红石模式、冷却剂 |
| `api/recipe` | 9 | `FormsCombinedRecipe` 配方体系与序列化 |
| `api/registry/registrate` | 1 | `TENRegistrate` |
| `api/transmission` | 7 | 线缆/管道传输网络（另有 `energy/` 3、`item/` 3） |
| `client`（main 源集） | 2 | 键位与按键处理，带 `Dist.CLIENT` 防护 |
| `common` | 1 | `CommonProxy`：注册入口与 capability 注册 |
| `common/block` | 4 | 矿石、金属块、原矿块等基础方块 |
| `common/block/machine` | 5 | 方块继承体系（`BaseMachineBlock` 及其派生） |
| `common/blockentity` | 5 | 线缆、管道、传输网络与燃料匹配 |
| `common/blockentity/channel` | 6 | 频道方块实体与共享存储门面 |
| `common/blockentity/machine` | 21 | 各机器 BE 实现 |
| `common/channel` | 4 | `ChannelKey` / `ChannelType` / `ChannelRegistry` / `SharedStorage` |
| `common/data` | 11 | 注册声明、模型 helper、配方类型、标签、拆解服务 |
| `common/gui` | 4 | LDLib2 UI 工厂、进度条、流体槽、传输模式按钮 |
| `common/item` | 8 | 物品、能量单元、频道连接器、扳手等 |
| `common/item/upgrades` | 17 | 升级组件与升级常量 |
| `common/registry` | 1 | `Registration`：`TENRegistrate` 单例 |
| `component` | 1 | `EnergyUnitData` 数据组件 |
| `config` | 2 | `TENConfig` / `ConfigHolder`，NeoForge `ModConfigSpec` |
| `core/mixin` | 2 | Mixin，含开发环境专用项 |
| `data` | 4 | DataGen 编排、配方生成、原版打包/拆分配方 |
| `data/lang` | 2 | 中英文语言入口与标签本地化 |
| `integration/emi` | 4 | EMI 集成（main 源集） |
| `integration/xei` | 1 | JEI/EMI 共享绘制组件 |
| `network` | 2 | `CustomPacketPayload` 与 JEI 缓存状态机 |
| `utils` | 9 | 通用工具 |
| `src/client/java`（客户端源集） | 12 | 客户端初始化、BER、JEI 5 个、Jade 4 个 |
| `src/test/java` | 4 | GameTest：面暴露、网络逻辑、机器 UI、测试 helper |

### 分层规矩

- `api/` 放会被机器实现或外部复用的抽象与数据对象，不反向依赖具体机器。
- `common/blockentity/machine/` 只放具体机器；横切机制（升级、面配置、批处理锁、主动 IO）在 `CmMachineBlockEntity`。
- `integration/<邻居>` 只写那个邻居的适配。邻居的类型名不得漏进总是加载的类。
- 客户端专属代码不要放进 main 源集；`src/main` 里的 `client` 子包只放必须与公共代码同源、且带 `Dist.CLIENT` 防护的键位类。

---

## 2. 机器框架

### 2.1 方块继承

```
Block
└── BaseMachineBlock (implements EntityBlock, BlockUIMenuType.BlockUI)
    ├── HorizontalMachineBlock (FACING: 水平四向)
    │   └── CableBased (WATERLOGGED + 六向 CONNECTION)
    ├── DirectionalMachineBlock (FACING: 六向)
    │   └── ChannelBlock (频道方块)
    └── 具体机器方块（熔炼机、粉碎机、压缩机…）
```

- `BaseMachineBlock`：`ACTIVE` 状态、GUI 打开、Ticker 分发、流体交互、掉落处理、红石连接。
- `HorizontalMachineBlock`：水平朝向，放置时按玩家朝向决定 `FACING`。
- `DirectionalMachineBlock`：六向朝向，按点击面反向放置。
- `CableBased`：线缆/管道；`WATERLOGGED` + 六向 `CONNECTION`；核心碰撞箱是 `CORE_SHAPE`。

### 2.2 BlockEntity 继承

```
CmBlockEntity (implements ISyncPersistRPCBlockEntity)
└── CmMachineBlockEntity (implements IUpgradableMachine)
    ├── ProcessingMachineBlockEntity
    │   └── RecipeMachineBlockEntity
    ├── EffectMachineBlockEntity
    │   └── RadiusMachineBlockEntity
    └── EngineBlockEntity
```

- `CmBlockEntity`：同步、NBT 持久化、RPC 通道与主线程被动同步。`serverTick()` 在服务端依次执行
  `tick()`、`onEndTick()`、`passivelySync()`；客户端只走 `onClientTick()`。
- `CmMachineBlockEntity`：能量/物品/流体三能力、升级、面配置、批处理锁、主动 IO、范围显示。
  多数子类按机器形态选择下级抽象基类；能量单元与创造能量单元直接继承本类。
- `ProcessingMachineBlockEntity`：逐 tick 加工主循环。子类实现 `baseTickTime()`，可覆盖
  `conditionStart()`、`cooking()`、`onProcessTick()`、`onCookFinish()`。
- `RecipeMachineBlockEntity`：配方查找、配方身份检测、四维 B 锁定、七阶段原子产出（堆叠上限见 `CmMachineBlockEntity`）。
- `EffectMachineBlockEntity`：按 `effectInterval()` 周期触发 `applyEffect()`，不走逐 tick 配方。
- `RadiusMachineBlockEntity`：维护 `radius` 与客户端预览半径，配合 `RangeDisplayBER` 画工作范围。
- `EngineBlockEntity`：燃料或光照驱动产能，能量面强制 OUT 主动推送。

### 2.3 机器清单与基类

| 基类 | 机器 |
|---|---|
| `ProcessingMachineBlockEntity` | 熔炼机、物质结晶器、祛魔机 |
| `RecipeMachineBlockEntity` | 粉碎机、压缩机、感应炉、灵能处理器、精炼机 |
| `EffectMachineBlockEntity` | 冷却器 |
| `RadiusMachineBlockEntity` | 信标模拟机、方块破坏器、方块成型器、农场管理机、生物啃噬者、采矿场 |
| `EngineBlockEntity` | 萃取引擎、金属引擎、生物质引擎、光合引擎 |
| `CmMachineBlockEntity` | 能量单元、创造能量单元 |

熔炼机走原版 `SMELTING` / `BLASTING` / `SMOKING` 配方；其余五类配方机器使用 `FormsCombinedRecipe`。

### 2.4 处理循环与停滞语义

`ProcessingMachineBlockEntity.process()` 的顺序：

1. 客户端直接返回。
2. 光合注能（`LevelupSyn`）先于所有门禁执行；有光就允许补能。
3. 检查 `conditionStart()`、`signalAllowRun()`、`energyAllowRun()`。
4. 计算期望 FE/t：`getActualEfficiency() × getLockedBatchSize()`；越界则停滞。
5. 能量不足、输出满 → 保留进度，等待恢复。
6. 首次进入周期时锁定 `maxProgress = ceil(baseTickTime() × durationMultiplier)`。
7. 原子扣能：先 `simulate` 验证全量可提取，再真实提取；真实提取少于期望直接抛异常。
8. `progress++`，执行 `onProcessTick()`；到 `maxProgress` 调用 `onCookFinish()`，清批处理锁并归零。

停滞的边界：

- 缺能、断信号、输出满：保留进度等待恢复。
- 输入消失或配方失配：处理型/配方型机器的 `conditionStart()` 为 false，进度归零；
  效果型机器在原料消失时同样归零。
- 部分取出：批处理锁定期间保持 `lockedB`，取出部分输入不改变本周期批量。

### 2.5 能量模型

`maxProgress = max(1, ceil(baseTickTime() × durationMultiplier))`。
处理时间由配方与时长升级决定，不再与配方总能耗绑定。实际每 tick 扣能是
`实际效率 × 锁定批量 B`，所以批量越大，总 FE/t 越高，但单件时间不变。

机器 IO 全面取消每 tick 限速：能量侧 `MachineEnergyStorage` 的 `maxReceive`/`maxExtract` 恒为
`Integer.MAX_VALUE`，进出只受储能容量与接收方上限约束（线缆仍按各自档位限速）；物品侧面 capability 的
`extractItem` 不再按 `maxExtractItem` 截断，流体侧本就只有显示镜像、从不截断。三者统一为
「只按面权限 / 槽位类型 / 信号门控，数量由调用方决定」。储能容量仍随 B 缩放，`doBaseData()` 每 tick 重算。
`LevelupStream`（原「解除传输速率限制」）因此不再有可观察效果，保留待升级内容重构重新定义。

左侧能量信息栏读 `energyUse` / `energyUseMax` 两个 `@DescSynced` 镜像：当前消耗是
最近 5 tick 的实测平均（处理型/效果型机器在成功扣能后 `recordEnergyUse(fePerTick)` 记账，
`doBaseData()` 每 tick 推进环形缓冲取均值，停机后随窗口滑出归零）；
最大消耗 = `getActualEfficiency() × 理论 B`，即当前升级配置可达的峰值。

### 2.6 面配置与 capability

- `FaceOption` 定义每面的能量/物品/流体模式；`CmMachineBlockEntity` 为六个方向各存一份。
- `CommonProxy.registerCapabilities()` 遍历所有 `BaseMachineBlock`，按方块实体类型分发：
  - 线缆走 `CableBlockEntity.getEnergy(side)`；
  - 管道走 `PipeBlockEntity.getTransportHandler(side)`；
  - 机器走 `CmMachineBlockEntity.getEnergyStorage(side)` / `getItemHandler(side)` / `getFluidHandler(side)`。
- 面配置数组 `energyFaceData` / `itemFaceData` / `fluidFaceData` 是服务端权威镜像，不持久化、不 `@DescSynced`。
  打开 GUI 时 `buildMachineUI()` 推一次，变更时 `syncAllFacesToClients()` 推一次。
- **观察读与抽取权限分离**：读侧返回机器真实内容，写/抽侧按面权限拒绝。
  这是 AE2 样板供应器阻挡模式能正确判断目标机器是否为空的前提。

### 2.7 升级

升级组件在 `common/item/upgrades/`，由 `IUpgradableMachine` 与 `CmMachineBlockEntity` 消费。
常用维度：

- 时长乘子 `durationMultiplier`：影响 `maxProgress`。
- 功率乘子 `powerMultiplier`：影响每 tick 扣能。
- 批量 `batch`：Shulker +3 / Power +1，服务端每 tick 重算并同步给客户端。
- 范围 `radius`：`LevelupRg` 累加，`RadiusMachineBlockEntity.resetUpgradeEffects()` 每轮重置回基础值。
- 特殊：`LevelupSyn` 光合注能、`LevelupBlast` / `LevelupSmoke` 切换熔炼配方模式。`LevelupStream` 自能量限速取消后为空效果。

### 2.8 冷却剂

冷却器（`CoolerBlockEntity`）按 `effectInterval` 周期把**正面一格**机器的 `progress` 直接推进
`reductionTicks × lockedB` tick；一份冷却剂作用 `uses` 次后消耗。它不启动机器——`conditionStart()`
要求目标机器已在运行。

准入与数值都是数据驱动（`api/option/Coolants`），与金属燃料按品位标签分档同构：

| 标签 | 周期 | 每次推进 | 次数 | 内置成员 |
|---|---|---|---|---|
| `c:coolants` | — | 准入总标签 | — | 三个分档标签 |
| `c:coolants/basic` | 10 s | 20 tick | 64 | `minecraft:ice` |
| `c:coolants/standard` | 8 s | 40 tick | 64 | `minecraft:packed_ice` |
| `c:coolants/premium` | 5 s | 80 tick | 64 | `minecraft:blue_ice` |

数据包或第三方往标签加物品即可新增冷却剂，无需改代码；`Coolants.register(Item, Coolant)`
保留为代码侧覆盖，优先级高于标签。展示条目 `CoolantRecipe` 是伪配方（无 `RecipeType`），
形状与 `EngineFuelRecipe` 一致，由 JEI 在运行时按标签合成。
`usesRemaining` / `coolantTotalUses` 是服务端每 tick 写的 `@DescSynced` 镜像——GUI 进度栏在客户端求值，
不经 `@DescSynced`/RPC 的字段到不了客户端。

### 2.9 槽位堆叠与超 99 持久化

堆叠上限由 `CmMachineBlockEntity.installBatchSlotLimit()` 一处给出，全机器统一：
可进/可出的槽 = `物品 maxStackSize × 理论 B`，不可堆叠物品（工具/武器槽）保持 1，
其余槽用 64。各机器不再自写 provider（旧实现按 `slotInfo` 或硬编码索引各算一份，
且固定 `64 × B` 会凭空放大非 64 上限的物品）。`MachineItemHandler` 的
`getSlotLimit`/`getStackLimit` 在 provider 生效时绕过 `maxStackSize`，并保证不低于槽内既有数量。

超 99 的数量必须走自定义读写：vanilla `ItemStack.CODEC` 的 count 是 `intRange(1, 99)`，
`ItemStackHandler.serializeNBT` 经 `ItemStack.save()` 编码超限堆叠会抛。
`MachineItemHandler` 因此覆写 `serializeNBT`/`deserializeNBT`，以 count=1 走 vanilla 编码、
真实数量另存 int 键 `BigCount`；`SharedStorage`（频道物品存储，上限 64×成员数）与
扩容迁移分支共用同一对 `saveStack`/`readStack`。网络侧不受此限（`OPTIONAL_STREAM_CODEC`
写 varint），所以只有一条持久化通道需要覆盖。

---

## 3. 注册与数据生成

### 3.1 Registrate

所有方块、物品、方块实体、流体走 `Registration.REGISTRATE`（`TENRegistrate` 单例）。
`CommonProxy.onRegister(RegisterEvent)` 的初始化顺序：

```
TENBlocks → TENFluids → TENBlockEntities → TENRecipeTypes → TENItems
```

`TENBlocks` / `TENItems` / `TENFluids` 里的 helper 负责统一模型、blockstate 与语言；具体模式见
`../CONTRIBUTING.md` 的「Registrate 注册」一节。

### 3.2 数据生成

| 入口 | 位置 | 职责 |
|---|---|---|
| `DataGenerators.gatherData` | `data/DataGenerators.java` | `zh_cn` 语言 Provider、材料变体配方、原版打包/拆分配方 |
| `TENDataGen.init` | `data/TENDataGen.java` | 给 Registrate 挂语言 Provider（`TENLangHandler`、`TENTagLangGen`） |
| `TENRecipeGen` | `data/TENRecipeGen.java` | 材料变体配方 |
| `TENVanillaPackGen` | `data/TENVanillaPackGen.java` | 原版打包/拆分配方 |

Registrate 自动产出方块/物品模型、blockstate 与英文语言；`src/generated/resources/` 是输出目录。
中文由多路合并：

1. `TENBlocks.ZH_NAMES` / `TENItems.ZH_NAMES` / `TENFluids.ZH_NAMES`；
2. `TENLangHandler.ZH_ENTRIES` 与 `TENTagLangGen.ZH_ENTRIES`；
3. `TENCreativeModeTabs.ZH_NAMES`、`TENFluids.ZH_FLUID_KEYS` 与 `DataGenerators` 里的硬编码杂项；
4. 手写 `src/main/resources/assets/kenergyengineering/lang/zh_cn.json` 补全。

英文名在注册链的 `.lang("English Name")` 设置，同时维护 `EN_NAMES` 以生成裸键；
`TENItems` / `TENBlocks` 的 `EN_NAMES` 与 `ZH_NAMES` 还会生成 `kenergyengineering.<id>` 裸键。
新增中文条目时同时提供英文原文；中文字符串文件保持 UTF-8。

### 3.3 配方

`FormsCombinedRecipe` 支持物品与流体输入/输出的组合形式，由 `FormsCombinedRecipeSerializer` 序列化。
当前注册五类：

| 配方类型 | 输入槽 | 输出槽 |
|---|---:|---:|
| `pulverizer` | 1 | 4 |
| `compressor` | 2 | 1 |
| `psionicant` | 2 | 1 |
| `induction_furnace` | 3 | 1 |
| `refiner` | 2 | 2 |

配方机器的 `findRecipe()` 返回当前配方；`RecipeMachineBlockEntity` 负责 B 锁定与原子产出。
配方变化会重置进度。

---

## 4. 频道系统

频道是跨维度、跨机器的虚拟共享存储，按类型隔离：

- `ChannelKey(name, type)`：名称 + 类型，同名称不同类型是不同频道。
- `ChannelType`：`ITEM` / `FLUID` / `ENERGY`。
- `ChannelRegistry`：频道注册与查找。
- `SharedStorage`：共享存储本体。

方块实体在 `common/blockentity/channel/`：

- `AbstractChannelBlockEntity`：频道接入、共享存储绑定与生命周期。
- `ChannelItemBlockEntity` / `ChannelFluidBlockEntity` / `ChannelEnergyBlockEntity`：三类频道。
- `ChannelItemHandlerFacade` / `ChannelFluidHandlerFacade`：对外的 capability 门面。

GUI 走「末影箱模式」：频道目录、创建、接入、退出、删除（仅空频道）。Jade 显示接入状态。

---

## 5. 传输系统

线缆与管道在 `api/transmission/` 与 `common/blockentity/`：

- `CableBlockEntity`：能量线缆，四档。基础 200 FE/t（1 kFE 缓冲）、石英 1 kFE/t（20 kFE）、
  蔚蓝 4 kFE/t（50 kFE）、星光 200 kFE/t（无限缓冲）。GUI 旧文案写 1/10/100/无限 kFE，以代码为准。
- `PipeBlockEntity`：物品管道，三档（普通 / 白 / 黑）。
- `TransferNetworks`：网络连接与传输调度。
- `CableBased`：共用方块形态与六向连接状态。

管道连接模式对齐 TE4-New 基准：`NONE` / `NORMAL` / `PUSH` / `PULL`。
源边只有显式 `PULL` 面才主动从设备抽取；默认 `NORMAL` 不再自动搬运。
**旧档已放置管道升级后需要用扳手重新设定模式**，物品不会丢失，留在原容器。

传输系统当前冻结维护：只修 bug，不做结构改造。

---

## 6. GUI、同步与网络

### 6.1 GUI

- `TENMachineBlockUIFactory`：机器 UI 的统一工厂。
- `RevealProgressBar`：渐显进度条，按 UV 裁切显示能量/燃料/进度。
- `TENFluidSlot`：流体槽。
- `TransferModeButtonState`：传输模式按钮状态。
- `RangeDisplayBER`（客户端源集）：范围机器的工作区线框。
- 配置面板与升级槽面板由 LDLib2 的 `ModularUI` 组装；面板底图与钮槽坐标要按纹理实测，不要凭感觉改。

### 6.2 同步

`CmBlockEntity` 实现 LDLib2 的 `ISyncPersistRPCBlockEntity`：

- `useAsyncThread()` 固定返回 `false`。异步线程与主线程写字段存在竞争，实测会把
  `progress` / `maxProgress` 推成 0/0，导致渐显进度条闪烁。
- 主线程 `serverTick()` 末尾统一调用 `passivelySync()`，推送脏字段。
- 持久化字段用 `@Persisted`，客户端同步字段用 `@DescSynced`，RPC 方法用 `@RPCMethod`。
- 面配置的 `int[]` 不走 `@DescSynced`（初始同步不可靠），改由 GUI 打开与变更时主动推送。

### 6.3 网络

- `ToggleEnergyUnitPayload`：`playToServer`，切换玩家背包中能量单元的充能开关。
- `JeiSyncState`：纯状态机，不依赖 JEI/Minecraft，协调客户端配方缓存与会话生命周期；
  设计为可独立测试，当前测试源集尚未覆盖。

---

## 7. 集成

| 邻居 | 位置 | 说明 |
|---|---|---|
| JEI | `src/client/java` + `integration/xei` | 配方查看、幽灵物品、熔炼机原版熔炉催化剂、引擎燃料与冷却剂页 |
| EMI | `integration/emi` | 配方查看与共享绘制组件；运行时待验证 |
| Jade | `src/client/java/com/modularmc/ten/integration/jade` | 频道状态、管道过滤与抽取面 |
| GuideME | `src/client/java/.../client/guide` + `common/data/TENGuideItems` | 可选独立指南；GuideME 缺席时指南物品不注册 |
| AE2 | capability 层 + 开发期依赖 | 样板供应器阻挡模式依赖面观察读语义；不直接 import `appeng` |
| Curios | 编译期可选 | 饰品槽支持 |
| Sodium / Iris / ModernFix | 编译期可选 | 开发期依赖，仓库暂无直接引用 |

集成红线：

- 按 mod id 门禁，门禁类与带类型的桥分开写。
- 编译期可选类型不得出现在总是加载的类里；缺邻居时整包要能启动。
- 开发运行期的 localRuntime 只用于实测，不构成玩家侧硬依赖。

---

## 8. 测试与验证

| 测试 | 覆盖 |
|---|---|
| `MachineFaceExposureGameTest` | 机器面 capability 的暴露与观察读 |
| `NetworkLogicGameTest` | 网络逻辑与同步 |
| `MachineBlockUITest` | 机器 UI 结构 |
| `TENGameTestHelpers` | GameTest 辅助 |

- `./gradlew runGameTestServer` 执行全部 GameTest；`test` 源集不是 JUnit，
  `build.gradle` 已关闭 `failOnNoDiscoveredTests`。
- `./gradlew build` 负责编译与打包，不执行 GameTest；GameTest 用 `./gradlew runGameTestServer` 单独跑，
  CI 的 `test-on-push.yml` 也单独跑它。
- `./gradlew spotlessCheck` 是 CI 格式门禁；提交前跑 `spotlessApply`。
- 界面交互、时序、粒子与渲染仍需 `runClient` 实机验证。

---

## 9. 常见改动指引

**加一台机器**：`TENBlocks` 注册方块 → `TENBlockEntities` 注册 BE 并 `validBlocks` →
选择合适的 BE 基类 → `TENModels` / blockstate → 中英文语言 → `CommonProxy.registerCapabilities()` 已按
`BaseMachineBlock` 自动覆盖，确认你的 BE 类型实现了对应 getter → （可选）JEI/Jade 分类 → GameTest。

**加一个升级**：`common/item/upgrades/` 新建类 → `TENItems` 注册 → `IUpgradableMachine` 的效果分支 →
`TENLangHandler.addUpgradeTips()` 补中英文 → 掉落表或配方（如需要）。

**加一条线缆/管道**：`TENBlocks` 用 `cable()` helper → blockstate/multipart 模型 →
`CableBlockEntity.capacityFor/transferFor` 或 `PipeBlockEntity` 的档位参数 → 语言 → 传输上限测试。

**加一个频道类型**：`ChannelType` 增加枚举 → `ChannelKey` 已按类型隔离 → 新 BE 与 capability 门面 →
`ChannelRegistry` 注册（含类型分支）→ GUI/Jade 显示。

**加一个邻居集成**：新建 `integration/<邻居>`（客户端侧放 `src/client/java/.../integration/<邻居>`）→
门禁类独立 → 只在门禁通过后碰邻居类型 → 语言与配置 → 文档记录依赖下限。

**加一个配置项**：`TENConfig` 对应内部类 → `define` / `defineInRange` → 中英文 lang 键 →
`ConfigHolder` 读取 → 配置屏验证。

**改机器 GUI**：先量纹理与 `panel_*.png` 的钮槽坐标，再改 `TENMachineBlockUIFactory` 与面板类；
改完在 1080p / 720p 与不同 GUI 缩放下对比底图。像素级对齐不要凭目测。

---

## 10. 维护者要守住的东西

- **生成的资源**：`src/generated/resources/` 只由 DataGen 写；手工改会在下次 `runData` 被覆盖。
- **同步线程**：不要恢复 `useAsyncThread() = true`；所有同步走主线程 `passivelySync()`。
- **面 capability**：观察读与抽取权限分离；不要让权限判断把读侧内容藏空。
- **批处理锁**：`lockedB` / `lockedMaxProgress` 在周期开始时锁定，完成后清理；中途改语义会破坏停滞契约。
- **原子产出**：`RecipeMachineBlockEntity` 的快照-模拟-提交不能改成边产边写；失败必须回滚。
- **客户端边界**：可选集成类型不进总是加载的类；客户端类不进 main 源集。
- **版本目录**：新增依赖优先走 `gradle/libs.versions.toml` 与 `gradle/forge.versions.toml`；
  现有硬编码坐标（Lombok、Curios、开发运行期模组、Kotlin 插件）暂按现状，迁移时再收编。
- **更新日志**：玩家可见的改动写 `CHANGELOG_UNRELEASED.md`，体例见 `CHANGELOG_STYLE.md`；不要直接改已封版段落。
