package com.modularmc.ten.component;

import com.modularmc.ten.common.item.EnergyUnitItem;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public class EnergyUnitData {

    private static final String TAG_ENERGY = "energy";
    private static final String TAG_CHARGING = "charging";

    private int energy;
    private boolean charging;

    public EnergyUnitData(int energy, boolean charging) {
        this.energy = energy;
        this.charging = charging;
    }

    public int getEnergy() {
        return energy;
    }

    public void setEnergy(int energy) {
        this.energy = Math.max(0, Math.min(energy, EnergyUnitItem.maxEnergy()));
    }

    public boolean isCharging() {
        return charging;
    }

    public void setCharging(boolean charging) {
        this.charging = charging;
    }

    public void save(ItemStack stack) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(TAG_ENERGY, energy);
        tag.putBoolean(TAG_CHARGING, charging);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static EnergyUnitData of(ItemStack stack) {
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null || custom.isEmpty()) {
            return new EnergyUnitData(0, EnergyUnitItem.chargingDefault());
        }
        CompoundTag tag = custom.copyTag();
        return new EnergyUnitData(
                tag.getInt(TAG_ENERGY).orElse(0),
                tag.getBoolean(TAG_CHARGING).orElse(EnergyUnitItem.chargingDefault()));
    }
}
