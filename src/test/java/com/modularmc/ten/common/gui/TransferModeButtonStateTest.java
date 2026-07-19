// -*- coding: utf-8 -*-
package com.modularmc.ten.common.gui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

/**
 * RED test for TransferModeButtonState — wish API.
 * Tests will fail until the production class is implemented.
 */
class TransferModeButtonStateTest {

    // === State index computation: index = (selected ? 2 : 0) + (hovered ? 1 : 0) ===

    @ParameterizedTest(name = "stateIndex selected={0} hovered={1} -> {2}")
    @CsvSource({
        "false, false, 0",   // unselected, not hovered → unselected normal
        "false, true,  1",   // unselected, hovered     → unselected hover
        "true,  false, 2",   // selected, not hovered   → selected normal
        "true,  true,  3"    // selected, hovered       → selected hover
    })
    void stateIndex_fromSelectedAndHovered(boolean selected, boolean hovered, int expectedIndex) {
        assertEquals(expectedIndex, TransferModeButtonState.stateIndex(selected, hovered));
    }

    // === Texture V = BASE_V + stateIndex * STATE_SIZE ===

    @Test
    void textureV_usesCorrectConstants() {
        assertEquals(126, TransferModeButtonState.BASE_V);
        assertEquals(14, TransferModeButtonState.STATE_SIZE);
    }

    @ParameterizedTest(name = "textureV stateIndex={0} -> v={1}")
    @CsvSource({
        "0, 126",   // unselected normal
        "1, 140",   // unselected hover
        "2, 154",   // selected normal
        "3, 168"    // selected hover
    })
    void textureV_fromStateIndex(int stateIndex, int expectedV) {
        assertEquals(expectedV, TransferModeButtonState.textureV(stateIndex));
    }

    @ParameterizedTest(name = "textureV selected={0} hovered={1} -> v={2}")
    @CsvSource({
        "false, false, 126",
        "false, true,  140",
        "true,  false, 154",
        "true,  true,  168"
    })
    void textureV_fromSelectedAndHovered(boolean selected, boolean hovered, int expectedV) {
        assertEquals(expectedV, TransferModeButtonState.textureV(
            TransferModeButtonState.stateIndex(selected, hovered)));
    }

    // === Click semantics: exactly one selected, click-to-select, click-selected-keeps ===

    @Test
    void click_onDifferentMode_returnsNewMode() {
        // From mode 0 (energy), click mode 1 (item)
        int result = TransferModeButtonState.click(0, 1);
        assertEquals(1, result);
    }

    @Test
    void click_onSameMode_keepsSameMode() {
        // From mode 0, click mode 0 again — no cancellation
        int result = TransferModeButtonState.click(0, 0);
        assertEquals(0, result);
    }

    @Test
    void click_onAnyMode_cyclesAllThree() {
        // 0 -> 1, 1 -> 2, 2 -> 0 (clicking different)
        assertEquals(1, TransferModeButtonState.click(0, 1));
        assertEquals(2, TransferModeButtonState.click(1, 2));
        assertEquals(0, TransferModeButtonState.click(2, 0));
    }

    @Test
    void click_selectedKeepsValue_allModes() {
        for (int mode = 0; mode < 3; mode++) {
            assertEquals(mode, TransferModeButtonState.click(mode, mode),
                "clicking already-selected mode " + mode + " must keep it");
        }
    }

    // === Validation: illegal values must fail fast ===

    @Test
    void textureV_negativeStateIndex_throws() {
        assertThrows(IllegalArgumentException.class,
            () -> TransferModeButtonState.textureV(-1));
    }

    @Test
    void textureV_outOfRangeStateIndex_throws() {
        assertThrows(IllegalArgumentException.class,
            () -> TransferModeButtonState.textureV(4));
    }

    @Test
    void click_negativeCurrentMode_throws() {
        assertThrows(IllegalArgumentException.class,
            () -> TransferModeButtonState.click(-1, 0));
    }

    @Test
    void click_negativeClickedMode_throws() {
        assertThrows(IllegalArgumentException.class,
            () -> TransferModeButtonState.click(0, -1));
    }

    @Test
    void click_outOfRangeCurrent_throws() {
        assertThrows(IllegalArgumentException.class,
            () -> TransferModeButtonState.click(3, 0));
    }

    @Test
    void click_outOfRangeClicked_throws() {
        assertThrows(IllegalArgumentException.class,
            () -> TransferModeButtonState.click(0, 3));
    }

    // === Button constants: U positions for each type ===

    @Test
    void buttonU_fluidIs76() {
        assertEquals(76, TransferModeButtonState.U_FLUID);
    }

    @Test
    void buttonU_energyIs91() {
        assertEquals(91, TransferModeButtonState.U_ENERGY);
    }

    @Test
    void buttonU_itemIs106() {
        assertEquals(106, TransferModeButtonState.U_ITEM);
    }

    @Test
    void buttonSize_is14() {
        assertEquals(14, TransferModeButtonState.SIZE);
    }
}
