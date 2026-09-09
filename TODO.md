# TODO — 1.21.1 移植清单（源：26.1.2 分支）

> **方向**：把 `Technical-Engineering-4-26.1.2` 分支（MC 26.1.2 / NeoForge 26.1.2.78）的机器功能、设计机制与颠覆性改动，反向移植到本分支（MC 1.21.1 / NeoForge 21.1.219）。
> **优先级规则**：P0 = 根本性/架构级改动（影响面大、被其他功能依赖，必须先行）；P1 = 功能级（依赖 P0 基类就绪）；P2 = 表面级（独立于核心，可最后叠加）。
> **来源标注**：`[CHANGELOG]` = 26.1.2 CHANGELOG 确认的 breaking；`[代码]` = 源码对比确认；`[推测]` = 基于代码影响面分析推断。
> 生成日期：2026-08-31（猫娘指挥官-莉莉丝汇总三路勘探产出）。
> **小黑屋（已冻结）**：能量线缆（CABLE 系列）+ 物品管道（PIPE 系列）传输系统——**保留 1.21.1 现状，不移植、不修改**（D5/P0-7 整项冻结；P0-3 面配置中与线缆/管道交互的部分同步保留现状）。频道（CHANNEL）不在冻结范围。

---

## 0. 版本与差距基线

| 属性 | 26.1.2（源） | 1.21.1（本分支） |
|------|-------------|-----------------|
| Minecraft | 26.1.2 | 1.21.1 |
| NeoForge | 26.1.2.78 | 21.1.219 |
| mod_version | 4.1.0 | 4.1.0 |
| 机器数 | **15 台**（+BlockBreaker/BlockFormer/Cooler） | **12 台**（缺 3 台新增） |
| 引擎/单元 | 4 引擎 + 2 单元 | 同（数量相同） |
| 频道 | 3（SavedData 全局注册表架构） | 3（本地 ConcurrentHashMap 撮合） |
| 管道 | ~~跳跳传输（hop-by-hop）~~ 🔒 冻结：保留 1.21.1 root 撮合旧模型 | 网络 root 撮合旧模型（现状保留） |
| 升级 | 乘法模型（duration/power/batch 乘子） | 旧百分比模型 |
| 能力层 | Resource/Transaction API + CapabilityAdapters 桥 | 经典 Capability（IItemHandler/IFluidHandler/IEnergyStorage） |
| BE 存储 | LDLib2 ValueInput/ValueOutput | CompoundTag 范式 |
| 配置 | NeoForge ModConfigSpec（te4-config.toml） | dev.toma.configuration 注解 |
| JEI | 本地集成移除 → TENRecipeSync 网络同步 | 本地 TENJeiCategory/Plugin |

**关键结论**：两分支是**跨 MC 大版本**移植。26.1.2 的部分改动（尤其能力层 CapabilityAdapters）是给 NeoForge 26.1.2 的 Resource API 做的桥，**移植到 1.21.1 不能照搬，需按经典 Capability 反向适配**（见 P0-6）。

---

## 1. 颠覆性改动清单（26.1.2 → 1.21.1）

| # | 改动 | 类型 | 来源 | 移植影响 |
|---|------|------|------|---------|
| D1 | **能量模型重构**：累加能量→进度 → 固定 FE/t 按 tick 消耗；总能耗 = recipe.time × baseFePerTick × B | 架构 | [代码]/plan_energy-system-refactor | 全部处理型机器 process 逻辑 |
| D2 | **批处理 B 系统**：B_actual 四维锁定（物品/流体/概率输出/能量），运行中不降 B；动态输出槽堆叠（64×B）；快照-回滚原子产出 | 架构 | [代码] | RecipeMachineBlockEntity 主体（1.21.1 的 5.0K → 26.1.2 的 54.5K 即此） |
| D3 | **面配置语义重定义**：OFF/BE_IN/BE_OUT/BOTH/IN/OUT；IN/OUT = 机器主动拉/推（64/tick）且不再对第三方开放；faceData client mirror 全量同步（6 面×3 类） | 架构 | [CHANGELOG] 2.0.0 | 所有机器 + 管道交互；旧档 IN/OUT 语义变化 |
| D4 | **升级系统乘法模型**：durationMultiplier/powerMultiplier/batch 乘子；Aug/Power/Shulker 可重复安装（B_max=19）、Syn 最多 1；Syn 行为变化（时间×1.5 + FE/t×0.8 + 光合注能自补） | 架构 | [代码]/plan | 升级系统整体重写 |
| D5 | ~~管道跳跳传输~~ 🔒 **已冻结（小黑屋）**：26.1.2 移除升级物品（Pull/Push/Speed/Page/Ender）与网络 root 撮合、主动/被动互补角色，改每管独立 tick、内部缓冲 64/tick、逐跳接力、缓冲持久化、扳手配置抽入点 | 架构 | [CHANGELOG] 2.0.0/1.1.0/1.0.0 | **不移植**：1.21.1 保留 root 撮合旧模型与现状，相关代码不动 |
| D6 | **频道架构重构**：ChannelKey/ChannelRegistry(SavedData 跨维度全局共享)/SharedStorage(拒绝缩容)/零 tick 传输（接入即绑定共享存储，join/leave 无需重建 UI） | 架构 | [代码]（新增） | 1.21.1 本地 ConcurrentHashMap 撮合 → 数据模型完全不同 |
| D7 | **停滞语义改变**：旧版停滞 progress 归零重置；新版保留 progress 等待恢复 | 行为 | [代码] | 所有机器挂机行为 |
| D8 | **能力层适配**：26.1.2 用 CapabilityAdapters/EnergyDirectRestorable/Item(Fluid)HandlerResourceAdapter 把经典接口桥到 NeoForge 26.1.2 Resource API | 架构 | [代码]（NeoForge 版本差） | **1.21.1 无 Resource API，此桥不适用**——保持经典 Capability 直连即可（见 P0-6） |
| D9 | **BE 存储迁移**：loadAdditional(CompoundTag)→loadAdditional(ValueInput)（LDLib2 范式） | 架构 | [代码] | 全部 BE 序列化路径；旧 NBT 读取需无条件重置兜底（26.1.2 1.0.0 处理） |
| D10 | **配置系统替换**：dev.toma.configuration → NeoForge ModConfigSpec（COMMON/CLIENT 双 TOML） | 中等 | [代码] | 配置键/范围需对照迁移 |
| D11 | **升级槽固定 6 全解锁 + 扩写升级移除**（upgSize 恒定） | 数据 | [代码] | 旧档升级槽/扩写物品语义失效 |
| D12 | **JEI 集成改造**：本地 TENJeiCategory/Plugin 移除 → TENRecipeSync 网络同步（服务端推配方） | UI | [代码] | JEI 层重写 |
| D13 | **新增 3 台机器 + Coolants 冷却液系统** | 功能 | [代码]（新增） | 纯新增，依赖 P0 基类 |
| D14 | **杂项**：物品改名（能量单元→能量模块、频道桥接器→频道连接器）；新增 CHANNEL_CONFIG DataComponent、WrenchDismantleService、GUI 新组件（RevealProgressBar/TENFluidSlot/TransferModeButtonState/冷却剂进度条） | 表面 | [代码] | 独立，最后处理 |

