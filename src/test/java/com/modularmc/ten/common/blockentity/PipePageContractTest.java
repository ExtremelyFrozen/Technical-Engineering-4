// -*- coding: utf-8 -*-
package com.modularmc.ten.common.blockentity;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 偏差 #3 修复契约：Pipe 扩写升级（PAGE）多页过滤 GUI + 翻页。
 * <p>
 * 用户设计：
 * <ul>
 *   <li>过滤槽从单页 27 槽扩为总槽 = 27×(1+pageLevel)（页数 = 1+pageLevel，上限 64 页 = 1728 槽）</li>
 *   <li>createUI 每页显示 27 槽（3×9 网格 x=7+(i%9)*18、y=24+(i/9)*18 不变），翻页只切当前页显示</li>
 *   <li>翻页按钮 ▲▼（复用 TENConstants.SCROLL_UP/DOWN_NORMAL/HOVER 素材，channel_buttons sheet），
 *       标题行右侧水平排列（3×9 网格占满 x=7..169 无右侧纵向空间）；页码 Label 显示「第 x/共 y 页」</li>
 *   <li>翻页状态为 UI 浏览态，不持久化（与频道列表 ChannelUIState 同款：每次打开 GUI 从第 1 页开始）</li>
 *   <li>过滤匹配 isItemAllowed 遍历全部页槽（容量扩容后 filterInventory.getSlots() 自动覆盖）</li>
 * </ul>
 * <p>
 * 使用源码模式扫描（项目既有约定）——PipeBlockEntity 依赖 MC bootstrap 无法在
 * 纯 JUnit 中实例化；语义断言锚定源码结构，避免运行时引导。
 */
class PipePageContractTest {

    private static final String PIPE_SRC = "com/modularmc/ten/common/blockentity/PipeBlockEntity.java";
    private static final String LANG_SRC = "com/modularmc/ten/data/lang/TENLangHandler.java";
    private static final String EN_JSON = "src/generated/resources/assets/kenergyengineering/lang/en_us.json";
    private static final String ZH_JSON = "src/generated/resources/assets/kenergyengineering/lang/zh_cn.json";

    static String readSource(String relativePath) throws Exception {
        var f = new File("src/main/java/" + relativePath);
        assertTrue(f.exists(), "Source file must exist: " + relativePath);
        return Files.readString(f.toPath());
    }

