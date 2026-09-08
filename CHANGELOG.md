# ChangeLog

本文件记录 Kenergy Engineering Retechnicalized（1.21.1 分支）的显著变更。

格式参照 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，
版本号遵循项目自身语义（见 `gradle.properties` 的 `mod_version`）。

## [Unreleased]

### Added
- **频道系统（P0-8）**：ChannelKey / ChannelType / SharedStorage / ChannelRegistry 数据层与 BE 层移植
- **JEI/EMI 深度集成（XEI）**：5 模块全量对齐 26.1.2 参照，适配 JEI 19.x API；熔炼机注册为原版熔炉配方催化剂
- **Jade 集成**：频道状态 + 管道过滤 provider
- **机器 GUI**：全机器标题标签、升级槽面板、渐显（UV 裁切）能量/燃料/进度仪表、范围显示 BER
- **批处理 B 锁定模型（P0-2）**：输入/流体/输出/能量四维锁定，Fixed-B 停滞契约（部分取出等待恢复）
- **面配置系统（P0-3）**：机器各面能量/物品/流体模式配置
- **升级乘法模型（P0-4）**
- **新机器（P1）**：方块破坏器、方块成型器、冷却器 + Coolants 冷却液系统
- **配置系统（P1-3）**：迁移至 NeoForge 原生 ModConfigSpec
- **物品体系（P2）**：能量模块（原能量单元）、频道连接器（原频道桥接器）等改名；扳手拆解服务（WrenchDismantleService）
- **配方数据生成**：DataGen 迁移 + Mushrium/Netherite 材料体系
- **mise.toml** 任务运行器配置

### Changed
- **能量模型重构（P0-1）**：maxProgress = baseTickTime × durationMultiplier，解除配方与总能耗绑定
- **停滞语义修正（P0-5）**：能量不足/信号关闭/输出满时保留进度等待恢复（旧版归零废弃）；效果型机器（冷却器/信标模拟机等）原料消失则进度作废归零，处理型/配方型机器缺料停滞仍保留进度
- **数据同步体系**：全量迁移 ISyncPersistRPCBlockEntity；禁用异步 sync 线程，改为主线程 passivelySync() 统一同步入口
- **LDLib2 升级 2.2.8 → 2.2.37**
- **全量化对齐 26.1.2**：本地化 / 模型 / 配方 / LDLib2 GUI
- **GUI 素材目录**重命名为英文，清理旧版 legacy blockstate/模型文件
- **农场**：工作区域从圆形改为后方 9×9 正方形，逐行扫描 / 灌木检测 / 成熟度排序

### Fixed
- **渐现进度条闪烁根治**：LDLib2 异步 sync 线程与主线程写字段竞争（官方文档确认的线程安全问题）导致客户端镜像被推入中间态 0/0；同步改为主线程驱动后消除；100% 完成不再倒放（下行瞬移守卫）；效果型机器输入取出进度归零
### Changed
- **管道连接模式语义（对齐 TE4-New 基准）**：物品管道源边仅显式 PULL 面才主动从设备抽取（扳手四态切换 NONE/NORMAL/PUSH/PULL），默认 NORMAL 不再自动搬运；**旧档已放置管道升级后需用扳手重新设定模式**（无数据丢失，物品留在原容器）
- 线缆/管道系列：能源回环防护、VoxelShape 与模型尺寸同步、初始放置连接更新
- 配方： pulverizer 输入 compressTagCategory、深矿物品名修正、严格槽位匹配
- 能量单元 FE 换算、Cell/CreativeCell 纹理切换时序
- 农场作物重置为 age 1（不再破坏重种）
- 库存槽位失配、网络序列化命名空间、重复 lang 键、废弃 keybind bus 参数

### Removed
- 根目录遗留垃圾（提交草稿 / 过期依赖快照），26 个未引用纹理（21 张 PNG + 5 个 mcmeta）归档至 `_unused/`
- 旧版 legacy blockstate / 模型文件
