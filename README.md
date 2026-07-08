# Kenergy Engineering Retechnicalized

多版本容器结构。每个版本为独立 Gradle 项目与独立 Git 仓库，位于各自子目录中。

| 目录 | Minecraft | NeoForge | Java | 状态 |
|------|-----------|----------|------|------|
| `Technical-Engineering-4-1.21.1/` | 1.21.1 | 21.1.219 | 21 | 当前稳定版本 |
| `Technical-Engineering-4-26.1.2/` | 26.1.2 | 26.1.2 | 25 | 初始副本，版本配置已确认 |

构建入口：
```bash
# 1.21.1 版本
cd Technical-Engineering-4-1.21.1
./gradlew build

# 26.1.2 版本
cd Technical-Engineering-4-26.1.2
./gradlew build
```