package com.modularmc.ten.client.gui;

import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;
import com.modularmc.ten.api.option.IngredientType;
import com.modularmc.ten.api.option.MachineType;
import com.modularmc.ten.common.item.upgrades.UpgradeItem;

import net.minecraft.core.BlockPos;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

import org.jetbrains.annotations.Nullable;

public class CmContainerMachine extends AbstractContainerMenu {

    private static final int PLAYER_INV_START = 0;
    private static final int PLAYER_INV_END = 36;
    private static final int HOTBAR_START = 0;
    private static final int HOTBAR_END = 9;

    public final BlockPos pos;
    @Nullable
    public final CmMachineBlockEntity machine;
    public final int machineType;
    public final int slotCount;
    public final boolean hasUpgradeSlots;
    private final SimpleContainer clientUpgradeContainer = new SimpleContainer(CmMachineBlockEntity.MAX_UPGRADE_SLOTS);
    private final java.util.List<Slot> upgradeSlots = new java.util.ArrayList<>();

    public CmContainerMachine(@Nullable MenuType<?> type, int id, Inventory playerInv, @Nullable CmMachineBlockEntity machine, @Nullable BlockPos pos) {
        this(type, id, playerInv, machine, pos,
                machine != null ? machine.machineType() : -1,
                machine != null ? machine.itemHandler.getSlots() : 0,
                machine != null && machine.hasUpgrade());
    }

    public CmContainerMachine(@Nullable MenuType<?> type, int id, Inventory playerInv,
                              @Nullable CmMachineBlockEntity machine, @Nullable BlockPos pos,
                              int machineType, int slotCount, boolean hasUpgradeSlots) {
        super(type, id);
        this.machine = machine;
        this.pos = pos;
        this.machineType = machineType;
        this.slotCount = slotCount;
        this.hasUpgradeSlots = hasUpgradeSlots;

        addPlayerInventory(playerInv);
        addMachineSlots(machineType, slotCount);
    }

    private void addPlayerInventory(Inventory playerInv) {
        layoutInventorySlots(playerInv, 141, 0);
        layoutInventorySlots(playerInv, 83, 9);
        layoutInventorySlots(playerInv, 101, 18);
        layoutInventorySlots(playerInv, 119, 27);
    }

    private void layoutInventorySlots(Inventory inventory, int y, int from) {
        for (int slot = 0; slot < 9; slot++) {
            addSlot(new Slot(inventory, slot + from, 8 + slot * 18, y + 1));
        }
    }

    private void addMachineSlots(int mType, int totalSlots) {
        if (totalSlots <= 0) return;

        switch (mType) {
            case MachineType.FURNACE, MachineType.MATTER_CONDENSER -> {
                if (mType == MachineType.FURNACE) {
                    addMachineSlot(0, 43, 20);
                    addMachineSlot(1, 115, 34);
                } else {
                    addMachineSlot(0, 79, 32);
                }
            }
            case MachineType.PULVERIZER -> {
                addMachineSlot(0, 43, 20);
                addMachineSlot(1, 112, 25);
                addMachineSlot(2, 130, 25);
                addMachineSlot(3, 112, 43);
                addMachineSlot(4, 130, 43);
            }
            case MachineType.COMPRESSOR, MachineType.PSIONICANT -> {
                if (mType == MachineType.COMPRESSOR) {
                    addMachineSlot(0, 43, 15);
                    addMachineSlot(1, 43, 51);
                } else {
                    addMachineSlot(0, 34, 20);
                    addMachineSlot(1, 52, 20);
                }
                addMachineSlot(2, 115, 34);
            }
            case MachineType.REFINER -> {
                addMachineSlot(0, 58, 34);
                addMachineSlot(1, 117, 34);
            }
            case MachineType.INDUCTION_FURNACE -> {
                addMachineSlot(0, 33, 20);
                addMachineSlot(1, 51, 20);
                addMachineSlot(2, 69, 20);
                addMachineSlot(3, 127, 34);
            }
            case MachineType.BEACON, MachineType.MOB_RIPPER -> addMachineSlot(0, 79, 31);
            case MachineType.ENCHANTMENT_FLUSHER -> {
                addMachineSlot(0, 43, 15);
                addMachineSlot(1, 43, 51);
                addMachineSlot(2, 115, 34);
            }
            case MachineType.QUARRY -> {
                addMachineSlot(0, 43, 34);
                addMachineSlot(1, 79, 16);
                addMachineSlot(2, 97, 16);
                addMachineSlot(3, 115, 16);
                addMachineSlot(4, 133, 16);
                addMachineSlot(5, 79, 34);
                addMachineSlot(6, 97, 34);
                addMachineSlot(7, 115, 34);
                addMachineSlot(8, 133, 34);
                addMachineSlot(9, 79, 52);
                addMachineSlot(10, 97, 52);
                addMachineSlot(11, 115, 52);
                addMachineSlot(12, 133, 52);
            }
            case MachineType.FARM -> {
                addMachineSlot(0, 43, 16);
                addMachineSlot(1, 61, 16);
                addMachineSlot(2, 43, 34);
                addMachineSlot(3, 61, 34);
                addMachineSlot(4, 43, 52);
                addMachineSlot(5, 61, 52);
                addMachineSlot(6, 97, 16);
                addMachineSlot(7, 115, 16);
                addMachineSlot(8, 97, 34);
                addMachineSlot(9, 115, 34);
                addMachineSlot(10, 97, 52);
                addMachineSlot(11, 115, 52);
            }
            case MachineType.CELL -> {
                addMachineSlot(0, 42, 32);
                addMachineSlot(1, 115, 32);
            }
            case MachineType.ENGINE_EXTRACTION, MachineType.ENGINE_METAL, MachineType.ENGINE_BIOMASS -> addMachineSlot(0, 43, 36);
            default -> {
                int half = totalSlots / 2;
                for (int i = 0; i < totalSlots; i++) {
                    int x = i < half ? 44 + (i % 3) * 18 : 116 + ((i - half) % 3) * 18;
                    int y = i < half ? 35 + (i / 3) * 18 : 35 + ((i - half) / 3) * 18;
                    addMachineSlot(i, x, y);
                }
            }
        }
        addUpgradeSlots();
    }

