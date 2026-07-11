# 证据文件: 官方版本裁决与 Artifact 勘探

- plan_id: `26_1_2_migration_completion`
- evidence_seq: `02`
- 创建日期: 2026-07-11
- 创建者: 猫娘规划师-缇娅（经猫娘检索员-诺雅外部资料检索）
- 触发来源: `REVIEW-TASK-010-20260711` 执行扫描 + `PLAN-CORRECTION-VERSION-20260711` 官方裁决

## 1. Minecraft 版本号制度——Mojang 年号制

- **来源**: https://www.minecraft.net/en-us/article/minecraft-new-version-numbering-system
- **裁决内容**: Mojang 于 2025 年起采用年号制版本号 `YYYY.MM.PATCH`。`26.1.2` 表示 **2026 年 1 月第 2 次补丁发布**，是真实存在的游戏版本，不是 `1.21.1` 的别名或延续。
- **发布日期**: https://www.minecraft.net/en-us/article/minecraft-java-edition-26-1-2
- **引用摘要**: "Minecraft Java Edition 26.1.2 is now available with bug fixes and improvements."

## 2. NeoForge 26.1 版本线

- **来源**: https://neoforged.net/news/26.1release/
- **裁决内容**: NeoForge 26.1 是适配 Minecraft 26.1.x 系列的模组加载器版本线。当前版本 `26.1.2.78` 前三段 (`26.1.2`) 对应目标 Minecraft `26.1.2`。
- **引用摘要**: 官方发布公告确认 26.1 分支目标 Minecraft 26.1.x。

## 3. Java 版本

- **本地构建工具链**: `build.gradle` 中 `java.toolchain.languageVersion = JavaLanguageVersion.of(25)`
- **CI 对齐**: TASK-010 已完成 `action.yml` java-version:25, `mise.toml` java="25"
- **构件 major**: `gradle.properties` 或构建产出推断 major=69 (JDK 25 对应 class major 69)
- **无 `--release` 降级**: 当前构建不添加 `--release` 标志

## 4. 活跃版本引用残项（执行扫描确认）

### 4.1 publish.yml MC_VERSION
- 文件: `.github/workflows/publish.yml`
- 当前值: 两处 `MC_VERSION: '1.21.1'`
- 目标值: `'26.1.2'`
- 来源: 与目标基线一致

### 4.2 publish-on-release.yml 版本语义
- 文件: `.github/workflows/publish-on-release.yml`
- 待读取确认具体位置: job name / tag condition / version input
- 原则: 仅改版本发布语义字段，不改分支策略/workflow 触发条件

### 4.3 JAR 命名双重版本
- 当前产出: `kenergyengineering-26.1.2-26.1.2-4.1.0.jar`
- 期望: `kenergyengineering-26.1.2-4.1.0.jar`（仅一次 26.1.2）
- 根因: `build.gradle`/`jars.gradle` 中 version 可能同时包含了 mod version 和 mc version

### 4.4 文档与注释
- CONTRIBUTING.md: 7 处旧 Minecraft/NeoForge/Java/Registrate 引用
- `docs/closure_verification.md`: 1 处 MC 1.21.1
- `docs/compile_warning_baseline_26.1.2.md`: 1 处 MC 1.21.1
- `CmBlockEntity.java`: 1 处过期注释

## 5. 排除项（保留历史不变）

- README 兄弟目录
- 旧 `plans/` 目录 (只读)
- `plans/.draft_plan_*.md` (只读)
- `plans/.evidence/` (仅追加，不改已有文件)
- `deps.txt`, logs, run/build 目录

## 6. 参考源码

- mc-publish 源码: https://github.com/Kir-Antipov/mc-publish/blob/v3.3.0/src/program.ts
  - 用于理解 `JAVA`/`MC_VERSION` 参数在 mc-publish action 中的语义
