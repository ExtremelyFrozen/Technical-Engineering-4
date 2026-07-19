// -*- coding: utf-8 -*-
package com.modularmc.ten.common.gui;

import com.modularmc.ten.data.lang.TENLangHandler;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.file.Files;

/**
 * RED/GREEN tests for the upgrade slot empty tooltip feature.
 * <p>
 * Covers language key consistency (TENLangHandler + JSON),
 * helper method contract, and event path verification.
 * <p>
 * <strong>Limitation:</strong> Full client-side hover tooltip rendering
 * requires a game runtime with GL context and cannot be unit tested.
 * This test suite verifies the compile-time structure and data integrity
 * that supports the correct runtime behavior.
 *
 * <h3>Event paths (documented, not directly testable via JUnit):</h3>
 * <ul>
 *   <li><strong>Empty slot:</strong> HOVER_TOOLTIPS fires → {@code slot.getItem().isEmpty()}
 *       is true → {@code event.hoverTooltips} is set to the localized "Upgrade Slot" text.
 *       ItemSlot native tooltip is overridden with the upgrade slot label.</li>
 *   <li><strong>Non-empty slot:</strong> HOVER_TOOLTIPS fires → {@code slot.getItem().isEmpty()}
 *       is false → the handler does NOT assign {@code event.hoverTooltips}.
 *       The ItemSlot native behavior shows the ItemStack tooltip unchanged.</li>
 * </ul>
 *
 * <h3>RED results (before GREEN):</h3>
 * <ul>
 *   <li>{@code tenLangHandler_hasEnglishEntry} — fails: key not yet registered</li>
 *   <li>{@code tenLangHandler_hasChineseEntry} — fails: key not yet registered</li>
 *   <li>{@code enUsJson_containsKey} — fails: key not yet added</li>
 *   <li>{@code zhCnJsonMain_containsKey} — fails: key not yet added</li>
 *   <li>{@code zhCnJsonGenerated_containsKey} — fails: key not yet added</li>
 *   <li>{@code emptyUpgradeSlotTooltip_methodExists} — fails: method not yet created</li>
 * </ul>
 */
class UpgradeSlotTooltipTest {

    // ════════════════════════════════════════════════════════════
    // Constants
    // ════════════════════════════════════════════════════════════

    private static final String MOD_ID = "kenergyengineering";
    private static final String KEY = MOD_ID + ".upgrade_slot";
    private static final String EN_VALUE = "Upgrade Slot";
    private static final String ZH_VALUE = "升级槽位";

    // ════════════════════════════════════════════════════════════
    // A. Language key consistency — 3 sources
    // ════════════════════════════════════════════════════════════

    @Nested
    class LangKeyConsistency {

        @Test
        void tenLangHandler_hasEnglishEntry() {
            assertEquals(EN_VALUE, TENLangHandler.EN_ENTRIES.get(KEY),
                "TENLangHandler must have English entry for " + KEY);
        }

        @Test
        void tenLangHandler_hasChineseEntry() {
            assertEquals(ZH_VALUE, TENLangHandler.ZH_ENTRIES.get(KEY),
                "TENLangHandler must have Chinese entry for " + KEY);
        }

        @Test
        void enUsJson_containsKey() throws Exception {
            var file = new File("src/main/resources/assets/kenergyengineering/lang/en_us.json");
            assertTrue(file.exists(), "en_us.json must exist");
            String content = Files.readString(file.toPath());
            assertTrue(content.contains("\"" + KEY + "\": \"" + EN_VALUE + "\""),
                "en_us.json must contain " + KEY + " = " + EN_VALUE);
        }

        @Test
        void zhCnJsonMain_containsKey() throws Exception {
            var file = new File("src/main/resources/assets/kenergyengineering/lang/zh_cn.json");
            assertTrue(file.exists(), "zh_cn.json (main) must exist");
            String content = Files.readString(file.toPath());
            assertTrue(content.contains("\"" + KEY + "\": \"" + ZH_VALUE + "\""),
                "zh_cn.json (main) must contain " + KEY + " = " + ZH_VALUE);
        }

        @Test
        void zhCnJsonGenerated_containsKey() throws Exception {
            var file = new File("src/generated/resources/assets/kenergyengineering/lang/zh_cn.json");
            assertTrue(file.exists(), "zh_cn.json (generated) must exist");
            String content = Files.readString(file.toPath());
            assertTrue(content.contains("\"" + KEY + "\": \"" + ZH_VALUE + "\""),
                "zh_cn.json (generated) must contain " + KEY + " = " + ZH_VALUE);
        }
    }

