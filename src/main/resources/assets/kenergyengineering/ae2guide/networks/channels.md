---
navigation:
  parent: index.md
  title: 频道系统
  icon: channel_item
  position: 1050
item_ids:
- kenergyengineering:channel_item
- kenergyengineering:channel_fluid
- kenergyengineering:channel_energy
- kenergyengineering:channel_connector
categories:
- networks
---

# 频道系统

<Row>
  <ItemImage id="channel_item" scale="4" />
  <ItemImage id="channel_fluid" scale="4" />
  <ItemImage id="channel_energy" scale="4" />
</Row>

频道是一份跨维度、跨机器的共享存储，按类型隔离：

* **物品频道** `channel_item`
* **流体频道** `channel_fluid`
* **能量频道** `channel_energy`

用 **频道连接器** `channel_connector` 绑定频道：先选择频道，再点击频道方块。频道名称与类型共同构成频道标识，同名但不同类型的频道互不相通。

频道界面支持创建、接入、退出与删除（仅空频道）。Jade 会显示频道接入状态。频道方块可以像普通容器一样通过面配置接入物品、流体或能量。
