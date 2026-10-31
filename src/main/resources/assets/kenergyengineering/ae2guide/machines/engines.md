---
navigation:
  parent: index.md
  title: 引擎
  icon: engine_extraction
  position: 1040
item_ids:
- kenergyengineering:engine_extraction
- kenergyengineering:engine_metal
- kenergyengineering:engine_biomass
- kenergyengineering:engine_solar
categories:
- machines
---

# 引擎

<Row>
  <ItemImage id="engine_extraction" scale="4" />
  <ItemImage id="engine_solar" scale="4" />
</Row>

* **萃取引擎** `engine_extraction`：燃烧燃料产能。
* **金属引擎** `engine_metal`：燃烧燃料产能。
* **生物质引擎** `engine_biomass`：燃烧生物质燃料产能。
* **光合引擎** `engine_solar`：依赖天空光照产能，需要露天放置。

引擎的能量面强制为输出，会主动向相邻机器或线缆推送能量；其他面不能输入能量。使用线缆或能量单元储存多余电力。
