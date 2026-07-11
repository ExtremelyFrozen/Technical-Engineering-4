# RunClient Development Environment

## Overview

RunClient 包含 5 个开发模组，通过 Gradle 标准 `clientLocalRuntime` 配置声明，
**不会**进入 server、gameTestServer 或发布 jar。

## Local Launch Entry

项目根目录的 `run_client.bat`（本地 gitignored）是启动开发客户端的快捷入口：

```batch
run_client.bat
```

此 bat **不管理 mod 配置**——它只调用 `gradlew.bat runClient --configuration-cache --console=plain`。
如需临时排除某个开发 mod，手动注释 `dependencies.gradle` 中对应行，
或使用独立分支/本地未提交修改，不由 bat 管理。

## Validation

修改开发 mod 依赖后验证配置正确性：

```batch
python scripts\validate_runclient_mods.py
```

该脚本检查：版本目录完整性、clientLocalRuntime 无条件声明、仓库隔离、无泄漏到 jar。

## Development-Only Mods

| Mod | 用途 | 26.1.2 坐标 |
|-----|------|-------------|
| JEI (runtime) | 物品/配方查看器 | `mezz.jei:jei-26.1.2-neoforge:29.13.0.42` |
| Jade | 方块/实体调试悬浮窗 | `maven.modrinth:jade:26.1.8+neoforge` |
| ModernFix | 启动/加载优化 | `maven.modrinth:modernfix:5.27.18+mc26.1.2` |
| FerriteCore | 内存占用减少 | `maven.modrinth:ferrite-core:9.0.0-neoforge` |
| ModernUI | UI 库（主菜单/界面） | `maven.modrinth:3sjzyvGR:pDpDBt4H` (Modrinth universal fat JAR) |

JEI `compileOnly` 始终启用（API 编译依赖），`clientLocalRuntime` 提供完整 JEI mod。
ModernFix 和 FerriteCore 是性能优化 mod，排障时可能需要临时排除。

### Not Included

| Mod | 原因 |
|-----|------|
| EMI | 官方无 26.1.2 NeoForge 构件。JEI 作为唯一 viewer。 |
| ImmediatelyFast | 主要为 FPS 优化，可能掩盖渲染 bug。本环境聚焦启动/加载/内存优化。 |

### ModernUI: Modrinth Universal Fat JAR 的原因

ModernUI 官方 Maven 仓库提供分构件发布（`icyllis.modernui:ModernUI-NeoForge:26.1.2-3.13.0.5`），
但该构件仅包含 227 个 MC 集成 class，缺少 core、Arc3D、Markflow 等核心模块，
且 POM 不含 dependencies 声明，导致 FML 在 class 加载阶段早期退出（`early termination`），
无法启动开发客户端。

通过二分定位确认此问题后，选择 Modrinth 发布的 universal fat JAR：
- project ID `3sjzyvGR`，version ID `pDpDBt4H`
- 包含完整 ModernUI 核心（core、Arc3D、Markflow、fragment）
- 通过 `clientLocalRuntime` 直接接入，无需额外 `implementation` 或 `additionalRuntimeClasspath`
- 经实测与 JEI + Jade + ModernFix + FerriteCore 四模组共同启动成功

若将来 ModernUI 官方 Maven 修复分构件依赖声明，可考虑切回。

## Important Notes

- 所有 5 个开发 mod 必须使用 `clientLocalRuntime`，不得使用 `localRuntime`/`runtimeOnly`/`implementation`/`jarJar`/`force`/`strictly`。
- 这些 mod 不会进入 `publish` jar（`clientLocalRuntime` 配置默认 exclude）。
- 不得在 `gradle.properties` 或 `dependencies.gradle` 添加自定义开关控制这些 mod。

## Performance Notes

- **Gradle Configuration Cache**：bat 调用 `--configuration-cache` 参数，该参数作用于 Gradle**配置阶段**（configuration phase），缓存 task graph 构建结果以降低后续重复运行的配置耗时。它**不**影响 Minecraft 进程自身的启动时间。
- **实测启动时间**：当前五模组配置下，从 Gradle 执行 `runClient` 到主菜单就绪约 **9 分 39 秒**（Minecraft 进程启动至主菜单的 wall-clock 时间）。该数据与旧基线（旧分支/旧依赖组合）相比**没有显著改善**，不应理解为性能优化成果。
- **模组配置目标**：五模组组合的首要目标是提供完整的**开发功能与兼容性验证**环境（JEI/Jade 调试支持、ModernUI 测试渲染器路径、ModernFix/FerriteCore 验证加载兼容性），而非启动加速。
- **关于性能 mod**：ModernFix 与 FerriteCore 虽被归类为性能优化模组，但在当前五模组组合中，没有隔离测量数据证明它们独立贡献了启动加速。如需评估其效果，应在隔离环境中分别对比有无单个模组的启动时间。
