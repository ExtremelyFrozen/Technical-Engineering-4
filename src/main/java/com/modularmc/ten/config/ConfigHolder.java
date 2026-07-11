package com.modularmc.ten.config;

import java.util.List;

/**
 * Facade for {@link TENConfig} — provides typed getter access to all configuration values.
 * <p>
 * All values are backed by NeoForge {@link net.neoforged.neoforge.common.ModConfigSpec}
 * with TOML persistence ({@code te4-config.toml} / {@code te4-client.toml}).
 * <p>
 * No mutable primitive fields — all values are loaded from the Spec at runtime.
 */
public final class ConfigHolder {

    private ConfigHolder() {
        // static access only
    }

    // ── Machine config ────────────────────────────────────────────────

    public static TENConfig.MachineConfig machine() {
        return TENConfig.MACHINE;
    }

    public static double energyMultiplier() {
        return TENConfig.MACHINE.energyMultiplier();
    }

    public static int baseEnergyCapacity() {
        return TENConfig.MACHINE.baseEnergyCapacity();
    }

    // ── Energy Unit config ────────────────────────────────────────────

    public static TENConfig.EnergyUnitConfig energyUnit() {
        return TENConfig.ENERGY_UNIT;
    }

    // ── Farm config ───────────────────────────────────────────────────

    public static TENConfig.FarmConfig farm() {
        return TENConfig.FARM;
    }

    public static List<String> bushCrops() {
        return TENConfig.FARM.bushCrops();
    }

    // ── Client config ─────────────────────────────────────────────────

    public static TENConfig.ClientConfig client() {
        return TENConfig.CLIENT;
    }
}
