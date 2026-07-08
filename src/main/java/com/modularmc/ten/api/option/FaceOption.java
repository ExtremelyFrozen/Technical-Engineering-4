package com.modularmc.ten.api.option;

public class FaceOption {

    public static final int NONE = -1;
    public static final int OFF = 0;
    public static final int IN = 1;
    public static final int OUT = 2;
    public static final int BE_IN = 3;
    public static final int BE_OUT = 4;
    public static final int BOTH = 5;

    public static int size() {
        return 6;
    }

    public static String toStr(int mode) {
        return switch (mode) {
            case OFF -> "off";
            case IN -> "in";
            case OUT -> "out";
            case BE_IN -> "be_in";
            case BE_OUT -> "be_out";
            case BOTH -> "both";
            default -> "off";
        };
    }

    public static boolean isIn(int mode) {
        return mode == IN || mode == BE_IN;
    }

    public static boolean isOut(int mode) {
        return mode == OUT || mode == BE_OUT;
    }

    public static boolean isPassive(int mode) {
        return mode == OFF || mode == BOTH || mode == BE_IN || mode == BE_OUT;
    }
}
