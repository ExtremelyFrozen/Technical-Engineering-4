# 兼容层标签化可行性审计

对 `TENCompatGen` 的 229 条兼容配方做静态审计：哪些仍以具体物品作输入、能否改成 `c:` 标签、改后是否引入歧义。
判据与 `RecipeSignature#claim`、`FormsCombinedRecipe#matches` 对齐，不靠人工印象。

**数据源**：`src/generated/resources/data/kenergyengineering/recipe/compat/**`（229 条）、
`src/main/resources/data/kenergyengineering/recipe/**` 手写原生配方，合计 553 条机器配方；
标签表 1118 个，来自本机 `tools/compat/config.json` 登记的 11 份邻居 dump 加原版/NeoForge。

---

## 0. 结论

1. **28 个输入项（20 条配方）可以无歧义改成标签**，全部属于「单成员等价标签」——标签当前的成员集合恰好就是那一个物品，
   替换后匹配集合完全不变，只是把硬编码换成扩展点。清单见 §2。
2. **16 个输入项不能直接改**：它们要用的标签只由另一个未门控的邻居定义，改了等于把配方绑死到那个邻居（§3）。
   补一份本模组的可选成员定义即可解锁。
3. **8 类改法有歧义，禁止**（§4）。它们要么与现有配方撞同一输入签名（生成期直接失败），
   要么把输入扩张到语义不同的一组物品。
4. 审计顺带查出**19 对既有的运行期重叠**（§5）：同一输入有两条粉碎机配方同时匹配，`findRecipe` 任取一条。
   这些不是改标签造成的，是手写原生配方与标签化兼容行互相覆盖；现有 `claim`/`TENRecipeAudit` 只看「同签名」，
   查不出这种 tag↔static 交叉，所以一直没人发现。
5. 手改 `TENCompatGen` 表行不持久：`inject.js` 会用转换器结果整体替换 `ALLOYS`/`GRIND`/`PRESS` 数组体。
   规则要落在 `convert.js`（§6）。

---

## 0.1 实施记录（2026-11-02）

按逐条选择落地，复验数据：机器配方 556 条，`tools/compat/check.js` 签名冲突 0、引用问题 0、
`overlap_audit`（按条件互斥过滤后）重叠 0 对，`runData` 通过。

| 项 | 落地方式 |
|---|---|
| 煤、木炭、焦炭（§2 第 1-3 组） | `TENCompatGen.GRIND_SPLIT` 九条，条件成对互斥：EIO 优先、Mek 兜底、IE 走黑染料与焦炭粉 |
| 砂岩、红砂岩（§2 第 6-7 组） | 同上四条；EIO 版红砂岩改出红沙；EIO 与 IE 二选一（IE 在时用带硝石的那版） |
| 黏土块、石英块（4A/5A） | 保留 EIO 版，IE 同名行进 `droppedRows` |
| 圆石、苔石圆石、深板岩圆石（8-10 组） | EIO 三条保留在 `GRIND`，原生兜底三条（`gravel`/`mossy_cobblestone`/`cobbled_deepslate`）带 `not(mod_loaded(enderio))` |
| 深板岩、石头（11A/12A） | 转正为 `deepslate.json`、`stone.json`；`c:stones` 拆成石头/安山岩/闪长岩/花岗岩/凝灰岩五条 |
| 烈焰棒、骨头（13A/14A） | 转正为 `blaze.json`（4 粉）、`bone_meal.json`（6+10%×2） |
| 下界金矿（15B） | IE 行进 `droppedRows`，由矿石三段式承担 |

配套改动：`RecipeSignature#claim` 与 `check.js` 放行「条件互斥的同签名配方」；
兜底侧否定的是 `and(mod_loaded(X), compat_enabled(X))` 而不是单独的 `mod_loaded(X)`，
这样玩家关掉某个来源的兼容配方时兜底分支仍能接上；
`convert.js` 只按素材真实归属写门控（标签输入不再按首个成员的模组门控）；
`config.json` 新增 `droppedRows`；素材全原版的兼容行转正为原生配方（紫水晶、粗土、下界疣、海晶三条、骨粉染白），
不再随 `compat_enabled` 消失。

