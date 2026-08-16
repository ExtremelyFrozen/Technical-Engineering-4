# 频道系统重构「末影箱模式」— 共享存储 + GUI 创建/接入/退出 + 连接器废弃

计划 ID：channel_ender_002
状态：已批准
草稿历史：plans/.draft_plan_channel_ender_002.md
正式路径：plans/plan_channel_ender_002.md
下一步：进入执行

## 目标

将频道系统从「坐标绑定+逐 tick 轮询传输」重构为「末影箱模式」：绑定同一频道的方块共享同一虚拟存储（物品/流体/能量按类型独立），频道在 GUI 内创建（命名）/接入（点击）/退出（按钮），容量=标准容量×接入数（槽位固定、堆叠/容量动态），零 tick 传输（接入后直接绑定共享存储），存档级持久化跨维度，废弃 ChannelConnectorItem。

## 范围

- 包含：新建 ChannelRegistry/SharedStorage/ChannelKey（channel 包）
- 包含：AbstractChannelBlockEntity + ChannelItem/Fluid/EnergyBlockEntity（channelId/join/leave/共享绑定/UI）
- 包含：TENMachineBlockUIFactory（如共享绑定工厂方法）
- 包含：TENItems/TENCreativeModeTabs/TENModelProvider/TENLangHandler（连接器废弃 5 处）
- 包含：删除 ChannelConnectorItem/IModeChangable
- 包含：P3NetworkContractTest（P3_T4 改写）+ 新契约测试
- 包含：验证：compileClientJava/compileTestJava/test/spotlessCheck/git diff --check
- 不包含：新材质绘制（频道 GUI 用现有 channel.png + modular 素材）
- 不包含：JEI/XEI 兼容层改造
- 不包含：旧存档数据迁移工具（旧连接器物品降级预期）
- 不包含：ModDev 游戏内视觉验证
- 不包含：玩家物品栏/升级槽/侧栏改造

## 执行顺序

1. T001 数据层：ChannelRegistry（存档级持久化、懒加载、跨维度）+ SharedStorage（共享 handler，容量模型：槽位固定、容量=标准×成员数动态）
2. T002 方块层：channelId 字段持久化；join/leave（leave 回流频道、满留本地）；移除逐 tick 传输（tick 无 move* 调用）
3. T003 共享存储绑定：接入后 UI 槽位绑定共享 handler（布局固定、动态容量）；本地缓冲保留
4. T004 UI 层：全局频道目录列表（按类型过滤）+ 创建按钮 + TextField 命名 + 点击接入 + 已接入条目退出按钮
5. T005 连接器废弃：移除 5 处引用 + 删 ChannelConnectorItem/IModeChangable
6. T006 契约测试：注册表/容量公式/join-leave 回流/动态容量新契约；P3_T4 冲突断言改写
7. T007 验证收口：全量编译测试 + spotless + diff check + grep 残留检查

## DoD

- [ ] T001 数据层：ChannelRegistry（Map<ChannelKey(名称,类型), SharedStorage>）+ SharedStorage（共享 ItemHandler/FluidTank/EnergyStorage）；容量模型：槽位数固定（物品9/流体tank/能量单元），容量=标准容量×成员数（物品每槽堆叠=64×成员数、流体=2000mB×成员数、能量=kFE(1)×成员数）动态重算；懒加载+存档级持久化+跨维度
- [ ] T002 方块层：channelId 字段（持久化）；join/leave API（leave 时本地内容优先回频道、频道满留本地）；移除逐 tick 传输（nextRoundRobin/resolveInputs/Outputs/move* 调用）；supportsUpgradeSlots=false 保留
- [ ] T003 共享存储绑定：接入后槽位绑定共享 handler（UI 布局固定、容量动态）；本地缓冲保留作断开回流源
- [ ] T004 UI 层：列表显示全局频道目录（按类型过滤）+ 右侧创建按钮 + TextField 命名 + 点击接入 + 已接入条目退出按钮 + 当前频道标识
- [ ] T005 连接器废弃：5 处引用移除（TENItems:136/TENCreativeModeTabs:130/TENModelProvider:165,252/TENLangHandler:84-91）+ 删 ChannelConnectorItem/IModeChangable
- [ ] T006 契约测试：新增注册表/容量公式（堆叠×成员）/join-leave 回流/动态容量契约；P3_T4_ChannelVerification 5 条冲突断言改写
- [ ] T007 验证：compileClientJava/compileTestJava/cleanTest test/spotlessCheck/git diff --check 全绿 + grep 残留（channel 包无 move*、src 无 channel_connector、无 nextRoundRobin 死代码）

## 风险与回退

- 风险：R1 LevelSavedData 跨维度持久化无先例 → 首选独立文件（EnderStorage 模式）或主维度 SavedData 转发
- 风险：R2 ItemHandler 动态堆叠/容量（setStackLimit 动态、缩容溢出）→ 子类暴露受控 API/重建迁移，内容不丢硬约束
- 风险：R3 LDLib2 TextField/Button/目录同步无先例 → 复用 @RPCMethod 先例；TextField 受限则降级预设按钮
- 风险：R4 移除逐 tick 传输影响 doBaseData/active 语义 → doBaseData 保留，仅移除轮询调用
- 风险：R5 P3_T4 五条旧断言冲突 → 显式改写而非删测试
- 风险：R6 连接器废弃连锁（IModeChangable/资源残留/旧存档物品）→ grep 全量扫描；旧物品降级预期不迁移
- 回退：执行前 git checkpoint
- 回退：T001-T005 每任务独立提交可单独 revert
- 回退：回退=恢复 checkpoint→还原频道文件→保留注册表类于 git 历史
- 回退：R2 容量溢出：拒绝缩容/回流本地缓冲，内容不丢为硬约束
