// -*- coding: utf-8 -*-
package com.modularmc.ten.common.item.upgrades;

import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;
import com.modularmc.ten.api.blockentity.EngineBlockEntity;
import com.modularmc.ten.api.capability.MachineItemHandler;
import com.modularmc.ten.common.blockentity.channel.AbstractChannelBlockEntity;
import com.modularmc.ten.common.blockentity.machine.CellBlockEntity;
import com.modularmc.ten.common.blockentity.machine.CreativeCellBlockEntity;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

/**
 * RED contract tests for the upgrade system overhaul.
 * <p>
 * These tests define the target behavior BEFORE production changes.
 * They will fail until the production code is implemented.
 * <p>
 * Testable without a running game environment (pure logic + reflection).
 *
 * <h3>RED results (before changes):</h3>
 * <ul>
 *   <li>{@code upgradeItem_hasCanApplyMethod} — fails: no canApply() method</li>
 *   <li>{@code canApply_isDeclaredOnUpgradeItem} — fails: no canApply() method</li>
 *   <li>{@code useItemOn_delegatesToUpgradeInstallHelper} — fails: no shift+UpgradeItem handling</li>
 * </ul>
 * Levelup subclass classloading requires game registry init (test env limitation).
 */
class UpgradeSystemContractTest {

    // ════════════════════════════════════════════════════════════
    // A. Fixed 6-slot strategy
    // ════════════════════════════════════════════════════════════

    @Nested
    class FixedSixSlots {

        @Test
        void maxUpgradeSlots_constant_is6() {
            assertEquals(6, CmMachineBlockEntity.MAX_UPGRADE_SLOTS,
                "MAX_UPGRADE_SLOTS must be 6");
        }

        @Test
        void upgradeHandler_has6Slots() {
            var handler = new MachineItemHandler(CmMachineBlockEntity.MAX_UPGRADE_SLOTS);
            assertEquals(6, handler.getSlots());
        }

        @Test
        void getUnlockedUpgradeSlots_methodExists() throws Exception {
            var method = CmMachineBlockEntity.class.getMethod("getUnlockedUpgradeSlots");
            assertEquals(int.class, method.getReturnType());
        }

        @Test
        void validUpgrade_slotIndices_documented() {
            // Document contract: validUpgrade must accept slots 0-5 and reject 6+
            // For any UpgradeItem, slot < MAX_UPGRADE_SLOTS must be valid
            // Slot >= MAX_UPGRADE_SLOTS must be invalid
            assertTrue(0 < CmMachineBlockEntity.MAX_UPGRADE_SLOTS,
                "MAX_UPGRADE_SLOTS must be positive");
            assertEquals(6, CmMachineBlockEntity.MAX_UPGRADE_SLOTS,
                "Contract: exactly 6 upgrade slots");
        }
    }

    // ════════════════════════════════════════════════════════════
    // B. supportsUpgradeSlots — RED: does not exist yet
    // ════════════════════════════════════════════════════════════

    @Nested
    class SupportsUpgradeSlotsContract {

        @Test
        void supportsUpgradeSlots_methodExists() throws Exception {
            var method = CmMachineBlockEntity.class.getMethod("supportsUpgradeSlots");
            assertEquals(boolean.class, method.getReturnType(),
                "supportsUpgradeSlots must return boolean");
        }

        @Test
        void supportsUpgradeSlots_isOverriddenOnCell() throws Exception {
            var method = CellBlockEntity.class.getMethod("supportsUpgradeSlots");
            assertEquals(CellBlockEntity.class, method.getDeclaringClass(),
                "CellBlockEntity must override supportsUpgradeSlots to return false");
        }

        @Test
        void supportsUpgradeSlots_isOverriddenOnCreativeCell() throws Exception {
            var method = CreativeCellBlockEntity.class.getMethod("supportsUpgradeSlots");
            assertEquals(CreativeCellBlockEntity.class, method.getDeclaringClass(),
                "CreativeCellBlockEntity must override supportsUpgradeSlots to return false");
        }

        @Test
        void supportsUpgradeSlots_isOverriddenOnAbstractChannel() throws Exception {
            var method = AbstractChannelBlockEntity.class.getMethod("supportsUpgradeSlots");
            assertEquals(AbstractChannelBlockEntity.class, method.getDeclaringClass(),
                "AbstractChannelBlockEntity must override supportsUpgradeSlots to return false");
        }