---

## 1. 判据

一项「物品 → 标签」的替换算**无歧义**，必须同时满足：

| # | 条件 | 不满足的后果 |
|---|---|---|
| A | 替换后不与任何机器配方同签名 | 生成期 `RecipeSignature#claim` 抛错，DataGen 失败 |
| B | 替换后不与同机器配方形成成员集合交叉（项数相同、可完美配对） | 运行期两条都能匹配，`findRecipe` 取第一条，产物不确定 |
| C | 标签定义者 ∈ 该行的 `mod_loaded` 门控 ∪ {原版, NeoForge, 本模组} | 标签为空，`machine()` 自动加的 `not(tag_empty)` 会让整条配方消失 |
| D | 标签语义是该物品的类别，不是上位类目 | 输入扩张到无关物品（如 `c:ingots`、`c:foods`） |

单成员等价标签天然满足 D：成员集合不变，语义不可能变。

---

## 2. 可直接改：28 个输入项 / 20 条配方

| 物品 | 改为 | 项数 | 涉及配方 |
|---|---|---:|---|
| `minecraft:egg` | `c:eggs` | 4 | `enderio/induction_furnace/black_dye`、`brown_dye_twigs`、`green_dye_clippings`、`red_dye`(×6) |
| `minecraft:beetroot` | `c:crops/beetroot` | 1 | `red_dye` |
| `minecraft:bone_block` | `c:storage_blocks/bone_meal` | 1 | `enderio/pulverizer/bone_block` |
| `minecraft:cactus` | `c:crops/cactus` | 1 | `enderio/pulverizer/cactus` |
| `minecraft:prismarine_shard` | `c:dusts/prismarine` | 1 | `enderio/pulverizer/prismarine_shard` |
| `minecraft:sugar_cane` | `c:crops/sugar_cane` | 1 | `enderio/pulverizer/sugar_canes` |
| `minecraft:copper_ingot` | `matIngot("copper")`（`c:mats/copper`） | 1 | `enderio_evolution/induction_furnace/construction_alloy_ingot` |
| `minecraft:iron_ingot` | `matIngot("iron")`（`c:mats/iron`） | 2 | `crude_steel_ingot`、`energetic_silver_ingot_from_iron` |
| `minecraft:slime_ball` ×2 | `c:slimeballs` | 1 | `crystalline_pink_slime_ingot_synthetic` |
| `minecraft:redstone` | `c:dusts/redstone` | 2 | `energetic_silver_ingot_from_iron`、`_from_silver` |
| `minecraft:glowstone_dust` | `c:dusts/glowstone` | 2 | 同上两条 |
| `minecraft:nether_star` | `c:nether_stars` | 1 | `stellar_alloy_ingot` |
| `minecraft:ender_pearl` | `c:ender_pearls` | 1 | `vivid_alloy_ingot` |
| `minecraft:charcoal` | `c:charcoal` | 1 | `immersiveengineering/pulverizer/black_dye` |
| `minecraft:coal_block` | `c:storage_blocks/coal` | 1 | `immersiveengineering/pulverizer/coal_block` |
| `enderio_evolution:crude_steel_ingot` | `c:ingots/crude_steel` | 1 | `construction_alloy_ingot` |
| `enderio_evolution:vivid_alloy_ingot` | `c:ingots/vivid_alloy` | 1 | `crystalline_alloy_ingot` |
| `enderio_evolution:crystalline_alloy_ingot` | `c:ingots/crystalline_alloy` | 2 | `crystalline_pink_slime_ingot`、`_synthetic` |
| `enderio_evolution:crystalline_pink_slime_ingot` | `c:ingots/crystalline_pink_slime` | 1 | `melodic_alloy_ingot` |
| `enderio_evolution:melodic_alloy_ingot` | `c:ingots/melodic_alloy` | 1 | `stellar_alloy_ingot` |
| `enderio_evolution:energetic_silver_ingot` | `c:ingots/energetic_silver` | 1 | `vivid_alloy_ingot` |

