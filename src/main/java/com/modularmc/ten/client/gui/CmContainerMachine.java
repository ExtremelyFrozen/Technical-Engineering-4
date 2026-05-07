package com.modularmc.ten.client.gui;

import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;
import com.modularmc.ten.api.wrapper.SyncedIntArray;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

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

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return pos.closerThan(player.getOnPos(), 12);
    }
}