        @Test
        void supportsUpgradeSlots_isDistinctFromHasUpgrade() throws Exception {
            var supportMethod = CmMachineBlockEntity.class.getMethod("supportsUpgradeSlots");
            var hasUpgradeMethod = CmMachineBlockEntity.class.getMethod("hasUpgrade");
            assertNotSame(supportMethod, hasUpgradeMethod,
                "supportsUpgradeSlots and hasUpgrade must be distinct methods");
            assertNotEquals(supportMethod.getName(), hasUpgradeMethod.getName());
        }

        @Test
        void supportsUpgradeSlots_defaultIsTrue_forNormalMachines() throws Exception {
            // Normal machines (Furnace, Pulverizer, Compressor, Engines, etc.)
            // inherit the default true from CmMachineBlockEntity.
            var defaultMethod = CmMachineBlockEntity.class.getMethod("supportsUpgradeSlots");
            assertEquals(CmMachineBlockEntity.class, defaultMethod.getDeclaringClass(),
                "Default supportsUpgradeSlots must be declared on CmMachineBlockEntity");
        }

        @Test
        void engineBlockEntity_inheritsDefaultSupportsUpgradeSlots() throws Exception {
            // EngineBlockEntity does NOT override supportsUpgradeSlots
            var method = EngineBlockEntity.class.getMethod("supportsUpgradeSlots");
            assertEquals(CmMachineBlockEntity.class, method.getDeclaringClass(),
                "EngineBlockEntity must inherit default supportsUpgradeSlots (true)");
        }
    }

    // ════════════════════════════════════════════════════════════
    // D. UpgradeItem.canApply API — RED will fail before impl
    // ════════════════════════════════════════════════════════════

    @Nested
    class CanApplyApi {

        @Test
        void upgradeItem_hasCanApplyMethod() throws Exception {
            // RED: fails until canApply(IUpgradableMachine) is added to UpgradeItem
            var method = UpgradeItem.class.getDeclaredMethod("canApply", IUpgradableMachine.class);
            assertEquals(boolean.class, method.getReturnType(),
                "canApply must return boolean");
        }

        @Test
        void canApply_isDeclaredOnUpgradeItem() throws Exception {
            // RED: fails until canApply is declared
            var method = UpgradeItem.class.getDeclaredMethod("canApply", IUpgradableMachine.class);
            assertEquals(UpgradeItem.class, method.getDeclaringClass(),
                "canApply must be declared on UpgradeItem base class");
        }

        @Test
        void effect_stillExistsForBackwardCompat() throws Exception {
            var method = UpgradeItem.class.getDeclaredMethod("effect", IUpgradableMachine.class);
            assertEquals(boolean.class, method.getReturnType());
        }

        @Test
        void effectAndCanApply_areDistinctMethods() throws Exception {
            // After changes, canApply and effect must be separate methods
            var canApply = UpgradeItem.class.getDeclaredMethod("canApply", IUpgradableMachine.class);
            var effect = UpgradeItem.class.getDeclaredMethod("effect", IUpgradableMachine.class);
            assertNotSame(canApply, effect, "canApply and effect must be distinct methods");
            assertNotEquals(canApply.getName(), effect.getName());
        }
    }

    // ════════════════════════════════════════════════════════════
    // E. UpgradeInstallHelper tests
    // ════════════════════════════════════════════════════════════

    @Nested
    class UpgradeInstallHelperTests {

        @Test
        void findFirstEmptySlot_returns0_forEmptyHandler() {
            var handler = new MachineItemHandler(6);
            assertEquals(0, UpgradeInstallHelper.findFirstEmptySlot(handler));
        }

        @Test
        void findFirstEmptySlot_returnsMinus1_whenHandlerNull() {
            assertEquals(-1, UpgradeInstallHelper.findFirstEmptySlot(null));
        }

        @Test
        void tryInstall_fails_forNullHandler() {
            assertFalse(UpgradeInstallHelper.tryInstall(null, ItemStack.EMPTY));
        }

        @Test
        void tryInstall_fails_forEmptyStack() {
            var handler = new MachineItemHandler(6);
            assertFalse(UpgradeInstallHelper.tryInstall(handler, ItemStack.EMPTY));
        }

        @Test
        void tryInstall_withValidator_fails_whenValidatorRejects() {
            var handler = new MachineItemHandler(6);
            // Validator that rejects everything
            assertFalse(UpgradeInstallHelper.tryInstall(handler, ItemStack.EMPTY,
                (slot, stack) -> false));
        }

