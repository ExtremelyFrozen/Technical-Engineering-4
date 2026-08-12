package com.modularmc.ten;

import net.minecraft.resources.Identifier;

public class TENConstants {

    public static final Identifier GUI_HANDLER = TEN.id("textures/gui/handler.png");
    public static final Identifier JEI_HANDLER_1 = TEN.id("textures/gui/jei_handler1.png");
    public static final Identifier JEI_HANDLER_2 = TEN.id("textures/gui/jei_handler2.png");
    public static final Identifier MACHINE_GUI = TEN.id("textures/gui/machine_gui.png"); // 176x166 内容于 256x256 画布

    // Modular 素材族（JEI 兼容层与 XEI 共享布局引擎专用）
    public static final Identifier JEI_HANDLER_MODULAR = TEN.id("textures/gui/modular/jei_handler.png"); // 底图：上层
                                                                                                         // 150x50，精炼机下层
                                                                                                         // 170x50
    public static final Identifier ITEM_SLOT_SMALL = TEN.id("textures/gui/modular/item_slot_small.png"); // 18x18
    public static final Identifier ITEM_SLOT_LARGE = TEN.id("textures/gui/modular/item_slot_large.png"); // 26x26
    public static final Identifier FLUID_SLOT = TEN.id("textures/gui/modular/fluid_slot.png"); // 18x50
    public static final Identifier ENERGY_GAUGE_BG = TEN.id("textures/gui/modular/energy_gauge_background.png"); // 14x46
    public static final Identifier ENERGY_GAUGE_FILL = TEN.id("textures/gui/modular/energy_gauge_fill.png"); // 14x46
    public static final Identifier PROGRESS_ARROW_SMELTER_BG = TEN.id("textures/gui/modular/progress_arrow_smelter_background.png"); // 22x16
    public static final Identifier PROGRESS_ARROW_SMELTER_FILL = TEN.id("textures/gui/modular/progress_arrow_smelter_fill.png"); // 22x16
    public static final Identifier PROGRESS_ARROW_PULVERIZER_BG = TEN.id("textures/gui/modular/progress_arrow_pulverizer_background.png"); // 22x16
    public static final Identifier PROGRESS_ARROW_PULVERIZER_FILL = TEN.id("textures/gui/modular/progress_arrow_pulverizer_fill.png"); // 22x16
    public static final Identifier PROGRESS_ARROW_COMPRESSOR_BG = TEN.id("textures/gui/modular/progress_arrow_compressor_background.png"); // 22x16
    public static final Identifier PROGRESS_ARROW_COMPRESSOR_FILL = TEN.id("textures/gui/modular/progress_arrow_compressor_fill.png"); // 22x16
    public static final Identifier PROGRESS_ARROW_REFINER_BG = TEN.id("textures/gui/modular/progress_arrow_refiner_background.png"); // 22x16
    public static final Identifier PROGRESS_ARROW_REFINER_FILL = TEN.id("textures/gui/modular/progress_arrow_refiner_fill.png"); // 22x16
    public static final Identifier PROGRESS_ARROW_INDUCTION_FURNACE_BG = TEN.id("textures/gui/modular/progress_arrow_induction_furnace_background.png"); // 22x16
    public static final Identifier PROGRESS_ARROW_INDUCTION_FURNACE_FILL = TEN.id("textures/gui/modular/progress_arrow_induction_furnace_fill.png"); // 22x16
    public static final Identifier PROGRESS_ARROW_PSIONICANT_BG = TEN.id("textures/gui/modular/progress_arrow_psionicant_background.png"); // 22x16
    public static final Identifier PROGRESS_ARROW_PSIONICANT_FILL = TEN.id("textures/gui/modular/progress_arrow_psionicant_fill.png"); // 22x16
    public static final Identifier PROGRESS_ARROW_MINI_COMPRESSOR_BG = TEN.id("textures/gui/modular/progress_arrow_mini_compressor_background.png"); // 8x54
                                                                                                                                                     // 整件：顶尖朝上=目标在上
    public static final Identifier PROGRESS_ARROW_MINI_ENCFLU_BG = TEN.id("textures/gui/modular/progress_arrow_mini_encflu_background.png"); // 8x54
                                                                                                                                             // 整件：尖朝下=目标在下
    public static final Identifier FUEL_GAUGE_BG = TEN.id("textures/gui/modular/fuel_gauge_background.png"); // 13x13
    public static final Identifier FUEL_GAUGE_FILL = TEN.id("textures/gui/modular/fuel_gauge_fill.png"); // 13x13

