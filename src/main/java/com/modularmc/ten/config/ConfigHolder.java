package com.modularmc.ten.config;

import com.modularmc.ten.api.option.IMachineType;
import com.modularmc.ten.api.option.MachineType;

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

    public static boolean enableMachine(IMachineType machineType) {
        var m = TENConfig.MACHINE;
        return switch (machineType) {
            case MachineType.FURNACE -> m.enableSmelter();
            case MachineType.PULVERIZER -> m.enablePulverizer();
            case MachineType.COMPRESSOR -> m.enableCompressor();
            case MachineType.REFINER -> m.enableRefiner();
            case MachineType.INDUCTION_FURNACE -> m.enableInductionFurnace();
            case MachineType.PSIONICANT -> m.enablePsionicant();
            case MachineType.BEACON -> m.enableBeacon();
            case MachineType.MOB_RIPPER -> m.enableMobRipper();
            case MachineType.QUARRY -> m.enableQuarry();
            case MachineType.ENCHANTMENT_FLUSHER -> m.enableEnchantmentFlusher();
            case MachineType.MATTER_CONDENSER -> m.enableCondenser();
            case MachineType.FARM -> m.enableFarmManager();
            default -> true;
        };
    }

    // ── Energy Module config ────────────────────────────────────────────

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