两条实现约定：

- 感应炉的锭输入统一走 `matIngot("iron")` / `matIngot("copper")`，不要写 `c:ingots/iron`。
  这两种材料本模组有粉形态、`c:mats/X` 已由 `TENMaterialTagGen` 生成，用 `c:ingots/X` 会与表内其他
  合金行的「锭或粉皆可入」拼写不一致（`convert.js#effectiveKey` 就是为了抹平这个差异才存在）。
- `minecraft:slime_ball` 用 `c:slimeballs`，不要用 `c:slime_balls`。两个别名都真实存在，
  `ALLOYS` 表已经统一用前者。

---

## 3. 不能直接改：16 个输入项（标签归属不在门控内）

| 物品 | 想用的标签 | 标签定义者 | 行门控 | 项数 |
|---|---|---|---|---:|
| `minecraft:glowstone` | `c:storage_blocks/glowstone` | `immersiveengineering` | `enderio` | 5 |
| `minecraft:clay_ball` | `c:clay` | `immersiveengineering` | `enderio` / `enderio_evolution` | 3 |
| `minecraft:amethyst_block` | `c:storage_blocks/amethyst` | `enderio` | `immersiveengineering` | 1 |
| `enderio:grains_of_infinity` | `c:dusts/grains_of_infinity` | `enderio` | `enderio_evolution` | 1 |
| `enderio:pulsating_powder` | `c:dusts/grains_of_pizeallity` | `enderio` | `enderio_evolution` | 2 |
| `enderio:pulsating_alloy_ingot` | `c:ingots/pulsating_alloy` | `enderio` | `enderio_evolution` | 2 |
| `enderio:vibrant_alloy_ingot` | `c:ingots/vibrant_alloy` | `enderio` | `enderio_evolution` | 1 |
| `enderio:end_steel_ingot` | `c:ingots/end_steel` | `enderio` | `enderio_evolution` | 1 |

三类处理方式，按代价从低到高：

1. **EVO 那 7 项**：`enderio_evolution` 硬依赖 `enderio`，EIO 不在时 EVO 根本加载不了，
   所以运行期标签必然存在。可以直接改，但要接受「条件里没写、靠模组依赖保证」这个隐式前提，
   或者在行门控里补 `enderio`，让条件自解释。
2. **`c:clay` / `c:storage_blocks/glowstone` / `c:storage_blocks/amethyst`**：这三个标签只有一家邻居定义，
   而使用者是另一家。解法是在本模组做一份可选成员定义（与 `TENMaterialTagGen` 同一路数，
   `{"id": "…#c:…", "required": false}` 或直接 `{"id": "minecraft:clay_ball", "required": false}`），
   让标签在任意组合下都非空。注意 `c:storage_blocks/glowstone` 在 NeoForge 1.21.1 的原版标签集里**并不存在**，
   只有沉浸工程定义它——这一点与直觉相反，别拿常规标签表推断。
3. **压缩机 `grains_to_dust_block`**：`INFINITY` 表的 `press()` 只接受 `String inputId`，没有标签分支。
   要改必须先让 `press()` 接受 `Input`（与 `pressed()` 一致），属于工厂方法改动，不是表行改动。

---

## 4. 禁止改：8 类（有歧义）

| 物品 | 候选标签 | 撞上的配方 | 原因 |
|---|---|---|---|
| `minecraft:clay`（EIO 行） | `c:storage_blocks/clay` | `immersiveengineering/pulverizer/clay` | 同签名，生成期直接失败；两条本就重复（2+硅 vs 4） |
| `minecraft:quartz_block`（IE 行） | `c:storage_blocks/quartz` | `enderio/pulverizer/quartz_block` | 同上（2+2×25% vs 4） |
| `minecraft:coal`（IE 行） | `minecraft:coals` | `enderio/pulverizer/coal` | 同签名失败；EIO 那句已吃 `minecraft:coals` |
| `minecraft:stone`（IE 行） | `c:stones` | `pulverizer/cobblestone`（输入 `c:stones`） | 同签名失败；且 `c:stones` 含 7 种石，语义扩张 |
| `minecraft:deepslate`（EIO 行） | `c:stones` | 同上 | 同签名失败；且产出应是深板岩圆石，不是圆石 |
| `minecraft:nether_gold_ore`（IE 行） | `c:ores/gold` | `pulverizer/chain/gold_ore_raw` | 同签名失败；矿石链首段已接管 `c:ores/gold` |
| 11 种原版花（EIO 行） | `minecraft:small_flowers` | 彼此 | 11 条撞成一个签名，全部失败；各花染料颜色不同，本来就不能共标签 |
| `minecraft:beetroot` | `c:crops` / `c:foods` | `sugar_canes` / `cactus` 等 | 非单成员上位类，输入会扩张到无关作物 |

