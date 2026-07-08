package com.modularmc.ten.common.item;

import com.modularmc.ten.TEN;
import com.modularmc.ten.component.EnergyUnitData;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = TEN.MOD_ID)
public class EnergyUnitHandler {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        ItemStack unitStack = findEnergyUnit(player);
        if (unitStack == null || unitStack.isEmpty()) return;

        EnergyUnitData data = EnergyUnitData.of(unitStack);
        if (!data.isCharging()) return;
        if (data.getEnergy() <= 0) return;

        List<ItemStack> targets = collectChargeableItems(player);
        if (targets.isEmpty()) return;

        int chargeRate = EnergyUnitItem.chargeRate();
        int perTarget = Math.min(chargeRate / Math.max(targets.size(), 1), data.getEnergy());
        boolean charged = false;

        for (ItemStack target : targets) {
            if (data.getEnergy() <= 0) break;
            IEnergyStorage storage = target.getCapability(Capabilities.EnergyStorage.ITEM);
            if (storage == null) continue;
            int received = storage.receiveEnergy(Math.min(perTarget, data.getEnergy()), false);
            if (received > 0) {
                data.setEnergy(data.getEnergy() - received);
                charged = true;
            }
        }

        if (charged) {
            data.save(unitStack);
        }
    }

    private static ItemStack findEnergyUnit(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof EnergyUnitItem) {
                return stack;
            }
        }
        return null;
    }

    private static List<ItemStack> collectChargeableItems(Player player) {
        List<ItemStack> targets = new ArrayList<>();

        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && canCharge(stack)) {
                targets.add(stack);
            }
        }

        ItemStack offhand = player.getOffhandItem();
        if (!offhand.isEmpty() && canCharge(offhand)) {
            targets.add(offhand);
        }

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                ItemStack armor = player.getItemBySlot(slot);
                if (!armor.isEmpty() && canCharge(armor)) {
                    targets.add(armor);
                }
            }
        }

        collectCurios(player, targets);
        return targets;
    }

    private static boolean canCharge(ItemStack stack) {
        if (stack.getItem() instanceof EnergyUnitItem) return false;
        IEnergyStorage storage = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (storage == null) return false;
        return storage.canReceive() && storage.getEnergyStored() < storage.getMaxEnergyStored();
    }

    private static void collectCurios(Player player, List<ItemStack> targets) {
        if (!net.neoforged.fml.ModList.get().isLoaded("curios")) return;
        com.modularmc.ten.common.item.CuriosIntegration.collectCurios(player, targets);
    }
}