---

## 2. 移植优先级（根本性优先）

### P0 — 根本性/架构级（先移植，其余功能依赖）

> **决策记录（2026-08-31 用户确认）**：① P0-4 升级照 26.1.2 乘法模型全量移植；② P0-6 能力层在经典 Capability 上实现轻量等价层（行为对齐）；③ LDLib2 升级（风险高，需先验证兼容，见 P0-0）。

- [x] **P0-0 LDLib2 兼容性验证与升级（前置）** ✅ **验证完成（2026-08-31）**
  - 验证结论：① 1.21.1 线 LDLib2 最高版 **2.2.37**（maven.firstdark.dev 权威，2026-08-24 仍维护；当前锁定 2.2.8 非最高，2.2.9~2.2.37 共 29 个更高版本）；② 2.2.37 与 26.1.2.28 的 ProgressBar/SpriteTexture/FieldManagedStorage/Persisted/DescSynced/RPCMethod **API 签名一致**（javap 对比），RevealProgressBar 依赖的 `updateProgressBarStyle(float)`/`onProgressBarStyleChanged()`/`setSprite` 等全部可用；③ SpriteTexture 仅差异为 `of(ResourceLocation)` vs `of(Identifier)`（MC 26.1.2 原版改名，1.21.1 侧天然用 ResourceLocation，无影响）。
  - 动作：1.21.1 依赖升级 2.2.8 → **2.2.37**（gradle/forge.versions.toml），升级后跑 compileJava 验证（同线小版本，风险低）。
  - 结论：P2-1 GUI 组件可随 2.2.37 直接移植；P1-2 BE 存储**必保留 CompoundTag**（MC 原版 API 差异，与 LDLib2 无关）。

- [x] **P0-1 能量模型重构**（D1）✅ **代码落地（2026-08-31）**
  - process() 七步重写：client guard → tryInjectPhotosynEnergy → conditionStart/signal/energy 门禁 → fePerTick=eff×B（越界保护）→ 能量不足/输出满暂停（保留 progress）→ maxProgress 兜底锁定（baseTickTime×durationMultiplier）→ 原子能量扣减（simulate→execute，失败快失败）→ progress++ → onProcessTick 钩子 → 完成清锁。
  - 新增文件：`utils/SkyLightHelper.java`（getOverworldClockTime→getDayTime 适配）、`common/item/upgrades/UpgradeConstants.java`（SYN_PHOTOSYN_FE=10 等常量）。
  - 基类改动：CmMachineBlockEntity 加批处理锁基础（lockedB/lockedMaxProgress + hasLockedBatch/clearLockedBatch/lockBatchForNewOperation/getLockedBatchSize/hasLockedMaxProgress/lockMaxProgressForNewOperation）、tryInjectPhotosynEnergy、photosynInstalled/durationMultiplier 字段、energyAllowRun lockedBatch 感知。
  - 验证：compileJava 通过（增量 12s）。行为验证（游戏内能量/进度）待 P0-2/3/4 基类稳定后统一做。
  - 遗留：EffectMachineBlockEntity.process()（效果型）能量模型未对齐（P0-2 范围）。
