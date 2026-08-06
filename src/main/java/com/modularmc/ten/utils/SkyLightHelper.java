package com.modularmc.ten.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * 共享光照判定辅助工具 — 供 {@link com.modularmc.ten.common.blockentity.machine.SolarBlockEntity}
 * 和 {@link com.modularmc.ten.api.blockentity.CmMachineBlockEntity#tryInjectPhotosynEnergy()} 共用。
 * <p>
 * 纯 Level + BlockPos 输入，无 BlockEntity 依赖，不涉及任何 API 具体 BE 类型。
 * <p>
 * 判定三条件（全部满足才算有效光照）：
 * <ol>
 * <li>位置上方可看到天空 — {@link Level#canSeeSky}</li>
 * <li>不在下雨 — {@link Level#isRaining()}</li>
 * <li>昼间 — {@link Level#getOverworldClockTime()} % 24000 &lt; 12000</li>
 * </ol>
 */
public final class SkyLightHelper {

    private SkyLightHelper() {}

    /**
     * 检查指定位置是否处于有效太阳光照下。
     *
     * @param level 世界实例（可为 null）
     * @param pos   目标位置（可为 null）
     * @return true 当全部三项条件满足；false 当任意不满足、level 为 null、
     *         pos 为 null、或 level 是客户端
     */
    public static boolean hasEffectiveLight(Level level, BlockPos pos) {
        if (level == null || pos == null || level.isClientSide()) return false;
        if (!level.canSeeSky(pos.above())) return false;
        if (level.isRaining()) return false;
        // MC 26.1 API: getOverworldClockTime() returns overworld clock total ticks
        return level.getOverworldClockTime() % 24000L < 12000L;
    }
}
