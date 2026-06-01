package com.modularmc.ten.common.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.List;

/**
 * Only loaded at runtime when Curios API is present.
 * No direct reference to this class from {@link EnergyUnitHandler} during normal operation.
 */
public class CuriosIntegration {

    public static void collectCurios(Player player, List<ItemStack> targets) {
        top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            for (var slotHandler : handler.getCurios().values()) {
                for (int i = 0; i < slotHandler.getSlots(); i++) {
                    ItemStack curio = slotHandler.getStacks().getStackInSlot(i);
                    if (!curio.isEmpty() && canCharge(curio)) {
                        targets.add(curio);
                    }
                }
            }
        });
    }

    private static boolean canCharge(ItemStack stack) {
        if (stack.getItem() instanceof EnergyUnitItem) return false;
        IEnergyStorage storage = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (storage == null) return false;
        return storage.canReceive() && storage.getEnergyStored() < storage.getMaxEnergyStored();
    }
}
