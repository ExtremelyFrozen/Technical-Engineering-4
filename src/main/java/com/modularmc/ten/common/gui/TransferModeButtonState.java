package com.modularmc.ten.common.gui;

/**
 * Pure state computation for transfer mode buttons (energy/item/fluid).
 * <p>
 * Each button has 4 visual states in handler.png, arranged at v offsets:
 *
 * <pre>
 *   state 0: v = 126  — unselected, not hovered
 *   state 1: v = 140  — unselected, hovered
 *   state 2: v = 154  — selected, not hovered
 *   state 3: v = 168  — selected, hovered
 * </pre>
 *
 * Formula: {@code stateIndex = (selected ? 2 : 0) + (hovered ? 1 : 0)}.
 * <p>
 * Thread-safe and stateless. No dependencies on Minecraft / LDLib / client code.
 */
public final class TransferModeButtonState {

    private TransferModeButtonState() {}

    /** Base V coordinate for the first (unselected, not hovered) state. */
    public static final int BASE_V = 126;

    /** Height of each state region, also the step between consecutive states. */
    public static final int STATE_SIZE = 14;

    /** Width and height of every transfer mode button. */
    public static final int SIZE = 14;

    /** U coordinate of the fluid mode button in handler.png. */
    public static final int U_FLUID = 76;

    /** U coordinate of the energy mode button in handler.png. */
    public static final int U_ENERGY = 91;

    /** U coordinate of the item mode button in handler.png. */
    public static final int U_ITEM = 106;

    /**
     * @param selected whether this button's mode is the currently active one
     * @param hovered  whether the mouse is over this button
     * @return state index in {@code [0, 3]}
     */
    public static int stateIndex(boolean selected, boolean hovered) {
        return (selected ? 2 : 0) + (hovered ? 1 : 0);
    }

    /**
     * @param stateIndex result of {@link #stateIndex(boolean, boolean)}
     * @return the V texture coordinate in handler.png
     * @throws IllegalArgumentException if stateIndex is not in {@code [0, 3]}
     */
    public static int textureV(int stateIndex) {
        if (stateIndex < 0 || stateIndex > 3) {
            throw new IllegalArgumentException("stateIndex must be 0..3, got " + stateIndex);
        }
        return BASE_V + stateIndex * STATE_SIZE;
    }

    /**
     * Radio-button click: clicking a different mode selects it;
     * clicking the already-selected mode keeps it (no cancellation).
     *
     * @param currentMode the currently selected mode {@code [0, 2]}
     * @param clickedMode the mode that was just clicked {@code [0, 2]}
     * @return the new selected mode
     * @throws IllegalArgumentException if either argument is outside {@code [0, 2]}
     */
    public static int click(int currentMode, int clickedMode) {
        if (currentMode < 0 || currentMode > 2) {
            throw new IllegalArgumentException("currentMode must be 0..2, got " + currentMode);
        }
        if (clickedMode < 0 || clickedMode > 2) {
            throw new IllegalArgumentException("clickedMode must be 0..2, got " + clickedMode);
        }
        return clickedMode;
    }
}
