# 项目待办表

> 状态图例：⬜ 未开始 | 🧪 待实机 | ⏸ 暂缓
> 优先级：P1 最高 | P2 | P3
>
> 本表只放**还没做完的事**。已完成条目不留档（看 `CHANGELOG.md` 与 git 历史）；
> 已放弃与有意不做的取舍去找对应的提交与代码注释，本表不再列。
> 发版流程见 `RELEASE.md`；工程约束见 `ARCHITECTURE.md`。

## 一、待实机

### AE2 样板供应器阻挡模式回归 — P2 🧪
- 2026-09 修复了面 capability 观察读被抽取权限藏空的问题。需要在 AE2 样板供应器 + 机器非可抽取面配置下复验：
  阻挡模式应生效，不再连发配方。
- 复验面：主动输出、被动输入、关闭三种面配置；同时确认写入/抽取仍按面权限门控，观察读仍能反映机器真实内容。

### GUI 配置面板像素对齐回归 — P3 🧪
- 2026-09 修正了内容钮原点与内边距。需要在 1080p / 720p 与不同 GUI 缩放下复验：
  6 个面配置钮、3 个类型钮与底图钮槽对齐，不被纹理原生留白二次偏移。

### 线缆缓冲偶发不向受端送电 — P2 🧪
- 现象：`cable_moves_energy_between_cells` 间歇失败。2026-11-08 抓到一次完整诊断：tick 60 时
  `source=3000 target=0`——源单元已推出 1000 FE（正好等于玻璃线缆缓冲 1_000），线缆满但受端恒为 0，
  说明「设备→线缆」这一段正常、「线缆→受端」这一段没成。
- 该条自 2026-05 起就间歇失败，同一份代码可一次通过一次不过；2026-11-08 已把断言窗口从 25 tick
  放宽到 60 tick（起测时服务器常卡 7~9 s / 150+ tick）并保留两端储能与面配置诊断，34/34 可全过。
- 待查方向：`EnergyNetwork` 第 171 行的受端筛选（`canReceive()` + `getEnergyStored() < getMaxEnergyStored()`）
  与 `CmMachineBlockEntity.getEnergyStorage(side).receiveEnergy` 的 `signalAllowRun()` / 面门控，
  在 GameTest 的 `setBlock` 时序下是否读到过瞬时状态；要区分「网络未成形」和「受端拒绝」。

### 正常链路启动复验（testframework） — P2 🧪
- 2026-10-31 已用 `runClient -x compileTestJava` 启动并进入世界，随后正常关闭；AE2 Pattern Disk、FTB Library、
  AE2WTLib、Architectury 均加载。
- 正常 `runClient` 仍会先编译 test 源集，并卡在 `net.neoforged:testframework:21.1.243`
  （`maven.neoforged.net` 不可达）。
- 网络恢复或本地缓存 testframework 后，去掉 `-x compileTestJava` 再跑一次 `runClient` 与 `runGameTestServer`。

### AE2 Pattern Disk 0.9.8 载入实机确认 — P2 🧪
- 0.9.7 的供应器在载入世界时对空网格节点解引用会崩服（dev 世界两次复现，同一坐标），上游 0.9.8 已修复发布。
- 开发依赖已 bump 到 CF file `9384288`（0.9.8），`runtimeClasspath` 解析通过，客户端本体启动无异常。
- 待做：在 dev 客户端进入该存档，确认 `(-37,71,-44)` 那台供应器不再触发崩服。

### 槽位堆叠统一与超 99 持久化实机复验 — P2 🧪
- 2026-10-31 堆叠上限改由 `CmMachineBlockEntity.installBatchSlotLimit()` 统一给出
  （可进/可出槽 = 物品 maxStackSize × 理论 B；不可堆叠物品 1；其余 64），
  删掉 Farm / Quarry / 方块破坏器 / 生物啃噬器与配方机器基类各写一份的 provider；
  `MachineItemHandler` 覆写 NBT 读写，超 99 数量存 `BigCount`（vanilla CODEC 的 count 限 1..99）。
