# JEI 兼容层重写 — modular/jei_handler.png 新素材接入（JEI + XEI 双侧一致）

计划 ID：jei_layer_rewrite_001
状态：已批准
草稿历史：plans/.draft_plan_jei_layer_rewrite_001.md
正式路径：plans/plan_jei_layer_rewrite_001.md
下一步：进入执行

## 目标

将全部 JEI 兼容层（机器通用、熔炼机 3 页、引擎燃料 3 页）与 XEI 共享布局引擎（TENRecipeWidget）从旧 gui/handler.png、gui/jei_handler1/2.png 迁移到新建 gui/modular/ 素材族（底图 jei_handler.png：精炼机 170×50 下层、其他机器 150×50 上层），实现 JEI 侧与机器 GUI 内嵌配方预览视觉一致，且全部编译测试通过。

## 范围

- 包含：src/main/java/com/modularmc/ten/TENConstants.java（新增 modular 素材常量，保留现有三常量）
- 包含：src/client/java/com/modularmc/ten/integration/jei/TENJeiCategory.java（补画背景 150×50/refiner 170×50、fluidSlot→FLUID_SLOT、槽背景→ITEM_SLOT_SMALL）
- 包含：src/client/java/com/modularmc/ten/integration/jei/SmelterJeiCategory.java（三页改 150×50 重排 + progress_arrow_01 + energy_gauge + item_slot_small）
- 包含：src/client/java/com/modularmc/ten/integration/jei/EngineFuelCategory.java（三页改 150×50 + fuel_gauge_01，仅本页允许）
- 包含：src/client/java/com/modularmc/ten/integration/xei/TENRecipeWidget.java（六布局切 modular 素材：compressor 150×58→150×50、DEFAULT 160×80→150×50、progress→PROGRESS_ARROW_NN、burnLeft→ENERGY_GAUGE、drawSlot→ITEM_SLOT_SMALL）
- 包含：src/test/java/com/modularmc/ten/integration/jei/SmelterJeiContractTest.java 等契约测试同步新布局常量
- 包含：modular/jei_handler.png 底图尺寸预检（下层 170×50，用户已改图）
- 不包含：handler_parts/ 素材目录（ModUI 现用平行目录）
- 不包含：删除旧 gui/handler.png / jei_handler1.png / jei_handler2.png（保留作回退）
- 不包含：配方、注册项、语言键、模型改动
- 不包含：ModDev 游戏内视觉验证（以测试+静态分析替代）
- 不包含：TENJeiSlotOverlay 槽文字逻辑
- 不包含：TENMachineBlockUIFactory（机器 GUI 本体，仅 XEI 内嵌预览受影响）

## 执行顺序

1. P0-CHK 底图预检：像素级验证 modular/jei_handler.png 下层为 170×50（上层 150×50）；未通过则暂停全部执行并提示用户
2. P1-CONST 常量接入：TENConstants.java 新增 JEI_HANDLER_MODULAR、ENERGY_GAUGE_BG/FILL、FLUID_SLOT、ITEM_SLOT_SMALL、PROGRESS_ARROW_01..06_BG/FILL、FUEL_GAUGE_01_BG/FILL；GUI_HANDLER/JEI_HANDLER_1/2 原样保留
3. P2-GENERIC 通用机器类别：TENJeiCategory.java 补画背景（150×50 上层 / refiner 170×50 下层）、fluidSlot→FLUID_SLOT(18×50)、槽背景→ITEM_SLOT_SMALL(18×18)
4. P3-SMELTER 熔炼机：SmelterJeiCategory.java 三页 170×54→150×50 重排（能量条(6,2)14×46、输入槽(40,16)、箭头 progress_arrow_01(64,17)、输出槽(92,16)、文本(40,38)），全换 modular 素材
5. P4-ENGINE 引擎燃料：EngineFuelCategory.java 三页改 150×50 + fuel_gauge_01(13×13)（替代 GUI_HANDLER burnIcon），仅本页允许引用 fuel_gauge
6. P5-XEI 布局引擎：TENRecipeWidget.java 六布局切 modular 素材（pulverizer/compressor/induction_furnace/psionicant→150×50 上层、refiner→170×50 下层、DEFAULT 160×80→150×50 重排、compressor 150×58→150×50 双输入槽重排、progress decoration→PROGRESS_ARROW_NN、burnLeft 13×13→ENERGY_GAUGE、drawSlot 24×24→ITEM_SLOT_SMALL）
7. P6-TEST 契约同步：SmelterJeiContractTest 断言改新布局常量（WIDTH=150/HEIGHT=50/INPUT_X=40/OUTPUT_X=92/ARROW_X=64/TEXT_X=40/TEXT_Y=38 等），背景断言 JEI_HANDLER_2→JEI_HANDLER_MODULAR；其余 8 个 JEI 测试回归确认
8. P7-VERIFY 验证：gradlew.bat compileClientJava compileTestJava cleanTest test + spotlessCheck + git diff --check 全绿

## DoD

- [ ] modular/jei_handler.png 下层确认为 170×50（P0 验证结果记录）
- [ ] 全部 JEI 兼容层与 XEI 六布局仅引用 modular/ 白名单素材（grep 确认 5 个目标 Java 文件无 GUI_HANDLER/JEI_HANDLER_1/2 残留，GUI_HANDLER 常量定义与 CableBlockEntity 引用除外）
- [ ] 熔炼机/引擎/五机器/XEI 布局尺寸与底图层一致（150×50 或 refiner 170×50）
- [ ] SmelterJeiContractTest 断言与新布局常量一一对应且全 GREEN，不删减契约覆盖
- [ ] compileClientJava / compileTestJava / test 全量通过（0 failed）
- [ ] spotlessCheck 与 git diff --check 通过
- [ ] 旧素材文件未删除（回退保障）

## 风险与回退

- 风险：底图改图未完成（下层仍 170×54）→ P0 拦截暂停
- 风险：progress_arrow 01~06 机器映射与用户意图不符 → 映射集中一处，改映射只动一处
- 风险：DEFAULT_LAYOUT 160×80 归属决策 → 推荐 150×50 重排；用户坚持保留则需新增底图层
- 风险：compressor 150×58→150×50 双输入槽出界 → P5 内校验无重叠后过审查
- 风险：SmelterJeiContractTest 大量文本级断言破坏（预期）→ P6 同步重写，禁止删减
- 风险：XEI 侧流体槽背景来源不明（ModUI 容器绘制？）→ 执行时确认
- 风险：GUI_HANDLER 删除误伤 CableBlockEntity ModUI 根背景 → 只增不删，审查门禁检查
- 回退：执行前建 git checkpoint（当前工作区未提交状态）
- 回退：P1~P5 每个任务文件独立 git 恢复回退
- 回退：旧 gui/handler.png/jei_handler1.png/jei_handler2.png 不删除，随时可回切
- 回退：P6 更新前先跑全量 JEI 测试留基线；失败即回退对应断言
- 回退：全局回退：恢复 checkpoint → 还原 5 个目标 Java 文件 → modular 素材无引用无副作用
