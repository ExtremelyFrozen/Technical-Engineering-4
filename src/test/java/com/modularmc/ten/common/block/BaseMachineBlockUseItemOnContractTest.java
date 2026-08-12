// -*- coding: utf-8 -*-
package com.modularmc.ten.common.block;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract tests for {@code BaseMachineBlock.useItemOn} item-forwarding.
 * <p>
 * Root cause: useItemOn only forwarded {@code stack.useOn} for SpannerItem;
 * other items (e.g. ChannelConnectorItem) fell straight into the
 * upgrade/fluid/GUI branches, making ChannelConnectorItem.useOn unreachable
 * on channel blocks.
 * <p>
 * FIX: useItemOn forwards {@code stack.useOn} for every item first; only
 * PASS/FAIL fall through to the legacy upgrade/fluid/GUI flow.
 * <p>
 * Uses source-code pattern scanning (established project convention) since
 * {@link net.minecraft.world.level.block.Block} cannot be instantiated in
 * unit tests without MC bootstrap.
 */
class BaseMachineBlockUseItemOnContractTest {

    private static final String SRC_PATH =
            "com/modularmc/ten/common/block/machine/BaseMachineBlock.java";

    static String readSource() throws Exception {
        var f = new File("src/main/java/" + SRC_PATH);
        assertTrue(f.exists(), "Source file must exist: " + SRC_PATH);
        return Files.readString(f.toPath());
    }

    static String methodBody(String src, String methodHeader) throws Exception {
        int methodIdx = src.indexOf(methodHeader);
        assertTrue(methodIdx >= 0, "Method must exist: " + methodHeader);
        int bodyStart = src.indexOf('{', methodIdx);
        int bodyEnd = findMatchingBrace(src, bodyStart);
        return src.substring(bodyStart, bodyEnd);
    }

    // ════════════════════════════════════════════════════════════════════
    // 1. useItemOn forwards stack.useOn for ALL items (not just SpannerItem)
    // ════════════════════════════════════════════════════════════════════

    @Nested
    class ItemForwarding {

        @Test
        void useItemOn_forward_stack_useOn_for_every_item() throws Exception {
            var src = readSource();
            String body = methodBody(src, "protected InteractionResult useItemOn");

            // Must call stack.useOn with the interaction context unconditionally
            // (outside any instanceof SpannerItem guard).
            assertTrue(body.contains("stack.useOn(new UseOnContext(level, player, hand, stack, hit))"),
                    "RED: useItemOn must forward stack.useOn for every item. " +
                    "FIX: call stack.useOn(new UseOnContext(level, player, hand, stack, hit)) " +
                    "at the top of useItemOn, outside item-type guards.");
        }

        @Test
        void useItemOn_no_spanner_special_case_inside_method() throws Exception {
            var src = readSource();
            String body = methodBody(src, "protected InteractionResult useItemOn");

            // The old code guarded stack.useOn behind instanceof SpannerItem.
            // After the fix the guard must be gone from useItemOn (useWithoutItem
            // keeps its own Spanner handling for the empty-hand path).
            assertFalse(body.contains("instanceof SpannerItem"),
                    "RED: useItemOn must not special-case SpannerItem anymore. " +
                    "FIX: forward stack.useOn for every item, Spanner included.");
        }

        @Test
        void useItemOn_falls_through_only_on_PASS_or_FAIL() throws Exception {
            var src = readSource();
            String body = methodBody(src, "protected InteractionResult useItemOn");

            // Non-PASS results must be returned directly; FAIL must still fall
            // through so BlockItem placement failure reaches the machine UI and
            // filled-bucket placement failure reaches the tank interaction.
            assertTrue(body.contains("itemResult != InteractionResult.PASS && itemResult != InteractionResult.FAIL"),
                    "RED: useItemOn must return directly on non-PASS/non-FAIL results. " +
                    "FIX: if (itemResult != InteractionResult.PASS && itemResult != InteractionResult.FAIL) return itemResult;");
        }

        @Test
        void useItemOn_keeps_legacy_fallback_branches() throws Exception {
            var src = readSource();
            String body = methodBody(src, "protected InteractionResult useItemOn");

            // Upgrade quick-install, fluid interaction and GUI branches must survive.
            assertTrue(body.contains("UpgradeInstallHelper.tryInstall"),
                    "RED: upgrade quick-install branch must remain after the fix.");
            assertTrue(body.contains("FluidUtil.interactWithFluidHandler"),
                    "RED: fluid interaction branch must remain after the fix.");
            assertTrue(body.contains("BlockUIMenuType.openUI"),
                    "RED: GUI branch must remain after the fix.");
        }
    }

    // ════════════════════════════════════════════════════════════════════

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
}
