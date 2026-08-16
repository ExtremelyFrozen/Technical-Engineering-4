// -*- coding: utf-8 -*-
package com.modularmc.ten.api.blockentity;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 单元 C：采矿场重构 — Quarry 游标扫描系统契约（RED→GREEN）。
 * <p>
 * 覆盖（单元 A/B 已实现，本文件固化其行为契约）：
 * <ul>
 *   <li>游标字段 scanX/scanZ/scanY/scanFast @Persisted（存档持久化）</li>
 *   <li>scanFast @Persisted + @DescSynced（GUI 同步）</li>
 *   <li>scanCursorInitialized @Persisted（M1：重启保持挖尽停机，不整坑重扫）</li>
 *   <li>scanMine 标准逐层 / 极速找非空气层双分支</li>
 *   <li>scanExhausted 挖尽停机门禁（conditionStart 检查 + applyEffect 早退 + 循环内 break）</li>
 *   <li>恢复路径：rpcToggleScanFast 切换 / 半径变化 / 模式变化 → resetScanCursor</li>
 *   <li>Mineral case 0,3 共用 scanMine（canBreak ore 过滤）</li>
 *   <li>mineRandomOre / mineRandomBlock 已删</li>
 * </ul>
 * <p>
 * 断言风格与既有 P3BatchCapacityContractTest 一致：读取源码字符串 + 纯逻辑模拟，
 * 不启动 MC 运行时。
 */
class QuarryScanContractTest {

    /** QuarryBlockEntity 源码字符串（相对工作区根，与既有契约测试一致）。 */
    private static String quarrySource() throws Exception {
        var sourceFile = new java.io.File(
                "src/main/java/com/modularmc/ten/common/blockentity/machine/QuarryBlockEntity.java");
        assertTrue(sourceFile.exists(), "QuarryBlockEntity source must exist");
        return java.nio.file.Files.readString(sourceFile.toPath());
    }

    /** 提取方法体：从 methodSig 到 nextSig（找不到 nextSig 时回退固定长度）。 */
    private static String methodBody(String content, String methodSig, String nextSig, int fallback) {
        int start = content.indexOf(methodSig);
        assertTrue(start >= 0, methodSig + " must exist");
        int end = content.indexOf(nextSig, start + methodSig.length());
        if (end < 0) end = Math.min(start + fallback, content.length());
        return content.substring(start, end);
    }

    // ════════════════════════════════════════════════════════════
    // A. 游标字段持久化（@Persisted / @DescSynced）
    // ════════════════════════════════════════════════════════════

    @Nested
    class CursorPersistence {

        @Test
        void scanX_scanZ_scanY_arePersisted() throws Exception {
            var content = quarrySource();
            assertTrue(content.contains("@Persisted\n    public int scanX"),
                    "scanX 必须 @Persisted（重启后保持栅格游标 X）");
            assertTrue(content.contains("@Persisted\n    public int scanZ"),
                    "scanZ 必须 @Persisted（重启后保持栅格游标 Z）");
            assertTrue(content.contains("@Persisted\n    public int scanY"),
                    "scanY 必须 @Persisted（重启后保持扫描层 Y）");
        }

        @Test
        void scanCursorInitialized_isPersisted() throws Exception {
            var content = quarrySource();
            assertTrue(content.contains("@Persisted\n    public boolean scanCursorInitialized"),
                    "scanCursorInitialized 必须 @Persisted（M1：重启后保持初始化状态，挖尽存档不整坑重扫空转）");
        }

        @Test
        void scanFast_isPersistedAndDescSynced() throws Exception {
            var content = quarrySource();
            assertTrue(content.contains("@Persisted\n    @DescSynced\n    public boolean scanFast"),
                    "scanFast 必须 @Persisted（存档持久化）且 @DescSynced（GUI 双向同步）");
        }

        @Test
        void cursorFields_arePublicAndTyped() throws Exception {
            var content = quarrySource();
            assertTrue(content.contains("public int scanX"), "scanX 为 public int");
            assertTrue(content.contains("public int scanZ"), "scanZ 为 public int");
            assertTrue(content.contains("public int scanY"), "scanY 为 public int");
            assertTrue(content.contains("public boolean scanFast"), "scanFast 为 public boolean");
        }
    }

