package com.modularmc.ten.common.data;

import com.modularmc.ten.TEN;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class TENTags {

    public static final TagKey<Block> MACHINES = blockTag("machines");

    private static TagKey<Block> blockTag(String path) {
        return BlockTags.create(ResourceLocation.fromNamespaceAndPath(TEN.MOD_ID, path));
    }

    private static TagKey<Item> itemTag(String path) {
        return ItemTags.create(ResourceLocation.fromNamespaceAndPath(TEN.MOD_ID, path));
    }

    public static void init() {}
}
