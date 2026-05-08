package com.modularmc.ten.client.gui;

import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;
import com.modularmc.ten.api.option.RedstoneMode;
import com.modularmc.ten.client.ClientData;
import com.modularmc.ten.client.gui.element.*;
import com.modularmc.ten.common.network.packet.RedstoneModePacket;
import com.modularmc.ten.common.network.packet.TransferModePacket;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

import static com.modularmc.ten.client.gui.element.ElementButton.ClickAction;

public class CmScreenMachine extends CmScreen<CmContainerMachine> {

    protected ElementBarEnergy barEnergy;
    protected ElementBarControl barControl;
    protected ElementBarIdeas barIdeas;
    protected ElementButton rsHigh, rsLow, rsOff;
    protected ElementButtonTransf front, back, left, right, up, down;
    protected ElementButton eSwitch, iSwitch, fSwitch;
    protected ElementButton u0, u1, u2, u3, u4, u5;
    protected int modeNow;
    protected String machineKey;
    private Direction clickedDirection;

    private boolean hasUpgradeSlots() {
        if (container.machine != null) {
            return container.machine.hasUpgrade();
        }
        return container.machineType != com.modularmc.ten.api.option.MachineType.CELL;
    }

    public CmScreenMachine(CmContainerMachine container, Inventory inv, Component title, String path, int texW, int texH) {
        super(container, inv, title, path, texW, texH);
        xSize = 176;
        ySize = 166;
    }

    @Override
    public void addWidgets() {
        super.addWidgets();
        int w = 26;
        int barx = -w - 1;

        if (hasUpgradeSlots()) {
            widgets.add(new ElementImage(23, -37, 131, 36, 0, 211, HANDLER));
        }

        widgets.add(barIdeas = new ElementBarIdeas(barx, 0, w, w, 159, 211, HANDLER,
                machineKey != null ? machineKey : ""));
        widgets.add(barEnergy = new ElementBarEnergy(barx, w + 1, w, w, 132, 211, HANDLER));
        widgets.add(barControl = new ElementBarControl(barx, (w + 1) * 3, w, w, 152, 40, HANDLER));

        rsHigh = new ElementButton(barx, (w + 1) * 2, w, w, 186, 211, HANDLER, this::cycleRedstone).withNoChange();
        rsLow = new ElementButton(barx, (w + 1) * 2, w, w, 186, 184, HANDLER, this::cycleRedstone).withNoChange();
        rsOff = new ElementButton(barx, (w + 1) * 2, w, w, 186, 157, HANDLER, this::cycleRedstone).withNoChange();
        rsHigh.setTxt("kenergyengineering.info.bar_redstone", "kenergyengineering.info.high");
        rsLow.setTxt("kenergyengineering.info.bar_redstone", "kenergyengineering.info.low");
        rsOff.setTxt("kenergyengineering.info.bar_redstone", "kenergyengineering.info.off");
        widgets.add(rsHigh);
        widgets.add(rsLow);
        widgets.add(rsOff);

        addTransferWidgets();
        addUpgradeSlots();
    }

    private void addUpgradeSlots() {
        if (!hasUpgradeSlots()) return;
        u0 = new ElementButtonSlot(32, -28, 18, 18, 227, 0, HANDLER, this::clickUpgradeSlot);
        u0.withNoChange();
        u1 = new ElementButtonSlot(51, -28, 18, 18, 227, 0, HANDLER, this::clickUpgradeSlot);
        u1.withNoChange();
        u2 = new ElementButtonSlot(70, -28, 18, 18, 227, 0, HANDLER, this::clickUpgradeSlot);
        u2.withNoChange();
        u3 = new ElementButtonSlot(89, -28, 18, 18, 227, 0, HANDLER, this::clickUpgradeSlot);
        u3.withNoChange();
        u4 = new ElementButtonSlot(108, -28, 18, 18, 227, 0, HANDLER, this::clickUpgradeSlot);
        u4.withNoChange();
        u5 = new ElementButtonSlot(127, -28, 18, 18, 227, 0, HANDLER, this::clickUpgradeSlot);
        u5.withNoChange();
        widgets.add(u0);
        widgets.add(u1);
        widgets.add(u2);
        widgets.add(u3);
        widgets.add(u4);
        widgets.add(u5);
    }

