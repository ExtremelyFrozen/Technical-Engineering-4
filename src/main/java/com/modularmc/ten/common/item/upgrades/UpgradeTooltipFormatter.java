package com.modularmc.ten.common.item.upgrades;

import com.modularmc.ten.TEN;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Runtime tooltip formatter for upgrade items.
 * <p>
 * Formats components from shared {@link UpgradeConstants} and item-specific
 * translation keys. The registry path for each upgrade is derived from its
 * class name via {@link #regPathFor(UpgradeItem)}, which works both in
 * production (with game registries) and in unit tests (without registries).
 * <p>
 * Numerical values use generic translation templates shared across upgrades.
 * Formatting uses {@link Locale#ROOT} to avoid floating-point noise.
 */
public final class UpgradeTooltipFormatter {

    private UpgradeTooltipFormatter() {}

    /**
     * Build the full tooltip component list for an upgrade item.
     * <p>
     * Line 0 is the title (GOLD + BOLD), subsequent lines are GOLD only.
     * Pure-text descriptions use item-specific translation keys;
     * numerical values use generic shared format keys.
     *
     * @param item the upgrade item instance
     * @return ordered list of tooltip components
     */
    public static List<Component> format(UpgradeItem item) {
        List<Component> lines = new ArrayList<>();
        String path = regPathFor(item);
        addTitle(lines, path);

        if (item instanceof LevelupAug aug) {
            addDurationPercent(lines, UpgradeConstants.AUG_DURATION);
            addPowerMultiplier(lines, UpgradeConstants.AUG_POWER);

        } else if (item instanceof LevelupPower pow) {
            addBatchCount(lines, UpgradeConstants.POWER_BATCH);
            addDurationPercent(lines, UpgradeConstants.POWER_DURATION);
            addPowerMultiplier(lines, UpgradeConstants.POWER_POWER);

        } else if (item instanceof LevelupShulker shulk) {
            addBatchCount(lines, UpgradeConstants.SHULKER_BATCH);
            addDurationPercent(lines, UpgradeConstants.SHULKER_DURATION);
            addPowerMultiplier(lines, UpgradeConstants.SHULKER_POWER);

        } else if (item instanceof LevelupSyn syn) {
            addDurationPercent(lines, UpgradeConstants.SYN_DURATION);
            addPowerMultiplier(lines, UpgradeConstants.SYN_POWER);
            addPhotosynFe(lines, UpgradeConstants.SYN_PHOTOSYN_FE);
            addPureText(lines, path, 4); // sky/rain/day limits
            addPureText(lines, path, 5); // Only 1 per machine

        } else if (item instanceof LevelupRg rg) {
            addRangeFraction(lines, UpgradeConstants.RG_RANGE_FRACTION);

        } else if (item instanceof LevelupPotion pot) {
            addAmplifier(lines, UpgradeConstants.POTION_AMPLIFIER);

        } else if (item instanceof LevelupBlast blast) {
            addPureText(lines, path, 1); // Recipe mode: Blast Furnace

        } else if (item instanceof LevelupSmoke smoke) {
            addPureText(lines, path, 1); // Recipe mode: Smoker

        } else if (item instanceof LevelupIce ice) {
            addPureText(lines, path, 1); // Output mode: Ice

        } else if (item instanceof LevelupMagma magma) {
            addPureText(lines, path, 1); // Output mode: Magma

        } else if (item instanceof LevelupMineral mineral) {
            addPureText(lines, path, 1); // Mining mode: Mineral

        } else if (item instanceof LevelupKnow know) {
            addPureText(lines, path, 1); // Adds fluid output tank
            addPureText(lines, path, 2); // Each recipe produces XP

        } else if (item instanceof LevelupStream stream) {
            addPureText(lines, path, 1); // Energy transfer rate: Infinite

        } else {
            // Unknown upgrade: static fallback
            addPureText(lines, "upgrade_tip.unknown", 0);
        }

        return lines;
    }

    /**
     * Derive the registry path for an upgrade item from its class type.
     * <p>
     * Uses a known mapping between UpgradeItem subclasses and their registry
     * paths. This works without requiring the Minecraft {@code BuiltInRegistries}
     * to be initialized, making it usable in unit tests.
     */
    static String regPathFor(UpgradeItem item) {
        if (item instanceof LevelupAug)          return "augmented_levelup";
        if (item instanceof LevelupPower)        return "powered_levelup";
        if (item instanceof LevelupShulker)      return "relic_levelup";
        if (item instanceof LevelupSyn)          return "photosyn_levelup";
        if (item instanceof LevelupRg)           return "range_levelup";
        if (item instanceof LevelupBlast)        return "blast_levelup";
        if (item instanceof LevelupSmoke)        return "smoke_levelup";
        if (item instanceof LevelupPotion)       return "potion_levelup";
        if (item instanceof LevelupIce)          return "ice_levelup";
        if (item instanceof LevelupMagma)        return "magma_levelup";
        if (item instanceof LevelupMineral)      return "mineral_levelup";
        if (item instanceof LevelupKnow)         return "knowledge_levelup";
        if (item instanceof LevelupStream)       return "stream_levelup";
        return item.getClass().getSimpleName(); // fallback
    }

    // ── Title ──────────────────────────────────────────────────────────

    /** Line 0: item-specific title, GOLD + BOLD. */
    private static void addTitle(List<Component> lines, String path) {
        String key = TEN.MOD_ID + "." + path + ".0";
        MutableComponent title = Component.translatable(key)
                .withStyle(ChatFormatting.GOLD)
                .withStyle(ChatFormatting.BOLD);
        lines.add(title);
    }

    // ── Numeric helpers ────────────────────────────────────────────────

    /**
     * Format an integer with explicit sign prefix.
     * <p>
     * Returns a string like {@code "+1"}, {@code "-3"}, {@code "+0"}.
     * Used for batch count, photosyn FE, potion amplifier, and range
     * percentage. The caller passes this string via {@code %s} to
     * {@link Component#translatable}, avoiding unsupported printf format
     * specifiers ({@code %+d}, {@code %d}, {@code %f}) in translation
     * templates.
     */
    public static String formatSignedInt(int value) {
        return (value >= 0 ? "+" : "") + value;
    }

    /**
     * Format a duration multiplier as a signed percent change.
     * <p>
     * (m - 1) * 100 → displayed with explicit +/- sign.
     * Examples: 0.75 → -25%, 1.50 → +50%, 0.60 → -40%
     */
    private static void addDurationPercent(List<Component> lines, double multiplier) {
        int pct = (int) Math.round((multiplier - 1.0) * 100.0);
        String pctStr = (pct >= 0 ? "+" : "") + pct;
        String key = TEN.MOD_ID + ".upgrade_tip.duration.percent";
        lines.add(Component.translatable(key, pctStr).withStyle(ChatFormatting.GOLD));
    }

    /**
     * Format a power multiplier as {@code x} + trailing-zero-free decimal.
     * <p>
     * Uses {@link String#format} with {@link Locale#ROOT} then strips trailing zeros.
     * Examples: 2.00 → "x2", 1.30 → "x1.3", 0.80 → "x0.8"
     */
    private static void addPowerMultiplier(List<Component> lines, double multiplier) {
        // Format with up to 2 decimal places, strip trailing zeros and decimal point
        String raw = String.format(Locale.ROOT, "%.2f", multiplier);
        if (raw.contains(".")) {
            raw = raw.replaceAll("0*$", "").replaceAll("\\.$", "");
        }
        String key = TEN.MOD_ID + ".upgrade_tip.power.multiplier";
        lines.add(Component.translatable(key, raw).withStyle(ChatFormatting.GOLD));
    }

    /**
     * Format an additive batch count with explicit sign.
     * Example: +1 batch, +3 batch
     */
    private static void addBatchCount(List<Component> lines, int count) {
        String key = TEN.MOD_ID + ".upgrade_tip.batch.add";
        lines.add(Component.translatable(key, formatSignedInt(count)).withStyle(ChatFormatting.GOLD));
    }

    /**
     * Format photosynthetic FE/t injection.
     * Example: +10 FE/t photosynthetic power
     */
    private static void addPhotosynFe(List<Component> lines, int fe) {
        String key = TEN.MOD_ID + ".upgrade_tip.photosyn.fe";
        lines.add(Component.translatable(key, formatSignedInt(fe)).withStyle(ChatFormatting.GOLD));
    }

    /**
     * Format additive range fraction as +XX%.
     * Example: 0.50 → +50%
     */
    private static void addRangeFraction(List<Component> lines, double fraction) {
        int pct = (int) Math.round(fraction * 100.0);
        String key = TEN.MOD_ID + ".upgrade_tip.range.add";
        lines.add(Component.translatable(key, formatSignedInt(pct)).withStyle(ChatFormatting.GOLD));
    }

    /**
     * Format potion amplifier boost.
     * Example: +1 potion effect level
     */
    private static void addAmplifier(List<Component> lines, int amp) {
        String key = TEN.MOD_ID + ".upgrade_tip.potion.amplifier";
        lines.add(Component.translatable(key, formatSignedInt(amp)).withStyle(ChatFormatting.GOLD));
    }

    // ── Pure text helper ───────────────────────────────────────────────

    /**
     * Add a pure-text line from item-specific translation key.
     */
    private static void addPureText(List<Component> lines, String path, int index) {
        String key = TEN.MOD_ID + "." + path + "." + index;
        MutableComponent text = Component.translatable(key).withStyle(ChatFormatting.GOLD);
        lines.add(text);
    }
}
