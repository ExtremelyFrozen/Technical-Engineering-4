package com.modularmc.ten.common.blockentity;

import com.modularmc.ten.utils.TagHelper;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import org.jspecify.annotations.Nullable;

public class MatchFuel {

    /**
     * Matches an item stack as furnace-equivalent fuel for the extraction engine.
     *
     * @param level    the level (nullable — if null, falls back to a hardcoded map).
     * @param stack    the fuel item stack.
     * @param simulate if true, the stack is not consumed.
     * @return burn time in game ticks (multiplied by 20 from vanilla furnace ticks).
     */
    public static int matchFuel(@Nullable Level level, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return 0;

        // Priority: use 26.1.2 FuelValues API when level is available.
        if (level != null) {
            int time = stack.getBurnTime(RecipeType.SMELTING, level.fuelValues());
            if (time > 0) {
                if (!simulate) stack.shrink(1);
                return time * 20;
            }
        }

        // Fallback: hardcoded common fuel values when level is not available.
        // TODO(26.x): Remove this fallback once all callers provide a Level reference.
        int time = hardcodedBurnTime(stack);
        if (time > 0 && !simulate) stack.shrink(1);
        return time * 20;
    }

    /**
     * @deprecated Use {@link #matchFuel(Level, ItemStack, boolean)} instead.
     *             This overload is kept for binary compatibility and falls back to
     *             hardcoded values (no level-aware FuelValues lookup).
     */
    @Deprecated
    public static int matchFuel(ItemStack stack, boolean simulate) {
        return matchFuel(null, stack, simulate);
    }

    private static int hardcodedBurnTime(ItemStack stack) {
        // Vanilla furnace fuel values (in furnace ticks, 200 = 1 item).
        if (stack.is(Items.LAVA_BUCKET)) return 20000;
        if (stack.is(Items.COAL_BLOCK)) return 16000;
        if (stack.is(Items.DRIED_KELP_BLOCK)) return 4000;
        if (stack.is(Items.BLAZE_ROD)) return 2400;
        if (stack.is(Items.COAL)) return 1600;
        if (stack.is(Items.CHARCOAL)) return 1600;
        if (stack.is(Items.BAMBOO_BLOCK)) return 300;
        if (stack.is(Items.BAMBOO_MOSAIC)) return 300;
        if (stack.is(Items.BAMBOO_PLANKS)) return 300;
        if (stack.is(Items.BAMBOO_SLAB)) return 150;
        if (stack.is(Items.BAMBOO_STAIRS)) return 300;

        // Logs and planks (tag-based check to support modded wood types)
        if (stack.is(ItemTags.LOGS)) return 300;
        if (stack.is(ItemTags.PLANKS)) return 300;
        if (stack.is(ItemTags.WOODEN_SLABS)) return 150;
        if (stack.is(ItemTags.WOODEN_STAIRS)) return 300;
        if (stack.is(ItemTags.WOODEN_TRAPDOORS)) return 300;
        if (stack.is(ItemTags.WOODEN_DOORS)) return 200;
        if (stack.is(ItemTags.WOODEN_PRESSURE_PLATES)) return 300;
        if (stack.is(ItemTags.WOODEN_BUTTONS)) return 100;
        if (stack.is(ItemTags.WOOL)) return 100;
        if (stack.is(ItemTags.BANNERS)) return 300;
        if (stack.is(ItemTags.BOATS)) return 600;
        if (stack.is(Items.STICK)) return 100;
        if (stack.is(Items.BOWL)) return 100;
        if (stack.is(Items.LADDER)) return 300;
        if (stack.is(Items.BOOKSHELF)) return 300;
        if (stack.is(Items.CHEST)) return 300;
        if (stack.is(Items.TRAPPED_CHEST)) return 300;
        if (stack.is(Items.CRAFTING_TABLE)) return 300;
        if (stack.is(Items.JUKEBOX)) return 300;
        if (stack.is(Items.NOTE_BLOCK)) return 300;
        if (stack.is(Items.FISHING_ROD)) return 300;
        if (stack.is(Items.BOW)) return 300;
        if (stack.is(Items.CROSSBOW)) return 300;
        if (stack.is(Items.WOODEN_SHOVEL)) return 200;
        if (stack.is(Items.WOODEN_PICKAXE)) return 200;
        if (stack.is(Items.WOODEN_AXE)) return 200;
        if (stack.is(Items.WOODEN_HOE)) return 200;
        if (stack.is(Items.WOODEN_SWORD)) return 200;
        if (stack.is(Items.FLINT_AND_STEEL)) return 100;
        if (stack.is(Items.SCAFFOLDING)) return 400;
        if (stack.is(Items.DAYLIGHT_DETECTOR)) return 300;
        if (stack.is(Items.COMPOSTER)) return 300;
        if (stack.is(Items.BARREL)) return 300;
        if (stack.is(Items.CARTOGRAPHY_TABLE)) return 300;
        if (stack.is(Items.FLETCHING_TABLE)) return 300;
        if (stack.is(Items.SMITHING_TABLE)) return 300;
        if (stack.is(Items.LOOM)) return 300;
        if (stack.is(Items.BEEHIVE)) return 300;
        if (stack.is(Items.BEE_NEST)) return 300;
        if (stack.is(Items.AZALEA)) return 100;
        if (stack.is(Items.FLOWERING_AZALEA)) return 100;

        return 0;
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
        boolean cn = TagHelper.containsItem(stack.getItem(), TagHelper.keyItem("kenergyengineering:common_ingots"));
        boolean uc = TagHelper.containsItem(stack.getItem(), TagHelper.keyItem("kenergyengineering:uncommon_ingots"));
        boolean vc = TagHelper.containsItem(stack.getItem(), TagHelper.keyItem("kenergyengineering:valuable_ingots"));
        if (cn) time = 1000;
        else if (uc) time = 1500;
        else if (vc) time = 6400;
        if (time > 0 && !simulate) stack.shrink(1);
        return time * 20;
    }
}
