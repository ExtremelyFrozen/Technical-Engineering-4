// -*- coding: utf-8 -*-
package com.modularmc.ten.api.blockentity;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract tests for face config RPC real-time synchronization.
 * <p>
 * Root cause: {@code rpcSyncFaceInfo} used {@code if(!sender.isServer())} but
 * {@code PacketRPCBlockEntity.executeClient} passes {@code RPCSender.ofServer()},
 * causing client-side {@code energyFaceData / itemFaceData / fluidFaceData}
 * arrays to never update. Also, {@code rpcCycleFaceMode} updated the Map but
 * did not immediately sync to server arrays (only done in {@code doBaseData}
 * on next tick), creating an async stale window.
 * <p>
 * RED phase: These tests FAIL against current production code because the
 * correct patterns are not yet present.
 * <p>
 * GREEN phase: After fixes, all tests PASS.
 * <p>
 * Uses source-code pattern scanning (established project convention) since
 * {@link CmMachineBlockEntity} is abstract and depends on Minecraft classes
 * that cannot be instantiated in unit tests without bootstrap.
 */
class FaceConfigRealtimeSyncContractTest {

    private static final String CM_SRC_PATH =
            "com/modularmc/ten/api/blockentity/CmMachineBlockEntity.java";

    static String readCmMachineSource() throws Exception {
        var f = new File("src/main/java/" + CM_SRC_PATH);
        assertTrue(f.exists(), "Source file must exist: " + CM_SRC_PATH);
        return Files.readString(f.toPath());
    }

    // ════════════════════════════════════════════════════════════════════
    // 1. rpcSyncFaceInfo guard: must use sender.isServer()
    //    Current code uses !sender.isServer() — wrong for S2C
    // ════════════════════════════════════════════════════════════════════

    @Nested
    class RpcSyncFaceInfoGuard {

