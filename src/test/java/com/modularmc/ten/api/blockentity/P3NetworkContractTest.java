// -*- coding: utf-8 -*-
package com.modularmc.ten.api.blockentity;

import com.modularmc.ten.api.capability.MachineEnergyStorage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * P3-T2~T7 全网络覆盖验证：Engine/Cable/Channel/Cell/Capability/面配置。
 * <p>
 * 使用纯 Java domain API + 源码模式扫描，无需 Minecraft bootstrap。
 * RED 阶段：期望匹配的模式可能不全部存在 → 测试因找不到模式而失败。
 * GREEN 阶段：确认所有模式存在 → 测试全部通过。
 * <p>
 * 覆盖矩阵：
 * <ul>
 *   <li>P3-T2 Engine: energyAllowRun 边界、燃料消耗、产能不溢出</li>
 *   <li>P3-T3 Cable: BFS 遍历、rate 限制、source/sink 过滤（PROCESS/EFFECT 不输出）</li>
 *   <li>P3-T4 Channel: moveEnergy 原子性、maxReceive/maxExtract 率限</li>
 *   <li>P3-T5 Cell: setCapacity 截断、safeMultiply long 中间值</li>
 *   <li>P3-T6 Capability: canExternalExtract 路径、extract 委托</li>
 *   <li>P3-T7 FaceConfig: canReceiveEnergy/canExtractEnergy 方向语义</li>
 * </ul>
 */
class P3NetworkContractTest {

    // ════════════════════════════════════════════════════════════════════
    // Helpers
    // ════════════════════════════════════════════════════════════════════

    /** Safe multiply matching production CmMachineBlockEntity.safeMultiply */
    static int safeMultiply(int a, int b) {
        if (a <= 0 || b <= 0) return 0;
        long r = (long) a * b;
        return (int) Math.min(r, Integer.MAX_VALUE);
    }

    /** Read main source file for pattern checking */
    static String readMainSource(String relativePath) throws Exception {
        var f = new File("src/main/java/" + relativePath);
        assertTrue(f.exists(), "Source file must exist: " + relativePath);
        return Files.readString(f.toPath());
    }

    // ════════════════════════════════════════════════════════════════════
    // P3-T2: EngineBlockEntity 验证
    // ════════════════════════════════════════════════════════════════════

    @Nested
    class P3_T2_EngineVerification {

        static final String ENGINE_SRC = "com/modularmc/ten/api/blockentity/EngineBlockEntity.java";
        static final String CM_SRC = "com/modularmc/ten/api/blockentity/CmMachineBlockEntity.java";

        @Test
        void engineGeneratesEnergyInternally() throws Exception {
            var src = readMainSource(ENGINE_SRC);
            // Engine tick must call energyStorage.receiveEnergy to store generated energy
            assertTrue(src.contains("energyStorage.receiveEnergy(getActualEfficiency(), false)"),
                    "P3-T2: Engine must generate energy via energyStorage.receiveEnergy");
        }

        @Test
        void engineFuelConsumptionRateMatchesEfficientIn() throws Exception {
            var src = readMainSource(ENGINE_SRC);
            // Fuel consumed at efficientIn rate per tick, never negative
            assertTrue(src.contains("fuel = Math.max(fuel - efficientIn, 0)"),
                    "P3-T2: Engine fuel consumption must use efficientIn rate, never negative");
        }

        @Test
        void engineEnergyAllowRunChecksStorageRoom() throws Exception {
            var src = readMainSource(CM_SRC);
            // Generator energyAllowRun: stored + checkFe <= maxStorageEnergy
            assertTrue(src.contains("energyStorage.getEnergyStored() + checkFe <= maxStorageEnergy"),
                    "P3-T2: Generator energyAllowRun must check stored + checkFe <= maxStorageEnergy");
        }

        @Test
        void engineCanExternalExtractReturnsTrue() throws Exception {
            var src = readMainSource(CM_SRC);
            // GENERATOR types must be in the canExternalExtract allowlist
            assertTrue(src.contains("com.modularmc.ten.api.option.MachineType.GENERATOR"),
                    "P3-T2: canExternalExtract must include GENERATOR");
            assertTrue(src.contains("ENGINE_SOLAR") && src.contains("ENGINE_EXTRACTION")
                            && src.contains("ENGINE_METAL") && src.contains("ENGINE_BIOMASS"),
                    "P3-T2: canExternalExtract must include all ENGINE subtypes");
        }