- [x] **P0-2 批处理 B 系统**（D2）✅ **代码落地（2026-08-31）**
  - RecipeMachineBlockEntity 整体移植 26.1.2 版（54.7K，对齐 54.5K）：conditionStart 身份检测（recipe ID 变化→重置 progress+清 B 锁）+ 四维 B 锁定（validateAndLockB：物品/流体/概率最坏输出/储能，钳位 0..19）+ Q1 Fixed-B 契约（锁定后缺料 stall 不缩 B，revalidateInputs 恢复）；installDynamicSlotLimit（输入/输出槽 64×理论B 动态堆叠）；onCookFinish 七阶段原子产出（InputConsumptionPlan 快照-回滚 + collectBatchOutputs 内存预收集 + validatePendingOutputsFit 预验证 + 双快照回滚）；催化剂（chance≤0）不随 B 倍增。
  - 新增：`api/blockentity/BatchMath.java`（纯数学无 MC 依赖）；MachineItemHandler 升级（setDynamicSlotLimit/getEffectiveSlotLimit/getStackLimit 覆盖超堆叠）；FormsCombinedIngredient 加 rolls（默认 1）；CmMachineBlockEntity 加 batch 字段 + getTheoreticalBatchSize + calculateBActual + validateAndLockB。
  - 适配：Identifier→ResourceLocation；Item.ABSOLUTE_MAX_STACK_SIZE→本类常量 99。
  - 验证：compileJava 通过（增量 14s）。配方机器子类（Pulverizer/Compressor/Refiner/Indfur/Psionicant）不 override 基类，自动继承批处理逻辑（与 26.1.2 一致）。行为验证（批处理契约：B 锁定不降级/stall 保 progress/概率 B 次产出）待基类稳定后游戏内统一做。
- [x] **P0-3 面配置主动/被动 IO 语义 + faceData 全量同步**（D3）✅ **代码落地（2026-08-31）**
  - canReceiveItem/canExtractItem 语义对齐：仅 BE_IN/BOTH 允许外部插入、BE_OUT/BOTH 允许外部提取；IN/OUT 归机器主动（不再开放外部）。
  - faceData mirror 去 @Persisted @DescSynced（int[] 同步不可靠）→ rebuildFaceData()（从 faceMode maps 重建，返回变化）+ syncAllFacesToClients()（RPC 全推 6 面×3 类）；doBaseData 变化时全推 + buildMachineUI 打开 GUI 全推 + rpcCycleFaceMode 全量同步。
  - doActiveItemIo/activePullItems/activePushItems/insertIntoInputSlots（IN 拉/OUT 推 64/tick，相邻为管道时跳过——兼容管道小黑屋现状）；doActiveEnergyIo（引擎/单元类机器主动推能量）；Cell/CreativeCell tick 调用（能量单元直供相邻机器）。
  - 验证：compileJava 通过（增量 8s）。FaceOption 两分支本就一致（6 值语义），无改动。
- [x] **P0-4 升级系统乘法模型**（D4）✅ **代码落地（2026-08-31，用户确认全量移植）**
  - IUpgradableMachine 接口整体替换为 26.1.2 版（applyDurationMultiplier/applyPowerMultiplier/applyBatchIncrease/applyPhotosyn/setRecipeMode/setUnlimitedEnergyTransfer default API + RECIPE_MODE 常量）。
  - 13 个 Levelup 类全部更新为乘法模型：Aug(×0.75/×1.30)、Power(×0.60/×1.50/batch+1)、Shulker(×0.40/×2.00/batch+3)、Syn(×1.5/×0.8/光合注能，PROCESS/EFFECT only)、Rg(+50% 半径)、Blast/Smoke(配方模式切换)、Ice/Magma/Mineral(Quarry 模式标记)、Know(Furnace XP 标记)、Stream(无限能量传输)；canApply 门控分离。
  - 新增：UpgradeTooltipFormatter（tooltip 格式化，Locale.ROOT）、UpgradeInstallHelper（快捷安装纯逻辑）。UpgradeItem 更新（canApply + appendHoverText 用 formatter）。
  - 基类：powerMultiplier/recipeMode/unlimitedEnergyTransfer 字段；getActualEfficiency = initialEfficientIn×powerMultiplier；applyUpgradeEffects 单次计算 + canApply 门控；resetUpgradeEffects 重置乘法字段（lockedB/lockedMaxProgress 不动）；validUpgrade（Syn 唯一/Blast↔Smoke 互斥）；doBaseData Stream unlimited 处理；getUnlockedUpgradeSlots = 6 全解锁。
  - 验证：compileJava 通过（增量 46s）。
- [x] **P0-5 停滞语义修正**（D7）✅ **代码落地（2026-09-01）**  - EffectMachineBlockEntity.process() 移植 26.1.2 版：client guard + tryInjectPhotosynEnergy + B 锁定（B_byEnergy 约束，钳位 0..19）+ maxProgress=ceil(effectInterval×20×durationMultiplier) + fePerTick=getActualEfficiency×B（越界保护）+ 能量不足/输出满暂停（保留 progress）+ 原子能量扣减（simulate→execute，fail-fast）+ else 分支保留 progress 不归零；新增 cooking() 钩子（默认 false）。
  - 子类停滞统一：Farm 加 cooking()（输出槽 6..11 剩余容量 <2 停滞，每周期 1 行最多 2 件）；Quarry 加 cooking()（输出槽 1..12 剩余容量 <1 停滞，每周期至多 1 格）。
  - Condenser 整体对齐 26.1.2 重构版：conditionStart 四维 B 锁定（输入/罐容量 floor(avail/5mB)/能量）+ maxProgress 锁定 + 催化剂缺失清锁；cooking() 改纯容量谓词（B×5mB，移除 progress=0 残留）；催化剂消耗移入 onProcessTick（每 20 处理 tick 1 个，停滞 tick 不消耗）；onCookFinish 产出 B×5mB + 清锁。
  - RecipeMachine/ProcessingMachine 停滞语义已在 P0-1/2 落地（能量不足/输出满/缺料 stall 均保留 progress），Furnace/Encflu/Beacon/MobRip 无归零残留，无需改动。
  - 验证：compileJava 通过（增量 12s）。行为验证（输出满停滞保留 progress、Condenser B 锁定）待游戏内统一做。
  - 审查（2026-09-01）：艾琳门禁通过（success），无阻断项；留档 3 轻微项：① Farm cooking 注释已修正（一格最多 2 件）；② Condenser catalystTickCounter 非 @Persisted（重启后节奏提前一次，与 26.1.2 一致，可接受）；③ Farm/Quarry cooking 阈值硬编码 2/1 耦合 1.21.1 每周期 1 行/1 格——若未来 applyEffect 接入 B 行批量需同步改 2*B/B。