    private void clickUpgradeSlot() {}

    private void addTransferWidgets() {
        int w = 26;
        int panelWidth = 60;
        int baseX = -panelWidth - 1;
        int baseY = (w + 1) * 3;

        eSwitch = new ElementButton(baseX + 7, baseY + 64, 14, 14, 91, 126, HANDLER, () -> modeNow = 0);
        iSwitch = new ElementButton(baseX + 23, baseY + 64, 14, 14, 106, 126, HANDLER, () -> modeNow = 1);
        fSwitch = new ElementButton(baseX + 39, baseY + 64, 14, 14, 76, 126, HANDLER, () -> modeNow = 2);
        eSwitch.setTxt("kenergyengineering.info.bar_mode", "kenergyengineering.info.energy");
        iSwitch.setTxt("kenergyengineering.info.bar_mode", "kenergyengineering.info.item");
        fSwitch.setTxt("kenergyengineering.info.bar_mode", "kenergyengineering.info.fluid");
        widgets.add(eSwitch);
        widgets.add(iSwitch);
        widgets.add(fSwitch);

        front = makeTransf(baseX + 22, baseY + 22, 12, 121, 126, this::cycleEnergy);
        back = makeTransf(baseX + 36, baseY + 36, 12, 121, 126, this::cycleEnergy);
        left = makeTransf(baseX + 8, baseY + 22, 12, 121, 126, this::cycleEnergy);
        right = makeTransf(baseX + 36, baseY + 22, 12, 121, 126, this::cycleEnergy);
        up = makeTransf(baseX + 22, baseY + 8, 12, 121, 126, this::cycleEnergy);
        down = makeTransf(baseX + 22, baseY + 36, 12, 121, 126, this::cycleEnergy);
        front.setTxt("kenergyengineering.info.front");
        back.setTxt("kenergyengineering.info.back");
        left.setTxt("kenergyengineering.info.left");
        right.setTxt("kenergyengineering.info.right");
        up.setTxt("kenergyengineering.info.up");
        down.setTxt("kenergyengineering.info.down");
        widgets.add(front);
        widgets.add(back);
        widgets.add(left);
        widgets.add(right);
        widgets.add(up);
        widgets.add(down);
    }

    protected void setSides() {
        if (container.pos == null || container.data == null) {
            return;
        }
        var em = ClientData.energy.get(container.pos);
        var im = ClientData.item.get(container.pos);
        var fm = ClientData.fluid.get(container.pos);
        var faces = getCurrentFaceModes(modeNow, em, im, fm);
        if (faces == null) {
            return;
        }
        front.mode = faces[0];
        back.mode = faces[1];
        left.mode = faces[2];
        right.mode = faces[3];
        up.mode = faces[4];
        down.mode = faces[5];
    }

    private ElementButtonTransf makeTransf(int x, int y, int size, int xOff, int yOff, ClickAction act) {
        var b = new ElementButtonTransf(x, y, size, size, xOff, yOff, HANDLER, act);
        b.withNoChange();
        return b;
    }

    private void cycleEnergy() {
        if (container.pos == null || clickedDirection == null) return;
        PacketDistributor.sendToServer(new TransferModePacket(container.pos, modeNow, clickedDirection));
    }

