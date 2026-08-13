// -*- coding: utf-8 -*-
package com.modularmc.ten.common.blockentity;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 偏差 #2 修复契约：管道独特升级（升级状态 + 右键升级交互）。
 * <p>
 * 用户设计：
 * <ul>
 *   <li>五种管道独特升级：拉取(粘性活塞,限1)/推入(活塞,限1)/速度(糖,限9)/扩写(书,限63)/末影(末影珍珠,限8)</li>
 *   <li>升级状态持久化于 PipeBlockEntity（readTileData/writeTileData），缺键/旧存档 → 0</li>
 *   <li>非潜行右键管道手持升级物 → 升级 +1、消耗 1 物品、反馈玩家；已达上限 → 不消耗提示</li>
 *   <li>粘性活塞/活塞是 BlockItem，须在 stack.useOn（放置）之前拦截；潜行右键仍开配置 GUI</li>
 * </ul>
 * <p>
 * 使用源码模式扫描（项目既有约定）——PipeBlockEntity 依赖 MC bootstrap 无法在
 * 纯 JUnit 中实例化；语义断言锚定源码结构，避免运行时引导。
 */
class PipeUpgradeContractTest {

    private static final String TYPE_SRC = "com/modularmc/ten/common/blockentity/PipeUpgradeType.java";
    private static final String PIPE_SRC = "com/modularmc/ten/common/blockentity/PipeBlockEntity.java";
    private static final String BASE_SRC = "com/modularmc/ten/common/block/machine/BaseMachineBlock.java";
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
    // 1. 升级类型：五种类型 + 材料 + 上限（单一事实来源）
    // ════════════════════════════════════════════════════════════════

    @Nested
    class UpgradeType {

        @Test
        void enumDefinesFiveTypes() throws Exception {
            var src = readSource(TYPE_SRC);
            assertTrue(src.contains("PULL(Items.STICKY_PISTON, 1"),
                    "RED: PULL must map sticky piston, cap 1");
            assertTrue(src.contains("PUSH(Items.PISTON, 1"),
                    "RED: PUSH must map piston, cap 1");
            assertTrue(src.contains("SPEED(Items.SUGAR, 9"),
                    "RED: SPEED must map sugar, cap 9");
            assertTrue(src.contains("PAGE(Items.BOOK, 63"),
                    "RED: PAGE must map book, cap 63");
            assertTrue(src.contains("ENDER(Items.ENDER_PEARL, 8"),
                    "RED: ENDER must map ender pearl, cap 8");
        }