- [x] **P0-6 能力层适配判断**（D8）✅ **核对完成（2026-09-01）**
  - ⚠️ 特殊项：26.1.2 的 CapabilityAdapters 是为了桥到 NeoForge 26.1.2 的 Resource/Transaction API；**1.21.1 是经典 Capability**，此桥**不适用、不要照搬**。
  - 动作（用户确认）：在 1.21.1 经典 Capability 上实现轻量等价层——机器 IN/OUT 面自拉/自推 64/tick、能量单元直供电相邻机器，行为与 26.1.2 对齐（无 Resource API 可用，需自行实现）。
  - 核对结论：轻量等价层主体已由 **P0-3 顺带落地**且与 26.1.2 逐行对齐：① doActiveItemIo（IN 拉/OUT 推，ACTIVE_IO_RATE=64，相邻管道跳过）；② doActiveEnergyIo（OUT/BOTH 能量面推送，Cell/CreativeCell tick 调用直供相邻机器）；③ canReceiveItem/canExtractItem（仅 BE_IN/BE_OUT/BOTH 开放外部，IN/OUT 归机器主动）；④ canReceiveEnergy/canExtractEnergy（isIn/isOut+BOTH，与 26.1.2 逐行一致）。
  - 剩余：**游戏内面配置行为验证**（IN/OUT 机器主动拉推、BE_IN/BE_OUT 外部插取门控）——compileJava 已过，运行时行为待统一游戏内测试（与 P0-1/2/3/4/5 行为验证合并做）。
- [ ] **P0-7 ~~管道跳跳传输重构~~** 🔒 **已冻结（小黑屋）**：1.21.1 保留 root 撮合现状，不移植、不修改（含线缆能量传输）。相关代码保持现状。
  - 注意：P0-3 面配置主动 IO 中「相邻为管道时跳过」的语义依赖 26.1.2 跳跳管道；冻结后，1.21.1 面配置移植需按现有 root 撮合管道定义交互（待用户确认细节）。
- [x] **P0-8 频道架构重构**（D6）`难度：高` ✅ **BE 层代码落地（2026-09-01）**
  - ✅ 数据层：common/channel/ 新增 ChannelKey(name,type)、ChannelType(ITEM/FLUID/ENERGY)、SharedStorage（物品 9 槽×64×成员数 / 流体 2 tank×2000mB×成员数 / 能量 kFE(10)×成员数，拒绝缩容）、ChannelRegistry（SavedData：懒加载 computeIfAbsent + 事件驱动保存 + 跨维度统一经 server.overworld()）。1.21.1 适配：SavedDataType/Codec → SavedData.Factory(BiFunction deserializer) + CompoundTag NBT；MachineEnergyStorage 补 setCapacity；FluidStack 序列化改用 FluidTank.writeToNBT/readFromNBT（1.21.1 无 writeToNBT/loadFromNBT 静态）。
  - ✅ BE 层（2026-09-01）：AbstractChannelBlockEntity 移植 26.1.2 版（891 行对齐：join/leave 接入退出、本地缓冲回流 pushLocalToShared、零 tick 传输、频道名正则 `[\w\u4e00-\u9fa5\- ]{1,16}`、生命周期 setRemoved 退频道、RPC 目录同步/创建/接入/退出/删除）；3 个频道 BE（Item/Fluid/Energy）改绑 SharedStorage（接入态面能力指向共享 handler，容量镜像字段 tick 修正）；ChannelItemHandlerFacade（IItemHandlerModifiable 动态门面，客户端按成员数推 64×n 槽上限）；ChannelFluidResourceFacade→**ChannelFluidHandlerFacade**（1.21.1 经典 Capability IFluidHandler 适配，26.1.2 的 Resource API 门面不适用）；ChannelBlock 方块类（定制 VoxelShape 六朝向静态表）；TENDataComponents CHANNEL_CONFIG（频道连接器复制/应用载荷）+ ChannelConnectorItem 重写（复制→应用→清空，含 channelId join）；TENConstants 补频道 UI 素材常量（CHANNEL_LIST_BG/CHANNEL_ENTRY_*/CHANNEL_ENTRY_STATE/CHANNEL_BUTTONS/SheetUV）；6 个 modular 频道素材 + machine_gui.png 从 26.1.2 复制；lang 键更新（channel.create/current/delete/leave/none/not_joined + channel_connector.*，zh_cn 物品名频道桥接器→频道连接器）。
  - 1.21.1 API 适配记录：`Identifier`→`ResourceLocation`；RPC 方向判定 `sender.isRemote()`→`sender.isServer()`（C→S 用 RPCSender.ofServer() 直接调用，S→C 用 rpcToTracking 广播替代 rpcToPlayer 单播）；`HoverTooltips.create()`→`new HoverTooltips(List.of(..), null, null, null)`（2.2.37 无 create 静态）；`MachineEnergyStorage` 补 getMaxReceive/getMaxExtract；`Item.use()` 返回 InteractionResultHolder（1.21.1 API）；`hasUpgrade()` 频道返回 false（1.21.1 无 supportsUpgradeSlots 两阶段控制）；机器 UI 槽位用 `machineSlot`（1.21.1 无 channelItemSlot）。
  - 验证：compileJava 通过（EXIT=0）。行为验证（join/leave 跨维度共享、零 tick 传输、频道目录 UI）待游戏内统一做。
  - 遗留：IModeChangable 接口孤儿化（旧链接模式遗留，无引用，可后续删除）；26.1.2 的 Jade ChannelJadeProvider（未移植，属集成层可选）。