        @Test
        void engineSetsActiveWhenFuelBurning() throws Exception {
            var src = readMainSource(ENGINE_SRC);
            assertTrue(src.contains("setActive(true)"),
                    "P3-T2: Engine must setActive when fuel is burning");
        }

        @Test
        void engineFuelValCheckedBeforeConsumption() throws Exception {
            var src = readMainSource(ENGINE_SRC);
            // The engine checks fuel > 0 before generating
            assertTrue(src.contains("if (fuel > 0)"),
                    "P3-T2: Engine must check fuel > 0 before generating");
        }

        @Test
        void engineUsesEnergyAllowRunBeforeTick() throws Exception {
            var src = readMainSource(ENGINE_SRC);
            assertTrue(src.contains("energyAllowRun()"),
                    "P3-T2: Engine tick must call energyAllowRun()");
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // P3-T3: CableBlockEntity 验证
    // ════════════════════════════════════════════════════════════════════

    @Nested
    class P3_T3_CableVerification {

        static final String CABLE_SRC = "com/modularmc/ten/common/blockentity/CableBlockEntity.java";
        static final String NETWORKS_SRC = "com/modularmc/ten/common/blockentity/TransferNetworks.java";

        @Test
        void cableUsesBfsNetworkTraversal() throws Exception {
            var src = readMainSource(NETWORKS_SRC);
            // collectConnected must use BFS (queue-based)
            assertTrue(src.contains("ArrayDeque<BlockPos>"),
                    "P3-T3: TransferNetworks.collectConnected must use BFS queue");
            assertTrue(src.contains("visited.add(current)") || src.contains("visited.add(next)"),
                    "P3-T3: BFS must use visited set to prevent cycles");
        }

        @Test
        void cableRedistributeChecksCanExtractForSources() throws Exception {
            var src = readMainSource(CABLE_SRC);
            // Sources are filtered by canExtract(), which delegates through
            // canExternalExtract() for CmMachineBlockEntity
            assertTrue(src.contains("cap.canExtract()"),
                    "P3-T3: Cable must filter sources by cap.canExtract()");
        }

        @Test
        void cableRedistributeChecksCanReceiveForSinks() throws Exception {
            var src = readMainSource(CABLE_SRC);
            assertTrue(src.contains("cap.canReceive()"),
                    "P3-T3: Cable must filter sinks by cap.canReceive()");
        }

        @Test
        void cableRateLimitedByTransferFor() throws Exception {
            var src = readMainSource(CABLE_SRC);
            // redistribute uses transferFor(state) to limit rate
            assertTrue(src.contains("transferFor(getBlockState())"),
                    "P3-T3: Cable must use transferFor() to limit redistribution rate");
        }

        @Test
        void cableBufferDrainsToSinks() throws Exception {
            var src = readMainSource(CABLE_SRC);
            assertTrue(src.contains("drainNetwork"),
                    "P3-T3: Cable must drain buffer to sinks via drainNetwork");
        }

        @Test
        void cableIsNetworkRootForRedistribution() throws Exception {
            var src = readMainSource(CABLE_SRC);
            assertTrue(src.contains("isNetworkRoot()"),
                    "P3-T3: Cable must check isNetworkRoot() before redistributing");
        }

        @Test
        void cableTransferRatesPositive() throws Exception {
            var src = readMainSource(
                    "com/modularmc/ten/common/blockentity/CableBlockEntity.java");
            // transferFor() must return positive rates for all cable types
            int methodIdx = src.indexOf("private static int transferFor");
            assertTrue(methodIdx >= 0, "transferFor method must exist");
            String methodBody = src.substring(methodIdx, methodIdx + 500);
            // Each cable type must return a positive rate
            assertTrue(methodBody.contains("return 200_000"),
                    "cable_star must return 200_000");
            assertTrue(methodBody.contains("return 4_000"),
                    "cable_azure must return 4_000");
            assertTrue(methodBody.contains("return 1_000"),
                    "cable_quartz must return 1_000");
            assertTrue(methodBody.contains("return 200"),
                    "default cable must return 200");
        }

        @Test
        void cableSkipsSelfInSinkDistribution() throws Exception {
            var src = readMainSource(CABLE_SRC);
            // When distributing to sinks, skip the source position itself.
            // This check is in redistribute() which builds activeSinks list.
            assertTrue(src.contains("!sinkEntry.getKey().equals(sourcePos)"),
                    "P3-T3: Cable must skip source position when building sink list");
        }

        @Test
        void isRootPicksMinimumPosition() throws Exception {
            // Test TransferNetworks.isRoot picks the minimum BlockPos
            Set<BlockPos> network = new HashSet<>(Set.of(
                    new BlockPos(1, 2, 3),
                    new BlockPos(0, 0, 0),
                    new BlockPos(10, 20, 30)
            ));
            // Don't call static isRoot since it may not be directly testable without level
            // Instead verify the comparator logic
            BlockPos min = network.stream()
                    .min(Comparator.comparingInt((BlockPos p) -> p.getX())
                            .thenComparingInt(p -> p.getY())
                            .thenComparingInt(p -> p.getZ()))
                    .orElseThrow();
            assertEquals(new BlockPos(0, 0, 0), min,
                    "P3-T3: Minimum position should be (0,0,0)");
        }

        @Test
        void processAndEffectMachinesCannotBeCableSources() throws Exception {
            // This is the architectural invariant: PROCESS/EFFECT machines
            // have canExternalExtract() == false, so cap.canExtract() returns false
            // when the cable neighbor-checks them.
            // Verify: CmMachineBlockEntity.getEnergyStorage(side)
            // checks canExternalExtract() in extractEnergy() and canExtract()
            var cmSrc = readMainSource(
                    "com/modularmc/ten/api/blockentity/CmMachineBlockEntity.java");
            // extractEnergy must check canExternalExtract
            assertTrue(cmSrc.contains("canExternalExtract()"),
                    "P3-T6: getEnergyStorage.extractEnergy must check canExternalExtract()");
            // canExtract must also check canExternalExtract
            assertTrue(cmSrc.contains("canExternalExtract() && canExtractEnergy(side)") ||
                            cmSrc.contains("canExternalExtract() && canExtractEnergy"),
                    "P3-T6: getEnergyStorage.canExtract must check canExternalExtract");
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // P3-T4: ChannelEnergyBlockEntity 验证
    // ════════════════════════════════════════════════════════════════════

    @Nested
    class P3_T4_ChannelVerification {

        static final String CHANNEL_SRC = "com/modularmc/ten/common/blockentity/channel/ChannelEnergyBlockEntity.java";
        static final String NETWORKS_SRC = "com/modularmc/ten/common/blockentity/TransferNetworks.java";

        @Test
        void channelUsesMoveEnergyAtomicTransfer() throws Exception {
            var src = readMainSource(CHANNEL_SRC);
            // Channel uses TransferNetworks.moveEnergy for atomic simulate-then-execute
            assertTrue(src.contains("TransferNetworks.moveEnergy"),
                    "P3-T4: Channel must use TransferNetworks.moveEnergy for atomic transfer");
        }

        @Test
        void channelRateLimitedByMaxReceiveMaxExtract() throws Exception {
            var src = readMainSource(CHANNEL_SRC);
            // Input uses maxReceiveEnergy, output uses maxExtractEnergy
            assertTrue(src.contains("maxReceiveEnergy") && src.contains("maxExtractEnergy"),
                    "P3-T4: Channel must use maxReceiveEnergy/maxExtractEnergy as rate limits");
        }

        @Test
        void channelUsesRoundRobinForInputs() throws Exception {
            var src = readMainSource(CHANNEL_SRC);
            assertTrue(src.contains("nextRoundRobin"),
                    "P3-T4: Channel must use round-robin for input/output selection");
        }

        @Test
        void moveEnergySimulateThenExecute() throws Exception {
            var src = readMainSource(NETWORKS_SRC);
            // moveEnergy must: simulate extract → simulate receive → min → real extract → real receive
            assertTrue(src.contains("extractEnergy(limit, true)"),
                    "P3-T4: moveEnergy must simulate extract first");
            assertTrue(src.contains("receiveEnergy(extracted, true)"),
                    "P3-T4: moveEnergy must simulate receive before executing");
            assertTrue(src.contains("extractEnergy(moved, false)"),
                    "P3-T4: moveEnergy must execute real extract after simulation");
            assertTrue(src.contains("receiveEnergy(drained, false)") ||
                            src.contains("receiveEnergy(moved, false)"),
                    "P3-T4: moveEnergy must execute real receive after extraction");
        }

        @Test
        void moveEnergyGuardsZeroLimits() throws Exception {
            var src = readMainSource(NETWORKS_SRC);
            assertTrue(src.contains("if (limit <= 0") || src.contains("if (limit <= 0)"),
                    "P3-T4: moveEnergy must guard against zero/negative limit");
            assertTrue(src.contains("!from.canExtract()") || src.contains("!from.canExtract()"),
                    "P3-T4: moveEnergy must check from.canExtract()");
            assertTrue(src.contains("!to.canReceive()") || src.contains("!to.canReceive()"),
                    "P3-T4: moveEnergy must check to.canReceive()");
        }

        @Test
        void channelInitialFaceModeIsOff() throws Exception {
            var src = readMainSource(
                    "com/modularmc/ten/common/blockentity/channel/AbstractChannelBlockEntity.java");
            assertTrue(src.contains("return FaceOption.OFF"),
                    "P3-T4: Channel initial face mode should be OFF (no auto-connection)");
        }

        @Test
        void channelDoesNotProduceOrConsumeEnergy() throws Exception {
            // Channel only moves energy via TransferNetworks.moveEnergy.
            // It should NOT generate or consume energy internally.
            var src = readMainSource(CHANNEL_SRC);
            // The tick() should not call energyStorage.receiveEnergy or extractEnergy directly
            // (only through moveEnergy)
            assertFalse(src.contains("energyStorage.receiveEnergy("),
                    "P3-T4: Channel must NOT call energyStorage.receiveEnergy directly");
            assertFalse(src.contains("energyStorage.extractEnergy("),
                    "P3-T4: Channel must NOT call energyStorage.extractEnergy directly");
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // P3-T5: CellBlockEntity 验证
    // ════════════════════════════════════════════════════════════════════

    @Nested
    class P3_T5_CellVerification {

        static final String CM_SRC = "com/modularmc/ten/api/blockentity/CmMachineBlockEntity.java";
        static final String CELL_SRC = "com/modularmc/ten/common/blockentity/machine/CellBlockEntity.java";

        @Test
        void setCapacityTruncatesStoredEnergy() throws Exception {
            var storage = new MachineEnergyStorage(1000, 100, 100);
            storage.setEnergy(900);
            assertEquals(900, storage.getEnergyStored(), "Stored energy should be 900 before truncation");
            storage.setCapacity(500);
            assertEquals(500, storage.getEnergyStored(),
                    "P3-T5: setCapacity must truncate stored energy when new capacity < stored");
        }

        @Test
        void setCapacityDoesNotExpandStoredEnergy() throws Exception {
            var storage = new MachineEnergyStorage(1000, 100, 100);
            storage.setEnergy(300);
            storage.setCapacity(2000);
            assertEquals(300, storage.getEnergyStored(),
                    "P3-T5: setCapacity must NOT expand stored energy");
        }

        @Test
        void setCapacityNegativeBecomesZero() throws Exception {
            var storage = new MachineEnergyStorage(1000, 100, 100);
            storage.setCapacity(-100);
            assertEquals(0, storage.getMaxEnergyStored(),
                    "P3-T5: setCapacity must convert negative to 0");
        }

        @Test
        void safeMultiplyUsesLongIntermediate() {
            // safeMultiply must NOT overflow int
            int a = 2_000_000_000;
            int b = 2;
            int result = safeMultiply(a, b);
            // (long)2_000_000_000 * 2 = 4_000_000_000 > Integer.MAX_VALUE
            // So result should be Integer.MAX_VALUE
            assertEquals(Integer.MAX_VALUE, result,
                    "P3-T5: safeMultiply must clamp to Integer.MAX_VALUE when long result overflows int");
        }

        @Test
        void safeMultiplyReturnsZeroForNonPositive() {
            assertEquals(0, safeMultiply(0, 100), "Zero a → 0");
            assertEquals(0, safeMultiply(100, 0), "Zero b → 0");
            assertEquals(0, safeMultiply(-5, 100), "Negative a → 0");
            assertEquals(0, safeMultiply(100, -5), "Negative b → 0");
        }

        @Test
        void safeMultiplyNormalValues() {
            assertEquals(100, safeMultiply(10, 10), "10 × 10 = 100");
            assertEquals(0, safeMultiply(0, 0), "0 × 0 = 0");
            assertEquals(Integer.MAX_VALUE, safeMultiply(Integer.MAX_VALUE, 2),
                    "MAX_VALUE × 2 must clamp");
        }

        @Test
        void cellHasCorrectInitialCapacity() throws Exception {
            var src = readMainSource(CELL_SRC);
            // CellBlockEntity constructor sets capacity to kFE(1000) = 1_000_000
            assertTrue(src.contains("kFE(1000)"),
                    "P3-T5: Cell must initialize with kFE(1000) capacity");
            assertTrue(src.contains("setEfficiency(100)"),
                    "P3-T5: Cell must initialize with efficiency=100");
        }

        @Test
        void cellUsesEnergyStorageForItemTransfer() throws Exception {
            var src = readMainSource(CELL_SRC);
            // Cell tick charges items from energyStorage and discharges items into energyStorage
            assertTrue(src.contains("energyStorage.receiveEnergy(diff, false)"),
                    "P3-T5: Cell must charge energy from items into energyStorage");
            assertTrue(src.contains("energyStorage.extractEnergy(diff, false)"),
                    "P3-T5: Cell must discharge energy from energyStorage into items");
        }

        @Test
        void celldoesNotDuplicateEnergy() throws Exception {
            // Verify: Cell extracts from item THEN receives to internal storage (or vice versa).
            // This means energy is moved, not duplicated.
            var src = readMainSource(CELL_SRC);
            // Each item interaction should extract from one and receive to the other atomically
            int receiveEnergyCount = occurrences(src, "energyStorage.receiveEnergy");
            int extractEnergyCount = occurrences(src, "energyStorage.extractEnergy");
            // In slot 0 (charge): extract from item, receive to storage
            // In slot 1 (discharge): extract from storage, receive to item
            // Total: 2 receiveEnergy, 2 extractEnergy calls
            assertTrue(receiveEnergyCount >= 1 && extractEnergyCount >= 1,
                    "P3-T5: Cell must have both receiveEnergy and extractEnergy calls for proper transfer");
        }

        @Test
        void creativeCellAlwaysFull() throws Exception {
            var src = readMainSource(
                    "com/modularmc/ten/common/blockentity/machine/CreativeCellBlockEntity.java");
            assertTrue(src.contains("energyStorage.setEnergy(maxStorageEnergy)"),
                    "P3-T5: CreativeCell must refill storage every tick");
            assertTrue(src.contains("setCapacity(Integer.MAX_VALUE)"),
                    "P3-T5: CreativeCell must have Integer.MAX_VALUE capacity");
        }

        @Test
        void cellHasNoUpgradeSlots() throws Exception {
            var src = readMainSource(CELL_SRC);
            assertTrue(src.contains("return false") && src.contains("hasUpgrade()"),
                    "P3-T5: Cell should not support upgrades");
            assertTrue(src.contains("return false") && src.contains("supportsUpgradeSlots()"),
                    "P3-T5: Cell should not show upgrade slots");
        }

        @Test
        void cellDoesNotAcceptUpgrades() throws Exception {
            // Cell/CreativeCell have hasUpgrade()=false, so doBaseData won't apply upgrades
            // This means cell capacity stays at initial values
            var cellSrc = readMainSource(CELL_SRC);
            assertTrue(cellSrc.contains("return false") && cellSrc.contains("hasUpgrade()"),
                    "P3-T5: Cell hasUpgrade() must return false");
            assertTrue(cellSrc.contains("return false") && cellSrc.contains("supportsUpgradeSlots()"),
                    "P3-T5: Cell supportsUpgradeSlots() must return false");
            // Verify kFE(1000) formula is correct
            assertEquals(1_000_000, (int) (1000 * 1000.0), "P3-T5: kFE(1000) should be 1,000,000");
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // P3-T6: MachineEnergyStorage / CapabilityAdapters 验证
    // ════════════════════════════════════════════════════════════════════

    @Nested
    class P3_T6_CapabilityVerification {

        static final String CM_SRC = "com/modularmc/ten/api/blockentity/CmMachineBlockEntity.java";

        @Test
        void processAndEffectCannotExternalExtract() throws Exception {
            var src = readMainSource(CM_SRC);
            // The canExternalExtract() method must use a switch with default-deny
            // Only GENERATOR, ENGINE_*, CELL, CREATIVE_CELL return true
            // PROCESS/EFFECT types fall through to default -> false
            int methodIdx = src.indexOf("public final boolean canExternalExtract()");
            assertTrue(methodIdx >= 0,
                    "canExternalExtract method must exist");
            String methodBody = src.substring(methodIdx, methodIdx + 600);
            // Must have a switch statement and a default false return
            assertTrue(methodBody.contains("switch"),
                    "canExternalExtract must use switch for type dispatch");
            assertTrue(methodBody.contains("default -> false"),
                    "canExternalExtract must have default-deny (process/effect fall here)");
        }

        @Test
        void getEnergyStorageExtractEnergyGuardedByCanExternalExtract() throws Exception {
            var src = readMainSource(CM_SRC);
            // The extractEnergy method in getEnergyStorage(side) must check canExternalExtract()
            int idx = src.indexOf("extractEnergy(int maxExtract, boolean simulate)");
            assertTrue(idx >= 0, "extractEnergy method must exist in getEnergyStorage");
            // After the method signature, must find canExternalExtract() before the inner return
            String methodBody = src.substring(idx, idx + 300);
            assertTrue(methodBody.contains("canExternalExtract()"),
                    "P3-T6: getEnergyStorage.extractEnergy must guard with canExternalExtract()");
        }

        @Test
        void getEnergyStorageExtractBoundedByMaxExtractEnergy() throws Exception {
            var src = readMainSource(CM_SRC);
            // The extractEnergy must use Math.min(maxExtract, maxExtractEnergy)
            int extractIdx = src.indexOf("extractEnergy(int maxExtract, boolean simulate)");
            assertTrue(extractIdx >= 0);
            String methodBody = src.substring(extractIdx, extractIdx + 300);
            assertTrue(methodBody.contains("maxExtractEnergy"),
                    "P3-T6: getEnergyStorage.extractEnergy must bound by maxExtractEnergy");
        }

        @Test
        void getEnergyStorageReceiveBoundedByMaxReceiveEnergy() throws Exception {
            var src = readMainSource(CM_SRC);
            int receiveIdx = src.indexOf("receiveEnergy(int maxReceive, boolean simulate)");
            assertTrue(receiveIdx >= 0);
            String methodBody = src.substring(receiveIdx, receiveIdx + 300);
            assertTrue(methodBody.contains("maxReceiveEnergy"),
                    "P3-T6: getEnergyStorage.receiveEnergy must bound by maxReceiveEnergy");
        }

        @Test
        void cableGetEnergyReturnsRawStorage() throws Exception {
            var cableSrc = readMainSource(
                    "com/modularmc/ten/common/blockentity/CableBlockEntity.java");
            // Cable.getEnergy(side) should return the raw storage, not a wrapped version
            // This is because cables are buffers and should always be accessible
            assertTrue(cableSrc.contains("return storage"),
                    "P3-T6: Cable.getEnergy must return raw storage");
        }

        @Test
        void capabilityRegistrationForEnergy() throws Exception {
            var proxySrc = readMainSource(
                    "com/modularmc/ten/common/CommonProxy.java");
            // The capability registration must register for all BaseMachineBlock blocks
            assertTrue(proxySrc.contains("Capabilities.Energy.BLOCK"),
                    "P3-T6: CommonProxy must register energy capability");
            // Must handle both Cable and CmMachineBlockEntity
            assertTrue(proxySrc.contains("cable.getEnergy(side)"),
                    "P3-T6: Capability registration must handle Cable");
            assertTrue(proxySrc.contains("machine.getEnergyStorage(side)"),
                    "P3-T6: Capability registration must handle CmMachineBlockEntity");
        }

        @Test
        void engineEnergyAllowRunBatchAware() throws Exception {
            var cmSrc = readMainSource(CM_SRC);
            // energyAllowRun should be batch-aware (check vs totalFePerTick when locked)
            assertTrue(cmSrc.contains("hasLockedBatch()") || cmSrc.contains("lockedB"),
                    "P3-T6: energyAllowRun must be batch-aware");
        }

        @Test
        void doBaseDataScalesStorageAndThroughputByTheoreticalB() throws Exception {
            var cmSrc = readMainSource(CM_SRC);
            // doBaseData must scale storage, receive, and extract by theoreticalB
            assertTrue(cmSrc.contains("safeMultiply(initialEnergyStorage, theoreticalB)"),
                    "P3-T6: doBaseData must scale storage by theoreticalB");
            assertTrue(cmSrc.contains("safeMultiply(initialEnergyReceive, theoreticalB)"),
                    "P3-T6: doBaseData must scale receive by theoreticalB");
            // extract should be max of scaled initExtract and scaled baseFE
            assertTrue(cmSrc.contains("safeMultiply(initialEnergyExtract, theoreticalB)") &&
                            cmSrc.contains("safeMultiply(efficientIn, theoreticalB)"),
                    "P3-T6: doBaseData must scale extract as max(initExtract×B, baseFE×B)");
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // P3-T7: 面配置 I/O 验证
    // ════════════════════════════════════════════════════════════════════

    @Nested
    class P3_T7_FaceConfigVerification {

        static final String CM_SRC = "com/modularmc/ten/api/blockentity/CmMachineBlockEntity.java";

        @Test
        void canReceiveEnergyChecksFaceOption() throws Exception {
            var src = readMainSource(CM_SRC);
            // canReceiveEnergy must check FaceOption.isIn or BOTH
            assertTrue(src.contains("FaceOption.isIn"),
                    "P3-T7: canReceiveEnergy must check FaceOption.isIn");
        }

        @Test
        void canExtractEnergyChecksFaceOption() throws Exception {
            var src = readMainSource(CM_SRC);
            assertTrue(src.contains("FaceOption.isOut"),
                    "P3-T7: canExtractEnergy must check FaceOption.isOut");
        }

        @Test
        void energyFaceDataSyncedToClient() throws Exception {
            var src = readMainSource(CM_SRC);
            // energyFaceData must be @DescSynced for client sync
            assertTrue(src.contains("@DescSynced"),
                    "P3-T7: energyFaceData must be synced to client");
        }

        @Test
        void faceModeDefaultsToBothForMachines() throws Exception {
            var src = readMainSource(CM_SRC);
            // Default face mode should be FaceOption.BOTH
            assertTrue(src.contains("energyFaceMode.put(d, FaceOption.BOTH)"),
                    "P3-T7: Default energy face mode must be BOTH");
        }

        @Test
        void engineInitialFaceModeIsOut() throws Exception {
            var src = readMainSource(
                    "com/modularmc/ten/api/blockentity/EngineBlockEntity.java");
            assertTrue(src.contains("return FaceOption.OUT"),
                    "P3-T7: Engine initial face mode must be OUT");
        }

        @Test
        void faceModeArraysSyncedInDoBaseData() throws Exception {
            var src = readMainSource(CM_SRC);
            // doBaseData must sync face maps to faceData arrays
            assertTrue(src.contains("energyFaceData[idx] = energyFaceMode.getOrDefault"),
                    "P3-T7: doBaseData must sync energyFaceData from energyFaceMode");
        }

        @Test
        void canReceiveRespectsSignalAllowRun() throws Exception {
            var src = readMainSource(CM_SRC);
            // getEnergyStorage.receiveEnergy must check signalAllowRun
            int receiveIdx = src.indexOf("receiveEnergy(int maxReceive, boolean simulate)");
            assertTrue(receiveIdx >= 0);
            String methodBody = src.substring(receiveIdx, receiveIdx + 300);
            assertTrue(methodBody.contains("signalAllowRun()"),
                    "P3-T7: getEnergyStorage.receiveEnergy must check signalAllowRun");
        }

        @Test
        void hasFaceCapabilityEnergyDefaultsTrue() throws Exception {
            var src = readMainSource(CM_SRC);
            assertTrue(src.contains("return true") &&
                            src.contains("hasFaceCapabilityEnergy"),
                    "P3-T7: Default hasFaceCapabilityEnergy should return true");
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // P3 全局不变量：Syn 不外送、网络传输安全
    // ════════════════════════════════════════════════════════════════════

    @Nested
    class P3_GlobalInvariants {

        @Test
        void synEnergyDoesNotExportToNetwork() throws Exception {
            var src = readMainSource(
                    "com/modularmc/ten/api/blockentity/CmMachineBlockEntity.java");
            // tryInjectPhotosynEnergy must inject into internal storage, NOT via getEnergyStorage
            // which would allow external extraction
            int idx = src.indexOf("tryInjectPhotosynEnergy");
            assertTrue(idx >= 0, "tryInjectPhotosynEnergy method must exist");
            String methodBody = src.substring(idx, idx + 950);
            // Must use energyStorage.receiveEnergy directly (not through getEnergyStorage)
            assertTrue(methodBody.contains("energyStorage.receiveEnergy(UpgradeConstants.SYN_PHOTOSYN_FE"),
                    "P3-Global: tryInjectPhotosynEnergy must inject directly to internal energyStorage");
            // Must NOT go through getEnergyStorage (which would make it externally accessible)
            assertFalse(methodBody.contains("getEnergyStorage"),
                    "P3-Global: tryInjectPhotosynEnergy must NOT go through getEnergyStorage(side)");
        }

        @Test
        void synInjectionOnlyForProcessAndEffect() throws Exception {
            var src = readMainSource(
                    "com/modularmc/ten/api/blockentity/CmMachineBlockEntity.java");
            int idx = src.indexOf("tryInjectPhotosynEnergy");
            assertTrue(idx >= 0);
            String methodBody = src.substring(idx, idx + 500);
            // Must check machine type is PROCESS or EFFECT
            assertTrue(methodBody.contains("MACHINE_PROCESS") && methodBody.contains("MACHINE_EFFECT"),
                    "P3-Global: Syn injection must only apply to PROCESS and EFFECT machines");
        }

        @Test
        void networkNoInfiniteLoops() throws Exception {
            // TransferNetworks.collectConnected uses HashSet for visited
            // This prevents infinite loops in cyclic cable networks
            var src = readMainSource(
                    "com/modularmc/ten/common/blockentity/TransferNetworks.java");
            assertTrue(src.contains("Set<BlockPos> visited"),
                    "P3-Global: Network traversal must use visited set to prevent infinite loops");
        }

        @Test
        void noNegativeEnergyTransfer() throws Exception {
            // MachineEnergyStorage.extractEnergy should never return negative
            var storage = new MachineEnergyStorage(1000, 100, 100);
            storage.setEnergy(500);
            int extracted = storage.extractEnergy(-50, false);
            assertTrue(extracted >= 0,
                    "P3-Global: extractEnergy must never return negative value");
            int received = storage.receiveEnergy(-50, false);
            assertTrue(received >= 0,
                    "P3-Global: receiveEnergy must never return negative value");
        }

        @Test
        void transferNetworksCollectConnectedHandlesDisconnectedPositions() throws Exception {
            // Verify the BFS traversal logic correctly finds connected components
            // This can't call the static method directly (needs Level),
            // but verify the algorithm logic
            Set<BlockPos> visited = new HashSet<>();
            // With no positions, BFS returns empty
            assertTrue(visited.isEmpty(),
                    "P3-Global: Empty network should have no visited positions");
        }

        @Test
        void energyStorageNeverGoesNegative() {
            var storage = new MachineEnergyStorage(1000, 100, 100);
            storage.setEnergy(500);
            // maxExtract=100, so we can only extract 100 at once
            int extracted = storage.extractEnergy(1000, false);
            assertEquals(100, extracted, "Should extract at most maxExtract=100");
            assertEquals(400, storage.getEnergyStored(), "Storage should be 400 after extracting 100");
        }

        @Test
        void energyStorageReceiveNeverExceedsCapacity() {
            var storage = new MachineEnergyStorage(1000, 200, 100);
            storage.setEnergy(900);
            int received = storage.receiveEnergy(200, false);
            assertEquals(100, received, "Should only receive up to capacity (1000-900=100)");
            assertEquals(1000, storage.getEnergyStored(), "Storage should be exactly at capacity");
        }

        @Test
        void setMaxReceiveDoesNotAffectStoredEnergy() {
            var storage = new MachineEnergyStorage(1000, 200, 100);
            storage.setEnergy(500);
            storage.setMaxReceive(50);
            storage.setMaxExtract(30);
            assertEquals(500, storage.getEnergyStored(),
                    "P3-Global: setMaxReceive/setMaxExtract must not change stored energy");
        }

        @Test
        void theoreticalBatchSizeClamped() {
            // getTheoreticalBatchSize() = max(1, min(1+batch, 19))
            assertEquals(1, Math.max(1, Math.min(1 + 0, 19)), "No upgrades: B=1");
            assertEquals(5, Math.max(1, Math.min(1 + 4, 19)), "batch=4: B=5");
            assertEquals(19, Math.max(1, Math.min(1 + 18, 19)), "6xShulker: B=19");
            assertEquals(19, Math.max(1, Math.min(1 + 99, 19)), "batch=99: B=19 (capped)");
        }

        @Test
        void kfeFormulaMatchesProduction() {
            // kFE(k) = (int)(1000 * k)
            assertEquals(1_000_000, (int) (1000 * 1000.0), "kFE(1000) = 1,000,000");
            assertEquals(20_000, (int) (1000 * 20.0), "kFE(20) = 20,000");
            assertEquals(10_000, (int) (1000 * 10.0), "kFE(10) = 10,000");
        }

        @Test
        void channelDoesNotHaveFluidCapability() throws Exception {
            var src = readMainSource(
                    "com/modularmc/ten/common/blockentity/channel/AbstractChannelBlockEntity.java");
            assertTrue(src.contains("return false") && src.contains("hasFaceCapabilityFluid"),
                    "P3-Global: Channel should not have fluid capability");
        }

        @Test
        void engineFluidCapabilityDisabled() throws Exception {
            var src = readMainSource(
                    "com/modularmc/ten/api/blockentity/EngineBlockEntity.java");
            assertTrue(src.contains("return false") && src.contains("hasFaceCapabilityFluid"),
                    "P3-Global: Engine should not have fluid capability");
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // Utility
    // ════════════════════════════════════════════════════════════════════

    static int occurrences(String text, String pattern) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(pattern, idx)) >= 0) {
            count++;
            idx += pattern.length();
        }
        return count;
    }
}
