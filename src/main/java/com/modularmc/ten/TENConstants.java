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

    // ───── 进度条素材（P2-1 移植自 26.1.2）─────
    public static final ResourceLocation PROGRESS_BAR_WIDE_BG = TEN.id("textures/gui/modular/progress_bar_wide_background.png"); // 80x5 横向灰底槽
    public static final ResourceLocation PROGRESS_BAR_WIDE_FILL = TEN.id("textures/gui/modular/progress_bar_wide_fill.png"); // 80x5 绿填充
    public static final ResourceLocation PROGRESS_BAR_WIDE_COOLANT = TEN.id("textures/gui/modular/progress_bar_wide_coolant.png"); // 80x5 冷却剂消耗进度栏
    public static final ResourceLocation PROGRESS_ARROW_MINI_COMPRESSOR_BG = TEN.id("textures/gui/modular/progress_arrow_mini_compressor_background.png"); // 8x54 顶尖朝上
    public static final ResourceLocation PROGRESS_ARROW_MINI_ENCFLU_BG = TEN.id("textures/gui/modular/progress_arrow_mini_encflu_background.png"); // 8x54 尖朝下

    // ───── 机器 GUI 素材族（全量化对齐 26.1.2，modular 独立切片）─────
    public static final ResourceLocation ITEM_SLOT_SMALL = TEN.id("textures/gui/modular/item_slot_small.png"); // 18x18
    public static final ResourceLocation ITEM_SLOT_LARGE = TEN.id("textures/gui/modular/item_slot_large.png"); // 26x26
    public static final ResourceLocation ITEM_SLOT_SMALL_CHARGE = TEN.id("textures/gui/modular/item_slot_small_charge.png"); // 18x18 充电动画
    public static final ResourceLocation ITEM_SLOT_SMALL_DISCHARGE = TEN.id("textures/gui/modular/item_slot_small_discharge.png"); // 18x18 放电动画
    public static final ResourceLocation ENERGY_GAUGE_BG = TEN.id("textures/gui/modular/energy_gauge_background.png"); // 14x46
    public static final ResourceLocation ENERGY_GAUGE_FILL = TEN.id("textures/gui/modular/energy_gauge_fill.png"); // 14x46
    public static final ResourceLocation FUEL_GAUGE_BG = TEN.id("textures/gui/modular/fuel_gauge_background.png"); // 13x13
    public static final ResourceLocation FUEL_GAUGE_FILL = TEN.id("textures/gui/modular/fuel_gauge_fill.png"); // 13x13
    public static final ResourceLocation PROGRESS_ARROW_SMELTER_BG = TEN.id("textures/gui/modular/progress_arrow_smelter_background.png"); // 22x16
    public static final ResourceLocation PROGRESS_ARROW_SMELTER_FILL = TEN.id("textures/gui/modular/progress_arrow_smelter_fill.png"); // 22x16
    public static final ResourceLocation PROGRESS_ARROW_PULVERIZER_BG = TEN.id("textures/gui/modular/progress_arrow_pulverizer_background.png"); // 22x16
    public static final ResourceLocation PROGRESS_ARROW_PULVERIZER_FILL = TEN.id("textures/gui/modular/progress_arrow_pulverizer_fill.png"); // 22x16
    public static final ResourceLocation PROGRESS_ARROW_COMPRESSOR_BG = TEN.id("textures/gui/modular/progress_arrow_compressor_background.png"); // 22x16
    public static final ResourceLocation PROGRESS_ARROW_COMPRESSOR_FILL = TEN.id("textures/gui/modular/progress_arrow_compressor_fill.png"); // 22x16
    public static final ResourceLocation PROGRESS_ARROW_REFINER_BG = TEN.id("textures/gui/modular/progress_arrow_refiner_background.png"); // 22x16
    public static final ResourceLocation PROGRESS_ARROW_REFINER_FILL = TEN.id("textures/gui/modular/progress_arrow_refiner_fill.png"); // 22x16
    public static final ResourceLocation PROGRESS_ARROW_INDUCTION_FURNACE_BG = TEN.id("textures/gui/modular/progress_arrow_induction_furnace_background.png"); // 22x16
    public static final ResourceLocation PROGRESS_ARROW_INDUCTION_FURNACE_FILL = TEN.id("textures/gui/modular/progress_arrow_induction_furnace_fill.png"); // 22x16
    public static final ResourceLocation PROGRESS_ARROW_PSIONICANT_BG = TEN.id("textures/gui/modular/progress_arrow_psionicant_background.png"); // 22x16
    public static final ResourceLocation PROGRESS_ARROW_PSIONICANT_FILL = TEN.id("textures/gui/modular/progress_arrow_psionicant_fill.png"); // 22x16
    public static final ResourceLocation JEI_HANDLER_MODULAR = TEN.id("textures/gui/modular/jei_handler.png"); // 上层150x50/下层170x50

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