### P1 — 功能级（依赖 P0 基类就绪）

- [x] **P1-1 新增 3 台机器**（D13）✅ **代码落地（2026-09-01）**
  - 方块破坏器 Block Breaker（RadiusMachine）：面向 B 格深×宽 ±(radius-1)，槽 0 工具 + 1..12 输出，破坏规则同采矿场普通模式（不破坏含 BE 方块、需正确工具含挖掘等级）；effectInterval 3s；applyEffect 批量+范围语义、canFitAll/fitAll 快照-模拟-提交防丢失、installDynamicOutputLimit 动态槽上限、cooking 输出满停滞、conditionStart 范围判定。
  - 方块成型器 Block Former（RadiusMachine）：槽 0 当前放置物块 + 1..12 候选栏（BOTH），需 canSurvive；effectInterval 5s；refillFromCandidates 候选栏自动补充；applyEffect 批量+范围放置。
  - 冷却器 Cooler（EffectMachine）：冷却剂→正面一格机器减耗时；新增 Coolants 注册中心（冰 10s/20tick×64 次、浮冰 8s/40tick、蓝冰 5s/80tick）+ Coolant record（intervalSeconds/reductionTicks/uses，入参校验）；双进度条 GUI（主进度 + 冷却剂进度）；effectInterval 由冷却剂数据驱动；applyEffect 推进正面机器 progress（safeMultiply×B）。
  - 基类补充：MachineType 加 BLOCK_BREAKER=24/BLOCK_FORMER=25/COOLER=26；CmMachineBlockEntity 加 safeMultiply/effectiveToolForDrops/getRangeBoxes。
  - 注册：TENBlocks 三台机器（machine helper）+ TENBlockEntities 三个 BE；纹理 machine_block_breaker/former/cooler (+_active) 从 26.1.2 复制；GUI 用 machine_gui 背景 + machineSlot/energyGauge/progressGauge（1.21.1 无 machineSlotModular/progressGaugeWide/coolantProgressBar，用现有组件等价替代）。
  - 验证：compileJava 通过。行为验证（破坏/放置/冷却游戏内）待统一做。
- [x] **P1-2 BE 存储迁移**（D9）✅ **核对完成（2026-09-01）**
  - 事实：26.1.2 的 ValueInput/ValueOutput 是 **MC 26.1.2 原版 API**（`net.minecraft.world.level.storage`），MC 1.21.1 不存在此类——**无论 LDLib2 是否升级都无法获得**。
  - 结论：1.21.1 保留 `loadAdditional(CompoundTag, HolderLookup)` 范式，仅移植机器逻辑（能量模型/批处理/面配置等），序列化差异单独处理（旧档 NBT 无条件重置兜底可参考 26.1.2 1.0.0 方案）。
  - 核对：1.21.1 全部 BE 序列化走 readTileData/writeTileData(CompoundTag)，无 ValueInput/ValueOutput 引用，结论已落地，无需改动。
- [x] **P1-3 配置系统 ModConfigSpec**（D10）✅ **代码落地（2026-09-01）**
  - 新增 TENConfig.java（NeoForge ModConfigSpec：COMMON te4-config.toml——machine 块 energyMultiplier/baseEnergyCapacity/14 机器开关 + energyUnit 块 5 项 + farm 块 bushCrops；CLIENT te4-client.toml——showMachineHUD/showCableHUD），替换 dev.toma.configuration 注解系统。
  - ConfigHolder 重写为静态门面（machine()/energyUnit()/farm()/client() + 各 getter + enableMachine(machineType) 开关查询）；TEN.java 构造器 registerConfig（ModConfig.Type.COMMON/CLIENT）；消费点更新（FarmBlockEntity isBushCrop → ConfigHolder.farm().bushCrops()；EnergyUnitItem 五项 getter → ConfigHolder.maxEnergy() 等）。
  - 验证：compileJava 通过（26.1.2 也未接入 enable 开关到放置逻辑，属对齐现状）。
