package com.modularmc.ten;

import net.minecraft.resources.ResourceLocation;

public class TENConstants {

    public static final ResourceLocation GUI_HANDLER = TEN.id("textures/gui/handler.png");
    public static final ResourceLocation JEI_HANDLER_1 = TEN.id("textures/gui/jei_handler1.png");
    public static final ResourceLocation JEI_HANDLER_2 = TEN.id("textures/gui/jei_handler2.png");
    public static final ResourceLocation MACHINE_GUI = TEN.id("textures/gui/machine_gui.png");

    // ───── 频道 UI 素材（P0-8 移植自 26.1.2，modular 独立切片）─────
    public static final ResourceLocation CHANNEL_LIST_BG = TEN.id("textures/gui/modular/channel_list_bg.png"); // 69x73 频道目录列表容器底图
    public static final ResourceLocation CHANNEL_ENTRY_BG_HOVER = TEN.id("textures/gui/modular/channel_entry_hover.png"); // 46x13 条目行悬停高亮
    public static final ResourceLocation CHANNEL_ENTRY_BG_NORMAL = TEN.id("textures/gui/modular/channel_entry_normal.png"); // 46x13 条目行普通背景
    // 条目行右侧接入状态小按钮雪碧图：2列×2行，每格 10x15（列0=未接入/列1=已接入；行0=normal、行1=hover）
    public static final ResourceLocation CHANNEL_ENTRY_STATE = TEN.id("textures/gui/modular/channel_entry_state.png"); // 20x30
    // 频道面板翻页/创建/删除/断开按钮雪碧图：5列×2行，每格 12x12（列0▲/列1▼/列2＋/列3✕/列4断连；行0=normal、行1=hover）
    public static final ResourceLocation CHANNEL_BUTTONS = TEN.id("textures/gui/modular/channel_buttons.png"); // 60x24

    /** 雪碧图内精灵区域（像素坐标：u, v, 宽, 高）。 */
    public record SheetUV(int u, int v, int width, int height) {}

    // channel_entry_state.png 各格 UV：列0=未接入、列1=已接入；行0=normal、行1=hover
    public static final SheetUV MINI_OUT_NORMAL = new SheetUV(0, 0, 10, 15);
    public static final SheetUV MINI_OUT_HOVER = new SheetUV(0, 15, 10, 15);
    public static final SheetUV MINI_IN_NORMAL = new SheetUV(10, 0, 10, 15);
    public static final SheetUV MINI_IN_HOVER = new SheetUV(10, 15, 10, 15);

    // channel_buttons.png 各按钮 UV：normal 在行0、hover 在行1
    public static final SheetUV SCROLL_UP_NORMAL = new SheetUV(0, 0, 12, 12);
    public static final SheetUV SCROLL_UP_HOVER = new SheetUV(0, 12, 12, 12);
    public static final SheetUV SCROLL_DOWN_NORMAL = new SheetUV(12, 0, 12, 12);
    public static final SheetUV SCROLL_DOWN_HOVER = new SheetUV(12, 12, 12, 12);
    public static final SheetUV CREATE_NORMAL = new SheetUV(24, 0, 12, 12);
    public static final SheetUV CREATE_HOVER = new SheetUV(24, 12, 12, 12);
    public static final SheetUV DELETE_NORMAL = new SheetUV(36, 0, 12, 12);
    public static final SheetUV DELETE_HOVER = new SheetUV(36, 12, 12, 12);
    public static final SheetUV DISCONNECT_NORMAL = new SheetUV(48, 0, 12, 12);
    public static final SheetUV DISCONNECT_HOVER = new SheetUV(48, 12, 12, 12);

    public static int WORLD_MIN = -64;
    public static int WORLD_MAX = 256;
}