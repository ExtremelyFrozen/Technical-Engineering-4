package com.modularmc.ten.api.option;

public class FaceOption {

    public static final int OFF = 0;
    public static final int IN = 1;
    public static final int OUT = 2;
    public static final int BOTH = 3;
    public static final int BE_IN = 4;
    public static final int BE_OUT = 5;

    public static int size() {
        return 6;
    }

    public static String toStr(int mode) {
        return switch (mode) {
            case OFF -> "off";
            case IN -> "in";
            case OUT -> "out";
            case BOTH -> "both";
            case BE_IN -> "be_in";
            case BE_OUT -> "be_out";
            default -> "off";
        };
    }
}
