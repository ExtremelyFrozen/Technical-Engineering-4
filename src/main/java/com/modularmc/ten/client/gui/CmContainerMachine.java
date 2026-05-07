package com.modularmc.ten.client.gui;

import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;
import com.modularmc.ten.api.option.MachineType;
import com.modularmc.ten.api.wrapper.SyncedIntArray;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

import org.jetbrains.annotations.Nullable;

public class CmContainerMachine extends AbstractContainerMenu {

    public final SyncedIntArray data;
    public final BlockPos pos;
    public final CmMachineBlockEntity machine;

    public CmContainerMachine(@Nullable MenuType<?> type, int id, Inventory playerInv, CmMachineBlockEntity machine, BlockPos pos) {
        super(type, id);
        this.machine = machine;
        this.pos = pos;
        this.data = machine.data;

        // Machine slots
        addMachineSlots();

        // Player inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
        }

        // Data sync
        if (data != null) {
            for (int i = 0; i < data.size(); i++) {
                final int idx = i;
                addDataSlot(new DataSlot() {

                    @Override
                    public int get() {
                        return data.get(idx);
                    }

                    @Override
                    public void set(int value) {
                        data.set(idx, value);
                    }
                });
            }
        }
    }

    private void addMachineSlots() {
        if (machine == null || machine.itemHandler == null) return;
        int totalSlots = machine.itemHandler.getSlots();
        if (totalSlots == 0) return;

        int type = machine.machineType();
        switch (type) {
            case MachineType.FURNACE -> {
                addMachineSlot(0, 56, 35);
                addMachineSlot(1, 116, 35);
            }
            case MachineType.PULVERIZER -> {
                addMachineSlot(0, 56, 35);
                addMachineSlot(1, 97, 17);
                addMachineSlot(2, 115, 17);
                addMachineSlot(3, 97, 53);
                addMachineSlot(4, 115, 53);
            }
            case MachineType.COMPRESSOR -> {
                addMachineSlot(0, 44, 35);
                addMachineSlot(1, 62, 35);
                addMachineSlot(2, 116, 35);
            }
            case MachineType.REFINER -> {
                addMachineSlot(0, 44, 35);
                addMachineSlot(1, 62, 35);
                addMachineSlot(2, 116, 35);
                addMachineSlot(3, 134, 35);
            }
            case MachineType.INDUCTION_FURNACE -> {
                addMachineSlot(0, 44, 35);
                addMachineSlot(1, 62, 35);
                addMachineSlot(2, 80, 35);
                addMachineSlot(3, 134, 35);
            }
            case MachineType.PSIONICANT -> {
                addMachineSlot(0, 44, 35);
                addMachineSlot(1, 62, 35);
                addMachineSlot(2, 116, 35);
            }
            case MachineType.MATTER_CONDENSER -> {
                addMachineSlot(0, 56, 35);
                addMachineSlot(1, 116, 35);
            }
            default -> {
                // Generic layout: first half=input, second half=output
                int half = totalSlots / 2;
                for (int i = 0; i < totalSlots; i++) {
                    int x = i < half ? 44 + (i % 3) * 18 : 116 + ((i - half) % 3) * 18;
                    int y = i < half ? 35 + (i / 3) * 18 : 35 + ((i - half) / 3) * 18;
                    addMachineSlot(i, x, y);
                }
            }
        }
    }

    private void addMachineSlot(int index, int x, int y) {
        if (index < machine.itemHandler.getSlots()) {
            addSlot(new SlotItemHandler(machine.itemHandler, index, x, y));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return pos.closerThan(player.getOnPos(), 12);
    }
}
