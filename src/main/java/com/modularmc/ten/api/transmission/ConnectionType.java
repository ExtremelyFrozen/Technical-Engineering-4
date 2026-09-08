package com.modularmc.ten.api.transmission;

import net.minecraft.util.StringRepresentable;

/**
 * 连接模式（移植自 TE4-New）：NONE 不连接；NORMAL 双向；PUSH 仅输出；PULL 仅输入。
 * 扳手在「管道↔设备」边上四态循环切换，「管道↔管道」边只分 NONE/NORMAL。
 */
public enum ConnectionType implements StringRepresentable {

    NONE,
    NORMAL,
    PUSH,
    PULL;

    public static ConnectionType of(int ordinal) {
        return values()[ordinal % values().length];
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase();
    }

    public ConnectionType next() {
        return ConnectionType.of(ordinal() + 1);
    }

    public boolean isPullOrNormal() {
        return this == NORMAL || this == PULL;
    }

    public boolean isPushOrNormal() {
        return this == NORMAL || this == PUSH;
    }
}
