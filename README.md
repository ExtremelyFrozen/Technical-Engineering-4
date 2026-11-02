# Kenergy Engineering Retechnicalized

Minecraft 1.21.1 / NeoForge 科技向模组（Technical Engineering 4 的重制分支），
基于 [LDLib2](https://github.com/Low-Drag-MC/LDLib2) 构建机器框架、GUI 与同步体系。

当前版本：`4.0.2`（见 `gradle.properties`） · 许可证：[AGPL-3.0](LICENSE)

## 内容概览

- **加工机器**：熔炼机、粉碎机、压缩机、精炼机、感应炉、灵能处理器、祛魔机、物质结晶器等，统一配方/批处理/停滞语义
- **环境与作业机器**：冷却器、信标模拟机、方块破坏器、方块成型器、农场管理机、生物啃噬者、采矿场
- **能量系统**：能量单元、能量线缆与物品管道传输（当前版本冻结维护）
- **频道系统**：跨机器共享存储与频道连接
- **升级系统**：批处理 / 效率 / 持续时间等升级维度，四维 B 锁定
- **面配置**：机器各面能量/物品/流体交互模式
- **生态集成**：JEI（含 XEI 兼容层）、Jade；EMI 运行时待验证
- **游戏内指南**：科能工程指南（需 GuideME）

## 多版本容器结构

本仓库父目录为多版本容器，每个版本为独立 Gradle 项目与独立 Git 仓库：

| 目录 | Minecraft | NeoForge | Java | 状态 |
|------|-----------|----------|------|------|
| `Technical-Engineering-4-1.21.1/` | 1.21.1 | 21.1.243 | 21 | 当前稳定版本 |
| `Technical-Engineering-4-26.1.2/` | 26.1.2 | 26.1.2.78 | 25 | 参照源分支（含完整 plans/docs） |

## 构建

```bash
# 1.21.1 版本
cd Technical-Engineering-4-1.21.1
gradlew build          # 构建
gradlew runClient      # 启动客户端测试

# 或使用 mise 任务
mise run build
mise run run-client
```

要求 JDK 21（详见 `CONTRIBUTING.md` 环境准备章节）。

## 文档

文档入口是 [AGENTS.md](AGENTS.md)：先看文档地图，再按任务读对应手册。

| 文档 | 内容 |
|------|------|
| [AGENTS.md](AGENTS.md) | 文档地图与动手前必读的硬约定 |
| [CONTRIBUTING.md](CONTRIBUTING.md) | 项目结构、技术栈、注册与编码约定、贡献流程 |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | 源集、包职责、机器框架、同步、注册与数据生成 |
| [docs/CHANGELOG_STYLE.md](docs/CHANGELOG_STYLE.md) | 更新日志的两份文件分工、六小节与发版搬运 |
| [docs/RELEASE.md](docs/RELEASE.md) | 发版流程、CI 链路、版本号与已知坑 |
| [docs/TODO.md](docs/TODO.md) | 未完成事项与待实机验证项 |
| [CHANGELOG_UNRELEASED.md](CHANGELOG_UNRELEASED.md) | 待发改动草稿 |
| [CHANGELOG.md](CHANGELOG.md) | 已发布版本留档 |
| [CODEOWNERS](CODEOWNERS) | 代码所有者与审批规则 |

## 许可证

本项目以 [AGPL-3.0](LICENSE) 许可证发布。
