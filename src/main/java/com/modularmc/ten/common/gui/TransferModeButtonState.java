package com.modularmc.ten.common.gui;

/**
 * 传输模式按钮的纯状态计算，无 Minecraft、LDLib 或客户端依赖。
 * <p>
 * 按钮使用 handler.png，四种状态的 V 坐标依次为 126、140、154、168，每格高 14px；
 * 状态顺序为未选中未悬停、未选中悬停、已选中未悬停、已选中悬停。
 */
public final class TransferModeButtonState {

    private TransferModeButtonState() {}

    public static final int BASE_V = 126;
    public static final int STATE_SIZE = 14;
    public static final int SIZE = 14;
    public static final int U_FLUID = 76;
    public static final int U_ENERGY = 91;
    public static final int U_ITEM = 106;

    /**
     * @param selected 是否为当前选中的模式
     * @param hovered  鼠标是否悬停
     * @return {@code [0, 3]} 内的纹理状态索引
     */
    public static int stateIndex(boolean selected, boolean hovered) {
        return (selected ? 2 : 0) + (hovered ? 1 : 0);
    }

    /**
     * @param stateIndex {@link #stateIndex(boolean, boolean)} 的结果
     * @return 纹理 V 坐标
     * @throws IllegalArgumentException stateIndex 不在 {@code [0, 3]} 内
     */
    public static int textureV(int stateIndex) {
        if (stateIndex < 0 || stateIndex > 3) {
            throw new IllegalArgumentException("stateIndex must be 0..3, got " + stateIndex);
        }
        return BASE_V + stateIndex * STATE_SIZE;
    }

    /**
     * 单选按钮点击：选择所点模式，重复点击已选中模式时保持不变。
     *
     * @param currentMode 当前选中的模式 {@code [0, 2]}
     * @param clickedMode 本次点击的模式 {@code [0, 2]}
     * @return 新的选中模式
     * @throws IllegalArgumentException 任一参数不在 {@code [0, 2]} 内
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
