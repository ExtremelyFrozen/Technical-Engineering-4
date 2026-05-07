package com.modularmc.ten.common.blockentity;

import com.modularmc.ten.utils.TagHelper;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;

public class MatchFuel {

    public static int matchFuel(ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return 0;
        int time = stack.getBurnTime(RecipeType.SMELTING);
        if (stack.is(Items.LAVA_BUCKET)) return 0;
        if (time > 0 && !simulate) stack.shrink(1);
        return time * 20;
    }

    public static int matchPlant(ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return 0;
        int time = 0;
        if (TagHelper.containsItem(stack.getItem(), ItemTags.LEAVES)) time = 20;
        else if (TagHelper.containsItem(stack.getItem(), ItemTags.LOGS)) time = 120;
        else if (TagHelper.containsItem(stack.getItem(), ItemTags.FLOWERS)) time = 10;
        else if (TagHelper.containsItem(stack.getItem(), ItemTags.SAPLINGS)) time = 15;
        else if (TagHelper.containsItem(stack.getItem(), TagHelper.keyItem("minecraft:saplings"))) time = 15;
        if (time > 0 && !simulate) stack.shrink(1);
        return time * 60;
    }

    public static int matchMetal(ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return 0;
        int time = 0;
        boolean cn = TagHelper.containsItem(stack.getItem(), TagHelper.keyItem("technicalengineering:common_ingots"));
        boolean uc = TagHelper.containsItem(stack.getItem(), TagHelper.keyItem("technicalengineering:uncommon_ingots"));
        boolean vc = TagHelper.containsItem(stack.getItem(), TagHelper.keyItem("technicalengineering:valuable_ingots"));
        if (cn) time = 1000;
        else if (uc) time = 1500;
        else if (vc) time = 6400;
        if (time > 0 && !simulate) stack.shrink(1);
        return time * 20;
    }
}
