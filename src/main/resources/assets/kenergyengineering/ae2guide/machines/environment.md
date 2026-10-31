---
navigation:
  parent: index.md
  title: 环境与作业机器
  icon: machine_quarry
  position: 1030
item_ids:
- kenergyengineering:machine_cooler
- kenergyengineering:machine_beacon_simulator
- kenergyengineering:machine_mob_ripper
- kenergyengineering:machine_quarry
- kenergyengineering:machine_farm_manager
- kenergyengineering:machine_block_breaker
- kenergyengineering:machine_block_former
categories:
- machines
---

# 环境与作业机器

<Row>
  <ItemImage id="machine_cooler" scale="4" />
  <ItemImage id="machine_quarry" scale="4" />
  <ItemImage id="machine_farm_manager" scale="4" />
</Row>

* **冷却器** `machine_cooler`：消耗冷却液进行周期冷却；需要 Coolants 冷却液。
* **信标模拟机** `machine_beacon_simulator`：在范围内提供信标效果；范围可用 Rg 升级扩展。
* **生物啃噬者** `machine_mob_ripper`：清理范围内的生物并收集掉落。
* **采矿场** `machine_quarry`：在范围内采矿；可使用工具/武器附魔与范围升级。
* **农场管理机** `machine_farm_manager`：管理机器后方 9×9 的农田，优先处理成熟作物。
* **方块破坏器** `machine_block_breaker`：破坏范围内方块。
* **方块成型器** `machine_block_former`：在范围内放置方块。

范围机器可以在界面里切换范围线框显示；开启工具/武器附魔后，采矿与破坏会按附魔效果结算。效果型机器在输入消失时进度归零，缺能、断信号或输出满则保留进度。
