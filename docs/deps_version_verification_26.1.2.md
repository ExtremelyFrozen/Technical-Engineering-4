# 依赖版本交叉验证报告 — 26.1.2

## 元数据

- 创建日期: 2026-07-11
- 项目 HEAD: `492ff5d` — `fix: align publish workflows with Minecraft 26.1.2 artifacts`
- 关联 TASK: `TASK-011` — libs.versions.toml TODO 清理
- 目标基线: NeoForge 26.1.2.78 / Minecraft 26.1.2

---

## 1. NeoForge Loader 版本

### 目标版本

`gradle/libs.versions.toml` 中 `loader = "3"` → 生成 metadata `loaderVersion = "[3,)"`

### 官方证据

| 来源 | loaderVersion 值 | 说明 |
|------|------------------|------|
| NeoForge tag `26.1.2` 官方模板 | `[3,]` | `src/main/templates/neoforge.mods.toml` 在 NeoForge 26.1.2 发布标签中声明 |
| FML 实际版本 | `11.0.x` | FML (Forge Mod Loader) 在 26.1.2 线为 11.x 系列，与 `[3,)` 语义兼容 |
| 项目生成 metadata（修改后） | `[3,)` | 参见下方验证命令输出 |

### loader=3 有效 vs 官方推荐差异说明

- **官方 `[3,]`** 与 **项目 `[3,)`** 均表达"最低版本 3，无上限"的语义。TOML 版本范围格式中：
  - `[3,]` 表示 inclusive lower bound 3, unbounded upper（NeoForge 官方模板风格）
  - `[3,)` 表示 inclusive lower bound 3, unbounded upper（项目 template `[${loader_version},)` 生成风格）
  - 两者在运行时效果完全等价，项目使用 `[${loader_version},)` 模板语法生成 `[3,)`，与官方要求一致。
- **之前 `loader = "4"` 不错误但过于严苛** — 它生成 `[4,)` 要求 loader ≥ 4，而 NeoForge 26.1.2 最低为 3。使用 4 可能不必要地排除 FML 11.x 范围内的有效版本。
- **修改结论**: `loader = "3"` 是 26.1.2 线的正确最低值，FML 11.x 完全覆盖。

### 验证命令与输出

```
# RED phase: loader = "4"
.\gradlew.bat :generateModMetadata
→ build/generated/sources/modMetadata/META-INF/neoforge.mods.toml
  → loaderVersion = "[4,)"

# GREEN phase: loader = "3"
.\gradlew.bat :generateModMetadata
→ build/generated/sources/modMetadata/META-INF/neoforge.mods.toml
  → loaderVersion = "[3,)"
```

### 风险与回退

- **风险**: 极低。`[3,)` 比 `[4,)` 更宽松，不会导致加载失败。
- **回退**: 将 `loader` 改回 `"4"` 并恢复 TODO 注释。

---

## 2. ModDevGradle 版本

### 目标版本

`gradle/libs.versions.toml` 中 `modDevGradle = "2.0.141"`（不变）

### 官方证据

| 来源 | 版本 | 说明 |
|------|------|------|
| Plugin Portal (Gradle) | `2.0.141` | 截至 2026-03-22 的最新发布版本 |
| NeoForge 26.1.2 官方 MDK | `2.0.141` | 官方 MDK 使用的版本 |
| NeoForge 26.1 博客推荐 | `2.0.141` | NeoForge 26.1 发布博文中推荐使用的 MDG 版本 |
| GitHub Releases | `2.0.141` | https://github.com/neoforged/moddevgradle/releases |

### 验证命令与输出

```
# 确认 Gradle 解析的插件版本
.\gradlew.bat :buildEnvironment
→ net.neoforged.moddev:net.neoforged.moddev.gradle.plugin:2.0.141
→ net.neoforged:moddev-gradle:2.0.141

# 确认运行时无版本警告
.\gradlew.bat :tasks
→ 无 modDevGradle 相关 WARN/ERROR 输出
```

### 风险与回退

- **风险**: 无 — 版本保持不变，仅清理了 TODO 注释。
- **回退**: 恢复 TODO 注释。

---

## 3. 综合结论

| 依赖 | 旧值 | 新值 | 证据 |
|------|------|------|------|
| loader | `4` (→ `[4,)`) | `3` (→ `[3,)`) | NeoForge 26.1.2 tag 官方模板: `[3,]` |
| modDevGradle | `2.0.141` | `2.0.141` (不变) | Plugin Portal latest; MDK 锁定; 官方推荐 |

两处 TODO 已替换为简短证据注释，详细证据链在本文档固化。

---

## 4. 变更文件清单

| 文件 | 变更类型 |
|------|----------|
| `gradle/libs.versions.toml` | loader 4→3 + TODO 替换为证据注释 |
| `docs/deps_version_verification_26.1.2.md` | 新增 — 本文件 |
