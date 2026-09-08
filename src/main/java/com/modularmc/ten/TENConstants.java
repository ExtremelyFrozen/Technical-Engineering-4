package com.modularmc.ten;

import net.minecraft.resources.ResourceLocation;

public class TENConstants {

    public static final ResourceLocation LEGACY_SHEET = TEN.id("textures/gui/legacy/handler.png");
    public static final ResourceLocation MACHINE_GUI = TEN.id("textures/gui/panels/machine_gui.png");

    // ───── 频道 UI 素材（P0-8 移植自 26.1.2，modular 独立切片）─────
    public static final ResourceLocation CHANNEL_LIST_BG = TEN.id("textures/gui/channels/list_bg.png"); // 69x73
                                                                                                        // 频道目录列表容器底图
    public static final ResourceLocation CHANNEL_ENTRY_BG_HOVER = TEN.id("textures/gui/channels/entry_bg_hover.png"); // 46x13
                                                                                                                      // 条目行悬停高亮
    public static final ResourceLocation CHANNEL_ENTRY_BG_NORMAL = TEN.id("textures/gui/channels/entry_bg_normal.png"); // 46x13
                                                                                                                        // 条目行普通背景
    // 条目行右侧接入状态小按钮雪碧图：2列×2行，每格 10x15（列0=未接入/列1=已接入；行0=normal、行1=hover）
    public static final ResourceLocation CHANNEL_ENTRY_STATE = TEN.id("textures/gui/channels/entry_states.png"); // 20x30
    // 频道面板翻页/创建/删除/断开按钮雪碧图：5列×2行，每格 12x12（列0▲/列1▼/列2＋/列3✕/列4断连；行0=normal、行1=hover）
    public static final ResourceLocation CHANNEL_BUTTONS = TEN.id("textures/gui/channels/buttons.png"); // 60x24

    // ───── 进度条素材（P2-1 移植自 26.1.2）─────
    public static final ResourceLocation PROGRESS_BAR_WIDE_BG = TEN.id("textures/gui/progress/bar_wide_bg.png"); // 80x5
                                                                                                                 // 横向灰底槽
    public static final ResourceLocation PROGRESS_BAR_WIDE_FILL = TEN.id("textures/gui/progress/bar_wide_fill.png"); // 80x5
                                                                                                                     // 绿填充
    public static final ResourceLocation PROGRESS_PROGRESS_BAR_WIDE_COOLANT = TEN.id("textures/gui/progress/bar_wide_coolant.png"); // 80x5
    // 冷却剂消耗进度栏
    public static final ResourceLocation PROGRESS_ARROW_MINI_COMPRESSOR_BG = TEN.id("textures/gui/progress/arrow_mini_compressor_bg.png"); // 8x54
                                                                                                                                           // 顶尖朝上
    public static final ResourceLocation PROGRESS_ARROW_MINI_ENCFLU_BG = TEN.id("textures/gui/progress/arrow_mini_encflu_bg.png"); // 8x54
                                                                                                                                   // 尖朝下

    // ───── 机器 GUI 素材族（全量化对齐 26.1.2，modular 独立切片）─────
    public static final ResourceLocation ITEM_SLOT_SMALL = TEN.id("textures/gui/slots/item_slot_small.png"); // 18x18
    public static final ResourceLocation ITEM_SLOT_LARGE = TEN.id("textures/gui/slots/item_slot_large.png"); // 26x26
    public static final ResourceLocation ITEM_SLOT_SMALL_CHARGE = TEN.id("textures/gui/slots/item_slot_small_charge.png"); // 18x18
                                                                                                                           // 充电动画
    public static final ResourceLocation ITEM_SLOT_SMALL_DISCHARGE = TEN.id("textures/gui/slots/item_slot_small_discharge.png"); // 18x18
                                                                                                                                 // 放电动画
    public static final ResourceLocation ENERGY_GAUGE_BG = TEN.id("textures/gui/gauges/energy_gauge_bg.png"); // 14x46
    public static final ResourceLocation ENERGY_GAUGE_FILL = TEN.id("textures/gui/gauges/energy_gauge_fill.png"); // 14x46
    public static final ResourceLocation FUEL_GAUGE_BG = TEN.id("textures/gui/gauges/fuel_gauge_bg.png"); // 13x13
    public static final ResourceLocation FUEL_GAUGE_FILL = TEN.id("textures/gui/gauges/fuel_gauge_fill.png"); // 13x13
    /** 太阳能引擎专用燃料表（13x13，配色独立于通用燃料表） */
    public static final ResourceLocation FUEL_GAUGE_SOLAR_BG = TEN.id("textures/gui/gauges/fuel_gauge_solar_bg.png");
    public static final ResourceLocation FUEL_GAUGE_SOLAR_FILL = TEN.id("textures/gui/gauges/fuel_gauge_solar_fill.png");
    public static final ResourceLocation PROGRESS_ARROW_SMELTER_BG = TEN.id("textures/gui/progress/arrow_smelter_bg.png"); // 22x16
    public static final ResourceLocation PROGRESS_ARROW_SMELTER_FILL = TEN.id("textures/gui/progress/arrow_smelter_fill.png"); // 22x16
    public static final ResourceLocation PROGRESS_ARROW_PULVERIZER_BG = TEN.id("textures/gui/progress/arrow_pulverizer_bg.png"); // 22x16
    public static final ResourceLocation PROGRESS_ARROW_PULVERIZER_FILL = TEN.id("textures/gui/progress/arrow_pulverizer_fill.png"); // 22x16
    public static final ResourceLocation PROGRESS_ARROW_COMPRESSOR_BG = TEN.id("textures/gui/progress/arrow_compressor_bg.png"); // 22x16
    public static final ResourceLocation PROGRESS_ARROW_COMPRESSOR_FILL = TEN.id("textures/gui/progress/arrow_compressor_fill.png"); // 22x16
    public static final ResourceLocation PROGRESS_ARROW_REFINER_BG = TEN.id("textures/gui/progress/arrow_refiner_bg.png"); // 22x16
    public static final ResourceLocation PROGRESS_ARROW_REFINER_FILL = TEN.id("textures/gui/progress/arrow_refiner_fill.png"); // 22x16
    public static final ResourceLocation PROGRESS_ARROW_INDUCTION_FURNACE_BG = TEN.id("textures/gui/progress/arrow_induction_furnace_bg.png"); // 22x16
    public static final ResourceLocation PROGRESS_ARROW_INDUCTION_FURNACE_FILL = TEN.id("textures/gui/progress/arrow_induction_furnace_fill.png"); // 22x16
    public static final ResourceLocation PROGRESS_ARROW_PSIONICANT_BG = TEN.id("textures/gui/progress/arrow_psionicant_bg.png"); // 22x16
    public static final ResourceLocation PROGRESS_ARROW_PSIONICANT_FILL = TEN.id("textures/gui/progress/arrow_psionicant_fill.png"); // 22x16
    public static final ResourceLocation JEI_RECIPE_BG = TEN.id("textures/gui/jei/recipe_bg.png"); // 上层150x50/下层170x50
    public static final ResourceLocation FLUID_SLOT = TEN.id("textures/gui/slots/fluid_slot.png"); // 18x50
    /** 流体槽覆盖层（18x50）：常驻顶层——空槽时叠在槽上，有流体时叠在流体层上 */
    public static final ResourceLocation FLUID_SLOT_OVERLAY = TEN.id("textures/gui/slots/fluid_slot_overlay.png");

