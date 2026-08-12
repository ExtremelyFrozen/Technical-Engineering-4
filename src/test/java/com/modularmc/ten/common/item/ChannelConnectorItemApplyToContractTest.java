// -*- coding: utf-8 -*-
package com.modularmc.ten.common.item;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract tests for {@code ChannelConnectorItem} face-config validation and
 * applied/join-failed message semantics.
 * <p>
 * Root cause 1: {@code applyTo} wrote face modes from the DataComponent without
 * validating the three face-mode lists — a malformed payload (size != 6 or an
 * out-of-range value) would throw IndexOutOfBounds or write invalid modes.
 * <p>
 * Root cause 2: when face config was written but the channel join failed, the
 * item first showed {@code join_failed} and then unconditionally showed
 * {@code applied}, leaving the user with contradictory feedback.
 * <p>
 * Uses source-code pattern scanning (established project convention).
 */
class ChannelConnectorItemApplyToContractTest {

    private static final String SRC_PATH =
            "com/modularmc/ten/common/item/ChannelConnectorItem.java";

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
    // 1. applyTo entry validation (size == 6, each value in FaceOption range)
    // ════════════════════════════════════════════════════════════════════

    @Nested
    class ApplyToEntryValidation {

        @Test
        void applyTo_validates_face_list_size() throws Exception {
            var src = readSource();
            String body = methodBody(src, "private static boolean isValidFaceList");

            assertTrue(body.contains("faces.size() != 6"),
                    "RED: applyTo must reject face lists whose size != 6. " +
                    "FIX: guard isValidFaceList with faces.size() != 6 before writing.");
        }

        @Test
        void applyTo_validates_face_value_range() throws Exception {
            var src = readSource();
            String body = methodBody(src, "private static boolean isValidFaceList");

            // Each value must satisfy 0 <= v < FaceOption.size()
            assertTrue(body.contains("FaceOption.size()"),
                    "RED: applyTo must bound face values by FaceOption.size(). " +
                    "FIX: reject values v < 0 or v >= FaceOption.size().");
            assertTrue(body.contains("v < 0") || body.contains("v < 0)"),
                    "RED: applyTo must reject negative face values. " +
                    "FIX: reject v < 0 in isValidFaceList.");
        }

        @Test
        void applyTo_returns_false_and_writes_nothing_on_invalid() throws Exception {
            var src = readSource();
            String body = methodBody(src, "private static boolean applyTo");

            // Validation must short-circuit BEFORE any face-map write so an
            // invalid payload never leaves a half-written config on the target.
            int guardIdx = body.indexOf("isValidFaceList");
            assertTrue(guardIdx >= 0, "applyTo must call isValidFaceList entry guard");
            int writeIdx = body.indexOf("energyFaceMode.put");
            assertTrue(writeIdx >= 0, "applyTo must write energyFaceMode");
            assertTrue(guardIdx < writeIdx,
                    "RED: entry validation must precede any write to target face maps. " +
                    "FIX: validate all three lists before the write loop, return false on invalid.");
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // 2. applied vs join-failed message semantics
    // ════════════════════════════════════════════════════════════════════

    @Nested
    class AppliedJoinFailedMessages {

        @Test
        void useOn_uses_applied_join_failed_when_join_fails() throws Exception {
            var src = readSource();
            String body = methodBody(src, "public InteractionResult useOn");

            // Face config already applied + join failed → single combined message
            assertTrue(body.contains("channel_connector.applied_join_failed"),
                    "RED: join failure after applied config must use applied_join_failed message. " +
                    "FIX: show \"applied_join_failed\" (face config applied, but join failed) " +
                    "instead of a bare join_failed followed by applied.");
        }

        @Test
        void useOn_does_not_use_legacy_join_failed_key() throws Exception {
            var src = readSource();
            String body = methodBody(src, "public InteractionResult useOn");

            assertFalse(body.contains("channel_connector.join_failed"),
                    "RED: legacy bare join_failed key must be replaced by applied_join_failed. " +
                    "FIX: remove channel_connector.join_failed usage.");
        }

        @Test
        void useOn_applied_only_shown_when_join_succeeded() throws Exception {
            var src = readSource();
            String body = methodBody(src, "public InteractionResult useOn");

            // "applied" must be inside an else (or conditional) of the joined flag,
            // not unconditionally appended after join handling.
            int joinFailedIdx = body.indexOf("applied_join_failed");
            int appliedIdx = body.indexOf("channel_connector.applied\"");
            assertTrue(joinFailedIdx >= 0 && appliedIdx >= 0,
                    "Both applied_join_failed and applied keys must exist in useOn");
            assertTrue(appliedIdx > joinFailedIdx,
                    "RED: applied message must be the success branch AFTER the join-failure branch. " +
                    "FIX: show applied only when join succeeded (else branch of !joined).");
        }

        @Test
        void useOn_invalid_config_message_for_malformed_data() throws Exception {
            var src = readSource();
            String body = methodBody(src, "public InteractionResult useOn");

            assertTrue(body.contains("channel_connector.invalid_config"),
                    "RED: applyTo returning false must surface an invalid_config message. " +
                    "FIX: on applyTo == false, send channel_connector.invalid_config and return.");
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