- [x] **P1-4 JEI 网络同步**（D12）✅ **评估完成（2026-09-01）：1.21.1 不需要 TENRecipeSync**
  - 事实：1.21.1 的 `OnDatapackSyncEvent` **无 `sendRecipes` 方法**（26.1.2 NeoForge API）；且 1.21.1 原版数据包同步会把全部服务端配方同步到客户端 RecipeManager。
  - 结论：1.21.1 保留本地 JEI（TENJeiPlugin）+ EMI（TENEmiPlugin）集成——两者均从客户端 RecipeManager 读取机器配方，原版同步天然覆盖单机+联机场景，无需网络同步层（26.1.2 因移除本地集成才需要 TENRecipeSync 补充）。

### P2 — 表面级（独立，最后）

- [x] **P2-1 GUI 新组件**：RevealProgressBar（UV 裁剪进度条，防纹理失真）、TENFluidSlot（过滤温度/气液键 tooltip）、TransferModeButtonState（纯状态计算，4 态贴图切换）、coolantProgressBar（冷却剂消耗进度栏，绑定 Cooler.getCoolantPercent）、EnergyUnitData（已存在，1.21.1 已有）、verticalProgressMini（迷你垂直进度箭头 8x54，顶尖朝上/朝下）。
  - 依赖 P0-0：LDLib2 2.2.37 已验证 API 兼容（ProgressBar/FluidSlot 等）。素材 progress_bar_wide_* + progress_arrow_mini_* 从 26.1.2 复制。Cooler 接入双进度条 GUI（冷却剂栏 + 主进度栏）。
  - 验证：compileJava 通过。
- [x] **P2-2 物品改名**：能量单元→能量模块（en_us/zh_cn lang 文案 + TENItems 注册名已改）、频道桥接器→频道连接器（P0-8 已改，本项确认完成）。
  - 验证：compileJava 通过。
- [x] **P2-3 WrenchDismantleService**：扳手拆解服务（1.21.1 CompoundTag 适配版：TagValueOutput→CompoundTag，ProblemReporter→try-catch，onDestroyedByPlayer 用 5 参签名）。SpannerItem 更新（潜行+右键→WrenchDismantleService.dismantle，右键→旋转；1.21.1 管道推拉配置已冻结不移植）。TENTags 补 WRENCH_DISMANTLEABLE tag。
  - 验证：compileJava 通过。
- [x] **P2-4 升级槽固定 6 全解锁 + 移除扩写升级**（D11）：`upgradeSize = MAX_UPGRADE_SLOTS` 已由 P0-4 落地（6 全解锁）；1.21.1 和 26.1.2 均无 Expander 类，扩写升级已移除。核对完成。#

---

## 3. 26.1.2 每台机器的功能与设计（移植参考）

> 状态标注：`已有` = 1.21.1 已存在该机器（移植重点是基类逻辑/参数对齐）；`新增` = 1.21.1 不存在，需整台实现。
> 能量参数：`kFE(x)` = x×1000 FE 容量；eff = 基础 FE/t。

### 3.1 处理型机器（RecipeMachineBlockEntity / ProcessingMachineBlockEntity）

| 机器 | 状态 | 功能与设计 | 输入/输出 | 关键参数 |
|------|------|-----------|----------|---------|
| **熔炼机 Smelter** | 已有 | 原版烹饪配方（smelting/blasting/smoking，由 Blast/Smoke 升级切换配方模式）；产出液态 XP（0.1 mB/tick 换算：max(1, round(cookingTime/10)) mB，XP 罐 4000mB）；Know 升级门控 XP 产出并按 XP 罐容量约束 B | 槽 0 输入、槽 1 输出 + XP 流体输出罐 | cap 20kFE / eff 15；baseTickTime=配方 cookingTime（默认 200）；onCookFinish 快照-回滚 |
| **粉碎机 Pulverizer** | 已有 | 粉碎配方（矿物粉碎，产物多槽带概率）；SlotInfo(0,0,1,4) 输入 1 输出 4 | 槽 0 输入、槽 1..4 输出 | cap 20 / eff 15；配方 maxInput 1 / maxOutput 4 |
| **压缩机 Compressor** | 已有 | 模具 + 材料压缩；模具用 `kenergyengineering:moulds` tag（槽 1）与材料（槽 0）分离；SlotInfo(0,1,2,2) | 槽 0 材料、槽 1 模具、槽 2 输出 | cap 20 / eff 15；maxInput 2 / maxOutput 1 |
| **精炼机 Refiner** | 已有 | 流体+物品精炼；2×6000mB 罐（槽 0 输入流体、槽 1 输出流体）；SlotInfo(0,0,1,1) | 槽 0 输入、槽 1 输出 + 双流体罐 | cap 20 / eff 15；maxInput 2 / maxOutput 2（含流体） |
| **感应炉 Induction Furnace** | 已有 | 三输入一输出的炉冶炼（合金类）；SlotInfo(0,2,3,3) | 槽 0..2 输入、槽 3 输出 | cap 20 / eff 15；maxInput 3 / maxOutput 1 |
| **灵能处理器 Psionicant** | 已有 | 灵能配方加工；2 输入 1 输出 + 流体输出罐；SlotInfo(0,1,2,2) | 槽 0..1 输入、槽 2 输出 + 流体输出罐 | cap 20 / eff 15；maxInput 2 / maxOutput 1 |
| **物质结晶器 Matter Condenser** | 已有 | 催化剂（`kenergyengineering:catalyst` tag，槽 0）持续产出**液态诡异物质**（LIQUID_BIZARRERIE，1000mB 罐）；每 20 处理 tick 消耗 1 催化剂（不随 B 倍增）；B 锁定：输入/罐容量（floor(availTank/5mB)）/能量四维 | 槽 0 催化剂输入 + 流体输出罐 | cap 20 / eff 30；baseTickTime 1000（50s）；单周期产出 5mB×B（B=1 → 0.1 mB/s ≥ 0.001 mB/s 规格） |
| **祛魔机 Enchantment Flusher** | 已有 | 附魔工具（槽 0）+ 可附魔目标/书（槽 1）→ 祛魔物品（大输出槽 2）+ 液态 XP（1000mB 罐，每附魔组 25mB）；**B_byTool=1 强制 B=1**（工具不可堆叠，不批量复制） | 槽 0 附魔工具、槽 1 目标/书、槽 2 输出 + XP 流体输出罐 | cap 20 / eff 100；baseTickTime 800；cooking() 门控输出槽/工具耗尽/XP 罐满 |

