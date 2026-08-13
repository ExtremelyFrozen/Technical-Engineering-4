// -*- coding: utf-8 -*-
package com.modularmc.ten.common.blockentity;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 偏差 #1 修复契约：Pipe 黑白名单过滤重构（用户设计）。
 * <p>
 * 用户设计：
 * <ul>
 *   <li>pipe_white（白名单）/ pipe_black（黑名单）各自通过潜行右键打开配置 GUI 获取标记物</li>
 *   <li>配置界面操作区为 3×9=27 个物品槽（标记物即白/黑名单匹配项）</li>
 *   <li>过滤逻辑：白名单=标记物非空且匹配才放行；黑名单=不匹配才放行；空标记物忽略</li>
 * </ul>
 * <p>
 * 使用源码模式扫描（项目既有约定）——PipeBlockEntity 依赖 MC bootstrap 无法在
 * 纯 JUnit 中实例化；语义断言锚定源码结构，避免运行时引导。
 */
class PipeFilterContractTest {

    private static final String PIPE_SRC = "com/modularmc/ten/common/blockentity/PipeBlockEntity.java";
    private static final String BASE_SRC = "com/modularmc/ten/common/block/machine/BaseMachineBlock.java";

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
    // 1. 过滤槽位：单页 3×9=27（总槽 = 27×(1+pageLevel)，见 PipePageContractTest）
    // ════════════════════════════════════════════════════════════════

    @Nested
    class FilterSlotCount {

        @Test
        void filterInventoryStartsWithSinglePage27Slots() throws Exception {
            var src = readSource(PIPE_SRC);
            // 单页容量常量 = 3×9 = 27；构造用常量而非裸数字（扩写升级按 27×页数动态扩容）
            assertTrue(src.contains("FILTER_SLOTS_PER_PAGE = 27"),
                    "RED: per-page slot constant must be 3×9=27");
            assertTrue(src.contains("new MachineItemHandler(FILTER_SLOTS_PER_PAGE)"),
                    "RED: filter inventory must init with per-page constant (27 slots at pageLevel 0)");
        }

