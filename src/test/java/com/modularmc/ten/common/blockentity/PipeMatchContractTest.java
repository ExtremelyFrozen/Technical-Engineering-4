// -*- coding: utf-8 -*-
package com.modularmc.ten.common.blockentity;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 管道撮合算法契约（docs/pipe-transport-redesign.md §3.2-3.6，指挥官已确认）。
 * <p>
 * 覆盖：空间优先排序（least-loaded first）、round-robin 轮转破平局（防振荡）、
 * 双侧过滤 AND（Pull 源侧 + Push 目标侧）、目标排除同源、每源每节拍限量、
 * 无目标退回源（无缓冲直通，不吞物品）。
 * <p>
 * 使用源码模式扫描（项目既有约定）——MC bootstrap 无法在纯 JUnit 中实例化
 * PipeBlockEntity；语义断言锚定源码结构，避免运行时引导。
 */
class PipeMatchContractTest {

    private static final String PIPE_SRC = "com/modularmc/ten/common/blockentity/PipeBlockEntity.java";

    static String readSource() throws Exception {
        var f = new File("src/main/java/" + PIPE_SRC);
        assertTrue(f.exists(), "Source file must exist: " + PIPE_SRC);
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
    // 1. 空间优先（least-loaded first）
    // ════════════════════════════════════════════════════════════════

    @Nested
    class SpaceFirst {

        @Test
        void targetSortDescendsByFreeSpace() throws Exception {
            var src = readSource();
            String body = methodBody(src, "private void sortTargetsSpaceFirst");
            assertTrue(body.contains("Comparator.comparingInt(PushTarget::freeSpace)"),
                    "RED: targets must be sorted by freeSpace comparator");
            assertTrue(body.contains(".reversed()"),
                    "RED: most-empty target must come first (descending free space)");
        }

        @Test
        void freeSpaceComputedFromSlotLimits() throws Exception {
            var src = readSource();
            String body = methodBody(src, "private List<PushTarget> collectPushTargets");
            assertTrue(body.contains("getSlotLimit(slot) - stack.getCount()"),
                    "RED: free space must be slotLimit - current count per slot");
        }

        @Test
        void matchIteratesTargetsInSortedOrder() throws Exception {
            var src = readSource();
            String body = methodBody(src, "private void matchAndTransfer");
            assertTrue(body.contains("for (PushTarget target : targets)"),
                    "RED: matching must iterate the (space-first) sorted target list");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 2. round-robin 破平局（防振荡）
    // ════════════════════════════════════════════════════════════════

    @Nested
    class RoundRobin {

        @Test
        void networkLevelPointerExists() throws Exception {
            var src = readSource();
            assertTrue(src.contains("long roundRobinIndex"),
                    "RED: a network-level round-robin pointer field must exist");
        }

        @Test
        void tieGroupsRotatedByPointer() throws Exception {
            var src = readSource();
            String body = methodBody(src, "private void sortTargetsSpaceFirst");
            assertTrue(body.contains("roundRobinIndex % targets.size()"),
                    "RED: rotation offset must be derived from the round-robin pointer");
            assertTrue(body.contains("groupOffset"),
                    "RED: equal-free-space groups must be cyclically rotated (break ties)");
        }

        @Test
        void pointerAdvancesPerBeat() throws Exception {
            var src = readSource();
            String body = methodBody(src, "private void sortTargetsSpaceFirst");
            assertTrue(body.contains("roundRobinIndex++"),
                    "RED: the pointer must advance each beat to keep rotating");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 3. 双侧过滤 AND（Pull 源侧 + Push 目标侧）
    // ════════════════════════════════════════════════════════════════

    @Nested
    class BothSideFiltering {

        @Test
        void sourceSideFilteredAtCollection() throws Exception {
            var src = readSource();
            String body = methodBody(src, "private List<PullCandidate> collectPullSources");
            assertTrue(body.contains("pipe.isItemAllowed(simulated)"),
                    "RED: candidate must pass the pull pipe's isItemAllowed at collection");
        }

        @Test
        void targetSideFilteredAtMatching() throws Exception {
            var src = readSource();
            String body = methodBody(src, "private void matchAndTransfer");
            assertTrue(body.contains("target.pushPipe.isItemAllowed(candidate.preview)"),
                    "RED: target must pass the push pipe's isItemAllowed at matching (AND semantics)");
        }

        @Test
        void filterRejectionSkipsTarget() throws Exception {
            var src = readSource();
            String body = methodBody(src, "private void matchAndTransfer");
            int filterIdx = body.indexOf("target.pushPipe.isItemAllowed");
            assertTrue(filterIdx >= 0, "RED: target-side filter must exist");
            String after = body.substring(filterIdx, Math.min(body.length(), filterIdx + 120));
            assertTrue(after.contains("continue;"),
                    "RED: filter rejection must skip to the next target (no transfer)");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 4. 目标排除同源（防自环）
    // ════════════════════════════════════════════════════════════════

    @Nested
    class ExcludeSameSource {

        @Test
        void sinkEqualToSourceSkipped() throws Exception {
            var src = readSource();
            String body = methodBody(src, "private void matchAndTransfer");
            assertTrue(body.contains("target.sinkPos.equals(candidate.sourcePos)"),
                    "RED: a sink equal to the candidate's source must be skipped (no self-loop)");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 5. 每源限量（perBeatLimit）
    // ════════════════════════════════════════════════════════════════

    @Nested
    class PerBeatLimit {

        @Test
        void movedCountTrackedPerSourceKey() throws Exception {
            var src = readSource();
            String body = methodBody(src, "private void matchAndTransfer");
            assertTrue(body.contains("movedBySource.merge"),
                    "RED: moved count must accumulate per source key");
            assertTrue(body.contains("perBeatLimit - movedBySource.getOrDefault"),
                    "RED: remaining budget must be perBeatLimit minus already moved");
        }

        @Test
        void limitComesFromSingleTransferAmount() throws Exception {
            var src = readSource();
            String body = methodBody(src, "private List<PullCandidate> collectPullSources");
            assertTrue(body.contains("singleTransferAmount()"),
                    "RED: perBeatLimit must come from singleTransferAmount() (SPEED/ENDER formula retained)");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 6. 无目标退回源 / 无缓冲直通
    // ════════════════════════════════════════════════════════════════

    @Nested
    class NoTargetFallback {

        @Test
        void noTargetsShortCircuitsWithoutMoving() throws Exception {
            var src = readSource();
            String body = methodBody(src, "private void matchAndTransfer");
            assertTrue(body.contains("candidates.isEmpty() || targets.isEmpty()"),
                    "RED: without push targets the beat must short-circuit (items stay in source)");
        }

        @Test
        void insertLeftoverReturnedToSource() throws Exception {
            var src = readSource();
            String body = methodBody(src, "private void matchAndTransfer");
            assertTrue(body.contains("TransferNetworks.insertItem(candidate.source, leftover, false)"),
                    "RED: insert leftovers must be returned to the source container (尽力而为，不吞物品)");
        }

        @Test
        void rollbackFailureLoggedAndNotSwallowed() throws Exception {
            var src = readSource();
            String body = methodBody(src, "private void matchAndTransfer");
            int rollbackIdx = body.indexOf("TransferNetworks.insertItem(candidate.source, leftover, false)");
            assertTrue(rollbackIdx >= 0,
                    "RED: insert leftovers must be returned to the source container (尽力而为，不吞物品)");
            String after = body.substring(rollbackIdx, Math.min(body.length(), rollbackIdx + 400));
            assertTrue(after.contains("rollbackLeftover"),
                    "RED: rollback return value must be captured (不得吞掉失败退回的物品)");
            assertTrue(after.contains("LOGGER.warn"),
                    "RED: rollback failure must log a warning (留滞目标侧并告警，便于排查)");
        }

        @Test
        void simulatePrecedesRealTransfer() throws Exception {
            var src = readSource();
            String body = methodBody(src, "private void matchAndTransfer");
            assertTrue(body.contains("insertItem(target.sink, toInsert, true)"),
                    "RED: capacity must be confirmed by simulate insert before real extract");
            assertTrue(body.contains("extractItem(candidate.slot, accepted, false)"),
                    "RED: real extract must only happen after simulate confirms capacity");
        }
    }
}