- 需复验：B≥2 时输出槽能堆到 128+；**保存并重进世界后数量不丢、不抛错**；
  频道物品存储（64×成员数）同样能存回；工具槽仍为 1；非 64 上限物品（如 16 上限）不再被凭空放大。
- 未覆盖：`ItemStack` 走 `validatedStreamCodec` 的同步路径若将来命中，需一并处理；
  目前实测界面显示与容器同步正常（varint 计数不受 99 限制）。

### GuideME 指南实机验证 — P2 🧪
- 指南物品只在 GuideME 在场时注册；需验证工具页指南书可打开、导航与页面物品图标正常。
- 未装 GuideME 时模组正常加载且不出现指南物品；指南不依赖 AE2。
- 2026-10-31 内容根 folder 由 `ae2guide` 改为独立的 `kenergyguide`（同 folder 会被 GuideME
  按跨命名空间合并成同一册）。需复验：不再出现在 AE2 手册目录树里，指南书直接打开到「开始」页。
- 2026-11-01 起始页改为不手写（吃 GuideME 缺省 `<ns>:index.md`）：旧写法 `TEN.id("index")`
  少扩展名，页面 map 取不到 → 书打开空白。folder 隔离把这条一直存在的缺陷从「借 AE2 目录树
  可见」变成「唯一入口」，所以看起来像改坏了。需复验同一项：首页可进 + 9 个子页可点。

### 机器 IO 取消限速实机复验 — P2 🧪
- 2026-10-31 移除机器能量 / 物品 / 流体的每 tick 限速：能量只受储能容量约束，物品 `extractItem`
  不再按 `maxExtractItem` 截断，流体本就只有显示镜像。需实机复验：
- 批处理机器（B > 1）不再因抽取限速停滞；引擎多面主动输出不再受每面额度限制。
- 线缆仍按 200 / 1k / 4k / 200k FE/t 档位限速；机器直接贴机器时按对面空余容量一拍打满。
- 外部抽取（漏斗 / 管道 / AE2 总线）单 tick 可搬空整槽，确认不炸配方循环与批量锁定语义。
- 左侧能量信息栏只剩两行：当前消耗 = 实际效率 × 锁定 B，最大消耗 = × 理论 B；装功率/批量卡后应随之变化。
- 星辉能量传输升级卡加装前后无差异（当前为空效果，待升级内容重构重新定义）。

### 冷却器与冷却剂实机复验 — P2 🧪
- 2026-10-31 冷却剂改标签驱动（`c:coolants` + basic/standard/premium 三档），冷却器状态改为
  `usesRemaining` / `coolantTotalUses` 两个 `@DescSynced` 镜像，次数初始化从周期末提到每 tick。
- 需复验：放入冰/浮冰/蓝冰后进度条立即满格并随周期递减；首个周期即生效；取出后归零；
  换档冷却剂数值随之改变；JEI 冷却器页列出三档成员与周期/推进量/次数。
- 未做：EMI 侧没有冷却剂页（引擎燃料有 `EngineFuelEmiRecipe`）。补之前先验证 EMI 运行路径，
  见「EMI 运行时切换」。

### 槽位物品唯一实机复验 — P2 🧪
- 2026-11-01 `CmMachineBlockEntity.canPlaceItem()` 按「输入组 / 输出组」分组查重，同组内已有同物品即拒收；
  它同时作为 `MachineItemHandler` 的校验位（构造与 `setValidator` 两处），
  所以界面放入与管道 / face 写入走同一条判定。
- 需复验：感应炉两输入槽放同种锭被拦、放两种锭正常；输出槽与输入槽同物品不算冲突；
  电池仓 / 频道存储豁免生效；漏斗 / AE2 总线从外面向输入槽塞重复物品被拒且不丢件。

### 兼容层配方与开关实机验证 — P2 🧪
- 2026-11-01 兼容层现有 175 条（EIO 102 / EVO 17 / ATO 55 等，`recipe/compat/<modid>/<机器类型>/`），
  每条带 `neoforge:mod_loaded` + 第三方门控 + `not(tag_empty(输入标签))` +
  `kenergyengineering:compat_enabled`；三段式改成候选模组的互斥变体
  （`kenergyengineering:provider_priority` + `compat.providerPriority`）。
