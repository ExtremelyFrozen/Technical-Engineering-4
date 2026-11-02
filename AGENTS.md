# AGENTS.md

本仓库的文档地图与动手前必读的硬约定。给进入仓库的 AI 代理，也给人看。
先读本页再动手；这里只做索引，细节都在被指向的文档里。

## 这是什么

Kenergy Engineering Retechnicalized（KER），Minecraft 1.21.1 / NeoForge 科技模组，TE4 重制分支。
核心是 LDLib2 机器框架、Registrate 注册与数据生成，内容覆盖加工机器、环境机器、能量与物品传输、
频道共享存储、升级与面配置。

**基线：Minecraft 1.21.1 · NeoForge 21.1.243 · Java 21 · LDLib2 2.2.44。**
权威来源是 `gradle/libs.versions.toml`、`gradle/forge.versions.toml` 与 `gradle.properties`，这一行只是速览。

## 一、规则类文档（进 git，动手前必读）

| 文档 | 什么时候读 |
|---|---|
| `docs/ARCHITECTURE.md` | 改任何代码前：源集、包职责、机器 BE 继承、同步与注册约定 |
| `docs/CHANGELOG_STYLE.md` | 改任一份更新日志前：两份文件分工、六小节、条目长度与发版搬运 |
| `docs/RELEASE.md` | 建 GitHub Release 前：版本号、CI 发布链、更新日志与已知坑 |
| `docs/TODO.md` | 想知道「还剩什么事没做」时：只放未完成事项 |
| `CHANGELOG_UNRELEASED.md` | **待发改动写这里**：未发布的累计调整，发版时整段并入 `CHANGELOG.md` |
| `CHANGELOG.md` | 已发布内容的留档；**改格式前先读 `docs/CHANGELOG_STYLE.md`** |
| `CONTRIBUTING.md` | 注册、方块继承、VoxelShape、DataGen、本地化与审查清单 |
| `README.md` | 项目自述、内容概览与构建入口 |

## 二、动手前必须知道的硬约定

- **语言**：注释、`docs/` 与两份更新日志一律中文；代码标识符与游戏内英文文案除外。
- **界面键要齐**：新增配置项必须在 `TENLangHandler.addConfig` 同时补 label 与 tooltip；
  按键键名用 `TEN.KEY_*` 配 `addRaw`（走 `add()` 会多一层 `kenergyengineering.` 前缀）。
  `runData` 末尾的 `verifyTranslationKeys()` 会遍历两个 spec 并列出缺失键。
- **生成资源不手改**：`src/generated/resources/` 由 DataGen 产出。模型、blockstate、语言、配方要改生成器。
- **客户端边界**：客户端类放 `src/client/java`；`src/main` 里的 `client` 子包必须带 `Dist.CLIENT` 防护。
  可选集成的类型名不得漏进总是加载的类，缺邻居时整包要能正常加载。
- **同步在主线程**：`CmBlockEntity` 已关闭 LDLib2 异步 sync 线程，改为主线程 tick 末尾 `passivelySync()`。
  不要恢复异步线程，否则进度条会被中间态覆盖成 0/0。
- **面 capability 的观察读**：`getStackInSlot` / `getFluidInTank` 这类读必须反映机器真实内容，不能被抽取权限藏空；
  写入与抽取再按面权限单独门控。AE2 样板供应器的阻挡模式依赖这一点。
- **批处理**：四维 B 锁定（物品/流体/输出/能量）与 `lockedMaxProgress` 是停滞语义的核心；
  配方产出走快照-模拟-提交，失败必须回滚。
- **依赖版本**：新增依赖优先走 `gradle/libs.versions.toml` 与 `gradle/forge.versions.toml`；
  现有硬编码坐标（Lombok、Curios、开发运行期模组、Kotlin 插件）暂按现状，迁移时再收编。
- **验证**：代码改动至少跑 `./gradlew build` 与 `./gradlew spotlessCheck`；GameTest 用 `./gradlew runGameTestServer`。
  界面、时序与实机交互仍要 `runClient` 手验。

## 三、常用命令

```bash
./gradlew build              # 全量构建
./gradlew compileJava        # 只编 main
./gradlew runClient          # 实机验证
./gradlew runGameTestServer  # GameTest
./gradlew runData            # 数据生成
./gradlew spotlessApply      # 格式化
```

## 四、读上游与依赖源码之前

- 先确认版本：`gradle/libs.versions.toml` 与 `gradle/forge.versions.toml` 是唯一事实源。
- 读 AE2、LDLib2、Registrate 的 API 时，以当前声明版本对应的源码或字节码为准；
  不要拿 Gradle 缓存里其他 Minecraft 版本的 sources jar 当结论。
- 可选集成按「mod id 门禁 + 特性各自守卫」处理。门禁类与带类型的桥分开写，别让编译期可选类型进入总是加载的类。
