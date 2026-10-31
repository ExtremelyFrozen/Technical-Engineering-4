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

### 正常链路启动复验（testframework） — P2 🧪
- 2026-10-31 已用 `runClient -x compileTestJava` 启动并进入世界，随后正常关闭；AE2 Pattern Disk、FTB Library、
  AE2WTLib、Architectury 均加载。
- 正常 `runClient` 仍会先编译 test 源集，并卡在 `net.neoforged:testframework:21.1.243`
  （`maven.neoforged.net` 不可达）。
- 网络恢复或本地缓存 testframework 后，去掉 `-x compileTestJava` 再跑一次 `runClient` 与 `runGameTestServer`。

### GuideME 指南实机验证 — P2 🧪
- 指南物品只在 GuideME 在场时注册；需验证工具页指南书可打开、导航与页面物品图标正常。
- 未装 GuideME 时模组正常加载且不出现指南物品；指南不依赖 AE2。

## 二、未开始

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