    private void addMachineSlot(int index, int x, int y) {
        if (machine != null && index < machine.itemHandler.getSlots()) {
            addSlot(new MachineSlot(machine.itemHandler, index, x + 1, y + 1));
        } else {
            addSlot(new Slot(new SimpleContainer(1), 0, x + 1, y + 1));
        }
    }

    private void addUpgradeSlots() {
        if (!hasUpgradeSlots) return;
        addUpgradeSlot(0, 32, -28);
        addUpgradeSlot(1, 51, -28);
        addUpgradeSlot(2, 70, -28);
        addUpgradeSlot(3, 89, -28);
        addUpgradeSlot(4, 108, -28);
        addUpgradeSlot(5, 127, -28);
    }

    private void addUpgradeSlot(int index, int x, int y) {
        Slot slot;
        if (machine != null && machine.upgradeHandler != null) {
            slot = new SlotItemHandler(machine.upgradeHandler, index, x + 1, y + 1);
        } else {
            slot = new Slot(clientUpgradeContainer, index, x + 1, y + 1);
        }
        addSlot(slot);
        upgradeSlots.add(slot);
    }

    private class MachineSlot extends SlotItemHandler {

        public MachineSlot(net.neoforged.neoforge.items.IItemHandler itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (machine == null) return super.mayPlace(stack);
            IngredientType type = machine.slotType(getSlotIndex());
            return type.canIn() && machine.valid(getSlotIndex(), stack) && super.mayPlace(stack);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = slots.get(index);

        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            moved = stack.copy();
            int machineStart = PLAYER_INV_END;
            int machineEnd = machineStart + slotCount;

            if (stack.getItem() instanceof UpgradeItem && hasUpgradeSlots) {
                if (isInBackpack(index)) {
                    for (Slot upgradeSlot : upgradeSlots) {
                        if (!upgradeSlot.hasItem()) {
                            upgradeSlot.set(stack.copyWithCount(1));
                            upgradeSlot.setChanged();
                            stack.shrink(1);
                            if (stack.isEmpty()) {
                                slot.set(ItemStack.EMPTY);
                            } else {
                                slot.setChanged();
                            }
                            break;
                        }
                    }
                } else if (!moveItemStackTo(stack, PLAYER_INV_START, PLAYER_INV_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (isInBackpack(index)) {
                    if (!moveItemStackTo(stack, machineStart, slots.size(), false)) {
                        if (!isInFastBar(index)) {
                            if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) {
                                return ItemStack.EMPTY;
                            }
                        } else if (!moveItemStackTo(stack, HOTBAR_END, PLAYER_INV_END, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                } else if (!moveItemStackTo(stack, PLAYER_INV_START, PLAYER_INV_END, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stack.getCount() == 0) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (stack.getCount() == moved.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        }

        return moved;
    }

    private boolean isInBackpack(int slot) {
        return slot >= PLAYER_INV_START && slot < PLAYER_INV_END;
    }

    private boolean isInFastBar(int slot) {
        return slot >= HOTBAR_START && slot < HOTBAR_END;
    }

    @Override
    public boolean stillValid(Player player) {
        if (player.level().isClientSide()) return true;
        if (machine == null || machine.isRemoved()) return false;
        return !(player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > 64.0D);
    }
}
