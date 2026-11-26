package com.modularmc.ten.api.transmission;

import net.minecraft.util.StringRepresentable;

/**
 * 连接模式：NONE 不连接；NORMAL 为默认连接；PUSH 仅输出；PULL 仅输入。
 * 扳手在设备边只切换 PULL/NORMAL，在管道间边只切换 NONE/NORMAL。
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