- 需复验：JEI 能看到承接配方且能真的产出；把 `compat.disabledSources` 填 `enderio_evolution` 后
  重启世界，该来源配方与 JEI 页一并消失；把 `compat.providerPriority` 里 `mekanism` 提到
  `kenergyengineering` 之前，锡/铅的粗料与粉改由通用机械提供、青铜锭改用通用机械的锭。
- 需复验显示同步：把沉浸工程或 EIO 单独摘掉后重启，ATO 的冲压/研磨行应整条从 JEI 消失
  （此前就是这里对不上）；材料缺席（如没有锌）时黄铜行也应消失。
- 需复验新增内容：粗矿块→粉 9(+40%×9) 与锭→粉对邻居材料可用；感应炉按
  3 铜 + 1 锡 → 4 青铜（黄铜/因瓦/电子/康铜同理），输入只认 `c:ingots/*` 标签。
- 需复验日志：`TENRecipeAudit` 在本次加载不输出「机器配方歧义」；`tools/compat/check.js` 退出码 0。
- 未做：`runGameTestServer` / 单元测试跑不了（`net.neoforged:testframework` 拉不到），
  条件求值与配置读取时序目前只有静态取证，须以实机为准。

### 采矿场循环与可挖判定实机验证 — P2 🧪
- 2026-11-01 `scanExhausted()` 的下界由常量 `WORLD_MIN` 改为 `level.getMinBuildHeight()`；
  到底且本轮确实挖到过方块时 `tick()` 复位游标，以机器自身为新起点重扫（空手到底不复位，防空转）；
  `canBreak(state, pos)` 增加 `state.getDestroySpeed(level, pos) < 0` 拒绝。
- 需复验：主世界挖到 -64 后能重新开始下一轮，输出继续进槽；基岩/屏障/结构空位不被挖掉；
  下界（底 -128）与末地（底 0）按各自底高判定，范围线框不再画到主世界的 -64。
- 需复验停机语义：挖尽且本轮无产出后不再消耗能量、不启动批次；切换标准/极速模式仍能复位重扫。
- 注意：`c:.../quarry_valids` 方块标签在 `CmMachineBlockEntity`/生成器里存在但从未被读取（死配置），
  本轮没有启用它；若要让白名单真正生效需另做决策。

### 配置界面与 Mod Menu 实机验证 — P2 🧪
- 2026-11-01 `TENClientSetup` 注册 `IConfigScreenFactory` → `ConfigurationScreen::new`，
  开发运行期加入 Mod Menu (NeoForge Edition) `1393224:7312908`（1.21.1-1.0.2，MIT，无声明前置）。
  Better ModList 未选：CF 上标到 1.21.1 的 fileId 无法核实，且其配置界面另需 config-screen 库。
- 需复验：主菜单 Mods → 科能工程 → Config 能打开，`compat.disabledSources` /
  `compat.providerPriority` 改值写回 `config/kenergyengineering-common.toml` 并在下一次进世界生效；
  Mod Menu 面板同样出现 Config 按钮（两者读同一扩展点）。
- 首次 `runClient` 才会真正下载该 jar（`dependencies` 报告只取到 POM），坐标已确认可解析。

### 合金配比、mats 聚合与模具标签实机复验 — P2 🧪
- 2026-11-01 感应炉默认配比按 ATO 的 `crafting/<alloy>/alloy_blending_from_dust.json`（青铜/黄铜 3+1→4、
  因瓦 2+1→3、电子/康铜 1+1→2），输入写 `c:mats/<材料>`；`TENMaterialTagGen.NEIGHBOUR_MATS`
  给邻居材料与五种合金也生成聚合标签（成员是对 `#c:ingots/X` / `#c:dusts/X` 的可选引用）。
  模具侧只保留 `c:moulds`，`c:moulds/shape` 子标签与其语言键删除。
- 需复验：感应炉同时接受铜粉与铜锭参与合青铜；JEI 该页显示 `c:mats/copper x3` + `c:mats/tin x1 → 4 青铜锭`；
  把 `compat.providerPriority` 的 `mekanism` 提到 `alltheores` 之前，青铜应变为通用机械的锭
  （黄铜/因瓦仍只有 ATO 提供，配方不变）。