### 3.2 效果型机器（EffectMachineBlockEntity / RadiusMachineBlockEntity）

| 机器 | 状态 | 功能与设计 | 输入/输出 | 关键参数 |
|------|------|-----------|----------|---------|
| **信标模拟机 Beacon Simulator** | 已有 | 槽 0 药水瓶（PotionContents）→ 每 10s 给半径内玩家施加药水效果；持续时间 400tick×B（long 中间值 + int 钳位）；Potion 升级加放大器 | 槽 0 药水输入；范围：以自身为中心 AABB 球体 inflate(radius) | cap 20 / eff 300；radius 初始 32；effectInterval 10s |
| **生物啃噬者 Mob Ripper** | 已有 | 槽 0 武器（DataComponents.TOOL）→ 每 effectInterval 随机伤害范围内生物（仙人掌伤害源），掉落进 1..12 输出槽；Y 固定上下各 3 格 | 槽 0 武器、槽 1..12 掉落输出 | cap 20 / eff 15；radius 4（背面 9×9）；范围=背面方形（宽 ±radius × 深 2*radius+1） |
| **采矿场 Quarry** | 已有 | 游标栅格扫描挖掘（从 machineY-1 起逐层 ±radius，chunk 缓存 + 挖不掉方块记 skipped 集合）；**4 模式**：mode 0 常规（quarry_valids tag，3 FE/次）/ mode 1 Ice 升级=生成冰系（75% 冰/75% 浮冰/蓝冰，15 FE/次）/ mode 2 Magma 升级=生成岩浆（75% 岩浆块/岩浆膏，30 FE/次）/ mode 3 Mineral 升级=矿石扫描（c:ores tag，0.4 FE/次）；scanFast 极速跳层（每调用限跳 32 层）；scanY 到 0 挖尽停机（跨重启保持）；半径/模式变化自动复位游标 | 槽 0 工具（镐）、槽 1..12 输出（动态槽上限 min(64×B, 99)） | cap 20 / eff 10；radius 初始 3 |
| **农场管理机 Farm Manager** | 已有 | 背面方形范围（radius 4 → 9×9，深轴 2*radius+1）自动收割+种植；三级收割：CropBlock（成熟→掉落+重置 AGE=1）/ 带 age 属性的再生作物（成熟→回退 maxAge-1）/ bushCrops 配置列表（收割+破坏）；成熟度快照按成熟优先排序逐行收割；空耕地自动种植（种子槽 BlockItem）；bushCrops 走配置 | 种子/收获输入输出槽；范围=背面方形 | cap 20 / eff 10；radius 初始 4 |
| **方块破坏器 Block Breaker** | **新增** | 面向方向 B 格（深度）× 宽轴 ±(radius-1) 周期性破坏方块；破坏规则参照采矿场普通模式：不破坏含方块实体（容器/机器防数据丢失）的方块、要求工具为正确工具（含挖掘等级）；掉落进 1..12 输出槽，消耗工具耐久；输出容量不足保留进度下周期重试 | 槽 0 工具（镐）、槽 1..12 输出 | cap 20 / eff 10；radius 初始 1（LevelupRg 左右扩大）；effectInterval 3s |
| **方块成型器 Block Former** | **新增** | 面向 B 格深 × 宽 ±(radius-1) 周期性放置方块；槽 0 为当前放置物块（输入）、1..12 候选栏（BOTH）；放置前检查 canSurvive（花/蘑菇/火把等不适宜表面跳过）；消耗后候选栏自动补充；占用格跳过 | 槽 0 输入（当前放置）、槽 1..12 候选栏 | cap 20 / eff 10；radius 初始 1；effectInterval 5s |
| **冷却器 Cooler** | **新增** | 冷却剂（槽 0）→ 每 intervalSeconds 减少**正面一格机器**的 progress（reductionTicks×B 推进）；冷却剂按 uses 计数消耗（耗尽移除物品）；Coolants 注册中心可拓展（注册任意物品为冷却剂）；GUI 双进度条（冷却剂消耗栏 + 主进度栏） | 槽 0 冷却剂输入 | cap 20 / eff 10；冷却剂：冰 10s/20tick×64 次、浮冰 8s/40tick×64 次、蓝冰 5s/80tick×64 次 |

### 3.3 引擎（EngineBlockEntity，能量面强制 OUT）

