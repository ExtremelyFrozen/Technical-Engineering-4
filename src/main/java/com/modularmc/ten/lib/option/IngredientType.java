package com.modularmc.ten.lib.option;

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
