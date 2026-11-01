# tools/compat — 兼容层转换器与校验

把邻居（Ender IO 系、Mekanism、沉浸工程、ATO、Allthemodium）的数据机械转成
`TENCompatGen` 的表行，并校验成品配方。规则本身写在
[`docs/ARCHITECTURE.md` §3.3](../../docs/ARCHITECTURE.md)，这里只讲怎么跑。

## 准备 dumps

脚本读的是解包后的 jar 目录，不是 jar。`config.json` 的 `dumps` 记录每个命名空间的数据根目录，
路径是本机的，换机器改这里即可。解包方式：

```bash
mkdir -p /d/Temp/eiall && cd /d/Temp/eiall && 7z x -y /path/to/enderio-xxx.jar
```

原版物品标签（`c:` 与 `minecraft:` 之外还要 `minecraft:wool` 这类原版标签）从
`build/moddev/artifacts/neoforge-*-client-extra-aka-minecraft-resources.jar` 里取
`data/minecraft/tags/item/**`，默认解到 `D:/Temp/mcall/data`：

```bash
unzip -o -q build/moddev/artifacts/neoforge-21.1.243-client-extra-aka-minecraft-resources.jar \
  "data/minecraft/tags/item/*" -d /d/Temp/mcall
```

## 日常流程

| 步骤 | 命令 | 说明 |
|---|---|---|
| 1 | `node tools/compat/convert.js` | 邻居数据 → `out/rows.json`，并打印 `ORE_CHAIN` 候选建议 |
| 2 | 手工核对建议 | 已有材料只补候选，别重复加行；新材料才加 `new ChainRow(...)` |
| 3 | `node tools/compat/inject.js` | 只替换 `TENCompatGen` 的 `ALLOYS` / `GRIND` 数组体 |
| 4 | `./gradlew runData -x compileTestJava` | 生成期断言会在这里拦住重复输入签名 |
| 5 | `node tools/compat/check.js` | 全量校验，退出码非 0 即有问题 |

`config.json` 的 `providerPriority` 是**编写期**的候选顺序；游戏内生效顺序由配置项
`compat.providerPriority` 决定（`TENConfig.CompatConfig`），两份要一起改，否则
转换器给出的候选次序与实际归属不一致。

## check.js 查什么

- **输入签名冲突**：同一机器类型里两条可匹配的配方（`findRecipe` 只取第一条）。
  `kenergyengineering:provider_priority` 的互斥变体组按设计豁免——同组内每个模组各一条，
  加载期只留赢家；但同组重复同一模组、同一输入挂两组、变体组撞无条件配方，仍然报。
  与 `RecipeSignature`（生成期断言 + 运行期 `TENRecipeAudit` 扫描）同一套判据。
- **标签输出了非空标签**：标准语义里配方输出必须是具体物品，写成标签会得到空产物。
- **引用能否落地**：`c:` 与其它命名空间的标签要在某个 dump 里有人定义；物品 id 要在该模组的
  `assets/<ns>/models/item` 或语言键里查得到。原版资产与流体不在 dump 里，这类引用只跳过不误报。
