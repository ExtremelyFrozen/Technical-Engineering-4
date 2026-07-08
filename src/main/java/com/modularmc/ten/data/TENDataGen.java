package com.modularmc.ten.data;

/**
 * Decommissioned — data generation migrated to {@link DataGenerators}.
 * <p>
 * Previously used Registrate's {@code REGISTRATE.addDataGenerator(ProviderType.LANG, ...)}.
 * Now all data providers (language, recipes, models) are registered directly
 * via {@link net.neoforged.neoforge.data.event.GatherDataEvent}.
 */
public final class TENDataGen {

    /**
     * No-op placeholder. Kept to avoid breaking {@code CommonProxy.init()} call sites.
     * Will be removed after full migration.
     */
    public static void init() {
        // Data generation is now handled by DataGenerators#gatherData
    }
}
