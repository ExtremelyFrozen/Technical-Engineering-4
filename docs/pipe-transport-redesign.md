# 管道传输策略重构 — 设计方案（待指挥官确认）

- 日期：见 git 提交
- 范围：Kenergy Engineering（NeoForge 26.1.2），物品管道 `pipe / pipe_white / pipe_black`
- 阶段：设计（只读，未实现）
- 状态：⏳ 待确认（本文件为设计交付物，不含代码改动）

---

## 1. 现状

### 1.1 当前传输机制全貌（证据：源码）

| 组件 | 位置 | 职责 |
|---|---|---|
| `tick()` | `PipeBlockEntity.java:296-314` | 每 tick：`tickPullPush()`（主动）；5 tick 节拍：`collectConnected` + `isRoot` + `processNetwork`（被动） |
| `tickPull()` | `PipeBlockEntity.java:322-348` | `pullLevel>0` 时，6 面遍历相邻容器 → `extractAllowed` 抽取单次量 → `transportHandler.insertItem` 分发到网络 → 放不下退回源 |
| `tickPush()` | `PipeBlockEntity.java:351-376` | `pushLevel>0` 时，6 面遍历相邻容器 → `transportHandler.extractItem` 从网络抽取 → 插入相邻容器 → 放不下退回网络 |
| `processNetwork()` | `PipeBlockEntity.java:403-432` | root 每 5 tick，网络内所有 (管道×6面) 源容器 → 所有 (管道×6面) 目标容器**两两配对**，`moveItems` 搬 1 个（过滤用源管道的 `isItemAllowed`） |
| `transportHandler` | `PipeBlockEntity.java:119-196` | 单槽假 handler：`insertItem`=分发到网络全部相邻容器、`extractItem`=从网络全部相邻容器抽取；每次调用实时 BFS |
| `singleTransferAmount()` | `PipeBlockEntity.java:384-391` | 单次量 = `(1 + 7×speedLevel) << enderLevel`（long 防溢出，钳 int；public 供 Jade） |
| `TransferNetworks` | `TransferNetworks.java` | `collectConnected`（BFS）/ `isRoot`（最小坐标）/ `getItems` / `moveItems`（限量+过滤+simulate）/ `insertItem`（槽遍历） |
| 升级系统 | `PipeUpgradeType.java` + `PipeBlockEntity.java:206-296` | PULL/PUSH 开关各 1 级；SPEED 9 级（+7/级）；PAGE 63 级（+27 槽/页）；ENDER 8 级（×2^level）；右键升级物升级，持久化缺键→0 |
| 过滤 | `PipeBlockEntity.java:498-509` | `isItemAllowed`：白名单（标记物匹配才放行）/ 黑名单（匹配则拦截）；27×页数多页槽 |

### 1.2 现状问题（重构动机）

1. **被动传输无方向概念**：`processNetwork` 把网络内所有相邻容器两两配对搬运，物品在任意相邻容器间自动流动，无法表达「从这里送出去 / 从那里收进来」的定向意图。
2. **中转语义别扭**：`transportHandler` 是「网络代理假 handler」，抽取/推入都先入网再出网，但网络实际无缓冲——包装层徒增理解成本，且每次调用重复 BFS。
3. **主动/被动双轨**：主动每 tick、被动 5 tick 两套机制并存，节拍与吞吐语义分裂（同一网络内不同管道节奏不同）。
4. **性能**：每 tick 每管道 2 次 BFS（tickPull/tickPush 各自 `collectConnected`），每 5 tick 每管道再 1 次 BFS；大型网络扫描重复。
5. **单向语义缺失**：无法表达「仅抽取」或「仅推入」的局部意图（如只从箱子抽走、只往熔炉送）。

---

## 2. 参考调研

> 本地 `run/server/mods`、`run/client/mods` 均为空，无参考 mod jar 可读源码；以下为经验知识综述，供对标参考。