    // ───── 侧边展开面板素材（分离式 tab：面板独立底图，九宫格源图）─────
    public static final ResourceLocation PANEL_SLICED = TEN.id("textures/gui/panels/panel_sliced.png"); // 128x128，四角
                                                                                                        // 4x4 固定
    /** 齿轮图标（26x26，配置 tab 头） */
    public static final ResourceLocation ICON_CONFIG = TEN.id("textures/gui/icons/config.png");
    /** 面朝向六态图标（12x12 整图，配置面板 front/back/left/right/up/down 按钮）：索引即 FaceOption 枚举值 */
    public static final ResourceLocation ICON_FACE_OFF = TEN.id("textures/gui/icons/face_off.png");
    public static final ResourceLocation ICON_FACE_IN = TEN.id("textures/gui/icons/face_in.png");
    public static final ResourceLocation ICON_FACE_OUT = TEN.id("textures/gui/icons/face_out.png");
    public static final ResourceLocation ICON_FACE_BOTH = TEN.id("textures/gui/icons/face_both.png");
    public static final ResourceLocation ICON_FACE_BE_IN = TEN.id("textures/gui/icons/face_be_in.png");
    public static final ResourceLocation ICON_FACE_BE_OUT = TEN.id("textures/gui/icons/face_be_out.png");
    /** 配置面板传输模式按钮图集（42x56，格 14x14：列=能量/物品/流体，行=四态） */
    public static final ResourceLocation CONFIG_MODE_BUTTONS = TEN.id("textures/gui/panels/config_mode_buttons.png");
    /** 配置面板附加底图（60x85）：介入九宫格底图与内容钮之间 */
    public static final ResourceLocation PANEL_CONFIG_LEGACY = TEN.id("textures/gui/panels/panel_config_legacy.png");
    /** 升级面板附加底图（42x62）：内容区 38x58 每边外扩 2px */
    public static final ResourceLocation PANEL_UPGRADE_UNDERLAY = TEN.id("textures/gui/panels/panel_upgrade_underlay.png");
    /** 升级面板升级槽底图（18x18，替代借用的小型物品槽） */
    public static final ResourceLocation UPGRADE_SLOT_BG = TEN.id("textures/gui/slots/upgrade_slot.png");
    /** 升级 tab 头（26x26） */
    public static final ResourceLocation ICON_UPGRADE = TEN.id("textures/gui/icons/upgrade.png");
    /** 机器信息 tab 头（26x26） */
    public static final ResourceLocation ICON_INFO = TEN.id("textures/gui/icons/info.png");
    /** 能量信息 tab 头（26x26） */
    public static final ResourceLocation ICON_ENERGY_INFO = TEN.id("textures/gui/icons/energy_info.png");
    /** 红石模式 tab 三态（26x26） */
    public static final ResourceLocation ICON_REDSTONE_OFF = TEN.id("textures/gui/icons/redstone_off.png");
    public static final ResourceLocation ICON_REDSTONE_LOW = TEN.id("textures/gui/icons/redstone_low.png");
    public static final ResourceLocation ICON_REDSTONE_HIGH = TEN.id("textures/gui/icons/redstone_high.png");

    /** 雪碧图内精灵区域（像素坐标：u, v, 宽, 高）。 */
    public record SheetUV(int u, int v, int width, int height) {}

    // channels/entry_states.png 各格 UV：列0=未接入、列1=已接入；行0=normal、行1=hover
    public static final SheetUV MINI_OUT_NORMAL = new SheetUV(0, 0, 10, 15);
    public static final SheetUV MINI_OUT_HOVER = new SheetUV(0, 15, 10, 15);
    public static final SheetUV MINI_IN_NORMAL = new SheetUV(10, 0, 10, 15);
    public static final SheetUV MINI_IN_HOVER = new SheetUV(10, 15, 10, 15);

    // channels/buttons.png 各按钮 UV：normal 在行0、hover 在行1
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
