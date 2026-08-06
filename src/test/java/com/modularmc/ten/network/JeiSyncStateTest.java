// -*- coding: utf-8 -*-
package com.modularmc.ten.network;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Real behavior tests for {@link JeiSyncState} — the pure state machine
 * that coordinates JEI recipe cache injection.
 * <p>
 * All assertions test actual method calls on the state object, NOT
 * source text scanning. The state machine has no JEI or Minecraft
 * dependencies and is fully testable from the test source set.
 * <p>
 * State dimensions tested:
 * <ul>
 *   <li>Session accepting gate (login/logout + late event rejection)</li>
 *   <li>Snapshot readiness + generation counter</li>
 *   <li>Machine injection guard (at most once per active runtime)</li>
 *   <li>Complete lifecycle: login → snapshot → runtime → unavailable → new runtime</li>
 * </ul>
 */
class JeiSyncStateTest {

    // ═══════════════════════════════════════════════════════════════════
    // A. Session lifecycle
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    class SessionLifecycle {

        @Test
        void initialState_notAccepting() {
            var state = new JeiSyncState();
            assertFalse(state.isSessionAccepting(),
                    "Fresh state must not be accepting");
        }

        @Test
        void startSession_accepting() {
            var state = new JeiSyncState();
            state.startSession();
            assertTrue(state.isSessionAccepting(),
                    "After startSession, must be accepting");
        }

        @Test
        void endSession_notAccepting() {
            var state = new JeiSyncState();
            state.startSession();
            state.endSession();
            assertFalse(state.isSessionAccepting(),
                    "After endSession, must not be accepting");
        }

        @Test
        void startSession_resetsCompleteSnapshot() {
            var state = new JeiSyncState();
            state.startSession();
            state.acceptSnapshot(); // mark ready
            assertTrue(state.isSnapshotReady());

            state.startSession(); // re-login
            assertFalse(state.isSnapshotReady(),
                    "startSession must reset snapshot readiness");
        }

        @Test
        void endSession_clearsSnapshot() {
            var state = new JeiSyncState();
            state.startSession();
            state.acceptSnapshot();
            state.endSession();
            assertFalse(state.isSnapshotReady(),
                    "endSession must clear snapshot readiness");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // B. Snapshot readiness
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    class SnapshotReadiness {

        @Test
        void notAccepting_acceptSnapshotReturnsFalse() {
            var state = new JeiSyncState();
            // session not started
            assertFalse(state.acceptSnapshot(),
                    "acceptSnapshot must fail without accepting session");
        }

        @Test
        void accepting_acceptSnapshotReturnsTrue() {
            var state = new JeiSyncState();
            state.startSession();
            assertTrue(state.acceptSnapshot(),
                    "acceptSnapshot must succeed during accepting session");
        }

        @Test
        void acceptSnapshot_incrementsGeneration() {
            var state = new JeiSyncState();
            state.startSession();
            int gen1 = state.getGeneration();
            state.acceptSnapshot();
            int gen2 = state.getGeneration();
            assertTrue(gen2 > gen1,
                    "acceptSnapshot must increment generation");
        }

        @Test
        void acceptSnapshot_twiceSameSessionIncrementsAgain() {
            var state = new JeiSyncState();
            state.startSession();
            state.acceptSnapshot();
            int genAfterFirst = state.getGeneration();
            state.acceptSnapshot(); // second complete snapshot
            int genAfterSecond = state.getGeneration();
            assertTrue(genAfterSecond > genAfterFirst,
                    "Second acceptSnapshot must increment generation again");
        }

        @Test
        void notAccepting_lateEventRejected() {
            var state = new JeiSyncState();
            state.startSession();
            state.acceptSnapshot();
            state.endSession(); // logout

            // Late event after logout
            assertFalse(state.acceptSnapshot(),
                    "Late acceptSnapshot after endSession must be rejected");
        }

        @Test
        void startSession_resetsGenerationStartsNewCounter() {
            var state = new JeiSyncState();
            state.startSession();
            state.acceptSnapshot();
            int genAfterFirstSession = state.getGeneration();
            state.endSession();

            state.startSession(); // new login
            state.acceptSnapshot();
            int genAfterSecondSession = state.getGeneration();
            assertTrue(genAfterSecondSession > genAfterFirstSession,
                    "New session must continue from higher generation");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // C. Machine injection guard — at most once per active runtime
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    class MachineInjectionGuard {

        @Test
        void runtimeBeforeCache_shouldNotInject() {
            var state = new JeiSyncState();
            state.startSession();
            state.activateRuntime(); // onRuntimeAvailable, cache not ready yet

            assertFalse(state.tryClaimInject(),
                    "Should not inject when cache snapshot not ready");
        }

        @Test
        void cacheBeforeRuntime_shouldInject() {
            var state = new JeiSyncState();
            state.startSession();
            state.acceptSnapshot(); // cache ready
            state.activateRuntime(); // runtime later

            assertTrue(state.tryClaimInject(),
                    "Should inject when cache already ready at runtime activation");
        }

        @Test
        void runtimeFirstThenCache_shouldInjectAfterCacheReady() {
            var state = new JeiSyncState();
            state.startSession();
            state.activateRuntime();

            // Cache not ready yet
            assertFalse(state.tryClaimInject());

            // Cache becomes ready
            state.acceptSnapshot();

            assertTrue(state.tryClaimInject(),
                    "Should inject after cache becomes ready");
        }

        @Test
        void injectOnlyOnce_perActiveRuntime() {
            var state = new JeiSyncState();
            state.startSession();
            state.acceptSnapshot();
            state.activateRuntime();

            // First call: inject
            assertTrue(state.tryClaimInject());
            state.markMachinesInjected();

            // Second call: same runtime, already injected
            assertFalse(state.tryClaimInject(),
                    "Must not inject again for same active runtime");
        }

        @Test
        void sameRuntime_cacheUpdateDoesNotReinject() {
            var state = new JeiSyncState();
            state.startSession();
            state.acceptSnapshot();
            state.activateRuntime();
            state.tryClaimInject();
            state.markMachinesInjected();

            // Cache gets a new snapshot (reload)
            state.acceptSnapshot();

            // Same runtime still active → must NOT inject again
            assertFalse(state.tryClaimInject(),
                    "Same runtime must not re-inject even with new cache generation");
        }

        @Test
        void runtimeUnavailableThenNewRuntime_injectsLatestCache() {
            var state = new JeiSyncState();
            state.startSession();
            state.acceptSnapshot();
            state.activateRuntime();
            state.tryClaimInject();
            state.markMachinesInjected();

            // Runtime goes away
            state.deactivateRuntime();

            // New runtime arrives
            state.activateRuntime();

            assertTrue(state.tryClaimInject(),
                    "New runtime must inject current snapshot");
        }

        @Test
        void sameRuntimeRepeatedActivation_doesNotDoubleInject() {
            var state = new JeiSyncState();
            state.startSession();
            state.acceptSnapshot();
            state.activateRuntime();
            state.tryClaimInject();
            state.markMachinesInjected();

            // Same runtime: simulate onRuntimeAvailable called again
            state.activateRuntime();

            assertFalse(state.tryClaimInject(),
                    "Re-activating same runtime must not re-inject");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // D. Complete lifecycle scenarios
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    class CompleteLifecycle {

        @Test
        void loginThenLogoutThenLogin_fullCycle() {
            var state = new JeiSyncState();
            int genInitial = state.getGeneration();

            // First login
            state.startSession();
            state.acceptSnapshot();
            state.activateRuntime();
            assertTrue(state.tryClaimInject());
            state.markMachinesInjected();
            int genAfterFirst = state.getGeneration();
            assertTrue(genAfterFirst > genInitial);

            // Logout
            state.deactivateRuntime();
            state.endSession();

            // Late RecipesReceived from old session (must be rejected)
            assertFalse(state.acceptSnapshot(),
                    "Late event after endSession must be rejected");

            // Second login — fresh state
            state.startSession();
            assertFalse(state.isSnapshotReady());
            assertTrue(state.isSessionAccepting());

            // New session's own RecipesReceived
            state.acceptSnapshot();
            state.activateRuntime();
            assertTrue(state.tryClaimInject(),
                    "New session must inject fresh snapshot");
            state.markMachinesInjected();
            assertFalse(state.tryClaimInject(),
                    "Must not double-inject in new session");
            int genAfterSecond = state.getGeneration();
            assertTrue(genAfterSecond > genAfterFirst,
                    "Second session generation must be higher");
        }
    }
}
