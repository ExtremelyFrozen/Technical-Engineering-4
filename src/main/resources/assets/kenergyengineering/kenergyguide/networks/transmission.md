---
navigation:
  parent: index.md
  title: 线缆与管道
  icon: cable_quartz
  position: 1060
item_ids:
- kenergyengineering:cable
- kenergyengineering:cable_quartz
- kenergyengineering:cable_azure
- kenergyengineering:cable_star
- kenergyengineering:pipe
- kenergyengineering:pipe_white
- kenergyengineering:pipe_black
categories:
- networks
---

# 线缆与管道

<Row>
  <ItemImage id="cable" scale="4" />
  <ItemImage id="cable_quartz" scale="4" />
  <ItemImage id="pipe" scale="4" />
</Row>

## 能量线缆

* **玻璃能量线缆** `cable`：200 FE/t，1 kFE 缓冲。
* **石英能量线缆** `cable_quartz`：1 kFE/t，20 kFE 缓冲。
* **蔚蓝能量线缆** `cable_azure`：4 kFE/t，50 kFE 缓冲。
* **星光能量线缆** `cable_star`：200 kFE/t，无限缓冲。

线缆会自动连接相邻线缆与机器，支持含水放置。

## 物品管道

* `pipe`：普通物品管道。
* `pipe_white`：白色物品管道。
* `pipe_black`：黑色物品管道。

用扳手切换连接模式：`NONE`、`NORMAL`、`PUSH`、`PULL`。源边只有显式设为 `PULL` 才会主动从设备抽取；默认 `NORMAL` 不会自动搬运。旧存档里的管道升级后需要用扳手重新设定模式。

## 面配置

传输设备的面配置决定能量与物品的输入输出。线缆与管道也会遵循面权限；读侧内容不会被抽取权限藏空。
