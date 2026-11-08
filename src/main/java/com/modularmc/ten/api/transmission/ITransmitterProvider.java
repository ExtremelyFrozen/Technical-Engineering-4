package com.modularmc.ten.api.transmission;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

/**
 * 传输节点宿主接口（BE 实现，移植自 TE4-New，有裁剪）：
 * New 版的 sendUpdatePacket/payload 同步未移植——连接外观改由
 * {@link CableRenderState} 经 {@code ModelData} 驱动，不再写进 BlockState。
 */
public interface ITransmitterProvider {

    BlockPos getBlockPos();

    @Nullable
    Level getLevel();

    boolean isInvalid();

    /** 状态变化后驱动持久化与渲染态刷新。 */
    void notifyChanges();

    Transmitter<?, ?, ?> getTransmitter();

    /** 打包后的连接/active 渲染态；方块碰撞箱在服务端也读它，不能只放客户端。 */
    default int getRenderPacked() {
        return 0;
    }

    /** 服务端重算渲染态（连接位或active变化后调用），不再触发 setBlock。 */
    default void refreshRenderState() {}
}
