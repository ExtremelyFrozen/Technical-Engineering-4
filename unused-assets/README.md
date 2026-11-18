# 未使用的资源

这里存放从 `src/main/resources/assets/kenergyengineering/` 移出的纹理。它们不被任何模型、blockstate
或代码引用，留在资源目录只会白白增大 jar，所以移到这里，**不参与打包**。

保留而不删除，是因为其中一部分是旧版界面纹理，重制 GUI 时可能复用。

## 目录对应

| 这里 | 原位置 |
|---|---|
| `_unused/` | `assets/kenergyengineering/_unused/` |
| `textures/_unused/` | `assets/kenergyengineering/textures/_unused/` |
| `textures/machine/` | `assets/kenergyengineering/textures/machine/` |
| `textures/fluid/` | `assets/kenergyengineering/textures/fluid/` |
| `textures/item/*.png` | 四个孤立的物品纹理 |

## 判定方法

1. 解析所有模型 JSON 的 `textures` / `parent` / `model` 字段
2. 扫描 Java 源码里的纹理路径字符串（含 `TENConstants` 的 `TEN.id(...)`）
3. 两者都未命中的判为孤立

## 一个已知的误判

流体纹理走 `TEN.id("block/" + name)` 动态拼接，静态扫描认不出来，会误报为孤立。
`textures/block/liquid_*_flowing.png` 这 5 个文件仍在使用，**不要移动**。
