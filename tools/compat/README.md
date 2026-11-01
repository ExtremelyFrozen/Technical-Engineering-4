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

`dumps` 里还可以放只提供标签、不提供机器配方的模组（如 `ae2` 的 `c:dusts/certus_quartz`、
`c:dusts/fluix`）——它们不参与 `providerPriority` 的归属顺序，只让 `check.js` 能证明
「这个输入标签确实有人定义」，否则会误报成无人定义。

> Info-ZIP 的 `unzip "dir/*"` 不跨目录层级：`unzip -o -q x.jar "data/*" -d out` 只会解出
> `data/` 下一层的文件，邻居按材料分层的配方目录会被解成空壳。要么整包解压，要么显式写到底层目录。

## 耗时尺规

`config.json` 的 `timeRule` 是导入行的时长来源，**不再**用邻居能量换算
（IE 一次冲压机 51200 FE ÷ 15 FE/t = 3413 tick，会被旧钳位压成 1200，与本模组原生差一个数量级）：

| 机器 | 尺规 | 原生依据 |
|---|---|---|
| 感应炉 | 50 tick × 输入物料份数（线性，不夹上限） | 手写行 2 份 100、3 份 150、5 份 250 |
| 粉碎机 | 一次操作 120；≥9 份输入（粗矿块）200 | 原生 80–200，1→8 出那一档 200 |
| 压缩机 | 恒 100，不看份数 | 原生板/齿轮/杆/线全部 100 |

`TENRecipeGen#furnaceTime` 用同一条感应炉尺规，原生 `recipe/induction_furnace/*.json` 也已按它重算。

## 日常流程

| 步骤 | 命令 | 说明 |
|---|---|---|
| 1 | `./gradlew runData -x compileTestJava` | 先让生成物落到当前状态，转换器的「本模组已有配方」基准读它 |
| 2 | `node tools/compat/convert.js` | 邻居数据 → `out/rows.json`，并打印 `ORE_CHAIN` 候选建议 |
| 3 | 手工核对建议 | 已有材料只补候选，别重复加行；新材料才加 `new ChainRow(...)` |
| 4 | `node tools/compat/inject.js` | 只替换 `TENCompatGen` 的 `ALLOYS` / `GRIND` / `PRESS` 数组体 |
| 5 | `./gradlew runData -x compileTestJava` | 生成期断言会在这里拦住重复输入签名与同名覆盖 |
| 6 | `node tools/compat/check.js` | 全量校验，退出码非 0 即有问题 |

第 1 步与第 5 步之间的顺序有意义：我们自己的通用段（锭→粉、粗矿块→粉）与邻居写的同输入兼容配方
是**互斥**的，转换器靠读上一轮生成物判断「这个输入本模组是否已有配方」。
本模组改了通用配方后要先跑一次 DataGen，再重跑转换器，否则会把已经被我们接管的那批导入进来，
第 5 步的断言会直接报错（这是预期的报错，不是转换器坏了）。

## 导入规则

- 行名 = 相对该机器配方目录的路径（`aluminum/ingot.json` → `aluminum_ingot`）。只取文件名会让
  ATO 那种按材料分层目录里的几十个 `ingot.json` 写进同一个输出文件、互相覆盖。
- 对方配方自带的第三方 `neoforge:mod_loaded` 门控要带过来（EVO 的 industrialforegoing 合金行、
  ATO 写给沉浸工程/通用机械/EIO 的兼容行都是这类）。
- 输入按标签、输出按优先级解析成具体物品；解析不到就不导入。
- 输入签名与本模组已有配方相同的条目不导入。
- **IE 的 `alloysmelter` 不在导入清单里**：青铜/黄铜/因瓦/电子/康铜由 `TENRecipeGen.ALLOY_CHAIN`
  按同一比率接管（比率照抄 IE 的原子守恒写法），邻居那份的输出是标签，我们要么写死一家、
  要么写互斥变体，后者已由通用合金链覆盖。
- `config.json` 的 `providerPriority` 是**编写期**的候选顺序；游戏内生效顺序由配置项
  `compat.providerPriority` 决定（`TENConfig.CompatConfig`），两份要一起改。

## check.js 查什么

- **输入签名冲突**：同一机器类型里两条可匹配的配方（`findRecipe` 只取第一条）。
  `kenergyengineering:provider_priority` 的互斥变体组按设计豁免——同组内每个模组各一条，
  加载期只留赢家；但同组重复同一模组、同一输入挂两组、变体组撞无条件配方，仍然报。
  与 `RecipeSignature`（生成期断言 + 运行期 `TENRecipeAudit` 扫描）同一套判据。
- **标签输出了非空标签**：标准语义里配方输出必须是具体物品，写成标签会得到空产物。
- **引用能否落地**：`c:` 与其它命名空间的标签要在某个 dump 里有人定义；物品 id 要在该模组的
  `assets/<ns>/models/item` 或语言键里查得到。原版资产与流体不在 dump 里，这类引用只跳过不误报。
