package com.modularmc.ten.common.data;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.recipe.CompatEnabledCondition;
import com.modularmc.ten.api.recipe.ProviderPriorityCondition;

import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import com.mojang.serialization.MapCodec;

import java.util.function.Supplier;

/** 自定义配方条件注册表（{@code neoforge:condition_codecs}）。 */
public final class TENRecipeConditions {

    private TENRecipeConditions() {}

    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITIONS = DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, TEN.MOD_ID);

    public static final Supplier<MapCodec<CompatEnabledCondition>> COMPAT_ENABLED = CONDITIONS.register("compat_enabled", () -> CompatEnabledCondition.CODEC);

    public static final Supplier<MapCodec<ProviderPriorityCondition>> PROVIDER_PRIORITY = CONDITIONS.register("provider_priority", () -> ProviderPriorityCondition.CODEC);
}
