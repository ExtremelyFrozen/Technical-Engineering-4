package com.modularmc.ten.common.blockentity;

import com.modularmc.ten.utils.TagHelper;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.Objects;

/**
 * Fuel value query and consumption for the three engine types.
 * <p>
 * <b>Query API (pure, no side effects):</b>
 * <ul>
 * <li>{@link #getExtractorFuelValue(Level, ItemStack)} — requires non-null Level</li>
 * <li>{@link #getMetalFuelValue(ItemStack)}</li>
 * <li>{@link #getBiomassFuelValue(ItemStack)}</li>
 * </ul>
 * <b>Consumer API (queries + conditionally shrinks stack):</b>
 * <ul>
 * <li>{@link #matchFuel(Level, ItemStack, boolean)} — requires non-null Level</li>
 * <li>{@link #matchFuel(ItemStack, boolean)} — deprecated, null Level fallback</li>
 * <li>{@link #matchMetal(ItemStack, boolean)}</li>
 * <li>{@link #matchPlant(ItemStack, boolean)}</li>
 * </ul>
 */
public class MatchFuel {

    // ════════════════════════════════════════════════════════════════
    // Query API — pure, no side effects
    // ════════════════════════════════════════════════════════════════

    /**
     * Returns the fuel budget for the Extractor engine from an item stack,
     * without consuming it.
     * <p>
     * First queries {@link Level#fuelValues()} via {@link ItemStack#getBurnTime}
     * for a complete fuel list including mod-added fuels. If that returns 0,
     * falls back to a hardcoded compatibility map for items the engine treats
     * as fuel beyond vanilla furnace rules.
     *
     * @param level the client/server level (must be non-null; use
     *              {@link Objects#requireNonNull} at call sites).
     * @param stack the fuel item stack (not modified).
     * @return fuel budget in FE-equivalent units; 0 if not a valid fuel.
     */
    public static int getExtractorFuelValue(Level level, ItemStack stack) {
        Objects.requireNonNull(level, "Level required for Extractor FuelValues lookup");
        if (stack.isEmpty()) return 0;

        // Primary: use 26.1.2 FuelValues API for complete mod-aware fuel list.
        int time = stack.getBurnTime(RecipeType.SMELTING, level.fuelValues());
        if (time > 0) {
            return time * 20;
        }

        // Compatibility supplement: hardcoded map for items recognised by this
        // engine but not by vanilla furnace fuel rules.
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

    /**
     * Matches an item stack as furnace-equivalent fuel for the extraction engine.
     * Requires a non-null Level for complete FuelValues lookup.
     *
     * @param level    the level (must be non-null).
     * @param stack    the fuel item stack.
     * @param simulate if true, the stack is not consumed.
     * @return fuel budget in FE-equivalent units.
     */
    public static int matchFuel(Level level, ItemStack stack, boolean simulate) {
        int value = getExtractorFuelValue(level, stack);
        if (value > 0 && !simulate) stack.shrink(1);
        return value;
    }

    /**
     * @deprecated Use {@link #matchFuel(Level, ItemStack, boolean)} instead.
     *             This overload passes a null Level, bypassing the FuelValues API.
     *             It is kept only for binary compatibility and must NOT be called
     *             from JEI registration or primary engine paths.
     */
    @Deprecated
    public static int matchFuel(ItemStack stack, boolean simulate) {
        int value = getExtractorFuelValueForDeprecated(stack);
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

    /**
     * Hardcoded-only Extractor fuel query for the deprecated null-Level overload.
     * This is intentionally separate so the main getExtractorFuelValue never
     * takes a null Level.
     */
    private static int getExtractorFuelValueForDeprecated(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        return hardcodedBurnTime(stack) * 20;
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

    private MatchFuel() {}
}