        @Test
        void filterContainerWrapsFilterInventoryDynamically() throws Exception {
            var src = readSource(PIPE_SRC);
            // Container 槽数/读写全部委托 filterInventory，槽数变化自动跟随
            assertTrue(src.contains("return filterInventory.getSlots();"),
                    "RED: filter container size must delegate to filterInventory.getSlots()");
            assertTrue(src.contains("return filterInventory.getStackInSlot(slot);"),
                    "RED: filter container getItem must delegate to filterInventory");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 2. GUI：3×9 网格 + 模式标题
    // ════════════════════════════════════════════════════════════════

    @Nested
    class GuiLayout {

        @Test
        void createUiRendersPerPage3x9Grid() throws Exception {
            var src = readSource(PIPE_SRC);
            String body = methodBody(src, "public ModularUI createUI");
            // 每页 3×9 网格：列 = i % 9，行 = i / 9（三行 y = 24 / 42 / 60）
            assertTrue(body.contains("(i % 9) * 18"),
                    "RED: createUI must position columns by (i % 9) * 18");
            assertTrue(body.contains("(i / 9) * 18"),
                    "RED: createUI must position rows by (i / 9) * 18 (3 rows × 9 cols)");
            // 每页遍历 FILTER_SLOTS_PER_PAGE 槽；页循环覆盖全部页（总槽 = 27×页数）
            assertTrue(body.contains("i < FILTER_SLOTS_PER_PAGE"),
                    "RED: createUI must iterate FILTER_SLOTS_PER_PAGE slots per page");
            assertTrue(body.contains("filterInventory.getSlots()"),
                    "RED: createUI must allocate slots for the full filter inventory");
        }

        @Test
        void createUiShowsModeTitle() throws Exception {
            var src = readSource(PIPE_SRC);
            String body = methodBody(src, "public ModularUI createUI");
            // 标题按方块种类标注白/黑名单模式
            assertTrue(body.contains("kenergyengineering.pipe.filter.whitelist"),
                    "RED: createUI must reference whitelist title key");
            assertTrue(body.contains("kenergyengineering.pipe.filter.blacklist"),
                    "RED: createUI must reference blacklist title key");
            assertTrue(body.contains("isWhitelist()"),
                    "RED: createUI must pick title by isWhitelist()");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 3. 持久化：readTileData/writeTileData 写入过滤槽
    // ════════════════════════════════════════════════════════════════

    @Nested
    class Persistence {

        @Test
        void writeTileDataStoresFilterSlots() throws Exception {
            var src = readSource(PIPE_SRC);
            String body = methodBody(src, "protected void writeTileData");
            assertTrue(body.contains("output.store(FILTER_KEY, Codec.list(ItemStack.OPTIONAL_CODEC), stacks)"),
                    "RED: writeTileData must store the 27 filter slots via " +
                    "Codec.list(ItemStack.OPTIONAL_CODEC) (BE CompoundTag standard).");
            assertTrue(body.contains("i < filterInventory.getSlots()"),
                    "RED: writeTileData must iterate all filter slots");
        }

        @Test
        void readTileDataRestoresFilterSlots() throws Exception {
            var src = readSource(PIPE_SRC);
            String body = methodBody(src, "protected void readTileData");
            assertTrue(body.contains("input.read(FILTER_KEY, Codec.list(ItemStack.OPTIONAL_CODEC))"),
                    "RED: readTileData must read filter slots via " +
                    "Codec.list(ItemStack.OPTIONAL_CODEC).");
            assertTrue(body.contains("filterInventory.setStackInSlot(i, stacks.get(i))"),
                    "RED: readTileData must write restored stacks back into filterInventory.");
        }

        @Test
        void readAndWriteShareSameKey() throws Exception {
            var src = readSource(PIPE_SRC);
            String readBody = methodBody(src, "protected void readTileData");
            String writeBody = methodBody(src, "protected void writeTileData");
            // 同一存储键 + 同一 codec 才能往返一致；键为共享常量，且常量字面值为 "filter"
            String keyToken = "FILTER_KEY, Codec.list(ItemStack.OPTIONAL_CODEC)";
            assertTrue(readBody.contains(keyToken), "RED: readTileData must use key/codec token " + keyToken);
            assertTrue(writeBody.contains(keyToken), "RED: writeTileData must use key/codec token " + keyToken);
            assertTrue(src.contains("FILTER_KEY = \"filter\""),
                    "RED: shared FILTER_KEY constant must be defined with value \"filter\"");
        }

        @Test
        void missingKeyLeavesFilterEmpty() throws Exception {
            var src = readSource(PIPE_SRC);
            String body = methodBody(src, "protected void readTileData");
            // 缺键/旧存档 → Optional.ifPresent 短路，过滤保持空（不炸旧存档）
            assertTrue(body.contains(".ifPresent(stacks ->"),
                    "RED: readTileData must use Optional.ifPresent so missing key is a no-op");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 4. 过滤逻辑：白名单=匹配放行、黑名单=不匹配放行、空标记物忽略
    // ════════════════════════════════════════════════════════════════

    @Nested
    class FilterLogic {

        @Test
        void plainPipeAllowsEverything() throws Exception {
            var src = readSource(PIPE_SRC);
            String body = methodBody(src, "private boolean isItemAllowed");
            assertTrue(body.contains("if (!isFiltered()) {\n            return true;\n        }") ||
                            body.contains("return true;"),
                    "RED: non-filtered pipe must allow all items");
        }

        @Test
        void whitelistAllowsMatchedBlacklistAllowsUnmatched() throws Exception {
            var src = readSource(PIPE_SRC);
            String body = methodBody(src, "private boolean isItemAllowed");
            assertTrue(body.contains("isWhitelist() ? matched : !matched"),
                    "RED: whitelist → matched allows, blacklist → unmatched allows");
            assertTrue(body.contains("ItemStack.isSameItemSameComponents(filter, stack)"),
                    "RED: match must compare item + data components");
        }

        @Test
        void emptyFilterMarkersAreIgnored() throws Exception {
            var src = readSource(PIPE_SRC);
            String body = methodBody(src, "private boolean isItemAllowed");
            assertTrue(body.contains("!filter.isEmpty()"),
                    "RED: empty marker slots must be skipped in matching");
        }

        @Test
        void hasUiKeepsFilteredOnly() throws Exception {
            var src = readSource(PIPE_SRC);
            String body = methodBody(src, "public boolean hasUi");
            // 普通 pipe 无过滤逻辑 → 无 UI；仅 pipe_white/pipe_black（isFiltered）可开配置
            assertTrue(body.contains("isFiltered()"),
                    "RED: hasUi must stay gated by isFiltered() (plain pipe has no UI)");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 5. 交互：潜行右键打开配置 GUI；Spanner 潜行=拆卸优先
    // ════════════════════════════════════════════════════════════════

    @Nested
    class Interaction {

        @Test
        void pipeUiRequiresShiftInUseWithoutItem() throws Exception {
            var src = readSource(BASE_SRC);
            String body = methodBody(src, "protected InteractionResult useWithoutItem");
            assertTrue(body.contains("!pipe.hasUi() || !player.isShiftKeyDown()"),
                    "RED: empty-hand pipe GUI must require shift (潜行右键打开配置)");
        }

        @Test
        void pipeUiRequiresShiftInUseItemOn() throws Exception {
            var src = readSource(BASE_SRC);
            String body = methodBody(src, "protected InteractionResult useItemOn");
            assertTrue(body.contains("!pipe.hasUi() || !player.isShiftKeyDown()"),
                    "RED: item-hand pipe GUI must require shift (潜行右键打开配置)");
        }

        @Test
        void spannerNotSpecialCasedInUseItemOn() throws Exception {
            var src = readSource(BASE_SRC);
            String body = methodBody(src, "protected InteractionResult useItemOn");
            // Spanner 潜行=拆卸/非潜行=旋转由 stack.useOn 先行 CONSUME，
            // useItemOn 不得再特判 Spanner —— 否则拆卸/旋转与配置 GUI 冲突。
            assertFalse(body.contains("instanceof SpannerItem"),
                    "RED: useItemOn must not special-case SpannerItem (forwarding already handles it)");
            assertTrue(body.contains("stack.useOn(new UseOnContext(level, player, hand, stack, hit))"),
                    "RED: useItemOn must forward stack.useOn for every item first");
        }
    }
}
