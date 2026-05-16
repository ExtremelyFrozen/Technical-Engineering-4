# Changelog

## [4.1.0] - 2026-05-16

### ✨ 新增
- 结构化物品注册系统：`registerVariants()` 方法，支持一行声明批量注册材料变体
- 新材料变体：下界合金、钻石、绿宝石、青金石、石英、紫水晶、红石的粒/粉/板/齿轮等变体
- 新材料：蘑菇杆、蘑菇线、蘑菇锭、星辉粉
- 新模具：压缩-小型、压缩-大型、拆分、币、致密板
- 引擎模型迁移：旧项目 Blockbench 3 段式模型（底座+腰环+顶盖）
- 能量单元模型：旧项目 Blockbench 14/13 元素空心框架模型，注册为非完整方块
- 物品纹理路径重构：`textures/item/` 按材料分类归档到 `material/`、`crafting/`、`mold/`、`upgrade/` 子目录
- 统一占位符纹理脚本：`scripts/generate_placeholders.ps1`
- 升级槽信息现在直接显示在工具提示中（无需按 Shift）
- 升级提示使用精确数值和 `\n` 换行格式
- 信标模拟器升级：安装后药水效果等级 +1（代码+描述）
- ItemZoomer 物品放大预览模组（开发环境）

### 🔧 修改
- ModernUI 修复：`modClientLocalRuntime` 解决 DataGen 与客户端兼容问题
- 创造标签页图标：锡锭 → 自律红石智能芯片
- 锡锭归化到批量注册体系（不再独立注册）
- 马什洛姆系列本地化名称：马什洛姆 → 蘑菇（引用泰拉瑞亚蘑菇锭）
- 冶炼升级提示：添加配方限定说明（高炉配方/烟熏配方）
- 采石场升级提示：移除概率明细，改为模式描述
- 液态经验桶/奇异物质桶本地化修复
- 删除重复的 `_legacy` 和 `__backup_*` 文件（42个）
- 清理临时 clone 目录 `te3-models`

### 🗑️ 删除
- `iron_nugget`、`gold_nugget` 注册（与原版冲突）
- `copper_nugget` 纹理（原版已注册）
- 旧纹理子目录冗余副本（`dust/`、`gear/`、`ingot/` 等）

### 🐛 修复
- 能量单元注册为非完整方块（`.noOcclusion()`），内部空心正确渲染
- 马什洛姆/星辉/grow_levelup 纹理未分类的路径修复
- `kenergyengineering.` 前缀 lang key → `item.kenergyengineering.` 正确格式