- 需复验模具：任意模具可放入压缩机催化槽（含成型模具），取出与批量运行不被消耗；
  删掉的 `c:moulds/shape` 不影响任何配方匹配（若出现某台机器拒收模具，说明还有隐藏的按分类标签读取点）。
- 需复验冷却剂页：与熔炉桥接、引擎燃料页同组出现在 JEI 的 KER 模组分页里，
  冷却器方块作为催化剂可点开该页（类别与催化剂现在共用 `buildAuxCategories()` 一份清单）。
- 未做：EMI 侧仍没有冷却剂页（`TENEmiPlugin` 只登记机器/熔炉/引擎），与 dev 环境禁用 EMI 一致。

### 合金扩容与耗时尺规实机复验 — P2 🧪
- 2026-11-01 感应炉合金从 5 种扩到 **11 种**（新增钢、蓝金/enderium、荧石金/lumium、信号金/signalum
  与 ECO 的铝合金、黑钨合金），全部由邻居的**粉末**配方推导；耗时尺规改为线性
  50 tick × 输入物料份数，原生 9 条手写感应炉配方同步重算。
- 需复验：钢的三条变体在只装其一时能自动落到下一家（`c:ingots/steel` 由 ATO/通用机械/沉浸工程都提供，
  默认归属按 `compat.providerPriority`）；lumium/signalum 需要 AE2 的 `c:dusts/certus_quartz`、
  `c:dusts/fluix`——没装 AE2 时这两条与铝合金行应整条从 JEI 消失，而不是显示跑不了的配方。
- 需复验耗时：感应炉 2 份输入 100 tick、8 份输入 400 tick；压缩机所有冲压/压缩配方恒 100 tick；
  装功率升级卡后时长随实际效率缩短、`B` 锁定值不因耗时改变而漂移。
- 需复验 ECO 侧：`neoecoae` 现在进 `compat.providerPriority`（默认在 `allthemodium` 之后、`minecraft` 之前）；
  把它写进 `compat.disabledSources` 会**同时**让它退出材料归属竞选（`firstProvider` 用 `enabled()` 过滤），
  而铝合金/黑钨合金目前没有第二家候选，所以那两条会整条消失而不是回退给别人——确认这是否符合预期。

### 配置界面文案与 ECO 归属实机复验 — P2 🧪
- 2026-11-01 配置语言键从 10 条补到 63 条（`kenergyengineering.configuration.*`，中英与 en_ud 三份齐），
  覆盖机器/能量模块/农场/兼容四组、分区名与标题；键的格式由 NeoForge 拼
  `<modid>.configuration.<点分路径>` 与 `.tooltip`（无 tooltip 时回退 TOML 注释）。
- 需复验：打开配置界面不再出现任何裸键名；`compat.disabledSources` 的列表编辑（增删行）
  在界面上可用并写回 `config/kenergyengineering-common.toml`；tooltip 显示中文说明。
- 需复验 ECO 归属：`neoecoae` 已排到 `compat.providerPriority` 第二（仅次于本模组），
  铝的粗料与粉、钨的整条链（`c:ores/tungsten → 粗钨 → 钨粉`）应默认由 ECO 提供；
  把 `neoecoae` 从列表移除后，铝应退回沉浸工程、钨的配方整条消失。
- 需复验：`c:storage_blocks/raw_tungsten` 与 `c:storage_blocks/raw_aluminum` 两条粗矿块→粉
  在装了 ECO 时可用（ECO 提供 `raw_tungsten_block` / `raw_aluminum_block`）。

### 面语义、主动 IO 与超堆叠实机复验 — P1 🧪

