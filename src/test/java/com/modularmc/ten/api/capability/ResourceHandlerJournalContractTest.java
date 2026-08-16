// -*- coding: utf-8 -*-
package com.modularmc.ten.api.capability;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 能力适配器 Journal 事务契约（NeoForge 26.1.2 Journal 模型）。
 * <p>
 * 背景：SDK 反编译确认 26.1.2 事务为 {@code SnapshotJournal + updateSnapshots}（无旧
 * CloseCallback）。原生 {@code ItemStackResourceHandler} 继承 SnapshotJournal。TEN 三个
 * 反向适配器必须遵循同模式，否则 simulate 事务（经 ResourceHandler 访问机器）会真实
 * 变更底层 handler，导致物品被抽入虚无 / 能量泄漏 / 复制。
 * <p>
 * 契约锚定：
 * <ul>
 *   <li>ItemHandlerResourceAdapter extends SnapshotJournal&lt;ItemStack[]&gt;：simulate 抽取不真实变更；
 *       revert 经 IItemHandlerModifiable 写回</li>
 *   <li>FluidHandlerResourceAdapter extends SnapshotJournal&lt;FluidStack[]&gt;：已正确，防回归</li>
 *   <li>CapabilityAdapters.asEnergyHandler → EnergyResourceAdapter extends SnapshotJournal&lt;Integer&gt;：
 *       能量 simulate 失真防护；revert 用差值恢复（IEnergyStorage 无 set）</li>
 *   <li>write 方法（insert/extract）在修改底层 handler 前调用 updateSnapshots（捕获 before）</li>
 * </ul>
 * 使用源码模式扫描（项目既有约定）——适配器依赖 MC bootstrap 无法在纯 JUnit 实例化。
 */
class ResourceHandlerJournalContractTest {

    static String readSource(String relativePath) throws Exception {
        var f = new File("src/main/java/" + relativePath);
        assertTrue(f.exists(), "Source file must exist: " + relativePath);
        return Files.readString(f.toPath());
    }

    @Nested
    class ItemHandlerAdapter {
        private static final String SRC = "com/modularmc/ten/api/capability/ItemHandlerResourceAdapter.java";

        @Test
        void extendsSnapshotJournal() throws Exception {
            String src = readSource(SRC);
            assertTrue(src.contains("extends SnapshotJournal<ItemStack[]>"),
                    "RED: adapter must extend SnapshotJournal<ItemStack[]> for transaction rollback");
            assertTrue(src.contains("implements ResourceHandler<ItemResource>"),
                    "RED: adapter must implement ResourceHandler<ItemResource>");
        }

        @Test
        void insertUpdateSnapshotsBeforeMutation() throws Exception {
            String src = readSource(SRC);
            // updateSnapshots 必须在 handler.insertItem(false)（真实修改）之前调用，
            // 否则快照捕获的是修改后状态，abort 无法恢复（物品被抽入虚无根因）。
            int snapshotIdx = src.indexOf("this.updateSnapshots(transaction);");
            int realInsertIdx = src.indexOf("handler.insertItem(index, toInsert, false);");
            assertTrue(snapshotIdx >= 0 && realInsertIdx >= 0,
                    "RED: insert must contain both updateSnapshots and real insert");
            assertTrue(snapshotIdx < realInsertIdx,
                    "RED: updateSnapshots must precede real insert (capture before-state)");
        }

