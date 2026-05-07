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
        xSize = getExtras() + 176;
        ySize = 222;
        getExtras(); // prime sidebar space
    }

    @Override
    public void addWidgets() {
        super.addWidgets();
        int w = 26;
        int xStart = -w - 1;

        widgets.add(barEnergy = new ElementBarEnergy(xStart, 0, w, w, 132, 211, HANDLER));
        widgets.add(barIdeas = new ElementBarIdeas(xStart, (w + 1) * 3, w, w, 152, 40, HANDLER,
                machineKey != null ? machineKey : ""));
        widgets.add(barControl = new ElementBarControl(xStart, (w + 1) * 4, w, w, 152, 40, HANDLER));

        // Redstone buttons
        rsHigh = new ElementButton(xStart, (w + 1) * 2, w, w, 186, 184, HANDLER, this::cycleRedstone).withNoChange();
        rsLow = new ElementButton(xStart, (w + 1) * 2, w, w, 186, 157, HANDLER, this::cycleRedstone).withNoChange();
        rsOff = new ElementButton(xStart, (w + 1) * 2, w, w, 186, 211, HANDLER, this::cycleRedstone).withNoChange();
        rsHigh.setTxt("technicalengineering.info.bar_redstone", "technicalengineering.info.high");
        rsLow.setTxt("technicalengineering.info.bar_redstone", "technicalengineering.info.low");
        rsOff.setTxt("technicalengineering.info.bar_redstone", "technicalengineering.info.off");
        widgets.add(rsHigh);
        widgets.add(rsLow);
        widgets.add(rsOff);

        setSides();
        addUpgradeSlots();
    }

    private void addUpgradeSlots() {
        if (!hasUpgradeSlots()) return;
        int w = 26;
        int xStart = -w - 1;
        int yBase = (w + 1) * 8;
        u0 = new ElementButtonSlot(xStart, yBase, w, w, 24, 105, HANDLER, this::clickUpgradeSlot);
        u0.withNoChange();
        u1 = new ElementButtonSlot(xStart, yBase + (w + 1), w, w, 24, 105, HANDLER, this::clickUpgradeSlot);
        u1.withNoChange();
        u2 = new ElementButtonSlot(xStart, yBase + (w + 1) * 2, w, w, 24, 105, HANDLER, this::clickUpgradeSlot);
        u2.withNoChange();
        u3 = new ElementButtonSlot(xStart, yBase + (w + 1) * 3, w, w, 24, 105, HANDLER, this::clickUpgradeSlot);
        u3.withNoChange();
        u4 = new ElementButtonSlot(xStart, yBase + (w + 1) * 4, w, w, 24, 105, HANDLER, this::clickUpgradeSlot);
        u4.withNoChange();
        u5 = new ElementButtonSlot(xStart, yBase + (w + 1) * 5, w, w, 24, 105, HANDLER, this::clickUpgradeSlot);
        u5.withNoChange();
        widgets.add(u0);
        widgets.add(u1);
        widgets.add(u2);
        widgets.add(u3);
        widgets.add(u4);
        widgets.add(u5);
    }

    private void clickUpgradeSlot() {}

    private void setSides() {
        int w = 26;
        int xStart = -w - 1;
        int yBase = (w + 1) * 6;
        front = makeTransf(xStart, yBase, w, 152, 0, this::cycleEnergy);
        back = makeTransf(xStart, yBase + (w + 1), w, 152, 0, this::cycleEnergy);
        left = makeTransf(xStart, yBase + (w + 1) * 2, w, 152, 0, this::cycleEnergy);
        right = makeTransf(xStart, yBase + (w + 1) * 3, w, 152, 0, this::cycleEnergy);
        up = makeTransf(xStart, yBase + (w + 1) * 4, w, 152, 0, this::cycleEnergy);
        down = makeTransf(xStart, yBase + (w + 1) * 5, w, 152, 0, this::cycleEnergy);
        front.setTxt("technicalengineering.info.front");
        back.setTxt("technicalengineering.info.back");
        left.setTxt("technicalengineering.info.left");
        right.setTxt("technicalengineering.info.right");
        up.setTxt("technicalengineering.info.up");
        down.setTxt("technicalengineering.info.down");
        widgets.add(front);
        widgets.add(back);
        widgets.add(left);
        widgets.add(right);
        widgets.add(up);
        widgets.add(down);

        // Mode switch buttons
        int yMode = yBase + (w + 1) * 6;
        eSwitch = new ElementButton(xStart, yMode, w, w, 24, 79, HANDLER, this::toNextMode).withNoChange();
        iSwitch = new ElementButton(xStart, yMode, w, w, 24, 53, HANDLER, this::toNextMode).withNoChange();
        fSwitch = new ElementButton(xStart, yMode, w, w, 24, 105, HANDLER, this::toNextMode).withNoChange();
        eSwitch.setTxt("technicalengineering.info.bar_mode", "technicalengineering.info.energy");
        iSwitch.setTxt("technicalengineering.info.bar_mode", "technicalengineering.info.item");
        fSwitch.setTxt("technicalengineering.info.bar_mode", "technicalengineering.info.fluid");
        widgets.add(eSwitch);
        widgets.add(iSwitch);
        widgets.add(fSwitch);
    }

    private ElementButtonTransf makeTransf(int x, int y, int w, int xOff, int yOff, ClickAction act) {
        var b = new ElementButtonTransf(x, y, w, w, xOff, yOff, HANDLER, act);
        b.withNoChange();
        return b;
    }

    private void cycleEnergy() {
        if (container.pos == null || clickedDirection == null) return;
        PacketDistributor.sendToServer(new TransferModePacket(container.pos, modeNow, clickedDirection));
    }

    private void toNextMode() {
        modeNow++;
        if (modeNow > 2) modeNow = 0;
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

        // Direction buttons visibility
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

        // Upgrade slots
        boolean hasUp = container.machine != null && container.machine.hasUpgrade();
        if (u0 != null) u0.setVisible(hasUp && !showAll);
        if (u1 != null) u1.setVisible(hasUp && !showAll);
        if (u2 != null) u2.setVisible(hasUp && !showAll);
        if (u3 != null) u3.setVisible(hasUp && !showAll);
        if (u4 != null) u4.setVisible(hasUp && !showAll);
        if (u5 != null) u5.setVisible(hasUp && !showAll);

        // Mode switch
        eSwitch.setVisible(showAll && modeNow != 0);
        iSwitch.setVisible(showAll && modeNow != 1);
        fSwitch.setVisible(showAll && modeNow != 2);

        // Update direction modes from client data
        if (container.pos != null) {
            var em = ClientData.energy.get(container.pos);
            var im = ClientData.item.get(container.pos);
            var fm = ClientData.fluid.get(container.pos);
            var faces = getCurrentFaceModes(modeNow, em, im, fm);
            if (faces != null) {
                front.mode = faces[0];
                back.mode = faces[1];
                left.mode = faces[2];
                right.mode = faces[3];
                up.mode = faces[4];
                down.mode = faces[5];
            }
        }
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