    private void cycleRedstone() {
        if (container.pos == null) return;
        int m = container.data.get(CmMachineBlockEntity.RED_MODE);
        m++;
        if (m >= RedstoneMode.size()) m = RedstoneMode.OFF;
        ClientData.redstone.put(container.pos, m);
        PacketDistributor.sendToServer(new RedstoneModePacket(m, container.pos));
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (container.data == null) return;
        var data = container.data;

        // Redstone
        rsHigh.setVisible(false);
        rsLow.setVisible(false);
        rsOff.setVisible(false);
        int redstoneMode = container.pos != null ? ClientData.redstone.getOrDefault(container.pos, data.get(CmMachineBlockEntity.RED_MODE)) : data.get(CmMachineBlockEntity.RED_MODE);
        switch (redstoneMode) {
            case RedstoneMode.HIGH -> rsHigh.setVisible(true);
            case RedstoneMode.LOW -> rsLow.setVisible(true);
            default -> rsOff.setVisible(true);
        }

        // Energy bar
        barEnergy.update(
                data.get(CmMachineBlockEntity.EFF_AUC),
                data.get(CmMachineBlockEntity.EFF),
                data.get(CmMachineBlockEntity.E_REC),
                data.get(CmMachineBlockEntity.E_EXT),
                data.get(CmMachineBlockEntity.I_REC),
                data.get(CmMachineBlockEntity.I_EXT),
                data.get(CmMachineBlockEntity.F_REC),
                data.get(CmMachineBlockEntity.F_EXT));

        boolean showAll = barControl.show;
        front.setVisible(showAll);
        back.setVisible(showAll);
        left.setVisible(showAll);
        right.setVisible(showAll);
        up.setVisible(showAll);
        down.setVisible(showAll);
        eSwitch.setVisible(showAll);
        iSwitch.setVisible(showAll);
        fSwitch.setVisible(showAll);

        if (u0 != null) u0.setVisible(hasUpgradeSlots());
        if (u1 != null) u1.setVisible(hasUpgradeSlots());
        if (u2 != null) u2.setVisible(hasUpgradeSlots());
        if (u3 != null) u3.setVisible(hasUpgradeSlots());
        if (u4 != null) u4.setVisible(hasUpgradeSlots());
        if (u5 != null) u5.setVisible(hasUpgradeSlots());

        if (u0 != null) u0.state = data.get(CmMachineBlockEntity.UPGSIZE) >= 1;
        if (u1 != null) u1.state = data.get(CmMachineBlockEntity.UPGSIZE) >= 2;
        if (u2 != null) u2.state = data.get(CmMachineBlockEntity.UPGSIZE) >= 3;
        if (u3 != null) u3.state = data.get(CmMachineBlockEntity.UPGSIZE) >= 4;
        if (u4 != null) u4.state = data.get(CmMachineBlockEntity.UPGSIZE) >= 5;
        if (u5 != null) u5.state = data.get(CmMachineBlockEntity.UPGSIZE) >= 6;

        eSwitch.state = modeNow == 0;
        iSwitch.state = modeNow == 1;
        fSwitch.state = modeNow == 2;

        setSides();
    }

    private Direction getDirectionForButton(ElementButtonTransf button) {
        Direction facing = Direction.from3DDataValue(container.data.get(CmMachineBlockEntity.FACE));
        if (button == front) return facing;
        if (button == back) return facing.getOpposite();
        if (button == left) return facing.getClockWise();
        if (button == right) return facing.getCounterClockWise();
        if (button == up) return net.minecraft.core.Direction.UP;
        if (button == down) return net.minecraft.core.Direction.DOWN;
        return null;
    }

    private int[] getCurrentFaceModes(int mode, List<Integer> em, List<Integer> im, List<Integer> fm) {
        List<Integer> list = null;
        if (mode == 0 && em != null) list = em;
        if (mode == 1 && im != null) list = im;
        if (mode == 2 && fm != null) list = fm;
        if (list == null || list.size() < 6) return null;
        Direction facing = Direction.from3DDataValue(container.data.get(CmMachineBlockEntity.FACE));
        return new int[] {
                list.get(facing.get3DDataValue()),
                list.get(facing.getOpposite().get3DDataValue()),
                list.get(facing.getClockWise().get3DDataValue()),
                list.get(facing.getCounterClockWise().get3DDataValue()),
                list.get(Direction.UP.get3DDataValue()),
                list.get(Direction.DOWN.get3DDataValue())
        };
    }

    public ElementBurnLeft getDefaultEne() {
        return new ElementBurnLeft(9, 18, 14, 46, 0, 0, HANDLER, true);
    }

    /**
     * Extra width taken by the sidebar
     */
    public int getExtras() {
        return 27 + 2;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        ElementBase element = getElementFromLocation((int) mouseX, (int) mouseY);
        clickedDirection = element instanceof ElementButtonTransf transf ? getDirectionForButton(transf) : null;
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
