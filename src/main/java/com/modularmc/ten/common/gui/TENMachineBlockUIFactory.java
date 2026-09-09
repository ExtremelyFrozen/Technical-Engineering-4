package com.modularmc.ten.common.gui;

import com.modularmc.ten.TENConstants;
import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;
import com.modularmc.ten.api.option.FaceOption;
import com.modularmc.ten.api.option.MachineType;
import com.modularmc.ten.api.option.RedstoneMode;
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
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.FillDirection;
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

public final class TENMachineBlockUIFactory {

    // 26 线旧图集（统一走 TENConstants.LEGACY_SHEET 注册，本类不再私持路径）

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
        // 26.1.2 对齐：容器左缘 7（背包栏槽位左移 1px），hotbar 上边距 4（快捷栏槽位上移 1px，左移同源）
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
    /** 面板展开/收起速度（px/tick）：底图九宫格与内容同由 panelWidth 驱动，速度一致；80px 约 14tick（0.7s）、升级面板 58px 约 10tick（0.5s）。 */
    private static final int CONFIG_PANEL_SPEED = 6;
    /** 主 GUI 宽 176，tab 邻主 GUI 间隙 1px → tab 左缘 177。 */
    private static final int CONFIG_TAB_X = 176 + 1;
    /** 面板左缘锚定 x = tab 左缘（左→右生长，覆盖主 GUI 右半）。 */
    private static final int CONFIG_PANEL_LEFT_X = CONFIG_TAB_X;
    /** 面板锚定 y：与功率 tab（左缘 -27,27）平齐。 */
    private static final int CONFIG_PANEL_Y = 27;
    /** 低于此宽度不绘制面板（边角 4px×2 + 中缝最小可见宽）。 */
    private static final int CONFIG_PANEL_MIN_VISIBLE = 2 * CONFIG_PANEL_BORDER;
    /** 层1 覆盖层（panel_config_legacy.png 布局图）与层0 底图九宫格内边框的恒等间距（四边一致）。 */
    private static final int CONFIG_OVERLAY_GAP = 6;
    /** 层1 覆盖层原点：容器 border(4) + gap(6) = 10；legacy 布局图 60x85 原尺寸不拉伸。 */
    private static final int CONFIG_OVERLAY_XY = CONFIG_PANEL_BORDER + CONFIG_OVERLAY_GAP;
    /** 覆盖层纹理尺寸（panel_config_legacy.png 60x85，配置项的布局图/默认状态）。 */
    private static final int OVERLAY_TEX_W = 60;
    private static final int OVERLAY_TEX_H = 85;
    /**
     * 展开面板目标尺寸：由 legacy 布局图 + 四边 (gap 6 + border 4) 反推，非固定 128。
     * 容器 = 60+2×10=80 × 85+2×10=105；四边间距：容器内边框(border 4 内沿)到 legacy 纹理边缘恒等 6px。
     */
    private static final int CONFIG_PANEL_WIDTH = OVERLAY_TEX_W + 2 * CONFIG_OVERLAY_XY;
    private static final int CONFIG_PANEL_HEIGHT = OVERLAY_TEX_H + 2 * CONFIG_OVERLAY_XY;

    // ───── 升级槽 tab（右缘、配置 tab 上侧，与左缘机器信息 tab 对齐 y=0）─────
    /** 升级槽尺寸（ITEM_SLOT_SMALL 18x18）。 */
    private static final int UPGRADE_SLOT_SIZE = 18;
    /** 升级槽 2x3 网格：槽间距/行距 2px。 */
    private static final int UPGRADE_SLOT_GAP = 2;
    /** 升级槽列数。 */
    private static final int UPGRADE_COLS = 2;
    /** 升级槽行数。 */
    private static final int UPGRADE_ROWS = 3;
    /** 升级面板内容区宽：2×18 + 列间 2 = 38。 */
    private static final int UPGRADE_CONTENT_W = UPGRADE_COLS * UPGRADE_SLOT_SIZE + (UPGRADE_COLS - 1) * UPGRADE_SLOT_GAP;
    /** 升级面板内容区高：3×18 + 行间 2×2 = 58。 */
    private static final int UPGRADE_CONTENT_H = UPGRADE_ROWS * UPGRADE_SLOT_SIZE + (UPGRADE_ROWS - 1) * UPGRADE_SLOT_GAP;
    /** 升级面板目标尺寸：内容区 + 四边 (gap 6 + border 4)，与配置面板同基准。 */
    private static final int UPGRADE_PANEL_WIDTH = UPGRADE_CONTENT_W + 2 * CONFIG_OVERLAY_XY;
    private static final int UPGRADE_PANEL_HEIGHT = UPGRADE_CONTENT_H + 2 * CONFIG_OVERLAY_XY;
    /** 升级槽 tab 头 y：与左缘机器信息 tab（y=0）平齐，位于配置 tab（y=27）上侧。 */
    private static final int UPGRADE_TAB_Y = 0;
    /** 升级面板锚定 y = tab 头 y。 */
    private static final int UPGRADE_PANEL_Y = UPGRADE_TAB_Y;