| Mod | 目标选择 | 单次量 | 防振荡/防循环 | 抽取/推入分离 | 节拍 |
|---|---|---|---|---|---|
| EnderIO 物品导管 | 优先级 + round-robin | 每 tick 每连接 1 次批量（升级加速） | 路由表自修复 | 每面独立 input/output 模式 | 每 tick |
| Pipez | 距离最近优先 | 栈传输（按升级） | 无 buffer 直接撮合 | 每根管道 pull/push 模式切换 | 每 tick 每管道 |
| Mekanism 物流管道 | 优先级 + 传输量排序 | 栈/批传输（升级加速） | 每 tick 限流 | pull/push 模式 + 面配置 | 每 tick |
| Thermal Dynamics | 伺服器=出口抽取（带过滤）；检索器=目标侧拉取 | 按 servo/retriever 升级 | round-robin 目标轮转 | **servo（源）/ retriever（目标）分离** | 每 tick |
| XNet | 通道路由 + 连接器 input/output | 批量 | 连接器按距离/优先级 | 连接器分 input/output | 每 tick 限次 |

**可借鉴要点：**
1. **目标选择**：Thermal 的 servo/retriever 分离模型与本项目新 Pull/Push 意图最接近（源侧管道负责抽取送出、目标侧管道负责接收灌入）。
2. **空间优先**：目标容器空缺量大者优先（least-loaded first），天然平衡多源竞争，收敛快。
3. **防振荡**：round-robin 轮转指针 + 单源限量 + 目标排除同源。
4. **堆叠传输**：一次搬一组/可达量，减少撮合次数与 IItemHandler 调用开销。
5. **无缓冲直通**：Pipez 风格容器↔容器直接撮合，不引入网络持久缓冲（避免物品滞留/存档风险）。

---

## 3. 新模型设计

### 3.1 核心语义

- **Pull 管道（出口角色，`pullLevel>0`）**：将自己相邻 6 面容器视为**源**，从源抽取物品送入网络。
- **Push 管道（入口角色，`pushLevel>0`）**：将自己相邻 6 面容器视为**目标**，从网络取物品灌入目标。
- **撮合（matching）**：同一网络内，root 每节拍收集所有 Pull 源候选与 Push 目标容量，做一次「源→目标」分配并执行。
- **无缓冲直通**：网络不持有物品（不引入虚拟 buffer/持久化）。Pull 抽取的物品必须在同一节拍内找到 Push 目标；找不到则退回源容器（尽力而为、不吞物品，与现状一致）。
- **默认行为**：未升级任何一方的网络 → 无传输（零开销守卫）；仅 Pull 无目标 → 抽取后退回（Pull 无效但不丢物品）；仅 Push 无来源 → 不动作。

### 3.2 流程（每节拍，root 执行）

```text
1. 发现网络：TransferNetworks.collectConnected（复用现状）
2. 角色收集：
   a. Pull 源集合：遍历网络内 pullLevel>0 的管道 → 6 面相邻容器（非管道）
      → 候选 = extractItem(slot, perBeatLimit, simulate) 且通过该 Pull 管道 isItemAllowed
   b. Push 目标集合：遍历网络内 pushLevel>0 的管道 → 6 面相邻容器（非管道）
      → 目标可接收量 = Σ 每槽 (getSlotLimit - 当前量)
3. 撮合：对每个候选（按 Pull 管道 → 槽位顺序），按 3.3 目标选择选一个目标；
   双侧过滤 AND：候选须通过 Pull 管道 isItemAllowed 且通过目标 Push 管道 isItemAllowed
4. 执行：extract（真实）→ insert（真实，TransferNetworks.insertItem 槽遍历）
   - insert 剩余 > 0 → 退回源容器（simulate 先行已保证可放，退回仅兜底）
   - 成功移动 → 该源计数累加，达 perBeatLimit 则跳过该源
5. 更新 ACTIVE 状态（setActive）
```

### 3.3 目标选择策略（决策优化点）

1. **空间优先（least-loaded first）**：目标按「剩余可接收量」降序排序，空缺最大者优先。
   - 理由：把物品送往最空的目标，收敛快、天然平衡、避免「塞满一个再堆下一个」的偏斜。
