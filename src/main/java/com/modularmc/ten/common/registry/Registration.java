package com.modularmc.ten.common.registry;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.registry.registrate.TENRegistrate;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;

public class Registration {

    public static final TENRegistrate REGISTRATE = TENRegistrate.create(TEN.MOD_ID, false);

    static {
        Registration.REGISTRATE.defaultCreativeTab((ResourceKey<CreativeModeTab>) null);
    }

    private Registration() {}
}
