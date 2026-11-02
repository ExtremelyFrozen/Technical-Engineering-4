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

    public static void init() {
        // ModConfigSpec 由 TEN 构造器 registerConfig 注册，此处无初始化动作（保留兼容入口）
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

    public static boolean enableMachine(int machineType) {
        var m = TENConfig.MACHINE;
        return switch (machineType) {
            case com.modularmc.ten.api.option.MachineType.FURNACE -> m.enableSmelter();
            case com.modularmc.ten.api.option.MachineType.PULVERIZER -> m.enablePulverizer();
            case com.modularmc.ten.api.option.MachineType.COMPRESSOR -> m.enableCompressor();
            case com.modularmc.ten.api.option.MachineType.REFINER -> m.enableRefiner();
            case com.modularmc.ten.api.option.MachineType.INDUCTION_FURNACE -> m.enableInductionFurnace();
            case com.modularmc.ten.api.option.MachineType.PSIONICANT -> m.enablePsionicant();
            case com.modularmc.ten.api.option.MachineType.BEACON -> m.enableBeacon();
            case com.modularmc.ten.api.option.MachineType.MOB_RIPPER -> m.enableMobRipper();
            case com.modularmc.ten.api.option.MachineType.QUARRY -> m.enableQuarry();
            case com.modularmc.ten.api.option.MachineType.ENCHANTMENT_FLUSHER -> m.enableEnchantmentFlusher();
            case com.modularmc.ten.api.option.MachineType.MATTER_CONDENSER -> m.enableCondenser();
            case com.modularmc.ten.api.option.MachineType.FARM -> m.enableFarmManager();
            default -> true;
        };
    }

    // ── Energy Unit config ────────────────────────────────────────────

    public static TENConfig.EnergyUnitConfig energyUnit() {
        return TENConfig.ENERGY_UNIT;
    }

    public static int maxEnergy() {
        return TENConfig.ENERGY_UNIT.maxEnergy();
    }

    public static int chargeRate() {
        return TENConfig.ENERGY_UNIT.chargeRate();
    }

    public static int inputRate() {
        return TENConfig.ENERGY_UNIT.inputRate();
    }

    public static int outputRate() {
        return TENConfig.ENERGY_UNIT.outputRate();
    }

    public static boolean chargingDefault() {
        return TENConfig.ENERGY_UNIT.chargingDefault();
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