已实施：面判定收进 `FaceOption` 四个单一语义方法（`machinePulls` / `machinePushes` /
`allowsExternalInsert` / `allowsExternalExtract`），三类 IO 统一为「仅 `IN` 自拉、仅 `OUT` 自推」，
`BE_IN` / `BE_OUT` / `BOTH` 机器一律不动；`writeTileData` 恒写 `faceSemantics=1`，缺键旧档把
产能类机器的能量 `BOTH` 面迁为 `OUT`；默认面改由构造期读 `initialFaceMode*()`。
流体侧 `doActiveFluidIo()` 逐 `canOut` 罐推送（`pushFluidTank`），输入罐不再被当成输出源。
本轮补：能量/流体的 `IN` 面自拉（此前只有自推，IN 面完全不动作）；GUI 逻辑面改读
`getFacing()`，此前读未初始化的 `facingVal`，所有机器都按北向解释；频道方块主动 IO 置空；
面配置可选范围改为两层判定：方向级按设备功能性（产能类才有能量输出、引擎不收电、物品/流体按
槽位与罐体方向），家族级靠 `no_active_face_energy/_item/_fluid` 三个「去除」标签（频道各一条）。
GameTest 34 条含新增 `MachineActivePullGameTest` 2 条（能量/流体 IN 拉取）与
`FaceModeRangeGameTest` 7 条（功能方向/标签/跳过/归一化），下列场景仍需实机验。

1. **旧档能量（先测）**：用上一发布版存档（含已接线、正在供电的能量单元/创造单元）升级进入。
   期望：单元 GUI 的能量面显示「主动输出」，机器仍从单元取电。旧档里玩家手动设成被动输入/禁用/
   被动输出的面不应被改写（迁移只动 `BOTH`）。
2. **新放置默认面**：新单元/创造单元默认主动输出，贴电缆即供电；新机器物品/流体面默认被动双向——
   物品本来就不自推，**流体会停送**，需手动改主动输出；新频道方块默认全关，接入频道后要在 GUI
   开启对应面，频道连接器复制/粘贴面配置仍生效。旧档频道方块存的是 `BOTH`，升级后照常传输。
3. **面配置随机器朝向（本轮修复）**：把同一台机器分别朝东/南/西/北放置，打开 GUI 点「前」，
   被改的应是机器正面（贴玩家的一面；Shift 放置时为背面），不是固定的北面。
   顺带核对六个逻辑面按钮（前/后/左/右/上/下）与世界方向的对应无误。
4. **物品四面语义**：`OUT` 自推、可被漏斗/管道塞入、不可被抽；`BE_OUT` 不自推，物品管道该面用
   扳手切 `PULL` 可抽、塞不进；`BOTH` 不自推、可抽可塞；`IN` 自拉、外部不可塞不可抽。
   顺带核对 GUI 图标与「主动/被动」标签不再错位。
5. **主动输出面贴物品管道（语义已澄清，非缺陷）**：机器 `OUT` 面直接贴本模组管道时物品不动
   （机器跳过管道，管道设备边默认 `NORMAL` 是向机器投放）；把管道切 `PULL` 也不动，因为 `OUT`
   面不允许外部抽取。要出料须用 `BE_OUT`/`BOTH` + `PULL`，或让 `OUT` 面朝容器。
   管道不是容器，不为此放宽 `OUT` 语义；只需确认玩家按 `BE_OUT`/`BOTH` + `PULL` 接能正常取料。
6. **超堆叠主动输出**：熔炉/精炼机装 1 个潜影组件（`B=4`，输出槽 256），装满后输出面朝箱子设
   主动输出。期望：机器减少量 = 箱子增加量（守恒 256），槽被搬空而非每 tick 只走 64；箱子满时
   物品留在槽内、不掉地。`extractItem` 契约与机器端到端两条已有 GameTest，实机确认表现即可。
7. **扳手拆卸超堆叠**：槽内 200 个，扳手拆卸后背包 + 地面总数 = 200，不丢不复制。
8. **流体语义（单罐）**：冷凝器/经验熔炉/熔炼机 XP 罐默认被动双向不再自动送液；改主动输出后送入
   相邻容器，改回被动双向即停。
9. **流体按罐取源（精炼机双罐）**：A 的 tank 0 装 `liquid_xp`、tank 1 积岩浆；相邻精炼机 B 输入罐
   留空并将该面设为被动输入，A 朝向 B 的面设主动输出。期望：B 收到岩浆；A 的 tank 0 `liquid_xp`
   **不减少**（修复前合并 handler 会先抽 tank 0）。B 输入罐预装水（拒收岩浆）时岩浆留在 A，
   不消失、不串液。