        @Test
        void extractSimulateThenRealWithSnapshot() throws Exception {
            String src = readSource(SRC);
            // 限定 extract 方法体，避免 updateSnapshots 匹配到 insert 里的第一处。
            String body = methodBody(src, "public int extract(int index, ItemResource resource, int amount, TransactionContext transaction)");
            int simIdx = body.indexOf("handler.extractItem(index, toExtract, true);");
            int snapIdx = body.indexOf("this.updateSnapshots(transaction);");
            int realIdx = body.indexOf("handler.extractItem(index, extractedCount, false);");
            assertTrue(simIdx >= 0 && snapIdx >= 0 && realIdx >= 0,
                    "RED: extract must simulate, snapshot, then real-extract");
            assertTrue(simIdx < snapIdx && snapIdx < realIdx,
                    "RED: simulate → updateSnapshots → real extract order required");
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
                    if (depth == 0) return i;
                }
            }
            throw new IllegalStateException("Unbalanced braces in source");
        }

        @Test
        void revertUsesIItemHandlerModifiable() throws Exception {
            String src = readSource(SRC);
            assertTrue(src.contains("instanceof IItemHandlerModifiable"),
                    "RED: revert must use IItemHandlerModifiable.setStackInSlot (IItemHandler has no setStackInSlot)");
            assertTrue(src.contains("modifiable.setStackInSlot(i, snapshot[i]);"),
                    "RED: revert must restore each slot from snapshot");
        }
    }

    @Nested
    class FluidHandlerAdapter {
        private static final String SRC = "com/modularmc/ten/api/capability/FluidHandlerResourceAdapter.java";

        @Test
        void extendsSnapshotJournal() throws Exception {
            String src = readSource(SRC);
            assertTrue(src.contains("extends SnapshotJournal<FluidStack[]>"),
                    "RED: fluid adapter must extend SnapshotJournal<FluidStack[]>");
        }

        @Test
        void updateSnapshotsBeforeTankMutation() throws Exception {
            String src = readSource(SRC);
            int snapIdx = src.indexOf("updateSnapshots(transaction);");
            int setIdx = src.indexOf("tank.setFluid(newFluid);");
            assertTrue(snapIdx >= 0 && setIdx >= 0,
                    "RED: fluid insert must snapshot then set fluid");
            assertTrue(snapIdx < setIdx,
                    "RED: updateSnapshots must precede tank.setFluid");
        }
    }

    @Nested
    class EnergyAdapter {
        private static final String SRC = "com/modularmc/ten/api/capability/CapabilityAdapters.java";

        @Test
        void energyAdapterIsJournalClass() throws Exception {
            String src = readSource(SRC);
            assertTrue(src.contains("extends SnapshotJournal<Integer> implements EnergyHandler"),
                    "RED: energy adapter must extend SnapshotJournal<Integer> (EnergyHandler is interface, cannot be anonymous journal)");
        }

        @Test
        void energySimulateBeforeReal() throws Exception {
            String src = readSource(SRC);
            int simInsertIdx = src.indexOf("storage.receiveEnergy(amount, true);");
            int realInsertIdx = src.indexOf("storage.receiveEnergy(canInsert, false);");
            int simExtractIdx = src.indexOf("storage.extractEnergy(amount, true);");
            int realExtractIdx = src.indexOf("storage.extractEnergy(canExtract, false);");
            assertTrue(simInsertIdx >= 0 && realInsertIdx >= 0
                    && simInsertIdx < realInsertIdx, "RED: insert must simulate then real");
            assertTrue(simExtractIdx >= 0 && realExtractIdx >= 0
                    && simExtractIdx < realExtractIdx, "RED: extract must simulate then real");
        }

        @Test
        void energyRevertPreciseFirstThenDiff() throws Exception {
            String src = readSource(SRC);
            // 优先精确恢复：底层为 MachineEnergyStorage（如 cable 本体）时用 setEnergy 绕过速率限制与门控
            assertTrue(src.contains("storage instanceof MachineEnergyStorage machine")
                            && src.contains("machine.setEnergy(snapshot);"),
                    "GREEN: energy revert must prefer precise setEnergy when storage is MachineEnergyStorage");
            // 兜底：非 MachineEnergyStorage（机器 side 包装/能量单元）用循环差值恢复（逐轮 extract/receive）
            assertTrue(src.contains("storage.extractEnergy(remaining, false)")
                            && src.contains("storage.receiveEnergy(remaining, false)"),
                    "GREEN: energy revert must loop-diff restore (extractEnergy/receiveEnergy) for non-MachineEnergyStorage");
        }
    }

    @Nested
    class MachineSideItemHandlerModifiable {
        private static final String SRC = "com/modularmc/ten/api/blockentity/CmMachineBlockEntity.java";

        @Test
        void machineSideItemWrapperMustBeModifiable() throws Exception {
            String src = readSource(SRC);
            // 机器 side 物品包装必须实现 IItemHandlerModifiable，否则 ItemHandlerResourceAdapter
            // 事务回滚（simulate 真实变更 + abort）因非 modifiable 跳过 → simulate 探测真实抽走/塞入
            // 机器物品无法还原（物品消失/复制）。此契约守护修复防回归。
            assertTrue(src.contains("new IItemHandlerModifiable()"),
                    "RED: machine side item wrapper must implement IItemHandlerModifiable");
            assertTrue(src.contains("public void setStackInSlot(int slot, ItemStack stack)"),
                    "RED: machine side item wrapper must expose setStackInSlot for journal rollback");
            // 快照/回滚对称性：getStackInSlot 必须直通内层（不能按 canExtractItem 门控返回 EMPTY），
            // 否则 IN-only 面 insert 事务快照被污染为全空，abort 回滚 setStackInSlot(EMPTY) 清空真实物品。
            assertFalse(src.contains("canExtractItem(side) ? itemHandler.getStackInSlot(slot) : ItemStack.EMPTY"),
                    "RED: getStackInSlot must pass through to inner handler (symmetry with setStackInSlot), "
                            + "not gate by canExtractItem - otherwise IN-only face insert rollback clears items");
            assertTrue(src.contains("return itemHandler.getStackInSlot(slot);"),
                    "GREEN: machine side getStackInSlot must pass through to inner handler");
        }
    }
}
