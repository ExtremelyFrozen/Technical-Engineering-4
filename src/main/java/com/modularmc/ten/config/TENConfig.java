package com.modularmc.ten.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/**
 * NeoForge ModConfigSpec definitions for TEN (Kenergy Engineering).
 * <p>
 * Replaces the {@code dev.toma.configuration} annotation system used in origin/1.21.
 * Split into COMMON ({@code te4-config.toml}) and CLIENT ({@code te4-client.toml}) specs.
 */
public final class TENConfig {

    public static final ModConfigSpec COMMON_SPEC;
    public static final MachineConfig MACHINE;
    public static final EnergyUnitConfig ENERGY_UNIT;
    public static final FarmConfig FARM;

    public static final ModConfigSpec CLIENT_SPEC;
    public static final ClientConfig CLIENT;

    static {
        var commonBuilder = new ModConfigSpec.Builder();
        MACHINE = new MachineConfig(commonBuilder);
        ENERGY_UNIT = new EnergyUnitConfig(commonBuilder);
        FARM = new FarmConfig(commonBuilder);
        COMMON_SPEC = commonBuilder.build();

        var clientBuilder = new ModConfigSpec.Builder();
        CLIENT = new ClientConfig(clientBuilder);
        CLIENT_SPEC = clientBuilder.build();
    }

    private TENConfig() {}

    public static final class MachineConfig {

        public final ModConfigSpec.DoubleValue energyMultiplier;
        public final ModConfigSpec.IntValue baseEnergyCapacity;
        public final ModConfigSpec.BooleanValue enableSmelter;
        public final ModConfigSpec.BooleanValue enablePulverizer;
        public final ModConfigSpec.BooleanValue enableCompressor;
        public final ModConfigSpec.BooleanValue enableRefiner;
        public final ModConfigSpec.BooleanValue enableInductionFurnace;
        public final ModConfigSpec.BooleanValue enablePsionicant;
        public final ModConfigSpec.BooleanValue enableBeacon;
        public final ModConfigSpec.BooleanValue enableMobRipper;
        public final ModConfigSpec.BooleanValue enableQuarry;
        public final ModConfigSpec.BooleanValue enableEnchantmentFlusher;
        public final ModConfigSpec.BooleanValue enableCondenser;
        public final ModConfigSpec.BooleanValue enableFarmManager;

        MachineConfig(ModConfigSpec.Builder builder) {
            builder.push("machine").comment("Machine-related configuration options");

            energyMultiplier = builder.comment("Global energy multiplier for all machines").defineInRange("energyMultiplier", 1.0, 0.01, 100.0);
            baseEnergyCapacity = builder.comment("Base energy capacity for machines").defineInRange("baseEnergyCapacity", 10000, 1000, 1_000_000_000);

            enableSmelter = builder.comment("Enable the Smelter machine").define("enableSmelter", true);
            enablePulverizer = builder.comment("Enable the Pulverizer machine").define("enablePulverizer", true);
            enableCompressor = builder.comment("Enable the Compressor machine").define("enableCompressor", true);
            enableRefiner = builder.comment("Enable the Refiner machine").define("enableRefiner", true);
            enableInductionFurnace = builder.comment("Enable the Induction Furnace machine").define("enableInductionFurnace", true);
            enablePsionicant = builder.comment("Enable the Psionicant machine").define("enablePsionicant", true);
            enableBeacon = builder.comment("Enable the Beacon machine").define("enableBeacon", true);
            enableMobRipper = builder.comment("Enable the Mob Ripper machine").define("enableMobRipper", true);
            enableQuarry = builder.comment("Enable the Quarry machine").define("enableQuarry", true);
            enableEnchantmentFlusher = builder.comment("Enable the Enchantment Flusher machine").define("enableEnchantmentFlusher", true);
            enableCondenser = builder.comment("Enable the Condenser machine").define("enableCondenser", true);
            enableFarmManager = builder.comment("Enable the Farm Manager machine").define("enableFarmManager", true);

            builder.pop();
        }