10. **能量主动输入（本轮修复）**：引擎默认主动输出经电缆给机器供电；机器能量面设主动输入、
    相邻创造能量单元该面设被动输出 → 机器储能上涨；把单元该面改回主动输出（机器推、不开放
    外部取）→ 应停止被抽；单元 `OUT` 面仍可被外部塞电（充电）。
11. **流体主动输入（本轮修复）**：机器流体面设主动输入，相邻容器/机器输出罐有液体 → 液体会被
    抽入机器输入罐；源罐侧须允许外部抽取（`BE_OUT`/`BOTH`）。AE2 等外部设备只要能暴露
    `IFluidHandler` 即可，不需要本模组特判。
12. **旧档流体（预期变更，记录用）**：旧档靠默认 `BOTH` 自动送液的机器升级后停送，手动改主动输出
    恢复；旧档不做流体迁移（迁成 `OUT` 会关掉外部抽取，比不迁更糟），这是有意的。
13. **面配置可选范围（本轮新增）**：打开频道方块 GUI，六个面按钮只应在「关闭 / 被动输入 / 被动输出 /
    被动双向」间循环，点不出主动输入与主动输出；物品频道只有物品页可配，能量与流体页点不动。
    无罐机器（如粉碎机）的流体页同样只剩「关闭」。
    普通机器的能量页应只剩「关闭 / 主动输入 / 被动输入 / 被动输出 / 被动双向」——**主动输出不出现**；
    引擎的能量页反过来只有主动输出（不出现主动输入），物品页只有主动输入（燃料槽）。
    能量单元两向都在。熔炉/祛魔机的流体页只应出现主动输出（XP 罐只出不进），精炼机两向都在。
    旧档若给频道配过主动面、或给普通机器配过能量主动输出，升级后该面应显示「关闭」。

## 二、未开始

### 采矿场等工具续扫实机复验 — P2 🧪
- 决策表 `QuarryScanDecision.decide()` 有脱机断言跑过（6 项全过）；方块实体接线部分只过了编译。
- 需复验：低阶镐扫到到底后停机且**不耗能**；放入更高阶镐立刻从原位置续挖（不重扫已挖空的层）；
  真的挖空整根柱子仍按原语义永久停机（不会一轮轮空扫）；GUI 切扫描模式可强制重扫。


### 锭/粉输入收敛后的实机抽验 — P2 🧪
- `c:mats/X` 现在只有感应炉读；粉碎机与压缩机一律 `c:ingots/X`（26 条邻居齿轮行的份数也回到 4 锭）。
- 需复验：铅粉之类**不再**能进压缩机催化成型；铅锭照常出 2 线；感应炉仍接受铜粉与铜锭混配青铜。
- 需复验：JEI 里压缩机/粉碎机页面的输入图标回到锭，不再显示「锭或粉」标签。

### IE 单独在场时缺 7 条板与 2 条杆 — P3 ⬜
- ATO 与沉浸工程的 metalpress 是同一机器类型，`convert.js` 的 `seen` 按源顺序让 ATO 先占同输入，赢家只写一条。
- 其中 9 条（aluminum/constantan/electrum/lead/silver/steel/uranium 的板，aluminum/steel 的杆）在只装 IE 的包里
  本可由 IE 自己提供产物，现在因赢家行的 `mod_loaded` 是「ATO + IE」的与语义而整条不加载。
- 机制已有：改走 `kenergyengineering:provider_priority` 的互斥变体（同输入每家各一条，加载期按 `compat.providerPriority`
  选唯一赢家，`RecipeSignature` 与 `tools/compat/check.js` 都已豁免变体组）。要做的是让压入侧也产出变体组。

### 冲压与水晶兼容行实机抽检 — P2 🧪
- 23 条冲压行（`compat/{alltheores,immersiveengineering}/compressor/`）与 8 条 AE2 家族水晶行只有静态与产物证据：
  `tools/compat/check.js` 冲突 0 / 引用 0，`runData written` 稳定，物品 id 与 modid 从 dump 核过。
