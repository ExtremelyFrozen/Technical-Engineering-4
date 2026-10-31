---
navigation:
  parent: index.md
  title: 加工机器
  icon: machine_smelter
  position: 1020
item_ids:
- kenergyengineering:machine_smelter
- kenergyengineering:machine_pulverizer
- kenergyengineering:machine_compressor
- kenergyengineering:machine_refiner
- kenergyengineering:machine_induction_furnace
- kenergyengineering:machine_psionicant
- kenergyengineering:machine_enchantment_flusher
- kenergyengineering:machine_matter_condenser
categories:
- machines
---

# 加工机器

<Row>
  <ItemImage id="machine_smelter" scale="4" />
  <ItemImage id="machine_pulverizer" scale="4" />
  <ItemImage id="machine_compressor" scale="4" />
</Row>

## 机器一览

* **熔炼机** `machine_smelter`：原版熔炉配方；Blast/Smoke 升级可切换高炉/烟熏模式。
* **粉碎机** `machine_pulverizer`：矿石、锭与材料粉碎。
* **压缩机** `machine_compressor`：压缩材料。
* **精炼机** `machine_refiner`：精炼与提纯。
* **感应炉** `machine_induction_furnace`：合金与高温配方。
* **灵能处理器** `machine_psionicant`：灵能配方。
* **祛魔机** `machine_enchantment_flusher`：处理附魔物品与液态经验。
* **物质结晶器** `machine_matter_condenser`：物质结晶与冷凝。

## 批处理

配方机器支持批量处理：一次消耗多份输入并产出多份输出。批量由输入、流体、输出空间与能量四维共同限制；处理中锁定批量，部分取出会等待恢复。详见[批处理与停滞](../batches.md)。

## 升级

升级槽可安装效率、时长、批处理与配方模式升级。不同机器支持的升级不同；不支持的升级会被拒绝。
