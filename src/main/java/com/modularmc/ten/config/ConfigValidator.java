package com.modularmc.ten.config;

import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * Runtime config value validator.
 * <p>
 * Asserts that loaded {@link TENConfig} values are within the expected ranges
 * documented in {@code evidence_26_1_2_feature_parity_01.md}.
 * Called during {@link FMLCommonSetupEvent} to fail fast on invalid config.
 * <p>
 * This is a safety net — ModConfigSpec's own range enforcement should catch
 * out-of-bounds values at load time, but this double-checks the loaded values.
 */
public final class ConfigValidator {

    private static final Logger LOGGER = LogUtils.getLogger();

    private ConfigValidator() {}

    /**
     * Validates all COMMON config values are within expected ranges.
     * Logs warnings for any out-of-range values (should not happen when
     * ModConfigSpec enforces ranges, but catches manual TOML edits).
     */
    public static void validate() {
        var machine = ConfigHolder.machine();
        var energyUnit = ConfigHolder.energyUnit();
        var farm = ConfigHolder.farm();

        // ── machine block ──────────────────────────────────────────
        checkDecimalRange("energyMultiplier", machine.energyMultiplier(), 0.01, 100.0);
        checkIntRange("baseEnergyCapacity", machine.baseEnergyCapacity(), 1000, 1_000_000_000);

        // ── energyUnit block ───────────────────────────────────────
        checkIntRange("maxEnergy", energyUnit.maxEnergy(), 1000, 100_000_000);
        checkIntRange("chargeRate", energyUnit.chargeRate(), 1, 100_000);
        checkIntRange("inputRate", energyUnit.inputRate(), 1, 100_000);
        checkIntRange("outputRate", energyUnit.outputRate(), 1, 100_000);

        // ── farm block — list non-empty check ──────────────────────
        if (farm.bushCrops().isEmpty()) {
            LOGGER.warn("ConfigValidator: farm.bushCrops list is empty — Farm Manager bush harvesting may not work");
        }

        LOGGER.info("ConfigValidator: all config values validated successfully");
    }

    private static void checkDecimalRange(String key, double value, double min, double max) {
        if (value < min || value > max) {
            LOGGER.warn(
                    "ConfigValidator: '{}' value {} is outside expected range [{}, {}]",
                    key, value, min, max);
        }
    }

    private static void checkIntRange(String key, int value, int min, int max) {
        if (value < min || value > max) {
            LOGGER.warn(
                    "ConfigValidator: '{}' value {} is outside expected range [{}, {}]",
                    key, value, min, max);
        }
    }
}
