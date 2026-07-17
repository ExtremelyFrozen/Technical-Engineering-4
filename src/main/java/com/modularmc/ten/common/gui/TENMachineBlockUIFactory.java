package com.modularmc.ten.common.gui;

import com.modularmc.ten.TEN;
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
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar;
import com.lowdragmc.lowdraglib2.gui.ui.elements.inventory.InventorySlots;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCSender;
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
            final int slotIndex = i;
            root.addChild(upgradeSlot(machine, i, xs[i], -28));
            root.addChild(textureElement(
                    xs[slotIndex], -28, 18, 18,
                    sprite(HANDLER, 227, 0, 18, 18),
                    () -> upgradeTooltip(machine, slotIndex),
                    null,
                    null,
                    null));
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

        var energyModeButton = dynamicTextureElement(-54, 145, 14, 14,
                () -> sprite(HANDLER, 91, 126 + (uiState.getSelectedTransferMode() == 0 ? 14 : 0), 14, 14),
                () -> energyModeTooltip(), () -> uiState.setSelectedTransferMode(0), null, null);
        var itemModeButton = dynamicTextureElement(-38, 145, 14, 14,
                () -> sprite(HANDLER, 106, 126 + (uiState.getSelectedTransferMode() == 1 ? 14 : 0), 14, 14),
                () -> itemModeTooltip(), () -> uiState.setSelectedTransferMode(1), null, null);
        var fluidModeButton = dynamicTextureElement(-22, 145, 14, 14,
                () -> sprite(HANDLER, 76, 126 + (uiState.getSelectedTransferMode() == 2 ? 14 : 0), 14, 14),
                () -> fluidModeTooltip(), () -> uiState.setSelectedTransferMode(2), null, null);

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

    public static ItemSlot machineSlot(CmMachineBlockEntity machine, int index, int x, int y) {
        Slot slot = new SlotItemHandler(machine.itemHandler, index, 0, 0) {

            @Override
            public boolean mayPlace(net.minecraft.world.item.ItemStack stack) {
                var type = machine.slotType(getSlotIndex());
                return type.canIn() && machine.valid(getSlotIndex(), stack) && super.mayPlace(stack);
            }
        };
        return itemSlot(slot, x, y, false);
    }

    private static ItemSlot upgradeSlot(CmMachineBlockEntity machine, int index, int x, int y) {
        return itemSlot(new SlotItemHandler(machine.upgradeHandler, index, 0, 0), x, y, false);
    }

    private static ItemSlot itemSlot(Slot slot, int x, int y, boolean isPlayerSlot) {
        var itemSlot = absolute(new ItemSlot(slot), x, y, 18, 18);
        itemSlot.style(style -> style.backgroundTexture(IGuiTexture.EMPTY));
        itemSlot.slotStyle(style -> style
                .slotOverlay(IGuiTexture.EMPTY)
                .showSlotOverlayOnlyEmpty(false)
                .isPlayerSlot(isPlayerSlot));
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
                        machine.rpcCycleFaceMode(RPCSender.ofServer(), uiState.getSelectedTransferMode(), direction.get3DDataValue());
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

    public static FluidSlot fluidGauge(CmMachineBlockEntity machine, int x, int y, int width, int height, int tankIndex, boolean showValue) {
        return fluidGaugeBase(machine, x, y, width, height, tankIndex, showValue, IGuiTexture.EMPTY);
    }

    public static FluidSlot fluidGaugeWithBackground(CmMachineBlockEntity machine, int x, int y, int width, int height, int tankIndex, boolean showValue) {
        var background = SpriteTexture.of(HANDLER).setSprite(0, 92, width, height);
        background.transform(-2.0f, -2.0f);
        return fluidGaugeBase(machine, x, y, width, height, tankIndex, showValue, background);
    }

    private static FluidSlot fluidGaugeBase(CmMachineBlockEntity machine, int x, int y, int width, int height, int tankIndex, boolean showValue, IGuiTexture background) {
        var slot = absolute(new FluidSlot(), x, y, width, height);
        slot.style(style -> style.backgroundTexture(background));
        slot.slotStyle(style -> style
                .slotOverlay(IGuiTexture.EMPTY)
                .showSlotOverlayOnlyEmpty(false)
                .fillDirection(FillDirection.DOWN_TO_UP)
                .showFluidTooltips(false));
        slot.amountLabel.setDisplay(false);
        slot.bind(machine.getFluidHandler(null), tankIndex);
        slot.addEventListener(UIEvents.HOVER_TOOLTIPS, event -> event.hoverTooltips = HoverTooltips.create(fluidTooltip(machine, tankIndex, showValue).toArray()));
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

    private static IGuiTexture sprite(Identifier resourceLocation, int x, int y, int width, int height) {
        return SpriteTexture.of(resourceLocation).setSprite(x, y, width, height);
    }

    private static IGuiTexture fullTexture(Identifier resourceLocation, int width, int height) {
        return SpriteTexture.of(resourceLocation).setSprite(0, 0, width, height);
    }

    public static Identifier backgroundFor(int machineType) {
        return switch (machineType) {
            case MachineType.FURNACE -> TEN.id("textures/gui/one_to_one.png");
            case MachineType.PULVERIZER -> TEN.id("textures/gui/pulverizer.png");
            case MachineType.COMPRESSOR -> TEN.id("textures/gui/compressor.png");
            case MachineType.REFINER -> TEN.id("textures/gui/one_to_one_fluid.png");
            case MachineType.INDUCTION_FURNACE -> TEN.id("textures/gui/three_to_one.png");
            case MachineType.PSIONICANT -> TEN.id("textures/gui/two_to_one.png");
            case MachineType.MATTER_CONDENSER -> TEN.id("textures/gui/matter_condenser.png");
            case MachineType.ENCHANTMENT_FLUSHER -> TEN.id("textures/gui/enchantment_flusher.png");
            case MachineType.BEACON -> TEN.id("textures/gui/beacon_simulator.png");
            case MachineType.MOB_RIPPER -> TEN.id("textures/gui/mob_ripper.png");
            case MachineType.QUARRY -> TEN.id("textures/gui/quarry.png");
            case MachineType.FARM -> TEN.id("textures/gui/farm_manager.png");
            case MachineType.CELL, MachineType.CREATIVE_CELL -> TEN.id("textures/gui/energy_cell.png");
            case MachineType.ENGINE_SOLAR -> TEN.id("textures/gui/engine_solar.png");
            case MachineType.ENGINE_EXTRACTION, MachineType.ENGINE_METAL, MachineType.ENGINE_BIOMASS -> TEN.id("textures/gui/engine.png");
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
        machine.rpcSetRedstoneMode(RPCSender.ofServer(), mode);
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

    private static List<Component> upgradeTooltip(CmMachineBlockEntity machine, int slotIndex) {
        return slotIndex >= machine.upgSize ? List.of(ComponentHelper.translated(ComponentHelper.RED, "kenergyengineering.locked_slot")) : List.of();
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