2. **round-robin 轮转（防振荡）**：同剩余量并列时，用网络级轮转指针（root 持有的环形指针，按目标索引轮转）打破平局。
   - 防振荡：避免多个源在同一节拍反复竞争同一目标（尤其多个目标空位相等时）。
3. **排除回源**：目标容器 == 候选源容器时跳过（同容器不进不出，防自环第一步）。
4. **优先级（可选扩展）**：暂无优先级升级；设计预留「按 Push 管道 speedLevel 隐式排序」或未来新增优先级升级的扩展位，本次不实现。

### 3.4 单次传输量（堆叠传输）

- **每源每节拍限量** `perBeatLimit = singleTransferAmount()` = `(1 + 7×speedLevel) << enderLevel`（沿用现状公式，公式语义不变）。
- 执行粒度：**堆叠传输**——单次 extract 尽量取满（min(perBeatLimit, 槽内量, 堆叠上限 64)），一次搬多物品；不再「每 tick 1 次操作」限流，改为「每节拍限量」批处理。

### 3.5 节拍定案（待确认，推荐 A）

| 方案 | 语义 | 吞吐 | Jade 文案 | 性能 |
|---|---|---|---|---|
| **A（推荐）**：root 每 tick 撮合 | 每源每 tick 限量 singleTransferAmount | singleTransferAmount 物品/tick | 「物品/tick」不变 | root 前剪枝（O(6) 局部比较，见下注记）→ 仅局部极小点 BFS，通常每网络每 tick 1 次 |
| B：5 tick 节拍撮合 | 每源每节拍限量 singleTransferAmount | singleTransferAmount/5 物品/tick | 需改（如「每 5 tick」） | 更低，响应延迟 5 tick |

**推荐 A**：Jade 文案不变、吞吐语义直观、扫描成本净下降；大网络可用「网络缓存 + 变更失效」（管道/相邻容器变化时重扫）作为后续优化，不阻塞本次。

**§3.5 注记（与实现一致）**：tick 中 root 判定前先做 O(6) 局部坐标剪枝（`TransferNetworks.hasSmallerNeighbor`）——存在按 x→y→z 字典序（与 `isRoot` 最小判定同序）更小的相邻管道 ⇒ 本管道必非 root（相邻管道同网络），直接 return、免整网络 BFS；仅「局部极小点」执行 `collectConnected`（sound 剪枝：不会误剪 root；非 root 但无更小邻居的节点仍 BFS 后由 `isRoot` 判定）。故实际 BFS 次数 = 每 tick 局部极小点数量：常规网络 1 次（≈宣称的「每网络每 tick 1 次」），复杂拓扑最坏 N 次（每管道一次，等价重构前）。**可接受性评估**：剪枝将「每管道每 tick 全量 BFS」降为「局部极小点 BFS」，主路径（root 撮合）不变；彻底的「每网络每 tick O(1) BFS」需网络缓存 + 变更失效，列为后续优化（见 §5 风险 2）。

### 3.6 防循环/防振荡

1. **目标排除同源**：目标容器 == 候选源容器 → 跳过（物品不回自身）。
2. **round-robin 打破平局**：同空间分数目标轮转，防多源抢同一目标振荡。
3. **每源限量**：perBeatLimit 上限，防单节拍清空源容器引发下游连锁。
4. **双侧过滤 AND**：Push 目标侧过滤拦截不需要的物品，从需求侧防止「塞入不需要物品又被抽走」的浪费型振荡。
5. **说明**：完整环路检测（图论，A→B→A 型）不在本次范围；由「排除回源 + 过滤」缓解，列为风险 5。

### 3.7 升级系统衔接

| 升级 | 现状 | 新模型 |
|---|---|---|
| PULL（粘性活塞，1 级） | 解锁主动拉取 | **保持**：解锁 Pull 出口角色（0→1） |
| PUSH（活塞，1 级） | 解锁主动推入 | **保持**：解锁 Push 入口角色（0→1） |
| SPEED（糖，9 级） | 单次量 +7/级 | **保持公式**：perBeatLimit = 1+7×speedLevel（作用对象从「单次操作量」变为「每 tick 每源限量」） |
| ENDER（末影珍珠，8 级） | 单次量 ×2^level | **保持公式**：×2^enderLevel（沿用「数量倍增」现状语义；「跨维度」为名称遗留，不在本次范围） |
| PAGE（书，63 级） | 过滤页数 27×(1+pageLevel) | **不变**：isItemAllowed 不变，双侧过滤同时受益 |