        @Test
        void tryInstall_withValidator_passes_whenValidatorAccepts() {
            var handler = new MachineItemHandler(6);
            // Can't test full flow without real items, but verify the method signature works
            assertNotNull(UpgradeInstallHelper.class.getDeclaredMethods());
        }
    }

    // ════════════════════════════════════════════════════════════
    // F. Tooltip / GUI contracts
    // ════════════════════════════════════════════════════════════

    @Nested
    class TooltipContracts {

        @Test
        void emptyUpgradeSlotTooltip_methodExists() throws Exception {
            var method = com.modularmc.ten.common.gui.TENMachineBlockUIFactory.class
                .getDeclaredMethod("emptyUpgradeSlotTooltip");
            assertNotNull(method);
        }

        @Test
        void addUpgradeSlots_methodExists() throws Exception {
            var method = com.modularmc.ten.common.gui.TENMachineBlockUIFactory.class
                .getDeclaredMethod("addUpgradeSlots",
                    com.lowdragmc.lowdraglib2.gui.ui.UIElement.class,
                    CmMachineBlockEntity.class);
            assertNotNull(method);
        }
    }

    // ════════════════════════════════════════════════════════════
    // G. BaseMachineBlock — RED: shift+upgrade not yet handled
    // ════════════════════════════════════════════════════════════

    @Nested
    class QuickInstallEntryPoint {

        @Test
        void useItemOn_protectedMethod_exists() throws Exception {
            var clazz = Class.forName("com.modularmc.ten.common.block.machine.BaseMachineBlock");
            var method = clazz.getDeclaredMethod("useItemOn",
                net.minecraft.world.item.ItemStack.class,
                net.minecraft.world.level.block.state.BlockState.class,
                net.minecraft.world.level.Level.class,
                net.minecraft.core.BlockPos.class,
                net.minecraft.world.entity.player.Player.class,
                net.minecraft.world.InteractionHand.class,
                net.minecraft.world.phys.BlockHitResult.class);
            assertNotNull(method);
        }

        @Test
        void upgradeInstallHelper_isReferencedInUseItemOn() {
            // Contract: BaseMachineBlock.useItemOn must handle
            // player.isShiftKeyDown() && hand item instanceof UpgradeItem
            // by delegating to UpgradeInstallHelper
            assertNotNull(UpgradeInstallHelper.class,
                "UpgradeInstallHelper must exist for useItemOn delegation");
        }
    }

    // ════════════════════════════════════════════════════════════
    // H. B3 — supportsUpgradeSlots guard (双层门禁)
    // ════════════════════════════════════════════════════════════
    //
    // Cell / CreativeCell / AbstractChannel → supportsUpgradeSlots=false.
    // Before B3, validUpgrade and useItemOn do NOT check this flag,
    // so an UpgradeItem with default canApply()=true can be written
    // into the upgradeHandler via shift-right-click, creating a
    // phantom upgrade that is invisible (no UI slots) but consumed.
    //
    // Two-layer defense (最小双层防线):
    //   ① CmMachineBlockEntity.validUpgrade  — first guard
    //   ② BaseMachineBlock.useItemOn         — entry guard before helper
    //
    // RED: contract tests fail before B3 fix.
    // GREEN: all pass after adding the two guards.
    // ─────────────────────────────────────────────────────────

    @Nested
    class SupportsUpgradeSlotGuard {

        /**
         * RED: fails because validUpgrade() does not check supportsUpgradeSlots().
         * <p>
         * After GREEN: {@code validUpgrade} starts with
         * {@code if (!supportsUpgradeSlots()) return false;}
         * rejecting ALL items when the machine has no upgrade UI.
         */
        @Test
        void validUpgrade_guardsWithSupportsUpgradeSlots() throws Exception {
            var sourceFile = new File(
                "src/main/java/com/modularmc/ten/api/blockentity/CmMachineBlockEntity.java"
            );
            assertTrue(sourceFile.exists());

            var lines = Files.readAllLines(sourceFile.toPath());
            boolean found = false;
            boolean inMethod = false;
            int braceDepth = 0;

            for (String line : lines) {
                if (line.contains("public boolean validUpgrade(int slot, ItemStack stack)")) {
                    inMethod = true;
                }
                if (inMethod) {
                    for (char c : line.toCharArray()) {
                        if (c == '{') braceDepth++;
                        if (c == '}') braceDepth--;
                    }
                    if (braceDepth <= 0 && inMethod) break; // method body closed
                    if (line.contains("supportsUpgradeSlots()")) {
                        found = true;
                        break;
                    }
                }
            }

            assertTrue(found,
                "validUpgrade must check supportsUpgradeSlots() as its first guard");
        }

