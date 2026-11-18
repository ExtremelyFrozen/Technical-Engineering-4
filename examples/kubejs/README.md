# KubeJS 配方示例

五个带数据包配方的机器各一份可运行的魔改案例。把需要的 `.js` 复制到整合包的
`kubejs/server_scripts/` 目录即可，不需要额外配置。

## 槽位

| 机器 | 输入槽 | 输出槽 |
|---|---|---|
| `pulverizer` 粉碎机 | 1 | 4 |
| `compressor` 压缩机 | 2 | 1 |
| `psionicant` 灵能处理器 | 2 | 1 |
| `induction_furnace` 感应炉 | 3 | 1 |
| `refiner` 精炼机 | 2 | 2 |

超出槽位数的条目会被截断，写配方前先对照这张表。

## 输入的四种写法

```js
'minecraft:coal'                    // 单个物品
'4x minecraft:coal'                 // 带数量
'#c:ores/iron'                      // 标签
'4x #c:ingots/iron'                 // 带数量的标签
{form: 'item', type: 'static', key: 'minecraft:coal', count: 1, chance: 1.0, rolls: 1}
```

前三种是简写，第四种是完整对象。**流体只能写完整对象**，因为简写无法区分「100 mB 流体」和
「100 个物品」。

## 完整对象的字段

| 字段 | 取值 | 说明 |
|---|---|---|
| `form` | `item` / `fluid` | 材料形态，缺省 `item` |
| `type` | `static` / `tag` | 具体 id 还是标签 |
| `key` | 资源位置 | 物品 id、标签 id 或流体 id |
| `count` | 整数 | 物品数量，`form=item` 时使用 |
| `amount` | 整数 | 流体数量（mB），`form=fluid` 时使用 |
| `chance` | 0.0–1.0 | 产出概率，缺省 1.0 |
| `rolls` | 整数 | 产出掷骰次数，缺省 1 |

`count` 与 `amount` 二选一，由 `form` 决定。输入侧一般不需要写 `chance`，它只对输出有意义。

## 配方 id

不指定 id 时，KubeJS 按机器类型与输入签名自动生成。需要固定 id（比如让数据包覆盖、或用
`event.remove` 精确删除）时在末尾链式调用：

```js
event.recipes.kenergyengineering.pulverizer(['minecraft:coal'], ['minecraft:diamond'], 100)
    .id('mypack:coal_to_diamond')
```

## 与其他模组的关系

这些配方和内置的数据包配方走同一套序列化器，`compat.disabledSources` 只影响本模组自带的兼容
配方，不影响脚本写入的内容。脚本配方的加载顺序在数据包之后，同输入签名时后加载的生效。
