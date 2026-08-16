# 机器主体 GUI 重写 — machine_gui.png 统一底图 + JEI v4 布局 + 大物品输出槽

计划 ID：machine_gui_rewrite_001
状态：已批准
草稿历史：plans/.draft_plan_machine_gui_rewrite_001.md
正式路径：plans/plan_machine_gui_rewrite_001.md
下一步：进入执行

## 目标

将 6 个配方处理类机器（熔炼机/粉碎机/感应炉/精炼机/灵能处理器/压缩机）的主体 GUI 从各机器专属背景 PNG + 透明槽位重构为：统一空白底图 machine_gui.png（空.png 重命名，176×166）+ JEI v4 布局规则（能量条 x=8 锚定、组件群水平居中、间距自适应）+ 单物品输出槽改 26×26 大槽（pulverizer 2×2 除外）+ 槽位背景切 modular 素材族。

## 范围

- 包含：TENConstants.java（新增 ITEM_SLOT_LARGE、MACHINE_GUI 常量）
- 包含：TENMachineBlockUIFactory.java（backgroundFor 映射、machineSlot 尺寸/背景参数化）
- 包含：6 机器 createUI：FurnaceBlockEntity(smelter)/PulverizerBlockEntity/CompressorBlockEntity/RefinerBlockEntity/IndfurBlockEntity/PsionicantBlockEntity
- 包含：textures/gui/空.png → machine_gui.png 重命名（git mv）
- 包含：验证：compileClientJava/compileTestJava/test/spotlessCheck/git diff --check
- 不包含：删除旧背景 PNG（one_to_one/pulverizer/compressor/one_to_one_fluid/three_to_one/two_to_one 保留回退）
- 不包含：删除 handler.png atlas
- 不包含：玩家物品栏/升级槽/左侧边栏
- 不包含：机器名 Label(6,4)
- 不包含：配方/注册项/语言键/模型
- 不包含：其他 10 种非 6 机器 GUI
- 不包含：JEI/XEI 兼容层（已由 plan_jei_layer_rewrite_001 覆盖）
- 不包含：ModDev 游戏内视觉验证

## 执行顺序

1. P0-CHK 预检：PIL 验证空.png=176×166 与 item_slot_large.png=26×26；空.png→machine_gui.png git mv 重命名；TENConstants 新增 ITEM_SLOT_LARGE(26×26) 与 MACHINE_GUI(176×166) 常量
2. P1-RSCH 调研：定位 LDLib2 源码/jar 确认 ItemSlot 26×26 配置方式（layout 尺寸/物品渲染区域/叠加方案），产出支持/需叠加/不支持三选一结论
3. P2-FACT 工厂基础：backgroundFor 六分支统一返回 MACHINE_GUI；itemSlot 尺寸参数化（18×18/26×26）；槽位背景 EMPTY→modular（ITEM_SLOT_SMALL/ITEM_SLOT_LARGE/FLUID_SLOT）；按 D1 切 energyGauge/fuelGauge/progressGauge 素材源
4. P3-LAYOUT 六机器重排（按 JEI v4：能量条 x=8 锚定、组件群水平居中、间距≥4px 自适应、垂直居中公式）：pulverizer(2×2 小槽块整体)→compressor→psionicant→indfur→smelter(XP 槽 y=66 契约)→refiner(流体 18×50 y=0、小能量条按 D2)
5. P4-VERIFY：compileClientJava/compileTestJava/cleanTest test（重点 XpFluidRefactorContractTest + JEI 契约）/spotlessCheck/git diff --check
6. P5-REVIEW 收口：艾琳审查门禁、证据留档、坐标表记录

## DoD

- [ ] 空.png 重命名为 machine_gui.png 且 git 历史保留
- [ ] TENConstants 新增 ITEM_SLOT_LARGE、MACHINE_GUI 常量，编译通过
- [ ] backgroundFor 六分支返回 MACHINE_GUI；6 机器 createUI 无旧背景残留
- [ ] 6 机器布局满足 JEI v4：能量条 x=8 锚定、组件群水平居中、间距≥4px 无重叠、垂直居中公式成立
- [ ] 5 机器（smelter/compressor/refiner/indfur/psionicant）单输出槽=26×26 大槽+ITEM_SLOT_LARGE；pulverizer 2×2 保持 18×18 小槽
- [ ] 槽位背景：机器槽/大槽/流体槽来自 modular 素材
- [ ] smelter createXpFluidSlot 行含 66；XpFluidRefactorContractTest 全 GREEN
- [ ] 燃料/进度/流体功能保持（绑定不变）
- [ ] compileClientJava/compileTestJava/cleanTest test 全量 0 failed
- [ ] spotlessCheck、git diff --check 通过
- [ ] 旧背景 PNG 与 handler.png 未删除（回退保障）
- [ ] 机器名 Label(6,4)、玩家物品栏(7,83,162,58)、升级槽(-28)、侧栏(-27) 未改动

## 风险与回退

- 风险：R1 smelter XP 槽契约锁定（y=66+非重叠断言）→ 保留 (8,66,14,46)
- 风险：R2 LDLib2 ItemSlot 26×26 渲染适配未知 → P1 前置调研，降级 D4 视觉大槽+18×18 交互槽
- 风险：R3 背景切换后槽位视觉来源变化 → 旧 PNG 保留回退
- 风险：R4 能量条 x=9→8 位移 → 非重叠断言核对
- 风险：R5 间距计算重叠 → P3 每机矩形校验
- 风险：R6 空.png 尺寸非 176×166 → P0 拦截上报
- 风险：R7 XpFluidRefactorContractTest 其他文本断言 → 保持 createXpFluidSlot 调用形态
- 回退：执行前 git checkpoint
- 回退：P2/P3 每机器独立提交可单独 revert
- 回退：旧背景 PNG + handler.png 保留
- 回退：回退=恢复 checkpoint→还原 6 createUI+工厂+常量→machine_gui.png 改回空.png
