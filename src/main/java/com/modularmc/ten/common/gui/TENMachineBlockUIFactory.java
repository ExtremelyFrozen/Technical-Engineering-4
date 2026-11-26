package com.modularmc.ten.common.gui;

import com.modularmc.ten.TENConstants;
import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;
import com.modularmc.ten.api.option.FaceOption;
import com.modularmc.ten.api.option.MachineType;
import com.modularmc.ten.api.option.RedstoneMode;
import com.modularmc.ten.config.TENConfig;
import com.modularmc.ten.utils.ComponentHelper;
import com.modularmc.ten.utils.DisplayHelper;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.items.SlotItemHandler;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.FillDirection;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.FluidSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar;
import com.lowdragmc.lowdraglib2.gui.ui.elements.inventory.InventorySlots;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import dev.vfyjxf.taffy.style.TaffyPosition;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

public final class TENMachineBlockUIFactory {

    // 小按钮使用 LDLib2 通用组件图标。
    private static final ResourceLocation LDLIB_EYE = ResourceLocation.fromNamespaceAndPath("ldlib2", "textures/gui/icon/eye.png");
    private static final ResourceLocation LDLIB_EYE_OFF = ResourceLocation.fromNamespaceAndPath("ldlib2", "textures/gui/icon/eye_off.png");
    private static final ResourceLocation LDLIB_EDIT_ON = ResourceLocation.fromNamespaceAndPath("ldlib2", "textures/gui/icon/edit_on.png");
    private static final ResourceLocation LDLIB_EDIT_OFF = ResourceLocation.fromNamespaceAndPath("ldlib2", "textures/gui/icon/edit_off.png");
    private static final ResourceLocation LDLIB_PLAY_FILL = ResourceLocation.fromNamespaceAndPath("ldlib2", "textures/gui/icon/play_fill.png");
    private static final ResourceLocation LDLIB_PLAY_EMPTY = ResourceLocation.fromNamespaceAndPath("ldlib2", "textures/gui/icon/play_empty.png");

    private static final int TOGGLE_ON_COLOR = 0xFF62E36A;
    private static final int TOGGLE_ON_HOVER = 0xFFA3FFA8;
    private static final int TOGGLE_OFF_COLOR = 0xFF9AA0A6;
    private static final int TOGGLE_OFF_HOVER = 0xFFE0E4E8;

    /** 小按钮底盘：LDLib2 通用方形按钮贴图（32×32，1px 描边 + 1px 斜面）。 */
    private static final ResourceLocation TOGGLE_BG = ResourceLocation.fromNamespaceAndPath("ldlib2", "textures/gui/button.png");
    /**
     * 底盘走 {@link SpriteTexture#setBorder(int)} 九宫格伸缩，2px 描边/斜面按源像素 1:1 钉住。
     * 不可整体等比压进 12×12——32→12 会把 1px 边框压成亚像素糊边。
     */
    private static final int TOGGLE_BG_BORDER = 2;
    /** 底盘三态色调：常态略压暗、hover 全亮、按下再压暗，替代 hover 专用贴图。 */
    private static final int TOGGLE_BG_COLOR = 0xFFD2D2D2;
    private static final int TOGGLE_BG_HOVER = 0xFFFFFFFF;
    private static final int TOGGLE_BG_PRESSED = 0xFFA8A8A8;
    /** 图标相对按钮内缩比例：0.65 让 1px 斜面边框不被图标盖住。 */
    private static final float TOGGLE_ICON_SCALE = 0.65f;

    /** GUI 内容区 176×166；四边内缩 7px 不得遮盖（标题所在的顶栏除外）。 */
    private static final int UI_SIDE_MARGIN = 7;
    private static final int UI_WIDTH = 176;
    private static final int TOGGLE_SIZE = 12;
    /** 切换按钮统一所在空带：背包栏（y=83）之上、进度条（y=74..79）左侧/右侧。 */
    public static final int TOGGLE_Y = 69;
    /** 左侧按钮起始 x（贴 7px 边距）。 */
    public static final int TOGGLE_LEFT_X = UI_SIDE_MARGIN;
    /** 右侧按钮 x：右缘内缩 7px，占 157..168。 */
    public static final int TOGGLE_RIGHT_X = UI_WIDTH - UI_SIDE_MARGIN - TOGGLE_SIZE;

    private TENMachineBlockUIFactory() {}

    public static UIElement createRoot(ResourceLocation background) {
        return new UIElement()
                .layout(layout -> {
                    layout.width(176);
                    layout.height(166);
                })
                .style(style -> style.backgroundTexture(fullTexture(background, 176, 166)));
    }

