// -*- coding: utf-8 -*-
package com.modularmc.ten.common.blockentity;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 管道传输模型（方案 A）契约：root 每 tick 撮合 Pull 源 → Push 目标。
 * <p>
 * 用户设计（docs/pipe-transport-redesign.md，指挥官已确认全部推荐决策点）：
 * <ul>
 *   <li>Pull 管道（pullLevel&gt;0）：相邻 6 面容器视为源，collectPullSources 收集
 *       （simulate 抽取 + 源侧 isItemAllowed 过滤）</li>
 *   <li>Push 管道（pushLevel&gt;0）：相邻 6 面容器视为目标，collectPushTargets 收集（可接收空间）</li>
 *   <li>root 每 tick 撮合 matchAndTransfer：空间优先 + round-robin 破平局，双侧过滤 AND，
 *       目标排除同源，每源每节拍限量 singleTransferAmount，simulate 先行，剩余退回源</li>
 *   <li>旧双轨（transportHandler / tickPull / tickPush / processNetwork / extractAllowed）已删除</li>
 * </ul>
 * <p>
 * 使用源码模式扫描（项目既有约定）——PipeBlockEntity 依赖 MC bootstrap 无法在
 * 纯 JUnit 中实例化；语义断言锚定源码结构，避免运行时引导。
 */
class PipePullPushContractTest {

    private static final String PIPE_SRC = "com/modularmc/ten/common/blockentity/PipeBlockEntity.java";

    static String readSource(String relativePath) throws Exception {
        var f = new File("src/main/java/" + relativePath);
        assertTrue(f.exists(), "Source file must exist: " + relativePath);
        return Files.readString(f.toPath());
    }

    static String methodBody(String src, String methodHeader) throws Exception {
        int methodIdx = src.indexOf(methodHeader);
        assertTrue(methodIdx >= 0, "Method must exist: " + methodHeader);
        int bodyStart = src.indexOf('{', methodIdx);
        int bodyEnd = findMatchingBrace(src, bodyStart);
        return src.substring(bodyStart, bodyEnd);
    }

