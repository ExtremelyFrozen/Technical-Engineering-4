package com.modularmc.ten.common.blockentity;

import com.modularmc.ten.utils.TagHelper;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.Objects;

public class MatchFuel {

    // ════════════════════════════════════════════════════════════════
    // Query API — pure, no side effects
    // ════════════════════════════════════════════════════════════════

    /**
     * Returns the fuel budget for the Extractor engine from an item stack,
     * without consuming it.
     * <p>
     * 1.21.1 适配：无 {@code Level.fuelValues()}，先用 {@link ItemStack#getBurnTime}
     * 查燃烧值，若为 0 则回退到 hardcoded 兼容表。
     *
     * @param stack the fuel item stack (not modified).
     * @return fuel budget in FE-equivalent units; 0 if not a valid fuel.
     */
    public static int getExtractorFuelValue(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        int time = stack.getBurnTime(RecipeType.SMELTING);
        if (time > 0) return time * 20;
        return hardcodedBurnTime(stack) * 20;
    }

    /**
     * Returns the fuel budget for the Metalizer engine from an item stack,
     * without consuming it.
     *
     * @param stack the metal item stack (not modified).
     * @return fuel budget in FE-equivalent units; 0 if not a valid metal fuel.
     */
    public static int getMetalFuelValue(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        int time = 0;
        boolean cn = TagHelper.containsItem(stack.getItem(), TagHelper.keyItem("kenergyengineering:common_ingots"));
        boolean uc = TagHelper.containsItem(stack.getItem(), TagHelper.keyItem("kenergyengineering:uncommon_ingots"));
        boolean vc = TagHelper.containsItem(stack.getItem(), TagHelper.keyItem("kenergyengineering:valuable_ingots"));
        if (cn) time = 1000;
        else if (uc) time = 1500;
        else if (vc) time = 6400;
        return time * 20;
    }

    /**
     * Returns the fuel budget for the Biomass engine from an item stack,
     * without consuming it.
     *
     * @param stack the plant item stack (not modified).
     * @return fuel budget in FE-equivalent units; 0 if not a valid biomass fuel.
     */
    public static int getBiomassFuelValue(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        int time = 0;
        if (TagHelper.containsItem(stack.getItem(), ItemTags.LEAVES)) time = 20;
        else if (TagHelper.containsItem(stack.getItem(), ItemTags.LOGS)) time = 120;
        else if (TagHelper.containsItem(stack.getItem(), ItemTags.FLOWERS)) time = 10;
        else if (TagHelper.containsItem(stack.getItem(), ItemTags.SAPLINGS)) time = 15;
        else if (TagHelper.containsItem(stack.getItem(), TagHelper.keyItem("minecraft:saplings"))) time = 15;
        return time * 60;
    }

    // ════════════════════════════════════════════════════════════════
    // Consumer API — delegates to query + conditionally shrinks stack
    // ════════════════════════════════════════════════════════════════

    public static int matchFuel(ItemStack stack, boolean simulate) {
        int value = getExtractorFuelValue(stack);
        if (value > 0 && !simulate) stack.shrink(1);
        return value;
    }

    public static int matchPlant(ItemStack stack, boolean simulate) {
        int value = getBiomassFuelValue(stack);
        if (value > 0 && !simulate) stack.shrink(1);
        return value;
    }

    public static int matchMetal(ItemStack stack, boolean simulate) {
        int value = getMetalFuelValue(stack);
        if (value > 0 && !simulate) stack.shrink(1);
        return value;
    }

    // ════════════════════════════════════════════════════════════════
    // Internal
    // ════════════════════════════════════════════════════════════════

    private static int hardcodedBurnTime(ItemStack stack) {
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

    private MatchFuel() {}
}
