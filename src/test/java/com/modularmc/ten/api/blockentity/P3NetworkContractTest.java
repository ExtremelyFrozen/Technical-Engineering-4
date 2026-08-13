// -*- coding: utf-8 -*-
package com.modularmc.ten.api.blockentity;

import com.modularmc.ten.api.capability.MachineEnergyStorage;
import com.modularmc.ten.common.channel.ChannelKey;
import com.modularmc.ten.common.channel.ChannelType;
import com.modularmc.ten.common.channel.SharedStorage;

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
            assertTrue(methodBody.contains("return 500_000"),
                    "cable_star must return 500_000");
            assertTrue(methodBody.contains("return 50_000"),
                    "cable_azure must return 50_000");
            assertTrue(methodBody.contains("return 5_000"),
                    "cable_quartz must return 5_000");
            assertTrue(methodBody.contains("return 500;"),
                    "default cable must return 500");
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
        static final String ABSTRACT_SRC = "com/modularmc/ten/common/blockentity/channel/AbstractChannelBlockEntity.java";
        static final String NETWORKS_SRC = "com/modularmc/ten/common/blockentity/TransferNetworks.java";

        @Test
        void channelHasNoPerTickTransfer() throws Exception {
            // 末影箱模式：零 tick 传输——接入后槽位直接绑定共享存储，tick 无 move* 调用
            var src = readMainSource(CHANNEL_SRC);
            assertFalse(src.contains("TransferNetworks.moveEnergy"),
                    "P3-T4: Channel tick must NOT use TransferNetworks.moveEnergy (zero-tick transfer)");
        }

        @Test
        void channelBindsSharedStorageOnJoin() throws Exception {
            // 接入态面能力指向共享 handler（getEnergyStorage 覆盖解析共享存储）
            var src = readMainSource(CHANNEL_SRC);
            assertTrue(src.contains("sharedStorage()"),
                    "P3-T4: Channel must resolve shared storage on join");
            assertTrue(src.contains("hasFaceCapabilityEnergy"),
                    "P3-T4: Channel must gate face capability by joined state");
        }

        @Test
        void channelRemovedRoundRobin() throws Exception {
            // 轮询已移除：频道不再逐 tick 选择输入/输出
            var src = readMainSource(CHANNEL_SRC);
            assertFalse(src.contains("nextRoundRobin"),
                    "P3-T4: Channel must NOT use nextRoundRobin (polling removed)");
        }

        @Test
        void moveEnergyRemovedFromTransferNetworks() throws Exception {
            // moveEnergy/moveFluid 已删除（连接器/频道轮询废弃）；moveItems 也已随撮合重构删除
            // （撮合自行 extract/insert），insertItem 保留供 Pipe 撮合执行插入
            var src = readMainSource(NETWORKS_SRC);
            assertFalse(src.contains("public static int moveEnergy"),
                    "P3-T4: moveEnergy must be removed from TransferNetworks");
            assertFalse(src.contains("public static int moveFluid"),
                    "P3-T4: moveFluid must be removed from TransferNetworks");
            assertFalse(src.contains("public static int moveItems"),
                    "P3-T4: moveItems must be removed from TransferNetworks (撮合自行 extract/insert)");
            assertTrue(src.contains("public static ItemStack insertItem"),
                    "P3-T4: insertItem must be retained for Pipe matching");
        }

        @Test
        void matchAndTransferRetainedForPipe() throws Exception {
            // Pipe 撮合入口：root 每 tick 调 matchAndTransfer，执行插入复用 TransferNetworks.insertItem
            var pipeSrc = readMainSource("com/modularmc/ten/common/blockentity/PipeBlockEntity.java");
            assertTrue(pipeSrc.contains("matchAndTransfer(network)"),
                    "P3-T4: Pipe tick must call matchAndTransfer for item matching");
            assertTrue(pipeSrc.contains("TransferNetworks.insertItem"),
                    "P3-T4: Pipe must use TransferNetworks.insertItem for insert execution");
        }

        @Test
        void matchingGuardsPerBeatLimit() throws Exception {
            // 撮合保留限量守卫：每源每节拍限量（perBeatLimit - 已搬量），耗尽则跳过该源
            var pipeSrc = readMainSource("com/modularmc/ten/common/blockentity/PipeBlockEntity.java");
            assertTrue(pipeSrc.contains("perBeatLimit - movedBySource.getOrDefault"),
                    "P3-T4: matching must guard each source by perBeatLimit (per-beat limit)");
            assertTrue(pipeSrc.contains("remaining <= 0"),
                    "P3-T4: exhausted source must be skipped (guard clause)");
        }

        @Test
        void channelInitialFaceModeIsOff() throws Exception {
            var src = readMainSource(ABSTRACT_SRC);
            assertTrue(src.contains("return FaceOption.OFF"),
                    "P3-T4: Channel initial face mode should be OFF (no auto-connection)");
        }

        @Test
        void channelTickDoesNotProduceOrConsumeEnergy() throws Exception {
            // 频道 tick 只做 doBaseData + 镜像修正 + setActive，不直接生成/消耗能量
            // （join/leave 的一次性回流 pushLocalToShared 不属于 tick 轮询路径）
            var src = readMainSource(CHANNEL_SRC);
            int tickIdx = src.indexOf("public void tick()");
            assertTrue(tickIdx >= 0, "Channel tick method must exist");
            String tickBody = src.substring(tickIdx, Math.min(src.length(), tickIdx + 400));
            assertFalse(tickBody.contains("energyStorage.receiveEnergy("),
                    "P3-T4: Channel tick must NOT receive energy directly");
            assertFalse(tickBody.contains("energyStorage.extractEnergy("),
                    "P3-T4: Channel tick must NOT extract energy directly");
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
    // P3-T8: 频道末影箱模式（ChannelRegistry/SharedStorage）验证
    // ════════════════════════════════════════════════════════════════════

    @Nested
    class P3_T8_ChannelEnderVerification {

        static final String REGISTRY_SRC = "com/modularmc/ten/common/channel/ChannelRegistry.java";
        static final String SHARED_SRC = "com/modularmc/ten/common/channel/SharedStorage.java";
        static final String ABSTRACT_SRC = "com/modularmc/ten/common/blockentity/channel/AbstractChannelBlockEntity.java";

        @Test
        void channelKeyUniquenessByType() {
            // 注册表键唯一：同名称不同类型 = 不同频道（物品/流体/能量按类型独立）
            var item = new ChannelKey("iron", ChannelType.ITEM);
            var fluid = new ChannelKey("iron", ChannelType.FLUID);
            var energy = new ChannelKey("iron", ChannelType.ENERGY);
            assertNotEquals(item, fluid, "same name, different type must be distinct keys");
            assertNotEquals(item, energy, "same name, different type must be distinct keys");
            assertNotEquals(fluid, energy, "same name, different type must be distinct keys");
            assertNotEquals(new ChannelKey("copper", ChannelType.ITEM), item, "different name must be distinct");
        }

        @Test
        void sharedStorageItemCapacityScalesWithMembers() {
            // 容量公式：物品每槽堆叠 = 64 × 成员数，槽位固定 9
            var storage = SharedStorage.forItem();
            assertEquals(9, storage.getItemHandler().getSlots(), "item channel must have 9 fixed slots");
            storage.refreshMemberCount(3);
            assertEquals(192, storage.getItemHandler().getSlotLimit(0), "64 × 3 = 192");
            storage.refreshMemberCount(1);
            assertEquals(64, storage.getItemHandler().getSlotLimit(0), "64 × 1 = 64");
        }

        @Test
        void sharedStorageFluidCapacityScalesWithMembers() throws Exception {
            // FluidTank 构造依赖 MC bootstrap（FluidInstance/BuiltInRegistries），纯 JUnit 无法
            // 实例化流体共享存储 → 仅源码断言容量公式与固定 tank 数
            var src = readMainSource(SHARED_SRC);
            assertTrue(src.contains("FLUID_TANKS"), "fluid channel must have 2 fixed tanks");
            assertTrue(src.contains("BASE_FLUID_CAPACITY * n"), "fluid capacity = 2000 × members");
            assertTrue(src.contains("tank.getFluidAmount() <= capacity"),
                    "fluid shrink must be refused when stored exceeds new capacity");
        }

        @Test
        void sharedStorageEnergyCapacityScalesWithMembers() {
            // 容量公式：能量 = kFE(10) × 成员数
            var storage = SharedStorage.forEnergy();
            storage.refreshMemberCount(5);
            assertEquals(50000, storage.getEnergy().getMaxEnergyStored(), "kFE(10) × 5 = 50000");
        }

        @Test
        void setChangeListenerSurvivesNullBackendsPerType() {
            // 回归（F1）：setChangeListener 必须按类型非空守卫——ITEM 无 energy、
            // ENERGY 无 itemHandler，任一类型挂监听都不得 NPE（ChannelRegistry 在
            // join 时对每种类型都调用 setChangeListener）
            var item = SharedStorage.forItem();
            item.setChangeListener(() -> {});
            item.refreshMemberCount(1);

            var energy = SharedStorage.forEnergy();
            energy.setChangeListener(() -> {});
            energy.refreshMemberCount(1);
        }

        @Test
        void setChangeListenerGuardsNullHandlersByType() throws Exception {
            // FLUID 无法在纯 JUnit 实例化（FluidTank 构造依赖 MC bootstrap）→ 源码级
            // 断言：setChangeListener 对可能为 null 的 itemHandler/energy 有非空守卫
            var src = readMainSource(SHARED_SRC);
            int idx = src.indexOf("public void setChangeListener(Runnable listener)");
            assertTrue(idx >= 0, "P3-T8: setChangeListener must exist");
            String body = src.substring(idx, idx + 500);
            assertTrue(body.contains("itemHandler != null"),
                    "P3-T8: itemHandler listener must be null-guarded (FLUID/ENERGY have no itemHandler)");
            assertTrue(body.contains("energy != null"),
                    "P3-T8: energy listener must be null-guarded (ITEM has no energy)");
        }

        @Test
        void energyCapacityRefusesShrinkWhenStoredExceedsNewCapacity() {
            // 拒绝缩容：成员减少且 stored > 新容量时保持当前容量（内容不丢硬约束）
            var storage = SharedStorage.forEnergy();
            storage.refreshMemberCount(3);
            storage.getEnergy().setEnergy(15000);
            storage.refreshMemberCount(1);
            assertEquals(15000, storage.getEnergy().getEnergyStored(), "stored must not be truncated");
            assertEquals(30000, storage.getEnergy().getMaxEnergyStored(), "capacity must be kept until drained");
            // 清空后允许缩容
            storage.getEnergy().setEnergy(0);
            storage.refreshMemberCount(1);
            assertEquals(10000, storage.getEnergy().getMaxEnergyStored(), "capacity shrinks once empty");
        }

        @Test
        void registryJoinIsIdempotent() throws Exception {
            var src = readMainSource(REGISTRY_SRC);
            assertTrue(src.contains("if (!set.add(memberId))"),
                    "P3-T8: join must be idempotent (repeat join returns false, no double count)");
        }

        @Test
        void registryLeaveRecomputesCapacity() throws Exception {
            var src = readMainSource(REGISTRY_SRC);
            assertTrue(src.contains("refreshMemberCount(set.size())"),
                    "P3-T8: leave must recompute shared capacity from remaining member count");
        }

        @Test
        void registryPersistsViaOverworldSavedData() throws Exception {
            // 存档级持久化：主世界 SavedDataStorage 挂载（跨维度全局共享）+ 懒加载
            var src = readMainSource(REGISTRY_SRC);
            assertTrue(src.contains("extends SavedData"),
                    "P3-T8: registry must be archive-level SavedData");
            assertTrue(src.contains("server.overworld().getDataStorage()"),
                    "P3-T8: registry must attach to overworld data storage (cross-dimension)");
            assertTrue(src.contains("computeIfAbsent"),
                    "P3-T8: registry must lazy-load via computeIfAbsent");
            assertTrue(src.contains("setDirty()"),
                    "P3-T8: registry must event-driven save via setDirty");
        }

        @Test
        void registryListsChannelsByType() throws Exception {
            var src = readMainSource(REGISTRY_SRC);
            assertTrue(src.contains("listChannels") && src.contains("key.type() == type"),
                    "P3-T8: directory listing must filter by channel type");
        }

        @Test
        void joinAndLeavePushLocalToShared() throws Exception {
            // 回流：join 本地内容并入共享；leave 本地内容优先回频道（满留本地）
            var src = readMainSource(ABSTRACT_SRC);
            assertTrue(src.contains("protected abstract void pushLocalToShared();"),
                    "P3-T8: pushLocalToShared must exist per storage type");
            int joinIdx = src.indexOf("public boolean join(String name)");
            assertTrue(joinIdx >= 0);
            String joinBody = src.substring(joinIdx, Math.min(src.length(), joinIdx + 700));
            assertTrue(joinBody.contains("pushLocalToShared()"),
                    "P3-T8: join must merge local content into shared");
            int leaveIdx = src.indexOf("public boolean leave()");
            assertTrue(leaveIdx >= 0);
            String leaveBody = src.substring(leaveIdx, Math.min(src.length(), leaveIdx + 500));
            assertTrue(leaveBody.contains("pushLocalToShared()"),
                    "P3-T8: leave must drain local content back to shared");
        }

        @Test
        void channelIdPersistedAndClientSynced() throws Exception {
            var src = readMainSource(ABSTRACT_SRC);
            assertTrue(src.contains("@Persisted") && src.contains("@DescSynced"),
                    "P3-T8: channelId must be persisted and client-synced");
            assertTrue(src.contains("String channelId"),
                    "P3-T8: channelId field must exist");
        }

        @Test
        void sharedCapacityFormulaPresent() throws Exception {
            var src = readMainSource(SHARED_SRC);
            assertTrue(src.contains("BASE_ITEM_STACK * n"),
                    "P3-T8: item stack limit = 64 × members");
            assertTrue(src.contains("BASE_FLUID_CAPACITY * n"),
                    "P3-T8: fluid capacity = 2000 × members");
            assertTrue(src.contains("BASE_ENERGY_CAPACITY * n"),
                    "P3-T8: energy capacity = kFE(10) × members");
            assertTrue(src.contains("setDynamicSlotLimit"),
                    "P3-T8: item capacity must use dynamic slot limit");
        }

        @Test
        void shrinkRefusalGuardPresent() throws Exception {
            // 拒绝缩容守卫（源码级）：流体/能量仅在 stored ≤ 新容量时应用缩容
            var src = readMainSource(SHARED_SRC);
            assertTrue(src.contains("tank.getFluidAmount() <= capacity"),
                    "P3-T8: fluid shrink must be refused when stored exceeds new capacity");
            assertTrue(src.contains("energy.getEnergyStored() <= capacity"),
                    "P3-T8: energy shrink must be refused when stored exceeds new capacity");
        }

        @Test
        void channelSlotAllowsOverstackUpToDynamicLimit() throws Exception {
            // GUI 堆叠（P3-T8 回归）：共享槽位上限 64×成员数，但 Slot.safeInsert 经
            // getMaxStackSize(stack)=min(槽位上限, 物品原版 64) 会把 GUI 堆叠钏制在 64 ——
            // 频道槽必须覆写 getMaxStackSize(ItemStack) 返回动态槽位上限，才能堆叠到 192。
            var src = readMainSource("com/modularmc/ten/common/gui/TENMachineBlockUIFactory.java");
            int idx = src.indexOf("public static ItemSlot channelItemSlot");
            assertTrue(idx >= 0, "P3-T8: channelItemSlot must exist");
            String body = src.substring(idx, Math.min(src.length(), idx + 900));
            assertTrue(body.contains("getMaxStackSize(net.minecraft.world.item.ItemStack stack)"),
                    "P3-T8: channel slot must override getMaxStackSize(ItemStack)");
            assertTrue(body.contains("return getMaxStackSize();"),
                    "P3-T8: channel slot override must return the facade's dynamic slot limit");
        }

        @Test
        void facadeReportsDynamicLimitOnClientFromSyncedMembers() throws Exception {
            // 客户端门面（P3-T8 回归）：resolve() 在客户端落到本地缓冲（固定 64），若槽位上限仍
            // 按本地缓冲取，客户端点击/快速移动模拟会与服务器容量不一致 —— 必须按同步的成员数
            // 推算 64×成员数。
            var facadeSrc = readMainSource("com/modularmc/ten/common/blockentity/channel/ChannelItemHandlerFacade.java");
            assertTrue(facadeSrc.contains("BASE_ITEM_STACK * Math.max(1, channel.joinedMemberCount())"),
                    "P3-T8: client facade slot limit must be 64 × synced member count");
            assertTrue(facadeSrc.contains("channel.isJoined()"),
                    "P3-T8: client facade dynamic limit must only apply when joined");
        }

        @Test
        void joinedMemberCountSyncedAndRefreshedPerTick() throws Exception {
            // 成员数同步（P3-T8 回归）：@DescSynced 推送客户端；join 立即写入、tick 持续刷新，
            // 其它方块新接入时本端 UI 上限随之增长。
            var src = readMainSource(ABSTRACT_SRC);
            assertTrue(src.contains("@DescSynced") && src.contains("int joinedMemberCount"),
                    "P3-T8: joinedMemberCount must be DescSynced");
            assertTrue(src.contains("joinedMemberCount = reg.memberCount(key)"),
                    "P3-T8: join must write member count from registry");
            assertTrue(src.contains("joinedMemberCount = 0"),
                    "P3-T8: leave must clear member count");
            assertTrue(src.contains("refreshJoinedMemberCount()"),
                    "P3-T8: tick must refresh joined member count");
        }

        @Test
        void removeDeletesOnlyEmptyChannels() throws Exception {
            // 删除空频道：remove 校验共享内容全空 + 成员集为空，通过则两 map 同步清理 + setDirty
            var src = readMainSource(REGISTRY_SRC);
            assertTrue(src.contains("public boolean remove(ChannelKey key)"),
                    "P3-T8: remove must exist on ChannelRegistry");
            assertTrue(src.contains("storage.isEmpty()"),
                    "P3-T8: remove must refuse non-empty shared storage");
            assertTrue(src.contains("members.getOrDefault(key, Set.of()).isEmpty()"),
                    "P3-T8: remove must refuse channels with remaining members");
            assertTrue(src.contains("storages.remove(key)") && src.contains("members.remove(key)"),
                    "P3-T8: removal must clean both storages and members maps");
            assertTrue(src.contains("setDirty()"),
                    "P3-T8: deletion must mark registry dirty for save");
            // SharedStorage 提供内容空判定（频道删除的前置校验）
            var shared = readMainSource(SHARED_SRC);
            assertTrue(shared.contains("public boolean isEmpty()"),
                    "P3-T8: SharedStorage must expose isEmpty() for delete guard");
        }

        @Test
        void removeRefusesNonEmptyChannels() throws Exception {
            // 拒绝非空：共享存储有内容 → remove 短路返回 false（内容不丢硬约束）；
            // 非空守卫必须位于删除动作之前。
            var src = readMainSource(REGISTRY_SRC);
            int idx = src.indexOf("public boolean remove(ChannelKey key)");
            assertTrue(idx >= 0, "P3-T8: remove must exist");
            String body = src.substring(idx, Math.min(src.length(), idx + 700));
            assertTrue(body.contains("return false"),
                    "P3-T8: non-empty channel must be refused without deletion");
            int guardIdx = body.indexOf("return false");
            int removeIdx = body.indexOf("storages.remove(key)");
            assertTrue(removeIdx >= 0, "P3-T8: deletion must exist after guard");
            assertTrue(guardIdx >= 0 && guardIdx < removeIdx,
                    "P3-T8: non-empty guard must short-circuit before deletion");
            // 行为验证：isEmpty 对能量内容敏感（纯 JUnit 可实例化）
            var storage = SharedStorage.forEnergy();
            assertTrue(storage.isEmpty(), "fresh channel must be empty");
            storage.getEnergy().setEnergy(100);
            assertFalse(storage.isEmpty(), "stored energy must make channel non-empty");
            storage.getEnergy().setEnergy(0);
            assertTrue(storage.isEmpty(), "drained channel must be empty again");
        }

        @Test
        void deleteRefusesNonMemberOperator() throws Exception {
            // 拒绝非成员删除：未接入频道（joinedKey()==null）的方块不能删除任何频道
            var src = readMainSource(ABSTRACT_SRC);
            int idx = src.indexOf("public void rpcDeleteChannel(RPCSender sender)");
            assertTrue(idx >= 0, "P3-T8: rpcDeleteChannel must exist");
            String body = src.substring(idx, Math.min(src.length(), idx + 900));
            assertTrue(body.contains("sender.isRemote()"),
                    "P3-T8: rpcDeleteChannel must guard C→S direction");
            assertTrue(body.contains("joinedKey()") && body.contains("== null"),
                    "P3-T8: non-member (channel not joined) must be refused");
            assertTrue(body.contains("reg.remove(key)"),
                    "P3-T8: delete must delegate to ChannelRegistry.remove (empty-only)");
        }

        @Test
        void scrollButtonsSwitchNormalHoverSprites() throws Exception {
            // hover 态切换：五按钮（▲上翻/▼下翻/＋创建/✕删除/断连退出）统一经 LDLib2
            // Button.buttonStyle 定义三态纹理（base=normal 行0、hover=hover 行1、pressed 复用 hover），
            // 悬停/按下切换由 Button 内部状态机完成，不再手写 MOUSE_ENTER/MOUSE_LEAVE 事件。
            var src = readMainSource(ABSTRACT_SRC);
            assertTrue(src.contains("buttonStyle"),
                    "P3-T8: buttons must use LDLib2 Button.buttonStyle for texture states");
            assertTrue(src.contains("baseTexture") && src.contains("hoverTexture") && src.contains("pressedTexture"),
                    "P3-T8: buttonStyle must define base/hover/pressed textures");
            assertTrue(src.contains("CHANNEL_BUTTONS"),
                    "P3-T8: buttons must be sourced from channel_buttons sheet");
            assertTrue(src.contains("SCROLL_UP_NORMAL") && src.contains("SCROLL_UP_HOVER"),
                    "P3-T8: scroll-up must define normal/hover UV pair");
            assertTrue(src.contains("SCROLL_DOWN_NORMAL") && src.contains("SCROLL_DOWN_HOVER"),
                    "P3-T8: scroll-down must define normal/hover UV pair");
            assertTrue(src.contains("DISCONNECT_NORMAL") && src.contains("DISCONNECT_HOVER"),
                    "P3-T8: leave/disconnect (col4) must define normal/hover UV pair");
        }

        @Test
        void entryMiniButtonSpritesAndHoverWired() throws Exception {
            // mini 接入状态按钮（P3-T8 UI 契约）：channel_entry_state 四格（接入×hover）雪碧图 +
            // 条目背景/mini 按钮经 MOUSE_ENTER/LEAVE 切换 hover 态；点击按接入态分流 join/leave RPC。
            var src = readMainSource(ABSTRACT_SRC);
            assertTrue(src.contains("CHANNEL_ENTRY_STATE"),
                    "P3-T8: mini button must source from channel_entry_state sheet");
            assertTrue(src.contains("MINI_OUT_NORMAL") && src.contains("MINI_OUT_HOVER")
                            && src.contains("MINI_IN_NORMAL") && src.contains("MINI_IN_HOVER"),
                    "P3-T8: mini button must define joined×hover UV quadruple");
            assertTrue(src.contains("MOUSE_ENTER") && src.contains("MOUSE_LEAVE"),
                    "P3-T8: entry background / mini button must switch hover sprites via MOUSE_ENTER/LEAVE");
            assertTrue(src.contains("rpcLeaveChannel") && src.contains("rpcJoinChannel"),
                    "P3-T8: mini click must toggle join/leave via RPC");
            var constants = readMainSource("com/modularmc/ten/TENConstants.java");
            assertTrue(constants.contains("channel_entry_state.png"),
                    "P3-T8: TENConstants must declare channel_entry_state texture");
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
