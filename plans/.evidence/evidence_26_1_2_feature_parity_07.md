# Evidence: EMI 官方无 26.1.2 版本 — 外部阻塞

> 用途：FP-007 EMI 缺口的权威阻塞证据
> 检索日期：2026-07-12
> 检索人：猫娘检索员-诺雅

---

## 阻塞声明

**EMI**（Emi's Inventory）官方对 NeoForge 26.1.2（MC 1.21.1）**无兼容发布版本**。当前最新 EMI 版本对应 MC 1.21.3/1.21.4（NeoForge 21.3+）。不采用任何非官方 fork。

---

## 证据来源

| 来源 | URL | 状态 |
|------|-----|------|
| EMI Modrinth 项目页 | https://modrinth.com/mod/emi | 最新版本不支持 1.21.1/26.1.2 |
| EMI GitHub Releases | https://github.com/emilyploszaj/emi/releases | 无 26.1.2 兼容标记 |
| EMI Maven 元数据 | https://maven.terraformersmc.com/releases/dev/emi/emi/ | 无 neo-1.21.1 后缀构件 |

---

## 当前项目状态

- `dependencies.gradle` 中 EMI 依赖被注释：`// Phase 4/5: compileOnly(forge.emi)`
- `TENEmiPlugin.java` 和 `TENEmiRecipe.java` 为空存根
- JEI 已启用：`compileOnly(forge.bundles.jei)` 可用
- 保持空存根不删除（保留未来恢复锚点），添加 BLOCKED 注释

---

## 未来恢复锚点

当 EMI 发布兼容 NeoForge 26.1.2 的版本时：
1. 取消 `dependencies.gradle` 中 EMI 注释
2. 将 EMI 版本提升到对应兼容版
3. 用 JEI 接口证据恢复 `TENEmiPlugin`/`TENEmiRecipe` 实现
4. 删除 BLOCKED 注释
