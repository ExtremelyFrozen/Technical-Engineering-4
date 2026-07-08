package com.modularmc.ten.api.recipe;

public interface CmSerializer<T extends FormsCombinedRecipe> {

    int fallBackTime = 150;

    String id();
}
