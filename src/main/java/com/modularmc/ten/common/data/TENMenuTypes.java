package com.modularmc.ten.common.data;

import com.modularmc.ten.TEN;
import com.modularmc.ten.client.gui.CmContainerMachine;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class TENMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, TEN.MOD_ID);

    public static final Supplier<MenuType<CmContainerMachine>> MACHINE = MENUS.register("machine",
            () -> new MenuType<>(
                    (IContainerFactory<CmContainerMachine>) (id, inv, buf) -> {
                        int mType = buf != null && buf.readableBytes() > 0 ? buf.readInt() : -1;
                        int slots = buf != null && buf.readableBytes() > 0 ? buf.readInt() : 0;
                        var pos = buf != null && buf.readableBytes() > 0 ? buf.readBlockPos() : null;
                        return new CmContainerMachine(null, id, inv, null, pos, mType, slots);
                    },
                    FeatureFlags.DEFAULT_FLAGS));

    public static void init() {}
}
