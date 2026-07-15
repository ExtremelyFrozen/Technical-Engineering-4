package com.modularmc.ten.data;

import com.modularmc.ten.common.data.Mat;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Validates the material variant recipe matrix at datagen startup.
 * Fail-fast only — no data files written. Post-datagen audit is handled
 * by the external python validation script.
 * <p>
 * Hardcoded business facts: only {@link #NO_RECIPE_ALLOWLIST} and
 * {@link #HISTORICAL_RECIPE_ALIASES}.
 * All form names/itemId/hasForm/isRegistered come from {@link Mat}.
 */
public class TENRecipeMatrixValidator implements DataProvider {

    /**
     * Historical recipe file aliases — legacy files from the renaming of
     * aerolium → tin / powered_aerolium → powered_tin.
     * These files are preserved under their old names so that no external
     * reference breaks, but their output items are the current tin_gear /
     * powered_tin_gear, not any "aerolium" item.
     */
    public static final Map<String, String> HISTORICAL_RECIPE_ALIASES = Map.ofEntries(
            Map.entry("aerolium_gear", "kenergyengineering:tin_gear"),
            Map.entry("powered_aerolium_gear", "kenergyengineering:powered_tin_gear")
    );

    /** Items deliberately without recipes, with historical justification (map = item → reason). */
    public static final Map<String, String> NO_RECIPE_ALLOWLIST = Map.ofEntries(
            Map.entry("mould_compressed_small",
                    "Historically required by compressor but no acquisition recipe; strict parity preserves this"),
            Map.entry("mould_compressed_large",
                    "Historically required by compressor but no acquisition recipe; strict parity preserves this"),
            Map.entry("mould_split",
                    "Historically required by compressor but no acquisition recipe; strict parity preserves this"),
            Map.entry("mould_coin",             "Coin mould — never had a recipe"),
            Map.entry("mould_dense_plate",      "Dense plate mould — never had a recipe"),
            Map.entry("spanner",                "Wrench tool — never had a recipe"),
            Map.entry("energy_capacity",        "Energy unit item — never had a recipe"),
            Map.entry("royal_jelly",            "Royal jelly drop — never had a recipe"),
            Map.entry("spicy_jelly",            "Spicy jelly drop — never had a recipe"),
            Map.entry("starlight_dust",         "Starlight dust — registered standalone, no recipe")
    );

    public TENRecipeMatrixValidator() {
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        validateFormConsistency();
        return CompletableFuture.completedFuture(null);
    }

    /** Check 2: Bidirectional form consistency. Vanilla-backed forms pass through. */
    private void validateFormConsistency() {
        for (Mat mat : Mat.values()) {
            for (String cat : Mat.formNames()) {
                boolean hf = mat.hasForm(cat);
                boolean ir = mat.isRegistered(cat);
                if (!hf && ir) {
                    throw new IllegalStateException(mat.id + "/" + cat + " hasForm=false but isRegistered=true");
                }
                if (hf && !ir && !mat.itemId(cat).startsWith("minecraft:")) {
                    // Starlight dust is registered standalone (in NO_RECIPE_ALLOWLIST), not via matrix.
                    String shortId = mat.itemId(cat).contains(":") ? mat.itemId(cat).substring(mat.itemId(cat).indexOf(':') + 1) : mat.itemId(cat);
                    if (!NO_RECIPE_ALLOWLIST.containsKey(shortId)) {
                        throw new IllegalStateException(mat.id + "/" + cat + " hasForm=true but isRegistered=false (item=" + mat.itemId(cat) + "). Register it via registerVariants or add to allowlist.");
                    }
                }
            }
        }
    }

    @Override
    public String getName() {
        return "TEN Recipe Matrix Validator";
    }
}