        @Test
        void fromItemResolvesByMaterial() throws Exception {
            var src = readSource(TYPE_SRC);
            String body = methodBody(src, "public static PipeUpgradeType fromItem");
            assertTrue(body.contains("type.material == item"),
                    "RED: fromItem must resolve by material identity");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 2. 升级状态：持久化 + canUpgrade/doUpgrade
    // ════════════════════════════════════════════════════════════════

    @Nested
    class UpgradeState {

        @Test
        void levelsArePersistedFields() throws Exception {
            var src = readSource(PIPE_SRC);
            for (String field : new String[]{"pullLevel", "pushLevel", "speedLevel", "pageLevel", "enderLevel"}) {
                assertTrue(src.contains("private int " + field + ";"),
                        "RED: persisted field missing: " + field);
            }
        }

        @Test
        void readTileDataDefaultsMissingLevelsToZero() throws Exception {
            var src = readSource(PIPE_SRC);
            String body = methodBody(src, "protected void readTileData");
            for (String field : new String[]{"pullLevel", "pushLevel", "speedLevel", "pageLevel", "enderLevel"}) {
                assertTrue(body.contains("input.getInt(\"" + field + "\").orElse(0)"),
                        "RED: readTileData must restore " + field + " with orElse(0) (old saves stay at 0)");
            }
        }

        @Test
        void writeTileDataStoresLevels() throws Exception {
            var src = readSource(PIPE_SRC);
            String body = methodBody(src, "protected void writeTileData");
            for (String field : new String[]{"pullLevel", "pushLevel", "speedLevel", "pageLevel", "enderLevel"}) {
                assertTrue(body.contains("output.store(\"" + field + "\", Codec.INT, " + field + ")"),
                        "RED: writeTileData must store " + field + " via Codec.INT");
            }
        }

        @Test
        void canUpgradeComparesAgainstMax() throws Exception {
            var src = readSource(PIPE_SRC);
            String body = methodBody(src, "public boolean canUpgrade");
            assertTrue(body.contains("getUpgradeLevel(type) < type.maxLevel()"),
                    "RED: canUpgrade must compare current level against type maxLevel");
        }

        @Test
        void doUpgradeIncrementsAndFailsAtCap() throws Exception {
            var src = readSource(PIPE_SRC);
            String body = methodBody(src, "public boolean doUpgrade");
            assertTrue(body.contains("getUpgradeLevel(type) + 1"),
                    "RED: doUpgrade must increment level by 1");
            assertTrue(body.contains("markDirty()"),
                    "RED: doUpgrade must mark dirty to persist the level");
            assertTrue(body.contains("return false;"),
                    "RED: doUpgrade must return false when at cap (no state change)");
        }

        @Test
        void getUpgradeLevelSwitchesOverAllTypes() throws Exception {
            var src = readSource(PIPE_SRC);
            String body = methodBody(src, "public int getUpgradeLevel");
            for (String field : new String[]{"pullLevel", "pushLevel", "speedLevel", "pageLevel", "enderLevel"}) {
                assertTrue(body.contains("-> " + field + ";"),
                        "RED: getUpgradeLevel must map a case to " + field);
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 3. 右键交互：优先拦截 + 消耗 + 反馈
    // ════════════════════════════════════════════════════════════════

    @Nested
    class Interaction {

        @Test
        void useItemOnInterceptsPipeUpgradeBeforeItemFirst() throws Exception {
            var src = readSource(BASE_SRC);
            String body = methodBody(src, "protected InteractionResult useItemOn");
            assertTrue(body.contains("pipe.tryApplyUpgrade(stack, player)"),
                    "RED: useItemOn must delegate to PipeBlockEntity.tryApplyUpgrade");
            assertTrue(body.contains("!player.isShiftKeyDown()"),
                    "RED: upgrade must be gated on non-shift (shift keeps config GUI)");
            // 拦截必须先于 stack.useOn（放置），否则 BlockItem（活塞）会先被放置
            int upgradeIdx = body.indexOf("pipe.tryApplyUpgrade(stack, player)");
            int itemFirstIdx = body.indexOf("stack.useOn(new UseOnContext");
            assertTrue(upgradeIdx >= 0 && itemFirstIdx > upgradeIdx,
                    "RED: pipe upgrade interception must come before stack.useOn (BlockItem placement)");
        }

        @Test
        void tryApplyUpgradeResolvesTypeFromHandItem() throws Exception {
            var src = readSource(PIPE_SRC);
            String body = methodBody(src, "public InteractionResult tryApplyUpgrade");
            assertTrue(body.contains("PipeUpgradeType.fromItem(stack.getItem())"),
                    "RED: tryApplyUpgrade must resolve upgrade type from held item");
            assertTrue(body.contains("return InteractionResult.PASS;"),
                    "RED: non-upgrade items must PASS through to original logic");
        }

        @Test
        void tryApplyUpgradeConsumesAndNotifiesPlayer() throws Exception {
            var src = readSource(PIPE_SRC);
            String body = methodBody(src, "public InteractionResult tryApplyUpgrade");
            assertTrue(body.contains("stack.shrink(1)"),
                    "RED: successful upgrade must consume 1 item");
            assertTrue(body.contains("hasInfiniteMaterials()"),
                    "RED: creative must not consume items");
            assertTrue(body.contains("sendSystemMessage"),
                    "RED: player must receive feedback message");
            assertTrue(body.contains("pipe.upgrade.success"),
                    "RED: success message key must be used");
            assertTrue(body.contains("pipe.upgrade.max"),
                    "RED: max-level message key must be used");
            assertTrue(body.contains("InteractionResult.CONSUME"),
                    "RED: upgrade interaction must CONSUME (block placement, no further logic)");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 4. 语言键：handler 注册 + generated json 同步
    // ════════════════════════════════════════════════════════════════

    @Nested
    class LangKeys {

        @Test
        void handlerRegistersAllPipeUpgradeKeys() throws Exception {
            var src = readSource(LANG_SRC);
            assertTrue(src.contains("addPipeUpgrade()"),
                    "RED: TENLangHandler static block must invoke addPipeUpgrade()");
            for (String suffix : new String[]{"pull", "push", "speed", "page", "ender"}) {
                assertTrue(src.contains("add(\"pipe.upgrade." + suffix + "\""),
                        "RED: missing type name key pipe.upgrade." + suffix);
            }
            assertTrue(src.contains("add(\"pipe.upgrade.success\""),
                    "RED: missing success message key");
            assertTrue(src.contains("add(\"pipe.upgrade.max\""),
                    "RED: missing max-level message key");
        }

        @Test
        void generatedLangFilesContainKeys() throws Exception {
            String en = readFile(EN_JSON);
            String zh = readFile(ZH_JSON);
            assertTrue(en.contains("\"kenergyengineering.pipe.upgrade.success\""),
                    "RED: en_us.json must contain pipe.upgrade.success");
            assertTrue(zh.contains("\"kenergyengineering.pipe.upgrade.success\""),
                    "RED: zh_cn.json must contain pipe.upgrade.success");
            assertTrue(zh.contains("\"kenergyengineering.pipe.upgrade.max\": \"%s 升级已达上限\""),
                    "RED: zh_cn.json must contain Chinese max-level message");
        }
    }
}
