#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成四张独特组件升级卡的 16x16 贴图。

为什么不直接给旧图上色：四张卡各自讲一件不同的事（压榨 / 筛选 / 催生 / 复种），
中心图案必须是独立绘制的符号；只调色会让玩家在升级槽里分不清谁是谁。

做法分两层：
1. 外壳逐像素继承 ``mineral_levelup.png`` 的灰阶，四张卡各用 9 阶色系按主题换色，明暗关系保持一致。
2. 中心 10x11 图案区用字符画声明，改一行字符即可调整图案，不需要图像编辑器。

用法：
    python tools/gen_upgrade_textures.py             # 写入资源目录
    python tools/gen_upgrade_textures.py --preview   # 另外输出放大对比图到 build/
"""

from __future__ import annotations

import argparse
import os
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
TEXTURE_DIR = ROOT / "src/main/resources/assets/kenergyengineering/textures/item/upgrade"
SHELL_SOURCE = TEXTURE_DIR / "mineral_levelup.png"
PREVIEW_PATH = ROOT / "build/tmp_upgrade_textures_preview.png"

# 外壳只提取内框之外，图案区可外扩覆盖内沿高光。
SHELL_X0, SHELL_Y0, SHELL_X1, SHELL_Y1 = 3, 3, 10, 11

# 图案区向内框外扩，覆盖内框高光。
ART_X0, ART_Y0 = 2, 2
ART_W, ART_H = 10, 11


def luminance(color: tuple[int, int, int, int]) -> float:
    r, g, b = color[0], color[1], color[2]
    return 0.2126 * r + 0.7152 * g + 0.0722 * b


def saturation(color: tuple[int, int, int, int]) -> int:
    return max(color[0], color[1], color[2]) - min(color[0], color[1], color[2])


def in_shell_hole(x: int, y: int) -> bool:
    return SHELL_X0 <= x <= SHELL_X1 and SHELL_Y0 <= y <= SHELL_Y1


# 外壳灰阶是带轻微偏色的冷灰（饱和度 2~19），中心图案的彩色（饱和度 47+）越过内容区时
# 会混进来。阈值 30 把两者分开，避免把矿物晶体和岩浆当成边框色。
SHELL_MAX_SATURATION = 30


def extract_shell() -> tuple[dict[tuple[int, int], int], list[tuple[int, int, int, int]]]:
    """返回 (每像素的灰阶档位, 按亮度排序的灰阶)。

    档位而不是颜色：目标色系阶数与原色阶一一对应，换色时明暗关系不会走样。
    图案溢出到边框区的彩色像素按亮度并入最近的一档，边缘不会留洞。
    """
    source = Image.open(SHELL_SOURCE).convert("RGBA")
    raw: dict[tuple[int, int], tuple[int, int, int, int]] = {}
    for y in range(16):
        for x in range(16):
            if in_shell_hole(x, y):
                continue
            pixel = source.getpixel((x, y))
            if pixel[3] == 0:
                continue
            raw[(x, y)] = pixel

    ramp = sorted({c for c in raw.values() if saturation(c) < SHELL_MAX_SATURATION}, key=luminance)
    levels = {color: index for index, color in enumerate(ramp)}

    shell: dict[tuple[int, int], int] = {}
    for pos, color in raw.items():
        if color in levels:
            shell[pos] = levels[color]
        else:
            nearest = min(range(len(ramp)), key=lambda i: abs(luminance(ramp[i]) - luminance(color)))
            shell[pos] = nearest
    return shell, ramp


SHELL_LEVELS, SHELL_RAMP = extract_shell()


def hex_to_rgba(value: str) -> tuple[int, int, int, int]:
    value = value.lstrip("#")
    return (int(value[0:2], 16), int(value[2:4], 16), int(value[4:6], 16), 255)


# 外壳色系：阶数与 SHELL_RAMP 相同，索引一一对应（0 = 最暗的描边，末位 = 最亮的高光）
CARDS: dict[str, dict] = {
    "press_levelup": {
        # 钢蓝灰：压机、砧座与金属夹具的冷色。
        "shell": ["#08090b", "#141920", "#1e242b", "#2b333b", "#3a444e", "#4c5966", "#64727f", "#83919e", "#a8b6c2"],
        "palette": {
            "P": "#cfdae6",  # 压板亮面
            "p": "#55616d",  # 压板暗面
            "A": "#8a97a5",  # 砧座亮面
            "a": "#3a444f",  # 砧座暗面
            "R": "#8f3a12",  # 被挤压的矿料
            "r": "#ff9a3c",  # 矿料高光
            "o": "#ffd27a",  # 外溢碎屑
        },
        "art": [
            "..........",
            "..P....P..",
            "..PPPPPP..",
            "..pppppp..",
            "...RRRRR..",
            "..RrrRRRR.",
            ".oRRRRRRRo",
            "..RRRRRR..",
            ".PAAAAAAP.",
            "..aaaaaa..",
            "..........",
        ],
    },
    "filter_levelup": {
        # 青灰：筛网、玻璃、水洗分选的冷绿
        "shell": ["#070b0c", "#111a1b", "#1a2527", "#253234", "#334346", "#44575a", "#5b7073", "#7a9093", "#9db2b5"],
        "palette": {
            "M": "#b6c9cc",  # 漏斗亮面
            "m": "#667d80",  # 漏斗暗面
            "N": "#e6f6f8",  # 筛网
            "n": "#7a9093",  # 筛网阴影
            "g": "#c8eef2",  # 落下的细粒
        },
        "art": [
            "..........",
            "MMMMMMMMMM",
            "mMMMMMMMMm",
            ".mMMMMMMm.",
            "..mMMMMm..",
            "...mMMm...",
            "NNNNNNNNNN",
            "nnnnnnnnnn",
            "..g...g...",
            "...g...g..",
            "..........",
        ],
    },
    "oddgrowth_levelup": {
        # 紫灰：奇异物质的偏色，与农场的绿形成对比
        "shell": ["#09070d", "#15111c", "#1f1a29", "#2b2540", "#393252", "#4a4269", "#605680", "#7e74a0", "#a298bf"],
        "palette": {
            "V": "#ff9df2",  # 奇异能量核心
            "v": "#c94ec4",  # 奇异能量外沿
            "G": "#7fdd4a",  # 作物亮部
            "g": "#3f8a26",  # 作物暗部
            "S": "#6b4a2a",  # 土壤亮面
            "s": "#3d2814",  # 土壤暗面
        },
        "art": [
            "....v.....",
            "...vVv....",
            "....v.....",
            ".g..g..g..",
            ".Gg.Gg.Gg.",
            ".gG.gG.gG.",
            ".g..g..g..",
            "SSSSSSSSSS",
            "sSSSSSSSSs",
            "..........",
            "..........",
        ],
    },
    "resprout_levelup": {
        # 绿棕灰：耕地与幼苗的暖绿
        "shell": ["#080a06", "#121810", "#1b2318", "#262f21", "#333f2c", "#43503a", "#596a4d", "#768863", "#99ab84"],
        "palette": {
            "G": "#9be04f",  # 幼苗亮部
            "g": "#4f8f28",  # 幼苗暗部
            "S": "#6b4a2a",  # 耕地亮面
            "s": "#3d2814",  # 耕地暗面
            "w": "#ecdca6",  # 飞出的种子
        },
        "art": [
            "....w.....",
            "..........",
            "..........",
            "...gGg....",
            "....G.....",
            "..w.G..w..",
            "....G.....",
            "....g.....",
            "SSSSSSSSSS",
            "sSSSSSSSSs",
            "....w.....",
        ],
    },
}


def build_card(spec: dict) -> Image.Image:
    ramp = [hex_to_rgba(c) for c in spec["shell"]]
    if len(ramp) != len(SHELL_RAMP):
        raise ValueError(f"外壳色阶必须是 {len(SHELL_RAMP)} 阶，实际 {len(ramp)}")

    art = spec["art"]
    if len(art) != ART_H or any(len(row) != ART_W for row in art):
        raise ValueError(f"字符画必须是 {ART_W}x{ART_H}")

    palette = {key: hex_to_rgba(value) for key, value in spec["palette"].items()}

    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    pixels = image.load()

    for (x, y), level in SHELL_LEVELS.items():
        pixels[x, y] = ramp[level]

    # 图案区外扩时，`.` 保持透明以露出外壳高光。
    for row_index, row in enumerate(art):
        for col_index, symbol in enumerate(row):
            x = ART_X0 + col_index
            y = ART_Y0 + row_index
            inside_hole = SHELL_X0 <= x <= SHELL_X1 and SHELL_Y0 <= y <= SHELL_Y1
            if symbol == ".":
                if not inside_hole:
                    continue
                # 底面在色阶 5/6 间加噪点，避免图案区死平。
                noise = (x * 5 + y * 3) % 7
                pixels[x, y] = ramp[6] if noise < 4 else ramp[5]
            else:
                if symbol not in palette:
                    raise ValueError(f"字符 {symbol!r} 不在调色板里")
                pixels[x, y] = palette[symbol]

    return image


def render_preview(images: dict[str, Image.Image]) -> None:
    names = list(images)
    references = ["mineral_levelup", "knowledge_levelup", "photosyn_levelup", "range_levelup"]
    scale, pad, label_h = 12, 16, 24
    cell = 16 * scale

    cols = 4
    rows = 2
    width = cols * (cell + pad) + pad
    height = rows * (cell + pad + label_h) + pad
    canvas = Image.new("RGBA", (width, height), (244, 244, 244, 255))
    draw = ImageDraw.Draw(canvas)
    try:
        font = ImageFont.truetype("C:/Windows/Fonts/arial.ttf", 15)
    except OSError:
        font = ImageFont.load_default()

    def place(index: int, image: Image.Image, label: str) -> None:
        row, col = divmod(index, cols)
        x = pad + col * (cell + pad)
        y = pad + row * (cell + pad + label_h)
        big = image.resize((cell, cell), Image.NEAREST)
        canvas.paste(big, (x, y), big)
        draw.text((x, y + cell + 3), label, fill=(20, 20, 20, 255), font=font)

    for index, name in enumerate(names):
        place(index, images[name], name.replace("_levelup", ""))

    for offset, name in enumerate(references):
        path = TEXTURE_DIR / f"{name}.png"
        if path.exists():
            place(cols + offset, Image.open(path).convert("RGBA"), name.replace("_levelup", ""))

    PREVIEW_PATH.parent.mkdir(parents=True, exist_ok=True)
    canvas.save(PREVIEW_PATH)
    print(f"预览图: {PREVIEW_PATH}")


def main() -> None:
    parser = argparse.ArgumentParser(description="生成四张独特组件升级卡的贴图")
    parser.add_argument("--preview", action="store_true", help="额外输出放大对比图")
    args = parser.parse_args()

    os.makedirs(TEXTURE_DIR, exist_ok=True)
    images: dict[str, Image.Image] = {}
    for name, spec in CARDS.items():
        image = build_card(spec)
        target = TEXTURE_DIR / f"{name}.png"
        image.save(target)
        images[name] = image
        print(f"写入 {target.relative_to(ROOT)}")

    if args.preview:
        render_preview(images)


if __name__ == "__main__":
    main()