无新增升级；目标选择策略（空间优先+round-robin）为网络级默认行为，不依赖升级。

### 3.8 移除清单

| 组件 | 处置 | 证据/说明 |
|---|---|---|
| `processNetwork()` | **删除** | 被动无差别搬运被撮合取代 |
| `transportHandler`（匿名 IItemHandler） | **删除** | 网络代理假 handler 被撮合取代；grep 确认 `getTransportHandler` 仅定义、无调用者 |
| `getTransportHandler()` | **删除** | 同上 |
| `tickPull()` / `tickPush()` / `tickPullPush()` | **重构** | 语义变为 Pull 源收集 / Push 目标收集；撮合在 root 执行 |
| `extractAllowed()` | **重构** | 逻辑（simulate+过滤+真实抽取）内联进 Pull 源收集 |
| `TransferNetworks.moveItems()` | **删除** | 撮合自行 extract/insert；仅 `processNetwork` 调用（注意 P3NetworkContractTest 有「moveItems 必须保留供 Pipe」断言，需同步改） |
| `TransferNetworks.insertItem()` | **保留** | 撮合执行插入 |
| `TransferNetworks.collectConnected()` / `isRoot()` | **保留** | 网络发现 |
| `singleTransferAmount()` | **保留** | perTickLimit 公式（public，Jade 依赖） |
| `isItemAllowed()` / `filterInventory` / 升级持久化 | **保留** | 不变 |

**新旧 tick 结构对比（方案 A）：**

```java
// 旧
protected void tick() {
    if (level == null || level.isClientSide()) return;
    tickPullPush();                                   // 每 tick：主动
    if (getAliveTime() % 5 != 0) return;              // 5 tick 节拍
    Set<BlockPos> network = TransferNetworks.collectConnected(...);
    if (!TransferNetworks.isRoot(network, worldPosition)) return;
    processNetwork(network);                          // 被动搬运
}

// 新（伪代码）
protected void tick() {
    if (level == null || level.isClientSide()) return;
    if (TransferNetworks.hasSmallerNeighbor(level, worldPosition, ...)) return;  // root 前剪枝（O(6)，非 root 免 BFS）
    Set<BlockPos> network = TransferNetworks.collectConnected(...);  // 仅局部极小点 BFS
    if (!TransferNetworks.isRoot(network, worldPosition)) return;     // 仅 root 撮合
    matchAndTransfer(network);                        // 收集 Pull 源 + Push 目标 → 分配 → 执行
}
```

---

## 4. 影响面

### 4.1 代码

| 文件 | 改动 |
|---|---|
| `PipeBlockEntity.java` | 删除 `transportHandler` / `getTransportHandler` / `processNetwork`；重构 `tick` / `tickPull` / `tickPush` / `extractAllowed` → 新增 `collectPullSources` / `collectPushTargets` / `matchAndTransfer` |
| `TransferNetworks.java` | 删除 `moveItems`；保留 `collectConnected` / `isRoot` / `getItems` / `insertItem` |
| `PipeUpgradeType.java` | **不变**（枚举、材料、上限） |
| `TENBlocks.java` / `TENBlockEntities.java` | **不变** |
| `CableBased.java` / `BaseMachineBlock.java` | **不变**（连接判定、GUI 打开逻辑） |

### 4.2 测试

