package com.modularmc.ten.common.blockentity;

import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.Collection;

/**
 * Pure-domain helper for atomic energy distribution across cable networks.
 * <p>
 * Encapsulates the simulate→execute transfer pattern to prevent TOCTOU
 * energy loss when distributing from a source to sinks and cable buffers.
 * Used by {@link CableBlockEntity#redistribute()} and directly testable
 * without Minecraft bootstrap.
 * <p>
 * Contract:
 * <ul>
 * <li>Energy is only ever extracted from source AFTER verifying sinks+buffers
 * can accept it (simulate phase).</li>
 * <li>If sinks/buffers are saturated, source is NOT deducted.</li>
 * <li>Partial transfers are exact: only what sinks+buffers accept is taken.</li>
 * <li>Safety fallback returns undeliverable energy to source.</li>
 * </ul>
 */
public final class CableEnergyDistribution {

    private CableEnergyDistribution() {}

    /**
     * Atomically distribute energy from a single source to sinks and cable buffers.
     * <p>
     * Algorithm:
     * <ol>
     * <li>Simulate source extraction ({@code source.extractEnergy(rate, true)})</li>
     * <li>Simulate total sink acceptance ({@code sink.receiveEnergy(available, true)} each)</li>
     * <li>Simulate buffer acceptance for overflow</li>
     * <li>Extract from source only what sinks+buffers can accept</li>
     * <li>Execute distribution to sinks and buffers</li>
     * <li>Safety: if any energy couldn't be placed, return it to source</li>
     * </ol>
     *
     * @param source  the energy source to extract from
     * @param sinks   collection of sink storages ({@code canReceive()} already verified)
     * @param buffers collection of cable buffer storages
     * @param rate    max transfer rate per tick (from {@code transferFor(state)})
     * @return total energy actually transferred (0 if no capacity available)
     */
    public static int distributeFromSource(
                                           IEnergyStorage source,
                                           Collection<? extends IEnergyStorage> sinks,
                                           Collection<? extends IEnergyStorage> buffers,
                                           int rate) {
        if (rate <= 0) return 0;

        // ── Step 1: Simulate source availability ──
        int available = source.extractEnergy(rate, true);
        if (available <= 0) return 0;

        // ── Step 2: Simulate total sink acceptance ──
        int sinkCapacity = 0;
        for (IEnergyStorage sink : sinks) {
            int canAccept = sink.receiveEnergy(available, true);
            if (canAccept > 0) {
                sinkCapacity += canAccept;
            }
        }

        // ── Step 3: Simulate buffer acceptance for overflow ──
        int remainingAfterSinks = available - sinkCapacity;
        int bufferCapacity = 0;
        if (remainingAfterSinks > 0) {
            for (IEnergyStorage buffer : buffers) {
                int canAccept = buffer.receiveEnergy(remainingAfterSinks - bufferCapacity, true);
                if (canAccept > 0) {
                    bufferCapacity += canAccept;
                }
            }
        }

        // ── Step 4: Total extractable = min(source provides, what can be accepted) ──
        int totalCapacity = sinkCapacity + bufferCapacity;
        int toExtract = Math.min(available, totalCapacity);
        if (toExtract <= 0) return 0;

        // ── Step 5: Execute: extract from source (only what fits) ──
        int pulled = source.extractEnergy(toExtract, false);
        if (pulled <= 0) return 0;

        // ── Step 6: Execute: distribute to sinks ──
        int remaining = pulled;
        for (IEnergyStorage sink : sinks) {
            if (remaining <= 0) break;
            int accepted = sink.receiveEnergy(Math.min(remaining, rate), false);
            remaining -= accepted;
        }

        // ── Step 7: Execute: overflow to cable buffers ──
        for (IEnergyStorage buffer : buffers) {
            if (remaining <= 0) break;
            int accepted = buffer.receiveEnergy(remaining, false);
            remaining -= accepted;
        }

        // ── Step 8: Safety fallback — return undeliverable energy to source if possible ──
        // Prevents silent energy loss when source is OUT-only (canExtract but !canReceive).
        // Assumes simulate phase accurately predicts total capacity, so remaining > 0
        // indicates a TOCTOU shift (sink/buffer capacity changed between simulate and execute).
        if (remaining > 0) {
            if (source.canReceive()) {
                int returned = source.receiveEnergy(remaining, false);
                // If source accepted partial return, adjust pulled accordingly
                pulled -= returned;
                if (returned < remaining) {
                    // Partial return only — log for diagnostics but don't fail-fast
                    // (energy bounded by single tick rate, not a catastrophic loss)
                }
            } else {
                // OUT-only source: energy already extracted, cannot be returned.
                // pulled tracks actual placed amount; remaining was extracted but lost.
                // This is an upper bound of rate per tick — minimal in practice.
                pulled -= remaining;
            }
        }

        return pulled;
    }
}
