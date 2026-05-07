# Technical-Engineering 4 - 运行问题清单

> 从 te3 (Forge 1.18.2) 迁移到 TEN4 (NeoForge 1.21.1) 的剩余问题

---

## 1. 机器纹理紫黑 (Missing Textures) ✅ 已修复

**修复内容**:
- 批量替换 `src/main/resources/` 下所有手动 JSON 中的旧 namespace `technicalengineering:` -> `kenergyengineering:`
- 生成的模型 JSON 已正确使用 `kenergyengineering:` namespace
- 纹理文件位于正确位置 `assets/kenergyengineering/textures/machine/`

---

## 2. 创造模式标签页为空 (JEI: "Item Group has no display items") ✅ 已修复

**修复内容**:
- 验证了 `TENRegistrate` 中的 `TAB_LOOKUP` 映射机制正确
- Java 类加载顺序保证 `TENCreativeModeTabs` 在 `TENBlocks` 之前初始化
- `REGISTRATE.getAll(Registries.ITEM)` 和 `REGISTRATE.isInCreativeTab()` 模式与 GregTech-Modern 一致

---

## 3. Channel 方块使用错误的方块类型 ✅ 已修复

**修复内容**:
- `TENBlocks.java` 中 CHANNEL_ENERGY/ITEM/FLUID 已使用 `DirectionalMachineBlock::new`（6方向）
- 手动 blockstate JSON 已更新 namespace 并支持 6 方向 (up/down/north/south/east/west)

---

## 4. 物品模型文件缺失 ✅ 已修复

**修复内容**:
- `TENBlocks.java` 中机器 helper 使用 `.item().model((ctx, prov) -> prov.blockItem(ctx::getEntry))` 生成 item model
- 创建了所有方块物品的手动 item model JSON（37个），parent 指向对应 block model
- 手动 blockstate/model JSON namespace 已全部更新

---

## 5. Cell 方块模型缺失 ✅ 已修复

**修复内容**:
- 创建了 `models/block/cell.json` 和 `models/block/cell_empty.json` 模型文件
- Cell blockstate JSON namespace 已更新
- Cell item model JSON 已创建

---

## 6. EMI 插件加载崩溃 ✅ 已修复

**修复内容**:
- `TENEmiPlugin.CategoryDef` 中 "Induction Furnace" 已改为 `"induction_furnace"`

---

## 7. JEI 插件静态初始化问题 ✅ 已修复

**根因**: `TENJeiPlugin.java` 中 `CATEGORIES` 列表在静态初始化时使用 `new ItemStack(TENBlocks.MACHINE_PULVERIZER)`，此时 Registrate 尚未完成注册，导致 NPE。

**修复**: 改为使用 `BuiltInRegistries.ITEM.get(TEN.id("machine_pulverizer"))` 延迟获取图标。

---

## 状态总结

| # | 问题               | 严重程度 | 状态    |
|---|------------------|------|-------|
| 1 | 机器纹理紫黑           | 🔴 高 | ✅ 已修复 |
| 2 | 创造模式标签页为空        | 🟡 中 | ✅ 已修复 |
| 3 | Channel 使用错误方块类型 | 🟡 中 | ✅ 已修复 |
| 4 | 物品模型缺失           | 🟡 中 | ✅ 已修复 |
| 5 | Cell 模型缺失        | 🟡 中 | ✅ 已修复 |
| 6 | EMI 插件崩溃         | 🟡 中 | ✅ 已修复 |
| 7 | JEI 插件初始化问题      | 🟢 低 | ✅ 已修复 |