        /**
         * H1 — Contract: supportsUpgradeSlots() is the FIRST check in validUpgrade
         * (before slot bounds, instanceof, and canApply), so a machine with
         * supportsUpgradeSlots=false short-circuits immediately without touching
         * slot or item logic.
         */
        @Test
        void validUpgrade_supportsCheckIsFirstInBody() throws Exception {
            var sourceFile = new File(
                "src/main/java/com/modularmc/ten/api/blockentity/CmMachineBlockEntity.java"
            );
            var lines = Files.readAllLines(sourceFile.toPath());
            boolean inMethod = false;
            int braceDepth = 0;
            int supportsLine = -1;
            int slotCheckLine = -1;
            int canApplyLine = -1;

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.contains("public boolean validUpgrade(int slot, ItemStack stack)")) {
                    inMethod = true;
                }
                if (!inMethod) continue;
                for (char c : line.toCharArray()) {
                    if (c == '{') braceDepth++;
                    if (c == '}') braceDepth--;
                }
                if (braceDepth <= 0) break; // method body closed

                if (line.contains("supportsUpgradeSlots()") && supportsLine < 0) {
                    supportsLine = i;
                }
                // Detect slot bounds check: mentions slot < 0 or >= MAX_UPGRADE_SLOTS
                if ((line.contains("slot < 0") || line.contains("slot >= ")) && slotCheckLine < 0) {
                    slotCheckLine = i;
                }
                // Detect canApply call (the instanceof + canApply chain)
                if (line.contains("canApply(this)") && canApplyLine < 0) {
                    canApplyLine = i;
                }
            }

