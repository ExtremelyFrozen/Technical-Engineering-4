package com.modularmc.ten.api.registry.registrate;

/**
 * Decommissioned — Registration migrated to native NeoForge DeferredRegister.
 * <p>
 * Previously extended {@code com.tterrag.registrate.AbstractRegistrate}.
 * {@link com.modularmc.ten.common.registry.Registration} now provides all
 * {@link net.neoforged.neoforge.registries.DeferredRegister} instances.
 * <p>
 * This stub exists solely to prevent "file not found" during incremental migration.
 * Will be deleted after Phase 1 is verified.
 *
 * @deprecated Scheduled for removal. Do not use.
 */
@Deprecated(forRemoval = true, since = "26.1.2")
public final class TENRegistrate {

    private TENRegistrate() {
        throw new UnsupportedOperationException("TENRegistrate is deprecated");
    }
}
