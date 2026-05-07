package com.modularmc.ten.common.data;

import com.modularmc.ten.TEN;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredRegister;

public class TENMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, TEN.MOD_ID);

    public static void init() {}
}
