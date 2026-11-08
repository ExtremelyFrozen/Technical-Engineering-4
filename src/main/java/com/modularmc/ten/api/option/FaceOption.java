package com.modularmc.ten.api.option;

/**
 * 机器面交互模式常量与判定。
 *
 * <p>
 * 语义分两类，判定统一走本类的方法，不要在调用点散写模式比较：
 * <ul>
 * <li><b>机器主动</b>：{@link #IN} 机器自拉、{@link #OUT} 机器自推；</li>
 * <li><b>被动</b>（中文语境里的「被」）：机器不动手，只等外部设备经 capability 读写——
 * {@link #BE_IN} 仅可被输入、{@link #BE_OUT} 仅可被输出、
 * {@link #BOTH} 既可被输入也可被输出。</li>
 * </ul>
 *
 * <p>
 * 「被动」不代表机器会替外部搬运：{@code BOTH} 是双向开放，不是双向自动传输。
 *
 * <p>
 * {@link #NONE}(-1) 是无效/未初始化面的哨兵；{@code size()} 返回 6，不含 NONE。
 */
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

    /** 机器是否从该面主动拉取：仅主动输入。 */
    public static boolean machinePulls(int mode) {
        return mode == IN;
    }

    /** 机器是否向该面主动推送：仅主动输出。 */
    public static boolean machinePushes(int mode) {
        return mode == OUT;
    }

    /**
     * 外部 capability 能否向该面写入（被输入）：被动输入 / 被动双向。
     *
     * <p>
     * 主动输出面额外放行——既有策略，允许外部从输出面补料；真正的接收仍由槽位类型
     * （{@code canIn}）与 {@code valid()} 约束，产物不会因此被塞进入口。
     */
    public static boolean allowsExternalInsert(int mode) {
        return mode == BE_IN || mode == BOTH || mode == OUT;
    }

    /** 外部 capability 能否从该面取出（被输出）：被动输出 / 被动双向。 */
    public static boolean allowsExternalExtract(int mode) {
        return mode == BE_OUT || mode == BOTH;
    }
}
