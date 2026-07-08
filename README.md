# Kenergy Engineering Retechnicalized

多版本单仓库结构。每个版本为独立 Gradle 项目，位于各自子目录中。

| 目录 | Minecraft | NeoForge | Java | 状态 |
|------|-----------|----------|------|------|
| `1.21/` | 1.21.1 | 21.1.219 | 21 | 当前稳定版本 |
| `26.1.2/` | 1.21.5 (待确认) | 26.1.2 | 25 | 初始副本，版本配置待确认 |

构建入口：
```bash
# 1.21 版本
cd 1.21
./gradlew build

# 26.1.2 版本
cd 26.1.2
./gradlew build
```