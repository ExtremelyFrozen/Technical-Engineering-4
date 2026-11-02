package com.modularmc.ten.api.option;

/**
 * 红石控制模式常量：OFF 忽略红石、LOW 无信号时运行、HIGH 有信号时运行。
 */
public class RedstoneMode {

    public static final int OFF = 0;
    public static final int LOW = 1;
    public static final int HIGH = 2;

    public static int size() {
        return 3;
    }
}