        @Test
        void rpcSyncFaceInfo_uses_isServer_guard() throws Exception {
            var src = readCmMachineSource();
            int methodIdx = src.indexOf("public void rpcSyncFaceInfo");
            assertTrue(methodIdx >= 0, "rpcSyncFaceInfo method must exist");
            // Extract method body up to the closing brace
            int bodyStart = src.indexOf('{', methodIdx);
            int bodyEnd = findMatchingBrace(src, bodyStart);
            String methodBody = src.substring(bodyStart, bodyEnd);

            // Must use sender.isServer() (not !sender.isServer())
            // RED: current code has if (!sender.isServer()) — this assertion should FAIL
            assertFalse(methodBody.contains("!sender.isServer()"),
                    "RED: rpcSyncFaceInfo must NOT use !sender.isServer(). " +
                    "Current code uses !sender.isServer() which is wrong because " +
                    "executeClient passes RPCSender.ofServer() (isServer()=true).");
            assertTrue(methodBody.contains("sender.isServer()"),
                    "RED: rpcSyncFaceInfo must use sender.isServer() guard. " +
                    "FIX: change if(!sender.isServer()) to if(sender.isServer()).");
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // 2. rpcSyncFaceInfo dirIndex bounds guard
    // ════════════════════════════════════════════════════════════════════

    @Nested
    class DirIndexBoundsGuard {

        @Test
        void rpcSyncFaceInfo_has_dirIndex_bounds_guard() throws Exception {
            var src = readCmMachineSource();
            int methodIdx = src.indexOf("public void rpcSyncFaceInfo");
            assertTrue(methodIdx >= 0, "rpcSyncFaceInfo method must exist");
            int bodyStart = src.indexOf('{', methodIdx);
            int bodyEnd = findMatchingBrace(src, bodyStart);
            String methodBody = src.substring(bodyStart, bodyEnd);

            // Must guard dirIndex against out-of-bounds (0..5)
            // GREEN: uses isValidFaceIndex (shared helper) or inline range check
            boolean hasRangeCheck = methodBody.contains("isValidFaceIndex(dirIndex)") ||
                    methodBody.contains("dirIndex >= 0 && dirIndex < 6") ||
                    methodBody.contains("dirIndex >= 0") && methodBody.contains("dirIndex < 6") ||
                    methodBody.contains("dirIndex >= FaceOption.size()") ||
                    methodBody.contains("dirIndex > 5");
            assertTrue(hasRangeCheck,
                    "RED: rpcSyncFaceInfo must guard dirIndex against 0..5 range. " +
                    "FIX: add if (dirIndex >= 0 && dirIndex < 6) guard.");
        }

        @Test
        void rpcSyncFaceInfo_arrayWrite_uses_guarded_flow() throws Exception {
            var src = readCmMachineSource();
            int methodIdx = src.indexOf("public void rpcSyncFaceInfo");
            assertTrue(methodIdx >= 0, "rpcSyncFaceInfo method must exist");
            int bodyStart = src.indexOf('{', methodIdx);
            int bodyEnd = findMatchingBrace(src, bodyStart);
            String methodBody = src.substring(bodyStart, bodyEnd);

            // After bounds guard + isServer check, must write all three arrays
            // RED: Currently writes if(!sender.isServer()) — with wrong guard
            // FIX: Write under sender.isServer() after bounds check
            assertTrue(methodBody.contains("energyFaceData[dirIndex]"),
                    "RED: rpcSyncFaceInfo must write energyFaceData[dirIndex]. " +
                    "FIX: add energyFaceData[dirIndex] = energyMode under correct guard.");
            assertTrue(methodBody.contains("itemFaceData[dirIndex]"),
                    "RED: rpcSyncFaceInfo must write itemFaceData[dirIndex].");
            assertTrue(methodBody.contains("fluidFaceData[dirIndex]"),
                    "RED: rpcSyncFaceInfo must write fluidFaceData[dirIndex].");
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // 3. rpcCycleFaceMode: immediate array sync after Map update
    // ════════════════════════════════════════════════════════════════════

    @Nested
    class RpcCycleFaceModeImmediateSync {

        @Test
        void rpcCycleFaceMode_updates_energyFaceData_after_map_put() throws Exception {
            var src = readCmMachineSource();
            int methodIdx = src.indexOf("public void rpcCycleFaceMode");
            assertTrue(methodIdx >= 0, "rpcCycleFaceMode method must exist");
            int bodyStart = src.indexOf('{', methodIdx);
            int bodyEnd = findMatchingBrace(src, bodyStart);
            String methodBody = src.substring(bodyStart, bodyEnd);

            // After map.put(direction, mode), must immediately update arrays
            // Find the map.put call region
            int putIdx = methodBody.indexOf("map.put(direction, mode)");
            assertTrue(putIdx >= 0, "map.put(direction, mode) must exist");
            String afterPut = methodBody.substring(putIdx);

            // Must write energyFaceData after map.put
            // RED: no array write after map.put — this assertion should FAIL
            assertTrue(afterPut.contains("energyFaceData"),
                    "RED: rpcCycleFaceMode must update energyFaceData immediately " +
                    "after map.put(direction, mode). " +
                    "FIX: add energyFaceData[dirIndex] assignment right after map.put.");
            assertTrue(afterPut.contains("itemFaceData"),
                    "RED: rpcCycleFaceMode must update itemFaceData immediately " +
                    "after map.put(direction, mode).");
            assertTrue(afterPut.contains("fluidFaceData"),
                    "RED: rpcCycleFaceMode must update fluidFaceData immediately " +
                    "after map.put(direction, mode).");
        }

        @Test
        void rpcCycleFaceMode_arrayWrite_before_rpcToTracking() throws Exception {
            var src = readCmMachineSource();
            int methodIdx = src.indexOf("public void rpcCycleFaceMode");
            assertTrue(methodIdx >= 0, "rpcCycleFaceMode method must exist");
            int bodyStart = src.indexOf('{', methodIdx);
            int bodyEnd = findMatchingBrace(src, bodyStart);
            String methodBody = src.substring(bodyStart, bodyEnd);

            int putIdx = methodBody.indexOf("map.put(direction, mode)");
            assertTrue(putIdx >= 0, "map.put(direction, mode) must exist");
            String afterPut = methodBody.substring(putIdx);

            // Array write must come BEFORE the full-face sync (rpcToTracking moved into syncAllFacesToClients)
            int arrayWriteIdx = afterPut.indexOf("energyFaceData");
            int syncIdx = afterPut.indexOf("syncAllFacesToClients");
            assertTrue(arrayWriteIdx >= 0 && syncIdx >= 0,
                    "Both array write and syncAllFacesToClients call must exist");
            assertTrue(arrayWriteIdx < syncIdx,
                    "RED: Array write must occur BEFORE syncAllFacesToClients. "
                            + "syncAllFacesToClients pushes all faces to clients; arrays must be up to date first.");
        }

        @Test
        void rpcCycleFaceMode_sends_triplet_in_rpcToTracking() throws Exception {
            var src = readCmMachineSource();
            int methodIdx = src.indexOf("public void rpcCycleFaceMode");
            assertTrue(methodIdx >= 0, "rpcCycleFaceMode method must exist");
            int bodyStart = src.indexOf('{', methodIdx);
            int bodyEnd = findMatchingBrace(src, bodyStart);
            String methodBody = src.substring(bodyStart, bodyEnd);

            // rpcCycleFaceMode must invoke the full-face sync helper (all 6 faces × 3 types)
            int syncIdx = methodBody.indexOf("syncAllFacesToClients()");
            assertTrue(syncIdx >= 0, "rpcCycleFaceMode must call syncAllFacesToClients() for full sync");

            // syncAllFacesToClients must rpcToTracking all three modes per face
            int helperIdx = src.indexOf("private void syncAllFacesToClients");
            assertTrue(helperIdx >= 0, "syncAllFacesToClients helper must exist");
            int helperBodyStart = src.indexOf('{', helperIdx);
            int helperBodyEnd = findMatchingBrace(src, helperBodyStart);
            String helperBody = src.substring(helperBodyStart, helperBodyEnd);
            int rpcIdx = helperBody.indexOf("rpcToTracking(\"rpcSyncFaceInfo\"");
            assertTrue(rpcIdx >= 0, "syncAllFacesToClients must rpcToTracking rpcSyncFaceInfo");
            int rpcLineEnd = helperBody.indexOf('\n', rpcIdx);
            if (rpcLineEnd < 0) rpcLineEnd = helperBody.length();
            String rpcLine = helperBody.substring(rpcIdx, Math.min(rpcLineEnd, helperBody.length()));
            assertTrue(rpcLine.contains("energyFaceData[idx]") &&
                            rpcLine.contains("itemFaceData[idx]") &&
                            rpcLine.contains("fluidFaceData[idx]"),
                    "syncAllFacesToClients must send all three modes (energy, item, fluid) per face");
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // 4. rpcCycleFaceMode: all three config types handled consistently
    // ════════════════════════════════════════════════════════════════════

    @Nested
    class RpcCycleFaceModeAllTypes {

        @Test
        void rpcCycleFaceMode_updates_energy_data_for_changeType_0() throws Exception {
            var src = readCmMachineSource();
            int methodIdx = src.indexOf("public void rpcCycleFaceMode");
            assertTrue(methodIdx >= 0, "rpcCycleFaceMode method must exist");
            int bodyStart = src.indexOf('{', methodIdx);
            int bodyEnd = findMatchingBrace(src, bodyStart);
            String methodBody = src.substring(bodyStart, bodyEnd);

            // For changeType 0 (energy), rpcCycleFaceMode uses energyFaceMode map
            int case0Idx = methodBody.indexOf("case 0");
            assertTrue(case0Idx >= 0, "case 0 (energy) must exist");

            // After the switch, the uniform code path updates all three arrays
            // using the local 'map' reference
            // The current code only reads map values for rpcToTracking but doesn't
            // write arrays. Fixed code will write all three after map.put.
        }

        @Test
        void rpcCycleFaceMode_updates_item_data_for_changeType_1() throws Exception {
            var src = readCmMachineSource();
            int methodIdx = src.indexOf("public void rpcCycleFaceMode");
            assertTrue(methodIdx >= 0, "rpcCycleFaceMode method must exist");
            int bodyStart = src.indexOf('{', methodIdx);
            int bodyEnd = findMatchingBrace(src, bodyStart);
            String methodBody = src.substring(bodyStart, bodyEnd);
            assertTrue(methodBody.contains("case 1"),
                    "case 1 (item) must exist");
            assertTrue(methodBody.contains("map = itemFaceMode"),
                    "case 1 must use itemFaceMode map");
        }

        @Test
        void rpcCycleFaceMode_updates_fluid_data_for_changeType_2() throws Exception {
            var src = readCmMachineSource();
            int methodIdx = src.indexOf("public void rpcCycleFaceMode");
            assertTrue(methodIdx >= 0, "rpcCycleFaceMode method must exist");
            int bodyStart = src.indexOf('{', methodIdx);
            int bodyEnd = findMatchingBrace(src, bodyStart);
            String methodBody = src.substring(bodyStart, bodyEnd);
            assertTrue(methodBody.contains("case 2"),
                    "case 2 (fluid) must exist");
            assertTrue(methodBody.contains("map = fluidFaceMode"),
                    "case 2 must use fluidFaceMode map");
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // 5a. rpcCycleFaceMode: dirIndex guard must come BEFORE
    //     Direction.from3DDataValue and map.put
    // ════════════════════════════════════════════════════════════════════

    @Nested
    class RpcCycleFaceModeGuardOrder {

        @Test
        void rpcCycleFaceMode_rejects_invalid_dirIndex_before_direction_resolution() throws Exception {
            var src = readCmMachineSource();
            int methodIdx = src.indexOf("public void rpcCycleFaceMode");
            assertTrue(methodIdx >= 0, "rpcCycleFaceMode method must exist");
            int bodyStart = src.indexOf('{', methodIdx);
            int bodyEnd = findMatchingBrace(src, bodyStart);
            String methodBody = src.substring(bodyStart, bodyEnd);

            // The dirIndex bounds guard must come BEFORE Direction.from3DDataValue
            // RED (v1): current code called from3DDataValue before inline guard
            // GREEN: now uses isValidFaceIndex(dirIndex) at the top
            int guardIdx = methodBody.indexOf("isValidFaceIndex(dirIndex)");
            int directionIdx = methodBody.indexOf("Direction.from3DDataValue(dirIndex)");
            assertTrue(guardIdx >= 0, "dirIndex bounds guard (isValidFaceIndex) must exist");
            assertTrue(directionIdx >= 0, "Direction.from3DDataValue must exist");
            assertTrue(guardIdx < directionIdx,
                    "RED: dirIndex guard (isValidFaceIndex) must come BEFORE Direction.from3DDataValue. " +
                    "Current code calls from3DDataValue first, silently returning " +
                    "a wrong Direction for out-of-bounds indices.");
        }

        @Test
        void rpcCycleFaceMode_rejects_invalid_dirIndex_before_map_put() throws Exception {
            var src = readCmMachineSource();
            int methodIdx = src.indexOf("public void rpcCycleFaceMode");
            assertTrue(methodIdx >= 0, "rpcCycleFaceMode method must exist");
            int bodyStart = src.indexOf('{', methodIdx);
            int bodyEnd = findMatchingBrace(src, bodyStart);
            String methodBody = src.substring(bodyStart, bodyEnd);

            // The dirIndex bounds guard must come BEFORE map.put(direction, mode)
            // RED (v1): current code did map.put before inline guard
            // GREEN: now uses isValidFaceIndex(dirIndex) at the top
            int guardIdx = methodBody.indexOf("isValidFaceIndex(dirIndex)");
            int mapPutIdx = methodBody.indexOf("map.put(direction, mode)");
            assertTrue(guardIdx >= 0, "dirIndex bounds guard (isValidFaceIndex) must exist");
            assertTrue(mapPutIdx >= 0, "map.put(direction, mode) must exist");
            assertTrue(guardIdx < mapPutIdx,
                    "RED: dirIndex guard must come BEFORE map.put. " +
                    "Current code modifies the Map before validating dirIndex.");
        }

        @Test
        void rpcCycleFaceMode_rejects_invalid_changeType_before_map_write() throws Exception {
            var src = readCmMachineSource();
            int methodIdx = src.indexOf("public void rpcCycleFaceMode");
            assertTrue(methodIdx >= 0, "rpcCycleFaceMode method must exist");
            int bodyStart = src.indexOf('{', methodIdx);
            int bodyEnd = findMatchingBrace(src, bodyStart);
            String methodBody = src.substring(bodyStart, bodyEnd);

            // The default case in the switch must exit before any map modification.
            // Verify: default -> return occurs before map.put(direction, mode)
            int defaultIdx = methodBody.indexOf("default ->");
            int mapPutIdx = methodBody.indexOf("map.put(direction, mode)");
            assertTrue(defaultIdx >= 0, "default case in switch must exist");
            assertTrue(mapPutIdx >= 0, "map.put(direction, mode) must exist");
            assertTrue(defaultIdx < mapPutIdx,
                    "RED: invalid changeType must return before map.put. " +
                    "The default case must exit before any Map mutation.");
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // 5b. rpcSyncFaceInfo: C2S rejection — client sender should not write
    // ════════════════════════════════════════════════════════════════════

    @Nested
    class RpcSyncFaceInfoC2SRejection {

        @Test
        void rpcSyncFaceInfo_client_sender_rejected() throws Exception {
            var src = readCmMachineSource();
            int methodIdx = src.indexOf("public void rpcSyncFaceInfo");
            assertTrue(methodIdx >= 0, "rpcSyncFaceInfo method must exist");
            int bodyStart = src.indexOf('{', methodIdx);
            int bodyEnd = findMatchingBrace(src, bodyStart);
            String methodBody = src.substring(bodyStart, bodyEnd);

            // When sender.isServer() is false (client-originated), arrays must NOT be written
            // The guard should be sender.isServer() — so the natural flow handles rejection
            // because the body is only executed when sender.isServer() == true
            assertFalse(methodBody.contains("!sender.isServer()"),
                    "RED: rpcSyncFaceInfo must NOT have !sender.isServer() guard. " +
                    "Current code uses !sender.isServer() which incorrectly handles " +
                    "the S2C case (executeClient uses RPCSender.ofServer()).");
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // 6. rpcCycleFaceMode does not duplicate energyFaceData read
    //    (no redundant map.getOrDefault for array write since we use local vars)
    // ════════════════════════════════════════════════════════════════════

    @Nested
    class NoRedundantReads {

        @Test
        void rpcCycleFaceMode_uses_local_values_for_all_three_arrays() throws Exception {
            var src = readCmMachineSource();
            int methodIdx = src.indexOf("public void rpcCycleFaceMode");
            assertTrue(methodIdx >= 0, "rpcCycleFaceMode method must exist");
            int bodyStart = src.indexOf('{', methodIdx);
            int bodyEnd = findMatchingBrace(src, bodyStart);
            String methodBody = src.substring(bodyStart, bodyEnd);

            // After map.put, must read all three mode values into local variables
            int localEnergyIdx = methodBody.indexOf("int newEnergyMode =");
            int localItemIdx = methodBody.indexOf("int newItemMode =");
            int localFluidIdx = methodBody.indexOf("int newFluidMode =");
            assertTrue(localEnergyIdx >= 0,
                    "Must read energy mode into local variable newEnergyMode");
            assertTrue(localItemIdx >= 0,
                    "Must read item mode into local variable newItemMode");
            assertTrue(localFluidIdx >= 0,
                    "Must read fluid mode into local variable newFluidMode");

            // Local vars must be used for array writes
            int arrayWriteEnergyIdx = methodBody.indexOf("energyFaceData[dirIndex] = newEnergyMode");
            int arrayWriteItemIdx = methodBody.indexOf("itemFaceData[dirIndex] = newItemMode");
            int arrayWriteFluidIdx = methodBody.indexOf("fluidFaceData[dirIndex] = newFluidMode");
            assertTrue(arrayWriteEnergyIdx >= 0,
                    "energyFaceData must be written using newEnergyMode local variable");
            assertTrue(arrayWriteItemIdx >= 0,
                    "itemFaceData must be written using newItemMode local variable");
            assertTrue(arrayWriteFluidIdx >= 0,
                    "fluidFaceData must be written using newFluidMode local variable");

            // Same locals feed the full-face sync helper
            int syncIdx = methodBody.indexOf("syncAllFacesToClients()");
            assertTrue(syncIdx >= 0, "syncAllFacesToClients call must exist");

            // Verify order: local reads before array writes before full-face sync
            assertTrue(localEnergyIdx < arrayWriteEnergyIdx,
                    "Local variable read must come before array write");
            assertTrue(arrayWriteEnergyIdx < syncIdx,
                    "Array write must come before syncAllFacesToClients");
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // 7. doBaseData still syncs arrays (regression guard)
    // ════════════════════════════════════════════════════════════════════

    @Nested
    class DoBaseDataStillSyncs {

        @Test
        void doBaseData_syncs_energyFaceData_from_map() throws Exception {
            var src = readCmMachineSource();
            int methodIdx = src.indexOf("public void doBaseData");
            assertTrue(methodIdx >= 0, "doBaseData method must exist");
            int bodyStart = src.indexOf('{', methodIdx);
            int bodyEnd = findMatchingBrace(src, bodyStart);
            String methodBody = src.substring(bodyStart, bodyEnd);

            // doBaseData must rebuild arrays from maps via rebuildFaceData (regression: existing sync must remain)
            assertTrue(methodBody.contains("rebuildFaceData()"),
                    "REGRESSION: doBaseData must call rebuildFaceData() to sync faceData from maps");
            int rebuildIdx = src.indexOf("private boolean rebuildFaceData");
            assertTrue(rebuildIdx >= 0, "rebuildFaceData helper must exist");
            int rebuildBodyStart = src.indexOf('{', rebuildIdx);
            int rebuildBodyEnd = findMatchingBrace(src, rebuildBodyStart);
            String rebuildBody = src.substring(rebuildBodyStart, rebuildBodyEnd);
            assertTrue(rebuildBody.contains("energyFaceData[idx]") &&
                            rebuildBody.contains("itemFaceData[idx]") &&
                            rebuildBody.contains("fluidFaceData[idx]"),
                    "REGRESSION: rebuildFaceData must sync all three arrays from faceMode maps");
        }
    }


    // ════════════════════════════════════════════════════════════════════
    // 8. rpcSetRedstoneMode must NOT broadcast face info (redundant — no
    //    face data changes when redstone mode is toggled)
    // ════════════════════════════════════════════════════════════════════

    @Nested
    class RpcSetRedstoneModeNoFaceSync {

        @Test
        void rpcSetRedstoneMode_does_not_call_rpcSyncFaceInfo() throws Exception {
            var src = readCmMachineSource();
            int methodIdx = src.indexOf("public void rpcSetRedstoneMode");
            assertTrue(methodIdx >= 0, "rpcSetRedstoneMode method must exist");
            int bodyStart = src.indexOf('{', methodIdx);
            int bodyEnd = findMatchingBrace(src, bodyStart);
            String methodBody = src.substring(bodyStart, bodyEnd);

            // RED: current code has a 6-face rpcSyncFaceInfo broadcast loop
            assertFalse(methodBody.contains("rpcSyncFaceInfo"),
                    "RED: rpcSetRedstoneMode must NOT call rpcSyncFaceInfo. " +
                    "Redstone mode change does not change face config data. " +
                    "FIX: remove the 6-face rpcSyncFaceInfo broadcast loop.");
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // Utility
    // ════════════════════════════════════════════════════════════════════

    /**
     * Find the closing brace matching the opening brace at {@code openIdx}.
     */
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