| 测试 | 处置 |
|---|---|
| `PipePullPushContractTest` | **重写**：tickPull/tickPush/transportHandler/processNetwork 断言全部失效；改为断言新语义（pullLevel>0 收集源、pushLevel>0 收集目标、root 撮合、双侧过滤 AND、目标排除同源、perTickLimit、空间优先+round-robin） |
| `P3NetworkContractTest` | **局部改**：P3-T4「moveItems 必须保留供 Pipe」断言随 moveItems 删除而失效，需改为断言新撮合入口 |
| `PipeJadeContractTest` | 基本保留：`singleTransferAmount` 保留则 IO 速率断言不变；方案 A 下 lang 断言不变 |
| `PipeUpgradeContractTest` | **保留**（升级枚举/持久化/交互不变） |
| `PipeFilterContractTest` / `PipePageContractTest` | **保留**（isItemAllowed/过滤槽/翻页不变） |
| （新增）`PipeMatchContractTest` | 新增：空间优先排序、round-robin 轮转、双侧过滤 AND、目标排除同源、每源限量、无目标退回源 |

### 4.3 Jade

- `PipeJadeProvider` 结构保留（过滤模式/升级层级/页数显示不变）；IO 速率行依赖节拍定案——方案 A 文案「物品/tick」不变，方案 B 需改 lang。

### 4.4 GUI

- 过滤 GUI（pipe_white/pipe_black 潜行右键）：**不变**（过滤槽/翻页/模式标题）。
- 普通 pipe 无 GUI：**不变**；Pull/Push 角色状态经 Jade 升级层级显示（`Pull: 1/1`）。
- **无模式切换 UI**：新模型用升级表达角色，无需 GUI 改动。

### 4.5 Lang

- `pipe.upgrade.*` / `pipe.filter.*` / `pipe.jade.pages`：**不变**。
- `pipe.jade.io_rate`：方案 A 不变；方案 B 改文案。

---

## 5. 风险

1. **语义变化（用户可见）**：移除被动自动搬运 → 未升级网络不再自动传输物品；依赖旧被动传输的存档网络升级后停滞，需玩家补 Pull/Push 升级。属预期行为变化，需在 CHANGELOG/更新说明中明确。
2. **撮合性能**：每 tick root 撮合对大型网络的候选/目标遍历成本。缓解：候选/目标收集带限量早退出、BFS 结果缓存+变更失效（后续优化）、节拍化开关（方案 B）。
3. **退回语义**：extract 成功但 insert 失败时退回源容器；simulate 先行保证 insert 可放，退回仅兜底。
4. **多源竞争目标**：多 Pull 源命中同一 Push 目标时，空间优先+限量避免过度竞争；目标塞满后剩余候选退回源、下节拍重试（每 tick 重试，无死锁）。
5. **A→B→A 环路**：完整环路检测不在本次范围；由「目标排除同源」+ 双侧过滤缓解。极端拓扑（B 容器同时是某 Pull 管的源与某 Push 管的目标）可能出现往返型浪费振荡——列为后续增强（来源跟踪 / 目标最近命中缓存）。
6. **存档兼容**：升级字段/过滤字段持久化不变 → 旧存档可读；无新持久化字段（无网络缓冲）→ 零迁移负担。
7. **测试重写成本**：`PipePullPushContractTest` 全重写 + `P3NetworkContractTest` 局部改 + 新增撮合契约测试；源码模式扫描断言需与新结构精确匹配。

---

## 6. 待指挥官确认的决策点

1. **节拍**：方案 A（每 tick root 撮合，Jade 文案不变，吞吐=singleTransferAmount/tick）✅ 推荐 vs 方案 B（5 tick 节拍，文案改）。
2. **目标选择**：空间优先 + round-robin ✅ 推荐；是否引入优先级（本次默认不加）。
3. **过滤**：双侧 AND（Pull 源侧 + Push 目标侧）✅ 推荐。
4. **移除范围**：`moveItems` 删除（✅ 推荐，同步改 P3NetworkContractTest）vs 停用保留。
5. **外部检索**：本地无参考 mod jar；如需精确对标某 mod（如 Thermal servo/retriever、Pipez 目标选择），列出资料需求由指挥官协调诺雅检索。

### 可选的外部检索需求（供指挥官协调诺雅）

- Pipez / EnderIO / Mekanism / Thermal Dynamics / XNet 的物品管道传输源码或 wiki 文档：目标选择算法（空间优先/round-robin/优先级）、单次量公式、防振荡与防循环机制、节拍设计。
- 本地 `run/*/mods` 无参考 jar；如需读源码需提供 jar 或源码路径。