    public static ModularUI buildModularUI(UIElement root, Player player) {
        return new ModularUI(
                UI.of(root, List.of(StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.MC))),
                player);
    }

    public static ModularUI createFallback(BlockUIMenuType.BlockUIHolder holder) {
        var root = new UIElement()
                .layout(layout -> {
                    layout.width(176);
                    layout.height(80);
                    layout.paddingAll(6);
                })
                .style(style -> style.backgroundTexture(fullTexture(TENConstants.LEGACY_SHEET, 176, 80)));
        root.addChild(new Label().setText(Component.literal("Missing block entity UI")));
        return new ModularUI(
                UI.of(root, List.of(StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.MC))),
                holder.player);
    }

    public static void addPlayerInventory(UIElement root) {
        // 容器左缘 7（背包栏槽位左移 1px），hotbar 上边距 4（快捷栏槽位上移 1px，左移同源）
        var inventory = absolute(new InventorySlots(), 7, 83, 162, 58);
        inventory.hotbar.getLayout().marginTop(4.0f);
        inventory.apply(slot -> {
            slot.style(style -> style.backgroundTexture(IGuiTexture.EMPTY));
            slot.slotStyle(style -> style.slotOverlay(IGuiTexture.EMPTY).showSlotOverlayOnlyEmpty(false));
        });
        root.addChild(inventory);
    }

    /** 右缘配置 tab 头尺寸（与左缘图标列 26×26 一致）。 */
    private static final int CONFIG_TAB_SIZE = 26;
    /** 九宫格源图固定尺寸（panel_sliced.png 本体）；角 4×4 固定，边/心源区随之确定。 */
    private static final int PANEL_SRC_SIZE = 128;
    /** 九宫格边框厚度：四角 4×4 固定，边/心拉伸适配任意目标宽高。 */
    private static final int CONFIG_PANEL_BORDER = 4;
    private static final int CONFIG_PANEL_SPEED = 6;
    /** 主 GUI 宽 176，tab 邻主 GUI 间隙 1px → tab 左缘 177。 */
    private static final int CONFIG_TAB_X = 176 + 1;
    /** 面板左缘锚定 x = tab 左缘（左→右生长，覆盖主 GUI 右半）。 */
    private static final int CONFIG_PANEL_LEFT_X = CONFIG_TAB_X;
    /** 面板锚定 y：与功率 tab（左缘 -27,27）平齐。 */
    private static final int CONFIG_PANEL_Y = 27;
    /** 低于此宽度不绘制面板（边角 4px×2 + 中缝最小可见宽）。 */
    private static final int CONFIG_PANEL_MIN_VISIBLE = 2 * CONFIG_PANEL_BORDER;
    /** 升级面板内容区到九宫格底图内边框的间距（四边均为 6px）。 */
    private static final int UPGRADE_CONTENT_GAP = 6;
    /**
     * 升级面板内容区原点 inset = border(4) + gap(6) = 10，仅供升级槽面板（尺寸/边框/槽位基准）。
     * 配置面板的内容坐标系原点是 CONFIG_UNDERLAY_XY（=8），两者不可混用。
     */
    private static final int UPGRADE_CONTENT_INSET = CONFIG_PANEL_BORDER + UPGRADE_CONTENT_GAP;
    /** 配置面板叠加层纹理尺寸（50×75）。 */
    private static final int OVERLAY_TEX_W = 50;
    private static final int OVERLAY_TEX_H = 75;
    /** 配置面板内边距：legacy 图形可见边缘到九宫格底图内边框，与升级面板附加底图对齐为 4px。 */
    private static final int CONFIG_CONTENT_GAP = 4;
    /**
     * 中间叠加层原点，同时是配置面板内容钮的坐标系原点：inset = border 4 + 内容间距 4 = 8。
     * 叠加层元素矩形与可见图形矩形重合；纹理 1:1 不拉伸，内容钮使用纹理像素坐标即可逐像素对齐。
     */
    private static final int CONFIG_UNDERLAY_XY = CONFIG_PANEL_BORDER + CONFIG_CONTENT_GAP;
    /** 展开面板目标尺寸 = 叠加层尺寸 + 四边 inset 8 → 66×91。 */
    private static final int CONFIG_PANEL_WIDTH = OVERLAY_TEX_W + 2 * CONFIG_UNDERLAY_XY;
    private static final int CONFIG_PANEL_HEIGHT = OVERLAY_TEX_H + 2 * CONFIG_UNDERLAY_XY;

    // ───── 升级槽 tab（右缘、配置 tab 上侧，与左缘机器信息 tab 对齐 y=0）─────
    /** 升级槽尺寸（18×18）。 */
    private static final int UPGRADE_SLOT_SIZE = 18;
    /** 升级槽 2x3 网格：槽间距/行距 2px。 */
    private static final int UPGRADE_SLOT_GAP = 2;
    private static final int UPGRADE_COLS = 2;
    private static final int UPGRADE_ROWS = 3;
    /** 升级面板内容区宽：2×18 + 列间 2 = 38。 */
    private static final int UPGRADE_CONTENT_W = UPGRADE_COLS * UPGRADE_SLOT_SIZE + (UPGRADE_COLS - 1) * UPGRADE_SLOT_GAP;
    /** 升级面板内容区高：3×18 + 行间 2×2 = 58。 */
    private static final int UPGRADE_CONTENT_H = UPGRADE_ROWS * UPGRADE_SLOT_SIZE + (UPGRADE_ROWS - 1) * UPGRADE_SLOT_GAP;
    /** 升级面板目标尺寸：内容区 + 四边 (gap 6 + border 4)，与配置面板同一九宫格 border4 基准。 */
    private static final int UPGRADE_PANEL_WIDTH = UPGRADE_CONTENT_W + 2 * UPGRADE_CONTENT_INSET;
    private static final int UPGRADE_PANEL_HEIGHT = UPGRADE_CONTENT_H + 2 * UPGRADE_CONTENT_INSET;
    /** 升级槽 tab 头 y：与左缘机器信息 tab（y=0）平齐，位于配置 tab（y=27）上侧。 */
    private static final int UPGRADE_TAB_Y = 0;
    private static final int UPGRADE_PANEL_Y = UPGRADE_TAB_Y;

    /** 升级面板附加底图尺寸（42x62，内容区每边外扩 2px）。 */
    private static final int PANEL_UPGRADE_UNDERLAY_W = 42;
    private static final int PANEL_UPGRADE_UNDERLAY_H = 62;
    /** 升级面板附加底图内边距：内容区原点 10 外扩 2px → (8,8)（右/下缘对称至 50，面板 58/78 内居中）。 */
    private static final int UPGRADE_PANEL_BORDER = UPGRADE_CONTENT_INSET - 2;

    // ───── 范围调节 tab（右缘第三个，y=54）─────
    /** 第三个右侧 tab 头 y：与左缘红石 tab 平齐，避开升级 (0) 与配置 (27)。 */
    private static final int ADJUST_TAB_Y = 54;
    /** 附加底图 64×90（6 行 × 2 个 12×12 占位格）贴 (8,8) → 面板 80×106。 */
    private static final int ADJUST_UNDERLAY_W = 64;
    private static final int ADJUST_UNDERLAY_H = 90;
    private static final int ADJUST_PANEL_WIDTH = ADJUST_UNDERLAY_W + 2 * CONFIG_UNDERLAY_XY;
    private static final int ADJUST_PANEL_HEIGHT = ADJUST_UNDERLAY_H + 2 * CONFIG_UNDERLAY_XY;
    /** 加减按钮：底图像素实测左列 x=5、右列 x=47，6 行 y=4+14i，中间 x=17..46 是数值区。 */
    private static final int ADJUST_BUTTON_SIZE = 12;
    private static final int ADJUST_ROWS = 6;
    private static final int ADJUST_PLUS_X = 5;
    private static final int ADJUST_MINUS_X = 47;
    private static final int ADJUST_ROW_Y = 4;
    private static final int ADJUST_ROW_STEP = 14;
    private static final int ADJUST_LABEL_X = 17;
    private static final int ADJUST_LABEL_W = 30;
    /** 中间 30px 里名称占左侧 18px、数值占右侧 12px：名称靠左，数字不参与左对齐。 */
    private static final int ADJUST_NAME_W = 18;
    /** 数值区本地化键：0..2 = X/Y/Z 半径，3..5 = X/Y/Z 偏移（中文见 TENLangHandler）。 */
    private static final String[] ADJUST_LANG_KEYS = {
            "kenergyengineering.info.adjust.range_x", "kenergyengineering.info.adjust.range_y", "kenergyengineering.info.adjust.range_z",
            "kenergyengineering.info.adjust.offset_x", "kenergyengineering.info.adjust.offset_y", "kenergyengineering.info.adjust.offset_z" };

    public static void addCommonSidebar(UIElement root, BlockUIMenuType.BlockUIHolder holder, CmMachineBlockEntity machine, UIState uiState) {
        // 侧栏 tab 使用独立 26×26 图标（icons/*.png）。
        root.addChild(textureElement(-27, 0, 26, 26, fullTexture(TENConstants.ICON_INFO, 26, 26), () -> ideaTooltips(holder), null, null, null));
        root.addChild(textureElement(-27, 27, 26, 26, fullTexture(TENConstants.ICON_ENERGY_INFO, 26, 26), () -> energyInfoTooltips(machine), null, null, null));

        root.addChild(dynamicTextureElement(
                -27, 54, 26, 26,
                () -> switch (machine.redstoneMode) {
                    case RedstoneMode.HIGH -> fullTexture(TENConstants.ICON_REDSTONE_HIGH, 26, 26);
                    case RedstoneMode.LOW -> fullTexture(TENConstants.ICON_REDSTONE_LOW, 26, 26);
                    default -> fullTexture(TENConstants.ICON_REDSTONE_OFF, 26, 26);
                },
                () -> redstoneTooltips(machine),
                null,
                () -> cycleRedstone(machine),
                null));

        // ───── 配置 tab（右缘）：常显 tab 头 + 平滑展开面板（尺寸见 CONFIG_PANEL_WIDTH/HEIGHT，九宫格拼接）─────
        // tab 邻主 GUI 1px（x=177）；y：无升级槽机器（频道等）配置 tab 上移对齐左缘机器信息 tab（y=0），
        // 有升级槽机器与功率 tab 平齐（y=27）避免与升级 tab 重叠；面板左缘钉住 x=177，从左往右生长。
        int configTabY = machine.supportsUpgradeSlots() ? CONFIG_PANEL_Y : 0;
        // 展开动画期间九宫格拼接：四角 4×4 固定，边/心随目标宽高拉伸填充（可适配任意目标尺寸）。
        // 修改 CONFIG_PANEL_WIDTH/HEIGHT 时需同步 syncSidebar/syncUpgrade 两套动画逻辑（宽高按比例同步对角生长，见 panelHeightFor）。
        var configButton = textureElement(CONFIG_TAB_X, configTabY, CONFIG_TAB_SIZE, CONFIG_TAB_SIZE,
                fullTexture(TENConstants.ICON_CONFIG, CONFIG_TAB_SIZE, CONFIG_TAB_SIZE),
                () -> controlTooltip(),
                () -> {
                    // 互斥：开配置面板先关升级面板
                    uiState.setUpgradeOpen(false);
                    uiState.setControlOpen(!uiState.isControlOpen());
                },
                null, null);

        var configPanel = absolute(new UIElement(), CONFIG_PANEL_LEFT_X, configTabY, CONFIG_PANEL_WIDTH, CONFIG_PANEL_HEIGHT);
        configPanel.setDisplay(false);
        // 拉伸底图（panel_sliced.png 九宫格）：先加入 → 下层
        List<PanelSlice> configPanelSlices = List.of(
                slice(0, 0, CONFIG_PANEL_BORDER, CONFIG_PANEL_BORDER, 0, 0),
                slice(PANEL_SRC_SIZE - CONFIG_PANEL_BORDER, 0, CONFIG_PANEL_BORDER, CONFIG_PANEL_BORDER, 2, 0),
                slice(0, PANEL_SRC_SIZE - CONFIG_PANEL_BORDER, CONFIG_PANEL_BORDER, CONFIG_PANEL_BORDER, 0, 2),
                slice(PANEL_SRC_SIZE - CONFIG_PANEL_BORDER, PANEL_SRC_SIZE - CONFIG_PANEL_BORDER, CONFIG_PANEL_BORDER, CONFIG_PANEL_BORDER, 2, 2),
                slice(CONFIG_PANEL_BORDER, 0, PANEL_SRC_SIZE - 2 * CONFIG_PANEL_BORDER, CONFIG_PANEL_BORDER, 1, 0),
                slice(CONFIG_PANEL_BORDER, PANEL_SRC_SIZE - CONFIG_PANEL_BORDER, PANEL_SRC_SIZE - 2 * CONFIG_PANEL_BORDER, CONFIG_PANEL_BORDER, 1, 2),
                slice(0, CONFIG_PANEL_BORDER, CONFIG_PANEL_BORDER, PANEL_SRC_SIZE - 2 * CONFIG_PANEL_BORDER, 0, 1),
                slice(PANEL_SRC_SIZE - CONFIG_PANEL_BORDER, CONFIG_PANEL_BORDER, CONFIG_PANEL_BORDER, PANEL_SRC_SIZE - 2 * CONFIG_PANEL_BORDER, 2, 1),
                slice(CONFIG_PANEL_BORDER, CONFIG_PANEL_BORDER, PANEL_SRC_SIZE - 2 * CONFIG_PANEL_BORDER, PANEL_SRC_SIZE - 2 * CONFIG_PANEL_BORDER, 1, 1));
        for (PanelSlice s : configPanelSlices) {
            configPanel.addChild(s.element());
        }
        // 附加底图位于九宫格与内容钮之间：50×75 纹理贴在 (8,8)，四边 inset 8，面板为 66×91。
        // 底图 zIndex=0，内容钮显式 zIndex=1；叠加层元素矩形与可见图形矩形重合。
        var configUnderlay = new UIElement().style(style -> style.backgroundTexture(
                fullTexture(TENConstants.PANEL_CONFIG_LEGACY, OVERLAY_TEX_W, OVERLAY_TEX_H)));
        configUnderlay.layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.left(CONFIG_UNDERLAY_XY);
            layout.top(CONFIG_UNDERLAY_XY);
            layout.width(OVERLAY_TEX_W);
            layout.height(OVERLAY_TEX_H);
        });
        configPanel.addChild(configUnderlay);
        // 内容钮坐标 = 叠加层原点 (8,8) + 纹理像素坐标；仅 fullyOpen 后可交互。
        // 面板底图 zIndex=0，内容钮显式 zIndex=1；不可删除 zIndex(1)，否则内容会被底图遮盖。
        // 原点取 CONFIG_UNDERLAY_XY（=8，叠加层矩形原点），不可用 UPGRADE_CONTENT_INSET（=10）——
        // 后者含升级面板附加底图自己的 2px 外扩，与配置面板无关，误用会让全部内容钮右下偏 2px。
        int overlayOriginX = CONFIG_PANEL_LEFT_X + CONFIG_UNDERLAY_XY;
        int overlayOriginY = configTabY + CONFIG_UNDERLAY_XY;

        // 类型按钮（能量/物品/流体）：CONFIG_MODE_BUTTONS 图集（42×56，格 14×14）——
        // 列=类型（左列流体 x=0 / 中列能量 x=14 / 右列物品 x=28）；行=状态：未悬停未选 y=0 / 悬停未选 y=14 /
        // 未悬停已选 y=28 / 悬停已选 y=42；syncTexture（TICK）实时刷新，hover 由 element.isHover() 取
        var modeIconTex = TENConstants.CONFIG_MODE_BUTTONS;
        var energyModeButton = textureElement(overlayOriginX + 2, overlayOriginY + 59, 14, 14,
                IGuiTexture.EMPTY, () -> energyModeTooltip(), () -> uiState.setSelectedTransferMode(0), null, null)
                .style(style -> style.zIndex(1));
        syncTexture(energyModeButton, () -> sprite(modeIconTex, 14,
                (uiState.getSelectedTransferMode() == 0 ? 2 : 0) * 14 + (energyModeButton.isHover() ? 14 : 0), 14, 14));
        var itemModeButton = textureElement(overlayOriginX + 18, overlayOriginY + 59, 14, 14,
                IGuiTexture.EMPTY, () -> itemModeTooltip(), () -> uiState.setSelectedTransferMode(1), null, null)
                .style(style -> style.zIndex(1));
        syncTexture(itemModeButton, () -> sprite(modeIconTex, 28,
                (uiState.getSelectedTransferMode() == 1 ? 2 : 0) * 14 + (itemModeButton.isHover() ? 14 : 0), 14, 14));
        var fluidModeButton = textureElement(overlayOriginX + 34, overlayOriginY + 59, 14, 14,
                IGuiTexture.EMPTY, () -> fluidModeTooltip(), () -> uiState.setSelectedTransferMode(2), null, null)
                .style(style -> style.zIndex(1));
        syncTexture(fluidModeButton, () -> sprite(modeIconTex, 0,
                (uiState.getSelectedTransferMode() == 2 ? 2 : 0) * 14 + (fluidModeButton.isHover() ? 14 : 0), 14, 14));

        var frontButton = faceModeElement(machine, uiState, overlayOriginX + 19, overlayOriginY + 17, 0, "kenergyengineering.info.front").style(style -> style.zIndex(1));
        var backButton = faceModeElement(machine, uiState, overlayOriginX + 33, overlayOriginY + 31, 1, "kenergyengineering.info.back").style(style -> style.zIndex(1));
        var leftButton = faceModeElement(machine, uiState, overlayOriginX + 5, overlayOriginY + 17, 2, "kenergyengineering.info.left").style(style -> style.zIndex(1));
        var rightButton = faceModeElement(machine, uiState, overlayOriginX + 33, overlayOriginY + 17, 3, "kenergyengineering.info.right").style(style -> style.zIndex(1));
        var upButton = faceModeElement(machine, uiState, overlayOriginX + 19, overlayOriginY + 3, 4, "kenergyengineering.info.up").style(style -> style.zIndex(1));
        var downButton = faceModeElement(machine, uiState, overlayOriginX + 19, overlayOriginY + 31, 5, "kenergyengineering.info.down").style(style -> style.zIndex(1));
        // close 热区（IGuiTexture.EMPTY，无图元可对齐）：钉面板右上角 10×10，同升级面板 close 锚法，
        // 不越出面板上缘；legacy 纹理右上区仅棋盘格空位，无 X 图元，故不进入纹理像素坐标系。
        var closeButton = textureElement(CONFIG_PANEL_LEFT_X + CONFIG_PANEL_WIDTH - 14, configTabY, 10, 10, IGuiTexture.EMPTY,
                () -> controlTooltip(), () -> uiState.setControlOpen(false), null, null)
                .style(style -> style.zIndex(1));

        root.addChild(configPanel);
        // 点击面板空白处收起面板：CLICK target 为面板子树内背景元素（切片/underlay），
        // 冒泡路径含 configPanel；内容钮/槽位是 root 直接子元素，点击不经过 panel 子树，不会误触发。
        // 点击九宫格切片或叠加层空白处收起；点击 configUnderlay 本体不收起。
        configPanel.addEventListener(UIEvents.CLICK, event -> {
            if (event.button == 0 && !isUnderlayTarget(event.target, configUnderlay)) {
                uiState.setControlOpen(false);
            }
        });
        root.addChild(configButton);
        root.addChild(energyModeButton);
        root.addChild(itemModeButton);
        root.addChild(fluidModeButton);
        root.addChild(frontButton);
        root.addChild(backButton);
        root.addChild(leftButton);
        root.addChild(rightButton);
        root.addChild(upButton);
        root.addChild(downButton);
        root.addChild(closeButton);

        List<UIElement> configContents = new ArrayList<>(List.of(energyModeButton, itemModeButton, fluidModeButton,
                frontButton, backButton, leftButton, rightButton, upButton, downButton, closeButton));
        // 叠加层与内容层统一显隐，仅在面板完全展开后显示。
        configContents.add(configUnderlay);

        // 展开状态：-1 收起完成；0..目标宽展开中；目标宽展开完成（fullyOpen 以 >=CONFIG_PANEL_WIDTH 判定，复用终值）
        int[] panelWidth = { uiState.isControlOpen() ? -1 : 0 };

        Runnable syncSidebar = () -> {
            boolean open = uiState.isControlOpen();
            int w = panelWidth[0];
            if (open) {
                // 收起→展开：面板显示并从 tab 左上角对角生长（宽高按比例同步）；收起完成瞬间（-1）面板隐藏
                if (w < 0) {
                    panelWidth[0] = 0;
                    configPanel.setDisplay(false);
                    for (UIElement content : configContents) {
                        content.setDisplay(false);
                    }
                    applyPanelSlices(configPanelSlices, 0, 0);
                } else if (w < CONFIG_PANEL_WIDTH) {
                    int next = Math.min(w + TENConfig.CLIENT.panelAnimationSpeed("config"), CONFIG_PANEL_WIDTH);
                    panelWidth[0] = next;
                    configPanel.setDisplay(next >= CONFIG_PANEL_MIN_VISIBLE);
                    applyPanelSlices(configPanelSlices, next, panelHeightFor(next, CONFIG_PANEL_WIDTH, CONFIG_PANEL_HEIGHT));
                } else {
                    configPanel.setDisplay(true);
                    applyPanelSlices(configPanelSlices, CONFIG_PANEL_WIDTH, CONFIG_PANEL_HEIGHT);
                }
                boolean fullyOpen = panelWidth[0] >= CONFIG_PANEL_WIDTH;
                uiState.setPanelFullyOpen(fullyOpen);
                for (UIElement content : configContents) {
                    content.setDisplay(fullyOpen);
                }
            } else {
                // 展开→收起：内容先隐藏，面板向 tab 左上角对角收窄至 0 后隐藏
                for (UIElement content : configContents) {
                    content.setDisplay(false);
                }
                if (w > 0) {
                    int next = Math.max(w - TENConfig.CLIENT.panelAnimationSpeed("config"), 0);
                    panelWidth[0] = next;
                    configPanel.setDisplay(next >= CONFIG_PANEL_MIN_VISIBLE);
                    applyPanelSlices(configPanelSlices, next, panelHeightFor(next, CONFIG_PANEL_WIDTH, CONFIG_PANEL_HEIGHT));
                } else {
                    panelWidth[0] = -1;
                    configPanel.setDisplay(false);
                }
            }
            // tab 显隐与动画协调：面板宽度尚未盖住 tab 区域（<26px）时保持可见，
            // 面板盖过后隐藏；升级面板展开期间其矩形会覆盖本 tab，一并隐藏
            configButton.setDisplay(!uiState.isUpgradeOpen() && panelWidth[0] < CONFIG_TAB_SIZE);
        };
        syncSidebar.run();
        root.addEventListener(UIEvents.TICK, event -> syncSidebar.run());
        // 第三个右侧展开面板：仅接入范围调节的机器注册（方块破坏器/成型器），和配置/升级面板三选一互斥
        if (machine.supportsRangeAdjust()) {
            addAdjustTab(root, machine, uiState);
        }
    }

    /**
     * 空位收敛判定：event.target 是否位于中间叠加层元素上（自身或其子元素）。
     * 叠加层为非交互背景，命中它或其子树时不算「点空白」，不触发收起。
     */
    private static boolean isUnderlayTarget(UIElement target, UIElement underlay) {
        for (UIElement e = target; e != null; e = e.getParent()) {
            if (e == underlay) {
                return true;
            }
        }
        return false;
    }

    /** 对角展开的高度插值：宽度按比例折算高度（从 tab 左上角向右下生长/收回）。 */
    private static int panelHeightFor(int width, int fullWidth, int fullHeight) {
        if (width <= 0) {
            return 0;
        }
        return Math.min(fullHeight, width * fullHeight / fullWidth);
    }

    /**
     * 升级槽 tab（右缘，配置 tab 上侧，与左缘机器信息 tab y=0 对齐）：
     * 面板内容为 2×3 六升级槽（18×18，间距 2px），内容区与九宫格底图内边框间距恒为 6px；
     * 面板尺寸由内容区 + 四边 border4+gap6 反推。
     * 由 buildMachineUI 在 supportsUpgradeSlots() 时挂载。
     * 面板展开态只存在于客户端 UIState，而快捷移动在服务端容器执行，故开合时由 C→S RPC
     * 同步到服务端，升级槽据此决定能否接收 quickMove（展开时优先，收起时排除）。
     */
    public static void addUpgradeSlotsTab(UIElement root, CmMachineBlockEntity machine, UIState uiState,
                                          Player player) {
        var upgradeButton = textureElement(CONFIG_TAB_X, UPGRADE_TAB_Y, CONFIG_TAB_SIZE, CONFIG_TAB_SIZE,
                fullTexture(TENConstants.ICON_UPGRADE, CONFIG_TAB_SIZE, CONFIG_TAB_SIZE),
                () -> List.of(ComponentHelper.translated("kenergyengineering.info.bar_upgrade")),
                () -> {
                    // 互斥：开升级面板先关配置面板
                    uiState.setControlOpen(false);
                    uiState.setUpgradeOpen(!uiState.isUpgradeOpen());
                },
                null, null).style(style -> style.zIndex(1));

        // 面板 zIndex=1，盖过配置面板（zIndex=0）的互斥动画残影。
        // 槽内物品渲染 z = 面板 zIndex + 32，故面板 1 对应物品 z=33；
        // 提高面板 zIndex 会使物品渲染更远，可能被深度缓冲剔除，必须保持当前值。
        var upgradePanel = absolute(new UIElement(), CONFIG_PANEL_LEFT_X, UPGRADE_PANEL_Y, UPGRADE_PANEL_WIDTH, UPGRADE_PANEL_HEIGHT)
                .style(style -> style.zIndex(1));
        upgradePanel.setDisplay(false);
        List<PanelSlice> upgradePanelSlices = panelSlices(UPGRADE_PANEL_WIDTH, UPGRADE_PANEL_HEIGHT);
        for (PanelSlice s : upgradePanelSlices) {
            upgradePanel.addChild(s.element());
        }
        // 附加底图位于九宫格与升级槽之间：42×62 覆盖内容区 38×58，每边外扩 2px。
        var upgradeUnderlay = new UIElement().style(style -> style.backgroundTexture(
                fullTexture(TENConstants.PANEL_UPGRADE_UNDERLAY, PANEL_UPGRADE_UNDERLAY_W, PANEL_UPGRADE_UNDERLAY_H)));
        upgradeUnderlay.layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.left(UPGRADE_PANEL_BORDER);
            layout.top(UPGRADE_PANEL_BORDER);
            layout.width(PANEL_UPGRADE_UNDERLAY_W);
            layout.height(PANEL_UPGRADE_UNDERLAY_H);
        });
        upgradePanel.addChild(upgradeUnderlay);

        // 六升级槽挂在面板子树内：内容区原点 = 面板原点 + (border4+gap6)=10，2×3 网格步长 18+2。
        // 槽 zIndex=0、面板 zIndex=1；物品渲染 z = 1 + 0 + 32 = 33，与主 GUI 机器槽 z=32 同安全档。
        int slotBaseX = UPGRADE_CONTENT_INSET;
        int slotBaseY = UPGRADE_CONTENT_INSET;
        int upgradeSlots = machine.upgradeHandler.getSlots();
        List<UIElement> upgradeContents = new ArrayList<>();

        upgradePanel.addEventListener(UIEvents.CLICK, event -> {
            // 点击升级槽或附加底图不收起；点击面板其余空白处收起。
            if (event.button == 0 && !(event.target instanceof ItemSlot) && !isUnderlayTarget(event.target, upgradeUnderlay)) {
                uiState.setUpgradeOpen(false);
            }
        });

        for (int i = 0; i < upgradeSlots; i++) {
            int col = i % UPGRADE_COLS;
            int row = i / UPGRADE_COLS;
            int sx = slotBaseX + col * (UPGRADE_SLOT_SIZE + UPGRADE_SLOT_GAP);
            int sy = slotBaseY + row * (UPGRADE_SLOT_SIZE + UPGRADE_SLOT_GAP);
            ItemSlot slot = upgradePanelSlot(machine, player, i, sx, sy);
            upgradePanel.addChild(slot);
            upgradeContents.add(slot);
        }

        root.addChild(upgradePanel);
        root.addChild(upgradeButton);
        // 升级面板 close 钮（右上角，仿配置面板）：面板原点 + (面板宽-14, 0)，贴顶避开与 2×3 槽区重叠；
        // zIndex 2 > 面板 1：保证不被面板底图遮挡且事件命中优先于面板的空白收起拦截
        var upgradeCloseButton = textureElement(CONFIG_PANEL_LEFT_X + UPGRADE_PANEL_WIDTH - 14, UPGRADE_PANEL_Y, 10, 10,
                IGuiTexture.EMPTY, () -> List.of(ComponentHelper.translated("kenergyengineering.info.bar_upgrade")),
                () -> uiState.setUpgradeOpen(false), null, null).style(style -> style.zIndex(2));
        root.addChild(upgradeCloseButton);
        upgradeContents.add(upgradeCloseButton);
        // 叠加层与内容层统一显隐，仅在面板完全展开后显示。
        upgradeContents.add(upgradeUnderlay);

        // 与配置面板同构的动画状态机：-1 收起完成；0..目标宽 展开中；目标宽 展开完成（从 tab 左上角对角生长）
        int[] panelWidth = { uiState.isUpgradeOpen() ? -1 : 0 };
        // 展开态上报幂等哨兵：-1 未上报，0 收起，1 展开；避免 TICK 每帧发包。
        // 首次执行随 GUI 构建上报初值（跨次打开记住的展开态也要让服务端知道）。
        int[] reportedOpen = { -1 };
        // 升级面板展开完成时是否已请求过数据（每次打开重置）：rpcToServer 请求服务端推送全量升级数据
        boolean[] upgradeDataRequested = { false };
        Runnable syncUpgrade = () -> {
            boolean open = uiState.isUpgradeOpen();
            int desiredOpen = open ? 1 : 0;
            if (player.level().isClientSide() && reportedOpen[0] != desiredOpen) {
                reportedOpen[0] = desiredOpen;
                machine.rpcToServer("rpcSetUpgradePanelOpen", desiredOpen);
            }
            int w = panelWidth[0];
            if (open) {
                if (w < 0) {
                    panelWidth[0] = 0;
                    upgradePanel.setDisplay(false);
                    for (UIElement content : upgradeContents) {
                        content.setDisplay(false);
                    }
                    applyPanelSlices(upgradePanelSlices, 0, 0);
                } else if (w < UPGRADE_PANEL_WIDTH) {
                    int next = Math.min(w + TENConfig.CLIENT.panelAnimationSpeed("upgrade"), UPGRADE_PANEL_WIDTH);
                    panelWidth[0] = next;
                    upgradePanel.setDisplay(next >= CONFIG_PANEL_MIN_VISIBLE);
                    applyPanelSlices(upgradePanelSlices, next, panelHeightFor(next, UPGRADE_PANEL_WIDTH, UPGRADE_PANEL_HEIGHT));
                } else {
                    upgradePanel.setDisplay(true);
                    applyPanelSlices(upgradePanelSlices, UPGRADE_PANEL_WIDTH, UPGRADE_PANEL_HEIGHT);
                }
                boolean fullyOpen = panelWidth[0] >= UPGRADE_PANEL_WIDTH;
                uiState.setUpgradePanelFullyOpen(fullyOpen);
                for (UIElement content : upgradeContents) {
                    content.setDisplay(fullyOpen);
                }
                // 展开完成即请求全量升级数据（C→S，服务端 rpcToTracking 广播）：
                // 客户端 upgradeHandler 不经 readTileData/更新包同步，
                // 由本请求与服务端变化广播（rpcSyncUpgradeData）维持数据。
                if (fullyOpen && !upgradeDataRequested[0]) {
                    upgradeDataRequested[0] = true;
                    // 面板全开时容器必已就绪，重发一次展开态，兜底 GUI 构建期的首次上报
                    machine.rpcToServer("rpcSetUpgradePanelOpen", 1);
                    machine.rpcToServer("rpcRequestUpgradeData");
                }
            } else {
                upgradeDataRequested[0] = false;
                for (UIElement content : upgradeContents) {
                    content.setDisplay(false);
                }
                if (w > 0) {
                    int next = Math.max(w - TENConfig.CLIENT.panelAnimationSpeed("upgrade"), 0);
                    panelWidth[0] = next;
                    upgradePanel.setDisplay(next >= CONFIG_PANEL_MIN_VISIBLE);
                    applyPanelSlices(upgradePanelSlices, next, panelHeightFor(next, UPGRADE_PANEL_WIDTH, UPGRADE_PANEL_HEIGHT));
                } else {
                    panelWidth[0] = -1;
                    upgradePanel.setDisplay(false);
                }
            }
            // tab 显隐与动画协调：面板宽度尚未盖住 tab 区域（<26px）时保持可见
            upgradeButton.setDisplay(panelWidth[0] < CONFIG_TAB_SIZE);
        };
        syncUpgrade.run();
        root.addEventListener(UIEvents.TICK, event -> syncUpgrade.run());
    }

    /**
     * 范围调节 tab（右缘第三个，y=54）：展开 80×106 面板，6 行各一对 12×12 加减按钮，
     * 分别调节 XYZ 跨度与 XYZ 相对偏移，点击经 rpcAdjustRange 发包。上下限由设备决定
     * （{@code adjustRangeCap} / {@code adjustOffsetCap}），只读行不出按钮、数值位显示「自动」。
     * 与配置/升级面板三选一互斥，展开动画复用同一套九宫格与 panelHeightFor。
     */
    public static void addAdjustTab(UIElement root, CmMachineBlockEntity machine, UIState uiState) {
        var adjustButton = textureElement(CONFIG_TAB_X, ADJUST_TAB_Y, CONFIG_TAB_SIZE, CONFIG_TAB_SIZE,
                fullTexture(TENConstants.ICON_ADJUST, CONFIG_TAB_SIZE, CONFIG_TAB_SIZE),
                () -> List.of(ComponentHelper.translated(ComponentHelper.GOLD, "kenergyengineering.info.adjust.tab")),
                () -> uiState.setAdjustOpen(!uiState.isAdjustOpen()),
                null, null).style(style -> style.zIndex(1));

        var adjustPanel = absolute(new UIElement(), CONFIG_PANEL_LEFT_X, ADJUST_TAB_Y,
                ADJUST_PANEL_WIDTH, ADJUST_PANEL_HEIGHT);
        adjustPanel.setDisplay(false);
        List<PanelSlice> adjustSlices = panelSlices(ADJUST_PANEL_WIDTH, ADJUST_PANEL_HEIGHT);
        for (PanelSlice s : adjustSlices) {
            adjustPanel.addChild(s.element());
        }
        // 附加底图贴在九宫格之上、内容之下；占位格坐标按底图像素写，1:1 不拉伸。
        var adjustUnderlay = new UIElement().style(style -> style.backgroundTexture(
                fullTexture(TENConstants.PANEL_ADJUST, ADJUST_UNDERLAY_W, ADJUST_UNDERLAY_H)));
        adjustUnderlay.layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.left(CONFIG_UNDERLAY_XY);
            layout.top(CONFIG_UNDERLAY_XY);
            layout.width(ADJUST_UNDERLAY_W);
            layout.height(ADJUST_UNDERLAY_H);
        });
        adjustPanel.addChild(adjustUnderlay);
        adjustPanel.addEventListener(UIEvents.CLICK, event -> {
            if (event.button == 0 && !isUnderlayTarget(event.target, adjustUnderlay)) {
                uiState.setAdjustOpen(false);
            }
        });

        int originX = CONFIG_PANEL_LEFT_X + CONFIG_UNDERLAY_XY;
        int originY = ADJUST_TAB_Y + CONFIG_UNDERLAY_XY;
        List<UIElement> adjustContents = new ArrayList<>();
        adjustContents.add(adjustUnderlay);

        for (int row = 0; row < ADJUST_ROWS; row++) {
            final int index = row;
            int y = originY + ADJUST_ROW_Y + row * ADJUST_ROW_STEP;
            // 只读行（采矿场 / 信标的 Y 半径与 Y 偏移）：数值位显示「自动」；按钮摘不摘由机型决定
            boolean locked = machine.isAdjustRowLocked(index);
            if (!machine.hidesAdjustRowButton(index)) {
                var plus = adjustValueButton(machine, index, 1, originX + ADJUST_PLUS_X, y);
                var minus = adjustValueButton(machine, index, -1, originX + ADJUST_MINUS_X, y);
                root.addChild(plus);
                root.addChild(minus);
                adjustContents.add(plus);
                adjustContents.add(minus);
            }
            // 名称与数字分开排：名称上下居中且靠左，数字单独靠右，不受左对齐约束。
            var name = new Label();
            name.textStyle(style -> style.textColor(0xFFB5B5B5).textShadow(false).fontSize(7)
                    .textAlignVertical(Vertical.CENTER).textAlignHorizontal(Horizontal.LEFT));
            name.setText(ComponentHelper.translated(ADJUST_LANG_KEYS[index]));
            absolute(name, originX + ADJUST_LABEL_X, y, ADJUST_NAME_W, ADJUST_BUTTON_SIZE)
                    .style(style -> style.zIndex(1));

            var value = new Label();
            value.textStyle(style -> style.textColor(0xFFB5B5B5).textShadow(false).fontSize(7)
                    .textAlignVertical(Vertical.CENTER).textAlignHorizontal(Horizontal.RIGHT));
            Runnable refresh = () -> value.setText(locked ? ComponentHelper.translated("kenergyengineering.info.adjust.auto") : Component.literal(machine.adjustDisplayValue(index)));
            refresh.run();
            value.addEventListener(UIEvents.TICK, event -> refresh.run());
            // 只读行没有加减钮，数值位左移并加宽，容得下「自动」两个字
            absolute(value, originX + ADJUST_LABEL_X + (locked ? ADJUST_NAME_W - 6 : ADJUST_NAME_W), y,
                    ADJUST_LABEL_W - ADJUST_NAME_W + (locked ? 6 : 0), ADJUST_BUTTON_SIZE)
                    .style(style -> style.zIndex(1));

            root.addChild(name);
            root.addChild(value);
            adjustContents.add(name);
            adjustContents.add(value);
        }

        var adjustClose = textureElement(CONFIG_PANEL_LEFT_X + ADJUST_PANEL_WIDTH - 14, ADJUST_TAB_Y, 10, 10,
                IGuiTexture.EMPTY, null,
                () -> uiState.setAdjustOpen(false), null, null).style(style -> style.zIndex(2));
        root.addChild(adjustClose);
        adjustContents.add(adjustClose);

        root.addChild(adjustPanel);
        root.addChild(adjustButton);

        int[] panelWidth = { uiState.isAdjustOpen() ? -1 : 0 };
        Runnable syncAdjust = () -> {
            boolean open = uiState.isAdjustOpen();
            int w = panelWidth[0];
            if (open) {
                if (w < 0) {
                    panelWidth[0] = 0;
                    adjustPanel.setDisplay(false);
                    for (UIElement content : adjustContents) content.setDisplay(false);
                    applyPanelSlices(adjustSlices, 0, 0);
                } else if (w < ADJUST_PANEL_WIDTH) {
                    int next = Math.min(w + TENConfig.CLIENT.panelAnimationSpeed("adjust"), ADJUST_PANEL_WIDTH);
                    panelWidth[0] = next;
                    adjustPanel.setDisplay(next >= CONFIG_PANEL_MIN_VISIBLE);
                    applyPanelSlices(adjustSlices, next, panelHeightFor(next, ADJUST_PANEL_WIDTH, ADJUST_PANEL_HEIGHT));
                } else {
                    adjustPanel.setDisplay(true);
                    applyPanelSlices(adjustSlices, ADJUST_PANEL_WIDTH, ADJUST_PANEL_HEIGHT);
                }
                boolean fullyOpen = panelWidth[0] >= ADJUST_PANEL_WIDTH;
                for (UIElement content : adjustContents) content.setDisplay(fullyOpen);
            } else {
                for (UIElement content : adjustContents) content.setDisplay(false);
                if (w > 0) {
                    int next = Math.max(w - TENConfig.CLIENT.panelAnimationSpeed("adjust"), 0);
                    panelWidth[0] = next;
                    adjustPanel.setDisplay(next >= CONFIG_PANEL_MIN_VISIBLE);
                    applyPanelSlices(adjustSlices, next, panelHeightFor(next, ADJUST_PANEL_WIDTH, ADJUST_PANEL_HEIGHT));
                } else {
                    panelWidth[0] = -1;
                    adjustPanel.setDisplay(false);
                }
            }
            // 配置/升级面板展开时其矩形会盖住本 tab（y=27..118 / y=0..78），一并隐藏
            adjustButton.setDisplay(!uiState.isControlOpen() && !uiState.isUpgradeOpen() && panelWidth[0] < CONFIG_TAB_SIZE);
        };
        syncAdjust.run();
        root.addEventListener(UIEvents.TICK, event -> syncAdjust.run());
    }

    /** 一行里的加/减按钮：24×24 图集取 12×12，上排默认、下排悬停；无 tooltip。 */
    private static UIElement adjustValueButton(CmMachineBlockEntity machine, int row, int delta, int x, int y) {
        var button = textureElement(x, y, ADJUST_BUTTON_SIZE, ADJUST_BUTTON_SIZE, IGuiTexture.EMPTY,
                null, null, null, null).style(style -> style.zIndex(1));
        button.addEventListener(UIEvents.MOUSE_DOWN, event -> {
            if (event.button != 0) return;
            // 只读行（Y 轴自动）的按钮留在原位但点了没反应；服务端 rpcAdjustRange 另有同一条拒绝
            if (machine.isAdjustRowLocked(row)) return;
            // Shift 沿按钮方向直接打到端点：加号 → 最大、减号 → 最小；普通点击步进 ±1（半径 ±0.5）。
            int mode = event.isShiftDown() ? delta : 0;
            machine.rpcToServer("rpcAdjustRange", row, delta, mode);
        });
        syncTexture(button, () -> sprite(TENConstants.ADJUST_BUTTONS,
                delta > 0 ? 0 : ADJUST_BUTTON_SIZE,
                button.isHover() ? ADJUST_BUTTON_SIZE : 0,
                ADJUST_BUTTON_SIZE, ADJUST_BUTTON_SIZE));
        return button;
    }

    private static List<PanelSlice> panelSlices(int width, int height) {
        return List.of(
                slice(0, 0, CONFIG_PANEL_BORDER, CONFIG_PANEL_BORDER, 0, 0),
                slice(PANEL_SRC_SIZE - CONFIG_PANEL_BORDER, 0, CONFIG_PANEL_BORDER, CONFIG_PANEL_BORDER, 2, 0),
                slice(0, PANEL_SRC_SIZE - CONFIG_PANEL_BORDER, CONFIG_PANEL_BORDER, CONFIG_PANEL_BORDER, 0, 2),
                slice(PANEL_SRC_SIZE - CONFIG_PANEL_BORDER, PANEL_SRC_SIZE - CONFIG_PANEL_BORDER, CONFIG_PANEL_BORDER, CONFIG_PANEL_BORDER, 2, 2),
                slice(CONFIG_PANEL_BORDER, 0, PANEL_SRC_SIZE - 2 * CONFIG_PANEL_BORDER, CONFIG_PANEL_BORDER, 1, 0),
                slice(CONFIG_PANEL_BORDER, PANEL_SRC_SIZE - CONFIG_PANEL_BORDER, PANEL_SRC_SIZE - 2 * CONFIG_PANEL_BORDER, CONFIG_PANEL_BORDER, 1, 2),
                slice(0, CONFIG_PANEL_BORDER, CONFIG_PANEL_BORDER, PANEL_SRC_SIZE - 2 * CONFIG_PANEL_BORDER, 0, 1),
                slice(PANEL_SRC_SIZE - CONFIG_PANEL_BORDER, CONFIG_PANEL_BORDER, CONFIG_PANEL_BORDER, PANEL_SRC_SIZE - 2 * CONFIG_PANEL_BORDER, 2, 1),
                slice(CONFIG_PANEL_BORDER, CONFIG_PANEL_BORDER, PANEL_SRC_SIZE - 2 * CONFIG_PANEL_BORDER, PANEL_SRC_SIZE - 2 * CONFIG_PANEL_BORDER, 1, 1));
    }

    /**
     * 升级面板内 18×18 槽：升级槽专用底图 + quickMovePriority(1000) + 空槽 tooltip。
     * mayPlace 叠加展开态门控：收起时把升级槽从快捷移动候选里剔除，物品不会被 Shift 进不可见的槽。
     * 展开时优先由「升级槽先于机器槽注册」保证（见 buildMachineUI），priority 是客户端样式计算后的加成：
     * 服务端 UI 树不跑 StyleEngine，slotStyle 的声明值在容器侧读不到。
     */
    private static ItemSlot upgradePanelSlot(CmMachineBlockEntity machine, Player player, int index, int x, int y) {
        SlotItemHandler slot = new SlotItemHandler(machine.upgradeHandler, index, 0, 0) {

            @Override
            public boolean mayPlace(net.minecraft.world.item.ItemStack stack) {
                // 客户端只做点击预测（隐藏槽本来就点不到），门控只作用于服务端容器的 quickMoveStack
                if (!player.level().isClientSide() && !machine.isUpgradePanelOpenFor(player)) {
                    return false;
                }
                return super.mayPlace(stack);
            }
        };
        // 升级槽使用专用底图 UPGRADE_SLOT_BG。
        var itemSlot = itemSlot(slot, x, y, false, UPGRADE_SLOT_SIZE, fullTexture(TENConstants.UPGRADE_SLOT_BG, UPGRADE_SLOT_SIZE, UPGRADE_SLOT_SIZE));
        itemSlot.slotStyle(style -> style.quickMovePriority(1000));
        itemSlot.addEventListener(UIEvents.HOVER_TOOLTIPS, event -> {
            if (slot.getItem().isEmpty()) {
                event.hoverTooltips = new HoverTooltips(List.of(emptyUpgradeSlotTooltip()), null, null, null);
            }
        });
        return itemSlot;
    }

    /**
     * 九宫格切片：源矩形（panel_sliced.png 内）+ 列/行角色。
     * colRole: 0=左列（固定宽）、1=中列（伸缩）、2=右列（固定宽）；rowRole 同理。
     */
    private record PanelSlice(UIElement element, SpriteTexture texture, int srcU, int srcV, int srcW, int srcH,
                              int colRole, int rowRole) {}

    private static PanelSlice slice(int srcU, int srcV, int srcW, int srcH, int colRole, int rowRole) {
        var tex = SpriteTexture.of(TENConstants.PANEL_SLICED).setSprite(srcU, srcV, srcW, srcH);
        var element = new UIElement().style(style -> style.backgroundTexture(tex));
        return new PanelSlice(element, tex, srcU, srcV, srcW, srcH, colRole, rowRole);
    }

    /**
     * 应用面板尺寸（左缘钉住，左→右生长）：标准拉伸式九宫格，可适配任意目标宽高。
     * 四角 srcW×srcH 固定 1:1；边/心源区固定（srcU/srcV/srcW/srcH 不变），
     * 目标列宽 = width-2b / 行高 = height-2b，由背景纹理拉伸绘制填充。
     */
    private static void applyPanelSlices(List<PanelSlice> slices, int width, int height) {
        int b = CONFIG_PANEL_BORDER;
        int midW = Math.max(width - 2 * b, 0);
        int midH = Math.max(height - 2 * b, 0);
        for (PanelSlice s : slices) {
            int x = switch (s.colRole()) {
                case 0 -> 0;
                case 1 -> b;
                default -> width - b;
            };
            int w = s.colRole() == 1 ? midW : b;
            int y = switch (s.rowRole()) {
                case 0 -> 0;
                case 1 -> b;
                default -> height - b;
            };
            int h = s.rowRole() == 1 ? midH : b;
            s.element().layout(layout -> {
                layout.positionType(TaffyPosition.ABSOLUTE);
                layout.left(x);
                layout.top(y);
                layout.width(w);
                layout.height(h);
            });
        }
    }

    /** 18×18 无底图槽，槽框由机器背景 PNG 绘制。 */
    public static ItemSlot machineSlot(CmMachineBlockEntity machine, int index, int x, int y) {
        return machineSlot(machine, index, x, y, 18, IGuiTexture.EMPTY);
    }

    /**
     * 18x18 机器槽 + modular 小槽底图（ITEM_SLOT_SMALL）。
     * 使用 MACHINE_GUI 空面板的机器必须用此变体补充槽框。
     */
    public static ItemSlot machineSlotModular(CmMachineBlockEntity machine, int index, int x, int y) {
        return machineSlot(machine, index, x, y, 18, fullTexture(TENConstants.ITEM_SLOT_SMALL, 18, 18));
    }

    /**
     * 26x26 大槽（ITEM_SLOT_LARGE）：26x26 区域可交互，5px padding 使 16x16 内容居中。
     */
    public static ItemSlot machineSlotLarge(CmMachineBlockEntity machine, int index, int x, int y) {
        return machineSlot(machine, index, x, y, 26, fullTexture(TENConstants.ITEM_SLOT_LARGE, 26, 26));
    }

    /**
     * 18x18 充/放电槽（modular 素材族）：chargeIn=true 用 ITEM_SLOT_SMALL_CHARGE（绿流入），
     * false 用 ITEM_SLOT_SMALL_DISCHARGE（红流出）。
     */
    public static ItemSlot machineSlotPower(CmMachineBlockEntity machine, int index, int x, int y, boolean chargeIn) {
        var texture = chargeIn ? TENConstants.ITEM_SLOT_SMALL_CHARGE : TENConstants.ITEM_SLOT_SMALL_DISCHARGE;
        return machineSlot(machine, index, x, y, 18, fullTexture(texture, 18, 18));
    }

    /** 任意 Slot 的 18x18 modular 小槽背景槽（如 Pipe filterContainer 来源）。 */
    public static ItemSlot itemSlotModular(Slot slot, int x, int y) {
        return itemSlot(slot, x, y, false, 18, fullTexture(TENConstants.ITEM_SLOT_SMALL, 18, 18));
    }

    /**
     * 18x18 频道物品槽：绑定 ChannelItemHandlerFacade（接入后动态指向共享存储），
     * ITEM_SLOT_SMALL 底图，末影箱动态堆叠上限。
     */
    public static ItemSlot channelItemSlot(com.modularmc.ten.common.blockentity.channel.AbstractChannelBlockEntity channel, int index, int x, int y) {
        Slot slot = new SlotItemHandler(channel.getChannelItemFacade(), index, 0, 0) {

            @Override
            public boolean mayPlace(net.minecraft.world.item.ItemStack stack) {
                var type = channel.slotType(getSlotIndex());
                return type.canIn() && channel.valid(getSlotIndex(), stack) && super.mayPlace(stack);
            }

            @Override
            public int getMaxStackSize(net.minecraft.world.item.ItemStack stack) {
                // 末影箱动态堆叠：上限=共享槽位上限（64×成员数），不按物品原版 64 封顶
                return getMaxStackSize();
            }
        };
        return itemSlot(slot, x, y, false, 18, fullTexture(TENConstants.ITEM_SLOT_SMALL, 18, 18));
    }

    private static ItemSlot machineSlot(CmMachineBlockEntity machine, int index, int x, int y, int size, IGuiTexture background) {
        Slot slot = new SlotItemHandler(machine.itemHandler, index, 0, 0) {

            @Override
            public boolean mayPlace(net.minecraft.world.item.ItemStack stack) {
                var type = machine.slotType(getSlotIndex());
                return type.canIn() && machine.canPlaceItem(getSlotIndex(), stack) && super.mayPlace(stack);
            }

            @Override
            public int getMaxStackSize(net.minecraft.world.item.ItemStack stack) {
                // 上限=底层 itemHandler 槽位上限（动态容量），不按物品原版 64 封顶
                return getMaxStackSize();
            }
        };
        return itemSlot(slot, x, y, false, size, background);
    }

    /** 空升级槽的本地化 tooltip（kenergyengineering.upgrade_slot）。 */
    private static Component emptyUpgradeSlotTooltip() {
        return ComponentHelper.translated("kenergyengineering.upgrade_slot");
    }

    private static ItemSlot itemSlot(Slot slot, int x, int y, boolean isPlayerSlot) {
        return itemSlot(slot, x, y, isPlayerSlot, 18, IGuiTexture.EMPTY);
    }

    private static ItemSlot itemSlot(Slot slot, int x, int y, boolean isPlayerSlot, int size, IGuiTexture background) {
        var itemSlot = absolute(new ItemSlot(slot), x, y, size, size);
        itemSlot.style(style -> style.backgroundTexture(background));
        itemSlot.slotStyle(style -> style
                .slotOverlay(IGuiTexture.EMPTY)
                .showSlotOverlayOnlyEmpty(false)
                .isPlayerSlot(isPlayerSlot));
        if (size == 26) {
            // 26x26 大槽：padding 5px → 内容 16x16 居中
            itemSlot.layout(layout -> layout.paddingAll(5));
        }
        return itemSlot;
    }

    /** 面朝向六态图标（TENConstants.ICON_FACE_*，12x12 整图）：索引即 FaceOption 枚举值。 */
    private static final IGuiTexture[] FACE_MODE_ICONS = {
            fullTexture(TENConstants.ICON_FACE_OFF, 12, 12),    // OFF 禁用
            fullTexture(TENConstants.ICON_FACE_IN, 12, 12),     // IN 主动输入（机器自拉）
            fullTexture(TENConstants.ICON_FACE_OUT, 12, 12),    // OUT 主动输出（机器自推）
            fullTexture(TENConstants.ICON_FACE_BE_IN, 12, 12),  // BE_IN 被动输入（仅可被塞入）
            fullTexture(TENConstants.ICON_FACE_BE_OUT, 12, 12), // BE_OUT 被动输出（仅可被抽取）
            fullTexture(TENConstants.ICON_FACE_BOTH, 12, 12),   // BOTH 被动双向（可被塞入也可被抽取）
    };

    private static UIElement faceModeElement(CmMachineBlockEntity machine, UIState uiState, int x, int y, int logicalSide, String tooltipKey) {
        return dynamicTextureElement(
                x, y, 12, 12,
                () -> {
                    int mode = faceMode(machine, uiState.getSelectedTransferMode(), logicalSide);
                    return mode >= 0 && mode < FACE_MODE_ICONS.length ? FACE_MODE_ICONS[mode] : FACE_MODE_ICONS[FaceOption.OFF];
                },
                () -> List.of(
                        ComponentHelper.translated(ComponentHelper.GOLD, tooltipKey),
                        ComponentHelper.translated("kenergyengineering.info." + FaceOption.toStr(faceMode(machine, uiState.getSelectedTransferMode(), logicalSide)))),
                () -> {
                    Direction direction = logicalDirection(machine, logicalSide);
                    if (direction != null) {
                        // 类型页由客户端 UIState 维护，服务端 UI 树只持打开时快照；
                        // 在客户端 localClick 读取实时页，并经 rpcToServer 发包。
                        machine.rpcToServer("rpcCycleFaceMode", uiState.getSelectedTransferMode(), direction.get3DDataValue());
                    }
                },
                null,
                uiState::isPanelFullyOpen);
    }

    // ───── Modular 素材族 gauge ─────

    public static ProgressBar energyGaugeModular(CmMachineBlockEntity machine, int x, int y, boolean displayValue) {
        return verticalGaugeModular(machine, x, y, 14, 46,
                com.modularmc.ten.TENConstants.ENERGY_GAUGE_BG, com.modularmc.ten.TENConstants.ENERGY_GAUGE_FILL,
                TENMachineBlockUIFactory::energyPercent, energyGaugeTooltip(machine, displayValue), displayValue);
    }

    public static ProgressBar fuelGaugeModular(CmMachineBlockEntity machine, int x, int y, boolean displayValue) {
        return verticalGaugeModular(machine, x, y, 13, 13,
                com.modularmc.ten.TENConstants.FUEL_GAUGE_BG, com.modularmc.ten.TENConstants.FUEL_GAUGE_FILL,
                TENMachineBlockUIFactory::fuelPercent, fuelTooltip(machine, displayValue), displayValue);
    }

    /**
     * 光合引擎独立燃料表（FUEL_GAUGE_SOLAR_BG/FILL.png 13×13，DOWN_TO_UP 渐显）：
     * 与通用 fuelGaugeModular 同构但引用独立图集，配色/视觉独立于通用燃料表。
     */
    public static ProgressBar fuelGaugeModular13(CmMachineBlockEntity machine, int x, int y, boolean displayValue) {
        return verticalGaugeModular(machine, x, y, 13, 13,
                TENConstants.FUEL_GAUGE_SOLAR_BG, TENConstants.FUEL_GAUGE_SOLAR_FILL,
                TENMachineBlockUIFactory::fuelPercent, fuelTooltip(machine, displayValue), displayValue);
    }

    public static ProgressBar progressGaugeModular(CmMachineBlockEntity machine, int x, int y, boolean showPercent) {
        return progressGaugeModular(machine, x, y, -1, showPercent);
    }

    /**
     * 进度箭头绑定指定流水线：多流水线机器（熔炼机）用这个重载把每条箭头
     * 只接到对应 lane 的进度与 tick 读数上，彼此不串数据。{@code lane = -1} 表示本机单进度。
     */
    public static ProgressBar progressGaugeModular(CmMachineBlockEntity machine, int x, int y, int lane, boolean showPercent) {
        int type = machine.machineType();
        return horizontalProgressModular(machine, x, y, 22, 16,
                progressArrowBg(type), progressArrowFill(type), lane, showPercent);
    }

    public static ProgressBar progressGaugeWide(CmMachineBlockEntity machine, int x, int y, boolean showPercent) {
        return horizontalProgressModular(machine, x, y, 80, 5,
                com.modularmc.ten.TENConstants.PROGRESS_BAR_WIDE_BG, com.modularmc.ten.TENConstants.PROGRESS_BAR_WIDE_FILL,
                -1, showPercent);
    }

    /**
     * LDLib2 通用小图标按钮：12×12、无文字，信息只挂在 tooltip。
     * 底盘按 {@link #TOGGLE_BG_BORDER} 九宫格伸缩，图标以 {@link GuiTextureGroup} 叠在上面并按
     * {@link #TOGGLE_ICON_SCALE} 内缩（图形保持等比），因此不需要自制按钮素材。
     * 三态只用色调表达，图标颜色按开关态与 hover 每帧重算（{@link IGuiTexture#dynamic(Supplier)}）。
     */
    public static Button smallIconToggleButton(Runnable onClick,
                                               Supplier<List<Component>> tooltipSupplier,
                                               ResourceLocation onIcon,
                                               ResourceLocation offIcon,
                                               int iconSize,
                                               BooleanSupplier active,
                                               int x, int y) {
        Button button = new Button();
        button.noText();
        Supplier<IGuiTexture> texture = () -> {
            boolean on = active.getAsBoolean();
            Button.State state = button.getState();
            boolean hover = state != Button.State.DEFAULT;
            int bgTint = state == Button.State.PRESSED ? TOGGLE_BG_PRESSED : (hover ? TOGGLE_BG_HOVER : TOGGLE_BG_COLOR);
            int iconColor = on ? (hover ? TOGGLE_ON_HOVER : TOGGLE_ON_COLOR) : (hover ? TOGGLE_OFF_HOVER : TOGGLE_OFF_COLOR);
            return GuiTextureGroup.of(
                    SpriteTexture.of(TOGGLE_BG).setSprite(0, 0, 32, 32).setBorder(TOGGLE_BG_BORDER).setColor(bgTint),
                    SpriteTexture.of(on ? onIcon : offIcon).setSprite(0, 0, iconSize, iconSize).setColor(iconColor).scale(TOGGLE_ICON_SCALE));
        };
        IGuiTexture dynamic = IGuiTexture.dynamic(texture);
        button.buttonStyle(style -> {
            style.baseTexture(dynamic);
            style.hoverTexture(dynamic);
            style.pressedTexture(dynamic);
        });
        button.setOnClick(event -> onClick.run());
        button.addEventListener(UIEvents.HOVER_TOOLTIPS, event -> event.hoverTooltips = new HoverTooltips(tooltipSupplier.get(), null, null, null));
        button.layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.left(x);
            layout.top(y);
            layout.width(TOGGLE_SIZE);
            layout.height(TOGGLE_SIZE);
        });
        return button;
    }

    /**
     * 范围显示切换按钮（默认右缘：x=157、y=69，右/下均留 7px 边距）：点击经 rpcToServer 发包。
     * BE 必须提供 rpcToggleRangeVisible；不支持时服务端会忽略请求。
     */
    public static Button rangeDisplayToggleButton(CmMachineBlockEntity machine) {
        return rangeDisplayToggleButton(machine, TOGGLE_RIGHT_X, TOGGLE_Y);
    }

    public static Button rangeDisplayToggleButton(CmMachineBlockEntity machine, int x, int y) {
        return smallIconToggleButton(
                () -> machine.rpcToServer("rpcToggleRangeVisible"),
                () -> List.of(
                        ComponentHelper.translated(ComponentHelper.GOLD, "kenergyengineering.info.range_display"),
                        machine.rangeVisible ? ComponentHelper.translated("kenergyengineering.info.range_display_on_tip") : ComponentHelper.translated("kenergyengineering.info.range_display_off_tip"),
                        ComponentHelper.translated("kenergyengineering.info.range_display_click")),
                LDLIB_EYE, LDLIB_EYE_OFF, 32,
                () -> machine.rangeVisible, x, y);
    }

    /** 应用工具/武器附魔切换按钮：点击经 rpcToServer 发包，服务端切换 useEnchantments。 */
    public static Button useEnchantmentsToggleButton(CmMachineBlockEntity machine, int x, int y) {
        return smallIconToggleButton(
                () -> machine.rpcToServer("rpcToggleUseEnchantments"),
                () -> List.of(
                        ComponentHelper.translated(ComponentHelper.GOLD, "kenergyengineering.info.use_enchantments"),
                        machine.useEnchantments ? ComponentHelper.translated("kenergyengineering.info.use_enchantments_on_tip") : ComponentHelper.translated("kenergyengineering.info.use_enchantments_off_tip"),
                        ComponentHelper.translated("kenergyengineering.info.use_enchantments_click")),
                LDLIB_EDIT_ON, LDLIB_EDIT_OFF, 32,
                () -> machine.useEnchantments, x, y);
    }

    /** 采矿场扫描模式小按钮：fast 用实心播放键，标准用空心播放键。 */
    public static Button scanModeToggleButton(BooleanSupplier fast, Runnable onClick, int x, int y) {
        return smallIconToggleButton(
                onClick,
                () -> List.of(
                        ComponentHelper.translated(ComponentHelper.GOLD, "kenergyengineering.info.quarry.scan_mode"),
                        fast.getAsBoolean() ? ComponentHelper.translated("kenergyengineering.info.quarry.scan_fast_tip") : ComponentHelper.translated("kenergyengineering.info.quarry.scan_normal_tip"),
                        ComponentHelper.translated("kenergyengineering.info.quarry.scan_click")),
                LDLIB_PLAY_FILL, LDLIB_PLAY_EMPTY, 64,
                fast, x, y);
    }

    private static ProgressBar verticalGaugeModular(CmMachineBlockEntity machine,
                                                    int x, int y, int width, int height,
                                                    ResourceLocation bgTexture, ResourceLocation fillTexture,
                                                    java.util.function.ToDoubleFunction<CmMachineBlockEntity> percent,
                                                    Supplier<List<Component>> tooltipSupplier,
                                                    boolean displayValue) {
        // bg（barContainer 背景）与 fill（bar 纹理）同为 0,0 起、同 width/height，共用 absolute 锚点 (x,y)；
        // 填充可见区域由 RevealProgressBar UV 裁切保证。
        var filled = SpriteTexture.of(fillTexture).setSprite(0, 0, width, height);
        var progress = absolute(new RevealProgressBar(filled), x, y, width, height);
        progress.barContainer(container -> container.style(style -> style.backgroundTexture(SpriteTexture.of(bgTexture).setSprite(0, 0, width, height)))
                .layout(layout -> layout.paddingAll(0)));
        progress.barBackground.style(style -> style.backgroundTexture(IGuiTexture.EMPTY));
        progress.label.setDisplay(false);
        progress.progressBarStyle(style -> style.fillDirection(FillDirection.DOWN_TO_UP).interpolate(true).interpolateStep(0.1f));
        progress.bindDataSource(SupplierDataSource.of(() -> (float) percent.applyAsDouble(machine)));
        progress.addEventListener(UIEvents.HOVER_TOOLTIPS, event -> event.hoverTooltips = new HoverTooltips(tooltipSupplier.get(), null, null, null));
        return progress;
    }

    private static ProgressBar horizontalProgressModular(CmMachineBlockEntity machine,
                                                         int x, int y, int width, int height,
                                                         ResourceLocation bgTexture, ResourceLocation fillTexture,
                                                         int lane,
                                                         boolean showTicks) {
        var filled = SpriteTexture.of(fillTexture).setSprite(0, 0, width, height);
        var progress = absolute(new RevealProgressBar(filled), x, y, width, height);
        progress.barContainer(container -> container.style(style -> style.backgroundTexture(SpriteTexture.of(bgTexture).setSprite(0, 0, width, height)))
                .layout(layout -> layout.paddingAll(0)));
        progress.barBackground.style(style -> style.backgroundTexture(IGuiTexture.EMPTY));
        progress.label.setDisplay(false);
        progress.progressBarStyle(style -> style.fillDirection(FillDirection.LEFT_TO_RIGHT).interpolate(true).interpolateStep(0.1f));
        progress.bindDataSource(SupplierDataSource.of(() -> (float) machine.progressFraction(lane)));
        if (showTicks) {
            // 读数用配方刻度而不是百分比：已推进刻度 / 配方耗时刻度。
            // 进度每 tick 推进 N 格，读数也随之一次跳 N（N=1 才逐格走），与进度条填充同源。
            progress.addEventListener(UIEvents.HOVER_TOOLTIPS, event -> event.hoverTooltips = new HoverTooltips(
                    List.of(Component.translatable("kenergyengineering.info.progress_ticks",
                            machine.progressDoneTicks(lane), machine.progressTotalTicks(lane))),
                    null, null, null));
        }
        // 把机器方块物品挂到进度条上，JEI/EMI 的「显示用途」键在此处即可查到本机器配方。
        MachineProgressShortcut.bind(progress, machine);
        return progress;
    }

    private static ResourceLocation progressArrowBg(int machineType) {
        return switch (machineType) {
            case MachineType.FURNACE -> com.modularmc.ten.TENConstants.PROGRESS_ARROW_SMELTER_BG;
            case MachineType.PULVERIZER -> com.modularmc.ten.TENConstants.PROGRESS_ARROW_PULVERIZER_BG;
            case MachineType.COMPRESSOR -> com.modularmc.ten.TENConstants.PROGRESS_ARROW_COMPRESSOR_BG;
            case MachineType.REFINER -> com.modularmc.ten.TENConstants.PROGRESS_ARROW_REFINER_BG;
            case MachineType.INDUCTION_FURNACE -> com.modularmc.ten.TENConstants.PROGRESS_ARROW_INDUCTION_FURNACE_BG;
            case MachineType.PSIONICANT -> com.modularmc.ten.TENConstants.PROGRESS_ARROW_PSIONICANT_BG;
            case MachineType.ENCHANTMENT_FLUSHER -> com.modularmc.ten.TENConstants.PROGRESS_ARROW_SMELTER_BG;
            default -> com.modularmc.ten.TENConstants.PROGRESS_ARROW_SMELTER_BG;
        };
    }

    private static ResourceLocation progressArrowFill(int machineType) {
        return switch (machineType) {
            case MachineType.FURNACE -> com.modularmc.ten.TENConstants.PROGRESS_ARROW_SMELTER_FILL;
            case MachineType.PULVERIZER -> com.modularmc.ten.TENConstants.PROGRESS_ARROW_PULVERIZER_FILL;
            case MachineType.COMPRESSOR -> com.modularmc.ten.TENConstants.PROGRESS_ARROW_COMPRESSOR_FILL;
            case MachineType.REFINER -> com.modularmc.ten.TENConstants.PROGRESS_ARROW_REFINER_FILL;
            case MachineType.INDUCTION_FURNACE -> com.modularmc.ten.TENConstants.PROGRESS_ARROW_INDUCTION_FURNACE_FILL;
            case MachineType.PSIONICANT -> com.modularmc.ten.TENConstants.PROGRESS_ARROW_PSIONICANT_FILL;
            case MachineType.ENCHANTMENT_FLUSHER -> com.modularmc.ten.TENConstants.PROGRESS_ARROW_SMELTER_FILL;
            default -> com.modularmc.ten.TENConstants.PROGRESS_ARROW_SMELTER_FILL;
        };
    }

    /**
     * 18×50 流体槽，使用 FLUID_SLOT 底图与 FLUID_SLOT_OVERLAY 覆盖层，适用于 MACHINE_GUI 空面板。
     * TENFluidSlot 隐藏温度/气液态行；渲染层序为底图 → 流体柱 → 覆盖层 → hover 高亮。
     */
    public static FluidSlot fluidGaugeModular(CmMachineBlockEntity machine, int x, int y, int width, int height, int tankIndex) {
        var slot = absolute(new TENFluidSlot(), x, y, width, height);
        // FluidSlot 默认 paddingAll(1) 会压缩 18×50 纹理；置零后底图、覆盖层与流体共用完整内容区。
        slot.layout(layout -> layout.paddingAll(0));
        slot.style(style -> style.backgroundTexture(fullTexture(TENConstants.FLUID_SLOT, width, height)));
        slot.slotStyle(style -> style
                .slotOverlay(fullTexture(TENConstants.FLUID_SLOT_OVERLAY, width, height))
                .showSlotOverlayOnlyEmpty(false)
                .fillDirection(FillDirection.DOWN_TO_UP)
                .showFluidTooltips(true));
        slot.amountLabel.setDisplay(false);
        var handler = machine.getFluidHandler(null);
        if (handler == null) {
            throw new IllegalStateException("fluidGauge on machine with no tanks: " + machine);
        }
        slot.bind(handler, tankIndex);
        return slot;
    }

    /** 熔炼机 XP 流体输出槽（固定可见，modular FLUID_SLOT 底图，tank 0）。 */
    public static FluidSlot createXpFluidSlot(CmMachineBlockEntity machine, int x, int y, int width, int height) {
        return fluidGaugeModular(machine, x, y, width, height, 0);
    }

    /**
     * 冷却剂消耗进度栏：绑定冷却器的冷却剂剩余比例，素材 progress_bar_wide_coolant。
     */
    public static ProgressBar coolantProgressBar(com.modularmc.ten.common.blockentity.machine.CoolerBlockEntity cooler, int x, int y) {
        var filled = SpriteTexture.of(com.modularmc.ten.TENConstants.PROGRESS_PROGRESS_BAR_WIDE_COOLANT).setSprite(0, 0, 80, 5);
        var progress = absolute(new RevealProgressBar(filled), x, y, 80, 5);
        progress.barContainer(container -> container.style(style -> style.backgroundTexture(SpriteTexture.of(com.modularmc.ten.TENConstants.PROGRESS_BAR_WIDE_BG).setSprite(0, 0, 80, 5)))
                .layout(layout -> layout.paddingAll(0)));
        progress.barBackground.style(style -> style.backgroundTexture(IGuiTexture.EMPTY));
        progress.label.setDisplay(false);
        progress.progressBarStyle(style -> style.fillDirection(FillDirection.LEFT_TO_RIGHT).interpolate(true).interpolateStep(0.1f));
        progress.bindDataSource(SupplierDataSource.of(() -> (float) cooler.getCoolantPercent()));
        progress.addEventListener(UIEvents.HOVER_TOOLTIPS, event -> event.hoverTooltips = new HoverTooltips(List.of(
                ComponentHelper.translated("kenergyengineering.info.coolant_progress"),
                ComponentHelper.make((int) (cooler.getCoolantPercent() * 100) + "%")), null, null, null));
        MachineProgressShortcut.bind(progress, cooler);
        return progress;
    }

    /**
     * 迷你垂直进度箭头：输入槽右侧 8x54 素材（顶尖朝上/朝下），非动态（仅背景）。
     */
    public static UIElement verticalProgressMini(int inputX, int inputTopY, int slotSize,
                                                 ResourceLocation texture, boolean targetOnTop) {
        return textureElement(inputX + slotSize, inputTopY, 8, 54,
                fullTexture(texture, 8, 54), null, null, null, null);
    }

    private static UIElement textureElement(int x, int y, int width, int height,
                                            IGuiTexture texture,
                                            @Nullable Supplier<List<Component>> tooltipSupplier,
                                            @Nullable Runnable localClick,
                                            @Nullable Runnable serverClick,
                                            @Nullable BooleanSupplier visibleSupplier) {
        var element = absolute(new UIElement(), x, y, width, height)
                .style(style -> style.backgroundTexture(texture));
        if (tooltipSupplier != null) {
            element.addEventListener(UIEvents.HOVER_TOOLTIPS, event -> event.hoverTooltips = new HoverTooltips(tooltipSupplier.get(), null, null, null));
        }
        if (localClick != null) {
            element.addEventListener(UIEvents.MOUSE_DOWN, event -> {
                if (event.button == 0) {
                    localClick.run();
                }
            });
        }
        if (serverClick != null) {
            element.addServerEventListener(UIEvents.MOUSE_DOWN, event -> {
                if (event.button == 0) {
                    serverClick.run();
                }
            });
        }
        if (visibleSupplier != null) {
            syncDisplay(element, visibleSupplier);
        }
        return element;
    }

    private static UIElement dynamicTextureElement(int x, int y, int width, int height,
                                                   Supplier<IGuiTexture> textureSupplier,
                                                   @Nullable Supplier<List<Component>> tooltipSupplier,
                                                   @Nullable Runnable localClick,
                                                   @Nullable Runnable serverClick,
                                                   @Nullable BooleanSupplier visibleSupplier) {
        var element = textureElement(x, y, width, height, IGuiTexture.EMPTY, tooltipSupplier, localClick, serverClick, visibleSupplier);
        syncTexture(element, textureSupplier);
        return element;
    }

    private static void syncTexture(UIElement element, Supplier<IGuiTexture> textureSupplier) {
        Runnable update = () -> element.style(style -> style.backgroundTexture(textureSupplier.get()));
        update.run();
        element.addEventListener(UIEvents.TICK, event -> update.run());
    }

    private static void syncDisplay(UIElement element, BooleanSupplier visibleSupplier) {
        Runnable update = () -> element.setDisplay(visibleSupplier.getAsBoolean());
        update.run();
        element.addEventListener(UIEvents.TICK, event -> update.run());
    }

    private static <T extends UIElement> T absolute(T element, int x, int y, int width, int height) {
        element.layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.left(x);
            layout.top(y);
            layout.width(width);
            layout.height(height);
        });
        return element;
    }

    private static IGuiTexture sprite(ResourceLocation resourceLocation, int x, int y, int width, int height) {
        return SpriteTexture.of(resourceLocation).setSprite(x, y, width, height);
    }

    private static IGuiTexture fullTexture(ResourceLocation resourceLocation, int width, int height) {
        return SpriteTexture.of(resourceLocation).setSprite(0, 0, width, height);
    }

    public static ResourceLocation backgroundFor(int machineType) {
        return switch (machineType) {
            // 机器使用 MACHINE_GUI 空面板，槽框与仪表由 modular 素材族绘制。
            case MachineType.FURNACE, MachineType.PULVERIZER, MachineType.COMPRESSOR, MachineType.REFINER, MachineType.INDUCTION_FURNACE, MachineType.PSIONICANT, MachineType.ENCHANTMENT_FLUSHER, MachineType.MATTER_CONDENSER, MachineType.BEACON, MachineType.MOB_RIPPER, MachineType.QUARRY, MachineType.FARM, MachineType.CELL, MachineType.CREATIVE_CELL, MachineType.ENGINE_SOLAR, MachineType.ENGINE_EXTRACTION, MachineType.ENGINE_METAL, MachineType.ENGINE_BIOMASS, MachineType.BLOCK_BREAKER, MachineType.BLOCK_FORMER, MachineType.COOLER -> TENConstants.MACHINE_GUI;
            default -> TENConstants.LEGACY_SHEET;
        };
    }

    private static double fuelPercent(CmMachineBlockEntity machine) {
        return machine.maxFuel == 0 ? 0 : (double) machine.fuel / machine.maxFuel;
    }

    private static double energyPercent(CmMachineBlockEntity machine) {
        return machine.maxEnergyStored == 0 ? 0 : (double) machine.energyStored / machine.maxEnergyStored;
    }

    private static void cycleRedstone(CmMachineBlockEntity machine) {
        int mode = machine.redstoneMode + 1;
        if (mode >= RedstoneMode.size()) {
            mode = RedstoneMode.OFF;
        }
        machine.rpcToServer("rpcSetRedstoneMode", mode);
    }

    private static int faceMode(CmMachineBlockEntity machine, int selectedMode, int logicalSide) {
        int[] list = selectedMode == 0 ? machine.energyFaceData : selectedMode == 1 ? machine.itemFaceData : machine.fluidFaceData;
        if (list.length < 6) {
            return FaceOption.OFF;
        }
        Direction direction = logicalDirection(machine, logicalSide);
        return direction == null ? FaceOption.OFF : list[direction.get3DDataValue()];
    }

    @Nullable
    private static Direction logicalDirection(CmMachineBlockEntity machine, int logicalSide) {
        // 逻辑面映射以 getFacing() 读到的方块状态 FACING 为准，facingVal 只是持久化镜像。
        // BE 侧映射表与拆卸重放共用，任意朝向都必须保持双射。
        return CmMachineBlockEntity.faceForLogicalSide(machine.getFacing(), logicalSide);
    }

    /**
     * 燃料条读数：已消耗 tick / 修正后的持续时间 tick。
     * 总刻度在开烧时按「燃料总能量 ÷ 实际产出率」写入，已消耗按已耗 FE 占比折算，
     * 因此读数与燃料条的填充比例同源，不会出现「条满了但 tick 没走完」。
     */
    private static Supplier<List<Component>> fuelTooltip(CmMachineBlockEntity machine, boolean displayValue) {
        return () -> List.of(Component.translatable("kenergyengineering.info.fuel_ticks", machine.fuelDoneTicks(), machine.fuelTotalTicks()));
    }

    private static Supplier<List<Component>> energyGaugeTooltip(CmMachineBlockEntity machine, boolean displayValue) {
        return () -> displayValue ? List.of(DisplayHelper.join(machine.energyStored, machine.maxEnergyStored)) : List.of(ComponentHelper.make((int) (energyPercent(machine) * 100) + "%"));
    }

    private static List<Component> fluidTooltip(CmMachineBlockEntity machine, int tankIndex, boolean showValue) {
        if (tankIndex < 0 || tankIndex >= machine.tanks.size()) {
            return List.of();
        }
        var tank = machine.tanks.get(tankIndex);
        var list = new ArrayList<Component>();
        if (!tank.getFluid().isEmpty()) {
            list.add(tank.getFluid().getHoverName());
        }
        if (showValue) {
            list.add(DisplayHelper.joinmB(tank.getFluidAmount(), tank.getCapacity()));
        } else {
            int percent = tank.getCapacity() > 0 ? (int) ((tank.getFluidAmount() * 100.0) / tank.getCapacity()) : 0;
            list.add(ComponentHelper.make(percent + "%"));
        }
        return list;
    }

    private static List<Component> ideaTooltips(BlockUIMenuType.BlockUIHolder holder) {
        var list = new ArrayList<Component>();
        list.add(ComponentHelper.translated(ComponentHelper.GOLD, "kenergyengineering.info.bar_ideas"));
        String path = BuiltInRegistries.BLOCK.getKey(holder.blockState.getBlock()).getPath();
        String keyBase = ComponentHelper.exceptMachineOrGiveCell(path);
        for (int i = 0; !keyBase.isEmpty(); i++) {
            String key = "kenergyengineering.info." + keyBase + "." + i;
            Component tooltip = ComponentHelper.translated(key);
            if (tooltip.getString().equals(key)) {
                break;
            }
            list.add(tooltip);
        }
        return list;
    }

    private static List<Component> energyInfoTooltips(CmMachineBlockEntity machine) {
        var list = new ArrayList<Component>();
        list.add(ComponentHelper.translated(ComponentHelper.GOLD, "kenergyengineering.info.bar_energy"));
        list.add(ComponentHelper.translated("kenergyengineering.info.bar_energy_use"));
        list.add(ComponentHelper.translated(ComponentHelper.RED, machine.energyUse + " FE/t"));
        list.add(ComponentHelper.translated("kenergyengineering.info.bar_energy_use_max"));
        list.add(ComponentHelper.translated(ComponentHelper.RED, machine.energyUseMax + " FE/t"));
        return list;
    }

    private static List<Component> redstoneTooltips(CmMachineBlockEntity machine) {
        String mode = switch (machine.redstoneMode) {
            case RedstoneMode.HIGH -> "kenergyengineering.info.high";
            case RedstoneMode.LOW -> "kenergyengineering.info.low";
            default -> "kenergyengineering.info.off";
        };
        // 标题与当前红石类型分行：第一行固定标题，第二行是 OFF / HIGH / LOW 三选一的当前值。
        return List.of(
                ComponentHelper.translated(ComponentHelper.GOLD, "kenergyengineering.info.bar_redstone"),
                ComponentHelper.translated(mode));
    }

    private static List<Component> controlTooltip() {
        return List.of(ComponentHelper.translated(ComponentHelper.GOLD, "kenergyengineering.info.bar_control"));
    }

    private static List<Component> energyModeTooltip() {
        return List.of(ComponentHelper.translated(ComponentHelper.GOLD, "kenergyengineering.info.bar_mode", "kenergyengineering.info.energy"));
    }

    private static List<Component> itemModeTooltip() {
        return List.of(ComponentHelper.translated(ComponentHelper.GOLD, "kenergyengineering.info.bar_mode", "kenergyengineering.info.item"));
    }

    private static List<Component> fluidModeTooltip() {
        return List.of(ComponentHelper.translated(ComponentHelper.GOLD, "kenergyengineering.info.bar_mode", "kenergyengineering.info.fluid"));
    }

    public static final class UIState {

        private static final Map<String, Snapshot> CACHE = new ConcurrentHashMap<>();

        private final String key;
        private int selectedTransferMode;
        private boolean controlOpen;
        private boolean upgradeOpen;
        private boolean adjustOpen;
        /** 视图态（不入快照）：展开面板动画是否已到全开；仅供 faceModeElement 等内部 syncDisplay 联动查询。 */
        private boolean panelFullyOpen;
        /** 视图态（不入快照）：升级槽面板动画是否已全开。 */
        private boolean upgradePanelFullyOpen;

        public UIState(BlockUIMenuType.BlockUIHolder holder) {
            this.key = holder.player.getUUID() + "@" + holder.pos.asLong();
            Snapshot snapshot = CACHE.computeIfAbsent(key, ignored -> new Snapshot());
            this.selectedTransferMode = snapshot.selectedTransferMode;
            this.controlOpen = snapshot.controlOpen;
            this.upgradeOpen = snapshot.upgradeOpen;
            this.adjustOpen = snapshot.adjustOpen;
            this.panelFullyOpen = false;
            this.upgradePanelFullyOpen = false;
        }

        public int getSelectedTransferMode() {
            return selectedTransferMode;
        }

        public void setSelectedTransferMode(int selectedTransferMode) {
            this.selectedTransferMode = selectedTransferMode;
            snapshot().selectedTransferMode = selectedTransferMode;
        }

        public boolean isControlOpen() {
            return controlOpen;
        }

        public void setControlOpen(boolean controlOpen) {
            // 三选一互斥：展开配置面板时收起升级与范围调节面板（快照同步写回，跨次打开不串态）
            if (controlOpen) {
                this.upgradeOpen = false;
                this.adjustOpen = false;
                snapshot().upgradeOpen = false;
                snapshot().adjustOpen = false;
            }
            this.controlOpen = controlOpen;
            snapshot().controlOpen = controlOpen;
            if (!controlOpen) {
                panelFullyOpen = false;
            }
        }

        public boolean isUpgradeOpen() {
            return upgradeOpen;
        }

        public void setUpgradeOpen(boolean upgradeOpen) {
            if (upgradeOpen) {
                this.controlOpen = false;
                this.adjustOpen = false;
                snapshot().controlOpen = false;
                snapshot().adjustOpen = false;
            }
            this.upgradeOpen = upgradeOpen;
            snapshot().upgradeOpen = upgradeOpen;
            if (!upgradeOpen) {
                upgradePanelFullyOpen = false;
            }
        }

        public boolean isAdjustOpen() {
            return adjustOpen;
        }

        public void setAdjustOpen(boolean adjustOpen) {
            if (adjustOpen) {
                this.controlOpen = false;
                this.upgradeOpen = false;
                snapshot().controlOpen = false;
                snapshot().upgradeOpen = false;
            }
            this.adjustOpen = adjustOpen;
            snapshot().adjustOpen = adjustOpen;
        }

        /** 展开面板动画是否已全开（视图态，仅当前帧有效）。 */
        public boolean isPanelFullyOpen() {
            return panelFullyOpen;
        }

        private void setPanelFullyOpen(boolean panelFullyOpen) {
            this.panelFullyOpen = panelFullyOpen;
        }

        /** 升级槽面板动画是否已全开（视图态）。 */
        public boolean isUpgradePanelFullyOpen() {
            return upgradePanelFullyOpen;
        }

        private void setUpgradePanelFullyOpen(boolean upgradePanelFullyOpen) {
            this.upgradePanelFullyOpen = upgradePanelFullyOpen;
        }

        private Snapshot snapshot() {
            return CACHE.computeIfAbsent(key, ignored -> new Snapshot());
        }
    }

    private static final class Snapshot {

        private int selectedTransferMode;
        private boolean controlOpen;
        private boolean upgradeOpen;
        private boolean adjustOpen;
    }
}