        public double energyMultiplier() {
            return energyMultiplier.get();
        }

        public int baseEnergyCapacity() {
            return baseEnergyCapacity.get();
        }

        public boolean enableSmelter() {
            return enableSmelter.get();
        }

        public boolean enablePulverizer() {
            return enablePulverizer.get();
        }

        public boolean enableCompressor() {
            return enableCompressor.get();
        }

        public boolean enableRefiner() {
            return enableRefiner.get();
        }

        public boolean enableInductionFurnace() {
            return enableInductionFurnace.get();
        }

        public boolean enablePsionicant() {
            return enablePsionicant.get();
        }

        public boolean enableBeacon() {
            return enableBeacon.get();
        }

        public boolean enableMobRipper() {
            return enableMobRipper.get();
        }

        public boolean enableQuarry() {
            return enableQuarry.get();
        }

        public boolean enableEnchantmentFlusher() {
            return enableEnchantmentFlusher.get();
        }

        public boolean enableCondenser() {
            return enableCondenser.get();
        }

        public boolean enableFarmManager() {
            return enableFarmManager.get();
        }
    }

    public static final class EnergyUnitConfig {

        public final ModConfigSpec.IntValue maxEnergy;
        public final ModConfigSpec.IntValue chargeRate;
        public final ModConfigSpec.IntValue inputRate;
        public final ModConfigSpec.IntValue outputRate;
        public final ModConfigSpec.BooleanValue chargingDefault;

        EnergyUnitConfig(ModConfigSpec.Builder builder) {
            builder.push("energyUnit").comment("Energy Unit item configuration");

            maxEnergy = builder.comment("Maximum energy storage of the Energy Unit").defineInRange("maxEnergy", 400_000, 1000, 100_000_000);
            chargeRate = builder.comment("Energy Unit charge rate (FE/t)").defineInRange("chargeRate", 2_000, 1, 100_000);
            inputRate = builder.comment("Energy Unit input rate (FE/t)").defineInRange("inputRate", 2_000, 1, 100_000);
            outputRate = builder.comment("Energy Unit output rate (FE/t)").defineInRange("outputRate", 2_000, 1, 100_000);
            chargingDefault = builder.comment("Default charging mode for new Energy Units").define("chargingDefault", false);

            builder.pop();
        }

        public int maxEnergy() {
            return maxEnergy.get();
        }

        public int chargeRate() {
            return chargeRate.get();
        }

        public int inputRate() {
            return inputRate.get();
        }

        public int outputRate() {
            return outputRate.get();
        }

        public boolean chargingDefault() {
            return chargingDefault.get();
        }
    }

    public static final class FarmConfig {

        public final ModConfigSpec.ConfigValue<List<? extends String>> bushCrops;

        FarmConfig(ModConfigSpec.Builder builder) {
            builder.push("farm").comment("Farm Manager crop configuration");

            bushCrops = builder.comment("List of bush crop block IDs").defineList("bushCrops",
                    List.of("minecraft:sweet_berry_bush"), o -> o instanceof String);

            builder.pop();
        }

        public List<String> bushCrops() {
            return bushCrops.get().stream().map(String::valueOf).toList();
        }
    }

    public static final class ClientConfig {

        public final ModConfigSpec.BooleanValue showMachineHUD;
        public final ModConfigSpec.BooleanValue showCableHUD;

        ClientConfig(ModConfigSpec.Builder builder) {
            builder.push("client").comment("Client-side HUD configuration");

            showMachineHUD = builder.comment("Show machine HUD overlay").define("showMachineHUD", true);
            showCableHUD = builder.comment("Show cable HUD overlay").define("showCableHUD", true);

            builder.pop();
        }

        public boolean showMachineHUD() {
            return showMachineHUD.get();
        }

        public boolean showCableHUD() {
            return showCableHUD.get();
        }
    }
}
