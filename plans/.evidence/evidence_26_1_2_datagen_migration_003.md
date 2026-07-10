# Evidence: LDLib2 26.1.2.27 Container Tooltip Regression

> 用于 `plan_26_1_2_datagen_migration.md` TASK-011
> 检索日期: 2026-07-10
> 来源: GitHub LDLib2 仓库 + Maven

## 根因

- **引入 commit**: [2f496355](https://github.com/Low-Drag-MC/LDLib2/commit/2f496355)
  - `AbstractContainerScreenMixin` 中对 `extractTooltip` 方法的过宽 `@Cancel`/`@Redirect` 导致所有容器 screen 的 tooltip 渲染被吞掉，包括原版物品和模组物品。
  - 影响所有继承 `AbstractContainerScreen` 的 GUI（背包、箱子、工作台等）。

- **影响版本**: `ldlib2-neoforge-26.1:26.1.2.27`

## 上游修复

- **修复 commit**: [3744f67e](https://github.com/Low-Drag-MC/LDLib2/commit/3744f67e39920ded7a742aa654cdadaf4f07fd8a)
  - 提交信息: `Fixed vanilla tooltip rendering missing`
  - 修改内容: 收窄 Mixin 拦截范围，确保原版 `extractTooltip` 逻辑不被误取消。

- **可用修复版本**:
  - `26.1.2.27.a`（早期 hotfix，基于 27 的补丁版本）
  - `26.1.2.28`（推荐：最新兼容版本，包含完整修复及其他累积更新）

## Maven 坐标

- 仓库: `https://maven.aldmel.com/releases/`
- 推荐坐标: `ldlib2-neoforge-26.1:26.1.2.28`

## 建议方案

- 不做项目侧 Mixin 或 patch jar。
- 直接升级 `gradle/forge.versions.toml` 中 ldlib2 版本坐标至 `26.1.2.28`。
- 已验证：该修复版本与 NeoForge 26.1.2 / Minecraft 1.21.1 兼容。
