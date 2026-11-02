package com.modularmc.ten.config;

import com.modularmc.ten.TEN;

import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

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
    public static final CompatConfig COMPAT;

    public static final ModConfigSpec CLIENT_SPEC;
    public static final ClientConfig CLIENT;

    static {
        var commonBuilder = new ModConfigSpec.Builder();
        MACHINE = new MachineConfig(commonBuilder);
        ENERGY_UNIT = new EnergyUnitConfig(commonBuilder);
        FARM = new FarmConfig(commonBuilder);
        COMPAT = new CompatConfig(commonBuilder);
        COMMON_SPEC = commonBuilder.build();

        var clientBuilder = new ModConfigSpec.Builder();
        CLIENT = new ClientConfig(clientBuilder);
        CLIENT_SPEC = clientBuilder.build();
    }

    private TENConfig() {}

    /**
     * 配置界面的翻译键：{@code <modid>.configuration.<路径>}，与 {@code TENLangHandler.addConfig}
     * 的条目一一对应。
     * <p>
     * 必须显式传给 {@code Builder.translation()}：NeoForge 的 {@code ConfigurationScreen} 在没设
     * 翻译键时回退到 {@code <modid>.configuration.<当前层键名>}，**不带分组前缀**——嵌套在
     * {@code compat} 里的 {@code disabledSources} 会去找 {@code ...configuration.disabledSources}，
     * 界面就显示原始键名。
     */
    private static String key(String path) {
        return TEN.MOD_ID + ".configuration." + path;
    }

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
            builder.push("machine").comment("Machine-related configuration options")
                    .translation(key("machine"));

            energyMultiplier = builder.comment("Global energy multiplier for all machines")
                    .translation(key("machine.energyMultiplier"))
                    .defineInRange("energyMultiplier", 1.0, 0.01, 100.0);
            baseEnergyCapacity = builder.comment("Base energy capacity for machines")
                    .translation(key("machine.baseEnergyCapacity"))
                    .defineInRange("baseEnergyCapacity", 10000, 1000, 1_000_000_000);

            enableSmelter = machineToggle(builder, "enableSmelter", "Smelter", true);
            enablePulverizer = machineToggle(builder, "enablePulverizer", "Pulverizer", true);
            enableCompressor = machineToggle(builder, "enableCompressor", "Compressor", true);
            enableRefiner = machineToggle(builder, "enableRefiner", "Refiner", true);
            enableInductionFurnace = machineToggle(builder, "enableInductionFurnace", "Induction Furnace", true);
            enablePsionicant = machineToggle(builder, "enablePsionicant", "Psionicant", true);
            enableBeacon = machineToggle(builder, "enableBeacon", "Beacon", true);
            enableMobRipper = machineToggle(builder, "enableMobRipper", "Mob Ripper", true);
            enableQuarry = machineToggle(builder, "enableQuarry", "Quarry", true);
            enableEnchantmentFlusher = machineToggle(builder, "enableEnchantmentFlusher", "Enchantment Flusher", true);
            enableCondenser = machineToggle(builder, "enableCondenser", "Matter Condenser", true);
            enableFarmManager = machineToggle(builder, "enableFarmManager", "Farm Manager", true);

            builder.pop();
        }

        /** 机器开关的翻译键与开关名同构，集中一处避免逐个手写时漏掉 {@code .translation()}。 */
        private static ModConfigSpec.BooleanValue machineToggle(ModConfigSpec.Builder builder, String name, String label, boolean def) {
            return builder.comment("Enable the " + label + " machine")
                    .translation(key("machine." + name))
                    .define(name, def);
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
            builder.push("energyUnit").comment("Energy Unit item configuration")
                    .translation(key("energyUnit"));

            maxEnergy = builder.comment("Maximum energy storage of the Energy Unit")
                    .translation(key("energyUnit.maxEnergy"))
                    .defineInRange("maxEnergy", 400_000, 1000, 100_000_000);
            chargeRate = builder.comment("Energy Unit charge rate (FE/t)")
                    .translation(key("energyUnit.chargeRate"))
                    .defineInRange("chargeRate", 2_000, 1, 100_000);
            inputRate = builder.comment("Energy Unit input rate (FE/t)")
                    .translation(key("energyUnit.inputRate"))
                    .defineInRange("inputRate", 2_000, 1, 100_000);
            outputRate = builder.comment("Energy Unit output rate (FE/t)")
                    .translation(key("energyUnit.outputRate"))
                    .defineInRange("outputRate", 2_000, 1, 100_000);
            chargingDefault = builder.comment("Default charging mode for new Energy Units")
                    .translation(key("energyUnit.chargingDefault"))
                    .define("chargingDefault", false);

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
            builder.push("farm").comment("Farm Manager crop configuration")
                    .translation(key("farm"));

            bushCrops = builder.comment("List of bush crop block IDs")
                    .translation(key("farm.bushCrops"))
                    .defineList("bushCrops", List.of("minecraft:sweet_berry_bush"),
                            () -> "minecraft:sweet_berry_bush", o -> o instanceof String);

            builder.pop();
        }

        public List<String> bushCrops() {
            return bushCrops.get().stream().map(String::valueOf).toList();
        }
    }

    public static final class ClientConfig {

        public final ModConfigSpec.BooleanValue showMachineHUD;
        public final ModConfigSpec.BooleanValue showCableHUD;
        public final ModConfigSpec.BooleanValue panelAnimationEnabled;
        public final ModConfigSpec.IntValue panelAnimationSpeed;
        public final ModConfigSpec.IntValue upgradePanelAnimationSpeed;

        ClientConfig(ModConfigSpec.Builder builder) {
            builder.push("client").comment("Client-side HUD configuration")
                    .translation(key("client"));

            showMachineHUD = builder.comment("Show machine HUD overlay")
                    .translation(key("client.showMachineHUD"))
                    .define("showMachineHUD", true);
            showCableHUD = builder.comment("Show cable HUD overlay")
                    .translation(key("client.showCableHUD"))
                    .define("showCableHUD", true);

            builder.pop();

            // ── 展开面板动画（用户需求 2026-09）：反馈展开过慢 → 可动态调速/关闭 ──
            builder.push("panelAnimation").comment("Expandable panel animation")
                    .translation(key("panelAnimation"));

            panelAnimationEnabled = builder.comment("Enable expand/collapse animation for config & upgrade panels. False = instant toggle")
                    .translation(key("panelAnimation.enabled"))
                    .define("enabled", true);
            // 统一速度（px/tick）：两面板共用；独立项覆盖统一值（独立函数部分）
            panelAnimationSpeed = builder.comment("Common panel expand speed (px per tick). Higher = faster. Applies when per-panel override is 0")
                    .translation(key("panelAnimation.speed"))
                    .defineInRange("speed", 6, 1, 100);
            upgradePanelAnimationSpeed = builder.comment("Upgrade panel expand speed override (px per tick). 0 = use common speed")
                    .translation(key("panelAnimation.upgradeSpeed"))
                    .defineInRange("upgradeSpeed", 0, 0, 100);

            builder.pop();
        }

        public boolean showMachineHUD() {
            return showMachineHUD.get();
        }

        public boolean showCableHUD() {
            return showCableHUD.get();
        }

        /** 统一函数+独立函数混合：独立项 0 = 回退统一速度。 */
        public int panelAnimationSpeed(String panel) {
            if (!panelAnimationEnabled.get()) {
                return Integer.MAX_VALUE; // 动画关闭 → 单 tick 直达目标宽
            }
            if ("upgrade".equals(panel) && upgradePanelAnimationSpeed.get() > 0) {
                return upgradePanelAnimationSpeed.get();
            }
            return panelAnimationSpeed.get();
        }
    }

    /**
     * 邻居兼容层开关。按来源模组关闭：列出的来源其 compat 配方在加载期整体消失，
     * JEI 也不再显示——1.21.1 没有加载后摘除配方的钩子，只能靠条件过滤。
     * 取值见 {@code TENCompatGen} 的来源表（enderio / enderio_evolution / mekanism /
     * immersiveengineering / alltheores / allthemodium / neoecoae / ae2 / ae2cs / extendedae /
     * extendedae_plus / data_energistics）。
     * <p>
     * {@code providerPriority} 是同一材料被多个模组提供时的归属顺序，由
     * {@code kenergyengineering:provider_priority} 条件在加载期求值：数据为每个候选模组
     * 各写一条互斥配方，装了什么模组、顺序怎么排，决定哪一条真的进配方表。
     */
    public static final class CompatConfig {

        /** 材料归属的默认顺序：本模组优先，其后是自带矿石资产的 ECO，再按「内容更贴近我们 / 更通用」排，原版垫底。 */
        private static final List<String> DEFAULT_PROVIDER_PRIORITY = List.of(
                "kenergyengineering", "neoecoae", "enderio_evolution", "enderio", "mekanism",
                "immersiveengineering", "alltheores", "allthemodium", "minecraft");

        public final ModConfigSpec.ConfigValue<List<? extends String>> disabledSources;
        public final ModConfigSpec.ConfigValue<List<? extends String>> providerPriority;

        CompatConfig(ModConfigSpec.Builder builder) {
            builder.push("compat").comment("Neighbour-mod compat recipe toggles")
                    .translation(key("compat"));
            disabledSources = builder
                    .comment("Compat sources to switch off, e.g. [\"enderio_evolution\"]. Empty = all on.",
                            "A switched-off source loses its compat recipes and stops owning shared",
                            "materials, so the ore chain falls through to the next provider.")
                    .translation(key("compat.disabledSources"))
                    // 必须给 newElementSupplier：不带 supplier 的重载在配置界面里无法新增条目，
                    // 默认空列表时整页是空白的（4.0.2 的「关闭的兼容来源」就是这个症状）。
                    .defineListAllowEmpty("disabledSources", List.of(), () -> "", s -> s instanceof String);
            providerPriority = builder
                    .comment("Which mod owns an item when several mods ship the same material,",
                            "best first. Only consulted where the data ships one variant per provider,",
                            "i.e. the pulverizer ore chain. Candidates missing from this list keep",
                            "their order from the data and lose to anything listed.")
                    .translation(key("compat.providerPriority"))
                    .defineList("providerPriority", DEFAULT_PROVIDER_PRIORITY, () -> "", s -> s instanceof String);
            builder.pop();
        }

        /** 该来源的兼容配方是否启用。 */
        public boolean enabled(String source) {
            return !read(disabledSources, () -> List.<String>of()).contains(source);
        }

        /**
         * 从 candidates 里按 {@code providerPriority} 挑第一个已加载的模组，决定这一份内容归属谁。
         * 不在列表里的候选排在末尾并保持数据里的顺序；一个都没加载时返回 {@code null}。
         */
        public String firstProvider(List<String> candidates) {
            var order = read(providerPriority, () -> DEFAULT_PROVIDER_PRIORITY);
            List<String> sorted = new ArrayList<>(candidates);
            sorted.sort(Comparator.comparingInt(mod -> providerRank(order, mod)));
            for (var mod : sorted) {
                // 关掉的来源既不贡献 compat 配方，也不再拥有共享材料，两个开关语义一致
                if (enabled(mod) && ModList.get().isLoaded(mod)) return mod;
            }
            return null;
        }

        private static int providerRank(List<? extends String> order, String mod) {
            int index = order.indexOf(mod);
            return index < 0 ? Integer.MAX_VALUE : index;
        }

        /**
         * 配方条件在数据包加载期求值，早于配置求值的场景（如极端第三方加载顺序）不能让
         * {@code get()} 抛 "Cannot get config value before config is loaded" 把整批配方带走，
         * 因此未加载时退到默认值。
         */
        private static <T> T read(ModConfigSpec.ConfigValue<T> value, Supplier<T> fallback) {
            return COMMON_SPEC.isLoaded() ? value.get() : fallback.get();
        }
    }
}