    // GUI 翻新 002（11 台未翻新机器 modular 化）素材接线
    public static final Identifier PROGRESS_BAR_WIDE_BG = TEN.id("textures/gui/modular/progress_bar_wide_background.png"); // 80x5
                                                                                                                           // 横向进度条灰底槽
    public static final Identifier PROGRESS_BAR_WIDE_FILL = TEN.id("textures/gui/modular/progress_bar_wide_fill.png"); // 80x5
                                                                                                                       // 横向进度条绿填充
    public static final Identifier ITEM_SLOT_SMALL_CHARGE = TEN.id("textures/gui/modular/item_slot_small_charge.png"); // 18x18
                                                                                                                       // 槽+右下绿流入符号（充电）
    public static final Identifier ITEM_SLOT_SMALL_DISCHARGE = TEN.id("textures/gui/modular/item_slot_small_discharge.png"); // 18x18
                                                                                                                             // 槽+右下红流出符号（放电）
    public static final Identifier CHANNEL_LIST_BG = TEN.id("textures/gui/modular/channel_list_bg.png"); // 69x73
                                                                                                         // 频道目录列表容器底图
                                                                                                         // （深色容器 + 4
                                                                                                         // 条行分隔线
                                                                                                         // + 右下高亮边框；条目行
                                                                                                         // channel_entry_*
                                                                                                         // 绘制其上）
    public static final Identifier CHANNEL_ENTRY_BG_HOVER = TEN.id("textures/gui/modular/channel_entry_hover.png"); // 46x13
                                                                                                                    // 条目行
                                                                                                                    // 悬停（hover）高亮背景（独立切片）
    public static final Identifier CHANNEL_ENTRY_BG_NORMAL = TEN.id("textures/gui/modular/channel_entry_normal.png"); // 46x13
                                                                                                                      // 条目行
                                                                                                                      // 未悬停普通背景（独立切片）
    // 每条目行右侧接入状态小按钮雪碧图：2列×2行，每格 10x15（列0=未接入 / 列1=已接入；行0=normal、行1=hover）
    public static final Identifier CHANNEL_ENTRY_STATE = TEN.id("textures/gui/modular/channel_entry_state.png"); // 20x30
    public static final Identifier CHANNEL_BUTTONS = TEN.id("textures/gui/modular/channel_buttons.png"); // 60x24
                                                                                                         // 频道面板翻页/创建/删除/断开按钮雪碧图：5列×2行，每格
                                                                                                         // 12x12
                                                                                                         // （列0▲上翻 /
                                                                                                         // 列1▼下翻 /
                                                                                                         // 列2＋创建 /
                                                                                                         // 列3✕删除 /
                                                                                                         // 列4断连（退出频道）；行0=normal，行1=hover）

    /** 雪碧图内精灵区域（像素坐标：u, v, 宽, 高）。 */
    public record SheetUV(int u, int v, int width, int height) {}

    // channel_entry_state.png（20x30）各格 UV：列0=未接入、列1=已接入；行0=normal、行1=hover
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
    // 列4断连（u=48）：操作区主按钮「断开 = 退出频道」（rpcLeaveChannel）。素材已移除旧的
    // 滚动列中位 ⇌ 断链（原 col4=UNLINK 删除双入口）——删除频道仅保留操作区列3✕一处。
    public static final SheetUV DISCONNECT_NORMAL = new SheetUV(48, 0, 12, 12);
    public static final SheetUV DISCONNECT_HOVER = new SheetUV(48, 12, 12, 12);
    // 注：channel.png / channel_item.png（textures/gui/）已无常量引用——全部部件已切为 modular/ 独立素材，
    // 两文件保留作回退。channel_item.png 仅比 channel.png 多 4 个预绘物品槽（x[7,43) y[26,62) 区域）。

    public static int WORLD_MIN = -64;
    public static int WORLD_MAX = 256;
}