另外两条**改造只会扩大冲突**、不要动：

- `minecraft:sandstone` / `red_sandstone`（IE 行与手写拆分行的静态输入）：EIO 行用的是父标签
  `c:sandstone/blocks`（含红砂岩与 4 种切制变体），改成子标签只会让两组继续交叉。
- `minecraft:glowstone` 的 5 条 `*_alt` 行：见 §3，不是禁忌而是缺标签定义。

---

## 5. 既有重叠：19 对（不是这次改造引入的）

同一台粉碎机、项数相同、成员集合可完美配对 → 两条都能匹配，`findRecipe` 取先加载的那条。
按产物差异分三档：

| 档 | 对数 | 说明 |
|---|---:|---|
| 产物完全不同 | 9 | 实机行为不确定，最需要修 |
| 产物数量/副产不同 | 9 | 结果不可预测，但都还「像那么回事」 |
| 产物相同 | 1 | 纯冗余 |

清单（`A <-> B`，共享输入）：

```
产物不同（9）
compat/enderio/pulverizer/coal      <-> compat/immersiveengineering/pulverizer/black_dye   共享 charcoal
compat/enderio/pulverizer/coal      <-> compat/immersiveengineering/pulverizer/coke        共享 IE coal_coke
compat/enderio/pulverizer/clay      <-> compat/immersiveengineering/pulverizer/clay        共享 clay
compat/enderio/pulverizer/quartz_block <-> compat/immersiveengineering/pulverizer/quartz    共享 quartz_block
compat/enderio/pulverizer/sandstone <-> compat/immersiveengineering/pulverizer/red_sandstone 共享 red_sandstone
compat/enderio/pulverizer/deepslate <-> pulverizer/cobblestone                             共享 deepslate
compat/enderio/pulverizer/cobbled_deepslate <-> pulverizer/gravel                          共享 cobbled_deepslate
compat/enderio/pulverizer/mossy_cobblestone <-> pulverizer/gravel                          共享 mossy_cobblestone
compat/immersiveengineering/pulverizer/nether_gold <-> pulverizer/chain/gold_ore_raw        共享 nether_gold_ore

数量/副产不同（9）
pulverizer/blaze            <-> compat/enderio/pulverizer/blaze_powder       共享 blaze_rod
pulverizer/bone_meal        <-> compat/enderio/pulverizer/bone               共享 bone
pulverizer/sand_from_sandstone      <-> compat/enderio/pulverizer/sandstone  共享 sandstone
pulverizer/red_sand_from_sandstone  <-> compat/enderio/pulverizer/sandstone  共享 red_sandstone
compat/immersiveengineering/pulverizer/sandstone     <-> compat/enderio/pulverizer/sandstone
compat/immersiveengineering/pulverizer/red_sandstone <-> compat/enderio/pulverizer/sandstone
compat/immersiveengineering/pulverizer/sandstone     <-> pulverizer/sand_from_sandstone
compat/immersiveengineering/pulverizer/red_sandstone <-> pulverizer/red_sand_from_sandstone
compat/enderio/pulverizer/cobblestone <-> pulverizer/gravel                   共享 cobblestone

产物相同（1）
compat/immersiveengineering/pulverizer/stone <-> pulverizer/cobblestone       共享 stone
```

三条根因：

