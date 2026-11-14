---
navigation:
  parent: index.md
  title: 面配置
  icon: spanner
  position: 1080
item_ids:
- kenergyengineering:spanner
categories:
- basics
---

# 面配置

<ItemImage id="spanner" scale="4" />

机器每个面都可以独立设置能量、物品与流体模式：

* `OFF`：禁用
* `IN`：主动输入
* `OUT`：主动输出
* `BE_IN`：被动输入
* `BE_OUT`：被动输出
* `BOTH`：被动双向

在机器界面里打开面配置面板，按面切换模式。引擎的能量面强制为主动输出；其他机器默认被动双向，可以按需要收窄。

面权限同时影响 capability 的写入与抽取。观察读（如 AE2 样板供应器读取目标机器内容）始终返回机器真实内容，不会被抽取权限藏空；这也是 AE2 阻挡模式能正确工作的前提。
