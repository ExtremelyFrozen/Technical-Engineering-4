package com.modularmc.ten.api.option;

/**
 * 槽位 IO 类型：INPUT 仅入、OUTPUT 仅出、IGNORE 忽略、BOTH 双向；
 * 驱动槽位校验与 GUI 快速移动路由。
 */
public enum IngredientType {

    INPUT,
    OUTPUT,
    IGNORE,
    BOTH;

    public boolean canOut() {
        return this == OUTPUT || this == BOTH;
    }

    public boolean canIn() {
        return this == INPUT || this == BOTH;
    }
}
