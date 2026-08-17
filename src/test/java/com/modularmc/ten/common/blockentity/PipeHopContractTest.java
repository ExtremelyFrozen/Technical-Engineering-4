// -*- coding: utf-8 -*-
package com.modularmc.ten.common.blockentity;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

/**
 * 管道逐级传递模型契约（化繁为简重构后）：
 * <ul>
 *   <li>统一速率：所有管道 TRANSFER_RATE = 64 物品/tick，无升级系统</li>
 *   <li>逐级传递：每根管道独立 tick（pushBuffer 推送 + pullFromContainers 抽取），无网络 root 撮合</li>
 *   <li>抽入点配置：扳手右键连接端切换（togglePullSide/isPullSide），未配置面仅被动 IO</li>
 *   <li>防回传：lastSourceDir 跳过本 tick 抽取来源方向</li>
 *   <li>缓冲持久化：管道内物品存档不丢失（BUFFER_KEY）</li>
 *   <li>过滤保留：pipe_white/pipe_black 标记物过滤仍用于源/目标侧</li>
 * </ul>
 */
class PipeHopContractTest {

    private static final String PIPE_SRC = "src/main/java/com/modularmc/ten/common/blockentity/PipeBlockEntity.java";

    static String readSource(String relativePath) throws Exception {
        var f = new File(relativePath);
        assertTrue(f.exists(), "Source file must exist: " + relativePath);
        return Files.readString(f.toPath());
    }

    static String methodBody(String src, String methodHeader) throws Exception {
        int methodIdx = src.indexOf(methodHeader);
        assertTrue(methodIdx >= 0, "Method must exist: " + methodHeader);
        int bodyStart = src.indexOf('{', methodIdx);
        assertTrue(bodyStart >= 0, "Method body start not found: " + methodHeader);
        int depth = 1;
        int i = bodyStart + 1;
        while (depth > 0 && i < src.length()) {
            char c = src.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') depth--;
            i++;
        }
        return src.substring(bodyStart, i);
    }

    @Test
    void unifiedRate64PerTick() throws Exception {
        var src = readSource(PIPE_SRC);
        assertTrue(src.contains("public static final int TRANSFER_RATE = 64"),
                "RED: all pipes must share TRANSFER_RATE = 64 (no upgrade scaling)");
        assertTrue(!src.contains("speedLevel") && !src.contains("enderLevel"),
                "RED: upgrade levels must be removed (no speed/ender scaling)");
    }

    @Test
    void noNetworkRootMatching() throws Exception {
        var src = readSource(PIPE_SRC);
        assertTrue(!src.contains("matchAndTransfer") && !src.contains("collectConnected"),
                "RED: hop model must not use network root matching (collectConnected/matchAndTransfer removed)");
        assertTrue(!src.contains("isPullActor") && !src.contains("passiveRoleForNetwork"),
                "RED: active/passive complement roles must be removed");
    }

    @Test
    void tickDrivesHopTransfer() throws Exception {
        var src = readSource(PIPE_SRC);
        String body = methodBody(src, "protected void tick");
        assertTrue(body.contains("pushBuffer()"),
                "RED: tick must push buffer to neighbors (hop-by-hop)");
        assertTrue(body.contains("pullFromContainers()"),
                "RED: tick must pull from containers into buffer");
        // 先抽取（记录来源）再推送（跳过来源）：pull 调用必须在 push 之前
        int pullIdx = body.indexOf("pullFromContainers()");
        int pushIdx = body.indexOf("pushBuffer()");
        assertTrue(pullIdx >= 0 && pushIdx >= 0 && pullIdx < pushIdx,
                "RED: tick must pull (record sources) before push (skip sources) for backflow prevention");
    }

    @Test
    void bufferCapacityCapsItems() throws Exception {
        var src = readSource(PIPE_SRC);
        assertTrue(src.contains("BUFFER_CAPACITY = 64"),
                "RED: buffer capacity must be 64 (single item type, one stack)");
        assertTrue(src.contains("BUFFER_CAPACITY - buffer.getCount()"),
                "RED: pull must respect remaining buffer space");
    }

    @Test
    void pullSidesGateActiveExtraction() throws Exception {
        var src = readSource(PIPE_SRC);
        String pullBody = methodBody(src, "private void pullFromContainers");
        assertTrue(pullBody.contains("pullSides[direction.get3DDataValue()]"),
                "RED: pull must only run on configured pull sides (spanner pull-points)");
        assertTrue(src.contains("public boolean togglePullSide(Direction side)"),
                "RED: togglePullSide must exist (spanner interaction)");
        assertTrue(src.contains("public boolean isPullSide(Direction side)"),
                "RED: isPullSide must exist (jade/UI read)");
    }

    @Test
    void backflowSkippedBySourceDirs() throws Exception {
        var src = readSource(PIPE_SRC);
        String pushBody = methodBody(src, "private void pushBuffer");
        assertTrue(pushBody.contains("sourceDirs.contains(direction)"),
                "RED: push must skip source directions recorded in sourceDirs (prevent A<->B oscillation)");
        assertTrue(src.contains("sourceDirs.add(direction)"),
                "RED: pull must record pulled-from direction into sourceDirs");
        assertTrue(src.contains("sourceDirs.add(fromDir)"),
                "RED: tryReceive must record incoming direction into sourceDirs");
        assertTrue(pushBody.contains("sourceDirs.clear()"),
                "RED: sourceDirs must clear once buffer is fully pushed (next pull re-records)");
    }

    @Test
    void bufferPersistedInTileData() throws Exception {
        var src = readSource(PIPE_SRC);
        String readBody = methodBody(src, "protected void readTileData");
        assertTrue(readBody.contains("BUFFER_KEY"),
                "RED: readTileData must restore hop buffer (items in pipes survive save/load)");
        String writeBody = methodBody(src, "protected void writeTileData");
        assertTrue(writeBody.contains("BUFFER_KEY"),
                "RED: writeTileData must persist hop buffer");
        assertTrue(writeBody.contains("\"pullSides\""),
                "RED: writeTileData must persist pull-side bitmask");
    }

    @Test
    void filterStillGatesBothSides() throws Exception {
        var src = readSource(PIPE_SRC);
        String pullBody = methodBody(src, "private void pullFromContainers");
        assertTrue(pullBody.contains("isItemAllowed(simulated)"),
                "RED: pull must apply source-side isItemAllowed filter");
        String pushBody = methodBody(src, "private void pushBuffer");
        assertTrue(pushBody.contains("isItemAllowed(buffer)"),
                "RED: push must apply target-side isItemAllowed filter (AND semantics)");
        assertTrue(src.contains("public boolean isFiltered()") && src.contains("public boolean isWhitelist()"),
                "RED: filter mode getters must remain (pipe_white/pipe_black variants kept)");
    }
}
