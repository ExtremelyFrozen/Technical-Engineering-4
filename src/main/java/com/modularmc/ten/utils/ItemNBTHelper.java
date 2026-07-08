package com.modularmc.ten.utils;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

import java.util.function.Consumer;

public class ItemNBTHelper {

    public static ItemStack[] merge(ItemStack i1, ItemStack i2) {
        SimpleContainer inv = new SimpleContainer(2);
        InvWrapper wrapper = new InvWrapper(inv);
        inv.setItem(0, i1.copy());
        ItemStack sr = wrapper.insertItem(0, i2.copy(), false);
        if (sr.isEmpty()) {
            return new ItemStack[] { inv.getItem(0) };
        }
        return new ItemStack[] { inv.getItem(0), sr };
    }

    public static void damage(ItemStack stack, Level world, int am) {
        if (world instanceof ServerLevel serverLevel) {
            Consumer<Item> breakHandler = (s) -> {};
            stack.hurtAndBreak(am, serverLevel, (ServerPlayer) null, breakHandler);
        }
    }

    public static void setTag(ItemStack stack, String name, int cr) {
        setTagD(stack, name, cr);
    }

    public static void tranTag(ItemStack stack, String name, int move) {
        setTagD(stack, name, getTag(stack, name) + move);
    }

    public static int getTag(ItemStack stack, String name) {
        return (int) getTagD(stack, name);
    }

    public static double getTagD(ItemStack stack, String name) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (data.isEmpty()) return 0;
        CompoundTag tag = data.copyTag();
        if (!tag.contains(name)) return 0;
        return tag.getDouble(name).orElse(0.0);
    }

    public static void setTagD(ItemStack stack, String name, double cr) {
        CompoundTag nbt = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        nbt.putDouble(name, cr);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
    }

    public static void tranTagD(ItemStack stack, String name, double move) {
        setTagD(stack, name, getTagD(stack, name) + move);
    }
}
