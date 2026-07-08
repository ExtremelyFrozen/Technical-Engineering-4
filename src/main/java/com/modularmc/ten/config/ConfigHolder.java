package com.modularmc.ten.config;

import com.modularmc.ten.TEN;

/**
 * Migration stub — Configuration library is not available for NeoForge 26.1.2.
 * <p>
 * Previously used {@code dev.toma.configuration} for annotation-driven config.
 * Now hardcoded to sensible defaults. A proper config system (NeoForge
 * {@link net.neoforged.neoforge.common.ModConfigSpec}) will be implemented
 * in a subsequent pass.
 * <p>
 * TODO(26.1.2): Replace with ModConfigSpec-based config.
 */
public final class ConfigHolder {

    public static ConfigHolder INSTANCE = new ConfigHolder();

    public static void init() {
        // Stub — config values use Java field defaults below.
        // ModConfigSpec migration pending.
    }

    // ── Config sections (hardcoded defaults) ──────────────────────

    public final MachineConfigs machine = new MachineConfigs();
    public final EnergyUnitConfigs energyUnit = new EnergyUnitConfigs();
    public final ClientConfigs client = new ClientConfigs();
    public final FarmConfigs farm = new FarmConfigs();

    public static class MachineConfigs {
        public double energyMultiplier = 1.0;
        public int baseEnergyCapacity = 10000;
        public boolean enableSmelter = true;
        public boolean enablePulverizer = true;
        public boolean enableCompressor = true;
        public boolean enableRefiner = true;
        public boolean enableInductionFurnace = true;
        public boolean enablePsionicant = true;
        public boolean enableBeacon = true;
        public boolean enableMobRipper = true;
        public boolean enableQuarry = true;
        public boolean enableEnchantmentFlusher = true;
        public boolean enableCondenser = true;
        public boolean enableFarmManager = true;
    }

    public static class FarmConfigs {
        public String[] bushCrops = { "minecraft:sweet_berry_bush" };
    }

    public static class EnergyUnitConfigs {
        public int maxEnergy = 400_000;
        public int chargeRate = 2_000;
        public int inputRate = 2_000;
        public int outputRate = 2_000;
        public boolean chargingDefault = false;
    }

    public static class ClientConfigs {
        public boolean showMachineHUD = true;
        public boolean showCableHUD = true;
    }
}
