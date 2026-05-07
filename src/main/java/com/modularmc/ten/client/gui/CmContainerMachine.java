package com.modularmc.ten.client.gui;

import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;
import com.modularmc.ten.api.option.MachineType;
import com.modularmc.ten.api.wrapper.SyncedIntArray;

import net.minecraft.core.BlockPos;
import net.minecraft.world.SimpleContainer;
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
    @Nullable
    public final CmMachineBlockEntity machine;
    public final int machineType;
    public final int slotCount;

    public CmContainerMachine(@Nullable MenuType<?> type, int id, Inventory playerInv, @Nullable CmMachineBlockEntity machine, @Nullable BlockPos pos) {
        this(type, id, playerInv, machine, pos,
                machine != null ? machine.machineType() : -1,
                machine != null ? machine.itemHandler.getSlots() : 0);
    }

    public CmContainerMachine(@Nullable MenuType<?> type, int id, Inventory playerInv,
                              @Nullable CmMachineBlockEntity machine, @Nullable BlockPos pos,
                              int machineType, int slotCount) {
        super(type, id);
        this.machine = machine;
        this.pos = pos;
        this.machineType = machineType;
        this.slotCount = slotCount;
        this.data = machine != null ? machine.data : new SyncedIntArray(40);

        // Machine slots - must match on both client and server
        addMachineSlots(machineType, slotCount);

        // Player inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        if (machine != null) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
            }
        } else {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
            }
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

    private void addMachineSlots(int mType, int totalSlots) {
        if (totalSlots <= 0) return;

        switch (mType) {
            case MachineType.FURNACE, MachineType.MATTER_CONDENSER -> {
                addDummySlot(0, 56, 35);
                addDummySlot(1, 116, 35);
            }
            case MachineType.PULVERIZER -> {
                addDummySlot(0, 56, 35);
                addDummySlot(1, 97, 17);
                addDummySlot(2, 115, 17);
                addDummySlot(3, 97, 53);
                addDummySlot(4, 115, 53);
            }
            case MachineType.COMPRESSOR, MachineType.PSIONICANT -> {
                addDummySlot(0, 44, 35);
                addDummySlot(1, 62, 35);
                addDummySlot(2, 116, 35);
            }
            case MachineType.REFINER -> {
                addDummySlot(0, 44, 35);
                addDummySlot(1, 62, 35);
                addDummySlot(2, 116, 35);
                addDummySlot(3, 134, 35);
            }
            case MachineType.INDUCTION_FURNACE -> {
                addDummySlot(0, 44, 35);
                addDummySlot(1, 62, 35);
                addDummySlot(2, 80, 35);
                addDummySlot(3, 134, 35);
            }
            default -> {
                int half = totalSlots / 2;
                for (int i = 0; i < totalSlots; i++) {
                    int x = i < half ? 44 + (i % 3) * 18 : 116 + ((i - half) % 3) * 18;
                    int y = i < half ? 35 + (i / 3) * 18 : 35 + ((i - half) / 3) * 18;
                    addDummySlot(i, x, y);
                }
            }
        }
    }

    private void addDummySlot(int index, int x, int y) {
        if (machine != null && index < machine.itemHandler.getSlots()) {
            addSlot(new SlotItemHandler(machine.itemHandler, index, x, y));
        } else {
            addSlot(new Slot(new SimpleContainer(1), 0, x, y));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (pos == null) return true; // Client-side dummy container
        return pos.closerThan(player.getOnPos(), 12);
    }
}
