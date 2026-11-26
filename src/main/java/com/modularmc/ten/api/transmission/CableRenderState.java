package com.modularmc.ten.api.transmission;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.data.ModelProperty;

/**
 * 线缆/管道的渲染状态：六个方向的连接外观（0=无，1=管道间 part，2=设备 connect）加一个 active 位，
 * 打包进一个 int 交给 {@code ModelData} 同步。连接外观不写入 BlockState。
 * <p>
 * {@link ModelProperty} 虽在 {@code client.model.data} 包，但它是 NeoForge 通用扩展点的一部分：
 * {@code IBlockEntityExtension#getModelData()} 位于 {@code common.extensions} 且签名直接返回
 * {@code ModelData}，专用服同样加载这些类。本类不引用任何 {@code OnlyIn(Dist.CLIENT)} 的类型。
 */
public record CableRenderState(int packed) {

    public static final ModelProperty<CableRenderState> PROPERTY = new ModelProperty<>();

    /** active 位，避开低 12 位的六组连接值。 */
    private static final int ACTIVE_BIT = 1 << 12;

    /** 7 种线缆/管道的注册名，DataGen 与客户端模型层共用一份清单。 */
    public static final String[] VARIANTS = {
            "cable", "cable_quartz", "cable_azure", "cable_star",
            "pipe", "pipe_white", "pipe_black"
    };

    public static boolean isCable(String name) {
        return name.startsWith("cable");
    }

    public static String modelDir(String name) {
        return isCable(name) ? "cable" : "pipe";
    }

    /** 服务端从 transmitter 位图打包；客户端读取方块实体同步后的同一份值。 */
    public static int pack(Transmitter<?, ?, ?> transmitter, boolean active) {
        byte tran = transmitter.getTransmitterConnections();
        byte acc = transmitter.getAcceptorConnections();
        int packed = active ? ACTIVE_BIT : 0;
        for (Direction d : Direction.values()) {
            int conn;
            if (Transmitter.connectionBit((byte) (tran | acc), d)) {
                conn = Transmitter.connectionBit(tran, d) ? 1 : 2;
            } else {
                conn = 0;
            }
            packed |= conn << (d.ordinal() * 2);
        }
        return packed;
    }

    /** 按 {@link Direction#ordinal()} 取连接外观值。 */
    public int connection(int ordinal) {
        return (packed >> (ordinal * 2)) & 3;
    }

    public boolean active() {
        return (packed & ACTIVE_BIT) != 0;
    }

    public static int withActive(int packed, boolean active) {
        return active ? (packed | ACTIVE_BIT) : (packed & ~ACTIVE_BIT);
    }
}
