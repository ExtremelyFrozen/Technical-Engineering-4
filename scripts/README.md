# scripts/ — TE4 校验与辅助脚本

Kenergy Engineering（TE4, NeoForge 26.1.2）的静态校验脚本集，用于在提交前
验证模组注册表、资源数据与重构契约，供 CI 与协作者直接运行。

## 运行方式

所有 Python 脚本在**仓库根目录**执行：

```bash
python scripts/<script_name>.py
```

退出码约定：

- `0` = GREEN（全部检查通过）
- `1` = RED（存在失败项）

## 脚本清单

### 数据 / 资源校验

| 脚本 | 用途 |
| --- | --- |
| `data_intersection_check.py` | Main/Generated 数据目录交集检查，避免双资源根重复定义 |
| `datapack_registration_matrix_check.py` | datapack 注册矩阵检查，对照历史基线校验 datapack 内容 |
| `generated_orphan_check.py` | `src/generated/resources/` 全树孤立/陈旧生成资源检查（基于 HashCache） |
| `mould_tag_check.py` | 模具 tag 检查：generated `moulds.json` 为 `replace=false` 且含 9 个模具 ID，main 侧无 moulds，tag provider 含 moulds |

### 注册与契约校验

| 脚本 | 用途 |
| --- | --- |
| `fluid_registration_chain_check.py` | 流体注册链 RED/GREEN TDD 校验 |
| `fluid_gui_capability_check.py` | 流体能力重构静态断言：GUI 用 ResourceHandler bind、BE 缓存适配器、CommonProxy 委托 |
| `recipe_matching_policy_check.py` | FormsCombinedRecipe 配方匹配策略 RED/GREEN 门禁 |
| `validate_item_definitions.py` | 物品定义完整性校验（所有已注册 kenergyengineering 物品） |
| `validate_custom_block_tooltips.py` | 自定义方块物品 Tooltip 校验：21 个目标物品注册、排除项、lang 键连续性 |
| `validate_brand_localization.py` | 品牌本地化与 Psionicant Gate 校验（en_us/zh_cn，Java/属性/语言文件） |
| `validate_ldlib_tooltip_fix.py` | LDLib2 `26.1.2.28` Tooltip 修复回归检查（版本号 + 字节码验证） |

### UI 行为校验

| 脚本 | 用途 |
| --- | --- |
| `player_inventory_slot_offset_check.py` | 玩家背包槽位偏移 TDD 校验 |
| `reveal_progress_bar_check.py` | RevealProgressBar TDD 校验 |

### 共享库与测试

| 脚本 | 用途 |
| --- | --- |
| `resource_roots.py` | main + generated 双资源根解析工具（冲突检测），被多个校验脚本依赖 |
| `test_resource_roots.py` | `resource_roots.py` 的单元测试（`python -m unittest scripts.test_resource_roots` 或直接运行） |

### 辅助

| 脚本 | 用途 |
| --- | --- |
| `gradle-utf8.ps1` | PowerShell 5.1 下强制 UTF-8 的 gradle 包装脚本（保存/恢复控制台编码，委托 `gradlew.bat`） |

## 约定

- 脚本须无本地绝对路径 / 任务编号残留，可跨机器直接运行。
- 新校验脚本请遵循：`exit 0` = 通过，`exit 1` = 失败；stdout 使用 UTF-8。
