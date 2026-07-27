// -*- coding: utf-8 -*-
package com.modularmc.ten.common.blockentity;

import com.modularmc.ten.api.capability.MachineEnergyStorage;

import net.neoforged.neoforge.energy.IEnergyStorage;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Domain tests for Cable energy transfer semantics (P3-T3 S1 fix).
 * <p>
 * Tests the atomic simulate→execute distribution logic via
 * {@link CableEnergyDistribution#distributeFromSource}, ensuring
 * no TOCTOU energy loss when sinks/buffers are saturated.
 * <p>
 * RED phase: all tests fail (helper does not exist yet).
 * GREEN phase: all tests pass after helper + redistribute fix.
 */
class CableEnergyTransferDomainTest {

    /** Helper to create a storage with given capacity, initial energy, and rates */
    static MachineEnergyStorage storage(int capacity, int energy, int maxReceive, int maxExtract) {
        var s = new MachineEnergyStorage(capacity, maxReceive, maxExtract);
        s.setEnergy(energy);
        return s;
    }

    /** Helper to create a cable buffer storage (high receive rate, stores overflow) */
    static MachineEnergyStorage buffer(int capacity, int energy) {
        return storage(capacity, energy, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    // ════════════════════════════════════════════════════════════
    // A. Source no-loss: sinks+buffer full → source unchanged
    // ════════════════════════════════════════════════════════════

    @Nested
    class SourceNoLossWhenSaturated {

        @Test
        void sourceNotDeducted_whenAllSinksFullAndBufferFull() {
            var source = storage(10_000, 5_000, 0, 1_000);
            var sink = storage(1_000, 1_000, 1_000, 0); // full
            var cableBuf = buffer(500, 500); // full

            int moved = CableEnergyDistribution.distributeFromSource(
                    source, List.of(sink), List.of(cableBuf), 1_000);

            assertEquals(0, moved,
                    "Must not transfer when sinks and buffer are full");
            assertEquals(5_000, source.getEnergyStored(),
                    "Source must NOT be deducted when no capacity available");
            assertEquals(1_000, sink.getEnergyStored(),
                    "Sink must remain full");
            assertEquals(500, cableBuf.getEnergyStored(),
                    "Buffer must remain full");
        }

        @Test
        void sourceNotDeducted_whenOnlySinksFullButNoBuffer() {
            var source = storage(10_000, 3_000, 0, 1_000);
            var sink = storage(1_000, 1_000, 1_000, 0); // full

            int moved = CableEnergyDistribution.distributeFromSource(
                    source, List.of(sink), List.of(), 1_000);

            assertEquals(0, moved,
                    "Must not transfer when sink is full and no buffer");
            assertEquals(3_000, source.getEnergyStored(),
                    "Source must NOT be deducted");
        }

        @Test
        void sourceNotDeducted_whenRateExceedsSourceButAllSinksFull() {
            // Source has energy but less than rate, but sinks are full
            var source = storage(10_000, 100, 0, 1_000); // only 100 energy
            var sink = storage(1_000, 1_000, 1_000, 0); // full

            int moved = CableEnergyDistribution.distributeFromSource(
                    source, List.of(sink), List.of(), 1_000);

            assertEquals(0, moved, "Must not transfer when sink is full");
            assertEquals(100, source.getEnergyStored(), "Source must not be deducted");
        }
    }

    // ════════════════════════════════════════════════════════════
    // B. Partial transfer: only transfer what can be accepted
    // ════════════════════════════════════════════════════════════

    @Nested
    class PartialTransferOnlyAcceptedAmount {

        @Test
        void partialTransfer_whenSinkHasSomeSpace() {
            var source = storage(10_000, 5_000, 0, 1_000);
            var sink = storage(1_000, 800, 1_000, 0); // 200 free

            int moved = CableEnergyDistribution.distributeFromSource(
                    source, List.of(sink), List.of(), 1_000);

            assertEquals(200, moved,
                    "Must transfer exactly the free capacity of the sink");
            assertEquals(4_800, source.getEnergyStored(),
                    "Source must be deducted by exactly what was transferred");
            assertEquals(1_000, sink.getEnergyStored(),
                    "Sink must be filled to capacity");
        }

        @Test
        void partialTransfer_whenMultipleSinksPartial() {
            var source = storage(10_000, 5_000, 0, 1_000);
            var sink1 = storage(1_000, 900, 1_000, 0); // 100 free
            var sink2 = storage(2_000, 1_800, 2_000, 0); // 200 free

            int moved = CableEnergyDistribution.distributeFromSource(
                    source, List.of(sink1, sink2), List.of(), 1_000);

            // Sink1 can take 100, sink2 can take 200, total = 300
            // But limited by rate=1000, source availability
            assertEquals(300, moved,
                    "Must transfer sum of free capacities across sinks");
            assertEquals(4_700, source.getEnergyStored(),
                    "Source deducted by total transferred");
            assertEquals(1_000, sink1.getEnergyStored(),
                    "Sink1 must be full");
            assertEquals(2_000, sink2.getEnergyStored(),
                    "Sink2 must be full");
        }

        @Test
        void partialTransfer_whenRateLimitsBelowSinkCapacity() {
            var source = storage(10_000, 5_000, 0, 100); // rate=100
            var sink = storage(1_000, 500, 1_000, 0); // 500 free

            int moved = CableEnergyDistribution.distributeFromSource(
                    source, List.of(sink), List.of(), 100);

            assertEquals(100, moved,
                    "Transfer must be limited by rate, not sink capacity");
            assertEquals(4_900, source.getEnergyStored(),
                    "Source deducted by rate-limited amount");
            assertEquals(600, sink.getEnergyStored(),
                    "Sink received rate-limited amount");
        }
    }

    // ════════════════════════════════════════════════════════════
    // C. Buffer overflow: excess after sinks goes to buffer
    // ════════════════════════════════════════════════════════════

    @Nested
    class BufferOverflow {

        @Test
        void excessAfterSinksGoesToBuffer() {
            var source = storage(10_000, 5_000, 0, 1_000);
            var sink = storage(1_000, 900, 1_000, 0); // 100 free
            var cableBuf = buffer(1_000, 0); // empty buffer

            int moved = CableEnergyDistribution.distributeFromSource(
                    source, List.of(sink), List.of(cableBuf), 1_000);

            // Sink can take 100, buffer can take 900, total = 1000
            assertEquals(1_000, moved,
                    "Must transfer rate amount: sink + buffer capacity");
            assertEquals(4_000, source.getEnergyStored(),
                    "Source deducted by 1000");
            assertEquals(1_000, sink.getEnergyStored(),
                    "Sink must be full");
            assertEquals(900, cableBuf.getEnergyStored(),
                    "Buffer received overflow");
        }

        @Test
        void bufferPartiallyFilled_whenSinksPartiallyAccept() {
            var source = storage(10_000, 5_000, 0, 1_000);
            var sink = storage(1_000, 950, 1_000, 0); // 50 free
            var cableBuf = buffer(1_000, 100); // 900 free

            int moved = CableEnergyDistribution.distributeFromSource(
                    source, List.of(sink), List.of(cableBuf), 1_000);

            // Sink takes 50, buffer takes up to 900, total = 950
            assertEquals(950, moved,
                    "Must fill sink then buffer: min(available=1000, sinkCap=50 + bufCap=900) = 950");
            assertEquals(4_050, source.getEnergyStored());
            assertEquals(1_000, sink.getEnergyStored(), "Sink full");
            assertEquals(1_000, cableBuf.getEnergyStored(), "Buffer full (100+900)");
        }

        @Test
        void noBufferOverflow_whenSinksAbsorbAll() {
            var source = storage(10_000, 5_000, 0, 1_000);
            var sink = storage(2_000, 1_000, 2_000, 0); // 1000 free
            var cableBuf = buffer(1_000, 0);

            int moved = CableEnergyDistribution.distributeFromSource(
                    source, List.of(sink), List.of(cableBuf), 1_000);

            assertEquals(1_000, moved,
                    "All energy goes to sink when it has capacity");
            assertEquals(4_000, source.getEnergyStored());
            assertEquals(2_000, sink.getEnergyStored(), "Sink received all");
            assertEquals(0, cableBuf.getEnergyStored(),
                    "Buffer must remain empty when sink absorbs all");
        }
    }

    // ════════════════════════════════════════════════════════════
    // D. Backpressure: all sinks full, buffer partially free
    // ════════════════════════════════════════════════════════════

    @Nested
    class Backpressure {

        @Test
        void sourceNotDeducted_whenSinksFullAndBufferFull() {
            var source = storage(1_000, 500, 0, 200);
            var sink = storage(500, 500, 500, 0);
            var buf = buffer(500, 500);

            int moved = CableEnergyDistribution.distributeFromSource(
                    source, List.of(sink), List.of(buf), 200);

            assertEquals(0, moved, "Backpressure: no capacity anywhere");
            assertEquals(500, source.getEnergyStored(), "Source preserved");
        }

        @Test
        void limitedByBufferOnly_whenSinksFull() {
            var source = storage(1_000, 500, 0, 1_000);
            var sink = storage(500, 500, 500, 0); // full
            var buf = buffer(500, 400); // 100 free

            int moved = CableEnergyDistribution.distributeFromSource(
                    source, List.of(sink), List.of(buf), 1_000);

            assertEquals(100, moved,
                    "Only transfer what buffer can accept when sinks full");
            assertEquals(400, source.getEnergyStored(),
                    "Source deducted by 100");
            assertEquals(500, buf.getEnergyStored(),
                    "Buffer filled to capacity");
        }
    }

    // ════════════════════════════════════════════════════════════
    // E. Multiple sources (independent distribution)
    // ════════════════════════════════════════════════════════════

    @Nested
    class MultipleSources {

        @Test
        void independentSources_eachDistributesSeparately() {
            var source1 = storage(10_000, 500, 0, 300);
            var source2 = storage(10_000, 200, 0, 300);
            var sink = storage(10_000, 9_000, 10_000, 0); // 1000 free

            int from1 = CableEnergyDistribution.distributeFromSource(
                    source1, List.of(sink), List.of(), 300);
            int from2 = CableEnergyDistribution.distributeFromSource(
                    source2, List.of(sink), List.of(), 300);

            assertEquals(300, from1, "Source1 transfers up to its rate");
            assertEquals(200, from2, "Source2 transfers its remaining energy");
            assertEquals(9_500, sink.getEnergyStored(),
                    "Sink received from both sources");
            assertEquals(200, source1.getEnergyStored());
            assertEquals(0, source2.getEnergyStored());
        }

        @Test
        void multipleSourcesWithSharedSink_firstSourceTakesCapacity() {
            // First source fills the sink, second source finds sink saturated
            var source1 = storage(10_000, 500, 0, 1_000);
            var source2 = storage(10_000, 500, 0, 1_000);
            var sink = storage(2_000, 1_500, 2_000, 0); // 500 free

            int from1 = CableEnergyDistribution.distributeFromSource(
                    source1, List.of(sink), List.of(), 1_000);
            int from2 = CableEnergyDistribution.distributeFromSource(
                    source2, List.of(sink), List.of(), 1_000);

            assertEquals(500, from1, "Source1 fills remaining sink capacity");
            assertEquals(2_000, sink.getEnergyStored(), "Sink full");
            // Sink has 0 remaining capacity, so source2 transfers 0
            assertEquals(0, from2, "Source2 finds no sink capacity");
            assertEquals(500, source2.getEnergyStored(),
                    "Source2 not deducted");
        }
    }

    // ════════════════════════════════════════════════════════════
    // F. Rate limiting
    // ════════════════════════════════════════════════════════════

    @Nested
    class RateLimiting {

        @Test
        void rateLimitsTotalTransfer() {
            var source = storage(10_000, 5_000, 0, 200);
            var sink = storage(5_000, 0, 5_000, 0); // empty, lots of space

            int moved = CableEnergyDistribution.distributeFromSource(
                    source, List.of(sink), List.of(), 200);

            assertEquals(200, moved, "Rate limits to 200");
            assertEquals(4_800, source.getEnergyStored());
            assertEquals(200, sink.getEnergyStored());
        }

        @Test
        void rateLimitWithMultipleSinks_totalStillBounded() {
            var source = storage(10_000, 5_000, 0, 500);
            var sink1 = storage(1_000, 0, 1_000, 0);
            var sink2 = storage(1_000, 0, 1_000, 0);

            // Each sink could take up to 500, but total from source is 500
            // Sink1 gets 500, sink2 gets 0
            int moved = CableEnergyDistribution.distributeFromSource(
                    source, List.of(sink1, sink2), List.of(), 500);

            assertEquals(500, moved, "Rate limits total from source");
            assertEquals(4_500, source.getEnergyStored());
            // Sink1 got 500, sink2 got nothing
            assertEquals(500, sink1.getEnergyStored());
            assertEquals(0, sink2.getEnergyStored());
        }

        @Test
        void rateZeroTransfersNothing() {
            var source = storage(10_000, 1_000, 0, 1_000);
            var sink = storage(1_000, 0, 1_000, 0);

            int moved = CableEnergyDistribution.distributeFromSource(
                    source, List.of(sink), List.of(), 0);

            assertEquals(0, moved, "Rate zero = no transfer");
            assertEquals(1_000, source.getEnergyStored(), "Source untouched");
        }
    }

    // ════════════════════════════════════════════════════════════
    // G. Simulate/Execute consistency
    // ════════════════════════════════════════════════════════════

    @Nested
    class SimulateExecuteConsistency {

        @Test
        void simulateAndExecuteReturnSame_whenStateUnchanged() {
            // The distributeFromSource must internally ensure simulate and
            // execute agree. Test by verifying total moved matches expected.
            var source = storage(10_000, 1_000, 0, 500);
            var sink = storage(1_000, 500, 1_000, 0);

            int moved = CableEnergyDistribution.distributeFromSource(
                    source, List.of(sink), List.of(), 500);

            // Expected: sink can take 500, source provides 500
            assertEquals(500, moved);
            assertEquals(500, source.getEnergyStored());
            assertEquals(1_000, sink.getEnergyStored());
        }
    }

    // ════════════════════════════════════════════════════════════
    // H. No negative / no over-capacity
    // ════════════════════════════════════════════════════════════

    @Nested
    class NoNegativeNoOverCapacity {

        @Test
        void noNegativeTransferReturned() {
            var source = storage(100, 0, 0, 100); // empty
            var sink = storage(100, 0, 100, 0);

            int moved = CableEnergyDistribution.distributeFromSource(
                    source, List.of(sink), List.of(), 100);

            assertTrue(moved >= 0, "Transfer result must never be negative");
            assertEquals(0, moved, "No transfer from empty source");
        }

        @Test
        void neverExceedsSinkCapacity() {
            var source = storage(10_000, 10_000, 0, 1_000);
            var sink = storage(100, 90, 100, 0); // 10 free

            int moved = CableEnergyDistribution.distributeFromSource(
                    source, List.of(sink), List.of(), 1_000);

            assertTrue(moved <= 10, "Must not exceed sink free capacity (10)");
            assertEquals(10, moved);
            assertEquals(100, sink.getEnergyStored(), "Sink not overfilled");
        }

        @Test
        void neverExceedsBufferCapacity() {
            var source = storage(10_000, 5_000, 0, 1_000);
            var sink = storage(500, 500, 500, 0); // full
            var buf = buffer(200, 100); // 100 free

            int moved = CableEnergyDistribution.distributeFromSource(
                    source, List.of(sink), List.of(buf), 1_000);

            assertEquals(100, moved, "Only buffer free capacity");
            assertEquals(4_900, source.getEnergyStored());
            assertEquals(200, buf.getEnergyStored(), "Buffer not overfilled");
        }

        @Test
        void sourceNeverGoesNegative() {
            var source = storage(100, 30, 0, 100); // 30 energy
            var sink = storage(10_000, 0, 10_000, 0); // lots of space

            int moved = CableEnergyDistribution.distributeFromSource(
                    source, List.of(sink), List.of(), 100);

            assertTrue(moved >= 0, "Transfer must be >= 0");
            assertTrue(source.getEnergyStored() >= 0, "Source must never go negative");
            assertEquals(30, moved, "Transfer only what source has");
            assertEquals(0, source.getEnergyStored());
        }
    }

    // ════════════════════════════════════════════════════════════
    // Z. OUT-only source (P4-M1: fallback safety)
    // ════════════════════════════════════════════════════════════

    @Nested
    class OutOnlySource {

        @Test
        void outOnlySource_noEnergyLoss_onFullBackpressure() {
            // Source: OUT-only (canExtract=true, canReceive=false, maxReceive=0)
            // All sinks full + buffer full → must not extract from source
            var source = storage(10_000, 5_000, 0, 1_000);
            var sink = storage(1_000, 1_000, 1_000, 0); // full
            var buf = buffer(500, 500); // full

            int moved = CableEnergyDistribution.distributeFromSource(
                    source, List.of(sink), List.of(buf), 1_000);

            assertEquals(0, moved, "OUT-only source: must not extract when no capacity");
            assertEquals(5_000, source.getEnergyStored(), "Source must not be deducted");
        }

        @Test
        void outOnlySource_transferOnlyWhatFits() {
            // Source: OUT-only, can't receive fallback
            // Sink has limited space, buffer is full
            var source = storage(10_000, 5_000, 0, 1_000);
            var sink = storage(1_000, 950, 1_000, 0); // 50 free
            var buf = buffer(500, 500); // full

            int moved = CableEnergyDistribution.distributeFromSource(
                    source, List.of(sink), List.of(buf), 1_000);

            // Only 50 can fit in sink, buffer is full → transfer exactly 50
            assertEquals(50, moved, "OUT-only: transfer exactly what sink can accept");
            assertEquals(4_950, source.getEnergyStored(), "Source deducted by only what was placed");
            assertEquals(1_000, sink.getEnergyStored(), "Sink filled");
            assertEquals(500, buf.getEnergyStored(), "Buffer unchanged");
        }

        @Test
        void outOnlySource_multipleSinks_accurateTotal() {
            // Source: OUT-only, multiple sinks with partial space
            var source = storage(10_000, 5_000, 0, 1_000);
            var sink1 = storage(500, 450, 500, 0); // 50 free
            var sink2 = storage(500, 480, 500, 0); // 20 free

            int moved = CableEnergyDistribution.distributeFromSource(
                    source, List.of(sink1, sink2), List.of(), 1_000);

            // Total free: 50 + 20 = 70
            assertEquals(70, moved, "OUT-only: sum of sink free capacities");
            assertEquals(4_930, source.getEnergyStored(), "Source deducted by sum");
            assertEquals(500, sink1.getEnergyStored(), "Sink1 filled");
            assertEquals(500, sink2.getEnergyStored(), "Sink2 filled");
        }

        @Test
        void outOnlySource_rateLimited() {
            // Source: OUT-only, limited rate
            var source = storage(10_000, 5_000, 0, 100);
            var sink = storage(10_000, 0, 10_000, 0); // lots of space

            int moved = CableEnergyDistribution.distributeFromSource(
                    source, List.of(sink), List.of(), 100);

            assertEquals(100, moved, "OUT-only: rate-limited transfer");
            assertEquals(4_900, source.getEnergyStored());
            assertEquals(100, sink.getEnergyStored());
        }

        @Test
        void outOnlySource_emptySource_noTransfer() {
            // Source: OUT-only but empty
            var source = storage(10_000, 0, 0, 1_000);
            var sink = storage(10_000, 0, 10_000, 0);

            int moved = CableEnergyDistribution.distributeFromSource(
                    source, List.of(sink), List.of(), 1_000);

            assertEquals(0, moved, "OUT-only empty source: no transfer");
            assertEquals(0, source.getEnergyStored(), "Source unchanged");
        }
    }

    // ════════════════════════════════════════════════════════════
    // Z2. Partial-accept patterns (P4-M2: simulate↔execute consistency)
    // ════════════════════════════════════════════════════════════

    @Nested
    class PartialAcceptPatterns {

        @Test
        void sinkPartialAccept_overMultipleSinks_accurateTotal() {
            // Multiple sinks each accepting different amounts
            var source = storage(100_000, 10_000, 0, 5_000);
            var sink1 = storage(1_000, 800, 1_000, 0); // 200 free
            var sink2 = storage(2_000, 1_500, 2_000, 0); // 500 free
            var sink3 = storage(500, 500, 500, 0); // full

            int moved = CableEnergyDistribution.distributeFromSource(
                    source, List.of(sink1, sink2, sink3), List.of(), 5_000);

            // sink1: 200, sink2: 500, sink3: 0 → total 700
            assertEquals(700, moved, "Partial accept: sum across sinks with different capacities");
            assertEquals(9_300, source.getEnergyStored());
            assertEquals(1_000, sink1.getEnergyStored(), "Sink1 full");
            assertEquals(2_000, sink2.getEnergyStored(), "Sink2 full");
            assertEquals(500, sink3.getEnergyStored(), "Sink3 unchanged (was full)");
        }

        @Test
        void bufferPartialAccept_afterSinkPartialAccept() {
            // Sinks accept some, buffer accepts remainder
            var source = storage(100_000, 10_000, 0, 1_000);
            var sink = storage(1_000, 950, 1_000, 0); // 50 free
            var buf = buffer(1_000, 850); // 150 free

            int moved = CableEnergyDistribution.distributeFromSource(
                    source, List.of(sink), List.of(buf), 1_000);

            // sink: 50, buffer: 150 → total 200
            assertEquals(200, moved, "Sink partial + buffer partial = total");
            assertEquals(9_800, source.getEnergyStored());
            assertEquals(1_000, sink.getEnergyStored(), "Sink filled");
            assertEquals(1_000, buf.getEnergyStored(), "Buffer filled");
        }

        @Test
        void multipleBuffers_sequentialPartialAccept() {
            // Multiple buffers with different free capacities
            var source = storage(100_000, 10_000, 0, 1_000);
            var sink = storage(500, 500, 500, 0); // full
            var buf1 = buffer(500, 400); // 100 free
            var buf2 = buffer(500, 300); // 200 free

            int moved = CableEnergyDistribution.distributeFromSource(
                    source, List.of(sink), List.of(buf1, buf2), 1_000);

            // sink: 0, buf1: 100, buf2: 200 → total 300
            assertEquals(300, moved, "Multiple buffers: sequential partial accept");
            assertEquals(9_700, source.getEnergyStored());
            assertEquals(500, buf1.getEnergyStored(), "Buf1 filled");
            assertEquals(500, buf2.getEnergyStored(), "Buf2 filled");
        }

        @Test
        void simulateMatchesExecute_underPartialAccept() {
            // Verify that simulate→execute produces consistent total
            // by running the same distribution twice
            var source = storage(100_000, 10_000, 0, 1_000);
            var sink1 = storage(2_000, 1_500, 2_000, 0); // 500 free
            var sink2 = storage(1_000, 900, 1_000, 0);   // 100 free
            var buf = buffer(400, 0);                     // 400 free — just enough for overflow

            int moved = CableEnergyDistribution.distributeFromSource(
                    source, List.of(sink1, sink2), List.of(buf), 1_000);

            // sink1: 500, sink2: 100, buf: 400 → total 1000
            assertEquals(1_000, moved, "Full rate distributed across sinks+buffer");
            assertEquals(9_000, source.getEnergyStored());

            // Run again with updated state — all should be saturated
            int moved2 = CableEnergyDistribution.distributeFromSource(
                    source, List.of(sink1, sink2), List.of(buf), 1_000);

            // All full now
            assertEquals(0, moved2, "Second run: all saturated, no transfer");
            assertEquals(9_000, source.getEnergyStored(), "Source unchanged in second run");
        }

        @Test
        void bufferCapacityExhausted_firstRun() {
            // Buffers with capacity just enough to absorb first-run overflow
            var source = storage(100_000, 10_000, 0, 1_000);
            var sink1 = storage(2_000, 1_500, 2_000, 0); // 500 free
            var sink2 = storage(1_000, 900, 1_000, 0);   // 100 free
            var buf = buffer(500, 100);                   // 400 free

            int moved = CableEnergyDistribution.distributeFromSource(
                    source, List.of(sink1, sink2), List.of(buf), 1_000);

            // sink1: 500, sink2: 100, buf: 400 → total 1000
            assertEquals(1_000, moved, "Full rate distributed");
            assertEquals(9_000, source.getEnergyStored());
            assertEquals(500, buf.getEnergyStored(), "Buffer exhausted (100+400)");
        }

        @Test
        void edgeCase_allSinksFull_noBuffer_transferZero() {
            var source = storage(10_000, 5_000, 0, 1_000);
            var sink1 = storage(500, 500, 500, 0);
            var sink2 = storage(500, 500, 500, 0);

            int moved = CableEnergyDistribution.distributeFromSource(
                    source, List.of(sink1, sink2), List.of(), 1_000);

            assertEquals(0, moved, "All sinks full, no buffer → zero transfer");
            assertEquals(5_000, source.getEnergyStored(), "Source preserved");
        }
    }

    // ════════════════════════════════════════════════════════════
    // I. Multiple buffers (cable network)
    // ════════════════════════════════════════════════════════════

    @Nested
    class MultipleBuffers {

        @Test
        void excessDistributesAcrossMultipleBuffers() {
            var source = storage(10_000, 2_000, 0, 1_000);
            var sink = storage(500, 500, 500, 0); // full
            var buf1 = buffer(500, 0);
            var buf2 = buffer(500, 0);

            int moved = CableEnergyDistribution.distributeFromSource(
                    source, List.of(sink), List.of(buf1, buf2), 1_000);

            // Sinks full → all goes to buffers
            assertEquals(1_000, moved, "All energy to buffers");
            assertEquals(1_000, source.getEnergyStored());
            assertEquals(500, buf1.getEnergyStored(), "Buf1 filled");
            assertEquals(500, buf2.getEnergyStored(), "Buf2 filled");
        }

        @Test
        void buffersReceiveOnlyOverflowAfterSinks() {
            var source = storage(10_000, 2_000, 0, 1_000);
            var sink = storage(1_000, 700, 1_000, 0); // 300 free
            var buf1 = buffer(1_000, 0);
            var buf2 = buffer(1_000, 0);

            int moved = CableEnergyDistribution.distributeFromSource(
                    source, List.of(sink), List.of(buf1, buf2), 1_000);

            // Sink takes 300, remaining 700 goes to buf1, buf2 gets nothing
            assertEquals(1_000, moved, "Full rate transferred");
            assertEquals(1_000, source.getEnergyStored());
            assertEquals(1_000, sink.getEnergyStored(), "Sink got 300");
            assertEquals(700, buf1.getEnergyStored(), "Buf1 got 700 overflow");
            assertEquals(0, buf2.getEnergyStored(), "Buf2 got 0");
        }
    }
}