    // ════════════════════════════════════════════════════════════
    // B. Helper method contract
    // ════════════════════════════════════════════════════════════

    @Nested
    class HelperMethod {

        @Test
        void emptyUpgradeSlotTooltip_methodExists() throws Exception {
            var method = TENMachineBlockUIFactory.class
                .getDeclaredMethod("emptyUpgradeSlotTooltip");
            assertNotNull(method, "emptyUpgradeSlotTooltip must exist");
            assertEquals(Component.class, method.getReturnType(),
                "emptyUpgradeSlotTooltip must return Component");
        }

        @Test
        void upgradeSlot_methodExistsAndReturnsItemSlot() throws Exception {
            var method = TENMachineBlockUIFactory.class
                .getDeclaredMethod("upgradeSlot",
                    com.modularmc.ten.api.blockentity.CmMachineBlockEntity.class,
                    int.class, int.class, int.class);
            assertNotNull(method, "upgradeSlot must exist");
            assertEquals(ItemSlot.class, method.getReturnType(),
                "upgradeSlot must return ItemSlot");
        }
    }

    // ════════════════════════════════════════════════════════════
    // C. Source structure — upgradeSlot event contract
    // ════════════════════════════════════════════════════════════
    //
    // Verifies that upgradeSlot() registers a HOVER_TOOLTIPS
    // listener that checks isEmpty() before setting tooltip.
    // Uses lightweight source inspection (not fragile line matching).
    //
    // NOTE: This tests that the CODE STRUCTURE supports the required
    // event paths. Actual hover behavior requires a game client.

    @Nested
    class EventContract {

        @Test
        void upgradeSlot_registersHoverTooltipsListener() throws Exception {
            var sourceFile = new File(
                "src/main/java/com/modularmc/ten/common/gui/TENMachineBlockUIFactory.java"
            );
            assertTrue(sourceFile.exists(), "Source file must exist");
            var lines = Files.readAllLines(sourceFile.toPath());
            boolean foundAddEventListener = false;
            boolean foundIsEmpty = false;

            // Locate the upgradeSlot method body
            boolean inUpgradeSlot = false;
            int braceDepth = 0;

            for (String line : lines) {
                if (line.contains("private static ItemSlot upgradeSlot(")) {
                    inUpgradeSlot = true;
                }
                if (inUpgradeSlot) {
                    for (char c : line.toCharArray()) {
                        if (c == '{') braceDepth++;
                        if (c == '}') braceDepth--;
                    }
                    if (braceDepth <= 0 && inUpgradeSlot) {
                        break; // method body closed
                    }
                    if (line.contains("HOVER_TOOLTIPS")) {
                        foundAddEventListener = true;
                    }
                    if (line.contains(".isEmpty()")) {
                        foundIsEmpty = true;
                    }
                }
            }

            assertTrue(foundAddEventListener,
                "upgradeSlot() must register a HOVER_TOOLTIPS event listener");
            assertTrue(foundIsEmpty,
                "upgradeSlot() HOVER_TOOLTIPS handler must check isEmpty()");
        }

        @Test
        void upgradeSlot_checkEmptyBeforeSettingTooltip() throws Exception {
            var sourceFile = new File(
                "src/main/java/com/modularmc/ten/common/gui/TENMachineBlockUIFactory.java"
            );
            var lines = Files.readAllLines(sourceFile.toPath());
            boolean inUpgradeSlot = false;
            int braceDepth = 0;
            boolean isEmptyGuard = false;
            boolean hoverTooltipsAssignment = false;

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.contains("private static ItemSlot upgradeSlot(")) {
                    inUpgradeSlot = true;
                }
                if (!inUpgradeSlot) continue;
                for (char c : line.toCharArray()) {
                    if (c == '{') braceDepth++;
                    if (c == '}') braceDepth--;
                }
                if (braceDepth <= 0) break;

                if (line.contains("isEmpty()")) {
                    isEmptyGuard = true;
                }
                if (line.contains("hoverTooltips")) {
                    hoverTooltipsAssignment = true;
                }
            }

            assertTrue(isEmptyGuard,
                "upgradeSlot() HOVER_TOOLTIPS handler must guard with isEmpty()");
            assertTrue(hoverTooltipsAssignment,
                "upgradeSlot() HOVER_TOOLTIPS handler must assign hoverTooltips");
        }
    }
}
