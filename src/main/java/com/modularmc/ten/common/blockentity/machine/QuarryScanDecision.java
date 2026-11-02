package com.modularmc.ten.common.blockentity.machine;

/**
 * 采矿场一轮扫描收尾的决策：只判「接下来该往哪走」，不碰世界与方块实体，便于脱离游戏单测。
 */
public final class QuarryScanDecision {

    /** 一轮扫描结束后的走向。 */
    public enum ScanOutcome {
        /** 继续扫下一层。 */
        CONTINUE,
        /** 本轮挖到过东西，以机器自身为起点再扫一轮。 */
        RESCAN_FROM_START,
        /** 真挖尽：区域已空，保持停机。 */
        EXHAUSTED_STOP,
        /** 被工具挡下：停在记录点，等合规工具续扫。 */
        AWAIT_TOOL
    }

    private QuarryScanDecision() {}

    /**
     * @param reachedFloor     游标已到本维度可挖下界
     * @param passMined        本轮真的挖到过方块
     * @param passBlockedByTool 本轮有方块因当前工具挖不动或无掉落而被跳过
     */
    public static ScanOutcome decide(boolean reachedFloor, boolean passMined, boolean passBlockedByTool) {
        if (!reachedFloor) {
            return ScanOutcome.CONTINUE;
        }
        if (passMined) {
            return ScanOutcome.RESCAN_FROM_START;
        }
        // 空手到底：被工具挡下过就不是真挖尽，停在记录点等下一把合规工具
        return passBlockedByTool ? ScanOutcome.AWAIT_TOOL : ScanOutcome.EXHAUSTED_STOP;
    }
}