    // ════════════════════════════════════════════════════════════
    // B. 标准 / 极速扫描分支（scanMine）
    // ════════════════════════════════════════════════════════════

    @Nested
    class ScanModeBranch {

        @Test
        void scanMine_hasFastAndStandardBranch() throws Exception {
            var content = quarrySource();
            String body = methodBody(content, "private boolean scanMine()", "private void advanceCursorInLayer", 2500);
            // 极速分支：findNextNonAirLayer 跳过空气层
            assertTrue(body.contains("findNextNonAirLayer"),
                    "scanMine 极速分支必须调用 findNextNonAirLayer 跳过空气层");
            // 标准分支：逐层递减 scanY--
            assertTrue(body.contains("scanY--"),
                    "scanMine 标准分支必须 scanY-- 逐层下降");
            // 分支由 scanFast 决定
            assertTrue(body.contains("scanFast"),
                    "扫描分支必须由 scanFast 标志决定");
        }

        @Test
        void findNextNonAirLayer_scansPlusMinusRadius() throws Exception {
            var content = quarrySource();
            String body = methodBody(content, "private int findNextNonAirLayer", "private void resetScanCursor", 700);
            assertTrue(body.contains("dx <= radius") && body.contains("dz <= radius"),
                    "极速找层必须扫描 ±radius 全范围");
        }
    }

    // ════════════════════════════════════════════════════════════
    // C. 挖尽停机门禁（scanExhausted + conditionStart + applyEffect 早退）
    // ════════════════════════════════════════════════════════════

    @Nested
    class ExhaustedStopGate {

        @Test
        void conditionStart_checksScanExhausted() throws Exception {
            var content = quarrySource();
            String body = methodBody(content, "public boolean conditionStart()", "public boolean cooking()", 400);
            assertTrue(body.contains("scanExhausted()"),
                    "conditionStart 必须检查 scanExhausted()（挖尽后不再锁定批次/耗能）");
            assertTrue(body.contains("scanCursorInitialized"),
                    "游标未初始化时放行首个周期（首次运行/重启）");
        }

        @Test
        void scanExhausted_returnsTrueWhenScanYAtZero() throws Exception {
            var content = quarrySource();
            String body = methodBody(content, "public boolean scanExhausted()", "public void rpcToggleScanFast", 200);
            assertTrue(body.contains("scanY <= 0"),
                    "scanY 到 0 即判定挖尽（极速模式无可挖层同样归 0）");
        }

        @Test
        void applyEffect_earlyReturnsOnExhausted() throws Exception {
            var content = quarrySource();
            String body = methodBody(content, "public void applyEffect()", "public boolean conditionStart()", 900);
            assertTrue(body.contains("scanExhausted()"),
                    "applyEffect 必须检测挖尽并早退，不再空转耗能");
            assertTrue(body.contains("setActive(false)"),
                    "挖尽必须停机（setActive(false)）");
            assertTrue(body.contains("clearLockedBatch()"),
                    "挖尽必须清锁批次（clearLockedBatch()）");
        }

        @Test
        void applyEffect_loopBreaksOnExhaustedMidCycle() throws Exception {
            var content = quarrySource();
            String body = methodBody(content, "public void applyEffect()", "public boolean conditionStart()", 900);
            // M2：循环内挖尽（本 cycle 中途 scanY→0）→ break，剩余 B−1 次不再空转耗能
            assertTrue(body.contains("scanExhausted()) break;"),
                    "applyEffect 循环内必须 break 于挖尽（M2：中途 scanY→0 后剩余迭代不空转）");
            // 契约协调（QuarryLoopMissVsBreak）：不得 break 于 executeSingleOperation() false
            assertFalse(content.contains("if (!executeSingleOperation()) break;"),
                    "循环内仅允许工具空/挖尽 break，操作 miss 必须 continue（QuarryLoopMissVsBreak 契约）");
        }
    }

