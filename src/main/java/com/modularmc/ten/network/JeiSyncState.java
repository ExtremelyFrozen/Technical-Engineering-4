// -*- coding: utf-8 -*-
package com.modularmc.ten.network;

/**
 * Pure state machine for JEI recipe cache↔runtime coordination.
 * <p>
 * Has no JEI or Minecraft dependencies — fully testable from the test
 * source set. Manages three orthogonal state dimensions:
 * <ol>
 * <li><b>Session accepting gate</b> — opened on login, closed on logout;
 * late recipe events are rejected.</li>
 * <li><b>Snapshot readiness</b> — set when a complete set of TEN machine
 * recipe types has been received; cleared on session end.</li>
 * <li><b>Machine injection guard</b> — at most one injection per active
 * runtime; reset when runtime becomes unavailable.</li>
 * </ol>
 * <p>
 * Thread safety: all methods are {@code synchronized} because the state
 * is accessed from both the NeoForge event bus thread (cache updates)
 * and JEI's runtime lifecycle thread.
 */
public final class JeiSyncState {

    // ── Session gate ─────────────────────────────────────────────────
    private boolean sessionAccepting;

    // ── Snapshot ─────────────────────────────────────────────────────
    private boolean snapshotReady;
    private int generation;

    // ── Machine injection guard ─────────────────────────────────────
    private boolean activeManagerPresent;
    private boolean machinesInjectedForActiveManager;

    // ═══════════════════════════════════════════════════════════════════
    // Session lifecycle
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Opens the session accepting gate. Must be called on login
     * ({@code ClientPlayerNetworkEvent.LoggingIn}).
     * Resets snapshot readiness for the new session.
     * Increments generation to invalidate stale references.
     */
    public synchronized void startSession() {
        sessionAccepting = true;
        snapshotReady = false;
        generation++;
    }

    /**
     * Closes the session accepting gate. Must be called on logout
     * ({@code ClientPlayerNetworkEvent.LoggingOut}) after the cache
     * has been cleared.
     * Resets all session-local state.
     */
    public synchronized void endSession() {
        sessionAccepting = false;
        snapshotReady = false;
        generation++;
    }

    /**
     * Returns {@code true} if the session is accepting recipe events.
     */
    public synchronized boolean isSessionAccepting() {
        return sessionAccepting;
    }

    // ═══════════════════════════════════════════════════════════════════
    // Snapshot
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Accepts a complete recipe snapshot into the state machine.
     * Only succeeds if the session gate is open; rejects late events.
     * Marks the snapshot as ready and increments the generation counter.
     *
     * @return {@code true} if the snapshot was accepted
     */
    public synchronized boolean acceptSnapshot() {
        if (!sessionAccepting) return false;
        snapshotReady = true;
        generation++;
        return true;
    }

    /**
     * Returns {@code true} if a complete recipe snapshot is ready.
     */
    public synchronized boolean isSnapshotReady() {
        return snapshotReady;
    }

    /**
     * Returns the current generation counter. Monotonically increases
     * on each logged session, snapshot accept, and snapshot end.
     */
    public synchronized int getGeneration() {
        return generation;
    }

    // ═══════════════════════════════════════════════════════════════════
    // Runtime activation
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Marks a JEI runtime as active. Must be called from
     * {@code onRuntimeAvailable}.
     * <p>
     * Multiple calls with the same runtime are idempotent — they do
     * not reset the injection guard.
     */
    public synchronized void activateRuntime() {
        if (activeManagerPresent) {
            // Already have an active runtime; re-activation is a no-op
            // (same manager identity, guard stays).
            return;
        }
        activeManagerPresent = true;
        machinesInjectedForActiveManager = false;
    }

    /**
     * Marks the JEI runtime as unavailable. Must be called from
     * {@code onRuntimeUnavailable}.
     * Resets the injection guard so the next runtime can inject.
     */
    public synchronized void deactivateRuntime() {
        activeManagerPresent = false;
        machinesInjectedForActiveManager = false;
    }

    /**
     * Returns {@code true} if a JEI runtime is currently active.
     */
    public synchronized boolean isRuntimeActive() {
        return activeManagerPresent;
    }

    // ═══════════════════════════════════════════════════════════════════
    // Machine injection
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Attempts to claim an injection slot. Returns {@code true} if
     * the caller should proceed with injection, {@code false} if
     * conditions are not met or injection has already happened.
     * <p>
     * Conditions: session accepting, snapshot ready, runtime active,
     * and not already injected for this active runtime.
     * <p>
     * After a successful claim, the caller MUST call
     * {@link #markMachinesInjected()} to record the injection.
     * This two-phase protocol prevents double-injection even if
     * the cache is updated between the claim and the injection.
     *
     * @return {@code true} if injection may proceed
     */
    public synchronized boolean tryClaimInject() {
        if (!sessionAccepting) return false;
        if (!snapshotReady) return false;
        if (!activeManagerPresent) return false;
        return !machinesInjectedForActiveManager;
    }

    /**
     * Records that machine recipes have been injected for the current
     * active runtime. Subsequent calls to {@link #tryClaimInject()}
     * will return {@code false} until the runtime is deactivated.
     */
    public synchronized void markMachinesInjected() {
        machinesInjectedForActiveManager = true;
    }

    /**
     * Returns {@code true} if machine recipes have been injected for
     * the current active runtime.
     */
    public synchronized boolean isMachinesInjected() {
        return machinesInjectedForActiveManager;
    }

    // ═══════════════════════════════════════════════════════════════════
    // Bulk reset
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Resets all state to initial values. Primarily for testing.
     */
    public synchronized void reset() {
        sessionAccepting = false;
        snapshotReady = false;
        generation = 0;
        activeManagerPresent = false;
        machinesInjectedForActiveManager = false;
    }
}