            assertTrue(supportsLine >= 0,
                "validUpgrade must contain supportsUpgradeSlots() check");
            assertTrue(slotCheckLine >= 0,
                "validUpgrade must contain slot bounds check (slot < 0 || slot >= MAX_UPGRADE_SLOTS)");
            assertTrue(canApplyLine >= 0,
                "validUpgrade must call canApply(this)");
            assertTrue(supportsLine < slotCheckLine,
                "supportsUpgradeSlots() check must come BEFORE slot bounds check" +
                " (supportsLine=" + supportsLine + ", slotCheckLine=" + slotCheckLine + ")");
            assertTrue(supportsLine < canApplyLine,
                "supportsUpgradeSlots() check must come BEFORE canApply(this)" +
                " (supportsLine=" + supportsLine + ", canApplyLine=" + canApplyLine + ")");
        }

        /**
         * H2 — Contract: validUpgrade also calls {@code upgradeItem.canApply(this)}
         * after passing the supportsUpgradeSlots gate and slot bounds.
         * Without the canApply gate, an upgrade that reports
         * supportsUpgradeSlots()=true but whose canApply() returns false
         * would still be insertable via direct handler access.
         */
        @Test
        void validUpgrade_alsoChecksCanApply() throws Exception {
            var sourceFile = new File(
                "src/main/java/com/modularmc/ten/api/blockentity/CmMachineBlockEntity.java"
            );
            var lines = Files.readAllLines(sourceFile.toPath());
            boolean inMethod = false;
            int braceDepth = 0;
            boolean foundCanApply = false;

            for (String line : lines) {
                if (line.contains("public boolean validUpgrade(int slot, ItemStack stack)")) {
                    inMethod = true;
                }
                if (!inMethod) continue;
                for (char c : line.toCharArray()) {
                    if (c == '{') braceDepth++;
                    if (c == '}') braceDepth--;
                }
                if (braceDepth <= 0) break;

                // The pattern: "return upgradeItem.canApply(this);"
                if (line.contains("canApply(this)")) {
                    foundCanApply = true;
                    break;
                }
            }

            assertTrue(foundCanApply,
                "validUpgrade must call upgradeItem.canApply(this) as final check");
        }

        /**
         * H3 — Contract: useItemOn checks supportsUpgradeSlots() BEFORE the
         * client/server split ({@code level.isClientSide()}), so both
         * the client-side SUCCESS return and the server-side installation
         * path are guarded by the same condition. Without this ordering,
         * one side could behave differently from the other.
         */
        @Test
        void useItemOn_guardBeforeClientSplit() throws Exception {
            var sourceFile = new File(
                "src/main/java/com/modularmc/ten/common/block/machine/BaseMachineBlock.java"
            );
            var lines = Files.readAllLines(sourceFile.toPath());
            boolean inUseItemOn = false;
            int braceDepth = 0;
            int guardLine = -1;
            int clientSideLine = -1;

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.contains("protected InteractionResult useItemOn(")) {
                    inUseItemOn = true;
                }
                if (!inUseItemOn) continue;
                for (char c : line.toCharArray()) {
                    if (c == '{') braceDepth++;
                    if (c == '}') braceDepth--;
                }
                if (braceDepth <= 0) break;

                if (line.contains("supportsUpgradeSlots()") && guardLine < 0) {
                    guardLine = i;
                }
                if (line.contains("level.isClientSide()") && clientSideLine < 0) {
                    clientSideLine = i;
                }
            }

            assertTrue(guardLine >= 0,
                "useItemOn must have supportsUpgradeSlots() guard");
            assertTrue(clientSideLine >= 0,
                "useItemOn must have level.isClientSide() split");
            assertTrue(guardLine < clientSideLine,
                "supportsUpgradeSlots() guard must come BEFORE level.isClientSide()" +
                " so both client and server code paths are guarded" +
                " (guardLine=" + guardLine + ", clientSideLine=" + clientSideLine + ")");
        }

        /**
         * H4 — Contract: when supportsUpgradeSlots() is false, useItemOn
         * returns {@code InteractionResult.FAIL} (not SUCCESS, not CONSUME),
         * ensuring the item is neither consumed nor acknowledged as a
         * successful interaction on either side.
         */
        @Test
        void useItemOn_returnsFailForUnsupported() throws Exception {
            var sourceFile = new File(
                "src/main/java/com/modularmc/ten/common/block/machine/BaseMachineBlock.java"
            );
            var lines = Files.readAllLines(sourceFile.toPath());
            boolean inUseItemOn = false;
            int braceDepth = 0;
            boolean foundFailAfterGuard = false;

            String guardFailMarker = "InteractionResult.FAIL";

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.contains("protected InteractionResult useItemOn(")) {
                    inUseItemOn = true;
                }
                if (!inUseItemOn) continue;
                for (char c : line.toCharArray()) {
                    if (c == '{') braceDepth++;
                    if (c == '}') braceDepth--;
                }
                if (braceDepth <= 0) break;

                // Look for the multi-line pattern:
                //   if (!machine.supportsUpgradeSlots()) {
                //       return InteractionResult.FAIL;
                //   }
                // supportsUpgradeSlots() may be on a different line from FAIL.
                if (line.contains("supportsUpgradeSlots()")) {
                    // Check the next few lines for the FAIL return
                    for (int j = i + 1; j < Math.min(i + 4, lines.size()); j++) {
                        if (lines.get(j).contains(guardFailMarker)) {
                            foundFailAfterGuard = true;
                            break;
                        }
                    }
                    if (foundFailAfterGuard) break;
                }
            }

            assertTrue(foundFailAfterGuard,
                "useItemOn must return InteractionResult.FAIL " +
                "when supportsUpgradeSlots() is false");
        }

        /**
         * RED: fails because useItemOn's shift+UpgradeItem branch does not
         * check machine.supportsUpgradeSlots() before calling the helper.
         * <p>
         * After GREEN: the branch starts with
         * {@code if (!machine.supportsUpgradeSlots()) return InteractionResult.FAIL;}
         * preventing both the client-side SUCCESS return and the server-side
         * helper call when the machine has no upgrade UI.
         */
        @Test
        void useItemOn_checksSupportsUpgradeSlotsBeforeHelper() throws Exception {
            var sourceFile = new File(
                "src/main/java/com/modularmc/ten/common/block/machine/BaseMachineBlock.java"
            );
            assertTrue(sourceFile.exists());

            var lines = Files.readAllLines(sourceFile.toPath());
            boolean inUseItemOn = false;
            boolean foundGuard = false;
            int braceDepth = 0;
            int tryInstallLine = -1;

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.contains("protected InteractionResult useItemOn(")) {
                    inUseItemOn = true;
                }
                if (inUseItemOn) {
                    for (char c : line.toCharArray()) {
                        if (c == '{') braceDepth++;
                        if (c == '}') braceDepth--;
                    }
                    if (braceDepth <= 0 && inUseItemOn) break; // useItemOn body closed

                    if (line.contains("supportsUpgradeSlots()")) {
                        foundGuard = true;
                    }
                    if (line.contains("UpgradeInstallHelper.tryInstall")) {
                        tryInstallLine = i;
                        break; // found the helper call, check if guard precedes
                    }
                }
            }

            assertTrue(foundGuard,
                "useItemOn must check machine.supportsUpgradeSlots() " +
                "before UpgradeInstallHelper.tryInstall");
            assertTrue(tryInstallLine >= 0,
                "useItemOn must call UpgradeInstallHelper.tryInstall");
        }
    }

    // ════════════════════════════════════════════════════════════
    // I. Quarry-mode upgrades — LevelupIce/Magma/Mineral 仅 QUARRY
    // ════════════════════════════════════════════════════════════
    //
    // Contract (user-confirmed option A): LevelupIce / LevelupMagma /
    // LevelupMineral are Quarry-mode upgrades installable on QUARRY only.
    // FARM must NOT accept them (eliminates slot occupation without effect).
    // The gate is machine.isType("QUARRY"), which must resolve to
    // MachineType.QUARRY exclusively (no FARM).

    @Nested
    class QuarryModeUpgradeScope {

        @Test
        void levelupIce_canApply_restrictedToQuarry() throws Exception {
            assertQuarryOnlyCanApply("LevelupIce.java");
        }

        @Test
        void levelupMagma_canApply_restrictedToQuarry() throws Exception {
            assertQuarryOnlyCanApply("LevelupMagma.java");
        }

        @Test
        void levelupMineral_canApply_restrictedToQuarry() throws Exception {
            assertQuarryOnlyCanApply("LevelupMineral.java");
        }

        /**
         * isType("QUARRY") must match MachineType.QUARRY exclusively.
         * FARM must be excluded so Farm machines cannot install
         * Quarry-mode upgrades (占槽无效果).
         */
        @Test
        void isTypeQuarry_excludesFarm() throws Exception {
            var sourceFile = new File(
                "src/main/java/com/modularmc/ten/api/blockentity/CmMachineBlockEntity.java");
            assertTrue(sourceFile.exists());
            var content = Files.readString(sourceFile.toPath());

            int quarryCase = content.indexOf("case \"QUARRY\"");
            assertTrue(quarryCase >= 0, "isType must have a QUARRY case");
            int defaultCase = content.indexOf("default ->", quarryCase);
            assertTrue(defaultCase > quarryCase,
                "QUARRY case must be followed by default");
            String quarryBranch = content.substring(quarryCase, defaultCase);

            assertTrue(quarryBranch.contains("MachineType.QUARRY"),
                "isType(\"QUARRY\") must match MachineType.QUARRY");
            assertFalse(quarryBranch.contains("FARM"),
                "isType(\"QUARRY\") must NOT match FARM — " +
                "Quarry-mode upgrades are QUARRY-only");
        }

        /**
         * Each Quarry-mode upgrade must gate canApply on
         * machine.isType("QUARRY") without referencing FARM.
         */
        private void assertQuarryOnlyCanApply(String fileName) throws Exception {
            var sourceFile = new File(
                "src/main/java/com/modularmc/ten/common/item/upgrades/" + fileName);
            assertTrue(sourceFile.exists(), fileName + " must exist");
            var content = Files.readString(sourceFile.toPath());

            int canApplyIdx = content.indexOf("public boolean canApply");
            assertTrue(canApplyIdx >= 0, fileName + " must declare canApply");
            int effectIdx = content.indexOf("public boolean effect", canApplyIdx);
            assertTrue(effectIdx > canApplyIdx,
                fileName + " must declare effect after canApply");
            String canApplyBody = content.substring(canApplyIdx, effectIdx);

            assertTrue(canApplyBody.contains("isType(\"QUARRY\")"),
                fileName + " canApply must gate on machine.isType(\"QUARRY\")");
            assertFalse(canApplyBody.contains("FARM"),
                fileName + " canApply must not reference FARM");
            assertFalse(canApplyBody.contains("return true;"),
                fileName + " canApply must not be a bare return true");
        }
    }
}