    // ════════════════════════════════════════════════════════════
    // D. 恢复路径：切换 / 半径变化 / 模式变化 → 游标复位
    // ════════════════════════════════════════════════════════════

    @Nested
    class RecoveryPaths {

        @Test
        void rpcToggleScanFast_flipsAndResetsCursor() throws Exception {
            var content = quarrySource();
            String body = methodBody(content, "public void rpcToggleScanFast", "private boolean canBreak", 500);
            assertTrue(body.contains("scanFast = !scanFast"),
                    "rpcToggleScanFast 必须翻转 scanFast（C→S 服务端执行）");
            assertTrue(body.contains("resetScanCursor()"),
                    "挖尽停机后切换模式必须复位游标重新扫描（恢复路径）");
        }

        @Test
        void tick_resetsCursorOnRadiusOrModeChange() throws Exception {
            var content = quarrySource();
            String body = methodBody(content, "public void tick()", "private void updateMode", 900);
            assertTrue(body.contains("lastScanRadius != radius"),
                    "tick 必须探测半径变化（升级 → 复位重扫）");
            assertTrue(body.contains("lastMode != mode"),
                    "tick 必须探测模式变化（挖尽停机后升级增减 → 复位重扫）");
            assertTrue(body.contains("resetScanCursor()"),
                    "变化后必须复位游标（恢复路径）");
        }

        @Test
        void scanMine_resetsCursorOnRadiusChange() throws Exception {
            var content = quarrySource();
            String body = methodBody(content, "private boolean scanMine()", "private void advanceCursorInLayer", 2500);
            assertTrue(body.contains("lastScanRadius != radius"),
                    "scanMine 必须探测半径变化（升级后从新边界重扫）");
            assertTrue(body.contains("resetScanCursor()"),
                    "半径变化后必须复位游标");
        }
    }

    // ════════════════════════════════════════════════════════════
    // E. Mineral case 0,3 共用 scanMine（canBreak ore 过滤）
    // ════════════════════════════════════════════════════════════

    @Nested
    class MineralScanShare {

        @Test
        void executeSingleOperation_case0And3_useScanMine() throws Exception {
            var content = quarrySource();
            String body = methodBody(content, "private boolean executeSingleOperation()", "private boolean scanMine()", 500);
            assertTrue(body.contains("case 0, 3 -> scanMine()"),
                    "mode 0/3 必须共用 scanMine（常规挖掘 + Mineral 矿石扫描同一游标栅格）");
        }

        @Test
        void canBreak_filtersOresInMineralMode() throws Exception {
            var content = quarrySource();
            String body = methodBody(content, "private boolean canBreak", "private boolean giveGeneratedLoot", 500);
            // 标准模式宽泛名单（用户需求）：仅排除含方块实体/留存数据的方块（如机器/容器），
            // 不再依赖 quarry_valids 标签；工具挖掘等级约束保留。
            assertTrue(body.contains("hasBlockEntity"),
                    "mode 0 必须排除含方块实体/额外数据组件的方块（hasBlockEntity）");
            assertTrue(body.contains("c:ores"),
                    "mode 3 必须过滤 c:ores 矿石标签");
            assertTrue(body.contains("isCorrectToolForDrops"),
                    "两种模式都必须校验工具正确性（isCorrectToolForDrops，含挖掘等级）");
        }
    }

    // ════════════════════════════════════════════════════════════
    // F. 旧随机挖掘方法移除
    // ════════════════════════════════════════════════════════════

    @Nested
    class LegacyRemoval {

        @Test
        void mineRandomOre_isRemoved() throws Exception {
            var content = quarrySource();
            assertFalse(content.contains("mineRandomOre"),
                    "mineRandomOre 必须已删除（被 scanMine 游标栅格扫描取代）");
        }

        @Test
        void mineRandomBlock_isRemoved() throws Exception {
            var content = quarrySource();
            assertFalse(content.contains("mineRandomBlock"),
                    "mineRandomBlock 必须已删除（被 scanMine 游标栅格扫描取代）");
        }
    }
}
