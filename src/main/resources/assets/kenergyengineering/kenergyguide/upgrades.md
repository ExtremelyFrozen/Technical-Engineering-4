---
navigation:
  parent: index.md
  title: 升级
  icon: powered_levelup
  position: 1070
item_ids:
- kenergyengineering:powered_levelup
- kenergyengineering:range_levelup
- kenergyengineering:augmented_levelup
- kenergyengineering:relic_levelup
categories:
- basics
---

# 升级

<Row>
  <ItemImage id="powered_levelup" scale="4" />
  <ItemImage id="range_levelup" scale="4" />
  <ItemImage id="augmented_levelup" scale="4" />
</Row>

升级组件放进机器界面的升级槽。不同机器支持的升级不同，不支持的升级会被拒绝；
冲突的升级会在放入时直接被拦下（例如已经装了整合组件，再放第二张会被拒绝）。

## 整合组件（全局倍率）

三档整合组件只提供一个全局倍率 k：运行速度 ×k、运行耗时 ÷k、并行运行数 ×k、FE/t 消耗与生产 ×k。
每份耗电保持不变，因此加速不省电。同一台机器只能装一张，换更高档要先取出旧卡。

* **升级：增强组件** `augmented_levelup`：全局倍率 ×2。
* **升级：充能组件** `powered_levelup`：全局倍率 ×3。
* **升级：潜影组件** `relic_levelup`：全局倍率 ×4。

## 增益组件（参数增量，可叠加）

四个系列各三级，同名系列的效果按 `1 + Σ(系数 − 1)` 汇总。每张卡只改一个通道，
没被改动的参数不会出现在卡面上：

* **升级：超频回路** `speed_levelup`：运行速度 +1 / +2 / +3，运行功率与配方长度都不变。
* **升级：工序精简** `duration_levelup`：运行耗时 -10% / -20% / -30%，速度与功率都不变。
* **升级：节能回路** `efficiency_levelup`：运行功率 -10% / -20% / -25%，速度与耗时都不动。
* **升级：并行处理核心** `parallel_levelup`：并行运行数 +1 / +2 / +3，功率与运行耗时都不动。

运行速度只能是自然数（1、2、3……），所以提速卡给的是整数增量而不是百分比；
「配方本身有多长」则由工序精简的耗时倍率负责。两者是独立通道，装在一起各自生效：
实际耗时 = `配方耗时 × 耗时倍率 ÷ 速度`。

## 独特组件（整机唯一）

* **光合供能抑制** `photosyn_levelup`：允许机器接受光合注能。
* **范围扩展** `range_levelup`：扩大范围机器的作用半径。
* **食物专业烟熏 / 矿物专业冶炼**：切换熔炼机的烟熏/高炉配方模式。
* **药水效能榨取**、**星辉能量传输**、**知识储备扩充**、**地下冻土开采**、**地幔深层钻井**、**矿物探测仪器**：对应机器的专用效果。

整合组件的倍率会同时改写速度、耗时与并行运行数，增益组件只动其中一维：功率只改每 tick 耗电，
速度只改每 tick 推进几格，耗时倍率只改配方本身多长。卡面上速度写成整数增量（`+2`），
其余倍率写成百分比（整数倍率仍写 ×2 / ×3 / ×4），
两者的具体数值关系见[运行模型](runtime.md)。
并行运行数仍受四维 B 锁定限制，详见[并行运行与停机](batches.md)。
