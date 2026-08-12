package com.modularmc.ten.common.gui;

import com.modularmc.ten.TEN;
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
import net.minecraft.resources.Identifier;
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
import com.lowdragmc.lowdraglib2.gui.ui.elements.FluidSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
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

    private static final Identifier HANDLER = TEN.id("textures/gui/handler.png");

    private TENMachineBlockUIFactory() {}

    public static UIElement createRoot(Identifier background) {
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
                .style(style -> style.backgroundTexture(fullTexture(HANDLER, 176, 80)));
        root.addChild(new Label().setText(Component.literal("Missing block entity UI")));
        return new ModularUI(
                UI.of(root, List.of(StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.MC))),
                holder.player);
    }

    public static void addPlayerInventory(UIElement root) {
        var inventory = absolute(new InventorySlots(), 7, 83, 162, 58);
        inventory.hotbar.getLayout().marginTop(4.0f);
        inventory.apply(slot -> {
            slot.style(style -> style.backgroundTexture(IGuiTexture.EMPTY));
            slot.slotStyle(style -> style.slotOverlay(IGuiTexture.EMPTY).showSlotOverlayOnlyEmpty(false));
        });
        root.addChild(inventory);
    }

    public static void addUpgradeSlots(UIElement root, CmMachineBlockEntity machine) {
        root.addChild(textureElement(23, -37, 131, 36, sprite(HANDLER, 0, 211, 131, 36), null, null, null, null));
        int[] xs = { 32, 51, 70, 89, 108, 127 };
        for (int i = 0; i < xs.length; i++) {
            root.addChild(upgradeSlot(machine, i, xs[i], -28));
        }
    }

    public static void addCommonSidebar(UIElement root, BlockUIMenuType.BlockUIHolder holder, CmMachineBlockEntity machine, UIState uiState) {
        root.addChild(textureElement(-27, 0, 26, 26, sprite(HANDLER, 159, 211, 26, 26), () -> ideaTooltips(holder), null, null, null));
        root.addChild(textureElement(-27, 27, 26, 26, sprite(HANDLER, 132, 211, 26, 26), () -> energyInfoTooltips(machine), null, null, null));

        root.addChild(dynamicTextureElement(
                -27, 54, 26, 26,
                () -> sprite(HANDLER, 186, redstoneY(machine), 26, 26),
                () -> redstoneTooltips(machine),
                null,
                () -> cycleRedstone(machine),
                null));

        var controlButton = textureElement(-27, 81, 26, 26, sprite(HANDLER, 152, 40, 26, 26), () -> controlTooltip(), () -> uiState.setControlOpen(true), null, null);
        var controlPanel = textureElement(-61, 81, 60, 85, sprite(HANDLER, 91, 40, 60, 85), null, null, null, null);
        var closeButton = textureElement(-11, 81, 10, 10, IGuiTexture.EMPTY, () -> controlTooltip(), () -> uiState.setControlOpen(false), null, null);

        var energyModeBinding = createTransferModeBinding(-54, 145, TransferModeButtonState.U_ENERGY, 0, uiState);
        var itemModeBinding = createTransferModeBinding(-38, 145, TransferModeButtonState.U_ITEM, 1, uiState);
        var fluidModeBinding = createTransferModeBinding(-22, 145, TransferModeButtonState.U_FLUID, 2, uiState);
        var energyModeButton = energyModeBinding.element;
        var itemModeButton = itemModeBinding.element;
        var fluidModeButton = fluidModeBinding.element;

        Runnable refreshTransferButtons = () -> {
            energyModeBinding.refresh.run();
            itemModeBinding.refresh.run();
            fluidModeBinding.refresh.run();
        };
        // Replace each button's click handler with one that refreshes all three immediately
        rebindClickToRefreshAll(energyModeButton, 0, uiState, refreshTransferButtons);
        rebindClickToRefreshAll(itemModeButton, 1, uiState, refreshTransferButtons);
        rebindClickToRefreshAll(fluidModeButton, 2, uiState, refreshTransferButtons);

        var frontButton = faceModeElement(machine, uiState, -39, 103, 0, "kenergyengineering.info.front");
        var backButton = faceModeElement(machine, uiState, -25, 117, 1, "kenergyengineering.info.back");
        var leftButton = faceModeElement(machine, uiState, -53, 103, 2, "kenergyengineering.info.left");
        var rightButton = faceModeElement(machine, uiState, -25, 103, 3, "kenergyengineering.info.right");
        var upButton = faceModeElement(machine, uiState, -39, 89, 4, "kenergyengineering.info.up");
        var downButton = faceModeElement(machine, uiState, -39, 117, 5, "kenergyengineering.info.down");

        root.addChild(controlButton);
        root.addChild(controlPanel);
        root.addChild(closeButton);
        root.addChild(energyModeButton);
        root.addChild(itemModeButton);
        root.addChild(fluidModeButton);
        root.addChild(frontButton);
        root.addChild(backButton);
        root.addChild(leftButton);
        root.addChild(rightButton);
        root.addChild(upButton);
        root.addChild(downButton);

        Runnable syncSidebar = () -> {
            boolean open = uiState.isControlOpen();
            controlButton.setDisplay(!open);
            controlPanel.setDisplay(open);
            closeButton.setDisplay(open);
            energyModeButton.setDisplay(open);
            itemModeButton.setDisplay(open);
            fluidModeButton.setDisplay(open);
            frontButton.setDisplay(open);
            backButton.setDisplay(open);
            leftButton.setDisplay(open);
            rightButton.setDisplay(open);
            upButton.setDisplay(open);
            downButton.setDisplay(open);
        };
        syncSidebar.run();
        root.addEventListener(UIEvents.TICK, event -> syncSidebar.run());
    }

    /**
     * Creates a standard 18x18 machine item slot with no background
     * ({@link IGuiTexture#EMPTY}).
     * <p>
     * Used by all non-modular machines: their legacy background PNG already
     * draws the slot frame, so adding a modular slot background would cause a
     * double border.
     */
    public static ItemSlot machineSlot(CmMachineBlockEntity machine, int index, int x, int y) {
        return machineSlot(machine, index, x, y, 18, IGuiTexture.EMPTY);
    }

    /**
     * Creates an 18x18 machine item slot with the modular small-slot background
     * ({@link TENConstants#ITEM_SLOT_SMALL}).
     * <p>
     * Modular-only (smelter/pulverizer/compressor/refiner/indfur/psionicant/encflu):
     * their background is the blank {@link TENConstants#MACHINE_GUI}, so the
     * slot frame must come from the modular texture family.
     */
    public static ItemSlot machineSlotModular(CmMachineBlockEntity machine, int index, int x, int y) {
        return machineSlot(machine, index, x, y, 18, fullTexture(TENConstants.ITEM_SLOT_SMALL, 18, 18));
    }

    /**
     * Creates an 18x18 channel item slot bound to the channel facade handler
     * ({@link com.modularmc.ten.common.blockentity.channel.ChannelItemHandlerFacade}).
     * <p>
     * 末影箱模式（T003）：接入后槽位直接绑定共享存储（服务端动态解析），未接入/客户端
     * 绑定本地缓冲。join/leave 切换后槽位自动指向新后端，无需重建 UI。
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
                // 末影箱动态堆叠（P3-T8）：上限=共享槽位上限（64×成员数），不按物品原版 64 封顶。
                // 否则 Slot.safeInsert/getMaxStackSize(stack) 会把 GUI 堆叠/快速移动钳制在原版上限。
                return getMaxStackSize();
            }
        };
        return itemSlot(slot, x, y, false, 18, fullTexture(TENConstants.ITEM_SLOT_SMALL, 18, 18));
    }

    /**
     * Creates a large (26x26) machine item slot.
     * <p>
     * Background: {@link TENConstants#ITEM_SLOT_LARGE}. The slot element is
     * 26x26 with 5px padding so the 16x16 content renders centered; the whole
     * 26x26 area remains interactive (P1: LDLib2 ItemSlot native 26x26).
     * Modular-only: only the 7 modular machines use the 26x26 output slot.
     */
    public static ItemSlot machineSlotLarge(CmMachineBlockEntity machine, int index, int x, int y) {
        return machineSlot(machine, index, x, y, 26, fullTexture(TENConstants.ITEM_SLOT_LARGE, 26, 26));
    }

    private static ItemSlot machineSlot(CmMachineBlockEntity machine, int index, int x, int y, int size, IGuiTexture background) {
        Slot slot = new SlotItemHandler(machine.itemHandler, index, 0, 0) {

            @Override
            public boolean mayPlace(net.minecraft.world.item.ItemStack stack) {
                var type = machine.slotType(getSlotIndex());
                return type.canIn() && machine.valid(getSlotIndex(), stack) && super.mayPlace(stack);
            }
        };
        return itemSlot(slot, x, y, false, size, background);
    }

    /**
     * Creates an upgrade slot with the standard item-slot background (HANDLER 227,0,18,18).
     * <p>
     * <strong>Tooltip behavior:</strong>
     * <ul>
     * <li><em>Empty slot:</em> Shows the localized "Upgrade Slot" tooltip
     * ({@code kenergyengineering.upgrade_slot}).</li>
     * <li><em>Non-empty slot:</em> The HOVER_TOOLTIPS handler does NOT override
     * the event, preserving the native ItemStack tooltip from the ItemSlot.</li>
     * </ul>
     */
    private static ItemSlot upgradeSlot(CmMachineBlockEntity machine, int index, int x, int y) {
        SlotItemHandler slot = new SlotItemHandler(machine.upgradeHandler, index, 0, 0);
        var itemSlot = itemSlot(slot, x, y, false, 18, IGuiTexture.EMPTY);
        // Use the standard item-slot background sprite so the slot is visible
        itemSlot.style(style -> style.backgroundTexture(sprite(HANDLER, 227, 0, 18, 18)));
        // Register tooltip: only for empty slots; non-empty preserves native ItemStack tooltip
        itemSlot.addEventListener(UIEvents.HOVER_TOOLTIPS, event -> {
            if (slot.getItem().isEmpty()) {
                event.hoverTooltips = HoverTooltips.create(emptyUpgradeSlotTooltip());
            }
            // Non-empty: do NOT set event.hoverTooltips → native ItemStack tooltip is preserved
        });
        return itemSlot;
    }

    private static ItemSlot itemSlot(Slot slot, int x, int y, boolean isPlayerSlot, int size, IGuiTexture background) {
        var itemSlot = absolute(new ItemSlot(slot), x, y, size, size);
        itemSlot.style(style -> style.backgroundTexture(background));
        itemSlot.slotStyle(style -> style
                .slotOverlay(IGuiTexture.EMPTY)
                .showSlotOverlayOnlyEmpty(false)
                .isPlayerSlot(isPlayerSlot));
        if (size == 26) {
            // 26x26 大槽：padding 5px → 内容 16x16 居中（P1 结论）
            itemSlot.layout(layout -> layout.paddingAll(5));
        }
        return itemSlot;
    }

    private static UIElement faceModeElement(CmMachineBlockEntity machine, UIState uiState, int x, int y, int logicalSide, String tooltipKey) {
        return dynamicTextureElement(
                x, y, 12, 12,
                () -> sprite(HANDLER, 121, 126 + faceMode(machine, uiState.getSelectedTransferMode(), logicalSide) * 12, 12, 12),
                () -> List.of(
                        ComponentHelper.translated(ComponentHelper.GOLD, tooltipKey),
                        ComponentHelper.translated("kenergyengineering.info." + FaceOption.toStr(faceMode(machine, uiState.getSelectedTransferMode(), logicalSide)))),
                null,
                () -> {
                    Direction direction = logicalDirection(machine, logicalSide);
                    if (direction != null) {
                        // LDLib2 不拦截 @RPCMethod 方法调用：直接调用会在客户端本地执行
                        // （服务端状态不变，静默失败），必须经 rpcToServer 显式发包到服务端。
                        machine.rpcToServer("rpcCycleFaceMode", uiState.getSelectedTransferMode(), direction.get3DDataValue());
                    }
                },
                uiState::isControlOpen);
    }

    public static RevealProgressBar energyGauge(CmMachineBlockEntity machine, int x, int y, int width, int height, int xOff, int yOff, boolean displayValue) {
        return verticalGauge(machine, x, y, width, height, xOff, yOff, TENMachineBlockUIFactory::energyPercent, energyGaugeTooltip(machine, displayValue), displayValue);
    }

    public static RevealProgressBar fuelGauge(CmMachineBlockEntity machine, int x, int y, int width, int height, int xOff, int yOff, boolean displayValue) {
        return verticalGauge(machine, x, y, width, height, xOff, yOff, TENMachineBlockUIFactory::fuelPercent, fuelTooltip(machine, displayValue), displayValue);
    }

    private static RevealProgressBar verticalGauge(CmMachineBlockEntity machine,
                                                   int x, int y, int width, int height,
                                                   int xOff, int yOff,
                                                   java.util.function.ToDoubleFunction<CmMachineBlockEntity> percent,
                                                   Supplier<List<Component>> tooltipSupplier,
                                                   boolean displayValue) {
        var filled = SpriteTexture.of(HANDLER).setSprite(xOff, yOff + height, width, height);
        var progress = absolute(new RevealProgressBar(filled), x, y, width, height);
        progress.barContainer(container -> container.style(style -> style.backgroundTexture(sprite(HANDLER, xOff, yOff, width, height)))
                .layout(layout -> layout.paddingAll(0)));
        progress.barBackground.style(style -> style.backgroundTexture(IGuiTexture.EMPTY));
        progress.label.setDisplay(false);
        progress.progressBarStyle(style -> style.fillDirection(FillDirection.DOWN_TO_UP).interpolate(false));
        progress.bindDataSource(SupplierDataSource.of(() -> (float) percent.applyAsDouble(machine)));
        progress.addEventListener(UIEvents.HOVER_TOOLTIPS, event -> event.hoverTooltips = HoverTooltips.create(tooltipSupplier.get().toArray()));
        return progress;
    }

    // ───── Modular 素材族 gauge（D1：机器 GUI 部件素材切换）─────

    /**
     * Modular 素材族能量条（14x46）：背景 ENERGY_GAUGE_BG、填充 ENERGY_GAUGE_FILL。
     */
    public static RevealProgressBar energyGaugeModular(CmMachineBlockEntity machine, int x, int y, boolean displayValue) {
        return verticalGaugeModular(machine, x, y, 14, 46,
                TENConstants.ENERGY_GAUGE_BG, TENConstants.ENERGY_GAUGE_FILL,
                TENMachineBlockUIFactory::energyPercent, energyGaugeTooltip(machine, displayValue), displayValue);
    }

    /**
     * Modular 素材族燃料条（13x13）：背景 FUEL_GAUGE_BG、填充 FUEL_GAUGE_FILL。
     */
    public static RevealProgressBar fuelGaugeModular(CmMachineBlockEntity machine, int x, int y, boolean displayValue) {
        return verticalGaugeModular(machine, x, y, 13, 13,
                TENConstants.FUEL_GAUGE_BG, TENConstants.FUEL_GAUGE_FILL,
                TENMachineBlockUIFactory::fuelPercent, fuelTooltip(machine, displayValue), displayValue);
    }

    /**
     * Modular 素材族进度箭头（22x16）：按机器类型映射 PROGRESS_ARROW_&lt;机器&gt;_BG/FILL。
     */
    public static RevealProgressBar progressGaugeModular(CmMachineBlockEntity machine, int x, int y, boolean showPercent) {
        int type = machine.machineType();
        return horizontalProgressModular(machine, x, y, 22, 16,
                progressArrowBg(type), progressArrowFill(type), showPercent);
    }

    /**
     * Modular 素材族宽进度条（80x5，L2R）：背景 PROGRESS_BAR_WIDE_BG、填充 PROGRESS_BAR_WIDE_FILL。
     * <p>
     * GUI 翻新 002：5 台未翻新机器（condenser/beacon/mob_ripper/quarry/farm）进度统一使用
     * 宽进度条，与旧 handler(97,0)/(97,5) 逐像素一致。
     */
    public static RevealProgressBar progressGaugeWide(CmMachineBlockEntity machine, int x, int y, boolean showPercent) {
        return horizontalProgressModular(machine, x, y, 80, 5,
                TENConstants.PROGRESS_BAR_WIDE_BG, TENConstants.PROGRESS_BAR_WIDE_FILL, showPercent);
    }

    /**
     * 创建一个 18x18 能量充放电槽（modular 素材族）。
     * <p>
     * GUI 翻新 002（D2）：Cell/CreativeCell 双槽语义——{@code chargeIn=true} 使用
     * ITEM_SLOT_SMALL_CHARGE（绿流入=充电），否则使用 ITEM_SLOT_SMALL_DISCHARGE（红流出=放电）。
     *
     * @param machine  所属机器
     * @param index    槽索引（绑定 machine.itemHandler）
     * @param x        槽左缘 x
     * @param y        槽顶 y
     * @param chargeIn true=充电槽（charge），false=放电槽（discharge）
     */
    public static ItemSlot machineSlotPower(CmMachineBlockEntity machine, int index, int x, int y, boolean chargeIn) {
        var texture = chargeIn ? TENConstants.ITEM_SLOT_SMALL_CHARGE : TENConstants.ITEM_SLOT_SMALL_DISCHARGE;
        return machineSlot(machine, index, x, y, 18, fullTexture(texture, 18, 18));
    }

    /**
     * 创建一个通用 18x18 modular 小槽背景槽（ITEM_SLOT_SMALL）。
     * <p>
     * GUI 翻新 002（P4）：Pipe filterSlot 复用——槽对象来自 {@link net.minecraft.world.Container}
     * 包装的 filterContainer（非 itemHandler），因此不走 machineSlot 系列。
     *
     * @param slot 任意 Slot（可为非 itemHandler 来源，如 Pipe 过滤容器）
     * @param x    槽左缘 x
     * @param y    槽顶 y
     */
    public static ItemSlot itemSlotModular(Slot slot, int x, int y) {
        return itemSlot(slot, x, y, false, 18, fullTexture(TENConstants.ITEM_SLOT_SMALL, 18, 18));
    }

    private static RevealProgressBar verticalGaugeModular(CmMachineBlockEntity machine,
                                                          int x, int y, int width, int height,
                                                          Identifier bgTexture, Identifier fillTexture,
                                                          java.util.function.ToDoubleFunction<CmMachineBlockEntity> percent,
                                                          Supplier<List<Component>> tooltipSupplier,
                                                          boolean displayValue) {
        var filled = SpriteTexture.of(fillTexture).setSprite(0, 0, width, height);
        var progress = absolute(new RevealProgressBar(filled), x, y, width, height);
        progress.barContainer(container -> container.style(style -> style.backgroundTexture(SpriteTexture.of(bgTexture).setSprite(0, 0, width, height)))
                .layout(layout -> layout.paddingAll(0)));
        progress.barBackground.style(style -> style.backgroundTexture(IGuiTexture.EMPTY));
        progress.label.setDisplay(false);
        progress.progressBarStyle(style -> style.fillDirection(FillDirection.DOWN_TO_UP).interpolate(false));
        progress.bindDataSource(SupplierDataSource.of(() -> (float) percent.applyAsDouble(machine)));
        progress.addEventListener(UIEvents.HOVER_TOOLTIPS, event -> event.hoverTooltips = HoverTooltips.create(tooltipSupplier.get().toArray()));
        return progress;
    }

    private static RevealProgressBar horizontalProgressModular(CmMachineBlockEntity machine,
                                                               int x, int y, int width, int height,
                                                               Identifier bgTexture, Identifier fillTexture,
                                                               boolean showPercent) {
        var filled = SpriteTexture.of(fillTexture).setSprite(0, 0, width, height);
        var progress = absolute(new RevealProgressBar(filled), x, y, width, height);
        progress.barContainer(container -> container.style(style -> style.backgroundTexture(SpriteTexture.of(bgTexture).setSprite(0, 0, width, height)))
                .layout(layout -> layout.paddingAll(0)));
        progress.barBackground.style(style -> style.backgroundTexture(IGuiTexture.EMPTY));
        progress.label.setDisplay(false);
        progress.progressBarStyle(style -> style.fillDirection(FillDirection.LEFT_TO_RIGHT).interpolate(false));
        progress.bindDataSource(SupplierDataSource.of(() -> (float) progressPercent(machine)));
        if (showPercent) {
            progress.addEventListener(UIEvents.HOVER_TOOLTIPS, event -> event.hoverTooltips = HoverTooltips.create(ComponentHelper.make((int) (progressPercent(machine) * 100) + "%")));
        }
        return progress;
    }

    /**
     * Modular 素材族 mini 装饰组件（整件 8x54 素材），用于「上下组合输入槽」机器
     * （compressor / encflu），静态标示上下输入槽的流向关系。
     * <p>
     * 纯装饰：整件 blit，不绑定 progressPercent 动画；加工进度由各机器另行添加的
     * 进度箭头（progressGaugeModular）承担，二者水平错开不重叠。
     * <p>
     * 几何（素材画布 8x54 与槽位垂直跨度精确对齐）：
     * <ul>
     * <li>水平：紧贴输入槽右缘，anchorX = inputX + slotSize（无间距）</li>
     * <li>垂直：画布顶 = 上方输入槽 y（inputTopY），画布高 54 恰好覆盖到
     * 下方输入槽底（inputBottomY + slotSize）；素材内非透明 8x41 区域
     * （箭头/方块一体）按素材原样呈现</li>
     * </ul>
     * 两版素材互为垂直镜像、方向内嵌于素材：compressor 顶尖朝上（目标在上），
     * encflu 尖朝下（目标在下）。{@code targetOnTop} 为调用方意图声明，与素材
     * 内嵌方向保持一致；垂直位置由画布与槽位对齐决定，不参与几何计算。
     *
     * @param inputX      输入槽左缘 x（上下两槽同 x）
     * @param inputTopY   上方输入槽 y
     * @param slotSize    输入槽尺寸（18）
     * @param texture     mini 素材常量（PROGRESS_ARROW_MINI_COMPRESSOR_BG / PROGRESS_ARROW_MINI_ENCFLU_BG）
     * @param targetOnTop 目标点箭头是否位于上方槽位（compressor=true，encflu=false）
     */
    public static UIElement verticalProgressMini(int inputX, int inputTopY, int slotSize,
                                                 Identifier texture, boolean targetOnTop) {
        // 整件 8x54：紧贴输入槽右缘（无间距），垂直对齐上方输入槽顶（画布底恰好覆盖下方槽底）
        return textureElement(inputX + slotSize, inputTopY, 8, 54,
                fullTexture(texture, 8, 54), null, null, null, null);
    }

    /**
     * 机器类型 → modular 进度箭头背景常量映射（集中一处）。
     */
    private static Identifier progressArrowBg(int machineType) {
        return switch (machineType) {
            case MachineType.FURNACE -> TENConstants.PROGRESS_ARROW_SMELTER_BG;
            case MachineType.PULVERIZER -> TENConstants.PROGRESS_ARROW_PULVERIZER_BG;
            case MachineType.COMPRESSOR -> TENConstants.PROGRESS_ARROW_COMPRESSOR_BG;
            case MachineType.REFINER -> TENConstants.PROGRESS_ARROW_REFINER_BG;
            case MachineType.INDUCTION_FURNACE -> TENConstants.PROGRESS_ARROW_INDUCTION_FURNACE_BG;
            case MachineType.PSIONICANT -> TENConstants.PROGRESS_ARROW_PSIONICANT_BG;
            case MachineType.ENCHANTMENT_FLUSHER -> TENConstants.PROGRESS_ARROW_SMELTER_BG;
            // 仅 7 台 Modular 机器有 progressArrow 素材；其他机器走旧版 progressGauge（HANDLER 素材）。
            // 误用 progressGaugeModular 时 fail-fast（设计如此，勿改为静默返回空纹理）。
            default -> throw new IllegalStateException("No modular progress arrow background for machine type " + machineType);
        };
    }

    /**
     * 机器类型 → modular 进度箭头填充常量映射（集中一处）。
     */
    private static Identifier progressArrowFill(int machineType) {
        return switch (machineType) {
            case MachineType.FURNACE -> TENConstants.PROGRESS_ARROW_SMELTER_FILL;
            case MachineType.PULVERIZER -> TENConstants.PROGRESS_ARROW_PULVERIZER_FILL;
            case MachineType.COMPRESSOR -> TENConstants.PROGRESS_ARROW_COMPRESSOR_FILL;
            case MachineType.REFINER -> TENConstants.PROGRESS_ARROW_REFINER_FILL;
            case MachineType.INDUCTION_FURNACE -> TENConstants.PROGRESS_ARROW_INDUCTION_FURNACE_FILL;
            case MachineType.PSIONICANT -> TENConstants.PROGRESS_ARROW_PSIONICANT_FILL;
            case MachineType.ENCHANTMENT_FLUSHER -> TENConstants.PROGRESS_ARROW_SMELTER_FILL;
            // 仅 7 台 Modular 机器有 progressArrow 素材；其他机器走旧版 progressGauge（HANDLER 素材）。
            // 误用 progressGaugeModular 时 fail-fast（设计如此，勿改为静默返回空纹理）。
            default -> throw new IllegalStateException("No modular progress arrow fill for machine type " + machineType);
        };
    }

    public static RevealProgressBar progressGauge(CmMachineBlockEntity machine, int x, int y, int width, int height, int xOff, int yOff, boolean showPercent) {
        var filled = SpriteTexture.of(HANDLER).setSprite(xOff, yOff + height, width, height);
        var progress = absolute(new RevealProgressBar(filled), x, y, width, height);
        progress.barContainer(container -> container.style(style -> style.backgroundTexture(sprite(HANDLER, xOff, yOff, width, height)))
                .layout(layout -> layout.paddingAll(0)));
        progress.barBackground.style(style -> style.backgroundTexture(IGuiTexture.EMPTY));
        progress.label.setDisplay(false);
        progress.progressBarStyle(style -> style.fillDirection(FillDirection.LEFT_TO_RIGHT).interpolate(false));
        progress.bindDataSource(SupplierDataSource.of(() -> (float) progressPercent(machine)));
        if (showPercent) {
            progress.addEventListener(UIEvents.HOVER_TOOLTIPS, event -> event.hoverTooltips = HoverTooltips.create(ComponentHelper.make((int) (progressPercent(machine) * 100) + "%")));
        }
        return progress;
    }

    /**
     * Creates a fluid gauge with no background.
     * <p>
     * Non-modular machines (e.g. Condenser) keep the legacy behavior: their
     * background PNG already draws the fluid tank frame, so adding the modular
     * FLUID_SLOT texture would cause a double border.
     */
    public static FluidSlot fluidGauge(CmMachineBlockEntity machine, int x, int y, int width, int height, int tankIndex) {
        return fluidGaugeBase(machine, x, y, width, height, tankIndex, IGuiTexture.EMPTY);
    }

    /**
     * Creates a fluid gauge with the modular fluid-slot background
     * ({@link TENConstants#FLUID_SLOT}, 18x50).
     * <p>
     * Modular-only: the 6 modular machines render on the blank
     * {@link TENConstants#MACHINE_GUI} background, so the tank frame must come
     * from the modular texture family.
     */
    public static FluidSlot fluidGaugeModular(CmMachineBlockEntity machine, int x, int y, int width, int height, int tankIndex) {
        return fluidGaugeBase(machine, x, y, width, height, tankIndex,
                SpriteTexture.of(TENConstants.FLUID_SLOT).setSprite(0, 0, 18, 50));
    }

    /**
     * Creates a fixed XP fluid output slot for the Furnace smelter.
     * <p>
     * Always visible — not gated by Knowledge. The tank and its capability
     * are permanent; Knowledge only controls whether XP fluid is produced
     * during recipe processing.
     * <p>
     * Uses the standard {@link FluidSlot#bind(ResourceHandler, int)} pattern
     * from LDLib2, with no per-tick manual refresh or custom sync protocol.
     *
     * @param machine the furnace block entity
     * @param x       horizontal position
     * @param y       vertical position
     * @param width   slot width (typically 18)
     * @param height  slot height (typically 50)
     * @return a configured FluidSlot bound to tank index 0
     */
    public static FluidSlot createXpFluidSlot(CmMachineBlockEntity machine, int x, int y, int width, int height) {
        // XP 槽为标准 18x50 流体槽（refiner 样式）→ 使用 modular FLUID_SLOT 背景，tank 0 绑定保持
        return fluidGaugeModular(machine, x, y, width, height, 0);
    }

    public static FluidSlot fluidGaugeWithBackground(CmMachineBlockEntity machine, int x, int y, int width, int height, int tankIndex) {
        var background = SpriteTexture.of(HANDLER).setSprite(0, 92, width, height);
        background.transform(-2.0f, -2.0f);
        return fluidGaugeBase(machine, x, y, width, height, tankIndex, background);
    }

    private static FluidSlot fluidGaugeBase(CmMachineBlockEntity machine, int x, int y, int width, int height, int tankIndex, IGuiTexture background) {
        var slot = absolute(new TENFluidSlot(), x, y, width, height);
        slot.style(style -> style.backgroundTexture(background));
        slot.slotStyle(style -> style
                .slotOverlay(IGuiTexture.EMPTY)
                .showSlotOverlayOnlyEmpty(false)
                .fillDirection(FillDirection.DOWN_TO_UP)
                .showFluidTooltips(true));
        slot.amountLabel.setDisplay(false);
        var handler = machine.getFluidResourceHandler(null);
        if (handler == null) {
            throw new IllegalStateException("fluidGauge on machine with no tanks: " + machine);
        }
        slot.bind(handler, tankIndex);
        return slot;
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
            element.addEventListener(UIEvents.HOVER_TOOLTIPS, event -> event.hoverTooltips = HoverTooltips.create(tooltipSupplier.get().toArray()));
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

    /**
     * Registers a tick-driven display visibility supplier for the given element.
     * <p>
     * Initially sets visibility via {@code visibleSupplier.getAsBoolean()},
     * then re-evaluates on every {@link UIEvents#TICK} event.
     *
     * @param element         the UI element whose display is controlled
     * @param visibleSupplier supplies true/false each tick for visibility
     */
    private static void syncDisplay(UIElement element, BooleanSupplier visibleSupplier) {
        Runnable update = () -> element.setDisplay(visibleSupplier.getAsBoolean());
        update.run();
        element.addEventListener(UIEvents.TICK, event -> update.run());
    }

    /**
     * Creates a transfer mode button element and its refresh runnable.
     * The texture switches between unselected/selected × normal/hover
     * based on {@link UIState#getSelectedTransferMode()} and the element's hover state.
     * <p>
     * Callers should compose the individual refresh runnables into a
     * combined {@code refreshAll} and pass it to {@link #rebindClickToRefreshAll}.
     */
    private static TransferModeBinding createTransferModeBinding(int x, int y, int textureU, int modeIndex, UIState uiState) {
        var element = absolute(new UIElement(), x, y, TransferModeButtonState.SIZE, TransferModeButtonState.SIZE);
        Runnable refresh = () -> {
            boolean selected = uiState.getSelectedTransferMode() == modeIndex;
            int stateIndex = TransferModeButtonState.stateIndex(selected, element.isHover());
            int v = TransferModeButtonState.textureV(stateIndex);
            element.style(style -> style.backgroundTexture(
                    sprite(HANDLER, textureU, v, TransferModeButtonState.SIZE, TransferModeButtonState.SIZE)));
        };
        refresh.run();
        element.addEventListener(UIEvents.TICK, event -> refresh.run());
        element.addEventListener(UIEvents.HOVER_TOOLTIPS, event -> {
            event.hoverTooltips = HoverTooltips.create(transferModeTooltip(modeIndex).toArray());
        });
        return new TransferModeBinding(element, refresh);
    }

    /**
     * Replaces the mouse-down handler on a transfer mode button so that
     * clicking sets the new mode and immediately refreshes all three buttons
     * within the same event cycle.
     */
    private static void rebindClickToRefreshAll(UIElement element, int modeIndex, UIState uiState, Runnable refreshAll) {
        // Remove existing MOUSE_DOWN listeners by clearing and re-adding with priority
        element.addEventListener(UIEvents.MOUSE_DOWN, event -> {
            if (event.button == 0) {
                uiState.setSelectedTransferMode(modeIndex);
                refreshAll.run();
            }
        });
    }

    /**
     * Bundles a transfer mode button element with its texture refresh runnable.
     */
    private record TransferModeBinding(UIElement element, Runnable refresh) {}

    private static List<Component> transferModeTooltip(int modeIndex) {
        return switch (modeIndex) {
            case 0 -> List.of(ComponentHelper.translated(ComponentHelper.GOLD, "kenergyengineering.info.bar_mode", "kenergyengineering.info.energy"));
            case 1 -> List.of(ComponentHelper.translated(ComponentHelper.GOLD, "kenergyengineering.info.bar_mode", "kenergyengineering.info.item"));
            case 2 -> List.of(ComponentHelper.translated(ComponentHelper.GOLD, "kenergyengineering.info.bar_mode", "kenergyengineering.info.fluid"));
            default -> List.of();
        };
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

    private static IGuiTexture sprite(Identifier resourceLocation, int x, int y, int width, int height) {
        return SpriteTexture.of(resourceLocation).setSprite(x, y, width, height);
    }

    private static IGuiTexture fullTexture(Identifier resourceLocation, int width, int height) {
        return SpriteTexture.of(resourceLocation).setSprite(0, 0, width, height);
    }

    public static Identifier backgroundFor(int machineType) {
        return switch (machineType) {
            // D1/P3：七台 Modular 机器统一使用空面板 machine_gui.png（部件素材由 modular 素材族绘制）
            case MachineType.FURNACE, MachineType.PULVERIZER, MachineType.COMPRESSOR, MachineType.REFINER, MachineType.INDUCTION_FURNACE, MachineType.PSIONICANT, MachineType.ENCHANTMENT_FLUSHER -> TENConstants.MACHINE_GUI;
            // GUI 翻新 002（D6）：11 台未翻新机器背景统一切换 MACHINE_GUI（专属背景 PNG 保留回退）
            case MachineType.MATTER_CONDENSER, MachineType.BEACON, MachineType.MOB_RIPPER, MachineType.QUARRY, MachineType.FARM, MachineType.CELL, MachineType.CREATIVE_CELL, MachineType.ENGINE_SOLAR, MachineType.ENGINE_EXTRACTION, MachineType.ENGINE_METAL, MachineType.ENGINE_BIOMASS -> TENConstants.MACHINE_GUI;
            default -> HANDLER;
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

    private static int redstoneY(CmMachineBlockEntity machine) {
        return switch (machine.redstoneMode) {
            case RedstoneMode.HIGH -> 211;
            case RedstoneMode.LOW -> 184;
            default -> 157;
        };
    }

    private static void cycleRedstone(CmMachineBlockEntity machine) {
        int mode = machine.redstoneMode + 1;
        if (mode >= RedstoneMode.size()) {
            mode = RedstoneMode.OFF;
        }
        // 同 rpcCycleFaceMode：必须经 rpcToServer 显式发包到服务端。
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

    /**
     * Returns the localized component for an empty upgrade slot tooltip.
     * Used by {@link #upgradeSlot(CmMachineBlockEntity, int, int, int)}
     * when the slot contains no item.
     */
    private static Component emptyUpgradeSlotTooltip() {
        return ComponentHelper.translated("kenergyengineering.upgrade_slot");
    }

    public static final class UIState {

        private static final Map<String, Snapshot> CACHE = new ConcurrentHashMap<>();

        private final String key;
        private int selectedTransferMode;
        private boolean controlOpen;

        public UIState(BlockUIMenuType.BlockUIHolder holder) {
            this.key = holder.player.getUUID() + "@" + holder.pos.asLong();
            Snapshot snapshot = CACHE.computeIfAbsent(key, ignored -> new Snapshot());
            this.selectedTransferMode = snapshot.selectedTransferMode;
            this.controlOpen = snapshot.controlOpen;
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
        }

        private Snapshot snapshot() {
            return CACHE.computeIfAbsent(key, ignored -> new Snapshot());
        }
    }

    private static final class Snapshot {

        private int selectedTransferMode;
        private boolean controlOpen;
    }
}