    static int findMatchingBrace(String s, int openIdx) {
        if (s.charAt(openIdx) != '{') {
            throw new IllegalArgumentException("Character at openIdx must be '{'");
        }
        int depth = 1;
        for (int i = openIdx + 1; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return i + 1; // exclusive end
            }
        }
        throw new IllegalArgumentException("No matching brace found from index " + openIdx);
    }

    // ════════════════════════════════════════════════════════════════
    // 1. tick 结构：每 tick root 撮合（旧双轨已删除）
    // ════════════════════════════════════════════════════════════════

    @Nested
    class TickStructure {

        @Test
        void tickDiscoversNetworkEveryTick() throws Exception {
            var src = readSource(PIPE_SRC);
            String body = methodBody(src, "protected void tick()");
            assertTrue(body.contains("TransferNetworks.collectConnected"),
                    "RED: tick must discover the network via collectConnected");
            assertTrue(body.contains("instanceof PipeBlockEntity"),
                    "RED: network predicate must be pipe adjacency");
        }

        @Test
        void onlyRootPerformsMatching() throws Exception {
            var src = readSource(PIPE_SRC);
            String body = methodBody(src, "protected void tick()");
            assertTrue(body.contains("TransferNetworks.isRoot"),
                    "RED: matching must be root-only (single orchestrator per network)");
            assertTrue(body.contains("matchAndTransfer(network);"),
                    "RED: root must invoke matchAndTransfer every tick");
        }

        @Test
        void nonRootPrunedBeforeBfs() throws Exception {
            var src = readSource(PIPE_SRC);
            String body = methodBody(src, "protected void tick()");
            assertTrue(body.contains("TransferNetworks.hasSmallerNeighbor"),
                    "RED: tick must prune non-root pipes via local coordinate comparison before BFS (低成本局部短路，避免每管道每 tick 全量 BFS)");
        }

        @Test
        void legacyDualTrackRemoved() throws Exception {
            var src = readSource(PIPE_SRC);
            assertFalse(src.contains("transportHandler"),
                    "RED: legacy network-proxy transportHandler must be removed");
            assertFalse(src.contains("tickPullPush"),
                    "RED: legacy tickPullPush must be removed");
            assertFalse(src.contains("private void tickPull("),
                    "RED: legacy tickPull must be removed");
            assertFalse(src.contains("private void tickPush("),
                    "RED: legacy tickPush must be removed");
            assertFalse(src.contains("processNetwork"),
                    "RED: legacy passive processNetwork must be removed");
            assertFalse(src.contains("private ItemStack extractAllowed"),
                    "RED: legacy extractAllowed must be removed (inlined into collectPullSources)");
            assertFalse(src.contains("getTransportHandler"),
                    "RED: legacy getTransportHandler must be removed");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 2. Pull 源收集：pullLevel>0 才收集；6 面相邻容器（跳过管道）；simulate + 源侧过滤
    // ════════════════════════════════════════════════════════════════

    @Nested
    class PullSourceCollection {

        @Test
        void collectPullSourcesGatedOnActiveOrPassiveComplement() throws Exception {
            var src = readSource(PIPE_SRC);
            String body = methodBody(src, "private List<PullCandidate> collectPullSources");
            // 主动/被动互补模型：Pull 源 = 主动 Pull（pullLevel>0）或被动端点（无升级）且网络互补方向为 PULL
            assertTrue(body.contains("isPullActor"),
                    "RED: pull source must use isPullActor (active Pull or passive complement)");
            assertTrue(src.contains("pipe.pullLevel > 0"),
                    "RED: active Pull (pullLevel>0) must be a pull actor");
            assertTrue(src.contains("passiveRole == PipeRole.PULL"),
                    "RED: passive endpoint with complementary PULL role must be a pull actor");
        }

        @Test
        void collectPullSourcesScansSixSidesSkipsPipes() throws Exception {
            var src = readSource(PIPE_SRC);
            String body = methodBody(src, "private List<PullCandidate> collectPullSources");
            assertTrue(body.contains("Direction.values()"),
                    "RED: pull collection must iterate all 6 directions");
            assertTrue(body.contains("pipePos.relative(direction)"),
                    "RED: pull collection must resolve neighbor positions relative to each pipe");
            assertTrue(body.contains("instanceof PipeBlockEntity"),
                    "RED: pull collection must skip adjacent pipe blocks (network interior)");
            assertTrue(body.contains("TransferNetworks.getItems"),
                    "RED: pull collection must look up item capability at the neighbor");
            assertTrue(body.contains("canHoldItems"),
                    "RED: only blocks that can hold items may count as network containers");
        }

        @Test
        void pullSourceSimulateExtractThenFilter() throws Exception {
            var src = readSource(PIPE_SRC);
            String body = methodBody(src, "private List<PullCandidate> collectPullSources");
            assertTrue(body.contains("extractItem(slot, perBeatLimit, true)"),
                    "RED: pull candidate must simulate-extract with perBeatLimit (no real extraction at collect time)");
            assertTrue(body.contains("pipe.isItemAllowed(simulated)"),
                    "RED: pull candidate must pass source-side isItemAllowed filter");
            assertTrue(body.contains("singleTransferAmount()"),
                    "RED: perBeatLimit must come from the pull pipe's singleTransferAmount()");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 3. Push 目标收集：pushLevel>0 才收集；6 面相邻容器（跳过管道）；可接收空间
    // ════════════════════════════════════════════════════════════════

    @Nested
    class PushTargetCollection {

        @Test
        void collectPushTargetsGatedOnActiveOrPassiveComplement() throws Exception {
            var src = readSource(PIPE_SRC);
            String body = methodBody(src, "private List<PushTarget> collectPushTargets");
            // 主动/被动互补模型：Push 目标 = 主动 Push（pushLevel>0）或被动端点（无升级）且网络互补方向为 PUSH
            assertTrue(body.contains("isPushActor"),
                    "RED: push target must use isPushActor (active Push or passive complement)");
            assertTrue(src.contains("pipe.pushLevel > 0"),
                    "RED: active Push (pushLevel>0) must be a push actor");
            assertTrue(src.contains("passiveRole == PipeRole.PUSH"),
                    "RED: passive endpoint with complementary PUSH role must be a push actor");
        }

        @Test
        void collectPushTargetsScansSixSidesSkipsPipes() throws Exception {
            var src = readSource(PIPE_SRC);
            String body = methodBody(src, "private List<PushTarget> collectPushTargets");
            assertTrue(body.contains("Direction.values()"),
                    "RED: push collection must iterate all 6 directions");
            assertTrue(body.contains("instanceof PipeBlockEntity"),
                    "RED: push collection must skip adjacent pipe blocks (network interior)");
            assertTrue(body.contains("TransferNetworks.getItems"),
                    "RED: push collection must look up item capability at the neighbor");
            assertTrue(body.contains("canHoldItems"),
                    "RED: only blocks that can hold items may count as network containers");
        }

        @Test
        void pushTargetComputesFreeSpace() throws Exception {
            var src = readSource(PIPE_SRC);
            String body = methodBody(src, "private List<PushTarget> collectPushTargets");
            assertTrue(body.contains("getSlotLimit(slot)"),
                    "RED: push target must compute free space from slot limits");
            assertTrue(body.contains("freeSpace > 0"),
                    "RED: full targets must be excluded (freeSpace > 0)");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 4. 撮合：空间优先 + round-robin；双侧过滤 AND；排除同源；每源限量
    // ════════════════════════════════════════════════════════════════

    @Nested
    class Matching {

        @Test
        void matchCallsBothCollections() throws Exception {
            var src = readSource(PIPE_SRC);
            String body = methodBody(src, "private void matchAndTransfer");
            assertTrue(body.contains("passiveRoleForNetwork(network)"),
                    "RED: matching must compute network-level passive complement direction");
            assertTrue(body.contains("collectPullSources(network, passiveRole)"),
                    "RED: matching must collect pull sources with passive role");
            assertTrue(body.contains("collectPushTargets(network, passiveRole)"),
                    "RED: matching must collect push targets with passive role");
        }

        @Test
        void noSourceOrNoTargetLeavesItemsInPlace() throws Exception {
            var src = readSource(PIPE_SRC);
            String body = methodBody(src, "private void matchAndTransfer");
            assertTrue(body.contains("candidates.isEmpty() || targets.isEmpty()"),
                    "RED: no pull source or no push target must short-circuit without moving (无缓冲直通)");
        }

        @Test
        void targetsSortedSpaceFirst() throws Exception {
            var src = readSource(PIPE_SRC);
            String sortBody = methodBody(src, "private void sortTargetsSpaceFirst");
            assertTrue(sortBody.contains("Comparator.comparingInt(PushTarget::freeSpace)"),
                    "RED: targets must be sorted by a comparator (space-first)");
            String matchBody = methodBody(src, "private void matchAndTransfer");
            assertTrue(matchBody.contains("sortTargetsSpaceFirst(targets)"),
                    "RED: matching must invoke the space-first sort before allocating");
        }

        @Test
        void roundRobinBreaksTies() throws Exception {
            var src = readSource(PIPE_SRC);
            assertTrue(src.contains("roundRobinIndex"),
                    "RED: network-level round-robin pointer must exist (break ties among equal free space)");
            String body = methodBody(src, "private void matchAndTransfer");
            assertTrue(body.contains("sortTargetsSpaceFirst(targets)"),
                    "RED: matching must invoke space-first + round-robin target sort");
        }

        @Test
        void targetExcludesSameSourceContainer() throws Exception {
            var src = readSource(PIPE_SRC);
            String body = methodBody(src, "private void matchAndTransfer");
            assertTrue(body.contains("target.sinkPos.equals(candidate.sourcePos)"),
                    "RED: a container must not receive items back into itself (排除同源)");
        }

        @Test
        void bothSidesFilteredWithAnd() throws Exception {
            var src = readSource(PIPE_SRC);
            String collectBody = methodBody(src, "private List<PullCandidate> collectPullSources");
            String matchBody = methodBody(src, "private void matchAndTransfer");
            assertTrue(collectBody.contains("isItemAllowed"),
                    "RED: source-side filter must run at pull collection (AND left operand)");
            assertTrue(matchBody.contains("target.pushPipe.isItemAllowed"),
                    "RED: target-side filter must run at matching (AND right operand)");
        }

        @Test
        void perBeatLimitPerSource() throws Exception {
            var src = readSource(PIPE_SRC);
            String body = methodBody(src, "private void matchAndTransfer");
            assertTrue(body.contains("movedBySource"),
                    "RED: matching must track moved count per source (per-beat limit)");
            assertTrue(body.contains("remaining <= 0"),
                    "RED: a source at its per-beat limit must be skipped");
        }

        @Test
        void simulateInsertBeforeRealExtract() throws Exception {
            var src = readSource(PIPE_SRC);
            String body = methodBody(src, "private void matchAndTransfer");
            assertTrue(body.contains("TransferNetworks.insertItem(target.sink, toInsert, true)"),
                    "RED: matching must simulate-insert first to confirm capacity");
        }

        @Test
        void leftoverReturnedToSource() throws Exception {
            var src = readSource(PIPE_SRC);
            String body = methodBody(src, "private void matchAndTransfer");
            assertTrue(body.contains("TransferNetworks.insertItem(candidate.source, leftover, false)"),
                    "RED: insert leftovers must be returned to the source container (不吞物品)");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 5. IO 模型保留：单次量公式不变（Jade/GUI/升级系统依赖）
    // ════════════════════════════════════════════════════════════════

    @Nested
    class TransferAmount {

        @Test
        void formulaIsBasePlusSevenPerSpeedTimesTwoToEnder() throws Exception {
            var src = readSource(PIPE_SRC);
            String body = methodBody(src, "public int singleTransferAmount()");
            assertTrue(body.contains("7L * speedLevel"),
                    "RED: per-speed increment must be 7×speedLevel (speed 9 → base 64)");
            assertTrue(body.contains("<< enderLevel"),
                    "RED: ender level must scale by 2^enderLevel (shift)");
            assertTrue(body.contains("1 +"),
                    "RED: base amount must start at 1");
        }

        @Test
        void overflowSafeLongWithIntClamp() throws Exception {
            var src = readSource(PIPE_SRC);
            String body = methodBody(src, "public int singleTransferAmount()");
            assertTrue(body.contains("long amount"),
                    "RED: amount must be computed in long to avoid int overflow (64 << 8 = 16384)");
            assertTrue(body.contains("Math.min(amount, Integer.MAX_VALUE)"),
                    "RED: long result must be clamped to int (IItemHandler.extractItem takes int)");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 网络级主动/被动互补：主动端点指定方向，被动端点（无升级）自动互补
    // ════════════════════════════════════════════════════════════════

    @Nested
    class PassiveComplement {

        @Test
        void activePullDrivesPassivePush() throws Exception {
            String src = readSource(PIPE_SRC);
            String body = methodBody(src, "private PipeRole passiveRoleForNetwork");
            assertTrue(body.contains("pullActive && !pushActive"),
                    "RED: active Pull without active Push must make passive endpoints PUSH");
            assertTrue(body.contains("return PipeRole.PUSH;"),
                    "RED: active Pull alone must complement passive as PUSH");
        }

        @Test
        void activePushDrivesPassivePull() throws Exception {
            String src = readSource(PIPE_SRC);
            String body = methodBody(src, "private PipeRole passiveRoleForNetwork");
            assertTrue(body.contains("pushActive && !pullActive"),
                    "RED: active Push without active Pull must make passive endpoints PULL");
            assertTrue(body.contains("return PipeRole.PULL;"),
                    "RED: active Push alone must complement passive as PULL");
        }

        @Test
        void bothActiveOrNoneIsNeutral() throws Exception {
            String src = readSource(PIPE_SRC);
            String body = methodBody(src, "private PipeRole passiveRoleForNetwork");
            assertTrue(body.contains("return PipeRole.NONE;"),
                    "RED: passive must be neutral when both active Pull+Push exist or none exist");
            assertTrue(src.contains("pipe.pullLevel > 0") && src.contains("pipe.pushLevel > 0"),
                    "RED: network must scan pullLevel/pushLevel to detect active endpoints");
        }
    }
}