    /** 升级面板附加底图尺寸（42x62，内容区每边外扩 2px）。 */
    private static final int PANEL_UPGRADE_UNDERLAY_W = 42;
    private static final int PANEL_UPGRADE_UNDERLAY_H = 62;
    /** 升级面板附加底图内边距：内容区原点 10 外扩 2px → (8,8)（右/下缘对称至 50，面板 58/78 内居中）。 */
    private static final int UPGRADE_PANEL_BORDER = CONFIG_OVERLAY_XY - 2;

    public static void addCommonSidebar(UIElement root, BlockUIMenuType.BlockUIHolder holder, CmMachineBlockEntity machine, UIState uiState) {
        // 侧栏 tab 全部引用独立 26×26 图标（icons/*.png），不再切旧图集
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
        // NOTE: 改 CONFIG_PANEL_WIDTH/HEIGHT 需同步 syncSidebar/syncUpgrade 两套动画逻辑（宽高按比例同步对角生长，见 panelHeightFor）。
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
        // 附加底图（用户需求）：介入九宫格底图与内容钮之间——legacy 布局图 60×85 精确覆盖内容区
        // （10 + 60 + 10 = 80 宽 / 10 + 85 + 10 = 105 高）；后 addChild → 同 zIndex(0) 下后画于切片，
        // 内容钮 zIndex=1（root 子级）仍在其上
        var configUnderlay = new UIElement().style(style -> style.backgroundTexture(
                fullTexture(TENConstants.PANEL_CONFIG_LEGACY, OVERLAY_TEX_W, OVERLAY_TEX_H)));
        configUnderlay.layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.left(CONFIG_OVERLAY_XY);
            layout.top(CONFIG_OVERLAY_XY);
            layout.width(OVERLAY_TEX_W);
            layout.height(OVERLAY_TEX_H);
        });
        configPanel.addChild(configUnderlay);
        // 层2+ 内容钮：坐标 = 覆盖层原点 (10,10) + legacy 原布局相对坐标；仅 fullyOpen 后可交互。
        // z 层规范（统一分层）：面板底图 zIndex=0、内容层（钮/槽）显式 zIndex=1——
        // 同 zIndex 时排序按 addChild 索引降序、倒序绘制 → 后 addChild 者反而在下层，
        // 故内容必须显式 zIndex 抬升才能盖过面板底图（勿删 zIndex(1)）。
        int overlayOriginX = CONFIG_PANEL_LEFT_X + CONFIG_OVERLAY_XY;
        int overlayOriginY = configTabY + CONFIG_OVERLAY_XY;

        // 类型按钮（能量/物品/流体）：CONFIG_MODE_BUTTONS 图集（42×56，格 14×14）——
        // 列=类型（左列流体 x=0 / 中列能量 x=14 / 右列物品 x=28）；行=状态：未悬停未选 y=0 / 悬停未选 y=14 /
        // 未悬停已选 y=28 / 悬停已选 y=42；syncTexture（TICK）实时刷新，hover 由 element.isHover() 取
        var modeIconTex = TENConstants.CONFIG_MODE_BUTTONS;
        var energyModeButton = textureElement(overlayOriginX + 7, overlayOriginY + 64, 14, 14,
                IGuiTexture.EMPTY, () -> energyModeTooltip(), () -> uiState.setSelectedTransferMode(0), null, null)
                .style(style -> style.zIndex(1));
        syncTexture(energyModeButton, () -> sprite(modeIconTex, 14,
                (uiState.getSelectedTransferMode() == 0 ? 2 : 0) * 14 + (energyModeButton.isHover() ? 14 : 0), 14, 14));
        var itemModeButton = textureElement(overlayOriginX + 23, overlayOriginY + 64, 14, 14,
                IGuiTexture.EMPTY, () -> itemModeTooltip(), () -> uiState.setSelectedTransferMode(1), null, null)
                .style(style -> style.zIndex(1));
        syncTexture(itemModeButton, () -> sprite(modeIconTex, 28,
                (uiState.getSelectedTransferMode() == 1 ? 2 : 0) * 14 + (itemModeButton.isHover() ? 14 : 0), 14, 14));
        var fluidModeButton = textureElement(overlayOriginX + 39, overlayOriginY + 64, 14, 14,
                IGuiTexture.EMPTY, () -> fluidModeTooltip(), () -> uiState.setSelectedTransferMode(2), null, null)
                .style(style -> style.zIndex(1));
        syncTexture(fluidModeButton, () -> sprite(modeIconTex, 0,
                (uiState.getSelectedTransferMode() == 2 ? 2 : 0) * 14 + (fluidModeButton.isHover() ? 14 : 0), 14, 14));

        var frontButton = faceModeElement(machine, uiState, overlayOriginX + 24, overlayOriginY + 22, 0, "kenergyengineering.info.front").style(style -> style.zIndex(1));
        var backButton = faceModeElement(machine, uiState, overlayOriginX + 38, overlayOriginY + 36, 1, "kenergyengineering.info.back").style(style -> style.zIndex(1));
        var leftButton = faceModeElement(machine, uiState, overlayOriginX + 10, overlayOriginY + 22, 2, "kenergyengineering.info.left").style(style -> style.zIndex(1));
        var rightButton = faceModeElement(machine, uiState, overlayOriginX + 38, overlayOriginY + 22, 3, "kenergyengineering.info.right").style(style -> style.zIndex(1));
        var upButton = faceModeElement(machine, uiState, overlayOriginX + 24, overlayOriginY + 8, 4, "kenergyengineering.info.up").style(style -> style.zIndex(1));
        var downButton = faceModeElement(machine, uiState, overlayOriginX + 24, overlayOriginY + 36, 5, "kenergyengineering.info.down").style(style -> style.zIndex(1));
        var closeButton = textureElement(overlayOriginX + 54, overlayOriginY - 6, 10, 10, IGuiTexture.EMPTY,
                () -> controlTooltip(), () -> uiState.setControlOpen(false), null, null)
                .style(style -> style.zIndex(1));

        root.addChild(configPanel);
        // 点击面板背景空白处收起面板：CLICK target 为面板子树内背景元素（切片/overlay），
        // 冒泡路径含 configPanel；内容钮/槽位是 root 直接子元素，点击不经过 panel 子树，不会误触发
        configPanel.addEventListener(UIEvents.CLICK, event -> {
            if (event.button == 0) {
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
        // 中间覆盖层随内容层统一显隐（用户需求：面板完全展开后才出现，不随切片动画提前露出）
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
                    int next = Math.min(w + CONFIG_PANEL_SPEED, CONFIG_PANEL_WIDTH);
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
                    int next = Math.max(w - CONFIG_PANEL_SPEED, 0);
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
     * 面板内容 2×3 六升级槽（ITEM_SLOT_SMALL 18×18，间距 2px），内容区与九宫格底图内边框间距恒等 6px
     * （面板尺寸由内容区 + 四边 border4+gap6 反推，见 UPGRADE_PANEL_WIDTH/HEIGHT）。
     * 由 buildMachineUI 在 supportsUpgradeSlots() 时挂载（替代原顶部 -28 常显条）。
     */
    public static void addUpgradeSlotsTab(UIElement root, CmMachineBlockEntity machine, UIState uiState) {
        var upgradeButton = textureElement(CONFIG_TAB_X, UPGRADE_TAB_Y, CONFIG_TAB_SIZE, CONFIG_TAB_SIZE,
                fullTexture(TENConstants.ICON_UPGRADE, CONFIG_TAB_SIZE, CONFIG_TAB_SIZE),
                () -> List.of(ComponentHelper.translated("kenergyengineering.info.bar_upgrade")),
                () -> {
                    // 互斥：开升级面板先关配置面板
                    uiState.setControlOpen(false);
                    uiState.setUpgradeOpen(!uiState.isUpgradeOpen());
                },
                null, null).style(style -> style.zIndex(1));

        // zIndex=1：面板底图层仅需盖过兄弟面板（配置面板 zIndex=0）的互斥动画残影——
        // painter 层叠靠「zIndex 大者后画」，取 1 即满足；绝不能大抬升：
        // LDLib2 2.2.37 槽内物品渲染 z = 元素 zIndex + 32（drawBackgroundAdditional -200 +
        // drawItemStack +232，且深度测试开启），面板 zIndex 越大 → 槽物品 z 越大越远，
        // 会被深度缓冲中更近内容剔除（旧 zIndex=200 → 物品 z=232 即此问题）。
        // 主 GUI 机器槽（zIndex=0 → 物品 z=32）为已验证安全档；面板 1 → 槽物品 z=33 同档。
        var upgradePanel = absolute(new UIElement(), CONFIG_PANEL_LEFT_X, UPGRADE_PANEL_Y, UPGRADE_PANEL_WIDTH, UPGRADE_PANEL_HEIGHT)
                .style(style -> style.zIndex(1));
        upgradePanel.setDisplay(false);
        List<PanelSlice> upgradePanelSlices = panelSlices(UPGRADE_PANEL_WIDTH, UPGRADE_PANEL_HEIGHT);
        for (PanelSlice s : upgradePanelSlices) {
            upgradePanel.addChild(s.element());
        }
        // 附加底图（用户需求）：介入九宫格底图与内容之间——42×62 覆盖内容区 38×58 每边外扩 2px；
        // 后 addChild → 同 zIndex(0) 下索引大者后画，压在九宫格切片之上、槽之下
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

        // 层2+ 六升级槽：挂在面板子树内（内容区原点 = 面板原点 + (border4+gap6)=10；2×3 网格步长 18+2）
        // NOTE: 同 zIndex 时按 addChild 索引降序排序、倒序绘制——切片（先 add，index 小）先画、
        // 槽（后 add，index 大）后画，故槽底图/物品绘制在面板切片之上（painter 顶层）。
        // 物品渲染 z = 面板 zIndex(1) + 槽 zIndex(0) + 32 = 33，与主 GUI 机器槽 z=32 同安全档。
        int slotBaseX = CONFIG_OVERLAY_XY;
        int slotBaseY = CONFIG_OVERLAY_XY;
        int upgradeSlots = machine.upgradeHandler.getSlots();
        List<UIElement> upgradeContents = new ArrayList<>();

        upgradePanel.addEventListener(UIEvents.CLICK, event -> {
            // 排除槽点击：槽在 panel 子树内，CLICK 冒泡必经 panel，不排除则点槽放取物品会误收起
            if (event.button == 0 && !(event.target instanceof ItemSlot)) {
                uiState.setUpgradeOpen(false);
            }
        });

        for (int i = 0; i < upgradeSlots; i++) {
            int col = i % UPGRADE_COLS;
            int row = i / UPGRADE_COLS;
            int sx = slotBaseX + col * (UPGRADE_SLOT_SIZE + UPGRADE_SLOT_GAP);
            int sy = slotBaseY + row * (UPGRADE_SLOT_SIZE + UPGRADE_SLOT_GAP);
            ItemSlot slot = upgradePanelSlot(machine, i, sx, sy);
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
        // 中间覆盖层随内容层统一显隐（用户需求：面板完全展开后才出现，不随切片动画提前露出）
        upgradeContents.add(upgradeUnderlay);

        // 与配置面板同构的动画状态机：-1 收起完成；0..目标宽 展开中；目标宽 展开完成（从 tab 左上角对角生长）
        int[] panelWidth = { uiState.isUpgradeOpen() ? -1 : 0 };
        // 升级面板展开完成时是否已请求过数据（每次打开重置）：rpcToServer 请求服务端推送全量升级数据
        boolean[] upgradeDataRequested = { false };
        Runnable syncUpgrade = () -> {
            boolean open = uiState.isUpgradeOpen();
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
                    int next = Math.min(w + CONFIG_PANEL_SPEED, UPGRADE_PANEL_WIDTH);
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
                // 展开完成即请求全量升级数据（C→S，服务端 rpcToPlayer 回复单播）：
                // 客户端 upgradeHandler 数据不经 readTileData/更新包（探针实证恒空），
                // 仅靠本请求 + 服务端变化广播（rpcSyncUpgradeData）维持同步
                if (fullyOpen && !upgradeDataRequested[0]) {
                    upgradeDataRequested[0] = true;
                    machine.rpcToServer("rpcRequestUpgradeData");
                }
            } else {
                upgradeDataRequested[0] = false;
                for (UIElement content : upgradeContents) {
                    content.setDisplay(false);
                }
                if (w > 0) {
                    int next = Math.max(w - CONFIG_PANEL_SPEED, 0);
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

    /** 升级面板内 18×18 槽：modular 小槽底图 + quickMovePriority(1000) + 空槽 tooltip。 */
    private static ItemSlot upgradePanelSlot(CmMachineBlockEntity machine, int index, int x, int y) {
        SlotItemHandler slot = new SlotItemHandler(machine.upgradeHandler, index, 0, 0);
        // 升级槽专用底图（用户需求）：不再借用小型物品槽
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

    /** 18x18 槽无底图（旧版背景 PNG 自绘槽框的机器专用）。 */
    public static ItemSlot machineSlot(CmMachineBlockEntity machine, int index, int x, int y) {
        return machineSlot(machine, index, x, y, 18, IGuiTexture.EMPTY);
    }

    /**
     * 18x18 机器槽 + modular 小槽底图（ITEM_SLOT_SMALL）。
     * 背景为 MACHINE_GUI 空面板的机器（翻新机）必须用此变体，否则槽框缺失。
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
     * ITEM_SLOT_SMALL 底图，末影箱动态堆叠上限（26.1.2 T003 对齐）。
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
                return type.canIn() && machine.valid(getSlotIndex(), stack) && super.mayPlace(stack);
            }

            @Override
            public int getMaxStackSize(net.minecraft.world.item.ItemStack stack) {
                // 26.1.2 对齐：上限=底层 itemHandler 槽位上限（动态容量），不按物品原版 64 封顶
                return getMaxStackSize();
            }
        };
        return itemSlot(slot, x, y, false, size, background);
    }

    /**
     * 26.1.2 对齐：升级槽专用底图（modular/slots/upgrade_slot.png 18×18）+ quickMovePriority(1000)。
     * 空槽显示 upgrade_slot 本地化 tooltip；非空保留原生物品 tooltip（不覆写事件）。
     * 旧版叠加 textureElement 拦截鼠标导致不可交互，已移除。
     */
    /** 空升级槽本地化 tooltip（kenergyengineering.upgrade_slot）。 */
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
            fullTexture(TENConstants.ICON_FACE_IN, 12, 12),     // IN 被动输入
            fullTexture(TENConstants.ICON_FACE_OUT, 12, 12),    // OUT 被动输出
            fullTexture(TENConstants.ICON_FACE_BE_IN, 12, 12),  // BE_IN 主动输入
            fullTexture(TENConstants.ICON_FACE_BE_OUT, 12, 12), // BE_OUT 主动输出
            fullTexture(TENConstants.ICON_FACE_BOTH, 12, 12),   // BOTH 被动双向
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
                null,
                () -> {
                    Direction direction = logicalDirection(machine, logicalSide);
                    if (direction != null) {
                        // [修复] 调节改由客户端发起：serverClick 在服务端 UI 树执行，其
                        // UIState.selectedTransferMode 停留在面板打开时的快照页（类型按钮切页是
                        // localClick，仅更新客户端实例）→ 服务端永远调节打开时那页。
                        // localClick 在客户端树执行读到实时页，经 rpcToServer 标准 C→S 发包
                        // （同 rpcRequestFaceSync 模式）。26.1.2 参照实现同样带病，未照抄。
                        machine.rpcToServer("rpcCycleFaceMode", uiState.getSelectedTransferMode(), direction.get3DDataValue());
                        System.out.println("[FaceDiag] local-click sent mode=" + uiState.getSelectedTransferMode() + " dir=" + direction);
                    }
                },
                null,
                uiState::isPanelFullyOpen);
    }

    // ───── Modular 素材族 gauge（全量化对齐 26.1.2）─────
    // 注：旧图集版 energyGauge/fuelGauge/energyGaugeReveal/fuelGaugeReveal 已全量
    // 切换至 modular 独立纹理（energy_gauge_background/fill.png 等）后删除。

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
        int type = machine.machineType();
        return horizontalProgressModular(machine, x, y, 22, 16,
                progressArrowBg(type), progressArrowFill(type), showPercent);
    }

    public static ProgressBar progressGaugeWide(CmMachineBlockEntity machine, int x, int y, boolean showPercent) {
        return horizontalProgressModular(machine, x, y, 80, 5,
                com.modularmc.ten.TENConstants.PROGRESS_BAR_WIDE_BG, com.modularmc.ten.TENConstants.PROGRESS_BAR_WIDE_FILL, showPercent);
    }

    /**
     * 范围显示切换按钮（默认位置 (2,66)）：点击经 rpcToServer 发包，服务端切换 rangeVisible。
     * 需 BE 侧 rpcToggleRangeVisible 支持（未移植该 RPC 的机器误用会在服务端静默忽略）。
     */
    public static Button rangeDisplayToggleButton(CmMachineBlockEntity machine) {
        return rangeDisplayToggleButton(machine, 2, 66);
    }

    public static Button rangeDisplayToggleButton(CmMachineBlockEntity machine, int x, int y) {
        Button button = new Button();
        button.textStyle(style -> style.textShadow(false));
        button.setOnClick(event -> machine.rpcToServer("rpcToggleRangeVisible"));
        Runnable refresh = () -> button.setText(machine.rangeVisible ? ComponentHelper.translated("kenergyengineering.info.range_display_on") : ComponentHelper.translated("kenergyengineering.info.range_display_off"));
        refresh.run();
        button.addEventListener(UIEvents.TICK, event -> refresh.run());
        button.addEventListener(UIEvents.HOVER_TOOLTIPS, event -> event.hoverTooltips = new HoverTooltips(
                List.of(
                        ComponentHelper.translated(ComponentHelper.GOLD, "kenergyengineering.info.range_display"),
                        machine.rangeVisible ? ComponentHelper.translated("kenergyengineering.info.range_display_on_tip") : ComponentHelper.translated("kenergyengineering.info.range_display_off_tip"),
                        ComponentHelper.translated("kenergyengineering.info.range_display_click")),
                null, null, null));
        button.layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.left(x);
            layout.top(y);
            layout.width(44);
            layout.height(16);
        });
        return button;
    }

    /** 应用工具/武器附魔切换按钮：点击经 rpcToServer 发包，服务端切换 useEnchantments。 */
    public static Button useEnchantmentsToggleButton(CmMachineBlockEntity machine, int x, int y) {
        Button button = new Button();
        button.textStyle(style -> style.textShadow(false));
        button.setOnClick(event -> machine.rpcToServer("rpcToggleUseEnchantments"));
        Runnable refresh = () -> button.setText(machine.useEnchantments ? ComponentHelper.translated("kenergyengineering.info.use_enchantments_on") : ComponentHelper.translated("kenergyengineering.info.use_enchantments_off"));
        refresh.run();
        button.addEventListener(UIEvents.TICK, event -> refresh.run());
        button.addEventListener(UIEvents.HOVER_TOOLTIPS, event -> event.hoverTooltips = new HoverTooltips(
                List.of(
                        ComponentHelper.translated(ComponentHelper.GOLD, "kenergyengineering.info.use_enchantments"),
                        machine.useEnchantments ? ComponentHelper.translated("kenergyengineering.info.use_enchantments_on_tip") : ComponentHelper.translated("kenergyengineering.info.use_enchantments_off_tip"),
                        ComponentHelper.translated("kenergyengineering.info.use_enchantments_click")),
                null, null, null));
        button.layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.left(x);
            layout.top(y);
            layout.width(44);
            layout.height(16);
        });
        return button;
    }

    private static ProgressBar verticalGaugeModular(CmMachineBlockEntity machine,
                                                    int x, int y, int width, int height,
                                                    ResourceLocation bgTexture, ResourceLocation fillTexture,
                                                    java.util.function.ToDoubleFunction<CmMachineBlockEntity> percent,
                                                    Supplier<List<Component>> tooltipSupplier,
                                                    boolean displayValue) {
        // 底图/覆盖层位置审计：bg（barContainer 背景）与 fill（bar 纹理）同为 0,0 起、同 width/height、
        // 同一 absolute 锚点（x,y），层叠坐标完全重合；填充可见区域由 RevealProgressBar UV 裁切保证。
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
                                                         boolean showPercent) {
        var filled = SpriteTexture.of(fillTexture).setSprite(0, 0, width, height);
        var progress = absolute(new RevealProgressBar(filled), x, y, width, height);
        progress.barContainer(container -> container.style(style -> style.backgroundTexture(SpriteTexture.of(bgTexture).setSprite(0, 0, width, height)))
                .layout(layout -> layout.paddingAll(0)));
        progress.barBackground.style(style -> style.backgroundTexture(IGuiTexture.EMPTY));
        progress.label.setDisplay(false);
        progress.progressBarStyle(style -> style.fillDirection(FillDirection.LEFT_TO_RIGHT).interpolate(true).interpolateStep(0.1f));
        progress.bindDataSource(SupplierDataSource.of(() -> (float) progressPercent(machine)));
        if (showPercent) {
            progress.addEventListener(UIEvents.HOVER_TOOLTIPS, event -> event.hoverTooltips = new HoverTooltips(List.of(ComponentHelper.make((int) (progressPercent(machine) * 100) + "%")), null, null, null));
        }
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

    // 注：HANDLER 图集版 fluidGauge（(0,92) 流体槽 overlay，HANDLER 素材）已全量切至
    // fluidGaugeModular（FLUID_SLOT 独立纹理 18×50）后删除。
    /**
     * 18x50 modular 流体槽（FLUID_SLOT 底图 + FLUID_SLOT_OVERLAY 覆盖层）：背景 MACHINE_GUI 空面板的机器用。
     * TENFluidSlot 隐藏温度/气液态行（26.1.2 fluidGaugeBase 对齐）。
     * 层序：底图（流体下方）→ 流体柱 → slotOverlay 覆盖层（常驻顶层：空槽时叠槽、有流体时叠流体）。
     */
    public static FluidSlot fluidGaugeModular(CmMachineBlockEntity machine, int x, int y, int width, int height, int tankIndex) {
        var slot = absolute(new TENFluidSlot(), x, y, width, height);
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
     * 冷却剂消耗进度栏（P2-1）：绑定冷却器的冷却剂剩余比例，素材 progress_bar_wide_coolant。
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
        progress.addEventListener(UIEvents.HOVER_TOOLTIPS, event -> event.hoverTooltips = new HoverTooltips(List.of(ComponentHelper.translated("kenergyengineering.info.coolant_progress")), null, null, null));
        return progress;
    }

    /**
     * 迷你垂直进度箭头（P2-1）：输入槽右侧 8x54 素材（顶尖朝上/朝下），非动态（仅背景）。
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
            // 26.1.2 对齐（D1/P3+翻新 002）：全部机器统一 MACHINE_GUI 空面板，
            // 槽框/仪表由 modular 素材族绘制；专属背景 PNG 保留作回退素材不删。
            case MachineType.FURNACE, MachineType.PULVERIZER, MachineType.COMPRESSOR, MachineType.REFINER, MachineType.INDUCTION_FURNACE, MachineType.PSIONICANT, MachineType.ENCHANTMENT_FLUSHER, MachineType.MATTER_CONDENSER, MachineType.BEACON, MachineType.MOB_RIPPER, MachineType.QUARRY, MachineType.FARM, MachineType.CELL, MachineType.CREATIVE_CELL, MachineType.ENGINE_SOLAR, MachineType.ENGINE_EXTRACTION, MachineType.ENGINE_METAL, MachineType.ENGINE_BIOMASS, MachineType.BLOCK_BREAKER, MachineType.BLOCK_FORMER, MachineType.COOLER -> TENConstants.MACHINE_GUI;
            default -> TENConstants.LEGACY_SHEET;
        };
    }

    private static double fuelPercent(CmMachineBlockEntity machine) {
        return machine.maxFuel == 0 ? 0 : (double) machine.fuel / machine.maxFuel;
    }

    private static double progressPercent(CmMachineBlockEntity machine) {
        return machine.maxProgress == 0 ? 0 : (double) machine.progress / machine.maxProgress;
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
        Direction facing = Direction.from3DDataValue(machine.facingVal);
        return switch (logicalSide) {
            case 0 -> facing;
            case 1 -> facing.getOpposite();
            case 2 -> facing.getClockWise();
            case 3 -> facing.getCounterClockWise();
            case 4 -> Direction.UP;
            case 5 -> Direction.DOWN;
            default -> null;
        };
    }

    private static Supplier<List<Component>> fuelTooltip(CmMachineBlockEntity machine, boolean displayValue) {
        return () -> displayValue ? List.of(DisplayHelper.join(machine.fuel, machine.maxFuel)) : List.of(ComponentHelper.make((int) (fuelPercent(machine) * 100) + "%"));
    }

    private static Supplier<List<Component>> energyGaugeTooltip(CmMachineBlockEntity machine, boolean displayValue) {
        return () -> displayValue ? List.of(DisplayHelper.join(machine.energyStored, machine.maxEnergyStored)) : List.of(ComponentHelper.make((int) (energyPercent(machine) * 100) + "%"));
    }

    private static List<Component> fluidTooltip(CmMachineBlockEntity machine, int tankIndex, boolean showValue) {
        // 唯一原调用者 fluidGauge 已删；保留备用（渠道终端历史 tooltip 逻辑参考）。
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
        list.add(ComponentHelper.translated("kenergyengineering.info.bar_energy_fact"));
        list.add(ComponentHelper.translated(ComponentHelper.RED, Math.abs(machine.eff) + " FE/t"));
        list.add(ComponentHelper.translated("kenergyengineering.info.bar_energy_max"));
        list.add(ComponentHelper.translated(ComponentHelper.RED, machine.effAuc + " FE/t"));
        list.add(ComponentHelper.translated("kenergyengineering.info.bar_energy_in_max"));
        list.add(ComponentHelper.translated(ComponentHelper.RED, machine.energyRec + " FE/t"));
        list.add(ComponentHelper.translated("kenergyengineering.info.bar_energy_out_max"));
        list.add(ComponentHelper.translated(ComponentHelper.RED, machine.energyExt + " FE/t"));
        list.add(ComponentHelper.translated("kenergyengineering.info.bar_item_in_max"));
        list.add(ComponentHelper.translated(ComponentHelper.RED, machine.itemRec + " IS/t"));
        list.add(ComponentHelper.translated("kenergyengineering.info.bar_item_out_max"));
        list.add(ComponentHelper.translated(ComponentHelper.RED, machine.itemExt + " IS/t"));
        list.add(ComponentHelper.translated("kenergyengineering.info.bar_fluid_in_max"));
        list.add(ComponentHelper.translated(ComponentHelper.RED, machine.fluidRec + " mB/t"));
        list.add(ComponentHelper.translated("kenergyengineering.info.bar_fluid_out_max"));
        list.add(ComponentHelper.translated(ComponentHelper.RED, machine.fluidExt + " mB/t"));
        return list;
    }

    private static List<Component> redstoneTooltips(CmMachineBlockEntity machine) {
        String mode = switch (machine.redstoneMode) {
            case RedstoneMode.HIGH -> "kenergyengineering.info.high";
            case RedstoneMode.LOW -> "kenergyengineering.info.low";
            default -> "kenergyengineering.info.off";
        };
        return List.of(ComponentHelper.translated(ComponentHelper.GOLD, "kenergyengineering.info.bar_redstone", mode));
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
            this.upgradeOpen = upgradeOpen;
            snapshot().upgradeOpen = upgradeOpen;
            if (!upgradeOpen) {
                upgradePanelFullyOpen = false;
            }
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
    }
}