1. **手写原生配方没跟着标签化清理**。`pulverizer/blaze.json`、`bone_meal.json` 用静态物品，
   EIO 的同语义行用 `c:rods/blaze`、`c:bones`，父标签把静态物品包含进去了。兼容层上线时只保证
   「导入行不撞本模组已有签名」，但交叉不是同签名，`claim` 放行。
2. **父标签与子标签混用**。`pulverizer/gravel.json` 吃 `c:cobblestones`（normal + mossy + deepslate +
   infested），EIO 另有 `c:cobblestones/normal`、`/mossy`、`/deepslate` 三条细化行；EIO 的 sandstone 行吃
   `c:sandstone/blocks`（红 + 素 + 切制变体），IE 分两行吃子标签。父标签行必然把所有子标签行盖掉。
3. **不同邻居对同一输入给出不同产物**。`clay`、`quartz_block`、`charcoal`、`coal_coke` 这四处是两家
   本来就不一致，转换器逐条导入，没有跨来源的等价判定（它只按「输入签名」去重，而两家输入一个是静态、
   一个是标签，签名不同）。

`TENRecipeAudit` 查不出这一类：它按 `RecipeSignature.ofInputs` 分组，`tag:c:x` 与
`static:minecraft:y` 是不同 key，永远落不进同一组。

---

## 6. 落地注意

1. **改在转换器里，不要只改生成的表**。`inject.js` 无条件替换三张表的数组体，手工改 `TENCompatGen.java`
   下一轮 `convert.js && inject.js` 就被打回。最小改法是在 `convert.js#inputOf` 之后加一步
   「静态物品 → 单成员 `c:` 标签」升级，复用 `common.js#tagIndex` 的反查。
2. **自动升级必须带冲突自检**。同一规则套到 `minecraft:quartz_block` 会升级成 `c:storage_blocks/quartz`
   直接撞 EIO 行——单成员规则本身没错，错在这条配方本来就重复。要么让 `ownSignatures()` 把「改后的签名」
   也纳入基准，要么维护一份排除名单。前者更稳。
3. **`machine()` 会自动补 `not(tag_empty)`**：换成标签后，配方对「标签为空」敏感。§2 那些标签都由
   原版/NeoForge 或本行门控的模组提供，行为不变；§3 必须先解决标签归属。
4. **JEI 标签名**。`TENTagLangGen` 只为本模组引用/注册的标签出键，`c:eggs`、`c:charcoal`、`c:clay`、
   `c:nether_stars`、`c:dusts/prismarine`、`c:crops/*` 都不在里面。改成标签后 JEI 会退化成
   `#c:eggs` 裸 id，需要在这里补中英文条目（键格式 `tag.item.c.eggs`）。
5. **`c:mats/X` 的边界**。EVO 的 7 种合金不在 `TENMaterialTagGen.NEIGHBOUR_MATS` 里，
   所以 `matIngot()` 对它们会退化成 `c:ingots/X`。如果希望它们也「锭或粉皆可入」，
   要把这几个 id 加进 `NEIGHBOUR_MATS`，同时确认 EIO 侧确实有对应 `c:dusts/X`。

---

## 7. 建议顺序

1. 先修 §5 里「产物不同」的 9 对。它们与标签无关，但会挡住 §2 的改造（clay/quartz 一改就撞）。
   优先修父标签行：`pulverizer/gravel.json` 改吃 `c:cobblestones/normal`，
   `pulverizer/blaze.json`、`bone_meal.json` 与 EIO 行二选一，`coal` 三条按「煤 ↦ 煤粉、焦炭 ↦ 焦炭粉、
   木炭 ↦ 黑染料」拆开（EIO 的 `minecraft:coals` 就该退回 `c:dusts/coal` 这类单义标签）。
2. 在 `convert.js` 加「单成员标签升级 + 改后签名自检」，跑一轮 `convert → inject → runData`。
3. 补 `TENTagLangGen` 的标签键，再补 §3 的自建标签定义，把余下 16 项收掉。
4. 把「tag↔static 交叉」加进 `check.js` 与 `TENRecipeAudit`，否则同类问题还会再长出来。