| 机器 | 状态 | 燃料/机制 | 关键参数 |
|------|------|----------|---------|
| **萃取引擎 Extraction Engine** | 已有 | MatchFuel.getExtractorFuelValue(level, stack) 判定燃料；1 输入槽 | cap 60kFE / eff 30 FE/t |
| **金属引擎 Metal Engine** | 已有 | MatchFuel.matchMetal(stack, true)；1 输入槽 | cap 80kFE / eff 80 FE/t |
| **生物质引擎 Biomass Engine** | 已有 | MatchFuel.matchPlant(stack, true)；1 输入槽 | cap 80kFE / eff 80 FE/t |
| **光合引擎 Solar Engine** | 已有 | 无槽；SkyLightHelper 判定天空光照（需露天） | cap 80kFE / eff 10 FE/t |

### 3.4 能量单元（CmMachineBlockEntity）

| 机器 | 状态 | 功能与设计 | 关键参数 |
|------|------|-----------|---------|
| **能量单元 Energy Cell** | 已有 | 电池充放电：槽 0 只放可放电（canExtract）物品、槽 1 只放可充电且未满（canReceive && stored<max）物品；2 槽 | cap 1000kFE / eff 100 FE/t |
| **创造能量单元 Creative Energy Cell** | 已有 | 无限能量源；默认能量面 OUT（主动输出，问题 10 修复）；2 槽充电 | cap MAX_VALUE |

### 3.5 频道（26.1.2 新架构；1.21.1 现有版本为本地撮合）

| 频道 | 状态 | 设计（26.1.2） | 容量 |
|------|------|---------------|------|
| **能量频道 Energy Channel** | 架构升级 | 末影箱式共享能量存储；成员集合驱动动态容量 | kFE(10)×成员数 |
| **物品频道 Item Channel** | 架构升级 | 共享物品存储；零 tick 传输（接入即绑定共享存储） | 9 槽 ×64×成员数 |
| **流体频道 Fluid Channel** | 架构升级 | 共享流体存储 | 2 tank ×2000mB×成员数 |

频道通用机制：`ChannelKey(name,type)` + `ChannelType` + `ChannelRegistry extends SavedData`（懒加载、事件驱动保存、**跨维度全局共享**统一经 overworld 实例）+ `SharedStorage`（**拒绝缩容**：stored > 新容量保持直至清空）；join/leave 幂等、`onChunkUnloaded/clearRemoved` 自动退频道；退出时本地缓冲优先回流频道（满则留本地）；频道名正则 `[\w\u4e00-\u9fa5\- ]{1,16}`。

---

## 4. 建议移植顺序

1. **P0-1 能量模型 → P0-2 批处理 B → P0-3 面配置**：三者构成处理型机器基类的主体（26.1.2 的 CmMachineBlockEntity 73.9K / RecipeMachineBlockEntity 54.5K 相对 1.21.1 的 34.8K / 5.0K 的膨胀即源于此），先落地基类再谈机器。
2. **P0-4 升级乘法模型 + P0-5 停滞语义**：紧随基类，依赖同一批字段（durationMultiplier/powerMultiplier/lockedB）。
3. **P0-6 能力层**：与 P0-1/2/3 并行评估——先确认 1.21.1 LDLib2/NeoForge 经典 Capability 下「面门控 + 主动 IO」等价实现，再动管道/频道。
4. **~~P0-7 管道跳跳传输~~ 🔒 已冻结**：不移植，保留 1.21.1 root 撮合现状（用户指令小黑屋）。
5. **P1-1 三台新机器 → P1-2/3/4 存储/配置/JEI**。
6. **P2 表面项收尾**。
7. 每步完成标准：compileJava + 对应回归脚本 + 游戏内验证（可参考 26.1.2 的 plans/.evidence/ 证据文件与契约测试）。

---

## 5. 移植禁忌与注意事项

- **能力层不要照搬**：CapabilityAdapters/EnergyDirectRestorable 是 26.1.2 的 NeoForge Resource API 专属桥，1.21.1 经典 Capability 下不适用（P0-6）。
- **旧存档兼容**：26.1.2 用「无 schema 版本 → 无条件重置 progress/maxProgress/lockedB」兜底；1.21.1 移植批处理/停滞语义时，旧档 progress 语义变化需同样兜底评估。
- **命名差异**：26.1.2 分支目录名「26.1.2」指 NeoForge 26.1.2 与 MC 26.1.2；本分支为 MC 1.21.1 / NeoForge 21.1.219，移植时以 API 差异为准（如 Resource/Transaction API 不可用）。
- **配方 JSON 不通用**：26.1.2 的 FormsCombinedRecipe 序列化（form/type/key/count/amount/chance/rolls）在 1.21.1 配方系统下需确认兼容；原版 `matches(RecipeInput, Level)` 直接 return true，机器侧用重载 `matches(IItemHandler, tanks, slotType, tankType)` 自定义匹配。


用户追加
KER中的所有的机器的数据包配方，无论自动生成还是手动创建疑似配方ID均无，导致配方管理器获取不到配方ID出现警告
配置面板的打开时的类型可以正常调节IO配置，但是不是打开时的那一页配置尝试调节时不会同步更新
配置类型主动输出，似乎不会主动输出能量
扳手需要在右键机器标签的方块时，不打开其的GUI
给升级面板和配置面板叠加的中间覆盖层，需要调整出现时间为面板完全展开后，而不是点击tab时