    static String readFile(String path) throws Exception {
        var f = new File(path);
        assertTrue(f.exists(), "File must exist: " + path);
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
    // 1. 页数与容量：页数 = 1+pageLevel；总槽 = 27×页数（最多 64 页 = 1728 槽）
    // ════════════════════════════════════════════════════════════════

    @Nested
    class PageCountAndCapacity {

        @Test
        void pageCountIsOnePlusPageLevel() throws Exception {
            var src = readSource(PIPE_SRC);
            String body = methodBody(src, "public int getFilterPageCount");
            assertTrue(body.contains("1 + pageLevel"),
                    "RED: page count must be 1 + pageLevel (0 upgrades → 1 page, 63 → 64 pages)");
        }

        @Test
        void totalSlotsArePerPageTimesPageCount() throws Exception {
            var src = readSource(PIPE_SRC);
            String body = methodBody(src, "private int filterSlotCount");
            assertTrue(body.contains("FILTER_SLOTS_PER_PAGE * getFilterPageCount()"),
                    "RED: total slots must be 27 × page count (max 64×27 = 1728)");
        }

        @Test
        void pageUpgradeResizesInventory() throws Exception {
            var src = readSource(PIPE_SRC);
            String body = methodBody(src, "private void setUpgradeLevel");
            // PAGE 分支：升级后容量随页数扩容（filterInventory.setSize），新页槽立即可用
            assertTrue(body.contains("case PAGE -> {"),
                    "RED: PAGE upgrade branch must resize the filter inventory");
            assertTrue(body.contains("filterInventory.setSize(filterSlotCount())"),
                    "RED: PAGE upgrade must resize inventory to 27 × (1 + pageLevel)");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 2. 持久化：pageLevel 先于 filter 恢复（决定容量），容量 = 27×(1+pageLevel)
    // ════════════════════════════════════════════════════════════════

    @Nested
    class Persistence {

        @Test
        void readTileDataRestoresPageLevelBeforeFilter() throws Exception {
            var src = readSource(PIPE_SRC);
            String body = methodBody(src, "protected void readTileData");
            // 先恢复 pageLevel 并 setSize，再读 filter —— 否则旧容量 27 会截断多页存档
            int pageLevelIdx = body.indexOf("input.getInt(\"pageLevel\").orElse(0)");
            int resizeIdx = body.indexOf("filterInventory.setSize(filterSlotCount())");
            int filterIdx = body.indexOf("input.read(FILTER_KEY");
            assertTrue(pageLevelIdx >= 0, "RED: readTileData must restore pageLevel");
            assertTrue(resizeIdx > pageLevelIdx,
                    "RED: readTileData must resize inventory after restoring pageLevel");
            assertTrue(filterIdx > resizeIdx,
                    "RED: readTileData must read filter slots only after resize (full capacity)");
        }

        @Test
        void writeTileDataStoresAllPages() throws Exception {
            var src = readSource(PIPE_SRC);
            String body = methodBody(src, "protected void writeTileData");
            assertTrue(body.contains("i < filterInventory.getSlots()"),
                    "RED: writeTileData must persist all page slots (27 × page count)");
        }

        @Test
        void pageBrowseStateIsNotPersisted() throws Exception {
            var src = readSource(PIPE_SRC);
            // 翻页浏览态是 UI 局部状态：不写存档（无 currentPage 存储键），避免污染存档
            assertFalse(src.contains("output.store(\"currentPage\""),
                    "RED: current page is UI browse state — must NOT be persisted");
            assertFalse(src.contains("input.getInt(\"currentPage\""),
                    "RED: current page is UI browse state — must NOT be read from save");
        }

        @Test
        void pageBrowseStateResetsPerGuiOpen() throws Exception {
            var src = readSource(PIPE_SRC);
            String body = methodBody(src, "public ModularUI createUI");
            // currentPage 为 createUI 局部状态（每次打开 GUI 从 0 = 第 1 页开始，与频道列表同款）
            // 空白规范化后匹配：容忍 spotless 的 `{ 0 }` 数组初始化间距，锁定「局部声明 + 初值 0」语义
            String normalized = body.replaceAll("\\s+", " ");
            assertTrue(normalized.contains("int[] currentPage = { 0 }"),
                    "RED: current page must be createUI-local state initialized to page 0");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 3. 翻页 UI：▲▼ 按钮 + 页码 Label + 边界钳制 + 无重叠坐标
    // ════════════════════════════════════════════════════════════════

    @Nested
    class PageControls {

        @Test
        void scrollButtonsUseChannelSheetSprites() throws Exception {
            var src = readSource(PIPE_SRC);
            assertTrue(src.contains("buttonStyle"),
                    "RED: page buttons must use LDLib2 Button.buttonStyle");
            assertTrue(src.contains("baseTexture") && src.contains("hoverTexture") && src.contains("pressedTexture"),
                    "RED: buttonStyle must define base/hover/pressed textures");
            assertTrue(src.contains("SCROLL_UP_NORMAL") && src.contains("SCROLL_UP_HOVER"),
                    "RED: prev (▲) must reference SCROLL_UP normal/hover UV pair");
            assertTrue(src.contains("SCROLL_DOWN_NORMAL") && src.contains("SCROLL_DOWN_HOVER"),
                    "RED: next (▼) must reference SCROLL_DOWN normal/hover UV pair");
            assertTrue(src.contains("CHANNEL_BUTTONS"),
                    "RED: buttons must source from channel_buttons sheet");
        }

        @Test
        void pageLabelUsesLocalizedKey() throws Exception {
            var src = readSource(PIPE_SRC);
            String createBody = methodBody(src, "public ModularUI createUI");
            assertTrue(createBody.contains("pageText(currentPage[0], pageCount)"),
                    "RED: createUI must render the page label via pageText");
            String pageTextBody = methodBody(src, "private static Component pageText");
            assertTrue(pageTextBody.contains("pipe.filter.page"),
                    "RED: pageText must use localized key kenergyengineering.pipe.filter.page");
            assertTrue(pageTextBody.contains("currentPage + 1"),
                    "RED: pageText must show 1-based current page");
        }

        @Test
        void prevButtonClampsAtFirstPage() throws Exception {
            var src = readSource(PIPE_SRC);
            String body = methodBody(src, "public ModularUI createUI");
            // 上一页：currentPage > 0 才减（首页钳制，不越界）
            assertTrue(body.contains("currentPage[0] > 0"),
                    "RED: prev button must clamp at first page (currentPage > 0 guard)");
        }

        @Test
        void nextButtonClampsAtLastPage() throws Exception {
            var src = readSource(PIPE_SRC);
            String body = methodBody(src, "public ModularUI createUI");
            // 下一页：currentPage < pageCount-1 才加（末页钳制，不越界）
            assertTrue(body.contains("currentPage[0] < pageCount - 1"),
                    "RED: next button must clamp at last page (currentPage < pageCount-1 guard)");
        }

        @Test
        void pageTurnOnlyTogglesDisplay() throws Exception {
            var src = readSource(PIPE_SRC);
            String body = methodBody(src, "private static void refreshPage");
            // 翻页 = 显示切换：setDisplay(page == currentPage)，槽绑定/容量不变
            assertTrue(body.contains(".setDisplay(page == currentPage)"),
                    "RED: page turn must toggle slot display only");
            assertTrue(body.contains("pageLabel.setText(pageText(currentPage, pageCount))"),
                    "RED: page turn must refresh the page label");
        }

        @Test
        void controlsDoNotOverlapGridOrInventory() throws Exception {
            var src = readSource(PIPE_SRC);
            String createBody = methodBody(src, "public ModularUI createUI");
            // 坐标常量：按钮 y=6（12×12 → 6..18），网格顶 y=24，物品栏 y=83 → 无重叠；
            // x 150..175 在 GUI 176 宽内、网格右缘 169 处网格仅到 y≥24，标题行横带内无碰撞
            assertTrue(createBody.contains("PAGE_CONTROLS_Y") && createBody.contains("PREV_BUTTON_X") && createBody.contains("NEXT_BUTTON_X"),
                    "RED: createUI must use named coordinate constants for page controls");
            assertTrue(src.contains("PREV_BUTTON_X = 150") && src.contains("NEXT_BUTTON_X = 163"),
                    "RED: buttons must sit at x=150/163 (title row, right side, inside 176-wide GUI)");
            assertTrue(src.contains("PAGE_CONTROLS_Y = 6"),
                    "RED: buttons must sit at y=6 (12px tall → bottom 18 < grid top 24)");
            // 数值几何验证：按钮底缘 18 < 网格顶 24，按钮右缘 175 ≤ GUI 宽 176
            assertEquals(150 + 12, 162, "prev button right edge");
            assertEquals(163 + 12, 175, "next button right edge");
            assertTrue(6 + 12 < 24, "button bottom must stay above the 3×9 grid (y≥24)");
            assertTrue(175 <= 176, "next button right edge must stay inside the 176-wide GUI");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 4. 过滤匹配：遍历全部页槽（不只当前页）
    // ════════════════════════════════════════════════════════════════

    @Nested
    class FilterMatching {

        @Test
        void isItemAllowedScansAllPageSlots() throws Exception {
            var src = readSource(PIPE_SRC);
            String body = methodBody(src, "private boolean isItemAllowed");
            // 容量 = 27×(1+pageLevel) 后 getSlots() 即全部页槽：遍历即覆盖全部页，与 UI 当前页无关
            assertTrue(body.contains("i < filterInventory.getSlots()"),
                    "RED: isItemAllowed must iterate the full filter inventory (all pages)");
        }

        @Test
        void filterContainerSizeFollowsCapacity() throws Exception {
            var src = readSource(PIPE_SRC);
            String body = methodBody(src, "public int getContainerSize");
            assertTrue(body.contains("filterInventory.getSlots()"),
                    "RED: filter container size must delegate to filterInventory (follows 27 × pages)");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 5. 语言键：handler 注册 + generated json 同步
    // ════════════════════════════════════════════════════════════════

    @Nested
    class LangKeys {

        @Test
        void handlerRegistersPageKey() throws Exception {
            var src = readSource(LANG_SRC);
            assertTrue(src.contains("add(\"pipe.filter.page\""),
                    "RED: TENLangHandler must register pipe.filter.page");
            assertTrue(src.contains("第 %s/%s 页"),
                    "RED: zh_cn translation must be 第 %s/%s 页");
        }

        @Test
        void generatedLangFilesContainPageKey() throws Exception {
            String en = readFile(EN_JSON);
            String zh = readFile(ZH_JSON);
            assertTrue(en.contains("\"kenergyengineering.pipe.filter.page\""),
                    "RED: en_us.json must contain pipe.filter.page (run runData to regenerate)");
            assertTrue(zh.contains("\"kenergyengineering.pipe.filter.page\""),
                    "RED: zh_cn.json must contain pipe.filter.page (run runData to regenerate)");
        }
    }
}