- `ae2cs`／`extendedae`／`extendedae_plus`／`data_energistics` 不在开发依赖里，这 8 条从未在游戏内加载过一次。
- 需抽验：`wire_lead` 用铅锭与铅粉各压一次（输入是 `c:mats/lead`，粉也该吃）；`certus_quartz_crystal` 进粉碎机出 AE2 粉；
  装 AE2 不装 Mekanism 时 `energized_crystal_dust` 应照常出现（门控已放宽）。


### 机器 enable* 开关没有消费点 — P2 ⬜
- `TENConfig.MachineConfig` 的 12 个 `enableX`（熔炼机/粉碎机/压缩机/精炼机/感应炉/灵能处理器/
  信标/生物啃噬器/采矿场/祛魔机/物质结晶器/农场管理器）只被 `ConfigHolder.enableMachine()` 读取，
  而后者在 `src/main` 与 `src/client` 里**没有任何调用点**——改这些开关当前不产生任何效果。
- 需要决策：(a) 接到机器可用性（放置/工作/配方显示）上；(b) 删掉这 12 项与 `enableMachine()`；
  (c) 保留但改成别的语义。语言键已按 (a) 的前提补齐，若选 (b) 需同步删除对应 `configuration.machine.enable*`。


### EMI 运行时切换 — P3 ⬜
- 当前开发运行期只挂 JEI，`dependencies.gradle` 里 EMI 的 `localRuntime` 行被注释。
- 验证 EMI 路径时按注释切换，并同时移除 AE2-JEI 桥接（`forge.ae2jeiintegration`），
  否则缺 JEI 前置会直接启动失败。
- 切换后跑一遍配方查看、幽灵物品与编码终端相关的手验。

### 26.1.2 参照分支同步 — P3 ⬜
- 同父目录的 `Technical-Engineering-4-26.1.2/` 是参照源，不是当前发布线。
- 本地化、模型、配方与 LDLib2 GUI 的改动需要评估是否回移；当前 1.21.1 线已完成一轮对齐，
  后续按实际变更维护，不做无差别同步。

### 线缆 GUI 文案与代码数值不一致 — P2 ⬜
- `TENLangHandler` 的 `cable.0` / `cable_quartz.0` / `cable_azure.0` / `cable_star.0` 仍写
  1 kFE / 10 kFE / 100 kFE / 无限 FE；
- `CableBlockEntity.capacityFor/transferFor` 实际为 1k / 20k / 50k / MAX 缓冲、200 / 1k / 4k / 200k FE/t。
- 统一文案或改数值前，以代码为准；`docs/ARCHITECTURE.md` 已按实际值记录。

### Modrinth 项目 id 核对 — P2 ⬜
- `.github/workflows/publish.yml` 里写的是 `PS4p8SFk`；公开 API 返回 404，可能是未发布/私有项目。
- 另有一个旧的 Technical Engineering 4 项目 id `tUaaXwLH`，不要混用。发布前用后台或带 token 的 API 核对。

### PR 标签兜底基线核对 — P3 ⬜
- `.github/json/config.json` 的 `tag_resolver.filter` 排除了 `v*-1.21` 形式的 tag；发布前用一次 dry run
  确认 `meta` 能生成兜底说明。
- `publish-21` 以 `meta` 为前置，`meta` 失败时整条发布链不会启动；修好后改版本号重新打 tag 发版，
  同一 Release Re-run 仍会用旧 tag 的 `config.json`。

### 4.1.0 更新日志复核 — P2 ⬜
- 4.1.0 尚未打 tag；`CHANGELOG_UNRELEASED.md` 的「新增」条目要对照 v4.0.0 实际玩法再确认一次，
  避免把 4.0.0 已有的功能写成新增。

## 三、暂缓

### 能量/物品传输系统重构 — ⏸
- 线缆与管道当前冻结维护：只修 bug，不做结构改造。
- 管道连接模式已对齐 TE4-New 基准；旧档管道升级后需要扳手重设，属于已记录的迁移行为。

## 四、归档索引

- 已完成条目与实现细节：`CHANGELOG.md`、git 历史、`ARCHITECTURE.md` 的模块说明。
- 发版流程与踩坑：`RELEASE.md`。
- 更新日志体例：`CHANGELOG_STYLE.md`。
