package com.modularmc.ten.utils;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TagHelper {

    public static Collection<Item> getItems(TagKey<Item> tag) {
        List<Item> list = new ArrayList<>();
        for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
            list.add(holder.value());
        }
        return list;
    }

    public static Collection<Fluid> getFluids(TagKey<Fluid> tag) {
        List<Fluid> list = new ArrayList<>();
        for (Holder<Fluid> holder : BuiltInRegistries.FLUID.getTagOrEmpty(tag)) {
            list.add(holder.value());
        }
        return list;
    }

    public static TagKey<Item> keyItem(String s) {
        return TagKey.create(Registries.ITEM, Identifier.parse(s));
    }

    public static TagKey<Fluid> keyFluid(String s) {
        return TagKey.create(Registries.FLUID, Identifier.parse(s));
    }

    public static TagKey<Block> keyBlock(String s) {
        return TagKey.create(Registries.BLOCK, Identifier.parse(s));
    }

    public static boolean containsItem(Item t, TagKey<Item> s) {
        return t.getDefaultInstance().is(s);
    }

    public static boolean containsFluid(Fluid t, TagKey<Fluid> s) {
        return t.defaultFluidState().is(s);
    }

    public static boolean containsBlock(Block t, TagKey<Block> s) {
        return t.defaultBlockState().is(s);
    }
}
