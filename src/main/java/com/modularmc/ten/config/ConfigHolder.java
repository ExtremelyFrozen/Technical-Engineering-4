package com.modularmc.ten.config;

import com.modularmc.ten.TEN;

import dev.toma.configuration.Configuration;
import dev.toma.configuration.config.Config;
import dev.toma.configuration.config.Configurable;
import dev.toma.configuration.config.format.ConfigFormats;
import org.jetbrains.annotations.ApiStatus;

@Config(id = TEN.MOD_ID)
public class ConfigHolder {

    public static ConfigHolder INSTANCE;

    @ApiStatus.Internal
    public static dev.toma.configuration.config.ConfigHolder<ConfigHolder> INTERNAL_INSTANCE;

    public static void init() {
        if (INSTANCE == null || INTERNAL_INSTANCE == null) {
            INTERNAL_INSTANCE = Configuration.registerConfig(ConfigHolder.class, ConfigFormats.YAML);
            INSTANCE = INTERNAL_INSTANCE.getConfigInstance();
        }
    }

    @Configurable
    @Configurable.Comment("Machine-related configuration options")
    public MachineConfigs machine = new MachineConfigs();

    @Configurable
    @Configurable.Comment("Energy Unit configuration options")
    public EnergyUnitConfigs energyUnit = new EnergyUnitConfigs();

    @Configurable
    @Configurable.Comment("Client-side configuration options")
    public ClientConfigs client = new ClientConfigs();

    public static class MachineConfigs {

        @Configurable
        @Configurable.Comment("Energy multiplier for machines (higher = more efficient)")
        @Configurable.DecimalRange(min = 0.01, max = 100.0)
        public double energyMultiplier = 1.0;

        @Configurable
        @Configurable.Comment("Base energy capacity for machines in FE")
        @Configurable.Range(min = 1000, max = 1000000000)
        public int baseEnergyCapacity = 10000;

        @Configurable
        @Configurable.Comment("Enable/disable specific machines")
        public boolean enableSmelter = true;

        @Configurable
        public boolean enablePulverizer = true;

        @Configurable
        public boolean enableCompressor = true;

        @Configurable
        public boolean enableRefiner = true;

        @Configurable
        public boolean enableInductionFurnace = true;

        @Configurable
        public boolean enablePsionicant = true;

        @Configurable
        public boolean enableBeacon = true;

        @Configurable
        public boolean enableMobRipper = true;

        @Configurable
        public boolean enableQuarry = true;

        @Configurable
        public boolean enableEnchantmentFlusher = true;

        @Configurable
        public boolean enableCondenser = true;

        @Configurable
        public boolean enableFarmManager = true;
    }

    @Configurable
    @Configurable.Comment("Farm Manager configuration options")
    public FarmConfigs farm = new FarmConfigs();

    public static class FarmConfigs {

        @Configurable
        @Configurable.Comment("Block IDs treated as bush-type crops (harvested without replanting)")
        public String[] bushCrops = {
            "minecraft:sweet_berry_bush"
        };
    }


    public static class EnergyUnitConfigs {

        @Configurable
        @Configurable.Comment("Maximum FE storage capacity of the Energy Unit")
        @Configurable.Range(min = 1000, max = 100000000)
        public int maxEnergy = 400_000;

        @Configurable
        @Configurable.Comment("FE per tick distributed to charged items when charging mode is on")
        @Configurable.Range(min = 1, max = 100000)
        public int chargeRate = 2_000;

        @Configurable
        @Configurable.Comment("FE per tick the Energy Unit can receive from external sources")
        @Configurable.Range(min = 1, max = 100000)
        public int inputRate = 2_000;

        @Configurable
        @Configurable.Comment("FE per tick the Energy Unit can output to external sources")
        @Configurable.Range(min = 1, max = 100000)
        public int outputRate = 2_000;

        @Configurable
        @Configurable.Comment("Whether charging mode is enabled by default when a new Energy Unit is crafted")
        public boolean chargingDefault = false;
    }

    public static class ClientConfigs {

        @Configurable
        @Configurable.Comment("Show machine info in HUD")
        public boolean showMachineHUD = true;

        @Configurable
        @Configurable.Comment("Show cable network info in HUD")
        public boolean showCableHUD = true;
    }
}
